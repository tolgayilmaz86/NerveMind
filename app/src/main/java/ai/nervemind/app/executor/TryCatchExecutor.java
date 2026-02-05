package ai.nervemind.app.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.NodeExecutorRegistry;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;

/**
 * Executor for the "tryCatch" node type - provides structured error handling.
 *
 * <p>
 * Wraps operations in try/catch/finally blocks for graceful error handling
 * within workflows. Allows workflows to continue execution even when individual
 * operations fail, and enables custom error recovery logic.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>TryCatch node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>tryOperations</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Operations to execute in try block</td>
 * </tr>
 * <tr>
 * <td>catchOperations</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Operations to execute on error</td>
 * </tr>
 * <tr>
 * <td>finallyOperations</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Operations that always execute</td>
 * </tr>
 * <tr>
 * <td>errorVariable</td>
 * <td>String</td>
 * <td>"error"</td>
 * <td>Variable name for error info</td>
 * </tr>
 * <tr>
 * <td>continueOnError</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Continue workflow after error</td>
 * </tr>
 * <tr>
 * <td>logErrors</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Log errors to console</td>
 * </tr>
 * </table>
 *
 * <h2>Operation Structure</h2>
 * <p>
 * Each operation in tryOperations, catchOperations, or finallyOperations:
 * </p>
 * <ul>
 * <li><strong>type</strong> - Node type to execute (e.g., "httpRequest")</li>
 * <li><strong>name</strong> - Display name (optional)</li>
 * <li><strong>config</strong> - Configuration for that node type</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * 
 * <pre>
 *   1. Execute tryOperations in sequence
 *   2. IF error occurs:
 *      - Store error info in [errorVariable]
 *      - Execute catchOperations
 *   3. ALWAYS execute finallyOperations
 *   4. Return combined output or throw if continueOnError=false
 * </pre>
 *
 * <h2>Error Information</h2>
 * <p>
 * When an error occurs, the following is stored in the error variable:
 * </p>
 * <ul>
 * <li><strong>message</strong> - Error message string</li>
 * <li><strong>type</strong> - Exception class name</li>
 * <li><strong>timestamp</strong> - When error occurred</li>
 * <li><strong>operation</strong> - Which operation failed</li>
 * </ul>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr>
 * <th>Key</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>success</td>
 * <td>Boolean</td>
 * <td>True if try block completed without errors</td>
 * </tr>
 * <tr>
 * <td>hasError</td>
 * <td>Boolean</td>
 * <td>True if an error was caught</td>
 * </tr>
 * <tr>
 * <td>[errorVariable]</td>
 * <td>Map</td>
 * <td>Error information (if error occurred)</td>
 * </tr>
 * </table>
 *
 * @see RetryExecutor For automatic retry on failure
 * @see NodeExecutionException The exception type for node failures
 */
@Component
public class TryCatchExecutor implements NodeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TryCatchExecutor.class);

    private final NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * Creates a new try-catch executor.
     *
     * @param nodeExecutorRegistry the registry for accessing other node executors
     */
    public TryCatchExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "tryCatch";
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();
        TryCatchConfig tryCatchConfig = extractConfig(config);

        TryCatchState state = new TryCatchState(new HashMap<>(input));

        executeTryBlock(tryCatchConfig, state, context);
        executeFinallyBlock(tryCatchConfig, state, context);

        state.result.put("_tryCatchSuccess", state.success);
        state.result.put("_tryCatchExecuted", true);

        return state.result;
    }

    @SuppressWarnings("unchecked")
    private TryCatchConfig extractConfig(Map<String, Object> config) {
        return new TryCatchConfig(
                (List<Map<String, Object>>) config.get("tryOperations"),
                (List<Map<String, Object>>) config.get("catchOperations"),
                (List<Map<String, Object>>) config.get("finallyOperations"),
                getStringConfig(config, "errorVariable", "error"),
                getBooleanConfig(config, "continueOnError", true),
                getBooleanConfig(config, "logErrors", true));
    }

    private void executeTryBlock(TryCatchConfig config, TryCatchState state,
            ExecutionService.ExecutionContext context) {
        try {
            if (config.tryOperations != null && !config.tryOperations.isEmpty()) {
                state.result = executeOperations(config.tryOperations, state.result, context);
            }
        } catch (Exception e) {
            state.success = false;
            handleTryBlockError(config, state, context, e);
        }
    }

    private void handleTryBlockError(TryCatchConfig config, TryCatchState state,
            ExecutionService.ExecutionContext context, Exception e) {
        Map<String, Object> errorInfo = captureError(e);

        if (config.logErrors) {
            logger.error("[TryCatch] Error in try block: {}", e.getMessage());
        }

        state.result.put(config.errorVariable, errorInfo);

        if (config.catchOperations != null && !config.catchOperations.isEmpty()) {
            executeCatchBlock(config, state, context, errorInfo);
        } else if (!config.continueOnError) {
            throw new NodeExecutionException("Error in try block", e);
        }
    }

    private void executeCatchBlock(TryCatchConfig config, TryCatchState state,
            ExecutionService.ExecutionContext context, Map<String, Object> errorInfo) {
        try {
            Map<String, Object> catchInput = new HashMap<>(state.result);
            catchInput.put("_caughtError", errorInfo);
            state.result = executeOperations(config.catchOperations, catchInput, context);
        } catch (Exception catchError) {
            handleCatchBlockError(config, state, catchError);
        }
    }

    private void handleCatchBlockError(TryCatchConfig config, TryCatchState state, Exception catchError) {
        Map<String, Object> catchErrorInfo = captureError(catchError);
        state.result.put("catchError", catchErrorInfo);

        if (config.logErrors) {
            logger.error("[TryCatch] Error in catch block: {}", catchError.getMessage());
        }

        if (!config.continueOnError) {
            throw new NodeExecutionException("Error in catch block", catchError);
        }
    }

    private void executeFinallyBlock(TryCatchConfig config, TryCatchState state,
            ExecutionService.ExecutionContext context) {
        if (config.finallyOperations == null || config.finallyOperations.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> finallyInput = new HashMap<>(state.result);
            finallyInput.put("_success", state.success);
            finallyInput.put("_hadError", !state.success);
            state.result = executeOperations(config.finallyOperations, finallyInput, context);
        } catch (Exception finallyError) {
            handleFinallyBlockError(config, state, finallyError);
        }
    }

    private void handleFinallyBlockError(TryCatchConfig config, TryCatchState state, Exception finallyError) {
        Map<String, Object> finallyErrorInfo = captureError(finallyError);
        state.result.put("finallyError", finallyErrorInfo);

        if (config.logErrors) {
            logger.error("[TryCatch] Error in finally block: {}", finallyError.getMessage());
        }

        if (!config.continueOnError) {
            throw new NodeExecutionException("Error in finally block", finallyError);
        }
    }

    private record TryCatchConfig(
            List<Map<String, Object>> tryOperations,
            List<Map<String, Object>> catchOperations,
            List<Map<String, Object>> finallyOperations,
            String errorVariable,
            boolean continueOnError,
            boolean logErrors) {
    }

    private static class TryCatchState {
        Map<String, Object> result;
        boolean success = true;

        TryCatchState(Map<String, Object> result) {
            this.result = result;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeOperations(
            List<Map<String, Object>> operations,
            Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        Map<String, Object> currentData = new HashMap<>(input);

        for (Map<String, Object> operation : operations) {
            String type = (String) operation.get("type");
            if (type == null)
                continue;

            Map<String, Object> opConfig = (Map<String, Object>) operation.getOrDefault("config", Map.of());
            String name = (String) operation.getOrDefault("name", type);

            // Create a temporary node for this operation
            Node tempNode = new Node(
                    "trycatch_" + UUID.randomUUID().toString().substring(0, 8),
                    type,
                    name,
                    new Node.Position(0, 0),
                    opConfig,
                    null,
                    false,
                    null);

            NodeExecutor executor = nodeExecutorRegistry.getExecutor(type);
            currentData = executor.execute(tempNode, currentData, context);
        }

        return currentData;
    }

    private Map<String, Object> captureError(Exception e) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("message", e.getMessage());
        errorInfo.put("type", e.getClass().getSimpleName());
        errorInfo.put("fullType", e.getClass().getName());
        errorInfo.put("timestamp", java.time.Instant.now().toString());

        // Capture stack trace
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        errorInfo.put("stackTrace", sw.toString());

        // Capture cause if present
        if (e.getCause() != null) {
            Map<String, Object> causeInfo = new HashMap<>();
            causeInfo.put("message", e.getCause().getMessage());
            causeInfo.put("type", e.getCause().getClass().getSimpleName());
            errorInfo.put("cause", causeInfo);
        }

        return errorInfo;
    }

    private boolean getBooleanConfig(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean b)
            return b;
        if (value instanceof String s)
            return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private String getStringConfig(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        return value.toString();
    }
}
