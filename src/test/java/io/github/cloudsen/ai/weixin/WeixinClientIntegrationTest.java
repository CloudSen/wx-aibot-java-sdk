package io.github.cloudsen.ai.weixin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实微信集成测试。
 *
 * <p>执行方式：</p>
 *
 * <pre>{@code
 * mvn -Dtest=WeixinClientIntegrationTest \
 *   -Dwx.weixin.it.enabled=true \
 *   test
 * }</pre>
 *
 * <p>可选参数：</p>
 * <ul>
 *     <li>{@code -Dwx.weixin.it.baseUrl=https://ilinkai.weixin.qq.com}</li>
 *     <li>{@code -Dwx.weixin.it.routeTag=...}</li>
 *     <li>{@code -Dwx.weixin.it.botType=3}</li>
 *     <li>{@code -Dwx.weixin.it.loginTimeoutSeconds=300}</li>
 *     <li>{@code -Dwx.weixin.it.waitSeconds=600}</li>
 *     <li>{@code -Dwx.weixin.it.requestTimeoutSeconds=15}</li>
 * </ul>
 *
 * <p>测试启动后会输出二维码链接。扫码成功后，向该微信账号发送消息即可触发回复。</p>
 * <p>发送 {@code 结束测试} / {@code end-test} / {@code endtest} 可结束测试。</p>
 * <p>发送 {@code 输入中} / {@code typing} 可触发 typing 接口验证。</p>
 */
@EnabledIfSystemProperty(named = "wx.weixin.it.enabled", matches = "true")
class WeixinClientIntegrationTest {

    @Test
    void shouldLoginByQrAndReplyUntilEndCommand() throws Exception {
        String configuredBaseUrl = readOptional("wx.weixin.it.baseUrl", "WEIXIN_IT_BASE_URL");
        String baseUrl = configuredBaseUrl == null ? WeixinClientOptions.DEFAULT_BASE_URL : configuredBaseUrl;
        String routeTag = readOptional("wx.weixin.it.routeTag", "WEIXIN_IT_ROUTE_TAG");
        String botType = readOptional("wx.weixin.it.botType", "WEIXIN_IT_BOT_TYPE");
        long loginTimeoutSeconds = readLong("wx.weixin.it.loginTimeoutSeconds", "WEIXIN_IT_LOGIN_TIMEOUT_SECONDS", 300L);
        long waitSeconds = readLong("wx.weixin.it.waitSeconds", "WEIXIN_IT_WAIT_SECONDS", 600L);
        long requestTimeoutMillis = TimeUnit.SECONDS.toMillis(
                readLong("wx.weixin.it.requestTimeoutSeconds", "WEIXIN_IT_REQUEST_TIMEOUT_SECONDS", 15L)
        );

        WeixinQrLoginStatusResult loginResult;
        try (WeixinClient loginClient = new WeixinClient(buildOptions(baseUrl, routeTag, botType, null, requestTimeoutMillis))) {
            System.out.println("[IT] Starting Weixin QR login...");
            WeixinQrLoginStartResult startResult = loginClient.startQrLogin();
            assertNotNull(startResult, "QR login start result must not be null");
            assertNotNull(startResult.qrcode(), "qrcode must not be null");
            assertNotNull(startResult.qrcodeUrl(), "qrcodeUrl must not be null");
            System.out.printf("[IT] QR code URL: %s%n", startResult.qrcodeUrl());
            System.out.printf("[IT] Scan the QR code within %d seconds.%n", loginTimeoutSeconds);

            loginResult = loginClient.waitForQrLogin(startResult.qrcode(), Duration.ofSeconds(loginTimeoutSeconds));
        }

        assertTrue(loginResult.confirmed(), "QR login must be confirmed");
        String token = requireNonBlank(loginResult.botToken(), "bot_token");
        String loginBaseUrl = loginResult.baseUrl() == null || loginResult.baseUrl().isBlank()
                ? baseUrl
                : loginResult.baseUrl();
        System.out.printf(
                "[IT] Login confirmed: accountId=%s, userId=%s, baseUrl=%s%n",
                loginResult.accountId(),
                loginResult.userId(),
                loginBaseUrl
        );
        System.out.printf("[IT] Waiting up to %d seconds. Send '结束测试' to finish.%n", waitSeconds);

        try (WeixinClient client = new WeixinClient(buildOptions(loginBaseUrl, routeTag, botType, token, requestTimeoutMillis))) {
            String cursor = "";
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(waitSeconds);
            Set<String> handledMessageKeys = new HashSet<>();

            while (System.currentTimeMillis() < deadline) {
                WeixinGetUpdatesResponse response = client.getUpdates(cursor);
                assertTrue(response.success(), () -> "getUpdates failed: ret=" + response.ret()
                        + ", errcode=" + response.errcode()
                        + ", errmsg=" + response.errmsg());

                if (response.getUpdatesBuf() != null) {
                    cursor = response.getUpdatesBuf();
                }

                List<WeixinMessage> messages = response.msgs() == null ? List.of() : response.msgs();
                for (WeixinMessage message : messages) {
                    String key = messageKey(message);
                    if (!handledMessageKeys.add(key)) {
                        continue;
                    }
                    if (!isInboundUserMessage(message)) {
                        continue;
                    }

                    String text = safeText(message);
                    System.out.printf(
                            "[IT] Received message: key=%s, from=%s, text=%s, contextToken=%s%n",
                            key,
                            message.fromUserId(),
                            text,
                            message.contextToken() == null ? "<none>" : "<present>"
                    );

                    String normalized = normalize(text);
                    if (matches(normalized, "结束测试", "end-test", "endtest")) {
                        client.sendTextMessage(message.fromUserId(), "已收到：结束测试，Weixin 集成测试退出", message.contextToken());
                        System.out.println("[IT] Finish command received, integration test completed.");
                        return;
                    }

                    if (matches(normalized, "输入中", "typing")) {
                        trySendTyping(client, message);
                        client.sendTextMessage(message.fromUserId(), "已收到：输入中测试", message.contextToken());
                        continue;
                    }

                    client.sendTextMessage(message.fromUserId(), buildReplyText(message, text), message.contextToken());
                }
            }
        }

        fail("Weixin integration test timed out. Send '结束测试' before waitSeconds expires.");
    }

    private static WeixinClientOptions buildOptions(String baseUrl,
                                                    String routeTag,
                                                    String botType,
                                                    String token,
                                                    long requestTimeoutMillis) {
        WeixinClientOptions.Builder builder = WeixinClientOptions.builder()
                .baseUrl(baseUrl)
                .apiTimeoutMillis(requestTimeoutMillis)
                .configTimeoutMillis(requestTimeoutMillis)
                .qrPollTimeoutMillis(Math.max(requestTimeoutMillis, 35_000L))
                .longPollTimeoutMillis(35_000L);
        if (routeTag != null) {
            builder.routeTag(routeTag);
        }
        if (botType != null) {
            builder.botType(botType);
        }
        if (token != null) {
            builder.token(token);
        }
        return builder.build();
    }

    private static void trySendTyping(WeixinClient client, WeixinMessage message) {
        try {
            WeixinGetConfigResponse config = client.getConfig(message.fromUserId(), message.contextToken());
            if (!config.success() || config.typingTicket() == null || config.typingTicket().isBlank()) {
                System.out.printf("[IT] Skip typing because typing_ticket unavailable. ret=%s, errmsg=%s%n",
                        config.ret(), config.errmsg());
                return;
            }
            client.sendTyping(message.fromUserId(), config.typingTicket(), WeixinTypingStatus.TYPING);
            Thread.sleep(1000L);
            client.sendTyping(message.fromUserId(), config.typingTicket(), WeixinTypingStatus.CANCEL);
            System.out.println("[IT] Typing API invoked successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Typing wait interrupted", e);
        } catch (Exception e) {
            System.out.printf("[IT] Typing API failed: %s%n", e.getMessage());
        }
    }

    private static boolean isInboundUserMessage(WeixinMessage message) {
        return message != null
                && message.fromUserId() != null
                && !message.fromUserId().isBlank()
                && Integer.valueOf(WeixinMessage.MESSAGE_TYPE_USER).equals(message.messageType());
    }

    private static String safeText(WeixinMessage message) {
        if (message == null || message.itemList() == null) {
            return "";
        }
        for (WeixinMessage.Item item : message.itemList()) {
            if (item == null) {
                continue;
            }
            if (Integer.valueOf(WeixinMessage.Item.TYPE_TEXT).equals(item.type())
                    && item.textItem() != null
                    && item.textItem().text() != null) {
                return item.textItem().text();
            }
            if (Integer.valueOf(WeixinMessage.Item.TYPE_VOICE).equals(item.type())
                    && item.voiceItem() != null
                    && item.voiceItem().text() != null) {
                return item.voiceItem().text();
            }
        }
        return "";
    }

    private static String buildReplyText(WeixinMessage message, String text) {
        String trimmed = text == null ? "" : text.trim();
        if (!trimmed.isBlank()) {
            return "已收到：" + abbreviate(trimmed, 120);
        }
        if (message == null || message.itemList() == null || message.itemList().isEmpty()) {
            return "已收到：其他消息";
        }
        WeixinMessage.Item first = message.itemList().get(0);
        if (Integer.valueOf(WeixinMessage.Item.TYPE_IMAGE).equals(first.type())) {
            return "已收到：图片消息";
        }
        if (Integer.valueOf(WeixinMessage.Item.TYPE_FILE).equals(first.type())) {
            return "已收到：文件消息";
        }
        if (Integer.valueOf(WeixinMessage.Item.TYPE_VIDEO).equals(first.type())) {
            return "已收到：视频消息";
        }
        if (Integer.valueOf(WeixinMessage.Item.TYPE_VOICE).equals(first.type())) {
            return "已收到：语音消息";
        }
        return "已收到：其他消息";
    }

    private static String abbreviate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static String messageKey(WeixinMessage message) {
        if (message.messageId() != null) {
            return "mid:" + message.messageId();
        }
        if (message.seq() != null) {
            return "seq:" + message.seq();
        }
        if (message.clientId() != null && !message.clientId().isBlank()) {
            return "cid:" + message.clientId();
        }
        return "fallback:" + message.fromUserId() + ":" + message.createTimeMs() + ":" + safeText(message);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String normalized, String... keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String readOptional(String propertyName, String envName) {
        String fromProperty = System.getProperty(propertyName);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return null;
    }

    private static long readLong(String propertyName, String envName, long defaultValue) {
        String value = readOptional(propertyName, envName);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        return value;
    }
}
