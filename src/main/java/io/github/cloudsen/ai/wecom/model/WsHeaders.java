package io.github.cloudsen.ai.wecom.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket 帧头。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WsHeaders {

    @JsonProperty("req_id")
    private String reqId;

    @JsonIgnore
    private final Map<String, JsonNode> extensions = new LinkedHashMap<>();

    public WsHeaders() {
    }

    public WsHeaders(String reqId) {
        this.reqId = reqId;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    @JsonAnySetter
    public void putExtension(String key, JsonNode value) {
        if (!"req_id".equals(key)) {
            extensions.put(key, value);
        }
    }
}
