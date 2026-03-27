package io.github.cloudsen.ai.weixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 微信消息协议模型。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeixinMessage(
        @JsonProperty("seq") Long seq,
        @JsonProperty("message_id") Long messageId,
        @JsonProperty("from_user_id") String fromUserId,
        @JsonProperty("to_user_id") String toUserId,
        @JsonProperty("client_id") String clientId,
        @JsonProperty("create_time_ms") Long createTimeMs,
        @JsonProperty("update_time_ms") Long updateTimeMs,
        @JsonProperty("delete_time_ms") Long deleteTimeMs,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("group_id") String groupId,
        @JsonProperty("message_type") Integer messageType,
        @JsonProperty("message_state") Integer messageState,
        @JsonProperty("item_list") List<Item> itemList,
        @JsonProperty("context_token") String contextToken
) {

    public static final int MESSAGE_TYPE_USER = 1;
    public static final int MESSAGE_TYPE_BOT = 2;
    public static final int MESSAGE_STATE_NEW = 0;
    public static final int MESSAGE_STATE_GENERATING = 1;
    public static final int MESSAGE_STATE_FINISH = 2;

    public static WeixinMessage text(String toUserId, String text, String contextToken) {
        return new WeixinMessage(
                null,
                null,
                null,
                toUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                MESSAGE_TYPE_BOT,
                MESSAGE_STATE_FINISH,
                List.of(Item.text(text)),
                contextToken
        );
    }

    public WeixinMessage withClientId(String newClientId) {
        return new WeixinMessage(
                seq,
                messageId,
                fromUserId,
                toUserId,
                newClientId,
                createTimeMs,
                updateTimeMs,
                deleteTimeMs,
                sessionId,
                groupId,
                messageType,
                messageState,
                itemList,
                contextToken
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("type") Integer type,
            @JsonProperty("create_time_ms") Long createTimeMs,
            @JsonProperty("update_time_ms") Long updateTimeMs,
            @JsonProperty("is_completed") Boolean completed,
            @JsonProperty("msg_id") String msgId,
            @JsonProperty("ref_msg") RefMessage refMsg,
            @JsonProperty("text_item") TextItem textItem,
            @JsonProperty("image_item") ImageItem imageItem,
            @JsonProperty("voice_item") VoiceItem voiceItem,
            @JsonProperty("file_item") FileItem fileItem,
            @JsonProperty("video_item") VideoItem videoItem
    ) {

        public static final int TYPE_TEXT = 1;
        public static final int TYPE_IMAGE = 2;
        public static final int TYPE_VOICE = 3;
        public static final int TYPE_FILE = 4;
        public static final int TYPE_VIDEO = 5;

        public static Item text(String text) {
            return new Item(TYPE_TEXT, null, null, null, null, null, new TextItem(text), null, null, null, null);
        }

        public static Item image(WeixinUploadResult uploadResult) {
            return new Item(
                    TYPE_IMAGE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new ImageItem(
                            CdnMedia.fromUpload(uploadResult),
                            null,
                            uploadResult.aesKeyHex(),
                            null,
                            uploadResult.fileSizeCiphertext(),
                            null,
                            null,
                            null,
                            null
                    ),
                    null,
                    null,
                    null
            );
        }

        public static Item file(String fileName, WeixinUploadResult uploadResult) {
            return new Item(
                    TYPE_FILE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new FileItem(
                            CdnMedia.fromUpload(uploadResult),
                            fileName,
                            null,
                            String.valueOf(uploadResult.fileSize())
                    ),
                    null
            );
        }

        public static Item video(WeixinUploadResult uploadResult) {
            return new Item(
                    TYPE_VIDEO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new VideoItem(
                            CdnMedia.fromUpload(uploadResult),
                            uploadResult.fileSizeCiphertext(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    )
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextItem(@JsonProperty("text") String text) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CdnMedia(
            @JsonProperty("encrypt_query_param") String encryptQueryParam,
            @JsonProperty("aes_key") String aesKey,
            @JsonProperty("encrypt_type") Integer encryptType
    ) {

        public static CdnMedia fromUpload(WeixinUploadResult uploadResult) {
            return new CdnMedia(uploadResult.downloadEncryptedQueryParam(), uploadResult.aesKeyBase64(), 1);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageItem(
            @JsonProperty("media") CdnMedia media,
            @JsonProperty("thumb_media") CdnMedia thumbMedia,
            @JsonProperty("aeskey") String aeskey,
            @JsonProperty("url") String url,
            @JsonProperty("mid_size") Long midSize,
            @JsonProperty("thumb_size") Long thumbSize,
            @JsonProperty("thumb_height") Long thumbHeight,
            @JsonProperty("thumb_width") Long thumbWidth,
            @JsonProperty("hd_size") Long hdSize
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VoiceItem(
            @JsonProperty("media") CdnMedia media,
            @JsonProperty("encode_type") Integer encodeType,
            @JsonProperty("bits_per_sample") Integer bitsPerSample,
            @JsonProperty("sample_rate") Integer sampleRate,
            @JsonProperty("playtime") Integer playtime,
            @JsonProperty("text") String text
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileItem(
            @JsonProperty("media") CdnMedia media,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("md5") String md5,
            @JsonProperty("len") String len
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoItem(
            @JsonProperty("media") CdnMedia media,
            @JsonProperty("video_size") Long videoSize,
            @JsonProperty("play_length") Integer playLength,
            @JsonProperty("video_md5") String videoMd5,
            @JsonProperty("thumb_media") CdnMedia thumbMedia,
            @JsonProperty("thumb_size") Long thumbSize,
            @JsonProperty("thumb_height") Long thumbHeight,
            @JsonProperty("thumb_width") Long thumbWidth
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefMessage(
            @JsonProperty("message_item") Item messageItem,
            @JsonProperty("title") String title
    ) {
    }
}
