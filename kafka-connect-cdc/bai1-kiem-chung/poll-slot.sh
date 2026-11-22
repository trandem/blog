# Chay trong container pgsrc: in thoi diem va confirmed_flush_lsn cua slot moi 0,2 giay trong 45 giay
for i in $(seq 1 225); do
  psql -U src -d shop -At -F ' ' -c "SELECT to_char(clock_timestamp(), 'HH24:MI:SS.MS'), confirmed_flush_lsn FROM pg_replication_slots WHERE slot_name = 'dbz_slot_filtered'"
  sleep 0.2
done
