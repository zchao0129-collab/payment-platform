# 支付商户端 H5 部署说明

本目录为「支付商户端」App 的 H5（网页版）静态部署包，由 Expo SDK 52 通过 `expo export --platform web` 导出。

## 一、目录结构

```
支付商户端_H5部署包/
├── html/                  # 站点文件（部署即可）
├── nginx.conf.example     # Nginx 配置示例
└── README.md
```

## 二、部署步骤（以 Nginx 为例）

1. 将 `html/` 目录下的所有内容上传到服务器，例如 `/var/www/payment-merchant-h5/`
2. 使用 `nginx.conf.example` 作为参考，修改 `server_name`、`root`、`proxy_pass` 后 `nginx -s reload`
3. 浏览器访问 `https://你的域名/` 即可打开

关键配置说明：

```nginx
root /var/www/payment-merchant-h5;          # 指向 html 目录

location / {
    try_files $uri $uri/ /index.html;       # SPA 路由回退（必须）
}

location /api/ {                            # 后端反向代理（同域，无需 CORS）
    proxy_pass http://127.0.0.1:8080;
}

location /_expo/  { expires 30d; add_header Cache-Control "public, immutable"; }
location /assets/ { expires 30d; add_header Cache-Control "public, immutable"; }
```

## 三、重要说明

### 1. 支持根路径与子目录部署（已用相对路径）
本次构建已设置 `experiments.baseUrl: "./"`，`index.html` 与 JS 资源、图标字体、返回箭头 PNG 均使用**相对路径**（`./_expo/...`、`./assets/...`），
因此**既可部署在域名根路径，也可放在子目录**（如 `https://xxx.cn/h5/`），图标与返回按钮都能正常显示。

> 注意：若放在子目录，Nginx 的 `location /` 需要指向该子目录对应的 `root`，`/api/` 反代仍需保留。

### 2. 接口走相对路径 `/api`（同域代理，已规避 CORS）
本次 H5 构建把接口地址内联为相对路径 `/api`：

```js
baseURL = "/api"
```

- 页面请求会发到「当前域名 + /api」，由 Nginx 的 `location /api/` 反代到后端 `127.0.0.1:8080`。
- 与后端**同域部署，因此无需任何 CORS 配置**。
- 若后端不在同一台服务器，改 `nginx.conf.example` 中 `proxy_pass` 为后端内网地址即可。

> 说明：`.env` 里的 `EXPO_PUBLIC_API_URL` 仍是绝对地址 `https://qrpay.csmmkj.cn/api`，
> 那是给**原生 App（APK/iOS）**用的（原生端没有同域概念，必须绝对地址）。
> H5 与原生端两套产物互不影响。

### 3. 弹窗（退出登录 / 修改密码 / 重新生成码牌等）在 H5 上已兼容
`react-native-web` 的 `Alert.alert` 是空实现，网页上原会静默失效（表现为「退出登录点了没反应」）。
本次已新增 `src/utils/dialog.js` 跨端封装：原生端走 `Alert`，H5 端走浏览器 `window.alert / window.confirm`，所有弹窗均已接入。

### 4. 收款到账语音播报（前台轮询）
登录后每 5 秒轮询一次「已支付」订单，新到账订单会自动语音播报「微信/支付宝收款到账 XX 元」。
- 原生走 `expo-speech`（系统 TTS），H5 走 Web Speech，**均无需申请任何权限**；
- 仅前台轮询（App 打开时），退到后台/锁屏不播报；后台推送需另接 FCM/APNs，作为后续迭代。

### 5. 首次访问白屏 / 图标缺失排查
- F12 → Network，看 `AppEntry-*.js` 是否 200；
- 若图标/字体 404：确认 `html/` 下的 `assets/`、`_expo/` 目录已完整上传；
- 若接口报 404/502：检查 nginx 的 `/api/` 反代是否生效、后端是否在运行。

## 四、重新构建 H5（后端地址或代码变化时）

H5 用相对 `/api`，正常情况后端域名变化无需改 H5。只有改了代码才需要重新导出：

```bash
cd payment-merchant-app

# 1) 使用 Node 20+（Windows 便携版示例）
export PATH="/d/project/.node-portable/node-v20.20.2-win-x64:$PATH"

# 2) 临时把 .env 改为相对 /api（导出后必须改回，避免影响原生 App）
cp .env .env.bak
echo "EXPO_PUBLIC_API_URL=/api" > .env

# 3) 导出（--clear 强制刷新 Metro 缓存，避免旧地址残留）
npx expo export --platform web --clear

# 4) 恢复 .env（原生 App 仍用绝对地址）
cp .env.bak .env && rm .env.bak

# 5) 后处理：assets/node_modules -> assets/vendor（见下方说明，必须执行）
node scripts/postbuild-h5.js

# 产物在 dist/ 目录，重新打包即可
```

> `app.json` 中的 `experiments.baseUrl: "./"` 只影响 Web 静态导出的资源路径（使其相对化），
> 不影响原生 App（APK/iOS）的打包与资源解析。
>
> ⚠ 第 5 步必须执行：Expo 会把 Web 资源放进 `dist/assets/node_modules/`，而 `node_modules`
> 是上传工具 / .gitignore / rsync / CI 普遍默认排除的目录，会导致部署后图标、返回箭头 404。
> `postbuild-h5.js` 会把该目录改名为 `assets/vendor/` 并同步改写 bundle 引用，从根上规避。
