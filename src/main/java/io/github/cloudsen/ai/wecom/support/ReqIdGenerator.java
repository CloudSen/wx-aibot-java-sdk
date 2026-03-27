package io.github.cloudsen.ai.wecom.support;

import java.security.SecureRandom;

/**
 * req_id 生成器。
 */
public final class ReqIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private ReqIdGenerator() {
    }

    public static String generate(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + randomHex(8);
    }

    private static String randomHex(int length) {
        byte[] bytes = new byte[(length + 1) / 2];
        RANDOM.nextBytes(bytes);
        char[] chars = new char[length];
        for (int i = 0; i < chars.length; i++) {
            int value = bytes[i / 2];
            value = (i % 2 == 0) ? (value >> 4) : value;
            chars[i] = HEX[value & 0x0F];
        }
        return new String(chars);
    }
}
