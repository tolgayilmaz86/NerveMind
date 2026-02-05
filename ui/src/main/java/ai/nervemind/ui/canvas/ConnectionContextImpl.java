/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.plugin.api.ConnectionContext;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Implementation of ConnectionContext that wraps a ConnectionLine.
 * 
 * <p>
 * This class bridges the plugin API with the internal UI implementation,
 * providing plugins with controlled access to connection operations.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
public class ConnectionContextImpl implements ConnectionContext {

    private final ConnectionLine connectionLine;
    private final WorkflowCanvas canvas;
    private final Connection connection;

    /**
     * Creates a new ConnectionContext implementation.
     * 
     * @param connectionLine the connection line to wrap
     * @param canvas         the workflow canvas
     */
    public ConnectionContextImpl(ConnectionLine connectionLine, WorkflowCanvas canvas) {
        this.connectionLine = connectionLine;
        this.canvas = canvas;
        this.connection = connectionLine.getConnection();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONNECTION INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String getConnectionId() {
        return connection.id();
    }

    @Override
    public String getSourceNodeId() {
        return connection.sourceNodeId();
    }

    @Override
    public String getTargetNodeId() {
        return connection.targetNodeId();
    }

    @Override
    public String getSourceOutput() {
        return connection.sourceOutput();
    }

    @Override
    public String getTargetInput() {
        return connection.targetInput();
    }

    @Override
    public Long getWorkflowId() {
        return canvas.getWorkflowId();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DECORATION API
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void addDecoration(String key, Object node) {
        if (!(node instanceof Node)) {
            throw new IllegalArgumentException("Decoration must be a JavaFX Node");
        }

        if (Platform.isFxApplicationThread()) {
            connectionLine.addDecoration(key, (Node) node);
        } else {
            Platform.runLater(() -> connectionLine.addDecoration(key, (Node) node));
        }
    }

    @Override
    public void removeDecoration(String key) {
        if (Platform.isFxApplicationThread()) {
            connectionLine.removeDecoration(key);
        } else {
            Platform.runLater(() -> connectionLine.removeDecoration(key));
        }
    }

    @Override
    public Object getDecoration(String key) {
        return connectionLine.getDecoration(key);
    }

    @Override
    public boolean hasDecoration(String key) {
        return connectionLine.hasDecoration(key);
    }

    @Override
    public double[] getMidpoint() {
        return connectionLine.getMidpoint();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LABEL CONVENIENCE API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Key used for the label decoration.
     */
    private static final String LABEL_DECORATION_KEY = "label";

    // Visual styling constants for labels
    private static final Color LABEL_BACKGROUND_COLOR = Color.web("#2d2d2d");
    private static final Color LABEL_TEXT_COLOR = Color.web("#e0e0e0");
    private static final Color LABEL_BORDER_COLOR = Color.web("#4a9eff");
    private static final double LABEL_PADDING_X = 6;
    private static final double LABEL_PADDING_Y = 3;
    private static final double LABEL_CORNER_RADIUS = 4;

    /**
     * Tracks the current label text for getLabel().
     */
    private String currentLabelText = null;

    @Override
    public void setLabel(String text) {
        this.currentLabelText = text;

        Runnable action = () -> {
            if (text == null || text.isBlank()) {
                connectionLine.removeDecoration(LABEL_DECORATION_KEY);
            } else {
                Group labelGroup = createLabelVisual(text);
                connectionLine.addDecoration(LABEL_DECORATION_KEY, labelGroup);
            }
        };

        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    @Override
    public String getLabel() {
        return currentLabelText;
    }

    /**
     * Creates the visual elements for a connection label.
     * 
     * @param labelText the text to display
     * @return a Group containing the styled label
     */
    private Group createLabelVisual(String labelText) {
        // Create the text
        Text text = new Text(labelText);
        text.setFill(LABEL_TEXT_COLOR);
        text.setFont(Font.font("System", FontWeight.NORMAL, 11));
        text.setMouseTransparent(true);

        // Force layout computation to get accurate bounds
        text.applyCss();
        Bounds textBounds = text.getBoundsInLocal();
        double textWidth = textBounds.getWidth();
        double textHeight = textBounds.getHeight();

        // Create the background rectangle
        Rectangle background = new Rectangle();
        background.setFill(LABEL_BACKGROUND_COLOR);
        background.setArcWidth(LABEL_CORNER_RADIUS * 2);
        background.setArcHeight(LABEL_CORNER_RADIUS * 2);
        background.setStroke(LABEL_BORDER_COLOR);
        background.setStrokeWidth(1);
        background.setMouseTransparent(true);

        // Size the background to fit the text with padding
        double bgWidth = textWidth + LABEL_PADDING_X * 2;
        double bgHeight = textHeight + LABEL_PADDING_Y * 2;
        background.setWidth(bgWidth);
        background.setHeight(bgHeight);

        // Center the background on origin (0,0)
        background.setX(-bgWidth / 2);
        background.setY(-bgHeight / 2);

        // Center the text on origin (0,0)
        // Text baseline is at Y=0, so we need to adjust
        text.setX(-textWidth / 2);
        text.setY(textHeight / 4); // Slight adjustment for vertical centering

        // Create the group (background first, then text on top)
        Group group = new Group(background, text);
        group.setMouseTransparent(true);

        return group;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getWorkflowSetting(String key) {
        if (canvas.getWorkflow() == null) {
            return null;
        }
        return (T) canvas.getWorkflow().settings().get(key);
    }

    @Override
    public void setWorkflowSetting(String key, Object value) {
        if (canvas.getWorkflow() == null) {
            return;
        }

        Map<String, Object> settings = new HashMap<>(canvas.getWorkflow().settings());
        if (value == null) {
            settings.remove(key);
        } else {
            settings.put(key, value);
        }
        canvas.updateWorkflowSettings(settings);
    }

    @Override
    public Map<String, Object> getWorkflowSettings() {
        if (canvas.getWorkflow() == null) {
            return Map.of();
        }
        return Map.copyOf(canvas.getWorkflow().settings());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    @Override
    public void showTextInputDialog(String title, String headerText, String promptText,
            String defaultValue, Consumer<String> onResult) {
        runOnFxThread(() -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle(title);
            if (headerText != null) {
                dialog.setHeaderText(headerText);
            }

            // Set button types
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            Label instructions = new Label(promptText);
            TextField inputField = new TextField();
            inputField.setPromptText("Enter text...");
            if (defaultValue != null) {
                inputField.setText(defaultValue);
            }
            inputField.setPrefWidth(300);

            content.getChildren().addAll(instructions, inputField);
            dialog.getDialogPane().setContent(content);

            // Request focus on the input field
            Platform.runLater(inputField::requestFocus);

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return inputField.getText().trim();
                }
                return null;
            });

            // Apply dark theme styling
            var styleResource = getClass().getResource("/ai/nervemind/ui/styles/dialog.css");
            if (styleResource != null) {
                dialog.getDialogPane().getStylesheets().add(styleResource.toExternalForm());
            }

            // Show and process result
            Optional<String> result = dialog.showAndWait();
            if (onResult != null) {
                onResult.accept(result.orElse(null));
            }
        });
    }
}
