#!/usr/bin/env bash
# Kiem chung 4b: vi tri cua tung transaction (theo GTID) trong binlog cua primary va replica.
source "$(dirname "$0")/chung.sh"
export COMPOSE_PROFILES=gtid
gtids() {  # HOST: in GTID, file, pos bat dau cua moi transaction trong binlog
  local run=sql; [ "$1" = mysql2 ] && run=sql2
  for f in $($run -N -e "SHOW BINARY LOGS" | awk '{print $1}'); do
    $run -N -e "SHOW BINLOG EVENTS IN '$f'" | awk -F'\t' '$3 == "Gtid" {sub(/.*GTID_NEXT= /, "", $6); print "  " $6 "  " $1 " pos " $2}'
  done
}
step "0. Cum moi, noi replication bang GTID"
fresh
sql -e "CREATE USER 'repl'@'%' IDENTIFIED BY 'repl'; GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';"
P=$(sql -N -e "SELECT @@gtid_executed" | tr -d '\r\n')
sql2 -e "RESET MASTER; SET GLOBAL gtid_purged='$P'; CHANGE REPLICATION SOURCE TO SOURCE_HOST='mysql', SOURCE_USER='repl', SOURCE_PASSWORD='repl', SOURCE_AUTO_POSITION=1, GET_SOURCE_PUBLIC_KEY=1; START REPLICA;"
sql < sql/mysql-init.sql
sql < sql/mysql-changes.sql
sql -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('GT-01','Ghi truoc failover',1000.00,1);"
sleep 3
echo "server_uuid: mysql=$(sql -N -e 'SELECT @@server_uuid') mysql2=$(sql2 -N -e 'SELECT @@server_uuid')"
echo "gtid_executed: mysql=$(sql -N -e 'SELECT @@gtid_executed') mysql2=$(sql2 -N -e 'SELECT @@gtid_executed')"
echo "-- mysql (primary):"; gtids mysql | tail -6
echo "-- mysql2 (replica):"; gtids mysql2 | tail -6
