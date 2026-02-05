package ai.nervemind.app.executor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "scheduleTrigger" node type - starts workflow execution on a
 * schedule.
 *
 * <p>
 * Enables time-based automated workflow execution using cron expressions.
 * The actual scheduling is managed by {@code SchedulerService}; this executor
 * is invoked when the scheduled time arrives.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Schedule trigger configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>cronExpression</td>
 * <td>String</td>
 * <td>""</td>
 * <td>Cron expression for scheduling</td>
 * </tr>
 * <tr>
 * <td>timezone</td>
 * <td>String</td>
 * <td>"UTC"</td>
 * <td>Timezone for schedule evaluation</td>
 * </tr>
 * </table>
 *
 * <h2>Cron Expression Examples</h2>
 * <ul>
 * <li><code>0 0 * * *</code> - Daily at midnight</li>
 * <li><code>0 9 * * 1-5</code> - Weekdays at 9 AM</li>
 * <li><code>0/30 * * * *</code> - Every 30 minutes</li>
 * <li><code>0 8 1 * *</code> - Monthly on the 1st at 8 AM</li>
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
 * <td>triggeredAt</td>
 * <td>String</td>
 * <td>ISO timestamp when execution started</td>
 * </tr>
 * <tr>
 * <td>triggerType</td>
 * <td>String</td>
 * <td>Always "schedule" for this trigger</td>
 * </tr>
 * <tr>
 * <td>cronExpression</td>
 * <td>String</td>
 * <td>The cron expression that fired</td>
 * </tr>
 * <tr>
 * <td>timezone</td>
 * <td>String</td>
 * <td>The configured timezone</td>
 * </tr>
 * <tr>
 * <td>scheduledTime</td>
 * <td>String</td>
 * <td>The scheduled time that triggered this run</td>
 * </tr>
 * </table>
 *
 * @see ManualTriggerExecutor For on-demand execution
 * @see WebhookTriggerExecutor For HTTP request-triggered execution
 * @see ai.nervemind.app.service.SchedulerService The service that manages
 *      schedules
 */
@Component
public class ScheduleTriggerExecutor implements NodeExecutor {

    /**
     * Default constructor.
     */
    public ScheduleTriggerExecutor() {
        // Default constructor
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        Map<String, Object> output = new HashMap<>(input);
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "schedule");

        // Include schedule info in output
        String cronExpression = (String) params.getOrDefault("cronExpression", "");
        String timezone = (String) params.getOrDefault("timezone", "UTC");

        output.put("cronExpression", cronExpression);
        output.put("timezone", timezone);
        output.put("scheduledTime", context.getInput().getOrDefault("scheduledTime", LocalDateTime.now().toString()));

        return output;
    }

    @Override
    public String getNodeType() {
        return "scheduleTrigger";
    }
}
