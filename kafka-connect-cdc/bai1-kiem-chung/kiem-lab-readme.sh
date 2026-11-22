#!/usr/bin/env bash
# Chay lai cac buoc README cua lab tren cum trong (chi dung docker-compose.yml goc).
set -u
cd "$(dirname "$0")/.."
source lib.sh
docker compose down --remove-orphans > /dev/null 2>&1
docker compose up -d --wait 2>&1 | tail -1
docker compose exec -T mysql mysql -uroot -proot < sql/mysql-init.sql 2>&1 | grep -v "Using a password"
for c in mysql-source outbox-source postgres-sink; do
  docker compose exec -T connect curl -s -o /dev/null -w "$c %{http_code}\n" \
    -X PUT -H "Content-Type: application/json" --data @- \
    http://localhost:8083/connectors/$c/config < connectors/$c.json
done
sleep 15
for c in mysql-source outbox-source postgres-sink; do status $c; done
docker compose exec -T mysql mysql -uroot -proot < sql/mysql-changes.sql 2>&1 | grep -v "Using a password"
sleep 10
ops shop.shop.products
docker compose exec -T postgres psql -U replica -d replica -c "SELECT id, sku, price, stock FROM products ORDER BY id;"
