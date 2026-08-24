# 支付平台开放 API 接口文档

> 面向外部商户/第三方系统，提供**下单、查单、支付链接、支付回调**能力。
> 商户需先在平台开通「开放 API」并获取 `api_secret`。

---

## 1. 接入准备

| 项 | 说明 |
|---|---|
| 接口地址 | 平台开放 API 对外域名（后端 `app.open-api-base-url` 配置项），下文用 `{BASE}` 代替 |
| appId | 即商户号 `merchant_no`（如 `M202608221234`） |
| api_secret | 平台下发的签名密钥（管理端「重置密钥」获取，仅显示一次） |
| 传输方式 | 全部 HTTPS + `application/json`（POST body） |
| 签名算法 | **MD5**（详见第 2 节） |

商户开通方式：管理端 `商户管理 → 配置开放API`（设置回调地址、IP 白名单、启用开关），再「重置密钥」获取 `api_secret`。

---

## 2. 签名算法（MD5）

### 2.1 规则

1. 将请求 body 中除 `sign` 外的所有参数，按参数名 **ASCII 字典序升序**排列；
2. 拼接为 `k1=v1&k2=v2&...`（**空字符串值不参与拼接**）；
3. 在末尾追加 `&key=` + `api_secret`；
4. 对整串做 **MD5**，取 **大写** 十六进制字符串作为 `sign`。

> 注意：所有参数值均以字符串形式参与签名；`timestamp` 为毫秒时间戳字符串。

### 2.2 示例

假设：`amount=1.00`、`appId=M202608221234`、`nonce=abc123`、`payChannel=ALIPAY`、`timestamp=1755000000000`，`api_secret=testsecret123`。

待签名串（按 key 排序）：

```
amount=1.00&appId=M202608221234&nonce=abc123&payChannel=ALIPAY&timestamp=1755000000000&key=testsecret123
```

计算 `MD5(上述串)` 得：

```
sign = 6880A402994418EC0F633F511E26BC8C
```

### 2.3 公共参数

每个需验签的接口（下单、查单）body 中必须包含：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| appId | string | 是 | 商户号 |
| timestamp | string | 是 | 毫秒时间戳，与服务器时间差 ≤ 5 分钟 |
| nonce | string | 是 | 随机串，5 分钟内不可重复 |
| sign | string | 是 | 签名（MD5，大写） |

---

## 3. 接口列表

### 3.1 创建订单

`POST {BASE}/api/open/order/create`

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| appId | string | 是 | 商户号 |
| timestamp | string | 是 | 毫秒时间戳 |
| nonce | string | 是 | 随机串 |
| sign | string | 是 | 签名 |
| productName | string | 否 | 商品名称，默认「扫码支付」 |
| amount | string | 是 | 金额，单位元，如 `100.00` |
| merchantOrderNo | string | 否 | 上游商户订单号，用于对账/查单 |
| payChannel | string | 否 | `ALIPAY` / `WECHAT`，默认 `ALIPAY` |
| tradeType | string | 否 | 支付宝交易类型：`WAP`（手机网站支付，默认）/ `F2F`（当面付）；微信通道忽略 |
| notifyUrl | string | 否 | 异步回调地址，覆盖商户默认 |
| returnUrl | string | 否 | 支付完成后页面跳转地址 |
| remark | string | 否 | 备注 |

**请求示例：**

```json
{
  "appId": "M202608221234",
  "timestamp": "1755000000000",
  "nonce": "abc123",
  "sign": "6880A402994418EC0F633F511E26BC8C",
  "productName": "测试商品",
  "amount": "100.00",
  "merchantOrderNo": "MCH202608220001",
  "payChannel": "ALIPAY",
  "notifyUrl": "https://mch.example.com/notify",
  "returnUrl": "https://mch.example.com/result"
}
```

**当面付请求示例：** `payChannel=ALIPAY` 且 `tradeType=F2F`（`tradeType` 参与签名）：

```json
{
  "appId": "M202608221234",
  "timestamp": "1755000000000",
  "nonce": "abc124",
  "sign": "……（含 tradeType 重新计算）",
  "amount": "100.00",
  "payChannel": "ALIPAY",
  "tradeType": "F2F"
}
```

**响应（手机网站支付 `tradeType=WAP`，默认）：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "orderNo": "ORD2026082212345",
    "amount": "100.00",
    "payChannel": "ALIPAY",
    "tradeType": "WAP",
    "payUrl": "https://qrpay.csmmkj.cn/api/open/pay/ORD2026082212345"
  }
}
```

> `payUrl` 是**支付链接**：商户将其交给用户，用户点击（浏览器打开）即可直接拉起支付，无需再输入金额。

**响应（当面付 `tradeType=F2F`）：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "orderNo": "ORD2026082212346",
    "amount": "100.00",
    "payChannel": "ALIPAY",
    "tradeType": "F2F",
    "payUrl": "https://qr.alipay.com/bax03431xyz..."
  }
}
```

> `payUrl` 为支付宝当面付二维码字符串（`https://qr.alipay.com/...`）：商户将其渲染成二维码图片展示给顾客，顾客用支付宝 App 扫码支付。注意：当面付的 `payUrl` 是二维码内容，而非可直接跳转的支付页面链接。

> 说明：`tradeType` 为可选参数，若填写会参与签名（与其它业务字段一致，按 key 升序拼接）。

---

### 3.2 查询订单

`POST {BASE}/api/open/order/query`

**请求体：**（公共参数 + `orderNo` 或 `merchantOrderNo`，二者**二选一**）

```json
{
  "appId": "M202608221234",
  "timestamp": "1755000000000",
  "nonce": "xyz789",
  "sign": "……",
  "orderNo": "ORD2026082212345"
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "orderNo": "ORD2026082212345",
    "merchantOrderNo": "MCH202608220001",
    "orderStatus": 2,
    "orderAmount": "100.00",
    "payChannel": "ALIPAY",
    "channelTradeNo": "2026082211007000000000001234",
    "payTime": "2026-08-22 12:00:00"
  }
}
```

`orderStatus` 取值：`1-新建 2-已支付 3-已回调 4-已退款 5-已失效 6-支付失败`。

---

### 3.3 支付链接（浏览器直接打开，无需验签）

`GET {BASE}/api/open/pay/{orderNo}`

- 支付宝手机网站支付（WAP）订单：返回自动提交的收银台表单页面，自动跳转支付宝收银台；
- 微信订单：302 跳转到微信 H5 支付页；
- 支付宝当面付（F2F）订单：返回二维码内容页面（正常由下单接口直接返回 `payUrl`，此链接仅兜底）。

> 该链接由下单接口的 `payUrl` 直接给出（仅 WAP 订单），用户点击即付；F2F 订单的 `payUrl` 为二维码字符串，需渲染成二维码图片供扫码，不用于浏览器直接跳转。

---

### 3.4 支付完成跳转（浏览器回调，无需验签）

`GET {BASE}/api/open/redirect/{orderNo}`

支付成功后，支付宝/微信回跳到本链接，本链接再 **302 跳转**到下单时填写的 `returnUrl`；若未填写则跳转到平台默认结果页。

---

## 4. 异步支付回调（平台 → 商户）

支付成功后，平台会向 `notifyUrl`（单笔 `notifyUrl` 优先，否则商户默认）发起 `POST`，`Content-Type: application/json`：

```json
{
  "orderNo": "ORD2026082212345",
  "merchantOrderNo": "MCH202608220001",
  "merchantNo": "M202608221234",
  "orderStatus": 2,
  "amount": "100.00",
  "payChannel": "ALIPAY",
  "channelTradeNo": "2026082211007000000000001234",
  "payTime": "2026-08-22 12:00:00",
  "sign": "……"
}
```

- 回调 `sign` 的签名方式与请求一致：对除 `sign` 外字段按 key 升序拼接 `k=v&k=v...`，追加 `&key=` + `api_secret`，MD5 大写。
- 商户收到后需**验签**，并返回纯文本 **`SUCCESS`** 表示接收成功。
- 未返回 `SUCCESS` 时平台会重试，最多 3 次（间隔约 1 分钟）。

---

## 5. 错误码

| code | 说明 |
|---|---|
| 200 | 成功 |
| 401 | 验签失败 / 缺少验签参数 / 请求过期 / 重复请求 / 商户停用 |
| 403 | 商户未开通开放API / 未配置密钥 / IP 不在白名单 / 无权查询该订单 |
| 404 | 订单不存在 |
| 500 | 业务异常 / 系统异常 |

---

## 6. 示例代码

### 6.1 Java

```java
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class SignDemo {
    static String sign(TreeMap<String, String> params, String secret) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (var e : params.entrySet()) {
            if ("sign".equals(e.getKey()) || e.getValue() == null || e.getValue().isEmpty()) continue;
            sb.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        sb.append("&key=").append(secret);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte x : b) hex.append(String.format("%02X", x & 0xFF));
        return hex.toString();
    }

    public static void main(String[] a) throws Exception {
        TreeMap<String, String> p = new TreeMap<>();
        p.put("appId", "M202608221234");
        p.put("timestamp", "1755000000000");
        p.put("nonce", "abc123");
        p.put("amount", "1.00");
        p.put("payChannel", "ALIPAY");
        p.put("sign", sign(p, "testsecret123")); // 6880A402994418EC0F633F511E26BC8C
        System.out.println(p.get("sign"));
    }
}
```

### 6.2 PHP

```php
function sign(array $params, string $secret): string {
    unset($params['sign']);
    ksort($params);
    $parts = [];
    foreach ($params as $k => $v) {
        if ($v === '' || $v === null) continue;
        $parts[] = $k . '=' . $v;
    }
    $str = implode('&', $parts) . '&key=' . $secret;
    return strtoupper(md5($str));
}

$params = [
    'appId' => 'M202608221234',
    'timestamp' => '1755000000000',
    'nonce' => 'abc123',
    'amount' => '1.00',
    'payChannel' => 'ALIPAY',
];
$params['sign'] = sign($params, 'testsecret123'); // 6880A402994418EC0F633F511E26BC8C
```
