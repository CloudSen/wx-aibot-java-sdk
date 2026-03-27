package io.github.cloudsen.ai.wecom.model;

/**
 * 主动发送消息的会话类型。
 */
public enum ChatType {
    SINGLE(1),
    GROUP(2);

    private final int code;

    ChatType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
