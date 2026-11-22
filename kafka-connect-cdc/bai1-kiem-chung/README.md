# Kiểm chứng bài 1: CDC và Kafka Connect từ bên trong

Các script trong thư mục này chạy lại từng chi tiết kỹ thuật của bài "CDC và Kafka Connect từ bên trong" (bộ bài Kafka Connect trên Backend Internal). Script dùng cụm Docker của thư mục cha (`../docker-compose.yml`), cộng thêm ba dịch vụ phụ khai báo trong `compose.yml`, mỗi dịch vụ một profile riêng:

- **`connect2` (profile `worker2`):** Worker thứ hai cùng `group.id`, để thử failover giữa hai worker.
- **`mysql2` (profile `gtid`):** Replica GTID của MySQL nguồn, cố tình không truyền `--log-bin`, `--binlog-format` để xem giá trị mặc định.
- **`pgsrc` (profile `pgsrc`):** PostgreSQL làm nguồn CDC, bật `wal_level=logical`.

## Chạy

Mọi lệnh chạy từ thư mục `kafka-connect-cdc`. Mỗi script tự dựng lại cụm từ đầu, nên chạy lần lượt từng script, không chạy hai script cùng lúc.

```bash
mkdir -p bai1-kiem-chung/ket-qua
bash bai1-kiem-chung/01-co-ban.sh > bai1-kiem-chung/ket-qua/01-co-ban.log 2>&1
```

- **Git Bash trên Windows:** `lib.sh` đã đặt sẵn `MSYS_NO_PATHCONV=1`.
- **Perl:** `phan-tich.pl` (đọc message, đo kích thước, giải mã `Decimal`) cần Perl có module `JSON::PP`. Git Bash có sẵn.
- **Kết quả:** Log, nội dung topic và file trung gian nằm trong `ket-qua/`, không commit.

## Các script

| Script | Kiểm chứng | Kết quả trên lab |
|---|---|---|
| `01-co-ban.sh` | Luồng MySQL → Kafka → PostgreSQL của bài: topic nội bộ, producer config của source task, snapshot, envelope, kích thước message, `connect-offsets`, SQL sink chạy trên PostgreSQL, `tasks.max`, xóa connector, sink `FAILED` rồi restart `onlyFailed` | Message UPDATE 3.627 byte, schema chiếm 81%; khóa toàn cục giữ 0,5 giây; xóa connector thì offset vẫn nằm lại trong `connect-offsets`; sink `tasks.max=3` trên topic 1 partition tạo 3 task nhưng chỉ task 0 nhận partition |
| `02-at-least-once.sh` | SIGKILL worker giữa hai chu kỳ flush (`offset.flush.interval.ms=60000`) | Dòng `ZZ-99` lên topic 2 lần cùng GTID; PostgreSQL chạy 3 câu upsert, bảng vẫn 1 dòng |
| `02b-offset-trong-transaction.sh` | Offset `{gtids 1-13, event 2, row 1}` ngay sau message DELETE (GTID 14) | SIGKILL rồi khởi động lại không gửi lại DELETE |
| `03-worker-failover.sh` | Hai worker, SIGKILL worker không phải leader | Task chạy lại ở worker còn lại sau 312 giây (`scheduled.rebalance.max.delay.ms` mặc định 5 phút); trong lúc chờ, status vẫn báo `RUNNING` trên worker đã chết |
| `04-gtid-failover.sh` | Replica GTID, primary chết, connector chuyển sang replica | Connector gửi tập GTID (`filename: , position: 4`); dòng ghi trước và sau failover đều lên topic đúng một lần |
| `04b-gtid-vi-tri.sh` | Vị trí của từng GTID trong binlog của primary và replica | Cùng `A:15` nằm ở `mysql-bin.000003` pos 3270 trên primary, `binlog.000001` pos 2728 trên replica |
| `05-postgres-source.sh` | PostgreSQL làm nguồn: publication, `REPLICA IDENTITY`, WAL tồn đọng khi bảng được theo dõi ít ghi, heartbeat, thứ tự offset và `confirmed_flush_lsn` | `UPDATE` với `REPLICA IDENTITY DEFAULT` cho `before = null`; chỉ bật `heartbeat.interval.ms` thì WAL bị giữ tăng tới 99 MB, thêm `heartbeat.action.query` thì giảm còn 17 MB; offset trên Kafka đổi trước slot khoảng 0,5 giây |
| `06-signal.sh` | Giá trị mặc định qua `/connector-plugins/.../config/validate`; signal và incremental snapshot | `incremental.snapshot.chunk.size=1024`, `plugin.name=decoderbufs`; thiếu quyền `INSERT` trên bảng signal thì incremental snapshot bị bỏ qua mà task vẫn `RUNNING` |
| `07-lock-tables.sh` | Thứ tự câu lệnh snapshot qua general log của MySQL, bảng 300.000 dòng | `FLUSH TABLES WITH READ LOCK`, `START TRANSACTION WITH CONSISTENT SNAPSHOT`, `SHOW MASTER STATUS`, `UNLOCK TABLES` sau 0,4 giây, đọc dữ liệu 25 giây, `ROLLBACK` |
| `09-phu.sh` | `decimal.handling.mode=string`, bảng không có khóa chính, `message.key.columns`, JDBC sink khi tắt schema, số luồng binlog | Key `null` khi bảng không có khóa chính; JDBC sink `FAILED` với `record_key cannot have null schema`; mỗi connector mở một luồng `Binlog Dump GTID` |
| `kiem-lab-readme.sh` | Các bước trong README của lab, trên cụm trống | |

## Lưu ý khi chạy

- **RAM:** `03-worker-failover.sh` chạy hai worker nên hạ heap của mỗi worker xuống 512 MB. Với VM Docker 3,4 GiB, bảng quá lớn ở `07-lock-tables.sh` (1,5 triệu dòng kèm schema JSON) từng làm container Kafka dừng, nên script dùng 300.000 dòng và tắt schema.
- **RDS và Aurora:** Docker không giả lập được cảnh khóa toàn cục bị chặn nhưng khóa từng bảng vẫn được: trên MySQL 8.0.41, quyền `FLUSH_TABLES` vẫn cho chạy `FLUSH TABLES WITH READ LOCK`.

## Môi trường đã chạy

Docker Desktop 20.10.17 (Compose v2.6.1) trên Windows 10, VM Docker 5 CPU và 3,4 GiB RAM. Image giống lab chính: `apache/kafka:4.3.1`, `quay.io/debezium/connect:3.6.3.Final`, `mysql:8.0.41`, `postgres:16.14`. Chạy ngày 2026-11-22.
