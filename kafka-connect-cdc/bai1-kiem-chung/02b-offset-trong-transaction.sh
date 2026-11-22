#!/usr/bin/env bash
# Kiem chung 2b: offset {gtids 1-13, event, row} sau message DELETE (GTID 14) co lam gui lai DELETE khong.
# Tat heartbeat de offset dung yen o message cuoi cung, cho flush xong roi SIGKILL worker.
source "$(dirname "$0")/chung.sh"

step "0. Cum moi, mysql-source khong heartbeat"
fresh
sql < sql/mysql-init.sql
put_config mysql-source $C/mysql-source-no-hb.json; wait_running mysql-source; sleep 5
sql < sql/mysql-changes.sql
sleep 12
echo "-- offset da flush:"; connect GET /connectors/mysql-source/offsets
echo "-- message tren topic: $(offsets shop.shop.products)"; ops shop.shop.products

step "1. SIGKILL worker, khoi dong lai"
docker compose kill -s SIGKILL connect > /dev/null 2>&1
docker compose up -d --wait connect 2>&1 | tail -1
wait_running mysql-source 120; sleep 10
clog | grep -E "Connected to binlog at|Skipping|skip" | tail -4 | cut -c1-300
echo "-- message tren topic: $(offsets shop.shop.products)"; ops shop.shop.products
echo "-- offset sau restart:"; connect GET /connectors/mysql-source/offsets
