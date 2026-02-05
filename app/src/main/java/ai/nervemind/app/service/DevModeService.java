/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.DevModeServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Service for Developer Mode features.
 * 
 * <p>
 * Centralizes all developer mode checks and provides utilities for:
 * <ul>
 * <li>Node execution timing and performance metrics</li>
 * <li>Debug bundle export (workflow + execution logs)</li>
 * <li>HTTP request verbose logging</li>
 * <li>Expression context inspection</li>
 * </ul>
 * 
 * <p>
 * Developer mode is controlled by the {@code advanced.devMode} setting.
 * When enabled, additional debugging features become available throughout
 * the application.
 * 
 * @since 1.0.0
 * @see SettingsDefaults#ADVANCED_DEV_MODE
 */
@Service
public class DevModeService implements DevModeServiceInterface {

    private static final Logger LOGGER = Logger.getLogger(DevModeService.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final SettingsServiceInterface settingsService;
    private final ObjectMapper objectMapper;

    // Node timing data for current execution
    private final Map<String, NodeTimingInfo> nodeTimings = new ConcurrentHashMap<>();
    private final List<ExecutionLogEntry> executionLog = new ArrayList<>();

    // Verbose HTTP logging buffer
    private final List<HttpRequestLog> httpRequestLogs = new ArrayList<>();
    private static final int MAX_HTTP_LOGS = 100;

    // Step-by-step execution control
    private final AtomicBoolean stepExecutionEnabled = new AtomicBoolean(false);
    private final AtomicBoolean stepExecutionCancelled = new AtomicBoolean(false);
    private final AtomicReference<String> pausedNodeId = new AtomicReference<>(null);
    private volatile CountDownLatch stepLatch = null;

    // Step execution listeners (for UI notifications)
    private final List<StepExecutionListener> stepExecutionListeners = new CopyOnWriteArrayList<>();

    /**
     * Represents timing information for a single node execution.
     * 
     * @param nodeId       the unique identifier of the node
     * @param nodeName     the display name of the node
     * @param nodeType     the type of the node
     * @param startTimeMs  start time in milliseconds
     * @param endTimeMs    end time in milliseconds
     * @param durationMs   execution duration in milliseconds
     * @param success      true if execution was successful
     * @param errorMessage error message if failed
     */

    public record NodeTimingInfo(
            String nodeId,
            String nodeName,
            String nodeType,
            long startTimeMs,
            long endTimeMs,
            long durationMs,
            boolean success,
            String errorMessage) {

        /**
         * Create a started timing record.
         * 
         * @param nodeId   the node ID
         * @param nodeName the node name
         * @param nodeType the node type
         * @return a new started NodeTimingInfo
         */
        public static NodeTimingInfo started(String nodeId, String nodeName, String nodeType) {
            return new NodeTimingInfo(nodeId, nodeName, nodeType, System.currentTimeMillis(), 0, 0, false, null);
        }

        /**
         * Create a completed timing record from a started record.
         * 
         * @param success true if execution succeeded
         * @param error   error message if failed
         * @return a new completed NodeTimingInfo
         */
        public NodeTimingInfo completed(boolean success, String error) {
            long end = System.currentTimeMillis();
            return new NodeTimingInfo(nodeId, nodeName, nodeType, startTimeMs, end, end - startTimeMs, success, error);
        }
    }

    /**
     * Represents a log entry in the execution log.
     * 
     * @param timestamp log timestamp
     * @param level     log level (INFO, etc.)
     * @param nodeId    source node ID
     * @param nodeName  source node name
     * @param message   log message
     * @param data      associated data
     */
    public record ExecutionLogEntry(
            Instant timestamp,
            String level,
            String nodeId,
            String nodeName,
            String message,
            Object data) {
    }

    /**
     * Represents an HTTP request/response log entry.
     * 
     * @param timestamp           log timestamp
     * @param nodeId              source node ID
     * @param method              HTTP method
     * @param url                 request URL
     * @param requestHeaders      request headers
     * @param requestBodyPreview  truncated request body
     * @param responseStatus      HTTP status code
     * @param responseHeaders     response headers
     * @param responseBodyPreview truncated response body
     * @param durationMs          request duration
     */
    public record HttpRequestLog(
            Instant timestamp,
            String nodeId,
            String method,
            String url,
            Map<String, String> requestHeaders,
            String requestBodyPreview,
            int responseStatus,
            Map<String, String> responseHeaders,
            String responseBodyPreview,
            long durationMs) {
    }

    /**
     * Creates the DevModeService with required dependencies.
     *
     * @param settingsService the settings service for reading dev mode state
     * @param objectMapper    the JSON object mapper for serialization
     */
    public DevModeService(SettingsServiceInterface settingsService, ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ========== Core Dev Mode Check ==========

    /**
     * Check if developer mode is enabled.
     *
     * @return true if developer mode is enabled in settings
     */
    public boolean isDevModeEnabled() {
        return settingsService.getBoolean(SettingsDefaults.ADVANCED_DEV_MODE, false);
    }

    /**
     * Execute an action only if dev mode is enabled.
     *
     * @param action the action to execute
     */
    public void ifDevMode(Runnable action) {
        if (isDevModeEnabled()) {
            action.run();
        }
    }

    // ========== Node Timing ==========

    /**
     * Record the start of a node execution.
     *
     * @param nodeId   the node ID
     * @param nodeName the node display name
     * @param nodeType the node type
     */
    public void recordNodeStart(String nodeId, String nodeName, String nodeType) {
        if (isDevModeEnabled()) {
            nodeTimings.put(nodeId, NodeTimingInfo.started(nodeId, nodeName, nodeType));
            logExecutionEvent("INFO", nodeId, nodeName, "Node execution started", null);
        }
    }

    /**
     * Record the end of a node execution.
     *
     * @param nodeId  the node ID
     * @param success whether execution succeeded
     * @param error   error message if failed
     * @return the duration in milliseconds, or -1 if not tracked
     */
    public long recordNodeEnd(String nodeId, boolean success, String error) {
        if (isDevModeEnabled()) {
            NodeTimingInfo timing = nodeTimings.get(nodeId);
            if (timing != null) {
                NodeTimingInfo completed = timing.completed(success, error);
                nodeTimings.put(nodeId, completed);

                String message = success
                        ? String.format("Node completed in %dms", completed.durationMs())
                        : String.format("Node failed after %dms: %s", completed.durationMs(), error);
                logExecutionEvent(success ? "INFO" : "ERROR", nodeId, timing.nodeName(), message, null);

                return completed.durationMs();
            }
        }
        return -1;
    }

    /**
     * Get timing information for a node.
     *
     * @param nodeId the node ID
     * @return timing info, or null if not available
     */
    public NodeTimingInfo getNodeTiming(String nodeId) {
        return nodeTimings.get(nodeId);
    }

    /**
     * Get all node timings for the current execution.
     *
     * @return map of node ID to timing info
     */
    public Map<String, NodeTimingInfo> getAllNodeTimings() {
        return new LinkedHashMap<>(nodeTimings);
    }

    /**
     * Clear all timing data (call at execution start).
     */
    public void clearTimings() {
        nodeTimings.clear();
        executionLog.clear();
    }

    /**
     * Format a timing summary for console display.
     *
     * @return formatted timing summary string
     */
    public String formatTimingSummary() {
        if (nodeTimings.isEmpty()) {
            return "No timing data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Node Execution Timing ===\n");

        long totalDuration = 0;
        for (NodeTimingInfo timing : nodeTimings.values()) {
            if (timing.endTimeMs() > 0) {
                String status = timing.success() ? "✓" : "✗";
                sb.append(String.format("  %s %s (%s): %dms%n",
                        status,
                        timing.nodeName(),
                        timing.nodeType(),
                        timing.durationMs()));
                totalDuration += timing.durationMs();
            }
        }

        sb.append(String.format("  ─────────────────────────────%n"));
        sb.append(String.format("  Total: %dms%n", totalDuration));

        return sb.toString();
    }

    // ========== Execution Logging ==========

    /**
     * Log an execution event.
     *
     * @param level    log level (INFO, WARN, ERROR, DEBUG)
     * @param nodeId   node ID (can be null for workflow-level events)
     * @param nodeName node name (can be null)
     * @param message  log message
     * @param data     optional data object to include
     */
    public void logExecutionEvent(String level, String nodeId, String nodeName, String message, Object data) {
        if (isDevModeEnabled()) {
            synchronized (executionLog) {
                executionLog.add(new ExecutionLogEntry(
                        Instant.now(), level, nodeId, nodeName, message, data));
            }
        }
    }

    /**
     * Get all execution log entries.
     *
     * @return list of log entries
     */
    public List<ExecutionLogEntry> getExecutionLog() {
        synchronized (executionLog) {
            return new ArrayList<>(executionLog);
        }
    }

    // ========== HTTP Verbose Logging ==========

    /**
     * Log an HTTP request/response for verbose mode.
     *
     * @param nodeId          the node ID making the request
     * @param method          HTTP method
     * @param url             request URL
     * @param requestHeaders  request headers
     * @param requestBody     request body (truncated for preview)
     * @param responseStatus  response status code
     * @param responseHeaders response headers
     * @param responseBody    response body (truncated for preview)
     * @param durationMs      request duration
     */
    public void logHttpRequest(String nodeId, String method, String url,
            Map<String, String> requestHeaders, String requestBody,
            int responseStatus, Map<String, String> responseHeaders, String responseBody,
            long durationMs) {
        if (!isDevModeEnabled()) {
            return;
        }

        synchronized (httpRequestLogs) {
            // Limit the log size
            if (httpRequestLogs.size() >= MAX_HTTP_LOGS) {
                httpRequestLogs.removeFirst();
            }

            // Truncate bodies for preview
            String reqBodyPreview = truncateForPreview(requestBody, 500);
            String resBodyPreview = truncateForPreview(responseBody, 500);

            httpRequestLogs.add(new HttpRequestLog(
                    Instant.now(),
                    nodeId,
                    method,
                    url,
                    requestHeaders,
                    reqBodyPreview,
                    responseStatus,
                    responseHeaders,
                    resBodyPreview,
                    durationMs));
        }
    }

    /**
     * Get HTTP request logs.
     *
     * @return list of HTTP request logs
     */
    @Override
    public List<HttpRequestLog> getHttpRequestLogs() {
        synchronized (httpRequestLogs) {
            return new ArrayList<>(httpRequestLogs);
        }
    }

    /**
     * Format HTTP request logs as a readable string.
     *
     * @return formatted HTTP logs string
     */
    @Override
    public String formatHttpLogs() {
        List<HttpRequestLog> logs = getHttpRequestLogs();
        StringBuilder sb = new StringBuilder();

        if (logs.isEmpty()) {
            sb.append("No HTTP requests logged yet.\n\n");
            sb.append("HTTP requests made by HTTP Request nodes will appear here\n");
            sb.append("when Developer Mode is enabled.");
        } else {
            for (HttpRequestLog log : logs) {
                sb.append(String.format("=== %s %s ===%n", log.method(), log.url()));
                sb.append(String.format("Time: %s | Duration: %dms | Status: %d%n",
                        log.timestamp(), log.durationMs(), log.responseStatus()));
                if (log.requestBodyPreview() != null) {
                    sb.append(String.format("Request Body: %s%n", log.requestBodyPreview()));
                }
                if (log.responseBodyPreview() != null) {
                    sb.append(String.format("Response Body: %s%n", log.responseBodyPreview()));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Clear HTTP request logs.
     */
    public void clearHttpRequestLogs() {
        synchronized (httpRequestLogs) {
            httpRequestLogs.clear();
        }
    }

    // ========== Debug Bundle Export ==========

    /**
     * Export a debug bundle containing workflow, execution data, and logs.
     *
     * @param workflow    the workflow to include
     * @param outputPath  the output directory path
     * @param executionId the execution ID (can be null)
     * @return the path to the created bundle file
     * @throws IOException if export fails
     */
    public Path exportDebugBundle(WorkflowDTO workflow, Path outputPath, String executionId) throws IOException {
        if (!isDevModeEnabled()) {
            throw new IllegalStateException("Debug bundle export is only available in developer mode");
        }

        // Create bundle filename with timestamp
        String timestamp = TIMESTAMP_FORMAT.format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        String bundleName = String.format("debug-bundle_%s_%s.json",
                sanitizeFilename(workflow.name()),
                timestamp);

        Path bundlePath = outputPath.resolve(bundleName);

        // Build bundle data
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportedAt", Instant.now().toString());
        bundle.put("devModeVersion", "1.0");

        // Workflow data
        bundle.put("workflow", workflow);

        // Execution timing
        bundle.put("nodeTimings", getAllNodeTimings());

        // Execution log
        bundle.put("executionLog", getExecutionLog());

        // HTTP logs
        bundle.put("httpRequestLogs", getHttpRequestLogs());

        // System info
        Map<String, String> systemInfo = new LinkedHashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        systemInfo.put("maxMemoryMB", String.valueOf(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        bundle.put("systemInfo", systemInfo);

        // Write bundle to file
        Files.createDirectories(outputPath);
        String json = objectMapper.writeValueAsString(bundle);
        Files.writeString(bundlePath, json, StandardCharsets.UTF_8);

        LOGGER.info(() -> "Debug bundle exported to: " + bundlePath);

        return bundlePath;
    }

    // ========== Node JSON Export ==========

    /**
     * Export a node's configuration as JSON string.
     *
     * @param node the node to export
     * @return JSON string representation
     */
    public String exportNodeAsJson(Object node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            LOGGER.warning("Failed to export node as JSON: " + e.getMessage());
            return "{ \"error\": \"Failed to serialize node\" }";
        }
    }

    // ========== Expression Context Inspection ==========

    /**
     * Represents expression context data for debugging.
     * 
     * @param variables    current workflow variables
     * @param nodeOutputs  outputs from previous nodes
     * @param workflowData workflow-level data
     */
    public record ExpressionContext(
            Map<String, Object> variables,
            Map<String, Object> nodeOutputs,
            Map<String, Object> workflowData) {
    }

    /**
     * Build expression context for inspection.
     *
     * @param variables    current workflow variables
     * @param nodeOutputs  outputs from previous nodes
     * @param workflowData workflow-level data
     * @return formatted expression context
     */
    @Override
    public Object buildExpressionContext(
            Map<String, Object> variables,
            Map<String, Object> nodeOutputs,
            Map<String, Object> workflowData) {
        return new ExpressionContext(variables, nodeOutputs, workflowData);
    }

    /**
     * Format expression context as readable string.
     *
     * @param context the expression context
     * @return formatted string
     */
    @Override
    public String formatExpressionContext(Object context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception _) {
            return "{ \"error\": \"Failed to format context\" }";
        }
    }

    // ========== Utility Methods ==========

    /**
     * Truncate a string for preview display.
     *
     * @param text      the text to truncate
     * @param maxLength maximum length
     * @return truncated text with ellipsis if needed
     */
    private String truncateForPreview(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * Sanitize a string for use as a filename.
     *
     * @param name the name to sanitize
     * @return sanitized filename-safe string
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }

    /**
     * Check if verbose HTTP logging is enabled.
     * This is a subset of dev mode for HTTP-specific logging.
     *
     * @return true if verbose HTTP logging should be used
     */
    @Override
    public boolean isVerboseHttpLoggingEnabled() {
        return isDevModeEnabled();
    }

    /**
     * Get the total number of nodes timed in current execution.
     *
     * @return count of timed nodes
     */
    @Override
    public int getTimedNodeCount() {
        return nodeTimings.size();
    }

    /**
     * Get the total execution time of all completed nodes.
     *
     * @return total duration in milliseconds
     */
    @Override
    public long getTotalExecutionTime() {
        return nodeTimings.values().stream()
                .filter(t -> t.endTimeMs() > 0)
                .mapToLong(NodeTimingInfo::durationMs)
                .sum();
    }

    // ========== Step-by-Step Execution ==========

    /**
     * Check if step-by-step execution mode is enabled.
     *
     * @return true if step mode is active
     */
    @Override
    public boolean isStepExecutionEnabled() {
        return isDevModeEnabled() && stepExecutionEnabled.get();
    }

    /**
     * Enable or disable step-by-step execution mode.
     *
     * @param enabled true to enable step mode
     */
    @Override
    public void setStepExecutionEnabled(boolean enabled) {
        stepExecutionEnabled.set(enabled);
        LOGGER.info(() -> "Step execution mode " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Wait for step continuation if in step mode.
     * Called by execution service after each node.
     *
     * @param nodeId   the node that just completed
     * @param nodeName the node name
     * @return true if execution should continue, false if cancelled
     * @throws InterruptedException if waiting is interrupted
     */
    @Override
    public boolean waitForStepContinue(String nodeId, String nodeName) throws InterruptedException {
        if (!isStepExecutionEnabled()) {
            return true; // Not in step mode, continue immediately
        }

        pausedNodeId.set(nodeId);
        stepLatch = new CountDownLatch(1);

        LOGGER.info(() -> String.format("Step execution paused after node: %s (%s)", nodeName, nodeId));

        // Notify listeners that execution is paused
        // Note: nodeIndex and totalNodes are passed as -1 since we don't track those
        // here
        // The UI can determine these from the workflow model if needed
        notifyExecutionPaused(nodeId, nodeName, -1, -1);

        // Wait for continue signal
        stepLatch.await();

        pausedNodeId.set(null);

        // Check if cancelled
        if (stepExecutionCancelled.get()) {
            LOGGER.info("Step execution cancelled by user");
            return false;
        }

        // Notify listeners that execution has resumed
        notifyExecutionResumed();

        return true;
    }

    /**
     * Signal to continue to the next step.
     * Called by UI when user clicks Continue.
     */
    @Override
    public void continueStep() {
        CountDownLatch latch = stepLatch;
        if (latch != null) {
            LOGGER.info("Continuing step execution");
            latch.countDown();
        }
    }

    /**
     * Signal to cancel step execution.
     * Called by UI when user clicks Cancel/Stop.
     */
    @Override
    public void cancelStepExecution() {
        stepExecutionCancelled.set(true);
        CountDownLatch latch = stepLatch;
        if (latch != null) {
            latch.countDown();
        }
        notifyExecutionResumed();
    }

    /**
     * Reset step execution state (call at execution start).
     */
    @Override
    public void resetStepExecution() {
        stepExecutionCancelled.set(false);
        pausedNodeId.set(null);
        stepLatch = null;
    }

    /**
     * Get the node ID that is currently paused (if any).
     *
     * @return node ID of paused node, or null if not paused
     */
    @Override
    public String getPausedNodeId() {
        return pausedNodeId.get();
    }

    // ========== Step Execution Listeners ==========

    /**
     * Adds a listener for step execution events.
     * 
     * @param listener the listener to add
     */
    @Override
    public void addStepExecutionListener(StepExecutionListener listener) {
        if (listener != null) {
            stepExecutionListeners.add(listener);
        }
    }

    /**
     * Removes a step execution listener.
     * 
     * @param listener the listener to remove
     */
    @Override
    public void removeStepExecutionListener(StepExecutionListener listener) {
        stepExecutionListeners.remove(listener);
    }

    /**
     * Notifies all listeners that execution is paused at a node.
     */
    private void notifyExecutionPaused(String nodeId, String nodeName, int nodeIndex, int totalNodes) {
        for (StepExecutionListener listener : stepExecutionListeners) {
            try {
                listener.onExecutionPaused(nodeId, nodeName, nodeIndex, totalNodes);
            } catch (Exception e) {
                LOGGER.warning("Error notifying step execution listener: " + e.getMessage());
            }
        }
    }

    /**
     * Notifies all listeners that execution has resumed or completed.
     */
    private void notifyExecutionResumed() {
        for (StepExecutionListener listener : stepExecutionListeners) {
            try {
                listener.onExecutionResumed();
            } catch (Exception e) {
                LOGGER.warning("Error notifying step execution listener: " + e.getMessage());
            }
        }
    }
}
