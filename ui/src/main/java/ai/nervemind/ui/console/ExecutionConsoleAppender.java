package ai.nervemind.ui.console;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;

/**
 * SLF4J Appender that forwards logs to the ExecutionConsole UI.
 * This allows any SLF4J logs to appear in the UI console window.
 *
 * Note: This class is instantiated by Logback, not Spring, so it cannot use
 * 
 * <p>
 * It uses @Component for dependency injection but handles initialization
 * manually.
 * </p>
 * via the singleton pattern.
 */
public class ExecutionConsoleAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Default constructor for ExecutionConsoleAppender.
     */
    public ExecutionConsoleAppender() {
        // Constructor - no initialization needed
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Get the console service lazily to avoid initialization issues
        ExecutionConsoleService consoleService = ExecutionConsoleService.getInstance();
        if (consoleService == null) {
            return;
        }

        // Convert logback level to our type
        String message = event.getFormattedMessage();
        String details = event.getThrowableProxy() != null ? event.getThrowableProxy().getMessage() : null;

        // Use a generic execution ID for system logs
        String executionId = "system";

        // Forward to UI on JavaFX thread
        try {
            Platform.runLater(() -> {
                switch (event.getLevel().toInt()) {
                    case Level.ERROR_INT -> consoleService.error(executionId, "system", message, details);
                    case Level.WARN_INT -> consoleService.info(executionId, "⚠️ " + message, details);
                    case Level.DEBUG_INT -> consoleService.debug(executionId, message, details);
                    default -> consoleService.info(executionId, message, details);
                }
            });
        } catch (IllegalStateException _) {
            // JavaFX Platform not started yet, or running in headless mode.
            // Ignore UI logging for startup messages.
        }
    }
}