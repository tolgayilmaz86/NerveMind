package ai.nervemind.common.service;

import java.time.Instant;
import java.util.Map;

/**
 * Handler interface for receiving execution log events during workflow
 * execution.
 * 
 * <p>
 * This interface provides a decoupled mechanism for UI components to receive
 * real-time updates about workflow execution progress. Implementations can
 * process
 * log entries for display, persistence, or analysis.
 * </p>
 * 
 * <h2>Event Flow</h2>
 * 
 * <pre>
 * ExecutionService
 *       │
 *       ├── EXECUTION_START
 *       │
 *       ├── NODE_START ──&gt; NODE_INPUT ──&gt; NODE_OUTPUT ──&gt; NODE_END
 *       │       │
 *       │       └── (or NODE_SKIP if disabled)
 *       │
 *       ├── DATA_FLOW (between nodes)
 *       │
 *       └── EXECUTION_END
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * class ConsoleLogHandler implements ExecutionLogHandler {
 *     public void handle(LogEntry entry) {
 *         System.out.printf("[%s] %s: %s%n",
 *                 entry.category(),
 *                 entry.level(),
 *                 entry.message());
 *     }
 * }
 * </pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Handlers may be called from multiple threads during parallel node execution.
 * Implementations should be thread-safe.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see LogEntry Structured log entry containing all event details
 * @see LogCategory Categories of execution events
 */
public interface ExecutionLogHandler {

    /**
     * Log level enumeration.
     */
    enum LogLevel {
        /** Detailed tracing information. */
        TRACE,
        /** Debugging information. */
        DEBUG,
        /** General informational messages. */
        INFO,
        /** Warning messages for potential issues. */
        WARN,
        /** Error messages for failed operations. */
        ERROR,
        /** Fatal errors causing system shutdown. */
        FATAL
    }

    /**
     * Log category enumeration.
     */
    enum LogCategory {
        /** Start of the entire execution. */
        EXECUTION_START,
        /** End of the entire execution. */
        EXECUTION_END,
        /** Start of a node execution. */
        NODE_START,
        /** End of a node execution. */
        NODE_END,
        /** Node execution was skipped. */
        NODE_SKIP,
        /** Logs node input data. */
        NODE_INPUT,
        /** Logs node output data. */
        NODE_OUTPUT,
        /** Data flow between nodes. */
        DATA_FLOW,
        /** Variable operations. */
        VARIABLE,
        /** Logs expression evaluation. */
        EXPRESSION_EVAL,
        /** Error event. */
        ERROR,
        /** Retry attempt. */
        RETRY,
        /** Rate limit hit. */
        RATE_LIMIT,
        /** Performance metric. */
        PERFORMANCE,
        /** Custom user log. */
        CUSTOM
    }

    /**
     * Log entry record containing all log information.
     *
     * @param id          Unique log entry ID
     * @param executionId ID of the execution session
     * @param timestamp   Time of the event
     * @param level       Severity level
     * @param category    Event category
     * @param message     Human-readable message
     * @param context     Additional structured context data
     */
    record LogEntry(
            String id,
            String executionId,
            Instant timestamp,
            LogLevel level,
            LogCategory category,
            String message,
            Map<String, Object> context) {
    }

    /**
     * Handle a log entry.
     *
     * @param entry The log entry to process
     */
    void handle(LogEntry entry);
}
