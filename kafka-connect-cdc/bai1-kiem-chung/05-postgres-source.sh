#!/usr/bin/env bash
# Kiem chung 5: PostgreSQL lam nguon CDC. Publication, REPLICA IDENTITY, WAL ton dong khi bang
# duoc theo doi it ghi, heartbeat, va thu tu cap nhat offset tren Kafka voi confirmed_flush_lsn.
# offset.flush.interval.ms = 30 giay de thay ro slot chi tien len o chu ky flush.
source "$(dirname "$0")/chung.sh"
export COMPOSE_PROFILES=pgsrc
export OFFSET_FLUSH_MS=30000
PA="perl $K/phan-tich.pl"

slot() { psrc -c "SELECT now()::time(0) AS luc, slot_name, pg_current_wal_lsn() AS wal_hien_tai, confirmed_flush_lsn, restart_lsn,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS wal_dang_giu FROM pg_replication_slots ORDER BY slot_name" | grep -v '^(' ; }
ghi_audit() { psrc -qc "INSERT INTO audit_log (msg) SELECT repeat('x', 200) FROM generate_series(1, 50000)"; }

step "0. Cum moi voi pgsrc (wal_level=logical)"
fresh
psrc -At -c "SHOW wal_level"
psrc -q <<'SQL'
CREATE TABLE products (id bigint PRIMARY KEY, sku text NOT NULL, price numeric(12,2) NOT NULL, stock int NOT NULL);
INSERT INTO products VALUES (1, 'KB-01', 1290000, 50), (2, 'MS-02', 450000, 120), (3, 'HP-03', 890000, 30);
CREATE TABLE audit_log (id bigserial PRIMARY KEY, msg text);
CREATE TABLE heartbeat (id int PRIMARY KEY, ts timestamptz);
SQL

step "1. Connector mac dinh: publication tu tao the nao, co topic schema history khong"
put_config pg-source $C/pg-source.json; wait_running pg-source; sleep 5
psrc -c "SELECT pubname, puballtables FROM pg_publication"
psrc -c "SELECT pubname, tablename FROM pg_publication_tables ORDER BY 1, 2"
kt --list | grep -vE "^(connect-|__consumer)"

step "2. REPLICA IDENTITY DEFAULT: UPDATE gia, UPDATE doi khoa chinh, DELETE"
psrc -At -c "SELECT relname, relreplident FROM pg_class WHERE relname = 'products'"
psrc -q -c "UPDATE products SET price = 1190000 WHERE id = 1" -c "UPDATE products SET id = 20 WHERE id = 2" -c "DELETE FROM products WHERE id = 3"
sleep 6
topic pg.public.products > $OUT/05-default.txt
$PA msgs $OUT/05-default.txt 4

step "3. REPLICA IDENTITY FULL: UPDATE va DELETE"
psrc -q -c "ALTER TABLE products REPLICA IDENTITY FULL" -c "UPDATE products SET stock = 49 WHERE id = 1" -c "DELETE FROM products WHERE id = 20"
sleep 6
topic pg.public.products > $OUT/05-full.txt
$PA msgs $OUT/05-full.txt $(( $(grep -c . $OUT/05-default.txt) + 1 ))
psrc -q -c "ALTER TABLE products REPLICA IDENTITY DEFAULT"

step "4. publication.autocreate.mode=filtered; xoa connector mac dinh va slot cua no"
connect DELETE /connectors/pg-source; sleep 5
psrc -At -c "SELECT pg_drop_replication_slot('dbz_slot')"
put_config pg-filtered $C/pg-filtered.json; wait_running pg-filtered; sleep 5
psrc -c "SELECT pubname, puballtables FROM pg_publication"
psrc -c "SELECT pubname, tablename FROM pg_publication_tables ORDER BY 1, 2"

step "5. Thu tu: offset tren Kafka va confirmed_flush_lsn (poll moi 0,2 giay trong 45 giay)"
sleep 35   # qua it nhat mot chu ky flush de offset va slot on dinh
docker compose exec -T pgsrc bash -s < $K/poll-slot.sh > $OUT/05-poll-slot.txt &
P1=$!
docker compose exec -T connect bash -s < $K/poll-offset.sh > $OUT/05-poll-offset.txt &
P2=$!
sleep 3
psrc -At -c "UPDATE products SET stock = 48 WHERE id = 1 RETURNING to_char(clock_timestamp(), 'HH24:MI:SS.MS') AS luc_update, pg_current_wal_lsn()"
wait $P1 $P2
echo "-- slot: lan doi gia tri confirmed_flush_lsn:"
awk '$2 != prev {print "   " $0; prev = $2}' $OUT/05-poll-slot.txt
echo "-- offset tren Kafka (GET /offsets): lan doi gia tri lsn:"
awk '{m = match($0, /"lsn":[0-9]+/); v = substr($0, RSTART, RLENGTH); if (v != prev) {print "   " $1 " " v; prev = v}}' $OUT/05-poll-offset.txt

step "6. WAL ton dong: bang products im lang, audit_log (ngoai publication) ghi lien tuc, chua bat heartbeat"
slot
for i in 1 2 3; do ghi_audit; sleep 25; slot | tail -n +3; done

step "7. Bat heartbeat.interval.ms=5000, khong co heartbeat.action.query"
put_config pg-filtered $C/pg-filtered-hb.json; sleep 5; wait_running pg-filtered
for i in 1 2 3; do ghi_audit; sleep 25; slot | tail -n +3; done
echo "-- message heartbeat: $(offsets __debezium-heartbeat.pgf 2>/dev/null)"

step "8. Them heartbeat.action.query ghi vao bang heartbeat (dua vao publication)"
put_config pg-filtered $C/pg-filtered-hb-query.json; sleep 5; wait_running pg-filtered
psrc -c "SELECT pubname, tablename FROM pg_publication_tables ORDER BY 1, 2"
for i in 1 2 3; do ghi_audit; sleep 25; slot | tail -n +3; done
psrc -c "SELECT * FROM heartbeat"

step "9. Connector dung han: WAL bi giu lai"
connect PUT /connectors/pg-filtered/stop; sleep 5
slot | tail -n +3
for i in 1 2; do ghi_audit; done; sleep 3
slot | tail -n +3
