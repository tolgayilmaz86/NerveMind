package ai.nervemind.app.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.database.model.ExecutionEntity;
import ai.nervemind.app.database.repository.ExecutionRepository;
import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.exception.DataParsingException;
import ai.nervemind.common.exception.NodeExecutionException;
import ai.nervemind.common.service.DevModeServiceInterface;
import ai.nervemind.common.service.ExecutionLogHandler;
import ai.nervemind.common.service.ExecutionServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Core workflow execution engine service.
 *
 * <p>
 * Responsible for the lifecycle and orchestration of workflow executions. This
 * service
 * acts as the central nervous system of NerveMind, managing:
 * </p>
 *
 * <ul>
 * <li><strong>Orchestration:</strong> Traversing the workflow graph (directed
 * acyclic graph)
 * and determining the execution order of nodes.</li>
 * <li><strong>State Management:</strong> Persisting execution status, logs, and
 * intermediate data in the {@link ExecutionRepository}.</li>
 * <li><strong>Concurrency:</strong> leveraging
 * {@link java.util.concurrent.ExecutorService} with
 * virtual threads (Project Loom) to handle high-concurrency node execution
 * efficiently.</li>
 * <li><strong>Error Handling:</strong> Managing global timeout policies, node
 * retries,
 * and failure propagation.</li>
 * </ul>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 * <li>Resolution of Trigger Nodes (start points).</li>
 * <li>Breadth-First Search (BFS) or topological traversal of the graph.</li>
 * <li>Delegation of specific tasks to the appropriate
 * {@link NodeExecutor}.</li>
 * <li>Parameter interpolation and variable resolution.</li>
 * </ol>
 *
 * @see NodeExecutorRegistry
 * @see ai.nervemind.common.domain.Workflow
 */
@Service
@Transactional
public class ExecutionService implements ExecutionServiceInterface {

    private static final String EXECUTION_CANCELLED_MESSAGE = "Execution cancelled by user";

    private final ExecutionRepository executionRepository;
    private final WorkflowService workflowService;
    private final CredentialService credentialService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final ExecutionLogger executionLogger;
    private final SettingsServiceInterface settingsService;
    private final DevModeServiceInterface devModeService;

    // Track running executions for cancellation support
    private final ConcurrentHashMap<Long, AtomicBoolean> runningExecutions = new ConcurrentHashMap<>();

    // Execution configuration (read from settings)
    private final int defaultTimeout;
    private final int maxParallelNodes;
    private final int retryAttempts;
    private final long retryDelay;

    /**
     * Creates a new execution service.
     * 
     * @param executionRepository  the execution repository
     * @param workflowService      the workflow service
     * @param credentialService    the credential service
     * @param nodeExecutorRegistry the node executor registry
     * @param objectMapper         the object mapper
     * @param executionLogger      the execution logger
     * @param settingsService      the settings service
     * @param devModeService       the dev mode service
     * @param logHandlers          the log handlers
     */
    public ExecutionService(
            ExecutionRepository executionRepository,
            WorkflowService workflowService,
            CredentialService credentialService,
            NodeExecutorRegistry nodeExecutorRegistry,
            ObjectMapper objectMapper,
            ExecutionLogger executionLogger,
            SettingsServiceInterface settingsService,
            DevModeServiceInterface devModeService,
            java.util.List<ExecutionLogHandler> logHandlers) {
        this.executionRepository = executionRepository;
        this.workflowService = workflowService;
        this.credentialService = credentialService;
        this.nodeExecutorRegistry = nodeExecutorRegistry;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.executionLogger = executionLogger;
        this.settingsService = settingsService;
        this.devModeService = devModeService;

        // Read execution configuration from settings
        this.defaultTimeout = settingsService.getInt(SettingsDefaults.EXECUTION_DEFAULT_TIMEOUT, 30000);
        this.maxParallelNodes = settingsService.getInt(SettingsDefaults.EXECUTION_MAX_PARALLEL, 10);
        this.retryAttempts = settingsService.getInt(SettingsDefaults.EXECUTION_RETRY_ATTEMPTS, 3);
        this.retryDelay = settingsService.getLong(SettingsDefaults.EXECUTION_RETRY_DELAY, 1000L);

        // Register all log handlers (ConsoleLogHandler, UILogHandler, etc.)
        for (ExecutionLogHandler handler : logHandlers) {
            this.executionLogger.addHandler(handler);
        }

        // Configure log handlers based on settings
        configureLogHandlers(logHandlers);
    }

    /**
     * Configure log handlers based on application settings.
     */
    private void configureLogHandlers(java.util.List<ExecutionLogHandler> logHandlers) {
        // Configure console logging level
        String logLevelStr = settingsService.getValue(SettingsDefaults.EXECUTION_LOG_LEVEL, "INFO");
        ExecutionLogHandler.LogLevel logLevel;
        try {
            logLevel = ExecutionLogHandler.LogLevel.valueOf(logLevelStr.toUpperCase());
        } catch (IllegalArgumentException _) {
            logLevel = ExecutionLogHandler.LogLevel.INFO; // Default fallback
        }

        // Apply configuration to ConsoleLogHandler instances
        for (ExecutionLogHandler handler : logHandlers) {
            if (handler instanceof ConsoleLogHandler consoleHandler) {
                consoleHandler.setMinLevel(logLevel);
                consoleHandler.setEnabled(true); // Always enable console logging
            }
        }
    }

    /**
     * Gets the default execution timeout from settings.
     * 
     * @return timeout in milliseconds
     */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * Gets the maximum number of parallel nodes from settings.
     * 
     * @return the maximum number of parallel nodes
     */
    public int getMaxParallelNodes() {
        return maxParallelNodes;
    }

    /**
     * Gets the default retry attempts from settings.
     * 
     * @return the default retry attempts
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Gets the default retry delay from settings.
     * 
     * @return delay in milliseconds
     */
    public long getRetryDelay() {
        return retryDelay;
    }

    @Override
    public List<ExecutionDTO> findAll() {
        return executionRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Optional<ExecutionDTO> findById(Long id) {
        return executionRepository.findById(id)
                .map(this::toDTO);
    }

    @Override
    public List<ExecutionDTO> findByWorkflowId(Long workflowId) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<ExecutionDTO> findRunningExecutions() {
        return executionRepository.findRunningExecutions().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void deleteAll() {
        executionRepository.deleteAll();
    }

    @Override
    public List<ExecutionDTO> findByTimeRange(Instant start, Instant end) {
        return executionRepository.findByTimeRange(start, end).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Execute a workflow asynchronously.
     */
    @Override
    public CompletableFuture<ExecutionDTO> executeAsync(Long workflowId, Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> execute(workflowId, input), executorService);
    }

    /**
     * Execute a workflow synchronously.
     */
    @Override
    public ExecutionDTO execute(Long workflowId, Map<String, Object> input) {
        WorkflowDTO workflow = workflowService.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // Reset step execution state at workflow start
        if (devModeService != null) {
            devModeService.resetStepExecution();
        }

        // Create execution record
        ExecutionEntity execution = new ExecutionEntity(workflowId, TriggerType.MANUAL);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        execution.setInputDataJson(serializeData(input));
        execution = executionRepository.save(execution);

        String executionIdStr = execution.getId().toString();

        // Register this execution for cancellation tracking
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        runningExecutions.put(execution.getId(), cancelFlag);

        // Start structured logging
        executionLogger.startExecution(executionIdStr, workflowId.toString(), workflow.name());

        try {
            // Build execution context
            ExecutionContext context = new ExecutionContext(
                    execution.getId(),
                    workflow,
                    input,
                    credentialService,
                    executionLogger,
                    cancelFlag);

            // Execute workflow
            Map<String, Object> output = executeWorkflow(workflow, context);

            // Check if cancelled during execution
            if (cancelFlag.get()) {
                execution.setStatus(ExecutionStatus.CANCELLED);
                execution.setFinishedAt(Instant.now());
                execution.setErrorMessage(EXECUTION_CANCELLED_MESSAGE);
                executionRepository.save(execution);
                executionLogger.endExecution(executionIdStr, false, null);
            } else {
                // Update execution as success
                execution.setStatus(ExecutionStatus.SUCCESS);
                execution.setFinishedAt(Instant.now());
                execution.setOutputDataJson(serializeData(output));
                execution.setExecutionLog(serializeData(context.getNodeExecutions()));
                executionRepository.save(execution);
                executionLogger.endExecution(executionIdStr, true, output);
            }

        } catch (Exception e) {
            // Check if this was a cancellation
            if (cancelFlag.get() || e.getMessage() != null && e.getMessage().contains("cancelled")) {
                execution.setStatus(ExecutionStatus.CANCELLED);
                execution.setFinishedAt(Instant.now());
                execution.setErrorMessage(EXECUTION_CANCELLED_MESSAGE);
                executionRepository.save(execution);
            } else {
                // Log the error
                executionLogger.error(executionIdStr, "workflow", e);

                // Update execution as failed
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setFinishedAt(Instant.now());
                execution.setErrorMessage(e.getMessage());
                executionRepository.save(execution);
            }

            // End structured logging (failure)
            executionLogger.endExecution(executionIdStr, false, null);
        } finally {
            // Clean up the running execution tracking
            runningExecutions.remove(execution.getId());
        }

        return toDTO(execution);
    }

    /**
     * Cancel a running execution.
     * Sets the cancellation flag which will be checked during node execution.
     * 
     * @param executionId the ID of the execution to cancel
     * @return true if the execution was cancelled, false if not found or not
     *         running
     */
    @Override
    public boolean cancelExecution(Long executionId) {
        // Set the cancellation flag for the running execution
        AtomicBoolean cancelFlag = runningExecutions.get(executionId);
        if (cancelFlag != null) {
            cancelFlag.set(true);
        }

        // Also cancel step execution in dev mode if active
        if (devModeService != null) {
            devModeService.cancelStepExecution();
        }

        // Update the database status
        Optional<ExecutionEntity> executionOpt = executionRepository.findById(executionId);
        if (executionOpt.isEmpty()) {
            return false;
        }

        ExecutionEntity execution = executionOpt.get();
        if (execution.getStatus() == ExecutionStatus.RUNNING) {
            execution.setStatus(ExecutionStatus.CANCELLED);
            execution.setFinishedAt(Instant.now());
            execution.setErrorMessage(EXECUTION_CANCELLED_MESSAGE);
            executionRepository.save(execution);
            return true;
        }
        return false;
    }

    /**
     * Cancel all running executions for a workflow.
     * 
     * @param workflowId the workflow ID
     * @return the number of executions cancelled
     */
    @Override
    public int cancelAllForWorkflow(Long workflowId) {
        List<ExecutionDTO> running = findRunningExecutions().stream()
                .filter(e -> e.workflowId().equals(workflowId))
                .toList();

        int cancelled = 0;
        for (ExecutionDTO exec : running) {
            if (cancelExecution(exec.id())) {
                cancelled++;
            }
        }
        return cancelled;
    }

    private Map<String, Object> executeWorkflow(WorkflowDTO workflow, ExecutionContext context) {
        // Find trigger nodes (entry points)
        List<Node> triggerNodes = workflow.getTriggerNodes();
        if (triggerNodes.isEmpty()) {
            throw new IllegalStateException("Workflow has no trigger nodes");
        }

        // Execute starting from trigger nodes
        Map<String, Object> lastOutput = new HashMap<>();
        for (Node trigger : triggerNodes) {
            lastOutput = executeNode(trigger, workflow, context, context.getInput());
        }

        return lastOutput;
    }

    private Map<String, Object> executeNode(
            Node node,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> input) {

        String executionIdStr = context.getExecutionId().toString();

        // Check for cancellation before executing
        if (context.isCancelled()) {
            throw new NodeExecutionException(EXECUTION_CANCELLED_MESSAGE, node.id(), node.type(), null);
        }

        if (node.disabled()) {
            executionLogger.nodeSkip(executionIdStr, node.id(), "Node is disabled");
            return input;
        }

        Instant startTime = Instant.now();
        Map<String, Object> output = executeNodeWithLogging(node, context, input, startTime, executionIdStr);

        // Check for cancellation after executing
        if (context.isCancelled()) {
            throw new NodeExecutionException(EXECUTION_CANCELLED_MESSAGE, node.id(), node.type(), null);
        }

        // Check if this execution branch should stop (e.g., non-primary merge thread)
        if (Boolean.TRUE.equals(output.get("_stopExecution"))) {
            return output;
        }

        // Step-by-step execution: wait for user to continue after each node
        if (devModeService != null && devModeService.isStepExecutionEnabled()) {
            try {
                boolean shouldContinue = devModeService.waitForStepContinue(node.id(), node.name());
                if (!shouldContinue) {
                    throw new NodeExecutionException(EXECUTION_CANCELLED_MESSAGE, node.id(), node.type(), null);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NodeExecutionException("Execution interrupted", node.id(), node.type(), e);
            }
        }

        List<Connection> outgoing = getFilteredOutgoingConnections(workflow, node, output);
        executeConnectedNodes(outgoing, workflow, context, output);

        return output;
    }

    private Map<String, Object> executeNodeWithLogging(
            Node node,
            ExecutionContext context,
            Map<String, Object> input,
            Instant startTime,
            String executionIdStr) {

        executionLogger.nodeStart(executionIdStr, node.id(), node.type(), node.name());
        executionLogger.nodeInput(executionIdStr, node.id(), node.name(), input);

        try {
            NodeExecutor executor = nodeExecutorRegistry.getExecutor(node.type());
            Map<String, Object> output = executor.execute(node, input, context);

            long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            executionLogger.nodeOutput(executionIdStr, node.id(), node.name(), output);
            context.recordNodeExecution(node.id(), ExecutionStatus.SUCCESS, startTime, output, null);
            executionLogger.nodeEnd(executionIdStr, node.id(), node.type(), durationMs, true);

            return output;
        } catch (Exception e) {
            handleNodeExecutionError(node, context, input, startTime, executionIdStr, e);
            throw new NodeExecutionException("Node execution failed: " + node.name(), node.id(), node.type(), e);
        }
    }

    private void handleNodeExecutionError(
            Node node,
            ExecutionContext context,
            Map<String, Object> input,
            Instant startTime,
            String executionIdStr,
            Exception e) {

        long durationMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
        context.recordNodeExecution(node.id(), ExecutionStatus.FAILED, startTime, null, e.getMessage());
        executionLogger.nodeEnd(executionIdStr, node.id(), node.type(), durationMs, false);
        executionLogger.errorWithContext(executionIdStr, node.id(), node.name(), input, e);
    }

    private List<Connection> getFilteredOutgoingConnections(
            WorkflowDTO workflow,
            Node node,
            Map<String, Object> output) {

        List<Connection> outgoing = workflow.getOutgoingConnections(node.id());
        String branch = output.get("branch") != null ? output.get("branch").toString() : null;

        if (branch == null) {
            return outgoing;
        }

        return outgoing.stream()
                .filter(conn -> isConnectionForBranch(conn, branch))
                .toList();
    }

    private boolean isConnectionForBranch(Connection conn, String branch) {
        String sourceOut = conn.sourceOutput();
        return sourceOut == null || sourceOut.equals("main") || sourceOut.equals(branch);
    }

    private void executeConnectedNodes(
            List<Connection> outgoing,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> output) {

        // Separate loop connections from non-loop connections
        List<Connection> loopConnections = outgoing.stream()
                .filter(conn -> "loop".equals(conn.sourceOutput()))
                .toList();
        List<Connection> nonLoopConnections = outgoing.stream()
                .filter(conn -> !"loop".equals(conn.sourceOutput()))
                .toList();

        // Execute loop connections - iterate over results and execute for each item
        if (!loopConnections.isEmpty()) {
            executeLoopConnections(loopConnections, workflow, context, output);
        }

        // Execute non-loop connections normally
        if (!nonLoopConnections.isEmpty()) {
            if (nonLoopConnections.size() > 1) {
                executeNodesInParallel(nonLoopConnections, workflow, context, output);
            } else {
                executeNodesSequentially(nonLoopConnections, workflow, context, output);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeLoopConnections(
            List<Connection> loopConnections,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> output) {

        // Get the results array from the loop executor output
        Object resultsObj = output.get("results");
        if (!(resultsObj instanceof List<?>)) {
            return; // No results to iterate over
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
        if (results.isEmpty()) {
            return; // Empty results
        }

        // For each item in results, execute all loop-connected nodes
        for (Map<String, Object> itemResult : results) {
            // Build input for this iteration - spread item properties into input
            Map<String, Object> iterationInput = new HashMap<>(output);

            // Add item and index directly to input
            iterationInput.put("item", itemResult.get("item"));
            iterationInput.put("index", itemResult.get("index"));

            // Also spread the item's properties directly so {{ propertyName }} works
            Object item = itemResult.get("item");
            if (item instanceof Map<?, ?> itemMap) {
                for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                    iterationInput.put(entry.getKey().toString(), entry.getValue());
                }
            }

            // Execute all loop-connected nodes for this item
            for (Connection connection : loopConnections) {
                Node targetNode = getTargetNode(workflow, connection);
                executeNode(targetNode, workflow, context, iterationInput);
            }
        }
    }

    private void executeNodesInParallel(
            List<Connection> outgoing,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> output) {

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<Map<String, Object>>> futures = outgoing.stream()
                    .map(connection -> executor.submit(() -> {
                        Node targetNode = getTargetNode(workflow, connection);
                        return executeNode(targetNode, workflow, context, output);
                    }))
                    .toList();

            waitForAllFutures(futures);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeExecutionException("Parallel execution interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new NodeExecutionException("Parallel execution failed", e.getCause());
        }
    }

    private void executeNodesSequentially(
            List<Connection> outgoing,
            WorkflowDTO workflow,
            ExecutionContext context,
            Map<String, Object> output) {

        for (Connection connection : outgoing) {
            Node targetNode = getTargetNode(workflow, connection);
            executeNode(targetNode, workflow, context, output);
        }
    }

    private Node getTargetNode(WorkflowDTO workflow, Connection connection) {
        Node targetNode = workflow.findNode(connection.targetNodeId());
        if (targetNode == null) {
            throw new IllegalStateException("Target node not found: " + connection.targetNodeId());
        }
        return targetNode;
    }

    private void waitForAllFutures(List<java.util.concurrent.Future<Map<String, Object>>> futures)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        for (var future : futures) {
            future.get();
        }
    }

    private ExecutionDTO toDTO(ExecutionEntity entity) {
        List<ExecutionDTO.NodeExecutionDTO> nodeExecutions = parseNodeExecutions(entity.getExecutionLog());

        Long durationMs = null;
        if (entity.getStartedAt() != null && entity.getFinishedAt() != null) {
            durationMs = java.time.Duration.between(entity.getStartedAt(), entity.getFinishedAt()).toMillis();
        }

        return new ExecutionDTO(
                entity.getId(),
                entity.getWorkflowId(),
                null, // workflowName - could fetch from workflow if needed
                entity.getStatus(),
                entity.getTriggerType(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                durationMs,
                parseData(entity.getInputDataJson()),
                parseData(entity.getOutputDataJson()),
                entity.getErrorMessage(),
                nodeExecutions);
    }

    private String serializeData(Object data) {
        if (data == null)
            return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to serialize data", "execution", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank())
            return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to parse data", "execution", e);
        }
    }

    private List<ExecutionDTO.NodeExecutionDTO> parseNodeExecutions(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class,
                            ExecutionDTO.NodeExecutionDTO.class));
        } catch (JsonProcessingException _) {
            return List.of();
        }
    }

    /**
     * Execution context holds state during workflow execution.
     */
    public static class ExecutionContext {
        private final Long executionId;
        private final WorkflowDTO workflow;
        private final Map<String, Object> input;
        private final CredentialService credentialService;
        private final ExecutionLogger executionLogger;
        private final AtomicBoolean cancelFlag;
        private final List<Map<String, Object>> nodeExecutions = new ArrayList<>();

        /**
         * Creates a new execution context.
         * 
         * @param executionId       the execution ID
         * @param workflow          the workflow DTO
         * @param input             the input data
         * @param credentialService the credential service
         * @param executionLogger   the execution logger
         * @param cancelFlag        the cancel flag
         */
        public ExecutionContext(Long executionId, WorkflowDTO workflow,
                Map<String, Object> input, CredentialService credentialService,
                ExecutionLogger executionLogger,
                AtomicBoolean cancelFlag) {
            this.executionId = executionId;
            this.workflow = workflow;
            this.input = input;
            this.credentialService = credentialService;
            this.executionLogger = executionLogger;
            this.cancelFlag = cancelFlag;
        }

        /**
         * Constructor without cancel flag (for testing/backwards compatibility).
         * 
         * @param executionId       the execution ID
         * @param workflow          the workflow DTO
         * @param input             the input data
         * @param credentialService the credential service
         * @param executionLogger   the execution logger
         */
        public ExecutionContext(Long executionId, WorkflowDTO workflow,
                Map<String, Object> input, CredentialService credentialService,
                ExecutionLogger executionLogger) {
            this(executionId, workflow, input, credentialService, executionLogger, new AtomicBoolean(false));
        }

        /**
         * Gets the execution ID.
         * 
         * @return the execution ID
         */
        public Long getExecutionId() {
            return executionId;
        }

        /**
         * Gets the workflow.
         * 
         * @return the workflow DTO
         */
        public WorkflowDTO getWorkflow() {
            return workflow;
        }

        /**
         * Gets the input data.
         * 
         * @return the input data map
         */
        public Map<String, Object> getInput() {
            return input;
        }

        /**
         * Check if this execution has been cancelled.
         * 
         * @return true if cancelled
         */
        public boolean isCancelled() {
            return cancelFlag != null && cancelFlag.get();
        }

        /**
         * Gets decrypted credential data.
         * 
         * @param credentialId the credential ID
         * @return the decrypted credential data
         */
        public String getDecryptedCredential(Long credentialId) {
            return credentialService.getDecryptedData(credentialId);
        }

        /**
         * Gets decrypted credential data by name.
         * 
         * @param name the credential name
         * @return the decrypted credential data or null if not found
         */
        public String getDecryptedCredentialByName(String name) {
            var credentialOpt = credentialService.findByName(name);
            if (credentialOpt.isPresent()) {
                return credentialService.getDecryptedData(credentialOpt.get().id());
            }
            return null;
        }

        /**
         * Gets the execution logger.
         * 
         * @return the execution logger
         */
        public ExecutionLogger getExecutionLogger() {
            return executionLogger;
        }

        /**
         * Records a node execution.
         * 
         * @param nodeId    the node ID
         * @param status    the execution status
         * @param startTime the start time
         * @param output    the output data
         * @param error     the error message
         */
        public void recordNodeExecution(String nodeId, ExecutionStatus status,
                Instant startTime, Map<String, Object> output, String error) {
            nodeExecutions.add(Map.of(
                    "nodeId", nodeId,
                    "status", status.name(),
                    "startedAt", startTime.toString(),
                    "finishedAt", Instant.now().toString(),
                    "output", output != null ? output : Map.of(),
                    "error", error != null ? error : ""));
        }

        /**
         * Gets the recorded node executions.
         * 
         * @return list of node execution records
         */
        public List<Map<String, Object>> getNodeExecutions() {
            return nodeExecutions;
        }
    }
}
