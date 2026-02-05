/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for WorkflowListDialogViewModel.
 */
class WorkflowListDialogViewModelTest extends ViewModelTestBase {

    private WorkflowListDialogViewModel viewModel;
    private List<WorkflowDTO> testWorkflows;

    @BeforeEach
    void setUp() {
        testWorkflows = createTestWorkflows();
        viewModel = new WorkflowListDialogViewModel(testWorkflows);
    }

    private List<WorkflowDTO> createTestWorkflows() {
        WorkflowDTO workflow1 = new WorkflowDTO(
                1L, "Test Workflow 1", "Description 1",
                Collections.emptyList(), Collections.emptyList(), Map.of(),
                true, TriggerType.MANUAL, null,
                Instant.now(), Instant.now(), null, 1);

        WorkflowDTO workflow2 = new WorkflowDTO(
                2L, "Test Workflow 2", "Description 2",
                Collections.emptyList(), Collections.emptyList(), Map.of(),
                false, TriggerType.SCHEDULE, "0 0 * * *",
                Instant.now(), Instant.now(), null, 1);

        return List.of(workflow1, workflow2);
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("Should have workflows from constructor")
        void shouldHaveWorkflowsFromConstructor() {
            assertEquals(2, viewModel.getWorkflows().size());
        }

        @Test
        @DisplayName("Should have no selection initially")
        void shouldHaveNoSelectionInitially() {
            assertNull(viewModel.getSelectedWorkflow());
            assertFalse(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should show default details text")
        void shouldShowDefaultDetailsText() {
            assertEquals("Select a workflow to see details", viewModel.getDetailsText());
        }

        @Test
        @DisplayName("Should not show delete button by default")
        void shouldNotShowDeleteButtonByDefault() {
            assertFalse(viewModel.isShowDeleteButton());
        }
    }

    @Nested
    @DisplayName("Selection")
    class Selection {

        @Test
        @DisplayName("Should track selection state")
        void shouldTrackSelectionState() {
            WorkflowDTO workflow = testWorkflows.get(0);

            viewModel.setSelectedWorkflow(workflow);

            assertEquals(workflow, viewModel.getSelectedWorkflow());
            assertTrue(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should update details text on selection")
        void shouldUpdateDetailsTextOnSelection() {
            WorkflowDTO workflow = testWorkflows.get(0);

            viewModel.setSelectedWorkflow(workflow);

            String details = viewModel.getDetailsText();
            assertTrue(details.contains("Test Workflow 1"));
            assertTrue(details.contains("MANUAL"));
        }

        @Test
        @DisplayName("Should clear details on deselection")
        void shouldClearDetailsOnDeselection() {
            viewModel.setSelectedWorkflow(testWorkflows.get(0));
            viewModel.setSelectedWorkflow(null);

            assertEquals("Select a workflow to see details", viewModel.getDetailsText());
            assertFalse(viewModel.hasSelection());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete selected workflow")
        void shouldDeleteSelectedWorkflow() {
            viewModel.setSelectedWorkflow(testWorkflows.get(0));

            WorkflowDTO deleted = viewModel.deleteSelectedWorkflow();

            assertEquals(testWorkflows.get(0), deleted);
            assertEquals(1, viewModel.getWorkflows().size());
            assertNull(viewModel.getSelectedWorkflow());
            assertTrue(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should return null when no selection")
        void shouldReturnNullWhenNoSelection() {
            WorkflowDTO deleted = viewModel.deleteSelectedWorkflow();

            assertNull(deleted);
            assertEquals(2, viewModel.getWorkflows().size());
        }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

        @Test
        @DisplayName("Should return false when workflows exist")
        void shouldReturnFalseWhenWorkflowsExist() {
            assertFalse(viewModel.isEmpty());
        }

        @Test
        @DisplayName("Should return true when no workflows")
        void shouldReturnTrueWhenNoWorkflows() {
            WorkflowListDialogViewModel emptyViewModel = new WorkflowListDialogViewModel(Collections.emptyList());

            assertTrue(emptyViewModel.isEmpty());
        }
    }

    @Nested
    @DisplayName("Validation and Result")
    class ValidationAndResult {

        @Test
        @DisplayName("Should be invalid without selection")
        void shouldBeInvalidWithoutSelection() {
            assertFalse(viewModel.validate());
            assertFalse(viewModel.confirm());
        }

        @Test
        @DisplayName("Should be valid with selection")
        void shouldBeValidWithSelection() {
            viewModel.setSelectedWorkflow(testWorkflows.get(0));

            assertTrue(viewModel.validate());
            assertTrue(viewModel.confirm());
        }

        @Test
        @DisplayName("Should build result from selection")
        void shouldBuildResultFromSelection() {
            viewModel.setSelectedWorkflow(testWorkflows.get(0));
            viewModel.confirm();

            assertEquals(testWorkflows.get(0), viewModel.getResult());
        }
    }

    @Nested
    @DisplayName("Show Delete Button")
    class ShowDeleteButton {

        @Test
        @DisplayName("Should toggle delete button visibility")
        void shouldToggleDeleteButtonVisibility() {
            viewModel.setShowDeleteButton(true);
            assertTrue(viewModel.isShowDeleteButton());

            viewModel.setShowDeleteButton(false);
            assertFalse(viewModel.isShowDeleteButton());
        }
    }

    @Nested
    @DisplayName("Static Helpers")
    class StaticHelpers {

        @Test
        @DisplayName("Should get display name")
        void shouldGetDisplayName() {
            WorkflowDTO workflow = testWorkflows.get(0);

            String displayName = WorkflowListDialogViewModel.getDisplayName(workflow);

            assertEquals("Test Workflow 1", displayName);
        }

        @Test
        @DisplayName("Should get info text")
        void shouldGetInfoText() {
            WorkflowDTO workflow = testWorkflows.get(0);

            String infoText = WorkflowListDialogViewModel.getInfoText(workflow);

            assertTrue(infoText.contains("nodes"));
            assertTrue(infoText.contains("MANUAL"));
        }
    }

    @Nested
    @DisplayName("Empty Workflow List")
    class EmptyWorkflowList {

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            WorkflowListDialogViewModel emptyViewModel = new WorkflowListDialogViewModel(Collections.emptyList());

            assertTrue(emptyViewModel.isEmpty());
            assertEquals(0, emptyViewModel.getWorkflows().size());
        }
    }
}
