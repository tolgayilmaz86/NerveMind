package ai.nervemind.common.domain;

/**
 * Represents a directional edge between two nodes in the workflow graph.
 *
 * <p>
 * Defines how control flow and data propagate from a {@code sourceNode} to a
 * {@code targetNode}. Connections map specific output handles to input handles,
 * allowing for complex routing logic (e.g., "true"/"false" branches).
 * </p>
 *
 * @param id           Unique identifier for the connection
 * @param sourceNodeId ID of the origin node
 * @param sourceOutput Specific output handle (default: "main")
 * @param targetNodeId ID of the destination node
 * @param targetInput  Specific input handle (default: "main")
 */
public record Connection(
        String id,
        String sourceNodeId,
        String sourceOutput,
        String targetNodeId,
        String targetInput) {
    /**
     * Compact constructor with validation.
     */
    public Connection {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Connection id cannot be null or blank");
        }
        if (sourceNodeId == null || sourceNodeId.isBlank()) {
            throw new IllegalArgumentException("Source node id cannot be null or blank");
        }
        if (targetNodeId == null || targetNodeId.isBlank()) {
            throw new IllegalArgumentException("Target node id cannot be null or blank");
        }
        if (sourceOutput == null || sourceOutput.isBlank()) {
            sourceOutput = "main";
        }
        if (targetInput == null || targetInput.isBlank()) {
            targetInput = "main";
        }
        if (sourceNodeId.equals(targetNodeId)) {
            throw new IllegalArgumentException("Cannot connect a node to itself");
        }
    }

    /**
     * Factory method for creating a simple main-to-main connection.
     * 
     * @param id           the connection ID
     * @param sourceNodeId the source node ID
     * @param targetNodeId the target node ID
     * @return a new Connection instance
     */
    public static Connection simple(String id, String sourceNodeId, String targetNodeId) {
        return new Connection(id, sourceNodeId, "main", targetNodeId, "main");
    }

    /**
     * Check if this connection involves the given node.
     * 
     * @param nodeId the node ID to check
     * @return true if the node is either source or target
     */
    public boolean involvesNode(String nodeId) {
        return sourceNodeId.equals(nodeId) || targetNodeId.equals(nodeId);
    }
}
