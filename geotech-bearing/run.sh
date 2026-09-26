#!/usr/bin/env bash
# 一步构建并拉起服务 + PostgreSQL 16。
set -euo pipefail
cd "$(dirname "$0")"

docker compose up --build -d

echo "等待服务就绪 ..."
for i in $(seq 1 40); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "服务已就绪： http://localhost:8080"
    exit 0
  fi
  sleep 2
done
echo "服务在预期时间内未就绪，请用 'docker compose logs api' 查看日志。" >&2
exit 1
