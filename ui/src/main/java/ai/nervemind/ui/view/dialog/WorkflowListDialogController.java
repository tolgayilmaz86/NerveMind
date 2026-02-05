/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.util.List;
import java.util.function.Consumer;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.ui.viewmodel.dialog.WorkflowListDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

/**
 * Controller for the Workflow List dialog FXML.
 * 
 * <p>
 * Handles UI interactions and binds to the WorkflowListDialogViewModel.
 */
public class WorkflowListDialogController {

    @FXML
    private ListView<WorkflowDTO> workflowListView;
    @FXML
    private Label detailsLabel;
    @FXML
    private Button deleteButton;

    private WorkflowListDialogViewModel viewModel;
    private Consumer<WorkflowDTO> onDeleteCallback;
    private final BooleanProperty showDeleteButton = new SimpleBooleanProperty(false);

    /**
     * Initialize the controller. Called by FXMLLoader after injection.
     */
    @FXML
    public void initialize() {
        // Setup list cell factory
        workflowListView.setCellFactory(lv -> new WorkflowListCell());
    }

    /**
     * Set the ViewModel and bind properties.
     * 
     * @param viewModel the ViewModel to use
     */
    public void setViewModel(WorkflowListDialogViewModel viewModel) {
        this.viewModel = viewModel;
        bindViewModel();
    }

    /**
     * Initialize with workflow list (convenience method).
     * 
     * @param workflows the list of workflows
     */
    public void initWithWorkflows(List<WorkflowDTO> workflows) {
        setViewModel(new WorkflowListDialogViewModel(workflows));
    }

    /**
     * Set the delete callback and show the delete button.
     * 
     * @param callback the callback to invoke when delete is confirmed
     */
    public void setOnDeleteCallback(Consumer<WorkflowDTO> callback) {
        this.onDeleteCallback = callback;
        showDeleteButton.set(callback != null);
        if (viewModel != null) {
            viewModel.setShowDeleteButton(callback != null);
        }
    }

    /**
     * Property for controlling delete button visibility.
     */
    public BooleanProperty showDeleteButtonProperty() {
        return showDeleteButton;
    }

    public boolean isShowDeleteButton() {
        return showDeleteButton.get();
    }

    /**
     * Get the ViewModel.
     */
    public WorkflowListDialogViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Get the selected workflow.
     */
    public WorkflowDTO getSelectedWorkflow() {
        return viewModel != null ? viewModel.getSelectedWorkflow() : null;
    }

    /**
     * Bind UI components to ViewModel properties.
     */
    private void bindViewModel() {
        if (viewModel == null) {
            return;
        }

        // Bind list items
        workflowListView.setItems(viewModel.getWorkflows());

        // Bind selection
        workflowListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> viewModel.setSelectedWorkflow(newVal));

        // Bind details text
        detailsLabel.textProperty().bind(viewModel.detailsTextProperty());

        // Bind delete button state
        if (deleteButton != null) {
            deleteButton.disableProperty().bind(viewModel.hasSelectionProperty().not());
            deleteButton.visibleProperty().bind(viewModel.showDeleteButtonProperty());
            deleteButton.managedProperty().bind(viewModel.showDeleteButtonProperty());
        }

        // Handle double-click
        workflowListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && viewModel.hasSelection()) {
                viewModel.confirm();
            }
        });

        // Show placeholder if empty
        if (viewModel.isEmpty()) {
            workflowListView.setPlaceholder(
                    new Label("No workflows found.\nCreate a new workflow to get started."));
        }
    }

    /**
     * Handle delete button click.
     */
    @FXML
    private void handleDelete() {
        WorkflowDTO selected = viewModel.getSelectedWorkflow();
        if (selected == null) {
            return;
        }

        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Workflow");
        confirm.setHeaderText("Delete workflow: " + selected.name() + "?");
        confirm.setContentText("This action cannot be undone. All workflow data will be permanently deleted.");

        confirm.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .ifPresent(buttonType -> {
                    WorkflowDTO deleted = viewModel.deleteSelectedWorkflow();
                    if (onDeleteCallback != null && deleted != null) {
                        onDeleteCallback.accept(deleted);
                    }

                    // Update placeholder if list is now empty
                    if (viewModel.isEmpty()) {
                        workflowListView.setPlaceholder(
                                new Label("No workflows found.\nCreate a new workflow to get started."));
                    }
                });
    }

    /**
     * Custom list cell for displaying workflow information.
     */
    private static class WorkflowListCell extends ListCell<WorkflowDTO> {
        @Override
        protected void updateItem(WorkflowDTO item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(createCellContent(item));
            }
        }

        private VBox createCellContent(WorkflowDTO item) {
            VBox content = new VBox(2);

            Label nameLabel = new Label(WorkflowListDialogViewModel.getDisplayName(item));
            nameLabel.getStyleClass().add("workflow-list-dialog__cell-name");

            Label infoLabel = new Label(WorkflowListDialogViewModel.getInfoText(item));
            infoLabel.getStyleClass().add("workflow-list-dialog__cell-info");

            content.getChildren().addAll(nameLabel, infoLabel);
            return content;
        }
    }
}
