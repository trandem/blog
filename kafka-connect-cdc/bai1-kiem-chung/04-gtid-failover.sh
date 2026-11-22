#!/usr/bin/env bash
# Kiem chung 4: GTID khi failover. mysql2 la replica GTID cua mysql. Connector doc mysql,
# dung lai, primary chet, replica len primary, connector chuyen sang mysql2 va doc tiep theo GTID.
source "$(dirname "$0")/chung.sh"
export COMPOSE_PROFILES=gtid

# gtid_pos HOST GTID: tim file va vi tri bat dau cua transaction mang GTID do trong binlog cua HOST
gtid_pos() {
  local run=sql; [ "$1" = mysql2 ] && run=sql2
  for f in $($run -N -e "SHOW BINARY LOGS" | awk '{print $1}'); do
    $run -N -e "SHOW BINLOG EVENTS IN '$f'" | awk -v g="$2" -v f="$f" -F'\t' '$6 ~ g"'"'"'" {print "  " f " pos " $2 ": " $6}'
  done
}

step "0. Cum moi: mysql (primary) va mysql2 (replica)"
fresh
echo "-- gia tri mac dinh tren mysql2 (khong truyen --log-bin, --binlog-format...):"
sql2 -e "SELECT @@log_bin, @@log_bin_basename, @@log_replica_updates, @@binlog_format, @@binlog_row_image, @@gtid_mode\G" | grep -v '^\*'
echo "-- server_uuid: mysql=$(sql -N -e 'SELECT @@server_uuid') mysql2=$(sql2 -N -e 'SELECT @@server_uuid')"

step "1. Noi replication bang GTID (SOURCE_AUTO_POSITION=1)"
sql -e "CREATE USER 'repl'@'%' IDENTIFIED BY 'repl'; GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';"
P=$(sql -N -e "SELECT @@gtid_executed" | tr -d '\r\n')
sql2 -e "RESET MASTER; SET GLOBAL gtid_purged='$P'; CHANGE REPLICATION SOURCE TO SOURCE_HOST='mysql', SOURCE_USER='repl', SOURCE_PASSWORD='repl', SOURCE_AUTO_POSITION=1, GET_SOURCE_PUBLIC_KEY=1; START REPLICA;"
sql < sql/mysql-init.sql
sleep 3
sql2 -e "SHOW REPLICA STATUS\G" | grep -E "Replica_IO_Running:|Replica_SQL_Running:|Executed_Gtid_Set|Last_.*Error:"
echo "-- products tren mysql2: $(sql2 -N -e 'SELECT COUNT(*) FROM shop.products') dong"

step "2. Connector doc primary, chay mysql-changes.sql"
put_config mysql-source connectors/mysql-source.json; wait_running mysql-source; sleep 5
sql < sql/mysql-changes.sql
sleep 8
topic shop.shop.products > $OUT/04-truoc.txt
perl $K/phan-tich.pl msgs $OUT/04-truoc.txt | grep -o '^#[0-9]* .*op=[a-z].*gtid=[^ ]* file=[^ ]* pos=[0-9]*' | sed -E 's/key=.*op=/op=/'
G=$(perl $K/phan-tich.pl msgs $OUT/04-truoc.txt | grep 'op=u' | grep -o 'gtid=[^ ]*' | cut -d= -f2)
echo "-- transaction UPDATE gia mang GTID $G. Vi tri trong binlog cua tung may:"
echo " mysql:";  gtid_pos mysql  "$G"
echo " mysql2:"; gtid_pos mysql2 "$G"

step "3. Dung connector, ghi GT-01 vao primary, primary chet, mysql2 len primary"
connect PUT /connectors/mysql-source/stop; sleep 5
echo "-- offset khi dung:"; connect GET /connectors/mysql-source/offsets
sql -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('GT-01','Ghi truoc failover',1000.00,1);"
sleep 2
echo "-- GT-01 tren mysql2: $(sql2 -N -e "SELECT COUNT(*) FROM shop.products WHERE sku='GT-01'")"
docker compose stop mysql > /dev/null 2>&1; echo "-- mysql da dung"
sql2 -e "STOP REPLICA; RESET REPLICA ALL;"

step "4. Tro connector sang mysql2, resume, ghi GT-02 vao primary moi"
put_config mysql-source $C/mysql-source-mysql2.json
connect PUT /connectors/mysql-source/resume
wait_running mysql-source; sleep 5
sql2 -e "INSERT INTO shop.products (sku,name,price,stock) VALUES ('GT-02','Ghi sau failover',2000.00,1);"
sleep 10
status mysql-source | cut -c1-300
topic shop.shop.products > $OUT/04-sau.txt
echo "-- so message: tong $(grep -c . $OUT/04-sau.txt), GT-01: $(grep -c GT-01 $OUT/04-sau.txt), GT-02: $(grep -c GT-02 $OUT/04-sau.txt)"
perl $K/phan-tich.pl msgs $OUT/04-sau.txt | grep -E "GT-0" -B2 | grep -o '^#[0-9]* .*op=[a-z].*gtid=[^ ]* file=[^ ]* pos=[0-9]*' | sed -E 's/key=.*op=/op=/'
ops shop.shop.products
echo "-- offset sau failover:"; connect GET /connectors/mysql-source/offsets
echo "-- log connector:"
clog | grep -iE "gtid|Connected to binlog at mysql2" | grep -v "gtid_mode" | tail -8 | sed -E 's/^[0-9-]+ ([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-300
