package ai.nervemind.app.executor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.NodeExecutorRegistry;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "rate_limit" node type - throttles operations using rate
 * limiting algorithms.
 *
 * <p>
 * Prevents overwhelming external services by limiting the rate of operations.
 * Supports two rate limiting strategies: Token Bucket (smooth rate limiting)
 * and
 * Sliding Window (strict per-window limits). Rate limit buckets are shared
 * globally
 * across workflow executions.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Rate limit node configuration parameters</caption>
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
 * <td>Operations to execute with rate limiting</td>
 * </tr>
 * <tr>
 * <td>bucketId</td>
 * <td>String</td>
 * <td>"default"</td>
 * <td>Rate limit bucket identifier</td>
 * </tr>
 * <tr>
 * <td>strategy</td>
 * <td>String</td>
 * <td>"token_bucket"</td>
 * <td>"token_bucket" or "sliding_window"</td>
 * </tr>
 * <tr>
 * <td>waitForTokens</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Wait for tokens if unavailable</td>
 * </tr>
 * <tr>
 * <td>maxWaitMs</td>
 * <td>Long</td>
 * <td>60000</td>
 * <td>Max wait time for tokens</td>
 * </tr>
 * </table>
 *
 * <h2>Token Bucket Parameters</h2>
 * <table border="1">
 * <caption>Token bucket specific parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>tokensPerSecond</td>
 * <td>Double</td>
 * <td>10.0</td>
 * <td>Rate of token refill</td>
 * </tr>
 * <tr>
 * <td>maxTokens</td>
 * <td>Integer</td>
 * <td>100</td>
 * <td>Maximum bucket capacity</td>
 * </tr>
 * <tr>
 * <td>tokensPerRequest</td>
 * <td>Integer</td>
 * <td>1</td>
 * <td>Tokens consumed per request</td>
 * </tr>
 * </table>
 *
 * <h2>Sliding Window Parameters</h2>
 * <table border="1">
 * <caption>Sliding window specific parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>windowSizeMs</td>
 * <td>Long</td>
 * <td>1000</td>
 * <td>Window duration in milliseconds</td>
 * </tr>
 * <tr>
 * <td>maxRequestsPerWindow</td>
 * <td>Integer</td>
 * <td>10</td>
 * <td>Max requests per window</td>
 * </tr>
 * </table>
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
 * <td>True if rate limit acquired and operation succeeded</td>
 * </tr>
 * <tr>
 * <td>throttled</td>
 * <td>Boolean</td>
 * <td>True if request was rejected due to rate limit</td>
 * </tr>
 * <tr>
 * <td>waitedMs</td>
 * <td>Long</td>
 * <td>Time spent waiting for rate limit</td>
 * </tr>
 * <tr>
 * <td>tokensRemaining</td>
 * <td>Double</td>
 * <td>Tokens left (token bucket only)</td>
 * </tr>
 * <tr>
 * <td>requestsInWindow</td>
 * <td>Integer</td>
 * <td>Requests in window (sliding window only)</td>
 * </tr>
 * </table>
 *
 * <h2>Shared Buckets</h2>
 * <p>
 * Buckets are stored in static {@link ConcurrentHashMap}s and shared across
 * all workflow executions. Use unique {@code bucketId}s to isolate rate limits.
 * </p>
 *
 * @see RetryExecutor For handling rate limit errors with backoff
 * @see HttpRequestExecutor Common use case for rate limiting
 */
@Component
public class RateLimitExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;

    // Global rate limit buckets (shared across executions)
    private static final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SlidingWindow> slidingWindows = new ConcurrentHashMap<>();

    /**
     * Creates a new RateLimitExecutor with the given node executor registry.
     *
     * @param nodeExecutorRegistry the registry for accessing other node executors
     */
    public RateLimitExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "rate_limit";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        List<Map<String, Object>> operations = (List<Map<String, Object>>) config.get("operations");
        String bucketId = getStringConfig(config, "bucketId", "default");
        String strategy = getStringConfig(config, "strategy", "token_bucket");
        boolean waitForTokens = getBooleanConfig(config, "waitForTokens", true);
        long maxWaitMs = getLongConfig(config, "maxWaitMs", 60000L);

        long startTime = System.currentTimeMillis();
        boolean acquired = false;
        long waitedMs = 0;

        if (strategy.equals("sliding_window")) {
            acquired = acquireSlidingWindow(config, bucketId, waitForTokens, maxWaitMs);
        } else {
            acquired = acquireTokenBucket(config, bucketId, waitForTokens, maxWaitMs);
        }

        waitedMs = System.currentTimeMillis() - startTime;

        Map<String, Object> output = new HashMap<>();
        output.put("bucketId", bucketId);
        output.put("strategy", strategy);
        output.put("waitedMs", waitedMs);
        output.put("throttled", !acquired);

        if (!acquired) {
            output.put("error", "Rate limit exceeded, could not acquire tokens within timeout");
            output.put("success", false);
            return output;
        }

        // Add bucket stats
        if (strategy.equals("sliding_window")) {
            SlidingWindow window = slidingWindows.get(bucketId);
            if (window != null) {
                output.put("requestsInWindow", window.getRequestCount());
            }
        } else {
            TokenBucket bucket = tokenBuckets.get(bucketId);
            if (bucket != null) {
                output.put("tokensRemaining", bucket.getAvailableTokens());
            }
        }

        // Execute operations if we have any
        if (operations != null && !operations.isEmpty()) {
            try {
                Map<String, Object> result = executeOperations(operations, input, context);
                output.put("success", true);
                output.put("result", result);
                output.putAll(result);
            } catch (Exception e) {
                output.put("success", false);
                output.put("error", e.getMessage());
            }
        } else {
            output.put("success", true);
            output.putAll(input);
        }

        return output;
    }

    private boolean acquireTokenBucket(Map<String, Object> config, String bucketId, boolean wait, long maxWaitMs) {
        double tokensPerSecond = getDoubleConfig(config, "tokensPerSecond", 10.0);
        int maxTokens = getIntConfig(config, "maxTokens", 100);
        int tokensPerRequest = getIntConfig(config, "tokensPerRequest", 1);

        TokenBucket bucket = tokenBuckets.computeIfAbsent(bucketId,
                k -> new TokenBucket(maxTokens, tokensPerSecond));

        if (wait) {
            return bucket.acquireWithWait(tokensPerRequest, maxWaitMs);
        } else {
            return bucket.tryAcquire(tokensPerRequest);
        }
    }

    private boolean acquireSlidingWindow(Map<String, Object> config, String bucketId, boolean wait, long maxWaitMs) {
        long windowSizeMs = getLongConfig(config, "windowSizeMs", 1000L);
        int maxRequestsPerWindow = getIntConfig(config, "maxRequestsPerWindow", 10);

        SlidingWindow window = slidingWindows.computeIfAbsent(bucketId,
                k -> new SlidingWindow(windowSizeMs, maxRequestsPerWindow));

        if (wait) {
            return window.acquireWithWait(maxWaitMs);
        } else {
            return window.tryAcquire();
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

            Node tempNode = new Node(
                    "ratelimit_" + UUID.randomUUID().toString().substring(0, 8),
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

    /**
     * Token Bucket implementation for rate limiting.
     */
    private static class TokenBucket {
        private final int maxTokens;
        private final double tokensPerSecond;
        private double availableTokens;
        private long lastRefillTime;
        private final ReentrantLock lock = new ReentrantLock();

        TokenBucket(int maxTokens, double tokensPerSecond) {
            this.maxTokens = maxTokens;
            this.tokensPerSecond = tokensPerSecond;
            this.availableTokens = maxTokens;
            this.lastRefillTime = System.nanoTime();
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;
            double tokensToAdd = elapsedSeconds * tokensPerSecond;
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillTime = now;
        }

        boolean tryAcquire(int tokens) {
            lock.lock();
            try {
                refill();
                if (availableTokens >= tokens) {
                    availableTokens -= tokens;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        boolean acquireWithWait(int tokens, long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;

            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire(tokens)) {
                    return true;
                }

                // Calculate wait time until enough tokens are available
                lock.lock();
                try {
                    refill();
                    double tokensNeeded = tokens - availableTokens;
                    if (tokensNeeded <= 0)
                        continue;

                    long waitMs = (long) ((tokensNeeded / tokensPerSecond) * 1000) + 10;
                    waitMs = Math.min(waitMs, deadline - System.currentTimeMillis());

                    if (waitMs > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(waitMs);
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            return tryAcquire(tokens);
        }

        double getAvailableTokens() {
            lock.lock();
            try {
                refill();
                return availableTokens;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Sliding Window implementation for rate limiting.
     */
    private static class SlidingWindow {
        private final long windowSizeMs;
        private final int maxRequests;
        private final LinkedList<Long> requestTimestamps = new LinkedList<>();
        private final ReentrantLock lock = new ReentrantLock();

        SlidingWindow(long windowSizeMs, int maxRequests) {
            this.windowSizeMs = windowSizeMs;
            this.maxRequests = maxRequests;
        }

        private void cleanOldRequests() {
            long now = System.currentTimeMillis();
            long cutoff = now - windowSizeMs;
            while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < cutoff) {
                requestTimestamps.poll();
            }
        }

        boolean tryAcquire() {
            lock.lock();
            try {
                cleanOldRequests();
                if (requestTimestamps.size() < maxRequests) {
                    requestTimestamps.add(System.currentTimeMillis());
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        boolean acquireWithWait(long maxWaitMs) {
            long deadline = System.currentTimeMillis() + maxWaitMs;

            while (System.currentTimeMillis() < deadline) {
                if (tryAcquire()) {
                    return true;
                }

                // Wait until oldest request expires
                lock.lock();
                try {
                    cleanOldRequests();
                    if (requestTimestamps.isEmpty())
                        continue;

                    long oldestExpiry = requestTimestamps.peek() + windowSizeMs;
                    long waitMs = oldestExpiry - System.currentTimeMillis() + 10;
                    waitMs = Math.min(waitMs, deadline - System.currentTimeMillis());

                    if (waitMs > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(waitMs);
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            return tryAcquire();
        }

        int getRequestCount() {
            lock.lock();
            try {
                cleanOldRequests();
                return requestTimestamps.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Clear all rate limit buckets. Useful for testing.
     */
    public static void clearAllBuckets() {
        tokenBuckets.clear();
        slidingWindows.clear();
    }

    /**
     * Clear a specific bucket.
     *
     * @param bucketId the ID of the bucket to clear
     */
    public static void clearBucket(String bucketId) {
        tokenBuckets.remove(bucketId);
        slidingWindows.remove(bucketId);
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
