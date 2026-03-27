package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cloudsen.ai.common.JsonSupport;
import okhttp3.*;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 微信协议 Java 客户端。
 */
public class WeixinClient implements AutoCloseable {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String AUTHORIZATION_TYPE = "ilink_bot_token";

    private final WeixinClientOptions options;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final boolean ownHttpClient;
    private final SecureRandom random = new SecureRandom();

    public WeixinClient(WeixinClientOptions options) {
        this.options = Objects.requireNonNull(options, "options");
        this.objectMapper = JsonSupport.getObjectMapper();
        this.ownHttpClient = options.getHttpClient() == null;
        this.httpClient = options.getHttpClient() == null
                ? new OkHttpClient.Builder()
                .connectTimeout(options.getApiTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(options.getLongPollTimeoutMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(options.getApiTimeoutMillis(), TimeUnit.MILLISECONDS)
                .build()
                : options.getHttpClient();
    }

    public WeixinQrLoginStartResult startQrLogin() {
        HttpUrl url = requireBaseUrl()
                .newBuilder()
                .addPathSegments("ilink/bot/get_bot_qrcode")
                .addQueryParameter("bot_type", options.getBotType())
                .build();
        Request.Builder builder = new Request.Builder().url(url).get();
        applyRouteTag(builder);
        return readJson(execute(builder.build(), options.getApiTimeoutMillis()), WeixinQrLoginStartResult.class);
    }

    public WeixinQrLoginStatusResult pollQrLoginStatus(String qrcode) {
        if (qrcode == null || qrcode.isBlank()) {
            throw new IllegalArgumentException("qrcode must not be blank");
        }
        HttpUrl url = requireBaseUrl()
                .newBuilder()
                .addPathSegments("ilink/bot/get_qrcode_status")
                .addQueryParameter("qrcode", qrcode)
                .build();
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get()
                .header("iLink-App-ClientVersion", "1");
        applyRouteTag(builder);
        return readJson(execute(builder.build(), options.getQrPollTimeoutMillis()), WeixinQrLoginStatusResult.class);
    }

    public WeixinQrLoginStatusResult waitForQrLogin(String qrcode, Duration timeout) {
        return waitForQrLogin(qrcode, timeout, Duration.ofSeconds(1));
    }

    public WeixinQrLoginStatusResult waitForQrLogin(String qrcode, Duration timeout, Duration interval) {
        long timeoutMillis = Math.max(timeout.toMillis(), 1_000L);
        long intervalMillis = Math.max(interval.toMillis(), 200L);
        long deadline = System.currentTimeMillis() + timeoutMillis;
        WeixinQrLoginStatusResult last = null;
        while (System.currentTimeMillis() < deadline) {
            last = pollQrLoginStatus(qrcode);
            if (last.confirmed() || last.expired()) {
                return last;
            }
            sleep(intervalMillis);
        }
        throw new WeixinException("微信二维码登录超时");
    }

    public WeixinGetUpdatesResponse getUpdates(String getUpdatesBuf) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("get_updates_buf", getUpdatesBuf == null ? "" : getUpdatesBuf);
        body.set("base_info", buildBaseInfoNode());
        try {
            String raw = postJson("ilink/bot/getupdates", body, options.getLongPollTimeoutMillis(), true, options.getToken());
            return readJson(raw, WeixinGetUpdatesResponse.class);
        } catch (WeixinException e) {
            if (e.getCause() instanceof InterruptedIOException) {
                return new WeixinGetUpdatesResponse(0, null, null, List.of(), getUpdatesBuf, null);
            }
            throw e;
        }
    }

    public String sendMessage(WeixinMessage message) {
        WeixinMessage normalized = ensureOutboundDefaults(message);
        ObjectNode body = objectMapper.createObjectNode();
        body.set("msg", objectMapper.valueToTree(normalized));
        body.set("base_info", buildBaseInfoNode());
        postJson("ilink/bot/sendmessage", body, options.getApiTimeoutMillis(), true, requireToken());
        return normalized.clientId();
    }

    public String sendTextMessage(String toUserId, String text, String contextToken) {
        return sendMessage(WeixinMessage.text(toUserId, text, contextToken));
    }

    public String sendImageMessage(String toUserId, WeixinUploadResult uploadResult, String contextToken) {
        return sendMessage(new WeixinMessage(
                null,
                null,
                null,
                toUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                WeixinMessage.MESSAGE_TYPE_BOT,
                WeixinMessage.MESSAGE_STATE_FINISH,
                List.of(WeixinMessage.Item.image(uploadResult)),
                contextToken
        ));
    }

    public String sendFileMessage(String toUserId, String fileName, WeixinUploadResult uploadResult, String contextToken) {
        return sendMessage(new WeixinMessage(
                null,
                null,
                null,
                toUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                WeixinMessage.MESSAGE_TYPE_BOT,
                WeixinMessage.MESSAGE_STATE_FINISH,
                List.of(WeixinMessage.Item.file(fileName, uploadResult)),
                contextToken
        ));
    }

    public String sendVideoMessage(String toUserId, WeixinUploadResult uploadResult, String contextToken) {
        return sendMessage(new WeixinMessage(
                null,
                null,
                null,
                toUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                WeixinMessage.MESSAGE_TYPE_BOT,
                WeixinMessage.MESSAGE_STATE_FINISH,
                List.of(WeixinMessage.Item.video(uploadResult)),
                contextToken
        ));
    }

    public WeixinGetUploadUrlResponse getUploadUrl(WeixinGetUploadUrlRequest request) {
        ObjectNode body = objectMapper.valueToTree(Objects.requireNonNull(request, "request"));
        body.set("base_info", buildBaseInfoNode());
        String raw = postJson("ilink/bot/getuploadurl", body, options.getApiTimeoutMillis(), true, requireToken());
        return readJson(raw, WeixinGetUploadUrlResponse.class);
    }

    public WeixinUploadResult uploadImage(Path filePath, String toUserId) {
        return uploadMedia(filePath, toUserId, WeixinUploadMediaType.IMAGE);
    }

    public WeixinUploadResult uploadVideo(Path filePath, String toUserId) {
        return uploadMedia(filePath, toUserId, WeixinUploadMediaType.VIDEO);
    }

    public WeixinUploadResult uploadFile(Path filePath, String toUserId) {
        return uploadMedia(filePath, toUserId, WeixinUploadMediaType.FILE);
    }

    public WeixinUploadResult uploadMedia(Path filePath, String toUserId, WeixinUploadMediaType mediaType) {
        try {
            byte[] plaintext = Files.readAllBytes(filePath);
            String rawFileMd5 = md5Hex(plaintext);
            int ciphertextSize = WeixinCdnCrypto.paddedSize(plaintext.length);
            byte[] aesKey = new byte[16];
            random.nextBytes(aesKey);
            String aesKeyHex = bytesToHex(aesKey);
            String filekey = randomHex(16);

            WeixinGetUploadUrlResponse uploadUrlResponse = getUploadUrl(new WeixinGetUploadUrlRequest(
                    filekey,
                    mediaType.code(),
                    toUserId,
                    (long) plaintext.length,
                    rawFileMd5,
                    (long) ciphertextSize,
                    null,
                    null,
                    null,
                    true,
                    aesKeyHex
            ));
            if (uploadUrlResponse.uploadParam() == null || uploadUrlResponse.uploadParam().isBlank()) {
                throw new WeixinException("getUploadUrl 未返回 upload_param");
            }

            String downloadParam = uploadBufferToCdn(plaintext, uploadUrlResponse.uploadParam(), filekey, aesKey);
            return new WeixinUploadResult(filekey, downloadParam, aesKeyHex, plaintext.length, ciphertextSize);
        } catch (IOException e) {
            throw new WeixinException("读取待上传文件失败: " + filePath, e);
        }
    }

    public WeixinGetConfigResponse getConfig(String ilinkUserId, String contextToken) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("ilink_user_id", ilinkUserId);
        if (contextToken != null && !contextToken.isBlank()) {
            body.put("context_token", contextToken);
        }
        body.set("base_info", buildBaseInfoNode());
        String raw = postJson("ilink/bot/getconfig", body, options.getConfigTimeoutMillis(), true, requireToken());
        return readJson(raw, WeixinGetConfigResponse.class);
    }

    public void sendTyping(String ilinkUserId, String typingTicket, WeixinTypingStatus status) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("ilink_user_id", ilinkUserId);
        body.put("typing_ticket", typingTicket);
        body.put("status", status.code());
        body.set("base_info", buildBaseInfoNode());
        postJson("ilink/bot/sendtyping", body, options.getConfigTimeoutMillis(), true, requireToken());
    }

    public byte[] downloadMedia(WeixinMessage.Item item) {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        String encryptedQueryParam = null;
        String aesKey = null;
        if (item.imageItem() != null && item.imageItem().media() != null) {
            encryptedQueryParam = item.imageItem().media().encryptQueryParam();
            if (item.imageItem().aeskey() != null && !item.imageItem().aeskey().isBlank()) {
                aesKey = Base64.getEncoder().encodeToString(WeixinCdnCrypto.hexToBytes(item.imageItem().aeskey()));
            } else {
                aesKey = item.imageItem().media().aesKey();
            }
        } else if (item.voiceItem() != null && item.voiceItem().media() != null) {
            encryptedQueryParam = item.voiceItem().media().encryptQueryParam();
            aesKey = item.voiceItem().media().aesKey();
        } else if (item.fileItem() != null && item.fileItem().media() != null) {
            encryptedQueryParam = item.fileItem().media().encryptQueryParam();
            aesKey = item.fileItem().media().aesKey();
        } else if (item.videoItem() != null && item.videoItem().media() != null) {
            encryptedQueryParam = item.videoItem().media().encryptQueryParam();
            aesKey = item.videoItem().media().aesKey();
        }
        if (encryptedQueryParam == null || encryptedQueryParam.isBlank()) {
            throw new WeixinException("消息项中没有可下载的 CDN 引用");
        }
        return downloadCdnMedia(encryptedQueryParam, aesKey);
    }

    public byte[] downloadCdnMedia(String encryptedQueryParam, String aesKeyBase64) {
        Request request = new Request.Builder()
                .url(buildCdnDownloadUrl(encryptedQueryParam))
                .get()
                .build();
        byte[] payload = executeBytes(request, options.getApiTimeoutMillis());
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
            return payload;
        }
        return WeixinCdnCrypto.decrypt(payload, WeixinCdnCrypto.parseAesKey(aesKeyBase64));
    }

    public String buildCdnDownloadUrl(String encryptedQueryParam) {
        return normalizedBase(options.getCdnBaseUrl()) + "download?encrypted_query_param="
                + URLEncoder.encode(encryptedQueryParam, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        if (ownHttpClient) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    private String uploadBufferToCdn(byte[] plaintext, String uploadParam, String filekey, byte[] aesKey) {
        byte[] ciphertext = WeixinCdnCrypto.encrypt(plaintext, aesKey);
        String uploadUrl = normalizedBase(options.getCdnBaseUrl())
                + "upload?encrypted_query_param="
                + URLEncoder.encode(uploadParam, StandardCharsets.UTF_8)
                + "&filekey=" + URLEncoder.encode(filekey, StandardCharsets.UTF_8);
        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(RequestBody.create(ciphertext, MediaType.get("application/octet-stream")))
                .build();
        try (Response response = newCall(request, options.getApiTimeoutMillis()).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() == null ? "" : response.body().string();
                throw new WeixinException("CDN 上传失败: " + response.code() + " " + body);
            }
            String downloadParam = response.header("x-encrypted-param");
            if (downloadParam == null || downloadParam.isBlank()) {
                throw new WeixinException("CDN 上传成功但未返回 x-encrypted-param");
            }
            return downloadParam;
        } catch (IOException e) {
            throw new WeixinException("CDN 上传失败", e);
        }
    }

    private WeixinMessage ensureOutboundDefaults(WeixinMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (message.toUserId() == null || message.toUserId().isBlank()) {
            throw new IllegalArgumentException("message.toUserId must not be blank");
        }
        if (message.itemList() == null || message.itemList().isEmpty()) {
            throw new IllegalArgumentException("message.itemList must not be empty");
        }
        WeixinMessage normalized = message;
        if (normalized.clientId() == null || normalized.clientId().isBlank()) {
            normalized = normalized.withClientId("openclaw-weixin-" + randomHex(12));
        }
        if (normalized.messageType() == null || normalized.messageState() == null) {
            normalized = new WeixinMessage(
                    normalized.seq(),
                    normalized.messageId(),
                    normalized.fromUserId(),
                    normalized.toUserId(),
                    normalized.clientId(),
                    normalized.createTimeMs(),
                    normalized.updateTimeMs(),
                    normalized.deleteTimeMs(),
                    normalized.sessionId(),
                    normalized.groupId(),
                    normalized.messageType() == null ? WeixinMessage.MESSAGE_TYPE_BOT : normalized.messageType(),
                    normalized.messageState() == null ? WeixinMessage.MESSAGE_STATE_FINISH : normalized.messageState(),
                    normalized.itemList(),
                    normalized.contextToken()
            );
        }
        return normalized;
    }

    private ObjectNode buildBaseInfoNode() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("channel_version", options.getChannelVersion());
        return node;
    }

    private String postJson(String endpoint, JsonNode body, long timeoutMillis, boolean includeWechatHeaders, String token) {
        try {
            String json = objectMapper.writeValueAsString(body);
            Request.Builder builder = new Request.Builder()
                    .url(resolveUrl(endpoint))
                    .post(RequestBody.create(json, JSON_MEDIA_TYPE));
            if (includeWechatHeaders) {
                applyJsonApiHeaders(builder, token);
            }
            return execute(builder.build(), timeoutMillis);
        } catch (IOException e) {
            throw new WeixinException("序列化请求失败", e);
        }
    }

    private void applyJsonApiHeaders(Request.Builder builder, String token) {
        builder.header("Content-Type", "application/json");
        builder.header("AuthorizationType", AUTHORIZATION_TYPE);
        builder.header("X-WECHAT-UIN", randomWechatUin());
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }
        applyRouteTag(builder);
    }

    private void applyRouteTag(Request.Builder builder) {
        if (options.getRouteTag() != null && !options.getRouteTag().isBlank()) {
            builder.header("SKRouteTag", options.getRouteTag());
        }
    }

    private String randomWechatUin() {
        long unsigned = Integer.toUnsignedLong(random.nextInt());
        return Base64.getEncoder().encodeToString(String.valueOf(unsigned).getBytes(StandardCharsets.UTF_8));
    }

    private String execute(Request request, long timeoutMillis) {
        try (Response response = newCall(request, timeoutMillis).execute()) {
            if (response.body() == null) {
                throw new WeixinException("HTTP 响应体为空");
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new WeixinException("HTTP 调用失败: " + response.code() + " " + body);
            }
            return body;
        } catch (IOException e) {
            throw new WeixinException("HTTP 调用失败: " + request.url(), e);
        }
    }

    private byte[] executeBytes(Request request, long timeoutMillis) {
        try (Response response = newCall(request, timeoutMillis).execute()) {
            if (response.body() == null) {
                throw new WeixinException("HTTP 响应体为空");
            }
            byte[] body = response.body().bytes();
            if (!response.isSuccessful()) {
                throw new WeixinException("HTTP 调用失败: " + response.code());
            }
            return body;
        } catch (IOException e) {
            throw new WeixinException("HTTP 调用失败: " + request.url(), e);
        }
    }

    private Call newCall(Request request, long timeoutMillis) {
        OkHttpClient client = httpClient.newBuilder()
                .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
                .build();
        return client.newCall(request);
    }

    private <T> T readJson(String raw, Class<T> type) {
        try {
            return objectMapper.readValue(raw, type);
        } catch (IOException e) {
            throw new WeixinException("解析响应失败: " + raw, e);
        }
    }

    private HttpUrl requireBaseUrl() {
        HttpUrl url = HttpUrl.parse(normalizedBase(options.getBaseUrl()));
        if (url == null) {
            throw new WeixinException("非法 baseUrl: " + options.getBaseUrl());
        }
        return url;
    }

    private String resolveUrl(String endpoint) {
        return normalizedBase(options.getBaseUrl()) + endpoint;
    }

    private static String normalizedBase(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String requireToken() {
        if (options.getToken() == null || options.getToken().isBlank()) {
            throw new WeixinException("token 未配置");
        }
        return options.getToken();
    }

    private static String md5Hex(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return bytesToHex(digest.digest(payload));
        } catch (Exception e) {
            throw new WeixinException("计算 MD5 失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String randomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeixinException("等待二维码状态时被中断", e);
        }
    }
}
