# 支付商户管理平台 — 完整部署方案

## 一、架构总览

```
┌─────────────────────────────────────────────────────┐
│                    用户/浏览器                         │
│   http://your-domain.com          (商户端 + 管理端)     │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│      Nginx (80/443)     │  ← 静态文件 + 反向代理
│  /            → 前端 SPA │
│  /api/*      → 后端 API │
└─────────┬───────────────┘
          │
    ┌─────┴─────┐
    ▼           ▼
┌────────┐ ┌──────────────────┐
│ 前端     │ │ 后端 Spring Boot  │
│ dist/   │ │ :8080             │
│ (Nginx  │ │ ┌──────────────┐ │
│  静态)   │ │ │ JWT + Security│ │
└────────┘ │ │ MyBatis-Plus  │ │
           │ │ Redisson Lock │ │
           │ └──────┬───────┘ │
           └────────┼─────────┘
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
   ┌──────────────┐   ┌──────────────┐
   │  MySQL 8.0   │   │  Redis 7     │
   │  :3306       │   │  :6379       │
   └──────────────┘   └──────────────┘
```

---

## 二、环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| OS | CentOS 7+ / Ubuntu 20.04+ / Windows Server | 推荐 Linux |
| JDK | 24 | `JAVA_HOME` 需正确配置 |
| MySQL | 8.0+ | 字符集 utf8mb4 |
| Redis | 6.0+ 或 7.x | 用于分布式锁 + 缓存 |
| Nginx | 1.20+ | 反向代理 + 静态资源 |
| Node.js | 18+ (仅构建用) | 前端编译，运行时不需要 |
| Maven | 3.9+ (仅构建用) | 后端编译，运行时不需要 |

---

## 三、数据库初始化

### 3.1 建库建表

```bash
# 连接 MySQL
mysql -h 127.0.0.1 -u root -p

# 执行建表脚本
source /opt/payment-platform/src/main/resources/db/schema.sql;

# （可选）导入测试数据  
source /opt/payment-platform/src/main/resources/db/test-data.sql;
```

### 3.2 创建专用账号（推荐）

```sql
CREATE USER 'payment_platform'@'%' IDENTIFIED BY 'your_strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON payment_platform.* TO 'payment_platform'@'%';
FLUSH PRIVILEGES;
```

### 3.3 Redis 配置

```bash
# redis.conf 关键配置
requirepass your_strong_redis_password
maxmemory 256mb
maxmemory-policy allkeys-lru
```

---

## 四、配置准备

### 4.1 生产环境配置文件

创建 `src/main/resources/application-prod.yml`：

```yaml
# ============================================================
# Profile: prod — 生产环境
# 激活方式: --spring.profiles.active=prod
# ============================================================
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/payment_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: payment_platform
    password: ${DB_PASSWORD}                    # ← 环境变量注入
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      idle-timeout: 300000
      max-lifetime: 1200000

  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 8

# 收银台前端地址（部署后改为正式域名）
app:
  cashier-base-url: https://your-domain.com

# 文件上传路径
file:
  upload-path: /data/payment-platform/upload

# 关闭 SQL 日志（生产环境）
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

logging:
  level:
    root: WARN
    com.payment.platform: INFO
  file:
    path: /data/payment-platform/logs
    name: /data/payment-platform/logs/app.log
```

### 4.2 环境变量（敏感信息通过环境变量注入）

```bash
# /etc/profile.d/payment-platform.sh
export DB_PASSWORD="your_strong_db_password"
export REDIS_PASSWORD="your_strong_redis_password"
export JWT_SECRET="your-256-bit-secret-key-change-in-production"
```

---

## 五、构建打包

### 5.1 后端构建（在开发机或 CI 服务器上）

```bash
cd payment-platform

# 设置 JDK
export JAVA_HOME=/path/to/jdk-24

# 编译打包
./mvnw clean package -DskipTests -P prod

# 产物位置
ls -lh target/payment-platform-1.0.0.jar
# ~60MB (含所有依赖的 fat JAR)
```

**Windows 下：**
```cmd
set JAVA_HOME=C:\path\to\jdk-24
mvnw.cmd clean package -DskipTests
```

### 5.2 前端构建

```bash
cd payment-platform/frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 产物位置
ls -lh dist/
# index.html + assets/ (JS/CSS/图片)
```

---

## 六、部署到服务器

### 6.1 目录规划

```
/data/payment-platform/
├── app.jar                  # 后端 JAR
├── dist/                    # 前端静态文件 (Nginx 指向这里)
├── upload/                  # 上传文件目录
├── logs/                    # 应用日志
├── start.sh                 # 启动脚本
├── stop.sh                  # 停止脚本
└── config/
    └── application-prod.yml # 外部配置文件（可选）
```

### 6.2 上传产物

```bash
# 在开发机上执行
scp target/payment-platform-1.0.0.jar user@server:/data/payment-platform/app.jar
scp -r frontend/dist/* user@server:/data/payment-platform/dist/
```

### 6.3 后端启动脚本

`/data/payment-platform/start.sh`：

```bash
#!/bin/bash
APP_HOME=/data/payment-platform
JAR=$APP_HOME/app.jar
LOG=$APP_HOME/logs/app.log

# JVM 参数
JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$APP_HOME/logs"
JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prod"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Duser.timezone=Asia/Shanghai"

# 创建目录
mkdir -p $APP_HOME/logs $APP_HOME/upload

# 启动
nohup java $JAVA_OPTS -jar $JAR > $LOG 2>&1 &

echo "PID: $!"
echo "Starting... wait 30s then check http://localhost:8080/actuator/health"
```

`/data/payment-platform/stop.sh`：

```bash
#!/bin/bash
PID=$(ps -ef | grep 'payment-platform.*\.jar' | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    kill -15 $PID
    echo "Sent SIGTERM to PID $PID, waiting..."
    sleep 10
    # 强制杀
    kill -9 $PID 2>/dev/null
    echo "Stopped."
else
    echo "No running process found."
fi
```

### 6.4 Nginx 配置

`/etc/nginx/conf.d/payment-platform.conf`：

```nginx
server {
    listen       80;
    server_name  your-domain.com;

    # 强制 HTTPS（有证书时启用）
    # return 301 https://$host$request_uri;

    charset utf-8;
    client_max_body_size 10m;

    # ============ 前端 SPA ============
    location / {
        root   /data/payment-platform/dist;
        index  index.html;
        try_files $uri $uri/ /index.html;   # Vue Router history mode
    }

    # ============ 后端 API 反向代理 ============
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    # ============ API 文档（内网可关闭） ============
    location /doc.html {
        proxy_pass http://127.0.0.1:8080;
    }
    location /swagger-ui/ {
        proxy_pass http://127.0.0.1:8080;
    }
    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8080;
    }

    # ============ 静态资源缓存 ============
    location /assets/ {
        root /data/payment-platform/dist;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # ============ 安全头 ============
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

重载 Nginx：

```bash
nginx -t && nginx -s reload
```

---

## 七、HTTPS 配置（推荐）

```bash
# 使用 Certbot 获取免费证书
yum install -y certbot python3-certbot-nginx   # CentOS
# 或
apt install -y certbot python3-certbot-nginx   # Ubuntu

certbot --nginx -d your-domain.com

# 证书自动续期（添加到 crontab）
# 0 3 * * * certbot renew --quiet
```

---

## 八、Systemd 服务（推荐，替代 nohup）

`/etc/systemd/system/payment-platform.service`：

```ini
[Unit]
Description=Payment Merchant Platform
After=network.target mysql.service redis.service

[Service]
Type=simple
User=appuser
Group=appuser
WorkingDirectory=/data/payment-platform
ExecStart=/usr/bin/java \
    -Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -jar /data/payment-platform/app.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:/data/payment-platform/logs/stdout.log
StandardError=append:/data/payment-platform/logs/stderr.log

[Install]
WantedBy=multi-user.target
```

启用服务：

```bash
systemctl daemon-reload
systemctl enable payment-platform
systemctl start payment-platform
systemctl status payment-platform

# 查看日志
journalctl -u payment-platform -f
```

---

## 九、定时任务

后端已内置两个定时任务，确保定时任务使用了 Redisson 分布式锁（多实例互斥）：

| 任务 | 类 | 说明 |
|------|-----|------|
| 订单过期 | `OrderServiceImpl.expireOrders()` | 超时5分钟的订单自动失效 |
| 佣金结算 | `CommissionServiceImpl.generateDailyCommissions()` | 每日凌晨结算佣金 |

---

## 十、安全清单

| 项 | 操作 |
|----|------|
| JWT Secret | 改为随机生成的 ≥256-bit 字符串 |
| DB 密码 | 通过环境变量注入，不写入配置文件 |
| Redis 密码 | 同上 |
| 防火墙 | 仅开放 80/443 端口，8080/3306/6379 仅本机访问 |
| 文件上传 | 限制 5MB，路径不要在 Web 根目录 |
| SQL 日志 | 生产环境关闭 `StdOutImpl` |
| 支付宝回调 | `notify-url` 必须是 HTTPS 公网可达地址 |
| API 文档 | 生产环境可关闭 Knife4j/Swagger |

---

## 十一、快速部署清单

```bash
# === 1. 服务器环境 ===
yum install -y java-24-openjdk nginx redis mysql-server

# === 2. 数据库 ===
mysql -u root -p < schema.sql

# === 3. 环境变量 ===
echo 'export DB_PASSWORD="xxx"' >> /etc/profile.d/payment.sh
echo 'export REDIS_PASSWORD="xxx"' >> /etc/profile.d/payment.sh
source /etc/profile.d/payment.sh

# === 4. 部署文件 ===
mkdir -p /data/payment-platform/{upload,logs}
scp app.jar user@server:/data/payment-platform/
scp -r frontend/dist/* user@server:/data/payment-platform/dist/

# === 5. Nginx ===
cp nginx.conf /etc/nginx/conf.d/payment-platform.conf
nginx -t && nginx -s reload

# === 6. 启动 ===
systemctl start payment-platform

# === 7. 验证 ===
curl http://localhost:8080/api/statistics/revenue   # API 健康
curl http://localhost/                               # 前端页面
```

---

## 十二、故障排查

| 问题 | 检查项 |
|------|--------|
| 后端启动失败 | `tail -100 /data/payment-platform/logs/app.log` |
| 数据库连接失败 | `telnet <db_host> 3306`、MySQL 账号权限 |
| Redis 连接失败 | `redis-cli -a <password> ping` |
| 前端 404 | Nginx `try_files` 配置、Vue Router history mode |
| 码牌扫码无效 | `app.cashier-base-url` 是否公网可达 |
| 内存溢出 | JVM `-Xmx` 参数、`jstat -gc <pid> 1s` |
