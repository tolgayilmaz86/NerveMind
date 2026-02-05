package ai.nervemind.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Central registry for all available {@link NodeExecutor} implementations.
 *
 * <p>
 * Acts as a lookup service used by the {@link ExecutionService} to find the
 * correct
 * execution logic for any given node type. Supports both built-in executors
 * (auto-wired
 * by Spring) and dynamic executors provided by plugins.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 * <li><strong>Auto-Discovery:</strong> Collects all Spring beans implementing
 * NodeExecutor on startup.</li>
 * <li><strong>Lookup:</strong> efficient O(1) retrieval of executors by type
 * string.</li>
 * <li><strong>Dynamic Registration:</strong> Allows runtime addition/removal of
 * executors (e.g., from plugins).</li>
 * </ul>
 */
@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executors = new HashMap<>();

    /**
     * Creates a new NodeExecutorRegistry with the provided list of executors.
     *
     * @param executorList the list of node executors to register
     */
    public NodeExecutorRegistry(List<NodeExecutor> executorList) {
        for (NodeExecutor executor : executorList) {
            executors.put(executor.getNodeType(), executor);
        }
    }

    /**
     * Retrieves the executor for the specified node type.
     *
     * @param nodeType the type of node to get the executor for
     * @return the executor for the node type
     * @throws IllegalArgumentException if no executor is found for the node type
     */
    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            throw new IllegalArgumentException("No executor found for node type: " + nodeType);
        }
        return executor;
    }

    /**
     * Checks if an executor exists for the specified node type.
     *
     * @param nodeType the type of node to check
     * @return true if an executor exists for the node type, false otherwise
     */
    public boolean hasExecutor(String nodeType) {
        return executors.containsKey(nodeType);
    }

    /**
     * Dynamically register a node executor.
     * Used by plugin system to add custom node types at runtime.
     * 
     * @param executor The executor to register
     * @throws IllegalArgumentException if an executor for this type already exists
     */
    public void registerExecutor(NodeExecutor executor) {
        String nodeType = executor.getNodeType();
        if (executors.containsKey(nodeType)) {
            throw new IllegalArgumentException("Executor already registered for node type: " + nodeType);
        }
        executors.put(nodeType, executor);
    }

    /**
     * Unregister a node executor.
     * Used by plugin system when unloading plugins.
     * 
     * @param nodeType The node type to unregister
     * @return true if executor was removed, false if it didn't exist
     */
    public boolean unregisterExecutor(String nodeType) {
        return executors.remove(nodeType) != null;
    }

    /**
     * Get all registered node types.
     * 
     * @return Set of registered node type identifiers
     */
    public java.util.Set<String> getRegisteredNodeTypes() {
        return java.util.Set.copyOf(executors.keySet());
    }
}
