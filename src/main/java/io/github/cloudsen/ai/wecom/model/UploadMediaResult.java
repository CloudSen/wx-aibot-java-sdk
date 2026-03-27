package io.github.cloudsen.ai.wecom.model;

/**
 * 上传临时素材结果。
 */
public record UploadMediaResult(WeComMediaType type, String mediaId, Long createdAt) {
}
