package ai.nervemind.app.database.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.nervemind.app.database.model.ExecutionEntity;
import ai.nervemind.common.enums.ExecutionStatus;

/**
 * Repository for Execution entities.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionEntity, Long> {

    /**
     * Find all executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @return list of execution entities ordered by start time descending
     */
    List<ExecutionEntity> findByWorkflowIdOrderByStartedAtDesc(Long workflowId);

    /**
     * Find paginated executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @param pageable   pagination information
     * @return page of execution entities
     */
    Page<ExecutionEntity> findByWorkflowId(Long workflowId, Pageable pageable);

    /**
     * Find all executions by status.
     * 
     * @param status the execution status
     * @return list of execution entities with the specified status
     */
    List<ExecutionEntity> findByStatus(ExecutionStatus status);

    /**
     * Find running executions.
     * 
     * @return list of execution entities that are currently running or waiting
     */
    @Query("SELECT e FROM ExecutionEntity e WHERE e.status IN ('RUNNING', 'WAITING')")
    List<ExecutionEntity> findRunningExecutions();

    /**
     * Find recent executions.
     * 
     * @param pageable pagination information
     * @return list of recent execution entities ordered by start time descending
     */
    @Query("SELECT e FROM ExecutionEntity e ORDER BY e.startedAt DESC")
    List<ExecutionEntity> findRecentExecutions(Pageable pageable);

    /**
     * Find executions within a time range.
     * 
     * @param start the start time
     * @param end   the end time
     * @return list of execution entities within the time range ordered by start
     *         time descending
     */
    @Query("SELECT e FROM ExecutionEntity e WHERE e.startedAt BETWEEN :start AND :end ORDER BY e.startedAt DESC")
    List<ExecutionEntity> findByTimeRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Count executions by status for a workflow.
     * 
     * @param workflowId the workflow ID
     * @param status     the execution status
     * @return count of executions with the specified status for the workflow
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = :status")
    long countByWorkflowIdAndStatus(@Param("workflowId") Long workflowId, @Param("status") ExecutionStatus status);

    /**
     * Count successful executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @return count of successful executions for the workflow
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = 'SUCCESS'")
    long countSuccessfulByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Count failed executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @return count of failed executions for the workflow
     */
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.workflowId = :workflowId AND e.status = 'FAILED'")
    long countFailedByWorkflowId(@Param("workflowId") Long workflowId);

    /**
     * Delete old executions (cleanup).
     * 
     * @param cutoff the cutoff timestamp
     */
    @Query("DELETE FROM ExecutionEntity e WHERE e.finishedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") Instant cutoff);
}
