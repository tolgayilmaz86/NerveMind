package ai.nervemind.app.executor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.FileWatcherService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "fileTrigger" node type - initiates workflow execution on
 * file system events.
 *
 * <p>
 * Monitors specified directories for file changes and triggers workflow
 * execution
 * when matching events occur. The actual file monitoring is performed by
 * {@link FileWatcherService}; this executor handles both automatic
 * event-triggered
 * runs and manual test executions.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>File trigger configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>watchPath</td>
 * <td>String</td>
 * <td>-</td>
 * <td><strong>Required.</strong> Directory path to monitor</td>
 * </tr>
 * <tr>
 * <td>filePattern</td>
 * <td>String</td>
 * <td>"*"</td>
 * <td>Glob pattern to filter files (e.g., "*.pdf", "report-*.xlsx")</td>
 * </tr>
 * <tr>
 * <td>eventTypes</td>
 * <td>String</td>
 * <td>"CREATE,MODIFY"</td>
 * <td>Comma-separated list: CREATE, MODIFY, DELETE</td>
 * </tr>
 * </table>
 *
 * <h2>Execution Modes</h2>
 * <p>
 * This executor operates in two modes:
 * </p>
 * <ul>
 * <li><strong>File Event Mode:</strong> Triggered automatically when a file
 * event occurs.
 * Input contains filePath, fileName, directory, and eventType.</li>
 * <li><strong>Manual Mode:</strong> Triggered via UI "Run" button for testing.
 * Provides configuration validation but no actual file data.</li>
 * </ul>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr>
 * <th>Key</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>watchPath</td>
 * <td>String</td>
 * <td>The monitored directory path</td>
 * </tr>
 * <tr>
 * <td>pathExists</td>
 * <td>Boolean</td>
 * <td>Whether the path exists on disk</td>
 * </tr>
 * <tr>
 * <td>isDirectory</td>
 * <td>Boolean</td>
 * <td>Whether the path is a valid directory</td>
 * </tr>
 * <tr>
 * <td>filePattern</td>
 * <td>String</td>
 * <td>The glob pattern being used</td>
 * </tr>
 * <tr>
 * <td>eventTypes</td>
 * <td>String</td>
 * <td>Monitored event types</td>
 * </tr>
 * <tr>
 * <td>triggeredAt</td>
 * <td>String</td>
 * <td>ISO timestamp</td>
 * </tr>
 * <tr>
 * <td>triggerType</td>
 * <td>String</td>
 * <td>Always "file_event"</td>
 * </tr>
 * <tr>
 * <td>mode</td>
 * <td>String</td>
 * <td>"file_event" or "manual"</td>
 * </tr>
 * </table>
 *
 * <h2>File Event Output (when triggered by actual file event)</h2>
 * <ul>
 * <li><strong>filePath</strong> - Full path to the affected file</li>
 * <li><strong>fileName</strong> - Just the file name</li>
 * <li><strong>directory</strong> - Parent directory</li>
 * <li><strong>eventType</strong> - CREATE, MODIFY, or DELETE</li>
 * </ul>
 *
 * @see ManualTriggerExecutor For on-demand execution
 * @see FileWatcherService The service that monitors file system changes
 */
@Component
public class FileTriggerExecutor implements NodeExecutor {

    private final ObjectProvider<FileWatcherService> fileWatcherServiceProvider;

    /**
     * Creates a new file trigger executor.
     *
     * @param fileWatcherServiceProvider provider for the file watcher service
     */
    public FileTriggerExecutor(ObjectProvider<FileWatcherService> fileWatcherServiceProvider) {
        this.fileWatcherServiceProvider = fileWatcherServiceProvider;
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(input);

        String watchPath = (String) node.parameters().get("watchPath");
        String filePattern = (String) node.parameters().getOrDefault("filePattern", "*");
        String eventTypes = (String) node.parameters().getOrDefault("eventTypes", "CREATE,MODIFY");

        // Validate configuration
        if (watchPath == null || watchPath.isBlank()) {
            throw new IllegalArgumentException("Watch path is required");
        }

        // Basic validation of the path
        java.nio.file.Path path = java.nio.file.Paths.get(watchPath);
        boolean pathExists = java.nio.file.Files.exists(path);

        output.put("watchPath", watchPath);
        output.put("pathExists", pathExists);
        output.put("isDirectory", pathExists && java.nio.file.Files.isDirectory(path));
        output.put("filePattern", filePattern);
        output.put("eventTypes", eventTypes);
        output.put("triggeredAt", java.time.LocalDateTime.now().toString());
        output.put("triggerType", "file_event"); // Standardize on trigger type

        // ADDED DIAGNOSTICS
        ai.nervemind.app.service.FileWatcherService watcher = fileWatcherServiceProvider.getIfAvailable();
        if (watcher != null) {
            output.put("watcherServiceStatus", "RUNNING");
            output.put("activeWatchers", watcher.getActiveWatcherCount());
            boolean isWatched = watcher.isWatched(context.getWorkflow().id());
            output.put("isThisWorkflowWatched", isWatched);
        } else {
            output.put("watcherServiceStatus", "UNAVAILABLE (Service bean not found)");
        }

        if (input.containsKey("filePath")) {
            // This execution was triggered by a real file event
            output.put("mode", "file_event");
            // Input already contains: filePath, fileName, directory, eventType
        } else {
            // Manual execution - provide sample/test data
            output.put("mode", "manual");
            output.put("message", "File trigger configured. When active, this will monitor: " + watchPath);
            output.put("note", "To test file watching, activate the workflow and drop a file in the watched folder.");
        }

        return output;
    }

    @Override
    public String getNodeType() {
        return "fileTrigger";
    }
}
