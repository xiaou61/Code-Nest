package com.xiaou.user.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xiaou.common.cache.RedisValueStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptchaServiceImplTest {

    @Test
    void generateCaptchaShouldNotLogPlainTextCaptchaCode() {
        RedisValueStore redisValueStore = mock(RedisValueStore.class);
        CaptchaServiceImpl service = new CaptchaServiceImpl(redisValueStore);

        ListAppender<ILoggingEvent> appender = attachListAppender();
        try {
            service.generateCaptcha();

            ArgumentCaptor<Object> captchaCaptor = ArgumentCaptor.forClass(Object.class);
            org.mockito.Mockito.verify(redisValueStore).put(anyString(), captchaCaptor.capture(), any());
            String captchaCode = String.valueOf(captchaCaptor.getValue());
            String logs = renderedLogs(appender);

            assertFalse(captchaCode.isBlank());
            assertFalse(logs.contains(captchaCode));
            assertFalse(logs.toLowerCase().contains("code:"));
        } finally {
            detachListAppender(appender);
        }
    }

    @Test
    void failedVerificationShouldNotLogInputOrStoredCaptchaCode() {
        RedisValueStore redisValueStore = mock(RedisValueStore.class);
        when(redisValueStore.find("user:captcha:captcha-key", String.class)).thenReturn(Optional.of("ABCD"));
        CaptchaServiceImpl service = new CaptchaServiceImpl(redisValueStore);

        ListAppender<ILoggingEvent> appender = attachListAppender();
        try {
            boolean result = service.verifyCaptcha("captcha-key", "WXYZ");
            String logs = renderedLogs(appender);

            assertFalse(result);
            assertFalse(logs.contains("ABCD"));
            assertFalse(logs.contains("WXYZ"));
        } finally {
            detachListAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> attachListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(CaptchaServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        return appender;
    }

    private void detachListAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(CaptchaServiceImpl.class);
        logger.detachAppender(appender);
        appender.stop();
    }

    private String renderedLogs(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }
}
