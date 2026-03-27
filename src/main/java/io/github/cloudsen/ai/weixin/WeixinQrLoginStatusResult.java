package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 二维码登录状态结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinQrLoginStatusResult(
        @JsonProperty("status") String status,
        @JsonProperty("bot_token") String botToken,
        @JsonProperty("ilink_bot_id") String accountId,
        @JsonProperty("baseurl") String baseUrl,
        @JsonProperty("ilink_user_id") String userId
) {

    public boolean confirmed() {
        return "confirmed".equalsIgnoreCase(status);
    }

    public boolean expired() {
        return "expired".equalsIgnoreCase(status);
    }
}
