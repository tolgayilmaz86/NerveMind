/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import ai.nervemind.common.domain.Connection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for a connection between two nodes on the workflow canvas.
 *
 * <p>
 * Manages the state of a single connection including:
 * <ul>
 * <li>Source and target node IDs</li>
 * <li>Connection points (coordinates)</li>
 * <li>Selection and hover state</li>
 * <li>Execution state (for visual feedback during workflow execution)</li>
 * <li>Path calculation for bezier curves</li>
 * </ul>
 *
 * <p>
 * This ViewModel is designed to support MVVM-style binding for connection
 * rendering.
 */
public class ConnectionViewModel {

    /**
     * Execution states for visual feedback on connections.
     */
    public enum ConnectionState {
        /** Normal state - connection is idle */
        IDLE,
        /** Data is flowing through this connection */
        ACTIVE,
        /** Connection was used successfully */
        SUCCESS,
        /** Error occurred during data flow */
        ERROR
    }

    // Connection ID
    private final StringProperty connectionId = new SimpleStringProperty();

    // Source node
    private final StringProperty sourceNodeId = new SimpleStringProperty();
    private final DoubleProperty sourceX = new SimpleDoubleProperty();
    private final DoubleProperty sourceY = new SimpleDoubleProperty();

    // Target node
    private final StringProperty targetNodeId = new SimpleStringProperty();
    private final DoubleProperty targetX = new SimpleDoubleProperty();
    private final DoubleProperty targetY = new SimpleDoubleProperty();

    // State
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty hovered = new SimpleBooleanProperty(false);
    private final ObjectProperty<ConnectionState> state = new SimpleObjectProperty<>(ConnectionState.IDLE);

    // Bezier curve control points (calculated)
    private final DoubleProperty controlX1 = new SimpleDoubleProperty();
    private final DoubleProperty controlY1 = new SimpleDoubleProperty();
    private final DoubleProperty controlX2 = new SimpleDoubleProperty();
    private final DoubleProperty controlY2 = new SimpleDoubleProperty();

    // The underlying domain connection
    private final ObjectProperty<Connection> connection = new SimpleObjectProperty<>();

    // Callbacks
    private Runnable onSelected;
    private Runnable onDeleted;

    /**
     * Creates a new ConnectionViewModel with no initial connection.
     */
    public ConnectionViewModel() {
        setupBindings();
    }

    /**
     * Creates a new ConnectionViewModel from a domain Connection.
     *
     * @param connection the domain connection to wrap
     */
    public ConnectionViewModel(Connection connection) {
        this();
        loadFromConnection(connection);
    }

    /**
     * Creates a new ConnectionViewModel with source and target coordinates.
     *
     * @param sourceNodeId the source node ID
     * @param targetNodeId the target node ID
     * @param sourceX      the source X coordinate
     * @param sourceY      the source Y coordinate
     * @param targetX      the target X coordinate
     * @param targetY      the target Y coordinate
     */
    public ConnectionViewModel(String sourceNodeId, String targetNodeId,
            double sourceX, double sourceY, double targetX, double targetY) {
        this();
        this.sourceNodeId.set(sourceNodeId);
        this.targetNodeId.set(targetNodeId);
        updateEndpoints(sourceX, sourceY, targetX, targetY);
        generateConnectionId();
    }

    /**
     * Sets up internal bindings and listeners.
     */
    private void setupBindings() {
        // Recalculate control points when endpoints change
        sourceX.addListener((obs, old, newVal) -> calculateControlPoints());
        sourceY.addListener((obs, old, newVal) -> calculateControlPoints());
        targetX.addListener((obs, old, newVal) -> calculateControlPoints());
        targetY.addListener((obs, old, newVal) -> calculateControlPoints());
    }

    // ===== Loading & Saving =====

    /**
     * Loads state from a domain Connection.
     *
     * @param domainConnection the connection to load from
     */
    public void loadFromConnection(Connection domainConnection) {
        if (domainConnection == null) {
            clear();
            return;
        }

        this.connection.set(domainConnection);
        this.connectionId.set(domainConnection.id());
        this.sourceNodeId.set(domainConnection.sourceNodeId());
        this.targetNodeId.set(domainConnection.targetNodeId());

        // Reset state
        this.state.set(ConnectionState.IDLE);
        this.selected.set(false);
        this.hovered.set(false);
    }

    /**
     * Creates a new domain Connection from the current state.
     *
     * @return a new Connection with the current ViewModel state
     */
    public Connection toConnection() {
        String id = connectionId.get();
        if (id == null || id.isEmpty()) {
            generateConnectionId();
            id = connectionId.get();
        }
        // Connection has 5 params: id, sourceNodeId, sourceOutput, targetNodeId,
        // targetInput
        return new Connection(id, sourceNodeId.get(), "main", targetNodeId.get(), "main");
    }

    /**
     * Clears all state.
     */
    public void clear() {
        connection.set(null);
        connectionId.set(null);
        sourceNodeId.set(null);
        targetNodeId.set(null);
        sourceX.set(0);
        sourceY.set(0);
        targetX.set(0);
        targetY.set(0);
        state.set(ConnectionState.IDLE);
        selected.set(false);
        hovered.set(false);
    }

    /**
     * Generates a connection ID from source and target node IDs.
     */
    private void generateConnectionId() {
        String source = sourceNodeId.get();
        String target = targetNodeId.get();
        if (source != null && target != null) {
            connectionId.set(source + "->" + target);
        }
    }

    // ===== Endpoint Methods =====

    /**
     * Updates the endpoint coordinates.
     *
     * @param srcX source X
     * @param srcY source Y
     * @param tgtX target X
     * @param tgtY target Y
     */
    public void updateEndpoints(double srcX, double srcY, double tgtX, double tgtY) {
        sourceX.set(srcX);
        sourceY.set(srcY);
        targetX.set(tgtX);
        targetY.set(tgtY);
    }

    /**
     * Updates only the source endpoint.
     *
     * @param x source X
     * @param y source Y
     */
    public void updateSourceEndpoint(double x, double y) {
        sourceX.set(x);
        sourceY.set(y);
    }

    /**
     * Updates only the target endpoint.
     *
     * @param x target X
     * @param y target Y
     */
    public void updateTargetEndpoint(double x, double y) {
        targetX.set(x);
        targetY.set(y);
    }

    /**
     * Calculates the bezier curve control points for smooth curves.
     * Uses a horizontal offset based on the distance between endpoints.
     */
    private void calculateControlPoints() {
        double sx = sourceX.get();
        double sy = sourceY.get();
        double tx = targetX.get();
        double ty = targetY.get();

        // Calculate horizontal offset for control points
        double dx = Math.abs(tx - sx);
        double offset = Math.max(50, dx * 0.5);

        // Control point 1: horizontal offset from source
        controlX1.set(sx + offset);
        controlY1.set(sy);

        // Control point 2: horizontal offset back from target
        controlX2.set(tx - offset);
        controlY2.set(ty);
    }

    // ===== Geometry Helpers =====

    /**
     * Calculates the length of the connection (straight-line distance).
     *
     * @return the distance between endpoints
     */
    public double getLength() {
        double dx = targetX.get() - sourceX.get();
        double dy = targetY.get() - sourceY.get();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates the midpoint X coordinate.
     *
     * @return the midpoint X
     */
    public double getMidpointX() {
        return (sourceX.get() + targetX.get()) / 2;
    }

    /**
     * Calculates the midpoint Y coordinate.
     *
     * @return the midpoint Y
     */
    public double getMidpointY() {
        return (sourceY.get() + targetY.get()) / 2;
    }

    /**
     * Checks if a point is near the connection line.
     * Uses a simple bounding box check plus distance-to-curve calculation.
     *
     * @param x         the X coordinate to test
     * @param y         the Y coordinate to test
     * @param threshold the maximum distance to consider "near"
     * @return true if the point is near the connection
     */
    public boolean isNearPoint(double x, double y, double threshold) {
        // Quick bounding box check
        double minX = Math.min(sourceX.get(), targetX.get()) - threshold;
        double maxX = Math.max(sourceX.get(), targetX.get()) + threshold;
        double minY = Math.min(sourceY.get(), targetY.get()) - threshold;
        double maxY = Math.max(sourceY.get(), targetY.get()) + threshold;

        if (x < minX || x > maxX || y < minY || y > maxY) {
            return false;
        }

        // Sample points along the bezier curve and check distance
        for (double t = 0; t <= 1.0; t += 0.1) {
            double px = bezierPoint(t, sourceX.get(), controlX1.get(), controlX2.get(), targetX.get());
            double py = bezierPoint(t, sourceY.get(), controlY1.get(), controlY2.get(), targetY.get());
            double dist = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));
            if (dist <= threshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates a point on a cubic bezier curve.
     */
    private double bezierPoint(double t, double p0, double p1, double p2, double p3) {
        double u = 1 - t;
        return u * u * u * p0 +
                3 * u * u * t * p1 +
                3 * u * t * t * p2 +
                t * t * t * p3;
    }

    // ===== State Methods =====

    /**
     * Sets the connection state.
     *
     * @param newState the new state
     */
    public void setState(ConnectionState newState) {
        state.set(newState);
    }

    /**
     * Resets the connection state to IDLE.
     */
    public void resetState() {
        state.set(ConnectionState.IDLE);
    }

    /**
     * Checks if this connection involves the given node.
     *
     * @param nodeId the node ID to check
     * @return true if the node is either source or target
     */
    public boolean involvesNode(String nodeId) {
        return nodeId != null &&
                (nodeId.equals(sourceNodeId.get()) || nodeId.equals(targetNodeId.get()));
    }

    // ===== Property Accessors =====

    // Connection ID
    /**
     * Gets the connection ID property.
     * 
     * @return the connection ID property
     */
    public ReadOnlyStringProperty connectionIdProperty() {
        return connectionId;
    }

    public String getConnectionId() {
        return connectionId.get();
    }

    // Source Node ID
    /**
     * Gets the source node ID property.
     * 
     * @return the source node ID property
     */
    public ReadOnlyStringProperty sourceNodeIdProperty() {
        return sourceNodeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId.get();
    }

    // Target Node ID
    /**
     * Gets the target node ID property.
     * 
     * @return the target node ID property
     */
    public ReadOnlyStringProperty targetNodeIdProperty() {
        return targetNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId.get();
    }

    // Source X
    /**
     * Gets the source X coordinate property.
     * 
     * @return the source X coordinate property
     */
    public DoubleProperty sourceXProperty() {
        return sourceX;
    }

    public double getSourceX() {
        return sourceX.get();
    }

    // Source Y
    /**
     * Gets the source Y coordinate property.
     * 
     * @return the source Y coordinate property
     */
    public DoubleProperty sourceYProperty() {
        return sourceY;
    }

    public double getSourceY() {
        return sourceY.get();
    }

    // Target X
    /**
     * Gets the target X coordinate property.
     * 
     * @return the target X coordinate property
     */
    public DoubleProperty targetXProperty() {
        return targetX;
    }

    public double getTargetX() {
        return targetX.get();
    }

    // Target Y
    /**
     * Gets the target Y coordinate property.
     * 
     * @return the target Y coordinate property
     */
    public DoubleProperty targetYProperty() {
        return targetY;
    }

    public double getTargetY() {
        return targetY.get();
    }

    // Control Points (read-only for view binding)
    /**
     * Gets the first control point X property.
     * 
     * @return the first control point X property
     */
    public ReadOnlyDoubleProperty controlX1Property() {
        return controlX1;
    }

    public double getControlX1() {
        return controlX1.get();
    }

    /**
     * Gets the first control point Y property.
     * 
     * @return the first control point Y property
     */
    public ReadOnlyDoubleProperty controlY1Property() {
        return controlY1;
    }

    public double getControlY1() {
        return controlY1.get();
    }

    /**
     * Gets the second control point X property.
     * 
     * @return the second control point X property
     */
    public ReadOnlyDoubleProperty controlX2Property() {
        return controlX2;
    }

    public double getControlX2() {
        return controlX2.get();
    }

    /**
     * Gets the second control point Y property.
     * 
     * @return the second control point Y property
     */
    public ReadOnlyDoubleProperty controlY2Property() {
        return controlY2;
    }

    public double getControlY2() {
        return controlY2.get();
    }

    // Selected
    /**
     * Gets the selected state property.
     * 
     * @return the selected state property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
        if (selected && onSelected != null) {
            onSelected.run();
        }
    }

    // Hovered
    /**
     * Gets the hovered state property.
     * 
     * @return the hovered state property
     */
    public BooleanProperty hoveredProperty() {
        return hovered;
    }

    public boolean isHovered() {
        return hovered.get();
    }

    public void setHovered(boolean hovered) {
        this.hovered.set(hovered);
    }

    // State
    /**
     * Gets the connection execution state property.
     * 
     * @return the connection execution state property
     */
    public ObjectProperty<ConnectionState> stateProperty() {
        return state;
    }

    public ConnectionState getState() {
        return state.get();
    }

    // Domain Connection
    /**
     * Gets the underlying domain connection property.
     * 
     * @return the underlying domain connection property
     */
    public ReadOnlyObjectProperty<Connection> connectionProperty() {
        return connection;
    }

    public Connection getConnection() {
        return connection.get();
    }

    // ===== Callbacks =====

    /**
     * Sets a callback for when the connection is selected.
     * 
     * @param callback the action to run
     */
    public void setOnSelected(Runnable callback) {
        this.onSelected = callback;
    }

    /**
     * Sets a callback for when the connection is deleted.
     * 
     * @param callback the action to run
     */
    public void setOnDeleted(Runnable callback) {
        this.onDeleted = callback;
    }

    /**
     * Notifies that the connection should be deleted.
     */
    public void notifyDeleted() {
        if (onDeleted != null) {
            onDeleted.run();
        }
    }

    @Override
    public String toString() {
        return "ConnectionViewModel{" +
                "id='" + connectionId.get() + '\'' +
                ", source='" + sourceNodeId.get() + '\'' +
                ", target='" + targetNodeId.get() + '\'' +
                ", state=" + state.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionViewModel that = (ConnectionViewModel) o;
        String thisId = connectionId.get();
        String thatId = that.connectionId.get();
        return thisId != null && thisId.equals(thatId);
    }

    @Override
    public int hashCode() {
        String id = connectionId.get();
        return id != null ? id.hashCode() : 0;
    }
}
