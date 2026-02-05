package ai.nervemind.common.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;

/**
 * Service interface for managing workflow variables.
 * 
 * <p>
 * Variables provide a way to store and share data across workflows and
 * workflow executions. They can be global (shared across all workflows) or
 * scoped to a specific workflow.
 * </p>
 * 
 * <h2>Variable Scopes</h2>
 * <ul>
 * <li><strong>GLOBAL</strong> - Available to all workflows</li>
 * <li><strong>WORKFLOW</strong> - Specific to a single workflow</li>
 * <li><strong>EXECUTION</strong> - Temporary, only during a single
 * execution</li>
 * </ul>
 * 
 * <h2>Variable Resolution</h2>
 * <p>
 * When resolving variables, workflow-scoped variables take precedence over
 * global variables with the same name. Variables are referenced using the
 * <code>${variableName}</code> syntax in node parameters.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * // Create a global variable
 * VariableDTO apiUrl = VariableDTO.globalVariable(
 *         "API_BASE_URL",
 *         "https://api.example.com",
 *         VariableType.STRING,
 *         "Base URL for external API");
 * variableService.create(apiUrl);
 * 
 * // Resolve variables in a string
 * String resolved = variableService.resolveVariables(
 *         "Calling ${API_BASE_URL}/users",
 *         workflowId);
 * // Result: "Calling https://api.example.com/users"
 * </pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see VariableDTO Data transfer object for variables
 * @see VariableScope Available variable scopes
 */
public interface VariableServiceInterface {

    /**
     * Find all variables.
     * 
     * @return list of all variables
     */
    List<VariableDTO> findAll();

    /**
     * Find all global variables.
     * 
     * @return list of global variables
     */
    List<VariableDTO> findGlobalVariables();

    /**
     * Find all variables for a specific workflow.
     * 
     * @param workflowId the workflow ID
     * @return list of variables for the workflow
     */
    List<VariableDTO> findByWorkflowId(Long workflowId);

    /**
     * Find a variable by ID.
     * 
     * @param id the variable ID
     * @return optional containing the variable if found
     */
    Optional<VariableDTO> findById(Long id);

    /**
     * Find a variable by name and scope.
     * 
     * @param name       the variable name
     * @param scope      the variable scope
     * @param workflowId the workflow ID (if scope is WORKFLOW)
     * @return optional containing the variable if found
     */
    Optional<VariableDTO> findByNameAndScope(String name, VariableScope scope, Long workflowId);

    /**
     * Create a new variable.
     * 
     * @param variable the variable to create
     * @return the created variable
     */
    VariableDTO create(VariableDTO variable);

    /**
     * Update an existing variable.
     * 
     * @param id       the variable ID to update
     * @param variable the updated variable data
     * @return the updated variable
     */
    VariableDTO update(Long id, VariableDTO variable);

    /**
     * Delete a variable.
     * 
     * @param id the variable ID to delete
     */
    void delete(Long id);

    /**
     * Get the resolved value of a variable.
     * For SECRET types, returns the decrypted value.
     * 
     * @param id the variable ID
     * @return the resolved/decrypted value
     */
    String getResolvedValue(Long id);

    /**
     * Get all variables as a map (name -> value) for a given workflow.
     * Includes both global and workflow-specific variables.
     * Workflow variables override global variables with the same name.
     * 
     * @param workflowId the workflow ID
     * @return map of variable names to values
     */
    Map<String, Object> getVariablesForWorkflow(Long workflowId);

    /**
     * Resolve variable references in a string.
     * Replaces ${variableName} with actual values.
     * 
     * @param input      the input string containing variable references
     * @param workflowId the workflow ID for scoping
     * @return the string with resolved variables
     */
    String resolveVariables(String input, Long workflowId);

    /**
     * Validate variable name format.
     * 
     * @param name the variable name to validate
     * @return true if the format is valid
     */
    boolean isValidVariableName(String name);
}
