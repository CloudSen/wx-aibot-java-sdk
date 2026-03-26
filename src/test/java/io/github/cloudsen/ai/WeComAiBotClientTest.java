package io.github.cloudsen.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cloudsen.ai.WeComAiBotClient;
import io.github.cloudsen.ai.WeComAiBotClientOptions;
import io.github.cloudsen.ai.listener.WeComAiBotListener;
import io.github.cloudsen.ai.model.*;
import io.github.cloudsen.ai.support.JsonSupport;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WeComAiBotClientTest {

    private final MockWebServer server = new MockWebServer();
    private WeComAiBotClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        server.shutdown();
    }

    @Test
    void shouldAuthenticateAndDispatchTextMessage() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        CountDownLatch authenticatedLatch = new CountDownLatch(1);
        CountDownLatch textLatch = new CountDownLatch(1);
        BlockingQueue<String> textMessages = new LinkedBlockingQueue<>();

        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());
        client.addListener(new WeComAiBotListener() {
            @Override
            public void onAuthenticated() {
                authenticatedLatch.countDown();
            }

            @Override
            public void onTextMessage(WsFrame<BaseMessage> frame) {
                textMessages.add(frame.getBody().getText().getContent());
                textLatch.countDown();
            }
        });

        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertNotNull(authFrame);
        assertEquals("aibot_subscribe", authFrame.get("cmd").asText());
        assertEquals("bot-id", authFrame.path("body").path("bot_id").asText());
        assertEquals("bot-secret", authFrame.path("body").path("secret").asText());

        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);
        serverListener.sendTextCallback("callback-1", "@robot hello");

        assertTrue(authenticatedLatch.await(3, TimeUnit.SECONDS));
        assertTrue(textLatch.await(3, TimeUnit.SECONDS));
        assertEquals("@robot hello", textMessages.poll(1, TimeUnit.SECONDS));
    }

    @Test
    void shouldParseNestedTemplateCardEventBusinessFields() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        CountDownLatch templateCardEventLatch = new CountDownLatch(1);
        BlockingQueue<String> eventKeys = new LinkedBlockingQueue<>();
        BlockingQueue<String> taskIds = new LinkedBlockingQueue<>();

        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());
        client.addListener(new WeComAiBotListener() {
            @Override
            public void onTemplateCardEvent(WsFrame<EventMessage> frame) {
                eventKeys.add(frame.getBody().getEvent().getEventKey());
                taskIds.add(frame.getBody().getEvent().getTaskId());
                templateCardEventLatch.countDown();
            }
        });

        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);
        serverListener.sendTemplateCardEventCallback("callback-template-card-1", "task-1", "confirm");

        assertTrue(templateCardEventLatch.await(3, TimeUnit.SECONDS));
        assertEquals("confirm", eventKeys.poll(1, TimeUnit.SECONDS));
        assertEquals("task-1", taskIds.poll(1, TimeUnit.SECONDS));
    }

    @Test
    void shouldSerializeRepliesForSameReqId() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        CountDownLatch textLatch = new CountDownLatch(1);
        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());
        client.addListener(new WeComAiBotListener() {
            @Override
            public void onTextMessage(WsFrame<BaseMessage> frame) {
                client.replyStream(frame, "stream-1", "first", false);
                client.replyStream(frame, "stream-1", "second", true);
                textLatch.countDown();
            }
        });

        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);
        serverListener.sendTextCallback("callback-req-1", "queue test");
        assertTrue(textLatch.await(3, TimeUnit.SECONDS));

        JsonNode firstReply = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertNotNull(firstReply);
        assertEquals("aibot_respond_msg", firstReply.get("cmd").asText());
        assertEquals("first", firstReply.path("body").path("stream").path("content").asText());

        JsonNode maybeSecondReply = serverListener.receivedFrames.poll(300, TimeUnit.MILLISECONDS);
        assertEquals(null, maybeSecondReply);

        serverListener.ack("callback-req-1", null);
        JsonNode secondReply = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertNotNull(secondReply);
        assertEquals("second", secondReply.path("body").path("stream").path("content").asText());
        assertTrue(secondReply.path("body").path("stream").path("finish").asBoolean());
    }

    @Test
    void shouldUploadMediaByChunks() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());
        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);

        byte[] bytes = new byte[600 * 1024];
        Arrays.fill(bytes, (byte) 7);
        serverListener.enableUploadScenario();

        UploadMediaResult result = client.uploadMedia(bytes, new UploadMediaOptions(WeComMediaType.IMAGE, "demo.png"))
                .get(5, TimeUnit.SECONDS);

        assertEquals(WeComMediaType.IMAGE, result.type());
        assertEquals("media-1", result.mediaId());
        assertEquals(2, serverListener.chunkCount);
    }

    @Test
    void shouldRegisterCallbacksViaClientHelpers() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        CountDownLatch authenticatedLatch = new CountDownLatch(1);
        CountDownLatch textLatch = new CountDownLatch(1);
        BlockingQueue<String> textMessages = new LinkedBlockingQueue<>();
        AtomicInteger removedListenerCalls = new AtomicInteger();

        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());

        client.onAuthenticated(authenticatedLatch::countDown);
        WeComAiBotListener removable = client.onTextMessage(frame -> removedListenerCalls.incrementAndGet());
        client.removeListener(removable);
        client.onTextMessage(frame -> {
            textMessages.add(frame.getBody().getText().getContent());
            textLatch.countDown();
        });

        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);
        serverListener.sendTextCallback("callback-helper-1", "helper hello");

        assertTrue(authenticatedLatch.await(3, TimeUnit.SECONDS));
        assertTrue(textLatch.await(3, TimeUnit.SECONDS));
        assertEquals("helper hello", textMessages.poll(1, TimeUnit.SECONDS));
        assertEquals(0, removedListenerCalls.get());
    }

    @Test
    void shouldBuildConvenienceReplyAndSendBodies() throws Exception {
        TestServerSocketListener serverListener = new TestServerSocketListener();
        server.enqueue(new MockResponse().withWebSocketUpgrade(serverListener));
        server.start();

        client = new WeComAiBotClient(WeComAiBotClientOptions.builder("bot-id", "bot-secret")
                .wsUrl(server.url("/ws").toString().replace("http", "ws"))
                .requestTimeoutMillis(2_000)
                .build());
        client.connect();

        JsonNode authFrame = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        serverListener.ack(authFrame.path("headers").path("req_id").asText(), null);

        WsFrame<Void> callbackFrame = new WsFrame<>("aibot_msg_callback", new WsHeaders("callback-req-2"), null);

        CompletableFuture<WsFrame<JsonNode>> markdownFuture = client.replyMarkdown(
                callbackFrame, "**markdown**", new ReplyFeedback("markdown-feedback"));
        JsonNode markdownReply = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertEquals("aibot_respond_msg", markdownReply.path("cmd").asText());
        assertEquals("markdown", markdownReply.path("body").path("msgtype").asText());
        assertEquals("**markdown**", markdownReply.path("body").path("markdown").path("content").asText());
        assertEquals("markdown-feedback",
                markdownReply.path("body").path("markdown").path("feedback").path("id").asText());
        serverListener.ack("callback-req-2", null);
        markdownFuture.get(3, TimeUnit.SECONDS);

        CompletableFuture<WsFrame<JsonNode>> simpleTextFuture = client.replySimpleText(callbackFrame, "收到");
        JsonNode simpleTextReply = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertEquals("stream", simpleTextReply.path("body").path("msgtype").asText());
        assertEquals("收到", simpleTextReply.path("body").path("stream").path("content").asText());
        assertTrue(simpleTextReply.path("body").path("stream").path("finish").asBoolean());
        serverListener.ack("callback-req-2", null);
        simpleTextFuture.get(3, TimeUnit.SECONDS);

        CompletableFuture<WsFrame<JsonNode>> streamWithCardFuture = client.replyStreamWithCard(
                callbackFrame,
                "stream-card-1",
                "处理中",
                true,
                Map.of(
                        "card_type", "text_notice",
                        "main_title", Map.of("title", "处理状态")
                ),
                java.util.List.of(ReplyMsgItem.image("base64-data", "md5-value")),
                new ReplyFeedback("stream-feedback"),
                new ReplyFeedback("card-feedback")
        );
        JsonNode streamWithCardReply = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertEquals("stream_with_template_card", streamWithCardReply.path("body").path("msgtype").asText());
        assertEquals("stream-card-1", streamWithCardReply.path("body").path("stream").path("id").asText());
        assertEquals("处理中", streamWithCardReply.path("body").path("stream").path("content").asText());
        assertEquals("base64-data",
                streamWithCardReply.path("body").path("stream").path("msg_item").get(0).path("image").path("base64").asText());
        assertEquals("card-feedback",
                streamWithCardReply.path("body").path("template_card").path("feedback").path("id").asText());
        serverListener.ack("callback-req-2", null);
        streamWithCardFuture.get(3, TimeUnit.SECONDS);

        CompletableFuture<WsFrame<JsonNode>> sendTemplateCardFuture = client.sendTemplateCardMessage(
                "chat-1",
                Map.of(
                        "card_type", "text_notice",
                        "main_title", Map.of("title", "主动推送")
                )
        );
        JsonNode sendTemplateCard = serverListener.receivedFrames.poll(3, TimeUnit.SECONDS);
        assertEquals("aibot_send_msg", sendTemplateCard.path("cmd").asText());
        assertEquals("chat-1", sendTemplateCard.path("body").path("chatid").asText());
        assertEquals("template_card", sendTemplateCard.path("body").path("msgtype").asText());
        String sendReqId = sendTemplateCard.path("headers").path("req_id").asText();
        serverListener.ack(sendReqId, null);
        sendTemplateCardFuture.get(3, TimeUnit.SECONDS);
    }

    private static final class TestServerSocketListener extends WebSocketListener {

        private final BlockingQueue<JsonNode> receivedFrames = new LinkedBlockingQueue<>();
        private volatile WebSocket webSocket;
        private volatile boolean uploadScenario;
        private volatile int chunkCount;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            this.webSocket = webSocket;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JsonNode frame = JsonSupport.getObjectMapper().readTree(text);
                receivedFrames.add(frame);

                if (!uploadScenario) {
                    return;
                }

                String cmd = frame.path("cmd").asText();
                String reqId = frame.path("headers").path("req_id").asText();
                if ("aibot_upload_media_init".equals(cmd)) {
                    ack(reqId, Map.of("upload_id", "upload-1"));
                } else if ("aibot_upload_media_chunk".equals(cmd)) {
                    chunkCount++;
                    ack(reqId, null);
                } else if ("aibot_upload_media_finish".equals(cmd)) {
                    ack(reqId, Map.of("type", "image", "media_id", "media-1", "created_at", 1_770_000_000L));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void enableUploadScenario() {
            this.uploadScenario = true;
        }

        void sendTextCallback(String reqId, String content) throws Exception {
            String payload = JsonSupport.getObjectMapper().writeValueAsString(Map.of(
                    "cmd", "aibot_msg_callback",
                    "headers", Map.of("req_id", reqId),
                    "body", Map.of(
                            "msgid", "msg-1",
                            "aibotid", "bot-id",
                            "chatid", "chat-1",
                            "chattype", "group",
                            "from", Map.of("userid", "user-1"),
                            "msgtype", "text",
                            "text", Map.of("content", content)
                    )
            ));
            webSocket.send(payload);
        }

        void sendTemplateCardEventCallback(String reqId, String taskId, String eventKey) throws Exception {
            String payload = JsonSupport.getObjectMapper().writeValueAsString(Map.of(
                    "cmd", "aibot_event_callback",
                    "headers", Map.of("req_id", reqId),
                    "body", Map.of(
                            "msgid", "event-1",
                            "aibotid", "bot-id",
                            "chatid", "chat-1",
                            "chattype", "group",
                            "from", Map.of("userid", "user-1"),
                            "msgtype", "event",
                            "event", Map.of(
                                    "eventtype", "template_card_event",
                                    "template_card_event", Map.of(
                                            "card_type", "button_interaction",
                                            "task_id", taskId,
                                            "event_key", eventKey
                                    )
                            )
                    )
            ));
            webSocket.send(payload);
        }

        void ack(String reqId, Map<String, Object> body) throws Exception {
            Map<String, Object> payload = body == null
                    ? Map.of(
                    "headers", Map.of("req_id", reqId),
                    "errcode", 0,
                    "errmsg", "ok"
            )
                    : Map.of(
                    "headers", Map.of("req_id", reqId),
                    "body", body,
                    "errcode", 0,
                    "errmsg", "ok"
            );
            webSocket.send(JsonSupport.getObjectMapper().writeValueAsString(payload));
        }
    }
}
