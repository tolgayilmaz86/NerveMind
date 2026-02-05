/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.canvas;

import java.io.IOException;

import org.kordamp.ikonli.javafx.FontIcon;

import ai.nervemind.common.domain.Execution;
import ai.nervemind.common.domain.Execution.NodeExecution;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.exception.UiInitializationException;
import ai.nervemind.ui.viewmodel.canvas.ExecutionHistoryViewModel;
import ai.nervemind.ui.viewmodel.canvas.ExecutionHistoryViewModel.TimelineEntry;
import ai.nervemind.ui.viewmodel.canvas.ExecutionHistoryViewModel.VariableEntry;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Component for displaying execution history.
 *
 * <p>
 * Uses fx:root pattern to extend VBox while loading FXML.
 * Binds to {@link ExecutionHistoryViewModel} for state management.
 */
public class ExecutionHistoryPanelComponent extends VBox {

    private final ExecutionHistoryViewModel viewModel;

    // ===== FXML Injected =====
    @FXML
    private FontIcon headerIcon;
    @FXML
    private Button closeButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button clearButton;
    @FXML
    private Label countLabel;

    @FXML
    private TableView<Execution> executionTable;
    @FXML
    private TableColumn<Execution, ExecutionStatus> statusColumn;
    @FXML
    private TableColumn<Execution, Execution> timeColumn;
    @FXML
    private TableColumn<Execution, Execution> durationColumn;
    @FXML
    private TableColumn<Execution, Execution> triggerColumn;

    @FXML
    private VBox detailsPane;
    @FXML
    private TabPane detailsTabPane;
    @FXML
    private Tab errorTab;

    @FXML
    private GridPane summaryGrid;
    @FXML
    private HBox statusBadge;
    @FXML
    private FontIcon summaryStatusIcon;
    @FXML
    private Label summaryStatusLabel;
    @FXML
    private Label summaryWorkflowLabel;
    @FXML
    private Label summaryTriggerLabel;
    @FXML
    private Label summaryStartedLabel;
    @FXML
    private Label summaryFinishedLabel;
    @FXML
    private Label summaryDurationLabel;
    @FXML
    private Label summaryIdLabel;

    @FXML
    private VBox nodesContainer;
    @FXML
    private TextArea errorTextArea;
    @FXML
    private VBox timelineContainer;

    @FXML
    private TableView<VariableEntry> variablesTable;
    @FXML
    private TableColumn<VariableEntry, String> variableNameColumn;
    @FXML
    private TableColumn<VariableEntry, String> variableValueColumn;

    /**
     * Creates a new ExecutionHistoryPanelComponent.
     */
    public ExecutionHistoryPanelComponent() {
        this(new ExecutionHistoryViewModel());
    }

    /**
     * Creates a new ExecutionHistoryPanelComponent with the specified ViewModel.
     *
     * @param viewModel the ViewModel to use
     */
    public ExecutionHistoryPanelComponent(ExecutionHistoryViewModel viewModel) {
        this.viewModel = viewModel;
        loadFxml();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ai/nervemind/ui/view/canvas/ExecutionHistoryPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new UiInitializationException("Failed to load ExecutionHistoryPanel.fxml", "ExecutionHistoryPanel",
                    e);
        }
    }

    @FXML
    private void initialize() {
        setupBindings();
        setupExecutionTable();
        setupVariablesTable();
        setupSelectionListener();
    }

    private void setupBindings() {
        // Panel visibility
        visibleProperty().bindBidirectional(viewModel.visibleProperty());
        managedProperty().bind(visibleProperty());

        // Count label
        countLabel.textProperty().bind(viewModel.executionCountTextProperty());

        // Details pane visibility
        detailsPane.visibleProperty().bind(viewModel.detailsVisibleProperty());
        detailsPane.managedProperty().bind(viewModel.detailsVisibleProperty());

        // Error tab visibility
        viewModel.selectedHasErrorProperty().addListener((obs, oldVal, newVal) -> {
            if (Boolean.TRUE.equals(newVal) && !detailsTabPane.getTabs().contains(errorTab)) {
                // Add error tab after Nodes tab (index 2)
                detailsTabPane.getTabs().add(2, errorTab);
                detailsTabPane.getSelectionModel().select(errorTab);
            } else if (Boolean.FALSE.equals(newVal) && detailsTabPane.getTabs().contains(errorTab)) {
                detailsTabPane.getTabs().remove(errorTab);
            }
        });

        // Remove error tab initially
        detailsTabPane.getTabs().remove(errorTab);
    }

    private void setupExecutionTable() {
        executionTable.setItems(viewModel.getExecutions());

        // Status column
        statusColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().status()));
        statusColumn.setCellFactory(col -> createStatusCell());

        // Time column
        timeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        timeColumn.setCellFactory(col -> createTimeCell());

        // Duration column
        durationColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        durationColumn.setCellFactory(col -> createDurationCell());

        // Trigger column
        triggerColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
        triggerColumn.setCellFactory(col -> createTriggerCell());

        // Row factory for double-click
        executionTable.setRowFactory(tv -> createExecutionRow());
    }

    private TableCell<Execution, ExecutionStatus> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(ExecutionStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                FontIcon icon = new FontIcon(ExecutionHistoryViewModel.getStatusIconName(status));
                icon.setIconSize(16);
                icon.getStyleClass().add("status-icon-" + status.name().toLowerCase());
                setGraphic(icon);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<Execution, Execution> createTimeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Execution execution, boolean empty) {
                super.updateItem(execution, empty);
                if (empty || execution == null) {
                    setText(null);
                    return;
                }
                setText(ExecutionHistoryViewModel.formatTime(execution.startedAt()));
            }
        };
    }

    private TableCell<Execution, Execution> createDurationCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Execution execution, boolean empty) {
                super.updateItem(execution, empty);
                if (empty || execution == null) {
                    setText(null);
                    return;
                }
                setText(ExecutionHistoryViewModel.formatDuration(execution.duration()));
                getStyleClass().add("duration-cell");
            }
        };
    }

    private TableCell<Execution, Execution> createTriggerCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Execution execution, boolean empty) {
                super.updateItem(execution, empty);
                if (empty || execution == null || execution.triggerType() == null) {
                    setText(null);
                    return;
                }
                setText(execution.triggerType().getDisplayName());
            }
        };
    }

    private TableRow<Execution> createExecutionRow() {
        TableRow<Execution> row = new TableRow<>();
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !row.isEmpty()) {
                viewModel.highlightSelectedExecution();
            }
        });
        return row;
    }

    private void setupVariablesTable() {
        variablesTable.setItems(viewModel.getVariableEntries());

        variableNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));

        variableValueColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedValue()));
    }

    private void setupSelectionListener() {
        executionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setSelectedExecution(newVal);
            if (newVal != null) {
                updateDetailsView(newVal);
            }
        });
    }

    private void updateDetailsView(Execution execution) {
        // Update summary
        updateSummaryView(execution);

        // Update nodes view
        updateNodesView(execution.nodeExecutions());

        // Update error view
        if (execution.status() == ExecutionStatus.FAILED && execution.errorMessage() != null) {
            errorTextArea.setText(execution.errorMessage());
        }

        // Update timeline view
        updateTimelineView();
    }

    private void updateSummaryView(Execution execution) {
        // Update status badge
        ExecutionStatus status = execution.status();
        statusBadge.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusBadge.getStyleClass().add(ExecutionHistoryViewModel.getStatusClassName(status));

        summaryStatusIcon.setIconLiteral(ExecutionHistoryViewModel.getStatusIconName(status));
        summaryStatusLabel.setText(status.name());

        // Update other fields
        summaryWorkflowLabel.setText(execution.workflowName());
        summaryTriggerLabel.setText(execution.triggerType() != null
                ? execution.triggerType().getDisplayName()
                : "-");
        summaryStartedLabel.setText(ExecutionHistoryViewModel.formatTime(execution.startedAt()));
        summaryFinishedLabel.setText(ExecutionHistoryViewModel.formatTime(execution.finishedAt()));
        summaryDurationLabel.setText(ExecutionHistoryViewModel.formatDuration(execution.duration()));
        summaryIdLabel.setText(execution.id() != null ? execution.id().toString() : "-");
    }

    private void updateNodesView(java.util.List<NodeExecution> nodeExecutions) {
        nodesContainer.getChildren().clear();

        if (nodeExecutions == null || nodeExecutions.isEmpty()) {
            nodesContainer.getChildren().add(new Label("No node executions recorded."));
            return;
        }

        for (NodeExecution nodeExec : nodeExecutions) {
            nodesContainer.getChildren().add(createNodeExecutionRow(nodeExec));
        }
    }

    private VBox createNodeExecutionRow(NodeExecution nodeExec) {
        VBox container = new VBox();

        HBox mainRow = new HBox(8);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(6, 10, 6, 10));
        mainRow.getStyleClass().add("node-execution-row");

        FontIcon statusIcon = new FontIcon(
                ExecutionHistoryViewModel.getStatusIconName(nodeExec.status()));
        statusIcon.setIconSize(16);
        statusIcon.getStyleClass().add("status-icon-" + nodeExec.status().name().toLowerCase());

        Label nameLabel = new Label(nodeExec.nodeName());
        nameLabel.getStyleClass().add("node-name-label");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label durationLabel = new Label(
                ExecutionHistoryViewModel.formatDuration(nodeExec.duration()));
        durationLabel.getStyleClass().add("duration-label");

        FontIcon expandIcon = new FontIcon("mdi2c-chevron-down");
        expandIcon.setIconSize(16);
        expandIcon.getStyleClass().add("expand-icon");

        mainRow.getChildren().addAll(statusIcon, nameLabel, durationLabel, expandIcon);

        // Details view (initially hidden)
        VBox detailsView = createNodeDetailsView(nodeExec);
        detailsView.setVisible(false);
        detailsView.setManaged(false);

        mainRow.setOnMouseClicked(e -> {
            boolean isVisible = detailsView.isVisible();
            detailsView.setVisible(!isVisible);
            detailsView.setManaged(!isVisible);
            expandIcon.setRotate(isVisible ? 0 : 180);
        });

        container.getChildren().addAll(mainRow, detailsView);
        return container;
    }

    private VBox createNodeDetailsView(NodeExecution nodeExec) {
        VBox details = new VBox(10);
        details.setPadding(new Insets(10, 10, 10, 30));
        details.getStyleClass().add("node-details-view");

        // Input Data
        details.getChildren().add(createDataSection("Input", nodeExec.inputData()));

        // Output Data
        details.getChildren().add(createDataSection("Output", nodeExec.outputData()));

        // Error if present
        if (nodeExec.errorMessage() != null) {
            details.getChildren().add(createDataSection("Error", nodeExec.errorMessage()));
        }

        return details;
    }

    private VBox createDataSection(String title, Object data) {
        VBox section = new VBox(5);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("data-section-title");

        TextArea dataArea = new TextArea(formatData(data));
        dataArea.setEditable(false);
        dataArea.setWrapText(true);
        dataArea.getStyleClass().add("data-textarea");
        dataArea.setPrefRowCount(5);

        section.getChildren().addAll(titleLabel, dataArea);
        return section;
    }

    private void updateTimelineView() {
        timelineContainer.getChildren().clear();

        var entries = viewModel.getTimelineEntries();
        if (entries.isEmpty()) {
            timelineContainer.getChildren().add(
                    new Label("No node executions to visualize."));
            return;
        }

        final double timelineWidth = 350;
        final double rowHeight = 25;

        for (TimelineEntry entry : entries) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(entry.nodeName());
            nameLabel.setPrefWidth(100);
            nameLabel.getStyleClass().add("timeline-node-label");

            Pane timelinePane = new Pane();
            timelinePane.setPrefHeight(rowHeight);
            HBox.setHgrow(timelinePane, Priority.ALWAYS);

            double barX = entry.startPercent() * timelineWidth;
            double barWidth = Math.max(2, entry.widthPercent() * timelineWidth);

            Rectangle bar = new Rectangle(barX, 0, barWidth, rowHeight);
            bar.getStyleClass().add("timeline-bar");
            bar.getStyleClass().add(ExecutionHistoryViewModel.getStatusClassName(entry.status()));

            Tooltip tooltip = new Tooltip(String.format(
                    "%s%nStart: %dms%nDuration: %dms",
                    entry.nodeName(),
                    entry.startOffsetMs(),
                    entry.durationMs()));
            Tooltip.install(bar, tooltip);

            timelinePane.getChildren().add(bar);
            row.getChildren().addAll(nameLabel, timelinePane);
            timelineContainer.getChildren().add(row);
        }
    }

    private String formatData(Object data) {
        if (data == null) {
            return "null";
        }
        if (data instanceof java.util.Map || data instanceof java.util.List) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                return mapper.writeValueAsString(data);
            } catch (Exception _) {
                return data.toString();
            }
        }
        return data.toString();
    }

    // ===== FXML Actions =====

    @FXML
    private void onClose() {
        viewModel.hide();
    }

    @FXML
    private void onRefresh() {
        viewModel.refresh();
    }

    @FXML
    private void onClearAll() {
        viewModel.clearAll();
    }

    // ===== Public API =====

    /**
     * Gets the ViewModel associated with this component.
     * 
     * @return the ViewModel
     */
    public ExecutionHistoryViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Show the panel.
     */
    public void show() {
        viewModel.show();
    }

    /**
     * Hide the panel.
     */
    public void hide() {
        viewModel.hide();
    }

    /**
     * Toggle visibility.
     */
    public void toggle() {
        viewModel.toggle();
    }

    /**
     * Add an execution to the history.
     * 
     * @param execution the execution to add
     */
    public void addExecution(Execution execution) {
        viewModel.addExecution(execution);
    }

    /**
     * Set the refresh callback.
     * 
     * @param callback the callback to run when refresh is requested
     */
    public void setOnRefresh(Runnable callback) {
        viewModel.setOnRefresh(callback);
    }

    /**
     * Set the clear all callback.
     * 
     * @param callback the callback to run when clear all is requested
     */
    public void setOnClearAll(Runnable callback) {
        viewModel.setOnClearAll(callback);
    }

    /**
     * Set the close callback.
     * 
     * @param callback the callback to run when the panel is closed
     */
    public void setOnClose(Runnable callback) {
        viewModel.setOnClose(callback);
    }

    /**
     * Set the highlight execution callback.
     * 
     * @param callback the callback to run when an execution should be highlighted
     */
    public void setOnHighlightExecution(java.util.function.Consumer<Execution> callback) {
        viewModel.setOnHighlightExecution(callback);
    }
}
