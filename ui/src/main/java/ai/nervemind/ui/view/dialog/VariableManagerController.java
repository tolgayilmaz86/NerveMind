/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.ui.viewmodel.dialog.VariableManagerViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Variable Manager dialog FXML.
 * 
 * <p>
 * Binds the FXML view to the {@link VariableManagerViewModel}.
 */
public class VariableManagerController {

    private VariableManagerViewModel viewModel;
    private Stage dialogStage;

    // Header
    @FXML
    private Label headerSubtitle;

    // Toolbar
    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    @FXML
    private ComboBox<VariableScope> scopeFilterCombo;

    // Table
    @FXML
    private TableView<VariableDTO> variableTable;

    @FXML
    private TableColumn<VariableDTO, String> nameColumn;

    @FXML
    private TableColumn<VariableDTO, VariableType> typeColumn;

    @FXML
    private TableColumn<VariableDTO, VariableScope> scopeColumn;

    @FXML
    private TableColumn<VariableDTO, String> valueColumn;

    @FXML
    private TableColumn<VariableDTO, String> descriptionColumn;

    // Footer
    @FXML
    private Button closeButton;

    /**
     * Initialize the controller with its view model.
     * 
     * @param viewModel   the ViewModel
     * @param dialogStage the dialog stage
     */
    public void initialize(VariableManagerViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupHeader();
        setupToolbar();
        setupTable();
        setupBindings();

        // Initial load
        viewModel.refreshVariables();
    }

    private void setupHeader() {
        Long workflowId = viewModel.getWorkflowId();
        if (workflowId != null) {
            headerSubtitle.setText("Managing variables for workflow #" + workflowId);
        } else {
            headerSubtitle.setText("Managing global variables");
        }
    }

    private void setupToolbar() {
        // Setup scope filter combo
        scopeFilterCombo.getItems().add(null); // All scopes
        scopeFilterCombo.getItems().addAll(VariableScope.values());
        scopeFilterCombo.setValue(null);

        // Custom cell factory for scope display
        scopeFilterCombo.setButtonCell(createScopeCell());
        scopeFilterCombo.setCellFactory(lv -> createScopeCell());

        // Bind to view model
        scopeFilterCombo.valueProperty().bindBidirectional(viewModel.scopeFilterProperty());

        // Disable edit/delete when no selection
        editButton.disableProperty().bind(Bindings.not(viewModel.hasSelectionProperty()));
        deleteButton.disableProperty().bind(Bindings.not(viewModel.hasSelectionProperty()));
    }

    private ListCell<VariableScope> createScopeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty) {
                    setText(null);
                } else if (scope == null) {
                    setText("All Scopes");
                } else {
                    setText(scope.getDisplayName());
                }
            }
        };
    }

    private void setupTable() {
        // Bind table to filtered list
        variableTable.setItems(viewModel.getFilteredVariables());

        // Selection binding
        variableTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> viewModel.setSelectedVariable(newVal));

        // Double-click to edit
        variableTable.setRowFactory(tv -> {
            TableRow<VariableDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditVariable();
                }
            });
            return row;
        });

        // Column setup
        setupNameColumn();
        setupTypeColumn();
        setupScopeColumn();
        setupValueColumn();
        setupDescriptionColumn();
    }

    private void setupNameColumn() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(VariableManagerViewModel.formatVariableName(name));
                    setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-text-fill: #a855f7;");
                }
            }
        });
    }

    private void setupTypeColumn() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(VariableType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(type.getDisplayName());
                    badge.getStyleClass().add("variable-manager-dialog__type-badge");
                    String color = VariableManagerViewModel.getTypeColor(type);
                    badge.setStyle(String.format(
                            "-fx-background-color: %s20; -fx-text-fill: %s; -fx-padding: 2 6; -fx-background-radius: 3;",
                            color, color));
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupScopeColumn() {
        scopeColumn.setCellValueFactory(new PropertyValueFactory<>("scope"));
        scopeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(VariableScope scope, boolean empty) {
                super.updateItem(scope, empty);
                if (empty || scope == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(scope.getDisplayName());
                    badge.getStyleClass().add("variable-manager-dialog__scope-badge");
                    String color = VariableManagerViewModel.getScopeColor(scope);
                    badge.setStyle(String.format(
                            "-fx-background-color: %s20; -fx-text-fill: %s; -fx-padding: 2 6; -fx-background-radius: 3;",
                            color, color));
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupValueColumn() {
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(col -> new ValueTableCell());
    }

    private void setupDescriptionColumn() {
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String description, boolean empty) {
                super.updateItem(description, empty);
                if (empty || description == null) {
                    setText(null);
                } else {
                    setText(description);
                    getStyleClass().add("variable-manager-dialog__description");
                }
            }
        });
    }

    private void setupBindings() {
        // Could bind loading indicator here if needed
    }

    // ===== Actions =====

    @FXML
    private void onAddVariable() {
        DialogFactory.showVariableEdit(dialogStage, null, viewModel.getWorkflowId())
                .ifPresent(variable -> {
                    if (viewModel.addVariable(variable)) {
                        // Success - table auto-updates
                    }
                });
    }

    @FXML
    private void onEditVariable() {
        VariableDTO selected = viewModel.getSelectedVariable();
        if (selected == null) {
            return;
        }

        DialogFactory.showVariableEdit(dialogStage, selected, viewModel.getWorkflowId())
                .ifPresent(variable -> {
                    if (viewModel.updateVariable(variable)) {
                        // Success - table auto-updates
                    }
                });
    }

    @FXML
    private void onDeleteVariable() {
        VariableDTO selected = viewModel.getSelectedVariable();
        if (selected == null) {
            return;
        }

        // Confirmation is handled by caller or could add confirmation dialog here
        boolean confirmed = ConfirmationDialogHelper.showConfirmation(
                "Delete Variable",
                "Delete \"" + selected.name() + "\"?",
                "Any workflows using this variable may fail.");

        if (confirmed) {
            viewModel.deleteSelectedVariable();
        }
    }

    @FXML
    private void onRefresh() {
        viewModel.refreshVariables();
    }

    @FXML
    private void onClose() {
        dialogStage.close();
    }

    // ===== Inner Classes =====

    /**
     * Table cell for displaying variable values with truncation.
     */
    private static class ValueTableCell extends TableCell<VariableDTO, String> {
        private static final int MAX_DISPLAY_LENGTH = 50;
        private static final int TRUNCATE_LENGTH = 47;

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText(null);
            } else {
                // Check if this is a secret type
                VariableDTO variable = getTableRow() != null ? getTableRow().getItem() : null;
                if (variable != null && variable.type() == VariableType.SECRET) {
                    setText("••••••••");
                    setStyle("-fx-text-fill: #737373;");
                } else {
                    String display = value.length() > MAX_DISPLAY_LENGTH
                            ? value.substring(0, TRUNCATE_LENGTH) + "..."
                            : value;
                    setText(display);
                    setStyle("-fx-text-fill: #a3a3a3;");
                }
            }
        }
    }

    /**
     * Simple helper for confirmation dialogs.
     */
    private static class ConfirmationDialogHelper {
        static boolean showConfirmation(String title, String header, String content) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            return alert.showAndWait()
                    .filter(response -> response == javafx.scene.control.ButtonType.OK)
                    .isPresent();
        }
    }
}
