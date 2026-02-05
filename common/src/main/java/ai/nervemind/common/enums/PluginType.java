package ai.nervemind.common.enums;

/**
 * Types of plugins supported by the application.
 * 
 * <p>
 * This enum replaces string-based plugin type identification
 * (e.g., "Trigger" or "Action") with type-safe constants.
 * </p>
 */
public enum PluginType {
    /** Plugin that triggers workflow execution */
    TRIGGER("Trigger", "Workflow trigger plugin"),

    /** Plugin that performs an action within a workflow */
    ACTION("Action", "Workflow action plugin");

    private final String displayName;
    private final String description;

    PluginType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the display name for this plugin type.
     * 
     * @return the user-facing name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description for this plugin type.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Parse a plugin type from its string representation.
     * Supports backward compatibility with existing string values.
     * 
     * @param value the string value ("Trigger" or "Action")
     * @return the corresponding PluginType, or ACTION as default
     */
    public static PluginType fromString(String value) {
        if (value == null) {
            return ACTION;
        }
        return switch (value.toLowerCase()) {
            case "trigger" -> TRIGGER;
            case "action" -> ACTION;
            default -> ACTION;
        };
    }
}
