/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.debug;

import java.util.logging.Logger;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * Floating workflow control bar with sleek pill-shaped design.
 * 
 * <p>
 * This toolbar appears as a floating overlay at the top center of the canvas,
 * providing workflow execution controls in a modern, minimal design.
 * </p>
 * 
 * <h2>Layout (left to right):</h2>
 * <ul>
 * <li>Restart button - restart workflow execution</li>
 * <li>Step Back button - go to previous step (when paused)</li>
 * <li>Play/Pause button - central large green button</li>
 * <li>Step Forward button - go to next step (when paused)</li>
 * <li>Debug/Inspect button - opens debug detail pane</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class DebugToolbar extends HBox {

    private static final Logger LOGGER = Logger.getLogger(DebugToolbar.class.getName());

    // State properties
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private final BooleanProperty stepModeEnabled = new SimpleBooleanProperty(false);

    // UI Components
    private Button restartButton;
    private Button stepBackButton;
    private Button playPauseButton;
    private Button stepForwardButton;
    private Button debugButton;
    private FontIcon playPauseIcon;

    // Callbacks
    private Runnable onPlay;
    private Runnable onStop;
    private Runnable onRestart;
    private Runnable onStepBack;
    private Runnable onStepForward;
    private Runnable onDebugInspect;

    // Dragging support
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean isDragging = false;

    // Animation
    private ScaleTransition pulseAnimation;

    /**
     * Creates a new floating control bar.
     */
    public DebugToolbar() {
        getStyleClass().add("floating-control-bar");
        setAlignment(Pos.CENTER);
        setSpacing(6);

        createButtons();
        setupLayout();
        setupDragging();
        setupStateBindings();

        // Always visible
        setVisible(true);
        setManaged(true);
    }

    /**
     * Creates all control buttons.
     */
    private void createButtons() {
        // Restart button
        restartButton = createCircleButton(MaterialDesignR.RESTART, "Restart Workflow", 18);
        restartButton.getStyleClass().addAll("control-button", "control-secondary");
        restartButton.setOnAction(e -> {
            if (onRestart != null) {
                onRestart.run();
            }
        });

        // Step Back button (Undo icon)
        stepBackButton = createCircleButton(MaterialDesignU.UNDO, "Step Back", 18);
        stepBackButton.getStyleClass().addAll("control-button", "control-secondary");
        stepBackButton.setOnAction(e -> {
            if (onStepBack != null) {
                onStepBack.run();
            }
        });

        // Play/Pause button (central, larger, green)
        playPauseIcon = new FontIcon(MaterialDesignP.PLAY);
        playPauseIcon.setIconSize(32);
        playPauseButton = new Button();
        playPauseButton.setGraphic(playPauseIcon);
        playPauseButton.getStyleClass().addAll("control-button", "control-play");
        playPauseButton.setTooltip(new Tooltip("Run Workflow (F5)"));
        playPauseButton.setOnAction(e -> handlePlayPauseClick());

        // Step Forward button (Redo icon)
        stepForwardButton = createCircleButton(MaterialDesignR.REDO, "Step Forward (F10)", 18);
        stepForwardButton.getStyleClass().addAll("control-button", "control-secondary");
        stepForwardButton.setOnAction(e -> {
            if (onStepForward != null) {
                onStepForward.run();
            }
        });

        // Debug/Inspect button
        debugButton = createCircleButton(MaterialDesignB.BUG, "Debug Inspector", 18);
        debugButton.getStyleClass().addAll("control-button", "control-secondary");
        debugButton.setOnAction(e -> {
            if (onDebugInspect != null) {
                onDebugInspect.run();
            }
        });
    }

    /**
     * Creates a circular button with an icon.
     */
    private Button createCircleButton(org.kordamp.ikonli.Ikon icon, String tooltip, int iconSize) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(iconSize);

        Button button = new Button();
        button.setGraphic(fontIcon);
        button.setTooltip(new Tooltip(tooltip));

        return button;
    }

    /**
     * Sets up the button layout.
     */
    private void setupLayout() {
        // Left spacer for visual balance
        Region leftPad = new Region();
        leftPad.setMinWidth(4);

        // Right spacer
        Region rightPad = new Region();
        rightPad.setMinWidth(4);

        getChildren().addAll(
                leftPad,
                restartButton,
                stepBackButton,
                playPauseButton,
                stepForwardButton,
                debugButton,
                rightPad);
    }

    /**
     * Sets up state bindings for button visibility and states.
     */
    private void setupStateBindings() {
        // Step back only enabled when paused (needs history to navigate)
        stepBackButton.disableProperty().bind(paused.not());
        // Step forward always enabled - starts step execution when idle, continues when
        // paused
        stepForwardButton.setDisable(false);

        // Update play/pause icon based on state
        running.addListener((obs, oldVal, isRunning) -> Platform.runLater(this::updatePlayButtonState));

        paused.addListener((obs, oldVal, isPaused) -> Platform.runLater(() -> {
            updatePlayButtonState();
            if (Boolean.TRUE.equals(isPaused)) {
                startPulseAnimation();
            } else {
                stopPulseAnimation();
            }
        }));
    }

    /**
     * Updates the play button appearance based on current state.
     */
    private void updatePlayButtonState() {
        if (running.get()) {
            // Running or paused -> show stop (stop is always available during execution)
            playPauseIcon.setIconCode(MaterialDesignS.STOP);
            playPauseButton.getStyleClass().remove("control-play");
            playPauseButton.getStyleClass().remove("control-paused");
            if (!playPauseButton.getStyleClass().contains("control-stop")) {
                playPauseButton.getStyleClass().add("control-stop");
            }
            playPauseButton.setTooltip(new Tooltip(paused.get()
                    ? "Stop Workflow (Shift+F5) - Use Step Forward to continue"
                    : "Stop Workflow (Shift+F5)"));
        } else {
            // Idle -> show play
            playPauseIcon.setIconCode(MaterialDesignP.PLAY);
            playPauseButton.getStyleClass().remove("control-stop");
            playPauseButton.getStyleClass().remove("control-paused");
            if (!playPauseButton.getStyleClass().contains("control-play")) {
                playPauseButton.getStyleClass().add("control-play");
            }
            playPauseButton.setTooltip(new Tooltip("Run Workflow (F5)"));
        }
    }

    /**
     * Handles play/pause button click based on current state.
     */
    private void handlePlayPauseClick() {
        if (running.get()) {
            // Running (or paused) -> stop execution
            LOGGER.info("Control bar: Stop clicked");
            if (onStop != null) {
                onStop.run();
            }
        } else {
            // Not running -> start
            LOGGER.info("Control bar: Play clicked");
            if (onPlay != null) {
                onPlay.run();
            }
        }
    }

    /**
     * Sets up dragging support for the toolbar.
     */
    private void setupDragging() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(e -> isDragging = false);
    }

    private void handleMousePressed(MouseEvent event) {
        dragOffsetX = event.getSceneX() - getTranslateX();
        dragOffsetY = event.getSceneY() - getTranslateY();
        isDragging = true;
        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (isDragging) {
            double newX = event.getSceneX() - dragOffsetX;
            double newY = event.getSceneY() - dragOffsetY;

            // Constrain to reasonable bounds
            if (getParent() != null) {
                double parentWidth = getParent().getLayoutBounds().getWidth();
                double parentHeight = getParent().getLayoutBounds().getHeight();

                // Allow dragging within parent bounds with some margin
                double margin = 50;
                newX = Math.clamp(newX, -parentWidth / 2 + margin, parentWidth / 2 - margin);
                newY = Math.clamp(newY, -20, parentHeight - getHeight() - 20);
            }

            setTranslateX(newX);
            setTranslateY(newY);
            event.consume();
        }
    }

    /**
     * Starts a subtle pulse animation when paused.
     */
    private void startPulseAnimation() {
        stopPulseAnimation();

        pulseAnimation = new ScaleTransition(Duration.millis(800), playPauseButton);
        pulseAnimation.setFromX(1.0);
        pulseAnimation.setFromY(1.0);
        pulseAnimation.setToX(1.1);
        pulseAnimation.setToY(1.1);
        pulseAnimation.setCycleCount(Animation.INDEFINITE);
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.play();
    }

    /**
     * Stops the pulse animation.
     */
    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
        }
        // Reset scale
        playPauseButton.setScaleX(1.0);
        playPauseButton.setScaleY(1.0);
    }

    // ========== Public API ==========

    /**
     * Shows the toolbar in running state.
     */
    public void showRunning() {
        Platform.runLater(() -> {
            running.set(true);
            paused.set(false);
        });
    }

    /**
     * Shows the toolbar in paused state.
     * 
     * @param nodeName  the name of the paused node
     * @param nodeId    the ID of the paused node
     * @param nodeIndex current node index
     * @param total     total nodes
     */
    public void showPaused(String nodeName, String nodeId, int nodeIndex, int total) {
        Platform.runLater(() -> {
            running.set(true);
            paused.set(true);
            String info = nodeIndex > 0 && total > 0
                    ? String.format(" (%d/%d)", nodeIndex, total)
                    : "";
            playPauseButton.setTooltip(
                    new Tooltip("Continue - Paused at: " + nodeName + info));
        });
    }

    /**
     * Resets the toolbar to idle state.
     */
    public void hide() {
        Platform.runLater(() -> {
            running.set(false);
            paused.set(false);
            stopPulseAnimation();
        });
    }

    /**
     * Enables or disables step mode.
     * 
     * @param enabled true to enable step mode
     */
    public void setStepModeEnabled(boolean enabled) {
        Platform.runLater(() -> stepModeEnabled.set(enabled));
    }

    // ========== Callback Setters ==========

    /**
     * Sets the play action callback.
     * 
     * @param callback the play action
     */
    public void setOnPlay(Runnable callback) {
        this.onPlay = callback;
    }

    /**
     * Sets the stop action callback.
     * 
     * @param callback the stop action
     */
    public void setOnStop(Runnable callback) {
        this.onStop = callback;
    }

    /**
     * Sets the restart action callback.
     * 
     * @param callback the restart action
     */
    public void setOnRestart(Runnable callback) {
        this.onRestart = callback;
    }

    /**
     * Sets the step back action callback.
     * 
     * @param callback the step back action
     */
    public void setOnStepBack(Runnable callback) {
        this.onStepBack = callback;
    }

    /**
     * Sets the step forward action callback.
     * 
     * @param callback the step forward action
     */
    public void setOnStepForward(Runnable callback) {
        this.onStepForward = callback;
    }

    /**
     * Sets the debug inspect action callback.
     * 
     * @param callback the inspect action
     */
    public void setOnDebugInspect(Runnable callback) {
        this.onDebugInspect = callback;
    }

    // Compatibility methods for existing WorkflowCanvas integration
    /**
     * Sets the continue action callback (alias for play).
     * 
     * @param callback the continue action
     */
    public void setOnContinue(Runnable callback) {
        this.onPlay = callback;
    }

    /**
     * Sets the step action callback (alias for step forward).
     * 
     * @param callback the step action
     */
    public void setOnStep(Runnable callback) {
        this.onStepForward = callback;
    }

    /**
     * Sets the inspect action callback (alias for debug inspect).
     * 
     * @param callback the inspect action
     */
    public void setOnInspect(Runnable callback) {
        this.onDebugInspect = callback;
    }

    // ========== Property Accessors ==========

    /**
     * Gets the running state property.
     * 
     * @return the running state property
     */
    public BooleanProperty runningProperty() {
        return running;
    }

    /**
     * Gets the paused state property.
     * 
     * @return the paused state property
     */
    public BooleanProperty pausedProperty() {
        return paused;
    }

    /**
     * Gets the step mode enabled state property.
     * 
     * @return the step mode enabled state property
     */
    public BooleanProperty stepModeEnabledProperty() {
        return stepModeEnabled;
    }

    // Legacy compatibility
    /**
     * Gets the toolbar visible property (legacy compatibility).
     * 
     * @return dummy property for visibility toggle mapping
     */
    public BooleanProperty toolbarVisibleProperty() {
        return new SimpleBooleanProperty(true);
    }
}
