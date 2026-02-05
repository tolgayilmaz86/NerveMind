/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel;
import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel.WorkflowSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Workflow Settings dialog FXML.
 */
public class WorkflowSettingsController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private CheckBox activeCheckbox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private WorkflowSettingsViewModel viewModel;
    private Stage dialogStage;
    private boolean applied = false;

    @FXML
    public void initialize() {
        // Initial setup
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     */
    public void initialize(WorkflowSettingsViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        bindViewModel();

        // Focus name field
        nameField.requestFocus();
    }

    private void bindViewModel() {
        // Bind fields
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        descriptionArea.textProperty().bindBidirectional(viewModel.descriptionProperty());
        activeCheckbox.selectedProperty().bindBidirectional(viewModel.activeProperty());
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

    public WorkflowSettings getResult() {
        return viewModel.getResult();
    }
}
