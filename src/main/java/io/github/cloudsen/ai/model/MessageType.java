package io.github.cloudsen.ai.model;

/**
 * 回调消息类型。
 */
public enum MessageType {
    TEXT("text"),
    IMAGE("image"),
    MIXED("mixed"),
    VOICE("voice"),
    FILE("file"),
    VIDEO("video");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
