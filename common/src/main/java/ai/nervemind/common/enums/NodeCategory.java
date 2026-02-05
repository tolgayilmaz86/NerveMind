package ai.nervemind.common.enums;

/**
 * Categories for organizing node types in the workflow palette.
 * 
 * <p>
 * This is the canonical definition of node categories used across all modules:
 * common, ui, app, and plugin-api. Each category groups related node types
 * together in the UI, making it easier for users to find and select appropriate
 * nodes.
 * </p>
 * 
 * <p>
 * Plugins should use this enum for their node category definitions.
 * </p>
 */
public enum NodeCategory {
    /**
     * Nodes that initiate workflow execution (Manual, Schedule, Webhook, File,
     * etc.)
     */
    TRIGGER("Triggers", "Nodes that start workflow execution"),

    /** Nodes that perform actions (HTTP Request, Code, Execute Command, etc.) */
    ACTION("Actions", "Nodes that perform operations"),

    /**
     * Nodes that control workflow execution flow (If, Switch, Loop, Merge, etc.)
     */
    FLOW("Flow Control", "Nodes that control execution flow"),

    /** Nodes that manipulate data (Set, Filter, Sort, Transform, etc.) */
    DATA("Data", "Nodes that transform and manipulate data"),

    /**
     * AI and machine learning nodes (LLM Chat, Text Classifier, Embedding, RAG,
     * etc.)
     */
    AI("AI & ML", "Artificial intelligence and machine learning nodes"),

    /** Advanced workflow features (Subworkflow, Parallel, Try/Catch, etc.) */
    ADVANCED("Advanced", "Advanced workflow features"),

    /** Integrations with external services */
    INTEGRATION("Integration", "Integration with external services"),

    /** Helper and utility nodes */
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

    /**
     * Parse a category from its name string.
     * Supports both enum names and display names.
     * 
     * @param value the category name (case-insensitive)
     * @return the corresponding NodeCategory, or UTILITY as default
     */
    public static NodeCategory fromString(String value) {
        if (value == null) {
            return UTILITY;
        }
        // Try exact enum name match first
        try {
            return NodeCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException _) {
            // Try display name match
            for (NodeCategory category : values()) {
                if (category.displayName.equalsIgnoreCase(value)) {
                    return category;
                }
            }
            // Handle legacy/alternative names
            return switch (value.toLowerCase()) {
                case "flow_control", "flow-control", "flowcontrol" -> FLOW;
                case "ai_ml", "ai-ml", "aiml" -> AI;
                default -> UTILITY;
            };
        }
    }
}
