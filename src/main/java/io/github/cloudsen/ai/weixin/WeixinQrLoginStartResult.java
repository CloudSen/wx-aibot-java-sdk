package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 二维码登录启动结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinQrLoginStartResult(
        @JsonProperty("qrcode") String qrcode,
        @JsonProperty("qrcode_img_content") String qrcodeUrl
) {
}
