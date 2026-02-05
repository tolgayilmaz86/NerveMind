/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.plugin.api;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Context object providing access to connection information and operations.
 * 
 * <p>
 * This interface is passed to plugins via the context menu action's context map
 * when handling {@link MenuLocation#CONNECTION_CONTEXT} menus. It provides all
 * the information and operations a plugin needs to work with connections.
 * </p>
 * 
 * <h2>Key Capabilities</h2>
 * <ul>
 * <li>Read connection metadata (IDs, source/target info)</li>
 * <li>Add visual decorations (labels, badges, etc.) to the connection</li>
 * <li>Update workflow settings to persist plugin data</li>
 * <li>Show dialogs using the provided JavaFX utilities</li>
 * </ul>
 * 
 * <h2>Decoration API</h2>
 * <p>
 * Plugins can add custom visual elements to connections using the decoration
 * API.
 * Decorations are positioned at the midpoint of the connection curve. The
 * plugin
 * is responsible for creating and styling the visual nodes - ConnectionContext
 * handles positioning.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // In your ContextMenuAction handler:
 * public void handleAddLabel(Map<String, Object> context) {
 *     ConnectionContext conn = (ConnectionContext) context.get("connection");
 * 
 *     // Create your own label visual (e.g., styled Text, Group, etc.)
 *     Text labelNode = new Text("Data Flow");
 *     labelNode.setFill(Color.WHITE);
 * 
 *     // Add as a decoration - it will be positioned at the curve midpoint
 *     conn.addDecoration("my-label", labelNode);
 * 
 *     // Persist to workflow settings
 *     Map<String, String> labels = conn.getWorkflowSetting("connectionLabels");
 *     if (labels == null)
 *         labels = new HashMap<>();
 *     labels.put(conn.getConnectionId(), "Data Flow");
 *     conn.setWorkflowSetting("connectionLabels", labels);
 * }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Methods that modify the UI (addDecoration, removeDecoration) must be called
 * on the JavaFX Application Thread. Use {@link #runOnFxThread(Runnable)} to
 * ensure correct threading.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ContextMenuAction
 * @see MenuLocation#CONNECTION_CONTEXT
 */
public interface ConnectionContext {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONNECTION INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets the unique identifier of this connection.
     * 
     * @return the connection ID, never null
     */
    String getConnectionId();

    /**
     * Gets the ID of the source node.
     * 
     * @return the source node ID
     */
    String getSourceNodeId();

    /**
     * Gets the ID of the target node.
     * 
     * @return the target node ID
     */
    String getTargetNodeId();

    /**
     * Gets the output handle identifier on the source node.
     * 
     * @return the source output handle ID
     */
    String getSourceOutput();

    /**
     * Gets the input handle identifier on the target node.
     * 
     * @return the target input handle ID
     */
    String getTargetInput();

    /**
     * Gets the workflow ID, if the workflow has been saved.
     * 
     * @return the workflow ID, or null if the workflow is unsaved
     */
    Long getWorkflowId();

    // ═══════════════════════════════════════════════════════════════════════════
    // DECORATION API - Generic visual extension point
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a visual decoration to this connection.
     * 
     * <p>
     * Decorations are positioned at the midpoint of the connection curve.
     * The node should be pre-configured with its visual appearance; this method
     * only handles positioning. The node's layout coordinates (layoutX, layoutY)
     * will be set to the midpoint - design your node to be centered at (0,0).
     * </p>
     * 
     * <p>
     * If a decoration with the same key already exists, it will be replaced.
     * </p>
     * 
     * <p>
     * Must be called on the JavaFX Application Thread.
     * </p>
     * 
     * @param key  a unique identifier for this decoration (for later
     *             retrieval/removal)
     * @param node the JavaFX node to display (should be centered at its origin)
     */
    void addDecoration(String key, Object node);

    /**
     * Removes a decoration from this connection.
     * 
     * @param key the identifier of the decoration to remove
     */
    void removeDecoration(String key);

    /**
     * Gets a decoration by its key.
     * 
     * @param key the decoration identifier
     * @return the decoration node, or null if not found
     */
    Object getDecoration(String key);

    /**
     * Checks if a decoration with the given key exists.
     * 
     * @param key the decoration identifier
     * @return true if the decoration exists
     */
    boolean hasDecoration(String key);

    // ═══════════════════════════════════════════════════════════════════════════
    // LABEL CONVENIENCE API - Common use case for plugins
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets a text label on this connection.
     * 
     * <p>
     * This is a convenience method for the common case of adding a text label
     * to a connection. The host application handles creating the visual elements
     * (styled text with background). This allows plugins to add labels without
     * depending on JavaFX.
     * </p>
     * 
     * <p>
     * Internally, this creates a decoration with the key "label". Calling this
     * method will replace any existing label decoration.
     * </p>
     * 
     * <p>
     * Note: This only updates the visual display. To persist the label, you should
     * also store it in workflow settings using
     * {@link #setWorkflowSetting(String, Object)}.
     * </p>
     * 
     * @param text the label text, or null to remove the label
     */
    void setLabel(String text);

    /**
     * Gets the current text label on this connection, if any.
     * 
     * @return the label text, or null if no label is set
     */
    String getLabel();

    /**
     * Gets the midpoint coordinates of this connection's curve.
     * 
     * <p>
     * Useful for plugins that need to position elements relative to the connection
     * but want to manage their own positioning logic.
     * </p>
     * 
     * @return array of [x, y] coordinates at the curve midpoint
     */
    double[] getMidpoint();

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW SETTINGS (for persistence)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets a value from the workflow settings.
     * 
     * <p>
     * Workflow settings are persisted with the workflow and are the recommended
     * way to store plugin-specific data.
     * </p>
     * 
     * @param <T> the expected type of the setting value
     * @param key the setting key
     * @return the setting value, or null if not set
     */
    <T> T getWorkflowSetting(String key);

    /**
     * Sets a value in the workflow settings.
     * 
     * <p>
     * The value will be serialized to JSON when the workflow is saved.
     * Use simple types (String, Number, Boolean, Map, List) for reliable
     * serialization.
     * </p>
     * 
     * @param key   the setting key
     * @param value the setting value, or null to remove
     */
    void setWorkflowSetting(String key, Object value);

    /**
     * Gets all workflow settings as an immutable map.
     * 
     * @return the workflow settings map
     */
    Map<String, Object> getWorkflowSettings();

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Executes a runnable on the JavaFX Application Thread.
     * 
     * <p>
     * Use this to ensure UI operations (like setLabel) are performed on the
     * correct thread.
     * </p>
     * 
     * @param runnable the code to execute
     */
    void runOnFxThread(Runnable runnable);

    /**
     * Shows a text input dialog and returns the result.
     * 
     * <p>
     * This is a convenience method for plugins that need to prompt for text input.
     * The dialog is styled to match the application theme.
     * </p>
     * 
     * @param title        the dialog title
     * @param headerText   the header text (can be null)
     * @param promptText   the prompt/instruction text
     * @param defaultValue the default value in the text field (can be null)
     * @param onResult     callback with the entered text, or null if cancelled
     */
    void showTextInputDialog(String title, String headerText, String promptText,
            String defaultValue, Consumer<String> onResult);
}
