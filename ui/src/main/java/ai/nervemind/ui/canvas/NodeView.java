package ai.nervemind.ui.canvas;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.HandleInfo;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Visual representation of a workflow node on the canvas.
 * Styled similar to n8n with centered icon and label below.
 * 
 * <p>
 * Supports multiple input/output handles based on node type configuration.
 * Handles are positioned on the LEFT, RIGHT, TOP, or BOTTOM of the node.
 * </p>
 */
public class NodeView extends StackPane {

    // n8n-style dimensions - square node box
    private static final double NODE_SIZE = 80;
    private static final double HANDLE_RADIUS = 8;
    private static final double CORNER_RADIUS = 12;
    private static final double HANDLE_SPACING = 18; // Spacing between multiple handles on same side

    // Handle colors - distinct colors for input/output
    private static final Color INPUT_HANDLE_COLOR = Color.web("#40c057"); // Green for input
    private static final Color OUTPUT_HANDLE_COLOR = Color.web("#4a9eff"); // Blue for output
    private static final Color HANDLE_HOVER_COLOR = Color.web("#ffd43b"); // Yellow on hover
    private static final Color HANDLE_ACTIVE_COLOR = Color.web("#ffd43b"); // Yellow when dragging

    // Node background
    private static final Color NODE_BG = Color.web("#262626");
    private static final Color NODE_BORDER = Color.web("#404040");

    private final Node node;
    private final WorkflowCanvas canvas;
    private final PluginServiceInterface pluginService;

    private final VBox container;
    private final StackPane nodeBox;
    private final Rectangle background;
    private final FontIcon icon;
    private final Label nameLabel;
    private final Label subtitleLabel;
    private final javafx.scene.control.ProgressIndicator executionIndicator;
    private final Label executionBadge;
    private final Label errorLabel;
    private final Tooltip errorTooltip;

    // Multi-handle support
    private final List<HandleInfo> handleDefinitions;
    private final Map<String, Circle> handles = new HashMap<>(); // handleId -> Circle
    private final Map<String, HandleInfo> handleInfoMap = new HashMap<>(); // handleId -> HandleInfo

    // Legacy single-handle access (for backward compatibility)
    private Circle inputHandle;
    private Circle outputHandle;

    private double dragOffsetX;
    private double dragOffsetY;
    private boolean selected = false;
    private ExecutionState executionState = ExecutionState.IDLE;
    private String errorMessage = null;

    // Context menu state
    private ContextMenu currentContextMenu = null;

    /** Execution states for visual feedback */
    public enum ExecutionState {
        /** Normal state */
        IDLE,
        /** Waiting to execute */
        QUEUED,
        /** Currently executing */
        RUNNING,
        /** Executed successfully */
        SUCCESS,
        /** Execution failed */
        ERROR,
        /** Skipped (disabled or conditional) */
        SKIPPED
    }

    /**
     * Creates a NodeView with default handles (1 input left, 1 output right).
     * For backward compatibility with existing code.
     * 
     * @param node   the domain node
     * @param canvas the parent canvas
     */
    public NodeView(Node node, WorkflowCanvas canvas) {
        this(node, canvas, null);
    }

    /**
     * Creates a NodeView with handles based on plugin configuration.
     *
     * @param node          the domain node
     * @param canvas        the parent canvas
     * @param pluginService the plugin service (can be null for default handles)
     */
    public NodeView(Node node, WorkflowCanvas canvas, PluginServiceInterface pluginService) {
        this.node = node;
        this.canvas = canvas;
        this.pluginService = pluginService;

        // Get handle definitions from plugin service or use defaults
        if (pluginService != null) {
            this.handleDefinitions = pluginService.getHandleDefinitions(node.type());
        } else {
            this.handleDefinitions = List.of(
                    new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                    new HandleInfo("out", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, null));
        }

        getStyleClass().add("node-view");

        // Position on canvas
        setLayoutX(node.position().x());
        setLayoutY(node.position().y());

        // === Create the node box (square with icon) ===
        nodeBox = new StackPane();
        nodeBox.setPrefSize(NODE_SIZE, NODE_SIZE);
        nodeBox.setMinSize(NODE_SIZE, NODE_SIZE);
        nodeBox.setMaxSize(NODE_SIZE, NODE_SIZE);
        nodeBox.getStyleClass().add("node-box");

        // Background rectangle with rounded corners and border
        background = new Rectangle(NODE_SIZE, NODE_SIZE);
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        background.setFill(NODE_BG);
        background.setStroke(NODE_BORDER);
        background.setStrokeWidth(1.5);

        // Large icon centered in the box - check for custom icon first
        icon = new FontIcon();
        icon.setIconSize(42);
        icon.getStyleClass().add("node-box__icon");
        updateIconFromNode();

        // === Execution indicator (spinning progress for RUNNING state) ===
        executionIndicator = new javafx.scene.control.ProgressIndicator();
        executionIndicator.setPrefSize(24, 24);
        executionIndicator.setMaxSize(24, 24);
        executionIndicator.setVisible(false);
        executionIndicator.getStyleClass().add("execution-indicator");

        // === Execution badge (for SUCCESS, ERROR, SKIPPED states) ===
        executionBadge = new Label();
        executionBadge.setPrefSize(20, 20);
        executionBadge.setMinSize(20, 20);
        executionBadge.setMaxSize(20, 20);
        executionBadge.setAlignment(Pos.CENTER);
        executionBadge.setVisible(false);
        executionBadge.getStyleClass().add("execution-badge");

        nodeBox.getChildren().addAll(background, icon, executionIndicator, executionBadge);
        StackPane.setAlignment(executionIndicator, Pos.TOP_RIGHT);
        StackPane.setAlignment(executionBadge, Pos.TOP_RIGHT);
        executionIndicator.setTranslateX(8);
        executionIndicator.setTranslateY(-8);
        executionBadge.setTranslateX(8);
        executionBadge.setTranslateY(-8);

        // === Create handles based on definitions ===
        StackPane nodeWithHandles = new StackPane();
        nodeWithHandles.getChildren().add(nodeBox);

        createHandlesFromDefinitions(nodeWithHandles);

        // Set legacy handles for backward compatibility
        inputHandle = getFirstInputHandle();
        outputHandle = getFirstOutputHandle();

        // === Labels below the node ===
        nameLabel = new Label(node.name());
        nameLabel.getStyleClass().add("node-name-label");
        nameLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-wrap-text: true; -fx-label-padding: 0 2 0 2;");
        // Use system font that supports emoji (Segoe UI Emoji on Windows)
        nameLabel.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", javafx.scene.text.FontWeight.BOLD, 11));
        nameLabel.setMaxWidth(95); // Slightly wider than node box for readability
        nameLabel.setMinWidth(70); // At least node width
        nameLabel.setWrapText(true); // Enable text wrapping - respects word boundaries by default
        nameLabel.setMaxHeight(34); // Allow ~2 lines of text
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        nameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS); // Ellipsis if still too long

        // Add tooltip for names that might be truncated or wrapped
        if (node.name() != null && node.name().length() > 12) {
            Tooltip nameTooltip = new Tooltip(node.name());
            nameTooltip.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #e5e5e5; " +
                    "-fx-font-size: 12px; -fx-padding: 6; -fx-background-radius: 4;");
            nameTooltip.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 12));
            nameTooltip.setShowDelay(javafx.util.Duration.millis(400));
            Tooltip.install(nameLabel, nameTooltip);
        }

        subtitleLabel = new Label(getSubtitleForType(node.type()));
        subtitleLabel.getStyleClass().add("node-subtitle-label");
        subtitleLabel.setStyle("-fx-text-fill: #737373; -fx-font-size: 10px;");
        subtitleLabel.setMaxWidth(90);
        subtitleLabel.setAlignment(Pos.CENTER);

        // === Error label (hidden by default) ===
        errorLabel = new Label();
        errorLabel.getStyleClass().add("node-error-label");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px; -fx-padding: 2 6 2 6; " +
                "-fx-background-color: rgba(239, 68, 68, 0.15); -fx-background-radius: 4;");
        errorLabel.setMaxWidth(140);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Error tooltip for longer messages
        errorTooltip = new Tooltip();
        errorTooltip.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #ef4444; -fx-font-size: 11px; " +
                "-fx-background-radius: 4; -fx-padding: 8;");
        errorTooltip.setWrapText(true);
        errorTooltip.setMaxWidth(300);

        // === Main container: node + labels ===
        container = new VBox(4);
        container.setAlignment(Pos.TOP_CENTER);
        container.getChildren().addAll(nodeWithHandles, nameLabel, subtitleLabel, errorLabel);

        getChildren().add(container);

        // Setup interactions
        setupDragBehavior();
        setupClickBehavior();
        setupHandleInteractions();

        // Apply drop shadow
        applyNodeShadow();

        // Apply type-specific border color
        applyNodeTypeStyle();
    }

    /**
     * Creates handles based on handle definitions from the plugin service.
     * Handles are positioned according to their HandleInfo.Position.
     */
    private void createHandlesFromDefinitions(StackPane nodeWithHandles) {
        // Group handles by position for proper spacing
        Map<HandleInfo.Position, List<HandleInfo>> handlesByPosition = new EnumMap<>(HandleInfo.Position.class);
        for (HandleInfo info : handleDefinitions) {
            handlesByPosition.computeIfAbsent(info.position(), k -> new ArrayList<>()).add(info);
        }

        // Create handles for each position
        for (var entry : handlesByPosition.entrySet()) {
            HandleInfo.Position position = entry.getKey();
            List<HandleInfo> handlesAtPosition = entry.getValue();
            int count = handlesAtPosition.size();

            for (int i = 0; i < count; i++) {
                HandleInfo info = handlesAtPosition.get(i);
                Circle handle = createHandleFromInfo(info);
                handles.put(info.id(), handle);
                handleInfoMap.put(info.id(), info);
                nodeWithHandles.getChildren().add(handle);

                // Position the handle
                positionHandle(handle, position, i, count);
            }
        }
    }

    /**
     * Creates a handle circle from a HandleInfo definition.
     */
    private Circle createHandleFromInfo(HandleInfo info) {
        boolean isInput = info.isInput();
        Circle handle = new Circle(HANDLE_RADIUS);
        Color baseColor = isInput ? INPUT_HANDLE_COLOR : OUTPUT_HANDLE_COLOR;
        handle.setFill(baseColor);
        handle.setStroke(Color.WHITE);
        handle.setStrokeWidth(2);
        handle.setCursor(Cursor.CROSSHAIR);
        handle.getStyleClass().add(isInput ? "input-handle" : "output-handle");
        handle.setUserData(info.id()); // Store handle ID for connection logic

        // Tooltip with label if available
        String tooltipText;
        if (info.label() != null && !info.label().isBlank()) {
            tooltipText = info.label() + (isInput ? " ⬅" : " ➡");
        } else {
            tooltipText = isInput ? "⬅ Input" : "Output ➡";
        }
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(handle, tooltip);

        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(baseColor);
        glow.setRadius(10);
        glow.setSpread(0.3);
        handle.setEffect(glow);

        // Hover effects
        handle.setOnMouseEntered(e -> {
            if (!canvas.isConnectionDragging() || !isInput) {
                handle.setFill(HANDLE_HOVER_COLOR);
                handle.setScaleX(1.3);
                handle.setScaleY(1.3);
            }
        });

        handle.setOnMouseExited(e -> {
            if (!canvas.isConnectionDragging() || !isInput) {
                handle.setFill(baseColor);
                handle.setScaleX(1.0);
                handle.setScaleY(1.0);
            }
        });

        return handle;
    }

    /**
     * Positions a handle on the node based on its position and index.
     * Multiple handles on the same side are spaced evenly.
     */
    private void positionHandle(Circle handle, HandleInfo.Position position, int index, int total) {
        // Calculate offset for multiple handles on same side
        double offset = calculateHandleOffset(index, total);

        switch (position) {
            case LEFT -> {
                StackPane.setAlignment(handle, Pos.CENTER_LEFT);
                handle.setTranslateX(-HANDLE_RADIUS);
                handle.setTranslateY(offset);
            }
            case RIGHT -> {
                StackPane.setAlignment(handle, Pos.CENTER_RIGHT);
                handle.setTranslateX(HANDLE_RADIUS);
                handle.setTranslateY(offset);
            }
            case TOP -> {
                StackPane.setAlignment(handle, Pos.TOP_CENTER);
                handle.setTranslateX(offset);
                handle.setTranslateY(-HANDLE_RADIUS);
            }
            case BOTTOM -> {
                StackPane.setAlignment(handle, Pos.BOTTOM_CENTER);
                handle.setTranslateX(offset);
                handle.setTranslateY(HANDLE_RADIUS);
            }
        }
    }

    /**
     * Calculates the offset for positioning multiple handles on the same side.
     */
    private double calculateHandleOffset(int index, int total) {
        if (total <= 1) {
            return 0;
        }
        // Distribute handles evenly, centered on the side
        double totalSpan = (total - 1) * HANDLE_SPACING;
        double startOffset = -totalSpan / 2;
        return startOffset + (index * HANDLE_SPACING);
    }

    /**
     * Gets the first input handle for backward compatibility.
     */
    private Circle getFirstInputHandle() {
        for (HandleInfo info : handleDefinitions) {
            if (info.isInput()) {
                return handles.get(info.id());
            }
        }
        return null;
    }

    /**
     * Gets the first output handle for backward compatibility.
     */
    private Circle getFirstOutputHandle() {
        for (HandleInfo info : handleDefinitions) {
            if (info.isOutput()) {
                return handles.get(info.id());
            }
        }
        return null;
    }

    /**
     * Gets a handle by its ID.
     * 
     * @param handleId the handle identifier
     * @return the handle circle, or null if not found
     */
    public Circle getHandle(String handleId) {
        return handles.get(handleId);
    }

    /**
     * Gets the handle info by ID.
     * 
     * @param handleId the handle identifier
     * @return the handle info, or null if not found
     */
    public HandleInfo getHandleInfo(String handleId) {
        return handleInfoMap.get(handleId);
    }

    /**
     * Gets all input handles.
     * 
     * @return list of input handle IDs
     */
    public List<String> getInputHandleIds() {
        return handleDefinitions.stream()
                .filter(HandleInfo::isInput)
                .map(HandleInfo::id)
                .toList();
    }

    /**
     * Gets all output handles.
     * 
     * @return list of output handle IDs
     */
    public List<String> getOutputHandleIds() {
        return handleDefinitions.stream()
                .filter(HandleInfo::isOutput)
                .map(HandleInfo::id)
                .toList();
    }

    /**
     * Gets the handle definitions for this node.
     * 
     * @return unmodifiable list of handle definitions
     */
    public List<HandleInfo> getHandleDefinitions() {
        return List.copyOf(handleDefinitions);
    }

    private void setupHandleInteractions() {
        // Setup interactions for all handles based on their type
        for (var entry : handles.entrySet()) {
            String handleId = entry.getKey();
            Circle handle = entry.getValue();
            HandleInfo info = handleInfoMap.get(handleId);

            if (info.isOutput()) {
                setupOutputHandleInteractions(handle, handleId);
            } else {
                setupInputHandleInteractions(handle, handleId);
            }
        }
    }

    /**
     * Sets up interaction handlers for output handles.
     */
    private void setupOutputHandleInteractions(Circle handle, String handleId) {
        handle.setOnMousePressed(e -> startConnectionDrag(e, handleId));
        handle.setOnMouseDragged(e -> updateConnectionDrag(e, handle));
        handle.setOnMouseReleased(e -> endConnectionDrag(e, handle));
    }

    /**
     * Sets up interaction handlers for input handles.
     */
    private void setupInputHandleInteractions(Circle handle, String handleId) {
        handle.setOnMousePressed(javafx.scene.input.MouseEvent::consume);
        handle.setOnMouseReleased(e -> {
            if (canvas.isConnectionDragging()) {
                canvas.completeConnection(this, handleId);
            }
            e.consume();
        });

        handle.setOnMouseEntered(e -> handleInputMouseEntered(e, handle));
        handle.setOnMouseExited(e -> handleInputMouseExited(e, handle));
    }

    /**
     * Handles mouse enter event for input handles.
     */
    private void handleInputMouseEntered(javafx.scene.input.MouseEvent e, Circle handle) {
        if (canvas.isConnectionDragging()) {
            highlightInputHandle(handle, true);
            canvas.setHoveredTarget(this, (String) handle.getUserData());
        } else {
            handle.setFill(HANDLE_HOVER_COLOR);
            handle.setScaleX(1.3);
            handle.setScaleY(1.3);
        }
    }

    /**
     * Handles mouse exit event for input handles.
     */
    private void handleInputMouseExited(javafx.scene.input.MouseEvent e, Circle handle) {
        if (canvas.isConnectionDragging()) {
            highlightInputHandle(handle, false);
            canvas.setHoveredTarget(null, null);
        } else {
            handle.setFill(INPUT_HANDLE_COLOR);
            handle.setScaleX(1.0);
            handle.setScaleY(1.0);
        }
    }

    private void startConnectionDrag(javafx.scene.input.MouseEvent e, String handleId) {
        if (e.getButton() == MouseButton.PRIMARY) {
            canvas.startConnectionDrag(this, handleId);
            Circle handle = handles.get(handleId);
            if (handle != null) {
                handle.setFill(HANDLE_ACTIVE_COLOR);
            }
            e.consume();
        }
    }

    private void updateConnectionDrag(javafx.scene.input.MouseEvent e, Circle handle) {
        if (canvas.isConnectionDragging()) {
            javafx.geometry.Point2D scenePoint = handle.localToScene(e.getX(), e.getY());
            canvas.updateConnectionDrag(scenePoint.getX(), scenePoint.getY());
            e.consume();
        }
    }

    private void endConnectionDrag(javafx.scene.input.MouseEvent e, Circle handle) {
        handle.setFill(OUTPUT_HANDLE_COLOR);
        canvas.endConnectionDrag();
        e.consume();
    }

    /**
     * Highlights an input handle as a potential connection target.
     */
    private void highlightInputHandle(Circle handle, boolean highlight) {
        if (highlight) {
            handle.setFill(HANDLE_ACTIVE_COLOR);
            handle.setScaleX(1.5);
            handle.setScaleY(1.5);

            // Pulse glow effect on handle
            DropShadow handleGlow = new DropShadow();
            handleGlow.setColor(HANDLE_ACTIVE_COLOR);
            handleGlow.setRadius(15);
            handleGlow.setSpread(0.5);
            handle.setEffect(handleGlow);

            // Glow on node
            DropShadow glow = new DropShadow();
            glow.setColor(HANDLE_ACTIVE_COLOR);
            glow.setRadius(12);
            glow.setSpread(0.3);
            nodeBox.setEffect(glow);
        } else {
            handle.setFill(INPUT_HANDLE_COLOR);
            handle.setScaleX(1.0);
            handle.setScaleY(1.0);

            // Restore normal glow
            DropShadow handleGlow = new DropShadow();
            handleGlow.setColor(INPUT_HANDLE_COLOR);
            handleGlow.setRadius(10);
            handleGlow.setSpread(0.3);
            handle.setEffect(handleGlow);

            applyNodeShadow();
        }
    }

    /**
     * Highlights a specific input handle as target.
     * 
     * @param handleId  the handle to highlight
     * @param highlight true to highlight, false to restore
     */
    public void highlightAsTarget(String handleId, boolean highlight) {
        Circle handle = handles.get(handleId);
        if (handle != null) {
            HandleInfo info = handleInfoMap.get(handleId);
            if (info != null && info.isInput()) {
                highlightInputHandle(handle, highlight);
            }
        }
    }

    private void setupDragBehavior() {
        nodeBox.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragOffsetX = e.getSceneX() - getLayoutX();
                dragOffsetY = e.getSceneY() - getLayoutY();
                toFront();
                canvas.selectNode(this);
                e.consume();
            }
        });

        nodeBox.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !canvas.isConnectionDragging()) {
                double newX = e.getSceneX() - dragOffsetX;
                double newY = e.getSceneY() - dragOffsetY;

                // Apply snap-to-grid if enabled
                if (canvas.isSnapToGrid()) {
                    newX = canvas.snapPositionToGrid(newX);
                    newY = canvas.snapPositionToGrid(newY);
                }

                setLayoutX(newX);
                setLayoutY(newY);

                canvas.updateNodePosition(node.id(), newX, newY);
                e.consume();
            }
        });

        nodeBox.setCursor(Cursor.HAND);
    }

    private void setupClickBehavior() {
        nodeBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                canvas.selectNode(this, e.isControlDown());
                // Hide context menu when clicking on node
                if (currentContextMenu != null) {
                    currentContextMenu.hide();
                    currentContextMenu = null;
                }
                if (e.getClickCount() == 2) {
                    canvas.openNodeEditor(this);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                showContextMenu(e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
    }

    private void showContextMenu(double screenX, double screenY) {
        // Hide any existing context menu
        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }

        ContextMenu contextMenu = new ContextMenu();
        currentContextMenu = contextMenu;

        MenuItem editItem = new MenuItem("Open Editor");
        editItem.setGraphic(FontIcon.of(MaterialDesignP.PENCIL, 14));
        editItem.setOnAction(e -> canvas.openNodeEditor(this));

        MenuItem executeItem = new MenuItem("Execute Node");
        executeItem.setGraphic(FontIcon.of(MaterialDesignP.PLAY, 14));
        executeItem.setOnAction(e -> canvas.executeNode(this));

        MenuItem debugViewItem = new MenuItem("Debug View...");
        debugViewItem.setGraphic(FontIcon.of(MaterialDesignB.BUG_OUTLINE, 14));
        debugViewItem.setOnAction(e -> canvas.showNodeDebugView(this));

        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 14));
        duplicateItem.setOnAction(e -> canvas.duplicateNode(this));

        MenuItem toggleItem = new MenuItem(node.disabled() ? "Enable" : "Disable");
        toggleItem.setGraphic(FontIcon.of(node.disabled() ? MaterialDesignE.EYE : MaterialDesignE.EYE_OFF, 14));
        toggleItem.setOnAction(e -> canvas.toggleNodeEnabled(this));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setGraphic(FontIcon.of(MaterialDesignR.RENAME_BOX, 14));
        renameItem.setOnAction(e -> canvas.renameNode(this));

        MenuItem changeIconItem = new MenuItem("Change Icon...");
        changeIconItem.setGraphic(FontIcon.of(MaterialDesignP.PALETTE, 14));
        changeIconItem.setOnAction(e -> canvas.changeNodeIcon(this));

        MenuItem deleteConnectionsItem = new MenuItem("Delete All Connections");
        deleteConnectionsItem.setGraphic(FontIcon.of(MaterialDesignL.LINK_OFF, 14));
        deleteConnectionsItem.setOnAction(e -> canvas.deleteAllConnectionsForNode(node.id()));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(FontIcon.of(MaterialDesignD.DELETE, 14));
        deleteItem.setStyle("-fx-text-fill: #fa5252;");
        deleteItem.setOnAction(e -> canvas.deleteNode(this));

        contextMenu.getItems().addAll(
                editItem,
                executeItem,
                debugViewItem,
                new SeparatorMenuItem(),
                duplicateItem,
                toggleItem,
                renameItem,
                changeIconItem,
                new SeparatorMenuItem(),
                deleteConnectionsItem,
                deleteItem);

        // Add developer mode items if enabled
        if (canvas.isDevModeEnabled()) {
            MenuItem copyJsonItem = new MenuItem("Copy Node JSON");
            copyJsonItem.setGraphic(FontIcon.of(MaterialDesignC.CODE_JSON, 14));
            copyJsonItem.setOnAction(e -> canvas.copyNodeAsJson(this));

            MenuItem showNodeIdItem = new MenuItem("Copy Node ID");
            showNodeIdItem.setGraphic(FontIcon.of(MaterialDesignI.IDENTIFIER, 14));
            showNodeIdItem.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(node.id());
                clipboard.setContent(content);
            });

            contextMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    copyJsonItem,
                    showNodeIdItem);
        }

        contextMenu.show(this, screenX, screenY);
    }

    private void applyNodeShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.6));
        shadow.setRadius(6);
        shadow.setOffsetY(2);
        nodeBox.setEffect(shadow);
    }

    private void applyNodeTypeStyle() {
        // Apply colored border based on node type
        Color borderColor = getBorderColorForType(node.type());
        background.setStroke(borderColor);

        String styleClass = switch (node.type()) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> "node-trigger";
            case "httpRequest", "code", "executeCommand" -> "node-action";
            case "if", "switch", "merge", "loop" -> "node-flow";
            case "set", "filter", "sort" -> "node-data";
            case "llmChat", "textClassifier", "embedding", "rag" -> "node-ai";
            default -> getStyleClassForCategory(node.type());
        };
        getStyleClass().add(styleClass);
    }

    /**
     * Gets the CSS style class based on the plugin's category.
     */
    private String getStyleClassForCategory(String type) {
        PluginServiceInterface.PluginInfo plugin = getPluginInfo(type);
        if (plugin != null && plugin.category() != null) {
            return switch (plugin.category()) {
                case TRIGGER -> "node-trigger";
                case ACTION -> "node-action";
                case FLOW -> "node-flow";
                case DATA -> "node-data";
                case AI -> "node-ai";
                case UTILITY -> "node-utility";
                default -> "node-default";
            };
        }
        return "node-default";
    }

    /**
     * Set the selection state of the node.
     *
     * @param selected true to select the node, false to deselect
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            background.setStroke(Color.web("#60a5fa"));
            background.setStrokeWidth(2.5);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#60a5fa"));
            glow.setRadius(10);
            glow.setSpread(0.2);
            nodeBox.setEffect(glow);
        } else {
            background.setStroke(getBorderColorForType(node.type()));
            background.setStrokeWidth(1.5);
            applyNodeShadow();
        }
    }

    /**
     * Check if the node is currently selected.
     *
     * @return true if the node is selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Get the domain node represented by this view.
     *
     * @return the domain node
     */
    public Node getNode() {
        return node;
    }

    /**
     * Get the input connection handle circle.
     *
     * @return the input handle circle
     */
    public Circle getInputHandle() {
        return inputHandle;
    }

    /**
     * Get the output connection handle circle.
     *
     * @return the output handle circle
     */
    public Circle getOutputHandle() {
        return outputHandle;
    }

    /**
     * Calculate the horizontal offset of the node box within this NodeView.
     * When the label is wider than the node box, the container centers everything,
     * so the node box is offset from layoutX by half the difference.
     */
    private double getNodeBoxOffsetX() {
        // The actual width of this NodeView may be wider than NODE_SIZE due to labels
        double actualWidth = getWidth();
        if (actualWidth <= 0) {
            // Before layout, assume no offset
            actualWidth = NODE_SIZE;
        }
        // The node box is centered within the container
        return (actualWidth - NODE_SIZE) / 2;
    }

    // Connection point coordinates - center of the handles

    /**
     * Gets the X coordinate for a specific handle.
     * 
     * @param handleId the handle identifier
     * @return X coordinate in canvas space
     */
    public double getHandleX(String handleId) {
        Circle handle = handles.get(handleId);
        HandleInfo info = handleInfoMap.get(handleId);
        if (handle == null || info == null) {
            // Fall back to default position
            return info != null && info.isInput() ? getInputX() : getOutputX();
        }

        double baseX = getLayoutX() + getNodeBoxOffsetX();
        return switch (info.position()) {
            case LEFT -> baseX - HANDLE_RADIUS;
            case RIGHT -> baseX + NODE_SIZE + HANDLE_RADIUS;
            case TOP, BOTTOM -> baseX + NODE_SIZE / 2 + handle.getTranslateX();
        };
    }

    /**
     * Gets the Y coordinate for a specific handle.
     * 
     * @param handleId the handle identifier
     * @return Y coordinate in canvas space
     */
    public double getHandleY(String handleId) {
        Circle handle = handles.get(handleId);
        HandleInfo info = handleInfoMap.get(handleId);
        if (handle == null || info == null) {
            // Fall back to default position
            return getLayoutY() + NODE_SIZE / 2;
        }

        double baseY = getLayoutY();
        return switch (info.position()) {
            case LEFT, RIGHT -> baseY + NODE_SIZE / 2 + handle.getTranslateY();
            case TOP -> baseY - HANDLE_RADIUS;
            case BOTTOM -> baseY + NODE_SIZE + HANDLE_RADIUS;
        };
    }

    /**
     * Legacy method - gets X coordinate of first input handle.
     *
     * @return the X coordinate of the input handle
     */
    public double getInputX() {
        // Input handle is at the left edge of the node box minus handle radius
        // Account for any offset caused by wider labels
        return getLayoutX() + getNodeBoxOffsetX() - HANDLE_RADIUS;
    }

    /**
     * Legacy method - gets Y coordinate of first input handle.
     *
     * @return the Y coordinate of the input handle
     */
    public double getInputY() {
        return getLayoutY() + NODE_SIZE / 2;
    }

    /**
     * Legacy method - gets X coordinate of first output handle.
     *
     * @return the X coordinate of the output handle
     */
    public double getOutputX() {
        // Output handle is at the right edge of the node box plus handle radius
        // Account for any offset caused by wider labels
        return getLayoutX() + getNodeBoxOffsetX() + NODE_SIZE + HANDLE_RADIUS;
    }

    /**
     * Legacy method - gets Y coordinate of first output handle.
     *
     * @return the Y coordinate of the output handle
     */
    public double getOutputY() {
        return getLayoutY() + NODE_SIZE / 2;
    }

    /**
     * Set the execution state and update visual indicators.
     *
     * @param state the execution state to set
     */
    public void setExecutionState(ExecutionState state) {
        this.executionState = state;

        // Reset visuals
        executionIndicator.setVisible(false);
        executionBadge.setVisible(false);
        nodeBox.getStyleClass().removeAll("node-running", "node-queued", "node-success", "node-error", "node-skipped");

        switch (state) {
            case IDLE -> {
                // Default state, no indicator
            }
            case QUEUED -> {
                nodeBox.getStyleClass().add("node-queued");
                executionBadge.setVisible(true);
                executionBadge.setText("⏱");
                executionBadge.setStyle(
                        "-fx-background-color: #6b7280; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 11px;");
            }
            case RUNNING -> {
                nodeBox.getStyleClass().add("node-running");
                executionIndicator.setVisible(true);

                // Add subtle pulsing border animation
                DropShadow runningGlow = new DropShadow();
                runningGlow.setColor(Color.web("#3b82f6"));
                runningGlow.setRadius(8);
                runningGlow.setSpread(0.2);
                nodeBox.setEffect(runningGlow);
            }
            case SUCCESS -> {
                nodeBox.getStyleClass().add("node-success");
                executionBadge.setVisible(true);
                executionBadge.setText("✓");
                executionBadge.setStyle(
                        "-fx-background-color: #10b981; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

                // Subtle green glow briefly
                DropShadow successGlow = new DropShadow();
                successGlow.setColor(Color.web("#10b981"));
                successGlow.setRadius(6);
                successGlow.setSpread(0.15);
                nodeBox.setEffect(successGlow);
            }
            case ERROR -> {
                nodeBox.getStyleClass().add("node-error");
                executionBadge.setVisible(true);
                executionBadge.setText("✕");
                executionBadge.setStyle(
                        "-fx-background-color: #ef4444; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

                // Subtle red glow
                DropShadow errorGlow = new DropShadow();
                errorGlow.setColor(Color.web("#ef4444"));
                errorGlow.setRadius(8);
                errorGlow.setSpread(0.2);
                nodeBox.setEffect(errorGlow);
            }
            case SKIPPED -> {
                nodeBox.getStyleClass().add("node-skipped");
                executionBadge.setVisible(true);
                executionBadge.setText("⊘");
                executionBadge.setStyle(
                        "-fx-background-color: #737373; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 11px;");
            }
        }
    }

    /**
     * Get the current execution state.
     *
     * @return the current execution state
     */
    public ExecutionState getExecutionState() {
        return executionState;
    }

    /**
     * Reset execution state to IDLE and restore normal shadow.
     */
    public void resetExecutionState() {
        setExecutionState(ExecutionState.IDLE);
        clearError();
        applyNodeShadow();
    }

    /**
     * Set an error message to display on the node.
     *
     * @param message the error message to display
     */
    public void setError(String message) {
        this.errorMessage = message;
        if (message != null && !message.isEmpty()) {
            // Show truncated message in label
            String displayText = message.length() > 30
                    ? message.substring(0, 27) + "..."
                    : message;
            errorLabel.setText("⚠ " + displayText);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            // Full message in tooltip
            errorTooltip.setText(message);
            Tooltip.install(errorLabel, errorTooltip);

            // Set error execution state
            setExecutionState(ExecutionState.ERROR);
        } else {
            clearError();
        }
    }

    /**
     * Get the current error message.
     *
     * @return the error message, or null if no error
     */
    public String getError() {
        return errorMessage;
    }

    /**
     * Clear the error message from the node.
     */
    public void clearError() {
        this.errorMessage = null;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        Tooltip.uninstall(errorLabel, errorTooltip);
    }

    /**
     * Check if the node has an error.
     *
     * @return true if the node has an error message
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * Check if this node can be a source for connections.
     *
     * @return true if this node can be a connection source
     */
    public boolean canBeConnectionSource() {
        return true;
    }

    /**
     * Check if this node can be a target for connections.
     *
     * @return true if this node can be a connection target
     */
    public boolean canBeConnectionTarget() {
        return !node.type().endsWith("Trigger");
    }

    /**
     * Check if this node can connect to the specified target node.
     *
     * @param target the target node to check
     * @return true if connection is allowed
     */
    public boolean canConnectTo(NodeView target) {
        if (target == this)
            return false;
        if (!target.canBeConnectionTarget())
            return false;
        return !canvas.hasConnection(this.node.id(), target.node.id());
    }

    /**
     * Update the icon display from the node's parameters. Checks for customIcon
     * parameter first, falls back to type-based icon.
     */
    private void updateIconFromNode() {
        String customIcon = getCustomIconCode();
        if (customIcon != null && !customIcon.isBlank()) {
            try {
                icon.setIconLiteral(customIcon);
                icon.setIconColor(getIconColorForType(node.type()));
                icon.setIconSize(42);
                return;
            } catch (Exception _) {
                // Invalid icon code, fall back to type-based
            }
        }
        // Default to type-based icon
        icon.setIconCode(getIconForType(node.type()));
        icon.setIconColor(getIconColorForType(node.type()));
        icon.setIconSize(42);
    }

    /**
     * Get the custom icon code from node parameters.
     *
     * @return The custom icon code, or null if not set
     */
    public String getCustomIconCode() {
        if (node.parameters() != null) {
            Object customIcon = node.parameters().get("customIcon");
            if (customIcon instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Update the node's icon. Called when user selects a new icon.
     *
     * @param iconCode The Ikonli icon code (e.g., "mdi2-c-cloud"), or null to reset
     *                 to default
     */
    public void updateIcon(String iconCode) {
        if (iconCode != null && !iconCode.isBlank()) {
            try {
                icon.setIconLiteral(iconCode);
                icon.setIconColor(getIconColorForType(node.type()));
                icon.setIconSize(42);
            } catch (Exception _) {
                // Invalid icon, revert to type-based
                icon.setIconCode(getIconForType(node.type()));
                icon.setIconColor(getIconColorForType(node.type()));
                icon.setIconSize(42);
            }
        } else {
            // Reset to default type-based icon
            icon.setIconCode(getIconForType(node.type()));
            icon.setIconColor(getIconColorForType(node.type()));
            icon.setIconSize(42);
        }
    }

    /**
     * Gets plugin info for the given node type, if available.
     */
    private PluginServiceInterface.PluginInfo getPluginInfo(String type) {
        if (pluginService == null) {
            return null;
        }
        return pluginService.getAllDiscoveredPlugins().stream()
                .filter(p -> p.id().equals(type) && p.enabled())
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the border color for the given node category.
     */
    private Color getBorderColorForCategory(ai.nervemind.common.enums.NodeCategory category) {
        if (category == null) {
            return Color.web("#525252"); // Gray
        }
        return switch (category) {
            case TRIGGER -> Color.web("#f59e0b"); // Amber
            case ACTION -> Color.web("#525252"); // Gray
            case FLOW -> Color.web("#10b981"); // Emerald
            case DATA -> Color.web("#8b5cf6"); // Violet
            case AI -> Color.web("#ec4899"); // Pink
            case UTILITY -> Color.web("#f97316"); // Orange
            default -> Color.web("#525252"); // Gray
        };
    }

    /**
     * Gets the icon color for the given node category.
     */
    private Color getIconColorForCategory(ai.nervemind.common.enums.NodeCategory category) {
        if (category == null) {
            return Color.web("#a1a1aa"); // Gray
        }
        return switch (category) {
            case TRIGGER -> Color.web("#fbbf24"); // Amber bright
            case ACTION -> Color.web("#60a5fa"); // Blue
            case FLOW -> Color.web("#34d399"); // Emerald bright
            case DATA -> Color.web("#a78bfa"); // Violet bright
            case AI -> Color.web("#f472b6"); // Pink bright
            case UTILITY -> Color.web("#fb923c"); // Orange bright
            default -> Color.web("#a1a1aa"); // Gray
        };
    }

    private Color getBorderColorForType(String type) {
        // First check built-in types
        Color builtIn = switch (type) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> Color.web("#f59e0b"); // Amber
            case "httpRequest", "code", "executeCommand" -> Color.web("#525252"); // Gray
            case "if", "switch", "merge", "loop" -> Color.web("#10b981"); // Emerald
            case "set", "filter", "sort" -> Color.web("#8b5cf6"); // Violet
            case "llmChat", "textClassifier", "embedding", "rag" -> Color.web("#ec4899"); // Pink
            case "subworkflow", "parallel", "tryCatch", "retry", "rate_limit" -> Color.web("#f97316"); // Orange
            default -> null;
        };
        if (builtIn != null) {
            return builtIn;
        }
        // Check plugin for color based on category
        PluginServiceInterface.PluginInfo plugin = getPluginInfo(type);
        if (plugin != null) {
            return getBorderColorForCategory(plugin.category());
        }
        return Color.web("#525252"); // Gray default
    }

    private Color getIconColorForType(String type) {
        // First check built-in types
        Color builtIn = switch (type) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> Color.web("#fbbf24"); // Amber bright
            case "httpRequest", "code", "executeCommand" -> Color.web("#60a5fa"); // Blue
            case "if", "switch", "merge", "loop" -> Color.web("#34d399"); // Emerald bright
            case "set", "filter", "sort" -> Color.web("#a78bfa"); // Violet bright
            case "llmChat", "textClassifier", "embedding", "rag" -> Color.web("#f472b6"); // Pink bright
            case "subworkflow", "parallel", "tryCatch", "retry", "rate_limit" -> Color.web("#fb923c"); // Orange bright
            default -> null;
        };
        if (builtIn != null) {
            return builtIn;
        }
        // Check plugin for color based on category
        PluginServiceInterface.PluginInfo plugin = getPluginInfo(type);
        if (plugin != null) {
            return getIconColorForCategory(plugin.category());
        }
        return Color.web("#a1a1aa"); // Gray default
    }

    private org.kordamp.ikonli.Ikon getIconForType(String type) {
        // First check built-in types
        org.kordamp.ikonli.Ikon builtIn = switch (type) {
            case "manualTrigger" -> MaterialDesignP.PLAY_CIRCLE;
            case "scheduleTrigger" -> MaterialDesignC.CLOCK_OUTLINE;
            case "webhookTrigger" -> MaterialDesignW.WEBHOOK;
            case "httpRequest" -> MaterialDesignW.WEB;
            case "code" -> MaterialDesignC.CODE_BRACES;
            case "executeCommand" -> MaterialDesignC.CONSOLE;
            case "if" -> MaterialDesignC.CALL_SPLIT;
            case "switch" -> MaterialDesignS.SWAP_HORIZONTAL;
            case "merge" -> MaterialDesignC.CALL_MERGE;
            case "loop" -> MaterialDesignR.REPEAT;
            case "set" -> MaterialDesignP.PENCIL;
            case "filter" -> MaterialDesignF.FILTER_OUTLINE;
            case "sort" -> MaterialDesignS.SORT_ASCENDING;
            case "llmChat" -> MaterialDesignR.ROBOT;
            case "textClassifier" -> MaterialDesignT.TAG_TEXT_OUTLINE;
            case "embedding" -> MaterialDesignV.VECTOR_BEZIER;
            case "rag" -> MaterialDesignB.BOOK_SEARCH;
            case "subworkflow" -> MaterialDesignS.SITEMAP;
            case "parallel" -> MaterialDesignF.FORMAT_ALIGN_JUSTIFY;
            case "tryCatch" -> MaterialDesignS.SHIELD_CHECK;
            case "retry" -> MaterialDesignR.REFRESH;
            case "rate_limit" -> MaterialDesignS.SPEEDOMETER;
            default -> null;
        };
        if (builtIn != null) {
            return builtIn;
        }
        // Check plugin for icon
        PluginServiceInterface.PluginInfo plugin = getPluginInfo(type);
        if (plugin != null && plugin.iconName() != null) {
            return ai.nervemind.ui.util.IconResolver.resolve(plugin.iconName());
        }
        return MaterialDesignC.CUBE_OUTLINE;
    }

    private String getSubtitleForType(String type) {
        // First check built-in types
        String builtIn = switch (type) {
            case "manualTrigger" -> "trigger";
            case "scheduleTrigger" -> "schedule";
            case "webhookTrigger" -> "webhook";
            case "httpRequest" -> "HTTP";
            case "code" -> "code";
            case "executeCommand" -> "command";
            case "if" -> "condition";
            case "switch" -> "router";
            case "merge" -> "merge";
            case "loop" -> "loop";
            case "set" -> "transform";
            case "filter" -> "filter";
            case "sort" -> "sort";
            case "llmChat" -> "AI chat";
            case "textClassifier" -> "classifier";
            case "embedding" -> "vectors";
            case "rag" -> "retrieval";
            case "subworkflow" -> "nested";
            case "parallel" -> "parallel";
            case "tryCatch" -> "error handling";
            case "retry" -> "retry";
            case "rate_limit" -> "throttle";
            default -> null;
        };
        if (builtIn != null) {
            return builtIn;
        }
        // Check plugin for subtitle
        PluginServiceInterface.PluginInfo plugin = getPluginInfo(type);
        if (plugin != null && plugin.subtitle() != null && !plugin.subtitle().isBlank()) {
            return plugin.subtitle();
        }
        return type;
    }
}
