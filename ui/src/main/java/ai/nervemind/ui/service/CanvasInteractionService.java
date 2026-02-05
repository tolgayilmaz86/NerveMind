/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Service for managing canvas panning and interaction state.
 *
 * <p>
 * Tracks:
 * <ul>
 * <li>Pan (drag) state and coordinates</li>
 * <li>Connection drag state</li>
 * <li>Selection rectangle state</li>
 * </ul>
 */
public class CanvasInteractionService {

    // Panning state
    private final BooleanProperty panning = new SimpleBooleanProperty(false);
    private double panStartX = 0;
    private double panStartY = 0;

    // Connection dragging state
    private final BooleanProperty connectionDragging = new SimpleBooleanProperty(false);
    private String connectionSourceNodeId = null;

    // Selection rectangle state
    private final BooleanProperty rectangleSelecting = new SimpleBooleanProperty(false);
    private double selectStartX = 0;
    private double selectStartY = 0;
    private double selectEndX = 0;
    private double selectEndY = 0;

    // Callbacks
    private PanCallback onPanUpdate;
    private Runnable onPanEnd;

    /**
     * Callback interface for pan operations.
     */
    @FunctionalInterface
    public interface PanCallback {
        /**
         * Called when pan position updates.
         *
         * @param deltaX The change in X
         * @param deltaY The change in Y
         */
        void onPan(double deltaX, double deltaY);
    }

    /**
     * Creates a new CanvasInteractionService.
     */
    public CanvasInteractionService() {
        // Default initialization
    }

    // ===== Pan Operations =====

    /**
     * Start panning operation.
     *
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     */
    public void startPan(double x, double y) {
        panning.set(true);
        panStartX = x;
        panStartY = y;
    }

    /**
     * Update pan position.
     *
     * @param x Current X coordinate
     * @param y Current Y coordinate
     * @return Array with [deltaX, deltaY]
     */
    public double[] updatePan(double x, double y) {
        if (!panning.get()) {
            return new double[] { 0, 0 };
        }

        double deltaX = x - panStartX;
        double deltaY = y - panStartY;
        panStartX = x;
        panStartY = y;

        if (onPanUpdate != null) {
            onPanUpdate.onPan(deltaX, deltaY);
        }

        return new double[] { deltaX, deltaY };
    }

    /**
     * End panning operation.
     */
    public void endPan() {
        panning.set(false);
        if (onPanEnd != null) {
            onPanEnd.run();
        }
    }

    // ===== Connection Drag Operations =====

    /**
     * Start dragging a new connection.
     *
     * @param sourceNodeId The source node ID
     */
    public void startConnectionDrag(String sourceNodeId) {
        connectionDragging.set(true);
        this.connectionSourceNodeId = sourceNodeId;
    }

    /**
     * End connection dragging.
     */
    public void endConnectionDrag() {
        connectionDragging.set(false);
        connectionSourceNodeId = null;
    }

    /**
     * Get the source node ID for connection dragging.
     *
     * @return The ID of the node starting the connection
     */
    public String getConnectionSourceNodeId() {
        return connectionSourceNodeId;
    }

    // ===== Selection Rectangle Operations =====

    /**
     * Start rectangle selection.
     *
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     */
    public void startRectangleSelect(double x, double y) {
        rectangleSelecting.set(true);
        selectStartX = x;
        selectStartY = y;
        selectEndX = x;
        selectEndY = y;
    }

    /**
     * Update rectangle selection.
     *
     * @param x Current X coordinate
     * @param y Current Y coordinate
     */
    public void updateRectangleSelect(double x, double y) {
        if (!rectangleSelecting.get()) {
            return;
        }
        selectEndX = x;
        selectEndY = y;
    }

    /**
     * End rectangle selection.
     *
     * @return Array with [minX, minY, maxX, maxY] of the selection bounds
     */
    public double[] endRectangleSelect() {
        rectangleSelecting.set(false);
        double minX = Math.min(selectStartX, selectEndX);
        double minY = Math.min(selectStartY, selectEndY);
        double maxX = Math.max(selectStartX, selectEndX);
        double maxY = Math.max(selectStartY, selectEndY);
        return new double[] { minX, minY, maxX, maxY };
    }

    /**
     * Get current selection rectangle bounds.
     *
     * @return Array with [minX, minY, maxX, maxY]
     */
    public double[] getSelectionBounds() {
        double minX = Math.min(selectStartX, selectEndX);
        double minY = Math.min(selectStartY, selectEndY);
        double maxX = Math.max(selectStartX, selectEndX);
        double maxY = Math.max(selectStartY, selectEndY);
        return new double[] { minX, minY, maxX, maxY };
    }

    // ===== Property Accessors =====

    /**
     * Property indicating if panning is active.
     *
     * @return The panning property
     */
    public ReadOnlyBooleanProperty panningProperty() {
        return panning;
    }

    /**
     * Check if panning is currently active.
     *
     * @return true if panning
     */
    public boolean isPanning() {
        return panning.get();
    }

    /**
     * Property indicating if connection dragging is active.
     *
     * @return The connection dragging property
     */
    public ReadOnlyBooleanProperty connectionDraggingProperty() {
        return connectionDragging;
    }

    /**
     * Check if connection dragging is currently active.
     *
     * @return true if dragging a connection
     */
    public boolean isConnectionDragging() {
        return connectionDragging.get();
    }

    /**
     * Property indicating if rectangle selection is active.
     *
     * @return The rectangle selection property
     */
    public ReadOnlyBooleanProperty rectangleSelectingProperty() {
        return rectangleSelecting;
    }

    /**
     * Check if rectangle selection is currently active.
     *
     * @return true if selecting
     */
    public boolean isRectangleSelecting() {
        return rectangleSelecting.get();
    }

    // ===== Callbacks =====

    /**
     * Set the callback for pan updates.
     *
     * @param callback The callback to run on pan update
     */
    public void setOnPanUpdate(PanCallback callback) {
        this.onPanUpdate = callback;
    }

    /**
     * Set the callback for pan end.
     *
     * @param callback The callback to run when panning ends
     */
    public void setOnPanEnd(Runnable callback) {
        this.onPanEnd = callback;
    }

    // ===== State Queries =====

    /**
     * Check if any interaction is in progress.
     *
     * @return true if panning, dragging connection, or rectangle selecting
     */
    public boolean isInteracting() {
        return panning.get() || connectionDragging.get() || rectangleSelecting.get();
    }

    /**
     * Cancel all interactions.
     */
    public void cancelAllInteractions() {
        if (panning.get()) {
            endPan();
        }
        if (connectionDragging.get()) {
            endConnectionDrag();
        }
        if (rectangleSelecting.get()) {
            rectangleSelecting.set(false);
        }
    }
}
