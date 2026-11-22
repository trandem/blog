#!/usr/bin/env bash
# Kiem chung 3: cum hai worker cung group.id. Gui cau hinh qua worker khong phai leader,
# roi SIGKILL mot worker va do thoi gian task chuyen sang worker con lai.
source "$(dirname "$0")/chung.sh"
export COMPOSE_PROFILES=worker2
export CONNECT_HEAP="-Xms256m -Xmx512m"

on() { docker compose exec -T "$1" curl -s "http://localhost:8083$2"; echo; }

step "0. Cum moi voi hai worker"
fresh
IP1=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" $(docker compose ps -q connect))
IP2=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" $(docker compose ps -q connect2))
echo "connect=$IP1 connect2=$IP2"
sql < sql/mysql-init.sql

step "1. Gui cau hinh qua connect2"
for c in mysql-source outbox-source; do
  docker compose exec -T connect2 curl -s -o /dev/null -w "$c qua connect2: http %{http_code}\n" \
    -X PUT -H "Content-Type: application/json" --data @- "http://localhost:8083/connectors/$c/config" < connectors/$c.json
done
wait_running mysql-source; wait_running outbox-source; sleep 5
LEADER=$(clog connect | grep -o "leaderUrl='[^']*'" | tail -1)
echo "leader: $LEADER"
clog connect2 | grep -iE "forward|leader" | grep -i "request" | tail -3 | cut -c1-220
for c in mysql-source outbox-source; do echo "$c: $(on connect /connectors/$c/status | grep -o '"tasks":\[.*\]')"; done

if echo "$LEADER" | grep -q "$IP1"; then VICTIM=connect2; VIP=$IP2; SURV=connect; else VICTIM=connect; VIP=$IP1; SURV=connect2; fi
echo "kill worker khong phai leader: $VICTIM ($VIP)"

step "2. SIGKILL $VICTIM, do thoi gian task cua no chay lai tren $SURV"
T0=$(date +%s)
docker compose kill -s SIGKILL $VICTIM > /dev/null 2>&1
for i in $(seq 1 100); do
  sleep 5
  now=$(( $(date +%s) - T0 ))
  line=""
  for c in mysql-source outbox-source; do
    line="$line $c=$(on $SURV /connectors/$c/status | grep -o '"tasks":\[{"id":0,"state":"[A-Z]*","worker_id":"[^"]*"' | sed -E 's/.*"state":"([A-Z]*)","worker_id":"([^"]*)"/\1@\2/')"
  done
  echo "t=+${now}s$line"
  echo "$line" | grep -q "$VIP" || break
done
echo "-- log cua $SURV:"
clog $SURV | grep -iE "delay|lost|Rebalance started|Joined group" | tail -12 | sed -E 's/^[0-9-]+ ([0-9:,]+) +[A-Z]+ +/\1 /' | cut -c1-230
