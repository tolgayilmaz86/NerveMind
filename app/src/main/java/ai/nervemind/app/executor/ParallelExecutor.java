package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.NodeExecutorRegistry;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "parallel" node type - executes multiple branches
 * concurrently.
 *
 * <p>
 * Enables parallel execution of independent operations using Java virtual
 * threads
 * (Project Loom). Branches execute simultaneously and results are combined
 * based on
 * the configured strategy. Ideal for independent API calls or data processing
 * tasks.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Parallel node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>branches</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>List of branch configurations</td>
 * </tr>
 * <tr>
 * <td>combineResults</td>
 * <td>String</td>
 * <td>"merge"</td>
 * <td>How to combine results (see strategies)</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Long</td>
 * <td>60000</td>
 * <td>Max execution time in milliseconds</td>
 * </tr>
 * <tr>
 * <td>failFast</td>
 * <td>Boolean</td>
 * <td>false</td>
 * <td>Cancel all on first failure</td>
 * </tr>
 * </table>
 *
 * <h2>Branch Structure</h2>
 * <p>
 * Each branch in the branches list contains:
 * </p>
 * <ul>
 * <li><strong>name</strong> - Branch identifier for result tracking</li>
 * <li><strong>operations</strong> - List of operations to execute sequentially
 * in this branch</li>
 * </ul>
 *
 * <h2>Operation Structure</h2>
 * <p>
 * Each operation within a branch:
 * </p>
 * <ul>
 * <li><strong>type</strong> - Node type to execute</li>
 * <li><strong>name</strong> - Display name (optional)</li>
 * <li><strong>config</strong> - Configuration for that node type</li>
 * </ul>
 *
 * <h2>Combine Strategies</h2>
 * <ul>
 * <li><strong>merge</strong> - Deep merge all branch results into single
 * object</li>
 * <li><strong>array</strong> - Collect results in an array</li>
 * <li><strong>first</strong> - Return only the first completed result</li>
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
 * <td>result</td>
 * <td>Map/List</td>
 * <td>Combined output based on strategy</td>
 * </tr>
 * <tr>
 * <td>completedBranches</td>
 * <td>List</td>
 * <td>Names of successfully completed branches</td>
 * </tr>
 * <tr>
 * <td>branchCount</td>
 * <td>Integer</td>
 * <td>Total number of branches</td>
 * </tr>
 * <tr>
 * <td>successCount</td>
 * <td>Integer</td>
 * <td>Number of successful branches</td>
 * </tr>
 * <tr>
 * <td>hasErrors</td>
 * <td>Boolean</td>
 * <td>True if any branch failed</td>
 * </tr>
 * <tr>
 * <td>errors</td>
 * <td>Map</td>
 * <td>Branch name → error message (if errors)</td>
 * </tr>
 * </table>
 *
 * <h2>Implementation Notes</h2>
 * <p>
 * Uses {@code Executors.newVirtualThreadPerTaskExecutor()} for lightweight
 * concurrent execution. Each branch runs in its own virtual thread.
 * </p>
 *
 * @see MergeExecutor For synchronizing parallel branches via graph edges
 * @see LoopExecutor For parallel iteration over collections
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 */
@Component
public class ParallelExecutor implements NodeExecutor {

    private final NodeExecutorRegistry nodeExecutorRegistry;

    /**
     * Creates a new parallel executor.
     *
     * @param nodeExecutorRegistry the registry for accessing other node executors
     */
    public ParallelExecutor(@Lazy NodeExecutorRegistry nodeExecutorRegistry) {
        this.nodeExecutorRegistry = nodeExecutorRegistry;
    }

    @Override
    public String getNodeType() {
        return "parallel";
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        // Validate and parse configuration
        ExecutionConfig execConfig = parseExecutionConfig(config);
        if (execConfig.hasValidationError()) {
            return Map.of("error", execConfig.getValidationError(), "input", input);
        }

        // Execute branches in parallel
        ExecutionResult execResult = executeBranchesInParallel(execConfig, input, context);

        // Combine and return results
        return buildFinalOutput(execConfig, execResult, input);
    }

    /**
     * Configuration holder for parallel execution.
     */
    private static class ExecutionConfig {
        private final List<Map<String, Object>> branches;
        private final long timeout;
        private final boolean failFast;
        private final String combineResults;
        private final String validationError;

        public ExecutionConfig(List<Map<String, Object>> branches, long timeout,
                boolean failFast, String combineResults, String validationError) {
            this.branches = branches;
            this.timeout = timeout;
            this.failFast = failFast;
            this.combineResults = combineResults;
            this.validationError = validationError;
        }

        public List<Map<String, Object>> getBranches() {
            return branches;
        }

        public long getTimeout() {
            return timeout;
        }

        public boolean isFailFast() {
            return failFast;
        }

        public String getCombineResults() {
            return combineResults;
        }

        public boolean hasValidationError() {
            return validationError != null;
        }

        public String getValidationError() {
            return validationError;
        }
    }

    /**
     * Result holder for parallel execution.
     */
    private static class ExecutionResult {
        private final Map<String, Object> results;
        private final Map<String, String> errors;
        private final List<String> completedBranches;

        public ExecutionResult(Map<String, Object> results, Map<String, String> errors,
                List<String> completedBranches) {
            this.results = results;
            this.errors = errors;
            this.completedBranches = completedBranches;
        }

        public Map<String, Object> getResults() {
            return results;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public List<String> getCompletedBranches() {
            return completedBranches;
        }
    }

    /**
     * Parses and validates execution configuration.
     * 
     * <p>
     * Supports two modes:
     * </p>
     * <ul>
     * <li><b>Fan-out mode:</b> When "branches" is a number, the parallel node acts
     * as a
     * pass-through that allows the graph engine to follow multiple downstream
     * connections</li>
     * <li><b>Inline mode:</b> When "branches" is a list of branch definitions with
     * operations,
     * each branch is executed in parallel within this node</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private ExecutionConfig parseExecutionConfig(Map<String, Object> config) {
        Object branchesValue = config.get("branches");

        // Fan-out mode: branches is a number (or missing)
        // In this mode, we just pass through - graph engine handles parallel downstream
        // execution
        if (branchesValue == null || branchesValue instanceof Number) {
            // Return empty branches list to trigger pass-through behavior
            return new ExecutionConfig(List.of(), 0, false, "passthrough", null);
        }

        // Inline mode: branches is a list of branch definitions
        if (!(branchesValue instanceof List)) {
            String typeName = branchesValue.getClass().getSimpleName();
            return new ExecutionConfig(null, 0, false, null,
                    "Invalid branches configuration: expected list or number, got " + typeName);
        }

        List<Map<String, Object>> branches = (List<Map<String, Object>>) branchesValue;
        if (branches.isEmpty()) {
            // Empty list also means pass-through
            return new ExecutionConfig(List.of(), 0, false, "passthrough", null);
        }

        long timeout = getLongConfig(config, "timeout", 60000L);
        boolean failFast = getBooleanConfig(config, "failFast", false);
        String combineResults = getStringConfig(config, "combineResults", "merge");

        return new ExecutionConfig(branches, timeout, failFast, combineResults, null);
    }

    /**
     * Executes all branches in parallel using virtual threads.
     */
    private ExecutionResult executeBranchesInParallel(ExecutionConfig config,
            Map<String, Object> input, ExecutionService.ExecutionContext context) {

        // Pass-through mode: no inline branches to execute
        if (config.getBranches().isEmpty()) {
            return new ExecutionResult(Map.of(), Map.of(), List.of());
        }

        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, String> errors = new ConcurrentHashMap<>();
        List<String> completedBranches = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = submitBranchTasks(config, input, context, results, errors, completedBranches,
                    executor);

            waitForCompletionWithTimeout(futures, config.getTimeout(), config.isFailFast(), errors);

        } catch (Exception e) {
            errors.put("_executor", e.getMessage());
        }

        return new ExecutionResult(results, errors, completedBranches);
    }

    /**
     * Submits branch execution tasks to the executor.
     */
    private List<Future<?>> submitBranchTasks(ExecutionConfig config, Map<String, Object> input,
            ExecutionService.ExecutionContext context, Map<String, Object> results,
            Map<String, String> errors, List<String> completedBranches, ExecutorService executor) {

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < config.getBranches().size(); i++) {
            Map<String, Object> branch = config.getBranches().get(i);
            String branchName = (String) branch.getOrDefault("name", "branch_" + i);

            futures.add(executor.submit(() -> executeSingleBranch(branch, branchName, input, context, results, errors,
                    completedBranches, config.isFailFast())));
        }

        return futures;
    }

    /**
     * Executes a single branch and handles errors.
     */
    private void executeSingleBranch(Map<String, Object> branch, String branchName,
            Map<String, Object> input, ExecutionService.ExecutionContext context,
            Map<String, Object> results, Map<String, String> errors,
            List<String> completedBranches, boolean failFast) {

        try {
            Map<String, Object> branchResult = executeBranch(branch, input, context);
            results.put(branchName, branchResult);
            completedBranches.add(branchName);
        } catch (Exception e) {
            errors.put(branchName, e.getMessage());
            if (failFast) {
                throw new RuntimeException("Branch failed: " + branchName, e);
            }
        }
    }

    /**
     * Waits for all futures to complete with timeout handling.
     */
    private void waitForCompletionWithTimeout(List<Future<?>> futures, long timeout,
            boolean failFast, Map<String, String> errors) {

        try {
            for (Future<?> future : futures) {
                future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (TimeoutException e) {
            cancelAllFutures(futures);
            errors.put("_timeout", "Execution timed out after " + timeout + "ms");
        } catch (ExecutionException e) {
            if (failFast) {
                cancelAllFutures(futures);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelAllFutures(futures);
        }
    }

    /**
     * Cancels all pending futures.
     */
    private void cancelAllFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }

    /**
     * Builds the final output based on execution results.
     */
    private Map<String, Object> buildFinalOutput(ExecutionConfig config,
            ExecutionResult result, Map<String, Object> input) {

        // Pass-through mode: just return input data for graph-based parallel execution
        if ("passthrough".equals(config.getCombineResults())) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("_parallelFanOut", true);
            return output;
        }

        Map<String, Object> output = new HashMap<>();
        output.put("completedBranches", result.getCompletedBranches());
        output.put("branchCount", config.getBranches().size());
        output.put("successCount", result.getCompletedBranches().size());
        output.put("hasErrors", !result.getErrors().isEmpty());

        if (!result.getErrors().isEmpty()) {
            output.put("errors", result.getErrors());
        }

        combineResultsBasedOnStrategy(config.getCombineResults(), result.getResults(), input, output);

        return output;
    }

    /**
     * Combines results based on the specified strategy.
     */
    private void combineResultsBasedOnStrategy(String strategy, Map<String, Object> results,
            Map<String, Object> input, Map<String, Object> output) {

        switch (strategy) {
            case "merge" -> output.put("result", mergeResults(results, input));
            case "array" -> output.put("result", new ArrayList<>(results.values()));
            case String s when s.equals("first") && !results.isEmpty() ->
                output.put("result", results.values().iterator().next());
            case "first" -> {
                /* results is empty, do nothing */ }
            default -> output.put("branches", results);
        }
    }

    /**
     * Merges all branch results into a single map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeResults(Map<String, Object> results, Map<String, Object> input) {
        Map<String, Object> merged = new HashMap<>(input);
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map) {
                merged.putAll((Map<String, Object>) map);
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeBranch(
            Map<String, Object> branch,
            Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        List<Map<String, Object>> operations = (List<Map<String, Object>>) branch.get("operations");
        if (operations == null || operations.isEmpty()) {
            return input;
        }

        Map<String, Object> currentData = new HashMap<>(input);

        for (Map<String, Object> operation : operations) {
            String type = (String) operation.get("type");
            if (type == null)
                continue;

            Map<String, Object> opConfig = (Map<String, Object>) operation.getOrDefault("config", Map.of());

            // Create a temporary node for this operation
            Node tempNode = new Node(
                    "parallel_" + UUID.randomUUID().toString().substring(0, 8),
                    type,
                    (String) operation.getOrDefault("name", type),
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
