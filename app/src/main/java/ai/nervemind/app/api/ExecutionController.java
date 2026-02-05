package ai.nervemind.app.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.dto.ExecutionDTO;

/**
 * REST API controller for workflow executions.
 */
@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Constructs a new ExecutionController.
     * 
     * @param executionService the execution service to use
     */
    public ExecutionController(final ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * Retrieves all workflow executions.
     * 
     * @return list of all executions
     */
    @GetMapping
    public List<ExecutionDTO> findAll() {
        return executionService.findAll();
    }

    /**
     * Retrieves a specific workflow execution by ID.
     * 
     * @param id the execution ID
     * @return the execution if found, or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionDTO> findById(@PathVariable final Long id) {
        return executionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all executions for a specific workflow.
     * 
     * @param workflowId the workflow ID
     * @return list of executions for the workflow
     */
    @GetMapping("/workflow/{workflowId}")
    public List<ExecutionDTO> findByWorkflowId(@PathVariable final Long workflowId) {
        return executionService.findByWorkflowId(workflowId);
    }

    /**
     * Retrieves all currently running workflow executions.
     * 
     * @return list of currently running executions
     */
    @GetMapping("/running")
    public List<ExecutionDTO> findRunning() {
        return executionService.findRunningExecutions();
    }

    /**
     * Triggers an asynchronous workflow execution.
     * 
     * @param workflowId the workflow to execute
     * @param input      optional input data
     * @return future with the execution result
     */
    @PostMapping("/run/{workflowId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<ExecutionDTO> executeWorkflow(
            @PathVariable final Long workflowId,
            @RequestBody(required = false) final Map<String, Object> input) {
        return executionService.executeAsync(workflowId, input != null ? input : Map.of());
    }

    /**
     * Triggers a synchronous workflow execution.
     * 
     * @param workflowId the workflow to execute
     * @param input      optional input data
     * @return the execution result
     */
    @PostMapping("/run/{workflowId}/sync")
    public ExecutionDTO executeWorkflowSync(
            @PathVariable final Long workflowId,
            @RequestBody(required = false) final Map<String, Object> input) {
        return executionService.execute(workflowId, input != null ? input : Map.of());
    }

    /**
     * Cancels a running workflow execution.
     * 
     * @param id the execution ID to cancel
     * @return 200 if cancelled, or 404
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable final Long id) {
        boolean cancelled = executionService.cancelExecution(id);
        if (cancelled) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
