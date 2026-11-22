# Ham dung chung cho cac kich ban kiem chung bai 1. Moi script: source "$(dirname "$0")/chung.sh"
set -u
cd "$(dirname "${BASH_SOURCE[0]}")/.."          # thu muc kafka-connect-cdc
export COMPOSE_PATH_SEPARATOR=";"
export COMPOSE_FILE="docker-compose.yml;bai1-kiem-chung/compose.yml"
source lib.sh
K=bai1-kiem-chung
C=$K/connectors
OUT=$K/ket-qua
mkdir -p "$OUT"

step() { echo; echo "=================== $* ==================="; }

# wait_running TEN [giay]: cho task 0 cua connector o trang thai RUNNING
wait_running() {
  for i in $(seq 1 "${2:-90}"); do
    connect GET "/connectors/$1/status" | grep -q '"tasks":\[{"id":0,"state":"RUNNING"' && return 0
    sleep 1
  done
  echo "!! $1 chua RUNNING sau ${2:-90}s"; status "$1" | cut -c1-600
}

# fresh: dung lai cum tu dau (khong co volume nao)
fresh() {
  docker compose --profile worker2 --profile gtid --profile pgsrc down --remove-orphans > /dev/null 2>&1
  docker compose up -d --wait 2>&1 | tail -1
}

pdst() { docker compose exec -T postgres psql -U replica -d replica "$@"; }
psrc() { docker compose exec -T pgsrc psql -U src -d shop "$@"; }
sql2() { docker compose exec -T mysql2 mysql -uroot -proot "$@" 2>&1 | grep -v "Using a password" || true; }
kt()   { docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 "$@"; }
groups() { docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 "$@"; }
clog() { docker compose logs --no-log-prefix "${1:-connect}"; }
