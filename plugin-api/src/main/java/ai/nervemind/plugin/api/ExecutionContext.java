package ai.nervemind.plugin.api;

import java.util.Map;
import java.util.Optional;

/**
 * Context passed to node executors during workflow execution.
 * 
 * <p>
 * The execution context provides everything a node executor needs to perform
 * its operation: configuration settings, input data from upstream nodes,
 * logging
 * utilities, and access to application services.
 * </p>
 * 
 * <h2>Context Contents</h2>
 * <ul>
 * <li><strong>Identifiers</strong> - Execution ID and workflow ID for
 * tracking</li>
 * <li><strong>Settings</strong> - The node's configuration from the properties
 * panel</li>
 * <li><strong>Input</strong> - Output data from the upstream node(s)</li>
 * <li><strong>Logging</strong> - Methods to log to the execution console</li>
 * <li><strong>Services</strong> - Access to application services if needed</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * public Map<String, Object> execute(ExecutionContext context) throws NodeExecutionException {
 *     // Get node configuration
 *     String targetPath = (String) context.getNodeSettings().get("path");
 * 
 *     // Get input from upstream node
 *     String fileContent = (String) context.getInput().get("content");
 * 
 *     // Log progress (appears in execution console)
 *     context.logInfo("Writing to: " + targetPath);
 * 
 *     try {
 *         Files.writeString(Path.of(targetPath), fileContent);
 *         context.logInfo("File written successfully");
 *         return Map.of("success", true, "path", targetPath);
 *     } catch (IOException e) {
 *         context.logError("Write failed", e);
 *         throw new NodeExecutionException("Failed to write file", e);
 *     }
 * }
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see NodeExecutor Where this context is used
 */
public interface ExecutionContext {

    /**
     * Gets the unique identifier for this execution run.
     * 
     * <p>
     * Each workflow execution has a unique ID that can be used for tracking,
     * logging, and debugging purposes.
     * </p>
     * 
     * @return the execution ID string (typically a UUID)
     */
    String getExecutionId();

    /**
     * Gets the ID of the workflow being executed.
     * 
     * @return the workflow's database ID
     */
    Long getWorkflowId();

    /**
     * Gets the current node's configuration settings.
     * 
     * <p>
     * These are the values entered by the user in the node's properties panel.
     * Keys correspond to {@link PropertyDefinition#name()} values.
     * </p>
     * 
     * <p>
     * Example:
     * </p>
     * 
     * <pre>{@code
     * String url = (String) context.getNodeSettings().get("url");
     * int timeout = (Integer) context.getNodeSettings().getOrDefault("timeout", 30);
     * }</pre>
     * 
     * @return a Map of setting name to value; never {@code null}
     */
    Map<String, Object> getNodeSettings();

    /**
     * Gets input data from upstream node(s).
     * 
     * <p>
     * This contains the output from the previous node in the workflow.
     * For trigger nodes, this may be empty or contain trigger-specific data.
     * </p>
     * 
     * <p>
     * The structure depends on what the upstream node returned from its
     * {@link NodeExecutor#execute(ExecutionContext)} method.
     * </p>
     * 
     * @return a Map of input data; never {@code null}, may be empty
     */
    Map<String, Object> getInput();

    /**
     * Logs an informational message to the execution console.
     * 
     * <p>
     * Use this for progress updates and general information that helps
     * users understand what the node is doing.
     * </p>
     * 
     * @param message the message to log
     */
    void logInfo(String message);

    /**
     * Logs a debug message to the execution console.
     * 
     * <p>
     * Use this for detailed technical information useful for troubleshooting.
     * Debug messages may be hidden by default depending on user settings.
     * </p>
     * 
     * @param message the debug message to log
     */
    void logDebug(String message);

    /**
     * Logs an error message to the execution console.
     * 
     * <p>
     * Use this when an error occurs but execution can continue, or when you
     * want to log error details before throwing a {@link NodeExecutionException}.
     * </p>
     * 
     * @param message the error message
     * @param error   the exception that caused the error (may be {@code null})
     */
    void logError(String message, Throwable error);

    /**
     * Gets a service from the application context.
     * 
     * <p>
     * This allows plugins to access core NerveMind services when needed.
     * Use sparingly - most nodes should only need settings and input data.
     * </p>
     * 
     * <p>
     * Example services that might be available:
     * </p>
     * <ul>
     * <li>CredentialService - for accessing stored credentials</li>
     * <li>VariableService - for workflow variables</li>
     * </ul>
     * 
     * @param <T>          the service type
     * @param serviceClass the class of the service to retrieve
     * @return an Optional containing the service if available, empty otherwise
     */
    <T> Optional<T> getService(Class<T> serviceClass);
}
