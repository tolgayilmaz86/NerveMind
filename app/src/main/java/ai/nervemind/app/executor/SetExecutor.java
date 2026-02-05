package ai.nervemind.app.executor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "set" node type - sets or transforms data values.
 *
 * <p>Provides data manipulation capabilities including setting new values,
 * transforming existing data, and referencing values from previous node
 * outputs.
 * Commonly used to prepare data for subsequent nodes or restructure output.</p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Set node configuration parameters</caption>
 * <tr><th>Parameter</th><th>Type</th><th>Default</th><th>Description</th></tr>
 * <tr><td>values</td><td>Map or JSON String</td><td>{}</td><td>Key-value pairs
 * to set</td></tr>
 * <tr><td>keepOnlySet</td><td>Boolean</td><td>false</td><td>If true, output
 * only contains set values (no input passthrough)</td></tr>
 * </table>
 *
 * <h2>Expression Syntax</h2>
 * <p>Values can reference input fields using expression syntax:</p>
 * <ul>
 * <li><code>$input.fieldName</code> - Reference a field from input data</li>
 * <li><code>$input.nested.path</code> - Reference nested fields</li>
 * </ul>
 *
 * <h2>Example Configuration</h2>
 * <pre>{@code
 * {
 * "values": {
 * "userName": "$input.user.name",
 * "status": "processed",
 * "timestamp": "$input.createdAt"
 * },
 * "keepOnlySet": false
 * }
 * }</pre>
 *
 * <h2>Output Behavior</h2>
 * <ul>
 * <li>When {@code keepOnlySet=false} (default): Input data is merged with new
 * values</li>
 * <li>When {@code keepOnlySet=true}: Output contains only the explicitly set
 * values</li>
 * </ul>
 *
 * @see FilterExecutor For removing data based on conditions
 * @see MergeExecutor For combining data from multiple branches
 */
@Component
public class SetExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public SetExecutor() {
        // Default constructor
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(input);

        Map<String, Object> values = extractValues(node.parameters().get("values"));
        boolean keepOnlySet = extractBoolean(node.parameters().get("keepOnlySet"), false);

        if (keepOnlySet) {
            output = new HashMap<>();
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Support simple expression syntax: $input.fieldName
            if (value instanceof String strValue && strValue.startsWith("$input.")) {
                String fieldPath = strValue.substring(7);
                value = getNestedValue(input, fieldPath);
            }

            output.put(key, value);
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractValues(Object valuesObj) {
        if (valuesObj == null) {
            return Map.of();
        }
        if (valuesObj instanceof Map) {
            return (Map<String, Object>) valuesObj;
        }
        if (valuesObj instanceof String jsonStr) {
            // Try to parse as JSON
            try {
                return objectMapper.readValue(jsonStr, Map.class);
            } catch (Exception _) {
                // Not valid JSON, return empty
                return Map.of();
            }
        }
        return Map.of();
    }

    private boolean extractBoolean(Object value, boolean defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return defaultVal;
    }

    private Object getNestedValue(Map<String, Object> data, String path) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    @Override
    public String getNodeType() {
        return "set";
    }
}
