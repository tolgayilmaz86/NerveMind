package ai.nervemind.plugin.api;

/**
 * Types of events that can be subscribed to by plugins.
 * 
 * <p>
 * Event types are organized into categories:
 * </p>
 * <ul>
 * <li>Workflow events - lifecycle of workflows</li>
 * <li>Execution events - execution of nodes and workflows</li>
 * <li>Application events - application lifecycle</li>
 * <li>Plugin events - plugin lifecycle</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see Event
 * @see EventHandler
 */
public enum EventType {

    // === Workflow Events ===

    /**
     * A workflow has been activated.
     * Source: workflow ID
     */
    WORKFLOW_ACTIVATED("Workflow Activated", "A workflow has been activated"),

    /**
     * A workflow has been deactivated.
     * Source: workflow ID
     */
    WORKFLOW_DEACTIVATED("Workflow Deactivated", "A workflow has been deactivated"),

    /**
     * A workflow execution has started.
     * Source: execution ID
     */
    WORKFLOW_STARTED("Workflow Started", "A workflow execution has started"),

    /**
     * A workflow execution has completed successfully.
     * Source: execution ID
     */
    WORKFLOW_COMPLETED("Workflow Completed", "A workflow execution has completed"),

    /**
     * A workflow execution has failed.
     * Source: execution ID
     */
    WORKFLOW_FAILED("Workflow Failed", "A workflow execution has failed"),

    // === Execution Events ===

    /**
     * A node execution has started.
     * Source: node ID
     */
    NODE_EXECUTION_STARTED("Node Execution Started", "A node has started executing"),

    /**
     * A node execution has completed.
     * Source: node ID
     */
    NODE_EXECUTION_COMPLETED("Node Execution Completed", "A node has completed executing"),

    /**
     * A node execution has failed.
     * Source: node ID
     */
    NODE_EXECUTION_FAILED("Node Execution Failed", "A node execution has failed"),

    /**
     * An execution error occurred.
     * Source: execution ID
     */
    EXECUTION_ERROR("Execution Error", "An error occurred during execution"),

    // === Application Events ===

    /**
     * The application has started.
     * Source: "application"
     */
    APPLICATION_STARTED("Application Started", "The application has started"),

    /**
     * The application is about to stop.
     * Source: "application"
     */
    APPLICATION_STOPPING("Application Stopping", "The application is stopping"),

    /**
     * Application settings have changed.
     * Source: "settings"
     */
    SETTINGS_CHANGED("Settings Changed", "Application settings have changed"),

    /**
     * Credentials have been updated.
     * Source: credential ID
     */
    CREDENTIALS_UPDATED("Credentials Updated", "Credentials have been updated"),

    // === Plugin Events ===

    /**
     * A plugin has been loaded.
     * Source: plugin ID
     */
    PLUGIN_LOADED("Plugin Loaded", "A plugin has been loaded"),

    /**
     * A plugin has been unloaded.
     * Source: plugin ID
     */
    PLUGIN_UNLOADED("Plugin Unloaded", "A plugin has been unloaded"),

    /**
     * A plugin has encountered an error.
     * Source: plugin ID
     */
    PLUGIN_ERROR("Plugin Error", "A plugin has encountered an error"),

    /**
     * A plugin has been updated.
     * Source: plugin ID
     */
    PLUGIN_UPDATED("Plugin Updated", "A plugin has been updated");

    private final String displayName;
    private final String description;

    EventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the user-friendly display name.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this event type.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this event type is a workflow event.
     * 
     * @return true if this is a workflow event
     */
    public boolean isWorkflowEvent() {
        return this.name().startsWith("WORKFLOW");
    }

    /**
     * Checks if this event type is an execution event.
     * 
     * @return true if this is an execution event
     */
    public boolean isExecutionEvent() {
        return this.name().startsWith("EXECUTION") ||
                this.name().startsWith("NODE_");
    }

    /**
     * Checks if this event type is an application event.
     * 
     * @return true if this is an application event
     */
    public boolean isApplicationEvent() {
        return this.name().startsWith("APPLICATION") ||
                this.name().startsWith("SETTINGS") ||
                this.name().startsWith("CREDENTIALS");
    }

    /**
     * Checks if this event type is a plugin event.
     * 
     * @return true if this is a plugin event
     */
    public boolean isPluginEvent() {
        return this.name().startsWith("PLUGIN");
    }
}
