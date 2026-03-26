package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用消息体。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseMessage {

    @JsonProperty("msgid")
    private String msgId;

    @JsonProperty("aibotid")
    private String aiBotId;

    @JsonProperty("chatid")
    private String chatId;

    @JsonProperty("chattype")
    private String chatType;

    private Actor from;

    @JsonProperty("create_time")
    private Long createTime;

    @JsonProperty("response_url")
    private String responseUrl;

    @JsonProperty("msgtype")
    private String msgType;

    private QuoteContent quote;
    private TextContent text;
    private MediaResourceContent image;
    private MixedContent mixed;
    private VoiceContent voice;
    private MediaResourceContent file;
    private MediaResourceContent video;

    @JsonIgnore
    private final Map<String, JsonNode> extensions = new LinkedHashMap<>();

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getResponseUrl() {
        return responseUrl;
    }

    public void setResponseUrl(String responseUrl) {
        this.responseUrl = responseUrl;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public QuoteContent getQuote() {
        return quote;
    }

    public void setQuote(QuoteContent quote) {
        this.quote = quote;
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

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    @JsonAnySetter
    public void putExtension(String key, JsonNode value) {
        if (!isKnownField(key)) {
            extensions.put(key, value);
        }
    }

    private boolean isKnownField(String key) {
        return "msgid".equals(key)
                || "aibotid".equals(key)
                || "chatid".equals(key)
                || "chattype".equals(key)
                || "from".equals(key)
                || "create_time".equals(key)
                || "response_url".equals(key)
                || "msgtype".equals(key)
                || "quote".equals(key)
                || "text".equals(key)
                || "image".equals(key)
                || "mixed".equals(key)
                || "voice".equals(key)
                || "file".equals(key)
                || "video".equals(key);
    }
}
