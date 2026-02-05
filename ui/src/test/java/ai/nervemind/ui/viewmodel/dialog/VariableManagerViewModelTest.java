/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.common.service.VariableServiceInterface;

/**
 * Unit tests for {@link VariableManagerViewModel}.
 * Tests all ViewModel logic without requiring JavaFX runtime.
 */
@ExtendWith(MockitoExtension.class)
class VariableManagerViewModelTest {

    @Mock
    private VariableServiceInterface variableService;

    private VariableManagerViewModel viewModel;

    // Test data
    private VariableDTO globalVar;
    private VariableDTO workflowVar;
    private VariableDTO secretVar;

    @BeforeEach
    void setUp() {
        globalVar = new VariableDTO(
                1L,
                "API_KEY",
                "test-api-key",
                VariableType.STRING,
                VariableScope.GLOBAL,
                null,
                "API key for testing",
                Instant.now(),
                Instant.now());

        workflowVar = new VariableDTO(
                2L,
                "WORKFLOW_VAR",
                "workflow-value",
                VariableType.STRING,
                VariableScope.WORKFLOW,
                100L,
                "Workflow-specific variable",
                Instant.now(),
                Instant.now());

        secretVar = new VariableDTO(
                3L,
                "SECRET_TOKEN",
                "secret-value",
                VariableType.SECRET,
                VariableScope.GLOBAL,
                null,
                "Secret token",
                Instant.now(),
                Instant.now());
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with empty list")
        void shouldInitializeWithEmptyList() {
            viewModel = new VariableManagerViewModel(variableService, null);

            assertTrue(viewModel.getAllVariables().isEmpty());
            assertTrue(viewModel.getFilteredVariables().isEmpty());
            assertNull(viewModel.getSelectedVariable());
            assertNull(viewModel.getScopeFilter());
            assertFalse(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should store workflow ID")
        void shouldStoreWorkflowId() {
            viewModel = new VariableManagerViewModel(variableService, 123L);

            assertEquals(123L, viewModel.getWorkflowId());
        }

        @Test
        @DisplayName("Should handle null workflow ID for global variables")
        void shouldHandleNullWorkflowId() {
            viewModel = new VariableManagerViewModel(variableService, null);

            assertNull(viewModel.getWorkflowId());
        }
    }

    @Nested
    @DisplayName("Refresh Tests")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh global variables when workflowId is null")
        void shouldRefreshGlobalVariablesOnly() {
            when(variableService.findGlobalVariables()).thenReturn(List.of(globalVar, secretVar));

            viewModel = new VariableManagerViewModel(variableService, null);
            viewModel.refreshVariables();

            assertEquals(2, viewModel.getAllVariables().size());
            verify(variableService).findGlobalVariables();
            verify(variableService, never()).findByWorkflowId(anyLong());
        }

        @Test
        @DisplayName("Should refresh workflow and global variables when workflowId is set")
        void shouldRefreshWorkflowAndGlobalVariables() {
            when(variableService.findByWorkflowId(100L)).thenReturn(List.of(workflowVar));
            when(variableService.findGlobalVariables()).thenReturn(List.of(globalVar));

            viewModel = new VariableManagerViewModel(variableService, 100L);
            viewModel.refreshVariables();

            assertEquals(2, viewModel.getAllVariables().size());
            verify(variableService).findByWorkflowId(100L);
            verify(variableService).findGlobalVariables();
        }

        @Test
        @DisplayName("Should handle service exception on refresh")
        void shouldHandleServiceExceptionOnRefresh() {
            when(variableService.findGlobalVariables())
                    .thenThrow(new RuntimeException("Database error"));

            viewModel = new VariableManagerViewModel(variableService, null);
            viewModel.refreshVariables();

            assertTrue(viewModel.getAllVariables().isEmpty());
            assertTrue(viewModel.getErrorMessage().contains("Failed to load variables"));
        }
    }

    @Nested
    @DisplayName("Filtering Tests")
    class FilteringTests {

        @BeforeEach
        void setUpWithVariables() {
            when(variableService.findByWorkflowId(100L)).thenReturn(List.of(workflowVar));
            when(variableService.findGlobalVariables()).thenReturn(List.of(globalVar, secretVar));

            viewModel = new VariableManagerViewModel(variableService, 100L);
            viewModel.refreshVariables();
        }

        @Test
        @DisplayName("Should show all variables when filter is null")
        void shouldShowAllVariablesWhenNoFilter() {
            viewModel.setScopeFilter(null);

            assertEquals(3, viewModel.getFilteredVariables().size());
        }

        @Test
        @DisplayName("Should filter by GLOBAL scope")
        void shouldFilterByGlobalScope() {
            viewModel.setScopeFilter(VariableScope.GLOBAL);

            assertEquals(2, viewModel.getFilteredVariables().size());
            assertTrue(viewModel.getFilteredVariables().stream()
                    .allMatch(v -> v.scope() == VariableScope.GLOBAL));
        }

        @Test
        @DisplayName("Should filter by WORKFLOW scope")
        void shouldFilterByWorkflowScope() {
            viewModel.setScopeFilter(VariableScope.WORKFLOW);

            assertEquals(1, viewModel.getFilteredVariables().size());
            assertEquals("WORKFLOW_VAR", viewModel.getFilteredVariables().get(0).name());
        }

        @Test
        @DisplayName("Should return empty when filtering by unused scope")
        void shouldReturnEmptyForUnusedScope() {
            viewModel.setScopeFilter(VariableScope.EXECUTION);

            assertTrue(viewModel.getFilteredVariables().isEmpty());
        }
    }

    @Nested
    @DisplayName("Selection Tests")
    class SelectionTests {

        @BeforeEach
        void setUpWithVariables() {
            when(variableService.findGlobalVariables()).thenReturn(List.of(globalVar));
            viewModel = new VariableManagerViewModel(variableService, null);
            viewModel.refreshVariables();
        }

        @Test
        @DisplayName("Should track selection state")
        void shouldTrackSelectionState() {
            assertFalse(viewModel.hasSelection());

            viewModel.setSelectedVariable(globalVar);

            assertTrue(viewModel.hasSelection());
            assertEquals(globalVar, viewModel.getSelectedVariable());
        }

        @Test
        @DisplayName("Should clear selection when set to null")
        void shouldClearSelection() {
            viewModel.setSelectedVariable(globalVar);
            viewModel.setSelectedVariable(null);

            assertFalse(viewModel.hasSelection());
            assertNull(viewModel.getSelectedVariable());
        }
    }

    @Nested
    @DisplayName("CRUD Operation Tests")
    class CrudOperationTests {

        @BeforeEach
        void setUpWithVariables() {
            when(variableService.findGlobalVariables()).thenReturn(new ArrayList<>(List.of(globalVar)));
            viewModel = new VariableManagerViewModel(variableService, null);
            viewModel.refreshVariables();
        }

        @Test
        @DisplayName("Should add new variable")
        void shouldAddNewVariable() {
            VariableDTO newVar = new VariableDTO(
                    null, "NEW_VAR", "new-value", VariableType.STRING,
                    VariableScope.GLOBAL, null, "New variable", null, null);
            VariableDTO createdVar = new VariableDTO(
                    10L, "NEW_VAR", "new-value", VariableType.STRING,
                    VariableScope.GLOBAL, null, "New variable", Instant.now(), Instant.now());

            when(variableService.create(newVar)).thenReturn(createdVar);

            boolean result = viewModel.addVariable(newVar);

            assertTrue(result);
            assertEquals(2, viewModel.getAllVariables().size());
            assertTrue(viewModel.isDirty());
            verify(variableService).create(newVar);
        }

        @Test
        @DisplayName("Should handle add variable failure")
        void shouldHandleAddVariableFailure() {
            VariableDTO newVar = new VariableDTO(
                    null, "NEW_VAR", "new-value", VariableType.STRING,
                    VariableScope.GLOBAL, null, "New variable", null, null);

            when(variableService.create(newVar))
                    .thenThrow(new RuntimeException("Duplicate name"));

            boolean result = viewModel.addVariable(newVar);

            assertFalse(result);
            assertEquals(1, viewModel.getAllVariables().size()); // Original unchanged
            assertTrue(viewModel.getErrorMessage().contains("Failed to create variable"));
        }

        @Test
        @DisplayName("Should update existing variable")
        void shouldUpdateExistingVariable() {
            VariableDTO updatedVar = new VariableDTO(
                    1L, "API_KEY", "updated-value", VariableType.STRING,
                    VariableScope.GLOBAL, null, "Updated description", Instant.now(), Instant.now());

            when(variableService.update(1L, updatedVar)).thenReturn(updatedVar);

            boolean result = viewModel.updateVariable(updatedVar);

            assertTrue(result);
            assertEquals("updated-value", viewModel.getAllVariables().get(0).value());
            assertTrue(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should delete selected variable")
        void shouldDeleteSelectedVariable() {
            viewModel.setSelectedVariable(globalVar);

            boolean result = viewModel.deleteSelectedVariable();

            assertTrue(result);
            assertTrue(viewModel.getAllVariables().isEmpty());
            assertNull(viewModel.getSelectedVariable());
            assertFalse(viewModel.hasSelection());
            assertTrue(viewModel.isDirty());
            verify(variableService).delete(1L);
        }

        @Test
        @DisplayName("Should not delete when no selection")
        void shouldNotDeleteWhenNoSelection() {
            boolean result = viewModel.deleteSelectedVariable();

            assertFalse(result);
            assertEquals(1, viewModel.getAllVariables().size());
            verify(variableService, never()).delete(anyLong());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @BeforeEach
        void setUpWithVariables() {
            when(variableService.findGlobalVariables())
                    .thenReturn(new ArrayList<>(List.of(globalVar, secretVar)));
            viewModel = new VariableManagerViewModel(variableService, null);
            viewModel.refreshVariables();
        }

        @Test
        @DisplayName("Should detect duplicate name")
        void shouldDetectDuplicateName() {
            assertFalse(viewModel.isNameUnique("API_KEY", null));
            assertFalse(viewModel.isNameUnique("api_key", null)); // Case-insensitive
        }

        @Test
        @DisplayName("Should allow unique name")
        void shouldAllowUniqueName() {
            assertTrue(viewModel.isNameUnique("NEW_UNIQUE_VAR", null));
        }

        @Test
        @DisplayName("Should exclude current variable when checking duplicates")
        void shouldExcludeCurrentVariableWhenCheckingDuplicates() {
            // When updating a variable, its own name should not be considered a duplicate
            assertTrue(viewModel.isNameUnique("API_KEY", 1L));
            assertFalse(viewModel.isNameUnique("SECRET_TOKEN", 1L)); // But other names are
        }
    }

    @Nested
    @DisplayName("Static Helper Tests")
    class StaticHelperTests {

        @Test
        @DisplayName("Should format variable name with brackets")
        void shouldFormatVariableName() {
            assertEquals("${API_KEY}", VariableManagerViewModel.formatVariableName("API_KEY"));
            assertEquals("${test}", VariableManagerViewModel.formatVariableName("test"));
        }

        @Test
        @DisplayName("Should return colors for variable types")
        void shouldReturnColorsForVariableTypes() {
            assertNotNull(VariableManagerViewModel.getTypeColor(VariableType.STRING));
            assertNotNull(VariableManagerViewModel.getTypeColor(VariableType.NUMBER));
            assertNotNull(VariableManagerViewModel.getTypeColor(VariableType.BOOLEAN));
            assertNotNull(VariableManagerViewModel.getTypeColor(VariableType.JSON));
            assertNotNull(VariableManagerViewModel.getTypeColor(VariableType.SECRET));
        }

        @Test
        @DisplayName("Should return colors for variable scopes")
        void shouldReturnColorsForVariableScopes() {
            assertNotNull(VariableManagerViewModel.getScopeColor(VariableScope.GLOBAL));
            assertNotNull(VariableManagerViewModel.getScopeColor(VariableScope.WORKFLOW));
            assertNotNull(VariableManagerViewModel.getScopeColor(VariableScope.EXECUTION));
        }
    }
}
