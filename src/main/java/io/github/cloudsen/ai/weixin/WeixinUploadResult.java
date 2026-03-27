package io.github.cloudsen.ai.weixin;

/**
 * CDN 上传结果。
 */
public record WeixinUploadResult(
        String filekey,
        String downloadEncryptedQueryParam,
        String aesKeyHex,
        long fileSize,
        long fileSizeCiphertext
) {

    public String aesKeyBase64() {
        return WeixinCdnCrypto.rawHexKeyToBase64(aesKeyHex);
    }
}
