#!/bin/bash
# 后端停止脚本
DIR="$(cd "$(dirname "$0")" && pwd)"
PID=$(ps -ef | grep "$DIR/app.jar" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
  kill -15 $PID
  echo "已发送 SIGTERM 到 PID $PID"
else
  echo "未发现运行中的进程"
fi
