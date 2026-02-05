/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for the Workflow Settings dialog.
 * 
 * <p>
 * Manages the workflow name, description, and active state.
 */
public class WorkflowSettingsViewModel extends BaseDialogViewModel<WorkflowSettingsViewModel.WorkflowSettings> {

    // Form fields
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    /**
     * Creates a new WorkflowSettingsViewModel.
     * 
     * @param currentName        the current workflow name
     * @param currentDescription the current workflow description
     * @param isActive           whether the workflow is active
     */
    public WorkflowSettingsViewModel(String currentName, String currentDescription, boolean isActive) {
        name.set(currentName != null ? currentName : "");
        description.set(currentDescription != null ? currentDescription : "");
        active.set(isActive);
    }

    // ===== Properties =====

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String value) {
        description.set(value);
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean value) {
        active.set(value);
    }

    // ===== Validation =====

    @Override
    public boolean validate() {
        // Name can be empty - will default to "Untitled Workflow"
        setValid(true);
        return true;
    }

    @Override
    protected void buildResult() {
        String finalName = name.get().trim();
        if (finalName.isEmpty()) {
            finalName = "Untitled Workflow";
        }
        setResult(new WorkflowSettings(
                finalName,
                description.get().trim(),
                active.get()));
    }

    /**
     * Record to hold workflow settings.
     *
     * @param name        The display name of the workflow
     * @param description Brief explanation of the workflow's purpose
     * @param isActive    Whether the workflow is enabled for execution (e.g.,
     *                    triggers are active)
     */
    public record WorkflowSettings(String name, String description, boolean isActive) {
    }
}
