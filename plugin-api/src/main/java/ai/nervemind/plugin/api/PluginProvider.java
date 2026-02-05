package ai.nervemind.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unified interface for NerveMind plugins.
 * 
 * <p>
 * This is the primary interface that plugin developers implement to provide
 * custom nodes, UI extensions, and samples to NerveMind. It consolidates the
 * functionality previously spread across {@link NodeDescriptor},
 * {@link NodeExecutor},
 * {@link TriggerProvider}, and {@link ActionProvider} into a single
 * comprehensive
 * interface.
 * </p>
 * 
 * <h2>Plugin Registration</h2>
 * <p>
 * Plugins are discovered using the Java ServiceLoader mechanism. Create a file
 * at:
 * </p>
 * 
 * <pre>
 * META - INF / services / ai.nervemind.plugin.api.PluginProvider
 * </pre>
 * <p>
 * The file should contain the fully qualified class name of each
 * implementation.
 * </p>
 * 
 * <h2>Required vs Optional Methods</h2>
 * <table border="1">
 * <caption>PluginProvider interface methods</caption>
 * <tr>
 * <th>Method</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link #getNodeType()}</td>
 * <td>Yes</td>
 * <td>Unique node type ID</td>
 * </tr>
 * <tr>
 * <td>{@link #getDisplayName()}</td>
 * <td>Yes</td>
 * <td>Name shown in UI</td>
 * </tr>
 * <tr>
 * <td>{@link #getDescription()}</td>
 * <td>Yes</td>
 * <td>Tooltip text</td>
 * </tr>
 * <tr>
 * <td>{@link #getCategory()}</td>
 * <td>Yes</td>
 * <td>Palette category</td>
 * </tr>
 * <tr>
 * <td>{@link #getProperties()}</td>
 * <td>Yes</td>
 * <td>Configurable properties</td>
 * </tr>
 * <tr>
 * <td>{@link #execute(ExecutionContext)}</td>
 * <td>Yes</td>
 * <td>Execution logic</td>
 * </tr>
 * <tr>
 * <td>{@link #getHandles()}</td>
 * <td>No</td>
 * <td>Custom connection handles</td>
 * </tr>
 * <tr>
 * <td>{@link #createNodeView(NodeViewContext)}</td>
 * <td>No</td>
 * <td>Custom rendering</td>
 * </tr>
 * <tr>
 * <td>{@link #getMenuContributions()}</td>
 * <td>No</td>
 * <td>Menu items</td>
 * </tr>
 * <tr>
 * <td>{@link #getSidePanel()}</td>
 * <td>No</td>
 * <td>Side panel</td>
 * </tr>
 * <tr>
 * <td>{@link #getSamples()}</td>
 * <td>No</td>
 * <td>Sample workflows</td>
 * </tr>
 * </table>
 * 
 * <h2>Minimal Implementation Example</h2>
 *
 * <pre>{@code
 * public class MyNodePlugin implements PluginProvider {
 *     &#64;Override
 *     public String getNodeType() {
 *         return "com.example.myplugin.mynode";
 *     }
 *
 *     &#64;Override
 *     public String getDisplayName() {
 *         return "My Custom Node";
 *     }
 *
 *     &#64;Override
 *     public String getDescription() {
 *         return "Does something useful";
 *     }
 *
 *     &#64;Override
 *     public NodeCategory getCategory() {
 *         return NodeCategory.ACTION;
 *     }
 *
 *     &#64;Override
 *     public List<PropertyDefinition> getProperties() {
 *         return List.of(
 *                 PropertyDefinition.requiredString("input", "Input", "The input value"));
 *     }
 *
 *     &#64;Override
 *     public Map<String, Object> execute(ExecutionContext context)
 *             throws NodeExecutionException {
 *         String input = (String) context.getNodeSettings().get("input");
 *         return Map.of("result", "Processed: " + input);
 *     }
 * }
 * }</pre>
 * 
 * <h2>Full-Featured Plugin Example</h2>
 *
 * <pre>{@code
 * public class AdvancedPlugin implements PluginProvider {
 *     // ... required methods ...
 *
 *     &#64;Override
 *     public List<HandleDefinition> getHandles() {
 *         return List.of(
 *                 HandleDefinition.input("in", HandlePosition.LEFT),
 *                 HandleDefinition.output("success", HandlePosition.RIGHT, "Success"),
 *                 HandleDefinition.output("error", HandlePosition.BOTTOM, "Error"));
 *     }
 *
 *     &#64;Override
 *     public javafx.scene.Node createNodeView(NodeViewContext context) {
 *         // Return custom JavaFX node for canvas rendering
 *         return new MyCustomNodeView(context);
 *     }
 *
 *     &#64;Override
 *     public List<MenuContribution> getMenuContributions() {
 *         return List.of(
 *                 MenuContribution.item(MenuLocation.TOOLS, "My Tool", "WRENCH", this::showTool));
 *     }
 * }
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ExecutionContext For runtime context during execution
 * @see PropertyDefinition For defining configurable properties
 * @see HandleDefinition For custom connection handles
 */
public interface PluginProvider {

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUIRED: Core Node Definition
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the unique identifier for this node type.
     * 
     * <p>
     * This ID must be globally unique across all plugins. Use reverse domain
     * notation to avoid conflicts (e.g., {@code "com.example.myplugin.slack"}).
     * </p>
     * 
     * <p>
     * This ID is used for serialization and must remain stable across versions.
     * </p>
     * 
     * @return the unique node type identifier
     */
    String getNodeType();

    /**
     * Gets the display name shown in the UI palette.
     * 
     * <p>
     * This should be a short, human-readable name (2-4 words).
     * </p>
     * 
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the description shown in tooltips.
     * 
     * <p>
     * This should briefly explain what the node does (1-2 sentences).
     * </p>
     * 
     * @return the description
     */
    String getDescription();

    /**
     * Gets the category for grouping in the node palette.
     * 
     * @return the node category
     */
    NodeCategory getCategory();

    /**
     * Gets the configurable properties for this node.
     * 
     * <p>
     * These properties are displayed in the properties panel when the node
     * is selected. Values are passed to {@link #execute(ExecutionContext)}
     * via the context's node settings.
     * </p>
     * 
     * @return list of property definitions
     */
    List<PropertyDefinition> getProperties();

    /**
     * Executes the node logic.
     * 
     * <p>
     * This method is called by the workflow runtime when the node runs.
     * It receives configuration and input data via the context and should
     * return output data for downstream nodes.
     * </p>
     * 
     * @param context the execution context
     * @return output data as a map of key-value pairs
     * @throws NodeExecutionException if execution fails
     */
    Map<String, Object> execute(ExecutionContext context) throws NodeExecutionException;

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Node Metadata
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the icon name from Material Design Icons.
     * 
     * <p>
     * See <a href="https://materialdesignicons.com/">Material Design Icons</a>
     * for available icons. Use the icon name in UPPER_SNAKE_CASE.
     * </p>
     * 
     * @return the icon name (default: "PUZZLE")
     */
    default String getIconName() {
        return "PUZZLE";
    }

    /**
     * Gets a short subtitle displayed below the node name.
     * 
     * <p>
     * The subtitle provides quick context about the node's function.
     * It should be very short (1-2 words). If not provided, a default
     * subtitle is derived from the display name.
     * </p>
     * 
     * @return the subtitle text (default: derived from display name)
     */
    default String getSubtitle() {
        // Default: extract last word from display name and lowercase it
        String name = getDisplayName();
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] words = name.split("\\s+");
        return words[words.length - 1].toLowerCase();
    }

    /**
     * Gets extended help text for the help panel.
     * 
     * <p>
     * This text is shown when users access help for this node type.
     * It should explain what the node does, its parameters, and provide
     * usage examples. Markdown formatting is supported.
     * </p>
     * 
     * @return the help text (default: uses description)
     */
    default String getHelpText() {
        return getDescription();
    }

    /**
     * Gets the plugin version.
     * 
     * <p>
     * Use semantic versioning (e.g., "1.0.0", "2.1.3").
     * </p>
     * 
     * @return the version string (default: "1.0.0")
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Indicates whether this node is a trigger (workflow entry point).
     * 
     * <p>
     * Trigger nodes start workflow execution. They typically respond to
     * external events or user actions. Non-trigger nodes are actions that
     * process data within the workflow.
     * </p>
     * 
     * @return true if this is a trigger node (default: false)
     */
    default boolean isTrigger() {
        return false;
    }

    /**
     * Indicates whether a trigger requires a background service.
     * 
     * <p>
     * Background triggers run continuously while the workflow is active
     * (e.g., file watchers, scheduled tasks). Non-background triggers
     * are activated manually or by one-time events.
     * </p>
     * 
     * <p>
     * This setting is only relevant when {@link #isTrigger()} returns true.
     * </p>
     * 
     * @return true if the trigger needs background processing (default: false)
     */
    default boolean requiresBackgroundService() {
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates the node's configuration.
     * 
     * <p>
     * Called before workflow execution to check if the node is properly
     * configured. Return validation errors for missing or invalid settings.
     * </p>
     * 
     * @param settings the node's current settings
     * @return validation result (default: always valid)
     */
    default ValidationResult validate(Map<String, Object> settings) {
        return ValidationResult.valid();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Custom Rendering
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the connection handles for this node.
     * 
     * <p>
     * Override this to define custom connection points. The default provides
     * a single input on the left and a single output on the right.
     * </p>
     * 
     * <p>
     * <strong>Example: Conditional Node</strong>
     * </p>
     * 
     * <pre>{@code
     * return List.of(
     *         HandleDefinition.input("in", HandlePosition.LEFT),
     *         HandleDefinition.output("then", HandlePosition.RIGHT, "Then"),
     *         HandleDefinition.output("else", HandlePosition.BOTTOM, "Else"));
     * }</pre>
     * 
     * @return list of handle definitions (default: standard left input, right
     *         output)
     */
    default List<HandleDefinition> getHandles() {
        return List.of(
                HandleDefinition.input("in", HandlePosition.LEFT),
                HandleDefinition.output("out", HandlePosition.RIGHT));
    }

    /**
     * Creates a custom view for rendering on the canvas.
     * 
     * <p>
     * Return null to use the default node rendering. Custom views can display
     * additional information, use different shapes, or provide interactive
     * elements within the node.
     * </p>
     * 
     * <p>
     * <strong>Note:</strong> The return type is {@code Object} to avoid a direct
     * dependency on JavaFX in the plugin API. Implementations should return a
     * {@code javafx.scene.Node} instance.
     * </p>
     * 
     * <p>
     * <strong>Guidelines</strong>
     * </p>
     * <ul>
     * <li>Respect the suggested dimensions in the context</li>
     * <li>Handle selection and execution states visually</li>
     * <li>Keep the view lightweight (many nodes may be visible)</li>
     * </ul>
     * 
     * @param context the view context with node state information
     * @return a custom javafx.scene.Node, or null to use default rendering
     */
    default Object createNodeView(NodeViewContext context) {
        return null;
    }

    /**
     * Creates a custom property editor for a specific property.
     * 
     * <p>
     * Return null to use the default editor for the property type. Custom
     * editors can provide specialized input controls like color pickers,
     * date selectors, or complex composite editors.
     * </p>
     * 
     * <p>
     * <strong>Note:</strong> The return type is {@code Object} to avoid a direct
     * dependency on JavaFX in the plugin API. Implementations should return a
     * {@code javafx.scene.Node} instance.
     * </p>
     * 
     * @param propertyName the name of the property
     * @param context      the editor context with current value and change callback
     * @return a custom javafx.scene.Node editor, or null to use default
     */
    default Object createPropertyEditor(String propertyName,
            PropertyEditorContext context) {
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: UI Extensions
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets menu items to contribute to the application.
     * 
     * <p>
     * Menu items can be added to the main menu bar or context menus.
     * Use {@link MenuLocation} to specify where items should appear.
     * </p>
     * 
     * @return list of menu contributions (default: empty)
     * @see MenuContribution
     * @see MenuLocation
     */
    default List<MenuContribution> getMenuContributions() {
        return List.of();
    }

    /**
     * Gets a side panel to contribute to the workspace.
     * 
     * <p>
     * Side panels appear in collapsible areas beside the main canvas.
     * They can provide tools, information displays, or additional controls.
     * </p>
     * 
     * @return optional side panel contribution (default: empty)
     * @see SidePanelContribution
     */
    default Optional<SidePanelContribution> getSidePanel() {
        return Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Sample Workflows
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets sample workflows that demonstrate this plugin.
     * 
     * <p>
     * Samples appear in the Samples Browser with a plugin badge. Provide
     * samples to help users understand how to use your node type effectively.
     * </p>
     * 
     * @return list of sample definitions (default: empty)
     * @see SampleDefinition
     */
    default List<SampleDefinition> getSamples() {
        return List.of();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Called when the plugin is enabled.
     * 
     * <p>
     * Use this to initialize resources, start background services, or
     * register additional components.
     * </p>
     */
    default void onEnable() {
        // Default: no action
    }

    /**
     * Called when the plugin is disabled.
     * 
     * <p>
     * Use this to clean up resources, stop background services, or
     * unregister components.
     * </p>
     */
    default void onDisable() {
        // Default: no action
    }

    /**
     * Called when a connection is created or restored on the canvas.
     * 
     * <p>
     * Plugins can override this to apply visual decorations, restore persisted
     * state (like labels), or perform other connection-related initialization.
     * This is called for both new connections and connections loaded from a
     * saved workflow.
     * </p>
     * 
     * <p>
     * Example: A label plugin can check workflow settings and restore labels:
     * </p>
     * 
     * <pre>{@code
     * @Override
     * public void onConnectionCreated(ConnectionContext connection) {
     *     Map<String, String> labels = connection.getWorkflowSetting("connectionLabels");
     *     if (labels != null && labels.containsKey(connection.getConnectionId())) {
     *         connection.setLabel(labels.get(connection.getConnectionId()));
     *     }
     * }
     * }</pre>
     * 
     * @param connection the connection context providing access to connection
     *                   information and operations
     */
    default void onConnectionCreated(ConnectionContext connection) {
        // Default: no action
    }

    /**
     * Called when execution is cancelled.
     * 
     * <p>
     * Override this to perform cleanup when a running execution is cancelled.
     * This is called from a different thread than
     * {@link #execute(ExecutionContext)}.
     * </p>
     */
    default void cancel() {
        // Default: no action
    }
}
