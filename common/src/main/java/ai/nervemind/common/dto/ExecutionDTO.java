package ai.nervemind.common.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.nervemind.common.domain.Execution;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;

/**
 * Data Transfer Object for Execution.
 * Used for API communication and UI display.
 *
 * @param id             Execution unique identifier
 * @param workflowId     ID of the executed workflow
 * @param workflowName   Name of the workflow
 * @param status         Current execution status
 * @param triggerType    How the execution was triggered
 * @param startedAt      Start timestamp
 * @param finishedAt     Completion timestamp
 * @param durationMs     Duration in milliseconds
 * @param inputData      Input data to the workflow
 * @param outputData     Output data from the workflow
 * @param errorMessage   Error message if failed
 * @param nodeExecutions Per-node execution details
 */
public record ExecutionDTO(
        Long id,
        Long workflowId,
        String workflowName,
        ExecutionStatus status,
        TriggerType triggerType,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        Map<String, Object> inputData,
        Map<String, Object> outputData,
        String errorMessage,
        List<NodeExecutionDTO> nodeExecutions) {

    /**
     * Per-node execution DTO.
     * 
     * @param nodeId       Unique node identifier
     * @param nodeName     Display name of the node
     * @param nodeType     Type of the node
     * @param status       Status of this node execution
     * @param startedAt    When node execution started
     * @param finishedAt   When node execution completed
     * @param durationMs   Duration in milliseconds
     * @param inputData    Input data passed to this node
     * @param outputData   Output data produced by this node
     * @param errorMessage Error message if node execution failed
     */
    public record NodeExecutionDTO(
            String nodeId,
            String nodeName,
            String nodeType,
            ExecutionStatus status,
            Instant startedAt,
            Instant finishedAt,
            Long durationMs,
            Map<String, Object> inputData,
            Map<String, Object> outputData,
            String errorMessage) {
        /**
         * Create from domain object.
         *
         * @param nodeExec The domain node execution
         * @return The DTO representation
         */
        public static NodeExecutionDTO from(Execution.NodeExecution nodeExec) {
            return new NodeExecutionDTO(
                    nodeExec.nodeId(),
                    nodeExec.nodeName(),
                    nodeExec.nodeType(),
                    nodeExec.status(),
                    nodeExec.startedAt(),
                    nodeExec.finishedAt(),
                    nodeExec.duration().toMillis(),
                    nodeExec.inputData(),
                    nodeExec.outputData(),
                    nodeExec.errorMessage());
        }
    }

    /**
     * Compact constructor.
     */
    public ExecutionDTO {
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
     * Create from domain Execution object.
     *
     * @param execution The domain execution object
     * @return The DTO representation
     */
    public static ExecutionDTO from(Execution execution) {
        return new ExecutionDTO(
                execution.id(),
                execution.workflowId(),
                execution.workflowName(),
                execution.status(),
                execution.triggerType(),
                execution.startedAt(),
                execution.finishedAt(),
                execution.duration().toMillis(),
                execution.inputData(),
                execution.outputData(),
                execution.errorMessage(),
                execution.nodeExecutions().stream()
                        .map(NodeExecutionDTO::from)
                        .toList());
    }

    /**
     * Check if execution is still running.
     *
     * @return true if status is RUNNING, PAUSED, or QUEUED
     */
    public boolean isRunning() {
        return status.isRunning();
    }

    /**
     * Check if execution completed.
     *
     * @return true if status is SUCCESS, FAILED, or CANCELLED
     */
    public boolean isComplete() {
        return status.isTerminal();
    }

    /**
     * Get duration as Duration object.
     *
     * @return Execution duration
     */
    public Duration duration() {
        return durationMs != null ? Duration.ofMillis(durationMs) : Duration.ZERO;
    }
}
