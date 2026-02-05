/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Expression Editor dialog.
 * 
 * <p>
 * Manages expression editing state and preview generation.
 */
public class ExpressionEditorDialogViewModel extends BaseDialogViewModel<String> {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    // Expression state
    private final StringProperty expression = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper previewText = new ReadOnlyStringWrapper("");
    private final ObservableList<String> availableVariables = FXCollections.observableArrayList();

    /**
     * Creates a new ExpressionEditorDialogViewModel.
     */
    public ExpressionEditorDialogViewModel() {
        this(List.of());
    }

    /**
     * Creates a new ExpressionEditorDialogViewModel with available variables.
     * 
     * @param variables list of variable names for autocomplete
     */
    public ExpressionEditorDialogViewModel(List<String> variables) {
        if (variables != null) {
            availableVariables.setAll(variables);
        }

        // Update preview when expression changes
        expression.addListener((obs, oldVal, newVal) -> updatePreview());
    }

    // ===== Properties =====

    /**
     * Gets the expression property.
     * 
     * @return the expression property
     */
    public StringProperty expressionProperty() {
        return expression;
    }

    /**
     * Gets the current expression string.
     * 
     * @return the expression string
     */
    public String getExpression() {
        return expression.get();
    }

    /**
     * Sets the expression string.
     * 
     * @param value the expression string to set
     */
    public void setExpression(String value) {
        expression.set(value);
    }

    /**
     * Gets the read-only preview text property.
     * 
     * @return the preview text property
     */
    public ReadOnlyStringProperty previewTextProperty() {
        return previewText.getReadOnlyProperty();
    }

    /**
     * Gets the current preview text.
     * 
     * @return the preview text
     */
    public String getPreviewText() {
        return previewText.get();
    }

    /**
     * Gets the list of available variable names for autocomplete.
     * 
     * @return the list of available variables
     */
    public ObservableList<String> getAvailableVariables() {
        return availableVariables;
    }

    /**
     * Sets the list of available variable names.
     * 
     * @param variables the list of variable names
     */
    public void setAvailableVariables(List<String> variables) {
        availableVariables.setAll(variables != null ? variables : List.of());
    }

    // ===== Actions =====

    /**
     * Copy the current expression to clipboard.
     */
    public void copyToClipboard() {
        String expr = expression.get();
        if (expr != null && !expr.isBlank()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(expr);
            clipboard.setContent(content);
        }
    }

    // ===== Validation =====

    @Override
    public boolean validate() {
        // Expression can be empty
        setValid(true);
        return true;
    }

    @Override
    protected void buildResult() {
        setResult(expression.get());
    }

    // ===== Private Methods =====

    private void updatePreview() {
        String expr = expression.get();
        if (expr == null || expr.isBlank()) {
            previewText.set("");
            return;
        }

        StringBuilder preview = new StringBuilder();
        preview.append("Expression: ").append(expr).append("\n");

        // Check for variables used
        Matcher matcher = VAR_PATTERN.matcher(expr);
        List<String> varsUsed = new ArrayList<>();
        while (matcher.find()) {
            varsUsed.add(matcher.group(1));
        }

        if (!varsUsed.isEmpty()) {
            preview.append("Variables used: ").append(String.join(", ", varsUsed));
        }

        previewText.set(preview.toString());
    }
}
