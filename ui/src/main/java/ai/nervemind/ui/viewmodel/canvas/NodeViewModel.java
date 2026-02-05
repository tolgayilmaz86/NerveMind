/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import java.util.HashMap;
import java.util.Map;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.domain.Node.Position;
import ai.nervemind.ui.canvas.NodeView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for an individual node on the workflow canvas.
 *
 * <p>
 * Manages the state of a single node including:
 * <ul>
 * <li>Position and dimensions</li>
 * <li>Selection state</li>
 * <li>Execution state (idle, running, success, error, etc.)</li>
 * <li>Visual properties (icon, color)</li>
 * <li>Enabled/disabled state</li>
 * </ul>
 *
 * <p>
 * This ViewModel is designed to be used with {@link NodeView} for
 * MVVM-style binding between model and view.
 */
public class NodeViewModel {

    /** Standard node size in pixels (n8n-style square node). */
    public static final double NODE_SIZE = 70.0;
    /** Radius of connection handles in pixels. */
    public static final double HANDLE_RADIUS = 8.0;
    /** Corner radius for rounded rectangles in pixels. */
    public static final double CORNER_RADIUS = 10.0;

    /**
     * Execution states for visual feedback on nodes.
     */
    public enum ExecutionState {
        /** Normal state - node is not executing */
        IDLE,
        /** Waiting to execute - in execution queue */
        QUEUED,
        /** Currently executing */
        RUNNING,
        /** Executed successfully */
        SUCCESS,
        /** Execution failed with error */
        ERROR,
        /** Skipped (disabled or conditional) */
        SKIPPED
    }

    /**
     * Node type categories for styling.
     */
    public enum NodeCategory {
        /** Trigger nodes that start workflows */
        TRIGGER,
        /** Action nodes that perform operations */
        ACTION,
        /** Flow control nodes (if/then, loops, etc.) */
        FLOW,
        /** Data processing and transformation nodes */
        DATA,
        /** AI/ML related nodes */
        AI,
        /** Default category for uncategorized nodes */
        DEFAULT
    }

    // Identity
    private final StringProperty nodeId = new SimpleStringProperty();
    private final StringProperty nodeType = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();

    // Position
    private final DoubleProperty layoutX = new SimpleDoubleProperty();
    private final DoubleProperty layoutY = new SimpleDoubleProperty();

    // State
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final BooleanProperty disabled = new SimpleBooleanProperty(false);
    private final BooleanProperty hovered = new SimpleBooleanProperty(false);
    private final BooleanProperty targetHighlighted = new SimpleBooleanProperty(false);
    private final ObjectProperty<ExecutionState> executionState = new SimpleObjectProperty<>(ExecutionState.IDLE);

    // Error state
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty hasError = new SimpleBooleanProperty(false);

    // Visual properties
    private final StringProperty customIcon = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();
    private final ObjectProperty<NodeCategory> category = new SimpleObjectProperty<>(NodeCategory.DEFAULT);

    // Notes
    private final StringProperty notes = new SimpleStringProperty();

    // The underlying domain node
    private final ObjectProperty<Node> node = new SimpleObjectProperty<>();

    // Parameters (for advanced use)
    private final ObjectProperty<Map<String, Object>> parameters = new SimpleObjectProperty<>(new HashMap<>());

    // Callbacks
    private Runnable onPositionChanged;
    private Runnable onStateChanged;
    private Runnable onSelected;
    private Runnable onDeleted;

    /**
     * Creates a new NodeViewModel with no initial node.
     */
    public NodeViewModel() {
        setupBindings();
    }

    /**
     * Creates a new NodeViewModel from a domain Node.
     *
     * @param node the domain node to wrap
     */
    public NodeViewModel(Node node) {
        this();
        loadFromNode(node);
    }

    /**
     * Sets up internal bindings and listeners.
     */
    private void setupBindings() {
        // Update category when node type changes
        nodeType.addListener((obs, oldVal, newVal) -> {
            category.set(getCategoryForType(newVal));
            subtitle.set(getSubtitleForType(newVal));
        });

        // Update hasError when errorMessage changes
        errorMessage.addListener((obs, oldVal, newVal) -> hasError.set(newVal != null && !newVal.isBlank()));

        // Track position changes
        layoutX.addListener((obs, oldVal, newVal) -> {
            if (onPositionChanged != null) {
                onPositionChanged.run();
            }
        });
        layoutY.addListener((obs, oldVal, newVal) -> {
            if (onPositionChanged != null) {
                onPositionChanged.run();
            }
        });

        // Track state changes
        executionState.addListener((obs, oldVal, newVal) -> {
            if (onStateChanged != null) {
                onStateChanged.run();
            }
        });
    }

    // ===== Loading & Saving =====

    /**
     * Loads state from a domain Node.
     *
     * @param domainNode the node to load from
     */
    public void loadFromNode(Node domainNode) {
        if (domainNode == null) {
            clear();
            return;
        }

        this.node.set(domainNode);
        this.nodeId.set(domainNode.id());
        this.nodeType.set(domainNode.type());
        this.name.set(domainNode.name());
        this.layoutX.set(domainNode.position().x());
        this.layoutY.set(domainNode.position().y());
        this.disabled.set(domainNode.disabled());
        this.notes.set(domainNode.notes());
        // customIcon is stored in parameters as "_customIcon" for visual customization
        Object iconParam = domainNode.parameters() != null ? domainNode.parameters().get("_customIcon") : null;
        this.customIcon.set(iconParam != null ? iconParam.toString() : null);

        if (domainNode.parameters() != null) {
            this.parameters.set(new HashMap<>(domainNode.parameters()));
        } else {
            this.parameters.set(new HashMap<>());
        }

        // Reset execution state
        this.executionState.set(ExecutionState.IDLE);
        this.errorMessage.set(null);
        this.selected.set(false);
    }

    /**
     * Creates a new domain Node from the current state.
     *
     * @return a new Node with the current ViewModel state
     */
    public Node toNode() {
        Node original = node.get();
        if (original == null) {
            return null;
        }

        // Store customIcon in parameters if set
        Map<String, Object> params = new HashMap<>(parameters.get());
        if (customIcon.get() != null) {
            params.put("_customIcon", customIcon.get());
        } else {
            params.remove("_customIcon");
        }

        return new Node(
                nodeId.get(),
                nodeType.get(),
                name.get(),
                new Position(layoutX.get(), layoutY.get()),
                params,
                original.credentialId(),
                disabled.get(),
                notes.get());
    }

    /**
     * Clears all state.
     */
    public void clear() {
        node.set(null);
        nodeId.set(null);
        nodeType.set(null);
        name.set(null);
        layoutX.set(0);
        layoutY.set(0);
        disabled.set(false);
        notes.set(null);
        customIcon.set(null);
        parameters.set(new HashMap<>());
        executionState.set(ExecutionState.IDLE);
        errorMessage.set(null);
        selected.set(false);
        hovered.set(false);
        targetHighlighted.set(false);
    }

    // ===== Position Methods =====

    /**
     * Updates the node position.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    public void setPosition(double x, double y) {
        layoutX.set(x);
        layoutY.set(y);
    }

    /**
     * Snaps the position to the given grid size.
     *
     * @param gridSize the grid size
     */
    public void snapToGrid(double gridSize) {
        layoutX.set(Math.round(layoutX.get() / gridSize) * gridSize);
        layoutY.set(Math.round(layoutY.get() / gridSize) * gridSize);
    }

    /**
     * Gets the center X coordinate of the node.
     *
     * @return the center X
     */
    public double getCenterX() {
        return layoutX.get() + NODE_SIZE / 2;
    }

    /**
     * Gets the center Y coordinate of the node.
     *
     * @return the center Y
     */
    public double getCenterY() {
        return layoutY.get() + NODE_SIZE / 2;
    }

    /**
     * Gets the X coordinate of the input handle.
     *
     * @return the input handle X
     */
    public double getInputX() {
        return layoutX.get() - HANDLE_RADIUS;
    }

    /**
     * Gets the Y coordinate of the input handle.
     *
     * @return the input handle Y
     */
    public double getInputY() {
        return layoutY.get() + NODE_SIZE / 2;
    }

    /**
     * Gets the X coordinate of the output handle.
     *
     * @return the output handle X
     */
    public double getOutputX() {
        return layoutX.get() + NODE_SIZE + HANDLE_RADIUS;
    }

    /**
     * Gets the Y coordinate of the output handle.
     *
     * @return the output handle Y
     */
    public double getOutputY() {
        return layoutY.get() + NODE_SIZE / 2;
    }

    // ===== Execution State Methods =====

    /**
     * Sets the execution state with an optional error message.
     *
     * @param state   the execution state
     * @param message optional error or status message
     */
    public void setExecutionState(ExecutionState state, String message) {
        executionState.set(state);
        if (state == ExecutionState.ERROR) {
            errorMessage.set(message);
        } else {
            errorMessage.set(null);
        }
    }

    /**
     * Resets the execution state to IDLE.
     */
    public void resetExecutionState() {
        executionState.set(ExecutionState.IDLE);
        errorMessage.set(null);
    }

    /**
     * Checks if the node is currently executing (QUEUED or RUNNING).
     *
     * @return true if executing
     */
    public boolean isExecuting() {
        ExecutionState state = executionState.get();
        return state == ExecutionState.QUEUED || state == ExecutionState.RUNNING;
    }

    // ===== Type & Category Helpers =====

    /**
     * Gets the node category for a given type.
     * 
     * <p>
     * Note: This method only knows about built-in node types. Plugin nodes
     * should provide their category through the plugin API and be looked up
     * via the PluginServiceInterface.
     * </p>
     *
     * @param type the node type
     * @return the category
     */
    public static NodeCategory getCategoryForType(String type) {
        if (type == null) {
            return NodeCategory.DEFAULT;
        }
        return switch (type) {
            case "manualTrigger", "scheduleTrigger", "webhookTrigger" -> NodeCategory.TRIGGER;
            case "httpRequest", "code", "executeCommand" -> NodeCategory.ACTION;
            case "if", "switch", "merge", "loop" -> NodeCategory.FLOW;
            case "set", "filter", "sort" -> NodeCategory.DATA;
            case "llmChat", "textClassifier", "embedding", "rag" -> NodeCategory.AI;
            default -> NodeCategory.DEFAULT;
        };
    }

    /**
     * Gets the subtitle text for a given node type.
     * 
     * <p>
     * Note: This method only knows about built-in node types. Plugin nodes
     * should provide their subtitle through the plugin API.
     * </p>
     *
     * @param type the node type
     * @return the subtitle text
     */
    public static String getSubtitleForType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "manualTrigger" -> "Trigger";
            case "scheduleTrigger" -> "Schedule";
            case "webhookTrigger" -> "Webhook";
            case "httpRequest" -> "HTTP";
            case "code" -> "Script";
            case "executeCommand" -> "Terminal";
            case "if" -> "Condition";
            case "switch" -> "Switch";
            case "merge" -> "Merge";
            case "loop" -> "Loop";
            case "set" -> "Set Data";
            case "filter" -> "Filter";
            case "sort" -> "Sort";
            case "llmChat" -> "AI Chat";
            case "textClassifier" -> "Classify";
            case "embedding" -> "Embed";
            case "rag" -> "RAG";
            case "subworkflow" -> "Workflow";
            case "parallel" -> "Parallel";
            case "tryCatch" -> "Try/Catch";
            case "retry" -> "Retry";
            case "rateLimit" -> "Rate Limit";
            default -> formatTypeAsLabel(type);
        };
    }

    /**
     * Formats a camelCase type string as a readable label.
     *
     * @param type the type string
     * @return the formatted label
     */
    private static String formatTypeAsLabel(String type) {
        if (type == null || type.isEmpty()) {
            return "";
        }
        // Convert camelCase to Title Case with spaces
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(type.charAt(0)));
        for (int i = 1; i < type.length(); i++) {
            char c = type.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Gets the default border color for a node category.
     *
     * @param category the category
     * @return the hex color string
     */
    public static String getBorderColorForCategory(NodeCategory category) {
        if (category == null) {
            return "#404040";
        }
        return switch (category) {
            case TRIGGER -> "#40c057"; // Green
            case ACTION -> "#4a9eff"; // Blue
            case FLOW -> "#be4bdb"; // Purple
            case DATA -> "#fab005"; // Yellow
            case AI -> "#ff6b6b"; // Red/Pink
            case DEFAULT -> "#404040"; // Gray
        };
    }

    // ===== Property Accessors =====

    // Node ID
    /**
     * Gets the node ID property.
     * 
     * @return the node ID property
     */
    public ReadOnlyStringProperty nodeIdProperty() {
        return nodeId;
    }

    /**
     * Gets the node ID.
     * 
     * @return the node ID
     */
    public String getNodeId() {
        return nodeId.get();
    }

    // Node Type
    /**
     * Gets the node type property.
     * 
     * @return the node type property
     */
    public ReadOnlyStringProperty nodeTypeProperty() {
        return nodeType;
    }

    /**
     * Gets the node type.
     * 
     * @return the node type
     */
    public String getNodeType() {
        return nodeType.get();
    }

    // Name
    /**
     * Gets the name property.
     * 
     * @return the name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Gets the node name.
     * 
     * @return the node name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the node name.
     * 
     * @param name the node name to set
     */
    public void setName(String name) {
        this.name.set(name);
    }

    // Layout X
    /**
     * Gets the layout X property.
     * 
     * @return the layout X property
     */
    public DoubleProperty layoutXProperty() {
        return layoutX;
    }

    /**
     * Gets the layout X coordinate.
     * 
     * @return the layout X coordinate
     */
    public double getLayoutX() {
        return layoutX.get();
    }

    /**
     * Sets the layout X coordinate.
     * 
     * @param x the layout X coordinate to set
     */
    public void setLayoutX(double x) {
        layoutX.set(x);
    }

    // Layout Y
    /**
     * Gets the layout Y property.
     * 
     * @return the layout Y property
     */
    public DoubleProperty layoutYProperty() {
        return layoutY;
    }

    /**
     * Gets the layout Y coordinate.
     * 
     * @return the layout Y coordinate
     */
    public double getLayoutY() {
        return layoutY.get();
    }

    /**
     * Sets the layout Y coordinate.
     * 
     * @param y the layout Y coordinate to set
     */
    public void setLayoutY(double y) {
        layoutY.set(y);
    }

    // Selected
    /**
     * Gets the selected property.
     * 
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * Checks if the node is selected.
     * 
     * @return true if selected
     */
    public boolean isSelected() {
        return selected.get();
    }

    /**
     * Sets the selection state.
     * 
     * @param selected true to select
     */
    public void setSelected(boolean selected) {
        this.selected.set(selected);
        if (selected && onSelected != null) {
            onSelected.run();
        }
    }

    // Disabled
    /**
     * Gets the disabled property.
     * 
     * @return the disabled property
     */
    public BooleanProperty disabledProperty() {
        return disabled;
    }

    /**
     * Checks if the node is disabled.
     * 
     * @return true if disabled
     */
    public boolean isDisabled() {
        return disabled.get();
    }

    /**
     * Sets the disabled state.
     * 
     * @param disabled true to disable
     */
    public void setDisabled(boolean disabled) {
        this.disabled.set(disabled);
    }

    // Hovered
    /**
     * Gets the hovered property.
     * 
     * @return the hovered property
     */
    public BooleanProperty hoveredProperty() {
        return hovered;
    }

    /**
     * Checks if the node is hovered.
     * 
     * @return true if hovered
     */
    public boolean isHovered() {
        return hovered.get();
    }

    /**
     * Sets the hovered state.
     * 
     * @param hovered true if hovered
     */
    public void setHovered(boolean hovered) {
        this.hovered.set(hovered);
    }

    // Target Highlighted
    /**
     * Gets the target highlighted property.
     * 
     * @return the target highlighted property
     */
    public BooleanProperty targetHighlightedProperty() {
        return targetHighlighted;
    }

    /**
     * Checks if the node is target highlighted.
     * 
     * @return true if target highlighted
     */
    public boolean isTargetHighlighted() {
        return targetHighlighted.get();
    }

    /**
     * Sets the target highlighted state.
     * 
     * @param highlighted true to highlight as target
     */
    public void setTargetHighlighted(boolean highlighted) {
        this.targetHighlighted.set(highlighted);
    }

    // Execution State
    /**
     * Gets the execution state property.
     * 
     * @return the execution state property
     */
    public ObjectProperty<ExecutionState> executionStateProperty() {
        return executionState;
    }

    /**
     * Gets the current execution state.
     * 
     * @return the current execution state
     */
    public ExecutionState getExecutionState() {
        return executionState.get();
    }

    /**
     * Sets the execution state.
     * 
     * @param state the execution state to set
     */
    public void setExecutionState(ExecutionState state) {
        setExecutionState(state, null);
    }

    // Error Message
    /**
     * Gets the error message property.
     * 
     * @return the error message property
     */
    public ReadOnlyStringProperty errorMessageProperty() {
        return errorMessage;
    }

    /**
     * Gets the error message.
     * 
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage.get();
    }

    // Has Error
    /**
     * Gets the has error property.
     * 
     * @return the has error property
     */
    public ReadOnlyBooleanProperty hasErrorProperty() {
        return hasError;
    }

    /**
     * Checks if the node has an error.
     * 
     * @return true if node has error
     */
    public boolean hasError() {
        return hasError.get();
    }

    // Custom Icon
    /**
     * Gets the custom icon property.
     * 
     * @return the custom icon property
     */
    public StringProperty customIconProperty() {
        return customIcon;
    }

    /**
     * Gets the custom icon identifier.
     * 
     * @return the custom icon identifier
     */
    public String getCustomIcon() {
        return customIcon.get();
    }

    /**
     * Sets the custom icon identifier.
     * 
     * @param icon the custom icon identifier to set
     */
    public void setCustomIcon(String icon) {
        customIcon.set(icon);
    }

    // Subtitle
    /**
     * Gets the subtitle property.
     * 
     * @return the subtitle property
     */
    public ReadOnlyStringProperty subtitleProperty() {
        return subtitle;
    }

    /**
     * Gets the subtitle text.
     * 
     * @return the subtitle text
     */
    public String getSubtitle() {
        return subtitle.get();
    }

    // Category
    /**
     * Gets the category property.
     * 
     * @return the category property
     */
    public ReadOnlyObjectProperty<NodeCategory> categoryProperty() {
        return category;
    }

    /**
     * Gets the node category.
     * 
     * @return the node category
     */
    public NodeCategory getCategory() {
        return category.get();
    }

    // Notes
    /**
     * Gets the notes property.
     * 
     * @return the notes property
     */
    public StringProperty notesProperty() {
        return notes;
    }

    /**
     * Gets the node notes.
     * 
     * @return the node notes
     */
    public String getNotes() {
        return notes.get();
    }

    /**
     * Sets the node notes.
     * 
     * @param notes the notes to set
     */
    public void setNotes(String notes) {
        this.notes.set(notes);
    }

    // Domain Node
    /**
     * Gets the domain node property.
     * 
     * @return the domain node property
     */
    public ReadOnlyObjectProperty<Node> nodeProperty() {
        return node;
    }

    /**
     * Gets the underlying domain node.
     * 
     * @return the underlying domain node
     */
    public Node getNode() {
        return node.get();
    }

    // Parameters
    /**
     * Gets the parameters property.
     * 
     * @return the parameters property
     */
    public ObjectProperty<Map<String, Object>> parametersProperty() {
        return parameters;
    }

    /**
     * Gets the map of node parameters.
     * 
     * @return the map of node parameters
     */
    public Map<String, Object> getParameters() {
        return parameters.get();
    }

    /**
     * Sets the map of node parameters.
     * 
     * @param params the map of parameters to set
     */
    public void setParameters(Map<String, Object> params) {
        parameters.set(params != null ? new HashMap<>(params) : new HashMap<>());
    }

    // ===== Callbacks =====

    /**
     * Sets the callback for position changes.
     * 
     * @param callback the callback to run when position changes
     */
    public void setOnPositionChanged(Runnable callback) {
        this.onPositionChanged = callback;
    }

    /**
     * Sets the callback for state changes.
     * 
     * @param callback the callback to run when state changes
     */
    public void setOnStateChanged(Runnable callback) {
        this.onStateChanged = callback;
    }

    /**
     * Sets the callback for selection.
     * 
     * @param callback the callback to run when node is selected
     */
    public void setOnSelected(Runnable callback) {
        this.onSelected = callback;
    }

    public void setOnDeleted(Runnable callback) {
        this.onDeleted = callback;
    }

    /**
     * Notifies that the node should be deleted.
     */
    public void notifyDeleted() {
        if (onDeleted != null) {
            onDeleted.run();
        }
    }

    @Override
    public String toString() {
        return "NodeViewModel{" +
                "nodeId='" + nodeId.get() + '\'' +
                ", nodeType='" + nodeType.get() + '\'' +
                ", name='" + name.get() + '\'' +
                ", position=(" + layoutX.get() + ", " + layoutY.get() + ")" +
                ", selected=" + selected.get() +
                ", executionState=" + executionState.get() +
                '}';
    }
}
