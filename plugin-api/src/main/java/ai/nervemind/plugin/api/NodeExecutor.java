package ai.nervemind.plugin.api;

import java.util.Map;

/**
 * Interface for executing the logic of a workflow node.
 * 
 * <p>
 * This is the core execution interface that plugin developers must implement
 * to define what a node does when it runs. Each node type (trigger or action)
 * must provide an executor that implements this interface.
 * </p>
 * 
 * <h2>Execution Flow</h2>
 * <ol>
 * <li>NerveMind calls {@link #validate(Map)} before workflow execution</li>
 * <li>If validation passes, {@link #execute(ExecutionContext)} is called</li>
 * <li>The returned Map becomes input for downstream nodes</li>
 * <li>If execution is cancelled, {@link #cancel()} is called</li>
 * </ol>
 * 
 * <h2>Implementation Example</h2>
 * 
 * <pre>
 * public class HttpRequestExecutor implements NodeExecutor {
 *     public Map&lt;String, Object&gt; execute(ExecutionContext context)
 *             throws NodeExecutionException {
 *         String url = (String) context.getNodeSettings().get("url");
 *         context.logInfo("Making request to " + url);
 * 
 *         try {
 *             String responseBody = httpClient.request(url);
 *             return Map.of("statusCode", 200, "body", responseBody);
 *         } catch (Exception e) {
 *             throw new NodeExecutionException("Request failed", e);
 *         }
 *     }
 * 
 *     public ValidationResult validate(Map&lt;String, Object&gt; settings) {
 *         String url = (String) settings.get("url");
 *         if (url == null || url.isBlank()) {
 *             return ValidationResult.invalid("URL is required");
 *         }
 *         return ValidationResult.valid();
 *     }
 * }
 * </pre>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 * <li>Throw {@link NodeExecutionException} for expected failures (e.g., HTTP
 * 404)</li>
 * <li>Unexpected exceptions will be caught and logged by the runtime</li>
 * <li>Use {@code context.logError()} to record error details</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * Executors may be called from multiple threads. Ensure your implementation
 * is thread-safe, especially when accessing shared resources.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ExecutionContext The context passed to execute()
 * @see NodeExecutionException For signaling execution failures
 * @see ValidationResult For returning validation errors
 */
public interface NodeExecutor {

    /**
     * Executes the node logic.
     * 
     * <p>
     * This method is called by the workflow runtime when the node is executed.
     * It receives the execution context containing configuration and input data,
     * and should return output data for downstream nodes.
     * </p>
     * 
     * @param context the execution context providing access to settings, input,
     *                logging utilities, and services
     * @return a Map containing output data; keys become accessible to downstream
     *         nodes via their input context; may be empty but never {@code null}
     * @throws NodeExecutionException if execution fails; include a descriptive
     *                                message and optionally wrap the underlying
     *                                cause
     */
    Map<String, Object> execute(ExecutionContext context) throws NodeExecutionException;

    /**
     * Validates the node configuration before execution.
     * 
     * <p>
     * This method is called during workflow validation (before execution starts).
     * Implementors should check that all required settings are present and valid.
     * </p>
     * 
     * <p>
     * Common validations include:
     * </p>
     * <ul>
     * <li>Required fields are not null/empty</li>
     * <li>URLs are well-formed</li>
     * <li>Numeric values are in expected ranges</li>
     * <li>Credentials exist when required</li>
     * </ul>
     * 
     * @param settings the node's configuration settings from the properties panel
     * @return a ValidationResult indicating success or containing error messages
     * @see ValidationResult#valid() For successful validation
     * @see ValidationResult#invalid(String) For validation failures
     */
    default ValidationResult validate(Map<String, Object> settings) {
        return ValidationResult.valid();
    }

    /**
     * Called when execution is cancelled by the user or system.
     * 
     * <p>
     * Implementations should clean up any resources (close connections,
     * cancel pending requests, etc.) and return promptly. This method may
     * be called from a different thread than {@link #execute(ExecutionContext)}.
     * </p>
     * 
     * <p>
     * Default implementation does nothing.
     * </p>
     */
    default void cancel() {
        // Default: no-op
    }
}
