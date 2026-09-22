#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

if [[ -f .env ]]; then
  set -a
  # 仅在当前进程中加载本地开发配置；不得输出或提交其中的凭据。
  source .env
  set +a
fi

maven_arguments=(-B -ntp -Pquality verify)
if [[ -n "${NVD_API_KEY:-}" ]]; then
  # Dependency-Check 不会自动将环境变量映射为其 Maven 用户属性。
  maven_arguments+=("-DnvdApiKey=${NVD_API_KEY}")
else
  echo "[RoleOS] 未配置 NVD_API_KEY；将以匿名 NVD API 运行，首次同步可能很慢。" >&2
fi

exec mvn "${maven_arguments[@]}"
