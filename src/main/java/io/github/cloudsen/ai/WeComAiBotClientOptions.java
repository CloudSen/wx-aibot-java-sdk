package io.github.cloudsen.ai;

import io.github.cloudsen.ai.logger.DefaultWeComAiBotLogger;
import io.github.cloudsen.ai.logger.WeComAiBotLogger;
import okhttp3.OkHttpClient;

import java.util.Objects;

/**
 * 企业微信 AI Bot 客户端配置。
 */
public final class WeComAiBotClientOptions {

    public static final String DEFAULT_WS_URL = "wss://openws.work.weixin.qq.com";
    public static final long DEFAULT_RECONNECT_BASE_DELAY_MILLIS = 1_000L;
    public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
    public static final int DEFAULT_MAX_AUTH_FAILURE_ATTEMPTS = 5;
    public static final long DEFAULT_HEARTBEAT_INTERVAL_MILLIS = 30_000L;
    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 10_000L;
    public static final int DEFAULT_MAX_REPLY_QUEUE_SIZE = 500;

    private final String botId;
    private final String secret;
    private final Integer scene;
    private final String plugVersion;
    private final String wsUrl;
    private final long reconnectBaseDelayMillis;
    private final int maxReconnectAttempts;
    private final int maxAuthFailureAttempts;
    private final long heartbeatIntervalMillis;
    private final long requestTimeoutMillis;
    private final int maxReplyQueueSize;
    private final WeComAiBotLogger logger;
    private final OkHttpClient httpClient;

    private WeComAiBotClientOptions(Builder builder) {
        this.botId = requireNonBlank(builder.botId, "botId");
        this.secret = requireNonBlank(builder.secret, "secret");
        this.scene = builder.scene;
        this.plugVersion = builder.plugVersion;
        this.wsUrl = builder.wsUrl == null || builder.wsUrl.isBlank() ? DEFAULT_WS_URL : builder.wsUrl;
        this.reconnectBaseDelayMillis = builder.reconnectBaseDelayMillis;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
        this.maxAuthFailureAttempts = builder.maxAuthFailureAttempts;
        this.heartbeatIntervalMillis = builder.heartbeatIntervalMillis;
        this.requestTimeoutMillis = builder.requestTimeoutMillis;
        this.maxReplyQueueSize = builder.maxReplyQueueSize;
        this.logger = builder.logger == null ? new DefaultWeComAiBotLogger() : builder.logger;
        this.httpClient = builder.httpClient;
    }

    public static Builder builder(String botId, String secret) {
        return new Builder(botId, secret);
    }

    public String getBotId() {
        return botId;
    }

    public String getSecret() {
        return secret;
    }

    public Integer getScene() {
        return scene;
    }

    public String getPlugVersion() {
        return plugVersion;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public long getReconnectBaseDelayMillis() {
        return reconnectBaseDelayMillis;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public int getMaxAuthFailureAttempts() {
        return maxAuthFailureAttempts;
    }

    public long getHeartbeatIntervalMillis() {
        return heartbeatIntervalMillis;
    }

    public long getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public int getMaxReplyQueueSize() {
        return maxReplyQueueSize;
    }

    public WeComAiBotLogger getLogger() {
        return logger;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    /**
     * 配置构建器。
     */
    public static final class Builder {

        private final String botId;
        private final String secret;
        private Integer scene;
        private String plugVersion;
        private String wsUrl = DEFAULT_WS_URL;
        private long reconnectBaseDelayMillis = DEFAULT_RECONNECT_BASE_DELAY_MILLIS;
        private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
        private int maxAuthFailureAttempts = DEFAULT_MAX_AUTH_FAILURE_ATTEMPTS;
        private long heartbeatIntervalMillis = DEFAULT_HEARTBEAT_INTERVAL_MILLIS;
        private long requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS;
        private int maxReplyQueueSize = DEFAULT_MAX_REPLY_QUEUE_SIZE;
        private WeComAiBotLogger logger;
        private OkHttpClient httpClient;

        private Builder(String botId, String secret) {
            this.botId = botId;
            this.secret = secret;
        }

        public Builder scene(Integer scene) {
            this.scene = scene;
            return this;
        }

        public Builder plugVersion(String plugVersion) {
            this.plugVersion = plugVersion;
            return this;
        }

        public Builder wsUrl(String wsUrl) {
            this.wsUrl = wsUrl;
            return this;
        }

        public Builder reconnectBaseDelayMillis(long reconnectBaseDelayMillis) {
            this.reconnectBaseDelayMillis = reconnectBaseDelayMillis;
            return this;
        }

        public Builder maxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
            return this;
        }

        public Builder maxAuthFailureAttempts(int maxAuthFailureAttempts) {
            this.maxAuthFailureAttempts = maxAuthFailureAttempts;
            return this;
        }

        public Builder heartbeatIntervalMillis(long heartbeatIntervalMillis) {
            this.heartbeatIntervalMillis = heartbeatIntervalMillis;
            return this;
        }

        public Builder requestTimeoutMillis(long requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        public Builder maxReplyQueueSize(int maxReplyQueueSize) {
            this.maxReplyQueueSize = maxReplyQueueSize;
            return this;
        }

        public Builder logger(WeComAiBotLogger logger) {
            this.logger = logger;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public WeComAiBotClientOptions build() {
            Objects.requireNonNull(botId, "botId");
            Objects.requireNonNull(secret, "secret");
            return new WeComAiBotClientOptions(this);
        }
    }
}
