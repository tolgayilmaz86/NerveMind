package ai.nervemind.ui.canvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.DevModeServiceInterface;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import ai.nervemind.ui.canvas.NodeView.ExecutionState;
import ai.nervemind.ui.console.ExecutionConsoleService;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeExecutionState;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeState;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeStateListener;
import ai.nervemind.ui.debug.DebugToolbar;
import ai.nervemind.ui.node.BuiltInNodeType;
import ai.nervemind.ui.node.NodeTypeDescriptor;
import ai.nervemind.ui.node.NodeTypeRegistry;
import ai.nervemind.ui.service.PluginMenuManager;
import ai.nervemind.ui.view.canvas.ExecutionHistoryPanelComponent;
import ai.nervemind.ui.view.canvas.NodePropertiesPanelComponent;
import ai.nervemind.ui.view.dialog.DialogFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * Workflow canvas for visual node editing.
 * Supports drag-drop, zoom, pan, and node connections.
 */
public class WorkflowCanvas extends BorderPane implements NodeStateListener {

    private static final Logger LOGGER = Logger.getLogger(WorkflowCanvas.class.getName());

    private static final double GRID_SIZE = 20;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 2.0;
    private static final double ZOOM_FACTOR = 0.1;
    private static final String DEFAULT_WORKFLOW_NAME = "New Workflow";
    private static final String EXECUTION_ERROR_TITLE = "Execution Error";

    // Node type registry for unified node management
    private final NodeTypeRegistry nodeTypeRegistry;

    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;
    private final PluginServiceInterface pluginService;

    // Optional plugin menu manager for connection context menus
    private PluginMenuManager pluginMenuManager;

    private final Pane canvasPane;
    private final Pane nodeLayer;
    private final Pane connectionLayer;
    private final Pane gridLayer;
    private final ScrollPane paletteScrollPane;
    private final ScrollPane canvasScrollPane;
    private VBox nodePalette;
    private final Label statusLabel;
    private final NodePropertiesPanelComponent propertiesPanel;
    private final ExecutionHistoryPanelComponent executionHistoryPanel;
    private final CanvasMinimap minimap;
    private final UndoRedoManager undoRedoManager;
    private final DebugToolbar debugToolbar;

    private WorkflowDTO workflow;
    private final Map<String, NodeView> nodeViews = new HashMap<>();
    private final Map<String, ConnectionLine> connectionLines = new HashMap<>();

    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;

    // Transform objects for zoom/pan (use transforms with pivot at origin for
    // consistent behavior)
    private final Scale nodeLayerScale = new Scale(1, 1, 0, 0);
    private final Scale connectionLayerScale = new Scale(1, 1, 0, 0);
    private final Translate nodeLayerTranslate = new Translate(0, 0);
    private final Translate connectionLayerTranslate = new Translate(0, 0);

    // Drag state
    private double dragStartX;
    private double dragStartY;
    private boolean isPanning = false;

    // Grid settings
    private boolean showGrid = true;
    private boolean snapToGrid = true;

    // Connection dragging state
    private boolean isConnectionDragging = false;
    private NodeView connectionSource = null;
    private String connectionSourceHandleId = null; // Track source handle for multi-handle support
    private NodeView hoveredTarget = null;
    private String hoveredTargetHandleId = null; // Track target handle for multi-handle support
    private CubicCurve tempConnectionLine = null;

    // Selection
    private final java.util.Set<NodeView> selectedNodes = new java.util.HashSet<>();
    private final java.util.Set<ConnectionLine> selectedConnections = new java.util.HashSet<>();

    // Clipboard
    private final java.util.List<Node> clipboardNodes = new java.util.ArrayList<>();

    // Context menu state
    private ContextMenu currentContextMenu = null;

    // File chooser state
    private java.io.File lastImportDirectory = null;
    private java.io.File lastExportDirectory = null;

    // Workflow settings (stored, shown in dialog)
    private String workflowName = DEFAULT_WORKFLOW_NAME;
    private String workflowDescription = "";
    private boolean workflowActive = false;
    private Label workflowTitleLabel;
    private Label activeIndicator;

    // Dirty state property
    private final javafx.beans.property.BooleanProperty dirty = new javafx.beans.property.SimpleBooleanProperty(false);

    public javafx.beans.property.BooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void markAsClean() {
        dirty.set(false);
    }

    public WorkflowCanvas(WorkflowServiceInterface workflowService, ExecutionServiceInterface executionService,
            PluginServiceInterface pluginService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.pluginService = pluginService;

        // Initialize node type registry with built-in nodes and plugins
        this.nodeTypeRegistry = new NodeTypeRegistry();
        this.nodeTypeRegistry.updateFromPluginService(pluginService);

        // Initialize layers
        gridLayer = new Pane();
        connectionLayer = new Pane();
        nodeLayer = new Pane();

        // Apply transforms to layers with pivot at origin for consistent zoom behavior
        // This ensures both nodeLayer and connectionLayer scale from the same point
        nodeLayer.getTransforms().addAll(nodeLayerTranslate, nodeLayerScale);
        connectionLayer.getTransforms().addAll(connectionLayerTranslate, connectionLayerScale);

        // Connection layer on top so connections can receive mouse events
        // But nodes will still receive events because they're added directly to
        // nodeLayer
        canvasPane = new Pane(gridLayer, nodeLayer, connectionLayer);
        canvasPane.getStyleClass().add("canvas-pane");

        // Make connection layer pass through mouse events to nodes below when not on a
        // connection
        connectionLayer.setPickOnBounds(false);

        // Status label for feedback
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("canvas-status");

        // Wrap in scroll pane
        canvasScrollPane = new ScrollPane(canvasPane);
        canvasScrollPane.setPannable(false);
        canvasScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        canvasScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        canvasScrollPane.setFitToWidth(true);
        canvasScrollPane.setFitToHeight(true);

        canvasScrollPane.getStyleClass().add("canvas-scroll");

        // Initialize undo/redo manager
        // Initialize undo/redo manager
        undoRedoManager = new UndoRedoManager();
        undoRedoManager.setOnStateChanged(() -> dirty.set(true));

        // Node palette on the left with scroll
        nodePalette = createNodePalette();
        paletteScrollPane = new ScrollPane(nodePalette);
        paletteScrollPane.setFitToWidth(true);
        paletteScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        paletteScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        paletteScrollPane.getStyleClass().add("palette-scroll");

        // Properties panel on the right (MVVM component)
        propertiesPanel = new NodePropertiesPanelComponent();
        propertiesPanel.setOnShowAdvancedEditor(nodeId -> {
            NodeView view = nodeViews.get(nodeId);
            if (view != null) {
                showAdvancedEditor(view);
            }
        });
        propertiesPanel.setOnShowHelp(this::showNodeHelpDialog);
        propertiesPanel.setOnApplyChanges((nodeId, updatedNode) -> {
            Node originalNode = workflow.nodes().stream()
                    .filter(n -> n.id().equals(nodeId))
                    .findFirst()
                    .orElse(null);

            if (originalNode != null) {
                undoRedoManager.executeCommand(
                        new ai.nervemind.ui.canvas.commands.EditNodeCommand(
                                this, originalNode, updatedNode, "Edit Properties"));
            }
        });

        // Execution history panel (toggleable on the right side below properties, MVVM
        // component)
        executionHistoryPanel = new ExecutionHistoryPanelComponent();

        // Create right side container for properties and history
        VBox rightContainer = new VBox();
        rightContainer.getChildren().addAll(propertiesPanel, executionHistoryPanel);
        VBox.setVgrow(propertiesPanel, Priority.NEVER);
        VBox.setVgrow(executionHistoryPanel, Priority.ALWAYS);

        // Minimap in bottom-right corner (positioned over canvas)
        minimap = new CanvasMinimap(this);
        minimap.setVisible(true);
        minimap.setManaged(true);

        // Floating control bar - always visible at top center
        debugToolbar = new DebugToolbar();
        debugToolbar.setVisible(true);
        debugToolbar.setManaged(true);
        // Prevent toolbar from expanding to fill the StackPane
        debugToolbar.setMaxWidth(Region.USE_PREF_SIZE);
        debugToolbar.setMaxHeight(Region.USE_PREF_SIZE);
        // Ensure mouse events pass through to canvas when not on the toolbar
        debugToolbar.setPickOnBounds(false);

        // Create a container for center with minimap and control bar overlays
        StackPane centerContainer = new StackPane(canvasScrollPane, minimap, debugToolbar);
        StackPane.setAlignment(minimap, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(minimap, new Insets(0, 16, 16, 0));
        StackPane.setAlignment(debugToolbar, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(debugToolbar, new Insets(20, 0, 0, 0));

        // Load debug toolbar stylesheet
        centerContainer.getStylesheets().add(
                getClass().getResource("/ai/nervemind/ui/styles/debug.css").toExternalForm());

        // Layout - no top bar, workflow info is in palette header
        setCenter(centerContainer);
        setLeft(paletteScrollPane);
        setRight(rightContainer);

        // Setup interactions
        setupCanvasInteraction();
        setupKeyboardShortcuts();

        // Register for node state changes from ExecutionConsoleService
        ExecutionConsoleService.getInstance().addNodeStateListener(this);

        // Draw grid
        drawGrid();

        // Create empty workflow
        newWorkflow();
    }

    private void updateActiveIndicator() {
        if (activeIndicator == null)
            return;
        if (workflowActive) {
            activeIndicator.setText("●");
            activeIndicator.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 10px;");
            Tooltip.install(activeIndicator, new Tooltip("Workflow is Active"));
        } else {
            activeIndicator.setText("");
        }
    }

    @SuppressWarnings("unused")
    private void updateActiveIndicatorLegacy(Label indicator) {
        if (workflowActive) {
            indicator.setText("● ACTIVE");
            indicator.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            indicator.setText("");
        }
    }

    private void showWorkflowSettings() {
        javafx.stage.Window owner = getScene() != null ? getScene().getWindow() : null;
        DialogFactory.showWorkflowSettings(owner, workflowName, workflowDescription, workflowActive)
                .ifPresent(settings -> {
                    workflowName = settings.name();
                    workflowDescription = settings.description();
                    workflowActive = settings.isActive();

                    // Update UI
                    workflowTitleLabel.setText(workflowName);
                    Tooltip.install(workflowTitleLabel, new Tooltip(workflowName));

                    // Update active indicator
                    updateActiveIndicator();

                    showStatus("Settings updated: " + workflowName);
                });
    }

    private VBox createNodePalette() {
        VBox palette = new VBox(8);
        palette.setPadding(new Insets(8));
        palette.setPrefWidth(200);
        palette.getStyleClass().add("node-palette");

        // Workflow header with name, settings icon, and active indicator
        HBox workflowHeader = new HBox(4);
        workflowHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        workflowHeader.setPadding(new Insets(2, 0, 6, 0));

        // Workflow icon
        FontIcon workflowIcon = new FontIcon(MaterialDesignF.FILE_TREE);
        workflowIcon.setIconSize(14);
        workflowIcon.setIconColor(Color.web("#88c0d0"));

        // Workflow title label (truncated if too long)
        workflowTitleLabel = new Label(workflowName);
        workflowTitleLabel.setStyle("-fx-text-fill: #e5e5e5; -fx-font-size: 12px; -fx-font-weight: bold;");
        workflowTitleLabel.setMaxWidth(120);
        workflowTitleLabel.setEllipsisString("...");
        workflowTitleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        Tooltip.install(workflowTitleLabel, new Tooltip(workflowName));
        HBox.setHgrow(workflowTitleLabel, Priority.ALWAYS);

        // Active indicator (small green dot)
        activeIndicator = new Label();
        updateActiveIndicator();

        // Settings button (small cog)
        Button settingsBtn = new Button();
        FontIcon settingsIcon = new FontIcon(MaterialDesignC.COG);
        settingsIcon.setIconSize(14);
        settingsIcon.setIconColor(Color.web("#a3a3a3"));
        settingsBtn.setGraphic(settingsIcon);
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
        settingsBtn.setMinSize(20, 20);
        settingsBtn.setMaxSize(20, 20);
        Tooltip.install(settingsBtn, new Tooltip("Workflow Settings"));
        settingsBtn.setOnAction(e -> showWorkflowSettings());

        workflowHeader.getChildren().addAll(workflowIcon, workflowTitleLabel, activeIndicator, settingsBtn);
        palette.getChildren().add(workflowHeader);

        // Separator line
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #404040; -fx-min-height: 1; -fx-max-height: 1;");
        palette.getChildren().add(separator);

        // Nodes title
        Label title = new Label("Nodes");
        title.getStyleClass().add("palette-title");
        title.setStyle("-fx-font-size: 11px; -fx-text-fill: #a3a3a3;");
        palette.getChildren().add(title);

        // Create sections from registry, organized by category
        for (NodeCategory category : nodeTypeRegistry.getActiveCategories()) {
            List<NodeTypeDescriptor> nodesInCategory = nodeTypeRegistry.getByCategory(category);
            if (!nodesInCategory.isEmpty()) {
                List<javafx.scene.Node> items = new ArrayList<>();
                for (NodeTypeDescriptor nodeType : nodesInCategory) {
                    items.add(createPaletteItem(
                            nodeType.getDisplayName(),
                            nodeType.getId(),
                            nodeType.getIcon()));
                }
                palette.getChildren().add(createPaletteSection(
                        category.getDisplayName(),
                        items.toArray(new javafx.scene.Node[0])));
            }
        }

        return palette;
    }

    /**
     * Refreshes the node palette to reflect changes in enabled plugins.
     * 
     * <p>
     * This method should be called after plugin enable/disable state changes
     * to update the UI. It recreates the entire palette by calling
     * {@link #createNodePalette()} and updates the scroll pane content.
     * </p>
     * 
     * <p>
     * <strong>When to Call</strong>
     * </p>
     * <ul>
     * <li>After enabling/disabling plugins in the Plugin Manager</li>
     * <li>After reloading plugins from the plugins directory</li>
     * <li>When returning to the Workflow view (called by MainViewController)</li>
     * </ul>
     * 
     * <p>
     * <strong>Thread Safety</strong>
     * </p>
     * <p>
     * Must be called from the JavaFX Application Thread.
     * </p>
     * 
     * @see ai.nervemind.ui.view.MainViewController#showWorkflowsView() Caller
     * @see ai.nervemind.common.service.PluginServiceInterface#setPluginEnabled(String,
     *      boolean)
     */
    public void refreshPalette() {
        if (paletteScrollPane != null) {
            // Update registry with latest plugin state
            nodeTypeRegistry.updateFromPluginService(pluginService);
            // Recreate palette
            nodePalette = createNodePalette();
            paletteScrollPane.setContent(nodePalette);
        }
    }

    private TitledPane createPaletteSection(String title, javafx.scene.Node... items) {
        VBox content = new VBox(2);
        content.setPadding(Insets.EMPTY);
        content.getChildren().addAll(items);

        TitledPane section = new TitledPane(title, content);
        section.setCollapsible(true);
        section.setExpanded(true);
        section.getStyleClass().add("palette-section");

        // Add specific class based on title (e.g., "palette-section-triggers")
        // Sanitize: lowercase, replace spaces with dashes, remove special chars like &
        String specificClass = "palette-section-" + title.toLowerCase()
                .replace(" & ", "-") // "AI & ML" -> "ai-ml"
                .replace(" ", "-")
                .replaceAll("[^a-z0-9-]", "");
        section.getStyleClass().add(specificClass);

        return section;
    }

    private HBox createPaletteItem(String name, String nodeType, org.kordamp.ikonli.Ikon icon) {
        HBox item = new HBox(8);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setPadding(new Insets(5, 10, 5, 10));
        item.getStyleClass().add("palette-item");

        FontIcon fontIcon = FontIcon.of(icon, 16);

        Label label = new Label(name);

        item.getChildren().addAll(fontIcon, label);

        // Drag to add node
        item.setOnDragDetected(e -> {
            item.startFullDrag();
            e.consume();
        });

        item.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                addNodeAtCenter(nodeType, name);
            }
        });

        // Enable dragging onto canvas
        item.setOnMouseReleased(e -> {
            if (e.getSceneX() > nodePalette.getWidth()) {
                Point2D canvasPoint = canvasPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                addNode(nodeType, name, canvasPoint.getX(), canvasPoint.getY());
            }
        });

        return item;
    }

    private void setupCanvasInteraction() {
        // Pan with middle mouse or space+drag
        canvasPane.setOnMousePressed(this::handleCanvasMousePressed);
        canvasPane.setOnMouseDragged(this::handleCanvasMouseDragged);
        canvasPane.setOnMouseReleased(this::handleCanvasMouseReleased);

        // Also track mouse movement for connection dragging
        canvasPane.setOnMouseMoved(this::handleCanvasMouseMoved);

        // Zoom with scroll
        canvasPane.setOnScroll(this::handleScroll);

        // Click on canvas to deselect or show context menu
        canvasPane.setOnMouseClicked(this::handleCanvasMouseClicked);
    }

    private void handleCanvasMousePressed(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.MIDDLE ||
                (e.getButton() == MouseButton.PRIMARY && e.isShiftDown())) {
            isPanning = true;
            dragStartX = e.getX();
            dragStartY = e.getY();
            canvasPane.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        }
    }

    private void handleCanvasMouseDragged(javafx.scene.input.MouseEvent e) {
        if (isPanning) {
            double deltaX = e.getX() - dragStartX;
            double deltaY = e.getY() - dragStartY;
            translateX += deltaX;
            translateY += deltaY;
            updateCanvasTransform();
            dragStartX = e.getX();
            dragStartY = e.getY();
        }
        // Update connection drag if active
        if (isConnectionDragging) {
            updateConnectionDrag(e.getSceneX(), e.getSceneY());
        }
    }

    private void handleCanvasMouseReleased(javafx.scene.input.MouseEvent e) {
        if (isPanning) {
            isPanning = false;
            canvasPane.setCursor(javafx.scene.Cursor.DEFAULT);
        }
        // Cancel connection drag if released on empty space
        if (isConnectionDragging) {
            endConnectionDrag();
        }
    }

    private void handleCanvasMouseMoved(javafx.scene.input.MouseEvent e) {
        if (isConnectionDragging) {
            updateConnectionDrag(e.getSceneX(), e.getSceneY());
        }
    }

    private void handleCanvasMouseClicked(javafx.scene.input.MouseEvent e) {
        if (!isCanvasTarget(e.getTarget())) {
            return;
        }
        if (e.getButton() == MouseButton.PRIMARY) {
            deselectAll();
            hideCurrentContextMenu();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            showCanvasContextMenu(e.getScreenX(), e.getScreenY(), e.getX(), e.getY());
        }
    }

    private boolean isCanvasTarget(Object target) {
        return target == canvasPane || target == nodeLayer ||
                target == connectionLayer || target == gridLayer;
    }

    private void hideCurrentContextMenu() {
        if (currentContextMenu != null) {
            currentContextMenu.hide();
            currentContextMenu = null;
        }
    }

    /**
     * Show context menu for canvas (empty space).
     */
    private void showCanvasContextMenu(double screenX, double screenY, double canvasX, double canvasY) {
        // Hide any existing context menu
        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }

        ContextMenu contextMenu = new ContextMenu();
        currentContextMenu = contextMenu;

        // Add Node submenu
        Menu addNodeMenu = new Menu("Add Node");
        addNodeMenu.setGraphic(FontIcon.of(MaterialDesignP.PLUS, 14));

        // Create category submenus from registry
        Map<NodeCategory, Menu> categoryMenus = new java.util.LinkedHashMap<>();

        for (NodeCategory category : nodeTypeRegistry.getActiveCategories()) {
            Menu categoryMenu = new Menu(category.getDisplayName());
            List<NodeTypeDescriptor> nodesInCategory = nodeTypeRegistry.getByCategory(category);

            for (NodeTypeDescriptor nodeType : nodesInCategory) {
                categoryMenu.getItems().add(
                        createAddNodeMenuItem(nodeType.getDisplayName(), nodeType.getId(), canvasX, canvasY));
            }

            categoryMenus.put(category, categoryMenu);
        }

        // Add category menus to main Add Node menu
        addNodeMenu.getItems().addAll(categoryMenus.values());

        // Auto-layout
        MenuItem autoLayoutItem = new MenuItem("Auto Layout");
        autoLayoutItem.setGraphic(FontIcon.of(MaterialDesignA.AUTO_FIX, 14));
        autoLayoutItem.setOnAction(e -> autoLayoutNodes());

        // Fit to View
        MenuItem fitViewItem = new MenuItem("Fit to View");
        fitViewItem.setGraphic(FontIcon.of(MaterialDesignF.FIT_TO_PAGE, 14));
        fitViewItem.setOnAction(e -> fitToView());

        // Select All
        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> selectAll());

        // Deselect All
        MenuItem deselectAllItem = new MenuItem("Deselect All");
        deselectAllItem.setOnAction(e -> deselectAll());

        // Copy
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 14));
        copyItem.setOnAction(e -> copySelected());

        // Paste (if something in clipboard)
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_PASTE, 14));
        pasteItem.setDisable(clipboardNodes.isEmpty());
        pasteItem.setOnAction(e -> pasteNodes());

        contextMenu.getItems().addAll(
                addNodeMenu,
                new SeparatorMenuItem(),
                autoLayoutItem,
                fitViewItem,
                new SeparatorMenuItem(),
                selectAllItem,
                deselectAllItem,
                new SeparatorMenuItem(),
                copyItem,
                pasteItem);

        contextMenu.show(canvasPane, screenX, screenY);
    }

    private MenuItem createAddNodeMenuItem(String name, String type, double x, double y) {
        MenuItem item = new MenuItem(name);
        item.setOnAction(e -> addNode(type, name, x, y));
        return item;
    }

    private void handleScroll(ScrollEvent e) {
        if (e.isControlDown()) {
            // Zoom
            double delta = e.getDeltaY() > 0 ? ZOOM_FACTOR : -ZOOM_FACTOR;
            double newScale = Math.clamp(scale + delta, MIN_ZOOM, MAX_ZOOM);

            if (newScale != scale) {
                scale = newScale;
                updateCanvasTransform();
            }
            e.consume();
        }
    }

    private void updateCanvasTransform() {
        // Use transform objects with pivot at (0,0) for consistent zoom behavior
        // This ensures both layers scale from the same origin point
        nodeLayerScale.setX(scale);
        nodeLayerScale.setY(scale);
        connectionLayerScale.setX(scale);
        connectionLayerScale.setY(scale);

        nodeLayerTranslate.setX(translateX);
        nodeLayerTranslate.setY(translateY);
        connectionLayerTranslate.setX(translateX);
        connectionLayerTranslate.setY(translateY);
    }

    /**
     * Update the canvas pane size to fit all content.
     * This ensures the ScrollPane knows the proper content bounds.
     */
    private void updateCanvasBounds() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        // Include all node positions
        for (NodeView nodeView : nodeViews.values()) {
            double x = nodeView.getLayoutX();
            double y = nodeView.getLayoutY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + 200); // Node width + margin
            maxY = Math.max(maxY, y + 120); // Node height + margin
        }

        // If no nodes, use default bounds
        if (minX == Double.MAX_VALUE) {
            minX = 0;
            minY = 0;
            maxX = 2000;
            maxY = 1500;
        }

        // Add padding
        minX -= 200;
        minY -= 200;
        maxX += 200;
        maxY += 200;

        // Ensure minimum size
        double width = Math.max(2000, maxX - minX);
        double height = Math.max(1500, maxY - minY);

        // Set canvas pane size
        canvasPane.setPrefSize(width, height);
        canvasPane.setMinSize(width, height);

        // Update minimap
        minimap.update();
    }

    private void setupKeyboardShortcuts() {
        setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                handleCtrlKeyShortcut(e);
            } else {
                handleRegularKeyShortcut(e);
            }
        });
        setFocusTraversable(true);
    }

    private void handleCtrlKeyShortcut(javafx.scene.input.KeyEvent e) {
        switch (e.getCode()) {
            case C -> {
                if (!selectedNodes.isEmpty()) {
                    copySelected();
                }
                e.consume();
            }
            case V -> {
                pasteNodes();
                e.consume();
            }
            case A -> {
                selectAll();
                e.consume();
            }
            case E -> {
                exportWorkflow();
                e.consume();
            }
            case Z -> {
                undo();
                e.consume();
            }
            case Y -> {
                redo();
                e.consume();
            }
            case X -> {
                // Cut: copy then delete
                if (!selectedNodes.isEmpty()) {
                    copySelected();
                    deleteSelected();
                }
                e.consume();
            }
            default -> {
                // Ignore other keys
            }
        }
    }

    private void handleRegularKeyShortcut(javafx.scene.input.KeyEvent e) {
        switch (e.getCode()) {
            case DELETE, BACK_SPACE -> handleDeleteKey();
            case ESCAPE -> handleEscapeKey();
            default -> {
                // No action for other keys
            }
        }
    }

    private void handleDeleteKey() {
        if (!selectedConnections.isEmpty()) {
            deleteSelectedConnections();
        } else {
            deleteSelected();
        }
    }

    private void handleEscapeKey() {
        deselectAll();
        deselectAllConnections();
        if (currentContextMenu != null) {
            currentContextMenu.hide();
            currentContextMenu = null;
        }
    }

    private void drawGrid() {
        gridLayer.getChildren().clear();

        if (!showGrid) {
            return;
        }

        double width = 2000;
        double height = 2000;

        // Draw grid lines
        for (double x = 0; x < width; x += GRID_SIZE) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, 0, x, height);
            line.setStroke(Color.gray(0.3, 0.3));
            line.setStrokeWidth(0.5);
            gridLayer.getChildren().add(line);
        }

        for (double y = 0; y < height; y += GRID_SIZE) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, y, width, y);
            line.setStroke(Color.gray(0.3, 0.3));
            line.setStrokeWidth(0.5);
            gridLayer.getChildren().add(line);
        }
    }

    public void newWorkflow() {
        workflow = WorkflowDTO.create(DEFAULT_WORKFLOW_NAME);
        nodeViews.clear();
        connectionLines.clear();
        nodeLayer.getChildren().clear();
        connectionLayer.getChildren().clear();

        // Add a default trigger node
        NodeTypeDescriptor manualTrigger = BuiltInNodeType.MANUAL_TRIGGER;
        addNode(manualTrigger.getId(), manualTrigger.getDisplayName(), 100, 100);

        // Reset workflow settings
        workflowName = DEFAULT_WORKFLOW_NAME;
        workflowDescription = "";
        workflowActive = false;
        workflowTitleLabel.setText(workflowName);
        Tooltip.install(workflowTitleLabel, new Tooltip(workflowName));

        // Update active indicator
        updateActiveIndicator();
        undoRedoManager.clear();
        markAsClean();
    }

    public void loadWorkflow(WorkflowDTO workflow) {
        this.workflow = workflow;
        nodeViews.clear();
        connectionLines.clear();
        nodeLayer.getChildren().clear();
        connectionLayer.getChildren().clear();

        // Load nodes
        for (Node node : workflow.nodes()) {
            NodeView nodeView = new NodeView(node, this);
            nodeViews.put(node.id(), nodeView);
            nodeLayer.getChildren().add(nodeView);
        }

        // Load connections
        for (Connection connection : workflow.connections()) {
            createConnectionLine(connection);
        }

        // Update canvas bounds and minimap
        updateCanvasBounds();

        // Update workflow settings from loaded workflow
        workflowName = workflow.name();
        workflowDescription = workflow.description() != null ? workflow.description() : "";
        workflowActive = workflow.isActive();
        workflowTitleLabel.setText(workflowName);
        Tooltip.install(workflowTitleLabel, new Tooltip(workflowName));

        // Update active indicator
        updateActiveIndicator();

        showStatus("Loaded: " + workflow.name());
        undoRedoManager.clear();
        markAsClean();
    }

    public void saveWorkflow() {
        try {
            WorkflowDTO saved;
            if (workflow.id() == null) {
                // New workflow - prompt for name if needed
                String name = workflowName;
                if (name == null || name.isBlank() || name.equals(DEFAULT_WORKFLOW_NAME)) {
                    TextInputDialog dialog = new TextInputDialog("My Workflow");
                    dialog.setTitle("Save Workflow");
                    dialog.setHeaderText("Enter workflow name:");
                    dialog.setContentText("Name:");

                    var result = dialog.showAndWait();
                    if (result.isEmpty()) {
                        return; // User cancelled
                    }
                    name = result.get();
                    workflowName = name;
                    workflowTitleLabel.setText(name);
                }

                // Create with active state
                workflow = workflow.withName(name).withActive(workflowActive);
                saved = workflowService.create(workflow);
            } else {
                // Update existing workflow with current settings
                String name = workflowName;
                if (name.isBlank()) {
                    name = workflow.name();
                    workflowName = name;
                    workflowTitleLabel.setText(name);
                }
                workflow = workflow.withName(name).withActive(workflowActive);
                saved = workflowService.update(workflow);
            }
            this.workflow = saved;
            markAsClean();
            showStatus("Saved: " + saved.name());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Workflow Saved");
            alert.setHeaderText(null);
            alert.setContentText("Workflow '" + saved.name() + "' saved successfully!");
            alert.showAndWait();
        } catch (Exception e) {
            showStatus("Error saving: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save workflow");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Save the workflow with a new name (Save As).
     */
    public void saveWorkflowAs() {
        TextInputDialog dialog = new TextInputDialog(workflowName != null ? workflowName : "My Workflow");
        dialog.setTitle("Save Workflow As");
        dialog.setHeaderText("Enter a new name for the workflow:");
        dialog.setContentText("Name:");

        var result = dialog.showAndWait();
        if (result.isEmpty()) {
            return; // User cancelled
        }

        String newName = result.get().trim();
        if (newName.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Name");
            alert.setHeaderText(null);
            alert.setContentText("Workflow name cannot be empty.");
            alert.showAndWait();
            return;
        }

        try {
            // Create a new workflow with the new name (removes ID to force create)
            workflowName = newName;
            workflowTitleLabel.setText(newName);
            workflow = new WorkflowDTO(
                    null, // New ID
                    newName,
                    workflow.description(),
                    workflow.nodes(),
                    workflow.connections(),
                    workflow.settings(),
                    workflowActive,
                    workflow.triggerType(),
                    workflow.cronExpression(),
                    java.time.Instant.now(),
                    java.time.Instant.now(),
                    null,
                    1);
            WorkflowDTO saved = workflowService.create(workflow);
            this.workflow = saved;
            markAsClean();
            showStatus("Saved as: " + saved.name());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Workflow Saved");
            alert.setHeaderText(null);
            alert.setContentText("Workflow saved as '" + saved.name() + "'!");
            alert.showAndWait();
        } catch (Exception e) {
            showStatus("Error saving: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Save Error");
            alert.setHeaderText("Failed to save workflow");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Stop the currently running workflow.
     */
    public void stopWorkflow() {
        if (workflow == null || workflow.id() == null) {
            showStatus("No workflow to stop");
            return;
        }

        showStatus("Stopping workflow...");
        try {
            int cancelled = executionService.cancelAllForWorkflow(workflow.id());
            if (cancelled > 0) {
                showStatus("Stopped " + cancelled + " execution(s)");
                // Reset debug toolbar and node highlights
                debugToolbar.hide();
                clearPausedNodeHighlight();
            } else {
                showStatus("No running executions to stop");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Failed to stop workflow: " + e.getMessage());
            showStatus("Error stopping workflow: " + e.getMessage());
        }
    }

    /**
     * Validate the current workflow for errors.
     */
    public void validateWorkflow() {
        var validator = new ai.nervemind.ui.canvas.validation.WorkflowValidator();
        var result = validator.validate(workflow);
        showValidationResults(result.errors(), result.warnings());
    }

    private void showValidationResults(java.util.List<String> errors, java.util.List<String> warnings) {
        Alert alert;
        if (errors.isEmpty() && warnings.isEmpty()) {
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Validation Passed");
            alert.setHeaderText("Workflow validation successful!");
            alert.setContentText("No errors or warnings found.");
        } else if (errors.isEmpty()) {
            alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Warnings");
            alert.setHeaderText("Workflow has " + warnings.size() + " warning(s)");
            alert.setContentText(String.join("\n• ", "Warnings:", String.join("\n• ", warnings)));
        } else {
            alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Failed");
            StringBuilder content = new StringBuilder();
            content.append("Errors (").append(errors.size()).append("):\n• ");
            content.append(String.join("\n• ", errors));
            if (!warnings.isEmpty()) {
                content.append("\n\nWarnings (").append(warnings.size()).append("):\n• ");
                content.append(String.join("\n• ", warnings));
            }
            alert.setHeaderText("Workflow has " + errors.size() + " error(s)");
            alert.setContentText(content.toString());
        }
        alert.showAndWait();
    }

    public void runWorkflow() {
        if (workflow.id() == null) {
            // Auto-save unsaved workflow before running
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Save Required");
            alert.setHeaderText("Workflow must be saved before running");
            alert.setContentText("The workflow will be saved automatically. Continue?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveWorkflow();
                    // After saving, check if it was successful and has an ID now
                    if (workflow.id() != null) {
                        runWorkflowAfterSave();
                    }
                }
            });
            return;
        }

        runWorkflowAfterSave();
    }

    private void runWorkflowAfterSave() {
        try {
            showStatus("Running: " + workflow.name() + "...");

            // Execute asynchronously
            executionService.executeAsync(workflow.id(), Map.of())
                    .thenAccept(result -> Platform.runLater(() -> {
                        // Reset debug toolbar to idle state when execution completes
                        debugToolbar.hide();
                        clearPausedNodeHighlight();

                        if (result.status() == ExecutionStatus.SUCCESS) {
                            showStatus("Completed: " + workflow.name());
                            showExecutionResult(result);
                        } else if (result.status() == ExecutionStatus.CANCELLED) {
                            showStatus("Cancelled: " + workflow.name());
                            // No alert for user-initiated cancellation
                        } else {
                            showStatus("Failed: " + workflow.name());
                            showExecutionError(result);
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            // Reset debug toolbar to idle state on error
                            debugToolbar.hide();
                            clearPausedNodeHighlight();
                            showWorkflowExecutionError(ex);
                        });
                        return null;
                    });
        } catch (Exception e) {
            showWorkflowStartError(e);
        }
    }

    private void showWorkflowExecutionError(Throwable ex) {
        showStatus("Error: " + ex.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(EXECUTION_ERROR_TITLE);
        alert.setHeaderText("Workflow execution failed");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void showWorkflowStartError(Exception e) {
        showStatus("Error: " + e.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(EXECUTION_ERROR_TITLE);
        alert.setHeaderText("Failed to start workflow execution");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void showExecutionResult(ExecutionDTO result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Execution Complete");
        alert.setHeaderText("Workflow executed successfully");
        alert.setContentText("Duration: " +
                (result.finishedAt() != null && result.startedAt() != null
                        ? java.time.Duration.between(result.startedAt(), result.finishedAt()).toMillis() + "ms"
                        : "N/A"));
        alert.showAndWait();
    }

    private void showExecutionError(ExecutionDTO result) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Execution Failed");
        alert.setHeaderText("Workflow execution failed");
        alert.setContentText("Status: " + result.status() +
                (result.errorMessage() != null ? "\nError: " + result.errorMessage() : ""));
        alert.showAndWait();
    }

    public void exportWorkflow() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Workflow");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName(workflow.name().replaceAll("[^a-zA-Z0-9]", "_") + ".json");

        // Set initial directory to last used location
        if (lastExportDirectory != null && lastExportDirectory.exists()) {
            fileChooser.setInitialDirectory(lastExportDirectory);
        }

        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            // Remember the directory for next time
            lastExportDirectory = file.getParentFile();

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                mapper.writeValue(file, workflow);
                showStatus("Exported: " + file.getName());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Workflow exported to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                showStatus("Export failed: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Failed to export workflow");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    public void importWorkflow() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Import Workflow");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));

        // Set initial directory to last used location
        if (lastImportDirectory != null && lastImportDirectory.exists()) {
            fileChooser.setInitialDirectory(lastImportDirectory);
        }

        java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            // Remember the directory for next time
            lastImportDirectory = file.getParentFile();

            try {
                WorkflowDTO newWorkflow = parseImportedWorkflow(file);

                loadWorkflow(newWorkflow);
                showStatus("Imported: " + newWorkflow.name());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Workflow '" + newWorkflow.name() + "' imported.\nRemember to save to persist changes.");
                alert.showAndWait();
            } catch (Exception e) {
                showStatus("Import failed: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import Error");
                alert.setHeaderText("Failed to import workflow");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Parse an imported workflow file.
     * Supports both regular workflow exports and sample workflow files.
     * Sample files have extra fields (category, difficulty, tags, etc.) that are
     * ignored.
     *
     * @param file the JSON file to parse
     * @return a new WorkflowDTO ready to be loaded
     * @throws java.io.IOException if parsing fails
     */
    @SuppressWarnings("unchecked")
    private WorkflowDTO parseImportedWorkflow(java.io.File file) throws java.io.IOException {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // Ignore unknown properties (sample files have extra fields like category,
        // difficulty, tags)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Read as generic map first to handle both workflow exports and sample files
        Map<String, Object> jsonMap = mapper.readValue(file, Map.class);

        // Extract the workflow portion - for sample files, this is in "workflow" field
        Map<String, Object> workflowMap = jsonMap;
        if (jsonMap.containsKey("workflow") && jsonMap.get("workflow") instanceof Map) {
            workflowMap = (Map<String, Object>) jsonMap.get("workflow");
        }

        // Extract fields with proper defaults
        String name = getStringOrDefault(workflowMap, "name", jsonMap, "Imported Workflow");
        String description = getStringOrDefault(workflowMap, "description", jsonMap, "");

        // Parse nodes - handle both direct list and nested format
        List<Node> nodes = parseNodes(mapper, workflowMap, jsonMap);

        // Parse connections
        List<Connection> connections = parseConnections(mapper, workflowMap, jsonMap);

        // Parse settings
        Map<String, Object> settings = parseSettings(workflowMap, jsonMap);

        // Parse trigger type
        TriggerType triggerType = parseTriggerType(workflowMap, jsonMap);

        // Parse cron expression
        String cronExpression = getStringOrDefault(workflowMap, "cronExpression", jsonMap, null);

        return new WorkflowDTO(
                null, // New ID will be assigned on save
                name + " (Imported)",
                description,
                nodes,
                connections,
                settings,
                false, // Not active by default
                triggerType,
                cronExpression,
                null, null, null, 1);
    }

    private String getStringOrDefault(Map<String, Object> primary, String key,
            Map<String, Object> fallback, String defaultValue) {
        Object value = primary.get(key);
        if (value == null && fallback != null) {
            value = fallback.get(key);
        }
        return value != null ? value.toString() : defaultValue;
    }

    private List<Node> parseNodes(com.fasterxml.jackson.databind.ObjectMapper mapper,
            Map<String, Object> workflowMap, Map<String, Object> jsonMap) {
        Object nodesObj = workflowMap.get("nodes");
        if (nodesObj == null) {
            nodesObj = jsonMap.get("nodes");
        }
        if (nodesObj instanceof List) {
            return mapper.convertValue(nodesObj,
                    mapper.getTypeFactory().constructCollectionType(List.class, Node.class));
        }
        return List.of();
    }

    private List<Connection> parseConnections(com.fasterxml.jackson.databind.ObjectMapper mapper,
            Map<String, Object> workflowMap, Map<String, Object> jsonMap) {
        Object connectionsObj = workflowMap.get("connections");
        if (connectionsObj == null) {
            connectionsObj = jsonMap.get("connections");
        }
        if (!(connectionsObj instanceof List)) {
            return List.of();
        }

        List<?> connectionList = (List<?>) connectionsObj;
        List<Connection> result = new ArrayList<>();
        int[] indexHolder = { 0 };

        for (Object connObj : connectionList) {
            parseConnectionObject(connObj, result, indexHolder);
        }
        return result;
    }

    /**
     * Parse a single connection object and add to the result list if valid.
     */
    @SuppressWarnings("unchecked")
    private void parseConnectionObject(Object connObj, List<Connection> result, int[] indexHolder) {
        if (!(connObj instanceof Map)) {
            return;
        }

        Map<String, Object> connMap = (Map<String, Object>) connObj;
        Connection connection = createConnectionFromMap(connMap, indexHolder);
        if (connection != null) {
            result.add(connection);
        }
    }

    /**
     * Create a Connection from a map representation.
     * Handles both standard and sample file field name formats.
     */
    private Connection createConnectionFromMap(Map<String, Object> connMap, int[] indexHolder) {
        String id = getConnectionField(connMap, "id", null);
        if (id == null || id.isBlank()) {
            id = "conn_" + (indexHolder[0]++) + "_" + UUID.randomUUID().toString().substring(0, 8);
        }

        String sourceNodeId = getConnectionField(connMap, "sourceNodeId", "source");
        String targetNodeId = getConnectionField(connMap, "targetNodeId", "target");

        if (sourceNodeId == null || targetNodeId == null) {
            return null;
        }

        String sourceOutput = getConnectionFieldWithDefault(connMap, "sourceOutput", "sourceHandle", "main");
        String targetInput = getConnectionFieldWithDefault(connMap, "targetInput", "targetHandle", "main");

        return new Connection(id, sourceNodeId, sourceOutput, targetNodeId, targetInput);
    }

    /**
     * Get a connection field value with a default if null.
     */
    private String getConnectionFieldWithDefault(Map<String, Object> map, String primaryKey,
            String fallbackKey, String defaultValue) {
        String value = getConnectionField(map, primaryKey, fallbackKey);
        return value != null ? value : defaultValue;
    }

    private String getConnectionField(Map<String, Object> map, String primaryKey, String fallbackKey) {
        Object value = map.get(primaryKey);
        if (value == null && fallbackKey != null) {
            value = map.get(fallbackKey);
        }
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSettings(Map<String, Object> workflowMap, Map<String, Object> jsonMap) {
        Object settingsObj = workflowMap.get("settings");
        if (settingsObj == null) {
            settingsObj = jsonMap.get("settings");
        }
        if (settingsObj instanceof Map) {
            return (Map<String, Object>) settingsObj;
        }
        return Map.of();
    }

    private TriggerType parseTriggerType(Map<String, Object> workflowMap, Map<String, Object> jsonMap) {
        Object triggerObj = workflowMap.get("triggerType");
        if (triggerObj == null) {
            triggerObj = jsonMap.get("triggerType");
        }
        if (triggerObj != null) {
            try {
                return TriggerType.valueOf(triggerObj.toString());
            } catch (IllegalArgumentException _) {
                // Default to MANUAL if invalid
            }
        }
        return TriggerType.MANUAL;
    }

    // ==================== Command Internal Methods ====================

    /**
     * Internal method to create a connection (used by AddConnectionCommand).
     */
    public String createConnectionInternal(String connectionId, String sourceNodeId, String sourceHandleId,
            String targetNodeId,
            String targetHandleId) {
        ai.nervemind.common.domain.Connection connection = new ai.nervemind.common.domain.Connection(
                connectionId, sourceNodeId, sourceHandleId,
                targetNodeId, targetHandleId);

        workflow = workflow.withAddedConnection(connection);
        createConnectionLine(connection);
        return connectionId;
    }

    // Existing addNode method delegates to command
    private void addNode(String type, String name, double x, double y) {
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.AddNodeCommand(this, type,
                        snapToGrid(x), snapToGrid(y)));
        updateCanvasBounds();
    }

    private void addNodeAtCenter(String type, String name) {
        double centerX = canvasPane.getWidth() / 2 - translateX;
        double centerY = canvasPane.getHeight() / 2 - translateY;
        addNode(type, name, centerX, centerY);
    }

    private double snapToGrid(double value) {
        if (!snapToGrid) {
            return value;
        }
        return Math.round(value / GRID_SIZE) * GRID_SIZE;
    }

    public void selectNode(NodeView nodeView) {
        selectNode(nodeView, false);
    }

    public void selectNode(NodeView nodeView, boolean multiSelect) {
        if (!multiSelect) {
            deselectAll();
        }

        if (selectedNodes.contains(nodeView)) {
            // Deselect if already selected
            selectedNodes.remove(nodeView);
            nodeView.setSelected(false);
        } else {
            // Select the node
            selectedNodes.add(nodeView);
            nodeView.setSelected(true);
        }

        // Show properties panel for single selection, hide for multi-selection
        if (selectedNodes.size() == 1) {
            propertiesPanel.show(selectedNodes.iterator().next());
        } else {
            propertiesPanel.hide();
        }
    }

    /**
     * Get the currently selected node (if exactly one is selected).
     *
     * @return the selected node, or null if none or multiple selected
     */
    public Node getSelectedNode() {
        if (selectedNodes.size() == 1) {
            return selectedNodes.iterator().next().getNode();
        }
        return null;
    }

    /**
     * Get all currently selected nodes.
     *
     * @return set of selected node views
     */
    public java.util.Set<NodeView> getSelectedNodes() {
        return java.util.Collections.unmodifiableSet(selectedNodes);
    }

    public void deselectAll() {
        for (NodeView nodeView : selectedNodes) {
            nodeView.setSelected(false);
        }
        selectedNodes.clear();
        // Hide properties panel
        propertiesPanel.hide();
    }

    /**
     * Deletes all currently selected nodes and their connections.
     */
    public void deleteSelected() {
        if (selectedNodes.isEmpty()) {
            return;
        }

        // Collect all node IDs to delete
        java.util.Set<String> nodeIdsToDelete = selectedNodes.stream()
                .map(nodeView -> nodeView.getNode().id())
                .collect(java.util.stream.Collectors.toSet());

        // Capture nodes and connections for undo
        java.util.List<Node> nodesToDelete = selectedNodes.stream()
                .map(NodeView::getNode)
                .toList();
        java.util.List<ai.nervemind.common.domain.Connection> connectionsToDelete = workflow.connections().stream()
                .filter(c -> nodeIdsToDelete.contains(c.sourceNodeId()) || nodeIdsToDelete.contains(c.targetNodeId()))
                .toList();

        selectedNodes.clear();

        undoRedoManager.executeCommand(
                ai.nervemind.ui.canvas.commands.CompositeCommand.deleteMultiple(
                        this, nodesToDelete, connectionsToDelete));
    }

    public void updateNodePosition(String nodeId, double x, double y) {
        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(nodeId)
                        ? n.withPosition(new Node.Position(snapToGrid(x), snapToGrid(y)))
                        : n)
                .toList();
        workflow = workflow.withNodes(newNodes);

        // Update connection lines
        updateConnectionsForNode(nodeId);

        // Update minimap to reflect new positions
        updateMinimap();
    }

    private void createConnectionLine(Connection connection) {
        NodeView source = nodeViews.get(connection.sourceNodeId());
        NodeView target = nodeViews.get(connection.targetNodeId());

        if (source != null && target != null) {
            ConnectionLine line = new ConnectionLine(connection, source, target, this);
            line.setPluginMenuManager(pluginMenuManager);
            connectionLines.put(connection.id(), line);
            connectionLayer.getChildren().add(line);

            // Notify plugins about the new connection so they can restore decorations
            ConnectionContextImpl context = new ConnectionContextImpl(line, this);
            pluginService.notifyConnectionCreated(context);
        }
    }

    private void updateConnectionsForNode(String nodeId) {
        connectionLines.values().stream()
                .filter(line -> line.involvesNode(nodeId))
                .forEach(ConnectionLine::updatePosition);
    }

    // ==================== Connection Selection System ====================

    /**
     * Select a connection line, optionally supporting multi-select.
     */
    public void selectConnection(ConnectionLine connectionLine, boolean multiSelect) {
        if (!multiSelect) {
            // Clear node selection when selecting connections
            deselectAll();
            deselectAllConnections();
        }

        if (selectedConnections.contains(connectionLine)) {
            // Deselect if already selected (toggle)
            selectedConnections.remove(connectionLine);
            connectionLine.setSelected(false);
        } else {
            // Select the connection
            selectedConnections.add(connectionLine);
            connectionLine.setSelected(true);
        }

        // Update status
        if (selectedConnections.size() == 1) {
            ConnectionLine selected = selectedConnections.iterator().next();
            showStatus("Connection selected: " +
                    getNodeNameById(selected.getConnection().sourceNodeId()) + " → " +
                    getNodeNameById(selected.getConnection().targetNodeId()));
        } else if (selectedConnections.size() > 1) {
            showStatus(selectedConnections.size() + " connections selected");
        }
    }

    /**
     * Deselect all connections.
     */
    public void deselectAllConnections() {
        for (ConnectionLine line : selectedConnections) {
            line.setSelected(false);
            line.hideContextMenu();
        }
        selectedConnections.clear();
    }

    /**
     * Delete all currently selected connections.
     */
    public void deleteSelectedConnections() {
        if (selectedConnections.isEmpty()) {
            return;
        }

        int count = selectedConnections.size();

        // Collect connection IDs to delete
        java.util.List<String> connectionIds = selectedConnections.stream()
                .map(line -> line.getConnection().id())
                .toList();

        // Delete each connection using undo/redo command
        for (String connectionId : connectionIds) {
            undoRedoManager
                    .executeCommand(new ai.nervemind.ui.canvas.commands.DeleteConnectionCommand(this, connectionId));
        }

        selectedConnections.clear();
        showStatus(count + " connection(s) deleted");
    }

    /**
     * Delete all connections for a specific node.
     */
    public void deleteAllConnectionsForNode(String nodeId) {
        java.util.List<String> connectionIds = connectionLines.values().stream()
                .filter(line -> line.involvesNode(nodeId))
                .map(line -> line.getConnection().id())
                .toList();

        if (connectionIds.isEmpty()) {
            showStatus("No connections to delete");
            return;
        }

        for (String connectionId : connectionIds) {
            undoRedoManager
                    .executeCommand(new ai.nervemind.ui.canvas.commands.DeleteConnectionCommand(this, connectionId));
        }

        showStatus(connectionIds.size() + " connection(s) deleted");
    }

    /**
     * Get node name by ID for status messages.
     */
    private String getNodeNameById(String nodeId) {
        NodeView nodeView = nodeViews.get(nodeId);
        return nodeView != null ? nodeView.getNode().name() : nodeId;
    }

    // ==================== Connection Dragging System ====================

    /**
     * Check if a connection drag is in progress.
     */
    public boolean isConnectionDragging() {
        return isConnectionDragging;
    }

    /**
     * Start dragging a new connection from a source node's output handle.
     * Legacy method for backward compatibility.
     */
    public void startConnectionDrag(NodeView source) {
        startConnectionDrag(source, "out"); // Default to "out" handle
    }

    /**
     * Start dragging a new connection from a specific output handle.
     * 
     * @param source   the source node
     * @param handleId the output handle ID
     */
    public void startConnectionDrag(NodeView source, String handleId) {
        if (!source.canBeConnectionSource()) {
            return;
        }

        isConnectionDragging = true;
        connectionSource = source;
        connectionSourceHandleId = handleId;

        // Create temporary connection line
        tempConnectionLine = new CubicCurve();
        tempConnectionLine.setFill(null);
        tempConnectionLine.setStroke(Color.web("#4a9eff"));
        tempConnectionLine.setStrokeWidth(2);
        tempConnectionLine.setStrokeDashOffset(0);
        tempConnectionLine.getStrokeDashArray().addAll(5.0, 5.0); // Dashed while dragging
        tempConnectionLine.setMouseTransparent(true);

        // Set start position based on handle
        double startX = source.getHandleX(handleId);
        double startY = source.getHandleY(handleId);
        tempConnectionLine.setStartX(startX);
        tempConnectionLine.setStartY(startY);
        tempConnectionLine.setControlX1(startX + 50);
        tempConnectionLine.setControlY1(startY);
        tempConnectionLine.setEndX(startX + 50);
        tempConnectionLine.setEndY(startY);
        tempConnectionLine.setControlX2(startX + 50);
        tempConnectionLine.setControlY2(startY);

        connectionLayer.getChildren().add(tempConnectionLine);

        // Highlight valid targets
        highlightValidTargets(source, true);
    }

    /**
     * Update the temporary connection line position during drag.
     */
    public void updateConnectionDrag(double sceneX, double sceneY) {
        if (!isConnectionDragging || tempConnectionLine == null || connectionSource == null) {
            return;
        }

        // Convert scene coordinates to canvas coordinates
        Point2D canvasPoint = connectionLayer.sceneToLocal(sceneX, sceneY);
        double endX = canvasPoint.getX();
        double endY = canvasPoint.getY();

        // Get start position from specific handle
        double startX = connectionSourceHandleId != null
                ? connectionSource.getHandleX(connectionSourceHandleId)
                : connectionSource.getOutputX();
        double startY = connectionSourceHandleId != null
                ? connectionSource.getHandleY(connectionSourceHandleId)
                : connectionSource.getOutputY();

        // Update bezier curve
        double dx = Math.abs(endX - startX) * 0.5;
        tempConnectionLine.setControlX1(startX + dx);
        tempConnectionLine.setControlY1(startY);
        tempConnectionLine.setControlX2(endX - dx);
        tempConnectionLine.setControlY2(endY);
        tempConnectionLine.setEndX(endX);
        tempConnectionLine.setEndY(endY);
    }

    /**
     * End the connection drag - check if hovering over valid target.
     */
    public void endConnectionDrag() {
        // Check if we're hovering over a valid target
        if (hoveredTarget != null && connectionSource != null && connectionSource.canConnectTo(hoveredTarget)) {
            String sourceOutput = connectionSourceHandleId != null ? connectionSourceHandleId : "main";
            String targetInput = hoveredTargetHandleId != null ? hoveredTargetHandleId : "main";
            // Create the connection through undo/redo
            undoRedoManager.executeCommand(
                    new ai.nervemind.ui.canvas.commands.AddConnectionCommand(
                            this, connectionSource.getNode().id(), sourceOutput,
                            hoveredTarget.getNode().id(), targetInput));
        } else if (connectionSource != null && tempConnectionLine != null) {
            // Try to find target under the mouse cursor
            NodeView target = findTargetNodeAtPoint(tempConnectionLine.getEndX(), tempConnectionLine.getEndY());
            if (target != null && connectionSource.canConnectTo(target)) {
                String sourceOutput = connectionSourceHandleId != null ? connectionSourceHandleId : "main";
                String targetInput = target.getInputHandleIds().isEmpty() ? "main" : target.getInputHandleIds().get(0);
                undoRedoManager.executeCommand(
                        new ai.nervemind.ui.canvas.commands.AddConnectionCommand(
                                this, connectionSource.getNode().id(), sourceOutput,
                                target.getNode().id(), targetInput));
            }
        }

        // Clean up
        if (tempConnectionLine != null) {
            connectionLayer.getChildren().remove(tempConnectionLine);
            tempConnectionLine = null;
        }

        if (connectionSource != null) {
            highlightValidTargets(connectionSource, false);
        }

        isConnectionDragging = false;
        connectionSource = null;
        connectionSourceHandleId = null;
        hoveredTarget = null;
        hoveredTargetHandleId = null;
    }

    /**
     * Find a node at the given canvas coordinates that can be a connection target.
     */
    private NodeView findTargetNodeAtPoint(double x, double y) {
        for (NodeView nodeView : nodeViews.values()) {
            if (nodeView != connectionSource && nodeView.canBeConnectionTarget()) {
                // Check all input handles of the node
                for (String handleId : nodeView.getInputHandleIds()) {
                    double handleX = nodeView.getHandleX(handleId);
                    double handleY = nodeView.getHandleY(handleId);

                    // Generous hit area around handle (25 pixel radius)
                    double distance = Math.sqrt(Math.pow(x - handleX, 2) + Math.pow(y - handleY, 2));
                    if (distance <= 25) {
                        return nodeView;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Set the currently hovered target node during connection drag.
     * Legacy method for backward compatibility.
     */
    public void setHoveredTarget(NodeView target) {
        this.hoveredTarget = target;
        this.hoveredTargetHandleId = target != null && !target.getInputHandleIds().isEmpty()
                ? target.getInputHandleIds().get(0)
                : null;
    }

    /**
     * Set the currently hovered target node and handle during connection drag.
     * 
     * @param target   the target node (or null)
     * @param handleId the handle ID (or null)
     */
    public void setHoveredTarget(NodeView target, String handleId) {
        this.hoveredTarget = target;
        this.hoveredTargetHandleId = handleId;
    }

    /**
     * Complete a connection to the target node.
     * Legacy method for backward compatibility.
     */
    public void completeConnection(NodeView target) {
        String targetHandleId = target.getInputHandleIds().isEmpty() ? "main" : target.getInputHandleIds().get(0);
        completeConnection(target, targetHandleId);
    }

    /**
     * Complete a connection to a specific input handle on the target node.
     * 
     * @param target   the target node
     * @param handleId the input handle ID
     */
    public void completeConnection(NodeView target, String handleId) {
        if (!isConnectionDragging || connectionSource == null) {
            return;
        }

        // Validate connection
        if (!connectionSource.canConnectTo(target)) {
            showConnectionError("Cannot connect these nodes");
            endConnectionDrag();
            return;
        }

        String sourceOutput = connectionSourceHandleId != null ? connectionSourceHandleId : "main";
        String targetInput = handleId != null ? handleId : "main";

        // Create the connection via undo/redo
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.AddConnectionCommand(
                        this, connectionSource.getNode().id(), sourceOutput, target.getNode().id(), targetInput));

        // Clean up
        endConnectionDrag();
    }

    /**
     * Check if a connection already exists between two nodes.
     */
    public boolean hasConnection(String sourceId, String targetId) {
        return workflow.connections().stream()
                .anyMatch(c -> c.sourceNodeId().equals(sourceId) &&
                        c.targetNodeId().equals(targetId));
    }

    /**
     * Highlight all valid connection targets.
     */
    private void highlightValidTargets(NodeView source, boolean highlight) {
        for (NodeView nodeView : nodeViews.values()) {
            if (highlight) {
                // Only highlight valid targets
                if (nodeView != source && source.canConnectTo(nodeView)) {
                    nodeView.getStyleClass().add("valid-target");
                    // Make input handle larger and more visible
                    for (String handleId : nodeView.getInputHandleIds()) {
                        nodeView.highlightAsTarget(handleId, true);
                    }
                }
            } else {
                // Always clear highlighting from ALL nodes
                nodeView.getStyleClass().remove("valid-target");
                for (String handleId : nodeView.getInputHandleIds()) {
                    nodeView.highlightAsTarget(handleId, false);
                }
            }
        }
    }

    /**
     * Show a connection error message.
     */
    private void showConnectionError(String message) {
        // Create a brief tooltip-style message near mouse
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Connection Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Delete a connection by its ID.
     */
    public void deleteConnection(String connectionId) {
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.DeleteConnectionCommand(this, connectionId));
    }

    // ==================== Node Action Methods ====================

    /**
     * Open the node editor - shows the Properties Panel for consistent editing.
     */
    public void openNodeEditor(NodeView nodeView) {
        // Simply show the properties panel for this node (consistent with single-click)
        selectNode(nodeView, false);
        propertiesPanel.show(nodeView);
    }

    /**
     * Show the advanced JSON editor dialog for raw parameter editing.
     * Accessible from the Properties Panel for power users.
     */
    public void showAdvancedEditor(NodeView nodeView) {
        Node node = nodeView.getNode();

        // Create and show editor dialog
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Advanced Editor: " + node.name());
        dialog.setHeaderText("Raw JSON Parameters - " + node.type());

        // Create content
        TextField nameField = new TextField(node.name());
        TextArea paramsArea = new TextArea();
        VBox content = createAdvancedEditorContent(node, nameField, paramsArea);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Map<String, Object> params = parseJsonParameters(paramsArea.getText());
                return Map.of("name", nameField.getText(), "parameters", params);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> applyAdvancedEditorResult(nodeView, node, result));
    }

    /**
     * Create the content VBox for the advanced editor dialog.
     */
    private VBox createAdvancedEditorContent(Node node, TextField nameField, TextArea paramsArea) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        HBox headerBox = createAdvancedEditorHeader(node, nameField);
        configureParamsArea(node, paramsArea);

        // Short hint instead of full help text - full help available via help button
        Label hintLabel = new Label("Click the help button (?) above for detailed documentation and examples.");
        hintLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11;");
        hintLabel.setWrapText(true);

        Label warningLabel = new Label("Changes here will override values set in the Properties Panel.");
        warningLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11;");

        Label paramsLabel = new Label("Parameters (JSON format):");
        content.getChildren().addAll(headerBox, paramsLabel, paramsArea, hintLabel, warningLabel);

        return content;
    }

    /**
     * Create the header box with name field and help button.
     */
    private HBox createAdvancedEditorHeader(Node node, TextField nameField) {
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label("Name:");
        nameField.setPrefWidth(320);

        Button helpButton = createHelpButton(node.type());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        headerBox.getChildren().addAll(nameLabel, nameField, spacer, helpButton);
        return headerBox;
    }

    /**
     * Create a help button for the advanced editor.
     */
    private Button createHelpButton(String nodeType) {
        Button helpButton = new Button();
        FontIcon helpIcon = FontIcon.of(MaterialDesignH.HELP_CIRCLE_OUTLINE, 18);
        helpIcon.setIconColor(Color.web("#60a5fa"));
        helpButton.setGraphic(helpIcon);
        helpButton.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 5;
                -fx-cursor: hand;
                """);
        Tooltip helpTooltip = new Tooltip("Show help & examples for this node");
        helpTooltip.setShowDelay(javafx.util.Duration.millis(300));
        Tooltip.install(helpButton, helpTooltip);
        helpButton.setOnAction(e -> showNodeHelpDialog(nodeType));
        return helpButton;
    }

    /**
     * Configure the parameters text area with existing values.
     */
    private void configureParamsArea(Node node, TextArea paramsArea) {
        paramsArea.setPromptText("{\n  \"key\": \"value\"\n}");
        paramsArea.setPrefRowCount(10);
        paramsArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        paramsArea.setText(formatParametersAsJson(node.parameters()));
    }

    /**
     * Format node parameters as a JSON string.
     */
    private String formatParametersAsJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }

        try {
            StringBuilder sb = new StringBuilder("{\n");
            var entries = parameters.entrySet().iterator();
            while (entries.hasNext()) {
                var entry = entries.next();
                sb.append("  \"").append(entry.getKey()).append("\": ");
                Object v = entry.getValue();
                if (v instanceof String) {
                    sb.append("\"").append(v.toString().replace("\"", "\\\"")).append("\"");
                } else {
                    sb.append(v);
                }
                if (entries.hasNext()) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception _) {
            return "{}";
        }
    }

    /**
     * Apply the result from the advanced editor dialog.
     */
    private void applyAdvancedEditorResult(NodeView nodeView, Node node, Map<String, Object> result) {
        String newName = (String) result.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> newParams = (Map<String, Object>) result.get("parameters");

        Node updatedNode = new Node(
                node.id(),
                node.type(),
                newName != null ? newName : node.name(),
                node.position(),
                newParams != null ? newParams : node.parameters(),
                node.credentialId(),
                node.disabled(),
                node.notes());

        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.EditNodeCommand(this, node, updatedNode, "Edit " + node.name()));

        if (propertiesPanel.isShowing()) {
            propertiesPanel.show(nodeViews.get(node.id()));
        }
    }

    /**
     * Parse JSON-like parameters text into a Map.
     */
    private Map<String, Object> parseJsonParameters(String text) {
        Map<String, Object> params = new HashMap<>();
        if (text == null || text.isBlank()) {
            return params;
        }

        try {
            String content = stripJsonBraces(text);
            for (String line : content.split("\n")) {
                parseJsonLine(line, params);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse JSON parameters: {0}", e.getMessage());
        }

        return params;
    }

    /**
     * Strip leading/trailing braces from JSON content.
     */
    private String stripJsonBraces(String text) {
        String content = text.trim();
        if (content.startsWith("{")) {
            content = content.substring(1);
        }
        if (content.endsWith("}")) {
            content = content.substring(0, content.length() - 1);
        }
        return content;
    }

    /**
     * Parse a single JSON line and add to params map.
     */
    private void parseJsonLine(String rawLine, Map<String, Object> params) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.equals(",")) {
            return;
        }
        if (line.endsWith(",")) {
            line = line.substring(0, line.length() - 1);
        }

        int colonIdx = line.indexOf(':');
        if (colonIdx <= 0) {
            return;
        }

        String key = extractJsonKey(line.substring(0, colonIdx).trim());
        String value = line.substring(colonIdx + 1).trim();
        params.put(key, parseJsonValue(value));
    }

    /**
     * Extract key from JSON, removing surrounding quotes.
     */
    private String extractJsonKey(String key) {
        if (key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    /**
     * Parse a JSON value string into the appropriate Java type.
     */
    private Object parseJsonValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"");
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        if ("null".equals(value)) {
            return null;
        }
        return parseJsonNumber(value);
    }

    /**
     * Parse a JSON number value, returning the original string if not a valid
     * number.
     */
    private Object parseJsonNumber(String value) {
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            return value;
        }
    }

    /**
     * Get help content from a plugin if available.
     * Returns null if the node type is not a plugin or plugin doesn't provide help.
     */
    private NodeHelpProvider.NodeHelp getPluginHelp(String nodeType) {
        for (var pluginInfo : pluginService.getAllDiscoveredPlugins()) {
            if (pluginInfo.id().equals(nodeType) && pluginInfo.enabled()) {
                String helpText = pluginInfo.helpText();
                if (helpText != null && !helpText.isBlank()) {
                    return new NodeHelpProvider.NodeHelp(
                            pluginInfo.name(),
                            pluginInfo.description(),
                            helpText,
                            "// See documentation above for usage examples");
                }
            }
        }
        return null;
    }

    /**
     * Show detailed help dialog for a node type.
     */
    private void showNodeHelpDialog(String nodeType) {
        // First check if plugin provides help text
        NodeHelpProvider.NodeHelp help = getPluginHelp(nodeType);
        if (help == null) {
            // Fall back to built-in help
            help = NodeHelpProvider.getHelp(nodeType);
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Help: " + help.title());
        dialog.setHeaderText(help.shortDescription());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Create content
        VBox content = new VBox(15);
        content.setPrefWidth(600);
        content.setPadding(new Insets(10));

        // Description section - render as Markdown
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox descContent = ai.nervemind.ui.util.MarkdownRenderer.render(help.detailedDescription());

        // Sample code section
        Label codeTitle = new Label("Sample Usage / Code");
        codeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea codeArea = new TextArea(help.sampleCode());
        codeArea.setWrapText(true);
        codeArea.setEditable(false);
        codeArea.setPrefRowCount(8);
        codeArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        content.getChildren().addAll(descTitle, descContent, codeTitle, codeArea);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(700, 600);

        dialog.showAndWait();
    }

    /**
     * Execute a single node for testing.
     */
    public void executeNode(NodeView nodeView) {
        Node node = nodeView.getNode();

        // Create temporary workflow with single node for execution
        WorkflowDTO tempWorkflow = WorkflowDTO.create("Single Node Test - " + node.name())
                .withAddedNode(node);

        try {
            // Save the temporary workflow first
            WorkflowDTO savedTempWorkflow = workflowService.create(tempWorkflow);

            // Execute asynchronously
            executionService.executeAsync(savedTempWorkflow.id(), Map.of())
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result.status() == ExecutionStatus.SUCCESS) {
                            showExecutionResult(result);
                        } else {
                            showExecutionError(result);
                        }
                        // Clean up: delete the temporary workflow
                        cleanupTemporaryWorkflow(savedTempWorkflow.id());
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showNodeExecutionError(ex));
                        // Clean up: delete the temporary workflow
                        cleanupTemporaryWorkflow(savedTempWorkflow.id());
                        return null;
                    });
        } catch (Exception e) {
            showNodeStartError(e);
        }
    }

    private void cleanupTemporaryWorkflow(Long workflowId) {
        try {
            workflowService.delete(workflowId);
        } catch (Exception _) {
            // Ignore cleanup errors
        }
    }

    private void showNodeExecutionError(Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(EXECUTION_ERROR_TITLE);
        alert.setHeaderText("Failed to execute node");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void showNodeStartError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(EXECUTION_ERROR_TITLE);
        alert.setHeaderText("Failed to start node execution");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    /**
     * Duplicate a node.
     */
    public void duplicateNode(NodeView nodeView) {
        Node original = nodeView.getNode();

        // Create new node with offset position
        String newId = UUID.randomUUID().toString();
        Node duplicate = new Node(
                newId,
                original.type(),
                original.name() + " (copy)",
                new Node.Position(original.position().x() + 50, original.position().y() + 50),
                original.parameters() != null ? new java.util.HashMap<>(original.parameters()) : Map.of(),
                original.credentialId(),
                original.disabled(),
                original.notes());

        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.AddNodeCommand(this, duplicate));

        // Select the new node
        NodeView newView = nodeViews.get(newId);
        if (newView != null) {
            selectNode(newView);
        }
    }

    /**
     * Copy selected nodes to clipboard.
     */
    public void copySelected() {
        clipboardNodes.clear();
        for (NodeView nodeView : selectedNodes) {
            clipboardNodes.add(nodeView.getNode());
        }
        updatePasteMenuState();
    }

    /**
     * Paste nodes from clipboard.
     */
    public void pasteNodes() {
        if (clipboardNodes.isEmpty()) {
            return;
        }

        // Calculate paste position (slightly offset from original positions)
        double baseX = 100;
        double baseY = 100;

        // Clear selection before pasting
        deselectAll();

        java.util.List<Node> pastedNodeList = new java.util.ArrayList<>();
        for (Node node : clipboardNodes) {
            String newId = UUID.randomUUID().toString();
            Node pastedNode = new Node(
                    newId,
                    node.type(),
                    node.name() + " (copy)",
                    new Node.Position(baseX, baseY),
                    node.parameters() != null ? new java.util.HashMap<>(node.parameters()) : Map.of(),
                    node.credentialId(),
                    node.disabled(),
                    node.notes());
            pastedNodeList.add(pastedNode);

            // Offset next node position
            baseX += 50;
            baseY += 50;
        }

        // Execute via composite command for single undo/redo
        undoRedoManager.executeCommand(
                ai.nervemind.ui.canvas.commands.CompositeCommand.pasteMultiple(this, pastedNodeList));

        // Select all pasted nodes
        for (Node pastedNode : pastedNodeList) {
            NodeView newView = nodeViews.get(pastedNode.id());
            if (newView != null) {
                selectedNodes.add(newView);
                newView.setSelected(true);
            }
        }

        // Update properties panel for multi-selection
        if (selectedNodes.size() == 1) {
            propertiesPanel.show(selectedNodes.iterator().next());
        } else {
            propertiesPanel.hide();
        }

        updateMinimap();
    }

    /**
     * Update paste menu item state based on clipboard contents.
     */
    private void updatePasteMenuState() {
        // This will be called when the context menu is shown
    }

    /**
     * Toggle node enabled/disabled state.
     */
    public void toggleNodeEnabled(NodeView nodeView) {
        Node node = nodeView.getNode();
        boolean newState = !node.disabled();

        Node updatedNode = new Node(node.id(), node.type(), node.name(), node.position(),
                node.parameters(), node.credentialId(), newState, node.notes());

        String desc = newState ? "Disable " + node.name() : "Enable " + node.name();
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.EditNodeCommand(this, node, updatedNode, desc));
    }

    /**
     * Rename a node.
     */
    public void renameNode(NodeView nodeView) {
        Node node = nodeView.getNode();

        TextInputDialog dialog = new TextInputDialog(node.name());
        dialog.setTitle("Rename Node");
        dialog.setHeaderText("Enter new name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isBlank() && !newName.equals(node.name())) {
                Node updatedNode = new Node(node.id(), node.type(), newName, node.position(),
                        node.parameters(), node.credentialId(), node.disabled(), node.notes());
                undoRedoManager.executeCommand(
                        new ai.nervemind.ui.canvas.commands.EditNodeCommand(
                                this, node, updatedNode, "Rename " + node.name()));
            }
        });
    }

    /**
     * Change a node's icon via the icon picker dialog.
     */
    public void changeNodeIcon(NodeView nodeView) {
        Node node = nodeView.getNode();
        String currentIcon = nodeView.getCustomIconCode();

        DialogFactory.showIconPicker(getOwnerWindow(), currentIcon).ifPresent(newIconCode -> {
            // Update if changed (null means reset to default)
            if ((newIconCode == null && currentIcon != null)
                    || (newIconCode != null && !newIconCode.equals(currentIcon))) {
                // Build updated node with new icon parameter
                Map<String, Object> newParams = new HashMap<>(
                        node.parameters() != null ? node.parameters() : Map.of());
                if (newIconCode != null) {
                    newParams.put("customIcon", newIconCode);
                } else {
                    newParams.remove("customIcon");
                }
                Node updatedNode = new Node(node.id(), node.type(), node.name(), node.position(),
                        newParams, node.credentialId(), node.disabled(), node.notes());
                undoRedoManager.executeCommand(
                        new ai.nervemind.ui.canvas.commands.EditNodeCommand(
                                this, node, updatedNode, "Change icon of " + node.name()));
            }
        });
    }

    /**
     * Get the owner window for dialogs.
     */
    private javafx.stage.Window getOwnerWindow() {
        if (getScene() != null) {
            return getScene().getWindow();
        }
        return null;
    }

    /**
     * Update a node's custom icon in the workflow.
     *
     * @param nodeId   The node ID
     * @param iconCode The icon code, or null to reset to default
     */

    /**
     * Show the debug view dialog for a node, displaying last execution data.
     */
    public void showNodeDebugView(NodeView nodeView) {
        Node node = nodeView.getNode();
        NodeExecutionState executionState = ExecutionConsoleService.getInstance()
                .getNodeExecutionState(node.id());

        if (executionState == null || executionState.state() == NodeState.IDLE) {
            // No execution data available
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Debug View");
            alert.setHeaderText("No Execution Data");
            alert.setContentText("""
                    This node has not been executed yet.

                    Run the workflow to see input/output data for this node.""");
            alert.getDialogPane().setStyle("-fx-background-color: #1a1a1a;");
            alert.showAndWait();
            return;
        }

        // Show debug dialog
        javafx.stage.Window owner = getScene() != null ? getScene().getWindow() : null;
        DialogFactory.showNodeDebug(owner, node, executionState);
    }

    /**
     * Delete a specific node.
     */
    public void deleteNode(NodeView nodeView) {
        String nodeId = nodeView.getNode().id();

        // Capture the node and its connections for undo
        Node nodeToDelete = nodeView.getNode();
        java.util.List<ai.nervemind.common.domain.Connection> connectionsToDelete = workflow.connections().stream()
                .filter(c -> c.involvesNode(nodeId))
                .toList();

        selectedNodes.remove(nodeView);

        undoRedoManager.executeCommand(
                ai.nervemind.ui.canvas.commands.CompositeCommand.deleteMultiple(
                        this, java.util.List.of(nodeToDelete), connectionsToDelete));
    }

    /**
     * Select all nodes.
     */
    public void selectAll() {
        deselectAll();
        for (NodeView nodeView : nodeViews.values()) {
            selectedNodes.add(nodeView);
            nodeView.setSelected(true);
        }
        // Hide properties panel for multi-selection
        propertiesPanel.hide();
    }

    // ===== Developer Mode Features =====

    /**
     * Check if developer mode is enabled.
     *
     * @return true if dev mode is enabled in settings
     */
    public boolean isDevModeEnabled() {
        // Check via settings service - this could be enhanced to cache the value
        // For now, we read from a static holder or return false if not available
        return DevModeHolder.isEnabled();
    }

    /**
     * Copy a node's configuration as JSON to clipboard.
     *
     * @param nodeView the node to export
     */
    public void copyNodeAsJson(NodeView nodeView) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(nodeView.getNode());

            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(json);
            clipboard.setContent(content);

            LOGGER.log(Level.INFO, "Node JSON copied to clipboard");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to copy node as JSON", e);
        }
    }

    /**
     * Holder for dev mode state (set by the application on startup).
     * This provides a simple way to check dev mode without dependency injection.
     */
    public static class DevModeHolder {
        private static volatile boolean enabled = false;

        private DevModeHolder() {
            // Private constructor to prevent instantiation
        }

        public static void setEnabled(boolean value) {
            enabled = value;
        }

        public static boolean isEnabled() {
            return enabled;
        }
    }

    // ===== Debug Toolbar =====

    /**
     * Sets up the floating control bar with callbacks for workflow execution.
     * This method should be called by the controller after injecting the
     * DevModeService.
     *
     * @param devModeService the developer mode service for step execution control
     */
    public void setupDebugToolbar(DevModeServiceInterface devModeService) {
        if (devModeService == null) {
            LOGGER.warning("DevModeService is null, control bar will have limited functionality");
        }

        registerStepExecutionListener(devModeService);
        setupDebugToolbarCallbacks(devModeService);

        LOGGER.info("Floating control bar setup complete");
    }

    /**
     * Register as a step execution listener to receive pause/resume notifications.
     */
    private void registerStepExecutionListener(DevModeServiceInterface devModeService) {
        if (devModeService == null) {
            return;
        }

        devModeService.addStepExecutionListener(new DevModeServiceInterface.StepExecutionListener() {
            @Override
            public void onExecutionPaused(String nodeId, String nodeName, int nodeIndex, int totalNodes) {
                showDebugToolbarPaused(nodeId, nodeName, nodeIndex, totalNodes);
            }

            @Override
            public void onExecutionResumed() {
                Platform.runLater(() -> {
                    debugToolbar.hide();
                    clearPausedNodeHighlight();
                });
            }
        });

        debugToolbar.setStepModeEnabled(devModeService.isStepExecutionEnabled());
    }

    /**
     * Setup all debug toolbar button callbacks.
     */
    private void setupDebugToolbarCallbacks(DevModeServiceInterface devModeService) {
        debugToolbar.setOnPlay(() -> handlePlayButton(devModeService));
        debugToolbar.setOnStepForward(() -> handleStepForwardButton(devModeService));
        debugToolbar.setOnStepBack(this::handleStepBackButton);
        debugToolbar.setOnStop(() -> handleStopButton(devModeService));
        debugToolbar.setOnRestart(() -> handleRestartButton(devModeService));
        debugToolbar.setOnDebugInspect(() -> handleDebugInspectButton(devModeService));
    }

    /**
     * Handle play button click - run workflow normally.
     */
    private void handlePlayButton(DevModeServiceInterface devModeService) {
        LOGGER.info("Control bar: Run workflow normally");
        if (devModeService != null) {
            devModeService.setStepExecutionEnabled(false);
        }
        debugToolbar.showRunning();
        runWorkflow();
    }

    /**
     * Handle step forward button - start or continue step execution.
     */
    private void handleStepForwardButton(DevModeServiceInterface devModeService) {
        if (!debugToolbar.runningProperty().get()) {
            LOGGER.info("Control bar: Start step-by-step execution");
            if (devModeService != null && devModeService.isDevModeEnabled()) {
                devModeService.setStepExecutionEnabled(true);
                debugToolbar.showRunning();
                runWorkflow();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Developer Mode Required");
                alert.setHeaderText("Step execution is not available");
                alert.setContentText("Developer mode must be enabled in settings to use step-by-step execution.");
                alert.showAndWait();
            }
        } else if (debugToolbar.pausedProperty().get()) {
            LOGGER.info("Control bar: Step forward to next node");
            if (devModeService != null && devModeService.isDevModeEnabled()) {
                devModeService.continueStep();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Developer Mode Required");
                alert.setHeaderText("Step execution is not available");
                alert.setContentText("Developer mode must be enabled in settings to use step-by-step execution.");
                alert.showAndWait();
            }
        }
    }

    /**
     * Handle step back button - navigate to previous node in history.
     */
    private void handleStepBackButton() {
        LOGGER.info("Control bar: Step back requested");
        ExecutionConsoleService consoleService = ExecutionConsoleService.getInstance();

        if (!consoleService.canStepBack()) {
            showStatus("Already at the first executed node");
            return;
        }

        String previousNodeId = consoleService.stepBackInHistory();
        if (previousNodeId == null) {
            return;
        }

        clearPausedNodeHighlight();
        highlightPausedNode(previousNodeId);
        updateDebugToolbarForStepBack(previousNodeId, consoleService);
    }

    /**
     * Update debug toolbar after stepping back to a previous node.
     */
    private void updateDebugToolbarForStepBack(String nodeId, ExecutionConsoleService consoleService) {
        NodeView nodeView = nodeViews.get(nodeId);
        if (nodeView == null) {
            return;
        }

        String nodeName = nodeView.getNode().name();
        int currentIndex = consoleService.getCurrentHistoryIndex() + 1;
        int totalNodes = consoleService.getExecutionHistory().size();
        debugToolbar.showPaused(nodeName, nodeId, currentIndex, totalNodes);
        showNodeDebugView(nodeView);
        showStatus("Stepped back to: " + nodeName);
    }

    /**
     * Handle stop button - cancel execution.
     */
    private void handleStopButton(DevModeServiceInterface devModeService) {
        LOGGER.info("Control bar: Stop execution");
        if (devModeService != null) {
            devModeService.cancelStepExecution();
        }
        debugToolbar.hide();
        clearPausedNodeHighlight();
    }

    /**
     * Handle restart button - restart workflow.
     */
    private void handleRestartButton(DevModeServiceInterface devModeService) {
        LOGGER.info("Control bar: Restart workflow");
        if (devModeService != null) {
            devModeService.cancelStepExecution();
        }
        debugToolbar.hide();
        clearPausedNodeHighlight();
        Platform.runLater(() -> {
            debugToolbar.showRunning();
            runWorkflow();
        });
    }

    /**
     * Handle debug inspect button - show debug panel for current node.
     */
    private void handleDebugInspectButton(DevModeServiceInterface devModeService) {
        if (devModeService != null) {
            String pausedNodeId = devModeService.getPausedNodeId();
            if (pausedNodeId != null) {
                NodeView nodeView = nodeViews.get(pausedNodeId);
                if (nodeView != null) {
                    showNodeDebugView(nodeView);
                    return;
                }
            }
        }
        showStatus("Debug Inspector: Run workflow and pause at a node to inspect");
    }

    /**
     * Shows the debug toolbar when execution is paused at a node.
     *
     * @param nodeId     the ID of the paused node
     * @param nodeName   the name of the paused node
     * @param nodeIndex  the current node index (1-based)
     * @param totalNodes the total number of nodes in execution
     */
    public void showDebugToolbarPaused(String nodeId, String nodeName, int nodeIndex, int totalNodes) {
        Platform.runLater(() -> {
            debugToolbar.showPaused(nodeName, nodeId, nodeIndex, totalNodes);
            highlightPausedNode(nodeId);
        });
    }

    /**
     * Hides the debug toolbar.
     */
    public void hideDebugToolbar() {
        Platform.runLater(() -> {
            debugToolbar.hide();
            clearPausedNodeHighlight();
        });
    }

    /**
     * Gets the debug toolbar for external configuration.
     *
     * @return the debug toolbar instance
     */
    public DebugToolbar getDebugToolbar() {
        return debugToolbar;
    }

    /**
     * Highlights the currently paused node with a special style.
     *
     * @param nodeId the ID of the node to highlight
     */
    private void highlightPausedNode(String nodeId) {
        // Clear any existing paused highlight
        clearPausedNodeHighlight();

        // Add highlight to the paused node
        NodeView nodeView = nodeViews.get(nodeId);
        if (nodeView != null) {
            nodeView.getStyleClass().add("node-paused");
        }
    }

    /**
     * Clears the paused node highlight from all nodes.
     */
    private void clearPausedNodeHighlight() {
        for (NodeView view : nodeViews.values()) {
            view.getStyleClass().remove("node-paused");
        }
    }

    // ===== Auto-Layout =====

    /** Auto-layout engine instance. */
    private final ai.nervemind.ui.canvas.layout.AutoLayoutEngine autoLayoutEngine = new ai.nervemind.ui.canvas.layout.AutoLayoutEngine();

    /**
     * Auto-layout nodes in a nice left-to-right arrangement.
     * 
     * <p>
     * Delegates to {@link ai.nervemind.ui.canvas.layout.AutoLayoutEngine} for
     * computing positions, then applies them with grid snapping and undo support.
     * </p>
     */
    public void autoLayoutNodes() {
        if (workflow.nodes().isEmpty()) {
            return;
        }

        // Capture old positions for undo
        java.util.Map<String, double[]> oldPositions = captureNodePositions();

        // Compute new layout using the engine
        java.util.Map<String, double[]> computedPositions = autoLayoutEngine.computeLayout(workflow);

        // Apply positions with grid snapping
        for (java.util.Map.Entry<String, double[]> entry : computedPositions.entrySet()) {
            String nodeId = entry.getKey();
            double[] pos = entry.getValue();
            NodeView view = nodeViews.get(nodeId);
            if (view != null) {
                double snappedX = snapToGrid(pos[0]);
                double snappedY = snapToGrid(pos[1]);
                view.setLayoutX(snappedX);
                view.setLayoutY(snappedY);
                updateNodePosition(nodeId, snappedX, snappedY);
            }
        }

        // Capture new positions for redo
        java.util.Map<String, double[]> newPositions = captureNodePositions();

        // Register command for undo/redo
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.AutoLayoutCommand(this, oldPositions, newPositions));

        // Update all connection lines
        for (String nodeId : nodeViews.keySet()) {
            updateConnectionsForNode(nodeId);
        }

        // Update canvas bounds and minimap after layout
        updateCanvasBounds();
    }

    /**
     * Capture current positions of all nodes for undo/redo support.
     */
    private java.util.Map<String, double[]> captureNodePositions() {
        java.util.Map<String, double[]> positions = new java.util.HashMap<>();
        for (Node node : workflow.nodes()) {
            NodeView view = nodeViews.get(node.id());
            if (view != null) {
                positions.put(node.id(), new double[] { view.getLayoutX(), view.getLayoutY() });
            }
        }
        return positions;
    }

    /**
     * Fit all nodes into view.
     */
    public void fitToView() {
        if (nodeViews.isEmpty())
            return;

        // Calculate bounds of all nodes
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (NodeView view : nodeViews.values()) {
            minX = Math.min(minX, view.getLayoutX());
            minY = Math.min(minY, view.getLayoutY());
            maxX = Math.max(maxX, view.getLayoutX() + view.getWidth());
            maxY = Math.max(maxY, view.getLayoutY() + view.getHeight());
        }

        // Calculate content dimensions
        double contentWidth = maxX - minX;
        double contentHeight = maxY - minY;

        // Ensure minimum content size to avoid division by zero
        if (contentWidth < 1)
            contentWidth = 1;
        if (contentHeight < 1)
            contentHeight = 1;

        // Get actual viewport dimensions (the visible area in the scroll pane)
        double padding = 50;
        double viewWidth = canvasScrollPane.getViewportBounds().getWidth() - (padding * 2);
        double viewHeight = canvasScrollPane.getViewportBounds().getHeight() - (padding * 2);

        // Ensure minimum viewport size
        if (viewWidth < 100)
            viewWidth = 100;
        if (viewHeight < 100)
            viewHeight = 100;

        // Calculate scale to fit content in viewport
        double scaleX = viewWidth / contentWidth;
        double scaleY = viewHeight / contentHeight;
        double newScale = Math.min(scaleX, scaleY);

        // Clamp scale (allow zooming in up to 1.0 for small content)
        newScale = Math.min(newScale, 1.0);
        scale = Math.clamp(newScale, MIN_ZOOM, MAX_ZOOM);

        // Calculate translation to center content in viewport
        // Content center in original coordinates
        double contentCenterX = minX + contentWidth / 2;
        double contentCenterY = minY + contentHeight / 2;

        // Viewport center
        double viewportCenterX = canvasScrollPane.getViewportBounds().getWidth() / 2;
        double viewportCenterY = canvasScrollPane.getViewportBounds().getHeight() / 2;

        // Translation: move scaled content center to viewport center
        translateX = viewportCenterX - (contentCenterX * scale);
        translateY = viewportCenterY - (contentCenterY * scale);

        updateCanvasTransform();
    }

    /**
     * Zoom in by one zoom factor step.
     */
    public void zoomIn() {
        double newScale = Math.clamp(scale + ZOOM_FACTOR, MIN_ZOOM, MAX_ZOOM);
        if (newScale != scale) {
            scale = newScale;
            updateCanvasTransform();
        }
    }

    /**
     * Zoom out by one zoom factor step.
     */
    public void zoomOut() {
        double newScale = Math.clamp(scale - ZOOM_FACTOR, MIN_ZOOM, MAX_ZOOM);
        if (newScale != scale) {
            scale = newScale;
            updateCanvasTransform();
        }
    }

    /**
     * Reset zoom to 100% (normal scale).
     */
    public void resetZoom() {
        if (scale != 1.0) {
            scale = 1.0;
            updateCanvasTransform();
        }
    }

    /**
     * Get the current zoom level as a percentage.
     */
    public int getZoomPercentage() {
        return (int) Math.round(scale * 100);
    }

    /**
     * Set whether the grid should be shown.
     */
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        drawGrid();
    }

    /**
     * Get whether the grid is currently shown.
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * Set whether nodes should snap to grid.
     */
    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    /**
     * Get whether nodes currently snap to grid.
     */
    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    /**
     * Snap a position to grid if snap-to-grid is enabled.
     */
    public double snapPositionToGrid(double value) {
        return snapToGrid(value);
    }

    public WorkflowDTO getWorkflow() {
        return workflow;
    }

    /**
     * Gets the ID of the current workflow, or null if not saved.
     * 
     * @return the workflow ID, or null
     */
    public Long getWorkflowId() {
        return workflow != null ? workflow.id() : null;
    }

    /**
     * Sets the plugin menu manager for connection context menus.
     * 
     * @param pluginMenuManager the plugin menu manager
     */
    public void setPluginMenuManager(PluginMenuManager pluginMenuManager) {
        this.pluginMenuManager = pluginMenuManager;

        // Update existing connection lines
        for (ConnectionLine line : connectionLines.values()) {
            line.setPluginMenuManager(pluginMenuManager);
        }
    }

    /**
     * Gets a connection line by its ID.
     * 
     * @param connectionId the connection ID
     * @return the ConnectionLine, or null if not found
     */
    public ConnectionLine getConnectionLine(String connectionId) {
        return connectionLines.get(connectionId);
    }

    /**
     * Updates the workflow settings.
     * This modifies the workflow in place.
     * 
     * @param newSettings the new settings map
     */
    public void updateWorkflowSettings(Map<String, Object> newSettings) {
        if (workflow == null) {
            return;
        }
        workflow = new ai.nervemind.common.dto.WorkflowDTO(
                workflow.id(),
                workflow.name(),
                workflow.description(),
                workflow.nodes(),
                workflow.connections(),
                newSettings,
                workflow.isActive(),
                workflow.triggerType(),
                workflow.cronExpression(),
                workflow.createdAt(),
                workflow.updatedAt(),
                workflow.lastExecuted(),
                workflow.version());
        // Note: The workflow will be saved when the user saves it
    }

    /**
     * Get the execution service.
     */
    public ExecutionServiceInterface getExecutionService() {
        return executionService;
    }

    /**
     * Update a node with new properties.
     */
    public void updateNode(NodeView nodeView, Node updatedNode) {
        String nodeId = updatedNode.id();

        // Update workflow nodes list
        var newNodes = workflow.nodes().stream()
                .map(n -> n.id().equals(nodeId) ? updatedNode : n)
                .toList();
        workflow = workflow.withNodes(newNodes);

        // Rebuild the node view
        nodeLayer.getChildren().remove(nodeView);
        nodeViews.remove(nodeId);

        NodeView newView = new NodeView(updatedNode, this);
        nodeViews.put(nodeId, newView);
        nodeLayer.getChildren().add(newView);

        // Re-select the new view
        selectNode(newView);

        // Update connections for this node
        updateConnectionsForNode(nodeId);
    }

    /**
     * Get the properties panel.
     */
    public NodePropertiesPanelComponent getPropertiesPanel() {
        return propertiesPanel;
    }

    /**
     * Get a node view by its ID.
     */
    public NodeView getNodeViewById(String nodeId) {
        return nodeViews.get(nodeId);
    }

    /**
     * Highlight and focus on specific nodes by their IDs.
     * Selects the nodes, adds a highlight effect, and pans to show them.
     *
     * @param nodeIds the list of node IDs to highlight
     */
    public void highlightNodes(java.util.List<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }

        // Clear current selection
        deselectAll();

        // Clear any existing guide highlights
        for (NodeView view : nodeViews.values()) {
            view.getStyleClass().remove("node-guide-highlight");
        }

        // Calculate bounds to focus on
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        int foundCount = 0;
        for (String nodeId : nodeIds) {
            NodeView nodeView = nodeViews.get(nodeId);
            if (nodeView != null) {
                // Add highlight style
                nodeView.getStyleClass().add("node-guide-highlight");
                // Also select it
                selectedNodes.add(nodeView);
                nodeView.setSelected(true);

                // Track bounds
                minX = Math.min(minX, nodeView.getLayoutX());
                minY = Math.min(minY, nodeView.getLayoutY());
                maxX = Math.max(maxX, nodeView.getLayoutX() + nodeView.getWidth());
                maxY = Math.max(maxY, nodeView.getLayoutY() + nodeView.getHeight());
                foundCount++;
            }
        }

        if (foundCount == 0) {
            return;
        }

        // Pan to center the highlighted nodes in view
        double contentCenterX = (minX + maxX) / 2;
        double contentCenterY = (minY + maxY) / 2;

        double viewportCenterX = canvasScrollPane.getViewportBounds().getWidth() / 2;
        double viewportCenterY = canvasScrollPane.getViewportBounds().getHeight() / 2;

        // Calculate new translation to center the highlighted nodes
        translateX = viewportCenterX - (contentCenterX * scale);
        translateY = viewportCenterY - (contentCenterY * scale);

        updateCanvasTransform();

        // Show properties panel for single selection
        if (selectedNodes.size() == 1) {
            propertiesPanel.show(selectedNodes.iterator().next());
        }
    }

    /**
     * Clear guide highlight from all nodes.
     */
    public void clearGuideHighlight() {
        for (NodeView view : nodeViews.values()) {
            view.getStyleClass().remove("node-guide-highlight");
        }
    }

    /**
     * Reset all node execution states to IDLE.
     */
    public void resetAllNodeExecutionStates() {
        for (NodeView nodeView : nodeViews.values()) {
            nodeView.resetExecutionState();
        }
    }

    /**
     * Handle node state changes from ExecutionConsoleService.
     * This updates the visual state of nodes during execution.
     */
    @Override
    public void onNodeStateChanged(String nodeId, NodeState state) {
        Platform.runLater(() -> {
            NodeView nodeView = getNodeViewById(nodeId);
            if (nodeView != null) {
                ExecutionState executionState = switch (state) {
                    case RUNNING -> ExecutionState.RUNNING;
                    case SUCCESS -> ExecutionState.SUCCESS;
                    case FAILED -> ExecutionState.ERROR;
                    case SKIPPED -> ExecutionState.SKIPPED;
                    case IDLE -> ExecutionState.IDLE;
                };
                nodeView.setExecutionState(executionState);
            }
        });
    }

    /**
     * Get the execution history panel.
     */
    public ExecutionHistoryPanelComponent getExecutionHistoryPanel() {
        return executionHistoryPanel;
    }

    /**
     * Toggle the execution history panel visibility.
     */
    public void toggleExecutionHistory() {
        executionHistoryPanel.toggle();
    }

    /**
     * Get all node views.
     */
    public java.util.Collection<NodeView> getNodeViews() {
        return nodeViews.values();
    }

    /**
     * Get the current viewport bounds.
     */
    public javafx.geometry.Bounds getViewportBounds() {
        return canvasScrollPane.getViewportBounds();
    }

    /**
     * Get current scroll X position.
     */
    public double getScrollX() {
        // Return the actual translated position instead of ScrollPane values
        // since we use transform-based panning
        return -translateX / scale;
    }

    /**
     * Get current scroll Y position.
     */
    public double getScrollY() {
        // Return the actual translated position instead of ScrollPane values
        // since we use transform-based panning
        return -translateY / scale;
    }

    /**
     * Center the canvas view on a specific position.
     */
    public void centerOn(double x, double y) {
        double viewportWidth = canvasScrollPane.getViewportBounds().getWidth();
        double viewportHeight = canvasScrollPane.getViewportBounds().getHeight();

        // Calculate the translation needed to center on (x, y)
        // We want the point (x, y) to be at the center of the viewport
        translateX = -(x * scale - viewportWidth / 2);
        translateY = -(y * scale - viewportHeight / 2);

        updateCanvasTransform();
    }

    /**
     * Get the minimap component.
     */
    public CanvasMinimap getMinimap() {
        return minimap;
    }

    /**
     * Toggle minimap visibility.
     */
    public void toggleMinimap() {
        minimap.toggle();
    }

    /**
     * Update the minimap (call after node changes).
     */
    public void updateMinimap() {
        if (minimap != null && minimap.isVisible()) {
            minimap.update();
        }
    }

    // ==================== Undo/Redo Support ====================

    /**
     * Get the undo/redo manager.
     */
    public UndoRedoManager getUndoRedoManager() {
        return undoRedoManager;
    }

    /**
     * Undo the last action.
     */
    public void undo() {
        undoRedoManager.undo();
    }

    /**
     * Redo the last undone action.
     */
    public void redo() {
        undoRedoManager.redo();
    }

    /**
     * Create a node internally (for command pattern).
     */
    public ai.nervemind.common.domain.Node createNodeInternal(String id, String type, double x, double y) {
        String name = getDefaultNameForType(type);
        ai.nervemind.common.domain.Node node = new ai.nervemind.common.domain.Node(
                id, type, name, new ai.nervemind.common.domain.Node.Position(x, y),
                java.util.Map.of(), null, false, "");

        workflow = workflow.withAddedNode(node);
        NodeView nodeView = new NodeView(node, this);
        nodeViews.put(id, nodeView);
        nodeViews.put(id, nodeView);
        nodeLayer.getChildren().add(nodeView);
        updateMinimap();
        updateCanvasBounds();
        return node;
    }

    /**
     * Delete a node internally (for command pattern).
     */
    public void deleteNodeInternal(String nodeId) {
        NodeView nodeView = nodeViews.remove(nodeId);
        if (nodeView != null) {
            nodeLayer.getChildren().remove(nodeView);
        }

        // Remove connections involving this node
        workflow.connections().stream()
                .filter(c -> c.sourceNodeId().equals(nodeId) || c.targetNodeId().equals(nodeId))
                .map(ai.nervemind.common.domain.Connection::id)
                .toList()
                .forEach(this::deleteConnectionInternal);

        workflow = workflow.withRemovedNode(nodeId);
        updateCanvasBounds();
    }

    /**
     * Restore a deleted node (for undo).
     */
    public void restoreNode(ai.nervemind.common.domain.Node node) {
        workflow = workflow.withAddedNode(node);
        NodeView nodeView = new NodeView(node, this);
        nodeViews.put(node.id(), nodeView);
        nodeLayer.getChildren().add(nodeView);
        updateMinimap();
    }

    /**
     * Get a node by ID.
     */
    public ai.nervemind.common.domain.Node getNodeById(String nodeId) {
        return workflow.nodes().stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Set node position (for move command).
     */
    public void setNodePosition(String nodeId, double x, double y) {
        NodeView nodeView = nodeViews.get(nodeId);
        if (nodeView != null) {
            double snappedX = snapToGrid(x);
            double snappedY = snapToGrid(y);
            nodeView.setLayoutX(snappedX);
            nodeView.setLayoutY(snappedY);
            updateNodePosition(nodeId, snappedX, snappedY);
        }
    }

    public void applyNodeState(Node node) {
        NodeView view = nodeViews.get(node.id());
        if (view != null) {
            updateNode(view, node);
        }
    }

    /**
     * Record a node move operation for undo/redo.
     * Called by NodeView after a drag operation completes.
     */
    public void recordNodeMove(String nodeId, double oldX, double oldY, double newX, double newY) {
        undoRedoManager.executeCommand(
                new ai.nervemind.ui.canvas.commands.MoveNodeCommand(this, nodeId, oldX, oldY, newX, newY));
    }

    /**
     * Delete a connection internally (for command pattern).
     */
    public void deleteConnectionInternal(String connectionId) {
        ConnectionLine line = connectionLines.remove(connectionId);
        if (line != null) {
            connectionLayer.getChildren().remove(line);
        }
        workflow = workflow.withRemovedConnection(connectionId);
    }

    /**
     * Get a connection by ID.
     */
    public ai.nervemind.common.domain.Connection getConnectionById(String connectionId) {
        return workflow.connections().stream()
                .filter(c -> c.id().equals(connectionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Restore a deleted connection (for undo).
     */
    public void restoreConnection(ai.nervemind.common.domain.Connection connection) {
        workflow = workflow.withAddedConnection(connection);

        NodeView source = nodeViews.get(connection.sourceNodeId());
        NodeView target = nodeViews.get(connection.targetNodeId());
        if (source != null && target != null) {
            ConnectionLine line = new ConnectionLine(connection, source, target, this);
            line.setPluginMenuManager(pluginMenuManager);
            connectionLines.put(connection.id(), line);
            connectionLayer.getChildren().add(line);

            // Notify plugins about the restored connection
            ConnectionContextImpl context = new ConnectionContextImpl(line, this);
            pluginService.notifyConnectionCreated(context);
        }
    }

    /**
     * Get default name for node type.
     */
    private String getDefaultNameForType(String type) {
        // First try to get from registry
        NodeTypeDescriptor descriptor = nodeTypeRegistry.get(type);
        if (descriptor != null) {
            return descriptor.getDisplayName();
        }

        // Fallback for legacy/unknown types
        return type;
    }
}
