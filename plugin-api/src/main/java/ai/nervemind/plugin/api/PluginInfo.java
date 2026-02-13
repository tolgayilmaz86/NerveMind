package ai.nervemind.plugin.api;

import java.util.List;
import java.util.Properties;

/**
 * Metadata and runtime information about a plugin.
 * 
 * <p>
 * This interface provides access to plugin metadata and configuration
 * that can be used at runtime.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
public interface PluginInfo {

    /**
     * Gets the unique node type identifier for this plugin.
     * 
     * @return the node type (e.g., "com.example.myplugin")
     */
    String getNodeType();

    /**
     * Gets the display name shown in the UI.
     * 
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the plugin version.
     * 
     * @return the version string (e.g., "1.0.0")
     */
    String getVersion();

    /**
     * Gets the category of this node.
     * 
     * @return the node category
     */
    NodeCategory getCategory();

    /**
     * Checks if this plugin is a trigger.
     * 
     * @return true if this is a trigger node
     */
    boolean isTrigger();

    /**
     * Gets the icon name for this plugin.
     * 
     * @return the Material Design icon name
     */
    String getIconName();

    /**
     * Gets the list of property definitions for this plugin.
     * 
     * @return property definitions
     */
    List<PropertyDefinition> getProperties();

    /**
     * Gets the plugin instance.
     * 
     * @return the plugin provider instance
     */
    PluginProvider getPluginProvider();

    /**
     * Gets the persistent configuration for this plugin.
     * 
     * @return configuration properties
     */
    Properties getConfiguration();

    /**
     * Checks if this plugin is currently enabled.
     * 
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the number of times this plugin has been executed.
     * 
     * @return execution count
     */
    long getExecutionCount();

    /**
     * Gets the last execution timestamp.
     * 
     * @return last execution time, or null if never executed
     */
    Long getLastExecutionTime();

    /**
     * Gets any error message from the last execution.
     * 
     * @return error message, or null if no error
     */
    String getLastError();

    /**
     * Gets the health status of this plugin.
     * 
     * @return health status
     */
    PluginHealthStatus getHealthStatus();
}
