package ai.nervemind.plugin.api;

import java.util.List;

/**
 * Base interface describing a workflow node provided by a plugin.
 * 
 * <p>
 * This interface defines the metadata contract that all plugin nodes (both
 * triggers
 * and actions) must implement. The metadata is used by the NerveMind UI to
 * display nodes
 * in the palette, configure them in the properties panel, and validate their
 * settings.
 * </p>
 * 
 * <h2>Inheritance Hierarchy</h2>
 * 
 * <pre>
 *                    NodeDescriptor
 *                          │
 *          ┌───────────────┴───────────────┐
 *          │                               │
 *   TriggerProvider                 ActionProvider
 *          │                               │
 *  (triggers workflows)           (processes data)
 * </pre>
 * 
 * <h2>Required vs Optional Methods</h2>
 * <table border="1">
 * <caption>NodeDescriptor interface methods</caption>
 * <tr>
 * <th>Method</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link #getNodeType()}</td>
 * <td>Yes</td>
 * <td>Unique ID for the node type</td>
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
 * <td>TRIGGER or ACTION</td>
 * </tr>
 * <tr>
 * <td>{@link #getProperties()}</td>
 * <td>Yes</td>
 * <td>Configurable properties</td>
 * </tr>
 * <tr>
 * <td>{@link #getIconName()}</td>
 * <td>No</td>
 * <td>Custom icon (default: PUZZLE)</td>
 * </tr>
 * <tr>
 * <td>{@link #getVersion()}</td>
 * <td>No</td>
 * <td>Version string (default: 1.0.0)</td>
 * </tr>
 * </table>
 * 
 * <h2>Node Type Naming Convention</h2>
 * <p>
 * Use reverse domain notation for node types to avoid conflicts:
 * </p>
 * <ul>
 * <li>Built-in: {@code ai.nervemind.httpRequest}</li>
 * <li>Plugin: {@code com.example.myplugin.customaction}</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see TriggerProvider For implementing trigger nodes
 * @see ActionProvider For implementing action nodes
 * @see PropertyDefinition For defining configurable properties
 */
public interface NodeDescriptor {

    /**
     * Gets the unique identifier for this node type.
     * 
     * <p>
     * This ID must be globally unique across all plugins. Convention is to use
     * reverse domain notation (e.g., {@code "ai.nervemind.plugin.filewatcher"}).
     * </p>
     * 
     * <p>
     * This ID is used internally for serialization and must remain stable
     * across versions to maintain workflow compatibility.
     * </p>
     * 
     * @return unique identifier string; must not be {@code null} or empty
     */
    String getNodeType();

    /**
     * Gets the human-readable display name for the UI.
     * 
     * <p>
     * This name appears in the node palette, on the canvas, and in dialogs.
     * It should be concise but descriptive (e.g., "HTTP Request", "File Trigger").
     * </p>
     * 
     * @return display name; must not be {@code null} or empty
     */
    String getDisplayName();

    /**
     * Gets a short description shown as tooltip in the UI.
     * 
     * <p>
     * Describe what the node does in 1-2 sentences. This appears when users
     * hover over the node in the palette.
     * </p>
     * 
     * @return description text; must not be {@code null}
     */
    String getDescription();

    /**
     * Gets the category for grouping this node in the palette.
     * 
     * @return the node category (TRIGGER or ACTION)
     * @see NodeCategory
     */
    NodeCategory getCategory();

    /**
     * Gets the icon name for display in the UI palette.
     * 
     * <p>
     * The icon name should correspond to a Material Design Icon name
     * (e.g., "FILE_EYE", "CODE", "WEB"). The default is "PUZZLE" for unknown nodes.
     * </p>
     * 
     * <p>
     * See <a href="https://materialdesignicons.com/">Material Design Icons</a>
     * for available icon names.
     * </p>
     * 
     * @return icon name string
     */
    default String getIconName() {
        return "PUZZLE";
    }

    /**
     * Gets the list of configurable properties for this node.
     * 
     * <p>
     * The UI will dynamically generate form fields based on these definitions.
     * Each property becomes a configurable field in the node's properties panel.
     * </p>
     * 
     * <p>
     * Example:
     * </p>
     * 
     * <pre>{@code
     * @Override
     * public List<PropertyDefinition> getProperties() {
     *     return List.of(
     *             PropertyDefinition.requiredString("url", "URL", "The endpoint URL"),
     *             PropertyDefinition.optionalString("method", "Method", "GET", "HTTP method"),
     *             PropertyDefinition.optionalBoolean("followRedirects", "Follow Redirects", true,
     *                     "Whether to follow HTTP redirects"));
     * }
     * }</pre>
     * 
     * @return list of property definitions; may be empty but never {@code null}
     * @see PropertyDefinition
     */
    List<PropertyDefinition> getProperties();

    /**
     * Gets the version of this node implementation.
     * 
     * <p>
     * Use semantic versioning (e.g., "1.0.0", "2.1.3"). This helps users
     * identify plugin versions and track compatibility.
     * </p>
     * 
     * @return version string in semantic versioning format
     */
    default String getVersion() {
        return "1.0.0";
    }
}
