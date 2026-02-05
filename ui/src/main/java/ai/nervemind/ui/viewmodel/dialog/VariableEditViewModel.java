/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for the Variable Edit dialog.
 * 
 * <p>
 * Manages the state for adding or editing a variable.
 */
public class VariableEditViewModel extends BaseDialogViewModel<VariableDTO> {

    private final VariableDTO existingVariable;
    private final Long workflowId;
    private final boolean editMode;

    // Form fields
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<VariableType> type = new SimpleObjectProperty<>(VariableType.STRING);
    private final ObjectProperty<VariableScope> scope = new SimpleObjectProperty<>(VariableScope.GLOBAL);
    private final StringProperty value = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");

    // State
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty scopeEditable = new SimpleBooleanProperty(true);

    /**
     * Creates a new VariableEditViewModel.
     * 
     * @param existing   the existing variable to edit, or null for a new variable
     * @param workflowId the workflow ID (for workflow-scoped variables)
     */
    public VariableEditViewModel(VariableDTO existing, Long workflowId) {
        this.existingVariable = existing;
        this.workflowId = workflowId;
        this.editMode = existing != null;

        if (existing != null) {
            name.set(existing.name());
            type.set(existing.type());
            scope.set(existing.scope());
            if (existing.type() != VariableType.SECRET) {
                value.set(existing.value());
            }
            description.set(existing.description() != null ? existing.description() : "");
            scopeEditable.set(false); // Can't change scope in edit mode
        }

        // Setup validation
        name.addListener((obs, oldVal, newVal) -> updateFormValid());
        value.addListener((obs, oldVal, newVal) -> updateFormValid());
        updateFormValid();
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

    public ObjectProperty<VariableType> typeProperty() {
        return type;
    }

    public VariableType getType() {
        return type.get();
    }

    public void setType(VariableType value) {
        type.set(value);
    }

    public ObjectProperty<VariableScope> scopeProperty() {
        return scope;
    }

    public VariableScope getScope() {
        return scope.get();
    }

    public void setScope(VariableScope value) {
        scope.set(value);
    }

    public StringProperty valueProperty() {
        return value;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String val) {
        value.set(val);
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

    public ReadOnlyBooleanProperty formValidProperty() {
        return formValid;
    }

    public boolean isFormValid() {
        return formValid.get();
    }

    public ReadOnlyBooleanProperty scopeEditableProperty() {
        return scopeEditable;
    }

    public boolean isScopeEditable() {
        return scopeEditable.get();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public VariableDTO getExistingVariable() {
        return existingVariable;
    }

    // ===== Validation =====

    private void updateFormValid() {
        boolean nameValid = name.get() != null && !name.get().trim().isEmpty();
        boolean valueValid = editMode || (value.get() != null && !value.get().isEmpty());
        formValid.set(nameValid && valueValid);
    }

    @Override
    public boolean validate() {
        boolean valid = isFormValid();
        setValid(valid);
        return valid;
    }

    @Override
    protected void buildResult() {
        VariableScope selectedScope = scope.get();
        Long varWorkflowId = selectedScope == VariableScope.WORKFLOW ? this.workflowId : null;
        String desc = description.get().trim();

        setResult(new VariableDTO(
                existingVariable != null ? existingVariable.id() : null,
                name.get().trim(),
                value.get(),
                type.get(),
                selectedScope,
                varWorkflowId,
                desc.isEmpty() ? null : desc,
                existingVariable != null ? existingVariable.createdAt() : null,
                null));
    }
}
