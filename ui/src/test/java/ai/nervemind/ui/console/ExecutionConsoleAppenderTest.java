/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.console;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.ui.integration.FxTestBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionConsoleAppender")
class ExecutionConsoleAppenderTest extends FxTestBase {

    @Mock
    private ExecutionConsoleService mockConsoleService;

    @Mock
    private ILoggingEvent event;

    @Mock
    private IThrowableProxy throwableProxy;

    private ExecutionConsoleService originalSingleton;

    @BeforeEach
    void setUpSingleton() throws Exception {
        originalSingleton = getConsoleSingleton();
        setConsoleSingleton(mockConsoleService);
    }

    @AfterEach
    void restoreSingleton() throws Exception {
        setConsoleSingleton(originalSingleton);
    }

    @Test
    @DisplayName("should map ERROR level to console error")
    void shouldMapErrorLevelToConsoleError() throws Exception {
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getFormattedMessage()).thenReturn("boom");
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(throwableProxy.getMessage()).thenReturn("stack details");

        TestableExecutionConsoleAppender appender = new TestableExecutionConsoleAppender();
        appender.appendEvent(event);
        waitForFxEvents();

        verify(mockConsoleService).error("system", "system", "boom", "stack details");
    }

    @Test
    @DisplayName("should map WARN level to info with warning prefix")
    void shouldMapWarnLevelToInfoWithWarningPrefix() throws Exception {
        when(event.getLevel()).thenReturn(Level.WARN);
        when(event.getFormattedMessage()).thenReturn("careful");
        when(event.getThrowableProxy()).thenReturn(null);

        TestableExecutionConsoleAppender appender = new TestableExecutionConsoleAppender();
        appender.appendEvent(event);
        waitForFxEvents();

        verify(mockConsoleService).info("system", "⚠️ careful", null);
    }

    @Test
    @DisplayName("should map DEBUG level to console debug")
    void shouldMapDebugLevelToConsoleDebug() throws Exception {
        when(event.getLevel()).thenReturn(Level.DEBUG);
        when(event.getFormattedMessage()).thenReturn("trace");
        when(event.getThrowableProxy()).thenReturn(null);

        TestableExecutionConsoleAppender appender = new TestableExecutionConsoleAppender();
        appender.appendEvent(event);
        waitForFxEvents();

        verify(mockConsoleService).debug("system", "trace", null);
    }

    @Test
    @DisplayName("should map other levels to console info")
    void shouldMapOtherLevelsToConsoleInfo() throws Exception {
        when(event.getLevel()).thenReturn(Level.INFO);
        when(event.getFormattedMessage()).thenReturn("hello");
        when(event.getThrowableProxy()).thenReturn(null);

        TestableExecutionConsoleAppender appender = new TestableExecutionConsoleAppender();
        appender.appendEvent(event);
        waitForFxEvents();

        verify(mockConsoleService).info("system", "hello", null);
    }

    private static ExecutionConsoleService getConsoleSingleton() throws Exception {
        Field field = ExecutionConsoleService.class.getDeclaredField("instance");
        field.setAccessible(true);
        return (ExecutionConsoleService) field.get(null);
    }

    private static void setConsoleSingleton(ExecutionConsoleService value) throws Exception {
        Field field = ExecutionConsoleService.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class TestableExecutionConsoleAppender extends ExecutionConsoleAppender {
        private void appendEvent(ILoggingEvent event) {
            append(event);
        }
    }
}
