# 支付管理平台前端 静态资源部署说明

本目录为「支付商户管理平台」前端的静态构建产物（Vue 3 + Vite），包含：

- **管理端（PC）**：系统配置、用户管理、订单、商户、统计等
- **商户端（PC）**：商户订单、佣金、码牌、提现等
- **收银台（H5）**：顾客扫码后打开的移动支付页 `/app/cashier`
- **商户移动页（H5）**：`/app/...` 系列移动端页面

## 一、目录结构

```
支付管理平台_前端静态资源部署包/
├── html/                  # 站点文件（部署到 Web 根目录即可）
├── nginx.conf.example     # Nginx 配置示例
└── README.md
```

## 二、部署步骤（以 Nginx 为例）

1. 将 `html/` 目录下的所有内容上传到服务器，例如 `/data/payment-platform/dist/`
2. 使用 `nginx.conf.example` 作为参考，修改 `server_name`、`root`、`proxy_pass` 后 `nginx -s reload`
3. 浏览器访问 `https://qrpay.csmmkj.cn/` 即可打开管理/商户端，扫码打开收银台

## 三、⚠ 必须注意的事项

### 1. 必须部署在域名根路径
`index.html` 中 JS/CSS 用绝对路径 `/assets/...` 引用，**必须部署在域名根路径**，不能放子目录，否则 404。

### 2. 接口走相对路径 `/api`（同域代理，已规避 CORS）
前端构建时（`.env.production`）已将接口地址内联为相对路径 `/api`：

```js
baseURL = "/api"   // 来源：VITE_API_BASE_URL=/api
```

- 请求发到「当前域名 + /api」，由 Nginx 的 `location /api/` 反代到后端 `127.0.0.1:8080`。
- 同域部署，**无需 CORS 配置**。

### 3. 收银台域名必须与码牌一致
码牌二维码内容指向 `https://qrpay.csmmkj.cn/app/cashier`（后端 `app.cashier-base-url` 配置）。
因此本前端**必须部署在 `qrpay.csmmkj.cn` 根路径**，否则扫码后收银台打不开。

### 4. 与「商户端 App H5」区分
本项目另有一套 Expo 商户端 App（可导出 H5，见 `payment-merchant-app`），
那是**另一套产物**，需部署在**不同的域名**（因其 index.html 也是根路径绝对引用，不能与本前端同域）。
若两者都要上线，建议：
- 本前端（管理端 + 收银台）→ `qrpay.csmmkj.cn`
- 商户端 App H5 → 另一个子域（如 `m.qrpay.csmmkj.cn`），并在其 nginx 中同样配置 `/api` 反代

## 四、重新构建前端

```bash
cd payment-platform/frontend
npm run build        # 读取 .env.production（VITE_API_BASE_URL=/api）
# 产物在 dist/ 目录，重新打包即可
```
