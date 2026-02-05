package ai.nervemind.app.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for watching file system events and triggering workflows.
 * Enables workflows to be triggered when files are created, modified, or
 * deleted
 * in specified directories.
 */
@Service
public class FileWatcherService {

    private static final Logger log = LoggerFactory.getLogger(FileWatcherService.class);
    private static final String PARAM_WATCH_PATH = "watchPath";
    private static final String PARAM_EVENT_TYPES = "eventTypes";
    private static final String PARAM_FILE_PATTERN = "filePattern";
    private static final String NODE_TYPE_FILE_TRIGGER = "fileTrigger";

    private final WorkflowService workflowService;
    private final ExecutionService executionService;

    private WatchService watchService;
    private final Map<WatchKey, WatcherContext> watchKeyToContext = new ConcurrentHashMap<>();
    private final Map<Long, WatchKey> workflowToWatchKey = new ConcurrentHashMap<>();
    private final ExecutorService watcherExecutor;

    private volatile boolean running = false;

    /**
     * Creates a new FileWatcherService with required dependencies.
     *
     * @param workflowService  the workflow service for managing workflows
     * @param executionService the execution service for triggering workflows
     */
    public FileWatcherService(WorkflowService workflowService, ExecutionService executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
        this.watcherExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Initializes the file watcher service after construction.
     * Sets up the watch service and registers active file-triggered workflows.
     */
    @PostConstruct
    public void initialize() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            running = true;

            // Start the watcher thread
            watcherExecutor.submit(this::watchLoop);

            // Register all active file-triggered workflows
            workflowService.findAll().stream()
                    .filter(w -> w.isActive() && w.triggerType() == TriggerType.FILE_EVENT)
                    .forEach(this::registerWorkflow);

            log.info("FileWatcherService initialized with {} active watchers", workflowToWatchKey.size());
        } catch (IOException e) {
            log.error("Failed to initialize FileWatcherService", e);
        }
    }

    /**
     * Shuts down the file watcher service and releases resources.
     * Stops the watcher thread and closes the watch service.
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        watcherExecutor.shutdown();
        try {
            if (!watcherExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watcherExecutor.shutdownNow();
            }
            if (watchService != null) {
                watchService.close();
            }
        } catch (InterruptedException | IOException _) {
            watcherExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Register a workflow for file watching.
     * 
     * @param workflow The workflow to register
     */
    public void registerWorkflow(WorkflowDTO workflow) {
        // If workflow is inactive or not a file trigger, ensure it's unregistered
        if (workflow.triggerType() != TriggerType.FILE_EVENT || !workflow.isActive()) {
            unregisterWorkflow(workflow.id());
            return;
        }

        // Unregister existing watcher if any
        unregisterWorkflow(workflow.id());

        // Extract watch path from workflow parameters
        String watchPathStr = extractWatchPath(workflow);
        if (watchPathStr == null || watchPathStr.isBlank()) {
            log.warn("Workflow {} has no watchPath configured", workflow.name());
            return;
        }

        Path watchPath = Path.of(watchPathStr);
        if (!Files.isDirectory(watchPath)) {
            log.warn("Watch path does not exist or is not a directory: {}", watchPath);
            return;
        }

        try {
            WatchKey watchKey = watchPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            String filePattern = extractFilePattern(workflow);
            String eventTypes = extractEventTypes(workflow);

            WatcherContext context = new WatcherContext(
                    workflow.id(),
                    workflow.name(),
                    watchPath,
                    filePattern,
                    eventTypes);

            watchKeyToContext.put(watchKey, context);
            workflowToWatchKey.put(workflow.id(), watchKey);
        } catch (IOException e) {
            log.error("Failed to register file watcher for workflow '{}'", workflow.name(), e);
        }
    }

    /**
     * Unregister a workflow from file watching.
     * 
     * @param workflowId The workflow ID to unregister
     */
    public void unregisterWorkflow(Long workflowId) {
        WatchKey watchKey = workflowToWatchKey.remove(workflowId);
        if (watchKey != null) {
            watchKey.cancel();
            watchKeyToContext.remove(watchKey);
            log.info("Unregistered file watcher for workflow ID: {}", workflowId);
        }
    }

    private void watchLoop() {
        log.info("File watcher loop started");
        while (running) {
            try {
                processWatchEvents();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in file watcher loop", e);
            }
        }
        log.info("File watcher loop stopped");
    }

    private void processWatchEvents() throws InterruptedException {
        WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
        if (key == null) {
            return;
        }

        WatcherContext context = watchKeyToContext.get(key);
        if (context == null) {
            key.cancel();
            return;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
            processEvent(event, context);
        }

        resetWatchKey(key, context);
    }

    private void processEvent(WatchEvent<?> event, WatcherContext context) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == StandardWatchEventKinds.OVERFLOW) {
            return;
        }

        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
        Path fileName = pathEvent.context();
        Path fullPath = context.watchPath().resolve(fileName);

        if (!matchesPattern(fileName.toString(), context.filePattern())) {
            return;
        }

        if (!matchesEventType(kind, context.eventTypes())) {
            return;
        }

        triggerWorkflow(context, kind.name(), fullPath);
    }

    private void resetWatchKey(WatchKey key, WatcherContext context) {
        boolean valid = key.reset();
        if (!valid) {
            log.warn("Watch key no longer valid for workflow: {}", context.workflowName());
            workflowToWatchKey.remove(context.workflowId());
            watchKeyToContext.remove(key);
        }
    }

    private void triggerWorkflow(WatcherContext context, String eventType, Path filePath) {
        try {
            Map<String, Object> triggerData = Map.of(
                    "triggeredAt", Instant.now().toString(),
                    "triggerType", "file_event",
                    "eventType", eventType,
                    "filePath", filePath.toString(),
                    "fileName", filePath.getFileName().toString(),
                    "directory", filePath.getParent().toString());

            executionService.execute(context.workflowId(), triggerData);
        } catch (Exception e) {
            log.error("Failed to trigger workflow '{}' for file event", context.workflowName(), e);
        }
    }

    private String extractWatchPath(WorkflowDTO workflow) {
        // Look for watchPath in trigger node parameters
        if (workflow.nodes() != null) {
            for (var node : workflow.nodes()) {
                if (NODE_TYPE_FILE_TRIGGER.equals(node.type())) {
                    Object watchPath = node.parameters().get(PARAM_WATCH_PATH);
                    return watchPath != null ? watchPath.toString() : null;
                }
            }
        }
        return null;
    }

    /**
     * Check if a workflow is currently being watched.
     * 
     * @param workflowId The workflow ID
     * @return true if watched
     */
    public boolean isWatched(Long workflowId) {
        return workflowToWatchKey.containsKey(workflowId);
    }

    /**
     * Get the number of active watchers.
     * 
     * @return active watcher count
     */
    public int getActiveWatcherCount() {
        return workflowToWatchKey.size();
    }

    private String extractFilePattern(WorkflowDTO workflow) {
        if (workflow.nodes() != null) {
            for (var node : workflow.nodes()) {
                if (NODE_TYPE_FILE_TRIGGER.equals(node.type())) {
                    Object pattern = node.parameters().get(PARAM_FILE_PATTERN);
                    if (pattern != null) {
                        return pattern.toString();
                    }
                }
            }
        }
        return "*"; // Match all files by default
    }

    private String extractEventTypes(WorkflowDTO workflow) {
        if (workflow.nodes() != null) {
            for (var node : workflow.nodes()) {
                if (NODE_TYPE_FILE_TRIGGER.equals(node.type())) {
                    Object eventTypes = node.parameters().get(PARAM_EVENT_TYPES);
                    if (eventTypes != null) {
                        return eventTypes.toString();
                    }
                }
            }
        }
        return "CREATE,MODIFY,DELETE"; // All events by default
    }

    private boolean matchesPattern(String fileName, String pattern) {
        if (pattern == null || pattern.equals("*") || pattern.isBlank()) {
            return true;
        }
        // Simple glob pattern matching
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return fileName.matches(regex);
    }

    private boolean matchesEventType(WatchEvent.Kind<?> kind, String eventTypes) {
        if (eventTypes == null || eventTypes.isBlank()) {
            return true;
        }
        String kindName = kind.name().replace("ENTRY_", "");
        return eventTypes.toUpperCase().contains(kindName);
    }

    /**
     * Context for a registered file watcher.
     */
    private record WatcherContext(
            Long workflowId,
            String workflowName,
            Path watchPath,
            String filePattern,
            String eventTypes) {
    }
}
