package ai.nervemind.app.executor.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * JavaScript execution strategy using GraalJS.
 * 
 * <p>
 * Executes JavaScript code in a sandboxed GraalVM context. This is the default
 * script execution strategy and provides fast, secure execution.
 * </p>
 * 
 * <h2>Available Globals</h2>
 * <ul>
 * <li><code>$input</code> / <code>input</code> - Data from previous nodes</li>
 * <li><code>$node</code> / <code>node</code> - Current node parameters</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * // Transform input data
 * const items = $input.items || [];
 * return {
 *   count: items.length,
 *   processed: items.map(x => x.toUpperCase())
 * };
 * }</pre>
 */
@Component
public class JavaScriptExecutionStrategy implements ScriptExecutionStrategy {

    /**
     * Default constructor.
     */
    public JavaScriptExecutionStrategy() {
        // Default constructor
    }

    private static final Logger log = LoggerFactory.getLogger(JavaScriptExecutionStrategy.class);
    private static final String LANGUAGE_ID = "js";

    @Override
    public Map<String, Object> execute(String code, Map<String, Object> input,
            Node node, ExecutionService.ExecutionContext context) throws ScriptExecutionException {

        if (code == null || code.isBlank()) {
            return new HashMap<>(input);
        }

        try (Context polyglotContext = Context.newBuilder(LANGUAGE_ID)
                .allowAllAccess(false)
                .build()) {

            // Bind input data
            Value bindings = polyglotContext.getBindings(LANGUAGE_ID);

            // Convert Java Map to JavaScript object recursively
            Value jsInput = javaToJavaScript(polyglotContext, input);
            bindings.putMember("$input", jsInput);
            bindings.putMember("input", jsInput);

            // Bind node parameters
            Value jsNode = javaToJavaScript(polyglotContext, node.parameters());
            bindings.putMember("$node", jsNode);
            bindings.putMember("node", jsNode);

            // Add console.log support
            bindings.putMember("console", createConsoleObject(polyglotContext, context));

            // Wrap and execute code
            String wrappedCode = wrapCode(code);
            Value result = polyglotContext.eval(LANGUAGE_ID, wrappedCode);

            // Extract result
            Map<String, Object> output = new HashMap<>(input);
            if (result.hasMembers()) {
                for (String key : result.getMemberKeys()) {
                    output.put(key, convertValue(result.getMember(key)));
                }
            } else if (!result.isNull()) {
                output.put("result", convertValue(result));
            }

            return output;

        } catch (PolyglotException e) {
            Integer lineNumber = e.getSourceLocation() != null
                    ? e.getSourceLocation().getStartLine()
                    : null;
            throw new ScriptExecutionException(
                    e.getMessage(), e, "javascript", code, lineNumber);
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    "JavaScript execution failed: " + e.getMessage(), e, "javascript");
        }
    }

    /**
     * Wrap user code in an IIFE to capture return value.
     */
    private String wrapCode(String code) {
        return """
                (function() {
                    %s
                })()
                """.formatted(code);
    }

    /**
     * Convert a Java object to JavaScript equivalent recursively.
     */
    @SuppressWarnings("unchecked")
    private Value javaToJavaScript(Context context, Object javaValue) {
        if (javaValue == null) {
            return context.eval(LANGUAGE_ID, "null");
        }

        if (javaValue instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) javaValue;
            Value jsObject = context.eval(LANGUAGE_ID, "({})");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsObject.putMember(entry.getKey(), javaToJavaScript(context, entry.getValue()));
            }
            return jsObject;
        }

        if (javaValue instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) javaValue;
            Value jsArray = context.eval(LANGUAGE_ID, "[]");
            for (int i = 0; i < list.size(); i++) {
                jsArray.setArrayElement(i, javaToJavaScript(context, list.get(i)));
            }
            return jsArray;
        }

        if (javaValue instanceof Object[] array) {
            Value jsArray = context.eval(LANGUAGE_ID, "[]");
            for (int i = 0; i < array.length; i++) {
                jsArray.setArrayElement(i, javaToJavaScript(context, array[i]));
            }
            return jsArray;
        }

        // Primitives and strings are handled automatically
        return context.asValue(javaValue);
    }

    /**
     * Create a console object for logging.
     * Routes JavaScript console.log/warn/error to the execution logger.
     */
    private Value createConsoleObject(Context context, ExecutionService.ExecutionContext execContext) {
        // Create a helper object that JavaScript can call
        ConsoleHelper helper = new ConsoleHelper(execContext);

        Value bindings = context.getBindings(LANGUAGE_ID);
        bindings.putMember("__consoleHelper", context.asValue(helper));

        return context.eval(LANGUAGE_ID, """
                ({
                    log: function(...args) {
                        __consoleHelper.log(args.map(a => String(a)).join(' '));
                    },
                    warn: function(...args) {
                        __consoleHelper.warn(args.map(a => String(a)).join(' '));
                    },
                    error: function(...args) {
                        __consoleHelper.error(args.map(a => String(a)).join(' '));
                    },
                    info: function(...args) {
                        __consoleHelper.log(args.map(a => String(a)).join(' '));
                    },
                    debug: function(...args) {
                        __consoleHelper.debug(args.map(a => String(a)).join(' '));
                    }
                })
                """);
    }

    /**
     * Helper class to bridge JavaScript console calls to Java logging.
     */
    public static class ConsoleHelper {
        private final ExecutionService.ExecutionContext execContext;
        private final List<String> messages = new ArrayList<>();

        /**
         * Creates a console helper for the given execution context.
         * 
         * @param execContext the execution context
         */
        public ConsoleHelper(ExecutionService.ExecutionContext execContext) {
            this.execContext = execContext;
        }

        /**
         * Log an info message.
         * 
         * @param message the message to log
         */
        @HostAccess.Export
        public void log(String message) {
            messages.add(message);
            logToExecution(ExecutionLogger.LogLevel.INFO, message);
        }

        /**
         * Log a warning message.
         * 
         * @param message the message to log
         */
        @HostAccess.Export
        public void warn(String message) {
            messages.add("[WARN] " + message);
            logToExecution(ExecutionLogger.LogLevel.WARN, message);
        }

        /**
         * Log an error message.
         * 
         * @param message the message to log
         */
        @HostAccess.Export
        public void error(String message) {
            messages.add("[ERROR] " + message);
            logToExecution(ExecutionLogger.LogLevel.ERROR, message);
        }

        /**
         * Log a debug message.
         * 
         * @param message the message to log
         */
        @HostAccess.Export
        public void debug(String message) {
            messages.add("[DEBUG] " + message);
            logToExecution(ExecutionLogger.LogLevel.DEBUG, message);
        }

        private void logToExecution(ExecutionLogger.LogLevel level, String message) {
            ExecutionLogger logger = execContext.getExecutionLogger();
            if (logger != null) {
                logger.custom(
                        String.valueOf(execContext.getExecutionId()),
                        level,
                        "[JS] " + message,
                        Map.of("source", "javascript", "console", true));
            }
            // Also log to SLF4J
            log.debug("[JS Console] {}", message);
        }

        /**
         * Get all logged messages.
         * 
         * @return list of logged messages
         */
        public List<String> getMessages() {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Convert GraalVM Value to Java object.
     */
    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.hasArrayElements()) {
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return java.util.Arrays.asList(array);
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertValue(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }

    @Override
    public String getLanguageId() {
        return "javascript";
    }

    @Override
    public String getDisplayName() {
        return "JavaScript (GraalJS)";
    }

    @Override
    public boolean isAvailable() {
        try (Context context = Context.newBuilder(LANGUAGE_ID).build()) {
            context.eval(LANGUAGE_ID, "1+1");
            return true;
        } catch (Exception e) {
            log.warn("GraalJS not available: {}", e.getMessage());
            return false;
        }
    }
}
