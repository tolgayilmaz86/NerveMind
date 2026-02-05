/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import ai.nervemind.common.dto.WorkflowDTO;

/**
 * Interface for Developer Mode features.
 * 
 * <p>
 * Provides utilities for debugging, performance analysis, and troubleshooting:
 * <ul>
 * <li>Node execution timing and metrics</li>
 * <li>Debug bundle export</li>
 * <li>Verbose HTTP logging</li>
 * <li>Expression context inspection</li>
 * <li>Step-by-step execution with UI listeners</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public interface DevModeServiceInterface {

    // ========== Step Execution Listener ==========

    /**
     * Listener interface for step execution events.
     * UI components can implement this to receive notifications when execution
     * pauses.
     */
    interface StepExecutionListener {
        /**
         * Called when execution pauses at a node.
         * 
         * @param nodeId     the ID of the paused node
         * @param nodeName   the display name of the paused node
         * @param nodeIndex  the current node index (1-based)
         * @param totalNodes the total number of nodes in the workflow
         */
        void onExecutionPaused(String nodeId, String nodeName, int nodeIndex, int totalNodes);

        /**
         * Called when step execution is cancelled or completed.
         */
        void onExecutionResumed();
    }

    /**
     * Adds a listener for step execution events.
     * 
     * @param listener the listener to add
     */
    void addStepExecutionListener(StepExecutionListener listener);

    /**
     * Removes a step execution listener.
     * 
     * @param listener the listener to remove
     */
    void removeStepExecutionListener(StepExecutionListener listener);

    // ========== Core Dev Mode Check ==========

    /**
     * Check if developer mode is enabled.
     *
     * @return true if developer mode is enabled in settings
     */
    boolean isDevModeEnabled();

    /**
     * Execute an action only if dev mode is enabled.
     *
     * @param action the action to execute
     */
    void ifDevMode(Runnable action);

    // ========== Node Timing ==========

    /**
     * Record the start of a node execution.
     *
     * @param nodeId   the node ID
     * @param nodeName the node display name
     * @param nodeType the node type
     */
    void recordNodeStart(String nodeId, String nodeName, String nodeType);

    /**
     * Record the end of a node execution.
     *
     * @param nodeId  the node ID
     * @param success whether execution succeeded
     * @param error   error message if failed
     * @return the duration in milliseconds, or -1 if not tracked
     */
    long recordNodeEnd(String nodeId, boolean success, String error);

    /**
     * Clear all timing data (call at execution start).
     */
    void clearTimings();

    /**
     * Format a timing summary for console display.
     *
     * @return formatted timing summary string
     */
    String formatTimingSummary();

    /**
     * Get the total number of nodes timed in current execution.
     *
     * @return count of timed nodes
     */
    int getTimedNodeCount();

    /**
     * Get the total execution time of all completed nodes.
     *
     * @return total duration in milliseconds
     */
    long getTotalExecutionTime();

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
    void logExecutionEvent(String level, String nodeId, String nodeName, String message, Object data);

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
    void logHttpRequest(String nodeId, String method, String url,
            Map<String, String> requestHeaders, String requestBody,
            int responseStatus, Map<String, String> responseHeaders, String responseBody,
            long durationMs);

    /**
     * Get HTTP request logs.
     *
     * @return list of HTTP request log records
     */
    List<?> getHttpRequestLogs();

    /**
     * Format HTTP request logs as a readable string.
     *
     * @return formatted HTTP logs string
     */
    String formatHttpLogs();

    /**
     * Clear HTTP request logs.
     */
    void clearHttpRequestLogs();

    /**
     * Check if verbose HTTP logging is enabled.
     *
     * @return true if verbose HTTP logging should be used
     */
    boolean isVerboseHttpLoggingEnabled();

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
    Path exportDebugBundle(WorkflowDTO workflow, Path outputPath, String executionId) throws IOException;

    // ========== Node JSON Export ==========

    /**
     * Export a node's configuration as JSON string.
     *
     * @param node the node to export
     * @return JSON string representation
     */
    String exportNodeAsJson(Object node);

    // ========== Expression Context Inspection ==========

    /**
     * Build expression context for inspection.
     *
     * @param variables    current workflow variables
     * @param nodeOutputs  outputs from previous nodes
     * @param workflowData workflow-level data
     * @return expression context record
     */
    Object buildExpressionContext(
            Map<String, Object> variables,
            Map<String, Object> nodeOutputs,
            Map<String, Object> workflowData);

    /**
     * Format expression context as readable string.
     *
     * @param context the expression context
     * @return formatted JSON string
     */
    String formatExpressionContext(Object context);

    // ========== Step-by-Step Execution ==========

    /**
     * Check if step-by-step execution mode is enabled.
     *
     * @return true if step mode is active
     */
    boolean isStepExecutionEnabled();

    /**
     * Enable or disable step-by-step execution mode.
     *
     * @param enabled true to enable step mode
     */
    void setStepExecutionEnabled(boolean enabled);

    /**
     * Wait for step continuation if in step mode.
     * Called by execution service after each node.
     *
     * @param nodeId   the node that just completed
     * @param nodeName the node name
     * @return true if execution should continue, false if cancelled
     * @throws InterruptedException if waiting is interrupted
     */
    boolean waitForStepContinue(String nodeId, String nodeName) throws InterruptedException;

    /**
     * Signal to continue to the next step.
     * Called by UI when user clicks Continue.
     */
    void continueStep();

    /**
     * Signal to cancel step execution.
     * Called by UI when user clicks Cancel/Stop.
     */
    void cancelStepExecution();

    /**
     * Reset step execution state (call at execution start).
     */
    void resetStepExecution();

    /**
     * Get the node ID that is currently paused (if any).
     *
     * @return node ID of paused node, or null if not paused
     */
    String getPausedNodeId();
}
