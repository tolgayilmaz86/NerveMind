package ai.nervemind.ui.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.CredentialServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.SampleServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.common.service.VariableServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import ai.nervemind.ui.view.dialog.DialogFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Default implementation of DialogService.
 * 
 * <p>
 * Creates dialogs with consistent NordDark styling and proper modality.
 * Uses FXML-based dialogs via DialogFactory where available.
 */
@Service
public class DefaultDialogService implements DialogService {

    private static final String DIALOG_STYLE = "-fx-background-color: #2e3440;";
    private static final String BUTTON_STYLE = "-fx-background-color: #5e81ac; -fx-text-fill: white;";

    private final SettingsServiceInterface settingsService;
    private final VariableServiceInterface variableService;
    private final CredentialServiceInterface credentialService;
    private final PluginServiceInterface pluginService;
    private final SampleServiceInterface sampleService;
    private final WorkflowServiceInterface workflowService;

    private Window owner;

    /**
     * Creates a new DefaultDialogService.
     *
     * @param settingsService   Service for application settings
     * @param variableService   Service for managing variables
     * @param credentialService Service for managing credentials
     * @param pluginService     Service for managing plugins
     * @param sampleService     Service for managing samples
     * @param workflowService   Service for managing workflows
     */
    public DefaultDialogService(
            SettingsServiceInterface settingsService,
            VariableServiceInterface variableService,
            CredentialServiceInterface credentialService,
            PluginServiceInterface pluginService,
            SampleServiceInterface sampleService,
            WorkflowServiceInterface workflowService) {
        this.settingsService = settingsService;
        this.variableService = variableService;
        this.credentialService = credentialService;
        this.pluginService = pluginService;
        this.sampleService = sampleService;
        this.workflowService = workflowService;
    }

    @Override
    public void setOwner(Window owner) {
        this.owner = owner;
    }

    @Override
    public void showInfo(String title, String message) {
        Alert alert = createAlert(AlertType.INFORMATION, title, message);
        alert.showAndWait();
    }

    @Override
    public void showWarning(String title, String message) {
        Alert alert = createAlert(AlertType.WARNING, title, message);
        alert.showAndWait();
    }

    @Override
    public void showError(String title, String message) {
        Alert alert = createAlert(AlertType.ERROR, title, message);
        alert.showAndWait();
    }

    @Override
    public void showError(String title, String message, Throwable exception) {
        Alert alert = createAlert(AlertType.ERROR, title, message);

        // Create expandable exception details
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Exception stacktrace:");
        label.setStyle("-fx-text-fill: #d8dee9;");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setStyle("-fx-control-inner-background: #3b4252; -fx-text-fill: #d8dee9;");
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    @Override
    public boolean confirm(String title, String message) {
        Alert alert = createAlert(AlertType.CONFIRMATION, title, message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    @Override
    public Optional<ButtonType> confirm(String title, String message, ButtonType... buttons) {
        Alert alert = createAlert(AlertType.CONFIRMATION, title, message);
        alert.getButtonTypes().setAll(buttons);
        return alert.showAndWait();
    }

    @Override
    public Optional<String> showTextInput(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        dialog.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        // Style the dialog
        dialog.getDialogPane().setStyle(DIALOG_STYLE);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(BUTTON_STYLE);

        return dialog.showAndWait();
    }

    @Override
    public Alert createAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            alert.initOwner(owner);
        }

        // Apply consistent styling
        alert.getDialogPane().setStyle(DIALOG_STYLE);
        alert.getDialogPane().lookup(".content.label")
                .setStyle("-fx-text-fill: #d8dee9;");

        // Style buttons
        alert.getDialogPane().getButtonTypes().forEach(buttonType -> {
            var button = alert.getDialogPane().lookupButton(buttonType);
            if (button != null) {
                if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE ||
                        buttonType.getButtonData() == ButtonBar.ButtonData.YES) {
                    button.setStyle(BUTTON_STYLE);
                } else {
                    button.setStyle("-fx-background-color: #4c566a; -fx-text-fill: white;");
                }
            }
        });

        return alert;
    }

    // Dialog factory methods using FXML-based dialogs

    @Override
    public void showSettingsDialog() {
        DialogFactory.showSettingsDialog(owner, settingsService, pluginService);
    }

    @Override
    public void showVariableManager(Long workflowId) {
        DialogFactory.showVariableManager(owner, variableService, workflowId);
    }

    @Override
    public void showCredentialManager() {
        DialogFactory.showCredentialManager(owner, credentialService);
    }

    @Override
    public void showPluginManager() {
        DialogFactory.showPluginManager(owner, pluginService);
    }

    @Override
    public void showSamplesBrowser(Consumer<String> onImport) {
        List<SampleWorkflow> samples = sampleService.getAllSamples();
        List<String> categories = sampleService.getCategories();
        List<String> languages = sampleService.getLanguages();

        // onGuideRequest is null here - guide viewing not supported via DialogService
        DialogFactory.showSamplesBrowser(owner, samples, categories, languages, null)
                .ifPresent(sample -> onImport.accept(sample.filePath()));
    }

    @Override
    public void showAboutDialog() {
        DialogFactory.showAboutDialog(owner);
    }

    @Override
    public Optional<String> showExpressionEditorDialog() {
        return showExpressionEditorDialog(null, List.of());
    }

    @Override
    public Optional<String> showExpressionEditorDialog(String initialExpression, List<String> availableVariables) {
        return DialogFactory.showExpressionEditor(owner, initialExpression, availableVariables);
    }

    @Override
    public Optional<Long> showWorkflowListDialog() {
        List<WorkflowDTO> workflows = workflowService.findAll();
        return DialogFactory.showWorkflowListDialog(owner, workflows)
                .map(WorkflowDTO::id);
    }
}
