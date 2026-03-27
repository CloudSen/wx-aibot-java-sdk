package io.github.cloudsen.ai.wecom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 流式回复结束时可附带的图文混排项。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplyMsgItem {

    @JsonProperty("msgtype")
    private String msgType;

    private Image image;

    public ReplyMsgItem() {
    }

    public ReplyMsgItem(String msgType, Image image) {
        this.msgType = msgType;
        this.image = image;
    }

    public static ReplyMsgItem image(String base64, String md5) {
        return new ReplyMsgItem("image", new Image(base64, md5));
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * 图片混排内容。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {

        private String base64;

        private String md5;

        public Image() {
        }

        public Image(String base64, String md5) {
            this.base64 = base64;
            this.md5 = md5;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }
}
