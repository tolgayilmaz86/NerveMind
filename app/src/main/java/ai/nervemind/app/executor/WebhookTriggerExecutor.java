package ai.nervemind.app.executor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "webhookTrigger" node type - starts workflow execution via
 * HTTP requests.
 *
 * <p>
 * Enables external systems to trigger workflow execution by calling an HTTP
 * endpoint.
 * The actual HTTP endpoint registration is managed by {@code WebhookService};
 * this executor
 * is invoked when an HTTP request arrives at the configured path.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Webhook trigger configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>String</td>
 * <td>"/webhook"</td>
 * <td>URL path for the webhook endpoint</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>String</td>
 * <td>"POST"</td>
 * <td>HTTP method (GET, POST, PUT, etc.)</td>
 * </tr>
 * <tr>
 * <td>authentication</td>
 * <td>String</td>
 * <td>"none"</td>
 * <td>Auth type: "none", "apiKey", "basic"</td>
 * </tr>
 * </table>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys containing incoming HTTP request data</caption>
 * <tr>
 * <th>Key</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>triggeredAt</td>
 * <td>String</td>
 * <td>ISO timestamp when request was received</td>
 * </tr>
 * <tr>
 * <td>triggerType</td>
 * <td>String</td>
 * <td>Always "webhook" for this trigger</td>
 * </tr>
 * <tr>
 * <td>body</td>
 * <td>Map</td>
 * <td>Parsed JSON request body</td>
 * </tr>
 * <tr>
 * <td>headers</td>
 * <td>Map</td>
 * <td>HTTP request headers</td>
 * </tr>
 * <tr>
 * <td>queryParams</td>
 * <td>Map</td>
 * <td>URL query parameters</td>
 * </tr>
 * <tr>
 * <td>requestMethod</td>
 * <td>String</td>
 * <td>HTTP method used in request</td>
 * </tr>
 * <tr>
 * <td>requestPath</td>
 * <td>String</td>
 * <td>Requested URL path</td>
 * </tr>
 * <tr>
 * <td>remoteAddress</td>
 * <td>String</td>
 * <td>IP address of the caller</td>
 * </tr>
 * </table>
 *
 * <h2>Webhook URL Format</h2>
 * 
 * <pre>
 *   POST http://localhost:8080/api/webhook/{workflowId}/{path}
 * </pre>
 *
 * /**
 * Executor for webhook-based workflow triggers.
 * Handles HTTP webhook requests and converts them into workflow execution
 * events.
 *
 * @see ManualTriggerExecutor For on-demand execution
 * @see ScheduleTriggerExecutor For time-based automated execution
 * @see "ai.nervemind.app.controller.WebhookController (The controller handling webhook requests)"
 */
@Component
public class WebhookTriggerExecutor implements NodeExecutor {

    // Default constructor for Spring
    /**
     * Default constructor for Spring.
     */
    public WebhookTriggerExecutor() {
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        Map<String, Object> output = new HashMap<>();
        output.put("triggeredAt", LocalDateTime.now().toString());
        output.put("triggerType", "webhook");

        // Include webhook configuration
        String path = (String) params.getOrDefault("path", "/webhook");
        String method = (String) params.getOrDefault("method", "POST");
        String authentication = (String) params.getOrDefault("authentication", "none");

        output.put("webhookPath", path);
        output.put("webhookMethod", method);
        output.put("authentication", authentication);

        // Include incoming request data from context
        Map<String, Object> contextInput = context.getInput();

        // HTTP request data passed from WebhookController
        output.put("body", contextInput.getOrDefault("body", Map.of()));
        output.put("headers", contextInput.getOrDefault("headers", Map.of()));
        output.put("queryParams", contextInput.getOrDefault("queryParams", Map.of()));
        output.put("requestMethod", contextInput.getOrDefault("requestMethod", method));
        output.put("requestPath", contextInput.getOrDefault("requestPath", path));
        output.put("remoteAddress", contextInput.getOrDefault("remoteAddress", "unknown"));

        return output;
    }

    @Override
    public String getNodeType() {
        return "webhookTrigger";
    }
}
