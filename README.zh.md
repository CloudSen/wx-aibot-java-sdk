# wx-aibot-java-sdk

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![Maven Central](https://img.shields.io/badge/Maven-1.0.0-orange.svg)](https://central.sonatype.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

[English](README.md) | 中文

基于 WebSocket 长连接协议实现的企业微信 AI Bot Java SDK，提供连接管理、消息收发、流式回复、媒体文件处理等完整能力。

## ✨ 特性

| 功能 | 说明 |
|------|------|
| 🔄 **自动连接管理** | 自动建立 `wss://openws.work.weixin.qq.com` 长连接并发送鉴权 |
| 💓 **心跳保活** | 内置应用层心跳，支持断线重连、鉴权失败重试 |
| 📬 **消息回调分发** | 支持文本、图片、文件、语音、视频、图文混排等消息类型 |
| 🎯 **流式回复** | 同一 `req_id` 的回复串行发送，避免流式回包乱序 |
| 📤 **主动推送** | 支持 Markdown、模板卡片、媒体消息主动发送 |
| 📁 **文件处理** | 临时素材三步上传（init -> chunk -> finish）、文件下载与 AES-256-CBC 解密 |

## 📑 目录

- [快速开始](#-快速开始)
- [常用 API](#-常用-api)
- [心跳与重连](#-心跳与重连)
- [集成测试](#-集成测试)
- [说明](#-说明)
- [相关项目](#-相关项目)
- [License](#-license)

## 🚀 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.github.clouds3n.ai</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 代码示例

```java
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

> 💡 **提示**: 如果只关心单个事件，推荐使用 `client.onTextMessage(...)`、`client.onEnterChat(...)` 这类便捷注册方法。底层仍然是 `WeComAiBotListener`，返回值就是实际注册进去的 listener，可配合 `removeListener(listener)` 移除。如需统一管理多个回调，继续使用 `addListener(new WeComAiBotListener() { ... })` 即可。

## 📚 常用 API

### 连接管理

| 方法 | 说明 |
|------|------|
| `connect()` | 建立 WebSocket 连接，并发送 `aibot_subscribe` 鉴权 |
| `disconnect()` | 主动断开当前连接，不销毁客户端实例，后续可再次 `connect()` |
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

### 普通消息回复

| 方法 | 说明 |
|------|------|
| `reply(frame, body)` | 通用回包方法，直接发送原始协议体，适合协议扩展字段 |
| `replySimpleText(frame, content)` | 语义化的“简单文本回复”；底层会转成 `stream + finish=true` |
| `replyMarkdown(frame, content)` | 回复一条 Markdown 消息 |
| `replyMarkdown(frame, content, feedback)` | 回复 Markdown，并携带反馈标识 |

### 流式回复

| 方法 | 说明 |
|------|------|
| `replyStream(frame, streamId, content, finish)` | 发送或刷新流式消息 |
| `replyStream(frame, streamId, content, finish, feedback)` | 发送流式消息，并在首次回包时附带反馈标识 |
| `replyStream(frame, streamId, content, finish, msgItems, feedback)` | 结束流式消息时附带 `msg_item` 图文混排内容 |
| `replyStreamWithCard(frame, streamId, content, finish, templateCard)` | 回复流式消息并附带模板卡片 |
| `replyStreamWithCard(frame, streamId, content, finish, templateCard, msgItems, streamFeedback, cardFeedback)` | 流式回复的完整形态，支持图文混排和反馈字段 |

> ⚠️ **当前实践结论**
> - `msg_item` 图文混排和 `stream_with_template_card` 在 SDK 侧已封装，但当前企业微信客户端中通常不会按预期渲染
> - 如果需要用户侧稳定可见，优先使用 `replyStream(...)`、`replyMarkdown(...)`、`replyTemplateCard(...)` 分开发送

### 欢迎语回复

| 方法 | 说明 |
|------|------|
| `replyWelcome(frame, body)` | 欢迎语通用回包方法，需在 `enter_chat` 事件后 5 秒内调用 |
| `replyWelcomeText(frame, content)` | 回复文本欢迎语 |
| `replyWelcomeTemplateCard(frame, templateCard)` | 回复欢迎模板卡片 |
| `replyWelcomeTemplateCard(frame, templateCard, feedback)` | 回复欢迎模板卡片，并附带反馈标识 |

### 模板卡片

| 方法 | 说明 |
|------|------|
| `replyTemplateCard(frame, templateCard, feedback)` | 对消息回调直接回复模板卡片 |
| `updateTemplateCard(frame, templateCard, userIds)` | 响应 `template_card_event` 更新已有卡片，需在事件后 5 秒内调用 |

### 主动推送

| 方法 | 说明 |
|------|------|
| `sendMessage(chatId, body)` | 主动发送通用消息体 |
| `sendMessage(chatId, chatType, body)` | 主动发送通用消息体，并显式指定单聊/群聊 |
| `sendMarkdownMessage(chatId, content)` | 主动发送 Markdown |
| `sendMarkdownMessage(chatId, chatType, content, feedback)` | 主动发送 Markdown，并附带 chatType、feedback |
| `sendTemplateCardMessage(chatId, templateCard)` | 主动发送模板卡片 |
| `sendTemplateCardMessage(chatId, chatType, templateCard, feedback)` | 主动发送模板卡片，并附带 chatType、feedback |

### 媒体消息

| 方法 | 说明 |
|------|------|
| `replyMedia(frame, mediaType, mediaId)` | 对回调消息回复媒体消息，支持 `file/image/voice/video` |
| `replyMedia(frame, mediaType, mediaId, title, description)` | 回复视频时可补充标题和描述 |
| `sendMediaMessage(chatId, mediaType, mediaId)` | 主动发送媒体消息 |
| `sendMediaMessage(chatId, chatType, mediaType, mediaId, title, description)` | 主动发送媒体消息，并支持群聊类型和视频扩展字段 |

### 文件处理

| 方法 | 说明 |
|------|------|
| `uploadMedia(fileBytes, options)` | 走 `init -> chunk -> finish` 上传临时素材，返回 `media_id` |
| `downloadFile(url, aesKey)` | 下载文件；若传入 `aesKey`，会自动做 AES-256-CBC 解密 |

## 💓 心跳与重连

SDK 内置了应用层心跳，不需要业务侧自己发 `ping`。

### 心跳触发时机

- 只有在 `aibot_subscribe` 鉴权成功后，SDK 才会启动心跳
- 连接断开、手动 `disconnect()`、`close()` 时，心跳任务会立即停止

### 心跳发送方式

- 心跳命令固定为 `ping`
- 默认每 `30` 秒发送一次，可通过 `heartbeatIntervalMillis(...)` 调整
- 每次心跳都会生成独立 `req_id`，并等待服务端 ack

### 失活判定

- 心跳 ack 和普通请求共用同一套超时机制，默认超时时间是 `10` 秒，可通过 `requestTimeoutMillis(...)` 调整
- 连续 `2` 次心跳未收到 ack，SDK 会主动关闭当前 WebSocket，并视为连接失活
- 关闭连接后，未完成的回复、发送、上传等 pending 请求会统一失败，避免一直挂起

### 自动重连

- 非手动断开时，SDK 会自动重连
- 默认采用指数退避：基准延迟 `1` 秒，逐步放大，最大不超过 `30` 秒
- 普通断线默认最多重连 `10` 次，可通过 `maxReconnectAttempts(...)` 调整
- 鉴权失败默认最多重试 `5` 次，可通过 `maxAuthFailureAttempts(...)` 调整
- 如果收到服务端的 `disconnected_event`，SDK 会停止自动重连，等待业务侧重新发起连接

### 推荐配置

```java
WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .requestTimeoutMillis(10_000)
        .reconnectBaseDelayMillis(1_000)
        .maxReconnectAttempts(10)
        .build();
```

> 💡 **提示**: 如果网络环境不稳定，可以把心跳间隔调到 `10~15` 秒，同时适当缩短 `requestTimeoutMillis`，更快发现死连接。

## 🧪 集成测试

模块内提供了真实企业微信连通测试：

- **测试类**: `WeComAiBotClientIntegrationTest`
- **默认跳过**: 不影响普通 `mvn test`
- **启用后会真实连接企业微信**: Connects to Enterprise WeChat when enabled
- **收到消息后会根据用户消息内容选择不同回复形式**: Replies based on user message content
- **只有收到文本指令 `结束测试`，测试才会结束**: Ends when receiving `结束测试` text command

### 文本触发指令

| 指令 | 调用方法 |
|------|---------|
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

### 事件与非文本消息

| 事件/消息类型 | 处理方式 |
|--------------|---------|
| `enter_chat` | 默认调用 `replyWelcomeTemplateCard(...)` |
| `template_card_event` | 优先调用 `updateTemplateCard(...)` |
| 图片 / 文件 / 视频消息 | 调用 `downloadFile(...)` 后回一条确认消息 |
| 语音消息 | 回 `已收到：语音消息` |
| 图文混排消息 | 回一条 Markdown 确认消息 |

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

也支持通过环境变量配置：

| 变量名 | 说明 |
|--------|------|
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

### 说明

- `welcomeMode=card`：调用 `replyWelcomeTemplateCard(...)`
- `welcomeMode=text`：调用 `replyWelcomeText(...)`
- `welcomeMode=raw`：调用 `replyWelcome(frame, body)`
- `回复图片` / `主动图片` 未配置 `imagePath` 时，会自动使用内置测试图片
- 文件、语音、视频相关指令只有在对应本地文件路径已配置时才会执行，否则会明确提示已跳过

## 📄 说明

- `sendMessage` / `reply` 支持直接传 `Map<String, Object>`，便于兼容官方协议新增字段
- `replySimpleText` 是对普通消息“文本回复”的语义封装，底层实际发送的是 `stream + finish=true`
- `replyStreamWithCard` 和 `msg_item` 已按协议封装，但当前企业微信客户端中通常不渲染；即使服务端 ack 成功，微信侧也可能不展示
- 涉及流式图文或流式卡片的场景，建议优先回退为 `replyStream(...)`、`replyMarkdown(...)`、`replyTemplateCard(...)` 分开发送
- 当前版本优先覆盖长连接核心协议和常用便捷方法，模板卡片复杂结构建议直接按官方 JSON 组装为 `Map` 传入

## 🔗 相关项目

- [企业微信 AI Bot 官方文档](https://developer.work.weixin.qq.com/document/path/101463)

## 📝 License

基于 Apache License 2.0 分发。更多信息请查看 [LICENSE](LICENSE)。
