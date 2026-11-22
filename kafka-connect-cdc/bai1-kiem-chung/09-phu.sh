#!/usr/bin/env bash
# Kiem chung 9: cac chi tiet le. decimal.handling.mode=string, bang khong co khoa chinh, message.key.columns,
# JDBC sink khi tat schema, moi connector mot luong binlog, binlog_row_metadata mac dinh.
source "$(dirname "$0")/chung.sh"
PA="perl $K/phan-tich.pl"

step "0. Cum moi"
fresh
sql < sql/mysql-init.sql
sql -e "CREATE TABLE shop.no_pk (sku VARCHAR(32) NOT NULL, qty INT NOT NULL); INSERT INTO shop.no_pk VALUES ('A-1', 1);"
sql -e "SELECT @@binlog_row_metadata, @@binlog_format, @@binlog_row_image\G" | grep -v '^\*'
for c in dec-string nopk nopk-keycols noschema; do put_config $c $C/$c.json; done
for c in dec-string nopk nopk-keycols noschema; do wait_running $c; done
sleep 5
sql -e "UPDATE shop.products SET price = 1190000.00 WHERE id = 1; UPDATE shop.no_pk SET qty = 2 WHERE sku = 'A-1';"
sleep 8

step "1. decimal.handling.mode=string"
topic dec.shop.products > $OUT/09-dec.txt; $PA msgs $OUT/09-dec.txt 4 | sed -E 's/ key=[^ ]* value=[^ ]*//'

step "2. Bang khong co khoa chinh: key cua message"
topic nopk.shop.no_pk > $OUT/09-nopk.txt; cut -c1-60 $OUT/09-nopk.txt
step "3. message.key.columns=shop.no_pk:sku"
topic nopk2.shop.no_pk > $OUT/09-nopk2.txt; cut -c1-160 $OUT/09-nopk2.txt | sed -E 's/\t.*//'

step "4. Topic khong kem schema va JDBC sink"
topic ns.shop.products | head -1 | cut -c1-200
put_config sink-noschema $C/sink-noschema.json; sleep 15
status sink-noschema | cut -c1-500
clog | grep -iE "schema" | grep -iE "error|exception|requires|null" | tail -3 | cut -c1-300

step "5. Moi connector mot luong binlog rieng"
sql -e "SELECT ID, USER, COMMAND, STATE FROM information_schema.PROCESSLIST WHERE COMMAND LIKE 'Binlog Dump%'"
