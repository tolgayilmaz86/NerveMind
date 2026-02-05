/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import ai.nervemind.ui.viewmodel.MainViewModel;
import ai.nervemind.ui.viewmodel.canvas.WorkflowCanvasViewModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.ExecutionSessionModel;

/**
 * Integration tests for execution monitoring and debugging user flows.
 * 
 * <p>
 * Tests the interaction between ExecutionConsoleViewModel and
 * the canvas during workflow execution.
 */
@DisplayName("Execution Console Flow Integration Tests")
class ExecutionConsoleFlowIntegrationTest {

    private ExecutionConsoleViewModel consoleViewModel;
    private WorkflowCanvasViewModel canvasViewModel;
    private MainViewModel mainViewModel;

    private static final String EXECUTION_ID = "exec-001";
    private static final String WORKFLOW_NAME = "Data Pipeline";

    @BeforeEach
    void setUp() {
        consoleViewModel = new ExecutionConsoleViewModel();
        canvasViewModel = new WorkflowCanvasViewModel(
                new CanvasZoomService(),
                new NodeSelectionService<>());
        mainViewModel = new MainViewModel();
    }

    private WorkflowDTO createDataPipeline() {
        Node fetchNode = new Node("fetch-1", "httpRequest", "Fetch Data", new Node.Position(100, 100),
                Map.of("url", "https://api.example.com/data"), null, false, null);
        Node transformNode = new Node("transform-1", "code", "Transform", new Node.Position(300, 100),
                Map.of("code", "return data.map(x => x * 2)"), null, false, null);
        Node saveNode = new Node("save-1", "database", "Save Results", new Node.Position(500, 100),
                Map.of("table", "results"), null, false, null);

        Connection conn1 = new Connection("c1", "fetch-1", "output", "transform-1", "input");
        Connection conn2 = new Connection("c2", "transform-1", "output", "save-1", "input");

        return new WorkflowDTO(1L, WORKFLOW_NAME, "A data pipeline",
                List.of(fetchNode, transformNode, saveNode),
                List.of(conn1, conn2),
                Map.of(), false, TriggerType.MANUAL, null,
                Instant.now(), Instant.now(), null, 1);
    }

    // ===== User Flow: Monitor Execution =====

    @Nested
    @DisplayName("Monitor Execution Flow")
    class MonitorExecutionFlow {

        @Test
        @DisplayName("User monitors workflow execution from start to finish")
        void userMonitorsWorkflowExecution() {
            // 1. Load workflow
            WorkflowDTO workflow = createDataPipeline();
            canvasViewModel.loadWorkflow(workflow);
            mainViewModel.setActiveWorkflow(workflow);

            // 2. Execution starts
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);
            assertThat(consoleViewModel.sessionCountProperty().get()).isEqualTo(1);
            assertThat(consoleViewModel.selectedSessionIdProperty().get()).isEqualTo(EXECUTION_ID);

            // 3. First node starts
            consoleViewModel.nodeStart(EXECUTION_ID, "fetch-1", "Fetch Data", "httpRequest");

            // 4. First node completes successfully
            consoleViewModel.nodeEnd(EXECUTION_ID, "fetch-1", "Fetch Data", true, 150);

            // 5. Second node processes
            consoleViewModel.nodeStart(EXECUTION_ID, "transform-1", "Transform", "code");
            consoleViewModel.nodeEnd(EXECUTION_ID, "transform-1", "Transform", true, 50);

            // 6. Third node saves
            consoleViewModel.nodeStart(EXECUTION_ID, "save-1", "Save Results", "database");
            consoleViewModel.nodeEnd(EXECUTION_ID, "save-1", "Save Results", true, 200);

            // 7. Execution completes
            consoleViewModel.endExecution(EXECUTION_ID, true, 400);

            // 8. Session shows completion
            ExecutionSessionModel session = consoleViewModel.getSelectedSession();
            assertThat(session).isNotNull();
            assertThat(session.isComplete()).isTrue();
            assertThat(session.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("User sees execution progress through log entries")
        void userSeesExecutionProgress() {
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);

            // Add some info logs
            consoleViewModel.info(EXECUTION_ID, "Starting data fetch", "URL: https://api.example.com");
            consoleViewModel.info(EXECUTION_ID, "Data received", "Records: 100");

            // Add different log levels
            consoleViewModel.info(EXECUTION_ID, "Info message", null);
            consoleViewModel.debug(EXECUTION_ID, "Debug message", null);

            // Debug is shown by default
            assertThat(consoleViewModel.showDebugProperty().get()).isTrue();

            // User disables debug
            consoleViewModel.showDebugProperty().set(false);
            assertThat(consoleViewModel.showDebugProperty().get()).isFalse();
        }

        @Test
        @DisplayName("User toggles display settings")
        void userTogglesDisplaySettings() {
            // Default settings
            assertThat(consoleViewModel.autoScrollProperty().get()).isTrue();
            assertThat(consoleViewModel.showTimestampsProperty().get()).isTrue();
            assertThat(consoleViewModel.showLineNumbersProperty().get()).isFalse();

            // User toggles settings
            consoleViewModel.autoScrollProperty().set(false);
            consoleViewModel.showTimestampsProperty().set(false);
            consoleViewModel.showLineNumbersProperty().set(true);

            assertThat(consoleViewModel.autoScrollProperty().get()).isFalse();
            assertThat(consoleViewModel.showTimestampsProperty().get()).isFalse();
            assertThat(consoleViewModel.showLineNumbersProperty().get()).isTrue();
        }
    }

    // ===== User Flow: Debug Failed Node =====

    @Nested
    @DisplayName("Debug Failed Node Flow")
    class DebugFailedNodeFlow {

        @Test
        @DisplayName("User debugs failed node execution")
        void userDebugsFailedNode() {
            // 1. Load workflow
            WorkflowDTO workflow = createDataPipeline();
            canvasViewModel.loadWorkflow(workflow);

            // 2. Execution fails at second node
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);
            consoleViewModel.nodeStart(EXECUTION_ID, "fetch-1", "Fetch Data", "httpRequest");
            consoleViewModel.nodeEnd(EXECUTION_ID, "fetch-1", "Fetch Data", true, 150);

            consoleViewModel.nodeStart(EXECUTION_ID, "transform-1", "Transform", "code");
            consoleViewModel.error(EXECUTION_ID, "transform-1", "TypeError: Cannot read property 'map'",
                    "at transform.js:1:15");
            consoleViewModel.nodeEnd(EXECUTION_ID, "transform-1", "Transform", false, 5);

            consoleViewModel.endExecution(EXECUTION_ID, false, 155);

            // 3. Session shows failure
            ExecutionSessionModel session = consoleViewModel.getSelectedSession();
            assertThat(session.isComplete()).isTrue();
            assertThat(session.isSuccess()).isFalse();

            // 4. User can see error in console
            assertThat(consoleViewModel.errorCountProperty().get()).isGreaterThan(0);
        }
    }

    // ===== User Flow: Multiple Sessions =====

    @Nested
    @DisplayName("Multiple Sessions Flow")
    class MultipleSessionsFlow {

        @Test
        @DisplayName("User switches between execution sessions")
        void userSwitchesBetweenSessions() {
            // Start first execution
            consoleViewModel.startExecution("exec-001", "Workflow A");
            consoleViewModel.info("exec-001", "Running Workflow A", null);

            // Start second execution
            consoleViewModel.startExecution("exec-002", "Workflow B");
            consoleViewModel.info("exec-002", "Running Workflow B", null);

            // Two sessions exist
            assertThat(consoleViewModel.sessionCountProperty().get()).isEqualTo(2);

            // User can switch between sessions
            consoleViewModel.selectedSessionIdProperty().set("exec-001");
            assertThat(consoleViewModel.selectedSessionIdProperty().get()).isEqualTo("exec-001");

            consoleViewModel.selectedSessionIdProperty().set("exec-002");
            assertThat(consoleViewModel.selectedSessionIdProperty().get()).isEqualTo("exec-002");
        }

        @Test
        @DisplayName("User can view all sessions")
        void userViewsAllSessions() {
            // Start and complete execution
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);
            consoleViewModel.endExecution(EXECUTION_ID, true, 100);

            assertThat(consoleViewModel.sessionCountProperty().get()).isEqualTo(1);
            assertThat(consoleViewModel.getAllSessions()).hasSize(1);
        }
    }

    // ===== User Flow: Export Logs =====

    @Nested
    @DisplayName("Export Logs Flow")
    class ExportLogsFlow {

        @Test
        @DisplayName("User exports logs as text")
        void userExportsLogsAsText() {
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);
            consoleViewModel.info(EXECUTION_ID, "Test message", "Details here");
            consoleViewModel.endExecution(EXECUTION_ID, true, 50);

            String textExport = consoleViewModel.exportAsText();

            assertThat(textExport).isNotEmpty();
            assertThat(textExport).contains("Test message");
        }

        @Test
        @DisplayName("User exports logs as JSON")
        void userExportsLogsAsJson() {
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);
            consoleViewModel.info(EXECUTION_ID, "Test message", "Details");
            consoleViewModel.endExecution(EXECUTION_ID, true, 50);

            String jsonExport = consoleViewModel.exportAsJson();

            assertThat(jsonExport).isNotEmpty();
            assertThat(jsonExport).contains("Test message");
            assertThat(jsonExport).startsWith("{"); // JSON object
        }
    }

    // ===== User Flow: Statistics =====

    @Nested
    @DisplayName("Statistics Flow")
    class StatisticsFlow {

        @Test
        @DisplayName("User sees log statistics")
        void userSeesStatistics() {
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);

            // Add various log types
            consoleViewModel.info(EXECUTION_ID, "Info 1", null);
            consoleViewModel.info(EXECUTION_ID, "Info 2", null);
            consoleViewModel.retry(EXECUTION_ID, 1, 3, 1000); // Creates WARN entry
            consoleViewModel.error(EXECUTION_ID, "node-1", "Error message", null);

            // Statistics are updated
            assertThat(consoleViewModel.infoCountProperty().get()).isGreaterThan(0);
            assertThat(consoleViewModel.warnCountProperty().get()).isGreaterThan(0);
            assertThat(consoleViewModel.errorCountProperty().get()).isGreaterThan(0);
        }

        @Test
        @DisplayName("User sees total entry count")
        void userSeesTotalEntryCount() {
            consoleViewModel.startExecution(EXECUTION_ID, WORKFLOW_NAME);

            consoleViewModel.info(EXECUTION_ID, "Message 1", null);
            consoleViewModel.info(EXECUTION_ID, "Message 2", null);

            assertThat(consoleViewModel.totalEntryCountProperty().get()).isGreaterThan(0);
        }
    }
}
