/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * ViewModel for the workflow canvas.
 *
 * <p>
 * Manages the canvas state including:
 * <ul>
 * <li>Current workflow</li>
 * <li>Nodes and connections</li>
 * <li>Zoom and pan (via CanvasZoomService)</li>
 * <li>Selection (via NodeSelectionService)</li>
 * <li>Grid and snap settings</li>
 * <li>Dirty state tracking</li>
 * </ul>
 */
public class WorkflowCanvasViewModel {

    private static final String DEFAULT_WORKFLOW_NAME = "New Workflow";
    public static final double GRID_SIZE = 20.0;

    // Services
    private final CanvasZoomService zoomService;
    private final NodeSelectionService<String, String> selectionService;

    // Workflow state
    private final ObjectProperty<WorkflowDTO> workflow = new SimpleObjectProperty<>();
    private final StringProperty workflowName = new SimpleStringProperty(DEFAULT_WORKFLOW_NAME);
    private final StringProperty workflowDescription = new SimpleStringProperty("");
    private final BooleanProperty workflowActive = new SimpleBooleanProperty(false);

    // Node and connection collections
    private final ObservableMap<String, Node> nodes = FXCollections.observableHashMap();
    private final ObservableMap<String, Connection> connections = FXCollections.observableHashMap();
    private final ObservableList<Node> nodeList = FXCollections.observableArrayList();
    private final ObservableList<Connection> connectionList = FXCollections.observableArrayList();

    // Grid settings
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);

    // Dirty state
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    // Execution state
    private final BooleanProperty executing = new SimpleBooleanProperty(false);

    // Clipboard
    private final ObservableList<Node> clipboard = FXCollections.observableArrayList();

    // Status
    private final StringProperty statusMessage = new SimpleStringProperty("");

    // Callbacks
    private Runnable onWorkflowChanged;
    private Runnable onNodesChanged;
    private Runnable onConnectionsChanged;

    /**
     * Creates a new WorkflowCanvasViewModel with default services.
     */
    public WorkflowCanvasViewModel() {
        this(new CanvasZoomService(), new NodeSelectionService<>());
    }

    /**
     * Creates a new WorkflowCanvasViewModel with the given services.
     *
     * @param zoomService      The zoom service to use
     * @param selectionService The selection service to use
     */
    public WorkflowCanvasViewModel(CanvasZoomService zoomService,
            NodeSelectionService<String, String> selectionService) {
        this.zoomService = zoomService;
        this.selectionService = selectionService;

        // Sync nodeList with nodes map
        nodes.addListener((javafx.collections.MapChangeListener<String, Node>) change -> {
            nodeList.setAll(nodes.values());
            markDirty();
            if (onNodesChanged != null) {
                onNodesChanged.run();
            }
        });

        // Sync connectionList with connections map
        connections.addListener((javafx.collections.MapChangeListener<String, Connection>) change -> {
            connectionList.setAll(connections.values());
            markDirty();
            if (onConnectionsChanged != null) {
                onConnectionsChanged.run();
            }
        });

        // Track workflow property changes
        workflow.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadFromWorkflow(newVal);
            }
            if (onWorkflowChanged != null) {
                onWorkflowChanged.run();
            }
        });
    }

    // ===== Workflow Operations =====

    /**
     * Load a workflow into the canvas.
     *
     * @param workflowDto The workflow to load
     */
    public void loadWorkflow(WorkflowDTO workflowDto) {
        workflow.set(workflowDto);
    }

    /**
     * Create a new empty workflow.
     */
    public void newWorkflow() {
        nodes.clear();
        connections.clear();
        selectionService.deselectAll();
        workflowName.set(DEFAULT_WORKFLOW_NAME);
        workflowDescription.set("");
        workflowActive.set(false);
        workflow.set(null);
        dirty.set(false);
        zoomService.resetZoom();
    }

    /**
     * Get the current workflow as a DTO.
     */
    public WorkflowDTO toWorkflowDTO() {
        WorkflowDTO current = workflow.get();
        return new WorkflowDTO(
                current != null ? current.id() : null,
                workflowName.get(),
                workflowDescription.get(),
                List.copyOf(nodeList),
                List.copyOf(connectionList),
                current != null ? current.settings() : Map.of(),
                workflowActive.get(),
                current != null ? current.triggerType() : TriggerType.MANUAL,
                current != null ? current.cronExpression() : null,
                current != null ? current.createdAt() : Instant.now(),
                Instant.now(),
                current != null ? current.lastExecuted() : null,
                current != null ? current.version() : 1);
    }

    private void loadFromWorkflow(WorkflowDTO dto) {
        nodes.clear();
        connections.clear();
        selectionService.deselectAll();

        workflowName.set(dto.name() != null ? dto.name() : DEFAULT_WORKFLOW_NAME);
        workflowDescription.set(dto.description() != null ? dto.description() : "");
        workflowActive.set(dto.isActive());

        // Load nodes
        if (dto.nodes() != null) {
            for (Node node : dto.nodes()) {
                nodes.put(node.id(), node);
            }
        }

        // Load connections
        if (dto.connections() != null) {
            for (Connection conn : dto.connections()) {
                connections.put(conn.id(), conn);
            }
        }

        dirty.set(false);
    }

    // ===== Node Operations =====

    /**
     * Add a node to the canvas.
     *
     * @param node The node to add
     */
    public void addNode(Node node) {
        nodes.put(node.id(), node);
    }

    /**
     * Remove a node from the canvas.
     *
     * @param nodeId The ID of the node to remove
     * @return The removed node, or null if not found
     */
    public Node removeNode(String nodeId) {
        // Also remove any connections to/from this node
        List<String> connToRemove = connections.values().stream()
                .filter(c -> c.sourceNodeId().equals(nodeId) || c.targetNodeId().equals(nodeId))
                .map(Connection::id)
                .toList();
        connToRemove.forEach(connections::remove);

        selectionService.deselectNode(nodeId);
        return nodes.remove(nodeId);
    }

    /**
     * Update a node's data.
     *
     * @param nodeId The ID of the node to update
     * @param node   The new node data
     */
    public void updateNode(String nodeId, Node node) {
        nodes.computeIfPresent(nodeId, (k, v) -> node);
    }

    /**
     * Get a node by ID.
     *
     * @param nodeId The node ID
     * @return The node, or null if not found
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Check if a node exists.
     *
     * @param nodeId The node ID
     * @return true if the node exists
     */
    public boolean hasNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    // ===== Connection Operations =====

    /**
     * Add a connection between nodes.
     *
     * @param connection The connection to add
     */
    public void addConnection(Connection connection) {
        connections.put(connection.id(), connection);
    }

    /**
     * Remove a connection.
     *
     * @param connectionId The ID of the connection to remove
     * @return The removed connection, or null if not found
     */
    public Connection removeConnection(String connectionId) {
        selectionService.deselectConnection(connectionId);
        return connections.remove(connectionId);
    }

    /**
     * Get a connection by ID.
     *
     * @param connectionId The connection ID
     * @return The connection, or null if not found
     */
    public Connection getConnection(String connectionId) {
        return connections.get(connectionId);
    }

    /**
     * Find connections for a node.
     *
     * @param nodeId   The node ID
     * @param outgoing true for outgoing connections, false for incoming
     * @return List of connections
     */
    public List<Connection> getConnectionsForNode(String nodeId, boolean outgoing) {
        return connections.values().stream()
                .filter(c -> outgoing ? c.sourceNodeId().equals(nodeId) : c.targetNodeId().equals(nodeId))
                .toList();
    }

    // ===== Selection Delegate Methods =====

    /**
     * Select a node.
     *
     * @param nodeId      The node ID to select
     * @param multiSelect If true, add to selection
     */
    public void selectNode(String nodeId, boolean multiSelect) {
        selectionService.selectNode(nodeId, multiSelect);
    }

    /**
     * Select a connection.
     *
     * @param connectionId The connection ID to select
     * @param multiSelect  If true, add to selection
     */
    public void selectConnection(String connectionId, boolean multiSelect) {
        selectionService.selectConnection(connectionId, multiSelect);
    }

    /**
     * Deselect all items.
     */
    public void deselectAll() {
        selectionService.deselectAll();
    }

    /**
     * Delete all selected items.
     */
    public void deleteSelected() {
        // Copy to avoid concurrent modification
        List<String> nodesToRemove = List.copyOf(selectionService.getSelectedNodes());
        List<String> connsToRemove = List.copyOf(selectionService.getSelectedConnections());

        for (String nodeId : nodesToRemove) {
            removeNode(nodeId);
        }
        for (String connId : connsToRemove) {
            removeConnection(connId);
        }
    }

    // ===== Zoom Delegate Methods =====

    public void zoomIn() {
        zoomService.zoomIn();
    }

    public void zoomOut() {
        zoomService.zoomOut();
    }

    public void resetZoom() {
        zoomService.resetZoom();
    }

    public int getZoomPercentage() {
        return zoomService.getZoomPercentage();
    }

    // ===== Grid Operations =====

    /**
     * Snap coordinates to grid.
     *
     * @param value The value to snap
     * @return The snapped value
     */
    public double snapToGrid(double value) {
        if (snapToGrid.get()) {
            return Math.round(value / GRID_SIZE) * GRID_SIZE;
        }
        return value;
    }

    /**
     * Toggle grid visibility.
     */
    public void toggleGrid() {
        showGrid.set(!showGrid.get());
    }

    /**
     * Toggle snap to grid.
     */
    public void toggleSnapToGrid() {
        snapToGrid.set(!snapToGrid.get());
    }

    // ===== Clipboard Operations =====

    /**
     * Copy selected nodes to clipboard.
     */
    public void copySelectedNodes() {
        clipboard.clear();
        for (String nodeId : selectionService.getSelectedNodes()) {
            Node node = nodes.get(nodeId);
            if (node != null) {
                clipboard.add(node);
            }
        }
    }

    /**
     * Get clipboard contents.
     */
    public ObservableList<Node> getClipboard() {
        return FXCollections.unmodifiableObservableList(clipboard);
    }

    /**
     * Check if clipboard has content.
     */
    public boolean hasClipboardContent() {
        return !clipboard.isEmpty();
    }

    // ===== State Operations =====

    /**
     * Mark the workflow as dirty (unsaved changes).
     */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Mark the workflow as clean (saved).
     */
    public void markClean() {
        dirty.set(false);
    }

    // ===== Property Accessors =====

    // Workflow properties
    public ObjectProperty<WorkflowDTO> workflowProperty() {
        return workflow;
    }

    public WorkflowDTO getWorkflow() {
        return workflow.get();
    }

    public StringProperty workflowNameProperty() {
        return workflowName;
    }

    public String getWorkflowName() {
        return workflowName.get();
    }

    public void setWorkflowName(String name) {
        workflowName.set(name);
        markDirty();
    }

    public StringProperty workflowDescriptionProperty() {
        return workflowDescription;
    }

    public String getWorkflowDescription() {
        return workflowDescription.get();
    }

    public void setWorkflowDescription(String description) {
        workflowDescription.set(description);
        markDirty();
    }

    public BooleanProperty workflowActiveProperty() {
        return workflowActive;
    }

    public boolean isWorkflowActive() {
        return workflowActive.get();
    }

    public void setWorkflowActive(boolean active) {
        workflowActive.set(active);
        markDirty();
    }

    // Collections
    public ObservableList<Node> getNodes() {
        return FXCollections.unmodifiableObservableList(nodeList);
    }

    public ObservableList<Connection> getConnections() {
        return FXCollections.unmodifiableObservableList(connectionList);
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getConnectionCount() {
        return connections.size();
    }

    // Grid properties
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    public boolean isShowGrid() {
        return showGrid.get();
    }

    public BooleanProperty snapToGridProperty() {
        return snapToGrid;
    }

    public boolean isSnapToGrid() {
        return snapToGrid.get();
    }

    // Dirty state
    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    // Execution state
    public BooleanProperty executingProperty() {
        return executing;
    }

    public boolean isExecuting() {
        return executing.get();
    }

    public void setExecuting(boolean value) {
        executing.set(value);
    }

    // Status
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String message) {
        statusMessage.set(message);
    }

    // Services (for advanced use)
    public CanvasZoomService getZoomService() {
        return zoomService;
    }

    public NodeSelectionService<String, String> getSelectionService() {
        return selectionService;
    }

    // Zoom properties (delegated)
    public ReadOnlyDoubleProperty scaleProperty() {
        return zoomService.scaleProperty();
    }

    public double getScale() {
        return zoomService.getScale();
    }

    public ReadOnlyDoubleProperty translateXProperty() {
        return zoomService.translateXProperty();
    }

    public double getTranslateX() {
        return zoomService.getTranslateX();
    }

    public ReadOnlyDoubleProperty translateYProperty() {
        return zoomService.translateYProperty();
    }

    public double getTranslateY() {
        return zoomService.getTranslateY();
    }

    // Selection properties (delegated)
    public ReadOnlyIntegerProperty selectedNodeCountProperty() {
        return selectionService.selectedNodeCountProperty();
    }

    public int getSelectedNodeCount() {
        return selectionService.getSelectedNodeCount();
    }

    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return selectionService.hasSelectionProperty();
    }

    public boolean hasSelection() {
        return selectionService.hasSelection();
    }

    // ===== Callbacks =====

    public void setOnWorkflowChanged(Runnable callback) {
        this.onWorkflowChanged = callback;
    }

    public void setOnNodesChanged(Runnable callback) {
        this.onNodesChanged = callback;
    }

    public void setOnConnectionsChanged(Runnable callback) {
        this.onConnectionsChanged = callback;
    }
}
