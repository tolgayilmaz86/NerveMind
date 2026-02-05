/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.console;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.ConsoleEventListener;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.ExecutionSessionModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.LogEntryModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.LogEntryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

/**
 * Controller for the Execution Console view.
 * 
 * <p>
 * This is a thin adapter between the FXML view and the
 * ExecutionConsoleViewModel.
 * It handles:
 * <ul>
 * <li>FXML field bindings</li>
 * <li>User action delegation to ViewModel</li>
 * <li>Visual rendering of log entries</li>
 * <li>Icon and styling setup</li>
 * </ul>
 */
public class ExecutionConsoleController implements Initializable {

    // =========================
    // Nord Color Constants
    // =========================

    private static final String COLOR_INFO = "#88c0d0";
    private static final String COLOR_DEBUG = "#a3be8c";
    private static final String COLOR_WARN = "#ebcb8b";
    private static final String COLOR_ERROR = "#bf616a";
    private static final String COLOR_TRACE = "#b48ead";
    private static final String COLOR_TIMESTAMP = "#616e88";
    private static final String COLOR_NODE = "#81a1c1";
    private static final String COLOR_SUCCESS = "#a3be8c";
    private static final String COLOR_EXECUTION = "#d8dee9";
    private static final String BG_DARK = "#2e3440";
    private static final String BG_MEDIUM = "#3b4252";
    private static final String BG_LIGHT = "#434c5e";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================
    // FXML Fields - Toolbar
    // =========================

    @FXML
    private ToolBar mainToolbar;
    @FXML
    private Button clearBtn;
    @FXML
    private Button copyBtn;
    @FXML
    private ToggleButton autoScrollBtn;
    @FXML
    private ToggleButton timestampBtn;
    @FXML
    private ToggleButton debugBtn;
    @FXML
    private ToggleButton traceBtn;
    @FXML
    private TextField filterField;
    @FXML
    private ComboBox<String> sessionSelector;

    // =========================
    // FXML Fields - Tabs
    // =========================

    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab summaryTab;
    @FXML
    private Tab logsTab;

    // =========================
    // FXML Fields - Summary Tab
    // =========================

    @FXML
    private ScrollPane summaryScrollPane;
    @FXML
    private VBox summaryContainer;
    @FXML
    private Label emptyStateLabel;
    @FXML
    private HBox sessionHeader;
    @FXML
    private Label sessionTitleLabel;
    @FXML
    private VBox statusCard;
    @FXML
    private GridPane statusGrid;
    @FXML
    private HBox statusBadgeContainer;
    @FXML
    private Label executionIdLabel;
    @FXML
    private Label startTimeLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private VBox progressCard;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private VBox metricsCard;
    @FXML
    private HBox metricsContainer;
    @FXML
    private VBox nodesCard;
    @FXML
    private VBox nodesContainer;

    // =========================
    // FXML Fields - Logs Tab
    // =========================

    @FXML
    private HBox logsToolbar;
    @FXML
    private HBox statsBox;
    @FXML
    private Label infoIconLabel;
    @FXML
    private Label warnIconLabel;
    @FXML
    private Label errorIconLabel;
    @FXML
    private Label infoCountLabel;
    @FXML
    private Label warnCountLabel;
    @FXML
    private Label errorCountLabel;
    @FXML
    private Button jumpErrorBtn;
    @FXML
    private ToggleButton lineNumBtn;
    @FXML
    private Button expandAllBtn;
    @FXML
    private Button collapseAllBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Label totalEntriesLabel;
    @FXML
    private ScrollPane logsScrollPane;
    @FXML
    private VBox logContainer;

    // =========================
    // FXML Fields - Status Bar
    // =========================

    @FXML
    private HBox statusBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label filterStatusLabel;
    @FXML
    private Label sessionCountLabel;

    // =========================
    // ViewModel
    // =========================

    private ExecutionConsoleViewModel viewModel;
    private int logLineNumber = 0;
    private boolean pendingAutoScroll = false;

    // =========================
    // Initialization
    // =========================

    /**
     * Default constructor for ExecutionConsoleController.
     * Initializes a default ViewModel.
     */
    public ExecutionConsoleController() {
        this.viewModel = new ExecutionConsoleViewModel();
    }

    /**
     * Constructs an ExecutionConsoleController with the given ViewModel.
     * 
     * @param viewModel the view model to use
     */
    public ExecutionConsoleController(ExecutionConsoleViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupIcons();
        setupBindings();
        setupEventListeners();
        setupAutoScroll();
    }

    private void setupIcons() {
        // Toolbar icons
        clearBtn.setGraphic(FontIcon.of(MaterialDesignD.DELETE_OUTLINE, 16, Color.web(COLOR_EXECUTION)));
        clearBtn.setTooltip(new Tooltip("Clear all logs"));

        copyBtn.setGraphic(FontIcon.of(MaterialDesignC.CONTENT_COPY, 16, Color.web(COLOR_EXECUTION)));
        copyBtn.setTooltip(new Tooltip("Copy all logs to clipboard"));

        autoScrollBtn.setGraphic(FontIcon.of(MaterialDesignA.ARROW_DOWN_BOLD, 16, Color.web(COLOR_EXECUTION)));
        autoScrollBtn.setTooltip(new Tooltip("Auto-scroll"));

        timestampBtn.setGraphic(FontIcon.of(MaterialDesignC.CLOCK_OUTLINE, 16, Color.web(COLOR_EXECUTION)));
        timestampBtn.setTooltip(new Tooltip("Show timestamps"));

        // Tab icons
        summaryTab.setGraphic(FontIcon.of(MaterialDesignC.CHART_BOX_OUTLINE, 14, Color.web(COLOR_INFO)));
        logsTab.setGraphic(FontIcon.of(MaterialDesignT.TEXT_BOX_OUTLINE, 14, Color.web(COLOR_DEBUG)));

        // Logs toolbar icons
        jumpErrorBtn.setGraphic(FontIcon.of(MaterialDesignA.ARROW_DOWN_CIRCLE, 14, Color.web(COLOR_ERROR)));

        expandAllBtn.setGraphic(FontIcon.of(MaterialDesignC.CHEVRON_DOWN_BOX, 14, Color.web(COLOR_EXECUTION)));
        expandAllBtn.setTooltip(new Tooltip("Expand all details"));

        collapseAllBtn.setGraphic(FontIcon.of(MaterialDesignC.CHEVRON_UP_BOX, 14, Color.web(COLOR_EXECUTION)));
        collapseAllBtn.setTooltip(new Tooltip("Collapse all details"));

        exportBtn.setGraphic(FontIcon.of(MaterialDesignE.EXPORT, 14, Color.web(COLOR_EXECUTION)));
        exportBtn.setTooltip(new Tooltip("Export logs to file"));

        // Statistics badge icons
        infoIconLabel.setGraphic(FontIcon.of(MaterialDesignI.INFORMATION_OUTLINE, 12, Color.web(COLOR_INFO)));
        warnIconLabel.setGraphic(FontIcon.of(MaterialDesignA.ALERT_OUTLINE, 12, Color.web(COLOR_WARN)));
        errorIconLabel.setGraphic(FontIcon.of(MaterialDesignA.ALERT_CIRCLE_OUTLINE, 12, Color.web(COLOR_ERROR)));
    }

    private void setupBindings() {
        // Display settings
        autoScrollBtn.selectedProperty().bindBidirectional(viewModel.autoScrollProperty());
        timestampBtn.selectedProperty().bindBidirectional(viewModel.showTimestampsProperty());
        lineNumBtn.selectedProperty().bindBidirectional(viewModel.showLineNumbersProperty());

        // Filter toggles
        debugBtn.selectedProperty().bindBidirectional(viewModel.showDebugProperty());
        traceBtn.selectedProperty().bindBidirectional(viewModel.showTraceProperty());
        filterField.textProperty().bindBidirectional(viewModel.filterTextProperty());

        // Session selector
        sessionSelector.setItems(viewModel.sessionIdsProperty());
        sessionSelector.valueProperty().bindBidirectional(viewModel.selectedSessionIdProperty());

        // Statistics
        infoCountLabel.textProperty().bind(viewModel.infoCountProperty().asString());
        warnCountLabel.textProperty().bind(viewModel.warnCountProperty().asString());
        errorCountLabel.textProperty().bind(viewModel.errorCountProperty().asString());
        totalEntriesLabel.textProperty().bind(viewModel.visibleEntryCountProperty().asString().concat(" entries"));

        // Status bar
        filterStatusLabel.textProperty().bind(viewModel.filterStatusProperty());
        sessionCountLabel.textProperty().bind(viewModel.sessionCountProperty().asString("Sessions: %d"));

        // Update toggle button styles based on selection
        debugBtn.selectedProperty().addListener((obs, old, val) -> styleToggleButton(debugBtn, COLOR_DEBUG, val));
        traceBtn.selectedProperty().addListener((obs, old, val) -> styleToggleButton(traceBtn, COLOR_TRACE, val));
        lineNumBtn.selectedProperty()
                .addListener((obs, old, val) -> styleToggleButton(lineNumBtn, COLOR_TIMESTAMP, val));

        // Initial styles
        styleToggleButton(debugBtn, COLOR_DEBUG, debugBtn.isSelected());
        styleToggleButton(traceBtn, COLOR_TRACE, traceBtn.isSelected());
        styleToggleButton(lineNumBtn, COLOR_TIMESTAMP, lineNumBtn.isSelected());
    }

    private void setupEventListeners() {
        viewModel.addEventListener(new ConsoleEventListener() {
            @Override
            public void onSessionStarted(String executionId, String workflowName) {
                // Note: Don't add header here - the selectedSessionIdProperty listener
                // will trigger refreshLogDisplay() which renders the session properly.
                // Adding header here would cause duplicates.
                Platform.runLater(() -> updateSummaryView());
            }

            @Override
            public void onSessionEnded(String executionId, boolean success) {
                Platform.runLater(() -> updateSummaryView());
            }

            @Override
            public void onEntryAdded(LogEntryModel entry) {
                // Only add entry if we're displaying this session and not about to refresh
                Platform.runLater(() -> {
                    String selectedId = viewModel.selectedSessionIdProperty().get();
                    if ((selectedId == null || selectedId.equals(entry.executionId()))
                            && viewModel.shouldShowEntry(entry)) {
                        logContainer.getChildren().add(createLogEntryView(entry));
                        pendingAutoScroll = true;
                    }
                });
            }

            @Override
            public void onCleared() {
                Platform.runLater(() -> {
                    logContainer.getChildren().clear();
                    logLineNumber = 0;
                    updateSummaryView();
                });
            }
        });

        // Refresh display when filters change
        viewModel.showDebugProperty().addListener((obs, old, val) -> refreshLogDisplay());
        viewModel.showTraceProperty().addListener((obs, old, val) -> refreshLogDisplay());
        viewModel.showTimestampsProperty().addListener((obs, old, val) -> refreshLogDisplay());
        viewModel.showLineNumbersProperty().addListener((obs, old, val) -> refreshLogDisplay());
        viewModel.filterTextProperty().addListener((obs, old, val) -> refreshLogDisplay());
        viewModel.selectedSessionIdProperty().addListener((obs, old, val) -> {
            refreshLogDisplay();
            updateSummaryView();
        });

        // Toggle button hover effects
        setupToggleButtonHoverEffects(debugBtn, COLOR_DEBUG);
        setupToggleButtonHoverEffects(traceBtn, COLOR_TRACE);
        setupToggleButtonHoverEffects(lineNumBtn, COLOR_TIMESTAMP);
    }

    private void setupAutoScroll() {
        logContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel.autoScrollProperty().get() && pendingAutoScroll) {
                logsScrollPane.setVvalue(1.0);
                pendingAutoScroll = false;
            }
        });
    }

    // =========================
    // Action Handlers
    // =========================

    @FXML
    private void onClear() {
        if (viewModel.sessionCountProperty().get() > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Clear all execution logs?", ButtonType.OK, ButtonType.CANCEL);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    viewModel.clear();
                }
            });
        }
    }

    @FXML
    private void onCopy() {
        String text = viewModel.getLogsAsText();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onJumpToError() {
        for (javafx.scene.Node node : logContainer.getChildren()) {
            if (node instanceof VBox vbox && isErrorEntry(vbox)) {
                logsScrollPane.setVvalue(vbox.getLayoutY() / logContainer.getHeight());
                highlightEntry(vbox);
                return;
            }
        }
    }

    @FXML
    private void onExpandAll() {
        for (javafx.scene.Node node : logContainer.getChildren()) {
            if (node instanceof VBox container) {
                expandEntryIfNeeded(container);
            }
        }
    }

    @FXML
    private void onCollapseAll() {
        for (javafx.scene.Node node : logContainer.getChildren()) {
            if (node instanceof VBox container && container.getChildren().size() > 1) {
                container.getChildren().remove(1, container.getChildren().size());
            }
        }
    }

    @FXML
    private void onExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logs");
        fileChooser.setInitialFileName("execution_logs.txt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(logContainer.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                if (file.getName().endsWith(".json")) {
                    writer.write(viewModel.exportAsJson());
                } else {
                    writer.write(viewModel.exportAsText());
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Logs exported successfully!", ButtonType.OK);
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Failed to export logs: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    // =========================
    // Summary View
    // =========================

    private void updateSummaryView() {
        ExecutionSessionModel session = viewModel.getSelectedSession();

        if (session == null) {
            // Show empty state
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            sessionHeader.setVisible(false);
            sessionHeader.setManaged(false);
            statusCard.setVisible(false);
            statusCard.setManaged(false);
            progressCard.setVisible(false);
            progressCard.setManaged(false);
            metricsCard.setVisible(false);
            metricsCard.setManaged(false);
            nodesCard.setVisible(false);
            nodesCard.setManaged(false);
            return;
        }

        // Hide empty state
        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);

        // Show header
        sessionHeader.setVisible(true);
        sessionHeader.setManaged(true);
        sessionTitleLabel.setText(session.getWorkflowName());

        // Update status card
        statusCard.setVisible(true);
        statusCard.setManaged(true);
        updateStatusBadge(session);
        executionIdLabel.setText(session.getExecutionId());
        startTimeLabel.setText(DATE_TIME_FORMAT.format(
                LocalDateTime.ofInstant(session.getStartTime(), ZoneId.systemDefault())));

        String duration = session.isComplete()
                ? viewModel.formatDuration(session.getDurationMs())
                : viewModel.formatDuration(session.getRunningDurationMs()) + " (running)";
        durationLabel.setText(duration);

        // Progress card (only when running)
        boolean showProgress = !session.isComplete();
        progressCard.setVisible(showProgress);
        progressCard.setManaged(showProgress);
        if (showProgress) {
            double progress = session.getProgress();
            progressBar.setProgress(progress);
            progressLabel.setText(String.format("%d / %d nodes completed (%.0f%%)",
                    session.getCompletedNodes(), session.getTotalNodes(), progress * 100));
        }

        // Metrics card
        metricsCard.setVisible(true);
        metricsCard.setManaged(true);
        updateMetricsView(session);

        // Nodes card
        nodesCard.setVisible(true);
        nodesCard.setManaged(true);
        updateNodesView(session);
    }

    private void updateStatusBadge(ExecutionSessionModel session) {
        statusBadgeContainer.getChildren().clear();

        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(4, 10, 4, 10));

        String status;
        String color;
        org.kordamp.ikonli.Ikon iconType;

        if (!session.isComplete()) {
            status = "RUNNING";
            color = COLOR_INFO;
            iconType = MaterialDesignS.SYNC;
        } else if (session.isSuccess()) {
            status = "COMPLETED";
            color = COLOR_SUCCESS;
            iconType = MaterialDesignC.CHECK_CIRCLE;
        } else {
            status = "FAILED";
            color = COLOR_ERROR;
            iconType = MaterialDesignC.CLOSE_CIRCLE;
        }

        badge.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 12;");

        FontIcon icon = FontIcon.of(iconType, 14, Color.web(color));
        Label label = new Label(status);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        badge.getChildren().addAll(icon, label);
        statusBadgeContainer.getChildren().add(badge);
    }

    private void updateMetricsView(ExecutionSessionModel session) {
        metricsContainer.getChildren().clear();

        int errorCount = (int) session.getEntries().stream()
                .filter(e -> e.type() == LogEntryType.ERROR).count();
        int warnCount = (int) session.getEntries().stream()
                .filter(e -> e.type() == LogEntryType.WARN).count();

        addMetricBox("Total Nodes", String.valueOf(session.getTotalNodes()), COLOR_NODE);
        addMetricBox("Completed", String.valueOf(session.getCompletedNodes()), COLOR_SUCCESS);
        addMetricBox("Warnings", String.valueOf(warnCount), COLOR_WARN);
        addMetricBox("Errors", String.valueOf(errorCount), COLOR_ERROR);
    }

    private void addMetricBox(String label, String value, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 20, 10, 20));
        box.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-background-radius: 6;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TIMESTAMP + ";");

        box.getChildren().addAll(valueLabel, nameLabel);
        metricsContainer.getChildren().add(box);
    }

    private void updateNodesView(ExecutionSessionModel session) {
        nodesContainer.getChildren().clear();

        var nodeEntries = session.getEntries().stream()
                .filter(e -> e.type() == LogEntryType.NODE_START
                        || e.type() == LogEntryType.NODE_END
                        || e.type() == LogEntryType.ERROR)
                .toList();

        if (nodeEntries.isEmpty()) {
            Label emptyLabel = new Label("No node executions yet...");
            emptyLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-font-style: italic;");
            nodesContainer.getChildren().add(emptyLabel);
        } else {
            for (LogEntryModel entry : nodeEntries) {
                nodesContainer.getChildren().add(createNodeExecutionRow(entry));
            }
        }
    }

    private HBox createNodeExecutionRow(LogEntryModel entry) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-background-radius: 4;");

        org.kordamp.ikonli.Ikon iconType;
        if (entry.type() == LogEntryType.NODE_START) {
            iconType = MaterialDesignP.PLAY;
        } else if (entry.type() == LogEntryType.ERROR) {
            iconType = MaterialDesignA.ALERT_CIRCLE;
        } else {
            iconType = MaterialDesignC.CHECK;
        }

        FontIcon icon = FontIcon.of(iconType, 12, Color.web(getColorForType(entry.type())));

        Label timeLabel = new Label(viewModel.formatTimestamp(entry.timestamp()));
        timeLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP
                + "; -fx-font-size: 10px; -fx-font-family: monospace;");

        Label msgLabel = new Label(entry.message());
        msgLabel.setStyle("-fx-text-fill: " + COLOR_EXECUTION + ";");
        HBox.setHgrow(msgLabel, Priority.ALWAYS);

        row.getChildren().addAll(icon, timeLabel, msgLabel);
        return row;
    }

    // =========================
    // Log Display
    // =========================

    private void refreshLogDisplay() {
        Platform.runLater(this::performLogDisplayRefresh);
    }

    private void performLogDisplayRefresh() {
        logContainer.getChildren().clear();
        logLineNumber = 0;

        String selectedSessionId = viewModel.selectedSessionIdProperty().get();
        if (selectedSessionId == null) {
            displayAllSessions();
        } else {
            displaySelectedSession();
        }
    }

    private void displayAllSessions() {
        for (ExecutionSessionModel session : viewModel.getAllSessions()) {
            displaySessionEntries(session);
        }
    }

    private void displaySelectedSession() {
        ExecutionSessionModel session = viewModel.getSelectedSession();
        if (session != null) {
            displaySessionEntries(session);
        }
    }

    private void displaySessionEntries(ExecutionSessionModel session) {
        addExecutionHeader(session.getExecutionId(), session.getWorkflowName());
        for (LogEntryModel entry : session.getEntries()) {
            if (viewModel.shouldShowEntry(entry)) {
                logContainer.getChildren().add(createLogEntryView(entry));
            }
        }
    }

    private void addExecutionHeader(String executionId, String workflowName) {
        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 12, 10, 12));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + BG_MEDIUM + "; -fx-background-radius: 6;");
        header.getStyleClass().add("execution-header");

        FontIcon icon = FontIcon.of(MaterialDesignP.PLAY_CIRCLE, 20, Color.web(COLOR_SUCCESS));

        Text title = new Text("Execution: " + workflowName);
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setFill(Color.web(COLOR_EXECUTION));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Text id = new Text("#" + executionId.substring(0, Math.min(8, executionId.length())));
        id.setFill(Color.web(COLOR_TIMESTAMP));
        id.setFont(Font.font("Monospace", 10));

        header.getChildren().addAll(icon, title, spacer, id);
        logContainer.getChildren().add(header);
    }

    private VBox createLogEntryView(LogEntryModel entry) {
        VBox container = new VBox(2);
        container.setPadding(new Insets(4, 10, 4, 10));
        container.setUserData(entry);
        container.getStyleClass().add("log-entry");

        // Alternating row background
        int lineNum = ++logLineNumber;
        if (lineNum % 2 == 0) {
            container.setStyle("-fx-background-color: " + BG_MEDIUM + "33;");
        }

        HBox mainLine = new HBox(6);
        mainLine.setAlignment(Pos.CENTER_LEFT);

        // Line number (if enabled)
        if (viewModel.showLineNumbersProperty().get()) {
            Label numLabel = new Label(String.format("%4d", lineNum));
            numLabel.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP
                    + "; -fx-font-family: 'Monospace'; -fx-font-size: 10px; -fx-min-width: 35;");
            mainLine.getChildren().add(numLabel);

            Separator sep = new Separator(Orientation.VERTICAL);
            sep.setStyle("-fx-background-color: " + BG_LIGHT + ";");
            mainLine.getChildren().add(sep);
        }

        // Timestamp
        if (viewModel.showTimestampsProperty().get()) {
            Text timestamp = new Text(viewModel.formatTimestamp(entry.timestamp()) + " ");
            timestamp.setFill(Color.web(COLOR_TIMESTAMP));
            timestamp.setFont(Font.font("Monospace", 10));
            mainLine.getChildren().add(timestamp);
        }

        // Indentation for hierarchy
        if (entry.depth() > 0) {
            Region indent = new Region();
            indent.setMinWidth(entry.depth() * 16.0);
            mainLine.getChildren().add(indent);
        }

        // Log level badge
        Label levelBadge = new Label(getLevelBadgeText(entry.type()));
        levelBadge.setStyle(String.format(
                "-fx-background-color: %s33; " +
                        "-fx-text-fill: %s; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 1 6 1 6; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold;",
                getColorForType(entry.type()), getColorForType(entry.type())));
        mainLine.getChildren().add(levelBadge);

        // Message text
        Text msgText = new Text(entry.message());
        msgText.setFill(Color.web(getColorForType(entry.type())));
        msgText.setFont(Font.font("System", 12));
        mainLine.getChildren().add(msgText);

        // Expandable details indicator
        if (entry.details() != null && !entry.details().isEmpty()) {
            Label detailsLink = new Label(" ▶");
            detailsLink.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-cursor: hand;");
            detailsLink.setOnMouseClicked(e -> toggleDetails(container, entry));
            detailsLink.setOnMouseEntered(
                    e -> detailsLink.setStyle("-fx-text-fill: " + COLOR_INFO + "; -fx-cursor: hand;"));
            detailsLink.setOnMouseExited(
                    e -> detailsLink.setStyle("-fx-text-fill: " + COLOR_TIMESTAMP + "; -fx-cursor: hand;"));
            mainLine.getChildren().add(detailsLink);
        }

        container.getChildren().add(mainLine);

        // Error entries get special background
        if (entry.type() == LogEntryType.ERROR) {
            container.setStyle("-fx-background-color: rgba(191, 97, 106, 0.1); -fx-border-color: "
                    + COLOR_ERROR + "44; -fx-border-width: 0 0 0 3;");
            container.getStyleClass().add("log-entry-error");
        } else if (entry.type() == LogEntryType.WARN) {
            container.setStyle(container.getStyle() + " -fx-border-color: "
                    + COLOR_WARN + "44; -fx-border-width: 0 0 0 2;");
            container.getStyleClass().add("log-entry-warn");
        }

        // Context menu
        ContextMenu contextMenu = createLogEntryContextMenu(container, entry);
        container.setOnContextMenuRequested(e -> contextMenu.show(container, e.getScreenX(), e.getScreenY()));

        return container;
    }

    private void toggleDetails(VBox container, LogEntryModel entry) {
        if (container.getChildren().size() > 1) {
            container.getChildren().remove(1);
        } else if (entry.details() != null) {
            VBox details = new VBox(5);
            details.setPadding(new Insets(8, 20, 8, 40));
            details.setStyle("-fx-background-color: " + BG_LIGHT + "44; -fx-background-radius: 4;");

            Text detailText = new Text(entry.details());
            detailText.setFill(Color.web(COLOR_EXECUTION));
            detailText.setFont(Font.font("Monospace", 11));

            details.getChildren().add(detailText);
            container.getChildren().add(details);
        }
    }

    private ContextMenu createLogEntryContextMenu(VBox container, LogEntryModel entry) {
        ContextMenu menu = new ContextMenu();

        MenuItem copyMsg = new MenuItem("Copy Message");
        copyMsg.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(entry.message());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem copyAll = new MenuItem("Copy with Details");
        copyAll.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            String text = viewModel.formatTimestamp(entry.timestamp())
                    + " [" + entry.type() + "] " + entry.message();
            if (entry.details() != null) {
                text += "\n" + entry.details();
            }
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });

        if (entry.details() != null && !entry.details().isEmpty()) {
            MenuItem toggleExpand = new MenuItem("Expand Details");
            toggleExpand.setOnAction(e -> toggleDetails(container, entry));
            menu.getItems().add(toggleExpand);
            menu.getItems().add(new SeparatorMenuItem());
        }

        menu.getItems().addAll(copyMsg, copyAll);
        return menu;
    }

    // =========================
    // Helper Methods
    // =========================

    private String getLevelBadgeText(LogEntryType type) {
        return switch (type) {
            case INFO -> "INFO";
            case DEBUG -> "DEBUG";
            case WARN -> "WARN";
            case ERROR -> "ERROR";
            case TRACE -> "TRACE";
            case NODE_START -> "START";
            case NODE_END -> "END";
            case NODE_INPUT -> "INPUT";
            case NODE_OUTPUT -> "OUTPUT";
            case EXPRESSION -> "EXPR";
            case SUCCESS -> "OK";
        };
    }

    private String getColorForType(LogEntryType type) {
        return switch (type) {
            case INFO -> COLOR_INFO;
            case DEBUG -> COLOR_DEBUG;
            case WARN -> COLOR_WARN;
            case ERROR -> COLOR_ERROR;
            case TRACE -> COLOR_TRACE;
            case NODE_START, NODE_END -> COLOR_NODE;
            case NODE_INPUT, NODE_OUTPUT -> COLOR_DEBUG;
            case EXPRESSION -> COLOR_TRACE;
            case SUCCESS -> COLOR_SUCCESS;
        };
    }

    private void styleToggleButton(ToggleButton btn, String color, boolean selected) {
        if (selected) {
            btn.setStyle(String.format(
                    "-fx-background-color: %s; " +
                            "-fx-text-fill: %s; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-radius: 12; " +
                            "-fx-border-width: 0; " +
                            "-fx-padding: 4 12 4 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 11px; " +
                            "-fx-cursor: hand; " +
                            "-fx-focus-color: transparent; " +
                            "-fx-faint-focus-color: transparent; " +
                            "-fx-background-insets: 0;",
                    color, BG_DARK));
        } else {
            btn.setStyle(String.format(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: %s; " +
                            "-fx-border-color: %s; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 12; " +
                            "-fx-background-radius: 12; " +
                            "-fx-padding: 3 11 3 11; " +
                            "-fx-font-size: 11px; " +
                            "-fx-opacity: 0.6; " +
                            "-fx-cursor: hand; " +
                            "-fx-focus-color: transparent; " +
                            "-fx-faint-focus-color: transparent; " +
                            "-fx-background-insets: 0;",
                    color, color));
        }
    }

    private void setupToggleButtonHoverEffects(ToggleButton btn, String color) {
        btn.setOnMouseEntered(e -> {
            if (!btn.isSelected()) {
                btn.setStyle(String.format(
                        "-fx-background-color: %s22; " +
                                "-fx-text-fill: %s; " +
                                "-fx-border-color: %s; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12; " +
                                "-fx-background-radius: 12; " +
                                "-fx-padding: 3 11 3 11; " +
                                "-fx-font-size: 11px; " +
                                "-fx-opacity: 1.0; " +
                                "-fx-cursor: hand; " +
                                "-fx-focus-color: transparent; " +
                                "-fx-faint-focus-color: transparent; " +
                                "-fx-background-insets: 0;",
                        color, color, color));
            }
        });
        btn.setOnMouseExited(e -> styleToggleButton(btn, color, btn.isSelected()));
    }

    private boolean isErrorEntry(VBox vbox) {
        return vbox.getStyle() != null && vbox.getStyle().contains("rgba(191, 97, 106");
    }

    private void highlightEntry(VBox vbox) {
        String originalStyle = vbox.getStyle();
        vbox.setStyle(originalStyle + " -fx-border-color: " + COLOR_ERROR
                + "; -fx-border-width: 2; -fx-border-radius: 4;");

        // Remove highlight after a delay
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> vbox.setStyle("-fx-background-color: rgba(191, 97, 106, 0.1);"));
        });
    }

    private void expandEntryIfNeeded(VBox container) {
        if (container.getChildren().size() == 1
                && container.getUserData() instanceof LogEntryModel entry
                && entry.details() != null
                && !entry.details().isEmpty()) {
            toggleDetails(container, entry);
        }
    }

    // =========================
    // Public API
    // =========================

    /**
     * Get the underlying ViewModel.
     * 
     * @return the execution console view model
     */
    public ExecutionConsoleViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Set the ViewModel (for testing or injection).
     * 
     * @param viewModel the view model to set
     */
    public void setViewModel(ExecutionConsoleViewModel viewModel) {
        this.viewModel = viewModel;
    }
}
