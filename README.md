# wx-aibot-java-sdk

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/badge/Maven-1.0.0-orange.svg)](https://central.sonatype.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

English | [中文](README.zh.md)

Enterprise WeChat AI Bot Java SDK based on WebSocket long-lived connection, providing connection management, message sending/receiving, streaming replies, media file handling, and more.

## ✨ Features

| Feature | Description |
|------|------|
| 🔄 **Automatic Connection Management** | Automatically connects to `wss://openws.work.weixin.qq.com` and sends authentication request |
| 💓 **Heartbeat Keepalive** | Built-in app-level heartbeat with reconnect and auth-failure retry |
| 📬 **Message Callback Dispatching** | Supports text, image, file, voice, video, and mixed-content messages |
| 🎯 **Streaming Reply** | Serializes replies with the same `req_id` to avoid out-of-order stream packets |
| 📤 **Active Push** | Supports active Markdown, template card, and media message sending |
| 📁 **File Handling** | 3-step temporary media upload (`init -> chunk -> finish`), file download, and AES-256-CBC decryption |

## 📑 Table of Contents

- [Quick Start](#-quick-start)
- [Common APIs](#-common-apis)
- [Heartbeat & Reconnect](#-heartbeat--reconnect)
- [Integration Test](#-integration-test)
- [Notes](#-notes)
- [Related](#-related)
- [License](#-license)

## 🚀 Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.clouds3n.ai</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Code Example

```java
WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .build();

WeComAiBotClient client = new WeComAiBotClient(options);
client.onAuthenticated(() -> System.out.println("authenticated"));
client.onTextMessage(frame -> {
    String content = frame.getBody().getText() == null ? "" : frame.getBody().getText().getContent();
    client.replySimpleText(frame, "Received: " + content);
});
client.onEnterChat(frame -> client.replyWelcomeText(frame, "Hello, I am a bot powered by this Java SDK"));

client.connect();
```

> 💡 **Tip**: If you only care about one event type, prefer convenience methods such as `client.onTextMessage(...)` and `client.onEnterChat(...)`. The underlying mechanism is still `WeComAiBotListener`, and the return value is the actual registered listener instance, so you can remove it by `removeListener(listener)`. If you want centralized callback management, continue using `addListener(new WeComAiBotListener() { ... })`.

## 📚 Common APIs

### Connection Management

| Method | Description |
|------|------|
| `connect()` | Establishes WebSocket connection and sends `aibot_subscribe` authentication |
| `disconnect()` | Disconnects current connection without destroying client instance; can `connect()` again later |
| `isConnected()` | Returns whether connection is established and authenticated |

### Event Listeners

| Method | Description |
|------|------|
| `addListener(listener)` | Registers full `WeComAiBotListener` for centralized callback management |
| `removeListener(listener)` | Removes a registered listener |
| `onConnected(callback)` | Connection established callback |
| `onAuthenticated(callback)` | Authentication success callback |
| `onDisconnected(callback)` | Disconnection callback |
| `onReconnecting(callback)` | Reconnect callback with retry count |
| `onError(callback)` | Error callback |
| `onMessage(callback)` | All message callback |
| `onTextMessage(callback)` | Text message callback |
| `onImageMessage(callback)` | Image message callback |
| `onMixedMessage(callback)` | Mixed-content message callback |
| `onVoiceMessage(callback)` | Voice message callback |
| `onFileMessage(callback)` | File message callback |
| `onVideoMessage(callback)` | Video message callback |
| `onEvent(callback)` | All event callback |
| `onEnterChat(callback)` | Enter-chat event callback |
| `onTemplateCardEvent(callback)` | Template-card click event callback |
| `onFeedbackEvent(callback)` | User feedback event callback |
| `onServerDisconnectedEvent(callback)` | Server disconnected event callback |

### Standard Message Reply

| Method | Description |
|------|------|
| `reply(frame, body)` | Generic reply API for raw protocol body and extended fields |
| `replySimpleText(frame, content)` | Semantic helper for simple text reply; internally sends `stream + finish=true` |
| `replyMarkdown(frame, content)` | Replies with a Markdown message |
| `replyMarkdown(frame, content, feedback)` | Replies Markdown with feedback flag |

### Streaming Reply

| Method | Description |
|------|------|
| `replyStream(frame, streamId, content, finish)` | Sends or refreshes streaming message |
| `replyStream(frame, streamId, content, finish, feedback)` | Sends streaming message with feedback in first packet |
| `replyStream(frame, streamId, content, finish, msgItems, feedback)` | Attaches `msg_item` mixed-content payload when finishing stream |
| `replyStreamWithCard(frame, streamId, content, finish, templateCard)` | Streaming reply with template card |
| `replyStreamWithCard(frame, streamId, content, finish, templateCard, msgItems, streamFeedback, cardFeedback)` | Full streaming form with mixed-content and feedback fields |

> ⚠️ **Current Practice Conclusion**
> - `msg_item` mixed-content and `stream_with_template_card` are wrapped in SDK, but currently are usually not rendered as expected in WeChat client
> - For stable user-visible output, prefer sending `replyStream(...)`, `replyMarkdown(...)`, and `replyTemplateCard(...)` separately

### Welcome Message Reply

| Method | Description |
|------|------|
| `replyWelcome(frame, body)` | Generic welcome reply; must be called within 5 seconds after `enter_chat` |
| `replyWelcomeText(frame, content)` | Replies with text welcome message |
| `replyWelcomeTemplateCard(frame, templateCard)` | Replies with welcome template card |
| `replyWelcomeTemplateCard(frame, templateCard, feedback)` | Welcome template card with feedback flag |

### Template Card

| Method | Description |
|------|------|
| `replyTemplateCard(frame, templateCard, feedback)` | Direct template card reply in message callback |
| `updateTemplateCard(frame, templateCard, userIds)` | Updates existing card on `template_card_event`; must be called within 5 seconds |

### Active Push

| Method | Description |
|------|------|
| `sendMessage(chatId, body)` | Active send with generic message body |
| `sendMessage(chatId, chatType, body)` | Active send with explicit one-to-one/group chat type |
| `sendMarkdownMessage(chatId, content)` | Active Markdown send |
| `sendMarkdownMessage(chatId, chatType, content, feedback)` | Active Markdown send with chatType and feedback |
| `sendTemplateCardMessage(chatId, templateCard)` | Active template card send |
| `sendTemplateCardMessage(chatId, chatType, templateCard, feedback)` | Active template card send with chatType and feedback |

### Media Message

| Method | Description |
|------|------|
| `replyMedia(frame, mediaType, mediaId)` | Replies media message for callback, supports `file/image/voice/video` |
| `replyMedia(frame, mediaType, mediaId, title, description)` | Adds title and description for video reply |
| `sendMediaMessage(chatId, mediaType, mediaId)` | Actively sends media message |
| `sendMediaMessage(chatId, chatType, mediaType, mediaId, title, description)` | Active media send with chat type and video extension fields |

### File Handling

| Method | Description |
|------|------|
| `uploadMedia(fileBytes, options)` | Uploads temporary media via `init -> chunk -> finish`, returns `media_id` |
| `downloadFile(url, aesKey)` | Downloads file; if `aesKey` provided, decrypts with AES-256-CBC automatically |

## 💓 Heartbeat & Reconnect

The SDK has built-in application-level heartbeat, so business code does not need to send `ping` manually.

### Heartbeat Timing

- Heartbeat starts only after `aibot_subscribe` authentication succeeds
- Heartbeat task stops immediately when connection is disconnected, manually `disconnect()` is called, or `close()` is called

### Heartbeat Method

- Heartbeat command is fixed as `ping`
- Default interval is every `30` seconds, configurable via `heartbeatIntervalMillis(...)`
- Each heartbeat has its own `req_id` and waits for server ack

### Liveness Detection

- Heartbeat ack and regular requests share the same timeout mechanism; default timeout is `10` seconds, configurable via `requestTimeoutMillis(...)`
- If `2` consecutive heartbeats do not receive ack, SDK closes the current WebSocket and treats the connection as dead
- After connection closes, pending reply/send/upload requests fail together to avoid hanging

### Auto Reconnect

- SDK auto reconnects unless disconnected manually
- Default strategy is exponential backoff: base delay `1` second, gradually increasing up to `30` seconds max
- Normal disconnect reconnect attempts default to `10`, configurable via `maxReconnectAttempts(...)`
- Auth failure retries default to `5`, configurable via `maxAuthFailureAttempts(...)`
- If server sends `disconnected_event`, SDK stops auto reconnect and waits for business code to reconnect

### Recommended Configuration

```java
WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .requestTimeoutMillis(10_000)
        .reconnectBaseDelayMillis(1_000)
        .maxReconnectAttempts(10)
        .build();
```

> 💡 **Tip**: In unstable networks, set heartbeat interval to `10~15` seconds and reduce `requestTimeoutMillis` appropriately to detect dead connections faster.

## 🧪 Integration Test

The module includes real Enterprise WeChat connectivity test:

- **Test Class**: `WeComAiBotClientIntegrationTest`
- **Skipped by Default**: Does not affect regular `mvn test`
- **Real Connection**: Connects to Enterprise WeChat when enabled
- **Dynamic Reply**: Chooses reply form based on incoming user text
- **Stop Condition**: Test ends only when text command `结束测试` is received

### Text Commands

| Command | API |
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
| `结束测试` | Sends confirmation reply and then exits test |

### Events & Non-text Messages

| Event/Message Type | Handling |
|--------------|---------|
| `enter_chat` | Calls `replyWelcomeTemplateCard(...)` by default |
| `template_card_event` | Calls `updateTemplateCard(...)` first |
| Image / File / Video message | Calls `downloadFile(...)` then replies confirmation text |
| Voice message | Replies `已收到：语音消息` |
| Mixed-content message | Replies a Markdown confirmation message |

### Execution

```bash
mvn -Dtest=WeComAiBotClientIntegrationTest \
  -Dwx.aibot.it.enabled=true \
  -Dwx.aibot.botId=your-bot-id \
  -Dwx.aibot.secret=your-secret \
  test
```

### Optional Parameters

| Parameter | Description |
|------|------|
| `-Dwx.aibot.scene=...` | Scene identifier |
| `-Dwx.aibot.plugVersion=...` | Plugin version |
| `-Dwx.aibot.wsUrl=...` | WebSocket URL |
| `-Dwx.aibot.waitSeconds=300` | Wait timeout |
| `-Dwx.aibot.requestTimeoutSeconds=15` | Request timeout |
| `-Dwx.aibot.welcomeMode=card|text|raw` | Welcome mode |
| `-Dwx.aibot.downloadDir=/tmp/wx-aibot-it-downloads` | Download directory |
| `-Dwx.aibot.imagePath=/abs/path/demo.png` | Test image path |
| `-Dwx.aibot.filePath=/abs/path/demo.pdf` | Test file path |
| `-Dwx.aibot.voicePath=/abs/path/demo.amr` | Test voice path |
| `-Dwx.aibot.videoPath=/abs/path/demo.mp4` | Test video path |

### Environment Variables

You can also configure via environment variables:

| Variable | Description |
|--------|------|
| `WECOM_AIBOT_BOT_ID` | Bot ID |
| `WECOM_AIBOT_SECRET` | Secret |
| `WECOM_AIBOT_SCENE` | Scene identifier |
| `WECOM_AIBOT_PLUG_VERSION` | Plugin version |
| `WECOM_AIBOT_WS_URL` | WebSocket URL |
| `WECOM_AIBOT_WAIT_SECONDS` | Wait timeout |
| `WECOM_AIBOT_REQUEST_TIMEOUT_SECONDS` | Request timeout |
| `WECOM_AIBOT_WELCOME_MODE` | Welcome mode |
| `WECOM_AIBOT_DOWNLOAD_DIR` | Download directory |
| `WECOM_AIBOT_IMAGE_PATH` | Test image path |
| `WECOM_AIBOT_FILE_PATH` | Test file path |
| `WECOM_AIBOT_VOICE_PATH` | Test voice path |
| `WECOM_AIBOT_VIDEO_PATH` | Test video path |

### Test Notes

- `welcomeMode=card`: calls `replyWelcomeTemplateCard(...)`
- `welcomeMode=text`: calls `replyWelcomeText(...)`
- `welcomeMode=raw`: calls `replyWelcome(frame, body)`
- If `imagePath` is not configured, `回复图片` and `主动图片` use a built-in test image
- File/voice/video related commands are executed only if corresponding local file path is configured; otherwise skipped with explicit message

## 📄 Notes

- `sendMessage` / `reply` supports direct `Map<String, Object>` payload for protocol extension compatibility
- `replySimpleText` is a semantic helper for plain text reply; internally it sends `stream + finish=true`
- `replyStreamWithCard` and `msg_item` are wrapped by protocol, but usually not rendered on current WeChat clients even when server ack is successful
- For streaming mixed-content or streaming card scenarios, prefer fallback to `replyStream(...)`, `replyMarkdown(...)`, and `replyTemplateCard(...)` separately
- Current release prioritizes long-connection core protocol and common convenience APIs; for complex template card structures, assemble official JSON as `Map` directly

## 🔗 Related

- [Enterprise WeChat AI Bot Official Docs](https://developer.work.weixin.qq.com/document/path/101463)

## 📝 License

Distributed under the Apache License 2.0. See [LICENSE](LICENSE) for more information.
