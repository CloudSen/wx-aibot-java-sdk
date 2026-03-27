package io.github.cloudsen.ai.weixin;

/**
 * 上传媒体类型。
 */
public enum WeixinUploadMediaType {

    IMAGE(1),
    VIDEO(2),
    FILE(3),
    VOICE(4);

    private final int code;

    WeixinUploadMediaType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
