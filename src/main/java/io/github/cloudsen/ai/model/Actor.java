package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 消息或事件发送者。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Actor {

    private String userid;
    private String corpid;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getCorpid() {
        return corpid;
    }

    public void setCorpid(String corpid) {
        this.corpid = corpid;
    }
}
