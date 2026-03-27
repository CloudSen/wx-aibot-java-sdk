package io.github.cloudsen.ai.wecom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 事件消息。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMessage {

    @JsonProperty("msgid")
    private String msgId;

    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("aibotid")
    private String aiBotId;

    @JsonProperty("chatid")
    private String chatId;

    @JsonProperty("chattype")
    private String chatType;

    private Actor from;

    @JsonProperty("msgtype")
    private String msgType;

    private EventPayload event;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getAiBotId() {
        return aiBotId;
    }

    public void setAiBotId(String aiBotId) {
        this.aiBotId = aiBotId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getChatType() {
        return chatType;
    }

    public void setChatType(String chatType) {
        this.chatType = chatType;
    }

    public Actor getFrom() {
        return from;
    }

    public void setFrom(Actor from) {
        this.from = from;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public EventPayload getEvent() {
        return event;
    }

    public void setEvent(EventPayload event) {
        this.event = event;
    }
}
