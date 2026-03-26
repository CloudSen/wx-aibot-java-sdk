package io.github.cloudsen.ai.listener;

import io.github.cloudsen.ai.model.BaseMessage;
import io.github.cloudsen.ai.model.EventMessage;
import io.github.cloudsen.ai.model.WsFrame;

/**
 * 企业微信 AI Bot 事件监听器。
 */
public interface WeComAiBotListener {

    default void onConnected() {
    }

    default void onAuthenticated() {
    }

    default void onDisconnected(String reason) {
    }

    default void onReconnecting(int attempt) {
    }

    default void onError(Throwable throwable) {
    }

    default void onMessage(WsFrame<BaseMessage> frame) {
    }

    default void onTextMessage(WsFrame<BaseMessage> frame) {
    }

    default void onImageMessage(WsFrame<BaseMessage> frame) {
    }

    default void onMixedMessage(WsFrame<BaseMessage> frame) {
    }

    default void onVoiceMessage(WsFrame<BaseMessage> frame) {
    }

    default void onFileMessage(WsFrame<BaseMessage> frame) {
    }

    default void onVideoMessage(WsFrame<BaseMessage> frame) {
    }

    default void onEvent(WsFrame<EventMessage> frame) {
    }

    default void onEnterChat(WsFrame<EventMessage> frame) {
    }

    default void onTemplateCardEvent(WsFrame<EventMessage> frame) {
    }

    default void onFeedbackEvent(WsFrame<EventMessage> frame) {
    }

    default void onServerDisconnectedEvent(WsFrame<EventMessage> frame) {
    }
}
