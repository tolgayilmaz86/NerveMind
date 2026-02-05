/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * ViewModel for the ExpressionEditor component.
 *
 * <p>
 * Manages expression text, validation, autocomplete suggestions, and
 * variable/function references.
 * This ViewModel contains all the business logic for expression editing, making
 * it testable
 * without the JavaFX runtime.
 *
 * <p>
 * <strong>IMPORTANT:</strong> This ViewModel only uses javafx.beans.* and
 * javafx.collections.*
 * imports. No javafx.scene.* classes are allowed to ensure testability.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Expression text binding with bidirectional support</li>
 * <li>Real-time validation with detailed error messages</li>
 * <li>Autocomplete suggestions for variables and functions</li>
 * <li>Suggestion filtering based on current input</li>
 * </ul>
 */
public class ExpressionEditorViewModel extends BaseViewModel {

    // ===== Regex Patterns =====

    /** Pattern to match variable references: ${variableName} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]*)}");

    /** Pattern to match empty variable references: ${ } */
    private static final Pattern EMPTY_VARIABLE_PATTERN = Pattern.compile("\\$\\{\\s*}");

    /** List of built-in functions (without parentheses for matching) */
    private static final List<String> VALID_FUNCTION_NAMES = List.of(
            "if", "and", "or", "not", "eq", "neq", "gt", "lt", "gte", "lte",
            "concat", "substring", "length", "upper", "lower", "trim", "replace",
            "split", "join", "contains", "startsWith", "endsWith",
            "now", "format", "parse", "round", "abs", "random");

    /** Pattern to match any function call for undefined function detection */
    private static final Pattern ANY_FUNCTION_PATTERN = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*\\(");

    /** Available functions with parentheses for insertion */
    private static final List<String> AVAILABLE_FUNCTIONS = List.of(
            "if(condition, then, else)",
            "and(a, b)",
            "or(a, b)",
            "not(value)",
            "eq(a, b)",
            "neq(a, b)",
            "gt(a, b)",
            "lt(a, b)",
            "gte(a, b)",
            "lte(a, b)",
            "concat(a, b, ...)",
            "substring(text, start, end)",
            "length(text)",
            "upper(text)",
            "lower(text)",
            "trim(text)",
            "replace(text, search, replacement)",
            "split(text, delimiter)",
            "join(list, delimiter)",
            "contains(text, search)",
            "startsWith(text, prefix)",
            "endsWith(text, suffix)",
            "now()",
            "format(date, pattern)",
            "parse(text, type)",
            "round(number, decimals)",
            "abs(number)",
            "random()");

    // ===== Properties =====

    private final StringProperty expression = new SimpleStringProperty("");
    private final BooleanProperty valid = new SimpleBooleanProperty(true);
    private final StringProperty validationMessage = new SimpleStringProperty();
    private final BooleanProperty showValidation = new SimpleBooleanProperty(false);

    private final ObservableList<String> availableVariables = FXCollections.observableArrayList();
    private final ObservableList<String> allSuggestions = FXCollections.observableArrayList();
    private final FilteredList<String> filteredSuggestions;
    private final StringProperty suggestionFilter = new SimpleStringProperty("");

    private final BooleanProperty suggestionsVisible = new SimpleBooleanProperty(false);
    private final ObjectProperty<String> selectedSuggestion = new SimpleObjectProperty<>();
    private final IntegerProperty caretPosition = new SimpleIntegerProperty(0);

    private final StringProperty promptText = new SimpleStringProperty("Enter expression (Ctrl+Space for suggestions)");
    private final IntegerProperty prefRowCount = new SimpleIntegerProperty(3);

    /**
     * Creates a new ExpressionEditorViewModel.
     */
    public ExpressionEditorViewModel() {
        // Setup filtered suggestions
        this.filteredSuggestions = new FilteredList<>(allSuggestions);

        // Validate expression on change
        expression.addListener((obs, oldVal, newVal) -> validateExpression(newVal));

        // Filter suggestions when filter text changes
        suggestionFilter.addListener((obs, oldVal, newVal) -> updateSuggestionFilter(newVal));

        // Rebuild suggestions when variables change
        availableVariables.addListener((javafx.collections.ListChangeListener<String>) change -> rebuildSuggestions());

        // Initialize suggestions with functions
        rebuildSuggestions();
    }

    // ===== Expression Properties =====

    /**
     * The expression text.
     *
     * @return the expression property
     */
    public StringProperty expressionProperty() {
        return expression;
    }

    /**
     * Gets the current expression text.
     * 
     * @return the expression text
     */
    public String getExpression() {
        return expression.get();
    }

    /**
     * Sets the expression text.
     * 
     * @param expression the expression text to set
     */
    public void setExpression(String expression) {
        this.expression.set(expression != null ? expression : "");
    }

    /**
     * Whether the current expression is valid.
     *
     * @return the valid property
     */
    public ReadOnlyBooleanProperty validProperty() {
        return valid;
    }

    /**
     * Checks if the current expression is valid.
     * 
     * @return true if valid
     */
    public boolean isValid() {
        return valid.get();
    }

    /**
     * The validation message (error or success).
     *
     * @return the validation message property
     */
    public StringProperty validationMessageProperty() {
        return validationMessage;
    }

    /**
     * Gets the current validation message.
     * 
     * @return the validation message
     */
    public String getValidationMessage() {
        return validationMessage.get();
    }

    /**
     * Whether to show the validation message.
     *
     * @return the show validation property
     */
    public BooleanProperty showValidationProperty() {
        return showValidation;
    }

    /**
     * Checks if the validation message should be shown.
     * 
     * @return true if validation should be shown
     */
    public boolean isShowValidation() {
        return showValidation.get();
    }

    // ===== Variable Properties =====

    /**
     * List of available variables for autocomplete and validation.
     *
     * @return the available variables list
     */
    public ObservableList<String> getAvailableVariables() {
        return availableVariables;
    }

    /**
     * Sets the available variables.
     *
     * @param variables the list of variable names
     */
    public void setAvailableVariables(List<String> variables) {
        availableVariables.setAll(variables != null ? variables : List.of());
        // Re-validate with new variables
        validateExpression(getExpression());
    }

    // ===== Suggestion Properties =====

    /**
     * All suggestions (variables and functions).
     *
     * @return the all suggestions list
     */
    public ObservableList<String> getAllSuggestions() {
        return allSuggestions;
    }

    /**
     * Filtered suggestions based on current input.
     *
     * @return the filtered suggestions list
     */
    public FilteredList<String> getFilteredSuggestions() {
        return filteredSuggestions;
    }

    /**
     * The current filter text for suggestions.
     *
     * @return the suggestion filter property
     */
    public StringProperty suggestionFilterProperty() {
        return suggestionFilter;
    }

    /**
     * Sets the suggestion filter text.
     * 
     * @param filter the filter text
     */
    public void setSuggestionFilter(String filter) {
        suggestionFilter.set(filter != null ? filter : "");
    }

    /**
     * Whether the suggestions popup should be visible.
     *
     * @return the suggestions visible property
     */
    public BooleanProperty suggestionsVisibleProperty() {
        return suggestionsVisible;
    }

    public boolean isSuggestionsVisible() {
        return suggestionsVisible.get();
    }

    /**
     * Sets whether the suggestions popup should be visible.
     * 
     * @param visible true to show
     */
    public void setSuggestionsVisible(boolean visible) {
        suggestionsVisible.set(visible);
    }

    /**
     * The currently selected suggestion.
     *
     * @return the selected suggestion property
     */
    public ObjectProperty<String> selectedSuggestionProperty() {
        return selectedSuggestion;
    }

    /**
     * Gets the currently selected suggestion.
     * 
     * @return the selected suggestion or null
     */
    public String getSelectedSuggestion() {
        return selectedSuggestion.get();
    }

    /**
     * Sets the selected suggestion.
     * 
     * @param suggestion the suggestion to select
     */
    public void setSelectedSuggestion(String suggestion) {
        selectedSuggestion.set(suggestion);
    }

    /**
     * The current caret position in the expression text area.
     *
     * @return the caret position property
     */
    public IntegerProperty caretPositionProperty() {
        return caretPosition;
    }

    /**
     * Gets the current caret position.
     * 
     * @return the caret position
     */
    public int getCaretPosition() {
        return caretPosition.get();
    }

    /**
     * Sets the current caret position.
     * 
     * @param position the caret position
     */
    public void setCaretPosition(int position) {
        caretPosition.set(position);
    }

    // ===== Display Properties =====

    /**
     * The prompt text for the expression input.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Gets the current prompt text.
     * 
     * @return the prompt text
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     * 
     * @param text the prompt text to set
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /**
     * The preferred row count for the expression input.
     *
     * @return the preferred row count property
     */
    public IntegerProperty prefRowCountProperty() {
        return prefRowCount;
    }

    /**
     * Gets the preferred row count.
     * 
     * @return the row count
     */
    public int getPrefRowCount() {
        return prefRowCount.get();
    }

    /**
     * Sets the preferred row count.
     * 
     * @param count the row count
     */
    public void setPrefRowCount(int count) {
        prefRowCount.set(count);
    }

    // ===== Actions =====

    /**
     * Shows the suggestions popup with all available suggestions.
     */
    public void showSuggestions() {
        suggestionFilter.set("");
        suggestionsVisible.set(!allSuggestions.isEmpty());
    }

    /**
     * Hides the suggestions popup.
     */
    public void hideSuggestions() {
        suggestionsVisible.set(false);
    }

    /**
     * Inserts the selected suggestion at the current caret position.
     *
     * @return the new expression text, or null if no suggestion is selected
     */
    public String insertSelectedSuggestion() {
        String selected = selectedSuggestion.get();
        if (selected == null) {
            return null;
        }

        int caret = caretPosition.get();
        String currentExpr = expression.get();

        String newExpression = currentExpr.substring(0, caret) + selected
                + currentExpr.substring(caret);

        hideSuggestions();
        return newExpression;
    }

    /**
     * Inserts a variable reference at the current caret position.
     *
     * @param variableName the variable name to insert
     * @return the new expression text
     */
    public String insertVariable(String variableName) {
        int caret = caretPosition.get();
        String currentExpr = expression.get();
        String varRef = "${" + variableName + "}";

        return currentExpr.substring(0, caret) + varRef + currentExpr.substring(caret);
    }

    /**
     * Inserts a function at the current caret position.
     *
     * @param function the function text to insert
     * @return the new expression text
     */
    public String insertFunction(String function) {
        int caret = caretPosition.get();
        String currentExpr = expression.get();

        return currentExpr.substring(0, caret) + function + currentExpr.substring(caret);
    }

    /**
     * Returns the list of available functions with their signatures.
     *
     * @return unmodifiable list of function signatures
     */
    public List<String> getAvailableFunctions() {
        return AVAILABLE_FUNCTIONS;
    }

    /**
     * Returns whether variables are available for insertion.
     *
     * @return true if at least one variable is defined
     */
    public boolean hasAvailableVariables() {
        return !availableVariables.isEmpty();
    }

    // ===== Validation =====

    /**
     * Validates the given expression and updates validation state.
     *
     * @param expr the expression to validate
     */
    private void validateExpression(String expr) {
        if (expr == null || expr.isBlank()) {
            setValidState(true, null);
            return;
        }

        String error = checkUnclosedVariableReferences(expr);
        if (error == null) {
            error = checkBalancedParentheses(expr);
        }
        if (error == null) {
            error = checkEmptyVariableReferences(expr);
        }
        if (error == null) {
            error = checkUndefinedVariables(expr);
        }
        if (error == null) {
            error = checkUndefinedFunctions(expr);
        }

        if (error != null) {
            setValidState(false, error);
        } else {
            setValidState(true, "✓ Valid expression");
        }
    }

    /**
     * Updates the validation state.
     *
     * @param isValid whether the expression is valid
     * @param message the validation message
     */
    private void setValidState(boolean isValid, String message) {
        valid.set(isValid);
        validationMessage.set(message);
        showValidation.set(message != null);
    }

    /**
     * Checks for unclosed variable references (missing closing brace).
     *
     * @param expr the expression to check
     * @return error message or null if valid
     */
    String checkUnclosedVariableReferences(String expr) {
        int openBraces = 0;
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '$' && i + 1 < expr.length() && expr.charAt(i + 1) == '{') {
                openBraces++;
                i += 2;
            } else if (c == '}' && openBraces > 0) {
                openBraces--;
                i++;
            } else {
                i++;
            }
        }
        return openBraces > 0 ? "Unclosed variable reference: missing '}'" : null;
    }

    /**
     * Checks for balanced parentheses.
     *
     * @param expr the expression to check
     * @return error message or null if valid
     */
    String checkBalancedParentheses(String expr) {
        int parenCount = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            }
            if (parenCount < 0) {
                return "Unbalanced parentheses: extra ')'";
            }
        }
        return parenCount > 0 ? "Unbalanced parentheses: missing ')'" : null;
    }

    /**
     * Checks for empty variable references.
     *
     * @param expr the expression to check
     * @return error message or null if valid
     */
    String checkEmptyVariableReferences(String expr) {
        Matcher matcher = EMPTY_VARIABLE_PATTERN.matcher(expr);
        return matcher.find() ? "Empty variable reference" : null;
    }

    /**
     * Checks for undefined variable references.
     *
     * @param expr the expression to check
     * @return error message or null if valid
     */
    String checkUndefinedVariables(String expr) {
        Matcher varMatcher = VARIABLE_PATTERN.matcher(expr);
        while (varMatcher.find()) {
            String varName = varMatcher.group(1).trim();
            if (!availableVariables.contains(varName)) {
                return "Unknown variable: " + varName;
            }
        }
        return null;
    }

    /**
     * Checks for undefined function calls.
     *
     * @param expr the expression to check
     * @return error message or null if valid
     */
    String checkUndefinedFunctions(String expr) {
        Matcher funcMatcher = ANY_FUNCTION_PATTERN.matcher(expr);
        while (funcMatcher.find()) {
            String funcName = funcMatcher.group(1);
            if (!VALID_FUNCTION_NAMES.contains(funcName)) {
                return "Unknown function: " + funcName;
            }
        }
        return null;
    }

    // ===== Private Helpers =====

    /**
     * Rebuilds the suggestions list from variables and functions.
     */
    private void rebuildSuggestions() {
        List<String> suggestions = new ArrayList<>();

        // Add variable references
        for (String variable : availableVariables) {
            suggestions.add("${" + variable + "}");
        }

        // Add functions
        suggestions.addAll(AVAILABLE_FUNCTIONS);

        allSuggestions.setAll(suggestions);
    }

    /**
     * Updates the suggestion filter predicate.
     *
     * @param filter the filter text
     */
    private void updateSuggestionFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            filteredSuggestions.setPredicate(null);
        } else {
            String lowerFilter = filter.toLowerCase();
            filteredSuggestions.setPredicate(suggestion -> suggestion.toLowerCase().contains(lowerFilter));
        }
    }

    /**
     * Gets the help text for the expression editor.
     *
     * @return the help text
     */
    public String getHelpText() {
        return """
                Expressions allow you to create dynamic values using:

                • Variables: ${variableName}
                  Reference stored values

                • Functions: functionName(arguments)
                  Transform and combine values

                • Text: Combine with regular text
                  Example: Hello, ${name}!

                Common Functions:
                • if(condition, then, else)
                • concat(a, b, ...) - Join strings
                • upper(text) / lower(text)
                • trim(text) - Remove whitespace
                • contains(text, search)
                • now() - Current timestamp

                Press Ctrl+Space for autocomplete.
                """;
    }
}
