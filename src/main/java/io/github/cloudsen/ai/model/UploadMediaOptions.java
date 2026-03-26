package io.github.cloudsen.ai.model;

import java.util.Objects;

/**
 * 上传临时素材参数。
 */
public class UploadMediaOptions {

    private final WeComMediaType type;
    private final String filename;

    public UploadMediaOptions(WeComMediaType type, String filename) {
        this.type = Objects.requireNonNull(type, "type");
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        this.filename = filename;
    }

    public WeComMediaType getType() {
        return type;
    }

    public String getFilename() {
        return filename;
    }
}
