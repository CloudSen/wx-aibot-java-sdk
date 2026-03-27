package io.github.cloudsen.ai.weixin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 微信 CDN AES-128-ECB 工具。
 */
public final class WeixinCdnCrypto {

    private WeixinCdnCrypto() {
    }

    public static byte[] encrypt(byte[] plaintext, byte[] aesKey) {
        return transform(Cipher.ENCRYPT_MODE, plaintext, aesKey);
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] aesKey) {
        return transform(Cipher.DECRYPT_MODE, ciphertext, aesKey);
    }

    public static int paddedSize(int plaintextSize) {
        return ((plaintextSize / 16) + 1) * 16;
    }

    public static byte[] parseAesKey(String aesKeyBase64) {
        byte[] decoded = Base64.getDecoder().decode(aesKeyBase64);
        if (decoded.length == 16) {
            return decoded;
        }
        if (decoded.length == 32) {
            String ascii = new String(decoded, StandardCharsets.US_ASCII);
            if (ascii.matches("[0-9a-fA-F]{32}")) {
                return hexToBytes(ascii);
            }
        }
        throw new WeixinException("aes_key 解析失败，既不是 16 字节原始 key，也不是 32 位 hex 文本");
    }

    public static String rawHexKeyToBase64(String hexKey) {
        return Base64.getEncoder().encodeToString(hexToBytes(hexKey));
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new WeixinException("非法 hex 字符串");
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return result;
    }

    private static byte[] transform(int mode, byte[] input, byte[] aesKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(mode, new SecretKeySpec(aesKey, "AES"));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new WeixinException("微信 CDN AES-128-ECB 处理失败", e);
        }
    }
}
