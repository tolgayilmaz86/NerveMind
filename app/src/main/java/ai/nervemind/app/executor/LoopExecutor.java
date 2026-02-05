package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;

/**
 * Executor for the "loop" node type - iterates over arrays or collections.
 *
 * <p>Enables processing of multiple items by iterating through a collection
 * and executing child operations for each item. Supports both sequential
 * and parallel execution modes using Java virtual threads.</p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Loop node configuration parameters</caption>
 * <tr><th>Parameter</th><th>Type</th><th>Default</th><th>Description</th></tr>
 * <tr><td>items</td><td>String</td><td>"items"</td><td>Field name containing
 * the array to iterate</td></tr>
 * <tr><td>parallel</td><td>Boolean</td><td>false</td><td>Enable virtual thread
 * parallelism</td></tr>
 * <tr><td>batchSize</td><td>Integer</td><td>10</td><td>Items processed
 * concurrently when parallel=true</td></tr>
 * </table>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr><th>Key</th><th>Type</th><th>Description</th></tr>
 * <tr><td>results</td><td>List</td><td>List of result objects, each with "item"
 * and "index"</td></tr>
 * <tr><td>count</td><td>Integer</td><td>Number of items processed</td></tr>
 * </table>
 *
 * <h2>Result Item Structure</h2>
 * <p>Each item in the results list contains:</p>
 * <ul>
 * <li><strong>item</strong> - The original item from the input array</li>
 * <li><strong>index</strong> - Zero-based index of the item in the original
 * array</li>
 * </ul>
 *
 * <h2>Parallel Execution</h2>
 * <p>When {@code parallel=true}, items are processed using virtual threads
 * (requires Java 21+). The batch size controls how many virtual threads
 * are spawned concurrently:</p>
 * <pre>{@code
 * // Configuration for parallel processing with batches of 5
 * {
 * "parallel": true,
 * "batchSize": 5,
 * "items": "dataArray"
 * }
 * }</pre>
 *
 * @see ParallelExecutor For branch-based parallel execution
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 */
@Component
public class LoopExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public LoopExecutor() {
        // Default constructor
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String itemsField = (String) params.getOrDefault("items", "items");
        boolean parallel = (boolean) params.getOrDefault("parallel", false);
        int batchSize = (int) params.getOrDefault("batchSize", 10);

        Object itemsObj = input.get(itemsField);
        if (itemsObj == null) {
            return Map.of("results", List.of(), "count", 0);
        }

        List<?> items;
        if (itemsObj instanceof List<?> list) {
            items = list;
        } else if (itemsObj.getClass().isArray()) {
            items = java.util.Arrays.asList((Object[]) itemsObj);
        } else {
            items = List.of(itemsObj);
        }

        List<Map<String, Object>> results;

        if (parallel && items.size() > 1) {
            // Use virtual threads for parallel processing
            results = executeParallel(items, input, batchSize);
        } else {
            // Sequential execution
            results = executeSequential(items, input);
        }

        Map<String, Object> output = new HashMap<>(input);
        output.put("results", results);
        output.put("count", results.size());
        return output;
    }

    private List<Map<String, Object>> executeSequential(List<?> items, Map<String, Object> input) {
        List<Map<String, Object>> results = new ArrayList<>();
        int index = 0;
        for (Object item : items) {
            // Start with parent input context so child nodes can access parent data
            Map<String, Object> itemResult = new HashMap<>(input);
            // Override with current loop item data
            itemResult.put("item", item);
            itemResult.put("index", index);
            itemResult.put("_loopIndex", index);
            itemResult.put("_loopTotal", items.size());
            itemResult.put("_isFirst", index == 0);
            itemResult.put("_isLast", index == items.size() - 1);
            index++;
            results.add(itemResult);
        }
        return results;
    }

    private List<Map<String, Object>> executeParallel(List<?> items, Map<String, Object> input, int batchSize) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Process in batches using virtual threads
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<?> batch = items.subList(i, end);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int startIndex = i;
                List<Future<Map<String, Object>>> futures = new ArrayList<>();

                for (int j = 0; j < batch.size(); j++) {
                    Object item = batch.get(j);
                    int index = startIndex + j;

                    final int totalItems = items.size();
                    // Each item processed in its own virtual thread
                    futures.add(executor.submit(() -> {
                        // Start with parent input context so child nodes can access parent data
                        Map<String, Object> itemResult = new HashMap<>(input);
                        // Override with current loop item data
                        itemResult.put("item", item);
                        itemResult.put("index", index);
                        itemResult.put("_loopIndex", index);
                        itemResult.put("_loopTotal", totalItems);
                        itemResult.put("_isFirst", index == 0);
                        itemResult.put("_isLast", index == totalItems - 1);
                        return itemResult;
                    }));
                }

                // Collect results
                for (var future : futures) {
                    results.add(future.get());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NodeExecutionException(
                        "Parallel loop execution interrupted", null, "loop", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new NodeExecutionException(
                        "Parallel loop execution failed: " + e.getCause().getMessage(),
                        null, "loop", e.getCause());
            }
        }

        return results;
    }

    @Override
    public String getNodeType() {
        return "loop";
    }
}
