package ai.nervemind.ui.console;

import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.common.service.ExecutionLogHandler;
import javafx.application.Platform;

/**
 * Log handler that forwards execution logs to the UI ExecutionConsole.
 * Bridges the backend ExecutionLogger to the UI for real-time display.
 */
@Component
public class UILogHandler implements ExecutionLogHandler {

    private static final String UNKNOWN = "unknown";
    private static final String SUCCESS = "success";
    private static final String NODE_NAME = "nodeName";
    private static final String NODE_ID = "nodeId";

    @Override
    public void handle(LogEntry entry) {
        ExecutionConsoleService consoleService = ExecutionConsoleService.getInstance();

        // Ensure UI updates happen on the JavaFX thread
        Platform.runLater(() -> {
            switch (entry.category()) {
                case EXECUTION_START -> handleExecutionStart(entry, consoleService);
                case EXECUTION_END -> handleExecutionEnd(entry, consoleService);
                case NODE_START -> handleNodeStart(entry, consoleService);
                case NODE_END -> handleNodeEnd(entry, consoleService);
                case NODE_SKIP -> handleNodeSkip(entry, consoleService);
                case NODE_INPUT -> handleNodeInput(entry, consoleService);
                case NODE_OUTPUT -> handleNodeOutput(entry, consoleService);
                case VARIABLE -> handleVariable(entry, consoleService);
                case EXPRESSION_EVAL -> handleExpressionEval(entry, consoleService);
                case ERROR -> handleError(entry, consoleService);
                case RETRY -> handleRetry(entry, consoleService);
                case RATE_LIMIT -> handleRateLimit(entry, consoleService);
                case DATA_FLOW -> handleDataFlow(entry, consoleService);
                default -> handleGeneric(entry, consoleService);
            }
        });
    }

    private void handleExecutionStart(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String workflowName = getString(context, "workflowName", "Unknown Workflow");

        service.executionStart(entry.executionId(), workflowName);
    }

    private void handleExecutionEnd(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        boolean success = getBoolean(context, SUCCESS, false);
        long durationMs = getLong(context, "duration_ms", 0);

        service.executionEnd(entry.executionId(), success, durationMs);
    }

    private void handleNodeStart(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, UNKNOWN);
        String nodeName = getString(context, NODE_NAME, "Unknown Node");
        String nodeType = getString(context, "nodeType", UNKNOWN);

        service.nodeStart(entry.executionId(), nodeId, nodeName, nodeType);
    }

    private void handleNodeEnd(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, UNKNOWN);
        String nodeName = getString(context, NODE_NAME, nodeId);
        boolean success = getBoolean(context, SUCCESS, true);
        long durationMs = getLong(context, "durationMs", 0);

        service.nodeEnd(entry.executionId(), nodeId, nodeName, success, durationMs);
    }

    private void handleNodeSkip(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, UNKNOWN);
        String reason = getString(context, "reason", "Unknown reason");

        service.nodeSkip(entry.executionId(), nodeId, nodeId, reason);
    }

    private void handleError(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, getString(context, "source", "workflow"));
        String message = getString(context, "errorMessage", entry.message());
        String stackTrace = getString(context, "stackTrace", null);

        service.error(entry.executionId(), nodeId, message, stackTrace);
    }

    private void handleRetry(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        int attempt = getInt(context, "attempt", 1);
        int maxRetries = getInt(context, "maxRetries", 3);
        long delayMs = getLong(context, "delayMs", 0);

        service.retry(entry.executionId(), attempt, maxRetries, delayMs);
    }

    private void handleRateLimit(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String bucketId = getString(context, "bucketId", "default");
        boolean throttled = getBoolean(context, "throttled", false);
        long waitMs = getLong(context, "waitMs", 0);

        service.rateLimit(entry.executionId(), bucketId, throttled, waitMs);
    }

    private void handleDataFlow(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String fromNode = getString(context, "fromNode", UNKNOWN);
        String toNode = getString(context, "toNode", UNKNOWN);
        int dataSize = getInt(context, "dataSize", 0);

        service.dataFlow(entry.executionId(), fromNode, toNode, dataSize);
    }

    private void handleVariable(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String operation = getString(context, "operation", UNKNOWN);
        String variableName = getString(context, "variableName", UNKNOWN);
        String valuePreview = getString(context, "valuePreview", "");
        String valueType = getString(context, "valueType", UNKNOWN);

        String message = operation + " variable: " + variableName;
        String details = "Type: " + valueType + (valuePreview.isEmpty() ? "" : " | Value: " + valuePreview);

        service.debug(entry.executionId(), message, details);
    }

    private void handleNodeInput(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, UNKNOWN);
        String nodeName = getString(context, NODE_NAME, UNKNOWN);

        // Use full data if available (for debug view), otherwise fall back to preview
        Object inputData = context.get("inputDataFull");
        if (inputData == null) {
            inputData = context.get("inputPreview");
        }
        service.nodeInput(entry.executionId(), nodeId, nodeName, inputData);
    }

    private void handleNodeOutput(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String nodeId = getString(context, NODE_ID, UNKNOWN);
        String nodeName = getString(context, NODE_NAME, UNKNOWN);

        // Use full data if available (for debug view), otherwise fall back to preview
        Object outputData = context.get("outputDataFull");
        if (outputData == null) {
            outputData = context.get("outputPreview");
        }
        service.nodeOutput(entry.executionId(), nodeId, nodeName, outputData);
    }

    private void handleExpressionEval(LogEntry entry, ExecutionConsoleService service) {
        Map<String, Object> context = entry.context();
        String expression = getString(context, "expression", UNKNOWN);
        String resultPreview = getString(context, "resultPreview", "");
        boolean success = getBoolean(context, SUCCESS, true);

        String message = success
                ? "Expression: " + expression
                : "Expression failed: " + expression;
        String details = "Result: " + resultPreview;

        // Use debug level for expression evaluations
        service.debug(entry.executionId(), message, details);
    }

    private void handleGeneric(LogEntry entry, ExecutionConsoleService service) {
        String details = formatContextDetails(entry.context());

        switch (entry.level()) {
            case ERROR, FATAL -> service.error(entry.executionId(), "system", entry.message(), details);
            case WARN -> service.info(entry.executionId(), "⚠️ " + entry.message(), details);
            case DEBUG -> service.debug(entry.executionId(), entry.message(), details);
            case TRACE -> service.debug(entry.executionId(), entry.message(), details);
            default -> service.info(entry.executionId(), entry.message(), details);
        }
    }

    /**
     * Format context map into readable details string.
     */
    private String formatContextDetails(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!first) {
                sb.append("\n");
            }
            sb.append("• ").append(entry.getKey()).append(": ");
            sb.append(formatValue(entry.getValue()));
            first = false;
        }

        return sb.toString();
    }

    /**
     * Format a single value for display.
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str) {
            return formatStringValue(str);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return formatMapValue(map);
        }
        if (value instanceof Iterable) {
            return formatIterableValue((Iterable<?>) value);
        }
        return value.toString();
    }

    private String formatStringValue(String str) {
        if (str.contains(" ") || str.contains("\n") || str.contains("\t")) {
            return "'" + str.replace("\n", "\\n").replace("\t", "\\t") + "'";
        }
        return str;
    }

    private String formatMapValue(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean firstInner = true;
        for (Map.Entry<String, Object> innerEntry : map.entrySet()) {
            if (!firstInner) {
                sb.append(", ");
            }
            sb.append(innerEntry.getKey()).append(": ").append(innerEntry.getValue());
            firstInner = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String formatIterableValue(Iterable<?> iterable) {
        StringBuilder sb = new StringBuilder("[");
        boolean firstInner = true;
        for (Object item : iterable) {
            if (!firstInner) {
                sb.append(", ");
            }
            sb.append(item != null ? item.toString() : "null");
            firstInner = false;
        }
        sb.append("]");
        return sb.toString();
    }

    // Helper methods for extracting values from context map
    private String getString(Map<String, Object> context, String key, String defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> context, String key, boolean defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Boolean b)
            return b;
        if (value != null)
            return Boolean.parseBoolean(value.toString());
        return defaultValue;
    }

    private int getInt(Map<String, Object> context, String key, int defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Number n)
            return n.intValue();
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> context, String key, long defaultValue) {
        if (context == null)
            return defaultValue;
        Object value = context.get(key);
        if (value instanceof Number n)
            return n.longValue();
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
