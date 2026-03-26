package io.github.cloudsen.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 图文混排内容。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MixedContent {

    @JsonProperty("msg_item")
    private List<MixedItem> msgItems;

    public List<MixedItem> getMsgItems() {
        return msgItems;
    }

    public void setMsgItems(List<MixedItem> msgItems) {
        this.msgItems = msgItems;
    }
}
