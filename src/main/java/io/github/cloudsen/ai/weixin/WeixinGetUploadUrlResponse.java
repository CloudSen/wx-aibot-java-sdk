package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 获取 CDN 上传参数响应。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinGetUploadUrlResponse(
        @JsonProperty("upload_param") String uploadParam,
        @JsonProperty("thumb_upload_param") String thumbUploadParam
) {
}
