package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * getUpdates 响应。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinGetUpdatesResponse(
        @JsonProperty("ret") Integer ret,
        @JsonProperty("errcode") Integer errcode,
        @JsonProperty("errmsg") String errmsg,
        @JsonProperty("msgs") List<WeixinMessage> msgs,
        @JsonProperty("get_updates_buf") String getUpdatesBuf,
        @JsonProperty("longpolling_timeout_ms") Long longpollingTimeoutMs
) {

    public boolean success() {
        return (ret == null || ret == 0) && (errcode == null || errcode == 0);
    }
}
