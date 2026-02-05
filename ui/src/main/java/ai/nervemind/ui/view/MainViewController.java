/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.springframework.stereotype.Component;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.Workflow;
import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.DevModeServiceInterface;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.MenuLocation;
import ai.nervemind.common.service.PluginServiceInterface.PanelPosition;
import ai.nervemind.common.service.PythonStatusServiceInterface;
import ai.nervemind.common.service.SampleServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import ai.nervemind.ui.canvas.WorkflowCanvas;
import ai.nervemind.ui.console.ExecutionConsoleService;
import ai.nervemind.ui.service.DialogService;
import ai.nervemind.ui.service.PluginMenuManager;
import ai.nervemind.ui.service.PluginPanelManager;
import ai.nervemind.ui.view.dialog.DialogFactory;
import ai.nervemind.ui.viewmodel.MainViewModel;
import ai.nervemind.ui.viewmodel.MainViewModel.NavigationView;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * Main application view controller.
 * 
 * <p>
 * This is a thin controller that manages the primary UI layout and delegates
 * business logic to {@link MainViewModel}. Follows the MVVM pattern where:
 * <ul>
 * <li>The Controller handles FXML bindings and UI events</li>
 * <li>The ViewModel manages state and business logic</li>
 * <li>Services are used through the DialogService interface</li>
 * </ul>
 */
@Component
@FxmlView("/ai/nervemind/ui/controller/Main.fxml")
public class MainViewController implements Initializable {

    private static final String ACTIVE_STYLE_CLASS = "active";
    private static final String STATUS_COMPLETED_STYLE = "status-completed";
    private static final String STATUS_FAILED_STYLE = "status-failed";
    private static final String STATUS_RUNNING_STYLE = "status-running";

    // Services
    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;
    private final PluginServiceInterface pluginService;
    private final SampleServiceInterface sampleService;
    private final DialogService dialogService;
    private final DevModeServiceInterface devModeService;
    private final SettingsServiceInterface settingsService;
    private final PythonStatusServiceInterface pythonStatusService;

    // Plugin UI Managers
    private final PluginMenuManager pluginMenuManager;
    private final PluginPanelManager pluginPanelManager;

    // ViewModel
    private final MainViewModel viewModel;

    // FXML injected fields
    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox sidebarNav;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pythonStatusLabel;

    @FXML
    private Button btnWorkflows;

    @FXML
    private Button btnExecutions;

    @FXML
    private Button btnExecutionConsole;

    @FXML
    private Button btnCredentials;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnPlugins;

    @FXML
    private CheckMenuItem menuShowGrid;

    @FXML
    private CheckMenuItem menuSnapToGrid;

    // Menu bar menus (for plugin contributions)
    @FXML
    private Menu menuFile;

    @FXML
    private Menu menuEdit;

    @FXML
    private Menu menuView;

    @FXML
    private Menu menuWorkflow;

    @FXML
    private Menu menuTools;

    @FXML
    private Menu menuHelp;

    // Developer menu (visible only when dev mode enabled)
    @FXML
    private Menu menuDeveloper;

    // Toolbar Buttons
    @FXML
    private Button btnNewWorkflow;
    @FXML
    private Button btnOpenWorkflow;
    @FXML
    private Button btnSaveWorkflow;
    @FXML
    private Button btnRunWorkflow;

    // Canvas instance (reused)
    private WorkflowCanvas workflowCanvas;

    /**
     * Creates the MainViewController with required dependencies.
     *
     * @param workflowService     the workflow service
     * @param executionService    the execution service
     * @param pluginService       the plugin service
     * @param sampleService       the sample service
     * @param dialogService       the dialog service
     * @param devModeService      the developer mode service
     * @param settingsService     the settings service
     * @param pythonStatusService the python status service
     * @param pluginMenuManager   the plugin menu manager
     * @param pluginPanelManager  the plugin panel manager
     */
    public MainViewController(
            WorkflowServiceInterface workflowService,
            ExecutionServiceInterface executionService,
            PluginServiceInterface pluginService,
            SampleServiceInterface sampleService,
            DialogService dialogService,
            DevModeServiceInterface devModeService,
            SettingsServiceInterface settingsService,
            PythonStatusServiceInterface pythonStatusService,
            PluginMenuManager pluginMenuManager,
            PluginPanelManager pluginPanelManager) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.pluginService = pluginService;
        this.sampleService = sampleService;
        this.dialogService = dialogService;
        this.devModeService = devModeService;
        this.settingsService = settingsService;
        this.pythonStatusService = pythonStatusService;
        this.pluginMenuManager = pluginMenuManager;
        this.pluginPanelManager = pluginPanelManager;
        this.viewModel = new MainViewModel(workflowService, executionService);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSidebarIcons();
        setupToolbarIcons();
        setupSidebarActions();
        setupBindings();
        setupDeveloperMode();
        setupPythonStatus();
        setupPluginExtensions();

        // Initialize ViewModel and show initial view
        viewModel.initialize();
        showWorkflowsView();
    }

    /**
     * Gets the ViewModel for testing or external access.
     *
     * @return the MainViewModel
     */
    public MainViewModel getViewModel() {
        return viewModel;
    }

    // ========== Setup Methods ==========

    private void setupSidebarIcons() {
        btnWorkflows.setGraphic(new FontIcon(MaterialDesignF.FILE_TREE));
        btnExecutions.setGraphic(new FontIcon(MaterialDesignP.PLAY_CIRCLE_OUTLINE));
        btnExecutionConsole.setGraphic(new FontIcon(MaterialDesignC.CONSOLE));
        btnCredentials.setGraphic(new FontIcon(MaterialDesignC.CREDIT_CARD_OUTLINE));
        btnPlugins.setGraphic(new FontIcon(MaterialDesignP.PUZZLE));
        btnSettings.setGraphic(new FontIcon(MaterialDesignC.COG_OUTLINE));
    }

    private void setupToolbarIcons() {
        btnNewWorkflow.setGraphic(new FontIcon(MaterialDesignP.PLUS));
        btnOpenWorkflow.setGraphic(new FontIcon(MaterialDesignF.FOLDER_OPEN));
        btnSaveWorkflow.setGraphic(new FontIcon(MaterialDesignC.CONTENT_SAVE));

        // Hide run button as it is replaced by floating toolbar
        if (btnRunWorkflow != null) {
            btnRunWorkflow.setVisible(false);
            btnRunWorkflow.setManaged(false);
        }
    }

    private void setupSidebarActions() {
        btnWorkflows.setOnAction(e -> showWorkflowsView());
        btnExecutions.setOnAction(e -> showExecutionsView());
        btnExecutionConsole.setOnAction(e -> onShowExecutionConsole());
        btnCredentials.setOnAction(e -> showCredentialsView());
        btnPlugins.setOnAction(e -> showPluginManager());
        btnSettings.setOnAction(e -> showSettingsView());
    }

    private void setupBindings() {
        // Bind status label to ViewModel
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        // Bind grid settings to ViewModel
        if (menuShowGrid != null) {
            menuShowGrid.selectedProperty().bindBidirectional(viewModel.showGridProperty());
        }
        if (menuSnapToGrid != null) {
            menuSnapToGrid.selectedProperty().bindBidirectional(viewModel.snapToGridProperty());
        }
    }

    /**
     * Setup developer mode based on settings.
     * Shows/hides the Developer menu and enables dev features.
     */
    private void setupDeveloperMode() {
        boolean devModeEnabled = devModeService != null && devModeService.isDevModeEnabled();

        // Set the static holder for canvas access
        ai.nervemind.ui.canvas.WorkflowCanvas.DevModeHolder.setEnabled(devModeEnabled);

        if (menuDeveloper != null) {
            menuDeveloper.setVisible(devModeEnabled);
        }

        // Listen for settings changes to toggle dev mode at runtime
        if (settingsService != null) {
            settingsService.addChangeListener((key, oldValue, newValue) -> {
                if ("advanced.devMode".equals(key)) {
                    boolean enabled = Boolean.parseBoolean(newValue);
                    ai.nervemind.ui.canvas.WorkflowCanvas.DevModeHolder.setEnabled(enabled);
                    javafx.application.Platform.runLater(() -> {
                        if (menuDeveloper != null) {
                            menuDeveloper.setVisible(enabled);
                        }
                        // Reset step execution when dev mode is disabled
                        if (!enabled && devModeService != null) {
                            devModeService.setStepExecutionEnabled(false);
                        }
                        viewModel.updateStatus(enabled ? "Developer mode enabled" : "Developer mode disabled");
                    });
                }
            });
        }
    }

    /**
     * Setup Python status indicator in status bar.
     * Shows current Python environment and allows clicking to open settings.
     */
    private void setupPythonStatus() {
        if (pythonStatusLabel == null || pythonStatusService == null) {
            return;
        }

        // Set initial status
        updatePythonStatusLabel();

        // Make clickable - opens Python settings
        pythonStatusLabel.setStyle("-fx-cursor: hand;");
        pythonStatusLabel.setOnMouseClicked(event -> {
            showSettingsView();
            // Note: Settings view should auto-select Python category
            viewModel.updateStatus("Opening Python settings...");
        });

        // Add tooltip with detailed info
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip();
        tooltip.setText(pythonStatusService.getDetailedInfo());
        tooltip.setShowDelay(javafx.util.Duration.millis(500));
        javafx.scene.control.Tooltip.install(pythonStatusLabel, tooltip);

        // Listen for Python settings changes
        if (settingsService != null) {
            settingsService.addChangeListener((key, oldValue, newValue) -> {
                if (key != null && key.startsWith("python.")) {
                    javafx.application.Platform.runLater(() -> {
                        pythonStatusService.refreshStatus();
                        updatePythonStatusLabel();
                        // Update tooltip
                        tooltip.setText(pythonStatusService.getDetailedInfo());
                    });
                }
            });
        }
    }

    /**
     * Update the Python status label text and style.
     */
    private void updatePythonStatusLabel() {
        if (pythonStatusLabel == null || pythonStatusService == null) {
            return;
        }

        String statusText = pythonStatusService.getStatusBarText();
        pythonStatusLabel.setText(statusText);

        // Apply style based on availability
        pythonStatusLabel.getStyleClass().removeAll("python-available", "python-unavailable");
        if (pythonStatusService.isPythonAvailable()) {
            pythonStatusLabel.getStyleClass().add("python-available");
        } else {
            pythonStatusLabel.getStyleClass().add("python-unavailable");
        }
    }

    /**
     * Setup plugin UI extensions (menus and panels).
     * Registers menus with the plugin menu manager and loads contributions.
     */
    private void setupPluginExtensions() {
        // Register menus with the plugin menu manager
        if (pluginMenuManager != null) {
            if (menuFile != null) {
                pluginMenuManager.registerMenu(MenuLocation.FILE, menuFile);
            }
            if (menuEdit != null) {
                pluginMenuManager.registerMenu(MenuLocation.EDIT, menuEdit);
            }
            if (menuView != null) {
                pluginMenuManager.registerMenu(MenuLocation.VIEW, menuView);
            }
            if (menuWorkflow != null) {
                pluginMenuManager.registerMenu(MenuLocation.WORKFLOW, menuWorkflow);
            }
            if (menuTools != null) {
                pluginMenuManager.registerMenu(MenuLocation.TOOLS, menuTools);
            }
            if (menuHelp != null) {
                pluginMenuManager.registerMenu(MenuLocation.HELP, menuHelp);
            }

            // Load plugin menu contributions
            pluginMenuManager.refreshPluginMenus();
        }

        // Setup plugin panels
        if (pluginPanelManager != null) {
            // Load plugin panel contributions
            pluginPanelManager.refreshPluginPanels();

            // If there are any right panels, add the container to the layout
            VBox rightPanelContainer = pluginPanelManager.getContainer(PanelPosition.RIGHT);
            if (rightPanelContainer != null && pluginPanelManager.getPanelCount() > 0) {
                // Add right panel container to the root pane
                rootPane.setRight(rightPanelContainer);
            }
        }
    }

    /**
     * Refreshes plugin UI extensions.
     * Call this when plugins are enabled/disabled.
     */
    public void refreshPluginExtensions() {
        if (pluginMenuManager != null) {
            pluginMenuManager.refreshPluginMenus();
        }
        if (pluginPanelManager != null) {
            pluginPanelManager.refreshPluginPanels();
        }
        viewModel.updateStatus("Plugin extensions refreshed");
    }

    // ========== Navigation Views ==========

    @FXML
    private void showWorkflowsView() {
        clearActiveButton();
        btnWorkflows.getStyleClass().add(ACTIVE_STYLE_CLASS);
        viewModel.navigateTo(NavigationView.WORKFLOWS);

        // Reuse existing workflow canvas or create new one
        if (workflowCanvas == null) {
            workflowCanvas = new WorkflowCanvas(workflowService, executionService, pluginService);
            // Setup debug toolbar with DevModeService
            workflowCanvas.setupDebugToolbar(devModeService);

            // Setup plugin menu manager for connection context menus
            if (pluginMenuManager != null) {
                workflowCanvas.setPluginMenuManager(pluginMenuManager);
            }
        } else {
            workflowCanvas.refreshPalette();
        }
        contentArea.getChildren().setAll(workflowCanvas);

        // Sync grid menu states with canvas
        syncGridMenuStates();
    }

    @FXML
    private void showExecutionsView() {
        clearActiveButton();
        btnExecutions.getStyleClass().add(ACTIVE_STYLE_CLASS);
        viewModel.navigateTo(NavigationView.EXECUTIONS);

        // Load executions and create view
        viewModel.loadExecutions();
        VBox executionsView = createExecutionsView();
        contentArea.getChildren().setAll(executionsView);
    }

    @FXML
    private void showCredentialsView() {
        clearActiveButton();
        btnCredentials.getStyleClass().add(ACTIVE_STYLE_CLASS);
        viewModel.navigateTo(NavigationView.CREDENTIALS);

        // Show credential manager dialog via service
        dialogService.showCredentialManager();

        // Return to workflows view after closing
        showWorkflowsView();
    }

    @FXML
    private void showSettingsView() {
        clearActiveButton();
        btnSettings.getStyleClass().add(ACTIVE_STYLE_CLASS);
        viewModel.navigateTo(NavigationView.SETTINGS);

        // Show settings dialog via service
        dialogService.showSettingsDialog();

        // Return to workflows view after closing
        showWorkflowsView();
    }

    private void showPluginManager() {
        clearActiveButton();
        btnPlugins.getStyleClass().add(ACTIVE_STYLE_CLASS);
        viewModel.navigateTo(NavigationView.PLUGINS);

        // Show plugin manager dialog via service
        dialogService.showPluginManager();

        // Refresh plugin extensions after dialog closes (user may have enabled/disabled
        // plugins)
        refreshPluginExtensions();

        // Also refresh the workflow canvas palette if it exists
        if (workflowCanvas != null) {
            workflowCanvas.refreshPalette();
        }

        // Return to workflows view after closing
        showWorkflowsView();
    }

    // ========== Execution View Building ==========

    private VBox createExecutionsView() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("executions-view");

        // Header
        HBox header = createExecutionsHeader();

        // Executions table
        TableView<ExecutionDTO> executionsTable = createExecutionsTable();

        // Bind table items to ViewModel
        executionsTable.setItems(viewModel.getExecutions());

        // Details panel
        VBox detailsPanel = createExecutionDetailsPanel();
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);

        // Connect selection to details
        executionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setSelectedExecution(newVal);
            if (newVal != null) {
                showExecutionDetails(detailsPanel, newVal);
                detailsPanel.setVisible(true);
                detailsPanel.setManaged(true);
            } else {
                detailsPanel.setVisible(false);
                detailsPanel.setManaged(false);
            }
        });

        container.getChildren().addAll(header, executionsTable, detailsPanel);
        VBox.setVgrow(executionsTable, Priority.ALWAYS);

        return container;
    }

    private HBox createExecutionsHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 28);
        icon.setIconColor(Color.web("#4ade80"));

        VBox titleBox = new VBox(2);
        Label title = new Label("Workflow Executions");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Monitor and review workflow execution history");
        subtitle.getStyleClass().add("section-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setGraphic(FontIcon.of(MaterialDesignR.REFRESH, 14));
        refreshBtn.setOnAction(e -> viewModel.refreshExecutions());

        header.getChildren().addAll(icon, titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().add(refreshBtn);

        return header;
    }

    @SuppressWarnings("unchecked")
    private TableView<ExecutionDTO> createExecutionsTable() {
        TableView<ExecutionDTO> table = new TableView<>();
        table.getStyleClass().add("executions-table");
        table.setPlaceholder(new Label("No executions found"));

        // Workflow Name column
        TableColumn<ExecutionDTO, String> workflowCol = new TableColumn<>("Workflow");
        workflowCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().workflowName()));
        workflowCol.setPrefWidth(200);

        // Status column
        TableColumn<ExecutionDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().toString()));
        statusCol.setCellFactory(column -> createStatusCell());
        statusCol.setPrefWidth(100);

        // Started column
        TableColumn<ExecutionDTO, String> startedCol = new TableColumn<>("Started");
        startedCol.setCellValueFactory(
                data -> new SimpleStringProperty(MainViewModel.formatInstant(data.getValue().startedAt())));
        startedCol.setPrefWidth(120);

        // Duration column
        TableColumn<ExecutionDTO, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(
                data -> new SimpleStringProperty(MainViewModel.formatDurationShort(data.getValue().durationMs())));
        durationCol.setPrefWidth(100);

        // Trigger column
        TableColumn<ExecutionDTO, String> triggerCol = new TableColumn<>("Trigger");
        triggerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().triggerType().toString()));
        triggerCol.setPrefWidth(80);

        table.getColumns().addAll(workflowCol, statusCol, startedCol, durationCol, triggerCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        return table;
    }

    private TableCell<ExecutionDTO, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    getStyleClass().removeAll(STATUS_COMPLETED_STYLE, STATUS_FAILED_STYLE, STATUS_RUNNING_STYLE);
                } else {
                    setText(status);
                    getStyleClass().removeAll(STATUS_COMPLETED_STYLE, STATUS_FAILED_STYLE, STATUS_RUNNING_STYLE);
                    switch (status) {
                        case "COMPLETED" -> getStyleClass().add(STATUS_COMPLETED_STYLE);
                        case "FAILED" -> getStyleClass().add(STATUS_FAILED_STYLE);
                        case "RUNNING" -> getStyleClass().add(STATUS_RUNNING_STYLE);
                        default -> {
                            /* no special style */ }
                    }
                }
            }
        };
    }

    private VBox createExecutionDetailsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.getStyleClass().add("execution-details-panel");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon detailsIcon = FontIcon.of(MaterialDesignF.FILE_DOCUMENT, 20);
        detailsIcon.setIconColor(Color.web("#60a5fa"));

        Label detailsTitle = new Label("Execution Details");
        detailsTitle.getStyleClass().add("details-title");

        header.getChildren().addAll(detailsIcon, detailsTitle);

        // Content area with scroll
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("details-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.getStyleClass().add("details-content");
        scrollPane.setContent(content);

        panel.getChildren().addAll(header, scrollPane);
        VBox.setVgrow(panel, Priority.ALWAYS);

        return panel;
    }

    private void showExecutionDetails(VBox detailsPanel, ExecutionDTO execution) {
        ScrollPane scrollPane = (ScrollPane) detailsPanel.getChildren().get(1);
        VBox content = (VBox) scrollPane.getContent();
        content.getChildren().clear();

        // Execution summary
        VBox summaryBox = createExecutionSummary(execution);
        content.getChildren().add(summaryBox);

        // Error message if present
        if (execution.errorMessage() != null && !execution.errorMessage().isBlank()) {
            VBox errorBox = createErrorBox(execution.errorMessage());
            content.getChildren().add(errorBox);
        }

        // Node executions
        if (execution.nodeExecutions() != null && !execution.nodeExecutions().isEmpty()) {
            Label nodesTitle = new Label("Node Execution Details");
            nodesTitle.getStyleClass().add("nodes-title");
            content.getChildren().add(nodesTitle);

            for (var nodeExec : execution.nodeExecutions()) {
                VBox nodeBox = createNodeExecutionBox(nodeExec);
                content.getChildren().add(nodeBox);
            }
        }
    }

    private VBox createExecutionSummary(ExecutionDTO execution) {
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(10));
        summaryBox.getStyleClass().add("execution-summary");

        Label summaryTitle = new Label("Execution Summary");
        summaryTitle.getStyleClass().add("summary-title");

        HBox summaryGrid = new HBox(20);
        summaryGrid.setPadding(new Insets(5, 0, 0, 0));

        VBox leftCol = new VBox(5);
        addDetailRow(leftCol, "Workflow:", execution.workflowName());
        addDetailRow(leftCol, "Status:", execution.status().toString());
        addDetailRow(leftCol, "Trigger:", execution.triggerType().toString());

        VBox rightCol = new VBox(5);
        addDetailRow(rightCol, "Started:", MainViewModel.formatInstantWithTime(execution.startedAt()));
        addDetailRow(rightCol, "Finished:", MainViewModel.formatInstantWithTime(execution.finishedAt()));
        addDetailRow(rightCol, "Duration:", MainViewModel.formatDuration(execution.durationMs()));

        summaryGrid.getChildren().addAll(leftCol, rightCol);
        summaryBox.getChildren().addAll(summaryTitle, summaryGrid);

        return summaryBox;
    }

    private VBox createErrorBox(String errorMessage) {
        VBox errorBox = new VBox(5);
        errorBox.setPadding(new Insets(10));
        errorBox.getStyleClass().add("execution-error");

        Label errorTitle = new Label("Error Message");
        errorTitle.getStyleClass().add("error-title");

        Label errorText = new Label(errorMessage);
        errorText.getStyleClass().add("error-text");
        errorText.setWrapText(true);
        errorText.setMaxWidth(Double.MAX_VALUE);

        errorBox.getChildren().addAll(errorTitle, errorText);
        return errorBox;
    }

    private VBox createNodeExecutionBox(ExecutionDTO.NodeExecutionDTO nodeExec) {
        VBox nodeBox = new VBox(8);
        nodeBox.setPadding(new Insets(10));
        nodeBox.getStyleClass().add("node-execution");

        // Node header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nodeName = new Label(nodeExec.nodeName() + " (" + nodeExec.nodeType() + ")");
        nodeName.getStyleClass().add("node-name");

        Label nodeStatusLabel = new Label(nodeExec.status().toString());
        nodeStatusLabel.getStyleClass().addAll("node-status", "status-" + nodeExec.status().toString().toLowerCase());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label durationLabel = new Label(MainViewModel.formatDuration(nodeExec.durationMs()));
        durationLabel.getStyleClass().add("node-duration");

        header.getChildren().addAll(nodeName, nodeStatusLabel, spacer, durationLabel);
        nodeBox.getChildren().add(header);

        // Node details
        HBox detailsRow = new HBox(15);
        detailsRow.setPadding(new Insets(5, 0, 0, 0));

        VBox timingCol = new VBox(3);
        addDetailRow(timingCol, "Started:", MainViewModel.formatInstantWithTime(nodeExec.startedAt()));
        addDetailRow(timingCol, "Finished:", MainViewModel.formatInstantWithTime(nodeExec.finishedAt()));

        detailsRow.getChildren().add(timingCol);

        // Error for failed nodes
        if (nodeExec.errorMessage() != null && !nodeExec.errorMessage().isBlank()) {
            VBox errorCol = new VBox(3);
            Label errorLabel = new Label("Error:");
            errorLabel.getStyleClass().add("node-error-label");
            Label errorText = new Label(nodeExec.errorMessage());
            errorText.getStyleClass().add("node-error-text");
            errorText.setWrapText(true);
            errorText.setMaxWidth(300);
            errorCol.getChildren().addAll(errorLabel, errorText);
            detailsRow.getChildren().add(errorCol);
        }

        nodeBox.getChildren().add(detailsRow);
        return nodeBox;
    }

    private void addDetailRow(VBox parent, String label, String value) {
        HBox row = new HBox(8);
        Label labelComp = new Label(label);
        labelComp.getStyleClass().add("detail-label");
        labelComp.setMinWidth(60);

        Label valueComp = new Label(value != null ? value : "N/A");
        valueComp.getStyleClass().add("detail-value");

        row.getChildren().addAll(labelComp, valueComp);
        parent.getChildren().add(row);
    }

    // ========== Menu Actions ==========

    @FXML
    private void onShowAbout() {
        dialogService.showAboutDialog();
    }

    @FXML
    private void onShowExpressionEditor() {
        dialogService.showExpressionEditorDialog();
    }

    @FXML
    private void onShowPluginManager() {
        showPluginManager();
    }

    @FXML
    private void onShowSamples() {
        List<SampleWorkflow> samples = sampleService.getAllSamples();
        List<String> categories = sampleService.getCategories();
        List<String> languages = sampleService.getLanguages();

        DialogFactory.showSamplesBrowser(
                getOwnerWindow(),
                samples,
                categories,
                languages,
                this::showGuideView).ifPresent(this::importSampleWorkflow);
    }

    private void showGuideView(SampleWorkflow sample) {
        // Set up node highlighting callback
        Consumer<List<String>> highlightCallback = nodeIds -> {
            if (workflowCanvas != null) {
                workflowCanvas.highlightNodes(nodeIds);
                viewModel.updateStatus("Highlighting nodes: " + String.join(", ", nodeIds));
            }
        };

        // Back callback to reopen samples browser
        Runnable backCallback = this::onShowSamples;

        javafx.stage.Window owner = contentArea.getScene() != null ? contentArea.getScene().getWindow() : null;
        DialogFactory.showGuideView(owner, sample, highlightCallback, this::importSampleWorkflow, backCallback);
    }

    private void importSampleWorkflow(SampleWorkflow sample) {
        try {
            Workflow workflow = sample.workflow();
            if (workflow != null) {
                WorkflowDTO workflowDto = WorkflowDTO.fromWorkflow(workflow);
                System.out.println("[importSampleWorkflow] Creating workflow: " + workflowDto.name());

                // Save the workflow to the database (creates new or updates if ID exists)
                workflowDto = workflowService.create(workflowDto);
                System.out.println("[importSampleWorkflow] Created workflow with ID: " + workflowDto.id());
                viewModel.updateStatus("Imported and saved sample: " + sample.name());

                showWorkflowsView();
                if (workflowCanvas != null) {
                    workflowCanvas.loadWorkflow(workflowDto);
                    viewModel.setActiveWorkflow(workflowDto);
                }
            }
        } catch (Exception e) {
            System.err.println("[importSampleWorkflow] Error: " + e.getMessage());
            e.printStackTrace();
            dialogService.showError("Import Error", "Failed to import sample workflow", e);
        }
    }

    // ========== Toolbar Actions ==========

    @FXML
    private void onNewWorkflow() {
        showWorkflowsView();
        if (workflowCanvas != null) {
            workflowCanvas.newWorkflow();
            viewModel.setActiveWorkflow(null);
            viewModel.updateStatus("New workflow");
        }
    }

    @FXML
    private void onOpenWorkflow() {
        List<WorkflowDTO> workflows = viewModel.getAllWorkflows();

        Consumer<WorkflowDTO> onDelete = workflow -> {
            try {
                viewModel.deleteWorkflow(workflow.id());
            } catch (Exception e) {
                dialogService.showError("Delete Failed", "Failed to delete workflow: " + e.getMessage());
            }
        };

        DialogFactory.showWorkflowListDialog(getOwnerWindow(), workflows, onDelete)
                .ifPresent(workflow -> {
                    showWorkflowsView();
                    if (workflowCanvas != null) {
                        workflowCanvas.loadWorkflow(workflow);
                        viewModel.setActiveWorkflow(workflow);
                    }
                });
    }

    @FXML
    private void onDeleteWorkflow() {
        List<WorkflowDTO> workflows = viewModel.getAllWorkflows();
        if (workflows.isEmpty()) {
            dialogService.showInfo("No Workflows", "No workflows found to delete.");
            return;
        }

        Consumer<WorkflowDTO> onDelete = workflow -> {
            try {
                viewModel.deleteWorkflow(workflow.id());
            } catch (Exception e) {
                dialogService.showError("Delete Failed", "Failed to delete workflow: " + e.getMessage());
            }
        };

        DialogFactory.showWorkflowListDialog(getOwnerWindow(), workflows, onDelete);
    }

    @FXML
    private void onSaveWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.saveWorkflow();
            viewModel.setWorkflowDirty(false);
        }
    }

    @FXML
    private void onRunWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.runWorkflow();
        }
    }

    @FXML
    private void onImportWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.importWorkflow();
        }
    }

    @FXML
    private void onExportWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.exportWorkflow();
        }
    }

    // ========== Zoom Actions ==========

    @FXML
    private void onZoomIn() {
        if (workflowCanvas != null) {
            workflowCanvas.zoomIn();
            viewModel.updateZoomStatus(workflowCanvas.getZoomPercentage());
        }
    }

    @FXML
    private void onZoomOut() {
        if (workflowCanvas != null) {
            workflowCanvas.zoomOut();
            viewModel.updateZoomStatus(workflowCanvas.getZoomPercentage());
        }
    }

    @FXML
    private void onResetZoom() {
        if (workflowCanvas != null) {
            workflowCanvas.resetZoom();
            viewModel.updateZoomStatus(workflowCanvas.getZoomPercentage());
        }
    }

    @FXML
    private void onFitToWindow() {
        if (workflowCanvas != null) {
            workflowCanvas.fitToView();
            viewModel.updateZoomStatus(workflowCanvas.getZoomPercentage());
        }
    }

    // ========== Grid Actions ==========

    @FXML
    private void onToggleShowGrid() {
        if (workflowCanvas != null && menuShowGrid != null) {
            workflowCanvas.setShowGrid(menuShowGrid.isSelected());
            // ViewModel will update status via binding
        }
    }

    @FXML
    private void onToggleSnapToGrid() {
        if (workflowCanvas != null && menuSnapToGrid != null) {
            workflowCanvas.setSnapToGrid(menuSnapToGrid.isSelected());
            // ViewModel will update status via binding
        }
    }

    // ========== Other Actions ==========

    @FXML
    private void onExit() {
        // Check for unsaved changes
        if (viewModel.isWorkflowDirty()) {
            boolean confirm = dialogService.confirm(
                    "Unsaved Changes",
                    "You have unsaved changes. Are you sure you want to exit?");
            if (!confirm) {
                return;
            }
        }
        // Shutdown all subsidiary windows/services
        ExecutionConsoleService.getInstance().shutdown();
        viewModel.dispose();
        javafx.application.Platform.exit();
    }

    @FXML
    private void onShowExecutionConsole() {
        ExecutionConsoleService.getInstance().showConsole();
    }

    @FXML
    private void onShowExecutionHistory() {
        if (workflowCanvas != null) {
            workflowCanvas.toggleExecutionHistory();
        }
    }

    // ========== Edit Menu Actions ==========

    @FXML
    private void onUndo() {
        if (workflowCanvas != null) {
            workflowCanvas.undo();
        }
    }

    @FXML
    private void onRedo() {
        if (workflowCanvas != null) {
            workflowCanvas.redo();
        }
    }

    @FXML
    private void onCut() {
        if (workflowCanvas != null) {
            workflowCanvas.copySelected();
            workflowCanvas.deleteSelected();
        }
    }

    @FXML
    private void onCopy() {
        if (workflowCanvas != null) {
            workflowCanvas.copySelected();
        }
    }

    @FXML
    private void onPaste() {
        if (workflowCanvas != null) {
            workflowCanvas.pasteNodes();
        }
    }

    @FXML
    private void onDelete() {
        if (workflowCanvas != null) {
            workflowCanvas.deleteSelected();
        }
    }

    @FXML
    private void onSelectAll() {
        if (workflowCanvas != null) {
            workflowCanvas.selectAll();
        }
    }

    // ========== File Menu Actions (Additional) ==========

    @FXML
    private void onSaveAs() {
        if (workflowCanvas != null) {
            workflowCanvas.saveWorkflowAs();
        }
    }

    // ========== Workflow Menu Actions (Additional) ==========

    @FXML
    private void onStopWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.stopWorkflow();
            viewModel.updateStatus("Workflow stopped");
        }
    }

    @FXML
    private void onValidateWorkflow() {
        if (workflowCanvas != null) {
            workflowCanvas.validateWorkflow();
        }
    }

    @FXML
    private void onClearExecutionData() {
        boolean confirm = dialogService.confirm(
                "Clear Execution Data",
                "Are you sure you want to clear all execution history?");
        if (confirm) {
            viewModel.clearExecutionHistory();
            viewModel.updateStatus("Execution history cleared");
        }
    }

    // ========== Help Menu Actions (Additional) ==========

    @FXML
    private void onShowDocumentation() {
        try {
            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://github.com/nervemind/nervemind/wiki"));
        } catch (Exception e) {
            dialogService.showError("Error", "Failed to open documentation", e);
        }
    }

    @FXML
    private void onShowKeyboardShortcuts() {
        DialogFactory.showKeyboardShortcutsDialog(getOwnerWindow());
    }

    // ========== Helper Methods ==========

    private void clearActiveButton() {
        btnWorkflows.getStyleClass().remove(ACTIVE_STYLE_CLASS);
        btnExecutions.getStyleClass().remove(ACTIVE_STYLE_CLASS);
        btnExecutionConsole.getStyleClass().remove(ACTIVE_STYLE_CLASS);
        btnCredentials.getStyleClass().remove(ACTIVE_STYLE_CLASS);
        btnPlugins.getStyleClass().remove(ACTIVE_STYLE_CLASS);
        btnSettings.getStyleClass().remove(ACTIVE_STYLE_CLASS);
    }

    private void syncGridMenuStates() {
        if (workflowCanvas != null) {
            if (menuShowGrid != null) {
                menuShowGrid.setSelected(workflowCanvas.isShowGrid());
            }
            if (menuSnapToGrid != null) {
                menuSnapToGrid.setSelected(workflowCanvas.isSnapToGrid());
            }
        }
    }

    /**
     * Get the owner window for dialogs.
     * 
     * @return the owner window or null if not available
     */
    private javafx.stage.Window getOwnerWindow() {
        if (rootPane != null && rootPane.getScene() != null) {
            return rootPane.getScene().getWindow();
        }
        return null;
    }

    // ========== Developer Menu Actions ==========

    /**
     * Copy the selected node's JSON to clipboard.
     */
    @FXML
    private void onCopyNodeJson() {
        if (workflowCanvas == null || devModeService == null) {
            return;
        }

        var selectedNode = workflowCanvas.getSelectedNode();
        if (selectedNode == null) {
            dialogService.showInfo("Copy Node JSON", "No node selected. Select a node first.");
            return;
        }

        String json = devModeService.exportNodeAsJson(selectedNode);
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(json);
        clipboard.setContent(content);

        viewModel.updateStatus("Node JSON copied to clipboard");
    }

    /**
     * Show expression context inspection dialog.
     */
    @FXML
    private void onInspectExpressionContext() {
        if (devModeService == null) {
            return;
        }

        // Build context from current execution state
        var context = devModeService.buildExpressionContext(
                java.util.Collections.emptyMap(), // Variables would come from execution context
                java.util.Collections.emptyMap(), // Node outputs
                java.util.Collections.emptyMap()); // Workflow data

        String contextJson = devModeService.formatExpressionContext(context);

        // Show in a text area dialog
        TextArea textArea = new TextArea(contextJson);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(60);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Expression Context");
        alert.setHeaderText("Current expression evaluation context");
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(700);
        alert.showAndWait();
    }

    /**
     * Show node execution timing summary.
     */
    @FXML
    private void onShowTimingSummary() {
        if (devModeService == null) {
            return;
        }

        String summary = devModeService.formatTimingSummary();

        TextArea textArea = new TextArea(summary);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(50);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Timing Summary");
        alert.setHeaderText("Node execution timing for last run");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    /**
     * Show HTTP request logs.
     */
    @FXML
    private void onShowHttpLogs() {
        if (devModeService == null) {
            return;
        }

        String logsContent = devModeService.formatHttpLogs();

        TextArea textArea = new TextArea(logsContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(70);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("HTTP Request Logs");
        alert.setHeaderText("HTTP requests from workflow execution");
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(800);
        alert.showAndWait();
    }

    /**
     * Export debug bundle (workflow + execution data + logs).
     */
    @FXML
    private void onExportDebugBundle() {
        if (devModeService == null || workflowCanvas == null) {
            return;
        }

        var workflow = workflowCanvas.getWorkflow();
        if (workflow == null) {
            dialogService.showInfo("Export Debug Bundle", "No workflow loaded. Open or create a workflow first.");
            return;
        }

        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Export Directory");
        chooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));

        java.io.File selectedDir = chooser.showDialog(getOwnerWindow());
        if (selectedDir == null) {
            return;
        }

        try {
            java.nio.file.Path bundlePath = devModeService.exportDebugBundle(
                    workflow,
                    selectedDir.toPath(),
                    null);

            dialogService.showInfo("Export Complete",
                    "Debug bundle exported to:\n" + bundlePath.toString());
            viewModel.updateStatus("Debug bundle exported");
        } catch (Exception e) {
            dialogService.showError("Export Failed", "Failed to export debug bundle", e);
        }
    }

    /**
     * Check if step-by-step execution mode is enabled.
     *
     * @return true if step execution mode is active
     */
    public boolean isStepExecutionEnabled() {
        return devModeService != null && devModeService.isStepExecutionEnabled();
    }
}
