package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "merge" node type - combines data from multiple input
 * branches.
 *
 * <p>
 * Handles synchronization of parallel workflow branches by collecting and
 * combining data from multiple incoming edges. Essential for rejoining parallel
 * execution paths created by {@link ParallelExecutor} or graph structure.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Merge node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>mode</td>
 * <td>String</td>
 * <td>"waitAll"</td>
 * <td>How to merge inputs (see modes below)</td>
 * </tr>
 * <tr>
 * <td>inputCount</td>
 * <td>Integer</td>
 * <td>2</td>
 * <td>Expected number of inputs (for waitAll mode)</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Integer</td>
 * <td>300</td>
 * <td>Timeout in seconds for waiting</td>
 * </tr>
 * <tr>
 * <td>outputKey</td>
 * <td>String</td>
 * <td>"merged"</td>
 * <td>Key under which to store merged data</td>
 * </tr>
 * <tr>
 * <td>waitForAll</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>If false, proceed with first input (for exclusive/conditional
 * branches)</td>
 * </tr>
 * </table>
 *
 * <h2>Merge Modes</h2>
 * <ul>
 * <li><strong>waitAll</strong> - Wait for all expected inputs before proceeding
 * (uses CountDownLatch for synchronization)</li>
 * <li><strong>waitAny</strong> - Proceed as soon as any input arrives</li>
 * <li><strong>append</strong> - Append all inputs to a list</li>
 * <li><strong>merge</strong> - Deep merge all input objects (later inputs
 * override earlier)</li>
 * <li><strong>passthrough</strong> - Pass through input directly without
 * nesting.
 * When waitForAll=false, proceeds with first input (ideal for IF/conditional
 * branches)</li>
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
 * <td>[outputKey]</td>
 * <td>Map/List</td>
 * <td>Combined data based on mode</td>
 * </tr>
 * <tr>
 * <td>_inputCount</td>
 * <td>Integer</td>
 * <td>Number of inputs that were merged</td>
 * </tr>
 * <tr>
 * <td>_mergeMode</td>
 * <td>String</td>
 * <td>The mode used for merging</td>
 * </tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Uses {@link ConcurrentHashMap} and {@link CountDownLatch} to handle
 * concurrent input from multiple threads in parallel execution scenarios.
 * </p>
 *
 * @see ParallelExecutor For spawning parallel branches
 */
@Component
public class MergeExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public MergeExecutor() {
        // Default constructor
    }

    private static final Logger log = LoggerFactory.getLogger(MergeExecutor.class);
    private static final String MODE_PASSTHROUGH = "passThrough";
    private static final String MODE_WAIT_ALL = "waitAll";
    private static final String KEY_MERGE_MODE = "_mergeMode";
    private static final String KEY_INPUTS_RECEIVED = "_inputsReceived";
    private static final String KEY_INPUTS_EXPECTED = "_inputsExpected";
    private static final String KEY_INPUTS_EXPECTED_LOG = "inputsExpected";
    private static final String KEY_INPUTS_RECEIVED_LOG = "inputsReceived";
    private static final String KEY_TIMED_OUT = "_timedOut";
    private static final String MODE_APPEND = "append";
    private static final String MODE_MERGE = "merge";
    private static final String MODE_WAIT_ANY = "waitAny";
    private static final String KEY_NODE_NAME = "nodeName";

    // Store pending inputs per execution context
    private final ConcurrentHashMap<String, MergeState> pendingMerges = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String mode = (String) params.getOrDefault("mode", MODE_WAIT_ALL);
        int timeout = ((Number) params.getOrDefault("timeout", 300)).intValue();
        String outputKey = (String) params.getOrDefault("outputKey", "merged");
        // waitForAll=false means exclusive/conditional branches - proceed with first
        // input
        boolean waitForAll = (Boolean) params.getOrDefault("waitForAll", true);

        // Calculate expected input count from workflow connections
        int inputCount = calculateExpectedInputCount(node, context);

        // Create unique key for this merge point in this execution
        String mergeKey = context.getExecutionId() + ":" + node.id();

        // Get or create merge state
        MergeState state = pendingMerges.computeIfAbsent(mergeKey,
                k -> new MergeState(inputCount, timeout, node.name(), context));

        // Log the input arrival (skip if exclusive mode - we proceed immediately)
        if (waitForAll || !mode.equalsIgnoreCase("passthrough")) {
            logInputReceived(context, node, state, inputCount);
        }

        return switch (mode.toLowerCase()) {
            case "waitany" -> handleWaitAny(input, outputKey);
            case MODE_APPEND -> handleAppend(mergeKey, input, state, outputKey, waitForAll);
            case MODE_MERGE -> handleMerge(mergeKey, input, state, outputKey, waitForAll);
            case "passthrough" -> handlePassThrough(mergeKey, input, state, waitForAll);
            default -> handleWaitAll(mergeKey, input, state, outputKey);
        };
    }

    private Map<String, Object> handleWaitAny(Map<String, Object> input, String outputKey) {
        // Just pass through the first input that arrives
        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, input);
        output.put(KEY_MERGE_MODE, MODE_WAIT_ANY);
        output.put(KEY_INPUTS_RECEIVED, 1);
        return output;
    }

    private Map<String, Object> handleWaitAll(String mergeKey, Map<String, Object> input,
            MergeState state, String outputKey) {

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs());
            output.put(KEY_MERGE_MODE, MODE_WAIT_ALL);
            output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
            return output;
        }

        // Wait for more inputs
        try {
            if (state.waitForCompletion()) {
                pendingMerges.remove(mergeKey);

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, state.getInputs());
                output.put(KEY_MERGE_MODE, MODE_WAIT_ALL);
                output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
                return output;
            } else {
                // Timeout - return what we have
                pendingMerges.remove(mergeKey);

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, state.getInputs());
                output.put(KEY_MERGE_MODE, MODE_WAIT_ALL);
                output.put(KEY_TIMED_OUT, true);
                output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
                output.put(KEY_INPUTS_EXPECTED, state.getExpectedCount());
                return output;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs());
            output.put(KEY_MERGE_MODE, MODE_WAIT_ALL);
            output.put("_interrupted", true);
            return output;
        }
    }

    private Map<String, Object> handleAppend(String mergeKey, Map<String, Object> input,
            MergeState state, String outputKey, boolean waitForAll) {

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, state.getInputs()); // List of all inputs
            output.put(KEY_MERGE_MODE, MODE_APPEND);
            output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
            return output;
        }

        try {
            // When waitForAll=false, use timeout-based waiting
            boolean completed = state.waitForCompletion();
            if (!completed && !waitForAll) {
                // Timeout reached and waitForAll=false - proceed with partial results
                pendingMerges.remove(mergeKey);

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, state.getInputs());
                output.put(KEY_MERGE_MODE, MODE_APPEND);
                output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
                output.put(KEY_INPUTS_EXPECTED, state.getExpectedCount());
                output.put(KEY_TIMED_OUT, true);
                output.put("_partialResults", true);
                return output;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        pendingMerges.remove(mergeKey);

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, state.getInputs());
        output.put(KEY_MERGE_MODE, MODE_APPEND);
        output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
        return output;
    }

    private Map<String, Object> handleMerge(String mergeKey, Map<String, Object> input,
            MergeState state, String outputKey, boolean waitForAll) {

        state.addInput(input);

        if (state.isComplete()) {
            pendingMerges.remove(mergeKey);

            // Merge all inputs into one map
            Map<String, Object> merged = new HashMap<>();
            for (Map<String, Object> inputMap : state.getInputs()) {
                merged.putAll(inputMap);
            }

            Map<String, Object> output = new HashMap<>();
            output.put(outputKey, merged);
            output.put(KEY_MERGE_MODE, MODE_MERGE);
            output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
            return output;
        }

        try {
            // When waitForAll=false, use timeout-based waiting
            boolean completed = state.waitForCompletion();
            if (!completed && !waitForAll) {
                // Timeout reached and waitForAll=false - proceed with partial results
                pendingMerges.remove(mergeKey);

                Map<String, Object> merged = new HashMap<>();
                for (Map<String, Object> inputMap : state.getInputs()) {
                    merged.putAll(inputMap);
                }

                Map<String, Object> output = new HashMap<>();
                output.put(outputKey, merged);
                output.put(KEY_MERGE_MODE, MODE_MERGE);
                output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
                output.put(KEY_INPUTS_EXPECTED, state.getExpectedCount());
                output.put(KEY_TIMED_OUT, true);
                output.put("_partialResults", true);
                return output;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        pendingMerges.remove(mergeKey);

        // Merge all inputs into one map
        Map<String, Object> merged = new HashMap<>();
        for (Map<String, Object> inputMap : state.getInputs()) {
            merged.putAll(inputMap);
        }

        Map<String, Object> output = new HashMap<>();
        output.put(outputKey, merged);
        output.put(KEY_MERGE_MODE, MODE_MERGE);
        output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
        return output;
    }

    /**
     * Pass-through mode: merges all inputs directly into output without nesting.
     * This is ideal for combining parallel branch results where each branch
     * produces different keys that should all be available at the top level.
     * 
     * <p>
     * When waitForAll=false, proceeds immediately with the first input.
     * This is useful for exclusive/conditional branches (IF nodes) where only
     * one branch will ever execute.
     * </p>
     * 
     * <p>
     * When waitForAll=true (default), only the thread that completes the merge
     * (adds the final input) will continue execution. Other threads receive a
     * "_stopExecution" flag.
     * </p>
     */
    private Map<String, Object> handlePassThrough(String mergeKey, Map<String, Object> input,
            MergeState state, boolean waitForAll) {

        // If not waiting for all, proceed immediately with first input (exclusive
        // branches)
        if (!waitForAll) {
            log.debug("Merge '{}': Exclusive mode - proceeding with first input", state.getNodeName());
            pendingMerges.remove(mergeKey);

            Map<String, Object> output = new HashMap<>(input);
            output.put(KEY_MERGE_MODE, MODE_PASSTHROUGH);
            output.put(KEY_INPUTS_RECEIVED, 1);
            output.put("_exclusive", true);
            return output;
        }

        boolean isPrimaryThread = state.addInputAndCheckPrimary(input);

        if (!isPrimaryThread) {
            // Wait for primary thread to complete, then return with stop flag
            try {
                state.waitForCompletion();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
            // Non-primary threads should not continue downstream
            return Map.of("_stopExecution", true, KEY_MERGE_MODE, MODE_PASSTHROUGH);
        }

        // Primary thread: wait for all inputs then continue
        try {
            state.waitForCompletion();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

        pendingMerges.remove(mergeKey);
        return buildPassThroughOutput(state);
    }

    private Map<String, Object> buildPassThroughOutput(MergeState state) {
        // Merge all inputs directly into output (no nesting under a key)
        Map<String, Object> output = new HashMap<>();
        for (Map<String, Object> inputMap : state.getInputs()) {
            output.putAll(inputMap);
        }
        output.put(KEY_MERGE_MODE, MODE_PASSTHROUGH);
        output.put(KEY_INPUTS_RECEIVED, state.getInputs().size());
        return output;
    }

    /**
     * Calculate expected input count by counting incoming connections to this node.
     */
    private int calculateExpectedInputCount(Node node, ExecutionService.ExecutionContext context) {
        // First check if explicitly set in parameters
        Map<String, Object> params = node.parameters();
        if (params.containsKey("inputCount")) {
            return ((Number) params.get("inputCount")).intValue();
        }

        // Count incoming connections from the workflow
        var workflow = context.getWorkflow();
        if (workflow != null && workflow.connections() != null) {
            long connectionCount = workflow.connections().stream()
                    .filter(conn -> conn.targetNodeId().equals(node.id()))
                    .count();
            if (connectionCount > 0) {
                log.debug("Merge node '{}': Found {} incoming connections", node.name(), connectionCount);
                return (int) connectionCount;
            }
        }

        // Default fallback
        return 2;
    }

    /**
     * Log when an input is received at the merge node.
     */
    private void logInputReceived(ExecutionService.ExecutionContext context, Node node,
            MergeState state, int expectedCount) {
        int received = state.getCurrentInputCount() + 1; // +1 because this input hasn't been added yet
        String message = String.format("⏳ Merge '%s': Waiting for inputs (%d/%d received)",
                node.name(), received, expectedCount);

        ExecutionLogger logger = context.getExecutionLogger();
        if (logger != null) {
            logger.custom(String.valueOf(context.getExecutionId()),
                    ExecutionLogger.LogLevel.INFO,
                    message,
                    Map.of(
                            "nodeId", node.id(),
                            KEY_NODE_NAME, node.name(),
                            KEY_INPUTS_RECEIVED_LOG, received,
                            KEY_INPUTS_EXPECTED_LOG, expectedCount,
                            "remaining", expectedCount - received));
        }

        log.info(message);
    }

    @Override
    public String getNodeType() {
        return MODE_MERGE;
    }

    private static class MergeState {
        private final List<Map<String, Object>> inputs = Collections.synchronizedList(new ArrayList<>());
        private final int expectedCount;
        private final int timeoutSeconds;
        private final CountDownLatch latch;
        private volatile boolean primaryAssigned = false;
        private final Object primaryLock = new Object();
        private final String nodeName;
        private final ExecutionService.ExecutionContext context;

        MergeState(int expectedCount, int timeoutSeconds, String nodeName,
                ExecutionService.ExecutionContext context) {
            this.expectedCount = expectedCount;
            this.timeoutSeconds = timeoutSeconds;
            this.latch = new CountDownLatch(expectedCount);
            this.nodeName = nodeName;
            this.context = context;
        }

        /**
         * Get current input count (before adding new input).
         */
        int getCurrentInputCount() {
            return inputs.size();
        }

        /**
         * Add input and determine if this thread is the primary (last to arrive).
         * 
         * @return true if this thread should continue downstream
         */
        boolean addInputAndCheckPrimary(Map<String, Object> input) {
            inputs.add(new HashMap<>(input));
            latch.countDown();
            logProgress();

            // The thread that adds the final input becomes the primary
            synchronized (primaryLock) {
                if (inputs.size() >= expectedCount && !primaryAssigned) {
                    primaryAssigned = true;
                    return true;
                }
            }
            return false;
        }

        void addInput(Map<String, Object> input) {
            inputs.add(new HashMap<>(input));
            latch.countDown();
            logProgress();
        }

        private void logProgress() {
            int received = inputs.size();
            if (received < expectedCount) {
                String message = String.format("⏳ Merge '%s': Still waiting... (%d/%d received, %d remaining)",
                        nodeName, received, expectedCount, expectedCount - received);

                ExecutionLogger logger = context.getExecutionLogger();
                if (logger != null) {
                    logger.custom(String.valueOf(context.getExecutionId()),
                            ExecutionLogger.LogLevel.DEBUG,
                            message,
                            Map.of(
                                    KEY_NODE_NAME, nodeName,
                                    KEY_INPUTS_RECEIVED_LOG, received,
                                    KEY_INPUTS_EXPECTED_LOG, expectedCount));
                }
            } else {
                String message = String.format("✓ Merge '%s': All inputs received (%d/%d)",
                        nodeName, received, expectedCount);

                ExecutionLogger logger = context.getExecutionLogger();
                if (logger != null) {
                    logger.custom(String.valueOf(context.getExecutionId()),
                            ExecutionLogger.LogLevel.INFO,
                            message,
                            Map.of(
                                    KEY_NODE_NAME, nodeName,
                                    KEY_INPUTS_RECEIVED_LOG, received,
                                    KEY_INPUTS_EXPECTED_LOG, expectedCount));
                }
            }
        }

        boolean isComplete() {
            return inputs.size() >= expectedCount;
        }

        int getExpectedCount() {
            return expectedCount;
        }

        String getNodeName() {
            return nodeName;
        }

        boolean waitForCompletion() throws InterruptedException {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        }

        List<Map<String, Object>> getInputs() {
            return new ArrayList<>(inputs);
        }
    }
}
