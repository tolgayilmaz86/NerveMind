package ai.nervemind.ui.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import ai.nervemind.ui.viewmodel.console.ExecutionConsoleViewModel;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Service that connects to the ExecutionConsoleViewModel for real-time logging.
 * Acts as a bridge between workflow execution and the console UI.
 * 
 * <p>
 * Uses the MVVM-based ExecutionConsole (FXML + Controller + ViewModel).
 */
@SuppressWarnings("java:S6548") // Singleton pattern is intentional for this service
public class ExecutionConsoleService {

    private static ExecutionConsoleService instance;
    private volatile ExecutionConsoleViewModel viewModel;
    private Stage consoleStage;
    private final ConcurrentHashMap<String, NodeExecutionState> nodeStates = new ConcurrentHashMap<>();

    // Execution history - tracks order of executed nodes for step back feature
    private final java.util.List<String> executionHistory = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AtomicInteger currentHistoryIndex = new AtomicInteger(-1);

    // Buffer for logs when ViewModel is not yet created
    private final ConcurrentLinkedQueue<Consumer<ExecutionConsoleViewModel>> logBuffer = new ConcurrentLinkedQueue<>();

    // Lock for viewModel/buffer synchronization to prevent race conditions
    private final Object viewModelLock = new Object();

    /**
     * Callback interface for node state changes.
     */
    public interface NodeStateListener {
        /**
         * Called when a node state changes.
         * 
         * @param nodeId the node ID
         * @param state  the new state
         */
        void onNodeStateChanged(String nodeId, NodeState state);
    }

    /**
     * Possible execution states for a node.
     */
    public enum NodeState {
        /** Node is idle. */
        IDLE,
        /** Node is running. */
        RUNNING,
        /** Node succeeded. */
        SUCCESS,
        /** Node failed. */
        FAILED,
        /** Node was skipped. */
        SKIPPED
    }

    /**
     * Holds the execution state and data for a node.
     * Includes input/output data for debug view purposes.
     *
     * @param nodeId     The unique identifier of the node
     * @param nodeName   The display name of the node
     * @param state      Current execution state (RUNNING, SUCCESS, etc.)
     * @param startTime  Epoch timestamp in ms when execution started
     * @param endTime    Epoch timestamp in ms when execution ended
     * @param error      Error message if state is FAILED
     * @param inputData  The raw input data received by the node
     * @param outputData The raw output data produced by the node
     */
    public record NodeExecutionState(
            String nodeId,
            String nodeName,
            NodeState state,
            long startTime,
            long endTime,
            String error,
            Object inputData,
            Object outputData) {

        /**
         * Convenience constructor when I/O data is not available.
         * 
         * @param nodeId    the node ID
         * @param nodeName  the node name
         * @param state     the node state
         * @param startTime start time
         * @param endTime   end time
         * @param error     error message
         */
        public NodeExecutionState(String nodeId, String nodeName, NodeState state,
                long startTime, long endTime, String error) {
            this(nodeId, nodeName, state, startTime, endTime, error, null, null);
        }

        /**
         * Returns a new state with RUNNING status.
         * 
         * @return new state instance
         */
        public NodeExecutionState running() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.RUNNING,
                    System.currentTimeMillis(), 0, null, inputData, outputData);
        }

        /**
         * Returns a new state with SUCCESS status.
         * 
         * @return new state instance
         */
        public NodeExecutionState success() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.SUCCESS,
                    startTime, System.currentTimeMillis(), null, inputData, outputData);
        }

        /**
         * Returns a new state with FAILED status.
         * 
         * @param error the error message
         * @return new state instance
         */
        public NodeExecutionState failed(String error) {
            return new NodeExecutionState(nodeId, nodeName, NodeState.FAILED,
                    startTime, System.currentTimeMillis(), error, inputData, outputData);
        }

        /**
         * Returns a new state with SKIPPED status.
         * 
         * @return new state instance
         */
        public NodeExecutionState skipped() {
            return new NodeExecutionState(nodeId, nodeName, NodeState.SKIPPED,
                    0, 0, null, null, null);
        }

        /**
         * Returns a new state with updated input data.
         * 
         * @param input the input data
         * @return new state instance
         */
        public NodeExecutionState withInput(Object input) {
            return new NodeExecutionState(nodeId, nodeName, state, startTime, endTime, error, input, outputData);
        }

        /**
         * Returns a new state with updated output data.
         * 
         * @param output the output data to associate
         * @return a new NodeExecutionState instance
         */
        public NodeExecutionState withOutput(Object output) {
            return new NodeExecutionState(nodeId, nodeName, state, startTime, endTime, error, inputData, output);
        }

        /**
         * Get duration in milliseconds.
         * 
         * @return duration in ms
         */
        public long durationMs() {
            if (startTime > 0 && endTime > 0) {
                return endTime - startTime;
            }
            return 0;
        }
    }

    private final java.util.List<NodeStateListener> nodeStateListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ExecutionConsoleService() {
    }

    /**
     * Gets the singleton instance of ExecutionConsoleService.
     * 
     * @return the service instance
     */
    public static synchronized ExecutionConsoleService getInstance() {
        if (instance == null) {
            instance = new ExecutionConsoleService();
        }
        return instance;
    }

    /**
     * Set the ViewModel instance.
     * 
     * @param viewModel The ExecutionConsoleViewModel
     */
    public void setViewModel(ExecutionConsoleViewModel viewModel) {
        synchronized (viewModelLock) {
            this.viewModel = viewModel;
            // Flush any buffered logs to the newly set ViewModel
            flushLogBuffer();
        }
    }

    /**
     * Get or create the console window.
     * Creates the FXML-based console if not already created.
     * 
     * @return The console Stage
     */
    public Stage getOrCreateConsole() {
        if (consoleStage == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/ai/nervemind/ui/view/console/ExecutionConsole.fxml"));
                javafx.scene.Parent root = loader.load();

                // Get the controller and its ViewModel
                ai.nervemind.ui.view.console.ExecutionConsoleController controller = loader.getController();

                synchronized (viewModelLock) {
                    this.viewModel = controller.getViewModel();
                    // Flush any buffered logs
                    flushLogBuffer();
                }

                consoleStage = new Stage();
                consoleStage.initStyle(StageStyle.DECORATED);
                consoleStage.setTitle("Execution Console");
                consoleStage.setMinWidth(700);
                consoleStage.setMinHeight(500);
                consoleStage.setWidth(1000);
                consoleStage.setHeight(700);

                Scene scene = new Scene(root);
                scene.getStylesheets().add(
                        getClass().getResource("/ai/nervemind/ui/styles/console.css").toExternalForm());
                consoleStage.setScene(scene);

                // Hide instead of close
                consoleStage.setOnCloseRequest(event -> {
                    event.consume();
                    consoleStage.hide();
                });
            } catch (Exception e) {
                throw new ai.nervemind.common.exception.UiInitializationException(
                        "ExecutionConsole", "Failed to load ExecutionConsole FXML", e);
            }
        }
        return consoleStage;
    }

    /**
     * Show the console window.
     */
    public void showConsole() {
        Platform.runLater(() -> {
            Stage stage = getOrCreateConsole();
            stage.show();
            stage.toFront();
        });
    }

    /**
     * Get the last execution state for a node.
     * Used by the debug view to display input/output data.
     *
     * @param nodeId The node ID
     * @return The execution state, or null if no execution data exists
     */
    public NodeExecutionState getNodeExecutionState(String nodeId) {
        return nodeStates.get(nodeId);
    }

    /**
     * Check if a node has execution data available.
     *
     * @param nodeId The node ID
     * @return true if execution data exists for this node
     */
    public boolean hasExecutionData(String nodeId) {
        NodeExecutionState state = nodeStates.get(nodeId);
        return state != null && state.state() != NodeState.IDLE;
    }

    /**
     * Flush buffered logs to the ViewModel.
     * Must be called while holding viewModelLock.
     */
    private void flushLogBuffer() {
        Consumer<ExecutionConsoleViewModel> logOperation;
        while ((logOperation = logBuffer.poll()) != null) {
            logOperation.accept(viewModel);
        }
    }

    /**
     * Execute an operation on the ViewModel, buffering if not available.
     * Thread-safe method to prevent race conditions between viewModel check and
     * buffer add.
     */
    private void executeOrBuffer(Consumer<ExecutionConsoleViewModel> operation) {
        synchronized (viewModelLock) {
            if (viewModel != null) {
                operation.accept(viewModel);
            } else {
                logBuffer.add(operation);
            }
        }
    }

    /**
     * Add a node state listener.
     * 
     * @param listener the listener to add
     */
    public void addNodeStateListener(NodeStateListener listener) {
        nodeStateListeners.add(listener);
    }

    /**
     * Remove a node state listener.
     * 
     * @param listener the listener to remove
     */
    public void removeNodeStateListener(NodeStateListener listener) {
        nodeStateListeners.remove(listener);
    }

    // ===== Logging Methods =====

    /**
     * Log execution start.
     * 
     * @param executionId  the unique execution ID
     * @param workflowName the name of the workflow being executed
     */
    public void executionStart(String executionId, String workflowName) {
        executeOrBuffer(vm -> vm.startExecution(executionId, workflowName));
        nodeStates.clear();
        // Clear execution history for new execution
        executionHistory.clear();
        currentHistoryIndex.set(-1);
    }

    /**
     * Log execution end.
     * 
     * @param executionId the execution ID
     * @param success     true if execution succeeded
     * @param durationMs  total duration in milliseconds
     */
    public void executionEnd(String executionId, boolean success, long durationMs) {
        executeOrBuffer(vm -> vm.endExecution(executionId, success, durationMs));

        // Reset all node states to idle after execution
        for (String nodeId : nodeStates.keySet()) {
            notifyNodeStateChanged(nodeId, NodeState.IDLE);
        }
    }

    /**
     * Log node start.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param nodeType    the node type
     */
    public void nodeStart(String executionId, String nodeId, String nodeName, String nodeType) {
        // Update node state
        NodeExecutionState state = new NodeExecutionState(nodeId, nodeName, NodeState.RUNNING,
                System.currentTimeMillis(), 0, null);
        nodeStates.put(nodeId, state);
        notifyNodeStateChanged(nodeId, NodeState.RUNNING);

        // Track in execution history for step back support
        if (!executionHistory.contains(nodeId)) {
            executionHistory.add(nodeId);
        }
        currentHistoryIndex.set(executionHistory.indexOf(nodeId));

        executeOrBuffer(vm -> vm.nodeStart(executionId, nodeId, nodeName, nodeType));
    }

    /**
     * Log node end.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param success     true if node execution succeeded
     * @param durationMs  node execution duration
     */
    public void nodeEnd(String executionId, String nodeId, String nodeName, boolean success, long durationMs) {
        // Update node state
        nodeStates.computeIfPresent(nodeId, (id, current) -> success ? current.success() : current.failed(null));
        notifyNodeStateChanged(nodeId, success ? NodeState.SUCCESS : NodeState.FAILED);

        executeOrBuffer(vm -> vm.nodeEnd(executionId, nodeId, nodeName, success, durationMs));
    }

    /**
     * Log node skip.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param reason      reason for skipping
     */
    public void nodeSkip(String executionId, String nodeId, String nodeName, String reason) {
        NodeExecutionState state = new NodeExecutionState(nodeId, nodeName, NodeState.SKIPPED, 0, 0, null);
        nodeStates.put(nodeId, state);
        notifyNodeStateChanged(nodeId, NodeState.SKIPPED);

        executeOrBuffer(vm -> vm.nodeSkip(executionId, nodeName, reason));
    }

    /**
     * Log node input data.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param input       the input data
     */
    public void nodeInput(String executionId, String nodeId, String nodeName, Object input) {
        // Store input data in node state for debug view
        nodeStates.computeIfPresent(nodeId, (id, current) -> current.withInput(input));

        executeOrBuffer(vm -> vm.nodeInput(executionId, nodeId, nodeName, input));
    }

    /**
     * Log node output data.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID
     * @param nodeName    the node name
     * @param output      the output data
     */
    public void nodeOutput(String executionId, String nodeId, String nodeName, Object output) {
        // Store output data in node state for debug view
        nodeStates.computeIfPresent(nodeId, (id, current) -> current.withOutput(output));

        executeOrBuffer(vm -> vm.nodeOutput(executionId, nodeId, nodeName, output));
    }

    /**
     * Log an error.
     * 
     * @param executionId the execution ID
     * @param nodeId      the node ID where the error occurred
     * @param nodeName    the node name
     * @param e           the exception
     */
    public void error(String executionId, String nodeId, String nodeName, Exception e) {
        String message = e.getMessage();
        String stackTrace = getStackTrace(e);

        // Update node state
        nodeStates.compute(nodeId, (id, current) -> {
            if (current != null) {
                return current.failed(message);
            } else {
                return new NodeExecutionState(nodeId, nodeName, NodeState.FAILED, 0, 0, message);
            }
        });
        notifyNodeStateChanged(nodeId, NodeState.FAILED);

        String finalNodeName = nodeName != null ? nodeName : nodeId;
        executeOrBuffer(vm -> vm.error(executionId, finalNodeName, message, stackTrace));
    }

    /**
     * Log error with custom message.
     * 
     * @param executionId the execution ID
     * @param source      error source name
     * @param message     error message
     * @param details     extra details/stack trace
     */
    public void error(String executionId, String source, String message, String details) {
        executeOrBuffer(vm -> vm.error(executionId, source, message, details));
    }

    /**
     * Log retry attempt.
     * 
     * @param executionId the execution ID
     * @param attempt     current attempt number
     * @param maxRetries  maximum allowed retries
     * @param delayMs     delay before next retry
     */
    public void retry(String executionId, int attempt, int maxRetries, long delayMs) {
        executeOrBuffer(vm -> vm.retry(executionId, attempt, maxRetries, delayMs));
    }

    /**
     * Log rate limiting.
     * 
     * @param executionId the execution ID
     * @param bucketId    rate limit bucket identifier
     * @param throttled   true if request was throttled
     * @param waitMs      estimated wait time
     */
    public void rateLimit(String executionId, String bucketId, boolean throttled, long waitMs) {
        executeOrBuffer(vm -> vm.rateLimit(executionId, bucketId, throttled, waitMs));
    }

    /**
     * Log data flow.
     * 
     * @param executionId the execution ID
     * @param fromNode    source node ID
     * @param toNode      target node ID
     * @param dataSize    amount of data transferred (e.g. bytes or record count)
     */
    public void dataFlow(String executionId, String fromNode, String toNode, int dataSize) {
        executeOrBuffer(vm -> vm.dataFlow(executionId, fromNode, toNode, dataSize));
    }

    /**
     * Log info message.
     * 
     * @param executionId the execution ID
     * @param message     info message
     * @param details     extra details
     */
    public void info(String executionId, String message, String details) {
        executeOrBuffer(vm -> vm.info(executionId, message, details));
    }

    /**
     * Log debug message.
     * 
     * @param executionId the execution ID
     * @param message     debug message
     * @param details     extra details
     */
    public void debug(String executionId, String message, String details) {
        executeOrBuffer(vm -> vm.debug(executionId, message, details));
    }

    /**
     * Get current state of a node.
     * 
     * @param nodeId the node ID
     * @return current state
     */
    public NodeState getNodeState(String nodeId) {
        NodeExecutionState state = nodeStates.get(nodeId);
        return state != null ? state.state() : NodeState.IDLE;
    }

    /**
     * Clear all states and history.
     */
    public void clearStates() {
        nodeStates.clear();
        executionHistory.clear();
        currentHistoryIndex.set(-1);
    }

    // ===== Execution History for Step Back =====

    /**
     * Get the previous node ID in execution history.
     * Used for step back functionality.
     *
     * @return the previous node ID, or null if at the beginning
     */
    public String getPreviousNodeId() {
        int index = currentHistoryIndex.get();
        if (index > 0) {
            return executionHistory.get(index - 1);
        }
        return null;
    }

    /**
     * Get the current node ID in execution history.
     *
     * @return the current node ID, or null if no history
     */
    public String getCurrentNodeId() {
        int index = currentHistoryIndex.get();
        if (index >= 0 && index < executionHistory.size()) {
            return executionHistory.get(index);
        }
        return null;
    }

    /**
     * Move to the previous node in execution history.
     * Returns the previous node ID and updates the current index.
     *
     * @return the previous node ID, or null if at the beginning
     */
    public String stepBackInHistory() {
        int index = currentHistoryIndex.get();
        if (index > 0) {
            currentHistoryIndex.decrementAndGet();
            return executionHistory.get(currentHistoryIndex.get());
        }
        return null;
    }

    /**
     * Move to the next node in execution history.
     * Returns the next node ID and updates the current index.
     *
     * @return the next node ID, or null if at the end
     */
    public String stepForwardInHistory() {
        int index = currentHistoryIndex.get();
        if (index < executionHistory.size() - 1) {
            currentHistoryIndex.incrementAndGet();
            return executionHistory.get(currentHistoryIndex.get());
        }
        return null;
    }

    /**
     * Check if step back is available (not at the beginning of history).
     *
     * @return true if step back is possible
     */
    public boolean canStepBack() {
        return currentHistoryIndex.get() > 0;
    }

    /**
     * Check if step forward is available (not at the end of history).
     *
     * @return true if step forward is possible
     */
    public boolean canStepForward() {
        return currentHistoryIndex.get() < executionHistory.size() - 1;
    }

    /**
     * Get the execution history list.
     *
     * @return unmodifiable list of node IDs in execution order
     */
    public java.util.List<String> getExecutionHistory() {
        return java.util.Collections.unmodifiableList(executionHistory);
    }

    /**
     * Get the current position in execution history.
     *
     * @return the current history index (0-based), or -1 if no history
     */
    public int getCurrentHistoryIndex() {
        return currentHistoryIndex.get();
    }

    /**
     * Set the current position in execution history.
     * Used when navigating to a specific point in history.
     *
     * @param nodeId the node ID to set as current
     */
    public void setCurrentHistoryPosition(String nodeId) {
        int index = executionHistory.indexOf(nodeId);
        if (index >= 0) {
            currentHistoryIndex.set(index);
        }
    }

    /**
     * Shutdown the console service and close any open windows.
     * Should be called when the application is exiting.
     */
    public void shutdown() {
        Platform.runLater(() -> {
            if (consoleStage != null) {
                consoleStage.close();
                consoleStage = null;
            }
        });
        synchronized (viewModelLock) {
            viewModel = null;
        }
        nodeStates.clear();
        executionHistory.clear();
        currentHistoryIndex.set(-1);
        nodeStateListeners.clear();
    }

    private void notifyNodeStateChanged(String nodeId, NodeState state) {
        for (NodeStateListener listener : nodeStateListeners) {
            try {
                listener.onNodeStateChanged(nodeId, state);
            } catch (Exception _) {
                // Don't let listener errors affect execution
            }
        }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
