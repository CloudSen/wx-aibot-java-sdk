package io.github.cloudsen.ai.wecom.model;

/**
 * 消息反馈配置。
 */
public class ReplyFeedback {

    private String id;

    public ReplyFeedback() {
    }

    public ReplyFeedback(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
