package io.github.cloudsen.ai.weixin;

/**
 * 微信协议调用异常。
 */
public class WeixinException extends RuntimeException {

    public WeixinException(String message) {
        super(message);
    }

    public WeixinException(String message, Throwable cause) {
        super(message, cause);
    }
}
