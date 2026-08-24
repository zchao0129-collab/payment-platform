#!/bin/bash
# 后端启动脚本 — 与 app.jar 同目录；外置配置放在 config/application.yml
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/app.jar"
CONFIG="$DIR/config/application.yml"
LOG="$DIR/logs/app.log"
mkdir -p "$DIR/logs"

JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

CONFIG_ARG=""
if [ -f "$CONFIG" ]; then
  CONFIG_ARG="--spring.config.additional-location=file:$CONFIG"
fi

nohup java $JAVA_OPTS -jar "$JAR" $CONFIG_ARG > "$LOG" 2>&1 &
echo "PID: $!"
echo "日志: $LOG"
