package io.github.cloudsen.ai.weixin;

/**
 * 输入状态。
 */
public enum WeixinTypingStatus {

    TYPING(1),
    CANCEL(2);

    private final int code;

    WeixinTypingStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
