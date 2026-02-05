package ai.nervemind.app.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.WorkflowService;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;

/**
 * Executor for the "subworkflow" node type - executes another workflow as a
 * nested operation.
 *
 * <p>
 * Enables modular workflow composition by invoking other workflows as
 * subroutines.
 * Supports input/output mapping to pass data between parent and child
 * workflows,
 * with optional async execution for fire-and-forget scenarios.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Subworkflow node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>workflowId</td>
 * <td>Long/String</td>
 * <td>-</td>
 * <td>ID or name of workflow to execute</td>
 * </tr>
 * <tr>
 * <td>workflowName</td>
 * <td>String</td>
 * <td>-</td>
 * <td>Name of workflow (alternative to ID)</td>
 * </tr>
 * <tr>
 * <td>inputMapping</td>
 * <td>Map</td>
 * <td>{}</td>
 * <td>Map subworkflow inputs to expressions</td>
 * </tr>
 * <tr>
 * <td>outputMapping</td>
 * <td>Map</td>
 * <td>{}</td>
 * <td>Map parent outputs to subworkflow paths</td>
 * </tr>
 * <tr>
 * <td>waitForCompletion</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Wait for subworkflow to complete</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Long</td>
 * <td>300000</td>
 * <td>Max execution time (5 min default)</td>
 * </tr>
 * </table>
 *
 * <h2>Input Mapping</h2>
 * <p>
 * Maps parent workflow data to subworkflow inputs:
 * </p>
 * 
 * <pre>{@code
 * "inputMapping": {
 *   "userId": "$.user.id",      // JSONPath-like reference
 *   "config": "configData",     // Direct field reference
 *   "static": "hardcoded"       // Static value
 * }
 * }</pre>
 *
 * <h2>Output Mapping</h2>
 * <p>
 * Maps subworkflow outputs back to parent workflow:
 * </p>
 * 
 * <pre>{@code
 * "outputMapping": {
 *   "result": "$.processedData",
 *   "status": "status"
 * }
 * }</pre>
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
 * <td>executionId</td>
 * <td>Long</td>
 * <td>ID of the subworkflow execution</td>
 * </tr>
 * <tr>
 * <td>status</td>
 * <td>String</td>
 * <td>Execution status (SUCCESS, FAILED, etc.)</td>
 * </tr>
 * <tr>
 * <td>success</td>
 * <td>Boolean</td>
 * <td>Whether subworkflow succeeded</td>
 * </tr>
 * <tr>
 * <td>output</td>
 * <td>Map</td>
 * <td>Mapped output from subworkflow</td>
 * </tr>
 * <tr>
 * <td>rawOutput</td>
 * <td>Map</td>
 * <td>Complete subworkflow output</td>
 * </tr>
 * <tr>
 * <td>error</td>
 * <td>String</td>
 * <td>Error message (if failed)</td>
 * </tr>
 * </table>
 *
 * <h2>Recursion Protection</h2>
 * <p>
 * The executor prevents infinite recursion by checking if the subworkflow
 * being invoked is the same as the currently executing workflow.
 * </p>
 *
 * @see ai.nervemind.app.service.ExecutionService For workflow execution
 * @see ai.nervemind.app.service.WorkflowService For workflow lookup
 */
@Component
public class SubworkflowExecutor implements NodeExecutor {

    private final WorkflowService workflowService;
    private final ApplicationContext applicationContext;

    // Use lazy loading to avoid circular dependency
    private ExecutionService executionService;

    /**
     * Creates a new subworkflow executor.
     *
     * @param workflowService    the workflow service for workflow access
     * @param applicationContext the Spring application context
     */
    public SubworkflowExecutor(WorkflowService workflowService, ApplicationContext applicationContext) {
        this.workflowService = workflowService;
        this.applicationContext = applicationContext;
    }

    private ExecutionService getExecutionService() {
        if (executionService == null) {
            executionService = applicationContext.getBean(ExecutionService.class);
        }
        return executionService;
    }

    @Override
    public String getNodeType() {
        return "subworkflow";
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> config = node.parameters();

        // Get workflow to execute
        Long workflowId = getWorkflowId(config);
        if (workflowId == null) {
            throw new IllegalArgumentException("Subworkflow node requires 'workflowId' configuration");
        }

        // Prevent infinite recursion - check if we're already executing this workflow
        Long currentWorkflowId = context.getWorkflow().id();
        if (workflowId.equals(currentWorkflowId)) {
            throw new IllegalStateException("Subworkflow cannot call itself (recursive execution detected)");
        }

        // Verify workflow exists
        Optional<WorkflowDTO> subworkflow = workflowService.findById(workflowId);
        if (subworkflow.isEmpty()) {
            throw new IllegalArgumentException("Subworkflow not found: " + workflowId);
        }

        // Build subworkflow input from input mapping
        Map<String, Object> subworkflowInput = buildSubworkflowInput(config, input);

        // Check if we should wait for completion
        boolean waitForCompletion = getBooleanConfig(config, "waitForCompletion", true);

        Map<String, Object> result = new HashMap<>();

        if (waitForCompletion) {
            // Execute synchronously and wait for result
            ExecutionDTO execution = getExecutionService().execute(workflowId, subworkflowInput);

            result.put("executionId", execution.id());
            result.put("status", execution.status().name());
            result.put("success", execution.status() == ExecutionStatus.SUCCESS);

            if (execution.status() == ExecutionStatus.SUCCESS) {
                // Apply output mapping
                Map<String, Object> subOutput = execution.outputData();
                result.put("output", applyOutputMapping(config, subOutput));
                result.put("rawOutput", subOutput);
            } else {
                result.put("error", execution.errorMessage());
                result.put("output", Map.of());
            }

            result.put("startedAt", execution.startedAt() != null ? execution.startedAt().toString() : null);
            result.put("finishedAt", execution.finishedAt() != null ? execution.finishedAt().toString() : null);
            result.put("durationMs", execution.durationMs());
        } else {
            // Execute asynchronously
            getExecutionService().executeAsync(workflowId, subworkflowInput);

            result.put("async", true);
            result.put("message", "Subworkflow started asynchronously");
            result.put("workflowId", workflowId);
            result.put("workflowName", subworkflow.get().name());

            // The caller can check execution history later
        }

        return result;
    }

    private Long getWorkflowId(Map<String, Object> config) {
        Object idObj = config.get("workflowId");
        if (idObj == null) {
            // Try getting by name
            String name = (String) config.get("workflowName");
            if (name != null) {
                return workflowService.findByName(name)
                        .map(WorkflowDTO::id)
                        .orElse(null);
            }
            return null;
        }
        if (idObj instanceof Number num) {
            return num.longValue();
        }
        if (idObj instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException _) {
                // Try as workflow name
                return workflowService.findByName(str)
                        .map(WorkflowDTO::id)
                        .orElse(null);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSubworkflowInput(Map<String, Object> config, Map<String, Object> parentInput) {
        Map<String, Object> inputMapping = (Map<String, Object>) config.get("inputMapping");
        if (inputMapping == null || inputMapping.isEmpty()) {
            // Pass through all parent input
            return new HashMap<>(parentInput);
        }

        Map<String, Object> subInput = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputMapping.entrySet()) {
            String targetKey = entry.getKey();
            Object sourceValue = entry.getValue();

            if (sourceValue instanceof String expr) {
                // Simple expression: direct key reference
                if (expr.startsWith("$.")) {
                    String path = expr.substring(2);
                    subInput.put(targetKey, getNestedValue(parentInput, path));
                } else {
                    subInput.put(targetKey, parentInput.getOrDefault(expr, expr));
                }
            } else {
                // Direct value
                subInput.put(targetKey, sourceValue);
            }
        }

        return subInput;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyOutputMapping(Map<String, Object> config, Map<String, Object> subOutput) {
        Map<String, Object> outputMapping = (Map<String, Object>) config.get("outputMapping");
        if (outputMapping == null || outputMapping.isEmpty()) {
            // Return all subworkflow output
            return subOutput != null ? new HashMap<>(subOutput) : Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : outputMapping.entrySet()) {
            String targetKey = entry.getKey();
            Object sourceExpr = entry.getValue();
            result.put(targetKey, resolveOutputValue(sourceExpr, subOutput));
        }

        return result;
    }

    private Object resolveOutputValue(Object sourceExpr, Map<String, Object> subOutput) {
        if (sourceExpr instanceof String path) {
            if (path.startsWith("$.")) {
                return getNestedValue(subOutput, path.substring(2));
            }
            return subOutput != null ? subOutput.get(path) : null;
        }
        return sourceExpr;
    }

    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map<?, ?> m) {
                current = m.get(part);
            } else {
                return null;
            }
        }

        return current;
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
}
