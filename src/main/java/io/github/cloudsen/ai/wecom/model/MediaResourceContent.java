package io.github.cloudsen.ai.wecom.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图片、文件、视频等资源内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaResourceContent {

    private String url;

    @JsonProperty("aeskey")
    private String aesKey;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
