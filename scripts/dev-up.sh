#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[RoleOS] 缺少命令：$1" >&2
    exit 1
  fi
}

require_command docker
require_command mvn
require_command java

if ! docker info >/dev/null 2>&1; then
  echo "[RoleOS] Docker daemon 未运行，请先启动 Docker Desktop。" >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "[RoleOS] 已从 .env.example 创建本地 .env，请按需修改其中的开发配置。"
fi

set -a
# .env 是受仓库控制的本地开发配置文件。
source .env
set +a

echo "[RoleOS] 正在启动 PostgreSQL..."
docker compose up -d postgres

echo "[RoleOS] 等待 PostgreSQL 健康检查..."
for attempt in {1..30}; do
  postgres_health="$(docker compose ps --format '{{.Health}}' postgres 2>/dev/null)"
  if [[ "$postgres_health" == "healthy" ]]; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    echo "[RoleOS] PostgreSQL 未在预期时间内就绪，请执行：docker compose logs postgres" >&2
    exit 1
  fi
  sleep 2
done

echo "[RoleOS] PostgreSQL 已就绪，正在构建应用..."
mvn -B -ntp -pl roleos-boot -am package -DskipTests

echo "[RoleOS] 正在启动应用：http://localhost:${ROLEOS_SERVER_PORT:-8080}"
exec java -jar roleos-boot/target/roleos-boot-0.1.0-SNAPSHOT.jar
