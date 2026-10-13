package com.mikasa.campusrunner.common.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mikasa.campusrunner.config.DiagnosticLoggingConfiguration;
import com.mikasa.campusrunner.controller.user.UserController;
import com.mikasa.campusrunner.handler.GlobalExceptionHandler;
import com.mikasa.campusrunner.common.exception.UserException;
import com.mikasa.campusrunner.pojo.dto.UserSaveDTO;
import com.mikasa.campusrunner.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiagnosticLogPrivacyTest {
    @Test void profileAndExceptionDiagnosticsNeverContainSensitiveValues() {
        Logger logger = (Logger) LoggerFactory.getLogger("com.mikasa.campusrunner");
        var capture = new ListAppender<ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        try {
            var dto = new UserSaveDTO(); dto.setPhone("PRIVATE_PHONE_SENTINEL"); dto.setUsername("PRIVATE_NAME_SENTINEL");
            var controller = new UserController(); var service = mock(UserService.class);
            ReflectionTestUtils.setField(controller, "userService", service);
            controller.update(dto); verify(service).save(dto);
            var handler = new GlobalExceptionHandler();
            handler.exceptionHandler(new Exception("PRIVATE_PAYMENT_BODY_SENTINEL"));
            handler.exceptionHandler(new UserException("PRIVATE_URL_SENTINEL"));
            assertEquals(3, capture.list.size());
            for (var event : capture.list) {
                assertFalse(event.getFormattedMessage().contains("PRIVATE_"));
                assertNull(event.getThrowableProxy(), "Exception messages and causes must not leak through stack traces");
            }
        } finally { logger.detachAppender(capture); capture.stop(); }
    }
    @Test void sqlParameterDiagnosticsStayDisabledEvenWhenMapperLoggerIsDebug() {
        var configuration = new org.apache.ibatis.session.Configuration();
        new DiagnosticLoggingConfiguration().disableSqlParameterLogging().customize(configuration);
        var diagnostic = org.apache.ibatis.logging.LogFactory.getLog("private.sql");
        assertEquals(org.apache.ibatis.logging.nologging.NoLoggingImpl.class, configuration.getLogImpl());
        assertFalse(diagnostic.isDebugEnabled()); assertFalse(diagnostic.isTraceEnabled());
    }
}
