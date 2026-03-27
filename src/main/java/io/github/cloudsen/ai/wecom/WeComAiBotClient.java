package io.github.cloudsen.ai.wecom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cloudsen.ai.wecom.listener.WeComAiBotListener;
import io.github.cloudsen.ai.wecom.logger.WeComAiBotLogger;
import io.github.cloudsen.ai.wecom.model.*;
import io.github.cloudsen.ai.common.JsonSupport;
import io.github.cloudsen.ai.wecom.support.ReqIdGenerator;
import io.github.cloudsen.ai.wecom.support.WsCommand;
import okhttp3.*;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 企业微信 AI Bot WebSocket 客户端。
 */
public class WeComAiBotClient implements AutoCloseable {

    private static final int NORMAL_CLOSE_CODE = 1000;
    private static final int AUTH_FAILURE_CLOSE_CODE = 1008;
    private static final int MAX_MISSED_HEARTBEAT_ACKS = 2;
    private static final int MAX_UPLOAD_CHUNKS = 100;
    private static final int UPLOAD_CHUNK_SIZE = 512 * 1024;
    private static final int MAX_UPLOAD_CHUNK_RETRIES = 2;
    private static final long MAX_RECONNECT_DELAY_MILLIS = 30_000L;

    private final WeComAiBotClientOptions options;
    private final ObjectMapper objectMapper;
    private final WeComAiBotLogger logger;
    private final OkHttpClient webSocketClient;
    private final OkHttpClient downloadClient;
    private final boolean ownHttpClient;
    private final List<WeComAiBotListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("wx-aibot-java-sdk"));
    private final ConcurrentMap<String, ReplyQueue> replyQueues = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingAck> pendingAcks = new ConcurrentHashMap<>();

    private volatile WebSocket webSocket;
    private volatile boolean authenticated;
    private volatile boolean lastCloseWasAuthFailure;
    private volatile boolean serverDisconnect;
    private volatile boolean closed;
    private volatile ScheduledFuture<?> heartbeatFuture;
    private volatile ScheduledFuture<?> reconnectFuture;
    private volatile int reconnectAttempts;
    private volatile int authFailureAttempts;
    private volatile int missedHeartbeatAcks;
    private volatile boolean manualClose;

    public WeComAiBotClient(WeComAiBotClientOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        this.objectMapper = JsonSupport.getObjectMapper();
        this.logger = options.getLogger();
        this.ownHttpClient = options.getHttpClient() == null;
        this.webSocketClient = options.getHttpClient() == null
                ? new OkHttpClient.Builder()
                .connectTimeout(options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .build()
                : options.getHttpClient();
        this.downloadClient = this.webSocketClient.newBuilder()
                .callTimeout(options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    public WeComAiBotClient addListener(WeComAiBotListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    public WeComAiBotClient removeListener(WeComAiBotListener listener) {
        listeners.remove(listener);
        return this;
    }

    /**
     * 注册连接建立回调。
     */
    public WeComAiBotListener onConnected(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onConnected() {
                callback.run();
            }
        });
    }

    /**
     * 注册鉴权成功回调。
     */
    public WeComAiBotListener onAuthenticated(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onAuthenticated() {
                callback.run();
            }
        });
    }

    /**
     * 注册断连回调。
     */
    public WeComAiBotListener onDisconnected(Consumer<String> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onDisconnected(String reason) {
                callback.accept(reason);
            }
        });
    }

    /**
     * 注册重连回调。
     */
    public WeComAiBotListener onReconnecting(IntConsumer callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onReconnecting(int attempt) {
                callback.accept(attempt);
            }
        });
    }

    /**
     * 注册错误回调。
     */
    public WeComAiBotListener onError(Consumer<Throwable> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onError(Throwable throwable) {
                callback.accept(throwable);
            }
        });
    }

    /**
     * 注册通用消息回调。
     */
    public WeComAiBotListener onMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册文本消息回调。
     */
    public WeComAiBotListener onTextMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onTextMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册图片消息回调。
     */
    public WeComAiBotListener onImageMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onImageMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册图文混排消息回调。
     */
    public WeComAiBotListener onMixedMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onMixedMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册语音消息回调。
     */
    public WeComAiBotListener onVoiceMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onVoiceMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册文件消息回调。
     */
    public WeComAiBotListener onFileMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onFileMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册视频消息回调。
     */
    public WeComAiBotListener onVideoMessage(Consumer<WsFrame<BaseMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onVideoMessage(WsFrame<BaseMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册通用事件回调。
     */
    public WeComAiBotListener onEvent(Consumer<WsFrame<EventMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onEvent(WsFrame<EventMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册进入会话事件回调。
     */
    public WeComAiBotListener onEnterChat(Consumer<WsFrame<EventMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onEnterChat(WsFrame<EventMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册模板卡片事件回调。
     */
    public WeComAiBotListener onTemplateCardEvent(Consumer<WsFrame<EventMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onTemplateCardEvent(WsFrame<EventMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册反馈事件回调。
     */
    public WeComAiBotListener onFeedbackEvent(Consumer<WsFrame<EventMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onFeedbackEvent(WsFrame<EventMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 注册服务端断开事件回调。
     */
    public WeComAiBotListener onServerDisconnectedEvent(Consumer<WsFrame<EventMessage>> callback) {
        Objects.requireNonNull(callback, "callback");
        return registerListener(new WeComAiBotListener() {
            @Override
            public void onServerDisconnectedEvent(WsFrame<EventMessage> frame) {
                callback.accept(frame);
            }
        });
    }

    /**
     * 建立连接并自动发起鉴权。
     */
    public synchronized WeComAiBotClient connect() {
        ensureNotClosed();
        manualClose = false;
        serverDisconnect = false;
        cancelReconnect();
        stopHeartbeat();
        authenticated = false;

        Request request = new Request.Builder()
                .url(options.getWsUrl())
                .build();
        WebSocket socket = webSocketClient.newWebSocket(request, new InternalWebSocketListener());
        webSocket = socket;
        logger.info("Connecting to WeCom AI Bot WebSocket: " + options.getWsUrl());
        return this;
    }

    /**
     * 断开连接，但保留客户端实例以便后续重连。
     */
    public synchronized void disconnect() {
        manualClose = true;
        cancelReconnect();
        stopHeartbeat();
        failAllPending("Client disconnected");
        WebSocket socket = webSocket;
        webSocket = null;
        authenticated = false;
        if (socket != null) {
            socket.close(NORMAL_CLOSE_CODE, "client disconnect");
            socket.cancel();
        }
    }

    public boolean isConnected() {
        return webSocket != null && authenticated;
    }

    public CompletableFuture<WsFrame<JsonNode>> reply(WsFrame<?> frame, Object body) {
        return reply(extractReqId(frame), body, WsCommand.RESPONSE);
    }

    public CompletableFuture<WsFrame<JsonNode>> reply(String reqId, Object body) {
        return reply(reqId, body, WsCommand.RESPONSE);
    }

    public CompletableFuture<WsFrame<JsonNode>> reply(String reqId, Object body, String cmd) {
        String safeReqId = requireNonBlank(reqId, "reqId");
        WsFrame<Object> frame = new WsFrame<>(cmd, new WsHeaders(safeReqId), body);
        return enqueueReplyFrame(safeReqId, frame);
    }

    public CompletableFuture<WsFrame<JsonNode>> replyStream(WsFrame<?> frame,
                                                            String streamId,
                                                            String content,
                                                            boolean finish) {
        return replyStream(frame, streamId, content, finish, null, null);
    }

    public CompletableFuture<WsFrame<JsonNode>> replyStream(WsFrame<?> frame,
                                                            String streamId,
                                                            String content,
                                                            boolean finish,
                                                            ReplyFeedback feedback) {
        return replyStream(frame, streamId, content, finish, null, feedback);
    }

    /**
     * 回复流式消息，可在结束帧携带图文混排项。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyStream(WsFrame<?> frame,
                                                            String streamId,
                                                            String content,
                                                            boolean finish,
                                                            List<ReplyMsgItem> msgItems,
                                                            ReplyFeedback feedback) {
        return reply(frame, buildStreamBody(streamId, content, finish, msgItems, feedback));
    }

    /**
     * 回复 Markdown 消息。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyMarkdown(WsFrame<?> frame, String content) {
        return replyMarkdown(frame, content, null);
    }

    /**
     * 回复 Markdown 消息。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyMarkdown(WsFrame<?> frame,
                                                              String content,
                                                              ReplyFeedback feedback) {
        return reply(frame, buildMarkdownBody(content, feedback));
    }

    /**
     * 回复一条简单文本消息。
     *
     * <p>企业微信长连接普通回包不支持 {@code text}，此方法内部会使用
     * {@code stream + finish=true} 封装一个最终文本回复。</p>
     */
    public CompletableFuture<WsFrame<JsonNode>> replySimpleText(WsFrame<?> frame, String content) {
        return replySimpleText(frame, content, null);
    }

    /**
     * 回复一条简单文本消息。
     */
    public CompletableFuture<WsFrame<JsonNode>> replySimpleText(WsFrame<?> frame,
                                                                String content,
                                                                ReplyFeedback feedback) {
        return replyStream(frame, ReqIdGenerator.generate("stream"), requireNonBlank(content, "content"), true, feedback);
    }

    public CompletableFuture<WsFrame<JsonNode>> replyWelcome(WsFrame<?> frame, Object body) {
        return reply(extractReqId(frame), body, WsCommand.RESPONSE_WELCOME);
    }

    public CompletableFuture<WsFrame<JsonNode>> replyWelcomeText(WsFrame<?> frame, String content) {
        return replyWelcome(frame, buildWelcomeTextBody(content));
    }

    /**
     * 回复欢迎模板卡片。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyWelcomeTemplateCard(WsFrame<?> frame,
                                                                         Map<String, Object> templateCard) {
        return replyWelcomeTemplateCard(frame, templateCard, null);
    }

    /**
     * 回复欢迎模板卡片。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyWelcomeTemplateCard(WsFrame<?> frame,
                                                                         Map<String, Object> templateCard,
                                                                         ReplyFeedback feedback) {
        return replyWelcome(frame, buildTemplateCardBody(templateCard, feedback));
    }

    public CompletableFuture<WsFrame<JsonNode>> replyTemplateCard(WsFrame<?> frame,
                                                                  Map<String, Object> templateCard,
                                                                  ReplyFeedback feedback) {
        return reply(frame, buildTemplateCardBody(templateCard, feedback));
    }

    /**
     * 回复流式消息并附带模板卡片。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyStreamWithCard(WsFrame<?> frame,
                                                                    String streamId,
                                                                    String content,
                                                                    boolean finish,
                                                                    Map<String, Object> templateCard) {
        return replyStreamWithCard(frame, streamId, content, finish, templateCard, null, null, null);
    }

    /**
     * 回复流式消息并附带模板卡片。
     */
    public CompletableFuture<WsFrame<JsonNode>> replyStreamWithCard(WsFrame<?> frame,
                                                                    String streamId,
                                                                    String content,
                                                                    boolean finish,
                                                                    Map<String, Object> templateCard,
                                                                    List<ReplyMsgItem> msgItems,
                                                                    ReplyFeedback streamFeedback,
                                                                    ReplyFeedback cardFeedback) {
        return reply(frame, buildStreamWithCardBody(streamId, content, finish, templateCard,
                msgItems, streamFeedback, cardFeedback));
    }

    public CompletableFuture<WsFrame<JsonNode>> updateTemplateCard(WsFrame<?> frame,
                                                                   Map<String, Object> templateCard,
                                                                   List<String> userIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("response_type", "update_template_card");
        body.put("template_card", new LinkedHashMap<>(templateCard));
        if (userIds != null && !userIds.isEmpty()) {
            body.put("userids", userIds);
        }
        return reply(extractReqId(frame), body, WsCommand.RESPONSE_UPDATE);
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMessage(String chatId, Object body) {
        return sendMessage(chatId, null, body);
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMessage(String chatId, ChatType chatType, Object body) {
        Map<String, Object> payload = new LinkedHashMap<>(toMap(body));
        payload.put("chatid", requireNonBlank(chatId, "chatId"));
        if (chatType != null) {
            payload.put("chat_type", chatType.getCode());
        }
        String reqId = ReqIdGenerator.generate(WsCommand.SEND_MSG);
        return sendFrameDirect(new WsFrame<>(WsCommand.SEND_MSG, new WsHeaders(reqId), payload));
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMarkdownMessage(String chatId,
                                                                    ChatType chatType,
                                                                    String content,
                                                                    ReplyFeedback feedback) {
        return sendMessage(chatId, chatType, buildMarkdownBody(content, feedback));
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMarkdownMessage(String chatId, String content) {
        return sendMarkdownMessage(chatId, null, content, null);
    }

    /**
     * 主动发送模板卡片消息。
     */
    public CompletableFuture<WsFrame<JsonNode>> sendTemplateCardMessage(String chatId,
                                                                        Map<String, Object> templateCard) {
        return sendTemplateCardMessage(chatId, null, templateCard, null);
    }

    /**
     * 主动发送模板卡片消息。
     */
    public CompletableFuture<WsFrame<JsonNode>> sendTemplateCardMessage(String chatId,
                                                                        ChatType chatType,
                                                                        Map<String, Object> templateCard,
                                                                        ReplyFeedback feedback) {
        return sendMessage(chatId, chatType, buildTemplateCardBody(templateCard, feedback));
    }

    public CompletableFuture<WsFrame<JsonNode>> replyMedia(WsFrame<?> frame,
                                                           WeComMediaType mediaType,
                                                           String mediaId) {
        return replyMedia(frame, mediaType, mediaId, null, null);
    }

    public CompletableFuture<WsFrame<JsonNode>> replyMedia(WsFrame<?> frame,
                                                           WeComMediaType mediaType,
                                                           String mediaId,
                                                           String title,
                                                           String description) {
        return reply(frame, buildMediaBody(mediaType, mediaId, title, description));
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMediaMessage(String chatId,
                                                                 WeComMediaType mediaType,
                                                                 String mediaId) {
        return sendMediaMessage(chatId, null, mediaType, mediaId, null, null);
    }

    public CompletableFuture<WsFrame<JsonNode>> sendMediaMessage(String chatId,
                                                                 ChatType chatType,
                                                                 WeComMediaType mediaType,
                                                                 String mediaId,
                                                                 String title,
                                                                 String description) {
        return sendMessage(chatId, chatType, buildMediaBody(mediaType, mediaId, title, description));
    }

    public CompletableFuture<UploadMediaResult> uploadMedia(byte[] fileBytes, UploadMediaOptions uploadOptions) {
        Objects.requireNonNull(uploadOptions, "uploadOptions");
        if (fileBytes == null || fileBytes.length == 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("fileBytes must not be empty"));
        }

        int totalChunks = (fileBytes.length + UPLOAD_CHUNK_SIZE - 1) / UPLOAD_CHUNK_SIZE;
        if (totalChunks > MAX_UPLOAD_CHUNKS) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("file size exceeds 100 chunks of 512KB"));
        }

        Map<String, Object> initBody = new LinkedHashMap<>();
        initBody.put("type", uploadOptions.getType().getValue());
        initBody.put("filename", uploadOptions.getFilename());
        initBody.put("total_size", fileBytes.length);
        initBody.put("total_chunks", totalChunks);
        initBody.put("md5", md5Hex(fileBytes));

        String initReqId = ReqIdGenerator.generate(WsCommand.UPLOAD_MEDIA_INIT);
        WsFrame<Object> initFrame = new WsFrame<>(WsCommand.UPLOAD_MEDIA_INIT, new WsHeaders(initReqId), initBody);

        return sendFrameDirect(initFrame)
                .thenCompose(initAck -> {
                    String uploadId = readRequiredText(initAck.getBody(), "upload_id");
                    return uploadChunks(fileBytes, uploadId)
                            .thenCompose(unused -> finishUpload(uploadId, uploadOptions.getType()))
                            .thenApply(body -> new UploadMediaResult(
                                    uploadOptions.getType(),
                                    readRequiredText(body, "media_id"),
                                    body != null && body.has("created_at") && !body.get("created_at").isNull()
                                            ? body.get("created_at").asLong()
                                            : null
                            ));
                });
    }

    public DownloadedFile downloadFile(String url, String aesKey) throws IOException {
        Request request = new Request.Builder()
                .url(requireNonBlank(url, "url"))
                .build();
        try (Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("download failed, http status=" + response.code());
            }
            byte[] encrypted = response.body().bytes();
            String filename = parseFilename(response.header("Content-Disposition"));
            if (aesKey == null || aesKey.isBlank()) {
                return new DownloadedFile(encrypted, filename);
            }
            return new DownloadedFile(decryptFile(encrypted, aesKey), filename);
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        disconnect();
        scheduler.shutdownNow();
        if (ownHttpClient) {
            webSocketClient.dispatcher().executorService().shutdown();
            webSocketClient.connectionPool().evictAll();
            if (webSocketClient.cache() != null) {
                try {
                    webSocketClient.cache().close();
                } catch (IOException e) {
                    logger.error("Failed to close okhttp cache", e);
                }
            }
        }
    }

    private CompletableFuture<JsonNode> finishUpload(String uploadId, WeComMediaType mediaType) {
        Map<String, Object> finishBody = new LinkedHashMap<>();
        finishBody.put("upload_id", uploadId);
        String finishReqId = ReqIdGenerator.generate(WsCommand.UPLOAD_MEDIA_FINISH);
        WsFrame<Object> finishFrame = new WsFrame<>(WsCommand.UPLOAD_MEDIA_FINISH, new WsHeaders(finishReqId), finishBody);
        return sendFrameDirect(finishFrame)
                .thenApply(ack -> {
                    JsonNode body = ack.getBody();
                    if (body == null) {
                        throw new IllegalStateException("upload finish response body is empty");
                    }
                    if (!body.hasNonNull("type")) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) body)
                                .put("type", mediaType.getValue());
                    }
                    return body;
                });
    }

    private CompletableFuture<Void> uploadChunks(byte[] fileBytes, String uploadId) {
        int totalChunks = (fileBytes.length + UPLOAD_CHUNK_SIZE - 1) / UPLOAD_CHUNK_SIZE;
        int concurrency = totalChunks <= 4 ? totalChunks : totalChunks <= 10 ? 3 : 2;
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int batchStart = 0; batchStart < totalChunks; batchStart += concurrency) {
            int currentBatchStart = batchStart;
            chain = chain.thenCompose(unused -> {
                List<CompletableFuture<WsFrame<JsonNode>>> batch = new ArrayList<>();
                int batchEnd = Math.min(totalChunks, currentBatchStart + concurrency);
                for (int chunkIndex = currentBatchStart; chunkIndex < batchEnd; chunkIndex++) {
                    batch.add(uploadChunkWithRetry(fileBytes, uploadId, chunkIndex, 0));
                }
                return CompletableFuture.allOf(batch.toArray(CompletableFuture[]::new));
            });
        }
        return chain;
    }

    private CompletableFuture<WsFrame<JsonNode>> uploadChunkWithRetry(byte[] fileBytes,
                                                                      String uploadId,
                                                                      int chunkIndex,
                                                                      int retryCount) {
        int start = chunkIndex * UPLOAD_CHUNK_SIZE;
        int end = Math.min(start + UPLOAD_CHUNK_SIZE, fileBytes.length);
        byte[] chunkBytes = new byte[end - start];
        System.arraycopy(fileBytes, start, chunkBytes, 0, chunkBytes.length);

        Map<String, Object> chunkBody = new LinkedHashMap<>();
        chunkBody.put("upload_id", uploadId);
        chunkBody.put("chunk_index", chunkIndex);
        chunkBody.put("base64_data", Base64.getEncoder().encodeToString(chunkBytes));

        String reqId = ReqIdGenerator.generate(WsCommand.UPLOAD_MEDIA_CHUNK);
        WsFrame<Object> chunkFrame = new WsFrame<>(WsCommand.UPLOAD_MEDIA_CHUNK, new WsHeaders(reqId), chunkBody);
        return sendFrameDirect(chunkFrame)
                .handle((ack, error) -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(ack);
                    }
                    if (retryCount >= MAX_UPLOAD_CHUNK_RETRIES) {
                        return CompletableFuture.<WsFrame<JsonNode>>failedFuture(error);
                    }
                    logger.warn("Upload chunk retry " + (retryCount + 1) + " for chunk " + chunkIndex);
                    return uploadChunkWithRetry(fileBytes, uploadId, chunkIndex, retryCount + 1);
                })
                .thenCompose(future -> future);
    }

    private CompletableFuture<WsFrame<JsonNode>> enqueueReplyFrame(String reqId, WsFrame<?> frame) {
        ensureNotClosed();
        ReplyQueue queue = replyQueues.computeIfAbsent(reqId, ignored -> new ReplyQueue());
        QueueItem item = new QueueItem(frame);
        synchronized (queue) {
            if (queue.items.size() >= options.getMaxReplyQueueSize()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("reply queue exceeded max size for req_id " + reqId));
            }
            queue.items.addLast(item);
            if (!queue.inFlight) {
                queue.inFlight = true;
                dispatchReplyQueue(reqId, queue);
            }
        }
        return item.future;
    }

    private void dispatchReplyQueue(String reqId, ReplyQueue queue) {
        QueueItem current;
        synchronized (queue) {
            current = queue.items.peekFirst();
            if (current == null) {
                queue.inFlight = false;
                replyQueues.remove(reqId, queue);
                return;
            }
        }

        sendFrameDirect(current.frame)
                .whenComplete((ack, error) -> {
                    synchronized (queue) {
                        queue.items.pollFirst();
                        if (queue.items.isEmpty()) {
                            queue.inFlight = false;
                            replyQueues.remove(reqId, queue);
                        } else {
                            dispatchReplyQueue(reqId, queue);
                        }
                    }
                    if (error != null) {
                        current.future.completeExceptionally(unwrap(error));
                    } else {
                        current.future.complete(ack);
                    }
                });
    }

    private CompletableFuture<WsFrame<JsonNode>> sendFrameDirect(WsFrame<?> frame) {
        ensureNotClosed();
        WebSocket socket = webSocket;
        if (socket == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("WebSocket is not connected"));
        }
        String reqId = requireNonBlank(frame.getReqId(), "frame.reqId");
        CompletableFuture<WsFrame<JsonNode>> future = new CompletableFuture<>();
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            PendingAck pending = pendingAcks.remove(reqId);
            if (pending != null) {
                pending.timeoutFuture.cancel(false);
                pending.future.completeExceptionally(new TimeoutException("Ack timeout for req_id=" + reqId));
            }
        }, options.getRequestTimeoutMillis(), TimeUnit.MILLISECONDS);

        PendingAck previous = pendingAcks.put(reqId, new PendingAck(future, timeoutFuture));
        if (previous != null) {
            previous.timeoutFuture.cancel(false);
            previous.future.completeExceptionally(
                    new IllegalStateException("Duplicate pending ack for req_id=" + reqId));
        }

        try {
            String payload = objectMapper.writeValueAsString(frame);
            logger.debug("Send frame cmd=" + frame.getCmd() + ", req_id=" + reqId);
            boolean sent = socket.send(payload);
            if (!sent) {
                PendingAck removed = pendingAcks.remove(reqId);
                if (removed != null) {
                    removed.timeoutFuture.cancel(false);
                }
                future.completeExceptionally(new IllegalStateException("Failed to send frame for req_id=" + reqId));
            }
        } catch (Exception e) {
            PendingAck removed = pendingAcks.remove(reqId);
            if (removed != null) {
                removed.timeoutFuture.cancel(false);
            }
            future.completeExceptionally(e);
        }
        return future;
    }

    private void sendAuth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", options.getBotId());
        body.put("secret", options.getSecret());
        if (options.getScene() != null) {
            body.put("scene", options.getScene());
        }
        if (options.getPlugVersion() != null && !options.getPlugVersion().isBlank()) {
            body.put("plug_version", options.getPlugVersion());
        }

        String reqId = ReqIdGenerator.generate(WsCommand.SUBSCRIBE);
        WsFrame<Object> authFrame = new WsFrame<>(WsCommand.SUBSCRIBE, new WsHeaders(reqId), body);
        sendFrameDirect(authFrame).whenComplete((ack, error) -> {
            if (error != null) {
                lastCloseWasAuthFailure = true;
                Throwable cause = unwrap(error);
                notifyListeners(listener -> listener.onError(cause));
                closeSocketSilently(AUTH_FAILURE_CLOSE_CODE, "authentication failed");
                return;
            }
            authenticated = true;
            reconnectAttempts = 0;
            authFailureAttempts = 0;
            lastCloseWasAuthFailure = false;
            missedHeartbeatAcks = 0;
            startHeartbeat();
            notifyListeners(WeComAiBotListener::onAuthenticated);
        });
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatFuture = scheduler.scheduleAtFixedRate(this::sendHeartbeat,
                options.getHeartbeatIntervalMillis(),
                options.getHeartbeatIntervalMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

    private void sendHeartbeat() {
        if (webSocket == null) {
            return;
        }
        if (missedHeartbeatAcks >= MAX_MISSED_HEARTBEAT_ACKS) {
            logger.warn("Heartbeat ack missed " + missedHeartbeatAcks + " times, closing socket");
            closeSocketSilently(NORMAL_CLOSE_CODE, "heartbeat timeout");
            return;
        }
        missedHeartbeatAcks++;
        String reqId = ReqIdGenerator.generate(WsCommand.HEARTBEAT);
        WsFrame<Void> pingFrame = new WsFrame<>(WsCommand.HEARTBEAT, new WsHeaders(reqId), null);
        sendFrameDirect(pingFrame).whenComplete((ack, error) -> {
            if (error == null) {
                missedHeartbeatAcks = 0;
            } else {
                logger.warn("Heartbeat ack failed: " + unwrap(error).getMessage());
            }
        });
    }

    private void handleIncomingText(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            WsFrame<JsonNode> frame = objectMapper.convertValue(root, new TypeReference<>() {
            });
            String cmd = frame.getCmd();
            if (WsCommand.CALLBACK.equals(cmd)) {
                dispatchMessageFrame(frame);
                return;
            }
            if (WsCommand.EVENT_CALLBACK.equals(cmd)) {
                dispatchEventFrame(frame);
                return;
            }
            if (frame.getReqId() != null && pendingAcks.containsKey(frame.getReqId())) {
                handleAckFrame(frame);
                return;
            }
            logger.warn("Ignoring unexpected frame: " + text);
        } catch (Exception e) {
            logger.error("Failed to parse incoming frame", e);
            notifyListeners(listener -> listener.onError(e));
        }
    }

    private void dispatchMessageFrame(WsFrame<JsonNode> rawFrame) {
        BaseMessage message = objectMapper.convertValue(rawFrame.getBody(), BaseMessage.class);
        WsFrame<BaseMessage> frame = rawFrame.withBody(message);
        notifyListeners(listener -> listener.onMessage(frame));

        String messageType = message.getMsgType() == null
                ? ""
                : message.getMsgType().toLowerCase(Locale.ROOT);
        if (MessageType.TEXT.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onTextMessage(frame));
        } else if (MessageType.IMAGE.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onImageMessage(frame));
        } else if (MessageType.MIXED.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onMixedMessage(frame));
        } else if (MessageType.VOICE.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onVoiceMessage(frame));
        } else if (MessageType.FILE.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onFileMessage(frame));
        } else if (MessageType.VIDEO.getValue().equals(messageType)) {
            notifyListeners(listener -> listener.onVideoMessage(frame));
        }
    }

    private void dispatchEventFrame(WsFrame<JsonNode> rawFrame) {
        EventMessage eventMessage = objectMapper.convertValue(rawFrame.getBody(), EventMessage.class);
        WsFrame<EventMessage> frame = rawFrame.withBody(eventMessage);
        notifyListeners(listener -> listener.onEvent(frame));

        String eventType = eventMessage.getEvent() == null || eventMessage.getEvent().getEventType() == null
                ? ""
                : eventMessage.getEvent().getEventType().toLowerCase(Locale.ROOT);
        if (EventType.ENTER_CHAT.getValue().equals(eventType)) {
            notifyListeners(listener -> listener.onEnterChat(frame));
        } else if (EventType.TEMPLATE_CARD_EVENT.getValue().equals(eventType)) {
            notifyListeners(listener -> listener.onTemplateCardEvent(frame));
        } else if (EventType.FEEDBACK_EVENT.getValue().equals(eventType)) {
            notifyListeners(listener -> listener.onFeedbackEvent(frame));
        } else if (EventType.DISCONNECTED_EVENT.getValue().equals(eventType)) {
            serverDisconnect = true;
            notifyListeners(listener -> listener.onServerDisconnectedEvent(frame));
        }
    }

    private void handleAckFrame(WsFrame<JsonNode> frame) {
        PendingAck pending = pendingAcks.remove(frame.getReqId());
        if (pending == null) {
            logger.warn("No pending ack found for req_id=" + frame.getReqId());
            return;
        }
        pending.timeoutFuture.cancel(false);
        if (frame.getErrcode() != null && frame.getErrcode() != 0) {
            pending.future.completeExceptionally(
                    new IllegalStateException("Ack failed for req_id=" + frame.getReqId()
                            + ", errcode=" + frame.getErrcode()
                            + ", errmsg=" + frame.getErrmsg()));
            return;
        }
        pending.future.complete(frame);
    }

    private void handleSocketClosed(String reason) {
        stopHeartbeat();
        failAllPending("WebSocket closed: " + reason);
        authenticated = false;
        webSocket = null;
        notifyListeners(listener -> listener.onDisconnected(reason));
        if (!manualClose && !closed && !serverDisconnect) {
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        if (closed || manualClose) {
            return;
        }
        boolean authFailure = lastCloseWasAuthFailure;
        int attempt = authFailure ? ++authFailureAttempts : ++reconnectAttempts;
        int maxAttempts = authFailure ? options.getMaxAuthFailureAttempts() : options.getMaxReconnectAttempts();
        if (maxAttempts != -1 && attempt > maxAttempts) {
            notifyListeners(listener -> listener.onError(
                    new IllegalStateException("Reconnect exhausted, attempts=" + attempt)));
            return;
        }
        long delay = calculateReconnectDelay(attempt);
        notifyListeners(listener -> listener.onReconnecting(attempt));
        cancelReconnect();
        reconnectFuture = scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
        logger.info("Scheduling reconnect attempt " + attempt + " after " + delay + "ms");
    }

    private long calculateReconnectDelay(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 10);
        return Math.min(options.getReconnectBaseDelayMillis() * multiplier, MAX_RECONNECT_DELAY_MILLIS);
    }

    private void cancelReconnect() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
    }

    private WeComAiBotListener registerListener(WeComAiBotListener listener) {
        addListener(listener);
        return listener;
    }

    private void notifyListeners(Consumer<WeComAiBotListener> callback) {
        for (WeComAiBotListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (Exception e) {
                logger.error("Listener callback failed", e);
            }
        }
    }

    private void failAllPending(String reason) {
        pendingAcks.forEach((reqId, pending) -> {
            pending.timeoutFuture.cancel(false);
            pending.future.completeExceptionally(new IllegalStateException(reason + ", req_id=" + reqId));
        });
        pendingAcks.clear();

        replyQueues.forEach((reqId, queue) -> {
            synchronized (queue) {
                while (!queue.items.isEmpty()) {
                    queue.items.pollFirst().future.completeExceptionally(
                            new IllegalStateException(reason + ", req_id=" + reqId));
                }
                queue.inFlight = false;
            }
        });
        replyQueues.clear();
    }

    private void closeSocketSilently(int code, String reason) {
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.close(code, reason);
            socket.cancel();
        }
    }

    private Map<String, Object> buildMediaBody(WeComMediaType mediaType,
                                               String mediaId,
                                               String title,
                                               String description) {
        Objects.requireNonNull(mediaType, "mediaType");
        Map<String, Object> mediaContent = new LinkedHashMap<>();
        mediaContent.put("media_id", requireNonBlank(mediaId, "mediaId"));
        if (mediaType == WeComMediaType.VIDEO) {
            if (title != null && !title.isBlank()) {
                mediaContent.put("title", title);
            }
            if (description != null && !description.isBlank()) {
                mediaContent.put("description", description);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", mediaType.getValue());
        body.put(mediaType.getValue(), mediaContent);
        return body;
    }

    private Map<String, Object> buildStreamBody(String streamId,
                                                String content,
                                                boolean finish,
                                                List<ReplyMsgItem> msgItems,
                                                ReplyFeedback feedback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "stream");
        body.put("stream", buildStreamPayload(streamId, content, finish, msgItems, feedback));
        return body;
    }

    private Map<String, Object> buildStreamWithCardBody(String streamId,
                                                        String content,
                                                        boolean finish,
                                                        Map<String, Object> templateCard,
                                                        List<ReplyMsgItem> msgItems,
                                                        ReplyFeedback streamFeedback,
                                                        ReplyFeedback cardFeedback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "stream_with_template_card");
        body.put("stream", buildStreamPayload(streamId, content, finish, msgItems, streamFeedback));
        if (templateCard != null && !templateCard.isEmpty()) {
            body.put("template_card", copyTemplateCard(templateCard, cardFeedback));
        }
        return body;
    }

    private Map<String, Object> buildStreamPayload(String streamId,
                                                   String content,
                                                   boolean finish,
                                                   List<ReplyMsgItem> msgItems,
                                                   ReplyFeedback feedback) {
        Map<String, Object> stream = new LinkedHashMap<>();
        stream.put("id", requireNonBlank(streamId, "streamId"));
        stream.put("finish", finish);
        if (content != null) {
            stream.put("content", content);
        }
        if (finish && msgItems != null && !msgItems.isEmpty()) {
            stream.put("msg_item", msgItems);
        }
        if (feedback != null) {
            stream.put("feedback", feedback);
        }
        return stream;
    }

    private Map<String, Object> buildMarkdownBody(String content, ReplyFeedback feedback) {
        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", requireNonBlank(content, "content"));
        if (feedback != null) {
            markdown.put("feedback", feedback);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "markdown");
        body.put("markdown", markdown);
        return body;
    }

    private Map<String, Object> buildTemplateCardBody(Map<String, Object> templateCard, ReplyFeedback feedback) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "template_card");
        body.put("template_card", copyTemplateCard(templateCard, feedback));
        return body;
    }

    private Map<String, Object> buildWelcomeTextBody(String content) {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", requireNonBlank(content, "content"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", text);
        return body;
    }

    private Map<String, Object> copyTemplateCard(Map<String, Object> templateCard, ReplyFeedback feedback) {
        Objects.requireNonNull(templateCard, "templateCard");
        Map<String, Object> card = new LinkedHashMap<>(templateCard);
        if (feedback != null) {
            card.put("feedback", feedback);
        }
        return card;
    }

    private Map<String, Object> toMap(Object body) {
        Objects.requireNonNull(body, "body");
        if (body instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return objectMapper.convertValue(body, new TypeReference<>() {
        });
    }

    private String parseFilename(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return null;
        }
        String utf8Prefix = "filename*=UTF-8''";
        int utf8Index = contentDisposition.indexOf(utf8Prefix);
        if (utf8Index >= 0) {
            String value = contentDisposition.substring(utf8Index + utf8Prefix.length()).split(";", 2)[0];
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        int filenameIndex = contentDisposition.indexOf("filename=");
        if (filenameIndex >= 0) {
            String value = contentDisposition.substring(filenameIndex + "filename=".length()).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                value = value.substring(1, value.length() - 1);
            }
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return null;
    }

    private byte[] decryptFile(byte[] encryptedBuffer, String aesKey) {
        try {
            byte[] key = Base64.getDecoder().decode(aesKey);
            if (key.length != 32) {
                throw new IllegalArgumentException("aesKey must decode to 32 bytes");
            }
            byte[] iv = new byte[16];
            System.arraycopy(key, 0, iv, 0, iv.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encryptedBuffer);
            return stripPkcs7Padding(decrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt file", e);
        }
    }

    private byte[] stripPkcs7Padding(byte[] decrypted) {
        int padLength = decrypted[decrypted.length - 1] & 0xFF;
        if (padLength < 1 || padLength > 32 || padLength > decrypted.length) {
            throw new IllegalStateException("Invalid PKCS#7 padding length: " + padLength);
        }
        for (int i = decrypted.length - padLength; i < decrypted.length; i++) {
            if ((decrypted[i] & 0xFF) != padLength) {
                throw new IllegalStateException("Invalid PKCS#7 padding bytes");
            }
        }
        byte[] result = new byte[decrypted.length - padLength];
        System.arraycopy(decrypted, 0, result, 0, result.length);
        return result;
    }

    private String md5Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }

    private String readRequiredText(JsonNode body, String fieldName) {
        if (body == null || !body.hasNonNull(fieldName)) {
            throw new IllegalStateException("response body missing field: " + fieldName);
        }
        return body.get(fieldName).asText();
    }

    private String extractReqId(WsFrame<?> frame) {
        Objects.requireNonNull(frame, "frame");
        return requireNonBlank(frame.getReqId(), "frame.reqId");
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Client already closed");
        }
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    private final class InternalWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            if (webSocket != WeComAiBotClient.this.webSocket) {
                return;
            }
            logger.info("WebSocket connected, sending auth frame");
            notifyListeners(WeComAiBotListener::onConnected);
            sendAuth();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (webSocket != WeComAiBotClient.this.webSocket) {
                return;
            }
            handleIncomingText(text);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            if (webSocket != WeComAiBotClient.this.webSocket) {
                return;
            }
            handleSocketClosed("code=" + code + ", reason=" + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            if (webSocket != WeComAiBotClient.this.webSocket) {
                return;
            }
            logger.error("WebSocket failure", throwable);
            notifyListeners(listener -> listener.onError(throwable));
            handleSocketClosed("failure: " + throwable.getMessage());
        }
    }

    private static final class PendingAck {
        private final CompletableFuture<WsFrame<JsonNode>> future;
        private final ScheduledFuture<?> timeoutFuture;

        private PendingAck(CompletableFuture<WsFrame<JsonNode>> future, ScheduledFuture<?> timeoutFuture) {
            this.future = future;
            this.timeoutFuture = timeoutFuture;
        }
    }

    private static final class ReplyQueue {
        private final Deque<QueueItem> items = new ArrayDeque<>();
        private boolean inFlight;
    }

    private static final class QueueItem {
        private final WsFrame<?> frame;
        private final CompletableFuture<WsFrame<JsonNode>> future = new CompletableFuture<>();

        private QueueItem(WsFrame<?> frame) {
            this.frame = frame;
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {

        private final String name;

        private DaemonThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        }
    }
}
