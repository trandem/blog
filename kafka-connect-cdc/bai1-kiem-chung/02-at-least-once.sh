#!/usr/bin/env bash
# Kiem chung 2: worker chet dot ngot (SIGKILL) giua hai chu ky flush thi source gui trung, sink ghi lai.
# offset.flush.interval.ms = 60 giay de khoang chua flush du rong.
source "$(dirname "$0")/chung.sh"
export OFFSET_FLUSH_MS=60000

step "0. Cum moi voi offset.flush.interval.ms=60000"
fresh
sql < sql/mysql-init.sql
pdst -c "ALTER SYSTEM SET log_statement = 'all'" -c "SELECT pg_reload_conf()" > /dev/null
put_config mysql-source connectors/mysql-source.json
put_config postgres-sink connectors/postgres-sink.json
wait_running mysql-source; wait_running postgres-sink
echo "Cho 70 giay de worker flush offset lan dau"; sleep 70
echo "-- offset source:"; connect GET /connectors/mysql-source/offsets
groups --describe --group connect-postgres-sink | grep shop

step "1. INSERT ZZ-99, 3 giay sau SIGKILL worker (chua toi chu ky flush)"
sql -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('ZZ-99','Hang thu',10000.00,1);"
sleep 3
echo "-- truoc khi kill: ZZ-99 tren topic = $(topic shop.shop.products | grep -c ZZ-99)"
groups --describe --group connect-postgres-sink | grep shop
docker compose kill -s SIGKILL connect > /dev/null 2>&1
echo "-- da kill luc $(date +%T)"

step "2. Khoi dong lai worker"
docker compose up -d --wait connect 2>&1 | tail -1
wait_running mysql-source 120; wait_running postgres-sink 120
sleep 12
echo "-- offset source sau restart:"; connect GET /connectors/mysql-source/offsets
echo "-- ZZ-99 tren topic = $(topic shop.shop.products | grep -c ZZ-99) message"
topic shop.shop.products | grep ZZ-99 | grep -o '"op":"[a-z]"\|"gtid":"[^"]*"' | paste - -
echo "-- cau upsert ZZ-99 trong log PostgreSQL = $(clog postgres | grep -A1 -E 'execute [^:]*: INSERT' | grep -c "ZZ-99")"
clog postgres | grep -A1 -E 'execute [^:]*: INSERT' | grep -B1 "ZZ-99" | grep -oE "^[0-9-]+ [0-9:.]+|DETAIL:.{0,80}" | paste - -
pdst -c "SELECT id, sku, price FROM products WHERE sku = 'ZZ-99'"
groups --describe --group connect-postgres-sink | grep shop
