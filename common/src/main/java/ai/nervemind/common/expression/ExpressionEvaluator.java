package ai.nervemind.common.expression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expression evaluator for dynamic workflow values.
 * 
 * <p>
 * This class provides a powerful expression language for dynamic value
 * computation
 * in workflows. It supports variable substitution and a rich set of built-in
 * functions.
 * </p>
 * 
 * <h2>Variable Syntax</h2>
 * <p>
 * Variables are referenced using the <code>${variableName}</code> syntax:
 * </p>
 * 
 * <pre>{@code
 * "Hello ${userName}!"       // Simple variable
 * "${input.body}"            // Nested access (as string key)
 * }</pre>
 * 
 * <h2>Built-in Functions</h2>
 * <table border="1">
 * <caption>Built-in expression functions</caption>
 * <tr>
 * <th>Category</th>
 * <th>Functions</th>
 * </tr>
 * <tr>
 * <td>Conditionals</td>
 * <td>{@code if(cond, then, else)}, {@code and(...)}, {@code or(...)},
 * {@code not(val)}</td>
 * </tr>
 * <tr>
 * <td>Comparisons</td>
 * <td>{@code eq(a, b)}, {@code ne(a, b)}, {@code gt(a, b)}, {@code lt(a, b)},
 * {@code gte(a, b)}, {@code lte(a, b)}</td>
 * </tr>
 * <tr>
 * <td>Strings</td>
 * <td>{@code contains, startsWith, endsWith, length, trim, upper, lower, concat, substring, replace, split, join}</td>
 * </tr>
 * <tr>
 * <td>Dates</td>
 * <td>{@code now()}, {@code format(date, pattern)}</td>
 * </tr>
 * <tr>
 * <td>Type Conversion</td>
 * <td>{@code toNumber(val)}, {@code toString(val)}, {@code toBoolean(val)}</td>
 * </tr>
 * </table>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * ExpressionEvaluator eval = new ExpressionEvaluator();
 * eval.setVariable("name", "Alice");
 * eval.setVariable("age", 30);
 * 
 * String greeting = eval.evaluate("Hello ${name}!");
 * // Result: "Hello Alice!"
 * 
 * String conditional = eval.evaluate("if(gt(${age}, 18), 'adult', 'minor')");
 * // Result: "adult"
 * 
 * Object ageAsInt = eval.evaluateToObject("${age}");
 * // Result: Long(30)
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * This class is <strong>not thread-safe</strong>. Each workflow execution
 * should
 * use its own instance with its own variable context.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see "ai.nervemind.ui.view.dialog.ExpressionEditorDialogController (Expression editor UI)"
 */
public class ExpressionEvaluator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([^)]*)\\)");
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private final Map<String, Object> variables = new HashMap<>();

    /**
     * Creates a new expression evaluator with no initial variables.
     */
    public ExpressionEvaluator() {
    }

    /**
     * Creates a new expression evaluator with the given initial variables.
     * 
     * @param variables initial variable map; keys are variable names,
     *                  values can be any object (toString() is used)
     */
    public ExpressionEvaluator(Map<String, Object> variables) {
        this.variables.putAll(variables);
    }

    /**
     * Set a variable value.
     * 
     * @param name  the variable name
     * @param value the variable value
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * Set multiple variables.
     * 
     * @param vars map of variables to set
     */
    public void setVariables(Map<String, Object> vars) {
        variables.putAll(vars);
    }

    /**
     * Evaluate an expression and return the result as a string.
     * 
     * @param expression the expression to evaluate
     * @return the result as a string
     */
    public String evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            return expression;
        }

        // First, substitute variables
        String result = substituteVariables(expression);

        // Then, evaluate functions
        result = evaluateFunctions(result);

        return result;
    }

    /**
     * Evaluate an expression and return the result as an object.
     * 
     * @param expression the expression to evaluate
     * @return the result as an object (Number, Boolean, or String)
     */
    public Object evaluateToObject(String expression) {
        String result = evaluate(expression);

        // Try to parse as number
        try {
            if (result.contains(".")) {
                return Double.parseDouble(result);
            }
            return Long.parseLong(result);
        } catch (NumberFormatException _) {
            // Ignore parsing errors
        }

        // Try to parse as boolean
        if (TRUE.equalsIgnoreCase(result))
            return true;
        if (FALSE.equalsIgnoreCase(result))
            return false;

        return result;
    }

    private String substituteVariables(String expression) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String evaluateFunctions(String expression) {
        String result = expression;
        Matcher matcher = FUNCTION_PATTERN.matcher(result);

        while (matcher.find()) {
            String funcName = matcher.group(1);
            String argsStr = matcher.group(2);
            List<String> args = parseArguments(argsStr);

            String funcResult = executeFunction(funcName, args);
            result = result.replace(matcher.group(0), funcResult);
            matcher = FUNCTION_PATTERN.matcher(result);
        }

        return result;
    }

    private List<String> parseArguments(String argsStr) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        ParseState state = new ParseState();

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            boolean isEscaped = i > 0 && argsStr.charAt(i - 1) == '\\';

            if (isQuoteChar(c) && !isEscaped) {
                state.handleQuote(c);
                current.append(c);
            } else if (c == '(' && !state.inString) {
                state.parenDepth++;
                current.append(c);
            } else if (c == ')' && !state.inString) {
                state.parenDepth--;
                current.append(c);
            } else if (c == ',' && state.parenDepth == 0 && !state.inString) {
                args.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) {
            args.add(last);
        }

        return args;
    }

    private boolean isQuoteChar(char c) {
        return c == '"' || c == '\'';
    }

    private static class ParseState {
        boolean inString = false;
        char stringChar = 0;
        int parenDepth = 0;

        void handleQuote(char c) {
            if (!inString) {
                inString = true;
                stringChar = c;
            } else if (c == stringChar) {
                inString = false;
            }
        }
    }

    private String executeFunction(String name, List<String> args) {
        return switch (name.toLowerCase()) {
            // Conditionals
            case "if" -> evaluateIf(args);
            case "and" -> evaluateAnd(args);
            case "or" -> evaluateOr(args);
            case "not" -> evaluateNot(args);

            // Comparisons
            case "eq" -> evaluateEquals(args);
            case "ne" -> evaluateNotEquals(args);
            case "gt" -> evaluateGreaterThan(args);
            case "lt" -> evaluateLessThan(args);
            case "gte" -> evaluateGreaterThanOrEqual(args);
            case "lte" -> evaluateLessThanOrEqual(args);

            // String functions
            case "contains" -> evaluateContains(args);
            case "startswith" -> evaluateStartsWith(args);
            case "endswith" -> evaluateEndsWith(args);
            case "length" -> evaluateLength(args);
            case "trim" -> evaluateTrim(args);
            case "upper" -> evaluateUpper(args);
            case "lower" -> evaluateLower(args);
            case "concat" -> evaluateConcat(args);
            case "substring" -> evaluateSubstring(args);
            case "replace" -> evaluateReplace(args);
            case "split" -> evaluateSplit(args);
            case "join" -> evaluateJoin(args);

            // Date functions
            case "now" -> evaluateNow();
            case "format" -> evaluateFormat(args);

            // Type conversion
            case "tonumber" -> evaluateToNumber(args);
            case "tostring" -> args.isEmpty() ? "" : stripQuotes(args.getFirst());
            case "toboolean" -> evaluateToBoolean(args);

            default -> name + "(" + String.join(", ", args) + ")";
        };
    }

    // Conditional functions

    private String evaluateIf(List<String> args) {
        if (args.size() < 3)
            return "";
        boolean condition = toBoolean(args.get(0));
        return condition ? stripQuotes(args.get(1)) : stripQuotes(args.get(2));
    }

    private String evaluateAnd(List<String> args) {
        return args.stream().allMatch(this::toBoolean) ? TRUE : FALSE;
    }

    private String evaluateOr(List<String> args) {
        return args.stream().anyMatch(this::toBoolean) ? TRUE : FALSE;
    }

    private String evaluateNot(List<String> args) {
        String value = args.isEmpty() ? "" : args.getFirst();
        return !toBoolean(value) ? TRUE : FALSE;
    }

    // Comparison functions

    private String evaluateEquals(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        return stripQuotes(args.get(0)).equals(stripQuotes(args.get(1))) ? TRUE : FALSE;
    }

    private String evaluateNotEquals(List<String> args) {
        if (args.size() < 2)
            return TRUE;
        return !stripQuotes(args.get(0)).equals(stripQuotes(args.get(1))) ? TRUE : FALSE;
    }

    private String evaluateGreaterThan(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return a > b ? TRUE : FALSE;
        } catch (NumberFormatException _) {
            return FALSE;
        }
    }

    private String evaluateLessThan(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return a < b ? TRUE : FALSE;
        } catch (NumberFormatException _) {
            return FALSE;
        }
    }

    private String evaluateGreaterThanOrEqual(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return a >= b ? TRUE : FALSE;
        } catch (NumberFormatException _) {
            return FALSE;
        }
    }

    private String evaluateLessThanOrEqual(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        try {
            double a = Double.parseDouble(args.get(0));
            double b = Double.parseDouble(args.get(1));
            return a <= b ? TRUE : FALSE;
        } catch (NumberFormatException _) {
            return FALSE;
        }
    }

    // String functions

    private String evaluateContains(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        return stripQuotes(args.get(0)).contains(stripQuotes(args.get(1))) ? TRUE : FALSE;
    }

    private String evaluateStartsWith(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        return stripQuotes(args.get(0)).startsWith(stripQuotes(args.get(1))) ? TRUE : FALSE;
    }

    private String evaluateEndsWith(List<String> args) {
        if (args.size() < 2)
            return FALSE;
        return stripQuotes(args.get(0)).endsWith(stripQuotes(args.get(1))) ? TRUE : FALSE;
    }

    private String evaluateLength(List<String> args) {
        return String.valueOf(args.isEmpty() ? 0 : stripQuotes(args.getFirst()).length());
    }

    private String evaluateTrim(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).trim();
    }

    private String evaluateUpper(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).toUpperCase();
    }

    private String evaluateLower(List<String> args) {
        return args.isEmpty() ? "" : stripQuotes(args.getFirst()).toLowerCase();
    }

    private String evaluateConcat(List<String> args) {
        return args.stream()
                .map(this::stripQuotes)
                .reduce("", (a, b) -> a + b);
    }

    private String evaluateSubstring(List<String> args) {
        if (args.isEmpty())
            return "";
        String str = stripQuotes(args.getFirst());
        int start = args.size() > 1 ? Integer.parseInt(args.get(1).trim()) : 0;
        int end = args.size() > 2 ? Integer.parseInt(args.get(2).trim()) : str.length();
        return str.substring(Math.max(0, start), Math.min(str.length(), end));
    }

    private String evaluateReplace(List<String> args) {
        if (args.size() < 3)
            return args.isEmpty() ? "" : stripQuotes(args.getFirst());
        return stripQuotes(args.get(0))
                .replace(stripQuotes(args.get(1)), stripQuotes(args.get(2)));
    }

    private String evaluateSplit(List<String> args) {
        if (args.size() < 2)
            return "[]";
        String[] parts = stripQuotes(args.get(0)).split(Pattern.quote(stripQuotes(args.get(1))));
        return "[" + String.join(", ", parts) + "]";
    }

    private String evaluateJoin(List<String> args) {
        if (args.size() < 2)
            return "";
        String input = stripQuotes(args.get(0));
        String delimiter = stripQuotes(args.get(1));
        // Remove array brackets if present
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }
        String[] parts = input.split(",\\s*");
        return String.join(delimiter, parts);
    }

    // Date functions

    private String evaluateNow() {
        return Instant.now().toString();
    }

    private String evaluateFormat(List<String> args) {
        if (args.size() < 2)
            return "";
        try {
            Instant instant = Instant.parse(stripQuotes(args.get(0)));
            String pattern = stripQuotes(args.get(1));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern)
                    .withZone(ZoneId.systemDefault());
            return formatter.format(instant);
        } catch (Exception _) {
            return args.get(0);
        }
    }

    // Type conversion

    private String evaluateToNumber(List<String> args) {
        if (args.isEmpty())
            return "0";
        try {
            String value = stripQuotes(args.getFirst());
            if (value.contains(".")) {
                return String.valueOf(Double.parseDouble(value));
            }
            return String.valueOf(Long.parseLong(value));
        } catch (NumberFormatException _) {
            return "0";
        }
    }

    private String evaluateToBoolean(List<String> args) {
        String value = args.isEmpty() ? "" : args.getFirst();
        return toBoolean(value) ? TRUE : FALSE;
    }

    // Helpers

    private String stripQuotes(String value) {
        if (value == null)
            return "";
        value = value.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean toBoolean(String value) {
        String v = stripQuotes(value).toLowerCase();
        return TRUE.equals(v) || "1".equals(v) || "yes".equals(v);
    }
}
