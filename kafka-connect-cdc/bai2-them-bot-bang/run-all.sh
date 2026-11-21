#!/usr/bin/env bash
# Chay toan bo kich ban bai 2 tren mot cum trong. Chay tu thu muc kafka-connect-cdc:
#   bash bai2-them-bot-bang/run-all.sh
# Tung buoc giong het README.md, in ra ket qua de doi chieu.
set -u
cd "$(dirname "$0")/.."
source lib.sh
C=bai2-them-bot-bang/connectors

step() { echo; echo "=================== $* ==================="; }

step "0. Dung cum moi va tao du lieu"
docker compose down > /dev/null 2>&1
docker compose up -d --wait 2>&1 | tail -1
sql < bai2-them-bot-bang/sql/init.sql

step "A1. Connector chi capture products, store.only.captured.tables.ddl=true"
put_config shop-source $C/1-products-only.json
sleep 15
status shop-source
echo "-- DDL trong schema history:"
topic schema-history.shop | grep -o '"ddl" : "[^"]\{0,50\}'

step "A2. Them orders vao table.include.list, roi ghi vao orders"
put_config shop-source $C/2-add-orders.json
sleep 10
sql -e "INSERT INTO shop.orders (product_id, qty, status) VALUES (3, 2, 'NEW');"
sleep 10
status shop-source
docker compose logs --no-log-prefix connect 2>/dev/null | grep -o "Encountered change event for table [a-z.]* whose schema isn't known to this connector" | head -1

step "B1. Stop connector, xem offset, xoa topic schema history"
connect PUT /connectors/shop-source/stop
sleep 3
status shop-source
connect GET /connectors/shop-source/offsets
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --delete --topic schema-history.shop
sleep 3

step "B2. snapshot.mode=recovery, store.only.captured.tables.ddl=false, them orders, resume"
put_config shop-source $C/3-recovery.json
connect PUT /connectors/shop-source/resume
sleep 15
status shop-source
echo "-- DDL trong schema history sau recovery:"
topic schema-history.shop | grep -o '"ddl" : "[^"]\{0,50\}'
echo "-- topic orders:"
topic shop.shop.orders | grep -o '"after":{"id":[0-9]*\|"op":"[a-z]"' | paste - -
echo "-- topic products:"
ops shop.shop.products

step "B3. Incremental snapshot cho orders qua bang signal"
sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-orders-1', 'execute-snapshot', '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\"}');"
sleep 12
topic shop.shop.orders | grep -o '"after":{"id":[0-9]*\|"snapshot":"[a-z]*"\|"op":"[a-z]"' | paste - - -
echo "-- bang signal:"
sql -e "SELECT id, type FROM shop.debezium_signal"

step "B4. Tra snapshot.mode ve initial"
put_config shop-source $C/4-after-recovery.json
sleep 12
status shop-source
ops shop.shop.products

step "C. Cach sua nhanh: tao connector moi voi when_needed"
put_config quickfix-v1 $C/5-quickfix-v1.json
sleep 12
put_config quickfix-v1 $C/6-quickfix-v1-add-orders.json
sleep 8
sql -e "INSERT INTO shop.orders (product_id, qty, status) VALUES (1, 5, 'NEW');"
sleep 8
status quickfix-v1 | cut -c1-220
connect DELETE /connectors/quickfix-v1
put_config quickfix-v2 $C/7-quickfix-v2.json
sleep 15
status quickfix-v2
echo "-- products:"; ops quickfix.shop.products
echo "-- orders:"; ops quickfix.shop.orders

step "D. Bot bang orders, ghi trong luc bo, them lai"
put_config shop-source $C/8-remove-orders.json
sleep 10
sql -e "INSERT INTO shop.orders (product_id, qty, status) VALUES (2, 1, 'GAP'); UPDATE shop.products SET stock = stock - 1 WHERE id = 2;"
sleep 8   # cho offset (flush moi 5 giay) di qua event GAP
put_config shop-source $C/4-after-recovery.json
sleep 10
sql -e "INSERT INTO shop.orders (product_id, qty, status) VALUES (3, 1, 'AFTER');"
sleep 8
sql -e "SELECT id, status FROM shop.orders"
topic shop.shop.orders | grep -o '"after":{"id":[0-9]*,[^}]*"status":"[A-Z]*"\|"op":"[a-z]"' | paste - -
echo "-- lap cho trong bang incremental snapshot co dieu kien:"
sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-orders-gap', 'execute-snapshot', '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\", \"additional-conditions\": [{\"data-collection\": \"shop.orders\", \"filter\": \"id >= 5\"}]}');"
sleep 10
topic shop.shop.orders | grep -o '"after":{"id":[0-9]*,[^}]*"status":"[A-Z]*"\|"op":"[a-z]"' | paste - - | tail -2

step "E1. Binlog bi purge trong luc connector dung, snapshot.mode=initial"
put_config purge-source $C/9-purge-initial.json
sleep 12
ops purge.shop.products
connect PUT /connectors/purge-source/stop
sleep 3
sql -e "UPDATE shop.products SET stock = stock + 10 WHERE id = 1; FLUSH BINARY LOGS;"
sleep 3
cur=$(sql -N -e "SHOW MASTER STATUS" | awk '{print $1}')
sql -e "PURGE BINARY LOGS TO '$cur'; SHOW BINARY LOGS;"
connect PUT /connectors/purge-source/resume
sleep 12
status purge-source | cut -c1-220
docker compose logs --no-log-prefix connect 2>/dev/null | grep -o "but this is no longer available on the server. Reconfigure the connector to use a snapshot mode when needed." | head -1

step "E2. Doi sang when_needed"
put_config purge-source $C/10-purge-when-needed.json
sleep 15
status purge-source
ops purge.shop.products

step "E3. Doi sang always, roi restart"
put_config purge-source $C/11-purge-always.json
sleep 12
ops purge.shop.products
connect POST "/connectors/purge-source/restart?includeTasks=true"
sleep 15
ops purge.shop.products

step "F. no_data, initial_only, va inconsistent.schema.handling.mode=warn"
put_config nodata-source $C/12-no-data.json
put_config initonly-source $C/13-initial-only.json
put_config warn-source $C/14-warn-products-only.json
sleep 15
put_config warn-source $C/15-warn-add-orders.json
sleep 10
sql -e "UPDATE shop.products SET stock = stock - 2 WHERE id = 3; INSERT INTO shop.orders (product_id, qty, status) VALUES (3, 9, 'WARN');"
sleep 10
for c in nodata-source initonly-source warn-source; do status $c | cut -c1-200; done
echo "-- nodata:"; ops nodata.shop.products
echo "-- initonly:"; ops initonly.shop.products
echo "-- warn products:"; ops warn.shop.products
echo "-- warn orders:"; offsets warn.shop.orders 2>&1 | tail -1
docker compose logs --no-log-prefix connect 2>/dev/null | grep "WARN   MySQL||binlog  Encountered change event" | grep -o "for table [a-z.]* whose schema isn't known to this connector[^\[]*" | head -1

step "G. Reset offset bang REST API"
connect PUT /connectors/initonly-source/stop
sleep 3
connect DELETE /connectors/initonly-source/offsets
connect GET /connectors/initonly-source/offsets
