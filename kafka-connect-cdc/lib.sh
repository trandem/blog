# Ham dung chung cho cac bai 2-4. Chay tu thu muc kafka-connect-cdc:  source lib.sh
# Moi lenh goi qua `docker compose exec`, khong can curl hay Kafka CLI tren may host.
export MSYS_NO_PATHCONV=1   # Git Bash tren Windows: khong doi duong dan /opt/... thanh duong dan Windows

# connect METHOD PATH [file-json]: goi REST API cua Kafka Connect
connect() {
  if [ -n "${3:-}" ]; then
    docker compose exec -T connect curl -s -X "$1" -H "Content-Type: application/json" \
      --data @- "http://localhost:8083$2" < "$3"
  else
    docker compose exec -T connect curl -s -X "$1" "http://localhost:8083$2"
  fi
  echo
}

# put_config TEN FILE: tao hoac cap nhat connector
put_config() { connect PUT "/connectors/$1/config" "$2" > /dev/null && echo "$1: da gui cau hinh $2"; }

# status TEN: trang thai connector va task (trace ngan gon khi FAILED)
status() { connect GET "/connectors/$1/status" | sed -E 's/\\n\\tat .*"/ ..."/'; }

# topic TEN [timeout-ms]: doc toan bo topic kem key
topic() {
  docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
    --topic "$1" --from-beginning --timeout-ms "${2:-5000}" --formatter-property print.key=true 2>/dev/null
}

# offsets TEN: so message moi partition cua topic
offsets() { docker compose exec -T kafka /opt/kafka/bin/kafka-get-offsets.sh --bootstrap-server kafka:9092 --topic "$1"; }

# ops TEN: dem message theo op (r, c, u, d) cua topic CDC
ops() { topic "$1" | grep -o '"op":"[a-z]"' | sort | uniq -c || true; }

# sql: chay SQL bang root, doc tu stdin hoac -e "..."
sql() { docker compose exec -T mysql mysql -uroot -proot "$@" 2>&1 | grep -v "Using a password" || true; }
