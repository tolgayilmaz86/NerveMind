package ai.nervemind.app.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "if" node type - provides conditional branching in
 * workflows.
 *
 * <p>
 * Evaluates a condition expression and routes execution to different branches
 * based on the result. Uses Spring Expression Language (SpEL) for condition
 * evaluation
 * with support for template interpolation.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>If node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>condition</td>
 * <td>String</td>
 * <td>"true"</td>
 * <td>SpEL expression to evaluate</td>
 * </tr>
 * </table>
 *
 * <h2>Template Variables</h2>
 * <p>
 * Use <code>{{ variableName }}</code> syntax to reference input data:
 * </p>
 * <ul>
 * <li><code>{{ status }} == 'active'</code> - Simple variable comparison</li>
 * <li><code>{{ data.count }} > 10</code> - Nested path access</li>
 * <li><code>{{ items[0].value }}</code> - Array index access</li>
 * </ul>
 *
 * <h2>SpEL Expression Examples</h2>
 * <ul>
 * <li><code>{{ price }} > 100 and {{ stock }} > 0</code> - Logical AND</li>
 * <li><code>{{ status }} == 'active' or {{ status }} == 'pending'</code> -
 * Logical OR</li>
 * <li><code>#input['response'] != null</code> - Null check with SpEL
 * syntax</li>
 * </ul>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr>
 * <th>Key</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>conditionResult</td>
 * <td>Boolean</td>
 * <td>The evaluated condition result</td>
 * </tr>
 * <tr>
 * <td>branch</td>
 * <td>String</td>
 * <td>"true" or "false" - indicates which branch to take</td>
 * </tr>
 * </table>
 *
 * @see SwitchExecutor For multi-branch routing
 * @see org.springframework.expression.spel.standard.SpelExpressionParser
 */
@Component
public class IfExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public IfExecutor() {
        // Default constructor
    }

    private static final Logger logger = LoggerFactory.getLogger(IfExecutor.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    // Use possessive quantifier (++) to prevent catastrophic backtracking (ReDoS)
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]++)\\}\\}");

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String condition = (String) params.getOrDefault("condition", "true");

        // First, interpolate {{ variable }} syntax to actual values
        String interpolatedCondition = interpolateVariables(condition, input);

        // Evaluate condition using Spring Expression Language
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariables(input);

        boolean result;
        try {
            if (interpolatedCondition == null || interpolatedCondition.isEmpty()) {
                result = false;
            } else {
                result = Boolean.TRUE.equals(
                        parser.parseExpression(interpolatedCondition).getValue(evalContext, Boolean.class));
            }
        } catch (Exception e) {
            // Log the error for debugging
            logger.error("IF condition evaluation failed: {}", e.getMessage());
            logger.error("  Original condition: {}", condition);
            logger.error("  Interpolated condition: {}", interpolatedCondition);
            // Default to false on evaluation error
            result = false;
        }

        Map<String, Object> output = new HashMap<>(input);
        output.put("conditionResult", result);
        output.put("branch", result ? "true" : "false");

        return output;
    }

    /**
     * Interpolate {{ variable }} syntax with values from input data.
     * Supports simple variable names and nested paths like {{ data.name }}.
     */
    private String interpolateVariables(String template, Map<String, Object> data) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varPath = matcher.group(1).trim();
            Object value = resolveVariable(varPath, data);

            if (value != null) {
                // For numeric values, keep as-is for proper comparison
                // For strings, wrap in quotes for SpEL
                String replacement;
                if (value instanceof Number) {
                    replacement = value.toString();
                } else if (value instanceof Boolean) {
                    replacement = value.toString();
                } else {
                    // Escape any special characters and wrap in quotes
                    replacement = "'" + value.toString().replace("'", "\\'") + "'";
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Keep original if variable not found (will likely cause SpEL error)
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Resolve a variable path like "data.name" or "items[0].value" from the data
     * map.
     */
    private Object resolveVariable(String path, Map<String, Object> data) {
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current == null) {
                return null;
            }
            current = resolvePathPart(part, current);
        }

        return current;
    }

    /**
     * Resolve a single part of a path, handling both simple keys and array access.
     */
    private Object resolvePathPart(String part, Object current) {
        int bracketIndex = part.indexOf('[');
        if (bracketIndex != -1) {
            return resolveArrayAccess(part, bracketIndex, current);
        }
        return getMapValue(part, current);
    }

    /**
     * Handle array access like items[0].
     */
    private Object resolveArrayAccess(String part, int bracketIndex, Object current) {
        String key = part.substring(0, bracketIndex);
        String indexStr = part.substring(bracketIndex + 1, part.indexOf(']'));
        int index = Integer.parseInt(indexStr);

        Object value = getMapValue(key, current);
        return getListElement(value, index);
    }

    /**
     * Get a value from a Map by key.
     */
    @SuppressWarnings("unchecked")
    private Object getMapValue(String key, Object current) {
        if (current instanceof Map) {
            return ((Map<String, Object>) current).get(key);
        }
        return null;
    }

    /**
     * Get an element from a List by index.
     */
    private Object getListElement(Object current, int index) {
        if (current instanceof java.util.List<?> list && index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    @Override
    public String getNodeType() {
        return "if";
    }
}
