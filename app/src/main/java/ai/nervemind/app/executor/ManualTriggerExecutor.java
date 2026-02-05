package ai.nervemind.app.executor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "manualTrigger" node type - initiates workflow execution on
 * demand.
 *
 * <p>
 * The Manual Trigger is the simplest trigger type, allowing users to start
 * workflow execution through the UI "Run" button or API calls. It serves as
 * the entry point for workflows that don't require automated scheduling or
 * external event triggers.
 * </p>
 *
 * <h2>Trigger Flow</h2>
 * 
 * <pre>
 *   User clicks "Run" → ExecutionService → ManualTriggerExecutor → Downstream nodes
 * </pre>
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
 * <td>triggeredAt</td>
 * <td>String</td>
 * <td>ISO timestamp when execution started</td>
 * </tr>
 * <tr>
 * <td>triggerType</td>
 * <td>String</td>
 * <td>Always "manual" for this trigger</td>
 * </tr>
 * </table>
 *
 * <h2>Input Data Passthrough</h2>
 * <p>
 * Any input data provided when starting the execution (e.g., via API)
 * is merged with the trigger outputs and passed to downstream nodes.
 * </p>
 *
 * @see ScheduleTriggerExecutor For time-based automated execution
 * @see WebhookTriggerExecutor For HTTP request-triggered execution
 * @see FileTriggerExecutor For file system event-triggered execution
 */
@Component
public class ManualTriggerExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public ManualTriggerExecutor() {
        // Default constructor
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> output = new HashMap<>(input);
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "manual");
        return output;
    }

    @Override
    public String getNodeType() {
        return "manualTrigger";
    }
}
