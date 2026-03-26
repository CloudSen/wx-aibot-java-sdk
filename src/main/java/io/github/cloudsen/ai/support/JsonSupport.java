package io.github.cloudsen.ai.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * SDK JSON 工具。
 */
public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonSupport() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
