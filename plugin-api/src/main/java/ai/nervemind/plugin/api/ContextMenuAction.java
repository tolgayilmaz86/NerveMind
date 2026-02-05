package ai.nervemind.plugin.api;

import java.util.Map;

/**
 * Callback interface for context-aware menu actions.
 * 
 * <p>
 * Used for menu items in context menus (NODE_CONTEXT, CONNECTION_CONTEXT, etc.)
 * where the action needs to know what element was right-clicked. The context
 * map provides information about the clicked element.
 * </p>
 * 
 * <h2>Connection Context Keys</h2>
 * For {@link MenuLocation#CONNECTION_CONTEXT} menus:
 * <ul>
 * <li>{@code connectionId} - String: The unique ID of the connection</li>
 * <li>{@code sourceNodeId} - String: The ID of the source node</li>
 * <li>{@code targetNodeId} - String: The ID of the target node</li>
 * <li>{@code sourceOutput} - String: The output handle identifier</li>
 * <li>{@code targetInput} - String: The input handle identifier</li>
 * <li>{@code workflowId} - Long: The workflow ID (may be null for unsaved)</li>
 * <li>{@code connectionLabel} - String: Current label if set (may be null)</li>
 * </ul>
 * 
 * <h2>Node Context Keys</h2>
 * For {@link MenuLocation#NODE_CONTEXT} menus:
 * <ul>
 * <li>{@code nodeId} - String: The unique ID of the node</li>
 * <li>{@code nodeType} - String: The type of the node</li>
 * <li>{@code nodeName} - String: The display name of the node</li>
 * <li>{@code workflowId} - Long: The workflow ID (may be null for unsaved)</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * MenuContribution.contextItem(
 *         MenuLocation.CONNECTION_CONTEXT,
 *         "Add Label",
 *         "LABEL",
 *         context -> {
 *             String connectionId = (String) context.get("connectionId");
 *             showLabelDialog(connectionId);
 *         })
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see MenuContribution#contextItem(MenuLocation, String, String,
 *      ContextMenuAction)
 */
@FunctionalInterface
public interface ContextMenuAction {

    /**
     * Executes the menu action with the provided context.
     * 
     * @param context map containing context information about the clicked element
     */
    void execute(Map<String, Object> context);
}
