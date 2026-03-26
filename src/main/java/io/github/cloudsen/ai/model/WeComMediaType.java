package io.github.cloudsen.ai.model;

/**
 * 支持的媒体类型。
 */
public enum WeComMediaType {
    FILE("file"),
    IMAGE("image"),
    VOICE("voice"),
    VIDEO("video");

    private final String value;

    WeComMediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
