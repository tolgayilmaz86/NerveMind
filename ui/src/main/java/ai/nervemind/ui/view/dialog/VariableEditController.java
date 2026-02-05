/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.ui.viewmodel.dialog.VariableEditViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Variable Edit dialog FXML.
 */
public class VariableEditController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<VariableType> typeCombo;
    @FXML
    private ComboBox<VariableScope> scopeCombo;
    @FXML
    private TextArea valueArea;
    @FXML
    private Label valueLabel;
    @FXML
    private TextField descriptionField;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private VariableEditViewModel viewModel;
    private Stage dialogStage;
    private boolean applied = false;

    @FXML
    public void initialize() {
        // Setup type combo
        typeCombo.getItems().addAll(VariableType.values());
        typeCombo.setCellFactory(lv -> createTypeListCell(true));
        typeCombo.setButtonCell(createTypeListCell(false));

        // Setup scope combo
        scopeCombo.getItems().addAll(VariableScope.GLOBAL, VariableScope.WORKFLOW);
        scopeCombo.setCellFactory(lv -> createScopeListCell(true));
        scopeCombo.setButtonCell(createScopeListCell(false));
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     */
    public void initialize(VariableEditViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        bindViewModel();
    }

    private void bindViewModel() {
        // Update title
        titleLabel.setText(viewModel.isEditMode() ? "Edit Variable" : "Add Variable");
        dialogStage.setTitle(viewModel.isEditMode() ? "Edit Variable" : "Add Variable");

        // Update value label
        valueLabel.setText(viewModel.isEditMode() ? "New Value (leave empty to keep):" : "Value:");

        // Update button text
        saveButton.setText(viewModel.isEditMode() ? "Save" : "Create");

        // Bind fields
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        typeCombo.valueProperty().bindBidirectional(viewModel.typeProperty());
        scopeCombo.valueProperty().bindBidirectional(viewModel.scopeProperty());
        valueArea.textProperty().bindBidirectional(viewModel.valueProperty());
        descriptionField.textProperty().bindBidirectional(viewModel.descriptionProperty());

        // Disable scope combo in edit mode
        scopeCombo.disableProperty().bind(viewModel.scopeEditableProperty().not());

        // Bind save button disable state
        saveButton.disableProperty().bind(viewModel.formValidProperty().not());
    }

    @FXML
    private void handleSave() {
        if (viewModel.validate()) {
            viewModel.confirm();
            applied = true;
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    public boolean wasApplied() {
        return applied;
    }

    public VariableDTO getResult() {
        return viewModel.getResult();
    }

    private ListCell<VariableType> createTypeListCell(boolean showDescription) {
        return new ListCell<>() {
            @Override
            protected void updateItem(VariableType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                } else {
                    setText(showDescription
                            ? type.getDisplayName() + " - " + type.getDescription()
                            : type.getDisplayName());
                }
            }
        };
    }

    private ListCell<VariableScope> createScopeListCell(boolean showDescription) {
        return new ListCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty || scope == null) {
                    setText(null);
                } else {
                    setText(showDescription
                            ? scope.getDisplayName() + " - " + scope.getDescription()
                            : scope.getDisplayName());
                }
            }
        };
    }
}
