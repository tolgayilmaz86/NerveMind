package ai.nervemind.common.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;

/**
 * Represents a single execution of a workflow.
 * 
 * <p>
 * An execution is created when a workflow is triggered (manually, by schedule,
 * or by event). It tracks the overall status and timing, as well as individual
 * node executions.
 * </p>
 * 
 * <h2>Status Lifecycle</h2>
 * 
 * <pre>
 * ┌─────────┐     ┌─────────┐     ┌─────────┐
 * │ PENDING │────▶│ RUNNING │────▶│ SUCCESS │
 * └─────────┘     └────┬────┘     └─────────┘
 *                      │
 *                      ├────────▶ ┌────────┐
 *                      │          │ FAILED │
 *                      │          └────────┘
 *                      │
 *                      └────────▶ ┌───────────┐
 *                                 │ CANCELLED │
 *                                 └───────────┘
 * </pre>
 * 
 * <h2>Node Executions</h2>
 * <p>
 * Each node execution is tracked separately with its own input, output,
 * timing, and status. This allows for detailed debugging and retry logic.
 * </p>
 * 
 * @param id             Unique execution identifier
 * @param workflowId     ID of the workflow being executed
 * @param workflowName   Name of the workflow (for display)
 * @param status         Current execution status
 * @param triggerType    How the execution was triggered
 * @param startedAt      When execution started
 * @param finishedAt     When execution completed (null if still running)
 * @param inputData      Initial input data to the workflow
 * @param outputData     Final output data from the workflow
 * @param errorMessage   Error message if execution failed
 * @param nodeExecutions List of per-node execution details
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ExecutionStatus Execution status values
 * @see NodeExecution Per-node execution details
 */
public record Execution(
        Long id,
        Long workflowId,
        String workflowName,
        ExecutionStatus status,
        TriggerType triggerType,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> inputData,
        Map<String, Object> outputData,
        String errorMessage,
        List<NodeExecution> nodeExecutions) {
    /**
     * Per-node execution details.
     * 
     * @param nodeId       Unique node identifier
     * @param nodeName     Display name of the node
     * @param nodeType     Type of the node
     * @param status       Status of this node execution
     * @param startedAt    When node execution started
     * @param finishedAt   When node execution completed
     * @param inputData    Input data passed to this node
     * @param outputData   Output data produced by this node
     * @param errorMessage Error message if node execution failed
     */
    public record NodeExecution(
            String nodeId,
            String nodeName,
            String nodeType,
            ExecutionStatus status,
            Instant startedAt,
            Instant finishedAt,
            Map<String, Object> inputData,
            Map<String, Object> outputData,
            String errorMessage) {
        /**
         * Calculate execution duration.
         * 
         * @return the duration of node execution
         */
        public Duration duration() {
            if (startedAt == null)
                return Duration.ZERO;
            Instant end = finishedAt != null ? finishedAt : Instant.now();
            return Duration.between(startedAt, end);
        }
    }

    /**
     * Compact constructor with validation.
     */
    public Execution {
        if (workflowId == null) {
            throw new IllegalArgumentException("Workflow id cannot be null");
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
        if (nodeExecutions == null) {
            nodeExecutions = List.of();
        }
    }

    /**
     * Calculate total execution duration.
     * 
     * @return the total duration of the execution
     */
    public Duration duration() {
        if (startedAt == null)
            return Duration.ZERO;
        Instant end = finishedAt != null ? finishedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    /**
     * Check if execution is still in progress.
     * 
     * @return true if currently running
     */
    public boolean isRunning() {
        return status.isRunning();
    }

    /**
     * Check if execution has completed (success, failure, or cancelled).
     * 
     * @return true if execution has finished
     */
    public boolean isComplete() {
        return status.isTerminal();
    }

    /**
     * Create a copy with updated status.
     * 
     * @param newStatus the new status
     * @return a new Execution instance
     */
    public Execution withStatus(ExecutionStatus newStatus) {
        return new Execution(
                id, workflowId, workflowName, newStatus, triggerType,
                startedAt, finishedAt, inputData, outputData, errorMessage, nodeExecutions);
    }

    /**
     * Create a copy marked as finished.
     * 
     * @param finalStatus the final status
     * @param finishTime  the completion time
     * @param output      the final output data
     * @param error       the error message if failed
     * @return a new Execution instance
     */
    public Execution withFinished(ExecutionStatus finalStatus, Instant finishTime, Map<String, Object> output,
            String error) {
        return new Execution(
                id, workflowId, workflowName, finalStatus, triggerType,
                startedAt, finishTime, inputData, output, error, nodeExecutions);
    }
}
