package ai.nervemind.plugin.api;

import java.util.List;
import java.util.Optional;

/**
 * Unified interface for NerveMind plugins.
 * 
 * <p>
 * This is the primary interface that plugin developers implement to provide
 * custom nodes to NerveMind.
 * </p>
 * 
 * <h2>Plugin Registration</h2>
 * <p>
 * Plugins are discovered using Java ServiceLoader. Create a file at:
 * </p>
 * 
 * <pre>{@code
 * META - INF / services / ai.nervemind.plugin.api.PluginProvider
 * }</pre>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 * <li>NerveMind starts and discovers plugins via ServiceLoader</li>
 * <li>For each plugin: {@link #init(PluginContext)} is called</li>
 * <li>Plugin is ready for workflow execution</li>
 * <li>On shutdown: {@link #destroy()} is called</li>
 * </ol>
 * 
 * <h2>Example Implementation</h2>
 * 
 * <pre>{@code
 * public class MyPlugin implements PluginProvider {
 *
 *     private PluginContext context;
 *
 *     public String getId() {
 *         return "com.example.myplugin";
 *     }
 *
 *     public String getName() {
 *         return "My Plugin";
 *     }
 *
 *     public String getVersion() {
 *         return "1.0.0";
 *     }
 *
 *     public String getDescription() {
 *         return "Does something useful";
 *     }
 *
 *     public List<PluginHandle> getHandles() {
 *         return List.of(
 *                 new PluginHandle(
 *                         "my-action", // handle ID
 *                         "My Action", // display name
 *                         "Does something", // description
 *                         NodeCategory.ACTION, // category
 *                         null, // trigger type (null for actions)
 *                         this::execute, // executor function
 *                         this::validate, // validation function
 *                         () -> "Help text", // help text supplier
 *                         () -> Map.of("type", "object") // JSON schema
 *                 ));
 *     }
 *
 *     public void init(PluginContext context) {
 *         this.context = context;
 *     }
 *
 *     public void destroy() {
 *         // Cleanup resources
 *     }
 *
 *     private Map execute(Map config, Map inputs, Map context) {
 *         return Map.of("result", "done");
 *     }
 *
 *     private Map validate(Map config) {
 *         return Map.of(); // Empty map = valid
 *     }
 * }
 * }</pre>
 *
 * @author NerveMind Team
 * @version 1.0.0
 * @see PluginHandle
 * @see PluginContext
 * @see PluginDependency
 */
public interface PluginProvider {

    // ===== Core Identity Methods =====

    /**
     * Unique identifier for this plugin.
     * 
     * <p>
     * Use reverse domain notation (e.g., "com.example.myplugin").
     * This ID must be unique across all plugins.
     * </p>
     * 
     * @return the unique plugin identifier
     */
    String getId();

    /**
     * Display name shown in the UI.
     * 
     * @return the human-readable plugin name
     */
    String getName();

    /**
     * Plugin version (semantic versioning recommended).
     * 
     * <p>
     * Format: MAJOR.MINOR.PATCH (e.g., "1.0.0")
     * </p>
     * 
     * @return the version string
     */
    String getVersion();

    /**
     * Brief description of what the plugin does.
     * 
     * @return a short description for the UI
     */
    String getDescription();

    /**
     * Plugin category for UI organization.
     * 
     * <p>
     * Determines which section of the node palette the plugin appears in.
     * Default is {@link NodeCategory#UTILITY}.
     * </p>
     * 
     * @return the node category
     */
    default NodeCategory getCategory() {
        return NodeCategory.UTILITY;
    }

    /**
     * Icon name for the plugin in the UI.
     * 
     * <p>
     * Use Material Design icon names (e.g., "ROCKET", "COG", "DATABASE").
     * </p>
     * 
     * @return the icon name, or null for default icon
     */
    default String getIconName() {
        return null;
    }

    /**
     * Subtitle shown below the plugin name in the UI.
     * 
     * <p>
     * Useful for additional categorization within a plugin category.
     * </p>
     * 
     * @return the subtitle text, or null for no subtitle
     */
    default String getSubtitle() {
        return null;
    }

    /**
     * Help text for the plugin.
     * 
     * <p>
     * Markdown-formatted documentation shown when users request help.
     * Can include headers, tables, code blocks, and lists.
     * </p>
     * 
     * @return the help text in Markdown format, or null for no help
     */
    default String getHelpText() {
        return null;
    }

    // ===== Node Definition =====

    /**
     * Handles provided by this plugin.
     * 
     * <p>
     * Each handle represents a node type in the workflow editor.
     * A plugin can provide multiple handles for different operations.
     * </p>
     * 
     * <p>
     * For example, a database plugin might provide handles for:
     * </p>
     * <ul>
     * <li>"query" - Execute a SQL query</li>
     * <li>"insert" - Insert a record</li>
     * <li>"update" - Update a record</li>
     * </ul>
     * 
     * @return a list of plugin handles (must not be empty)
     * @see PluginHandle
     */
    List<PluginHandle> getHandles();

    // ===== Lifecycle Methods =====

    /**
     * Called when the plugin is first loaded.
     * 
     * <p>
     * Use for initialization tasks such as:
     * </p>
     * <ul>
     * <li>Opening database connections</li>
     * <li>Reading configuration from
     * {@link PluginContext#getPersistentConfig()}</li>
     * <li>Registering event handlers via
     * {@link PluginContext#registerEventHandler(EventHandler)}</li>
     * <li>Getting services from {@link PluginContext#getService(Class)}</li>
     * </ul>
     * 
     * @param context the plugin context providing access to services and
     *                configuration
     * @see PluginContext
     */
    default void init(PluginContext context) {
        // Optional - override to initialize resources
    }

    /**
     * Called when NerveMind is shutting down.
     * 
     * <p>
     * Use for cleanup tasks such as:
     * </p>
     * <ul>
     * <li>Closing database connections</li>
     * <li>Saving state to persistent configuration</li>
     * <li>Releasing external resources</li>
     * </ul>
     */
    default void destroy() {
        // Optional - override to cleanup resources
    }

    // ===== Dependencies =====

    /**
     * Plugin dependencies (optional).
     * 
     * <p>
     * If specified, these plugins must be loaded and initialized before
     * this plugin. Use for plugins that build on top of other plugins.
     * </p>
     * 
     * <p>
     * Example:
     * </p>
     * 
     * <pre>{@code
     * public List<PluginDependency> getDependencies() {
     *     return List.of(
     *             PluginDependency.required("com.example.database", ">=1.0.0"),
     *             PluginDependency.optional("com.example.logging", ">=2.0.0"));
     * }
     * }</pre>
     * 
     * @return a list of plugin dependencies (empty by default)
     * @see PluginDependency
     */
    default List<PluginDependency> getDependencies() {
        return List.of();
    }

    // ===== UI Extensions =====

    /**
     * Get menu contributions from this plugin.
     * 
     * <p>
     * Allows adding items to application menus and context menus.
     * </p>
     * 
     * @return a list of menu contributions (empty by default)
     * @see MenuContribution
     */
    default List<MenuContribution> getMenuContributions() {
        return List.of();
    }

    /**
     * Get side panel contribution from this plugin.
     * 
     * <p>
     * Allows adding a custom panel to the sidebar for plugin-specific UI.
     * </p>
     * 
     * @return an optional side panel contribution (empty by default)
     * @see SidePanelContribution
     */
    default Optional<SidePanelContribution> getSidePanel() {
        return Optional.empty();
    }

    /**
     * Create a custom node view for this plugin.
     * 
     * <p>
     * Override to provide custom JavaFX rendering for nodes on the canvas.
     * Return null to use the default node appearance.
     * </p>
     * 
     * @param context the node view context with node state information
     * @return a JavaFX Node for custom rendering, or null for default
     * @see NodeViewContext
     */
    default Object createNodeView(NodeViewContext context) {
        return null;
    }

    /**
     * Called when a connection is created involving this plugin's node.
     * 
     * <p>
     * Useful for validating connections or updating internal state.
     * </p>
     * 
     * @param context the connection context with connection details
     * @see ConnectionContext
     */
    default void onConnectionCreated(ConnectionContext context) {
        // Optional - override to handle connection events
    }

    // ===== Trigger-Specific Methods =====

    /**
     * Whether this plugin is a trigger (starts workflows).
     * 
     * <p>
     * Triggers are workflow entry points like:
     * </p>
     * <ul>
     * <li>Webhook triggers</li>
     * <li>Schedule triggers</li>
     * <li>File watcher triggers</li>
     * <li>Manual triggers</li>
     * </ul>
     * 
     * @return true if this plugin is a trigger, false for actions
     */
    default boolean isTrigger() {
        return false;
    }

    /**
     * Whether this trigger requires a background service.
     * 
     * <p>
     * Set to true for triggers that need continuous monitoring:
     * </p>
     * <ul>
     * <li>File watchers</li>
     * <li>Message queue listeners</li>
     * <li>HTTP webhook listeners</li>
     * </ul>
     * 
     * <p>
     * Set to false for triggers that are invoked on-demand:
     * </p>
     * <ul>
     * <li>Manual triggers</li>
     * <li>Scheduled triggers (handled by scheduler)</li>
     * </ul>
     * 
     * @return true if background service is needed, false otherwise
     */
    default boolean requiresBackgroundService() {
        return false;
    }

    // ===== Helper Methods =====

    /**
     * Get the first handle's ID (convenience method).
     * 
     * <p>
     * Useful for single-handle plugins where the handle ID serves as the node type.
     * </p>
     * 
     * @return the first handle's ID, or the plugin ID if no handles exist
     */
    default String getNodeType() {
        List<PluginHandle> handles = getHandles();
        if (handles != null && !handles.isEmpty()) {
            return handles.get(0).id();
        }
        return getId();
    }

    /**
     * Get display name (alias for {@link #getName()}).
     * 
     * @return the display name
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * Get properties for this plugin.
     * 
     * <p>
     * Defines configurable properties shown in the node configuration panel.
     * Each property has a type, name, description, and optional constraints.
     * </p>
     * 
     * @return a list of property definitions (empty by default)
     * @see PropertyDefinition
     */
    default List<PropertyDefinition> getProperties() {
        return List.of();
    }
}
