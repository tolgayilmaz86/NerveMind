/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Controller for the Keyboard Shortcuts dialog.
 * 
 * <p>
 * Displays all available keyboard shortcuts organized by category.
 * This is a simple display-only dialog with no ViewModel needed.
 */
public class KeyboardShortcutsController {

    @FXML
    private Button closeButton;

    private Runnable onCloseCallback;

    /**
     * Default constructor for FXML loading.
     */
    public KeyboardShortcutsController() {
        // Default constructor for FXML
    }

    /**
     * Initialize the controller. Called by FXMLLoader after injection.
     */
    @FXML
    public void initialize() {
        // No initialization needed - all content is static in FXML
    }

    /**
     * Set a callback to be invoked when the dialog closes.
     * 
     * @param callback the close callback
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Handle the close button click.
     */
    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            // Fallback: close the window directly
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        }
    }
}
