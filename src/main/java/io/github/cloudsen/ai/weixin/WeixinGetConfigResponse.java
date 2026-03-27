package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * getConfig 响应。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinGetConfigResponse(
        @JsonProperty("ret") Integer ret,
        @JsonProperty("errmsg") String errmsg,
        @JsonProperty("typing_ticket") String typingTicket
) {

    public boolean success() {
        return ret == null || ret == 0;
    }
}
