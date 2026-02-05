/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Service for managing canvas grid operations.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Grid visibility control</li>
 * <li>Snap-to-grid functionality</li>
 * <li>Grid size configuration</li>
 * </ul>
 */
public class GridService {

    /** Default grid size in pixels. */
    public static final double DEFAULT_GRID_SIZE = 20.0;

    private final DoubleProperty gridSize = new SimpleDoubleProperty(DEFAULT_GRID_SIZE);
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);

    /**
     * Creates a new GridService with default settings.
     */
    public GridService() {
        // Default initialization
    }

    /**
     * Creates a new GridService with the specified grid size.
     *
     * @param gridSize The grid cell size in pixels
     */
    public GridService(double gridSize) {
        this.gridSize.set(gridSize);
    }

    // ===== Grid Operations =====

    /**
     * Snap a value to the nearest grid line.
     *
     * @param value The value to snap
     * @return The snapped value, or original if snap is disabled
     */
    public double snap(double value) {
        if (!snapToGrid.get()) {
            return value;
        }
        return Math.round(value / gridSize.get()) * gridSize.get();
    }

    /**
     * Snap X and Y coordinates to the grid.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return An array with [snappedX, snappedY]
     */
    public double[] snap(double x, double y) {
        return new double[] { snap(x), snap(y) };
    }

    /**
     * Get the nearest grid intersection point.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return An array with [gridX, gridY]
     */
    public double[] nearestGridPoint(double x, double y) {
        double gx = Math.round(x / gridSize.get()) * gridSize.get();
        double gy = Math.round(y / gridSize.get()) * gridSize.get();
        return new double[] { gx, gy };
    }

    /**
     * Get the grid cell indices for a point.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return An array with [cellX, cellY] indices
     */
    public int[] getCellIndices(double x, double y) {
        int cellX = (int) Math.floor(x / gridSize.get());
        int cellY = (int) Math.floor(y / gridSize.get());
        return new int[] { cellX, cellY };
    }

    /**
     * Get the top-left corner of a grid cell.
     *
     * @param cellX The cell X index
     * @param cellY The cell Y index
     * @return An array with [x, y] coordinates
     */
    public double[] getCellOrigin(int cellX, int cellY) {
        return new double[] { cellX * gridSize.get(), cellY * gridSize.get() };
    }

    // ===== Toggle Operations =====

    /**
     * Toggle grid visibility.
     */
    public void toggleShowGrid() {
        showGrid.set(!showGrid.get());
    }

    /**
     * Toggle snap to grid.
     */
    public void toggleSnapToGrid() {
        snapToGrid.set(!snapToGrid.get());
    }

    // ===== Property Accessors =====

    /**
     * Property for grid cell size.
     *
     * @return The grid size property
     */
    public DoubleProperty gridSizeProperty() {
        return gridSize;
    }

    /**
     * Get the current grid cell size.
     *
     * @return The grid cell size
     */
    public double getGridSize() {
        return gridSize.get();
    }

    /**
     * Set the grid cell size.
     *
     * @param size The new grid cell size (must be positive)
     */
    public void setGridSize(double size) {
        if (size > 0) {
            gridSize.set(size);
        }
    }

    /**
     * Property for grid visibility.
     *
     * @return The show grid property
     */
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    /**
     * Check if grid is visible.
     *
     * @return true if visible
     */
    public boolean isShowGrid() {
        return showGrid.get();
    }

    /**
     * Set grid visibility.
     *
     * @param show true to show grid
     */
    public void setShowGrid(boolean show) {
        showGrid.set(show);
    }

    /**
     * Property for snap-to-grid.
     *
     * @return The snap-to-grid property
     */
    public BooleanProperty snapToGridProperty() {
        return snapToGrid;
    }

    /**
     * Check if snap-to-grid is enabled.
     *
     * @return true if snap enabled
     */
    public boolean isSnapToGrid() {
        return snapToGrid.get();
    }

    /**
     * Enable or disable snap-to-grid.
     *
     * @param snap true to enable snap
     */
    public void setSnapToGrid(boolean snap) {
        snapToGrid.set(snap);
    }

    // ===== Grid Calculation Utilities =====

    /**
     * Calculate how many grid lines fit in a given dimension.
     *
     * @param dimension The width or height
     * @return Number of grid lines
     */
    public int getGridLineCount(double dimension) {
        return (int) Math.ceil(dimension / gridSize.get()) + 1;
    }

    /**
     * Calculate the offset for drawing grid lines with current pan.
     *
     * @param translate Current pan offset
     * @return The offset to start drawing grid lines
     */
    public double getGridOffset(double translate) {
        double gs = gridSize.get();
        return (translate % gs + gs) % gs;
    }
}
