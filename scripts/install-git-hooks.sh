#!/usr/bin/env bash
set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit

echo "[RoleOS] 已启用仓库 Git Hooks：.githooks"
echo "[RoleOS] 建议安装 gitleaks，以在提交前扫描暂存内容。"
