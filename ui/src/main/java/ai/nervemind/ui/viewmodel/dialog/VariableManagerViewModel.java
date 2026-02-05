/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.common.service.VariableServiceInterface;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * ViewModel for the Variable Manager dialog.
 * 
 * <p>
 * Manages the list of variables, filtering, and CRUD operations.
 * Does not depend on any JavaFX UI classes - only uses javafx.beans and
 * javafx.collections.
 */
public class VariableManagerViewModel extends BaseViewModel {

    private final VariableServiceInterface variableService;
    private final Long workflowId;

    private final ObservableList<VariableDTO> allVariables = FXCollections.observableArrayList();
    private final FilteredList<VariableDTO> filteredVariables;
    private final ObjectProperty<VariableDTO> selectedVariable = new SimpleObjectProperty<>();
    private final ObjectProperty<VariableScope> scopeFilter = new SimpleObjectProperty<>();
    private final BooleanProperty hasSelection = new SimpleBooleanProperty(false);

    /**
     * Creates a new VariableManagerViewModel.
     * 
     * @param variableService the service for variable operations
     * @param workflowId      the workflow ID (null for global variables only)
     */
    public VariableManagerViewModel(VariableServiceInterface variableService, Long workflowId) {
        this.variableService = variableService;
        this.workflowId = workflowId;

        // Setup filtered list
        this.filteredVariables = new FilteredList<>(allVariables);

        // Update filter when scope changes
        scopeFilter.addListener((obs, oldVal, newVal) -> updateFilter());

        // Track selection
        selectedVariable.addListener((obs, oldVal, newVal) -> hasSelection.set(newVal != null));
    }

    // ===== Properties =====

    /**
     * All variables (unfiltered).
     */
    public ObservableList<VariableDTO> getAllVariables() {
        return allVariables;
    }

    /**
     * Filtered list of variables based on scope filter.
     */
    public FilteredList<VariableDTO> getFilteredVariables() {
        return filteredVariables;
    }

    /**
     * Currently selected variable.
     */
    public ObjectProperty<VariableDTO> selectedVariableProperty() {
        return selectedVariable;
    }

    public VariableDTO getSelectedVariable() {
        return selectedVariable.get();
    }

    public void setSelectedVariable(VariableDTO variable) {
        selectedVariable.set(variable);
    }

    /**
     * Current scope filter.
     */
    public ObjectProperty<VariableScope> scopeFilterProperty() {
        return scopeFilter;
    }

    public VariableScope getScopeFilter() {
        return scopeFilter.get();
    }

    public void setScopeFilter(VariableScope scope) {
        scopeFilter.set(scope);
    }

    /**
     * Whether a variable is currently selected.
     */
    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection;
    }

    public boolean hasSelection() {
        return hasSelection.get();
    }

    /**
     * The workflow ID (null for global variables).
     */
    public Long getWorkflowId() {
        return workflowId;
    }

    // ===== Actions =====

    /**
     * Refresh the variable list from the service.
     */
    public void refreshVariables() {
        setLoading(true);
        try {
            List<VariableDTO> vars;
            if (workflowId != null) {
                vars = new java.util.ArrayList<>(variableService.findByWorkflowId(workflowId));
                // Also include global variables
                vars.addAll(variableService.findGlobalVariables());
            } else {
                vars = variableService.findGlobalVariables();
            }
            allVariables.setAll(vars);
            clearError();
        } catch (Exception e) {
            setErrorMessage("Failed to load variables: " + e.getMessage());
        } finally {
            setLoading(false);
        }
    }

    /**
     * Add a new variable.
     * 
     * @param variable the variable to add
     * @return true if successful
     */
    public boolean addVariable(VariableDTO variable) {
        try {
            VariableDTO created = variableService.create(variable);
            allVariables.add(created);
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to create variable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update an existing variable.
     * 
     * @param variable the variable to update
     * @return true if successful
     */
    public boolean updateVariable(VariableDTO variable) {
        try {
            VariableDTO updated = variableService.update(variable.id(), variable);
            int index = findVariableIndex(variable.id());
            if (index >= 0) {
                allVariables.set(index, updated);
            }
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to update variable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete the currently selected variable.
     * 
     * @return true if successful
     */
    public boolean deleteSelectedVariable() {
        VariableDTO selected = selectedVariable.get();
        if (selected == null) {
            return false;
        }

        try {
            variableService.delete(selected.id());
            allVariables.remove(selected);
            selectedVariable.set(null);
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to delete variable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a variable name is unique.
     * 
     * @param name      the name to check
     * @param excludeId ID to exclude (for updates)
     * @return true if the name is unique
     */
    public boolean isNameUnique(String name, Long excludeId) {
        return allVariables.stream()
                .filter(v -> !v.id().equals(excludeId))
                .noneMatch(v -> v.name().equalsIgnoreCase(name));
    }

    // ===== Private Methods =====

    private void updateFilter() {
        VariableScope filter = scopeFilter.get();
        if (filter == null) {
            filteredVariables.setPredicate(v -> true);
        } else {
            filteredVariables.setPredicate(v -> v.scope() == filter);
        }
    }

    private int findVariableIndex(Long id) {
        for (int i = 0; i < allVariables.size(); i++) {
            if (allVariables.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // ===== Static Helpers =====

    /**
     * Get display color for a variable type.
     */
    public static String getTypeColor(VariableType type) {
        return switch (type) {
            case STRING -> "#3b82f6";
            case NUMBER -> "#22c55e";
            case BOOLEAN -> "#f59e0b";
            case JSON -> "#8b5cf6";
            case SECRET -> "#ef4444";
        };
    }

    /**
     * Get display color for a variable scope.
     */
    public static String getScopeColor(VariableScope scope) {
        return switch (scope) {
            case GLOBAL -> "#3b82f6";
            case WORKFLOW -> "#22c55e";
            case EXECUTION -> "#f59e0b";
        };
    }

    /**
     * Format variable name for display.
     */
    public static String formatVariableName(String name) {
        return "${" + name + "}";
    }
}
