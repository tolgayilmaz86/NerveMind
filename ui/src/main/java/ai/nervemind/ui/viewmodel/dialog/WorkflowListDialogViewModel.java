/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Workflow List dialog.
 * 
 * <p>
 * Manages the list of workflows, selection, and details display.
 */
public class WorkflowListDialogViewModel extends BaseDialogViewModel<WorkflowDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<WorkflowDTO> workflows = FXCollections.observableArrayList();
    private final ObjectProperty<WorkflowDTO> selectedWorkflow = new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper detailsText = new ReadOnlyStringWrapper("Select a workflow to see details");
    private final BooleanProperty hasSelection = new SimpleBooleanProperty(false);
    private final BooleanProperty showDeleteButton = new SimpleBooleanProperty(false);

    /** Callback for when delete is requested for a workflow. */
    private Consumer<WorkflowDTO> onDeleteRequested;

    /**
     * Creates a new WorkflowListDialogViewModel with the given workflows.
     * 
     * @param workflowList the list of workflows to display
     */
    public WorkflowListDialogViewModel(List<WorkflowDTO> workflowList) {
        this.workflows.setAll(workflowList);

        // Update hasSelection and details when selection changes
        selectedWorkflow.addListener((obs, oldVal, newVal) -> {
            hasSelection.set(newVal != null);
            updateDetailsText(newVal);
        });
    }

    // ===== Properties =====

    public ObservableList<WorkflowDTO> getWorkflows() {
        return workflows;
    }

    public ObjectProperty<WorkflowDTO> selectedWorkflowProperty() {
        return selectedWorkflow;
    }

    public WorkflowDTO getSelectedWorkflow() {
        return selectedWorkflow.get();
    }

    public void setSelectedWorkflow(WorkflowDTO workflow) {
        selectedWorkflow.set(workflow);
    }

    public ReadOnlyStringProperty detailsTextProperty() {
        return detailsText.getReadOnlyProperty();
    }

    public String getDetailsText() {
        return detailsText.get();
    }

    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection;
    }

    public boolean hasSelection() {
        return hasSelection.get();
    }

    public BooleanProperty showDeleteButtonProperty() {
        return showDeleteButton;
    }

    public boolean isShowDeleteButton() {
        return showDeleteButton.get();
    }

    public void setShowDeleteButton(boolean show) {
        showDeleteButton.set(show);
    }

    /**
     * Set the callback to invoke when delete is requested for a workflow.
     * 
     * @param callback the callback to invoke with the workflow to delete
     */
    public void setOnDeleteRequested(Consumer<WorkflowDTO> callback) {
        this.onDeleteRequested = callback;
        this.showDeleteButton.set(callback != null);
    }

    // ===== Actions =====

    /**
     * Delete the currently selected workflow from the list.
     * 
     * @return the deleted workflow, or null if nothing was selected
     */
    public WorkflowDTO deleteSelectedWorkflow() {
        WorkflowDTO selected = selectedWorkflow.get();
        if (selected != null) {
            // Notify external listener first (e.g., to delete from database)
            if (onDeleteRequested != null) {
                onDeleteRequested.accept(selected);
            }
            workflows.remove(selected);
            selectedWorkflow.set(null);
            markDirty();
        }
        return selected;
    }

    /**
     * Check if the workflows list is empty.
     * 
     * @return true if no workflows exist
     */
    public boolean isEmpty() {
        return workflows.isEmpty();
    }

    // ===== Validation & Result =====

    @Override
    public boolean validate() {
        boolean isValid = selectedWorkflow.get() != null;
        setValid(isValid);
        return isValid;
    }

    @Override
    protected void buildResult() {
        setResult(selectedWorkflow.get());
    }

    // ===== Private Methods =====

    private void updateDetailsText(WorkflowDTO workflow) {
        if (workflow == null) {
            detailsText.set("Select a workflow to see details");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(workflow.name()).append("\n\n");

        if (workflow.description() != null && !workflow.description().isBlank()) {
            sb.append("Description:\n").append(workflow.description()).append("\n\n");
        }

        sb.append("Trigger: ").append(workflow.triggerType()).append("\n");
        sb.append("Nodes: ").append(workflow.nodes().size()).append("\n");
        sb.append("Connections: ").append(workflow.connections().size()).append("\n");
        sb.append("Active: ").append(workflow.isActive() ? "Yes" : "No").append("\n\n");

        appendTimestamp(sb, "Created: ", workflow.createdAt());
        appendTimestamp(sb, "Updated: ", workflow.updatedAt());
        appendTimestamp(sb, "Last Run: ", workflow.lastExecuted());

        detailsText.set(sb.toString());
    }

    private void appendTimestamp(StringBuilder sb, String label, Instant timestamp) {
        if (timestamp != null) {
            sb.append(label)
                    .append(DATE_FORMAT.format(timestamp.atZone(ZoneId.systemDefault())))
                    .append("\n");
        }
    }

    // ===== List Item Display Helpers =====

    /**
     * Get the display name for a workflow.
     */
    public static String getDisplayName(WorkflowDTO workflow) {
        return workflow.name();
    }

    /**
     * Get the info text for a workflow (shown below name in list).
     */
    public static String getInfoText(WorkflowDTO workflow) {
        StringBuilder sb = new StringBuilder();
        sb.append(workflow.nodes().size()).append(" nodes");
        sb.append(" • ").append(workflow.triggerType());
        if (workflow.updatedAt() != null) {
            sb.append(" • Updated: ")
                    .append(DATE_FORMAT.format(workflow.updatedAt().atZone(ZoneId.systemDefault())));
        }
        return sb.toString();
    }
}
