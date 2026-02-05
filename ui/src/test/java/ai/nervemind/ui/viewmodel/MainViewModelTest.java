/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.WorkflowServiceInterface;
import ai.nervemind.ui.viewmodel.MainViewModel.NavigationView;

/**
 * Unit tests for {@link MainViewModel}.
 * 
 * <p>
 * Tests the main application ViewModel without JavaFX dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MainViewModel")
class MainViewModelTest extends ViewModelTestBase {

    @Mock
    private WorkflowServiceInterface workflowService;

    @Mock
    private ExecutionServiceInterface executionService;

    private MainViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new MainViewModel(workflowService, executionService);
    }

    @Nested
    @DisplayName("Navigation")
    class NavigationTests {

        @Test
        @DisplayName("should start with WORKFLOWS view")
        void shouldStartWithWorkflowsView() {
            assertEquals(NavigationView.WORKFLOWS, viewModel.getCurrentView());
        }

        @Test
        @DisplayName("should navigate to different view")
        void shouldNavigateToDifferentView() {
            viewModel.navigateTo(NavigationView.EXECUTIONS);

            assertEquals(NavigationView.EXECUTIONS, viewModel.getCurrentView());
            assertEquals(NavigationView.WORKFLOWS, viewModel.getPreviousView());
        }

        @Test
        @DisplayName("should track previous view on navigation")
        void shouldTrackPreviousView() {
            viewModel.navigateTo(NavigationView.EXECUTIONS);
            viewModel.navigateTo(NavigationView.SETTINGS);

            assertEquals(NavigationView.SETTINGS, viewModel.getCurrentView());
            assertEquals(NavigationView.EXECUTIONS, viewModel.getPreviousView());
        }

        @Test
        @DisplayName("should return to previous view")
        void shouldReturnToPreviousView() {
            viewModel.navigateTo(NavigationView.EXECUTIONS);
            viewModel.navigateTo(NavigationView.SETTINGS);

            viewModel.navigateBack();

            assertEquals(NavigationView.EXECUTIONS, viewModel.getCurrentView());
        }

        @Test
        @DisplayName("should return to WORKFLOWS when no previous view")
        void shouldReturnToWorkflowsWhenNoPreviousView() {
            // Navigate to same view shouldn't change previous
            viewModel.navigateTo(NavigationView.WORKFLOWS);

            viewModel.navigateBack();

            assertEquals(NavigationView.WORKFLOWS, viewModel.getCurrentView());
        }

        @Test
        @DisplayName("should not navigate if already on target view")
        void shouldNotNavigateIfAlreadyOnView() {
            viewModel.navigateTo(NavigationView.EXECUTIONS);
            NavigationView prev = viewModel.getPreviousView();

            viewModel.navigateTo(NavigationView.EXECUTIONS);

            // Previous should not change
            assertEquals(prev, viewModel.getPreviousView());
        }

        @Test
        @DisplayName("should update status when navigating")
        void shouldUpdateStatusWhenNavigating() {
            viewModel.navigateTo(NavigationView.EXECUTIONS);

            assertEquals("Executions", viewModel.getStatusMessage());
        }
    }

    @Nested
    @DisplayName("Status Bar")
    class StatusBarTests {

        @Test
        @DisplayName("should have initial status 'Ready'")
        void shouldHaveInitialStatus() {
            assertEquals("Ready", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should update status message")
        void shouldUpdateStatusMessage() {
            viewModel.updateStatus("Loading...");

            assertEquals("Loading...", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should update zoom status")
        void shouldUpdateZoomStatus() {
            viewModel.updateZoomStatus(150);

            assertEquals("Zoom: 150%", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should have application version")
        void shouldHaveApplicationVersion() {
            assertNotNull(viewModel.getApplicationVersion());
            assertTrue(viewModel.getApplicationVersion().contains("NerveMind"));
        }
    }

    @Nested
    @DisplayName("Active Workflow")
    class ActiveWorkflowTests {

        @Test
        @DisplayName("should have no active workflow initially")
        void shouldHaveNoActiveWorkflowInitially() {
            assertNull(viewModel.getActiveWorkflow());
            assertFalse(viewModel.hasActiveWorkflow());
        }

        @Test
        @DisplayName("should set active workflow")
        void shouldSetActiveWorkflow() {
            WorkflowDTO workflow = createTestWorkflow(1L, "Test Workflow");

            viewModel.setActiveWorkflow(workflow);

            assertEquals(workflow, viewModel.getActiveWorkflow());
            assertTrue(viewModel.hasActiveWorkflow());
            assertEquals("Test Workflow", viewModel.getActiveWorkflowName());
        }

        @Test
        @DisplayName("should update status when setting workflow")
        void shouldUpdateStatusWhenSettingWorkflow() {
            WorkflowDTO workflow = createTestWorkflow(1L, "My Workflow");

            viewModel.setActiveWorkflow(workflow);

            assertEquals("Loaded: My Workflow", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should clear workflow name when workflow is null")
        void shouldClearWorkflowNameWhenNull() {
            WorkflowDTO workflow = createTestWorkflow(1L, "Test Workflow");
            viewModel.setActiveWorkflow(workflow);

            viewModel.setActiveWorkflow(null);

            assertNull(viewModel.getActiveWorkflowName());
            assertFalse(viewModel.hasActiveWorkflow());
        }

        @Test
        @DisplayName("should track dirty state")
        void shouldTrackDirtyState() {
            assertFalse(viewModel.isWorkflowDirty());

            viewModel.setWorkflowDirty(true);

            assertTrue(viewModel.isWorkflowDirty());
        }
    }

    @Nested
    @DisplayName("Canvas Settings")
    class CanvasSettingsTests {

        @Test
        @DisplayName("should have grid shown by default")
        void shouldHaveGridShownByDefault() {
            assertTrue(viewModel.isShowGrid());
        }

        @Test
        @DisplayName("should have snap to grid enabled by default")
        void shouldHaveSnapToGridByDefault() {
            assertTrue(viewModel.isSnapToGrid());
        }

        @Test
        @DisplayName("should update status when toggling grid")
        void shouldUpdateStatusWhenTogglingGrid() {
            viewModel.setShowGrid(false);
            assertEquals("Grid: Hidden", viewModel.getStatusMessage());

            viewModel.setShowGrid(true);
            assertEquals("Grid: Shown", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should update status when toggling snap")
        void shouldUpdateStatusWhenTogglingSnap() {
            viewModel.setSnapToGrid(false);
            assertEquals("Snap to Grid: Disabled", viewModel.getStatusMessage());

            viewModel.setSnapToGrid(true);
            assertEquals("Snap to Grid: Enabled", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should set zoom level")
        void shouldSetZoomLevel() {
            viewModel.setZoomLevel(1.5);

            assertEquals(1.5, viewModel.getZoomLevel());
            assertEquals("Zoom: 150%", viewModel.getStatusMessage());
        }
    }

    @Nested
    @DisplayName("Execution Data")
    class ExecutionDataTests {

        @Test
        @DisplayName("should have empty executions initially")
        void shouldHaveEmptyExecutionsInitially() {
            assertTrue(viewModel.getExecutions().isEmpty());
        }

        @Test
        @DisplayName("should load executions from service")
        void shouldLoadExecutionsFromService() {
            List<ExecutionDTO> testExecutions = List.of(
                    createTestExecution("exec-1", "Workflow 1"),
                    createTestExecution("exec-2", "Workflow 2"));
            when(executionService.findAll()).thenReturn(testExecutions);

            viewModel.loadExecutions();

            assertEquals(2, viewModel.getExecutions().size());
            assertFalse(viewModel.isExecutionsLoading());
        }

        @Test
        @DisplayName("should handle load errors gracefully")
        void shouldHandleLoadErrorsGracefully() {
            when(executionService.findAll()).thenThrow(new RuntimeException("DB error"));

            viewModel.loadExecutions();

            assertTrue(viewModel.hasError());
            assertTrue(viewModel.getErrorMessage().contains("Failed to load executions"));
        }

        @Test
        @DisplayName("should set selected execution")
        void shouldSetSelectedExecution() {
            ExecutionDTO execution = createTestExecution("exec-1", "Test");

            viewModel.setSelectedExecution(execution);

            assertEquals(execution, viewModel.getSelectedExecution());
        }

        @Test
        @DisplayName("should refresh executions")
        void shouldRefreshExecutions() {
            when(executionService.findAll()).thenReturn(List.of(
                    createTestExecution("exec-1", "Test")));

            viewModel.refreshExecutions();

            verify(executionService, times(1)).findAll();
        }

        @Test
        @DisplayName("should clear execution history")
        void shouldClearExecutionHistory() {
            // Setup: Add some executions
            viewModel.getExecutions().add(createTestExecution("exec-1", "Test 1"));
            viewModel.getExecutions().add(createTestExecution("exec-2", "Test 2"));
            viewModel.setSelectedExecution(createTestExecution("exec-1", "Test 1"));

            viewModel.clearExecutionHistory();

            verify(executionService).deleteAll();
            assertTrue(viewModel.getExecutions().isEmpty());
            assertNull(viewModel.getSelectedExecution());
        }

        @Test
        @DisplayName("should handle clear execution history errors gracefully")
        void shouldHandleClearExecutionHistoryErrorsGracefully() {
            doThrow(new RuntimeException("DB error")).when(executionService).deleteAll();
            viewModel.getExecutions().add(createTestExecution("exec-1", "Test 1"));

            viewModel.clearExecutionHistory();

            assertTrue(viewModel.hasError());
            assertTrue(viewModel.getErrorMessage().contains("Failed to clear execution history"));
        }

        @Test
        @DisplayName("should handle null execution service for clear")
        void shouldHandleNullExecutionServiceForClear() {
            MainViewModel vmWithoutService = new MainViewModel(workflowService, null);

            // Should not throw
            vmWithoutService.clearExecutionHistory();

            // Verify no error occurred
            assertFalse(vmWithoutService.hasError());
        }
    }

    @Nested
    @DisplayName("Workflow Operations")
    class WorkflowOperationsTests {

        @Test
        @DisplayName("should get all workflows")
        void shouldGetAllWorkflows() {
            List<WorkflowDTO> workflows = List.of(
                    createTestWorkflow(1L, "Workflow 1"),
                    createTestWorkflow(2L, "Workflow 2"));
            when(workflowService.findAll()).thenReturn(workflows);

            List<WorkflowDTO> result = viewModel.getAllWorkflows();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("should delete workflow")
        void shouldDeleteWorkflow() {
            viewModel.deleteWorkflow(1L);

            verify(workflowService).delete(1L);
            assertEquals("Deleted workflow", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should get workflow by id")
        void shouldGetWorkflowById() {
            WorkflowDTO workflow = createTestWorkflow(1L, "Test");
            when(workflowService.findById(1L)).thenReturn(Optional.of(workflow));

            Optional<WorkflowDTO> result = viewModel.getWorkflow(1L);

            assertTrue(result.isPresent());
            assertEquals("Test", result.get().name());
        }

        @Test
        @DisplayName("should return empty list when no service")
        void shouldReturnEmptyListWhenNoService() {
            MainViewModel vmNoService = new MainViewModel(null, null);

            List<WorkflowDTO> result = vmNoService.getAllWorkflows();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Formatting Helpers")
    class FormattingTests {

        @Test
        @DisplayName("should format instant")
        void shouldFormatInstant() {
            Instant instant = Instant.parse("2025-01-15T10:30:00Z");

            String result = MainViewModel.formatInstant(instant);

            assertNotNull(result);
            assertNotEquals("N/A", result);
        }

        @Test
        @DisplayName("should return N/A for null instant")
        void shouldReturnNaForNullInstant() {
            assertEquals("N/A", MainViewModel.formatInstant(null));
        }

        @Test
        @DisplayName("should format duration in milliseconds")
        void shouldFormatDurationMs() {
            assertEquals("500ms", MainViewModel.formatDuration(500L));
        }

        @Test
        @DisplayName("should format duration in seconds")
        void shouldFormatDurationSeconds() {
            assertEquals("2.50s", MainViewModel.formatDuration(2500L));
        }

        @Test
        @DisplayName("should format short duration")
        void shouldFormatShortDuration() {
            assertEquals("2.5s", MainViewModel.formatDurationShort(2500L));
        }

        @Test
        @DisplayName("should return N/A for null duration")
        void shouldReturnNaForNullDuration() {
            assertEquals("N/A", MainViewModel.formatDuration(null));
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("should initialize and load data")
        void shouldInitializeAndLoadData() {
            when(executionService.findAll()).thenReturn(List.of());

            viewModel.initialize();

            verify(executionService).findAll();
            assertEquals("Ready", viewModel.getStatusMessage());
        }

        @Test
        @DisplayName("should dispose and clear data")
        void shouldDisposeAndClearData() {
            WorkflowDTO workflow = createTestWorkflow(1L, "Test");
            viewModel.setActiveWorkflow(workflow);
            viewModel.getExecutions().add(createTestExecution("exec-1", "Test"));

            viewModel.dispose();

            assertTrue(viewModel.getExecutions().isEmpty());
            assertNull(viewModel.getActiveWorkflow());
            assertNull(viewModel.getSelectedExecution());
        }
    }

    @Nested
    @DisplayName("Navigation View Enum")
    class NavigationViewEnumTests {

        @Test
        @DisplayName("should have correct display names")
        void shouldHaveCorrectDisplayNames() {
            assertEquals("Workflows", NavigationView.WORKFLOWS.getDisplayName());
            assertEquals("Executions", NavigationView.EXECUTIONS.getDisplayName());
            assertEquals("Execution Console", NavigationView.EXECUTION_CONSOLE.getDisplayName());
            assertEquals("Credentials", NavigationView.CREDENTIALS.getDisplayName());
            assertEquals("Plugins", NavigationView.PLUGINS.getDisplayName());
            assertEquals("Settings", NavigationView.SETTINGS.getDisplayName());
        }
    }

    // ========== Helper Methods ==========

    private WorkflowDTO createTestWorkflow(Long id, String name) {
        return new WorkflowDTO(
                id,
                name,
                "Test description",
                List.of(),
                List.of(),
                Map.of(),
                false,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }

    private ExecutionDTO createTestExecution(String id, String workflowName) {
        return new ExecutionDTO(
                Long.valueOf(Math.abs(id.hashCode())),
                1L,
                workflowName,
                ExecutionStatus.SUCCESS,
                TriggerType.MANUAL,
                Instant.now().minusSeconds(60),
                Instant.now(),
                1000L,
                null,
                null,
                null,
                null);
    }
}
