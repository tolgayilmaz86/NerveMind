/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Execution;
import ai.nervemind.common.domain.Execution.NodeExecution;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;
import ai.nervemind.ui.viewmodel.canvas.ExecutionHistoryViewModel.TimelineEntry;
import ai.nervemind.ui.viewmodel.canvas.ExecutionHistoryViewModel.VariableEntry;

/**
 * Unit tests for ExecutionHistoryViewModel.
 *
 * <p>
 * Tests cover:
 * <ul>
 * <li>Initial state</li>
 * <li>Execution list management</li>
 * <li>Selection handling</li>
 * <li>Timeline and variable data population</li>
 * <li>Formatting utilities</li>
 * <li>Callback invocations</li>
 * </ul>
 */
class ExecutionHistoryViewModelTest extends ViewModelTestBase {

    private ExecutionHistoryViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ExecutionHistoryViewModel();
    }

    // ===== Helper Methods =====

    private Execution createTestExecution(Long id, ExecutionStatus status) {
        return new Execution(
                id, 1L, "Test Workflow", status, TriggerType.MANUAL,
                Instant.now().minusSeconds(60), Instant.now(),
                Map.of("input", "value"),
                Map.of("output", "result"),
                status == ExecutionStatus.FAILED ? "Test error" : null,
                List.of());
    }

    private Execution createExecutionWithNodes(List<NodeExecution> nodes) {
        return new Execution(
                1L, 1L, "Test Workflow", ExecutionStatus.SUCCESS, TriggerType.MANUAL,
                Instant.now().minusSeconds(60), Instant.now(),
                Map.of(), Map.of("result", "ok"),
                null, nodes);
    }

    private NodeExecution createNodeExecution(String name, ExecutionStatus status,
            Instant start, Instant end) {
        return new NodeExecution(
                "node-" + name, name, "httpRequest", status,
                start, end, Map.of(), Map.of(), null);
    }

    @Nested
    class InitialState {

        @Test
        void shouldHaveEmptyExecutionList() {
            assertTrue(viewModel.getExecutions().isEmpty());
        }

        @Test
        void shouldHaveNoSelectedExecution() {
            assertNull(viewModel.getSelectedExecution());
            assertFalse(viewModel.hasSelectedExecution());
        }

        @Test
        void shouldBeHiddenInitially() {
            assertFalse(viewModel.isVisible());
        }

        @Test
        void shouldHaveDetailsHidden() {
            assertFalse(viewModel.isDetailsVisible());
        }

        @Test
        void shouldHaveZeroExecutionCount() {
            assertEquals(0, viewModel.getExecutionCount());
            assertEquals("0 executions", viewModel.getExecutionCountText());
        }
    }

    @Nested
    class ExecutionListManagement {

        @Test
        void shouldAddExecution() {
            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);

            viewModel.addExecution(exec);

            assertEquals(1, viewModel.getExecutions().size());
            assertEquals(exec, viewModel.getExecutions().getFirst());
        }

        @Test
        void shouldAddNewExecutionsAtBeginning() {
            Execution exec1 = createTestExecution(1L, ExecutionStatus.SUCCESS);
            Execution exec2 = createTestExecution(2L, ExecutionStatus.FAILED);

            viewModel.addExecution(exec1);
            viewModel.addExecution(exec2);

            assertEquals(2, viewModel.getExecutions().size());
            assertEquals(exec2, viewModel.getExecutions().getFirst());
        }

        @Test
        void shouldLimitHistorySize() {
            // Add more than max (100)
            for (int i = 0; i < 105; i++) {
                viewModel.addExecution(createTestExecution((long) i, ExecutionStatus.SUCCESS));
            }

            assertEquals(100, viewModel.getExecutions().size());
        }

        @Test
        void shouldSetExecutions() {
            List<Execution> executions = List.of(
                    createTestExecution(1L, ExecutionStatus.SUCCESS),
                    createTestExecution(2L, ExecutionStatus.FAILED));

            viewModel.setExecutions(executions);

            assertEquals(2, viewModel.getExecutions().size());
        }

        @Test
        void shouldSortByStartTimeNewestFirst() {
            Instant now = Instant.now();
            Execution older = new Execution(
                    1L, 1L, "Older", ExecutionStatus.SUCCESS, TriggerType.MANUAL,
                    now.minusSeconds(120), now.minusSeconds(100),
                    Map.of(), Map.of(), null, List.of());
            Execution newer = new Execution(
                    2L, 1L, "Newer", ExecutionStatus.SUCCESS, TriggerType.MANUAL,
                    now.minusSeconds(60), now,
                    Map.of(), Map.of(), null, List.of());

            viewModel.setExecutions(List.of(older, newer));

            assertEquals(newer, viewModel.getExecutions().getFirst());
        }

        @Test
        void shouldUpdateExecutionCount() {
            viewModel.addExecution(createTestExecution(1L, ExecutionStatus.SUCCESS));
            assertEquals(1, viewModel.getExecutionCount());
            assertEquals("1 executions", viewModel.getExecutionCountText());

            viewModel.addExecution(createTestExecution(2L, ExecutionStatus.SUCCESS));
            assertEquals(2, viewModel.getExecutionCount());
        }

        @Test
        void shouldHandleNullExecution() {
            viewModel.addExecution(null);
            assertTrue(viewModel.getExecutions().isEmpty());
        }

        @Test
        void shouldHandleNullExecutionList() {
            viewModel.setExecutions(null);
            assertTrue(viewModel.getExecutions().isEmpty());
        }
    }

    @Nested
    class Selection {

        @Test
        void shouldSetSelectedExecution() {
            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);
            viewModel.addExecution(exec);

            viewModel.setSelectedExecution(exec);

            assertEquals(exec, viewModel.getSelectedExecution());
            assertTrue(viewModel.hasSelectedExecution());
        }

        @Test
        void shouldShowDetailsWhenSelected() {
            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);

            viewModel.setSelectedExecution(exec);

            assertTrue(viewModel.isDetailsVisible());
        }

        @Test
        void shouldHideDetailsWhenCleared() {
            viewModel.setSelectedExecution(createTestExecution(1L, ExecutionStatus.SUCCESS));
            assertTrue(viewModel.isDetailsVisible());

            viewModel.clearSelection();

            assertFalse(viewModel.isDetailsVisible());
            assertFalse(viewModel.hasSelectedExecution());
        }

        @Test
        void shouldDetectSelectedHasError() {
            Execution failed = createTestExecution(1L, ExecutionStatus.FAILED);

            viewModel.setSelectedExecution(failed);

            assertTrue(viewModel.selectedHasError());
        }

        @Test
        void shouldNotHaveErrorForSuccess() {
            Execution success = createTestExecution(1L, ExecutionStatus.SUCCESS);

            viewModel.setSelectedExecution(success);

            assertFalse(viewModel.selectedHasError());
        }

        @Test
        void shouldInvokeSelectionChangedCallback() {
            AtomicReference<Execution> callbackExec = new AtomicReference<>();
            viewModel.setOnSelectionChanged(callbackExec::set);

            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);
            viewModel.setSelectedExecution(exec);

            assertEquals(exec, callbackExec.get());
        }
    }

    @Nested
    class NodeExecutionsAndTimeline {

        @Test
        void shouldPopulateNodeExecutions() {
            Instant base = Instant.now().minusSeconds(60);
            List<NodeExecution> nodes = List.of(
                    createNodeExecution("Node1", ExecutionStatus.SUCCESS, base, base.plusSeconds(10)),
                    createNodeExecution("Node2", ExecutionStatus.SUCCESS, base.plusSeconds(10), base.plusSeconds(20)));
            Execution exec = createExecutionWithNodes(nodes);

            viewModel.setSelectedExecution(exec);

            assertEquals(2, viewModel.getSelectedNodeExecutions().size());
        }

        @Test
        void shouldBuildTimelineEntries() {
            Instant base = Instant.now().minusSeconds(60);
            List<NodeExecution> nodes = List.of(
                    createNodeExecution("Node1", ExecutionStatus.SUCCESS, base, base.plusSeconds(30)),
                    createNodeExecution("Node2", ExecutionStatus.FAILED, base.plusSeconds(30), base.plusSeconds(60)));
            Execution exec = createExecutionWithNodes(nodes);

            viewModel.setSelectedExecution(exec);

            assertEquals(2, viewModel.getTimelineEntries().size());

            TimelineEntry first = viewModel.getTimelineEntries().getFirst();
            assertEquals("Node1", first.nodeName());
            assertEquals(ExecutionStatus.SUCCESS, first.status());
        }

        @Test
        void shouldClearDataOnClearSelection() {
            Instant base = Instant.now().minusSeconds(60);
            List<NodeExecution> nodes = List.of(
                    createNodeExecution("Node1", ExecutionStatus.SUCCESS, base, base.plusSeconds(30)));
            viewModel.setSelectedExecution(createExecutionWithNodes(nodes));
            assertFalse(viewModel.getSelectedNodeExecutions().isEmpty());

            viewModel.clearSelection();

            assertTrue(viewModel.getSelectedNodeExecutions().isEmpty());
            assertTrue(viewModel.getTimelineEntries().isEmpty());
        }
    }

    @Nested
    class Variables {

        @Test
        void shouldPopulateVariables() {
            Execution exec = new Execution(
                    1L, 1L, "Test", ExecutionStatus.SUCCESS, TriggerType.MANUAL,
                    Instant.now(), Instant.now(),
                    Map.of(),
                    Map.of("key1", "value1", "key2", 42),
                    null, List.of());

            viewModel.setSelectedExecution(exec);

            assertEquals(2, viewModel.getVariableEntries().size());
        }

        @Test
        void shouldFormatVariableValue() {
            VariableEntry entry = new VariableEntry("test", "value");
            assertEquals("value", entry.getFormattedValue());
        }

        @Test
        void shouldHandleNullVariableValue() {
            VariableEntry entry = new VariableEntry("test", null);
            assertEquals("null", entry.getFormattedValue());
        }
    }

    @Nested
    class ShowHideToggle {

        @Test
        void shouldShowPanel() {
            viewModel.show();
            assertTrue(viewModel.isVisible());
        }

        @Test
        void shouldHidePanel() {
            viewModel.show();
            viewModel.hide();
            assertFalse(viewModel.isVisible());
        }

        @Test
        void shouldToggleVisibility() {
            assertFalse(viewModel.isVisible());

            viewModel.toggle();
            assertTrue(viewModel.isVisible());

            viewModel.toggle();
            assertFalse(viewModel.isVisible());
        }

        @Test
        void shouldInvokeCloseCallback() {
            AtomicBoolean closeCalled = new AtomicBoolean(false);
            viewModel.setOnClose(() -> closeCalled.set(true));
            viewModel.show();

            viewModel.hide();

            assertTrue(closeCalled.get());
        }
    }

    @Nested
    class Callbacks {

        @Test
        void shouldInvokeRefreshCallback() {
            AtomicBoolean refreshCalled = new AtomicBoolean(false);
            viewModel.setOnRefresh(() -> refreshCalled.set(true));

            viewModel.refresh();

            assertTrue(refreshCalled.get());
        }

        @Test
        void shouldInvokeClearAllCallback() {
            AtomicBoolean clearCalled = new AtomicBoolean(false);
            viewModel.setOnClearAll(() -> clearCalled.set(true));

            viewModel.clearAll();

            assertTrue(clearCalled.get());
        }

        @Test
        void shouldClearWithoutCallback() {
            viewModel.addExecution(createTestExecution(1L, ExecutionStatus.SUCCESS));

            viewModel.clearAll();

            assertTrue(viewModel.getExecutions().isEmpty());
        }

        @Test
        void shouldInvokeHighlightCallback() {
            AtomicReference<Execution> highlighted = new AtomicReference<>();
            viewModel.setOnHighlightExecution(highlighted::set);
            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);
            viewModel.setSelectedExecution(exec);

            viewModel.highlightSelectedExecution();

            assertEquals(exec, highlighted.get());
        }
    }

    @Nested
    class TabSelection {

        @Test
        void shouldDefaultToSummaryTab() {
            Execution exec = createTestExecution(1L, ExecutionStatus.SUCCESS);

            viewModel.setSelectedExecution(exec);

            assertEquals("Summary", viewModel.getSelectedTab());
        }

        @Test
        void shouldSelectErrorTabOnFailure() {
            Execution failed = createTestExecution(1L, ExecutionStatus.FAILED);

            viewModel.setSelectedExecution(failed);

            assertEquals("Error", viewModel.getSelectedTab());
        }

        @Test
        void shouldAllowManualTabSelection() {
            viewModel.setSelectedTab("Timeline");
            assertEquals("Timeline", viewModel.getSelectedTab());
        }
    }

    @Nested
    class FormattingUtilities {

        @Test
        void shouldFormatShortDuration() {
            Duration duration = Duration.ofMillis(500);
            assertEquals("500ms", ExecutionHistoryViewModel.formatDuration(duration));
        }

        @Test
        void shouldFormatMediumDuration() {
            Duration duration = Duration.ofSeconds(5);
            assertEquals("5.00s", ExecutionHistoryViewModel.formatDuration(duration));
        }

        @Test
        void shouldFormatLongDuration() {
            Duration duration = Duration.ofMinutes(2).plusSeconds(30);
            assertEquals("2m 30s", ExecutionHistoryViewModel.formatDuration(duration));
        }

        @Test
        void shouldFormatNullDuration() {
            assertEquals("-", ExecutionHistoryViewModel.formatDuration(null));
        }

        @Test
        void shouldFormatNullTime() {
            assertEquals("-", ExecutionHistoryViewModel.formatTime(null));
        }

        @Test
        void shouldGetStatusClassName() {
            assertEquals("status-success",
                    ExecutionHistoryViewModel.getStatusClassName(ExecutionStatus.SUCCESS));
            assertEquals("status-failed",
                    ExecutionHistoryViewModel.getStatusClassName(ExecutionStatus.FAILED));
        }

        @Test
        void shouldGetStatusIconName() {
            assertEquals("mdi2c-check-circle",
                    ExecutionHistoryViewModel.getStatusIconName(ExecutionStatus.SUCCESS));
            assertEquals("mdi2a-alert-circle",
                    ExecutionHistoryViewModel.getStatusIconName(ExecutionStatus.FAILED));
            assertEquals("mdi2p-play-circle",
                    ExecutionHistoryViewModel.getStatusIconName(ExecutionStatus.RUNNING));
        }
    }

    @Nested
    class Disposal {

        @Test
        void shouldClearOnDispose() {
            viewModel.addExecution(createTestExecution(1L, ExecutionStatus.SUCCESS));
            viewModel.setSelectedExecution(viewModel.getExecutions().getFirst());

            viewModel.dispose();

            assertTrue(viewModel.getExecutions().isEmpty());
            assertTrue(viewModel.getSelectedNodeExecutions().isEmpty());
            assertTrue(viewModel.getTimelineEntries().isEmpty());
            assertTrue(viewModel.getVariableEntries().isEmpty());
        }
    }
}
