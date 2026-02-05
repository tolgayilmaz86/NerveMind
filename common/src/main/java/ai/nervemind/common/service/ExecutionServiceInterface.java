package ai.nervemind.common.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ai.nervemind.common.dto.ExecutionDTO;

/**
 * Service interface for workflow execution operations.
 * 
 * <p>
 * This interface provides the contract for executing workflows and querying
 * their execution history. It is defined in the {@code common} module to allow
 * the {@code ui} module to depend on it without circular dependencies.
 * </p>
 * 
 * <h2>Execution Modes</h2>
 * <ul>
 * <li><strong>Synchronous</strong> - {@link #execute(Long, Map)} blocks until
 * completion</li>
 * <li><strong>Asynchronous</strong> - {@link #executeAsync(Long, Map)} returns
 * immediately</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * // Execute a workflow asynchronously
 * CompletableFuture&lt;ExecutionDTO&gt; future = executionService.executeAsync(
 *         workflowId,
 *         Map.of("inputParam", "value"));
 * 
 * // Handle completion
 * future.thenAccept(result -&gt; {
 *     if (result.status() == ExecutionStatus.SUCCESS) {
 *         System.out.println("Output: " + result.outputData());
 *     }
 * });
 * 
 * // Query execution history
 * List&lt;ExecutionDTO&gt; history = executionService.findByWorkflowId(workflowId);
 * </pre>
 * 
 * <h2>Execution Lifecycle</h2>
 * 
 * <pre>
 * PENDING → RUNNING → SUCCESS
 *              │
 *              └──→ FAILED
 *              │
 *              └──→ CANCELLED
 * </pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ExecutionDTO Data transfer object for executions
 * @see ai.nervemind.common.enums.ExecutionStatus Execution states
 * @see WorkflowServiceInterface For workflow management
 */
public interface ExecutionServiceInterface {

    /**
     * Find all executions.
     * 
     * @return list of all executions
     */
    List<ExecutionDTO> findAll();

    /**
     * Find an execution by ID.
     * 
     * @param id the execution ID
     * @return optional containing the execution if found
     */
    Optional<ExecutionDTO> findById(Long id);

    /**
     * Find executions by workflow ID.
     * 
     * @param workflowId the workflow ID
     * @return list of executions for the workflow
     */
    List<ExecutionDTO> findByWorkflowId(Long workflowId);

    /**
     * Find currently running executions.
     * 
     * @return list of running executions
     */
    List<ExecutionDTO> findRunningExecutions();

    /**
     * Find executions within a time range.
     * 
     * @param start the start of the range
     * @param end   the end of the range
     * @return list of executions within the range
     */
    List<ExecutionDTO> findByTimeRange(Instant start, Instant end);

    /**
     * Execute a workflow asynchronously.
     * 
     * @param workflowId the workflow ID to execute
     * @param input      the input data for the workflow
     * @return a future that completes with the execution result
     */
    CompletableFuture<ExecutionDTO> executeAsync(Long workflowId, Map<String, Object> input);

    /**
     * Execute a workflow synchronously.
     * 
     * @param workflowId the workflow ID to execute
     * @param input      the input data for the workflow
     * @return the execution result
     */
    ExecutionDTO execute(Long workflowId, Map<String, Object> input);

    /**
     * Delete all executions.
     * Clears the entire execution history.
     */
    void deleteAll();

    /**
     * Cancel a running execution.
     * 
     * @param executionId the ID of the execution to cancel
     * @return true if the execution was cancelled, false if not found or not
     *         running
     */
    boolean cancelExecution(Long executionId);

    /**
     * Cancel all running executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @return the number of executions cancelled
     */
    int cancelAllForWorkflow(Long workflowId);
}
