# Tenant guard cho MySQL: hook ở GORM và Hibernate

Code cho mục "Tenant guard cho MySQL" của bài "Row Level Security trong PostgreSQL: lưới chặn lộ dữ liệu khi query quên điều kiện tenant". Bộ này chỉ chạy demo hành vi, không đo thời gian.

## Kịch bản

`schema.sql`: hai bảng có `tenant_id` (`customers`, `orders`), một bảng dùng chung (`countries`), dữ liệu của tenant 1 và tenant 2.

- **Go (`go/`):** `tenantguard/guard.go` parse câu SQL bằng parser của TiDB, cài vào driver MySQL qua `sqlhooks`, GORM dùng driver đã bọc. `main.go` chạy 13 bước: `Find` thiếu tenant, `First` theo id, `Update` theo id, `OR 1 = 1`, LEFT JOIN đặt điều kiện tenant nhầm bảng (kèm bước [6b] chạy cùng câu qua connection không có guard để thấy dữ liệu lộ), subquery tính tổng, câu trong transaction, `Preload`, bảng dùng chung, `WithoutTenantCheck`, chế độ `LogOnly`. Output ở [go-output.txt](go-output.txt).
- **Java (`java/`):** `TenantGuard.java` parse bằng JSqlParser, `TenantGuardInspector.java` cài vào Hibernate qua `StatementInspector`. `Main.java` chạy 15 bước trên ba SessionFactory: entity thường (HQL, `find`, HQL update, native query, LEFT JOIN, lazy load, `withoutTenantCheck`), entity có `@TenantId` (HQL, `find`, lazy load, native query; bước [12] và [13] in câu SQL Hibernate sinh ra), và chế độ `LOG_ONLY`. Output ở [java-output.txt](java-output.txt).

## Chạy

Cần Docker. Source đưa vào container qua `tar` theo stdin, không bind mount thư mục nào, nên Docker Desktop không cần quyền chia sẻ ổ đĩa. Trên Git Bash cho Windows cần `MSYS_NO_PATHCONV=1`.

```bash
export MSYS_NO_PATHCONV=1
docker network create tg-net
docker run -d --name tg-mysql --network tg-net --tmpfs /var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=pw -e MYSQL_DATABASE=crm mysql:8.0.41
# đợi MySQL sẵn sàng rồi nạp schema
docker exec -i tg-mysql mysql -uroot -ppw crm < schema.sql

# Go: parser TiDB bản mới cần Go 1.27.3, GOTOOLCHAIN=auto tự tải toolchain
docker run -d --name tg-go --network tg-net -v tg-gomod:/go/pkg/mod -w /src golang:1.24 sleep infinity
docker exec tg-go mkdir -p /src && tar -c -C go . | docker exec -i tg-go tar -x -C /src
docker exec -e GOTOOLCHAIN=auto tg-go sh -c 'go build -o /tmp/demo . && /tmp/demo'

# Java
docker run -d --name tg-mvn --network tg-net -v tg-m2:/root/.m2 -w /src maven:3.9-eclipse-temurin-17 sleep infinity
docker exec tg-mvn mkdir -p /src && tar -c -C java . | docker exec -i tg-mvn tar -x -C /src
docker exec tg-mvn mvn -q -B compile exec:java

docker rm -f tg-mysql tg-go tg-mvn && docker network rm tg-net
```

Hai volume `tg-gomod` và `tg-m2` giữ cache thư viện cho lần chạy sau; xóa bằng `docker volume rm tg-gomod tg-m2`.

## Phiên bản

MySQL 8.0.41. Go 1.27.3 (tự tải từ image `golang:1.24`), GORM 1.31.2, `gorm.io/driver/mysql` 1.6.0, `go-sql-driver/mysql` 1.10.1, `sqlhooks/v2` 2.1.0, parser TiDB `v0.0.0-20261116081432-aa410927742d`. JDK 17.0.20, Hibernate 6.6.59.Final, JSqlParser 5.4, MySQL Connector/J 9.4.0. Docker Desktop 20.10.17 trên Windows 10, AMD Ryzen 5 3600, 16 GB RAM. Chạy ngày 2026-11-16.
