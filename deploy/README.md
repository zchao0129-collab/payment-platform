# 部署说明

> 打包产出目录 `release/`（不入库），结构如下：

```
release/
├── README.md                  # 本说明
├── backend/
│   ├── app.jar                # 后端可执行 jar（配置已剥离）
│   ├── config/
│   │   └── application.yml.example   # 外置配置模板 → 复制为 application.yml
│   ├── start.sh / stop.sh     # Linux 启停
│   └── start.bat              # Windows 启动
└── frontend/
    ├── index.html 等静态文件   # Vue 构建产物（含运行时 config.js）
    └── nginx.conf.example     # Nginx 反代示例
```

## 一、后端部署

1. **准备外置配置**
   ```bash
   cd backend/config
   cp application.yml.example application.yml
   # 编辑 application.yml，填入数据库/Redis/JWT/域名等真实配置
   ```
   敏感项（数据库密码、Redis 密码、JWT 密钥）建议用环境变量注入，例如：
   ```bash
   export DB_PASSWORD=xxx REDIS_PASSWORD=xxx JWT_SECRET=xxx
   ```

2. **启动 / 停止**
   ```bash
   cd backend
   ./start.sh    # 后台启动，日志在 logs/app.log
   ./stop.sh     # 停止
   ```
   Windows 下运行 `start.bat`（前台）。

3. **配置优先级说明**
   启动脚本会以 `--spring.config.additional-location=file:config/application.yml` 加载外置配置，
   覆盖 jar 内的默认值；环境变量 `${VAR}` 优先于文件默认值。

## 二、前端部署

前端为纯静态文件，用 Nginx 托管即可。根目录的 `config.js` 是**运行时配置**，
改它不需要重新构建：

```js
window.__APP_CONFIG__ = {
  apiBaseUrl: '/api',              // 改成实际 API 地址（同域反代保持 /api）
  appTitle: '支付商户管理平台',
}
```

Nginx 反代示例（`nginx.conf.example`）关键段：

```nginx
location / {
    root   /data/payment-platform/frontend;
    index  index.html;
    try_files $uri $uri/ /index.html;   # history 路由回退
}
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

## 三、重新打包

在项目根目录执行（Windows 用 `package.bat`，Linux 用 `package.sh`），
自动构建后端 jar + 前端 dist 并组装到 `release/`。
