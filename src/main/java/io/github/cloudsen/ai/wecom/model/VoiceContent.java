package io.github.cloudsen.ai.wecom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 语音内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoiceContent {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
