/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignK;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.ui.viewmodel.dialog.CredentialManagerViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the Credential Manager dialog FXML.
 * 
 * <p>
 * Binds the FXML view to the {@link CredentialManagerViewModel}.
 */
public class CredentialManagerController {

    /**
     * Default constructor for CredentialManagerController.
     */
    public CredentialManagerController() {
        // Required by FXMLLoader
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private CredentialManagerViewModel viewModel;
    private Stage dialogStage;

    // Toolbar
    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    // Table
    @FXML
    private TableView<CredentialDTO> credentialTable;

    @FXML
    private TableColumn<CredentialDTO, String> nameColumn;

    @FXML
    private TableColumn<CredentialDTO, CredentialType> typeColumn;

    @FXML
    private TableColumn<CredentialDTO, String> createdColumn;

    @FXML
    private TableColumn<CredentialDTO, String> updatedColumn;

    // Footer
    @FXML
    private Button closeButton;

    /**
     * Initialize the controller with its view model.
     * 
     * @param viewModel   the ViewModel
     * @param dialogStage the dialog stage
     */
    public void initialize(CredentialManagerViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupToolbar();
        setupTable();

        // Initial load
        viewModel.refreshCredentials();
    }

    private void setupToolbar() {
        // Disable edit/delete when no selection
        editButton.disableProperty().bind(Bindings.not(viewModel.hasSelectionProperty()));
        deleteButton.disableProperty().bind(Bindings.not(viewModel.hasSelectionProperty()));
    }

    private void setupTable() {
        // Bind table to credentials list
        credentialTable.setItems(viewModel.getCredentials());

        // Selection binding
        credentialTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> viewModel.setSelectedCredential(newVal));

        // Double-click to edit
        credentialTable.setRowFactory(tv -> {
            TableRow<CredentialDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEditCredential();
                }
            });
            return row;
        });

        // Setup columns
        setupNameColumn();
        setupTypeColumn();
        setupCreatedColumn();
        setupUpdatedColumn();
    }

    private void setupNameColumn() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                } else {
                    setText(name);
                    getStyleClass().add("credential-manager-dialog__name");
                }
            }
        });
    }

    private void setupTypeColumn() {
        typeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().type()));
        typeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CredentialType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);
                    FontIcon icon = getIconForType(type);
                    Label label = new Label(type.getDisplayName());
                    label.getStyleClass().add("credential-manager-dialog__type-label");
                    box.getChildren().addAll(icon, label);
                    setGraphic(box);
                }
            }
        });
    }

    private void setupCreatedColumn() {
        createdColumn.setCellValueFactory(data -> {
            if (data.getValue().createdAt() != null) {
                return new SimpleStringProperty(DATE_FORMATTER.format(data.getValue().createdAt()));
            }
            return new SimpleStringProperty("-");
        });
    }

    private void setupUpdatedColumn() {
        updatedColumn.setCellValueFactory(data -> {
            if (data.getValue().updatedAt() != null) {
                return new SimpleStringProperty(DATE_FORMATTER.format(data.getValue().updatedAt()));
            }
            return new SimpleStringProperty("-");
        });
    }

    private FontIcon getIconForType(CredentialType type) {
        FontIcon icon = switch (type) {
            case API_KEY -> FontIcon.of(MaterialDesignK.KEY, 14);
            case HTTP_BASIC -> FontIcon.of(MaterialDesignA.ACCOUNT_KEY, 14);
            case HTTP_BEARER -> FontIcon.of(MaterialDesignS.SHIELD_KEY, 14);
            case OAUTH2 -> FontIcon.of(MaterialDesignS.SECURITY, 14);
            case CUSTOM_HEADER -> FontIcon.of(MaterialDesignC.CODE_BRACES, 14);
        };
        icon.getStyleClass().add("credential-manager-dialog__type-icon");
        return icon;
    }

    // ===== Actions =====

    @FXML
    private void onAddCredential() {
        DialogFactory.showCredentialEdit(dialogStage, null)
                .ifPresent(result -> viewModel.addCredential(result.dto(), result.data()));
    }

    @FXML
    private void onEditCredential() {
        CredentialDTO selected = viewModel.getSelectedCredential();
        if (selected == null) {
            return;
        }

        DialogFactory.showCredentialEdit(dialogStage, selected)
                .ifPresent(result -> viewModel.updateCredential(result.dto(), result.data()));
    }

    @FXML
    private void onDeleteCredential() {
        CredentialDTO selected = viewModel.getSelectedCredential();
        if (selected == null) {
            return;
        }

        boolean confirmed = ConfirmationDialogHelper.showConfirmation(
                "Delete Credential",
                "Delete \"" + selected.name() + "\"?",
                "This action cannot be undone. Any workflows using this credential will fail.");

        if (confirmed) {
            viewModel.deleteSelectedCredential();
        }
    }

    @FXML
    private void onRefresh() {
        viewModel.refreshCredentials();
    }

    @FXML
    private void onClose() {
        dialogStage.close();
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
