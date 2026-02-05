package ai.nervemind.app.executor.script;

import java.util.Map;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Strategy interface for script execution.
 * 
 * <p>
 * Implementations provide different ways to execute scripts:
 * </p>
 * <ul>
 * <li>{@link JavaScriptExecutionStrategy} - GraalJS JavaScript engine</li>
 * <li>{@link EmbeddedPythonExecutionStrategy} - GraalPy embedded Python</li>
 * <li>{@link ExternalPythonExecutionStrategy} - External Python subprocess
 * (future)</li>
 * </ul>
 * 
 * @see ai.nervemind.app.executor.CodeExecutor
 */
public interface ScriptExecutionStrategy {

    /**
     * Execute a script with the given input data.
     *
     * @param code    the script code to execute
     * @param input   input data from previous nodes
     * @param node    the node being executed (for accessing parameters)
     * @param context the execution context
     * @return output data including script results merged with input
     * @throws ScriptExecutionException if execution fails
     */
    Map<String, Object> execute(String code, Map<String, Object> input,
            Node node, ExecutionService.ExecutionContext context) throws ScriptExecutionException;

    /**
     * Get the language identifier this strategy handles.
     *
     * @return the language ID (e.g., "javascript", "python")
     */
    String getLanguageId();

    /**
     * Get a human-readable name for display.
     *
     * @return display name (e.g., "JavaScript (GraalJS)", "Python (GraalPy)")
     */
    String getDisplayName();

    /**
     * Check if this strategy is available (dependencies installed, etc.).
     *
     * @return true if the strategy can be used
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Get information about why this strategy might not be available.
     *
     * @return diagnostic message, or empty string if available
     */
    default String getAvailabilityInfo() {
        return isAvailable() ? "" : "Strategy not available";
    }
}
