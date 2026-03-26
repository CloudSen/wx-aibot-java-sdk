package io.github.cloudsen.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cloudsen.ai.WeComAiBotClient;
import io.github.cloudsen.ai.WeComAiBotClientOptions;
import io.github.cloudsen.ai.model.*;
import io.github.cloudsen.ai.support.JsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 真实企业微信集成测试。
 *
 * <p>手动执行示例：</p>
 *
 * <pre>{@code
 * mvn -Dtest=WeComAiBotClientIntegrationTest \
 *   -Dwx.aibot.it.enabled=true \
 *   -Dwx.aibot.botId=your-bot-id \
 *   -Dwx.aibot.secret=your-secret \
 *   test
 * }</pre>
 *
 * <p>可在会话中发送以下文本指令触发不同 API：</p>
 * <ul>
 *     <li>{@code 原始回复}</li>
 *     <li>{@code 简单文本}</li>
 *     <li>{@code markdown回复}</li>
 *     <li>{@code 流式回复}</li>
 *     <li>{@code 流式卡片}</li>
 *     <li>{@code 卡片回复}</li>
 *     <li>{@code 主动原始}</li>
 *     <li>{@code 主动markdown}</li>
 *     <li>{@code 主动卡片}</li>
 *     <li>{@code 回复图片}/{@code 回复文件}/{@code 回复语音}/{@code 回复视频}</li>
 *     <li>{@code 主动图片}/{@code 主动文件}/{@code 主动语音}/{@code 主动视频}</li>
 *     <li>{@code 结束测试}</li>
 * </ul>
 */
@EnabledIfSystemProperty(named = "wx.aibot.it.enabled", matches = "true")
class WeComAiBotClientIntegrationTest {

    private static final String FALLBACK_REPLY = "已收到：其他消息";
    private static final String DEFAULT_IMAGE_FILENAME = "it-default.png";
    private static final byte[] DEFAULT_MSG_ITEM_IMAGE_BYTES = java.util.Base64.getDecoder().decode(
            "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"
    );

    @Test
    void shouldConnectRealWeComAndCoverCommonApis() throws Exception {
        String botId = requireConfig("wx.aibot.botId", "WECOM_AIBOT_BOT_ID");
        String secret = requireConfig("wx.aibot.secret", "WECOM_AIBOT_SECRET");
        long waitSeconds = readLong("wx.aibot.waitSeconds", "WECOM_AIBOT_WAIT_SECONDS", 300L);
        long requestTimeoutMillis = TimeUnit.SECONDS.toMillis(
                readLong("wx.aibot.requestTimeoutSeconds", "WECOM_AIBOT_REQUEST_TIMEOUT_SECONDS", 15L)
        );

        WeComAiBotClientOptions.Builder optionsBuilder = WeComAiBotClientOptions.builder(botId, secret)
                .requestTimeoutMillis(requestTimeoutMillis)
                .heartbeatIntervalMillis(30_000L);

        String scene = readOptional("wx.aibot.scene", "WECOM_AIBOT_SCENE");
        if (scene != null) {
            optionsBuilder.scene(Integer.parseInt(scene));
        }

        String plugVersion = readOptional("wx.aibot.plugVersion", "WECOM_AIBOT_PLUG_VERSION");
        if (plugVersion != null) {
            optionsBuilder.plugVersion(plugVersion);
        }

        String wsUrl = readOptional("wx.aibot.wsUrl", "WECOM_AIBOT_WS_URL");
        if (wsUrl != null) {
            optionsBuilder.wsUrl(wsUrl);
        }

        IntegrationSettings settings = IntegrationSettings.load();
        Files.createDirectories(settings.downloadDir());

        CompletableFuture<Void> authenticatedFuture = new CompletableFuture<>();
        CompletableFuture<Void> finishFuture = new CompletableFuture<>();

        try (WeComAiBotClient client = new WeComAiBotClient(optionsBuilder.build())) {
            client.onAuthenticated(() -> {
                System.out.println("[IT] WeComAiBotClient authenticated");
                authenticatedFuture.complete(null);
            });

            client.onMessage((WsFrame<BaseMessage> frame) -> {
                BaseMessage body = frame.getBody();
                System.out.printf(
                        "[IT] Received message: msgType=%s, from=%s, chatId=%s, content=%s%n",
                        body.getMsgType(),
                        body.getFrom() == null ? null : body.getFrom().getUserid(),
                        body.getChatId(),
                        describeMessage(body)
                );
            });

            client.onEvent((WsFrame<EventMessage> frame) -> {
                EventMessage body = frame.getBody();
                System.out.printf(
                        "[IT] Received event: eventType=%s, eventKey=%s, taskId=%s%n",
                        body.getEvent() == null ? null : body.getEvent().getEventType(),
                        body.getEvent() == null ? null : body.getEvent().getEventKey(),
                        body.getEvent() == null ? null : body.getEvent().getTaskId()
                );
            });

            client.onEnterChat(frame -> trackAction(
                    handleEnterChat(client, frame, settings),
                    finishFuture,
                    false
            ));
            client.onTemplateCardEvent(frame -> trackAction(
                    handleTemplateCardEvent(client, frame),
                    finishFuture,
                    false
            ));
            client.onTextMessage(frame -> {
                String normalized = normalize(safeText(frame.getBody()));
                boolean finishTest = matches(normalized, "结束测试", "end-test", "endtest");
                trackAction(
                        handleTextMessage(client, frame, settings),
                        finishFuture,
                        finishTest
                );
            });
            client.onImageMessage(frame -> trackAction(
                    handleDownloadableMessage(client, frame, "图片", frame.getBody().getImage(), settings.downloadDir()),
                    finishFuture,
                    false
            ));
            client.onFileMessage(frame -> trackAction(
                    handleDownloadableMessage(client, frame, "文件", frame.getBody().getFile(), settings.downloadDir()),
                    finishFuture,
                    false
            ));
            client.onVideoMessage(frame -> trackAction(
                    handleDownloadableMessage(client, frame, "视频", frame.getBody().getVideo(), settings.downloadDir()),
                    finishFuture,
                    false
            ));
            client.onVoiceMessage(frame -> trackAction(
                    client.replySimpleText(frame, "已收到：语音消息")
                            .thenApply(ack -> "replySimpleText(voice)"),
                    finishFuture,
                    false
            ));
            client.onMixedMessage(frame -> trackAction(
                    client.replyMarkdown(frame, "## 已收到：图文混排消息")
                            .thenApply(ack -> "replyMarkdown(mixed)"),
                    finishFuture,
                    false
            ));

            client.onError(throwable -> {
                System.err.printf("[IT] Client error: %s%n", throwable.getMessage());
                if (!authenticatedFuture.isDone()) {
                    authenticatedFuture.completeExceptionally(throwable);
                }
                if (!finishFuture.isDone()) {
                    finishFuture.completeExceptionally(throwable);
                }
            });

            client.onDisconnected(reason -> System.out.printf("[IT] Client disconnected: %s%n", reason));

            client.connect();

            authenticatedFuture.get(30, TimeUnit.SECONDS);
            System.out.printf("[IT] Waiting up to %d seconds for real WeCom interactions. Send '结束测试' to finish.%n",
                    waitSeconds);

            assertDoesNotThrow(() -> finishFuture.get(waitSeconds, TimeUnit.SECONDS));
        }
    }

    private static CompletableFuture<String> handleEnterChat(WeComAiBotClient client,
                                                             WsFrame<EventMessage> frame,
                                                             IntegrationSettings settings) {
        if ("text".equals(settings.welcomeMode())) {
            return client.replyWelcomeText(frame,
                            "真实集成测试已连接。可发送：简单文本 / markdown回复 / 流式回复 / 流式卡片 / 卡片回复 / 主动markdown")
                    .thenApply(ack -> "replyWelcomeText");
        }
        if ("raw".equals(settings.welcomeMode())) {
            return client.replyWelcome(frame, buildWelcomeTextBody("真实集成测试已连接（raw welcome）。"))
                    .thenApply(ack -> "replyWelcome(raw)");
        }
        return client.replyWelcomeTemplateCard(frame, buildWelcomeTemplateCard(), new ReplyFeedback("it_welcome_feedback"))
                .thenApply(ack -> "replyWelcomeTemplateCard");
    }

    private static CompletableFuture<String> handleTemplateCardEvent(WeComAiBotClient client,
                                                                     WsFrame<EventMessage> frame) {
        logTemplateCardEventDiagnostics(frame);
        EventMessage body = frame.getBody();
        EventPayload event = body == null ? null : body.getEvent();
        String taskId = resolveEventField(event, "task_id", "taskId", "taskid");
        String eventKey = resolveEventField(event, "event_key", "eventKey", "key", "button_key");
        if (taskId == null || taskId.isBlank()) {
            System.out.printf("[IT] Skip updateTemplateCard because taskId is missing. eventKey=%s, rawExtensions=%s%n",
                    eventKey, event == null ? null : event.getExtensions());
            return CompletableFuture.completedFuture("skipUpdateTemplateCard(missing-taskId)");
        }
        Map<String, Object> templateCard = logTemplateCardPayload("updateTemplateCard",
                buildUpdatedTemplateCard(taskId, eventKey));
        return client.updateTemplateCard(frame, templateCard, null)
                .thenApply(ack -> "updateTemplateCard");
    }

    private static CompletableFuture<String> handleTextMessage(WeComAiBotClient client,
                                                               WsFrame<BaseMessage> frame,
                                                               IntegrationSettings settings) {
        BaseMessage body = frame.getBody();
        String content = safeText(body);
        String normalized = normalize(content);
        SendTarget sendTarget = resolveSendTarget(body);

        if (matches(normalized, "结束测试", "end-test", "endtest")) {
            return client.replySimpleText(frame, "已收到：结束测试，真实集成测试即将退出")
                    .thenApply(ack -> "replySimpleText(end-test)");
        }
        if (matches(normalized, "流式卡片", "stream-card", "streamcard")) {
            return handleStreamWithCard(client, frame, settings);
        }
        if (matches(normalized, "流式图文", "stream-item", "streamitem")) {
            return handleStreamWithMsgItem(client, frame, settings);
        }
        if (matches(normalized, "主动markdown", "send-markdown", "sendmarkdown")) {
            return client.sendMarkdownMessage(sendTarget.targetId(),
                            sendTarget.chatType(),
                            "## 已收到：主动 Markdown",
                            new ReplyFeedback("it_send_markdown_feedback"))
                    .thenApply(ack -> "sendMarkdownMessage");
        }
        if (matches(normalized, "主动卡片", "send-card", "sendcard")) {
            Map<String, Object> templateCard = logTemplateCardPayload("sendTemplateCardMessage",
                    buildNoticeTemplateCard("主动卡片消息", "这是一条由真实集成测试主动发送的卡片。"));
            return client.sendTemplateCardMessage(sendTarget.targetId(),
                            sendTarget.chatType(),
                            templateCard,
                            new ReplyFeedback("it_send_card_feedback"))
                    .thenApply(ack -> "sendTemplateCardMessage");
        }
        if (matches(normalized, "主动原始", "send-raw", "sendraw")) {
            return client.sendMessage(sendTarget.targetId(),
                            buildMarkdownBody("# 主动原始消息\n\n已收到：主动原始"))
                    .thenApply(ack -> "sendMessage");
        }
        if (matches(normalized, "回复图片", "reply-image", "replyimage")) {
            return handleReplyMedia(client, frame, settings.imagePath(), WeComMediaType.IMAGE);
        }
        if (matches(normalized, "回复文件", "reply-file", "replyfile")) {
            return handleReplyMedia(client, frame, settings.filePath(), WeComMediaType.FILE);
        }
        if (matches(normalized, "回复语音", "reply-voice", "replyvoice")) {
            return handleReplyMedia(client, frame, settings.voicePath(), WeComMediaType.VOICE);
        }
        if (matches(normalized, "回复视频", "reply-video", "replyvideo")) {
            return handleReplyMedia(client, frame, settings.videoPath(), WeComMediaType.VIDEO);
        }
        if (matches(normalized, "主动图片", "send-image", "sendimage")) {
            return handleSendMedia(client, frame, sendTarget, settings.imagePath(), WeComMediaType.IMAGE);
        }
        if (matches(normalized, "主动文件", "send-file", "sendfile")) {
            return handleSendMedia(client, frame, sendTarget, settings.filePath(), WeComMediaType.FILE);
        }
        if (matches(normalized, "主动语音", "send-voice", "sendvoice")) {
            return handleSendMedia(client, frame, sendTarget, settings.voicePath(), WeComMediaType.VOICE);
        }
        if (matches(normalized, "主动视频", "send-video", "sendvideo")) {
            return handleSendMedia(client, frame, sendTarget, settings.videoPath(), WeComMediaType.VIDEO);
        }
        if (matches(normalized, "原始回复", "reply-raw", "replyraw")) {
            return client.reply(frame, buildMarkdownBody("# 原始回复\n\n已收到：原始回复"))
                    .thenApply(ack -> "reply(raw)");
        }
        if (matches(normalized, "简单文本", "simple")) {
            return client.replySimpleText(frame, "已收到：简单文本")
                    .thenApply(ack -> "replySimpleText");
        }
        if (matches(normalized, "markdown回复", "markdown")) {
            return client.replyMarkdown(frame, "## 已收到：Markdown 回复", new ReplyFeedback("it_reply_markdown_feedback"))
                    .thenApply(ack -> "replyMarkdown");
        }
        if (matches(normalized, "流式回复", "stream")) {
            return handleStreamReply(client, frame);
        }
        if (matches(normalized, "卡片回复", "card")) {
            Map<String, Object> templateCard = logTemplateCardPayload("replyTemplateCard",
                    buildActionTemplateCard("模板卡片回复", "点击按钮后会触发 updateTemplateCard"));
            return client.replyTemplateCard(frame,
                            templateCard,
                            new ReplyFeedback("it_reply_card_feedback"))
                    .thenApply(ack -> "replyTemplateCard");
        }
        return client.replySimpleText(frame, FALLBACK_REPLY)
                .thenApply(ack -> "replySimpleText(fallback)");
    }

    private static CompletableFuture<String> handleStreamReply(WeComAiBotClient client, WsFrame<BaseMessage> frame) {
        String streamId = "it_stream_" + System.currentTimeMillis();
        return client.replyStream(frame, streamId, "处理中...", false, new ReplyFeedback("it_stream_feedback"))
                .thenCompose(ack -> client.replyStream(frame, streamId, "已收到：流式回复", true))
                .thenApply(ack -> "replyStream");
    }

    private static CompletableFuture<String> handleStreamWithMsgItem(WeComAiBotClient client,
                                                                     WsFrame<BaseMessage> frame,
                                                                     IntegrationSettings settings) {
        try {
            ReplyMsgItem msgItem = buildImageMsgItem(settings.imagePath());
            String streamId = "it_stream_item_" + System.currentTimeMillis();
            return client.replyStream(frame, streamId, "处理中...", false, new ReplyFeedback("it_stream_item_feedback"))
                    .thenCompose(ack -> client.replyStream(frame, streamId, "已收到：流式图文", true,
                            List.of(msgItem), null))
                    .thenApply(ack -> "replyStream(msgItem)");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<String> handleStreamWithCard(WeComAiBotClient client,
                                                                  WsFrame<BaseMessage> frame,
                                                                  IntegrationSettings settings) {
        try {
            List<ReplyMsgItem> msgItems = settings.imagePath() == null
                    ? null
                    : List.of(buildImageMsgItem(settings.imagePath()));
            Map<String, Object> templateCard = logTemplateCardPayload("replyStreamWithCard",
                    buildActionTemplateCard("流式卡片回复", "点击按钮可触发卡片更新"));
            return client.replyStreamWithCard(frame,
                            "it_stream_card_" + System.currentTimeMillis(),
                            "已收到：流式卡片",
                            true,
                            templateCard,
                            msgItems,
                            new ReplyFeedback("it_stream_card_feedback"),
                            new ReplyFeedback("it_stream_card_template_feedback"))
                    .thenApply(ack -> "replyStreamWithCard");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<String> handleReplyMedia(WeComAiBotClient client,
                                                              WsFrame<BaseMessage> frame,
                                                              Path mediaPath,
                                                              WeComMediaType mediaType) {
        try {
            MediaUploadSource mediaSource = resolveUploadSource(mediaPath, mediaType);
            if (mediaSource == null) {
                return client.replySimpleText(frame, "已收到：未配置 " + mediaType.getValue() + " 测试文件，已跳过")
                        .thenApply(ack -> "replySimpleText(skip-missing-" + mediaType.getValue() + ")");
            }
            UploadMediaOptions uploadOptions = new UploadMediaOptions(mediaType, mediaSource.filename());
            return client.uploadMedia(mediaSource.bytes(), uploadOptions)
                    .thenCompose(result -> {
                        System.out.printf("[IT] Uploaded media for reply: type=%s, filename=%s, mediaId=%s%n",
                                mediaType.getValue(), mediaSource.filename(), result.mediaId());
                        if (mediaType == WeComMediaType.VIDEO) {
                            return client.replyMedia(frame, mediaType, result.mediaId(), "真实集成测试视频", "replyMedia(video)");
                        }
                        return client.replyMedia(frame, mediaType, result.mediaId());
                    })
                    .thenApply(ack -> "replyMedia(" + mediaType.getValue() + ")");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<String> handleSendMedia(WeComAiBotClient client,
                                                             WsFrame<BaseMessage> frame,
                                                             SendTarget sendTarget,
                                                             Path mediaPath,
                                                             WeComMediaType mediaType) {
        try {
            MediaUploadSource mediaSource = resolveUploadSource(mediaPath, mediaType);
            if (mediaSource == null) {
                return client.replySimpleText(frame, "已收到：未配置 " + mediaType.getValue() + " 测试文件，已跳过")
                        .thenApply(ack -> "replySimpleText(skip-send-missing-" + mediaType.getValue() + ")");
            }
            UploadMediaOptions uploadOptions = new UploadMediaOptions(mediaType, mediaSource.filename());
            return client.uploadMedia(mediaSource.bytes(), uploadOptions)
                    .thenCompose(result -> {
                        System.out.printf("[IT] Uploaded media for send: type=%s, filename=%s, mediaId=%s, target=%s%n",
                                mediaType.getValue(), mediaSource.filename(), result.mediaId(), sendTarget.targetId());
                        if (mediaType == WeComMediaType.VIDEO) {
                            return client.sendMediaMessage(sendTarget.targetId(), sendTarget.chatType(), mediaType,
                                    result.mediaId(), "真实集成测试视频", "sendMediaMessage(video)");
                        }
                        return client.sendMediaMessage(sendTarget.targetId(), sendTarget.chatType(), mediaType,
                                result.mediaId(), null, null);
                    })
                    .thenApply(ack -> "sendMediaMessage(" + mediaType.getValue() + ")");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static CompletableFuture<String> handleDownloadableMessage(WeComAiBotClient client,
                                                                       WsFrame<BaseMessage> frame,
                                                                       String label,
                                                                       MediaResourceContent media,
                                                                       Path downloadDir) {
        if (media == null || media.getUrl() == null || media.getUrl().isBlank()) {
            return client.replySimpleText(frame, FALLBACK_REPLY)
                    .thenApply(ack -> "replySimpleText(fallback-download-missing)");
        }
        try {
            DownloadedFile downloadedFile = client.downloadFile(media.getUrl(), media.getAesKey());
            Path savedFile = saveDownloadedFile(downloadDir, label, downloadedFile);
            return client.replySimpleText(frame, "已收到：" + label + "消息，文件已下载：" + savedFile.getFileName())
                    .thenApply(ack -> "downloadFile(" + label + ")");
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static Path saveDownloadedFile(Path downloadDir, String label, DownloadedFile downloadedFile) throws IOException {
        String filename = downloadedFile.filename();
        String safeName = (filename == null || filename.isBlank())
                ? "it_" + label + "_" + System.currentTimeMillis()
                : filename;
        Path target = downloadDir.resolve(safeName);
        Files.write(target, downloadedFile.buffer());
        System.out.printf("[IT] Downloaded %s message to %s (%d bytes)%n",
                label, target, downloadedFile.buffer().length);
        return target;
    }

    private static void trackAction(CompletableFuture<String> actionFuture,
                                    CompletableFuture<Void> interactionFuture,
                                    boolean completeInteraction) {
        actionFuture.whenComplete((action, error) -> {
            if (error != null) {
                if (!interactionFuture.isDone()) {
                    interactionFuture.completeExceptionally(error);
                }
                return;
            }
            System.out.printf("[IT] Action completed: %s%n", action);
            if (completeInteraction && !interactionFuture.isDone()) {
                interactionFuture.complete(null);
            }
        });
    }

    private static String describeMessage(BaseMessage body) {
        if (body == null) {
            return null;
        }
        if (body.getText() != null && body.getText().getContent() != null) {
            return body.getText().getContent();
        }
        if (body.getVoice() != null && body.getVoice().getContent() != null) {
            return body.getVoice().getContent();
        }
        if (body.getImage() != null && body.getImage().getUrl() != null) {
            return body.getImage().getUrl();
        }
        if (body.getFile() != null && body.getFile().getUrl() != null) {
            return body.getFile().getUrl();
        }
        if (body.getVideo() != null && body.getVideo().getUrl() != null) {
            return body.getVideo().getUrl();
        }
        if (body.getMixed() != null && body.getMixed().getMsgItems() != null) {
            return "mixed(" + body.getMixed().getMsgItems().size() + ")";
        }
        return body.getMsgType();
    }

    private static String safeText(BaseMessage body) {
        return body == null || body.getText() == null || body.getText().getContent() == null
                ? ""
                : body.getText().getContent();
    }

    private static String normalize(String content) {
        return content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String normalized, String... keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static SendTarget resolveSendTarget(BaseMessage body) {
        String chatType = body == null ? null : body.getChatType();
        if ("group".equalsIgnoreCase(chatType)) {
            return new SendTarget(body.getChatId(), ChatType.GROUP);
        }
        String userId = body != null && body.getFrom() != null ? body.getFrom().getUserid() : null;
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve send target from message");
        }
        return new SendTarget(userId, ChatType.SINGLE);
    }

    private static ReplyMsgItem buildImageMsgItem(Path imagePath) throws IOException {
        byte[] bytes = imagePath == null ? DEFAULT_MSG_ITEM_IMAGE_BYTES : Files.readAllBytes(imagePath);
        return ReplyMsgItem.image(java.util.Base64.getEncoder().encodeToString(bytes), md5Hex(bytes));
    }

    private static MediaUploadSource resolveUploadSource(Path mediaPath,
                                                         WeComMediaType mediaType)
            throws IOException {
        if (mediaPath != null) {
            return new MediaUploadSource(Files.readAllBytes(mediaPath), mediaPath.getFileName().toString());
        }
        if (mediaType == WeComMediaType.IMAGE) {
            return new MediaUploadSource(DEFAULT_MSG_ITEM_IMAGE_BYTES, DEFAULT_IMAGE_FILENAME);
        }
        return null;
    }

    private static String md5Hex(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] md5Bytes = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(md5Bytes.length * 2);
            for (byte md5Byte : md5Bytes) {
                builder.append(String.format("%02x", md5Byte & 0xFF));
            }
            return builder.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    private static String resolveEventField(EventPayload event, String... keys) {
        if (event == null) {
            return null;
        }
        if (contains(keys, "task_id") && event.getTaskId() != null && !event.getTaskId().isBlank()) {
            return event.getTaskId();
        }
        if (contains(keys, "event_key") && event.getEventKey() != null && !event.getEventKey().isBlank()) {
            return event.getEventKey();
        }
        for (String key : keys) {
            JsonNode value = event.getExtensions().get(key);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String expected) {
        for (String value : values) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> buildMarkdownBody(String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", Map.of("content", content));
        return body;
    }

    private static Map<String, Object> buildWelcomeTextBody(String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of("content", content));
        return body;
    }

    private static Map<String, Object> buildWelcomeTemplateCard() {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("card_type", "text_notice");
        card.put("main_title", Map.of(
                "title", "真实集成测试已连接",
                "desc", "发送：简单文本 / markdown回复 / 流式回复 / 流式卡片 / 卡片回复 / 主动markdown / 结束测试"
        ));
        card.put("card_action", buildUrlCardAction());
        return card;
    }

    private static Map<String, Object> buildNoticeTemplateCard(String title, String desc) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("card_type", "text_notice");
        card.put("main_title", Map.of("title", title, "desc", desc));
        card.put("card_action", buildUrlCardAction());
        return card;
    }

    private static Map<String, Object> buildActionTemplateCard(String title, String desc) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("card_type", "button_interaction");
        card.put("main_title", Map.of("title", title, "desc", desc));
        card.put("task_id", "it_task_" + System.currentTimeMillis());
        card.put("button_list", List.of(
                Map.of("text", "确认", "key", "it_confirm", "style", 1),
                Map.of("text", "取消", "key", "it_cancel", "style", 2)
        ));
        return card;
    }

    private static Map<String, Object> buildUpdatedTemplateCard(String taskId, String eventKey) {
        String title = "it_confirm".equals(eventKey) ? "已确认" : "已取消";
        String desc = "模板卡片事件已回调，eventKey=" + eventKey;
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("card_type", "text_notice");
        card.put("main_title", Map.of("title", title, "desc", desc));
        card.put("card_action", buildUrlCardAction());
        if (taskId != null && !taskId.isBlank()) {
            card.put("task_id", taskId);
        }
        return card;
    }

    private static Map<String, Object> buildUrlCardAction() {
        return Map.of(
                "type", 1,
                "url", "https://work.weixin.qq.com/"
        );
    }

    private static Map<String, Object> logTemplateCardPayload(String action, Map<String, Object> templateCard) {
        System.out.printf("[IT] %s template_card payload:%n%s%n", action, toPrettyJson(templateCard));
        return templateCard;
    }

    private static void logTemplateCardEventDiagnostics(WsFrame<EventMessage> frame) {
        EventMessage body = frame.getBody();
        EventPayload event = body == null ? null : body.getEvent();
        System.out.printf("[IT] template_card_event payload:%n%s%n", toPrettyJson(event));
        System.out.printf("[IT] template_card_event frame:%n%s%n", toPrettyJson(frame));
    }

    private static String toPrettyJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return JsonSupport.getObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static String requireConfig(String propertyKey, String envKey) {
        String value = readOptional(propertyKey, envKey);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing config: set system property " + propertyKey + " or env " + envKey
            );
        }
        return value;
    }

    private static String readOptional(String propertyKey, String envKey) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }

    private static long readLong(String propertyKey, String envKey, long defaultValue) {
        String value = readOptional(propertyKey, envKey);
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    private record SendTarget(String targetId, ChatType chatType) {
    }

    private record MediaUploadSource(byte[] bytes, String filename) {
    }

    private record IntegrationSettings(String welcomeMode,
                                       Path downloadDir,
                                       Path imagePath,
                                       Path filePath,
                                       Path voicePath,
                                       Path videoPath) {

        private static IntegrationSettings load() {
            String welcomeMode = normalizeWelcomeMode(readOptional("wx.aibot.welcomeMode", "WECOM_AIBOT_WELCOME_MODE"));
            Path downloadDir = resolvePath(readOptional("wx.aibot.downloadDir", "WECOM_AIBOT_DOWNLOAD_DIR"));
            if (downloadDir == null) {
                downloadDir = Paths.get(System.getProperty("java.io.tmpdir"), "wx-aibot-it-downloads");
            }
            return new IntegrationSettings(
                    welcomeMode,
                    downloadDir,
                    resolvePath(readOptional("wx.aibot.imagePath", "WECOM_AIBOT_IMAGE_PATH")),
                    resolvePath(readOptional("wx.aibot.filePath", "WECOM_AIBOT_FILE_PATH")),
                    resolvePath(readOptional("wx.aibot.voicePath", "WECOM_AIBOT_VOICE_PATH")),
                    resolvePath(readOptional("wx.aibot.videoPath", "WECOM_AIBOT_VIDEO_PATH"))
            );
        }

        private static String normalizeWelcomeMode(String welcomeMode) {
            if (welcomeMode == null || welcomeMode.isBlank()) {
                return "card";
            }
            String normalized = welcomeMode.trim().toLowerCase(Locale.ROOT);
            if ("text".equals(normalized) || "raw".equals(normalized)) {
                return normalized;
            }
            return "card";
        }

        private static Path resolvePath(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Paths.get(value).toAbsolutePath().normalize();
        }
    }
}
