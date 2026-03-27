package io.github.cloudsen.ai.wecom.support;

/**
 * WebSocket 命令常量。
 */
public final class WsCommand {

    public static final String SUBSCRIBE = "aibot_subscribe";
    public static final String HEARTBEAT = "ping";
    public static final String RESPONSE = "aibot_respond_msg";
    public static final String RESPONSE_WELCOME = "aibot_respond_welcome_msg";
    public static final String RESPONSE_UPDATE = "aibot_respond_update_msg";
    public static final String SEND_MSG = "aibot_send_msg";
    public static final String UPLOAD_MEDIA_INIT = "aibot_upload_media_init";
    public static final String UPLOAD_MEDIA_CHUNK = "aibot_upload_media_chunk";
    public static final String UPLOAD_MEDIA_FINISH = "aibot_upload_media_finish";
    public static final String CALLBACK = "aibot_msg_callback";
    public static final String EVENT_CALLBACK = "aibot_event_callback";

    private WsCommand() {
    }
}
