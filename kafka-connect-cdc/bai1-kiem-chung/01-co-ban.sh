#!/usr/bin/env bash
# Kiem chung 1: chay lai luong MySQL -> Kafka -> PostgreSQL cua bai va do cac con so bai trich.
source "$(dirname "$0")/chung.sh"
PA="perl $K/phan-tich.pl"

step "0. Dung cum moi, phien ban"
fresh
clog | grep -m2 -oE "Kafka version: [0-9.]+|Debezium version: [0-9.A-Za-z]+"
docker compose exec -T connect sh -c 'ls /kafka/connect/debezium-connector-mysql | grep -m1 -E "^debezium-core"'

step "1. Topic noi bo cua worker truoc khi co connector"
kt --describe --topic 'connect-.*' | grep PartitionCount
echo "-- cau hinh worker:"
clog | awk '/DistributedConfig values/{f=1} f' | grep -m12 -E "^\s(key.converter|value.converter|offset.flush.interval.ms|scheduled.rebalance.max.delay.ms|session.timeout.ms|group.id|rest.port|connect.protocol) ="

step "2. Tao du lieu, bat log_statement=all o PostgreSQL dich, dang ky 3 connector"
sql < sql/mysql-init.sql
pdst -c "ALTER SYSTEM SET log_statement = 'all'" -c "SELECT pg_reload_conf()" > /dev/null
for c in mysql-source outbox-source postgres-sink; do put_config $c connectors/$c.json; done
for c in mysql-source outbox-source postgres-sink; do wait_running $c; done
sleep 8
status mysql-source

step "3. Snapshot cua mysql-source: thu tu buoc, khoa, thoi gian chan ghi"
clog | grep "MySQL|shop|snapshot" | grep -iE "step|lock|binlog position|prevented|transaction|Snapshot ended|Exporting data" | sed -E 's/^[0-9-]+ ([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-170

step "4. Producer cua source task mysql-source"
clog | awk '/ProducerConfig values/{inb=1; blk=""; next} inb && /^\t/{blk=blk $0 "\n"; next} inb{inb=0; if (blk ~ /client.id = connector-producer-mysql-source-0/) print blk}' \
  | grep -E "^\s(acks|enable.idempotence|max.in.flight.requests.per.connection|retries|delivery.timeout.ms|max.block.ms|max.request.size|linger.ms) =" | sort -u

step "5. Thay doi du lieu, doc topic shop.shop.products"
sql < sql/mysql-changes.sql
sleep 8
topic shop.shop.products > $OUT/01-products.txt
$PA msgs $OUT/01-products.txt
echo "-- giai ma price:"
for b in B7BiQA== BxfLwA==; do $PA decimal $b 2; done
sql -e "SELECT @@gtid_executed\G" | grep -v '^\*'

step "6. Topic outbox.event.product"
docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic outbox.event.product --from-beginning --timeout-ms 5000 \
  --formatter-property print.key=true --formatter-property print.headers=true 2>/dev/null > $OUT/01-outbox.txt
cat $OUT/01-outbox.txt
awk -F'\t' '{print "value =", length($3), "byte; key =", length($2), "byte"}' $OUT/01-outbox.txt

step "7. Danh sach topic, schema history, heartbeat"
kt --list
kt --describe --topic 'schema-history.shop|shop.shop.products|shop|__debezium-heartbeat.shop|connect-.*' | grep PartitionCount
echo "-- so ban ghi schema-history.shop: $(topic schema-history.shop | grep -c .)"
topic schema-history.shop | grep -o '"ddl" : "[^"]\{0,45\}'
echo "-- heartbeat: $(offsets __debezium-heartbeat.shop)"

step "8. connect-offsets sau chu ky flush"
sleep 7
topic connect-offsets
connect GET /connectors/mysql-source/offsets

step "9. PostgreSQL dich"
pdst -c "SELECT id, sku, price, stock, updated_at FROM products ORDER BY id"
pdst -c "\d products"
echo "-- SQL sink da chay:"
clog postgres | grep -oE "statement: (INSERT|DELETE|UPDATE|CREATE|ALTER).{0,170}" | sort | uniq -c
groups --describe --group connect-postgres-sink

step "10. Restart worker binh thuong roi them mot dong"
docker compose restart connect > /dev/null 2>&1 && docker compose up -d --wait connect 2>&1 | tail -1
wait_running mysql-source 120
sql -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('CB-05','Cap USB-C',99000.00,500);"
sleep 10
clog | grep -E "previous offset indicating a completed snapshot|Connected to binlog at" | tail -4 | cut -c1-260
offsets shop.shop.products; ops shop.shop.products

step "11. mysql-source voi tasks.max=3"
put_config mysql-source $C/mysql-source-3-tasks.json; sleep 12
status mysql-source | grep -o '"tasks":\[.*\]'
put_config mysql-source connectors/mysql-source.json; sleep 8

step "12. postgres-sink voi tasks.max=3 tren topic 1 partition"
put_config postgres-sink $C/postgres-sink-3-tasks.json; sleep 15
status postgres-sink | grep -o '"tasks":\[.*\]'
groups --describe --group connect-postgres-sink --members
put_config postgres-sink connectors/postgres-sink.json; sleep 10

step "13. DELETE connector: offset con lai khong, tao lai cung ten"
connect DELETE /connectors/mysql-source; sleep 3
connect GET /connectors
echo "-- connect-offsets (key mysql-source):"; topic connect-offsets | grep mysql-source
put_config mysql-source connectors/mysql-source.json; wait_running mysql-source; sleep 6
clog | grep "previous offset indicating a completed snapshot" | tail -1 | cut -c1-200
offsets shop.shop.products; ops shop.shop.products

step "14. Sink FAILED, sua nguyen nhan, restart onlyFailed"
pdst -c "ALTER TABLE products ADD CONSTRAINT price_limit CHECK (price < 5000000)"
sql -e "UPDATE shop.products SET price = 6000000.00 WHERE id = 2; UPDATE shop.products SET stock = 99 WHERE id = 1;"
sleep 12
status postgres-sink | cut -c1-400
groups --describe --group connect-postgres-sink
pdst -c "SELECT id, price, stock FROM products ORDER BY id"
pdst -c "ALTER TABLE products DROP CONSTRAINT price_limit"
echo "-- restart:"; docker compose exec -T connect curl -s -w " http=%{http_code}" -X POST "http://localhost:8083/connectors/postgres-sink/restart?includeTasks=true&onlyFailed=true"; echo
sleep 12
status postgres-sink
groups --describe --group connect-postgres-sink
pdst -c "SELECT id, price, stock FROM products ORDER BY id"
