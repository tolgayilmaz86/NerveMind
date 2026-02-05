package ai.nervemind.ui.canvas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.service.PluginServiceInterface.HandleInfo;
import ai.nervemind.common.service.PluginServiceInterface.MenuLocation;
import ai.nervemind.ui.service.PluginMenuManager;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;

/**
 * Visual representation of a connection between two nodes.
 * Uses a bezier curve with an arrow at the end.
 * Supports click selection and context menu for deletion.
 * 
 * <p>
 * Supports handle-specific connections where the curve starts and ends at
 * specific handles on the source and target nodes (not just the default
 * output/input handles).
 * </p>
 * 
 * <p>
 * Provides a generic decoration API allowing plugins to add visual elements
 * (like labels, badges, etc.) at the connection's midpoint. The core connection
 * class does not know about specific decoration types - plugins manage their
 * own
 * visual components.
 * </p>
 */
public class ConnectionLine extends Group {

    private static final Color CONNECTION_COLOR = Color.web("#4a9eff");
    private static final Color CONNECTION_SELECTED_COLOR = Color.web("#ffd43b");
    private static final Color CONNECTION_HOVER_COLOR = Color.web("#7ab8ff");
    private static final double STROKE_WIDTH = 2;
    private static final double HIT_AREA_STROKE_WIDTH = 12; // Wider area for easier clicking
    private static final double HANDLE_RADIUS = 8;

    private final Connection connection;
    private final NodeView sourceNode;
    private final NodeView targetNode;
    private final WorkflowCanvas canvas;

    private final CubicCurve curve;
    private final CubicCurve hitArea; // Invisible wider curve for easier clicking
    private final Polygon arrow;

    // Generic decoration support - plugins can add visual elements
    private final Map<String, Node> decorations = new HashMap<>();
    private final Group decorationLayer;

    private boolean selected = false;
    private ContextMenu currentContextMenu = null;

    // Optional plugin menu manager for context menu extensions
    private PluginMenuManager pluginMenuManager;

    /**
     * Creates a new connection line visual.
     *
     * @param connection the underlying connection model
     * @param sourceNode the visual node which is the source of this connection
     * @param targetNode the visual node which is the target of this connection
     * @param canvas     the parent workflow canvas
     */
    public ConnectionLine(Connection connection, NodeView sourceNode, NodeView targetNode, WorkflowCanvas canvas) {
        this.connection = connection;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.canvas = canvas;

        // Create nearly invisible hit area curve (wider for easier clicking)
        // Note: Using very low opacity instead of TRANSPARENT because transparent
        // doesn't receive mouse events
        hitArea = new CubicCurve();
        hitArea.setFill(null);
        hitArea.setStroke(Color.rgb(0, 0, 0, 0.01)); // Nearly invisible but receives mouse events
        hitArea.setStrokeWidth(HIT_AREA_STROKE_WIDTH);
        hitArea.setPickOnBounds(false); // Use actual curve shape for hit testing

        // Create visible bezier curve
        curve = new CubicCurve();
        curve.setFill(null);
        curve.setStroke(CONNECTION_COLOR);
        curve.setStrokeWidth(STROKE_WIDTH);
        curve.getStyleClass().add("connection-line");
        curve.setMouseTransparent(true); // Let hit area handle mouse events

        // Create arrow
        arrow = new Polygon();
        arrow.setFill(CONNECTION_COLOR);
        arrow.getStyleClass().add("connection-arrow");
        arrow.setMouseTransparent(true); // Let hit area handle mouse events

        // Create decoration layer for plugin-added visuals
        decorationLayer = new Group();
        decorationLayer.setMouseTransparent(true);

        getChildren().addAll(hitArea, curve, arrow, decorationLayer);

        // Setup mouse interactions
        setupMouseHandlers();

        // Bind positions
        setupBindings();

        // Initial position
        updatePosition();
    }

    private void setupBindings() {
        // Update when source or target moves
        sourceNode.layoutXProperty().addListener((obs, old, val) -> updatePosition());
        sourceNode.layoutYProperty().addListener((obs, old, val) -> updatePosition());
        targetNode.layoutXProperty().addListener((obs, old, val) -> updatePosition());
        targetNode.layoutYProperty().addListener((obs, old, val) -> updatePosition());

        // Also update when node width changes (can affect handle position due to label
        // width)
        sourceNode.widthProperty().addListener((obs, old, val) -> updatePosition());
        targetNode.widthProperty().addListener((obs, old, val) -> updatePosition());
    }

    /**
     * Updates the connection line position based on source and target handles.
     * Uses handle-specific coordinates when available.
     */
    public void updatePosition() {
        // Get handle IDs from the connection
        String sourceHandleId = connection.sourceOutput();
        String targetHandleId = connection.targetInput();

        // Source point (specific output handle or default)
        double sourceCenterX;
        double sourceCenterY;
        HandleInfo sourceHandleInfo = sourceNode.getHandleInfo(sourceHandleId);
        if (sourceHandleInfo != null) {
            sourceCenterX = sourceNode.getHandleX(sourceHandleId);
            sourceCenterY = sourceNode.getHandleY(sourceHandleId);
        } else {
            // Fallback to default output handle
            sourceCenterX = sourceNode.getOutputX();
            sourceCenterY = sourceNode.getOutputY();
        }

        // Target point (specific input handle or default)
        double targetCenterX;
        double targetCenterY;
        HandleInfo targetHandleInfo = targetNode.getHandleInfo(targetHandleId);
        if (targetHandleInfo != null) {
            targetCenterX = targetNode.getHandleX(targetHandleId);
            targetCenterY = targetNode.getHandleY(targetHandleId);
        } else {
            // Fallback to default input handle
            targetCenterX = targetNode.getInputX();
            targetCenterY = targetNode.getInputY();
        }

        // Determine curve direction based on handle positions
        HandleInfo.Position sourcePos = sourceHandleInfo != null ? sourceHandleInfo.position()
                : HandleInfo.Position.RIGHT;
        HandleInfo.Position targetPos = targetHandleInfo != null ? targetHandleInfo.position()
                : HandleInfo.Position.LEFT;

        // Calculate start and end points (offset from center by handle radius)
        double[] startPoint = calculateEdgePoint(sourceCenterX, sourceCenterY, sourcePos);
        double[] endPoint = calculateEdgePoint(targetCenterX, targetCenterY, targetPos);

        double startX = startPoint[0];
        double startY = startPoint[1];
        double endX = endPoint[0];
        double endY = endPoint[1];

        // Calculate control points based on handle positions
        double[] controlPoints = calculateControlPoints(startX, startY, endX, endY, sourcePos, targetPos);

        // Set visible curve points
        curve.setStartX(startX);
        curve.setStartY(startY);
        curve.setControlX1(controlPoints[0]);
        curve.setControlY1(controlPoints[1]);
        curve.setControlX2(controlPoints[2]);
        curve.setControlY2(controlPoints[3]);
        curve.setEndX(endX);
        curve.setEndY(endY);

        // Set hit area curve points (same shape, wider stroke)
        hitArea.setStartX(startX);
        hitArea.setStartY(startY);
        hitArea.setControlX1(controlPoints[0]);
        hitArea.setControlY1(controlPoints[1]);
        hitArea.setControlX2(controlPoints[2]);
        hitArea.setControlY2(controlPoints[3]);
        hitArea.setEndX(endX);
        hitArea.setEndY(endY);

        // Update arrow position and rotation
        updateArrow(controlPoints[2], controlPoints[3], endX, endY);

        // Update decoration positions
        updateDecorationPositions();
    }

    /**
     * Calculates the edge point (where the line starts/ends) based on handle
     * position.
     */
    private double[] calculateEdgePoint(double centerX, double centerY, HandleInfo.Position position) {
        double x = centerX;
        double y = centerY;

        // Offset from center based on position (line starts/ends at edge of handle)
        switch (position) {
            case LEFT -> x = centerX - HANDLE_RADIUS;
            case RIGHT -> x = centerX + HANDLE_RADIUS;
            case TOP -> y = centerY - HANDLE_RADIUS;
            case BOTTOM -> y = centerY + HANDLE_RADIUS;
        }

        return new double[] { x, y };
    }

    /**
     * Calculates bezier control points based on source and target handle positions.
     * Creates smooth curves that respect the direction of each handle.
     */
    private double[] calculateControlPoints(double startX, double startY, double endX, double endY,
            HandleInfo.Position sourcePos, HandleInfo.Position targetPos) {
        // Control point distance based on connection length
        double dx = Math.abs(endX - startX);
        double dy = Math.abs(endY - startY);
        double ctrlDist = Math.max(Math.min(dx, dy) * 0.5, 30);

        double ctrlX1;
        double ctrlY1;
        double ctrlX2;
        double ctrlY2;

        // Source control point - extends in the direction of the handle
        switch (sourcePos) {
            case RIGHT -> {
                ctrlX1 = startX + ctrlDist;
                ctrlY1 = startY;
            }
            case LEFT -> {
                ctrlX1 = startX - ctrlDist;
                ctrlY1 = startY;
            }
            case BOTTOM -> {
                ctrlX1 = startX;
                ctrlY1 = startY + ctrlDist;
            }
            case TOP -> {
                ctrlX1 = startX;
                ctrlY1 = startY - ctrlDist;
            }
            default -> {
                ctrlX1 = startX + ctrlDist;
                ctrlY1 = startY;
            }
        }

        // Target control point - extends in the opposite direction of the handle
        switch (targetPos) {
            case LEFT -> {
                ctrlX2 = endX - ctrlDist;
                ctrlY2 = endY;
            }
            case RIGHT -> {
                ctrlX2 = endX + ctrlDist;
                ctrlY2 = endY;
            }
            case TOP -> {
                ctrlX2 = endX;
                ctrlY2 = endY - ctrlDist;
            }
            case BOTTOM -> {
                ctrlX2 = endX;
                ctrlY2 = endY + ctrlDist;
            }
            default -> {
                ctrlX2 = endX - ctrlDist;
                ctrlY2 = endY;
            }
        }

        return new double[] { ctrlX1, ctrlY1, ctrlX2, ctrlY2 };
    }

    private void updateArrow(double fromX, double fromY, double toX, double toY) {
        double arrowSize = 8;
        double angle = Math.atan2(toY - fromY, toX - fromX);

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Arrow points
        double x1 = toX - arrowSize * cos - arrowSize * 0.5 * sin;
        double y1 = toY - arrowSize * sin + arrowSize * 0.5 * cos;
        double x2 = toX - arrowSize * cos + arrowSize * 0.5 * sin;
        double y2 = toY - arrowSize * sin - arrowSize * 0.5 * cos;

        arrow.getPoints().setAll(
                toX, toY,
                x1, y1,
                x2, y2);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DECORATION API - Generic plugin extension point
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a decoration (visual element) to this connection.
     * 
     * <p>
     * Decorations are positioned at the midpoint of the connection curve.
     * The node should be pre-configured with its visual appearance; this method
     * only handles positioning.
     * </p>
     * 
     * @param key  a unique identifier for this decoration (for later removal)
     * @param node the JavaFX node to display
     */
    public void addDecoration(String key, Node node) {
        // Remove existing decoration with same key
        removeDecoration(key);

        decorations.put(key, node);
        decorationLayer.getChildren().add(node);

        // Position the decoration
        updateDecorationPositions();
    }

    /**
     * Removes a decoration from this connection.
     * 
     * @param key the identifier of the decoration to remove
     */
    public void removeDecoration(String key) {
        Node existing = decorations.remove(key);
        if (existing != null) {
            decorationLayer.getChildren().remove(existing);
        }
    }

    /**
     * Gets a decoration by its key.
     * 
     * @param key the decoration identifier
     * @return the decoration node, or null if not found
     */
    public Node getDecoration(String key) {
        return decorations.get(key);
    }

    /**
     * Checks if a decoration exists.
     * 
     * @param key the decoration identifier
     * @return true if the decoration exists
     */
    public boolean hasDecoration(String key) {
        return decorations.containsKey(key);
    }

    /**
     * Gets the midpoint coordinates of this connection's curve.
     * Useful for plugins that need to position elements relative to the connection.
     * 
     * @return array of [x, y] coordinates at the curve midpoint
     */
    public double[] getMidpoint() {
        double t = 0.5;
        double t1 = 1 - t;

        double startX = curve.getStartX();
        double startY = curve.getStartY();
        double ctrlX1 = curve.getControlX1();
        double ctrlY1 = curve.getControlY1();
        double ctrlX2 = curve.getControlX2();
        double ctrlY2 = curve.getControlY2();
        double endX = curve.getEndX();
        double endY = curve.getEndY();

        // De Casteljau's algorithm for cubic bezier at t=0.5
        double midX = t1 * t1 * t1 * startX
                + 3 * t1 * t1 * t * ctrlX1
                + 3 * t1 * t * t * ctrlX2
                + t * t * t * endX;
        double midY = t1 * t1 * t1 * startY
                + 3 * t1 * t1 * t * ctrlY1
                + 3 * t1 * t * t * ctrlY2
                + t * t * t * endY;

        return new double[] { midX, midY };
    }

    /**
     * Updates positions of all decorations to the curve midpoint.
     */
    private void updateDecorationPositions() {
        if (decorations.isEmpty()) {
            return;
        }

        double[] midpoint = getMidpoint();
        double midX = midpoint[0];
        double midY = midpoint[1];

        for (Node decoration : decorations.values()) {
            // Center the decoration on the midpoint
            decoration.setLayoutX(midX);
            decoration.setLayoutY(midY);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SELECTION AND STATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set the selected state of the connection.
     * 
     * @param selected true to highlight the connection
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        Color color = selected ? CONNECTION_SELECTED_COLOR : CONNECTION_COLOR;
        curve.setStroke(color);
        arrow.setFill(color);
        // Adjust stroke width for selected state
        curve.setStrokeWidth(selected ? STROKE_WIDTH + 1 : STROKE_WIDTH);
    }

    /**
     * Checks if the connection is selected.
     * 
     * @return true if the connection is currently selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Gets the underlying connection data.
     * 
     * @return the underlying connection data model
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Checks if either end of the connection is attached to the specified node.
     * 
     * @param nodeId the node ID to check
     * @return true if this connection involves the node
     */
    public boolean involvesNode(String nodeId) {
        return connection.sourceNodeId().equals(nodeId) ||
                connection.targetNodeId().equals(nodeId);
    }

    /**
     * Setup mouse event handlers for selection, hover effects, and context menu.
     */
    private void setupMouseHandlers() {
        // Hover effect
        hitArea.setOnMouseEntered(e -> {
            if (!selected) {
                curve.setStroke(CONNECTION_HOVER_COLOR);
                arrow.setFill(CONNECTION_HOVER_COLOR);
            }
            setCursor(javafx.scene.Cursor.HAND);
        });

        hitArea.setOnMouseExited(e -> {
            if (!selected) {
                curve.setStroke(CONNECTION_COLOR);
                arrow.setFill(CONNECTION_COLOR);
            }
            setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Click to select
        hitArea.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                // Ctrl+click for multi-select, otherwise single select
                boolean multiSelect = e.isControlDown();
                canvas.selectConnection(this, multiSelect);
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // Right-click shows context menu
                // First select this connection if not already selected
                if (!selected) {
                    canvas.selectConnection(this, false);
                }
                showContextMenu(e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });
    }

    /**
     * Sets the plugin menu manager for adding plugin context menu items.
     * 
     * @param pluginMenuManager the plugin menu manager, or null to disable
     */
    public void setPluginMenuManager(PluginMenuManager pluginMenuManager) {
        this.pluginMenuManager = pluginMenuManager;
    }

    /**
     * Show context menu for this connection.
     * Includes built-in actions and any plugin-contributed items.
     */
    private void showContextMenu(double screenX, double screenY) {
        // Hide any existing context menu
        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }

        ContextMenu contextMenu = new ContextMenu();
        currentContextMenu = contextMenu;

        // Built-in delete action
        MenuItem deleteItem = new MenuItem("Delete Connection");
        deleteItem.setGraphic(FontIcon.of(MaterialDesignD.DELETE, 14));
        deleteItem.setStyle("-fx-text-fill: #fa5252;");
        deleteItem.setOnAction(e -> canvas.deleteSelectedConnections());

        contextMenu.getItems().add(deleteItem);

        // Add plugin-contributed menu items
        if (pluginMenuManager != null) {
            Map<String, Object> context = buildContextMap();
            List<MenuItem> pluginItems = pluginMenuManager.getMenuItemsForLocation(
                    MenuLocation.CONNECTION_CONTEXT, context);
            contextMenu.getItems().addAll(pluginItems);
        }

        contextMenu.show(this, screenX, screenY);
    }

    /**
     * Builds the context map for plugin menu actions.
     * Contains information about this connection for context-aware actions.
     */
    private Map<String, Object> buildContextMap() {
        Map<String, Object> context = new HashMap<>();
        context.put("connectionId", connection.id());
        context.put("sourceNodeId", connection.sourceNodeId());
        context.put("targetNodeId", connection.targetNodeId());
        context.put("sourceOutput", connection.sourceOutput());
        context.put("targetInput", connection.targetInput());

        // Add workflow ID if available through canvas
        Long workflowId = canvas.getWorkflowId();
        if (workflowId != null) {
            context.put("workflowId", workflowId);
        }

        // Add ConnectionContext for plugins to use
        context.put("connection", new ConnectionContextImpl(this, canvas));

        return context;
    }

    /**
     * Hide the context menu if visible.
     */
    public void hideContextMenu() {
        if (currentContextMenu != null) {
            currentContextMenu.hide();
            currentContextMenu = null;
        }
    }
}
