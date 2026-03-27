# Weixin SDK

[Overview](README.md) | [WeCom](README.wecom.md) | Weixin | [中文](README.weixin.zh.md)

Java SDK for the underlying protocol exposed by `@tencent-weixin/openclaw-weixin`.

Package: `io.github.cloudsen.ai.weixin`

This wraps the real HTTP, QR login, long-polling, and CDN protocol. It does not wrap the `openclaw-weixin-cli` installer itself.

## Features

| Feature | Description |
|------|------|
| QR login | Starts login with QR code and polls until confirmed |
| Long polling | Receives messages through `getUpdates` |
| Message send | Supports text, image, file, and video sending |
| Typing API | Wraps `getConfig` and `sendTyping` |
| CDN upload and download | Supports upload URL negotiation, CDN upload, and CDN download |
| Media crypto | Built-in AES-128-ECB media encryption and decryption |

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### QR Login

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

### Send and Receive

```java
import io.github.cloudsen.ai.weixin.WeixinClient;
import io.github.cloudsen.ai.weixin.WeixinClientOptions;
import io.github.cloudsen.ai.weixin.WeixinGetUpdatesResponse;

try (WeixinClient client = new WeixinClient(
        WeixinClientOptions.builder()
                .token(token)
                .build())) {
    WeixinGetUpdatesResponse updates = client.getUpdates("");
    client.sendTextMessage("user@im.wechat", "hello", "context-token");
}
```

## Common APIs

### Login

| Method | Description |
|------|------|
| `startQrLogin()` | Requests QR code and QR URL |
| `pollQrLoginStatus(qrcode)` | Polls current QR login status |
| `waitForQrLogin(qrcode, timeout)` | Waits until confirmed or expired |

### Messaging

| Method | Description |
|------|------|
| `getUpdates(getUpdatesBuf)` | Long-polls inbound messages |
| `sendMessage(message)` | Sends raw `WeixinMessage` |
| `sendTextMessage(toUserId, text, contextToken)` | Sends text |
| `sendImageMessage(toUserId, uploadResult, contextToken)` | Sends image |
| `sendFileMessage(toUserId, fileName, uploadResult, contextToken)` | Sends file |
| `sendVideoMessage(toUserId, uploadResult, contextToken)` | Sends video |

### Media

| Method | Description |
|------|------|
| `getUploadUrl(request)` | Requests upload metadata |
| `uploadImage(filePath, toUserId)` | Uploads image and returns download metadata |
| `uploadVideo(filePath, toUserId)` | Uploads video |
| `uploadFile(filePath, toUserId)` | Uploads generic file |
| `uploadMedia(filePath, toUserId, mediaType)` | Generic upload entry |
| `downloadMedia(item)` | Downloads media from an inbound message item |
| `downloadCdnMedia(encryptedQueryParam, aesKeyBase64)` | Downloads CDN payload directly |
| `buildCdnDownloadUrl(encryptedQueryParam)` | Builds CDN download URL |

### Typing and Config

| Method | Description |
|------|------|
| `getConfig(ilinkUserId, contextToken)` | Fetches typing ticket and related config |
| `sendTyping(ilinkUserId, typingTicket, status)` | Sends typing state |

## Common Configuration

`WeixinClientOptions.builder()` supports these commonly used fields:

| Option | Description |
|------|------|
| `baseUrl(...)` | API base URL, default `https://ilinkai.weixin.qq.com` |
| `cdnBaseUrl(...)` | CDN base URL |
| `token(...)` | Bot token returned after QR login |
| `routeTag(...)` | Optional `SKRouteTag` header |
| `botType(...)` | Bot type, default `3` |
| `channelVersion(...)` | Sent in `base_info.channel_version` |
| `apiTimeoutMillis(...)` | Regular API timeout |
| `longPollTimeoutMillis(...)` | `getUpdates` timeout |
| `configTimeoutMillis(...)` | `getConfig` and `sendTyping` timeout |
| `qrPollTimeoutMillis(...)` | QR login poll timeout |

## Integration Test

Real Weixin connectivity test is available in `WeixinClientIntegrationTest`.

- Skipped by default
- Starts from QR login
- Prints QR code URL to the console
- Waits for inbound messages and replies in real protocol flow
- Ends when text command `结束测试`, `end-test`, or `endtest` is received
- Calls `getConfig + sendTyping` when text contains `输入中` or `typing`

### Run

```bash
mvn -Dtest=WeixinClientIntegrationTest \
  -Dwx.weixin.it.enabled=true \
  test
```

### Optional Parameters

| Parameter | Description |
|------|------|
| `-Dwx.weixin.it.baseUrl=https://ilinkai.weixin.qq.com` | API base URL |
| `-Dwx.weixin.it.routeTag=...` | Optional route tag |
| `-Dwx.weixin.it.botType=3` | Bot type |
| `-Dwx.weixin.it.loginTimeoutSeconds=300` | QR login timeout |
| `-Dwx.weixin.it.waitSeconds=600` | Overall test wait timeout |
| `-Dwx.weixin.it.requestTimeoutSeconds=15` | Request timeout |

### Environment Variables

| Variable | Description |
|------|------|
| `WEIXIN_IT_BASE_URL` | API base URL |
| `WEIXIN_IT_ROUTE_TAG` | Optional route tag |
| `WEIXIN_IT_BOT_TYPE` | Bot type |
| `WEIXIN_IT_LOGIN_TIMEOUT_SECONDS` | QR login timeout |
| `WEIXIN_IT_WAIT_SECONDS` | Overall test wait timeout |
| `WEIXIN_IT_REQUEST_TIMEOUT_SECONDS` | Request timeout |

## Notes

- This SDK targets the protocol used by `@tencent-weixin/openclaw-weixin`, not the CLI installer package.
- API calls after login need a valid `bot_token`.
- Media upload uses AES-128-ECB encryption before CDN upload.
- Inbound CDN download can be decrypted through `downloadMedia(...)` or `WeixinCdnCrypto`.
- See [docs/openclaw-weixin-protocol.md](docs/openclaw-weixin-protocol.md) for protocol analysis.

## License

Distributed under the Apache License 2.0. See [LICENSE](LICENSE) for more information.
