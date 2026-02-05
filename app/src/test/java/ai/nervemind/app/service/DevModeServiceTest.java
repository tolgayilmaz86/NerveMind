/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Unit tests for {@link DevModeService}.
 * 
 * <p>
 * Tests all developer mode features including:
 * <ul>
 * <li>Dev mode enable/disable checks</li>
 * <li>Node execution timing</li>
 * <li>Debug bundle export</li>
 * <li>HTTP request logging</li>
 * <li>Expression context inspection</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DevModeService Tests")
class DevModeServiceTest {

    @Mock
    private SettingsServiceInterface settingsService;

    private ObjectMapper objectMapper;
    private DevModeService devModeService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        devModeService = new DevModeService(settingsService, objectMapper);
    }

    // ========== Core Dev Mode Check Tests ==========

    @Nested
    @DisplayName("Dev Mode State")
    class DevModeStateTests {

        @Test
        @DisplayName("isDevModeEnabled returns true when setting is enabled")
        void isDevModeEnabledReturnsTrue() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            boolean result = devModeService.isDevModeEnabled();

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isDevModeEnabled returns false when setting is disabled")
        void isDevModeEnabledReturnsFalse() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            boolean result = devModeService.isDevModeEnabled();

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("ifDevMode executes action when enabled")
        void ifDevModeExecutesActionWhenEnabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            var counter = new int[] { 0 };
            devModeService.ifDevMode(() -> counter[0]++);

            assertThat(counter[0]).isEqualTo(1);
        }

        @Test
        @DisplayName("ifDevMode does not execute action when disabled")
        void ifDevModeDoesNotExecuteActionWhenDisabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            var counter = new int[] { 0 };
            devModeService.ifDevMode(() -> counter[0]++);

            assertThat(counter[0]).isEqualTo(0);
        }
    }

    // ========== Node Timing Tests ==========

    @Nested
    @DisplayName("Node Timing")
    class NodeTimingTests {

        @Test
        @DisplayName("records node start and end timing")
        void recordsNodeTiming() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.recordNodeStart("node1", "HTTP Request", "httpRequest");

            // Small delay to ensure measurable duration
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long duration = devModeService.recordNodeEnd("node1", true, null);

            assertThat(duration).isGreaterThanOrEqualTo(10);
            assertThat(devModeService.getTimedNodeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns -1 when dev mode is disabled")
        void returnsNegativeOneWhenDisabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            devModeService.recordNodeStart("node1", "HTTP Request", "httpRequest");
            long duration = devModeService.recordNodeEnd("node1", true, null);

            assertThat(duration).isEqualTo(-1);
        }

        @Test
        @DisplayName("clearTimings removes all timing data")
        void clearTimingsRemovesAllData() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.recordNodeStart("node1", "Node 1", "type1");
            devModeService.recordNodeEnd("node1", true, null);
            devModeService.recordNodeStart("node2", "Node 2", "type2");
            devModeService.recordNodeEnd("node2", true, null);

            assertThat(devModeService.getTimedNodeCount()).isEqualTo(2);

            devModeService.clearTimings();

            assertThat(devModeService.getTimedNodeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("formatTimingSummary returns formatted string")
        void formatTimingSummaryReturnsFormattedString() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.recordNodeStart("node1", "HTTP Request", "httpRequest");
            devModeService.recordNodeEnd("node1", true, null);

            String summary = devModeService.formatTimingSummary();

            assertThat(summary)
                    .contains("Node Execution Timing")
                    .contains("HTTP Request")
                    .contains("httpRequest")
                    .contains("ms");
        }

        @Test
        @DisplayName("formatTimingSummary shows message when no data")
        void formatTimingSummaryShowsNoDataMessage() {
            String summary = devModeService.formatTimingSummary();

            assertThat(summary).contains("No timing data available");
        }

        @Test
        @DisplayName("getTotalExecutionTime sums all node durations")
        void getTotalExecutionTimeSumsAllDurations() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.recordNodeStart("node1", "Node 1", "type1");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            devModeService.recordNodeEnd("node1", true, null);

            devModeService.recordNodeStart("node2", "Node 2", "type2");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            devModeService.recordNodeEnd("node2", true, null);

            long total = devModeService.getTotalExecutionTime();

            assertThat(total).isGreaterThanOrEqualTo(20);
        }

        @Test
        @DisplayName("records failed node with error message")
        void recordsFailedNodeWithError() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.recordNodeStart("node1", "HTTP Request", "httpRequest");
            devModeService.recordNodeEnd("node1", false, "Connection timeout");

            var timing = devModeService.getNodeTiming("node1");

            assertThat(timing).isNotNull();
            assertThat(timing.success()).isFalse();
            assertThat(timing.errorMessage()).isEqualTo("Connection timeout");
        }
    }

    // ========== HTTP Logging Tests ==========

    @Nested
    @DisplayName("HTTP Request Logging")
    class HttpLoggingTests {

        @Test
        @DisplayName("logs HTTP request when dev mode enabled")
        void logsHttpRequestWhenEnabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            Map<String, String> reqHeaders = Map.of("Content-Type", "application/json");
            Map<String, String> resHeaders = Map.of("X-Request-Id", "123");

            devModeService.logHttpRequest(
                    "node1", "POST", "https://api.example.com/data",
                    reqHeaders, "{\"key\": \"value\"}",
                    200, resHeaders, "{\"result\": \"ok\"}",
                    150);

            var logs = devModeService.getHttpRequestLogs();

            assertThat(logs).hasSize(1);
        }

        @Test
        @DisplayName("does not log HTTP request when dev mode disabled")
        void doesNotLogHttpRequestWhenDisabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            devModeService.logHttpRequest(
                    "node1", "GET", "https://api.example.com",
                    Map.of(), null,
                    200, Map.of(), "response",
                    100);

            var logs = devModeService.getHttpRequestLogs();

            assertThat(logs).isEmpty();
        }

        @Test
        @DisplayName("clearHttpRequestLogs removes all logs")
        void clearHttpRequestLogsRemovesAll() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.logHttpRequest(
                    "node1", "GET", "https://api.example.com",
                    Map.of(), null, 200, Map.of(), "response", 100);

            devModeService.clearHttpRequestLogs();

            assertThat(devModeService.getHttpRequestLogs()).isEmpty();
        }

        @Test
        @DisplayName("isVerboseHttpLoggingEnabled returns dev mode state")
        void isVerboseHttpLoggingEnabledReturnsDevModeState() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            boolean result = devModeService.isVerboseHttpLoggingEnabled();

            assertThat(result).isTrue();
        }
    }

    // ========== Debug Bundle Export Tests ==========

    @Nested
    @DisplayName("Debug Bundle Export")
    class DebugBundleExportTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("exports debug bundle when dev mode enabled")
        void exportsDebugBundleWhenEnabled() throws IOException {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            WorkflowDTO workflow = new WorkflowDTO(
                    1L, "Test Workflow", "A test workflow",
                    List.of(), List.of(), Map.of(),
                    true, ai.nervemind.common.enums.TriggerType.MANUAL, null,
                    java.time.Instant.now(), java.time.Instant.now(), null, 1);

            Path bundlePath = devModeService.exportDebugBundle(workflow, tempDir, null);

            assertThat(bundlePath).exists();
            assertThat(Files.size(bundlePath)).isGreaterThan(0);

            String content = Files.readString(bundlePath);
            assertThat(content)
                    .contains("Test Workflow")
                    .contains("devModeVersion")
                    .contains("systemInfo");
        }

        @Test
        @DisplayName("throws exception when dev mode disabled")
        void throwsExceptionWhenDisabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            WorkflowDTO workflow = new WorkflowDTO(
                    1L, "Test", "Description",
                    List.of(), List.of(), Map.of(),
                    true, ai.nervemind.common.enums.TriggerType.MANUAL, null,
                    java.time.Instant.now(), java.time.Instant.now(), null, 1);

            assertThatThrownBy(() -> devModeService.exportDebugBundle(workflow, tempDir, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("developer mode");
        }
    }

    // ========== Node JSON Export Tests ==========

    @Nested
    @DisplayName("Node JSON Export")
    class NodeJsonExportTests {

        @Test
        @DisplayName("exports node as formatted JSON")
        void exportsNodeAsFormattedJson() {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", "node123");
            node.put("type", "httpRequest");
            node.put("name", "Fetch Data");

            String json = devModeService.exportNodeAsJson(node);

            assertThat(json)
                    .contains("node123")
                    .contains("httpRequest")
                    .contains("Fetch Data");
        }

        @Test
        @DisplayName("returns error JSON for invalid object")
        void returnsErrorJsonForInvalidObject() {
            // Create an object that will fail serialization
            Object problematicObject = new Object() {
                @SuppressWarnings("unused")
                public Object getSelf() {
                    return this;
                } // Circular reference
            };

            String json = devModeService.exportNodeAsJson(problematicObject);

            assertThat(json).contains("error");
        }
    }

    // ========== Expression Context Tests ==========

    @Nested
    @DisplayName("Expression Context Inspection")
    class ExpressionContextTests {

        @Test
        @DisplayName("builds expression context from maps")
        void buildsExpressionContextFromMaps() {
            Map<String, Object> variables = Map.of("counter", 5);
            Map<String, Object> nodeOutputs = Map.of("node1", Map.of("data", "value"));
            Map<String, Object> workflowData = Map.of("name", "Test Workflow");

            var context = devModeService.buildExpressionContext(variables, nodeOutputs, workflowData);

            assertThat(context).isNotNull();
        }

        @Test
        @DisplayName("formats expression context as JSON")
        void formatsExpressionContextAsJson() {
            Map<String, Object> variables = Map.of("counter", 5);
            Map<String, Object> nodeOutputs = Map.of("node1", Map.of("data", "value"));
            Map<String, Object> workflowData = Map.of("name", "Test Workflow");

            var context = devModeService.buildExpressionContext(variables, nodeOutputs, workflowData);
            String json = devModeService.formatExpressionContext(context);

            assertThat(json)
                    .contains("counter")
                    .contains("node1")
                    .contains("Test Workflow");
        }
    }

    // ========== Execution Logging Tests ==========

    @Nested
    @DisplayName("Execution Logging")
    class ExecutionLoggingTests {

        @Test
        @DisplayName("logs execution event when dev mode enabled")
        void logsExecutionEventWhenEnabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.logExecutionEvent("INFO", "node1", "HTTP Request",
                    "Making API call", Map.of("url", "https://example.com"));

            var logs = devModeService.getExecutionLog();

            assertThat(logs).hasSize(1);
        }

        @Test
        @DisplayName("does not log execution event when dev mode disabled")
        void doesNotLogExecutionEventWhenDisabled() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            devModeService.logExecutionEvent("INFO", "node1", "Test", "Message", null);

            var logs = devModeService.getExecutionLog();

            assertThat(logs).isEmpty();
        }

        @Test
        @DisplayName("clearTimings also clears execution log")
        void clearTimingsAlsoClearsExecutionLog() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.logExecutionEvent("INFO", "node1", "Test", "Message", null);
            assertThat(devModeService.getExecutionLog()).isNotEmpty();

            devModeService.clearTimings();

            assertThat(devModeService.getExecutionLog()).isEmpty();
        }
    }

    // ========== Step Execution Tests ==========

    @Nested
    @DisplayName("Step Execution")
    class StepExecutionTests {

        @Test
        @DisplayName("step execution disabled by default")
        void stepExecutionDisabledByDefault() {
            assertThat(devModeService.isStepExecutionEnabled()).isFalse();
        }

        @Test
        @DisplayName("can enable and disable step execution")
        void canEnableAndDisableStepExecution() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.setStepExecutionEnabled(true);
            assertThat(devModeService.isStepExecutionEnabled()).isTrue();

            devModeService.setStepExecutionEnabled(false);
            assertThat(devModeService.isStepExecutionEnabled()).isFalse();
        }

        @Test
        @DisplayName("step execution requires dev mode enabled")
        void stepExecutionRequiresDevMode() {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(false);

            devModeService.setStepExecutionEnabled(true);

            // Even when step execution is set, it should be disabled if dev mode is off
            assertThat(devModeService.isStepExecutionEnabled()).isFalse();
        }

        @Test
        @DisplayName("continueStep releases waiting thread")
        void continueStepReleasesWaitingThread() throws Exception {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.setStepExecutionEnabled(true);

            // Start a thread that will wait
            java.util.concurrent.CompletableFuture<Boolean> waitFuture = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return devModeService.waitForStepContinue("node1", "Test Node");
                        } catch (InterruptedException e) {
                            return false;
                        }
                    });

            // Give the waiting thread time to start
            Thread.sleep(100);

            // Verify it's paused
            assertThat(devModeService.getPausedNodeId()).isEqualTo("node1");

            // Continue execution
            devModeService.continueStep();

            // Verify it continues
            Boolean result = waitFuture.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(result).isTrue();
            assertThat(devModeService.getPausedNodeId()).isNull();
        }

        @Test
        @DisplayName("cancelStepExecution stops execution")
        void cancelStepExecutionStopsExecution() throws Exception {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.setStepExecutionEnabled(true);

            // Start a thread that will wait
            java.util.concurrent.CompletableFuture<Boolean> waitFuture = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return devModeService.waitForStepContinue("node1", "Test Node");
                        } catch (InterruptedException e) {
                            return false;
                        }
                    });

            // Give the waiting thread time to start
            Thread.sleep(100);

            // Cancel execution
            devModeService.cancelStepExecution();

            // Verify it returns false (cancelled)
            Boolean result = waitFuture.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("resetStepExecution clears state")
        void resetStepExecutionClearsState() {
            // No need to stub settingsService - resetStepExecution doesn't check dev mode
            devModeService.resetStepExecution();

            assertThat(devModeService.getPausedNodeId()).isNull();
        }

        @Test
        @DisplayName("waitForStepContinue returns immediately when step mode disabled")
        void waitForStepContinueReturnsImmediatelyWhenDisabled() throws Exception {
            when(settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false))
                    .thenReturn(true);

            devModeService.setStepExecutionEnabled(false);

            // Should return immediately without blocking
            boolean result = devModeService.waitForStepContinue("node1", "Test Node");
            assertThat(result).isTrue();
            assertThat(devModeService.getPausedNodeId()).isNull();
        }
    }
}
