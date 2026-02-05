package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.NodeExecutorRegistry;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "retry" node type - implements retry logic with configurable
 * backoff strategies.
 *
 * <p>
 * Provides automatic retry functionality for transient failures with
 * sophisticated
 * backoff algorithms. Ideal for unreliable external services, rate-limited
 * APIs, or
 * operations that may temporarily fail.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Retry node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>operations</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Operations to execute with retry protection</td>
 * </tr>
 * <tr>
 * <td>maxRetries</td>
 * <td>Integer</td>
 * <td>3</td>
 * <td>Maximum retry attempts</td>
 * </tr>
 * <tr>
 * <td>backoffStrategy</td>
 * <td>String</td>
 * <td>"exponential"</td>
 * <td>Backoff algorithm (see strategies below)</td>
 * </tr>
 * <tr>
 * <td>initialDelayMs</td>
 * <td>Long</td>
 * <td>1000</td>
 * <td>Initial delay between retries</td>
 * </tr>
 * <tr>
 * <td>maxDelayMs</td>
 * <td>Long</td>
 * <td>30000</td>
 * <td>Maximum delay cap</td>
 * </tr>
 * <tr>
 * <td>multiplier</td>
 * <td>Double</td>
 * <td>2.0</td>
 * <td>Backoff multiplier</td>
 * </tr>
 * <tr>
 * <td>jitter</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Add random jitter to delays</td>
 * </tr>
 * <tr>
 * <td>jitterFactor</td>
 * <td>Double</td>
 * <td>0.1</td>
 * <td>Maximum jitter as fraction of delay</td>
 * </tr>
 * <tr>
 * <td>retryableErrors</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Error types to retry (empty = all)</td>
 * </tr>
 * <tr>
 * <td>nonRetryableErrors</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Error types to never retry</td>
 * </tr>
 * </table>
 *
 * <h2>Backoff Strategies</h2>
 * <ul>
 * <li><strong>fixed</strong> - Constant delay: {@code initialDelayMs}</li>
 * <li><strong>linear</strong> - Linear increase:
 * {@code initialDelayMs * attempt}</li>
 * <li><strong>exponential</strong> - Exponential increase:
 * {@code initialDelayMs * multiplier^(attempt-1)}</li>
 * <li><strong>fibonacci</strong> - Fibonacci sequence:
 * {@code initialDelayMs * fib(attempt)}</li>
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
 * <td>True if operation eventually succeeded</td>
 * </tr>
 * <tr>
 * <td>attemptCount</td>
 * <td>Integer</td>
 * <td>Total attempts made (including initial)</td>
 * </tr>
 * <tr>
 * <td>totalDelayMs</td>
 * <td>Long</td>
 * <td>Total time spent waiting between retries</td>
 * </tr>
 * <tr>
 * <td>errors</td>
 * <td>List</td>
 * <td>Error messages from failed attempts</td>
 * </tr>
 * <tr>
 * <td>result</td>
 * <td>Map</td>
 * <td>Final operation result if successful</td>
 * </tr>
 * </table>
 *
 * <h2>Jitter</h2>
 * <p>
 * When {@code jitter=true}, a random amount (0 to {@code jitterFactor * delay})
 * is added or subtracted from each delay to prevent thundering herd problems
 * when
 * multiple retry operations execute simultaneously.
 * </p>
 *
 * @see TryCatchExecutor For try-catch error handling
 * @see RateLimitExecutor For throttling API calls
 */
@Component
public class RetryExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final Random random = new Random();

    /**
     * Creates a new RetryExecutor with the given node executor registry.
     *
     * @param nodeExecutorRegistry the registry for accessing other node executors
     */
    public RetryExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "retry";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.get("operations");
        if (operations == null || operations.isEmpty()) {
            return Map.of("error", "No operations configured", "input", input);
        }

        RetryConfig retryConfig = buildRetryConfig(config);
        RetryState state = executeWithRetry(operations, input, context, retryConfig);

        return buildOutput(state, retryConfig);
    }

    @SuppressWarnings("unchecked")
    private RetryConfig buildRetryConfig(Map<String, Object> config) {
        return new RetryConfig(
                getIntConfig(config, "maxRetries", 3),
                getStringConfig(config, "backoffStrategy", "exponential"),
                getLongConfig(config, "initialDelayMs", 1000L),
                getLongConfig(config, "maxDelayMs", 30000L),
                getDoubleConfig(config, "multiplier", 2.0),
                getBooleanConfig(config, "jitter", true),
                getDoubleConfig(config, "jitterFactor", 0.1),
                (List<String>) config.getOrDefault("retryableErrors", List.of()),
                (List<String>) config.getOrDefault("nonRetryableErrors", List.of()));
    }

    private RetryState executeWithRetry(List<Map<String, Object>> operations, Map<String, Object> input,
            ExecutionService.ExecutionContext context, RetryConfig config) {
        RetryState state = new RetryState();

        int attempt = 0;
        boolean shouldContinue = true;

        while (shouldContinue && attempt <= config.maxRetries) {
            state.attemptCount = attempt + 1;

            try {
                state.result = executeOperations(operations, input, context);
                state.success = true;
                shouldContinue = false;
            } catch (Exception e) {
                recordError(state, e);

                boolean isRetryable = shouldRetry(e, config.retryableErrors, config.nonRetryableErrors);
                boolean canRetry = isRetryable && attempt < config.maxRetries;

                if (canRetry) {
                    shouldContinue = applyDelayBeforeRetry(state, attempt, config);
                } else {
                    shouldContinue = false;
                }
            }

            attempt++;
        }

        return state;
    }

    private void recordError(RetryState state, Exception e) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("attempt", state.attemptCount);
        errorInfo.put("message", e.getMessage());
        errorInfo.put("type", e.getClass().getSimpleName());
        errorInfo.put("timestamp", java.time.Instant.now().toString());
        state.attemptErrors.add(errorInfo);
    }

    private boolean applyDelayBeforeRetry(RetryState state, int attempt, RetryConfig config) {
        long delay = calculateDelay(attempt, config.backoffStrategy, config.initialDelayMs, config.maxDelayMs,
                config.multiplier);
        delay = applyJitter(delay, config);
        state.totalDelayMs += delay;

        try {
            TimeUnit.MILLISECONDS.sleep(delay);
            return true;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private long applyJitter(long delay, RetryConfig config) {
        if (!config.jitter) {
            return delay;
        }
        long maxJitter = (long) (delay * config.jitterFactor);
        long jitterAmount = maxJitter > 0 ? random.nextLong(maxJitter) : 0;
        delay = delay + (random.nextBoolean() ? jitterAmount : -jitterAmount);
        return Math.max(0, delay);
    }

    private Map<String, Object> buildOutput(RetryState state, RetryConfig config) {
        Map<String, Object> output = new HashMap<>();
        output.put("success", state.success);
        output.put("attemptCount", state.attemptCount);
        output.put("maxRetries", config.maxRetries);
        output.put("totalDelayMs", state.totalDelayMs);
        output.put("totalTimeMs", System.currentTimeMillis() - state.startTime);
        output.put("backoffStrategy", config.backoffStrategy);

        if (state.success && state.result != null) {
            output.put("result", state.result);
            output.putAll(state.result);
        } else {
            output.put("errors", state.attemptErrors);
            if (!state.attemptErrors.isEmpty()) {
                output.put("lastError", state.attemptErrors.getLast());
            }
        }

        return output;
    }

    private record RetryConfig(int maxRetries, String backoffStrategy, long initialDelayMs, long maxDelayMs,
            double multiplier, boolean jitter, double jitterFactor, List<String> retryableErrors,
            List<String> nonRetryableErrors) {
    }

    private static class RetryState {
        List<Map<String, Object>> attemptErrors = new ArrayList<>();
        Map<String, Object> result = null;
        long totalDelayMs = 0;
        int attemptCount = 0;
        boolean success = false;
        long startTime = System.currentTimeMillis();
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

            Node tempNode = new Node(
                    "retry_" + UUID.randomUUID().toString().substring(0, 8),
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

    private long calculateDelay(int attempt, String strategy, long initialDelay, long maxDelay, double multiplier) {
        long delay = switch (strategy.toLowerCase()) {
            case "fixed" -> initialDelay;
            case "linear" -> (long) (initialDelay * (1 + attempt * multiplier));
            case "exponential" -> (long) (initialDelay * Math.pow(multiplier, attempt));
            case "fibonacci" -> initialDelay * fibonacci(attempt + 1);
            default -> (long) (initialDelay * Math.pow(multiplier, attempt));
        };

        return Math.min(delay, maxDelay);
    }

    private long fibonacci(int n) {
        if (n <= 1)
            return n;
        long a = 0;
        long b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }

    private boolean shouldRetry(Exception e, List<String> retryable, List<String> nonRetryable) {
        String errorType = e.getClass().getSimpleName();
        String errorFullType = e.getClass().getName();

        // Check non-retryable first
        if (!nonRetryable.isEmpty()) {
            for (String type : nonRetryable) {
                if (errorType.equalsIgnoreCase(type) || errorFullType.equalsIgnoreCase(type)) {
                    return false;
                }
            }
        }

        // If retryable list is empty, retry all errors
        if (retryable.isEmpty()) {
            return true;
        }

        // Check if error is in retryable list
        for (String type : retryable) {
            if (errorType.equalsIgnoreCase(type) || errorFullType.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    private int getIntConfig(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLongConfig(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDoubleConfig(Map<String, Object> config, String key, double defaultValue) {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number n)
            return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException _) {
                return defaultValue;
            }
        }
        return defaultValue;
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
