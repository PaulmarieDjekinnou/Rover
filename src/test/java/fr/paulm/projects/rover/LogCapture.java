package fr.paulm.projects.rover;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

// Collects the messages logged by a class, to check them in tests.
public final class LogCapture {
    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture(Logger logger) {
        this.logger = logger;
    }

    public static LogCapture start(Class<?> loggingClass) {
        LogCapture capture = new LogCapture((Logger) LoggerFactory.getLogger(loggingClass));
        capture.appender.start();
        capture.logger.addAppender(capture.appender);
        return capture;
    }

    public void stop() {
        logger.detachAppender(appender);
        appender.stop();
    }

    public List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
