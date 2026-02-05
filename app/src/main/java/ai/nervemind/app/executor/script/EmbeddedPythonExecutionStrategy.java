package ai.nervemind.app.executor.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Embedded Python execution strategy using GraalPy.
 * 
 * <p>
 * Executes Python code using GraalVM's Python implementation (GraalPy). This
 * provides zero-install Python execution with a sandboxed environment.
 * </p>
 * 
 * <h2>Features</h2>
 * <ul>
 * <li>No Python installation required</li>
 * <li>Sandboxed execution (no file system or network access by default)</li>
 * <li>Fast startup compared to subprocess</li>
 * <li>Access to Python standard library subset</li>
 * </ul>
 * 
 * <h2>Limitations</h2>
 * <ul>
 * <li>Not all Python packages available (no pip)</li>
 * <li>Some C extension modules not supported</li>
 * <li>Slight compatibility differences from CPython</li>
 * </ul>
 * 
 * <h2>Available Globals</h2>
 * <ul>
 * <li><code>input</code> / <code>$input</code> - Data from previous nodes
 * (dict)</li>
 * <li><code>node</code> / <code>$node</code> - Current node parameters
 * (dict)</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * # Transform input data
 * items = input.get('items', [])
 * result = {
 *     'count': len(items),
 *     'processed': [x.upper() for x in items]
 * }
 * return result
 * }</pre>
 */
@Component
public class EmbeddedPythonExecutionStrategy implements ScriptExecutionStrategy {

    /**
     * Default constructor for EmbeddedPythonExecutionStrategy.
     */
    public EmbeddedPythonExecutionStrategy() {
        // Required for Spring component scanning
    }

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPythonExecutionStrategy.class);
    private static final String LANGUAGE_ID = "python";

    // Cache availability check result
    private Boolean available = null;
    private String availabilityMessage = "";

    @Override
    public Map<String, Object> execute(String code, Map<String, Object> input,
            Node node, ExecutionService.ExecutionContext context) throws ScriptExecutionException {

        if (code == null || code.isBlank()) {
            return new HashMap<>(input);
        }

        try (Context polyglotContext = Context.newBuilder(LANGUAGE_ID)
                .allowAllAccess(false)
                .allowExperimentalOptions(true)
                .option("python.ForceImportSite", "false") // Faster startup
                .option("python.WarnOptions", "") // Suppress warnings
                .build()) {

            Value bindings = polyglotContext.getBindings(LANGUAGE_ID);

            // Convert Java Map to Python dict
            Value pyInput = javaToPython(polyglotContext, input);
            bindings.putMember("input", pyInput);
            bindings.putMember("$input", pyInput);

            // Convert node parameters to Python dict
            Value pyNode = javaToPython(polyglotContext, node.parameters());
            bindings.putMember("node", pyNode);
            bindings.putMember("$node", pyNode);

            // Wrap code to capture return value
            String wrappedCode = wrapCode(code);

            log.debug("Executing Python code:\n{}", wrappedCode);
            polyglotContext.eval(LANGUAGE_ID, wrappedCode);

            // Get the result
            Value resultValue = bindings.getMember("__workflow_result__");

            // Extract result
            Map<String, Object> output = new HashMap<>(input);

            if (resultValue != null && !resultValue.isNull()) {
                Object result = pythonToJava(resultValue);
                if (result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    output.putAll(resultMap);
                } else {
                    output.put("result", result);
                }
            }

            return output;

        } catch (PolyglotException e) {
            Integer lineNumber = extractLineNumber(e);
            String message = extractPythonError(e);
            throw new ScriptExecutionException(message, e, LANGUAGE_ID, code, lineNumber);
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    "Python execution failed: " + e.getMessage(), e, LANGUAGE_ID);
        }
    }

    /**
     * Wrap user code to capture return value.
     * 
     * <p>
     * The wrapper defines a function, executes the user code inside it,
     * and stores the return value in a global variable.
     * </p>
     */
    private String wrapCode(String code) {
        // Indent user code for the function body
        String indentedCode = code.lines()
                .map(line -> "    " + line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        return """
                def __workflow_main__():
                %s

                __workflow_result__ = __workflow_main__()
                """.formatted(indentedCode);
    }

    /**
     * Convert a Java object to Python equivalent.
     */
    private Value javaToPython(Context context, Object javaValue) {
        if (javaValue == null) {
            return context.eval(LANGUAGE_ID, "None");
        }

        if (javaValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) javaValue;
            Value pyDict = context.eval(LANGUAGE_ID, "{}");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                // Use Python's __setitem__ method instead of putMember
                // putMember doesn't work for adding new keys to a Python dict
                pyDict.invokeMember("__setitem__", entry.getKey(), javaToPython(context, entry.getValue()));
            }
            return pyDict;
        }

        if (javaValue instanceof List) {
            List<?> list = (List<?>) javaValue;
            Value pyList = context.eval(LANGUAGE_ID, "[]");
            for (int i = 0; i < list.size(); i++) {
                // Use Python list append
                Value appendMethod = pyList.getMember("append");
                appendMethod.execute(javaToPython(context, list.get(i)));
            }
            return pyList;
        }

        if (javaValue instanceof Object[] array) {
            Value pyList = context.eval(LANGUAGE_ID, "[]");
            for (Object item : array) {
                Value appendMethod = pyList.getMember("append");
                appendMethod.execute(javaToPython(context, item));
            }
            return pyList;
        }

        // Primitives and strings are handled automatically
        return context.asValue(javaValue);
    }

    /**
     * Convert a Python value to Java equivalent.
     */
    private Object pythonToJava(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isHostObject()) {
            return value.asHostObject();
        }

        Object primitive = convertPrimitive(value);
        if (primitive != null) {
            return primitive;
        }

        if (value.hasArrayElements()) {
            return convertList(value);
        }

        if (isPythonDict(value)) {
            return convertDict(value);
        }

        if (value.hasMembers()) {
            return convertGenericObject(value);
        }

        return value.toString();
    }

    /**
     * Convert primitive Python values (boolean, number, string).
     * 
     * @return the converted value, or null if not a primitive
     */
    private Object convertPrimitive(Value value) {
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

        return null;
    }

    /**
     * Convert a Python list to a Java List.
     */
    private java.util.List<Object> convertList(Value value) {
        int size = (int) value.getArraySize();
        java.util.List<Object> list = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(pythonToJava(value.getArrayElement(i)));
        }
        return list;
    }

    /**
     * Check if the value is a Python dict.
     */
    private boolean isPythonDict(Value value) {
        return value.hasMember("keys") && value.hasMember("__getitem__");
    }

    /**
     * Convert a Python dict to a Java Map.
     */
    private Map<String, Object> convertDict(Value value) {
        Map<String, Object> map = new HashMap<>();
        Value keys = value.invokeMember("keys");
        Value keysList = keys.invokeMember("__iter__");

        final int maxKeys = 100_000;
        int keyCount = 0;
        while (keyCount < maxKeys) {
            String keyStr = getNextKey(keysList);
            if (keyStr == null) {
                break;
            }
            Value pyValue = value.invokeMember("__getitem__", keyStr);
            map.put(keyStr, pythonToJava(pyValue));
            keyCount++;
        }
        return map;
    }

    /**
     * Get the next key from a Python iterator.
     * 
     * @return the next key as string, or null if iteration is complete
     */
    private String getNextKey(Value keysList) {
        try {
            Value key = keysList.invokeMember("__next__");
            return key.asString();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("StopIteration")) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Convert a generic Python object with members to a Java Map.
     */
    private Map<String, Object> convertGenericObject(Value value) {
        Map<String, Object> map = new HashMap<>();
        for (String key : value.getMemberKeys()) {
            if (!key.startsWith("__")) {
                map.put(key, pythonToJava(value.getMember(key)));
            }
        }
        return map;
    }

    /**
     * Extract line number from a PolyglotException.
     */
    private Integer extractLineNumber(PolyglotException e) {
        if (e.getSourceLocation() != null) {
            // Adjust for wrapper function (subtract 2 lines)
            int line = e.getSourceLocation().getStartLine();
            return line > 2 ? line - 2 : line;
        }
        return null;
    }

    /**
     * Extract a clean Python error message.
     */
    private String extractPythonError(PolyglotException e) {
        String message = e.getMessage();

        // Try to extract just the Python error part
        if (message != null) {
            // GraalPy often includes "python:" prefix
            if (message.startsWith("python:")) {
                message = message.substring(7).trim();
            }

            // Remove internal wrapper references
            message = message.replace("__workflow_main__", "script");
        }

        return message != null ? message : "Unknown Python error";
    }

    /**
     * @return the language ID ("python")
     */
    @Override
    public String getLanguageId() {
        return LANGUAGE_ID;
    }

    /**
     * @return display name with GraalPy mention
     */
    @Override
    public String getDisplayName() {
        return "Python (GraalPy - Embedded)";
    }

    /**
     * @return true if GraalPy runtime is available
     */
    @Override
    public boolean isAvailable() {
        if (available == null) {
            checkAvailability();
        }
        return available;
    }

    /**
     * @return details about GraalPy version or error message
     */
    @Override
    public String getAvailabilityInfo() {
        if (available == null) {
            checkAvailability();
        }
        return availabilityMessage;
    }

    private synchronized void checkAvailability() {
        if (available != null) {
            return;
        }

        try {
            try (Context context = Context.newBuilder(LANGUAGE_ID)
                    .allowExperimentalOptions(true)
                    .option("python.ForceImportSite", "false")
                    .build()) {
                Value result = context.eval(LANGUAGE_ID, "1 + 1");
                if (result.asInt() == 2) {
                    available = true;

                    // Get Python version
                    Value version = context.eval(LANGUAGE_ID,
                            "import sys; sys.version.split()[0]");
                    availabilityMessage = "GraalPy " + version.asString() + " available";
                    log.info("GraalPy available: {}", availabilityMessage);
                } else {
                    available = false;
                    availabilityMessage = "GraalPy evaluation failed";
                }
            }
        } catch (Exception e) {
            available = false;
            availabilityMessage = "GraalPy not available: " + e.getMessage();
            log.warn("GraalPy not available: {}", e.getMessage());
        }
    }
}
