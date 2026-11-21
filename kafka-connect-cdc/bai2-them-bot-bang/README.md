# Bài 2: thêm bớt bảng trong source connector

Lab cho bài "Thêm bớt bảng trong Debezium connector" (bộ bài Kafka Connect trên Backend Internal). Dùng chung cụm Docker của thư mục cha (`../docker-compose.yml`).

## Chạy nhanh toàn bộ

Từ thư mục `kafka-connect-cdc`:

```bash
bash bai2-them-bot-bang/run-all.sh
```

Script dựng cụm mới, chạy lần lượt các phần A tới G dưới đây và in kết quả của từng bước. Mất khoảng 6 phút (chưa tính lần đầu tải image).

## Chạy từng bước

```bash
cd kafka-connect-cdc
source lib.sh          # hàm connect, put_config, status, topic, ops, sql
docker compose up -d --wait
sql < bai2-them-bot-bang/sql/init.sql
```

`init.sql` tạo bảng `products` (3 dòng), bảng `orders` (2 dòng, lúc đầu không capture) và bảng signal `debezium_signal`.

Các file cấu hình connector đánh số theo thứ tự dùng:

| File | Thay đổi so với file trước |
|---|---|
| `1-products-only.json` | Chỉ capture `products`, `schema.history.internal.store.only.captured.tables.ddl=true` |
| `2-add-orders.json` | Thêm `shop.orders` vào `table.include.list` |
| `3-recovery.json` | `store.only.captured.tables.ddl=false`, `snapshot.mode=recovery` |
| `4-after-recovery.json` | `snapshot.mode=initial` |
| `5`, `6`, `7-quickfix-*.json` | Cách sửa nhanh: connector mới, topic history mới, `snapshot.mode=when_needed` |
| `8-remove-orders.json` | Bỏ `orders` khỏi `table.include.list` |
| `9`, `10`, `11-purge-*.json` | `initial`, `when_needed`, `always` khi binlog đã bị purge |
| `12-no-data.json`, `13-initial-only.json` | Hai mode `no_data`, `initial_only` |
| `14`, `15-warn-*.json` | `inconsistent.schema.handling.mode=warn` |

### A. Tái hiện sự cố

```bash
put_config shop-source bai2-them-bot-bang/connectors/1-products-only.json
topic schema-history.shop | grep -o '"ddl" : "[^"]\{0,50\}'      # có products, KHÔNG có orders

put_config shop-source bai2-them-bot-bang/connectors/2-add-orders.json
sql -e "INSERT INTO shop.orders (product_id, qty, status) VALUES (3, 2, 'NEW');"
status shop-source      # connector RUNNING, task FAILED
docker compose logs connect | grep "whose schema isn't known"
```

Kết quả: `Encountered change event for table shop.orders whose schema isn't known to this connector`.

### B. Quy trình sửa: recovery rồi incremental snapshot

```bash
connect PUT /connectors/shop-source/stop
connect GET /connectors/shop-source/offsets       # offset vẫn còn, nằm trước event lỗi
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 \
  --delete --topic schema-history.shop
put_config shop-source bai2-them-bot-bang/connectors/3-recovery.json
connect PUT /connectors/shop-source/resume
```

Connector dựng lại schema history từ cấu trúc hiện tại của mọi bảng, đọc tiếp binlog từ offset cũ, và event INSERT từng làm task lỗi giờ thành message `op=c` trên `shop.shop.orders`. Bảng `products` không bị snapshot lại.

Lấy nốt các dòng `orders` có từ trước:

```bash
sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-orders-1', 'execute-snapshot',
  '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\"}');"
topic shop.shop.orders | grep -o '"after":{"id":[0-9]*\|"snapshot":"[a-z]*"\|"op":"[a-z]"' | paste - - -
put_config shop-source bai2-them-bot-bang/connectors/4-after-recovery.json
```

### C. Cách sửa nhanh: connector mới với `when_needed`

Dựng lại sự cố trên connector `quickfix-v1`, xóa connector đó rồi tạo `quickfix-v2` với topic history mới và `snapshot.mode=when_needed` (xem `run-all.sh`, phần C). Topic `quickfix.shop.products` từ 3 lên 6 message `r`: cả 3 sản phẩm bị gửi lại.

### D. Bớt bảng rồi thêm lại

Bỏ `orders` ra (`8-remove-orders.json`), ghi một dòng `GAP` vào `orders`, chờ offset đi qua event đó, rồi thêm `orders` lại (`4-after-recovery.json`). Dòng `GAP` không có trên topic. Lấp bằng incremental snapshot có điều kiện:

```bash
sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-orders-gap', 'execute-snapshot',
  '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\",
    \"additional-conditions\": [{\"data-collection\": \"shop.orders\", \"filter\": \"id >= 5\"}]}');"
```

### E. `snapshot.mode` khi binlog đã bị purge

Connector `purge-source` dừng, MySQL `FLUSH BINARY LOGS` rồi `PURGE BINARY LOGS TO '<file hiện tại>'`, connector chạy lại:

- **`initial`:** Task `FAILED`: `... but this is no longer available on the server. Reconfigure the connector to use a snapshot mode when needed.`
- **`when_needed`:** Tự snapshot lại, topic từ 3 lên 6 message `r`.
- **`always`:** Snapshot lại mỗi lần task khởi động: đổi config lên 9, restart lên 12.

### F. `no_data`, `initial_only`, `warn`

- **`no_data`:** Không có message `r`, chỉ có thay đổi xảy ra sau khi connector chạy.
- **`initial_only`:** 3 message `r`, rồi không đọc binlog nữa: UPDATE sau đó không lên topic.
- **`inconsistent.schema.handling.mode=warn`:** INSERT vào `orders` bị bỏ qua, chỉ có một dòng WARN trong log, task vẫn `RUNNING`, topic `warn.shop.orders` không được tạo.

### G. Reset offset bằng REST API

```bash
connect PUT /connectors/initonly-source/stop
connect DELETE /connectors/initonly-source/offsets
connect GET /connectors/initonly-source/offsets     # {"offsets":[]}
```

## Môi trường đã chạy

Như lab gốc: Docker Desktop 20.10.17, `apache/kafka:4.3.1`, `quay.io/debezium/connect:3.6.3.Final` (Kafka Connect 4.3.0), `mysql:8.0.41`. Chạy ngày 2026-11-21.
