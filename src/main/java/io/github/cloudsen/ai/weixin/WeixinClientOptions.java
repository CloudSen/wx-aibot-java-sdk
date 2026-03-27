package io.github.cloudsen.ai.weixin;

import okhttp3.OkHttpClient;

import java.util.Objects;

/**
 * 微信协议客户端配置。
 */
public final class WeixinClientOptions {

    public static final String DEFAULT_BASE_URL = "https://ilinkai.weixin.qq.com";
    public static final String DEFAULT_CDN_BASE_URL = "https://novac2c.cdn.weixin.qq.com/c2c";
    public static final String DEFAULT_BOT_TYPE = "3";
    public static final long DEFAULT_API_TIMEOUT_MILLIS = 15_000L;
    public static final long DEFAULT_LONG_POLL_TIMEOUT_MILLIS = 35_000L;
    public static final long DEFAULT_CONFIG_TIMEOUT_MILLIS = 10_000L;
    public static final long DEFAULT_QR_POLL_TIMEOUT_MILLIS = 35_000L;

    private final String baseUrl;
    private final String cdnBaseUrl;
    private final String token;
    private final String routeTag;
    private final String channelVersion;
    private final String botType;
    private final long apiTimeoutMillis;
    private final long longPollTimeoutMillis;
    private final long configTimeoutMillis;
    private final long qrPollTimeoutMillis;
    private final OkHttpClient httpClient;

    private WeixinClientOptions(Builder builder) {
        this.baseUrl = normalizeRequired(builder.baseUrl, "baseUrl");
        this.cdnBaseUrl = normalizeRequired(builder.cdnBaseUrl, "cdnBaseUrl");
        this.token = normalizeOptional(builder.token);
        this.routeTag = normalizeOptional(builder.routeTag);
        this.channelVersion = normalizeRequired(builder.channelVersion, "channelVersion");
        this.botType = normalizeRequired(builder.botType, "botType");
        this.apiTimeoutMillis = builder.apiTimeoutMillis;
        this.longPollTimeoutMillis = builder.longPollTimeoutMillis;
        this.configTimeoutMillis = builder.configTimeoutMillis;
        this.qrPollTimeoutMillis = builder.qrPollTimeoutMillis;
        this.httpClient = builder.httpClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCdnBaseUrl() {
        return cdnBaseUrl;
    }

    public String getToken() {
        return token;
    }

    public String getRouteTag() {
        return routeTag;
    }

    public String getChannelVersion() {
        return channelVersion;
    }

    public String getBotType() {
        return botType;
    }

    public long getApiTimeoutMillis() {
        return apiTimeoutMillis;
    }

    public long getLongPollTimeoutMillis() {
        return longPollTimeoutMillis;
    }

    public long getConfigTimeoutMillis() {
        return configTimeoutMillis;
    }

    public long getQrPollTimeoutMillis() {
        return qrPollTimeoutMillis;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static final class Builder {

        private String baseUrl = DEFAULT_BASE_URL;
        private String cdnBaseUrl = DEFAULT_CDN_BASE_URL;
        private String token;
        private String routeTag;
        private String channelVersion = resolveDefaultChannelVersion();
        private String botType = DEFAULT_BOT_TYPE;
        private long apiTimeoutMillis = DEFAULT_API_TIMEOUT_MILLIS;
        private long longPollTimeoutMillis = DEFAULT_LONG_POLL_TIMEOUT_MILLIS;
        private long configTimeoutMillis = DEFAULT_CONFIG_TIMEOUT_MILLIS;
        private long qrPollTimeoutMillis = DEFAULT_QR_POLL_TIMEOUT_MILLIS;
        private OkHttpClient httpClient;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder cdnBaseUrl(String cdnBaseUrl) {
            this.cdnBaseUrl = cdnBaseUrl;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder routeTag(String routeTag) {
            this.routeTag = routeTag;
            return this;
        }

        public Builder channelVersion(String channelVersion) {
            this.channelVersion = channelVersion;
            return this;
        }

        public Builder botType(String botType) {
            this.botType = botType;
            return this;
        }

        public Builder apiTimeoutMillis(long apiTimeoutMillis) {
            this.apiTimeoutMillis = apiTimeoutMillis;
            return this;
        }

        public Builder longPollTimeoutMillis(long longPollTimeoutMillis) {
            this.longPollTimeoutMillis = longPollTimeoutMillis;
            return this;
        }

        public Builder configTimeoutMillis(long configTimeoutMillis) {
            this.configTimeoutMillis = configTimeoutMillis;
            return this;
        }

        public Builder qrPollTimeoutMillis(long qrPollTimeoutMillis) {
            this.qrPollTimeoutMillis = qrPollTimeoutMillis;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public WeixinClientOptions build() {
            Objects.requireNonNull(baseUrl, "baseUrl");
            Objects.requireNonNull(cdnBaseUrl, "cdnBaseUrl");
            return new WeixinClientOptions(this);
        }

        private static String resolveDefaultChannelVersion() {
            Package pkg = WeixinClientOptions.class.getPackage();
            String version = pkg == null ? null : pkg.getImplementationVersion();
            return version == null || version.isBlank() ? "java-sdk" : version;
        }
    }
}
