package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventPayload {

    @JsonProperty("eventtype")
    private String eventType;

    @JsonProperty("event_key")
    private String eventKey;

    @JsonProperty("task_id")
    private String taskId;

    @JsonIgnore
    private final Map<String, JsonNode> extensions = new LinkedHashMap<>();

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }

    @JsonAnySetter
    public void putExtension(String key, JsonNode value) {
        if (!"eventtype".equals(key) && !"event_key".equals(key) && !"task_id".equals(key)) {
            extractBusinessFields(value);
            extensions.put(key, value);
        }
    }

    private void extractBusinessFields(JsonNode value) {
        if (value == null || !value.isObject()) {
            return;
        }
        if ((eventKey == null || eventKey.isBlank()) && value.hasNonNull("event_key")) {
            String nestedEventKey = value.path("event_key").asText();
            if (!nestedEventKey.isBlank()) {
                this.eventKey = nestedEventKey;
            }
        }
        if ((taskId == null || taskId.isBlank()) && value.hasNonNull("task_id")) {
            String nestedTaskId = value.path("task_id").asText();
            if (!nestedTaskId.isBlank()) {
                this.taskId = nestedTaskId;
            }
        }
    }
}
