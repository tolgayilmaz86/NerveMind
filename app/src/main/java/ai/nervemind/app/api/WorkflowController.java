package ai.nervemind.app.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nervemind.app.service.WorkflowService;
import ai.nervemind.common.dto.WorkflowDTO;

/**
 * REST API controller for workflow management.
 */
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final ai.nervemind.app.service.FileWatcherService fileWatcherService;

    /**
     * Creates a new workflow controller.
     *
     * @param workflowService    the workflow service for business logic
     * @param fileWatcherService the file watcher service for file monitoring
     */
    public WorkflowController(final WorkflowService workflowService,
            final ai.nervemind.app.service.FileWatcherService fileWatcherService) {
        this.workflowService = workflowService;
        this.fileWatcherService = fileWatcherService;
    }

    /**
     * Retrieves all workflows.
     *
     * @return list of all workflows
     */
    @GetMapping
    public List<WorkflowDTO> findAll() {
        return workflowService.findAll();
    }

    /**
     * Retrieves a workflow by its ID.
     *
     * @param id the workflow ID
     * @return the workflow if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDTO> findById(@PathVariable final Long id) {
        return workflowService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new workflow.
     *
     * @param dto the workflow data to create
     * @return the created workflow
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO create(@RequestBody final WorkflowDTO dto) {
        WorkflowDTO created = workflowService.create(dto);
        fileWatcherService.registerWorkflow(created);
        return created;
    }

    /**
     * Updates an existing workflow.
     *
     * @param id  the workflow ID to update
     * @param dto the updated workflow data
     * @return the updated workflow if successful, 400 if ID mismatch
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDTO> update(@PathVariable final Long id, @RequestBody final WorkflowDTO dto) {
        if (!id.equals(dto.id())) {
            return ResponseEntity.badRequest().build();
        }
        final WorkflowDTO updated = workflowService.update(dto);
        fileWatcherService.registerWorkflow(updated);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a workflow by its ID.
     *
     * @param id the workflow ID to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        workflowService.delete(id);
        fileWatcherService.unregisterWorkflow(id);
    }

    /**
     * Duplicates an existing workflow.
     *
     * @param id the workflow ID to duplicate
     * @return the duplicated workflow DTO
     */
    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDTO duplicate(@PathVariable final Long id) {
        WorkflowDTO duplicated = workflowService.duplicate(id);
        fileWatcherService.registerWorkflow(duplicated);
        return duplicated;
    }

    /**
     * Activates or deactivates a workflow.
     *
     * @param id     the workflow ID to activate/deactivate
     * @param active true to activate, false to deactivate
     */
    @PatchMapping("/{id}/active")
    @ResponseStatus(HttpStatus.OK)
    public void activate(@PathVariable final Long id, @RequestParam final boolean active) {
        workflowService.setActive(id, active);
        workflowService.findById(id).ifPresent(fileWatcherService::registerWorkflow);
    }
}
