package ai.nervemind.plugin.api;

/**
 * Categories for organizing nodes in the palette.
 * 
 * <p>
 * This enum defines the standard node categories that plugins can use
 * to classify their provided nodes. The NerveMind application will use
 * these categories to organize nodes in the workflow palette UI.
 * </p>
 * 
 * <p>
 * The core application has a corresponding
 * {@code ai.nervemind.common.enums.NodeCategory}
 * enum with additional values. When plugins use this enum, the application
 * will map the values appropriately.
 * </p>
 * 
 * <p>
 * <strong>Usage in Plugin Development:</strong>
 * </p>
 * 
 * <pre>{@code
 * public class MyTriggerProvider implements TriggerProvider {
 *     @Override
 *     public NodeCategory getCategory() {
 *         return NodeCategory.TRIGGER;
 *     }
 *     // ... other methods
 * }
 * }</pre>
 */
public enum NodeCategory {
    /** Workflow entry points (Manual, Schedule, Webhook, File) */
    TRIGGER("Triggers", "Nodes that start workflow execution"),

    /** HTTP, Database, API calls */
    ACTION("Actions", "Nodes that perform operations"),

    /** If/Switch/Loop/Merge */
    FLOW_CONTROL("Flow Control", "Nodes that control execution flow"),

    /** Set, Get, Transform data */
    DATA("Data", "Nodes that transform and manipulate data"),

    /** AI/ML operations */
    AI("AI & ML", "Artificial intelligence and machine learning nodes"),

    /** Integrations with external services */
    INTEGRATION("Integration", "Integration with external services"),

    /** Custom/Other */
    UTILITY("Utility", "Helper and utility nodes");

    private final String displayName;
    private final String description;

    NodeCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the display name for this category.
     * 
     * @return the user-facing name for this category
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this category.
     * 
     * @return human-readable description
     */
    public String getDescription() {
        return description;
    }
}
