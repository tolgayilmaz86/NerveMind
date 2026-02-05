package ai.nervemind.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.nervemind.app.entity.VariableEntity;
import ai.nervemind.common.dto.VariableDTO.VariableScope;

/**
 * Repository for Variable entities.
 */
@Repository
public interface VariableRepository extends JpaRepository<VariableEntity, Long> {

    /**
     * Find all global variables.
     *
     * @param scope the variable scope to search for
     * @return list of variables with the specified scope
     */
    List<VariableEntity> findByScope(VariableScope scope);

    /**
     * Find all variables for a specific workflow.
     *
     * @param workflowId the workflow ID
     * @return list of variables belonging to the workflow
     */
    List<VariableEntity> findByWorkflowId(Long workflowId);

    /**
     * Find by name and scope (for global variables).
     *
     * @param name  the variable name
     * @param scope the variable scope
     * @return optional containing the variable if found
     */
    Optional<VariableEntity> findByNameAndScopeAndWorkflowIdIsNull(String name, VariableScope scope);

    /**
     * Find by name, scope, and workflow ID.
     *
     * @param name       the variable name
     * @param scope      the variable scope
     * @param workflowId the workflow ID
     * @return optional containing the variable if found
     */
    Optional<VariableEntity> findByNameAndScopeAndWorkflowId(String name, VariableScope scope, Long workflowId);

    /**
     * Find all variables for a workflow (both global and workflow-specific).
     *
     * @param scope      the variable scope
     * @param workflowId the workflow ID
     * @return list of variables matching the criteria
     */
    List<VariableEntity> findByScopeOrWorkflowId(VariableScope scope, Long workflowId);

    /**
     * Check if a variable name exists in a scope.
     *
     * @param name       the variable name to check
     * @param scope      the variable scope
     * @param workflowId the workflow ID (null for global)
     * @return true if a variable with the given name exists, false otherwise
     */
    boolean existsByNameAndScopeAndWorkflowId(String name, VariableScope scope, Long workflowId);
}
