package ai.nervemind.plugin.api;

/**
 * Provider interface for Action nodes in NerveMind workflows.
 * 
 * <p>
 * Actions perform operations on data as it flows through the workflow. They
 * receive
 * input from upstream nodes, execute their logic, and pass output to downstream
 * nodes.
 * Common actions include HTTP requests, code execution, data transformation,
 * and AI operations.
 * </p>
 * 
 * <h2>Plugin Registration</h2>
 * <p>
 * Implementations must be registered via the Java ServiceLoader mechanism by
 * creating
 * a file at:
 * </p>
 * 
 * <pre>
 * META - INF / services / ai.nervemind.plugin.api.ActionProvider
 * </pre>
 * <p>
 * The file should contain the fully qualified class name of each
 * implementation.
 * </p>
 * 
 * <h2>Implementation Example</h2>
 * 
 * <pre>
 * {@code
 * package com.example.myplugin;
 * 
 * public class SlackNotifyProvider implements ActionProvider {
 *     &#64;Override
 *     public String getNodeType() {
 *         return "com.example.myplugin.slack";
 *     }
 * 
 *     &#64;Override
 *     public String getDisplayName() {
 *         return "Slack Notification";
 *     }
 * 
 *     &#64;Override
 *     public String getDescription() {
 *         return "Send a message to a Slack channel";
 *     }
 * 
 *     &#64;Override
 *     public List<PropertyDefinition> getProperties() {
 *         return List.of(
 *                 PropertyDefinition.requiredString("channel", "Channel", "Slack channel ID"),
 *                 PropertyDefinition.requiredString("message", "Message", "Message to send"));
 *     }
 * 
 *     @Override
 *     public NodeExecutor getExecutor() {
 *         return new SlackNotifyExecutor();
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>Data Flow</h2>
 * <p>
 * Action nodes receive data from the execution context:
 * </p>
 * <ul>
 * <li>{@code context.getInput()} - Data from upstream node(s)</li>
 * <li>{@code context.getNodeSettings()} - This node's configuration</li>
 * <li>Return value becomes input for downstream nodes</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see NodeDescriptor Base interface with common metadata
 * @see NodeExecutor Interface your executor must implement
 * @see TriggerProvider For trigger node implementations
 * @see ExecutionContext The context passed to executors
 */
public interface ActionProvider extends NodeDescriptor {

    /**
     * Gets the executor that performs the action logic.
     * 
     * <p>
     * The executor receives input from upstream nodes and should return
     * a Map containing output data for downstream nodes.
     * </p>
     * 
     * @return the executor instance; must not be {@code null}
     * @see NodeExecutor
     */
    NodeExecutor getExecutor();

    /**
     * Returns the category for this node.
     * 
     * <p>
     * For action providers, this always returns {@link NodeCategory#ACTION}.
     * </p>
     * 
     * @return {@link NodeCategory#ACTION}
     */
    @Override
    default NodeCategory getCategory() {
        return NodeCategory.ACTION;
    }
}
