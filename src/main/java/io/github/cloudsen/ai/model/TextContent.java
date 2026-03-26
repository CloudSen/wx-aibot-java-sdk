package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 文本内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextContent {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
