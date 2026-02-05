package ai.nervemind.plugin.api;

import java.util.Map;

/**
 * Context provided to plugins when creating custom node views.
 * 
 * <p>
 * This record provides all the information a plugin needs to render a custom
 * node visualization on the workflow canvas. It includes node metadata, current
 * settings, and optional styling hints.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * @Override
 * public javafx.scene.Node createNodeView(NodeViewContext context) {
 *     var vbox = new VBox(5);
 *     vbox.setPrefSize(context.width(), context.height());
 * 
 *     var title = new Label(context.displayName());
 *     var status = new Label("Status: " + context.settings().get("status"));
 * 
 *     if (context.selected()) {
 *         vbox.setStyle("-fx-border-color: blue; -fx-border-width: 2;");
 *     }
 * 
 *     vbox.getChildren().addAll(title, status);
 *     return vbox;
 * }
 * }</pre>
 * 
 * @param nodeId      unique identifier of this node instance
 * @param nodeType    the node type identifier (e.g., "com.example.mynode")
 * @param displayName the user-visible name of the node
 * @param settings    current property settings for this node instance
 * @param width       suggested width in pixels (default: 80)
 * @param height      suggested height in pixels (default: 80)
 * @param selected    whether the node is currently selected on the canvas
 * @param executing   whether the node is currently being executed
 * @param error       whether the node has an error state
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#createNodeView(NodeViewContext)
 */
public record NodeViewContext(
        String nodeId,
        String nodeType,
        String displayName,
        Map<String, Object> settings,
        double width,
        double height,
        boolean selected,
        boolean executing,
        boolean error) {

    /**
     * Creates a default context for a node with standard dimensions.
     * 
     * @param nodeId      unique identifier of the node
     * @param nodeType    type identifier of the node
     * @param displayName display name for the node
     * @param settings    node configuration settings
     * @return a NodeViewContext with default dimensions and no special states
     */
    public static NodeViewContext defaults(String nodeId, String nodeType,
            String displayName, Map<String, Object> settings) {
        return new NodeViewContext(nodeId, nodeType, displayName, settings,
                80.0, 80.0, false, false, false);
    }

    /**
     * Creates a copy of this context with the selected state changed.
     * 
     * @param isSelected the new selected state
     * @return a new context with the updated selection state
     */
    public NodeViewContext withSelected(boolean isSelected) {
        return new NodeViewContext(nodeId, nodeType, displayName, settings,
                width, height, isSelected, executing, error);
    }

    /**
     * Creates a copy of this context with the executing state changed.
     * 
     * @param isExecuting the new executing state
     * @return a new context with the updated execution state
     */
    public NodeViewContext withExecuting(boolean isExecuting) {
        return new NodeViewContext(nodeId, nodeType, displayName, settings,
                width, height, selected, isExecuting, error);
    }

    /**
     * Creates a copy of this context with the error state changed.
     * 
     * @param hasError the new error state
     * @return a new context with the updated error state
     */
    public NodeViewContext withError(boolean hasError) {
        return new NodeViewContext(nodeId, nodeType, displayName, settings,
                width, height, selected, executing, hasError);
    }
}
