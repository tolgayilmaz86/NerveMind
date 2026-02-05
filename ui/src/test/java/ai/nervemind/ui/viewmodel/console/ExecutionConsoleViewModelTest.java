/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.ConsoleEventListener;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.ExecutionSessionModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.LogEntryModel;
import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel.LogEntryType;

/**
 * Unit tests for ExecutionConsoleViewModel.
 * 
 * <p>
 * Tests cover:
 * <ul>
 * <li>Session management (start, end, selection)</li>
 * <li>Log entry creation and filtering</li>
 * <li>Node execution tracking</li>
 * <li>Statistics calculation</li>
 * <li>Export functionality</li>
 * <li>Event listener notifications</li>
 * </ul>
 */
class ExecutionConsoleViewModelTest extends ViewModelTestBase {

    private ExecutionConsoleViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ExecutionConsoleViewModel();
    }

    // =========================
    // Session Management Tests
    // =========================

    @Nested
    @DisplayName("Session Management")
    class SessionManagementTests {

        @Test
        @DisplayName("Should start execution session")
        void shouldStartExecutionSession() {
            viewModel.startExecution("exec-1", "Test Workflow");

            assertThat(viewModel.sessionCountProperty().get()).isEqualTo(1);
            assertThat(viewModel.sessionIdsProperty()).containsExactly("exec-1");
            assertThat(viewModel.selectedSessionIdProperty().get()).isEqualTo("exec-1");
        }

        @Test
        @DisplayName("Should support multiple sessions")
        void shouldSupportMultipleSessions() {
            viewModel.startExecution("exec-1", "Workflow 1");
            viewModel.startExecution("exec-2", "Workflow 2");
            viewModel.startExecution("exec-3", "Workflow 3");

            assertThat(viewModel.sessionCountProperty().get()).isEqualTo(3);
            assertThat(viewModel.sessionIdsProperty()).hasSize(3);
            // Last session should be selected
            assertThat(viewModel.selectedSessionIdProperty().get()).isEqualTo("exec-3");
        }

        @Test
        @DisplayName("Should end execution session successfully")
        void shouldEndExecutionSessionSuccessfully() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.endExecution("exec-1", true, 1234);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session).isNotNull();
            assertThat(session.isComplete()).isTrue();
            assertThat(session.isSuccess()).isTrue();
            assertThat(session.getDurationMs()).isEqualTo(1234);
        }

        @Test
        @DisplayName("Should end execution session with failure")
        void shouldEndExecutionSessionWithFailure() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.endExecution("exec-1", false, 5678);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session).isNotNull();
            assertThat(session.isComplete()).isTrue();
            assertThat(session.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should get selected session")
        void shouldGetSelectedSession() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.startExecution("exec-2", "Other Workflow");

            viewModel.selectedSessionIdProperty().set("exec-1");
            ExecutionSessionModel session = viewModel.getSelectedSession();

            assertThat(session).isNotNull();
            assertThat(session.getWorkflowName()).isEqualTo("Test Workflow");
        }

        @Test
        @DisplayName("Should return null for no selected session")
        void shouldReturnNullForNoSelectedSession() {
            assertThat(viewModel.getSelectedSession()).isNull();
        }

        @Test
        @DisplayName("Should get all sessions")
        void shouldGetAllSessions() {
            viewModel.startExecution("exec-1", "Workflow 1");
            viewModel.startExecution("exec-2", "Workflow 2");

            List<ExecutionSessionModel> sessions = viewModel.getAllSessions();
            assertThat(sessions).hasSize(2);
        }
    }

    // =========================
    // Node Execution Tests
    // =========================

    @Nested
    @DisplayName("Node Execution Tracking")
    class NodeExecutionTests {

        @Test
        @DisplayName("Should track node start")
        void shouldTrackNodeStart() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeStart("exec-1", "node-1", "HTTP Request", "HttpNode");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getTotalNodes()).isEqualTo(1);
            assertThat(session.getCompletedNodes()).isZero();
        }

        @Test
        @DisplayName("Should track node end")
        void shouldTrackNodeEnd() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeStart("exec-1", "node-1", "HTTP Request", "HttpNode");
            viewModel.nodeEnd("exec-1", "node-1", "HTTP Request", true, 500);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getTotalNodes()).isEqualTo(1);
            assertThat(session.getCompletedNodes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track nested nodes with depth")
        void shouldTrackNestedNodesWithDepth() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeStart("exec-1", "node-1", "Parent Node", "ParentType");
            viewModel.nodeStart("exec-1", "node-2", "Child Node", "ChildType");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getNodeDepth("node-1")).isZero();
            assertThat(session.getNodeDepth("node-2")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should track node skip")
        void shouldTrackNodeSkip() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeSkip("exec-1", "Skipped Node", "Condition not met");

            // Should create a DEBUG entry
            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasSkipEntry = session.getEntries().stream()
                    .anyMatch(e -> e.message().contains("Skipped"));
            assertThat(hasSkipEntry).isTrue();
        }

        @Test
        @DisplayName("Should track node input")
        void shouldTrackNodeInput() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeStart("exec-1", "node-1", "Node", "Type");
            viewModel.nodeInput("exec-1", "node-1", "Node", "input data");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasInputEntry = session.getEntries().stream()
                    .anyMatch(e -> e.type() == LogEntryType.TRACE && e.message().contains("Input"));
            assertThat(hasInputEntry).isTrue();
        }

        @Test
        @DisplayName("Should track node output")
        void shouldTrackNodeOutput() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.nodeStart("exec-1", "node-1", "Node", "Type");
            viewModel.nodeOutput("exec-1", "node-1", "Node", "output data");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasOutputEntry = session.getEntries().stream()
                    .anyMatch(e -> e.type() == LogEntryType.TRACE && e.message().contains("Output"));
            assertThat(hasOutputEntry).isTrue();
        }
    }

    // =========================
    // Log Entry Tests
    // =========================

    @Nested
    @DisplayName("Log Entry Creation")
    class LogEntryTests {

        @Test
        @DisplayName("Should create info log entry")
        void shouldCreateInfoLogEntry() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Test message", "Test details");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getEntries())
                    .hasSize(1)
                    .first()
                    .extracting(LogEntryModel::type)
                    .isEqualTo(LogEntryType.INFO);
        }

        @Test
        @DisplayName("Should create debug log entry")
        void shouldCreateDebugLogEntry() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.debug("exec-1", "Debug message", "Debug details");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getEntries()).anyMatch(e -> e.type() == LogEntryType.DEBUG);
        }

        @Test
        @DisplayName("Should create error log entry")
        void shouldCreateErrorLogEntry() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.error("exec-1", "TestSource", "Error message", "Stack trace");

            ExecutionSessionModel session = viewModel.getSelectedSession();
            assertThat(session.getEntries()).anyMatch(e -> e.type() == LogEntryType.ERROR);
        }

        @Test
        @DisplayName("Should track retry attempts")
        void shouldTrackRetryAttempts() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.retry("exec-1", 2, 3, 1000);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasRetryEntry = session.getEntries().stream()
                    .anyMatch(e -> e.type() == LogEntryType.WARN && e.message().contains("Retry"));
            assertThat(hasRetryEntry).isTrue();
        }

        @Test
        @DisplayName("Should track rate limiting")
        void shouldTrackRateLimiting() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.rateLimit("exec-1", "api-bucket", true, 500);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasRateLimitEntry = session.getEntries().stream()
                    .anyMatch(e -> e.message().contains("Rate limited"));
            assertThat(hasRateLimitEntry).isTrue();
        }

        @Test
        @DisplayName("Should track data flow")
        void shouldTrackDataFlow() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.dataFlow("exec-1", "Node A", "Node B", 1024);

            ExecutionSessionModel session = viewModel.getSelectedSession();
            boolean hasDataFlowEntry = session.getEntries().stream()
                    .anyMatch(e -> e.type() == LogEntryType.TRACE && e.message().contains("Data:"));
            assertThat(hasDataFlowEntry).isTrue();
        }
    }

    // =========================
    // Filtering Tests
    // =========================

    @Nested
    @DisplayName("Log Filtering")
    class FilteringTests {

        @Test
        @DisplayName("Should filter by DEBUG level")
        void shouldFilterByDebugLevel() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info message", null);
            viewModel.debug("exec-1", "Debug message", null);

            // DEBUG is shown by default
            assertThat(viewModel.showDebugProperty().get()).isTrue();
            viewModel.refreshVisibleEntries();

            // Both INFO and DEBUG should be visible
            List<LogEntryModel> visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible).anyMatch(e -> e.type() == LogEntryType.DEBUG);
            assertThat(visible).anyMatch(e -> e.type() == LogEntryType.INFO);

            // Disable DEBUG
            viewModel.showDebugProperty().set(false);
            viewModel.refreshVisibleEntries();

            // Now only INFO should be visible
            visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible).noneMatch(e -> e.type() == LogEntryType.DEBUG);
            assertThat(visible).anyMatch(e -> e.type() == LogEntryType.INFO);
        }

        @Test
        @DisplayName("Should filter by TRACE level")
        void shouldFilterByTraceLevel() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info message", null);
            viewModel.dataFlow("exec-1", "A", "B", 100); // Creates TRACE entry

            // Enable TRACE filter
            viewModel.showTraceProperty().set(true);
            viewModel.refreshVisibleEntries();

            List<LogEntryModel> visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible).anyMatch(e -> e.type() == LogEntryType.TRACE);
        }

        @Test
        @DisplayName("Should filter by text")
        void shouldFilterByText() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Apple message", null);
            viewModel.info("exec-1", "Banana message", null);
            viewModel.info("exec-1", "Cherry message", null);

            viewModel.filterTextProperty().set("banana");
            viewModel.refreshVisibleEntries();

            List<LogEntryModel> visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible).isNotEmpty()
                    .allMatch(e -> e.message().toLowerCase().contains("banana"));
        }

        @Test
        @DisplayName("Should filter by session")
        void shouldFilterBySession() {
            viewModel.startExecution("exec-1", "Workflow 1");
            viewModel.info("exec-1", "Message 1", null);

            viewModel.startExecution("exec-2", "Workflow 2");
            viewModel.info("exec-2", "Message 2", null);

            viewModel.selectedSessionIdProperty().set("exec-1");
            viewModel.refreshVisibleEntries();

            List<LogEntryModel> visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible).isNotEmpty()
                    .allMatch(e -> e.executionId().equals("exec-1"));
        }

        @Test
        @DisplayName("Should show all sessions when none selected")
        void shouldShowAllSessionsWhenNoneSelected() {
            viewModel.startExecution("exec-1", "Workflow 1");
            viewModel.info("exec-1", "Message 1", null);

            viewModel.startExecution("exec-2", "Workflow 2");
            viewModel.info("exec-2", "Message 2", null);

            viewModel.selectedSessionIdProperty().set(null);
            viewModel.refreshVisibleEntries();

            List<LogEntryModel> visible = new ArrayList<>(viewModel.visibleEntriesProperty());
            assertThat(visible.stream().map(LogEntryModel::executionId).distinct().count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should update filter status")
        void shouldUpdateFilterStatus() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Message", null);

            viewModel.showDebugProperty().set(true);
            viewModel.filterTextProperty().set("test");
            viewModel.refreshVisibleEntries();

            String status = viewModel.filterStatusProperty().get();
            assertThat(status).contains("DEBUG", "test");
        }
    }

    // =========================
    // Statistics Tests
    // =========================

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should count INFO entries")
        void shouldCountInfoEntries() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info 1", null);
            viewModel.info("exec-1", "Info 2", null);
            viewModel.info("exec-1", "Info 3", null);

            assertThat(viewModel.infoCountProperty().get()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count WARN entries")
        void shouldCountWarnEntries() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.retry("exec-1", 1, 3, 100);
            viewModel.rateLimit("exec-1", "bucket", true, 50);

            assertThat(viewModel.warnCountProperty().get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count ERROR entries")
        void shouldCountErrorEntries() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.error("exec-1", "Source1", "Error 1", null);
            viewModel.error("exec-1", "Source2", "Error 2", null);

            assertThat(viewModel.errorCountProperty().get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count total entries")
        void shouldCountTotalEntries() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info", null);
            viewModel.debug("exec-1", "Debug", null);
            viewModel.error("exec-1", "Src", "Error", null);

            assertThat(viewModel.totalEntryCountProperty().get()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should count visible entries")
        void shouldCountVisibleEntries() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info", null);
            viewModel.debug("exec-1", "Debug", null); // Shown by default now

            viewModel.refreshVisibleEntries();
            // Both INFO and DEBUG are visible (DEBUG is enabled by default)
            assertThat(viewModel.visibleEntryCountProperty().get()).isEqualTo(2);

            // Disable DEBUG to hide it
            viewModel.showDebugProperty().set(false);
            viewModel.refreshVisibleEntries();
            assertThat(viewModel.visibleEntryCountProperty().get()).isEqualTo(1);
        }
    }

    // =========================
    // Clear & Copy Tests
    // =========================

    @Nested
    @DisplayName("Clear and Copy Operations")
    class ClearCopyTests {

        @Test
        @DisplayName("Should clear all sessions")
        void shouldClearAllSessions() {
            viewModel.startExecution("exec-1", "Workflow 1");
            viewModel.startExecution("exec-2", "Workflow 2");
            viewModel.info("exec-1", "Message", null);

            viewModel.clear();

            assertThat(viewModel.sessionCountProperty().get()).isZero();
            assertThat(viewModel.sessionIdsProperty()).isEmpty();
            assertThat(viewModel.selectedSessionIdProperty().get()).isNull();
            assertThat(viewModel.visibleEntriesProperty()).isEmpty();
        }

        @Test
        @DisplayName("Should reset statistics on clear")
        void shouldResetStatisticsOnClear() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Info", null);
            viewModel.error("exec-1", "Src", "Error", null);

            viewModel.clear();

            assertThat(viewModel.infoCountProperty().get()).isZero();
            assertThat(viewModel.errorCountProperty().get()).isZero();
            assertThat(viewModel.totalEntryCountProperty().get()).isZero();
        }

        @Test
        @DisplayName("Should export logs as text")
        void shouldExportLogsAsText() {
            viewModel.startExecution("exec-1", "Test Workflow");
            viewModel.info("exec-1", "Test message", "Test details");

            String text = viewModel.getLogsAsText();
            assertThat(text).contains("Test Workflow");
            assertThat(text).contains("Test message");
        }

        @Test
        @DisplayName("Should export as text format")
        void shouldExportAsTextFormat() {
            viewModel.startExecution("exec-1", "Export Workflow");
            viewModel.info("exec-1", "Export message", null);

            String export = viewModel.exportAsText();
            assertThat(export).contains("Execution Console Export");
            assertThat(export).contains("Export Workflow");
            assertThat(export).contains("[INFO]");
        }

        @Test
        @DisplayName("Should export as JSON format")
        void shouldExportAsJsonFormat() {
            viewModel.startExecution("exec-1", "JSON Workflow");
            viewModel.info("exec-1", "JSON message", null);

            String json = viewModel.exportAsJson();
            assertThat(json).contains("\"workflowName\"");
            assertThat(json).contains("JSON Workflow");
            assertThat(json).contains("\"level\": \"INFO\"");
        }
    }

    // =========================
    // Display Settings Tests
    // =========================

    @Nested
    @DisplayName("Display Settings")
    class DisplaySettingsTests {

        @Test
        @DisplayName("Should have auto-scroll enabled by default")
        void shouldHaveAutoScrollEnabledByDefault() {
            assertThat(viewModel.autoScrollProperty().get()).isTrue();
        }

        @Test
        @DisplayName("Should have timestamps enabled by default")
        void shouldHaveTimestampsEnabledByDefault() {
            assertThat(viewModel.showTimestampsProperty().get()).isTrue();
        }

        @Test
        @DisplayName("Should have line numbers disabled by default")
        void shouldHaveLineNumbersDisabledByDefault() {
            assertThat(viewModel.showLineNumbersProperty().get()).isFalse();
        }

        @Test
        @DisplayName("Should toggle auto-scroll")
        void shouldToggleAutoScroll() {
            viewModel.autoScrollProperty().set(false);
            assertThat(viewModel.autoScrollProperty().get()).isFalse();

            viewModel.autoScrollProperty().set(true);
            assertThat(viewModel.autoScrollProperty().get()).isTrue();
        }
    }

    // =========================
    // Event Listener Tests
    // =========================

    @Nested
    @DisplayName("Event Listeners")
    class EventListenerTests {

        @Test
        @DisplayName("Should notify on session started")
        void shouldNotifyOnSessionStarted() {
            AtomicBoolean notified = new AtomicBoolean(false);
            AtomicReference<String> capturedId = new AtomicReference<>();

            viewModel.addEventListener(new ConsoleEventListener() {
                @Override
                public void onSessionStarted(String executionId, String workflowName) {
                    notified.set(true);
                    capturedId.set(executionId);
                }
            });

            viewModel.startExecution("test-exec", "Test Workflow");

            assertThat(notified.get()).isTrue();
            assertThat(capturedId.get()).isEqualTo("test-exec");
        }

        @Test
        @DisplayName("Should notify on session ended")
        void shouldNotifyOnSessionEnded() {
            AtomicBoolean notified = new AtomicBoolean(false);
            AtomicReference<Boolean> capturedSuccess = new AtomicReference<>();

            viewModel.addEventListener(new ConsoleEventListener() {
                @Override
                public void onSessionEnded(String executionId, boolean success) {
                    notified.set(true);
                    capturedSuccess.set(success);
                }
            });

            viewModel.startExecution("test-exec", "Test Workflow");
            viewModel.endExecution("test-exec", true, 100);

            assertThat(notified.get()).isTrue();
            assertThat(capturedSuccess.get()).isTrue();
        }

        @Test
        @DisplayName("Should notify on entry added")
        void shouldNotifyOnEntryAdded() {
            AtomicInteger addedCount = new AtomicInteger(0);

            viewModel.addEventListener(new ConsoleEventListener() {
                @Override
                public void onEntryAdded(LogEntryModel entry) {
                    addedCount.incrementAndGet();
                }
            });

            viewModel.startExecution("test-exec", "Test Workflow");
            viewModel.info("test-exec", "Message 1", null);
            viewModel.info("test-exec", "Message 2", null);

            assertThat(addedCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should notify on cleared")
        void shouldNotifyOnCleared() {
            AtomicBoolean notified = new AtomicBoolean(false);

            viewModel.addEventListener(new ConsoleEventListener() {
                @Override
                public void onCleared() {
                    notified.set(true);
                }
            });

            viewModel.startExecution("test-exec", "Test Workflow");
            viewModel.clear();

            assertThat(notified.get()).isTrue();
        }

        @Test
        @DisplayName("Should remove event listener")
        void shouldRemoveEventListener() {
            AtomicInteger count = new AtomicInteger(0);

            ConsoleEventListener listener = new ConsoleEventListener() {
                @Override
                public void onSessionStarted(String executionId, String workflowName) {
                    count.incrementAndGet();
                }
            };

            viewModel.addEventListener(listener);
            viewModel.startExecution("exec-1", "Workflow");
            assertThat(count.get()).isEqualTo(1);

            viewModel.removeEventListener(listener);
            viewModel.startExecution("exec-2", "Workflow");
            assertThat(count.get()).isEqualTo(1); // Should not increment
        }
    }

    // =========================
    // Helper Method Tests
    // =========================

    @Nested
    @DisplayName("Helper Methods")
    class HelperMethodTests {

        @Test
        @DisplayName("Should format duration in milliseconds")
        void shouldFormatDurationInMilliseconds() {
            assertThat(viewModel.formatDuration(500)).isEqualTo("500ms");
        }

        @Test
        @DisplayName("Should format duration in seconds")
        void shouldFormatDurationInSeconds() {
            assertThat(viewModel.formatDuration(2500)).isEqualTo("2.50s");
        }

        @Test
        @DisplayName("Should format duration in minutes")
        void shouldFormatDurationInMinutes() {
            assertThat(viewModel.formatDuration(125000)).isEqualTo("2m 5s");
        }

        @Test
        @DisplayName("Should format timestamp")
        void shouldFormatTimestamp() {
            Instant now = Instant.now();
            String formatted = viewModel.formatTimestamp(now);

            assertThat(formatted).matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
        }
    }

    // =========================
    // ExecutionSessionModel Tests
    // =========================

    @Nested
    @DisplayName("ExecutionSessionModel")
    class ExecutionSessionModelTests {

        @Test
        @DisplayName("Should calculate progress")
        void shouldCalculateProgress() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");
            session.pushNode("node-1");
            session.pushNode("node-2");
            session.pushNode("node-3");
            session.pushNode("node-4");

            session.popNode("node-1");
            session.popNode("node-2");

            assertThat(session.getProgress()).isEqualTo(0.5); // 2/4 = 50%
        }

        @Test
        @DisplayName("Should get status text when running")
        void shouldGetStatusTextWhenRunning() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");
            assertThat(session.getStatusText()).isEqualTo("running");
        }

        @Test
        @DisplayName("Should get status text when completed")
        void shouldGetStatusTextWhenCompleted() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");
            session.setComplete(true);
            session.setSuccess(true);
            assertThat(session.getStatusText()).isEqualTo("completed");
        }

        @Test
        @DisplayName("Should get status text when failed")
        void shouldGetStatusTextWhenFailed() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");
            session.setComplete(true);
            session.setSuccess(false);
            assertThat(session.getStatusText()).isEqualTo("failed");
        }

        @Test
        @DisplayName("Should track node depth")
        void shouldTrackNodeDepth() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");

            int depth1 = session.pushNode("node-1");
            int depth2 = session.pushNode("node-2");
            int depth3 = session.pushNode("node-3");

            assertThat(depth1).isZero();
            assertThat(depth2).isEqualTo(1);
            assertThat(depth3).isEqualTo(2);

            assertThat(session.getCurrentDepth()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return running duration")
        void shouldReturnRunningDuration() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");

            // Running duration should be > 0
            assertThat(session.getRunningDurationMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should return set duration when complete")
        void shouldReturnSetDurationWhenComplete() {
            ExecutionSessionModel session = new ExecutionSessionModel("exec-1", "Test");
            session.setComplete(true);
            session.setDurationMs(5000);

            assertThat(session.getRunningDurationMs()).isEqualTo(5000);
        }
    }

    // =========================
    // LogEntryModel Tests
    // =========================

    @Nested
    @DisplayName("LogEntryModel")
    class LogEntryModelTests {

        @Test
        @DisplayName("Should create log entry model")
        void shouldCreateLogEntryModel() {
            LogEntryModel entry = new LogEntryModel(
                    Instant.now(),
                    2,
                    LogEntryType.INFO,
                    "Test message",
                    "Test details",
                    "exec-1");

            assertThat(entry.depth()).isEqualTo(2);
            assertThat(entry.type()).isEqualTo(LogEntryType.INFO);
            assertThat(entry.message()).isEqualTo("Test message");
            assertThat(entry.details()).isEqualTo("Test details");
            assertThat(entry.executionId()).isEqualTo("exec-1");
        }

        @Test
        @DisplayName("Should allow null details")
        void shouldAllowNullDetails() {
            LogEntryModel entry = new LogEntryModel(
                    Instant.now(),
                    0,
                    LogEntryType.INFO,
                    "Message",
                    null,
                    "exec-1");

            assertThat(entry.details()).isNull();
        }
    }
}
