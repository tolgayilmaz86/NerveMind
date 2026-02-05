package ai.nervemind.common.service;

import java.util.List;
import java.util.function.Supplier;

import ai.nervemind.common.enums.PluginType;

/**
 * Service interface for managing plugins in NerveMind.
 * 
 * <p>
 * This interface defines the contract for plugin management operations
 * including
 * discovery, enabling/disabling, and querying plugin states. It is designed to
 * be
 * implemented in the {@code app} module while being accessible from the
 * {@code ui}
 * module to avoid circular dependencies.
 * </p>
 * 
 * <h2>Architecture Overview</h2>
 * 
 * <pre>
 * ┌────────────────────────────────────────────────────────────┐
 * │                     common module                          │
 * │  ┌─────────────────────────────────────────────────────┐   │
 * │  │          PluginServiceInterface                     │   │
 * │  │  (Contract for plugin management)                   │   │
 * │  └─────────────────────────────────────────────────────┘   │
 * └────────────────────────────────────────────────────────────┘
 *                              ▲
 *                              │ implements
 * ┌────────────────────────────────────────────────────────────┐
 * │                      app module                            │
 * │  ┌─────────────────────────────────────────────────────┐   │
 * │  │              PluginService                          │   │
 * │  │  (ServiceLoader-based implementation)               │   │
 * │  └─────────────────────────────────────────────────────┘   │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // In a Spring-managed component:
 * @Autowired
 * private PluginServiceInterface pluginService;
 * 
 * // Get all plugins for display
 * List<PluginInfo> plugins = pluginService.getAllDiscoveredPlugins();
 * 
 * // Enable a plugin by its ID
 * pluginService.enablePlugin("ai.nervemind.plugin.filewatcher");
 * 
 * // Check if a plugin is enabled before using it
 * if (pluginService.isPluginEnabled("ai.nervemind.plugin.filewatcher")) {
 *     // Plugin is available for workflow nodes
 * }
 * }</pre>
 * 
 * <h2>Plugin Lifecycle</h2>
 * <ol>
 * <li><strong>Discovery</strong> - Plugins are discovered via Java
 * ServiceLoader at startup</li>
 * <li><strong>Registration</strong> - Discovered plugins are registered
 * internally</li>
 * <li><strong>Enablement</strong> - Users enable/disable plugins via the Plugin
 * Manager UI</li>
 * <li><strong>Persistence</strong> - Enabled states are persisted via
 * {@link SettingsServiceInterface}</li>
 * </ol>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see "ai.nervemind.app.service.PluginService Implementation in app module"
 * @see "ai.nervemind.plugin.api.TriggerProvider Trigger plugin interface"
 * @see "ai.nervemind.plugin.api.ActionProvider Action plugin interface"
 * @see "ai.nervemind.ui.view.dialog.PluginManagerController Plugin Manager UI"
 */
public interface PluginServiceInterface {

    /**
     * Retrieves all discovered plugins, both enabled and disabled.
     * 
     * <p>
     * This method returns metadata about all plugins that were discovered
     * during application startup, regardless of their enabled state. Use this
     * for displaying the complete plugin list in the Plugin Manager UI.
     * </p>
     * 
     * @return an unmodifiable list of {@link PluginInfo} records representing
     *         all discovered plugins; never {@code null}, may be empty if no
     *         plugins were discovered
     * @see #isPluginEnabled(String) To check individual plugin states
     */
    List<PluginInfo> getAllDiscoveredPlugins();

    /**
     * Checks whether a specific plugin is currently enabled.
     * 
     * <p>
     * Enabled plugins are available for use in workflow nodes. Disabled
     * plugins are not shown in the node palette or context menus.
     * </p>
     * 
     * @param pluginId the unique identifier of the plugin (e.g.,
     *                 {@code "ai.nervemind.plugin.filewatcher"})
     * @return {@code true} if the plugin is enabled, {@code false} otherwise
     *         or if the plugin ID is not recognized
     * @see #enablePlugin(String) To enable a plugin
     * @see #disablePlugin(String) To disable a plugin
     */
    boolean isPluginEnabled(String pluginId);

    /**
     * Enables a plugin, making it available for use in workflows.
     * 
     * <p>
     * After enabling, the plugin will appear in:
     * </p>
     * <ul>
     * <li>The node palette sidebar</li>
     * <li>The right-click context menu for adding nodes</li>
     * <li>The list of available triggers/actions when configuring workflows</li>
     * </ul>
     * 
     * <p>
     * The enabled state is persisted immediately and survives application restarts.
     * </p>
     * 
     * @param pluginId the unique identifier of the plugin to enable
     * @throws IllegalArgumentException if pluginId is null or empty
     * @see #disablePlugin(String) To reverse this operation
     * @see #setPluginEnabled(String, boolean) For conditional enable/disable
     */
    void enablePlugin(String pluginId);

    /**
     * Disables a plugin, removing it from available workflow nodes.
     * 
     * <p>
     * <strong>Important:</strong> Disabling a plugin does not affect existing
     * workflows that use nodes from this plugin. Those workflows will show an
     * error indicator, but the workflow definition is preserved.
     * </p>
     * 
     * @param pluginId the unique identifier of the plugin to disable
     * @throws IllegalArgumentException if pluginId is null or empty
     * @see #enablePlugin(String) To reverse this operation
     */
    void disablePlugin(String pluginId);

    /**
     * Sets the enabled state of a plugin.
     * 
     * <p>
     * This is a convenience method that delegates to either
     * {@link #enablePlugin(String)} or {@link #disablePlugin(String)}
     * based on the {@code enabled} parameter.
     * </p>
     * 
     * @param pluginId the unique identifier of the plugin
     * @param enabled  {@code true} to enable, {@code false} to disable
     * @see #enablePlugin(String)
     * @see #disablePlugin(String)
     */
    void setPluginEnabled(String pluginId, boolean enabled);

    /**
     * Gets the handle definitions for a plugin node type.
     * 
     * <p>
     * Handle definitions describe the connection points on a node. Most nodes
     * have a single input on the left and a single output on the right, but
     * plugins can define custom handle configurations.
     * </p>
     * 
     * @param nodeType the node type identifier
     * @return list of handle definitions, or default (1 input, 1 output) if not a
     *         plugin
     */
    default List<HandleInfo> getHandleDefinitions(String nodeType) {
        // Default implementation returns standard handles
        return List.of(
                new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                new HandleInfo("out", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, null));
    }

    /**
     * Gets the custom view for a plugin node, if available.
     * 
     * <p>
     * Plugins may provide custom JavaFX nodes for rendering on the canvas.
     * This method returns the custom view or null if the default rendering
     * should be used.
     * </p>
     * 
     * @param nodeType    the node type identifier
     * @param nodeId      the instance node ID
     * @param displayName the display name of the node
     * @param settings    the node's current settings
     * @param selected    whether the node is selected
     * @param executing   whether the node is executing
     * @param error       whether the node has an error
     * @return the custom JavaFX node to render, or null to use default
     */
    default Object getCustomNodeView(String nodeType, String nodeId, String displayName,
            java.util.Map<String, Object> settings, boolean selected,
            boolean executing, boolean error) {
        return null;
    }

    /**
     * Gets all menu contributions from enabled plugins.
     * 
     * <p>
     * Aggregates menu contributions from all currently enabled plugins.
     * Each contribution specifies where it should appear and what action
     * to perform when clicked.
     * </p>
     * 
     * @return list of menu contributions from all enabled plugins
     */
    default List<MenuContribution> getAllMenuContributions() {
        return List.of();
    }

    /**
     * Gets all side panel contributions from enabled plugins.
     * 
     * <p>
     * Aggregates side panel contributions from all currently enabled plugins.
     * Each panel can appear on the left, right, or bottom of the main canvas.
     * </p>
     * 
     * @return list of side panel contributions from all enabled plugins
     */
    default List<SidePanelContribution> getAllSidePanelContributions() {
        return List.of();
    }

    /**
     * Gets the actual plugin provider instance for a given plugin ID.
     * 
     * <p>
     * This is used for advanced plugin integration where direct access to the
     * plugin instance is needed, such as setting up listeners on plugin classes.
     * The returned object is the actual PluginProvider instance loaded by the
     * plugin's classloader.
     * </p>
     * 
     * @param pluginId the plugin ID (node type)
     * @return the plugin provider instance, or null if not found or not enabled
     */
    default Object getPluginProviderInstance(String pluginId) {
        return null;
    }

    /**
     * Notifies all enabled plugins that a connection has been created.
     * 
     * <p>
     * This is called when a connection is created on the canvas, either through
     * user interaction or when loading a workflow. Plugins can use this to restore
     * visual decorations (like labels) from workflow settings.
     * </p>
     * 
     * @param connectionContext the connection context providing access to
     *                          connection information and operations
     */
    default void notifyConnectionCreated(Object connectionContext) {
        // Default: no action - implemented by PluginService
    }

    /**
     * Callback interface for context-aware menu actions.
     * 
     * <p>
     * Used for menu items in context menus (NODE_CONTEXT, CONNECTION_CONTEXT, etc.)
     * where the action needs to know what element was right-clicked.
     * </p>
     */
    @FunctionalInterface
    interface ContextMenuAction {
        /**
         * Executes the menu action with the provided context.
         * 
         * @param context map containing context information about the clicked element
         */
        void execute(java.util.Map<String, Object> context);
    }

    /**
     * Record representing a menu contribution from a plugin.
     * 
     * @param pluginId      the ID of the plugin providing this contribution
     * @param location      where in the menu structure this item should appear
     * @param label         the text to display (null for separators)
     * @param iconName      Material Design icon name, or null for no icon
     * @param action        the action to run when clicked (null for
     *                      context/submenus/separators)
     * @param contextAction the context-aware action for context menus (null for
     *                      regular items)
     * @param children      child menu items for submenus (empty for regular items)
     * @param isSeparator   whether this is a separator line
     */
    record MenuContribution(
            String pluginId,
            MenuLocation location,
            String label,
            String iconName,
            Runnable action,
            ContextMenuAction contextAction,
            List<MenuContribution> children,
            boolean isSeparator) {

        /**
         * Creates a regular menu item contribution.
         * 
         * @param pluginId the plugin ID
         * @param location the menu location
         * @param label    the display label
         * @param iconName the icon name
         * @param action   the action to run
         * @return a new MenuContribution instance
         */
        public static MenuContribution item(String pluginId, MenuLocation location,
                String label, String iconName, Runnable action) {
            return new MenuContribution(pluginId, location, label, iconName, action, null, List.of(), false);
        }

        /**
         * Creates a context menu item contribution.
         * 
         * @param pluginId      the plugin ID
         * @param location      the menu location
         * @param label         the display label
         * @param iconName      the icon name
         * @param contextAction the context-aware action
         * @return a new MenuContribution instance
         */
        public static MenuContribution contextItem(String pluginId, MenuLocation location,
                String label, String iconName, ContextMenuAction contextAction) {
            return new MenuContribution(pluginId, location, label, iconName, null, contextAction, List.of(), false);
        }

        /**
         * Creates a menu separator contribution.
         * 
         * @param pluginId the plugin ID
         * @param location the menu location
         * @return a new MenuContribution instance
         */
        public static MenuContribution separator(String pluginId, MenuLocation location) {
            return new MenuContribution(pluginId, location, null, null, null, null, List.of(), true);
        }

        /**
         * Creates a submenu contribution.
         * 
         * @param pluginId the plugin ID
         * @param location the menu location
         * @param label    the display label
         * @param iconName the icon name
         * @param children the list of child menu items
         * @return a new MenuContribution instance
         */
        public static MenuContribution submenu(String pluginId, MenuLocation location,
                String label, String iconName, List<MenuContribution> children) {
            return new MenuContribution(pluginId, location, label, iconName, null, null, children, false);
        }

        /**
         * Checks if this contribution is a submenu (has children).
         * 
         * @return true if this is a submenu
         */
        public boolean isSubmenu() {
            return !children.isEmpty();
        }

        /**
         * Checks if this contribution represents a clickable action item.
         * 
         * @return true if this is an action item
         */
        public boolean isActionItem() {
            return !isSeparator && (action != null || contextAction != null) && children.isEmpty();
        }

        /**
         * Checks if this contribution is a context-aware action item.
         * 
         * @return true if this is a context-aware action item
         */
        public boolean isContextActionItem() {
            return contextAction != null;
        }
    }

    /**
     * Menu locations where plugin contributions can be placed.
     */
    enum MenuLocation {
        /** File menu. */
        FILE,
        /** Edit menu. */
        EDIT,
        /** View menu. */
        VIEW,
        /** Workflow menu. */
        WORKFLOW,
        /** Tools menu. */
        TOOLS,
        /** Help menu. */
        HELP,
        /** Right-click context menu on the canvas. */
        CANVAS_CONTEXT,
        /** Right-click context menu on a node. */
        NODE_CONTEXT,
        /** Right-click context menu on a connection. */
        CONNECTION_CONTEXT
    }

    /**
     * Record representing a side panel contribution from a plugin.
     * 
     * @param pluginId        the ID of the plugin providing this panel
     * @param id              unique identifier for this panel
     * @param title           the display title shown in the panel header
     * @param iconName        Material Design icon name for the panel tab
     * @param position        where the panel should appear
     * @param preferredWidth  preferred width in pixels
     * @param contentSupplier factory function that creates the panel content
     */
    record SidePanelContribution(
            String pluginId,
            String id,
            String title,
            String iconName,
            PanelPosition position,
            int preferredWidth,
            Supplier<Object> contentSupplier) {

        /**
         * Creates a right-side panel contribution.
         * 
         * @param pluginId the plugin ID
         * @param id       the panel ID
         * @param title    the panel title
         * @param iconName the icon name
         * @param supplier the content supplier
         * @return a new SidePanelContribution instance
         */
        public static SidePanelContribution rightPanel(String pluginId, String id,
                String title, String iconName, Supplier<Object> supplier) {
            return new SidePanelContribution(pluginId, id, title, iconName,
                    PanelPosition.RIGHT, 300, supplier);
        }

        /**
         * Creates a left-side panel contribution.
         * 
         * @param pluginId the plugin ID
         * @param id       the panel ID
         * @param title    the panel title
         * @param iconName the icon name
         * @param supplier the content supplier
         * @return a new SidePanelContribution instance
         */
        public static SidePanelContribution leftPanel(String pluginId, String id,
                String title, String iconName, Supplier<Object> supplier) {
            return new SidePanelContribution(pluginId, id, title, iconName,
                    PanelPosition.LEFT, 250, supplier);
        }
    }

    /**
     * Positions where side panels can appear.
     */
    enum PanelPosition {
        /** Left side panel. */
        LEFT,
        /** Right side panel. */
        RIGHT,
        /** Bottom side panel. */
        BOTTOM
    }

    /**
     * Record representing a connection handle on a node.
     * 
     * @param id       unique handle identifier within the node
     * @param type     whether this is an input or output handle
     * @param position where the handle is positioned on the node
     * @param label    optional display label for the handle
     */
    record HandleInfo(String id, Type type, Position position, String label) {
        /** Handle types. */
        public enum Type {
            /** Input handle. */
            INPUT,
            /** Output handle. */
            OUTPUT
        }

        /** Handle positions. */
        public enum Position {
            /** Positioned on the left side. */
            LEFT,
            /** Positioned on the right side. */
            RIGHT,
            /** Positioned on the top side. */
            TOP,
            /** Positioned on the bottom side. */
            BOTTOM
        }

        /**
         * Checks if this handle is an input handle.
         * 
         * @return true if an input handle
         */
        public boolean isInput() {
            return type == Type.INPUT;
        }

        /**
         * Checks if this handle is an output handle.
         * 
         * @return true if an output handle
         */
        public boolean isOutput() {
            return type == Type.OUTPUT;
        }
    }

    /**
     * Immutable record containing plugin metadata for UI display.
     * 
     * <p>
     * This record provides all information needed to display a plugin
     * in the Plugin Manager dialog and render plugin nodes in the UI,
     * including visual appearance and help content.
     * </p>
     * 
     * <h2>Field Descriptions</h2>
     * <dl>
     * <dt>{@code id}</dt>
     * <dd>Unique plugin identifier, typically the node type
     * (e.g., {@code "fileTrigger"})</dd>
     * 
     * <dt>{@code name}</dt>
     * <dd>Human-readable display name shown in the UI
     * (e.g., {@code "File Trigger"})</dd>
     * 
     * <dt>{@code description}</dt>
     * <dd>Brief description of what the plugin does</dd>
     * 
     * <dt>{@code version}</dt>
     * <dd>Semantic version string (e.g., {@code "1.0.0"})</dd>
     * 
     * <dt>{@code pluginType}</dt>
     * <dd>Plugin category: {@link PluginType#TRIGGER} or
     * {@link PluginType#ACTION}</dd>
     * 
     * <dt>{@code enabled}</dt>
     * <dd>Current enabled state at the time of query</dd>
     * 
     * <dt>{@code iconName}</dt>
     * <dd>Material Design icon name in UPPER_SNAKE_CASE (e.g., "FILE_EYE")</dd>
     * 
     * <dt>{@code category}</dt>
     * <dd>Node category for palette organization</dd>
     * 
     * <dt>{@code subtitle}</dt>
     * <dd>Short text shown below the node name (e.g., "file watch")</dd>
     * 
     * <dt>{@code helpText}</dt>
     * <dd>Extended help content for the help panel</dd>
     * </dl>
     * 
     * @param id          unique plugin identifier (node type)
     * @param name        display name for UI
     * @param description brief description of functionality
     * @param version     semantic version string
     * @param pluginType  plugin type enum (TRIGGER or ACTION)
     * @param enabled     current enabled state
     * @param iconName    Material Design icon name
     * @param category    node category for palette
     * @param subtitle    short subtitle for node display
     * @param helpText    extended help content
     */
    record PluginInfo(
            String id,
            String name,
            String description,
            String version,
            PluginType pluginType,
            boolean enabled,
            String iconName,
            ai.nervemind.common.enums.NodeCategory category,
            String subtitle,
            String helpText) {

        /**
         * Check if this is a trigger plugin.
         * 
         * @return true if this is a trigger plugin
         */
        public boolean isTrigger() {
            return pluginType == PluginType.TRIGGER;
        }

        /**
         * Check if this is an action plugin.
         * 
         * @return true if this is an action plugin
         */
        public boolean isAction() {
            return pluginType == PluginType.ACTION;
        }
    }
}
