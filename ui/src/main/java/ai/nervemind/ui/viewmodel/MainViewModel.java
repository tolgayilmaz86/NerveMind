/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the main application view.
 * 
 * <p>
 * Manages global application state including:
 * <ul>
 * <li>Current view/navigation state</li>
 * <li>Status bar messages</li>
 * <li>Active workflow state</li>
 * <li>Grid and zoom settings</li>
 * <li>Execution history</li>
 * </ul>
 * 
 * <p>
 * This ViewModel coordinates between the main UI components and services,
 * providing a clean separation of concerns.
 */
public class MainViewModel extends BaseViewModel {

    /**
     * Enum representing the main navigation views.
     */
    public enum NavigationView {
        /** The workflows view. */
        WORKFLOWS("Workflows"),
        /** The executions view. */
        EXECUTIONS("Executions"),
        /** The execution console view. */
        EXECUTION_CONSOLE("Execution Console"),
        /** The credentials view. */
        CREDENTIALS("Credentials"),
        /** The plugins view. */
        PLUGINS("Plugins"),
        /** The settings view. */
        SETTINGS("Settings");

        private final String displayName;

        NavigationView(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Gets the display name for this navigation view.
         *
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    // Services
    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;

    // Navigation state
    private final ObjectProperty<NavigationView> currentView = new SimpleObjectProperty<>(NavigationView.WORKFLOWS);
    private final ObjectProperty<NavigationView> previousView = new SimpleObjectProperty<>(NavigationView.WORKFLOWS);

    // Status bar
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final StringProperty applicationVersion = new SimpleStringProperty("NerveMind v0.1.0");

    // Active workflow
    private final ObjectProperty<WorkflowDTO> activeWorkflow = new SimpleObjectProperty<>();
    private final BooleanProperty workflowDirty = new SimpleBooleanProperty(false);
    private final StringProperty activeWorkflowName = new SimpleStringProperty();

    // Canvas settings
    private final BooleanProperty showGrid = new SimpleBooleanProperty(true);
    private final BooleanProperty snapToGrid = new SimpleBooleanProperty(true);
    private final ObjectProperty<Double> zoomLevel = new SimpleObjectProperty<>(1.0);

    // Execution data
    private final ObservableList<ExecutionDTO> executions = FXCollections.observableArrayList();
    private final ObjectProperty<ExecutionDTO> selectedExecution = new SimpleObjectProperty<>();
    private final BooleanProperty executionsLoading = new SimpleBooleanProperty(false);

    // Date formatter
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss");

    /**
     * Creates a new MainViewModel with the required services.
     *
     * @param workflowService  the workflow service
     * @param executionService the execution service
     */
    public MainViewModel(WorkflowServiceInterface workflowService,
            ExecutionServiceInterface executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;

        // Update status when view changes
        currentView.addListener((obs, oldView, newView) -> {
            if (newView != null) {
                previousView.set(oldView);
                updateStatus(newView.getDisplayName());
            }
        });

        // Sync active workflow name
        activeWorkflow.addListener((obs, oldWf, newWf) -> {
            if (newWf != null) {
                activeWorkflowName.set(newWf.name());
            } else {
                activeWorkflowName.set(null);
            }
        });
    }

    /**
     * Creates a MainViewModel with null services (for testing).
     */
    public MainViewModel() {
        this(null, null);
    }

    // ========== Navigation ==========

    /**
     * The current navigation view.
     *
     * @return the current view property
     */
    public ObjectProperty<NavigationView> currentViewProperty() {
        return currentView;
    }

    public NavigationView getCurrentView() {
        return currentView.get();
    }

    public void setCurrentView(NavigationView view) {
        currentView.set(view);
    }

    /**
     * The previous navigation view (for returning after dialogs).
     *
     * @return the previous view property
     */
    public ReadOnlyObjectProperty<NavigationView> previousViewProperty() {
        return previousView;
    }

    /**
     * Gets the previous navigation view.
     *
     * @return the previous view
     */
    public NavigationView getPreviousView() {
        return previousView.get();
    }

    /**
     * Navigates to the specified view.
     *
     * @param view the view to navigate to
     */
    public void navigateTo(NavigationView view) {
        if (view != getCurrentView()) {
            setCurrentView(view);
        }
    }

    /**
     * Returns to the previous view.
     */
    public void navigateBack() {
        NavigationView prev = getPreviousView();
        if (prev != null && prev != getCurrentView()) {
            setCurrentView(prev);
        } else {
            setCurrentView(NavigationView.WORKFLOWS);
        }
    }

    // ========== Status Bar ==========

    /**
     * The current status message.
     *
     * @return the status message property
     */
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    /**
     * Updates the status message.
     *
     * @param message the new status message
     */
    public void updateStatus(String message) {
        statusMessage.set(message);
    }

    /**
     * Updates the status with zoom percentage.
     *
     * @param percentage the zoom percentage
     */
    public void updateZoomStatus(int percentage) {
        updateStatus("Zoom: " + percentage + "%");
    }

    /**
     * The application version string.
     *
     * @return the application version property
     */
    public ReadOnlyStringProperty applicationVersionProperty() {
        return applicationVersion;
    }

    public String getApplicationVersion() {
        return applicationVersion.get();
    }

    // ========== Active Workflow ==========

    /**
     * The currently active workflow.
     *
     * @return the active workflow property
     */
    public ObjectProperty<WorkflowDTO> activeWorkflowProperty() {
        return activeWorkflow;
    }

    public WorkflowDTO getActiveWorkflow() {
        return activeWorkflow.get();
    }

    public void setActiveWorkflow(WorkflowDTO workflow) {
        activeWorkflow.set(workflow);
        if (workflow != null) {
            updateStatus("Loaded: " + workflow.name());
        }
    }

    /**
     * The name of the active workflow.
     *
     * @return the active workflow name property
     */
    public ReadOnlyStringProperty activeWorkflowNameProperty() {
        return activeWorkflowName;
    }

    public String getActiveWorkflowName() {
        return activeWorkflowName.get();
    }

    /**
     * Whether the active workflow has unsaved changes.
     *
     * @return the workflow dirty property
     */
    public BooleanProperty workflowDirtyProperty() {
        return workflowDirty;
    }

    public boolean isWorkflowDirty() {
        return workflowDirty.get();
    }

    public void setWorkflowDirty(boolean dirty) {
        workflowDirty.set(dirty);
    }

    /**
     * Checks if there's an active workflow.
     *
     * @return true if a workflow is currently loaded
     */
    public boolean hasActiveWorkflow() {
        return activeWorkflow.get() != null;
    }

    // ========== Canvas Settings ==========

    /**
     * Whether to show the canvas grid.
     *
     * @return the show grid property
     */
    public BooleanProperty showGridProperty() {
        return showGrid;
    }

    public boolean isShowGrid() {
        return showGrid.get();
    }

    public void setShowGrid(boolean show) {
        showGrid.set(show);
        updateStatus("Grid: " + (show ? "Shown" : "Hidden"));
    }

    /**
     * Whether to snap to grid.
     *
     * @return the snap to grid property
     */
    public BooleanProperty snapToGridProperty() {
        return snapToGrid;
    }

    public boolean isSnapToGrid() {
        return snapToGrid.get();
    }

    public void setSnapToGrid(boolean snap) {
        snapToGrid.set(snap);
        updateStatus("Snap to Grid: " + (snap ? "Enabled" : "Disabled"));
    }

    /**
     * The current zoom level (1.0 = 100%).
     *
     * @return the zoom level property
     */
    public ObjectProperty<Double> zoomLevelProperty() {
        return zoomLevel;
    }

    public Double getZoomLevel() {
        return zoomLevel.get();
    }

    public void setZoomLevel(Double level) {
        zoomLevel.set(level);
        updateZoomStatus((int) (level * 100));
    }

    // ========== Execution Data ==========

    /**
     * The list of workflow executions.
     *
     * @return the executions list
     */
    public ObservableList<ExecutionDTO> getExecutions() {
        return executions;
    }

    /**
     * The currently selected execution.
     *
     * @return the selected execution property
     */
    public ObjectProperty<ExecutionDTO> selectedExecutionProperty() {
        return selectedExecution;
    }

    public ExecutionDTO getSelectedExecution() {
        return selectedExecution.get();
    }

    public void setSelectedExecution(ExecutionDTO execution) {
        selectedExecution.set(execution);
    }

    /**
     * Whether executions are currently loading.
     *
     * @return the executions loading property
     */
    public ReadOnlyBooleanProperty executionsLoadingProperty() {
        return executionsLoading;
    }

    public boolean isExecutionsLoading() {
        return executionsLoading.get();
    }

    /**
     * Loads the list of executions from the service.
     */
    public void loadExecutions() {
        if (executionService == null) {
            return;
        }

        executionsLoading.set(true);
        clearError();

        try {
            List<ExecutionDTO> loaded = executionService.findAll();
            executions.clear();
            executions.addAll(loaded);
        } catch (Exception e) {
            setErrorMessage("Failed to load executions: " + e.getMessage());
        } finally {
            executionsLoading.set(false);
        }
    }

    /**
     * Refreshes the executions list.
     */
    public void refreshExecutions() {
        loadExecutions();
    }

    /**
     * Clears all execution history.
     */
    public void clearExecutionHistory() {
        if (executionService == null) {
            return;
        }
        try {
            executionService.deleteAll();
            executions.clear();
            selectedExecution.set(null);
        } catch (Exception e) {
            setErrorMessage("Failed to clear execution history: " + e.getMessage());
        }
    }

    // ========== Workflow Operations ==========

    /**
     * Gets all workflows from the service.
     *
     * @return list of all workflows
     */
    public List<WorkflowDTO> getAllWorkflows() {
        if (workflowService == null) {
            return List.of();
        }
        return workflowService.findAll();
    }

    /**
     * Deletes a workflow.
     *
     * @param workflowId the workflow ID to delete
     * @throws RuntimeException if deletion fails
     */
    public void deleteWorkflow(Long workflowId) {
        if (workflowService == null) {
            throw new IllegalStateException("Workflow service not available");
        }
        workflowService.delete(workflowId);
        updateStatus("Deleted workflow");
    }

    /**
     * Gets a workflow by ID.
     *
     * @param id the workflow ID
     * @return the workflow, or empty if not found
     */
    public Optional<WorkflowDTO> getWorkflow(Long id) {
        if (workflowService == null) {
            return Optional.empty();
        }
        return workflowService.findById(id);
    }

    // ========== Formatting Helpers ==========

    /**
     * Formats an instant for display in the UI.
     *
     * @param instant the instant to format
     * @return formatted string, or "N/A" if null
     */
    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return DATE_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Formats an instant with time for display.
     *
     * @param instant the instant to format
     * @return formatted string with seconds, or "N/A" if null
     */
    public static String formatInstantWithTime(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        return DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    /**
     * Formats a duration in milliseconds for display.
     *
     * @param durationMs the duration in milliseconds
     * @return formatted string, or "N/A" if null
     */
    public static String formatDuration(Long durationMs) {
        if (durationMs == null) {
            return "N/A";
        }
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        return String.format("%.2fs", durationMs / 1000.0);
    }

    /**
     * Formats a duration for table display (shorter format).
     *
     * @param durationMs the duration in milliseconds
     * @return formatted string for tables
     */
    public static String formatDurationShort(Long durationMs) {
        if (durationMs == null) {
            return "N/A";
        }
        if (durationMs < 1000) {
            return durationMs + "ms";
        }
        return String.format("%.1fs", durationMs / 1000.0);
    }

    // ========== Lifecycle ==========

    @Override
    public void initialize() {
        // Load initial data
        if (executionService != null) {
            loadExecutions();
        }
        updateStatus("Ready");
    }

    @Override
    public void dispose() {
        executions.clear();
        selectedExecution.set(null);
        activeWorkflow.set(null);
    }
}
