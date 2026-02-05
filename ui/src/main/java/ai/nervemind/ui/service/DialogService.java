package ai.nervemind.ui.service;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Service interface for creating and displaying dialogs.
 * 
 * <p>
 * Centralizes dialog creation to:
 * <ul>
 * <li>Ensure consistent styling across all dialogs</li>
 * <li>Enable testing by mocking dialog interactions</li>
 * <li>Decouple ViewModels from concrete dialog implementations</li>
 * </ul>
 */
public interface DialogService {

    /**
     * Shows an information alert.
     * 
     * @param title   the dialog title
     * @param message the message to display
     */
    void showInfo(String title, String message);

    /**
     * Shows a warning alert.
     * 
     * @param title   the dialog title
     * @param message the warning message
     */
    void showWarning(String title, String message);

    /**
     * Shows an error alert.
     * 
     * @param title   the dialog title
     * @param message the error message
     */
    void showError(String title, String message);

    /**
     * Shows an error alert with exception details.
     * 
     * @param title     the dialog title
     * @param message   the error message
     * @param exception the exception (details shown in expandable area)
     */
    void showError(String title, String message, Throwable exception);

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     * 
     * @param title   the dialog title
     * @param message the confirmation message
     * @return true if user clicked Yes
     */
    boolean confirm(String title, String message);

    /**
     * Shows a confirmation dialog with custom buttons.
     * 
     * @param title   the dialog title
     * @param message the confirmation message
     * @param buttons the button types to display
     * @return the button type that was clicked
     */
    Optional<ButtonType> confirm(String title, String message, ButtonType... buttons);

    /**
     * Shows a text input dialog.
     * 
     * @param title        the dialog title
     * @param message      the prompt message
     * @param defaultValue the default input value
     * @return the entered text, or empty if cancelled
     */
    Optional<String> showTextInput(String title, String message, String defaultValue);

    /**
     * Shows the Settings dialog.
     */
    void showSettingsDialog();

    /**
     * Shows the Variable Manager dialog.
     * 
     * @param workflowId the workflow ID to manage variables for (null for global)
     */
    void showVariableManager(Long workflowId);

    /**
     * Shows the Credential Manager dialog.
     */
    void showCredentialManager();

    /**
     * Shows the Plugin Manager dialog.
     */
    void showPluginManager();

    /**
     * Shows the Samples Browser dialog.
     * 
     * @param onImport callback when a sample is imported
     */
    void showSamplesBrowser(Consumer<String> onImport);

    /**
     * Shows the About dialog.
     */
    void showAboutDialog();

    /**
     * Shows the Expression Editor dialog.
     * 
     * <p>
     * Opens a standalone expression editor for building and testing expressions
     * with variables and functions.
     * 
     * @return the expression entered, or empty if cancelled
     */
    Optional<String> showExpressionEditorDialog();

    /**
     * Shows the Expression Editor dialog with initial context.
     * 
     * @param initialExpression  the initial expression to edit
     * @param availableVariables the variables available for autocomplete
     * @return the expression entered, or empty if cancelled
     */
    Optional<String> showExpressionEditorDialog(String initialExpression, java.util.List<String> availableVariables);

    /**
     * Shows a workflow list dialog for opening workflows.
     * 
     * @return the selected workflow ID, or empty if cancelled
     */
    Optional<Long> showWorkflowListDialog();

    /**
     * Sets the owner window for dialogs.
     * 
     * @param owner the owner window
     */
    void setOwner(Window owner);

    /**
     * Creates a preconfigured Alert with application styling.
     * 
     * @param type    the alert type
     * @param title   the dialog title
     * @param message the message
     * @return the configured Alert
     */
    Alert createAlert(Alert.AlertType type, String title, String message);
}
