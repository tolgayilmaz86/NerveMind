/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import org.kordamp.ikonli.javafx.FontIcon;

import ai.nervemind.ui.viewmodel.dialog.NodeDebugViewModel;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the Node Debug dialog FXML.
 */
public class NodeDebugController {

    @FXML
    private Label nodeNameLabel;
    @FXML
    private Label nodeTypeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private FontIcon statusIcon;
    @FXML
    private Label timestampLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private TextArea inputDataArea;
    @FXML
    private TextArea outputDataArea;
    @FXML
    private VBox errorSection;
    @FXML
    private TextArea errorArea;
    @FXML
    private Button copyInputButton;
    @FXML
    private Button copyOutputButton;
    @FXML
    private Button copyErrorButton;
    @FXML
    private Button closeButton;

    private NodeDebugViewModel viewModel;
    private Stage dialogStage;

    /**
     * Default constructor for FXML loading.
     */
    public NodeDebugController() {
        // Default constructor for FXML
    }

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        // Initial setup
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     *
     * @param viewModel the node debug view model
     * @param stage     the dialog stage
     */
    public void initialize(NodeDebugViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        stage.setTitle("Debug View: " + viewModel.getNodeName());

        bindViewModel();
    }

    private void bindViewModel() {
        // Bind node name and type header
        nodeNameLabel.textProperty().bind(viewModel.nodeNameProperty());
        nodeTypeLabel.textProperty().bind(viewModel.nodeTypeProperty());

        // Bind status
        statusLabel.textProperty().bind(viewModel.statusTextProperty());
        viewModel.statusStyleClassProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                statusLabel.getStyleClass().remove(oldVal);
            }
            if (newVal != null) {
                statusLabel.getStyleClass().add(newVal);
            }
        });
        statusLabel.getStyleClass().add(viewModel.getStatusStyleClass());

        // Update status icon
        statusIcon.setIconLiteral(viewModel.getStatusIconCode());

        // Bind timestamp and duration
        timestampLabel.textProperty().bind(viewModel.timestampTextProperty());
        durationLabel.textProperty().bind(viewModel.durationTextProperty());

        // Bind data areas
        inputDataArea.textProperty().bind(viewModel.inputDataTextProperty());
        outputDataArea.textProperty().bind(viewModel.outputDataTextProperty());

        // Handle error section visibility
        if (viewModel.hasError()) {
            errorSection.setVisible(true);
            errorSection.setManaged(true);
            errorArea.textProperty().bind(viewModel.errorTextProperty());
        } else {
            errorSection.setVisible(false);
            errorSection.setManaged(false);
        }
    }

    @FXML
    private void handleCopyInput() {
        copyToClipboard(viewModel.getInputDataText(), copyInputButton);
    }

    @FXML
    private void handleCopyOutput() {
        copyToClipboard(viewModel.getOutputDataText(), copyOutputButton);
    }

    @FXML
    private void handleCopyError() {
        copyToClipboard(viewModel.getErrorText(), copyErrorButton);
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }

    private void copyToClipboard(String text, Button button) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);

        // Show feedback
        String originalText = button.getText();
        button.setText("Copied!");
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(ev -> button.setText(originalText));
        pause.play();
    }
}
