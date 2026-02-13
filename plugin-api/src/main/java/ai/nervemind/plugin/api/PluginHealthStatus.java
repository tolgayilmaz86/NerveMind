package ai.nervemind.plugin.api;

/**
 * Represents the health status of a plugin.
 * 
 * <p>
 * Used to indicate whether a plugin is functioning correctly,
 * has warnings, or is in an error state.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
public enum PluginHealthStatus {

    /**
     * Plugin is healthy and functioning correctly.
     */
    HEALTHY("Healthy", "The plugin is working correctly"),

    /**
     * Plugin is functional but has warnings.
     * For example: deprecated API usage, configuration warnings.
     */
    WARNING("Warning", "The plugin is functional but has warnings"),

    /**
     * Plugin is not working correctly.
     * For example: missing dependencies, configuration errors.
     */
    UNHEALTHY("Unhealthy", "The plugin has errors and may not work correctly"),

    /**
     * Plugin is disabled and not running.
     */
    DISABLED("Disabled", "The plugin is disabled"),

    /**
     * Plugin status is unknown.
     * For example: not yet initialized.
     */
    UNKNOWN("Unknown", "Plugin status is unknown");

    private final String displayName;
    private final String description;

    PluginHealthStatus(String displayName, String description) {
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
     * Gets the description of this health status.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status indicates a functional plugin.
     * 
     * @return true if the plugin is functional (HEALTHY or WARNING)
     */
    public boolean isFunctional() {
        return this == HEALTHY || this == WARNING;
    }

    /**
     * Checks if this status indicates a problem.
     * 
     * @return true if the plugin has issues (WARNING or UNHEALTHY)
     */
    public boolean hasIssues() {
        return this == WARNING || this == UNHEALTHY;
    }
}
