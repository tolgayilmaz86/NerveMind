package ai.nervemind.common.dto;

import java.time.Instant;

/**
 * Data Transfer Object for workflow variables.
 * 
 * <p>
 * Variables store reusable values that can be referenced in workflow nodes
 * using the <code>${variableName}</code> syntax. They can be scoped globally
 * or to specific workflows.
 * </p>
 * 
 * <h2>Variable Scopes</h2>
 * <ul>
 * <li><strong>GLOBAL</strong> - Shared across all workflows, defined at
 * application level</li>
 * <li><strong>WORKFLOW</strong> - Specific to a single workflow, overrides
 * global vars</li>
 * <li><strong>EXECUTION</strong> - Temporary, only available during a single
 * execution</li>
 * </ul>
 * 
 * <h2>Variable Types</h2>
 * <ul>
 * <li><strong>STRING</strong> - Plain text value</li>
 * <li><strong>NUMBER</strong> - Integer or decimal value</li>
 * <li><strong>BOOLEAN</strong> - True/false value</li>
 * <li><strong>JSON</strong> - Complex object or array</li>
 * <li><strong>SECRET</strong> - Encrypted sensitive value</li>
 * </ul>
 * 
 * @param id          Unique identifier
 * @param name        Variable name (used in ${} references)
 * @param value       Variable value (encrypted if SECRET type)
 * @param type        Value type for validation and display
 * @param scope       Variable scope (GLOBAL, WORKFLOW, EXECUTION)
 * @param workflowId  Workflow ID (null for global variables)
 * @param description Help text describing the variable
 * @param createdAt   Creation timestamp
 * @param updatedAt   Last modification timestamp
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see VariableType Supported variable value types
 * @see VariableScope Available variable scopes
 */
public record VariableDTO(
        Long id,
        String name,
        String value,
        VariableType type,
        VariableScope scope,
        Long workflowId, // null for global variables
        String description,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * Create a new global variable.
     * 
     * @param name        the variable name
     * @param value       the variable value
     * @param type        the variable type
     * @param description the description
     * @return a new global variable DOT
     */
    public static VariableDTO globalVariable(String name, String value, VariableType type, String description) {
        return new VariableDTO(null, name, value, type, VariableScope.GLOBAL, null, description, null, null);
    }

    /**
     * Create a new workflow variable.
     * 
     * @param name        the variable name
     * @param value       the variable value
     * @param type        the variable type
     * @param workflowId  the workflow ID
     * @param description the description
     * @return a new workflow variable DTO
     */
    public static VariableDTO workflowVariable(String name, String value, VariableType type, Long workflowId,
            String description) {
        return new VariableDTO(null, name, value, type, VariableScope.WORKFLOW, workflowId, description, null, null);
    }

    /**
     * Variable value types.
     */
    public enum VariableType {
        /** Plain text value. */
        STRING("String", "Text value"),
        /** Numeric value. */
        NUMBER("Number", "Numeric value (integer or decimal)"),
        /** True/false value. */
        BOOLEAN("Boolean", "True/False value"),
        /** Complex JSON object or array. */
        JSON("JSON", "JSON object or array"),
        /** Encrypted sensitive value. */
        SECRET("Secret", "Encrypted sensitive value");

        private final String displayName;
        private final String description;

        VariableType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Gets the display name.
         * 
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the description.
         * 
         * @return the description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Variable scope.
     */
    public enum VariableScope {
        /** Application-wide variable. */
        GLOBAL("Global", "Available across all workflows"),
        /** Specific to a single workflow. */
        WORKFLOW("Workflow", "Available only within a specific workflow"),
        /** Temporary variable for a single execution. */
        EXECUTION("Execution", "Temporary, only during execution");

        private final String displayName;
        private final String description;

        VariableScope(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        /**
         * Gets the display name.
         * 
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Gets the description.
         * 
         * @return the description
         */
        public String getDescription() {
            return description;
        }
    }
}
