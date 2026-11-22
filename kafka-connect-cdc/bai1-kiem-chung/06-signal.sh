#!/usr/bin/env bash
# Kiem chung 6: kenh signal va incremental snapshot; gia tri mac dinh cua cac tham so bai nhac toi.
source "$(dirname "$0")/chung.sh"

# defaults PLUGIN TEN...: goi /config/validate roi in gia tri mac dinh cua tung tham so
defaults() {
  local plugin=$1; shift
  local body
  case $plugin in
    MySqlConnector) body='{"connector.class":"io.debezium.connector.mysql.MySqlConnector","topic.prefix":"x","database.hostname":"mysql","database.user":"debezium","database.password":"dbz","database.server.id":"1"}';;
    PostgresConnector) body='{"connector.class":"io.debezium.connector.postgresql.PostgresConnector","topic.prefix":"x","database.hostname":"postgres","database.user":"replica","database.password":"replica","database.dbname":"replica"}';;
    JdbcSinkConnector) body='{"connector.class":"io.debezium.connector.jdbc.JdbcSinkConnector","topics":"x","connection.url":"jdbc:postgresql://postgres:5432/replica"}';;
  esac
  echo "$body" | docker compose exec -T connect curl -s -X PUT -H "Content-Type: application/json" --data @- \
    "http://localhost:8083/connector-plugins/$plugin/config/validate" > $OUT/06-validate-$plugin.json
  perl -MJSON::PP -0777 -e 'my $d = decode_json(<STDIN>); my %w = map {$_=>1} @ARGV;
    for my $c (@{$d->{configs}}) { my $n = $c->{definition}{name}; next unless $w{$n};
      printf "  %-45s mac dinh = %s\n", $n, defined $c->{definition}{default_value} ? $c->{definition}{default_value} : "(khong co)"; }' "$@" < $OUT/06-validate-$plugin.json
}

step "0. Cum moi"
fresh

step "1. Gia tri mac dinh qua /connector-plugins/.../config/validate"
echo "MySQL source:"
defaults MySqlConnector snapshot.mode signal.data.collection signal.enabled.channels signal.kafka.topic incremental.snapshot.chunk.size \
  heartbeat.interval.ms decimal.handling.mode tombstones.on.delete schema.history.internal.store.only.captured.tables.ddl message.key.columns
echo "PostgreSQL source:"
defaults PostgresConnector plugin.name slot.name publication.name publication.autocreate.mode heartbeat.action.query flush.lsn.source slot.drop.on.stop
echo "JDBC sink:"
defaults JdbcSinkConnector insert.mode primary.key.mode delete.enabled schema.evolution

step "2. Bang orders 2.500 dong, bang signal; connector chi theo doi products"
sql < sql/mysql-init.sql
sql <<'SQL'
USE shop;
CREATE TABLE orders (id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT NOT NULL, qty INT NOT NULL);
SET SESSION cte_max_recursion_depth = 5000;
INSERT INTO orders (product_id, qty) WITH RECURSIVE s(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM s WHERE n < 2500) SELECT 1 + n % 3, 1 + n % 5 FROM s;
CREATE TABLE debezium_signal (id VARCHAR(42) PRIMARY KEY, type VARCHAR(32) NOT NULL, data VARCHAR(2048) NULL);
SQL
put_config mysql-source $C/mysql-signal-1.json; wait_running mysql-source; sleep 5
ops shop.shop.products

step "3. Them orders vao table.include.list: snapshot ban dau khong chay lai"
put_config mysql-source $C/mysql-signal-2.json; sleep 5; wait_running mysql-source; sleep 5
echo "-- topic shop.shop.orders: $(offsets shop.shop.orders 2>&1 | tail -1)"

step "4. Gui signal khi user connector CHUA co quyen INSERT tren bang signal"
sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-1', 'execute-snapshot', '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\"}');"
sleep 12
status mysql-source | cut -c1-700
clog | grep -iE "denied|signal" | tail -5 | sed -E 's/^[0-9-]+ ([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-260

step "5. Cap quyen INSERT, restart task, gui lai signal neu can"
sql -e "GRANT INSERT ON shop.debezium_signal TO 'debezium'@'%';"
connect POST "/connectors/mysql-source/restart?includeTasks=true&onlyFailed=true"
wait_running mysql-source; sleep 15
echo "-- orders tren topic sau restart: $(topic shop.shop.orders 15000 | grep -c .)"
if [ "$(topic shop.shop.orders | grep -c .)" -lt 2500 ]; then
  sql -e "INSERT INTO shop.debezium_signal (id, type, data) VALUES ('snap-2', 'execute-snapshot', '{\"data-collections\": [\"shop.orders\"], \"type\": \"incremental\"}');"
  sleep 20
fi
topic shop.shop.orders 15000 > $OUT/06-orders.txt
echo "-- shop.shop.orders: $(grep -c . $OUT/06-orders.txt) message"
grep -o '"snapshot":"[a-z]*"\|"op":"[a-z]"' $OUT/06-orders.txt | paste - - | sort | uniq -c
echo "-- cac dong trong bang signal:"
sql -e "SELECT type, COUNT(*) FROM shop.debezium_signal GROUP BY type"
clog | grep -iE "incremental snapshot|chunk|window" | tail -8 | sed -E 's/^[0-9-]+ ([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-220
