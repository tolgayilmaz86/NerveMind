package ai.nervemind.common.domain;

import java.util.List;
import java.util.Map;

/**
 * The root aggregate representing a fully defined executable workflow.
 *
 * <p>
 * A Workflow is a container for a Directed Acyclic Graph (DAG) of {@link Node}s
 * connected
 * by {@link Connection}s. It captures the entire logic of an automated process,
 * including
 * configuration settings used by the execution engine.
 * </p>
 *
 * <h2>Structure</h2>
 * <ul>
 * <li><strong>Nodes:</strong> The operational steps (actions/triggers).</li>
 * <li><strong>Connections:</strong> The flow logic defining data and control
 * transfer.</li>
 * <li><strong>Settings:</strong> Global configuration (timeouts, retry
 * policies, environment vars).</li>
 * </ul>
 *
 * @param id          Unique identifier (UUID string)
 * @param name        User-friendly display name
 * @param description Optional description or documentation for the workflow
 * @param nodes       List of all nodes in this workflow
 * @param connections List of all connections defining the graph edges
 * @param settings    Workflow-level configuration for the execution engine
 */
public record Workflow(
        String id,
        String name,
        String description,
        List<Node> nodes,
        List<Connection> connections,
        Map<String, Object> settings) {

    /**
     * Create an empty workflow with the given name.
     * 
     * @param name the name of the workflow
     * @return a new empty Workflow instance
     */
    public static Workflow empty(String name) {
        return new Workflow(null, name, "", List.of(), List.of(), Map.of());
    }

    /**
     * Find a node by ID.
     *
     * @param nodeId The node ID to search for
     * @return The node, or null if not found
     */
    public Node findNode(String nodeId) {
        if (nodes == null || nodeId == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> nodeId.equals(n.id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all connections originating from a node.
     *
     * @param nodeId The source node ID
     * @return List of outgoing connections
     */
    public List<Connection> getOutgoingConnections(String nodeId) {
        if (connections == null || nodeId == null) {
            return List.of();
        }
        return connections.stream()
                .filter(c -> nodeId.equals(c.sourceNodeId()))
                .toList();
    }

    /**
     * Get all connections targeting a node.
     *
     * @param nodeId The target node ID
     * @return List of incoming connections
     */
    public List<Connection> getIncomingConnections(String nodeId) {
        if (connections == null || nodeId == null) {
            return List.of();
        }
        return connections.stream()
                .filter(c -> nodeId.equals(c.targetNodeId()))
                .toList();
    }

    /**
     * Find trigger nodes (nodes with no incoming connections).
     *
     * @return List of trigger/start nodes
     */
    public List<Node> getTriggerNodes() {
        if (nodes == null) {
            return List.of();
        }
        return nodes.stream()
                .filter(node -> getIncomingConnections(node.id()).isEmpty())
                .toList();
    }
}
