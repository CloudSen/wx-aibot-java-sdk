package io.github.cloudsen.ai.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 默认 JUL 日志实现。
 */
public class DefaultWeComAiBotLogger implements WeComAiBotLogger {

    private static final Logger LOGGER = Logger.getLogger(DefaultWeComAiBotLogger.class.getName());

    @Override
    public void debug(String message) {
        LOGGER.fine(message);
    }

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warning(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }
}
