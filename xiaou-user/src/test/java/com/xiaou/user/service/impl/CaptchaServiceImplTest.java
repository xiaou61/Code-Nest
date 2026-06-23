package com.xiaou.user.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xiaou.common.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptchaServiceImplTest {

    @Test
    void generateCaptchaShouldNotLogPlainTextCaptchaCode() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.set(anyString(), anyString(), anyLong())).thenReturn(true);
        CaptchaServiceImpl service = new CaptchaServiceImpl(redisUtil);

        ListAppender<ILoggingEvent> appender = attachListAppender();
        try {
            service.generateCaptcha();

            ArgumentCaptor<Object> captchaCaptor = ArgumentCaptor.forClass(Object.class);
            org.mockito.Mockito.verify(redisUtil).set(anyString(), captchaCaptor.capture(), anyLong());
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
        RedisUtil redisUtil = mock(RedisUtil.class);
        when(redisUtil.get("user:captcha:captcha-key")).thenReturn("ABCD");
        CaptchaServiceImpl service = new CaptchaServiceImpl(redisUtil);

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
