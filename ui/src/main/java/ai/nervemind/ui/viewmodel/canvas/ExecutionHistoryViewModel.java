/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ai.nervemind.common.domain.Execution;
import ai.nervemind.common.domain.Execution.NodeExecution;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the ExecutionHistoryPanel.
 *
 * <p>
 * Manages execution history state including:
 * <ul>
 * <li>List of executions with filtering</li>
 * <li>Selected execution details</li>
 * <li>Tab state for detail views</li>
 * <li>Timeline data for visualization</li>
 * </ul>
 *
 * <p>
 * <strong>IMPORTANT:</strong> This ViewModel only uses javafx.beans.* and
 * javafx.collections.*
 * imports. No javafx.scene.* classes are allowed to ensure testability.
 */
public class ExecutionHistoryViewModel extends BaseViewModel {

    private static final int MAX_HISTORY_SIZE = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // ===== Execution List =====
    private final ObservableList<Execution> executions = FXCollections.observableArrayList();
    private final ObjectProperty<Execution> selectedExecution = new SimpleObjectProperty<>();

    // ===== Panel State =====
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty detailsVisible = new SimpleBooleanProperty(false);
    private final StringProperty selectedTab = new SimpleStringProperty("Summary");

    // ===== Computed Properties =====
    private final SimpleIntegerProperty executionCount = new SimpleIntegerProperty(0);
    private final StringProperty executionCountText = new SimpleStringProperty("0 executions");
    private final BooleanProperty hasSelectedExecution = new SimpleBooleanProperty(false);
    private final BooleanProperty selectedHasError = new SimpleBooleanProperty(false);

    // ===== Node Executions for Selected =====
    private final ObservableList<NodeExecution> selectedNodeExecutions = FXCollections.observableArrayList();
    private final ObservableList<TimelineEntry> timelineEntries = FXCollections.observableArrayList();
    private final ObservableList<VariableEntry> variableEntries = FXCollections.observableArrayList();

    // ===== Callbacks =====
    private Runnable onRefresh;
    private Runnable onClearAll;
    private Runnable onClose;
    private java.util.function.Consumer<Execution> onHighlightExecution;
    private java.util.function.Consumer<Execution> onSelectionChanged;

    /**
     * Creates a new ExecutionHistoryViewModel.
     */
    public ExecutionHistoryViewModel() {
        // Bind execution count
        executionCount.bind(Bindings.size(executions));
        executionCountText.bind(executionCount.asString().concat(" executions"));

        // Track selection state
        selectedExecution.addListener((obs, oldVal, newVal) -> {
            hasSelectedExecution.set(newVal != null);
            detailsVisible.set(newVal != null);
            updateSelectedExecutionDetails(newVal);
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(newVal);
            }
        });
    }

    // ===== Execution List Properties =====

    /**
     * Gets the observable list of executions.
     * 
     * @return the list of executions
     */
    public ObservableList<Execution> getExecutions() {
        return executions;
    }

    /**
     * Gets the selected execution property.
     * 
     * @return the selected execution property
     */
    public ObjectProperty<Execution> selectedExecutionProperty() {
        return selectedExecution;
    }

    /**
     * Gets the currently selected execution.
     * 
     * @return the selected execution or null
     */
    public Execution getSelectedExecution() {
        return selectedExecution.get();
    }

    /**
     * Sets the selected execution.
     * 
     * @param execution the execution to select
     */
    public void setSelectedExecution(Execution execution) {
        selectedExecution.set(execution);
    }

    // ===== Panel State Properties =====

    /**
     * Gets the visible property.
     * 
     * @return the visible property
     */
    public BooleanProperty visibleProperty() {
        return visible;
    }

    /**
     * Checks if the panel is visible.
     * 
     * @return true if visible
     */
    public boolean isVisible() {
        return visible.get();
    }

    /**
     * Sets the panel visibility.
     * 
     * @param visible true to show
     */
    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    /**
     * Gets the details visible property.
     * 
     * @return the details visible property
     */
    public BooleanProperty detailsVisibleProperty() {
        return detailsVisible;
    }

    /**
     * Checks if details pane is visible.
     * 
     * @return true if visible
     */
    public boolean isDetailsVisible() {
        return detailsVisible.get();
    }

    /**
     * Gets the selected tab property.
     * 
     * @return the selected tab property
     */
    public StringProperty selectedTabProperty() {
        return selectedTab;
    }

    /**
     * Gets the currently selected tab name.
     * 
     * @return the selected tab name
     */
    public String getSelectedTab() {
        return selectedTab.get();
    }

    /**
     * Sets the selected tab.
     * 
     * @param tab the tab name to select
     */
    public void setSelectedTab(String tab) {
        selectedTab.set(tab);
    }

    // ===== Computed Properties =====

    /**
     * Gets the execution count property.
     * 
     * @return the execution count property
     */
    public ReadOnlyIntegerProperty executionCountProperty() {
        return executionCount;
    }

    /**
     * Gets the total number of executions.
     * 
     * @return the execution count
     */
    public int getExecutionCount() {
        return executionCount.get();
    }

    /**
     * Gets the execution count text property.
     * 
     * @return the execution count text property
     */
    public ReadOnlyStringProperty executionCountTextProperty() {
        return executionCountText;
    }

    /**
     * Gets the execution count display text.
     * 
     * @return the execution count text
     */
    public String getExecutionCountText() {
        return executionCountText.get();
    }

    /**
     * Gets the has selected execution property.
     * 
     * @return the has selected execution property
     */
    public BooleanProperty hasSelectedExecutionProperty() {
        return hasSelectedExecution;
    }

    /**
     * Checks if an execution is selected.
     * 
     * @return true if selected
     */
    public boolean hasSelectedExecution() {
        return hasSelectedExecution.get();
    }

    /**
     * Gets the selected has error property.
     * 
     * @return the selected has error property
     */
    public BooleanProperty selectedHasErrorProperty() {
        return selectedHasError;
    }

    /**
     * Checks if the selected execution has an error.
     * 
     * @return true if error exists
     */
    public boolean selectedHasError() {
        return selectedHasError.get();
    }

    // ===== Node Executions =====

    /**
     * Gets the node executions for the selected execution.
     * 
     * @return the list of node executions
     */
    public ObservableList<NodeExecution> getSelectedNodeExecutions() {
        return selectedNodeExecutions;
    }

    /**
     * Gets the timeline entries for visualization.
     * 
     * @return the list of timeline entries
     */
    public ObservableList<TimelineEntry> getTimelineEntries() {
        return timelineEntries;
    }

    /**
     * Gets the variable entries for the selected execution.
     * 
     * @return the list of variable entries
     */
    public ObservableList<VariableEntry> getVariableEntries() {
        return variableEntries;
    }

    // ===== Actions =====

    /**
     * Show the panel.
     */
    public void show() {
        visible.set(true);
    }

    /**
     * Hide the panel.
     */
    public void hide() {
        visible.set(false);
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Toggle panel visibility.
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Request execution list refresh.
     */
    public void refresh() {
        if (onRefresh != null) {
            onRefresh.run();
        }
    }

    /**
     * Clear all executions.
     */
    public void clearAll() {
        if (onClearAll != null) {
            onClearAll.run();
        } else {
            executions.clear();
            selectedExecution.set(null);
        }
    }

    /**
     * Add an execution to the history.
     * Maintains order with newest first.
     * 
     * @param execution the execution to add
     */
    public void addExecution(Execution execution) {
        if (execution != null) {
            executions.addFirst(execution);

            // Trim to max size
            while (executions.size() > MAX_HISTORY_SIZE) {
                executions.removeLast();
            }
        }
    }

    /**
     * Set the full list of executions.
     * Sorts by start time (newest first).
     * 
     * @param newExecutions the list of new executions
     */
    public void setExecutions(List<Execution> newExecutions) {
        executions.clear();
        if (newExecutions != null) {
            List<Execution> sorted = newExecutions.stream()
                    .sorted((a, b) -> b.startedAt().compareTo(a.startedAt()))
                    .toList();
            executions.addAll(sorted);
        }
    }

    /**
     * Highlight the selected execution on the canvas.
     */
    public void highlightSelectedExecution() {
        Execution selected = selectedExecution.get();
        if (selected != null && onHighlightExecution != null) {
            onHighlightExecution.accept(selected);
        }
    }

    /**
     * Clear the current selection.
     */
    public void clearSelection() {
        selectedExecution.set(null);
    }

    // ===== Callbacks =====

    /**
     * Sets the on-refresh callback.
     * 
     * @param callback the runnable to execute on refresh
     */
    public void setOnRefresh(Runnable callback) {
        this.onRefresh = callback;
    }

    /**
     * Sets the on-clear-all callback.
     * 
     * @param callback the runnable to execute on clear
     */
    public void setOnClearAll(Runnable callback) {
        this.onClearAll = callback;
    }

    /**
     * Sets the on-close callback.
     * 
     * @param callback the runnable to execute on close
     */
    public void setOnClose(Runnable callback) {
        this.onClose = callback;
    }

    /**
     * Sets the on-highlight-execution callback.
     * 
     * @param callback the consumer to execute for highlighting
     */
    public void setOnHighlightExecution(java.util.function.Consumer<Execution> callback) {
        this.onHighlightExecution = callback;
    }

    /**
     * Sets the on-selection-changed callback.
     * 
     * @param callback the consumer to execute on selection change
     */
    public void setOnSelectionChanged(java.util.function.Consumer<Execution> callback) {
        this.onSelectionChanged = callback;
    }

    // ===== Private Helpers =====

    private void updateSelectedExecutionDetails(Execution execution) {
        selectedNodeExecutions.clear();
        timelineEntries.clear();
        variableEntries.clear();

        if (execution == null) {
            selectedHasError.set(false);
            return;
        }

        selectedHasError.set(execution.status() == ExecutionStatus.FAILED
                && execution.errorMessage() != null);

        // Populate node executions
        if (execution.nodeExecutions() != null) {
            selectedNodeExecutions.addAll(execution.nodeExecutions());
            buildTimelineEntries(execution);
        }

        // Populate variables
        if (execution.outputData() != null) {
            execution.outputData().forEach((key, value) -> variableEntries.add(new VariableEntry(key, value)));
        }

        // Select error tab if there's an error
        if (selectedHasError.get()) {
            selectedTab.set("Error");
        } else {
            selectedTab.set("Summary");
        }
    }

    private void buildTimelineEntries(Execution execution) {
        List<NodeExecution> nodeExecutions = execution.nodeExecutions();
        if (nodeExecutions == null || nodeExecutions.isEmpty()) {
            return;
        }

        long totalDuration = execution.duration().toMillis();
        if (totalDuration == 0) {
            return;
        }

        for (NodeExecution nodeExec : nodeExecutions) {
            long nodeStartOffset = Duration.between(execution.startedAt(), nodeExec.startedAt()).toMillis();
            long nodeDuration = nodeExec.duration().toMillis();

            double startPercent = (double) nodeStartOffset / totalDuration;
            double widthPercent = (double) nodeDuration / totalDuration;

            timelineEntries.add(new TimelineEntry(
                    nodeExec.nodeName(),
                    nodeExec.status(),
                    startPercent,
                    widthPercent,
                    nodeStartOffset,
                    nodeDuration));
        }
    }

    // ===== Static Utility Methods =====

    /**
     * Format a timestamp for display.
     * 
     * @param time the timestamp to format
     * @return the formatted time string
     */
    public static String formatTime(Instant time) {
        if (time == null)
            return "-";
        return TIME_FORMATTER.format(time);
    }

    /**
     * Format a duration for display.
     * 
     * @param duration the duration to format
     * @return the formatted duration string
     */
    public static String formatDuration(Duration duration) {
        if (duration == null)
            return "-";

        long millis = duration.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            long mins = millis / 60000;
            long secs = (millis % 60000) / 1000;
            return String.format("%dm %ds", mins, secs);
        }
    }

    /**
     * Get status display class name.
     * 
     * @param status the execution status
     * @return the CSS class name
     */
    public static String getStatusClassName(ExecutionStatus status) {
        return "status-" + status.name().toLowerCase();
    }

    /**
     * Get status icon name.
     * 
     * @param status the execution status
     * @return the Ikonli icon literal
     */
    public static String getStatusIconName(ExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> "mdi2c-check-circle";
            case FAILED -> "mdi2a-alert-circle";
            case RUNNING -> "mdi2p-play-circle";
            case PENDING -> "mdi2c-clock-outline";
            case CANCELLED -> "mdi2c-cancel";
            case WAITING -> "mdi2p-pause-circle";
        };
    }

    // ===== Data Classes =====

    /**
     * Entry for timeline visualization.
     *
     * @param nodeName      The name of the node
     * @param status        The execution status of the node
     * @param startPercent  Horizontal start position (0.0 to 1.0)
     * @param widthPercent  Horizontal width (0.0 to 1.0)
     * @param startOffsetMs Offset from workflow start in ms
     * @param durationMs    Execution duration in ms
     */
    public record TimelineEntry(
            String nodeName,
            ExecutionStatus status,
            double startPercent,
            double widthPercent,
            long startOffsetMs,
            long durationMs) {
    }

    /**
     * Entry for variable display.
     *
     * @param name  The name of the variable
     * @param value The value of the variable
     */
    public record VariableEntry(
            String name,
            Object value) {
        /**
         * Gets the formatted value as string.
         * 
         * @return formatted value
         */
        public String getFormattedValue() {
            if (value == null)
                return "null";
            return value.toString();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        executions.clear();
        selectedNodeExecutions.clear();
        timelineEntries.clear();
        variableEntries.clear();
    }
}
