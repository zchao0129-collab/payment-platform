#!/bin/bash
# 一键打包脚本（Linux/macOS）：构建后端 jar + 前端 dist，组装到 release/
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
RELEASE="$ROOT/release"

echo "==> 清理 release/"
rm -rf "$RELEASE"
mkdir -p "$RELEASE/backend/config" "$RELEASE/frontend"

echo "==> [1/4] 构建后端 (mvn clean package -DskipTests)"
(cd "$ROOT" && mvn clean package -DskipTests -q)
cp "$ROOT/target/payment-platform-1.0.0.jar" "$RELEASE/backend/app.jar"

echo "==> [2/4] 复制外置配置与启停脚本"
cp "$ROOT/deploy/config/application.yml.example" "$RELEASE/backend/config/application.yml.example"
cp "$ROOT/deploy/backend/start.sh" "$ROOT/deploy/backend/stop.sh" "$ROOT/deploy/backend/start.bat" "$RELEASE/backend/"

echo "==> [3/4] 构建前端 (npm run build)"
(cd "$ROOT/frontend" && npm run build)
cp -r "$ROOT/frontend/dist/." "$RELEASE/frontend/"
cp "$ROOT/frontend/nginx.conf.example" "$RELEASE/frontend/nginx.conf.example"

echo "==> [4/4] 复制说明"
cp "$ROOT/deploy/README.md" "$RELEASE/README.md"

echo ""
echo "打包完成: $RELEASE"
ls -R "$RELEASE"
