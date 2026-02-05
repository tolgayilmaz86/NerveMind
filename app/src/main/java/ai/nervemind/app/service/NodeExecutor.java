package ai.nervemind.app.service;

import java.util.Map;

import ai.nervemind.common.domain.Node;

/**
 * Core interface for defining workflow node execution logic.
 * 
 * <p>
 * Each node type in a workflow (e.g., "httpRequest", "if", "code") must have a
 * corresponding implementation of this interface registered in the
 * {@link NodeExecutorRegistry}.
 * The execution engine invokes {@link #execute} when the workflow flow reaches
 * the node.
 * </p>
 *
 * <h2>Contract</h2>
 * <ol>
 * <li><strong>Input:</strong> Receives execution context and aggregated data
 * from previous nodes.</li>
 * <li><strong>Output:</strong> Returns a Map of data that becomes the input for
 * subsequent nodes.</li>
 * <li><strong>State:</strong> Implementations should be stateless regarding
 * workflow instance data.
 * Use the {@code context} to access shared state if absolutely necessary.</li>
 * <li><strong>Errors:</strong> Should throw
 * {@link ai.nervemind.common.exception.NodeExecutionException}
 * for anticipated failures to allow the engine to handle retries or error
 * flows.</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations must be thread-safe as a single instance is shared across all
 * executing workflows. Avoid mutable instance fields.
 * </p>
 */
public interface NodeExecutor {

    /**
     * Executes the business logic for this node type.
     *
     * @param node    The node definition containing parameters and configuration.
     *                Use {@link Node#parameters()} to access user settings.
     * @param input   Combined output from all upstream nodes that connected to this
     *                node.
     *                For simple flows, this contains the direct predecessor's
     *                output.
     *                For merge nodes, it contains combined data.
     * @param context Verification context providing access to workflow-scoped
     *                services,
     *                logger, and execution metadata.
     * @return A Map containing the results of this node's execution.
     *         Keys in this map become available variables for downstream nodes.
     *         <br>
     *         <strong>Note:</strong> Returning {@code null} is treated as an empty
     *         map.
     * @throws RuntimeException If execution fails. The engine will catch this
     *                          and determine whether to retry or fail the workflow
     *                          based on policy.
     */
    Map<String, Object> execute(Node node, Map<String, Object> input, ExecutionService.ExecutionContext context);

    /**
     * Unique identifier for the node type this executor handles.
     * This must match the 'type' field in the JSON definition of the node.
     * 
     * @return The unique type string (e.g., "httpRequest", "llmChat").
     */
    String getNodeType();
}
