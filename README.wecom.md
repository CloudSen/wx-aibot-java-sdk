# WeCom SDK

[Overview](README.md) | WeCom | [Weixin](README.weixin.md) | [中文](README.wecom.zh.md)

Enterprise WeChat AI Bot Java SDK based on WebSocket long-lived connection.

Package: `io.github.cloudsen.ai.wecom`

## Features

| Feature | Description |
|------|------|
| Automatic connection management | Connects to `wss://openws.work.weixin.qq.com` and sends `aibot_subscribe` authentication |
| Heartbeat keepalive | Built-in app-level heartbeat with reconnect and auth-failure retry |
| Message callback dispatching | Supports text, image, file, voice, video, and mixed-content messages |
| Streaming reply | Serializes replies with the same `req_id` to avoid out-of-order stream packets |
| Active push | Supports Markdown, template card, and media message sending |
| File handling | 3-step temporary media upload, file download, and AES-256-CBC decryption |

## Quick Start

### Maven

```xml
<dependency>
    <groupId>io.github.cloudsen</groupId>
    <artifactId>wx-aibot-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Example

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
    client.replySimpleText(frame, "Received: " + content);
});
client.onEnterChat(frame -> client.replyWelcomeText(frame, "Hello, I am a bot powered by this Java SDK"));

client.connect();
```

## Common APIs

### Connection Management

| Method | Description |
|------|------|
| `connect()` | Establishes WebSocket connection and sends `aibot_subscribe` authentication |
| `disconnect()` | Disconnects current connection without destroying client instance |
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

### Reply APIs

| Method | Description |
|------|------|
| `reply(frame, body)` | Generic reply API for raw protocol body and extended fields |
| `replySimpleText(frame, content)` | Plain text reply helper |
| `replyMarkdown(frame, content)` | Markdown reply |
| `replyStream(frame, streamId, content, finish)` | Streaming reply |
| `replyStreamWithCard(...)` | Streaming reply with template card |
| `replyWelcome(frame, body)` | Generic welcome reply |
| `replyWelcomeText(frame, content)` | Text welcome reply |
| `replyWelcomeTemplateCard(frame, templateCard)` | Welcome template card reply |
| `replyTemplateCard(frame, templateCard, feedback)` | Template card reply |
| `updateTemplateCard(frame, templateCard, userIds)` | Update existing card on `template_card_event` |

### Active Send

| Method | Description |
|------|------|
| `sendMessage(chatId, body)` | Generic active send |
| `sendMarkdownMessage(chatId, content)` | Active Markdown send |
| `sendTemplateCardMessage(chatId, templateCard)` | Active template card send |
| `sendMediaMessage(chatId, mediaType, mediaId)` | Active media send |

### Media and File Handling

| Method | Description |
|------|------|
| `uploadMedia(fileBytes, options)` | Upload temporary media and return `media_id` |
| `downloadFile(url, aesKey)` | Download file and decrypt automatically when `aesKey` is present |

## Heartbeat and Reconnect

- Heartbeat starts only after `aibot_subscribe` succeeds.
- Default heartbeat interval is `30` seconds.
- If `2` consecutive heartbeats fail, the SDK closes the current WebSocket and treats it as dead.
- Reconnect uses exponential backoff by default, capped at `30` seconds.
- If server sends `disconnected_event`, the SDK stops auto reconnect and waits for business code to reconnect.

Recommended configuration:

```java
WeComAiBotClientOptions options = WeComAiBotClientOptions.builder("bot-id", "secret")
        .heartbeatIntervalMillis(30_000)
        .requestTimeoutMillis(10_000)
        .reconnectBaseDelayMillis(1_000)
        .maxReconnectAttempts(10)
        .build();
```

## Integration Test

Real Enterprise WeChat connectivity test is available in `WeComAiBotClientIntegrationTest`.

- Skipped by default, so regular `mvn test` is unaffected
- Connects to the real Enterprise WeChat WebSocket service
- Chooses different reply forms based on incoming user text
- Ends only when text command `结束测试` is received

### Text Commands

| Command | API |
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
| `结束测试` | Sends confirmation reply and exits |

### Run

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

| Variable | Description |
|------|------|
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

## Notes

- `sendMessage` and `reply` accept direct `Map<String, Object>` payloads for protocol extension compatibility.
- `replySimpleText` is a semantic helper; internally it sends `stream + finish=true`.
- `replyStreamWithCard` and `msg_item` are wrapped by the SDK, but current WeChat clients usually do not render them reliably.
- For stable user-visible output, prefer `replyStream(...)`, `replyMarkdown(...)`, and `replyTemplateCard(...)` separately.

## Related

- [Enterprise WeChat AI Bot Official Docs](https://developer.work.weixin.qq.com/document/path/101463)

## License

Distributed under the Apache License 2.0. See [LICENSE](LICENSE) for more information.
