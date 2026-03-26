package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 引用消息内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteContent {

    private String msgtype;
    private TextContent text;
    private MediaResourceContent image;
    private MixedContent mixed;
    private VoiceContent voice;
    private MediaResourceContent file;
    private MediaResourceContent video;

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

    public MixedContent getMixed() {
        return mixed;
    }

    public void setMixed(MixedContent mixed) {
        this.mixed = mixed;
    }

    public VoiceContent getVoice() {
        return voice;
    }

    public void setVoice(VoiceContent voice) {
        this.voice = voice;
    }

    public MediaResourceContent getFile() {
        return file;
    }

    public void setFile(MediaResourceContent file) {
        this.file = file;
    }

    public MediaResourceContent getVideo() {
        return video;
    }

    public void setVideo(MediaResourceContent video) {
        this.video = video;
    }
}
