/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.console;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Execution Console.
 * 
 * <p>
 * Manages execution sessions, log entries, filtering, and display settings.
 * This class contains no JavaFX UI imports (only javafx.beans.*) to ensure
 * testability without the JavaFX runtime.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>Session management - track multiple workflow executions</li>
 * <li>Log entry filtering by level (DEBUG, TRACE, etc.)</li>
 * <li>Text-based filtering of log messages</li>
 * <li>Display settings (timestamps, line numbers, auto-scroll)</li>
 * <li>Log statistics (info, warn, error counts)</li>
 * <li>Export functionality (text and JSON formats)</li>
 * </ul>
 */
public class ExecutionConsoleViewModel extends BaseViewModel {

    // =========================
    // Constants
    // =========================

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String JSON_COMMA_NEWLINE = ",\n";

    // =========================
    // Session Management
    // =========================

    private final Map<String, ExecutionSessionModel> sessions = new ConcurrentHashMap<>();
    private final ObservableList<String> sessionIds = FXCollections.observableArrayList();
    private final ObjectProperty<String> selectedSessionId = new SimpleObjectProperty<>();
    private final IntegerProperty sessionCount = new SimpleIntegerProperty(0);

    // =========================
    // Filter Properties
    // =========================

    private final BooleanProperty showDebug = new SimpleBooleanProperty(true);
    private final BooleanProperty showTrace = new SimpleBooleanProperty(true);
    private final StringProperty filterText = new SimpleStringProperty("");
    private final StringProperty filterStatus = new SimpleStringProperty("");

    // =========================
    // Display Settings
    // =========================

    private final BooleanProperty autoScroll = new SimpleBooleanProperty(true);
    private final BooleanProperty showTimestamps = new SimpleBooleanProperty(true);
    private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(false);

    // =========================
    // Log Statistics
    // =========================

    private final IntegerProperty infoCount = new SimpleIntegerProperty(0);
    private final IntegerProperty warnCount = new SimpleIntegerProperty(0);
    private final IntegerProperty errorCount = new SimpleIntegerProperty(0);
    private final IntegerProperty totalEntryCount = new SimpleIntegerProperty(0);
    private final IntegerProperty visibleEntryCount = new SimpleIntegerProperty(0);

    // =========================
    // Filtered Log Entries
    // =========================

    private final ObservableList<LogEntryModel> visibleEntries = FXCollections.observableArrayList();

    // =========================
    // Listeners
    // =========================

    private final List<ConsoleEventListener> eventListeners = new CopyOnWriteArrayList<>();

    // =========================
    // Constructor
    // =========================

    /**
     * Constructs a new ExecutionConsoleViewModel and initializes listeners.
     */
    public ExecutionConsoleViewModel() {
        // Set up filter change listeners
        showDebug.addListener((obs, oldVal, newVal) -> refreshVisibleEntries());
        showTrace.addListener((obs, oldVal, newVal) -> refreshVisibleEntries());
        filterText.addListener((obs, oldVal, newVal) -> refreshVisibleEntries());
        selectedSessionId.addListener((obs, oldVal, newVal) -> refreshVisibleEntries());
    }

    // =========================
    // Session Management API
    // =========================

    /**
     * Start a new execution session.
     *
     * @param executionId  Unique identifier for the execution
     * @param workflowName Name of the workflow being executed
     */
    public void startExecution(String executionId, String workflowName) {
        ExecutionSessionModel session = new ExecutionSessionModel(executionId, workflowName);
        sessions.put(executionId, session);
        sessionIds.add(executionId);
        selectedSessionId.set(executionId);
        sessionCount.set(sessions.size());

        notifyListeners(listener -> listener.onSessionStarted(executionId, workflowName));
        refreshVisibleEntries();
    }

    /**
     * End an execution session.
     *
     * @param executionId Execution identifier
     * @param success     Whether the execution was successful
     * @param durationMs  Total duration in milliseconds
     */
    public void endExecution(String executionId, boolean success, long durationMs) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            session.setComplete(true);
            session.setSuccess(success);
            session.setDurationMs(durationMs);

            LogEntryType type = success ? LogEntryType.SUCCESS : LogEntryType.ERROR;
            String message = success ? "✓ Execution completed" : "✗ Execution failed";
            String details = String.format("Duration: %s", formatDuration(durationMs));

            addLogEntry(executionId, 0, type, message, details);
            notifyListeners(listener -> listener.onSessionEnded(executionId, success));
        }
    }

    /**
     * Record the start of a node execution.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param nodeType    the node type
     */
    public void nodeStart(String executionId, String nodeId, String nodeName, String nodeType) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            int depth = session.pushNode(nodeId);
            String message = "▶ " + nodeName + " (" + nodeType + ")";
            String details = "Node ID: " + nodeId;
            addLogEntry(executionId, depth, LogEntryType.NODE_START, message, details);
        }
    }

    /**
     * Record the end of a node execution.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param success     true if succeeded
     * @param durationMs  duration in milliseconds
     */
    public void nodeEnd(String executionId, String nodeId, String nodeName, boolean success, long durationMs) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            int depth = session.getNodeDepth(nodeId);
            LogEntryType type = success ? LogEntryType.NODE_END : LogEntryType.ERROR;
            String message = (success ? "✓ " : "✗ ") + nodeName;
            String details = String.format("Duration: %s", formatDuration(durationMs));
            addLogEntry(executionId, depth, type, message, details);
            session.popNode(nodeId);
        }
    }

    /**
     * Record node input data.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param input       the input data
     */
    public void nodeInput(String executionId, String nodeId, String nodeName, Object input) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            int depth = session.getNodeDepth(nodeId) + 1;
            addLogEntry(executionId, depth, LogEntryType.TRACE, "📥 Input: " + nodeName, formatDataPreview(input));
        }
    }

    /**
     * Record node output data.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param output      the output data
     */
    public void nodeOutput(String executionId, String nodeId, String nodeName, Object output) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            int depth = session.getNodeDepth(nodeId) + 1;
            addLogEntry(executionId, depth, LogEntryType.TRACE, "📤 Output: " + nodeName, formatDataPreview(output));
        }
    }

    /**
     * Record a skipped node.
     * 
     * @param executionId the execution ID
     * @param nodeName    the node name
     * @param reason      the skip reason
     */
    public void nodeSkip(String executionId, String nodeName, String reason) {
        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            addLogEntry(executionId, session.getCurrentDepth(), LogEntryType.DEBUG, "⏭ Skipped: " + nodeName, reason);
        }
    }

    /**
     * Record an error.
     * 
     * @param executionId the execution ID
     * @param source      the error source
     * @param message     the error message
     * @param stackTrace  the stack trace
     */
    public void error(String executionId, String source, String message, String stackTrace) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        addLogEntry(executionId, depth, LogEntryType.ERROR, "❌ Error in " + source, message);
        if (stackTrace != null && !stackTrace.isEmpty()) {
            addLogEntry(executionId, depth + 1, LogEntryType.TRACE, "Stack trace:", stackTrace);
        }
    }

    /**
     * Record a retry attempt.
     * 
     * @param executionId the execution ID
     * @param attempt     current retry attempt
     * @param maxRetries  maximum allowed retries
     * @param delayMs     delay before next retry
     */
    public void retry(String executionId, int attempt, int maxRetries, long delayMs) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        String message = String.format("🔄 Retry %d/%d", attempt, maxRetries);
        String details = String.format("Waiting %dms", delayMs);
        addLogEntry(executionId, depth, LogEntryType.WARN, message, details);
    }

    /**
     * Record rate limiting event.
     * 
     * @param executionId the execution ID
     * @param bucketId    the rate limit bucket identifier
     * @param throttled   true if execution was throttled
     * @param waitMs      wait duration in milliseconds
     */
    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        if (throttled) {
            addLogEntry(executionId, depth, LogEntryType.WARN, "⏱ Rate limited: " + bucketId,
                    "Waited " + waitMs + "ms");
        } else {
            addLogEntry(executionId, depth, LogEntryType.DEBUG, "⏱ Rate limit passed: " + bucketId, null);
        }
    }

    /**
     * Record data flow between nodes.
     * 
     * @param executionId the execution ID
     * @param fromNode    the source node name
     * @param toNode      the target node name
     * @param dataSize    size of data in bytes
     */
    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        String message = String.format("📦 Data: %s → %s", fromNode, toNode);
        addLogEntry(executionId, depth, LogEntryType.TRACE, message, dataSize + " bytes");
    }

    /**
     * Record an info message.
     * 
     * @param executionId the execution ID
     * @param message     the info message
     * @param details     additional details
     */
    public void info(String executionId, String message, String details) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        addLogEntry(executionId, depth, LogEntryType.INFO, message, details);
    }

    /**
     * Record a debug message.
     * 
     * @param executionId the execution ID
     * @param message     the debug message
     * @param details     additional details
     */
    public void debug(String executionId, String message, String details) {
        ExecutionSessionModel session = sessions.get(executionId);
        int depth = session != null ? session.getCurrentDepth() : 0;
        addLogEntry(executionId, depth, LogEntryType.DEBUG, message, details);
    }

    // =========================
    // Log Entry Management
    // =========================

    private void addLogEntry(String executionId, int depth, LogEntryType type, String message, String details) {
        LogEntryModel entry = new LogEntryModel(Instant.now(), depth, type, message, details, executionId);

        ExecutionSessionModel session = sessions.get(executionId);
        if (session != null) {
            session.addEntry(entry);
        }

        updateStatistics(type);

        // Add to visible entries if it passes filters
        String selected = selectedSessionId.get();
        if ((selected == null || selected.equals(executionId)) && shouldShowEntry(entry)) {
            visibleEntries.add(entry);
            visibleEntryCount.set(visibleEntries.size());
            notifyListeners(listener -> listener.onEntryAdded(entry));
        }
    }

    private void updateStatistics(LogEntryType type) {
        switch (type) {
            case INFO, NODE_START, NODE_END, SUCCESS -> infoCount.set(infoCount.get() + 1);
            case WARN -> warnCount.set(warnCount.get() + 1);
            case ERROR -> errorCount.set(errorCount.get() + 1);
            default -> {
                /* DEBUG, TRACE, etc. don't count toward main stats */ }
        }
        totalEntryCount.set(totalEntryCount.get() + 1);
    }

    /**
     * Check if a log entry should be shown based on current filters.
     * 
     * @param entry the log entry to check
     * @return true if the entry should be visible
     */
    public boolean shouldShowEntry(LogEntryModel entry) {
        if (!passesLevelFilter(entry)) {
            return false;
        }
        return passesTextFilter(entry);
    }

    private boolean passesLevelFilter(LogEntryModel entry) {
        // DEBUG and TRACE entries are only shown if their respective toggles are
        // enabled
        if (entry.type() == LogEntryType.DEBUG) {
            return showDebug.get();
        }
        if (entry.type() == LogEntryType.TRACE) {
            return showTrace.get();
        }
        // All other entries (INFO, WARN, ERROR, NODE_START, NODE_END, SUCCESS) are
        // always shown
        return true;
    }

    private boolean passesTextFilter(LogEntryModel entry) {
        String filter = filterText.get();
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        String lowerFilter = filter.toLowerCase();
        String details = entry.details() != null ? entry.details() : "";
        String searchable = (entry.message() + " " + details).toLowerCase();
        return searchable.contains(lowerFilter);
    }

    /**
     * Refresh the visible entries based on current filters.
     */
    public void refreshVisibleEntries() {
        visibleEntries.clear();

        String selected = selectedSessionId.get();
        List<LogEntryModel> entriesToFilter = getEntriesToFilter(selected);
        addFilteredEntries(entriesToFilter);

        visibleEntryCount.set(visibleEntries.size());
        updateFilterStatus();
        recalculateStatistics();
    }

    private List<LogEntryModel> getEntriesToFilter(String selectedId) {
        if (selectedId == null) {
            return sessions.values().stream()
                    .flatMap(session -> session.getEntries().stream())
                    .toList();
        }
        ExecutionSessionModel session = sessions.get(selectedId);
        return session != null ? session.getEntries() : List.of();
    }

    private void addFilteredEntries(List<LogEntryModel> entries) {
        for (LogEntryModel entry : entries) {
            if (shouldShowEntry(entry)) {
                visibleEntries.add(entry);
            }
        }
    }

    private void recalculateStatistics() {
        int info = 0;
        int warn = 0;
        int error = 0;

        String selected = selectedSessionId.get();
        List<LogEntryModel> entries = selected != null && sessions.containsKey(selected)
                ? sessions.get(selected).getEntries()
                : sessions.values().stream().flatMap(s -> s.getEntries().stream()).toList();

        for (LogEntryModel entry : entries) {
            switch (entry.type()) {
                case INFO, NODE_START, NODE_END, SUCCESS -> info++;
                case WARN -> warn++;
                case ERROR -> error++;
                default -> {
                    /* ignore */ }
            }
        }

        infoCount.set(info);
        warnCount.set(warn);
        errorCount.set(error);
        totalEntryCount.set(entries.size());
    }

    private void updateFilterStatus() {
        StringBuilder status = new StringBuilder();
        if (showDebug.get()) {
            status.append("DEBUG ");
        }
        if (showTrace.get()) {
            status.append("TRACE ");
        }
        String filter = filterText.get();
        if (filter != null && !filter.isEmpty()) {
            status.append("\"").append(filter).append("\" ");
        }
        if (!status.isEmpty()) {
            status.append("(").append(visibleEntries.size()).append(" shown)");
        }
        filterStatus.set(status.toString().trim());
    }

    // =========================
    // Clear & Copy Operations
    // =========================

    /**
     * Clear all execution logs and sessions.
     */
    public void clear() {
        sessions.clear();
        sessionIds.clear();
        selectedSessionId.set(null);
        sessionCount.set(0);
        visibleEntries.clear();
        infoCount.set(0);
        warnCount.set(0);
        errorCount.set(0);
        totalEntryCount.set(0);
        visibleEntryCount.set(0);
        filterStatus.set("");
        notifyListeners(ConsoleEventListener::onCleared);
    }

    /**
     * Get all logs as text for clipboard copy.
     * 
     * @return all logs formatted as text
     */
    public String getLogsAsText() {
        StringBuilder sb = new StringBuilder();
        for (ExecutionSessionModel session : sessions.values()) {
            sb.append("=== Execution: ").append(session.getWorkflowName()).append(" ===\n");
            for (LogEntryModel entry : session.getEntries()) {
                sb.append(formatEntryAsText(entry)).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // =========================
    // Export Operations
    // =========================

    /**
     * Export logs as plain text.
     * 
     * @return exported logs as plain text
     */
    public String exportAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Execution Console Export ===\n");
        sb.append("Exported at: ").append(LocalDateTime.now().format(DATE_TIME_FORMAT)).append("\n\n");

        for (ExecutionSessionModel session : sessions.values()) {
            sb.append("─".repeat(60)).append("\n");
            sb.append("Workflow: ").append(session.getWorkflowName()).append("\n");
            sb.append("Execution ID: ").append(session.getExecutionId()).append("\n");
            sb.append("Status: ").append(session.getStatusText()).append("\n");
            sb.append("─".repeat(60)).append("\n\n");

            int lineNum = 1;
            for (LogEntryModel entry : session.getEntries()) {
                sb.append(String.format("%4d | ", lineNum++));
                sb.append(formatTimestamp(entry.timestamp())).append(" ");
                sb.append("  ".repeat(entry.depth()));
                sb.append("[").append(entry.type().name()).append("] ");
                sb.append(entry.message());
                if (entry.details() != null && !entry.details().isEmpty()) {
                    sb.append("\n      | ").append(entry.details().replace("\n", "\n      | "));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Export logs as JSON.
     * 
     * @return exported logs as JSON string
     */
    public String exportAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"exportTime\": \"").append(LocalDateTime.now()).append(JSON_COMMA_NEWLINE);
        sb.append("  \"sessions\": [\n");

        boolean firstSession = true;
        for (ExecutionSessionModel session : sessions.values()) {
            if (!firstSession) {
                sb.append(JSON_COMMA_NEWLINE);
            }
            firstSession = false;

            sb.append("    {\n");
            sb.append("      \"workflowName\": \"").append(escapeJson(session.getWorkflowName()))
                    .append(JSON_COMMA_NEWLINE);
            sb.append("      \"executionId\": \"").append(session.getExecutionId()).append(JSON_COMMA_NEWLINE);
            sb.append("      \"status\": \"").append(session.getStatusText().toLowerCase()).append(JSON_COMMA_NEWLINE);
            sb.append("      \"entries\": [\n");

            boolean firstEntry = true;
            for (LogEntryModel entry : session.getEntries()) {
                if (!firstEntry) {
                    sb.append(JSON_COMMA_NEWLINE);
                }
                firstEntry = false;

                sb.append("        {");
                sb.append("\"timestamp\": \"").append(entry.timestamp()).append("\", ");
                sb.append("\"level\": \"").append(entry.type().name()).append("\", ");
                sb.append("\"message\": \"").append(escapeJson(entry.message())).append("\"");
                if (entry.details() != null) {
                    sb.append(", \"details\": \"").append(escapeJson(entry.details())).append("\"");
                }
                sb.append("}");
            }
            sb.append("\n      ]\n    }");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    // =========================
    // Summary Data
    // =========================

    /**
     * Get the selected session for summary display.
     * 
     * @return the selected session or null
     */
    public ExecutionSessionModel getSelectedSession() {
        String id = selectedSessionId.get();
        return id != null ? sessions.get(id) : null;
    }

    /**
     * Get all sessions.
     * 
     * @return list of all execution sessions
     */
    public List<ExecutionSessionModel> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    // =========================
    // Property Getters
    // =========================

    /**
     * Gets the observable list of all session IDs.
     * 
     * @return the session IDs property
     */
    public ObservableList<String> sessionIdsProperty() {
        return sessionIds;
    }

    /**
     * Gets the property representing the currently selected session ID.
     * 
     * @return the selected session ID property
     */
    public ObjectProperty<String> selectedSessionIdProperty() {
        return selectedSessionId;
    }

    /**
     * Gets the property representing the total number of sessions.
     * 
     * @return the session count property
     */
    public IntegerProperty sessionCountProperty() {
        return sessionCount;
    }

    /**
     * Gets the property controlling whether DEBUG level logs are shown.
     * 
     * @return the show debug property
     */
    public BooleanProperty showDebugProperty() {
        return showDebug;
    }

    /**
     * Gets the property controlling whether TRACE level logs are shown.
     * 
     * @return the show trace property
     */
    public BooleanProperty showTraceProperty() {
        return showTrace;
    }

    /**
     * Gets the property for the current text-based log filter.
     * 
     * @return the filter text property
     */
    public StringProperty filterTextProperty() {
        return filterText;
    }

    /**
     * Gets the property for filtering by execution status.
     * 
     * @return the filter status property
     */
    public StringProperty filterStatusProperty() {
        return filterStatus;
    }

    /**
     * Gets the property controlling automatic scrolling to latest logs.
     * 
     * @return the auto scroll property
     */
    public BooleanProperty autoScrollProperty() {
        return autoScroll;
    }

    /**
     * Gets the property controlling timestamp visibility in logs.
     * 
     * @return the show timestamps property
     */
    public BooleanProperty showTimestampsProperty() {
        return showTimestamps;
    }

    /**
     * Gets the property controlling line number visibility.
     * 
     * @return the show line numbers property
     */
    public BooleanProperty showLineNumbersProperty() {
        return showLineNumbers;
    }

    /**
     * Gets the property for the total number of INFO messages.
     * 
     * @return the info count property
     */
    public IntegerProperty infoCountProperty() {
        return infoCount;
    }

    /**
     * Gets the property for the total number of WARN messages.
     * 
     * @return the warn count property
     */
    public IntegerProperty warnCountProperty() {
        return warnCount;
    }

    /**
     * Gets the property for the total number of ERROR messages.
     * 
     * @return the error count property
     */
    public IntegerProperty errorCountProperty() {
        return errorCount;
    }

    /**
     * Gets the property for the total number of log entries in the current session.
     * 
     * @return the total entry count property
     */
    public IntegerProperty totalEntryCountProperty() {
        return totalEntryCount;
    }

    /**
     * Gets the property for the number of log entries that pass the current filter.
     * 
     * @return the visible entry count property
     */
    public IntegerProperty visibleEntryCountProperty() {
        return visibleEntryCount;
    }

    /**
     * Gets the observable list of log entries currently visible after filtering.
     * 
     * @return the visible entries property
     */
    public ObservableList<LogEntryModel> visibleEntriesProperty() {
        return visibleEntries;
    }

    // =========================
    // Event Listeners
    // =========================

    /**
     * Adds an event listener for console events.
     * 
     * @param listener the listener to add
     */
    public void addEventListener(ConsoleEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * Removes an event listener.
     * 
     * @param listener the listener to remove
     */
    public void removeEventListener(ConsoleEventListener listener) {
        eventListeners.remove(listener);
    }

    private void notifyListeners(java.util.function.Consumer<ConsoleEventListener> action) {
        for (ConsoleEventListener listener : eventListeners) {
            action.accept(listener);
        }
    }

    // =========================
    // Helper Methods
    // =========================

    /**
     * Format a duration in milliseconds to human-readable string.
     * 
     * @param ms duration in milliseconds
     * @return human-readable duration string
     */
    public String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        if (ms < 60000) {
            return String.format("%.2fs", ms / 1000.0);
        }
        long mins = ms / 60000;
        long secs = (ms % 60000) / 1000;
        return String.format("%dm %ds", mins, secs);
    }

    /**
     * Format a timestamp for display.
     * 
     * @param timestamp the timestamp to format
     * @return formatted time string
     */
    public String formatTimestamp(Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private String formatDataPreview(Object data) {
        if (data == null) {
            return "null";
        }
        String str = data.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }

    private String formatEntryAsText(LogEntryModel entry) {
        return formatTimestamp(entry.timestamp()) + " " + "  ".repeat(entry.depth())
                + "[" + entry.type().name() + "] " + entry.message()
                + (entry.details() != null ? " — " + entry.details() : "");
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =========================
    // Inner Types
    // =========================

    /**
     * Log entry types.
     */
    public enum LogEntryType {
        /** General information. */
        INFO,
        /** Debugging information. */
        DEBUG,
        /** Warning message. */
        WARN,
        /** Error message. */
        ERROR,
        /** Trace-level detailed log. */
        TRACE,
        /** Node execution started. */
        NODE_START,
        /** Node execution ended. */
        NODE_END,
        /** Node input data log. */
        NODE_INPUT,
        /** Node output data log. */
        NODE_OUTPUT,
        /** Expression evaluation result. */
        EXPRESSION,
        /** Execution successfully completed. */
        SUCCESS
    }

    /**
     * Immutable log entry data model.
     *
     * @param timestamp   The exact time the log entry was created
     * @param depth       The indentation level for hierarchical logs
     * @param type        The classification of the log entry (INFO, ERROR, etc.)
     * @param message     The primary log message
     * @param details     Extended multi-line details or stack traces
     * @param executionId Unique ID of the execution session this belongs to
     */
    public record LogEntryModel(
            Instant timestamp,
            int depth,
            LogEntryType type,
            String message,
            String details,
            String executionId) {
    }

    /**
     * Execution session model.
     */
    public static class ExecutionSessionModel {
        private final String executionId;
        private final String workflowName;
        private final Instant startTime;
        private final List<String> nodeStack = new ArrayList<>();
        private final Map<String, Integer> nodeDepths = new HashMap<>();
        private final List<LogEntryModel> entries = new ArrayList<>();
        private boolean complete;
        private boolean success;
        private long durationMs;
        private int totalNodes = 0;
        private int completedNodes = 0;

        /**
         * Constructs a new ExecutionSessionModel.
         * 
         * @param executionId  the execution ID
         * @param workflowName the workflow name
         */
        public ExecutionSessionModel(String executionId, String workflowName) {
            this.executionId = executionId;
            this.workflowName = workflowName;
            this.startTime = Instant.now();
        }

        /**
         * Push a node onto the stack.
         * 
         * @param nodeId the node ID
         * @return the depth of the node
         */
        public int pushNode(String nodeId) {
            int depth = nodeStack.size();
            nodeStack.add(nodeId);
            nodeDepths.put(nodeId, depth);
            totalNodes++;
            return depth;
        }

        /**
         * Pop a node from the stack.
         * 
         * @param nodeId the node ID
         */
        public void popNode(String nodeId) {
            nodeStack.remove(nodeId);
            nodeDepths.remove(nodeId);
            completedNodes++;
        }

        /**
         * Gets the depth of a specific node.
         * 
         * @param nodeId the node ID
         * @return the depth of the node
         */
        public int getNodeDepth(String nodeId) {
            return nodeDepths.getOrDefault(nodeId, 0);
        }

        /**
         * Gets the current depth of the node stack.
         * 
         * @return the current depth of the stack
         */
        public int getCurrentDepth() {
            return nodeStack.size();
        }

        /**
         * Adds a log entry to this session.
         * 
         * @param entry the log entry to add
         */
        public void addEntry(LogEntryModel entry) {
            entries.add(entry);
        }

        /**
         * Gets all log entries in this session.
         * 
         * @return a copy of the log entries
         */
        public List<LogEntryModel> getEntries() {
            return new ArrayList<>(entries);
        }

        /**
         * Gets the execution ID.
         * 
         * @return the execution ID
         */
        public String getExecutionId() {
            return executionId;
        }

        /**
         * Gets the workflow name.
         * 
         * @return the workflow name
         */
        public String getWorkflowName() {
            return workflowName;
        }

        /**
         * Gets the session start time.
         * 
         * @return the start time
         */
        public Instant getStartTime() {
            return startTime;
        }

        /**
         * Checks if the session is complete.
         * 
         * @return true if the session is complete
         */
        public boolean isComplete() {
            return complete;
        }

        /**
         * Sets the completion status.
         * 
         * @param complete the complete status to set
         */
        public void setComplete(boolean complete) {
            this.complete = complete;
        }

        /**
         * Checks if the session was successful.
         * 
         * @return true if the session was successful
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Sets the success status.
         * 
         * @param success the success status to set
         */
        public void setSuccess(boolean success) {
            this.success = success;
        }

        /**
         * Gets the total duration in milliseconds.
         * 
         * @return the duration in milliseconds
         */
        public long getDurationMs() {
            return durationMs;
        }

        /**
         * Sets the total duration.
         * 
         * @param durationMs the duration in milliseconds to set
         */
        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        /**
         * Gets the total number of nodes executed.
         * 
         * @return the total number of nodes
         */
        public int getTotalNodes() {
            return totalNodes;
        }

        /**
         * Gets the number of completed nodes.
         * 
         * @return the number of completed nodes
         */
        public int getCompletedNodes() {
            return completedNodes;
        }

        /**
         * Get current running duration if not complete.
         * 
         * @return current duration in milliseconds
         */
        public long getRunningDurationMs() {
            if (complete) {
                return durationMs;
            }
            return Duration.between(startTime, Instant.now()).toMillis();
        }

        /**
         * Get status as text.
         * 
         * @return the status text
         */
        public String getStatusText() {
            if (!complete) {
                return "running";
            } else if (success) {
                return "completed";
            } else {
                return "failed";
            }
        }

        /**
         * Calculate progress percentage.
         * 
         * @return the progress as a double between 0 and 1
         */
        public double getProgress() {
            if (totalNodes == 0) {
                return 0;
            }
            return (double) completedNodes / totalNodes;
        }
    }

    /**
     * Event listener interface for console events.
     */
    public interface ConsoleEventListener {
        /**
         * Called when a new execution session starts.
         * 
         * @param executionId  the execution ID
         * @param workflowName the workflow name
         */
        default void onSessionStarted(String executionId, String workflowName) {
        }

        /**
         * Called when an execution session ends.
         * 
         * @param executionId the execution ID
         * @param success     true if successful
         */
        default void onSessionEnded(String executionId, boolean success) {
        }

        /**
         * Called when a new log entry is added.
         * 
         * @param entry the log entry added
         */
        default void onEntryAdded(LogEntryModel entry) {
        }

        /**
         * Called when logs are cleared.
         */
        default void onCleared() {
        }
    }
}
