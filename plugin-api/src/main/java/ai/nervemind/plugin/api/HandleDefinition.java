package ai.nervemind.plugin.api;

/**
 * Defines a connection handle on a workflow node.
 * 
 * <p>
 * Handles are the points where connections (edges) attach to nodes. Each handle
 * has
 * a unique ID, a type (input or output), a position on the node, and an
 * optional label.
 * Plugins can define custom handles to create nodes with multiple
 * inputs/outputs or
 * non-standard connection layouts.
 * </p>
 * 
 * <h2>Standard vs Custom Handles</h2>
 * <p>
 * Most nodes use the default configuration: one input handle on the left, one
 * output
 * handle on the right. However, certain node types benefit from custom handles:
 * </p>
 * <ul>
 * <li><strong>Conditional nodes:</strong> Multiple output handles for different
 * branches</li>
 * <li><strong>Merge nodes:</strong> Multiple input handles to combine data</li>
 * <li><strong>Transform nodes:</strong> Separate handles for success/error
 * paths</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Define handles for a conditional (if/else) node
 * List<HandleDefinition> handles = List.of(
 *         HandleDefinition.input("condition", HandlePosition.LEFT),
 *         HandleDefinition.output("then", HandlePosition.RIGHT, "Then"),
 *         HandleDefinition.output("else", HandlePosition.BOTTOM, "Else"));
 * }</pre>
 * 
 * @param id       Unique identifier for this handle within the node (e.g.,
 *                 "in", "out", "error")
 * @param type     Whether this is an input or output handle
 * @param position The side of the node where this handle is placed
 * @param label    Optional display label shown near the handle (null for no
 *                 label)
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see HandlePosition For available positions
 * @see HandleType For input/output types
 */
public record HandleDefinition(
        String id,
        HandleType type,
        HandlePosition position,
        String label) {

    /**
     * Creates an input handle at the specified position.
     * 
     * @param id       unique identifier for the handle
     * @param position where to place the handle on the node
     * @return a new HandleDefinition configured as an input
     */
    public static HandleDefinition input(String id, HandlePosition position) {
        return new HandleDefinition(id, HandleType.INPUT, position, null);
    }

    /**
     * Creates an input handle with a label.
     * 
     * @param id       unique identifier for the handle
     * @param position where to place the handle on the node
     * @param label    display label for the handle
     * @return a new HandleDefinition configured as an input with label
     */
    public static HandleDefinition input(String id, HandlePosition position, String label) {
        return new HandleDefinition(id, HandleType.INPUT, position, label);
    }

    /**
     * Creates an output handle at the specified position.
     * 
     * @param id       unique identifier for the handle
     * @param position where to place the handle on the node
     * @return a new HandleDefinition configured as an output
     */
    public static HandleDefinition output(String id, HandlePosition position) {
        return new HandleDefinition(id, HandleType.OUTPUT, position, null);
    }

    /**
     * Creates an output handle with a label.
     * 
     * @param id       unique identifier for the handle
     * @param position where to place the handle on the node
     * @param label    display label for the handle
     * @return a new HandleDefinition configured as an output with label
     */
    public static HandleDefinition output(String id, HandlePosition position, String label) {
        return new HandleDefinition(id, HandleType.OUTPUT, position, label);
    }

    /**
     * Checks if this handle can accept incoming connections.
     * 
     * @return true if this is an input handle
     */
    public boolean isInput() {
        return type == HandleType.INPUT;
    }

    /**
     * Checks if this handle produces outgoing connections.
     * 
     * @return true if this is an output handle
     */
    public boolean isOutput() {
        return type == HandleType.OUTPUT;
    }
}
