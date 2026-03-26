package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 图文混排子项。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MixedItem {

    private String msgtype;
    private TextContent text;
    private MediaResourceContent image;

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public TextContent getText() {
        return text;
    }

    public void setText(TextContent text) {
        this.text = text;
    }

    public MediaResourceContent getImage() {
        return image;
    }

    public void setImage(MediaResourceContent image) {
        this.image = image;
    }
}
