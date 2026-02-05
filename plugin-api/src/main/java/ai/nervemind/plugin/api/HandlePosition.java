package ai.nervemind.plugin.api;

/**
 * Defines the position where a connection handle can be placed on a node.
 * 
 * <p>
 * Connection handles are the points where edges connect to nodes. Standard
 * nodes
 * have an input handle on the left and an output handle on the right, but
 * plugins
 * can define custom handle positions for specialized layouts.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Standard node with left input, right output
 * List.of(
 *         HandleDefinition.input("in", HandlePosition.LEFT),
 *         HandleDefinition.output("out", HandlePosition.RIGHT));
 * 
 * // Conditional node with multiple outputs
 * List.of(
 *         HandleDefinition.input("in", HandlePosition.LEFT),
 *         HandleDefinition.output("true", HandlePosition.RIGHT),
 *         HandleDefinition.output("false", HandlePosition.BOTTOM));
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see HandleDefinition For defining connection handles
 */
public enum HandlePosition {

    /**
     * Handle positioned on the left side of the node.
     * Typically used for input connections in left-to-right workflows.
     */
    LEFT,

    /**
     * Handle positioned on the right side of the node.
     * Typically used for output connections in left-to-right workflows.
     */
    RIGHT,

    /**
     * Handle positioned on the top of the node.
     * Useful for top-down workflow layouts or special node types.
     */
    TOP,

    /**
     * Handle positioned on the bottom of the node.
     * Useful for bottom-up layouts or branching outputs (e.g., conditional nodes).
     */
    BOTTOM
}
