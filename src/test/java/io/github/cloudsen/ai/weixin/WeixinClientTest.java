package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cloudsen.ai.common.JsonSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WeixinClientTest {

    private final MockWebServer apiServer = new MockWebServer();
    private final MockWebServer cdnServer = new MockWebServer();
    private WeixinClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        apiServer.shutdown();
        cdnServer.shutdown();
    }

    @Test
    void shouldSendTextMessageWithProtocolHeadersAndBody() throws Exception {
        apiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        apiServer.start();

        client = new WeixinClient(WeixinClientOptions.builder()
                .baseUrl(apiServer.url("/").toString())
                .token("bot-token")
                .routeTag("route-1")
                .channelVersion("2.0.1")
                .build());

        String messageId = client.sendTextMessage("user@im.wechat", "你好，微信", "ctx-1");
        assertNotNull(messageId);

        RecordedRequest request = apiServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/ilink/bot/sendmessage", request.getPath());
        assertEquals("ilink_bot_token", request.getHeader("AuthorizationType"));
        assertEquals("Bearer bot-token", request.getHeader("Authorization"));
        assertEquals("route-1", request.getHeader("SKRouteTag"));

        String uinRaw = new String(Base64.getDecoder().decode(request.getHeader("X-WECHAT-UIN")), StandardCharsets.UTF_8);
        assertTrue(uinRaw.matches("\\d+"));

        JsonNode payload = JsonSupport.getObjectMapper().readTree(request.getBody().readUtf8());
        assertEquals("2.0.1", payload.path("base_info").path("channel_version").asText());
        assertEquals("user@im.wechat", payload.path("msg").path("to_user_id").asText());
        assertEquals("ctx-1", payload.path("msg").path("context_token").asText());
        assertEquals(2, payload.path("msg").path("message_type").asInt());
        assertEquals(2, payload.path("msg").path("message_state").asInt());
        assertEquals(1, payload.path("msg").path("item_list").size());
        assertEquals(1, payload.path("msg").path("item_list").get(0).path("type").asInt());
        assertEquals("你好，微信", payload.path("msg").path("item_list").get(0).path("text_item").path("text").asText());
    }

    @Test
    void shouldStartAndConfirmQrLogin() throws Exception {
        apiServer.enqueue(new MockResponse().setResponseCode(200).setBody("""
                {"qrcode":"qr-token","qrcode_img_content":"https://example.com/qr.png"}
                """));
        apiServer.enqueue(new MockResponse().setResponseCode(200).setBody("""
                {"status":"wait"}
                """));
        apiServer.enqueue(new MockResponse().setResponseCode(200).setBody("""
                {"status":"confirmed","bot_token":"bot-token","ilink_bot_id":"bot-id","baseurl":"https://ilinkai.weixin.qq.com","ilink_user_id":"user@im.wechat"}
                """));
        apiServer.start();

        client = new WeixinClient(WeixinClientOptions.builder()
                .baseUrl(apiServer.url("/").toString())
                .build());

        WeixinQrLoginStartResult start = client.startQrLogin();
        assertEquals("qr-token", start.qrcode());
        assertEquals("https://example.com/qr.png", start.qrcodeUrl());

        WeixinQrLoginStatusResult status = client.waitForQrLogin("qr-token", Duration.ofSeconds(3), Duration.ofMillis(10));
        assertTrue(status.confirmed());
        assertEquals("bot-token", status.botToken());
        assertEquals("bot-id", status.accountId());
        assertEquals("user@im.wechat", status.userId());

        RecordedRequest startRequest = apiServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(startRequest);
        assertEquals("GET", startRequest.getMethod());
        assertEquals("/ilink/bot/get_bot_qrcode?bot_type=3", startRequest.getPath());

        RecordedRequest pollRequest1 = apiServer.takeRequest(3, TimeUnit.SECONDS);
        RecordedRequest pollRequest2 = apiServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(pollRequest1);
        assertNotNull(pollRequest2);
        assertEquals("GET", pollRequest1.getMethod());
        assertEquals("1", pollRequest1.getHeader("iLink-App-ClientVersion"));
        assertTrue(pollRequest1.getPath().startsWith("/ilink/bot/get_qrcode_status?qrcode=qr-token"));
        assertTrue(pollRequest2.getPath().startsWith("/ilink/bot/get_qrcode_status?qrcode=qr-token"));
    }

    @Test
    void shouldReturnEmptyUpdatesOnClientTimeout() throws Exception {
        apiServer.enqueue(new MockResponse()
                .setHeadersDelay(2, TimeUnit.SECONDS)
                .setResponseCode(200)
                .setBody("{}"));
        apiServer.start();

        client = new WeixinClient(WeixinClientOptions.builder()
                .baseUrl(apiServer.url("/").toString())
                .token("bot-token")
                .longPollTimeoutMillis(150)
                .build());

        WeixinGetUpdatesResponse response = client.getUpdates("cursor-1");
        assertTrue(response.success());
        assertEquals("cursor-1", response.getUpdatesBuf());
        assertEquals(List.of(), response.msgs());
    }

    @Test
    void shouldUploadMediaAndDecryptDownloadedPayload(@TempDir Path tempDir) throws Exception {
        byte[] plaintext = "hello weixin upload".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("demo.png");
        Files.write(file, plaintext);

        apiServer.enqueue(new MockResponse().setResponseCode(200).setBody("""
                {"upload_param":"upload-param"}
                """));
        apiServer.start();

        cdnServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("x-encrypted-param", "download-param")
                .setBody(""));
        cdnServer.start();

        client = new WeixinClient(WeixinClientOptions.builder()
                .baseUrl(apiServer.url("/").toString())
                .cdnBaseUrl(cdnServer.url("/c2c/").toString())
                .token("bot-token")
                .build());

        WeixinUploadResult uploadResult = client.uploadImage(file, "user@im.wechat");
        assertEquals("download-param", uploadResult.downloadEncryptedQueryParam());
        assertEquals(plaintext.length, uploadResult.fileSize());

        RecordedRequest uploadUrlRequest = apiServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(uploadUrlRequest);
        assertEquals("/ilink/bot/getuploadurl", uploadUrlRequest.getPath());

        JsonNode uploadBody = JsonSupport.getObjectMapper().readTree(uploadUrlRequest.getBody().readUtf8());
        assertEquals(1, uploadBody.path("media_type").asInt());
        assertEquals("user@im.wechat", uploadBody.path("to_user_id").asText());
        assertTrue(uploadBody.path("no_need_thumb").asBoolean());
        assertEquals(32, uploadBody.path("aeskey").asText().length());

        RecordedRequest cdnUploadRequest = cdnServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(cdnUploadRequest);
        assertEquals("POST", cdnUploadRequest.getMethod());
        assertTrue(cdnUploadRequest.getPath().startsWith("/c2c/upload?encrypted_query_param=upload-param"));

        byte[] encryptedUpload = cdnUploadRequest.getBody().readByteArray();
        byte[] decryptedUpload = WeixinCdnCrypto.decrypt(encryptedUpload, WeixinCdnCrypto.hexToBytes(uploadResult.aesKeyHex()));
        assertArrayEquals(plaintext, decryptedUpload);

        byte[] encryptedDownload = WeixinCdnCrypto.encrypt(plaintext, WeixinCdnCrypto.hexToBytes(uploadResult.aesKeyHex()));
        cdnServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(new okio.Buffer().write(encryptedDownload)));

        byte[] downloaded = client.downloadCdnMedia("download-param", uploadResult.aesKeyBase64());
        assertArrayEquals(plaintext, downloaded);

        RecordedRequest cdnDownloadRequest = cdnServer.takeRequest(3, TimeUnit.SECONDS);
        assertNotNull(cdnDownloadRequest);
        assertEquals("GET", cdnDownloadRequest.getMethod());
        assertEquals("/c2c/download?encrypted_query_param=download-param", cdnDownloadRequest.getPath());
    }
}
