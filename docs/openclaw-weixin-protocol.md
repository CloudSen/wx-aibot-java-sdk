# Weixin 协议分析

结论先说：

- `@tencent-weixin/openclaw-weixin-cli` 只是安装器，不包含微信收发协议。
- 真正的协议实现位于 `@tencent-weixin/openclaw-weixin`。
- 截至 `2026-03-24`，该插件最新版本为 `2.0.1`。

## 协议来源

- npm 包页: https://www.npmjs.com/package/@tencent-weixin/openclaw-weixin
- registry 元数据: https://registry.npmjs.org/@tencent-weixin/openclaw-weixin
- tarball: https://registry.npmjs.org/@tencent-weixin/openclaw-weixin/-/openclaw-weixin-2.0.1.tgz

## 连接模型

插件不是 WebSocket，也不是官方公开微信客户端协议直连。

它的模型是：

1. 先通过二维码接口拿登录二维码
2. 用户扫码后拿到 `bot_token`
3. 后续所有消息收发都走 HTTP JSON API
4. 媒体文件走独立 CDN，文件内容使用 `AES-128-ECB` 加密

## 登录流程

### 1. 获取二维码

- `GET /ilink/bot/get_bot_qrcode?bot_type=3`

响应里核心字段：

- `qrcode`
- `qrcode_img_content`

### 2. 轮询二维码状态

- `GET /ilink/bot/get_qrcode_status?qrcode=<qrcode>`

响应状态：

- `wait`
- `scaned`
- `confirmed`
- `expired`

确认成功后返回：

- `bot_token`
- `ilink_bot_id`
- `baseurl`
- `ilink_user_id`

## 业务 API

所有业务接口均为 `POST` JSON，请求头关键字段：

- `AuthorizationType: ilink_bot_token`
- `Authorization: Bearer <token>`
- `X-WECHAT-UIN: <随机 uint32 十进制再 base64>`
- `Content-Type: application/json`
- `SKRouteTag` 可选

### 1. 长轮询拉消息

- `POST /ilink/bot/getupdates`

请求体核心字段：

- `get_updates_buf`
- `base_info.channel_version`

响应体核心字段：

- `ret`
- `errcode`
- `errmsg`
- `msgs`
- `get_updates_buf`
- `longpolling_timeout_ms`

### 2. 发消息

- `POST /ilink/bot/sendmessage`

核心消息结构：

- `msg.to_user_id`
- `msg.context_token`
- `msg.item_list`

消息项类型：

- `1` TEXT
- `2` IMAGE
- `3` VOICE
- `4` FILE
- `5` VIDEO

### 3. 获取上传参数

- `POST /ilink/bot/getuploadurl`

核心字段：

- `filekey`
- `media_type`
- `to_user_id`
- `rawsize`
- `rawfilemd5`
- `filesize`
- `aeskey`

### 4. 获取输入态配置

- `POST /ilink/bot/getconfig`

返回：

- `typing_ticket`

### 5. 发送输入态

- `POST /ilink/bot/sendtyping`

状态值：

- `1` 正在输入
- `2` 取消输入

## CDN 媒体协议

### 上传

1. 本地文件明文计算 `MD5`
2. 生成随机 16 字节 AES key
3. 用 `AES-128-ECB + PKCS#7` 计算密文
4. 调 `getuploadurl`
5. `POST <cdn>/upload?encrypted_query_param=...&filekey=...`
6. CDN 响应头 `x-encrypted-param` 作为后续下载参数

### 下载

- `GET <cdn>/download?encrypted_query_param=...`

下载后如果带 `aes_key`，需要做 `AES-128-ECB` 解密。

插件源码里 `aes_key` 存在两种格式：

- `base64(raw 16 bytes)`
- `base64(32位 hex 文本)`

Java SDK 里两种都做了兼容解析。

## 已落地的 Java SDK 映射

包路径：

- `io.github.cloudsen.ai.weixin`

核心类：

- `WeixinClient`
- `WeixinClientOptions`
- `WeixinMessage`
- `WeixinCdnCrypto`

核心方法：

- `startQrLogin()`
- `pollQrLoginStatus(...)`
- `waitForQrLogin(...)`
- `getUpdates(...)`
- `sendMessage(...)`
- `sendTextMessage(...)`
- `getUploadUrl(...)`
- `uploadImage(...)`
- `uploadVideo(...)`
- `uploadFile(...)`
- `sendImageMessage(...)`
- `sendVideoMessage(...)`
- `sendFileMessage(...)`
- `getConfig(...)`
- `sendTyping(...)`
- `downloadCdnMedia(...)`
- `downloadMedia(...)`
