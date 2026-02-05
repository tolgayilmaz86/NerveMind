/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.CredentialServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.common.service.VariableServiceInterface;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeExecutionState;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel.CredentialEditResult;
import ai.nervemind.ui.viewmodel.dialog.CredentialManagerViewModel;
import ai.nervemind.ui.viewmodel.dialog.ExpressionEditorDialogViewModel;
import ai.nervemind.ui.viewmodel.dialog.GuideViewViewModel;
import ai.nervemind.ui.viewmodel.dialog.IconPickerViewModel;
import ai.nervemind.ui.viewmodel.dialog.NodeDebugViewModel;
import ai.nervemind.ui.viewmodel.dialog.PluginManagerViewModel;
import ai.nervemind.ui.viewmodel.dialog.SamplesBrowserViewModel;
import ai.nervemind.ui.viewmodel.dialog.SettingsDialogViewModel;
import ai.nervemind.ui.viewmodel.dialog.VariableEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.VariableManagerViewModel;
import ai.nervemind.ui.viewmodel.dialog.WorkflowListDialogViewModel;
import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel;
import ai.nervemind.ui.viewmodel.dialog.WorkflowSettingsViewModel.WorkflowSettings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Factory for creating FXML-based dialogs.
 * 
 * <p>
 * Provides a centralized way to create dialogs with proper styling and
 * configuration. All dialogs follow the MVVM pattern with FXML views.
 */
public final class DialogFactory {

    private static final Logger LOGGER = Logger.getLogger(DialogFactory.class.getName());
    private static final String FXML_PATH = "/ai/nervemind/ui/view/dialog/";

    private DialogFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Show the About dialog.
     * 
     * @param owner the owner window (can be null)
     */
    public static void showAboutDialog(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "AboutDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("About NerveMind", owner, false);
            dialogStage.setScene(new Scene(root));

            AboutDialogController controller = loader.getController();
            controller.setOnCloseCallback(dialogStage::close);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load AboutDialog.fxml", e);
        }
    }

    /**
     * Show the Settings dialog.
     * 
     * @param owner           the owner window (can be null)
     * @param settingsService the settings service
     * @param pluginService   the plugin service (for plugin enable/disable
     *                        checkboxes)
     */
    public static void showSettingsDialog(Window owner, SettingsServiceInterface settingsService,
            ai.nervemind.common.service.PluginServiceInterface pluginService) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "SettingsDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Settings", owner, true);
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);
            dialogStage.setScene(new Scene(root));

            SettingsDialogController controller = loader.getController();
            SettingsDialogViewModel viewModel = new SettingsDialogViewModel(settingsService, pluginService);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load SettingsDialog.fxml", e);
        }
    }

    /**
     * Show the Variable Manager dialog.
     * 
     * @param owner           the owner window (can be null)
     * @param variableService the variable service
     * @param workflowId      the workflow ID (null for global variables)
     */
    public static void showVariableManager(Window owner, VariableServiceInterface variableService, Long workflowId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "VariableManagerDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Variable Manager", owner, true);
            dialogStage.setMinWidth(700);
            dialogStage.setMinHeight(500);
            dialogStage.setScene(new Scene(root));

            VariableManagerController controller = loader.getController();
            VariableManagerViewModel viewModel = new VariableManagerViewModel(variableService, workflowId);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load VariableManagerDialog.fxml", e);
        }
    }

    /**
     * Show the Credential Manager dialog.
     * 
     * @param owner             the owner window (can be null)
     * @param credentialService the credential service
     */
    public static void showCredentialManager(Window owner, CredentialServiceInterface credentialService) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "CredentialManagerDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Credential Manager", owner, true, false);
            dialogStage.setMinWidth(700);
            dialogStage.setMinHeight(500);
            dialogStage.setScene(new Scene(root));

            CredentialManagerController controller = loader.getController();
            CredentialManagerViewModel viewModel = new CredentialManagerViewModel(credentialService);
            controller.initialize(viewModel, dialogStage);

            dialogStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load CredentialManagerDialog.fxml", e);
        }
    }

    /**
     * Show the Workflow List dialog.
     * 
     * @param owner     the owner window (can be null)
     * @param workflows the list of workflows to display
     * @return the selected workflow, or empty if cancelled
     */
    public static Optional<WorkflowDTO> showWorkflowListDialog(Window owner, List<WorkflowDTO> workflows) {
        return showWorkflowListDialog(owner, workflows, null);
    }

    /**
     * Show the Workflow List dialog with delete support.
     * 
     * @param owner          the owner window (can be null)
     * @param workflows      the list of workflows to display
     * @param onDeleteAction callback when a workflow should be deleted
     * @return the selected workflow, or empty if cancelled
     */
    public static Optional<WorkflowDTO> showWorkflowListDialog(
            Window owner,
            List<WorkflowDTO> workflows,
            Consumer<WorkflowDTO> onDeleteAction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "WorkflowListDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Open Workflow", owner, true);
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(400);
            dialogStage.setScene(new Scene(root));

            WorkflowListDialogController controller = loader.getController();
            WorkflowListDialogViewModel viewModel = new WorkflowListDialogViewModel(workflows);

            // Set up delete callback
            if (onDeleteAction != null) {
                viewModel.setOnDeleteRequested(onDeleteAction);
            }

            controller.setViewModel(viewModel);

            // Handle confirmation - use holder class to capture result
            class ResultHolder {
                Optional<WorkflowDTO> value = Optional.empty();
            }
            final ResultHolder result = new ResultHolder();

            viewModel.confirmedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal) && viewModel.getSelectedWorkflow() != null) {
                    result.value = Optional.of(viewModel.getSelectedWorkflow());
                    dialogStage.close();
                }
            });

            dialogStage.showAndWait();
            return result.value;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load WorkflowListDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Show the Samples Browser dialog.
     * 
     * @param owner          the owner window (can be null)
     * @param samples        list of available samples
     * @param categories     list of unique categories
     * @param languages      list of unique languages
     * @param onGuideRequest callback when user wants to view a guide
     * @return the selected sample workflow, or empty if cancelled
     */
    public static Optional<SampleWorkflow> showSamplesBrowser(
            Window owner,
            List<SampleWorkflow> samples,
            List<String> categories,
            List<String> languages,
            Consumer<SampleWorkflow> onGuideRequest) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "SamplesBrowserDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("📚 Sample Workflows & Guides", owner, true);
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(500);
            dialogStage.setScene(new Scene(root));

            SamplesBrowserController controller = loader.getController();
            SamplesBrowserViewModel viewModel = new SamplesBrowserViewModel(samples, categories, languages);

            // Set up guide request callback
            viewModel.setOnViewGuide(sample -> {
                if (onGuideRequest != null) {
                    onGuideRequest.accept(sample);
                }
            });

            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            return Optional.ofNullable(controller.getResult());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load SamplesBrowserDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Show the Plugin Manager dialog.
     * 
     * @param owner         the owner window (can be null)
     * @param pluginService the plugin service
     */
    public static void showPluginManager(Window owner, PluginServiceInterface pluginService) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "PluginManagerDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Plugin Manager", owner, true, false);
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(500);
            dialogStage.setScene(new Scene(root));

            PluginManagerController controller = loader.getController();
            PluginManagerViewModel viewModel = new PluginManagerViewModel(pluginService);
            controller.initialize(viewModel, dialogStage);

            dialogStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load PluginManagerDialog.fxml", e);
        }
    }

    /**
     * Show the Icon Picker dialog.
     * 
     * @param owner       the owner window (can be null)
     * @param currentIcon the currently selected icon code, or null
     * @return the selected icon code, or empty if cancelled (null result means
     *         reset to default)
     */
    public static Optional<String> showIconPicker(Window owner, String currentIcon) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "IconPickerDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Select Icon", owner, false);
            dialogStage.setScene(new Scene(root));

            IconPickerController controller = loader.getController();
            IconPickerViewModel viewModel = new IconPickerViewModel(currentIcon);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            if (controller.wasApplied()) {
                return Optional.ofNullable(controller.getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load IconPickerDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Create a standard dialog stage with common settings.
     */
    private static Stage createDialogStage(String title, Window owner, boolean resizable) {
        return createDialogStage(title, owner, resizable, true);
    }

    /**
     * Create a standard dialog stage with common settings.
     * 
     * @param title     the dialog title
     * @param owner     the owner window (can be null)
     * @param resizable whether the dialog is resizable
     * @param modal     whether the dialog should be modal (block parent window)
     */
    private static Stage createDialogStage(String title, Window owner, boolean resizable, boolean modal) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);

        if (modal) {
            dialogStage.initModality(Modality.APPLICATION_MODAL);
        } else {
            dialogStage.initModality(Modality.NONE);
        }

        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setResizable(resizable);

        if (owner != null) {
            dialogStage.initOwner(owner);

            // For non-modal dialogs, close when owner closes
            if (!modal) {
                owner.addEventHandler(javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                    if (dialogStage.isShowing()) {
                        dialogStage.close();
                    }
                });
            }
        }

        // Set icon
        try {
            Image icon = new Image(Objects.requireNonNull(
                    DialogFactory.class.getResourceAsStream("/images/icon.png")));
            dialogStage.getIcons().add(icon);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Icon not found", e);
        }

        return dialogStage;
    }

    // ========== Edit Dialogs ==========

    /**
     * Show the Credential Edit dialog.
     * 
     * @param owner    the owner window (can be null)
     * @param existing the existing credential to edit, or null for a new credential
     * @return the credential edit result, or empty if cancelled
     */
    public static Optional<CredentialEditResult> showCredentialEdit(Window owner, CredentialDTO existing) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "CredentialEditDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage(existing != null ? "Edit Credential" : "Add Credential", owner,
                    false);
            dialogStage.setScene(new Scene(root));

            CredentialEditController controller = loader.getController();
            CredentialEditViewModel viewModel = new CredentialEditViewModel(existing);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            if (controller.wasApplied()) {
                return Optional.ofNullable(controller.getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load CredentialEditDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Show the Variable Edit dialog.
     * 
     * @param owner      the owner window (can be null)
     * @param existing   the existing variable to edit, or null for a new variable
     * @param workflowId the workflow ID (for workflow-scoped variables)
     * @return the variable DTO, or empty if cancelled
     */
    public static Optional<VariableDTO> showVariableEdit(Window owner, VariableDTO existing, Long workflowId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "VariableEditDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage(existing != null ? "Edit Variable" : "Add Variable", owner, false);
            dialogStage.setScene(new Scene(root));

            VariableEditController controller = loader.getController();
            VariableEditViewModel viewModel = new VariableEditViewModel(existing, workflowId);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            if (controller.wasApplied()) {
                return Optional.ofNullable(controller.getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load VariableEditDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Show the Workflow Settings dialog.
     * 
     * @param owner              the owner window (can be null)
     * @param currentName        the current workflow name
     * @param currentDescription the current workflow description
     * @param isActive           whether the workflow is active
     * @return the workflow settings, or empty if cancelled
     */
    public static Optional<WorkflowSettings> showWorkflowSettings(
            Window owner,
            String currentName,
            String currentDescription,
            boolean isActive) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "WorkflowSettingsDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Workflow Settings", owner, true);
            dialogStage.setScene(new Scene(root));

            WorkflowSettingsController controller = loader.getController();
            WorkflowSettingsViewModel viewModel = new WorkflowSettingsViewModel(currentName, currentDescription,
                    isActive);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            if (controller.wasApplied()) {
                return Optional.ofNullable(controller.getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load WorkflowSettingsDialog.fxml", e);
            return Optional.empty();
        }
    }

    // ========== Debug/Info Dialogs ==========

    /**
     * Show the Node Debug dialog.
     * 
     * @param owner          the owner window (can be null)
     * @param node           the node being inspected
     * @param executionState the last execution state data
     */
    public static void showNodeDebug(Window owner, Node node, NodeExecutionState executionState) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "NodeDebugDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Debug View: " + node.name(), owner, true);
            dialogStage.setScene(new Scene(root));

            NodeDebugController controller = loader.getController();
            NodeDebugViewModel viewModel = new NodeDebugViewModel(node, executionState);
            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load NodeDebugDialog.fxml", e);
        }
    }

    /**
     * Show the Guide View dialog.
     * 
     * @param owner            the owner window (can be null)
     * @param sample           the sample workflow with guide
     * @param onHighlightNodes callback for highlighting nodes
     * @param onImport         callback when user wants to import the sample
     * @param onBack           callback when user wants to go back to samples
     *                         browser
     */
    public static void showGuideView(
            Window owner,
            SampleWorkflow sample,
            Consumer<List<String>> onHighlightNodes,
            Consumer<SampleWorkflow> onImport,
            Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "GuideViewDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("📖 Guide: " + sample.name(), owner, true, false);
            dialogStage.setMinWidth(700);
            dialogStage.setMinHeight(500);
            dialogStage.setScene(new Scene(root));

            GuideViewController controller = loader.getController();
            GuideViewViewModel viewModel = new GuideViewViewModel(sample);

            if (onHighlightNodes != null) {
                viewModel.setOnHighlightNodes(onHighlightNodes);
            }

            controller.initialize(viewModel, dialogStage);

            // Set import callback directly on controller
            if (onImport != null) {
                controller.setOnImport(onImport);
            }

            // Set back callback for returning to samples browser
            if (onBack != null) {
                controller.setOnBack(onBack);
            }

            dialogStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load GuideViewDialog.fxml", e);
        }
    }

    /**
     * Show the Expression Editor dialog.
     * 
     * @param owner              the owner window (can be null)
     * @param initialExpression  the initial expression, or null
     * @param availableVariables the list of available variables
     * @return the expression if inserted, or empty if cancelled
     */
    public static Optional<String> showExpressionEditor(
            Window owner,
            String initialExpression,
            List<String> availableVariables) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "ExpressionEditorDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Expression Editor", owner, true);
            dialogStage.setScene(new Scene(root));

            ExpressionEditorDialogController controller = loader.getController();
            ExpressionEditorDialogViewModel viewModel = new ExpressionEditorDialogViewModel(availableVariables);

            if (initialExpression != null && !initialExpression.isBlank()) {
                viewModel.setExpression(initialExpression);
            }

            controller.initialize(viewModel, dialogStage);

            dialogStage.showAndWait();

            if (controller.wasApplied()) {
                return Optional.ofNullable(controller.getResult());
            }
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load ExpressionEditorDialog.fxml", e);
            return Optional.empty();
        }
    }

    /**
     * Show the Keyboard Shortcuts dialog.
     * 
     * @param owner the owner window (can be null)
     */
    public static void showKeyboardShortcutsDialog(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogFactory.class.getResource(FXML_PATH + "KeyboardShortcutsDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = createDialogStage("Keyboard Shortcuts", owner, false);
            dialogStage.setScene(new Scene(root));

            KeyboardShortcutsController controller = loader.getController();
            controller.setOnCloseCallback(dialogStage::close);

            dialogStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load KeyboardShortcutsDialog.fxml", e);
        }
    }
}
