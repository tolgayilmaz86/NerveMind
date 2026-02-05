package ai.nervemind.common.service;

import java.util.List;
import java.util.Optional;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;

/**
 * Service interface for workflow CRUD and lifecycle operations.
 * 
 * <p>
 * This interface defines the contract for managing workflows in NerveMind.
 * It is defined in the {@code common} module to allow the {@code ui} module
 * to depend on it without creating circular dependencies with the {@code app}
 * module.
 * </p>
 * 
 * <h2>Architecture</h2>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                         Workflow Management                                 │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  common module                     app module                               │
 * │  ┌─────────────────────────┐      ┌─────────────────────────┐              │
 * │  │ WorkflowServiceInterface│◀─────│    WorkflowService      │              │
 * │  │       (Contract)        │      │   (Implementation)      │              │
 * │  └─────────────────────────┘      └─────────────────────────┘              │
 * │           ▲                                   │                             │
 * │           │ uses                              │ persists to                 │
 * │  ┌─────────────────────────┐      ┌─────────────────────────┐              │
 * │  │      UI Components      │      │   WorkflowRepository    │              │
 * │  │ (Canvas, Dialogs, etc.) │      │     (Spring Data)       │              │
 * │  └─────────────────────────┘      └─────────────────────────┘              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Key Operations</h2>
 * <ul>
 * <li><strong>CRUD</strong> - Create, read, update, delete workflows</li>
 * <li><strong>Query</strong> - Find workflows by ID, trigger type, etc.</li>
 * <li><strong>Lifecycle</strong> - Activate/deactivate, duplicate
 * workflows</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // In a UI component (injected via Spring)
 * @Autowired
 * private WorkflowServiceInterface workflowService;
 * 
 * // Create a new workflow
 * WorkflowDTO newWorkflow = new WorkflowDTO();
 * newWorkflow.setName("My Workflow");
 * WorkflowDTO saved = workflowService.create(newWorkflow);
 * 
 * // Find all workflows with manual triggers
 * List<WorkflowDTO> manualWorkflows = workflowService.findByTriggerType(TriggerType.MANUAL);
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see WorkflowDTO Data transfer object for workflows
 * @see ai.nervemind.common.domain.Workflow Domain model for workflows
 * @see ExecutionServiceInterface For executing workflows
 */
public interface WorkflowServiceInterface {

    /**
     * Retrieves all workflows in the system.
     * 
     * @return a list of all workflows; never {@code null}, may be empty
     */
    List<WorkflowDTO> findAll();

    /**
     * Finds a workflow by its unique identifier.
     * 
     * @param id the workflow ID
     * @return an Optional containing the workflow if found, or empty if not
     */
    Optional<WorkflowDTO> findById(Long id);

    /**
     * Finds all workflows that use a specific trigger type.
     * 
     * <p>
     * This is useful for filtering workflows in the UI (e.g., showing
     * only scheduled workflows or only manual workflows).
     * </p>
     * 
     * @param triggerType the type of trigger to filter by
     * @return a list of matching workflows; never {@code null}, may be empty
     * @see TriggerType
     */
    List<WorkflowDTO> findByTriggerType(TriggerType triggerType);

    /**
     * Creates a new workflow.
     * 
     * <p>
     * The workflow ID will be auto-generated. The created workflow
     * will initially be inactive.
     * </p>
     * 
     * @param dto the workflow data to create
     * @return the created workflow with generated ID
     * @throws IllegalArgumentException if dto is null or invalid
     */
    WorkflowDTO create(WorkflowDTO dto);

    /**
     * Updates an existing workflow.
     * 
     * <p>
     * The workflow must already exist. All fields in the DTO will
     * be persisted, replacing the existing values.
     * </p>
     * 
     * @param dto the workflow data with updates
     * @return the updated workflow
     * @throws IllegalArgumentException                         if dto is null or
     *                                                          has
     *                                                          no
     *                                                          ID
     * @throws ai.nervemind.common.exception.NerveMindException if workflow not
     *                                                          found
     */
    WorkflowDTO update(WorkflowDTO dto);

    /**
     * Deletes a workflow by ID.
     * 
     * <p>
     * <strong>Warning:</strong> This operation also deletes all
     * associated execution history for the workflow.
     * </p>
     * 
     * @param id the workflow ID to delete
     * @throws ai.nervemind.common.exception.NerveMindException if workflow not
     *                                                          found
     */
    void delete(Long id);

    /**
     * Creates a duplicate of an existing workflow.
     * 
     * <p>
     * The duplicated workflow will have a new ID and its name will
     * be suffixed with " (Copy)". The duplicate is initially inactive.
     * </p>
     * 
     * @param id the ID of the workflow to duplicate
     * @return the newly created duplicate workflow
     * @throws ai.nervemind.common.exception.NerveMindException if source workflow
     *                                                          not
     *                                                          found
     */
    WorkflowDTO duplicate(Long id);

    /**
     * Sets the active state of a workflow.
     * 
     * <p>
     * When a workflow is active:
     * </p>
     * <ul>
     * <li>Scheduled triggers will run according to their cron expressions</li>
     * <li>Webhook triggers will respond to incoming requests</li>
     * <li>File watchers will monitor configured directories</li>
     * </ul>
     * 
     * <p>
     * Inactive workflows can still be executed manually.
     * </p>
     * 
     * @param id     the workflow ID
     * @param active {@code true} to activate, {@code false} to deactivate
     * @throws ai.nervemind.common.exception.NerveMindException if workflow not
     *                                                          found
     */
    void setActive(Long id, boolean active);
}
