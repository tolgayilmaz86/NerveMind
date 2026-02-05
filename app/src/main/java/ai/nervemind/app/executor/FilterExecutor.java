package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "filter" node type - filters arrays based on conditions.
 *
 * <p>Processes an array of items and returns only those that match specified
 * conditions. Uses the same condition evaluation logic as {@link
 * SwitchExecutor}
 * for consistency across conditional operations.</p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Filter node configuration parameters</caption>
 * <tr><th>Parameter</th><th>Type</th><th>Default</th><th>Description</th></tr>
 * <tr><td>inputField</td><td>String</td><td>"items"</td><td>Field containing
 * array to filter</td></tr>
 * <tr><td>outputField</td><td>String</td><td>"filtered"</td><td>Field to store
 * filtered results</td></tr>
 * <tr><td>conditions</td><td>List</td><td>[]</td><td>List of condition
 * objects</td></tr>
 * <tr><td>combineWith</td><td>String</td><td>"and"</td><td>"and" or "or" - how
 * to combine conditions</td></tr>
 * <tr><td>keepMatching</td><td>Boolean</td><td>true</td><td>true to keep
 * matching items, false to keep non-matching</td></tr>
 * </table>
 *
 * <h2>Condition Structure</h2>
 * <p>Same as {@link SwitchExecutor} - each condition has field, operator, and
 * value.</p>
 *
 * <h2>Example Configuration</h2>
 * <pre>{@code
 * {
 * "inputField": "products",
 * "outputField": "activeProducts",
 * "conditions": [
 * { "field": "status", "operator": "equals", "value": "active" },
 * { "field": "price", "operator": "greaterThan", "value": 0 }
 * ],
 * "combineWith": "and"
 * }
 * }</pre>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr><th>Key</th><th>Type</th><th>Description</th></tr>
 * <tr><td>[outputField]</td><td>List</td><td>Array of items that matched
 * conditions</td></tr>
 * <tr><td>_filteredCount</td><td>Integer</td><td>Number of items in
 * result</td></tr>
 * <tr><td>_originalCount</td><td>Integer</td><td>Number of items before
 * filtering</td></tr>
 * <tr><td>_removedCount</td><td>Integer</td><td>Number of items removed by
 * filtering</td></tr>
 * </table>
 *
 * @see SwitchExecutor For condition operators reference
 * @see SortExecutor For sorting filtered results
 */
@Component
public class FilterExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public FilterExecutor() {
        // Default constructor
    }

    private static final String FILTERED_COUNT = "_filteredCount";
    private static final String ORIGINAL_COUNT = "_originalCount";
    private static final String REMOVED_COUNT = "_removedCount";

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String inputField = (String) params.getOrDefault("inputField", "items");
        String outputField = (String) params.getOrDefault("outputField", "filtered");
        boolean keepMatching = (Boolean) params.getOrDefault("keepMatching", true);
        String combineWith = (String) params.getOrDefault("combineWith", "and");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) params.getOrDefault("conditions", List.of());

        // Get the input array
        Object inputData = getNestedValue(input, inputField);

        Map<String, Object> output = new HashMap<>(input);

        if (!(inputData instanceof List<?>)) {
            return handleNonListInput(inputData, conditions, combineWith, keepMatching, outputField, inputField,
                    output);
        }

        List<?> items = (List<?>) inputData;
        List<Object> filtered = filterItems(items, conditions, combineWith, keepMatching);

        output.put(outputField, filtered);
        output.put(FILTERED_COUNT, filtered.size());
        output.put(ORIGINAL_COUNT, items.size());
        output.put(REMOVED_COUNT, items.size() - filtered.size());

        return output;
    }

    private Map<String, Object> handleNonListInput(Object inputData, List<Map<String, Object>> conditions,
            String combineWith, boolean keepMatching, String outputField, String inputField,
            Map<String, Object> output) {
        if (inputData instanceof Map<?, ?> singleItem) {
            return handleSingleItemInput(singleItem, conditions, combineWith, keepMatching, outputField, output);
        }
        output.put(outputField, List.of());
        output.put(FILTERED_COUNT, 0);
        output.put(ORIGINAL_COUNT, 0);
        output.put("_error", "Input field '" + inputField + "' is not an array");
        return output;
    }

    private Map<String, Object> handleSingleItemInput(Map<?, ?> singleItem, List<Map<String, Object>> conditions,
            String combineWith, boolean keepMatching, String outputField, Map<String, Object> output) {
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) singleItem;
        boolean matches = evaluateConditions(conditions, item, combineWith);
        boolean shouldKeep = shouldKeepItem(keepMatching, matches);

        output.put(outputField, shouldKeep ? List.of(item) : List.of());
        output.put(FILTERED_COUNT, shouldKeep ? 1 : 0);
        output.put(ORIGINAL_COUNT, 1);
        return output;
    }

    private List<Object> filterItems(List<?> items, List<Map<String, Object>> conditions,
            String combineWith, boolean keepMatching) {
        List<Object> filtered = new ArrayList<>();

        for (Object item : items) {
            if (shouldIncludeItem(item, conditions, combineWith, keepMatching)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean shouldIncludeItem(Object item, List<Map<String, Object>> conditions,
            String combineWith, boolean keepMatching) {
        if (item instanceof Map<?, ?> mapItem) {
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) mapItem;
            boolean matches = evaluateConditions(conditions, itemMap, combineWith);
            return shouldKeepItem(keepMatching, matches);
        }
        // For non-map items, include them if keepMatching is true and no conditions
        return conditions.isEmpty() && keepMatching;
    }

    private boolean shouldKeepItem(boolean keepMatching, boolean matches) {
        return (keepMatching && matches) || (!keepMatching && !matches);
    }

    private boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> item,
            String combineWith) {
        if (conditions.isEmpty()) {
            return true;
        }

        boolean isAnd = "and".equalsIgnoreCase(combineWith);

        for (Map<String, Object> condition : conditions) {
            boolean result = evaluateCondition(condition, item);

            if (isAnd && !result) {
                return false;
            }
            if (!isAnd && result) {
                return true;
            }
        }

        return isAnd;
    }

    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> item) {
        String field = (String) condition.get("field");
        String operator = (String) condition.getOrDefault("operator", "equals");
        Object expectedValue = condition.get("value");

        Object actualValue = getNestedValue(item, field);

        return switch (operator.toLowerCase()) {
            case "equals", "eq", "==" -> Objects.equals(actualValue, expectedValue);
            case "notequals", "neq", "!=" -> !Objects.equals(actualValue, expectedValue);
            case "contains" -> actualValue != null && actualValue.toString().contains(String.valueOf(expectedValue));
            case "notcontains" ->
                actualValue == null || !actualValue.toString().contains(String.valueOf(expectedValue));
            case "startswith" ->
                actualValue != null && actualValue.toString().startsWith(String.valueOf(expectedValue));
            case "endswith" -> actualValue != null && actualValue.toString().endsWith(String.valueOf(expectedValue));
            case "matches", "regex" ->
                actualValue != null && Pattern.matches(String.valueOf(expectedValue), actualValue.toString());
            case "gt", ">" -> compareNumbers(actualValue, expectedValue) > 0;
            case "gte", ">=" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "lt", "<" -> compareNumbers(actualValue, expectedValue) < 0;
            case "lte", "<=" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "isempty" -> actualValue == null || actualValue.toString().isEmpty();
            case "isnotempty" -> actualValue != null && !actualValue.toString().isEmpty();
            case "isnull" -> actualValue == null;
            case "isnotnull" -> actualValue != null;
            case "in" -> {
                if (expectedValue instanceof List<?> list) {
                    yield list.contains(actualValue);
                }
                yield false;
            }
            case "notin" -> {
                if (expectedValue instanceof List<?> list) {
                    yield !list.contains(actualValue);
                }
                yield true;
            }
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object data, String path) {
        if (path == null || path.isEmpty() || data == null) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private int compareNumbers(Object actual, Object expected) {
        try {
            double actualNum = actual instanceof Number n ? n.doubleValue() : Double.parseDouble(actual.toString());
            double expectedNum = expected instanceof Number n ? n.doubleValue()
                    : Double.parseDouble(expected.toString());
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException | NullPointerException _) {
            return 0;
        }
    }

    @Override
    public String getNodeType() {
        return "filter";
    }
}
