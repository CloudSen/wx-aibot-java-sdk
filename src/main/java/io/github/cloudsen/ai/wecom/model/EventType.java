package io.github.cloudsen.ai.wecom.model;

/**
 * 回调事件类型。
 */
public enum EventType {
    ENTER_CHAT("enter_chat"),
    TEMPLATE_CARD_EVENT("template_card_event"),
    FEEDBACK_EVENT("feedback_event"),
    DISCONNECTED_EVENT("disconnected_event");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
