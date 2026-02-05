package ai.nervemind.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.nervemind.app.database.model.WorkflowEntity;
import ai.nervemind.common.enums.TriggerType;

/**
 * Repository for Workflow entities.
 */
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {

    /**
     * Find workflow by exact name.
     *
     * @param name the exact name to search for
     * @return an Optional containing the workflow if found
     */
    Optional<WorkflowEntity> findByName(String name);

    /**
     * Find all workflows by name (case-insensitive).
     *
     * @param name the name to search for (partial match, case-insensitive)
     * @return a list of workflows matching the name
     */
    List<WorkflowEntity> findByNameContainingIgnoreCase(String name);

    /**
     * Find all active workflows.
     *
     * @return a list of all active workflows
     */
    List<WorkflowEntity> findByIsActiveTrue();

    /**
     * Find all workflows by trigger type.
     *
     * @param triggerType the trigger type to filter by
     * @return a list of workflows with the specified trigger type
     */
    List<WorkflowEntity> findByTriggerType(TriggerType triggerType);

    /**
     * Find all active scheduled workflows.
     *
     * @return a list of active workflows with SCHEDULE trigger type
     */
    @Query("SELECT w FROM WorkflowEntity w WHERE w.isActive = true AND w.triggerType = 'SCHEDULE'")
    List<WorkflowEntity> findActiveScheduledWorkflows();

    /**
     * Find all active webhook workflows.
     *
     * @return a list of active workflows with WEBHOOK trigger type
     */
    @Query("SELECT w FROM WorkflowEntity w WHERE w.isActive = true AND w.triggerType = 'WEBHOOK'")
    List<WorkflowEntity> findActiveWebhookWorkflows();

    /**
     * Count total workflows.
     *
     * @return the total number of workflows
     */
    @Query("SELECT COUNT(w) FROM WorkflowEntity w")
    long countWorkflows();

    /**
     * Count active workflows.
     *
     * @return the number of active workflows
     */
    @Query("SELECT COUNT(w) FROM WorkflowEntity w WHERE w.isActive = true")
    long countActiveWorkflows();
}
