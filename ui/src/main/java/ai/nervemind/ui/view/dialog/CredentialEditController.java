/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel.CredentialEditResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the Credential Edit dialog FXML.
 */
public class CredentialEditController {

    /**
     * Default constructor for CredentialEditController.
     */
    public CredentialEditController() {
        // Required by FXMLLoader
    }

    @FXML
    private Label titleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<CredentialType> typeCombo;
    @FXML
    private PasswordField dataField;
    @FXML
    private TextField dataVisibleField;
    @FXML
    private CheckBox showDataCheckbox;
    @FXML
    private Label dataLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private CredentialEditViewModel viewModel;
    private Stage dialogStage;
    private boolean applied = false;

    /**
     * Called by FXMLLoader after injection.
     */
    @FXML
    public void initialize() {
        // Setup type combo
        typeCombo.getItems().addAll(CredentialType.values());
        typeCombo.setCellFactory(lv -> createTypeListCell(true));
        typeCombo.setButtonCell(createTypeListCell(false));

        // Setup show/hide password toggle
        showDataCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            dataField.setVisible(!newVal);
            dataField.setManaged(!newVal);
            dataVisibleField.setVisible(newVal);
            dataVisibleField.setManaged(newVal);
        });

        // Bind visible field to password field
        dataVisibleField.textProperty().bindBidirectional(dataField.textProperty());
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     * 
     * @param viewModel the ViewModel to bind to
     * @param stage     the dialog stage
     */
    public void initialize(CredentialEditViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        bindViewModel();
    }

    private void bindViewModel() {
        // Update title
        titleLabel.setText(viewModel.isEditMode() ? "Edit Credential" : "Add Credential");
        dialogStage.setTitle(viewModel.isEditMode() ? "Edit Credential" : "Add Credential");

        // Update data label
        dataLabel.setText(viewModel.isEditMode() ? "New Data (leave empty to keep):" : "Credential Data:");

        // Update button text
        saveButton.setText(viewModel.isEditMode() ? "Save" : "Create");

        // Bind fields
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        typeCombo.valueProperty().bindBidirectional(viewModel.typeProperty());
        dataField.textProperty().bindBidirectional(viewModel.dataProperty());
        showDataCheckbox.selectedProperty().bindBidirectional(viewModel.showDataProperty());

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

    /**
     * Checks if the changes were applied (save button clicked).
     * 
     * @return true if the save button was clicked
     */
    public boolean wasApplied() {
        return applied;
    }

    /**
     * Gets the resulting credential data from the dialog.
     * 
     * @return the resulting credential data
     */
    public CredentialEditResult getResult() {
        return viewModel.getResult();
    }

    private ListCell<CredentialType> createTypeListCell(boolean showDescription) {
        return new ListCell<>() {
            @Override
            protected void updateItem(CredentialType type, boolean empty) {
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
}
