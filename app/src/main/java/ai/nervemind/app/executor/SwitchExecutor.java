package ai.nervemind.app.executor;

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
 * Executor for the "switch" node type - provides multi-branch conditional
 * routing.
 *
 * <p>
 * Routes workflow execution to different branches based on evaluating a set of
 * rules against input data. Unlike {@link IfExecutor} which provides binary
 * branching,
 * SwitchExecutor supports multiple target branches with complex condition
 * matching.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <dl>
 * <dt>rules</dt>
 * <dd>List of rule objects with conditions (default: [])</dd>
 * <dt>fallbackOutput</dt>
 * <dd>Output branch when no rules match (default: "fallback")</dd>
 * </dl>
 *
 * <h2>Rule Structure</h2>
 * <p>
 * Each rule in the rules list has:
 * </p>
 * <ul>
 * <li><strong>name</strong> - Output branch name</li>
 * <li><strong>conditions</strong> - List of condition objects</li>
 * <li><strong>combineWith</strong> - "and" or "or" (how to combine conditions,
 * default: "and")</li>
 * </ul>
 *
 * <h2>Condition Structure</h2>
 * <p>
 * Each condition object has the following properties:
 * </p>
 * <ul>
 * <li><strong>field</strong> - Field name from input data to evaluate</li>
 * <li><strong>operator</strong> - Comparison operator (see below)</li>
 * <li><strong>value</strong> - Value to compare against</li>
 * </ul>
 *
 * <h2>Supported Operators</h2>
 * <ul>
 * <li><code>equals</code> / <code>==</code> / <code>eq</code> - Equality
 * check</li>
 * <li><code>notEquals</code> / <code>!=</code> / <code>neq</code> - Inequality
 * check</li>
 * <li><code>contains</code> - String contains or list membership</li>
 * <li><code>notContains</code> - Negation of contains</li>
 * <li><code>greaterThan</code> / <code>&gt;</code> / <code>gt</code></li>
 * <li><code>lessThan</code> / <code>&lt;</code> / <code>lt</code></li>
 * <li><code>greaterThanOrEquals</code> / <code>&gt;=</code> /
 * <code>gte</code></li>
 * <li><code>lessThanOrEquals</code> / <code>&lt;=</code> /
 * <code>lte</code></li>
 * <li><code>startsWith</code> - String prefix check</li>
 * <li><code>endsWith</code> - String suffix check</li>
 * <li><code>isEmpty</code> - Null, empty string, or empty collection</li>
 * <li><code>isNotEmpty</code> - Has value</li>
 * <li><code>matches</code> / <code>regex</code> - Regular expression match</li>
 * <li><code>isNull</code> - Check if value is null</li>
 * <li><code>isNotNull</code> - Check if value is not null</li>
 * <li><code>in</code> - Check if value is in a list</li>
 * <li><code>notIn</code> - Check if value is not in a list</li>
 * </ul>
 *
 * <h2>Output Data</h2>
 * <p>
 * The executor adds the following keys to the output:
 * </p>
 * <ul>
 * <li><code>_branch</code> - The name of the selected output branch</li>
 * <li><code>_matchedRuleIndex</code> - Index of the matched rule (-1 if none)</li>
 * <li><code>_matched</code> - Boolean indicating if any rule matched</li>
 * <li><code>_selectedOutput</code> - Name of the selected output branch (legacy)</li>
 * </ul>
 *
 * @see IfExecutor For simple binary branching
 */
@Component
public class SwitchExecutor implements NodeExecutor {

    // Default constructor for Spring
    /**
     * Default constructor for Spring.
     */
    public SwitchExecutor() {
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) params.getOrDefault("rules", List.of());
        // Safely handle fallbackOutput parameter which might be parsed as other types
        Object fallbackObj = params.getOrDefault("fallbackOutput", "fallback");
        String fallbackOutput = String.valueOf(fallbackObj);

        Map<String, Object> output = new HashMap<>(input);

        // Evaluate each rule in order
        String matchedBranch = null;
        int matchedRuleIndex = -1;

        for (int i = 0; i < rules.size(); i++) {
            Map<String, Object> rule = rules.get(i);
            String branchName = (String) rule.getOrDefault("name", "output" + i);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.getOrDefault("conditions",
                    List.of());
            String combineWith = (String) rule.getOrDefault("combineWith", "and");

            boolean ruleMatches = evaluateConditions(conditions, input, combineWith);

            if (ruleMatches) {
                matchedBranch = branchName;
                matchedRuleIndex = i;
                break;
            }
        }

        // Set output routing information
        if (matchedBranch != null) {
            output.put("_branch", matchedBranch);
            output.put("_matchedRuleIndex", matchedRuleIndex);
            output.put("_matched", true);
        } else {
            output.put("_branch", fallbackOutput);
            output.put("_matchedRuleIndex", -1);
            output.put("_matched", false);
        }

        return output;
    }

    private boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> input,
            String combineWith) {
        if (conditions.isEmpty()) {
            return true;
        }

        boolean isAnd = "and".equalsIgnoreCase(combineWith);

        for (Map<String, Object> condition : conditions) {
            boolean conditionResult = evaluateCondition(condition, input);

            if (isAnd && !conditionResult) {
                return false;
            }
            if (!isAnd && conditionResult) {
                return true;
            }
        }

        return isAnd;
    }

    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> input) {
        String field = (String) condition.get("field");
        String operator = (String) condition.getOrDefault("operator", "equals");
        Object expectedValue = condition.get("value");

        Object actualValue = getNestedValue(input, field);

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
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
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
        if (actual == null || expected == null) {
            return 0;
        }
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
        return "switch";
    }
}
