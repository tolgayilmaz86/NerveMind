/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.nervemind.ui.viewmodel.dialog.AboutDialogViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * Controller for the About dialog FXML.
 * 
 * <p>
 * Handles UI interactions and binds to the AboutDialogViewModel.
 * Following MVVM pattern: Controller handles view logic, ViewModel handles
 * data.
 */
public class AboutDialogController {

    /**
     * Default constructor for AboutDialogController.
     */
    public AboutDialogController() {
        // Required by FXMLLoader
    }

    private static final Logger LOGGER = Logger.getLogger(AboutDialogController.class.getName());

    // FXML injected components
    @FXML
    private ImageView logoImageView;
    @FXML
    private Label appNameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label versionLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label licenseLabel;
    @FXML
    private Hyperlink projectLink;
    @FXML
    private Button closeButton;

    private AboutDialogViewModel viewModel;
    private Runnable onCloseCallback;

    /**
     * Initialize the controller. Called by FXMLLoader after injection.
     */
    @FXML
    public void initialize() {
        viewModel = new AboutDialogViewModel();
        bindViewModel();
        loadLogo();
    }

    /**
     * Set the ViewModel (for dependency injection scenarios).
     * 
     * @param viewModel the ViewModel to use
     */
    public void setViewModel(AboutDialogViewModel viewModel) {
        this.viewModel = viewModel;
        bindViewModel();
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
     * Bind UI components to ViewModel properties.
     */
    private void bindViewModel() {
        if (viewModel == null) {
            return;
        }

        // Bind labels to ViewModel properties
        appNameLabel.textProperty().bind(viewModel.appNameProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
        versionLabel.textProperty().bind(viewModel.appVersionProperty());
        authorLabel.textProperty().bind(viewModel.authorTextProperty());
        licenseLabel.textProperty().bind(viewModel.licenseTextProperty());
        projectLink.textProperty().bind(viewModel.projectUrlProperty());
    }

    /**
     * Load the application logo.
     */
    private void loadLogo() {
        try {
            Image logo = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/logo.png")));
            logoImageView.setImage(logo);
        } catch (Exception _) {
            // Fallback: try icon.png
            try {
                Image icon = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/icon.png")));
                logoImageView.setImage(icon);
                logoImageView.setFitWidth(64);
                logoImageView.setFitHeight(64);
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "No logo image available", ex);
            }
        }
    }

    /**
     * Handle project link click - opens URL in browser.
     */
    @FXML
    private void handleProjectLinkClick() {
        String url = viewModel.getProjectUrl();
        openUrl(url);
    }

    /**
     * Handle close button click.
     */
    @FXML
    private void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }

        // Close the dialog window
        Stage stage = (Stage) closeButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Open a URL in the default browser.
     */
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (IOException | URISyntaxException _) {
            LOGGER.log(Level.WARNING, "Failed to open URL: {0}", url);
        }
    }
}
