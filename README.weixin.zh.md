# Weixin SDK

[总览](README.zh.md) | [WeCom](README.wecom.zh.md) | Weixin | [English](README.weixin.md)

基于 `@tencent-weixin/openclaw-weixin` 底层协议实现的 Java SDK。

包名：`io.github.cloudsen.ai.weixin`

这里封装的是实际的 HTTP、二维码登录、长轮询、CDN 协议，不是 `openclaw-weixin-cli` 安装器。

## 特性

| 功能 | 说明 |
|------|------|
| 二维码登录 | 发起扫码登录并轮询确认状态 |
| 长轮询收消息 | 通过 `getUpdates` 拉取消息 |
| 消息发送 | 支持文本、图片、文件、视频发送 |
| 输入态接口 | 封装 `getConfig` 和 `sendTyping` |
| CDN 上传下载 | 支持上传地址协商、CDN 上传与下载 |
| 媒体加解密 | 内置 AES-128-ECB 媒体加解密 |

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 二维码登录

```java
import io.github.cloudsen.ai.weixin.WeixinClient;
import io.github.cloudsen.ai.weixin.WeixinClientOptions;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStartResult;
import io.github.cloudsen.ai.weixin.WeixinQrLoginStatusResult;

import java.time.Duration;

String token;
try (WeixinClient loginClient = new WeixinClient(WeixinClientOptions.builder().build())) {
    WeixinQrLoginStartResult qr = loginClient.startQrLogin();
    System.out.println(qr.qrcodeUrl());

    WeixinQrLoginStatusResult login = loginClient.waitForQrLogin(qr.qrcode(), Duration.ofMinutes(5));
    token = login.botToken();
}
```

### 收发消息

```java
import io.github.cloudsen.ai.weixin.WeixinClient;
import io.github.cloudsen.ai.weixin.WeixinClientOptions;
import io.github.cloudsen.ai.weixin.WeixinGetUpdatesResponse;

try (WeixinClient client = new WeixinClient(
        WeixinClientOptions.builder()
                .token(token)
                .build())) {
    WeixinGetUpdatesResponse updates = client.getUpdates("");
    client.sendTextMessage("user@im.wechat", "你好", "context-token");
}
```

## 常用 API

### 登录相关

| 方法 | 说明 |
|------|------|
| `startQrLogin()` | 获取二维码和二维码链接 |
| `pollQrLoginStatus(qrcode)` | 轮询当前扫码登录状态 |
| `waitForQrLogin(qrcode, timeout)` | 等待扫码确认或二维码过期 |

### 消息相关

| 方法 | 说明 |
|------|------|
| `getUpdates(getUpdatesBuf)` | 长轮询拉取消息 |
| `sendMessage(message)` | 发送原始 `WeixinMessage` |
| `sendTextMessage(toUserId, text, contextToken)` | 发送文本 |
| `sendImageMessage(toUserId, uploadResult, contextToken)` | 发送图片 |
| `sendFileMessage(toUserId, fileName, uploadResult, contextToken)` | 发送文件 |
| `sendVideoMessage(toUserId, uploadResult, contextToken)` | 发送视频 |

### 媒体相关

| 方法 | 说明 |
|------|------|
| `getUploadUrl(request)` | 获取上传元数据 |
| `uploadImage(filePath, toUserId)` | 上传图片并返回下载信息 |
| `uploadVideo(filePath, toUserId)` | 上传视频 |
| `uploadFile(filePath, toUserId)` | 上传通用文件 |
| `uploadMedia(filePath, toUserId, mediaType)` | 通用上传入口 |
| `downloadMedia(item)` | 从入站消息项下载媒体 |
| `downloadCdnMedia(encryptedQueryParam, aesKeyBase64)` | 直接下载 CDN 内容 |
| `buildCdnDownloadUrl(encryptedQueryParam)` | 构造 CDN 下载地址 |

### 输入态与配置

| 方法 | 说明 |
|------|------|
| `getConfig(ilinkUserId, contextToken)` | 获取 typing ticket 等配置 |
| `sendTyping(ilinkUserId, typingTicket, status)` | 发送输入态 |

## 常用配置

`WeixinClientOptions.builder()` 常用字段如下：

| 配置项 | 说明 |
|------|------|
| `baseUrl(...)` | API 地址，默认 `https://ilinkai.weixin.qq.com` |
| `cdnBaseUrl(...)` | CDN 地址 |
| `token(...)` | 扫码登录后返回的 `bot_token` |
| `routeTag(...)` | 可选的 `SKRouteTag` 请求头 |
| `botType(...)` | Bot 类型，默认 `3` |
| `channelVersion(...)` | 发送到 `base_info.channel_version` |
| `apiTimeoutMillis(...)` | 普通 API 超时时间 |
| `longPollTimeoutMillis(...)` | `getUpdates` 超时时间 |
| `configTimeoutMillis(...)` | `getConfig` 和 `sendTyping` 超时时间 |
| `qrPollTimeoutMillis(...)` | 二维码轮询超时时间 |

## 集成测试

真实微信连通测试位于 `WeixinClientIntegrationTest`。

- 默认跳过
- 从二维码登录开始
- 启动后会在控制台输出二维码链接
- 登录成功后进入真实消息长轮询并自动回复
- 收到 `结束测试`、`end-test`、`endtest` 时退出
- 收到 `输入中` 或 `typing` 时，会额外走一遍 `getConfig + sendTyping`

### 执行方式

```bash
mvn -Dtest=WeixinClientIntegrationTest \
  -Dwx.weixin.it.enabled=true \
  test
```

### 可选参数

| 参数 | 说明 |
|------|------|
| `-Dwx.weixin.it.baseUrl=https://ilinkai.weixin.qq.com` | API 地址 |
| `-Dwx.weixin.it.routeTag=...` | 可选路由标记 |
| `-Dwx.weixin.it.botType=3` | Bot 类型 |
| `-Dwx.weixin.it.loginTimeoutSeconds=300` | 扫码登录超时时间 |
| `-Dwx.weixin.it.waitSeconds=600` | 整体等待超时时间 |
| `-Dwx.weixin.it.requestTimeoutSeconds=15` | 请求超时时间 |

### 环境变量

| 变量名 | 说明 |
|------|------|
| `WEIXIN_IT_BASE_URL` | API 地址 |
| `WEIXIN_IT_ROUTE_TAG` | 可选路由标记 |
| `WEIXIN_IT_BOT_TYPE` | Bot 类型 |
| `WEIXIN_IT_LOGIN_TIMEOUT_SECONDS` | 扫码登录超时时间 |
| `WEIXIN_IT_WAIT_SECONDS` | 整体等待超时时间 |
| `WEIXIN_IT_REQUEST_TIMEOUT_SECONDS` | 请求超时时间 |

## 说明

- 这个 SDK 对应的是 `@tencent-weixin/openclaw-weixin` 的底层协议，不是 CLI 安装器。
- 登录后的接口调用需要有效的 `bot_token`。
- 媒体上传前会做 AES-128-ECB 加密。
- 入站消息媒体可通过 `downloadMedia(...)` 或 `WeixinCdnCrypto` 解密下载。
- 更完整的协议分析见 [docs/openclaw-weixin-protocol.md](docs/openclaw-weixin-protocol.md)。

## License

基于 Apache License 2.0 分发。更多信息请查看 [LICENSE](LICENSE)。
