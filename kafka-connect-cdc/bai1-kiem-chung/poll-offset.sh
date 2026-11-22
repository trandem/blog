# Chay trong container connect: in thoi diem va offset da ghi cua pg-filtered moi 0,2 giay trong 45 giay
for i in $(seq 1 225); do
  echo "$(date +%H:%M:%S.%3N) $(curl -s http://localhost:8083/connectors/pg-filtered/offsets)"
  sleep 0.2
done
