# WeCom SDK

[总览](README.zh.md) | WeCom | [Weixin](README.weixin.zh.md) | [English](README.wecom.md)

基于 WebSocket 长连接协议实现的企业微信 AI Bot Java SDK。

包名：`io.github.cloudsen.ai.wecom`

## 特性

| 功能 | 说明 |
|------|------|
| 自动连接管理 | 自动建立 `wss://openws.work.weixin.qq.com` 长连接并发送 `aibot_subscribe` 鉴权 |
| 心跳保活 | 内置应用层心跳，支持断线重连、鉴权失败重试 |
| 消息回调分发 | 支持文本、图片、文件、语音、视频、图文混排等消息类型 |
| 流式回复 | 同一 `req_id` 的回复串行发送，避免流式回包乱序 |
| 主动推送 | 支持 Markdown、模板卡片、媒体消息主动发送 |
| 文件处理 | 支持临时素材三步上传、文件下载与 AES-256-CBC 解密 |

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 代码示例

```java
import io.github.cloudsen.ai.wecom.WeComAiBotClient;
import io.github.cloudsen.ai.wecom.WeComAiBotClientOptions;

WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .build();

WeComAiBotClient client = new WeComAiBotClient(options);
client.onAuthenticated(() -> System.out.println("authenticated"));
client.onTextMessage(frame -> {
    String content = frame.getBody().getText() == null ? "" : frame.getBody().getText().getContent();
    client.replySimpleText(frame, "收到：" + content);
});
client.onEnterChat(frame -> client.replyWelcomeText(frame, "你好，我是 Java SDK 接入的机器人"));

client.connect();
```

## 常用 API

### 连接管理

| 方法 | 说明 |
|------|------|
| `connect()` | 建立 WebSocket 连接，并发送 `aibot_subscribe` 鉴权 |
| `disconnect()` | 主动断开当前连接，不销毁客户端实例 |
| `isConnected()` | 返回当前是否已完成连接且鉴权成功 |

### 事件监听

| 方法 | 说明 |
|------|------|
| `addListener(listener)` | 注册完整的 `WeComAiBotListener`，适合统一管理多种回调 |
| `removeListener(listener)` | 移除已注册的 listener |
| `onConnected(callback)` | 连接建立回调 |
| `onAuthenticated(callback)` | 鉴权成功回调 |
| `onDisconnected(callback)` | 断连回调 |
| `onReconnecting(callback)` | 重连回调，可拿到重试次数 |
| `onError(callback)` | 异常回调 |
| `onMessage(callback)` | 所有消息回调 |
| `onTextMessage(callback)` | 文本消息回调 |
| `onImageMessage(callback)` | 图片消息回调 |
| `onMixedMessage(callback)` | 图文混排消息回调 |
| `onVoiceMessage(callback)` | 语音消息回调 |
| `onFileMessage(callback)` | 文件消息回调 |
| `onVideoMessage(callback)` | 视频消息回调 |
| `onEvent(callback)` | 所有事件回调 |
| `onEnterChat(callback)` | 进入会话事件回调 |
| `onTemplateCardEvent(callback)` | 模板卡片点击事件回调 |
| `onFeedbackEvent(callback)` | 用户反馈事件回调 |
| `onServerDisconnectedEvent(callback)` | 服务端断连事件回调 |

### 回复相关

| 方法 | 说明 |
|------|------|
| `reply(frame, body)` | 通用回包方法，适合原始协议扩展字段 |
| `replySimpleText(frame, content)` | 普通文本回复 |
| `replyMarkdown(frame, content)` | Markdown 回复 |
| `replyStream(frame, streamId, content, finish)` | 流式回复 |
| `replyStreamWithCard(...)` | 带模板卡片的流式回复 |
| `replyWelcome(frame, body)` | 通用欢迎语回复 |
| `replyWelcomeText(frame, content)` | 文本欢迎语 |
| `replyWelcomeTemplateCard(frame, templateCard)` | 欢迎模板卡片 |
| `replyTemplateCard(frame, templateCard, feedback)` | 模板卡片回复 |
| `updateTemplateCard(frame, templateCard, userIds)` | 在 `template_card_event` 中更新已有卡片 |

### 主动发送

| 方法 | 说明 |
|------|------|
| `sendMessage(chatId, body)` | 主动发送通用消息体 |
| `sendMarkdownMessage(chatId, content)` | 主动发送 Markdown |
| `sendTemplateCardMessage(chatId, templateCard)` | 主动发送模板卡片 |
| `sendMediaMessage(chatId, mediaType, mediaId)` | 主动发送媒体消息 |

### 媒体与文件

| 方法 | 说明 |
|------|------|
| `uploadMedia(fileBytes, options)` | 上传临时素材，返回 `media_id` |
| `downloadFile(url, aesKey)` | 下载文件；传入 `aesKey` 时自动解密 |

## 心跳与重连

- 只有在 `aibot_subscribe` 鉴权成功后，SDK 才会启动心跳。
- 默认心跳间隔是 `30` 秒。
- 连续 `2` 次心跳失败，SDK 会主动关闭当前 WebSocket，并视为连接失活。
- 自动重连默认采用指数退避，最大退避时间 `30` 秒。
- 如果服务端发出 `disconnected_event`，SDK 会停止自动重连，等待业务侧重新连接。

推荐配置：

```java
WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .requestTimeoutMillis(10_000)
        .reconnectBaseDelayMillis(1_000)
        .maxReconnectAttempts(10)
        .build();
```

## 集成测试

真实企业微信连通测试位于 `WeComAiBotClientIntegrationTest`。

- 默认跳过，不影响普通 `mvn test`
- 启用后会真实连接企业微信 WebSocket 服务
- 会根据收到的用户消息选择不同回复形式
- 只有收到文本指令 `结束测试`，测试才会结束

### 文本触发指令

| 指令 | 调用方法 |
|------|------|
| `原始回复` | `reply(frame, body)` |
| `简单文本` | `replySimpleText(...)` |
| `markdown 回复` | `replyMarkdown(...)` |
| `流式回复` | `replyStream(...)` |
| `流式图文` | `replyStream(..., msgItems, feedback)` |
| `流式卡片` | `replyStreamWithCard(...)` |
| `卡片回复` | `replyTemplateCard(...)` |
| `主动原始` | `sendMessage(...)` |
| `主动 markdown` | `sendMarkdownMessage(...)` |
| `主动卡片` | `sendTemplateCardMessage(...)` |
| `回复图片` | `uploadMedia(...) + replyMedia(...)` |
| `主动图片` | `uploadMedia(...) + sendMediaMessage(...)` |
| `回复文件` / `回复语音` / `回复视频` | `uploadMedia(...) + replyMedia(...)` |
| `主动文件` / `主动语音` / `主动视频` | `uploadMedia(...) + sendMediaMessage(...)` |
| `结束测试` | 回复确认消息，并结束测试 |

### 执行方式

```bash
mvn -Dtest=WeComAiBotClientIntegrationTest \
  -Dwx.aibot.it.enabled=true \
  -Dwx.aibot.botId=your-bot-id \
  -Dwx.aibot.secret=your-secret \
  test
```

### 可选参数

| 参数 | 说明 |
|------|------|
| `-Dwx.aibot.scene=...` | 场景标识 |
| `-Dwx.aibot.plugVersion=...` | 插件版本 |
| `-Dwx.aibot.wsUrl=...` | WebSocket 地址 |
| `-Dwx.aibot.waitSeconds=300` | 等待超时时间 |
| `-Dwx.aibot.requestTimeoutSeconds=15` | 请求超时时间 |
| `-Dwx.aibot.welcomeMode=card|text|raw` | 欢迎语模式 |
| `-Dwx.aibot.downloadDir=/tmp/wx-aibot-it-downloads` | 下载目录 |
| `-Dwx.aibot.imagePath=/abs/path/demo.png` | 测试图片路径 |
| `-Dwx.aibot.filePath=/abs/path/demo.pdf` | 测试文件路径 |
| `-Dwx.aibot.voicePath=/abs/path/demo.amr` | 测试语音路径 |
| `-Dwx.aibot.videoPath=/abs/path/demo.mp4` | 测试视频路径 |

### 环境变量

| 变量名 | 说明 |
|------|------|
| `WECOM_AIBOT_BOT_ID` | Bot ID |
| `WECOM_AIBOT_SECRET` | Secret |
| `WECOM_AIBOT_SCENE` | 场景标识 |
| `WECOM_AIBOT_PLUG_VERSION` | 插件版本 |
| `WECOM_AIBOT_WS_URL` | WebSocket 地址 |
| `WECOM_AIBOT_WAIT_SECONDS` | 等待超时时间 |
| `WECOM_AIBOT_REQUEST_TIMEOUT_SECONDS` | 请求超时时间 |
| `WECOM_AIBOT_WELCOME_MODE` | 欢迎语模式 |
| `WECOM_AIBOT_DOWNLOAD_DIR` | 下载目录 |
| `WECOM_AIBOT_IMAGE_PATH` | 测试图片路径 |
| `WECOM_AIBOT_FILE_PATH` | 测试文件路径 |
| `WECOM_AIBOT_VOICE_PATH` | 测试语音路径 |
| `WECOM_AIBOT_VIDEO_PATH` | 测试视频路径 |

## 说明

- `sendMessage` 和 `reply` 支持直接传 `Map<String, Object>`，便于兼容协议新增字段。
- `replySimpleText` 是语义封装，底层实际发送的是 `stream + finish=true`。
- `replyStreamWithCard` 和 `msg_item` 虽然已封装，但当前微信客户端通常不稳定渲染。
- 如果要稳定展示，优先使用 `replyStream(...)`、`replyMarkdown(...)`、`replyTemplateCard(...)` 分开发送。

## 相关项目

- [企业微信 AI Bot 官方文档](https://developer.work.weixin.qq.com/document/path/101463)

## License

基于 Apache License 2.0 分发。更多信息请查看 [LICENSE](LICENSE)。
