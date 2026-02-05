/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/**
 * Service for managing canvas node and connection selection.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Single and multi-selection support</li>
 * <li>Observable properties for selection state</li>
 * <li>Selection change callbacks</li>
 * </ul>
 *
 * <p>
 * This service manages selection at the identity level (node/connection IDs)
 * while the view layer handles the visual representation.
 *
 * @param <N> Node identifier type
 * @param <C> Connection identifier type
 */
public class NodeSelectionService<N, C> {

    @SuppressWarnings("unchecked")
    private final ObservableSet<N> selectedNodes = FXCollections.observableSet();
    @SuppressWarnings("unchecked")
    private final ObservableSet<C> selectedConnections = FXCollections.observableSet();

    private final SimpleIntegerProperty selectedNodeCount = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty selectedConnectionCount = new SimpleIntegerProperty(0);
    private final SimpleBooleanProperty hasSelection = new SimpleBooleanProperty(false);

    // Callbacks
    private Runnable onSelectionChanged;
    private java.util.function.Consumer<N> onNodeSelected;
    private java.util.function.Consumer<N> onNodeDeselected;

    /**
     * Creates a new NodeSelectionService.
     */
    public NodeSelectionService() {
        // Track selection counts
        selectedNodes.addListener((SetChangeListener<N>) change -> {
            selectedNodeCount.set(selectedNodes.size());
            updateHasSelection();
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
            if (change.wasAdded() && onNodeSelected != null) {
                onNodeSelected.accept(change.getElementAdded());
            }
            if (change.wasRemoved() && onNodeDeselected != null) {
                onNodeDeselected.accept(change.getElementRemoved());
            }
        });

        selectedConnections.addListener((SetChangeListener<C>) change -> {
            selectedConnectionCount.set(selectedConnections.size());
            updateHasSelection();
            if (onSelectionChanged != null) {
                onSelectionChanged.run();
            }
        });
    }

    // ===== Node Selection Operations =====

    /**
     * Select a node.
     *
     * @param node        The node to select
     * @param multiSelect If true, add to selection; if false, replace selection
     */
    public void selectNode(N node, boolean multiSelect) {
        if (!multiSelect) {
            // Clear existing selections
            selectedNodes.clear();
            selectedConnections.clear();
        }
        selectedNodes.add(node);
    }

    /**
     * Select a single node, clearing other selections.
     *
     * @param node The node to select
     */
    public void selectNode(N node) {
        selectNode(node, false);
    }

    /**
     * Deselect a specific node.
     *
     * @param node The node to deselect
     */
    public void deselectNode(N node) {
        selectedNodes.remove(node);
    }

    /**
     * Toggle node selection.
     *
     * @param node The node to toggle
     * @return true if the node is now selected
     */
    public boolean toggleNodeSelection(N node) {
        if (selectedNodes.contains(node)) {
            selectedNodes.remove(node);
            return false;
        } else {
            selectedNodes.add(node);
            return true;
        }
    }

    /**
     * Check if a node is selected.
     *
     * @param node The node to check
     * @return true if selected
     */
    public boolean isNodeSelected(N node) {
        return selectedNodes.contains(node);
    }

    /**
     * Select multiple nodes.
     *
     * @param nodes       The nodes to select
     * @param multiSelect If true, add to selection; if false, replace selection
     */
    public void selectNodes(java.util.Collection<N> nodes, boolean multiSelect) {
        if (!multiSelect) {
            selectedNodes.clear();
            selectedConnections.clear();
        }
        selectedNodes.addAll(nodes);
    }

    // ===== Connection Selection Operations =====

    /**
     * Select a connection.
     *
     * @param connection  The connection to select
     * @param multiSelect If true, add to selection; if false, replace selection
     */
    public void selectConnection(C connection, boolean multiSelect) {
        if (!multiSelect) {
            selectedNodes.clear();
            selectedConnections.clear();
        }
        selectedConnections.add(connection);
    }

    /**
     * Deselect a specific connection.
     *
     * @param connection The connection to deselect
     */
    public void deselectConnection(C connection) {
        selectedConnections.remove(connection);
    }

    /**
     * Check if a connection is selected.
     *
     * @param connection The connection to check
     * @return true if selected
     */
    public boolean isConnectionSelected(C connection) {
        return selectedConnections.contains(connection);
    }

    // ===== Bulk Operations =====

    /**
     * Clear all selections.
     */
    public void deselectAll() {
        selectedNodes.clear();
        selectedConnections.clear();
    }

    /**
     * Select all provided items.
     *
     * @param nodes       Nodes to select (can be null)
     * @param connections Connections to select (can be null)
     */
    public void selectAll(java.util.Collection<N> nodes, java.util.Collection<C> connections) {
        if (nodes != null) {
            selectedNodes.addAll(nodes);
        }
        if (connections != null) {
            selectedConnections.addAll(connections);
        }
    }

    // ===== Property Accessors =====

    /**
     * Get the set of selected nodes (observable).
     *
     * @return The set of selected nodes
     */
    public ObservableSet<N> getSelectedNodes() {
        return FXCollections.unmodifiableObservableSet(selectedNodes);
    }

    /**
     * Get the set of selected connections (observable).
     *
     * @return The set of selected connections
     */
    public ObservableSet<C> getSelectedConnections() {
        return FXCollections.unmodifiableObservableSet(selectedConnections);
    }

    /**
     * Get the count of selected nodes (observable).
     *
     * @return The property for selected node count
     */
    public ReadOnlyIntegerProperty selectedNodeCountProperty() {
        return selectedNodeCount;
    }

    /**
     * Get the current number of selected nodes.
     *
     * @return The count of selected nodes
     */
    public int getSelectedNodeCount() {
        return selectedNodeCount.get();
    }

    /**
     * Get the count of selected connections (observable).
     *
     * @return The property for selected connection count
     */
    public ReadOnlyIntegerProperty selectedConnectionCountProperty() {
        return selectedConnectionCount;
    }

    /**
     * Get the current number of selected connections.
     *
     * @return The count of selected connections
     */
    public int getSelectedConnectionCount() {
        return selectedConnectionCount.get();
    }

    /**
     * Property indicating if there is any selection.
     *
     * @return The property indicating selection presence
     */
    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection;
    }

    /**
     * Check if there are any selected items.
     *
     * @return true if any nodes or connections are selected
     */
    public boolean hasSelection() {
        return hasSelection.get();
    }

    // ===== Callbacks =====

    /**
     * Set callback for selection changes.
     *
     * @param callback The callback to run when selection changes
     */
    public void setOnSelectionChanged(Runnable callback) {
        this.onSelectionChanged = callback;
    }

    /**
     * Set callback when a node is selected.
     *
     * @param callback The callback consuming the selected node
     */
    public void setOnNodeSelected(java.util.function.Consumer<N> callback) {
        this.onNodeSelected = callback;
    }

    /**
     * Set callback when a node is deselected.
     *
     * @param callback The callback consuming the deselected node
     */
    public void setOnNodeDeselected(java.util.function.Consumer<N> callback) {
        this.onNodeDeselected = callback;
    }

    // ===== Private Helpers =====

    private void updateHasSelection() {
        hasSelection.set(!selectedNodes.isEmpty() || !selectedConnections.isEmpty());
    }

    /**
     * Get the first selected node, if any.
     *
     * @return The first selected node, or null if none selected
     */
    public N getFirstSelectedNode() {
        return selectedNodes.isEmpty() ? null : selectedNodes.iterator().next();
    }

    /**
     * Get the first selected connection, if any.
     *
     * @return The first selected connection, or null if none selected
     */
    public C getFirstSelectedConnection() {
        return selectedConnections.isEmpty() ? null : selectedConnections.iterator().next();
    }
}
