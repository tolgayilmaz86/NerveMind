/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.editor;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import ai.nervemind.common.exception.UiInitializationException;
import ai.nervemind.ui.viewmodel.editor.ExpressionEditorViewModel;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Controller/Component for the ExpressionEditor.
 *
 * <p>
 * This is a custom component that extends VBox and loads its FXML using the
 * fx:root pattern.
 * It provides a rich expression editing experience with:
 * <ul>
 * <li>Toolbar with Variable, Function, and Help buttons</li>
 * <li>TextArea for expression input with syntax validation</li>
 * <li>Autocomplete suggestions via Ctrl+Space</li>
 * <li>Real-time validation feedback</li>
 * </ul>
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * ExpressionEditorComponent editor = new ExpressionEditorComponent();
 * editor.setAvailableVariables(List.of("name", "age", "city"));
 * editor.setExpression("Hello, ${name}!");
 * editor.setOnExpressionChange(expr -> System.out.println("Changed: " + expr));
 * </pre>
 *
 * <p>
 * This component delegates business logic to {@link ExpressionEditorViewModel}.
 */
public class ExpressionEditorComponent extends VBox {

    private final ExpressionEditorViewModel viewModel;

    // FXML components
    @FXML
    private Button variableButton;

    @FXML
    private Button functionButton;

    @FXML
    private Button helpButton;

    @FXML
    private TextArea expressionArea;

    @FXML
    private Label validationLabel;

    // Suggestions popup (created programmatically)
    private final Popup suggestionPopup;
    private final ListView<String> suggestionList;

    // Callback
    private Consumer<String> onExpressionChange;

    /**
     * Creates a new ExpressionEditorComponent.
     */
    public ExpressionEditorComponent() {
        this.viewModel = new ExpressionEditorViewModel();

        // Create suggestions popup
        this.suggestionPopup = new Popup();
        this.suggestionList = new ListView<>();

        // Load FXML
        loadFxml();

        // Setup after FXML is loaded
        setupBindings();
        setupSuggestions();
    }

    /**
     * Creates a new ExpressionEditorComponent with an existing ViewModel.
     *
     * @param viewModel the ViewModel to use
     */
    public ExpressionEditorComponent(ExpressionEditorViewModel viewModel) {
        this.viewModel = viewModel;

        // Create suggestions popup
        this.suggestionPopup = new Popup();
        this.suggestionList = new ListView<>();

        // Load FXML
        loadFxml();

        // Setup after FXML is loaded
        setupBindings();
        setupSuggestions();
    }

    /**
     * Loads the FXML file and sets this component as both root and controller.
     */
    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ai/nervemind/ui/view/editor/ExpressionEditor.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new UiInitializationException("Failed to load ExpressionEditor FXML", "ExpressionEditor", e);
        }
    }

    /**
     * Sets up bindings between view and ViewModel.
     */
    private void setupBindings() {
        // Bind expression text bidirectionally
        expressionArea.textProperty().bindBidirectional(viewModel.expressionProperty());

        // Bind prompt text
        expressionArea.promptTextProperty().bind(viewModel.promptTextProperty());

        // Bind pref row count
        expressionArea.prefRowCountProperty().bind(viewModel.prefRowCountProperty());

        // Bind validation display
        validationLabel.textProperty().bind(viewModel.validationMessageProperty());
        validationLabel.visibleProperty().bind(viewModel.showValidationProperty());
        validationLabel.managedProperty().bind(viewModel.showValidationProperty());

        // Update validation style class based on validity
        viewModel.validProperty().addListener((obs, oldVal, newVal) -> updateValidationStyle(newVal));

        // Track caret position
        expressionArea.caretPositionProperty()
                .addListener((obs, oldVal, newVal) -> viewModel.setCaretPosition(newVal.intValue()));

        // Listen for expression changes to notify callback
        viewModel.expressionProperty().addListener((obs, oldVal, newVal) -> {
            if (onExpressionChange != null) {
                onExpressionChange.accept(newVal);
            }
        });

        // Listen for suggestions visibility
        viewModel.suggestionsVisibleProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal)) {
                showSuggestionsPopup();
            } else {
                suggestionPopup.hide();
            }
        });
    }

    /**
     * Updates the validation style based on validity.
     */
    private void updateValidationStyle(boolean isValid) {
        validationLabel.getStyleClass().removeAll(
                "expression-editor__validation--valid",
                "expression-editor__validation--invalid");
        validationLabel.getStyleClass().add(
                isValid ? "expression-editor__validation--valid" : "expression-editor__validation--invalid");

        expressionArea.getStyleClass().remove("expression-editor__input--invalid");
        if (!isValid) {
            expressionArea.getStyleClass().add("expression-editor__input--invalid");
        }
    }

    /**
     * Sets up the suggestions popup and list.
     */
    private void setupSuggestions() {
        suggestionList.setPrefWidth(300);
        suggestionList.setPrefHeight(200);
        suggestionList.getStyleClass().add("expression-editor__suggestions");

        // Bind suggestions list to ViewModel
        suggestionList.setItems(viewModel.getFilteredSuggestions());

        // Custom cell factory for styling
        suggestionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll(
                            "expression-editor__suggestion--variable",
                            "expression-editor__suggestion--function");
                } else {
                    setText(item);
                    getStyleClass().removeAll(
                            "expression-editor__suggestion--variable",
                            "expression-editor__suggestion--function");
                    if (item.startsWith("${")) {
                        getStyleClass().add("expression-editor__suggestion--variable");
                    } else {
                        getStyleClass().add("expression-editor__suggestion--function");
                    }
                }
            }
        });

        // Handle selection
        suggestionList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                insertSelectedSuggestion();
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                insertSelectedSuggestion();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                viewModel.hideSuggestions();
            }
        });

        // Bind selection to ViewModel
        suggestionList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> viewModel.setSelectedSuggestion(newVal));

        suggestionPopup.getContent().add(suggestionList);
        suggestionPopup.setAutoHide(true);
    }

    /**
     * Shows the suggestions popup below the expression area.
     */
    private void showSuggestionsPopup() {
        if (!viewModel.getAllSuggestions().isEmpty() && getScene() != null) {
            Bounds bounds = expressionArea.localToScreen(expressionArea.getBoundsInLocal());
            if (bounds != null) {
                suggestionPopup.show(expressionArea, bounds.getMinX(), bounds.getMaxY());
            }
        }
    }

    /**
     * Inserts the selected suggestion into the expression.
     */
    private void insertSelectedSuggestion() {
        String newExpression = viewModel.insertSelectedSuggestion();
        if (newExpression != null) {
            expressionArea.setText(newExpression);
            expressionArea.requestFocus();
        }
    }

    // ===== FXML Event Handlers =====

    @FXML
    private void onInsertVariable() {
        List<String> variables = viewModel.getAvailableVariables();
        if (variables.isEmpty()) {
            showInfo("No Variables",
                    "No variables are defined. Use the Variable Manager to create variables.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(variables.getFirst(), variables);
        dialog.setTitle("Insert Variable");
        dialog.setHeaderText("Select a variable to insert");
        dialog.setContentText("Variable:");

        dialog.showAndWait().ifPresent(variable -> {
            String newExpression = viewModel.insertVariable(variable);
            expressionArea.setText(newExpression);
            expressionArea.requestFocus();
        });
    }

    @FXML
    private void onInsertFunction() {
        List<String> functions = viewModel.getAvailableFunctions();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(functions.getFirst(), functions);
        dialog.setTitle("Insert Function");
        dialog.setHeaderText("Select a function to insert");
        dialog.setContentText("Function:");

        dialog.showAndWait().ifPresent(func -> {
            String newExpression = viewModel.insertFunction(func);
            expressionArea.setText(newExpression);
            expressionArea.requestFocus();
        });
    }

    @FXML
    private void onShowHelp() {
        Alert help = new Alert(Alert.AlertType.INFORMATION);
        help.setTitle("Expression Editor Help");
        help.setHeaderText("Writing Expressions");
        help.setContentText(viewModel.getHelpText());
        help.showAndWait();
    }

    @FXML
    private void onKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
            viewModel.showSuggestions();
            event.consume();
        }
    }

    // ===== Public API =====

    /**
     * Gets the expression text.
     *
     * @return the expression
     */
    public String getExpression() {
        return viewModel.getExpression();
    }

    /**
     * Sets the expression text.
     *
     * @param expression the expression
     */
    public void setExpression(String expression) {
        viewModel.setExpression(expression);
    }

    /**
     * Gets the expression property for binding.
     *
     * @return the expression property
     */
    public StringProperty expressionProperty() {
        return viewModel.expressionProperty();
    }

    /**
     * Returns whether the current expression is valid.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return viewModel.isValid();
    }

    /**
     * Gets the valid property for binding.
     *
     * @return the valid property
     */
    public ReadOnlyBooleanProperty validProperty() {
        return viewModel.validProperty();
    }

    /**
     * Sets the available variables for autocomplete and validation.
     *
     * @param variables the list of variable names
     */
    public void setAvailableVariables(List<String> variables) {
        viewModel.setAvailableVariables(variables);
    }

    /**
     * Sets the callback for expression changes.
     *
     * @param handler the change handler
     */
    public void setOnExpressionChange(Consumer<String> handler) {
        this.onExpressionChange = handler;
    }

    /**
     * Sets the prompt text for the expression input.
     *
     * @param text the prompt text
     */
    public void setPromptText(String text) {
        viewModel.setPromptText(text);
    }

    /**
     * Sets the preferred row count for the expression input.
     *
     * @param count the row count
     */
    public void setPrefRowCount(int count) {
        viewModel.setPrefRowCount(count);
    }

    /**
     * Gets the underlying ViewModel (for testing).
     *
     * @return the ViewModel
     */
    public ExpressionEditorViewModel getViewModel() {
        return viewModel;
    }

    // ===== Helper Methods =====

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
