# Lab Kafka Connect và CDC: MySQL → Kafka → PostgreSQL

Lab cho bài "CDC và Kafka Connect từ bên trong" (bộ bài Kafka Connect trên Backend Internal). Dựng một cụm nhỏ trên Docker:

- **MySQL 8.0** (nguồn): bật binlog ROW và GTID.
- **Kafka 4.3** (KRaft, một broker).
- **Kafka Connect** (image Debezium 3.6.3.Final, Connect runtime 4.3.0): một worker chế độ distributed.
- **PostgreSQL 16** (đích): nhận dữ liệu qua JDBC sink connector.

Ba connector chạy trên cùng worker:

| Connector | Loại | Việc làm |
|---|---|---|
| `mysql-source` | Debezium MySQL source | Đọc binlog, phát mọi thay đổi của bảng `shop.products` lên topic `shop.shop.products` |
| `outbox-source` | Debezium MySQL source + Outbox Event Router | Đọc bảng `shop.outbox`, phát mỗi dòng thành một event nghiệp vụ lên `outbox.event.<aggregatetype>` |
| `postgres-sink` | Debezium JDBC sink | Đọc `shop.shop.products`, upsert/delete vào bảng `products` của PostgreSQL |

## Cần có

- Docker và Docker Compose v2. Không mở cổng nào ra máy host, không mount thư mục, nên không đụng cổng đang dùng.
- Một shell bash. Trên Windows dùng Git Bash, và chạy `export MSYS_NO_PATHCONV=1` trước, để Git Bash không đổi đường dẫn `/opt/kafka/...` thành đường dẫn Windows.
- Khoảng 2,5 GB RAM trống cho Docker (Kafka 512 MB heap, Connect 768 MB heap, MySQL, PostgreSQL).

## Chạy

Mọi lệnh chạy trong thư mục này.

### 1. Dựng cụm

```bash
export MSYS_NO_PATHCONV=1   # chỉ cần trên Git Bash cho Windows
docker compose up -d --wait
```

Lần đầu phải tải image (khoảng 2 GB). Lệnh trả về khi cả 4 container ở trạng thái `healthy`.

Ngay lúc này Kafka đã có ba topic nội bộ của Connect, dù chưa có connector nào:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
# __consumer_offsets
# connect-configs
# connect-offsets
# connect-status
```

### 2. Tạo dữ liệu nguồn

```bash
docker compose exec -T mysql mysql -uroot -proot < sql/mysql-init.sql
```

File này tạo user `debezium` với đúng các quyền connector cần, database `shop`, bảng `products` (3 dòng) và bảng `outbox`.

### 3. Đăng ký connector

`PUT /connectors/<tên>/config` tạo connector nếu chưa có, cập nhật nếu đã có, nên chạy lại bao nhiêu lần cũng được:

```bash
for c in mysql-source outbox-source postgres-sink; do
  docker compose exec -T connect curl -s -o /dev/null -w "$c %{http_code}\n" \
    -X PUT -H "Content-Type: application/json" --data @- \
    http://localhost:8083/connectors/$c/config < connectors/$c.json
done
# mysql-source 201
# outbox-source 201
# postgres-sink 201
```

Kiểm tra trạng thái (đợi khoảng 10 giây cho snapshot xong):

```bash
for c in mysql-source outbox-source postgres-sink; do
  docker compose exec -T connect curl -s http://localhost:8083/connectors/$c/status; echo
done
```

Cả connector lẫn task đều phải ở `"state":"RUNNING"`. Task `FAILED` thì trường `trace` chứa stack trace của lỗi.

### 4. Xem snapshot

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic shop.shop.products --from-beginning --timeout-ms 5000 \
  --formatter-property print.key=true --formatter-property key.separator=" | "
```

Ba message đầu có `"op":"r"` (read): connector chụp lại 3 dòng đang có trước khi đọc binlog. Mỗi message gồm key (`{"id":1}`) và value (envelope `before` / `after` / `source` / `op` / `ts_ms`), cả hai đều kèm phần `schema` vì worker dùng `JsonConverter` với `schemas.enable=true` (mặc định).

### 5. Thay đổi dữ liệu

```bash
docker compose exec -T mysql mysql -uroot -proot < sql/mysql-changes.sql
```

File này INSERT một sản phẩm, đổi giá sản phẩm 1 kèm một dòng outbox trong cùng transaction, rồi DELETE sản phẩm 3. Chạy lại lệnh đọc topic ở bước 4: sau 3 message `r` có thêm 4 message:

| Message | `op` | `before` | `after` |
|---|---|---|---|
| INSERT `MN-04` | `c` | `null` | dòng mới |
| UPDATE giá `KB-01` | `u` | dòng cũ | dòng mới |
| DELETE `HP-03` | `d` | dòng cũ | `null` |
| Tombstone của `HP-03` | (value `null`) | | |

Cột `price` (`DECIMAL(12,2)`) hiện dạng `"B7BiQA=="`: kiểu `Decimal` của Kafka Connect được `JsonConverter` ghi thành chuỗi base64 của số nguyên không dấu phẩy (129000000, scale 2, tức 1290000.00). Đặt `decimal.handling.mode=string` trên source connector nếu consumer đọc JSON thô.

### 6. Xem dữ liệu đã sang PostgreSQL

```bash
docker compose exec -T postgres psql -U replica -d replica -c "SELECT id, sku, price, stock FROM products ORDER BY id;"
#  id |  sku  |   price    | stock
# ----+-------+------------+-------
#   1 | KB-01 | 1190000.00 |    50
#   2 | MS-02 |  450000.00 |   120
#   4 | MN-04 | 4590000.00 |    15
```

Sink tự tạo bảng (`schema.evolution=basic`), áp UPDATE bằng upsert theo khóa chính và xóa dòng 3 khi gặp event `d`.

Offset của sink nằm ở consumer group `connect-<tên connector>`:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --describe --group connect-postgres-sink
```

### 7. Xem event outbox

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic outbox.event.product --from-beginning --timeout-ms 5000 \
  --formatter-property print.key=true --formatter-property print.headers=true
```

Cùng một transaction ở bước 5 tạo ra hai loại message: topic `shop.shop.products` nhận ảnh của dòng `products` (`before` / `after`, đủ mọi cột), còn topic `outbox.event.product` nhận đúng event nghiệp vụ `{"newPrice":1190000,"oldPrice":1290000,"productId":1}` với key `1` (`aggregateid`) và header `id` (khóa chính của dòng outbox).

### 8. Xem checkpoint

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic connect-offsets --from-beginning --timeout-ms 5000 --formatter-property print.key=true
```

Key là `["<tên connector>",{"server":"<topic.prefix>"}]`, value là vị trí binlog đã phát xong (`file`, `pos`, `gtids`). Worker ghi offset mỗi `offset.flush.interval.ms`; lab hạ xuống 5 giây (mặc định 60 giây).

Restart worker rồi thêm một dòng để thấy connector đọc tiếp từ offset, không snapshot lại:

```bash
docker compose restart connect && docker compose up -d --wait connect
docker compose exec -T mysql mysql -uroot -proot \
  -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('CB-05','Cap USB-C',99000.00,500);"
sleep 10   # worker healthy rồi, connector cần thêm vài giây để chạy lại
docker compose logs connect | grep "previous offset indicating a completed snapshot"
docker compose exec kafka /opt/kafka/bin/kafka-get-offsets.sh --bootstrap-server kafka:9092 --topic shop.shop.products
# shop.shop.products:0:8   (7 message trước restart + 1 message của dòng mới)
```

### 9. Dọn

```bash
docker compose down
```

Không có volume nào, nên `down` xóa sạch dữ liệu.

## File

- **`docker-compose.yml`:** Bốn service, mỗi service có healthcheck để `up --wait` chờ đủ.
- **`sql/mysql-init.sql`:** User, quyền, database, hai bảng và dữ liệu ban đầu.
- **`sql/mysql-changes.sql`:** INSERT, UPDATE kèm dòng outbox trong cùng transaction, DELETE.
- **`connectors/*.json`:** Cấu hình ba connector, gửi bằng `PUT /connectors/<tên>/config`.

## Môi trường đã chạy

Docker Desktop 20.10.17 (Compose v2.6.1) trên Windows 10, VM Docker 5 CPU và 3,4 GiB RAM. Image: `apache/kafka:4.3.1`, `quay.io/debezium/connect:3.6.3.Final` (Kafka Connect 4.3.0, Java 25), `mysql:8.0.41`, `postgres:16.14`. Chạy ngày 2026-11-21.
