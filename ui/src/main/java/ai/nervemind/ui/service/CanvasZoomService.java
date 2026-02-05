/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Service for managing canvas zoom and pan operations.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Zoom in/out with configurable limits</li>
 * <li>Pan (translate) operations</li>
 * <li>Fit-to-content calculations</li>
 * <li>Observable properties for binding</li>
 * </ul>
 */
public class CanvasZoomService {

    // Zoom limits
    /** Minimum allowed zoom scale (25%). */
    public static final double MIN_ZOOM = 0.25;
    /** Maximum allowed zoom scale (200%). */
    public static final double MAX_ZOOM = 2.0;
    /** Scale increment per scroll/zoom action. */
    public static final double ZOOM_FACTOR = 0.1;
    /** Initial/default zoom scale. */
    public static final double DEFAULT_ZOOM = 1.0;

    // Observable properties
    private final DoubleProperty scale = new SimpleDoubleProperty(DEFAULT_ZOOM);
    private final DoubleProperty translateX = new SimpleDoubleProperty(0);
    private final DoubleProperty translateY = new SimpleDoubleProperty(0);

    /**
     * Creates a new CanvasZoomService with default values.
     */
    public CanvasZoomService() {
        // Default initialization
    }

    // ===== Zoom Operations =====

    /**
     * Zoom in by one zoom factor step.
     *
     * @return true if zoom changed, false if already at max
     */
    public boolean zoomIn() {
        double newScale = Math.min(scale.get() + ZOOM_FACTOR, MAX_ZOOM);
        if (newScale != scale.get()) {
            scale.set(newScale);
            return true;
        }
        return false;
    }

    /**
     * Zoom out by one zoom factor step.
     *
     * @return true if zoom changed, false if already at min
     */
    public boolean zoomOut() {
        double newScale = Math.max(scale.get() - ZOOM_FACTOR, MIN_ZOOM);
        if (newScale != scale.get()) {
            scale.set(newScale);
            return true;
        }
        return false;
    }

    /**
     * Zoom by a delta amount (positive = zoom in, negative = zoom out).
     *
     * @param delta The amount to change the zoom by
     * @return true if zoom changed
     */
    public boolean zoom(double delta) {
        double newScale = clamp(scale.get() + delta, MIN_ZOOM, MAX_ZOOM);
        if (newScale != scale.get()) {
            scale.set(newScale);
            return true;
        }
        return false;
    }

    /**
     * Set the zoom level directly.
     *
     * @param newScale The new scale value (will be clamped to limits)
     */
    public void setScale(double newScale) {
        scale.set(clamp(newScale, MIN_ZOOM, MAX_ZOOM));
    }

    /**
     * Reset zoom to 100% (1.0).
     */
    public void resetZoom() {
        scale.set(DEFAULT_ZOOM);
        translateX.set(0);
        translateY.set(0);
    }

    /**
     * Get the current zoom level as a percentage.
     *
     * @return Zoom level (e.g., 100 for 1.0)
     */
    public int getZoomPercentage() {
        return (int) Math.round(scale.get() * 100);
    }

    // ===== Pan Operations =====

    /**
     * Pan the canvas by the given delta.
     *
     * @param deltaX X offset
     * @param deltaY Y offset
     */
    public void pan(double deltaX, double deltaY) {
        translateX.set(translateX.get() + deltaX);
        translateY.set(translateY.get() + deltaY);
    }

    /**
     * Set the pan position directly.
     *
     * @param x X translation
     * @param y Y translation
     */
    public void setPan(double x, double y) {
        translateX.set(x);
        translateY.set(y);
    }

    // ===== Fit to Content =====

    /**
     * Calculate zoom and pan to fit all content in the viewport.
     *
     * @param contentMinX    Minimum X of content bounds
     * @param contentMinY    Minimum Y of content bounds
     * @param contentMaxX    Maximum X of content bounds
     * @param contentMaxY    Maximum Y of content bounds
     * @param viewportWidth  Width of the viewport
     * @param viewportHeight Height of the viewport
     * @param padding        Padding around the content
     */
    public void fitToContent(double contentMinX, double contentMinY,
            double contentMaxX, double contentMaxY,
            double viewportWidth, double viewportHeight,
            double padding) {

        double contentWidth = contentMaxX - contentMinX + (padding * 2);
        double contentHeight = contentMaxY - contentMinY + (padding * 2);

        // Calculate required scale
        double scaleX = viewportWidth / contentWidth;
        double scaleY = viewportHeight / contentHeight;
        double newScale = clamp(Math.min(scaleX, scaleY), MIN_ZOOM, MAX_ZOOM);

        // Calculate translation to center content
        double newTranslateX = -contentMinX + padding;
        double newTranslateY = -contentMinY + padding;

        scale.set(newScale);
        translateX.set(newTranslateX);
        translateY.set(newTranslateY);
    }

    // ===== Property Accessors =====

    /**
     * Property for the current scale factor.
     *
     * @return The scale property
     */
    public ReadOnlyDoubleProperty scaleProperty() {
        return scale;
    }

    /**
     * Get the current scale factor (1.0 = 100%).
     *
     * @return The current scale
     */
    public double getScale() {
        return scale.get();
    }

    /**
     * Property for horizontal translation.
     *
     * @return The translateX property
     */
    public ReadOnlyDoubleProperty translateXProperty() {
        return translateX;
    }

    /**
     * Get the current horizontal translation.
     *
     * @return The X translation
     */
    public double getTranslateX() {
        return translateX.get();
    }

    /**
     * Property for vertical translation.
     *
     * @return The translateY property
     */
    public ReadOnlyDoubleProperty translateYProperty() {
        return translateY;
    }

    /**
     * Get the current vertical translation.
     *
     * @return The Y translation
     */
    public double getTranslateY() {
        return translateY.get();
    }

    // ===== Utility =====

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Check if zoom can increase (not at max).
     *
     * @return true if safe to zoom in
     */
    public boolean canZoomIn() {
        return scale.get() < MAX_ZOOM;
    }

    /**
     * Check if zoom can decrease (not at min).
     *
     * @return true if safe to zoom out
     */
    public boolean canZoomOut() {
        return scale.get() > MIN_ZOOM;
    }
}
