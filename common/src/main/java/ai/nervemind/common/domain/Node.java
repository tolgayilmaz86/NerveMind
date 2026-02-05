package ai.nervemind.common.domain;

import java.util.Map;

/**
 * Immutable representation of a single unit of work within a Workflow.
 *
 * <p>
 * A Node acts as a container for configuration {@link #parameters()} and
 * metadata
 * required to execute a specific task. Nodes are connected via
 * {@link Connection} objects
 * to form the workflow graph.
 * </p>
 *
 * <h2>Key Characteristics</h2>
 * <ul>
 * <li>**Immutable:** Implemented as a Java Record. To modify, use the
 * {@code withX()} helper methods.</li>
 * <li>**Identifiable:** Uniquely identified by a UUID string
 * {@link #id()}.</li>
 * <li>**Typed:** The {@link #type()} determines which
 * NodeExecutor (in app module)
 * handles its execution.</li>
 * </ul>
 *
 * @param id           Unique identifier within the workflow (UUID string,
 *                     required)
 * @param type         Node type identifier (e.g., "httpRequest", "code", "if")
 * @param name         User-defined display name (defaults to type if null)
 * @param position     Visual coordinates on the editor canvas
 * @param parameters   Configuration map passed to the NodeExecutor (never null)
 * @param credentialId Optional reference to a stored credential for
 *                     authentication
 * @param disabled     If true, the execution engine skips this node (and
 *                     potentially its children)
 * @param notes        Markdown-formatted user documentation for the node
 */
public record Node(
        String id,
        String type,
        String name,
        Position position,
        Map<String, Object> parameters,
        Long credentialId,
        boolean disabled,
        String notes) {
    /**
     * Canvas position record.
     * 
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public record Position(double x, double y) {
        /**
         * Compact constructor with validation.
         */
        public Position {
            // Validate coordinates are reasonable
            if (x < -10000 || x > 10000 || y < -10000 || y > 10000) {
                throw new IllegalArgumentException("Position coordinates out of bounds");
            }
        }
    }

    /**
     * Compact constructor with validation.
     */
    public Node {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id cannot be null or blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Node type cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            name = type; // Default to type if no name provided
        }
        if (position == null) {
            position = new Position(0, 0);
        }
        if (parameters == null) {
            parameters = Map.of();
        }
    }

    /**
     * Create a new node with updated position.
     * 
     * @param newPosition the new position
     * @return a new Node instance
     */
    public Node withPosition(Position newPosition) {
        return new Node(id, type, name, newPosition, parameters, credentialId, disabled, notes);
    }

    /**
     * Create a new node with updated parameters.
     * 
     * @param newParameters the new parameters map
     * @return a new Node instance
     */
    public Node withParameters(Map<String, Object> newParameters) {
        return new Node(id, type, name, position, newParameters, credentialId, disabled, notes);
    }

    /**
     * Create a new node with updated disabled state.
     * 
     * @param newDisabled the new disabled state
     * @return a new Node instance
     */
    public Node withDisabled(boolean newDisabled) {
        return new Node(id, type, name, position, parameters, credentialId, newDisabled, notes);
    }
}
