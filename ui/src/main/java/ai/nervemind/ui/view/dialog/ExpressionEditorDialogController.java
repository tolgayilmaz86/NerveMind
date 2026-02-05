/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import ai.nervemind.ui.component.ExpressionEditorComponent;
import ai.nervemind.ui.viewmodel.dialog.ExpressionEditorDialogViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Expression Editor dialog FXML.
 */
public class ExpressionEditorDialogController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab editorTab;
    @FXML
    private Tab helpTab;
    @FXML
    private VBox editorContent;
    @FXML
    private ScrollPane helpScrollPane;
    @FXML
    private TextArea previewArea;
    @FXML
    private Button insertButton;
    @FXML
    private Button copyButton;
    @FXML
    private Button cancelButton;

    private ExpressionEditorDialogViewModel viewModel;
    private ExpressionEditorComponent editor;
    private Stage dialogStage;
    private boolean applied = false;

    /**
     * Default constructor for FXML loading.
     */
    public ExpressionEditorDialogController() {
        // Default constructor for FXML
    }

    /**
     * Initializes the controller after FXML loading.
     * Sets up the expression editor component and binds UI elements.
     */
    @FXML
    public void initialize() {
        // Create the expression editor component
        editor = new ExpressionEditorComponent();
        editor.setPrefRowCount(8);
        editor.setPromptText("Enter your expression here... (Use Ctrl+Space for suggestions)");

        // Add editor to content
        editorContent.getChildren().add(1, editor);
        VBox.setVgrow(editor, javafx.scene.layout.Priority.ALWAYS);
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     * 
     * @param viewModel the ViewModel to use
     * @param stage     the dialog stage
     */
    public void initialize(ExpressionEditorDialogViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        bindViewModel();
    }

    private void bindViewModel() {
        // Set available variables
        editor.setAvailableVariables(viewModel.getAvailableVariables());

        // Bind expression
        editor.expressionProperty().bindBidirectional(viewModel.expressionProperty());

        // Bind preview
        previewArea.textProperty().bind(viewModel.previewTextProperty());

        // Update preview when expression changes
        editor.setOnExpressionChange(expr -> {
            // Expression property binding handles this
        });
    }

    @FXML
    private void handleInsert() {
        if (viewModel.validate()) {
            viewModel.confirm();
            applied = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCopy() {
        viewModel.copyToClipboard();
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /**
     * Set the initial expression.
     * 
     * @param expression the expression string to set
     */
    public void setExpression(String expression) {
        viewModel.setExpression(expression);
    }

    /**
     * Checks if the dialog was applied (Insert clicked).
     * 
     * @return true if applied
     */
    public boolean wasApplied() {
        return applied;
    }

    /**
     * Gets the resulting expression.
     * 
     * @return the result string
     */
    public String getResult() {
        return viewModel.getResult();
    }
}
