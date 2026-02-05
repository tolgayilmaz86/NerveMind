/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeExecutionState;
import ai.nervemind.ui.console.ExecutionConsoleService.NodeState;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for NodeDebugViewModel.
 */
@DisplayName("NodeDebugViewModel")
class NodeDebugViewModelTest extends ViewModelTestBase {

    private static Node createTestNode() {
        return new Node(
                "node-1",
                "httpRequest",
                "API Call",
                new Node.Position(100, 100),
                Map.of("url", "https://api.example.com"),
                null,
                false,
                null);
    }

    private static NodeExecutionState createSuccessState() {
        return new NodeExecutionState(
                "node-1",
                "API Call",
                NodeState.SUCCESS,
                System.currentTimeMillis() - 5000,
                System.currentTimeMillis() - 4850,
                null,
                Map.of("input", "test input"),
                Map.of("output", "test output"));
    }

    private static NodeExecutionState createFailedState() {
        return new NodeExecutionState(
                "node-1",
                "API Call",
                NodeState.FAILED,
                System.currentTimeMillis() - 3000,
                System.currentTimeMillis() - 2950,
                "Connection timeout",
                Map.of("input", "test"),
                null);
    }

    private static NodeExecutionState createRunningState() {
        return new NodeExecutionState(
                "node-1",
                "API Call",
                NodeState.RUNNING,
                System.currentTimeMillis(),
                0,
                null,
                null,
                null);
    }

    private static NodeExecutionState createSkippedState() {
        return new NodeExecutionState(
                "node-1",
                "API Call",
                NodeState.SKIPPED,
                0,
                0,
                null,
                null,
                null);
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should store node and execution state")
        void shouldStoreNodeAndExecutionState() {
            var node = createTestNode();
            var state = createSuccessState();
            var viewModel = new NodeDebugViewModel(node, state);

            assertThat(viewModel.getNode()).isEqualTo(node);
            assertThat(viewModel.getExecutionState()).isEqualTo(state);
        }

        @Test
        @DisplayName("Should display node name")
        void shouldDisplayNodeName() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getNodeName()).isEqualTo("API Call");
        }
    }

    @Nested
    @DisplayName("Status Display")
    class StatusDisplay {

        @Test
        @DisplayName("Should display success status")
        void shouldDisplaySuccessStatus() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getStatusText()).isEqualTo("Success");
            assertThat(viewModel.getStatusStyleClass()).contains("success");
            assertThat(viewModel.getStatusIconCode()).contains("check");
        }

        @Test
        @DisplayName("Should display failed status")
        void shouldDisplayFailedStatus() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createFailedState());

            assertThat(viewModel.getStatusText()).isEqualTo("Failed");
            assertThat(viewModel.getStatusStyleClass()).contains("failed");
            assertThat(viewModel.getStatusIconCode()).contains("alert");
        }

        @Test
        @DisplayName("Should display running status")
        void shouldDisplayRunningStatus() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createRunningState());

            assertThat(viewModel.getStatusText()).isEqualTo("Running");
            assertThat(viewModel.getStatusStyleClass()).contains("running");
        }

        @Test
        @DisplayName("Should display skipped status")
        void shouldDisplaySkippedStatus() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSkippedState());

            assertThat(viewModel.getStatusText()).isEqualTo("Skipped");
            assertThat(viewModel.getStatusStyleClass()).contains("skipped");
        }
    }

    @Nested
    @DisplayName("Timestamp Display")
    class TimestampDisplay {

        @Test
        @DisplayName("Should display timestamp when available")
        void shouldDisplayTimestampWhenAvailable() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getTimestampText()).contains("Last Executed:");
            assertThat(viewModel.getTimestampText()).doesNotContain("N/A");
        }

        @Test
        @DisplayName("Should display N/A when no timestamp")
        void shouldDisplayNaWhenNoTimestamp() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSkippedState());

            assertThat(viewModel.getTimestampText()).contains("N/A");
        }
    }

    @Nested
    @DisplayName("Duration Display")
    class DurationDisplay {

        @Test
        @DisplayName("Should display duration when available")
        void shouldDisplayDurationWhenAvailable() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getDurationText()).contains("150");
            assertThat(viewModel.getDurationText()).contains("ms");
        }

        @Test
        @DisplayName("Should be empty when no duration")
        void shouldBeEmptyWhenNoDuration() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createRunningState());

            assertThat(viewModel.getDurationText()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Input/Output Data")
    class InputOutputData {

        @Test
        @DisplayName("Should display input data as JSON")
        void shouldDisplayInputDataAsJson() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getInputDataText()).contains("input");
            assertThat(viewModel.getInputDataText()).contains("test input");
        }

        @Test
        @DisplayName("Should display output data as JSON")
        void shouldDisplayOutputDataAsJson() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.getOutputDataText()).contains("output");
            assertThat(viewModel.getOutputDataText()).contains("test output");
        }

        @Test
        @DisplayName("Should handle null input data")
        void shouldHandleNullInputData() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createRunningState());

            assertThat(viewModel.getInputDataText()).isEqualTo("(no data)");
        }

        @Test
        @DisplayName("Should handle null output data")
        void shouldHandleNullOutputData() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createFailedState());

            assertThat(viewModel.getOutputDataText()).isEqualTo("(no data)");
        }
    }

    @Nested
    @DisplayName("Error Display")
    class ErrorDisplay {

        @Test
        @DisplayName("Should display error message when failed")
        void shouldDisplayErrorMessageWhenFailed() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createFailedState());

            assertThat(viewModel.hasError()).isTrue();
            assertThat(viewModel.getErrorText()).contains("Connection timeout");
        }

        @Test
        @DisplayName("Should not have error on success")
        void shouldNotHaveErrorOnSuccess() {
            var viewModel = new NodeDebugViewModel(createTestNode(), createSuccessState());

            assertThat(viewModel.hasError()).isFalse();
        }
    }
}
