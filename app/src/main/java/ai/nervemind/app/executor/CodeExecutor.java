package ai.nervemind.app.executor;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.executor.script.ScriptExecutionException;
import ai.nervemind.app.executor.script.ScriptExecutionStrategy;
import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.app.service.SettingsService;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "code" node type - executes dynamic JavaScript or Python
 * code.
 *
 * <p>
 * This executor enables custom logic within workflows by running user-defined
 * code snippets using GraalVM Polyglot for multi-language support. It provides
 * a sandboxed execution environment with access to workflow data.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Code node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>String</td>
 * <td>""</td>
 * <td>The code to execute</td>
 * </tr>
 * <tr>
 * <td>language</td>
 * <td>String</td>
 * <td>"js"</td>
 * <td>Language: "js", "javascript", "py", or "python"</td>
 * </tr>
 * </table>
 *
 * <h2>Available Bindings</h2>
 * <p>
 * The following variables are available within the code:
 * </p>
 * <ul>
 * <li><strong>$input</strong> / <strong>input</strong> - Object containing data
 * from previous nodes</li>
 * <li><strong>$node</strong> / <strong>node</strong> - Object containing node
 * parameters</li>
 * </ul>
 *
 * <h2>Output Handling</h2>
 * <ul>
 * <li>If the code returns an object with members, each member is added to
 * output</li>
 * <li>If the code returns a non-null primitive, it's stored under key
 * "result"</li>
 * <li>All input data is preserved in output (merged with returned values)</li>
 * </ul>
 *
 * <h2>Example Usage (JavaScript)</h2>
 * 
 * <pre>{@code
 * // Access input data
 * const data = $input.previousNodeOutput;
 *
 * // Transform data
 * return {
 *   processed: data.map(item => item.toUpperCase()),
 *   count: data.length,
 *   timestamp: new Date().toISOString()
 * };
 * }</pre>
 *
 * <h2>Example Usage (Python)</h2>
 * 
 * <pre>{@code
 * # Access input data
 * data = input.get('previousNodeOutput', [])
 *
 * # Transform data
 * return {
 *   'processed': [item.upper() for item in data],
 *   'count': len(data),
 *   'timestamp': __import__('datetime').datetime.now().isoformat()
 * }
 * }</pre>
 *
 * <h2>Security Notes</h2>
 * <p>
 * Code execution is sandboxed with {@code allowAllAccess(false)} to prevent
 * access to the host system. However, be cautious with user-provided code.
 * </p>
 *
 * @see NodeExecutor
 * @see ScriptExecutionStrategy
 */
@Component
public class CodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutor.class);
    private static final String DEFAULT_LANGUAGE = "javascript";

    private final Map<String, ScriptExecutionStrategy> strategies;
    private final SettingsService settingsService;

    /**
     * Create a new CodeExecutor with available script execution strategies.
     *
     * @param strategyList    list of available strategies (auto-injected by Spring)
     * @param settingsService settings service for Python mode configuration
     */
    public CodeExecutor(List<ScriptExecutionStrategy> strategyList, SettingsService settingsService) {
        this.settingsService = settingsService;
        this.strategies = new java.util.HashMap<>();
        for (ScriptExecutionStrategy strategy : strategyList) {
            strategies.put(strategy.getLanguageId(), strategy);
            log.info("Registered script execution strategy: {} ({})",
                    strategy.getLanguageId(), strategy.getDisplayName());
        }
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {

        // Debug: log input data
        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG, "CodeExecutor input keys: " + input.keySet(), Map.of());

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String valueStr = entry.getValue() != null
                    ? entry.getValue().toString().substring(0, Math.min(100, entry.getValue().toString().length()))
                    : "null";
            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.TRACE, "CodeExecutor input[" + entry.getKey() + "] = " + valueStr,
                    Map.of());
        }

        String code = (String) node.parameters().getOrDefault("code", "");
        String language = normalizeLanguage((String) node.parameters().getOrDefault("language", DEFAULT_LANGUAGE));

        if (code.isBlank()) {
            return input;
        }

        // Find the appropriate strategy
        ScriptExecutionStrategy strategy = strategies.get(language);
        if (strategy == null) {
            throw new RuntimeException("Unsupported script language: " + language
                    + ". Available: " + strategies.keySet());
        }

        if (!strategy.isAvailable()) {
            throw new RuntimeException("Script execution strategy not available: "
                    + strategy.getDisplayName() + ". " + strategy.getAvailabilityInfo());
        }

        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG,
                "Executing " + language + " code using " + strategy.getDisplayName(),
                Map.of());

        try {
            return strategy.execute(code, input, node, context);
        } catch (ScriptExecutionException e) {
            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.ERROR,
                    e.getDetailedMessage(),
                    Map.of());
            throw new RuntimeException(e.getDetailedMessage(), e);
        }
    }

    /**
     * Normalize language identifier to canonical form.
     * For Python, respects the execution mode setting (embedded/external/auto).
     *
     * @param language the input language string
     * @return normalized language ID that maps to a strategy
     */
    private String normalizeLanguage(String language) {
        if (language == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = switch (language.toLowerCase()) {
            case "js", "javascript" -> "javascript";
            case "py", "python" -> resolvePythonStrategy();
            default -> language.toLowerCase();
        };
        log.debug("Normalized language '{}' to strategy '{}'", language, normalized);
        return normalized;
    }

    /**
     * Resolve which Python strategy to use based on settings.
     * 
     * @return the strategy language ID to use for Python
     */
    private String resolvePythonStrategy() {
        String mode = settingsService.getValue(SettingsDefaults.PYTHON_EXECUTION_MODE, "embedded");

        switch (mode.toLowerCase()) {
            case "external":
                // User explicitly wants external Python
                ScriptExecutionStrategy external = strategies.get("python-external");
                if (external != null && external.isAvailable()) {
                    return "python-external";
                }
                log.warn("External Python requested but not available, falling back to embedded");
                return "python";

            case "auto":
                // Try external first (for full pip support), fall back to embedded
                ScriptExecutionStrategy autoExternal = strategies.get("python-external");
                if (autoExternal != null && autoExternal.isAvailable()) {
                    return "python-external";
                }
                // Fall back to embedded
                ScriptExecutionStrategy embedded = strategies.get("python");
                if (embedded != null && embedded.isAvailable()) {
                    return "python";
                }
                // Neither available
                log.error("No Python execution strategy available!");
                return "python"; // Will fail with clear error message

            case "embedded":
            default:
                // Use embedded GraalPy (default)
                return "python";
        }
    }

    /**
     * Get available script languages.
     *
     * @return map of language ID to display name
     */
    public Map<String, String> getAvailableLanguages() {
        Map<String, String> available = new java.util.LinkedHashMap<>();
        for (ScriptExecutionStrategy strategy : strategies.values()) {
            if (strategy.isAvailable()) {
                available.put(strategy.getLanguageId(), strategy.getDisplayName());
            }
        }
        return available;
    }

    @Override
    public String getNodeType() {
        return "code";
    }
}
