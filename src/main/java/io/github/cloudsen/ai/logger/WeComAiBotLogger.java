package io.github.cloudsen.ai.logger;

/**
 * SDK 日志抽象。
 */
public interface WeComAiBotLogger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message, Throwable throwable);
}
