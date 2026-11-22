#!/usr/bin/env bash
# Kiem chung 7: snapshot co quyen RELOAD (FLUSH TABLES WITH READ LOCK) so voi khong co RELOAD
# (Debezium chuyen sang LOCK TABLES, giong RDS va Aurora). Bat general log de thay dung thu tu cau lenh,
# do thoi gian chan ghi va thu mot cau INSERT trong luc snapshot.
source "$(dirname "$0")/chung.sh"

thu_insert() {  # INSERT trong luc snapshot dang chay, in thoi gian cho
  local t0=$(date +%s%3N)
  sql -e "INSERT INTO shop.big_items (note) VALUES ('ghi trong luc snapshot');"
  echo "   INSERT tu phien khac trong luc snapshot mat $(( $(date +%s%3N) - t0 )) ms"
}

step "0. Cum moi, bang big_items 300.000 dong, user khong co RELOAD"
fresh
sql < sql/mysql-init.sql
sql <<'SQL'
USE shop;
CREATE TABLE big_items (id BIGINT AUTO_INCREMENT PRIMARY KEY, note VARCHAR(100) NOT NULL);
SET SESSION cte_max_recursion_depth = 2000000;
INSERT INTO big_items (note) WITH RECURSIVE s(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM s WHERE n < 300000) SELECT CONCAT('item-', n) FROM s;
CREATE USER 'debezium_nolock'@'%' IDENTIFIED BY 'dbz';
GRANT SELECT, SHOW DATABASES, LOCK TABLES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium_nolock'@'%';
-- FLUSH_TABLES cho phep FLUSH TABLES <bang> WITH READ LOCK nhung khong cho FLUSH TABLES WITH READ LOCK toan cuc
-- (lenh toan cuc can RELOAD): giong RDS va Aurora, noi khoa toan cuc bi chan.
GRANT FLUSH_TABLES ON *.* TO 'debezium_nolock'@'%';
SET GLOBAL general_log_file = '/tmp/general.log';
SET GLOBAL general_log = ON;
SQL
sql -N -e "SELECT COUNT(*) FROM shop.big_items"

for p in a b; do
  u=debezium; [ $p = b ] && u=debezium_nolock
  step "lock-$p: user $u"
  S=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  docker compose exec -T mysql sh -c ': > /tmp/general.log'
  put_config lock-$p $C/lock-$p.json
  sleep 3; thu_insert
  for i in $(seq 1 150); do docker compose logs --since "$S" connect 2>/dev/null | grep -q "Snapshot ended with" && break; sleep 2; done
  docker compose logs --no-log-prefix --since "$S" connect 2>/dev/null \
    | grep "MySQL||snapshot" | grep -iE "lock|prevented|Snapshot step 7|Finished exporting|Snapshot ended with" \
    | sed -E 's/^[0-9-]+T([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-200
  echo "-- cac cau lenh connector chay (general log, theo thu tu):"
  docker compose exec -T mysql sh -c "grep -E 'FLUSH|LOCK|UNLOCK|START TRANSACTION|ISOLATION|SHOW MASTER STATUS|SHOW BINARY LOG STATUS|SELECT \* FROM .shop.\..big_items|COMMIT|ROLLBACK' /tmp/general.log" \
    | grep -v "ghi trong luc snapshot" | sed -E 's/^[0-9T:.-]+Z +//' | cut -c1-150 | head -25
  status lock-$p | grep -o '"tasks":\[{"id":0,"state":"[A-Z]*"'
done
