/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;

/**
 * Node executor for Slack messaging via the Slack Web API.
 *
 * <p>
 * Sends messages to Slack channels and retrieves channel history using
 * the official Slack Web API over HTTP. No SDK dependency required.
 * Requires a Slack Bot Token (xoxb-*) stored as a credential.
 * </p>
 *
 * <h2>Node Configuration</h2>
 * <table border="1">
 * <caption>Slack node parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>action</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>SEND or HISTORY</td>
 * </tr>
 * <tr>
 * <td>channel</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Channel ID (e.g. C01234567)</td>
 * </tr>
 * <tr>
 * <td>message</td>
 * <td>String</td>
 * <td>SEND</td>
 * <td>Message text (supports Slack mrkdwn)</td>
 * </tr>
 * <tr>
 * <td>limit</td>
 * <td>Integer</td>
 * <td>No</td>
 * <td>Max messages for HISTORY (default: 10)</td>
 * </tr>
 * </table>
 *
 * <h2>Authentication</h2>
 * <p>
 * Assign an API_KEY credential containing the Slack Bot Token (xoxb-...).
 * The token needs <code>chat:write</code> scope for SEND and
 * <code>channels:history</code> for HISTORY.
 * </p>
 *
 * <h2>Output Data</h2>
 * <ul>
 * <li><strong>success</strong> - Boolean indicating API success</li>
 * <li><strong>ok</strong> - Slack API ok field</li>
 * <li><strong>ts</strong> - Message timestamp (SEND)</li>
 * <li><strong>channel</strong> - Channel ID</li>
 * <li><strong>messages</strong> - List of messages (HISTORY)</li>
 * <li><strong>response</strong> - Full API response as JSON string</li>
 * </ul>
 *
 * @since 1.1.0
 * @see NodeExecutor
 */
@Component
public class SlackExecutor implements NodeExecutor {

    private static final String SLACK_API_BASE = "https://slack.com/api/";
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_HISTORY_LIMIT = 10;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Default constructor.
     */
    public SlackExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String action = ((String) params.getOrDefault("action", "SEND")).toUpperCase();
        String channel = getRequiredString(params, "channel");

        // Get bot token from credential
        String botToken = resolveToken(node, context);

        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG,
                "Slack: action=" + action + ", channel=" + channel,
                Map.of());

        return switch (action) {
            case "SEND" -> sendMessage(params, channel, botToken, context);
            case "HISTORY" -> getHistory(params, channel, botToken, context);
            default -> throw new NodeExecutionException(
                    "Unsupported Slack action: " + action + ". Use SEND or HISTORY.");
        };
    }

    @Override
    public String getNodeType() {
        return "slack";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendMessage(Map<String, Object> params,
            String channel, String botToken, ExecutionService.ExecutionContext context) {
        String message = getRequiredString(params, "message");

        Map<String, Object> body = new HashMap<>();
        body.put("channel", channel);
        body.put("text", message);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SLACK_API_BASE + "chat.postMessage"))
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + botToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> apiResponse = objectMapper.readValue(response.body(), Map.class);
            boolean ok = Boolean.TRUE.equals(apiResponse.get("ok"));

            if (!ok) {
                String error = (String) apiResponse.getOrDefault("error", "unknown_error");
                throw new NodeExecutionException("Slack API error: " + error);
            }

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "Slack: message sent, ts=" + apiResponse.get("ts"),
                    Map.of());

            Map<String, Object> output = new HashMap<>();
            output.put("success", true);
            output.put("ok", true);
            output.put("ts", apiResponse.get("ts"));
            output.put("channel", channel);
            output.put("response", response.body());
            return output;

        } catch (NodeExecutionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeExecutionException("Slack request interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new NodeExecutionException("Slack send failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getHistory(Map<String, Object> params,
            String channel, String botToken, ExecutionService.ExecutionContext context) {
        int limit = ((Number) params.getOrDefault("limit", DEFAULT_HISTORY_LIMIT)).intValue();

        try {
            String url = SLACK_API_BASE + "conversations.history?channel=" + channel + "&limit=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                    .header("Authorization", "Bearer " + botToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> apiResponse = objectMapper.readValue(response.body(), Map.class);
            boolean ok = Boolean.TRUE.equals(apiResponse.get("ok"));

            if (!ok) {
                String error = (String) apiResponse.getOrDefault("error", "unknown_error");
                throw new NodeExecutionException("Slack API error: " + error);
            }

            Object messages = apiResponse.get("messages");
            int messageCount = messages instanceof java.util.List<?> list ? list.size() : 0;

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "Slack: fetched " + messageCount + " messages from history",
                    Map.of());

            Map<String, Object> output = new HashMap<>();
            output.put("success", true);
            output.put("ok", true);
            output.put("messages", messages);
            output.put("messageCount", messageCount);
            output.put("channel", channel);
            output.put("response", response.body());
            return output;

        } catch (NodeExecutionException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeExecutionException("Slack request interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new NodeExecutionException("Slack history failed: " + e.getMessage(), e);
        }
    }

    private String resolveToken(Node node, ExecutionService.ExecutionContext context) {
        if (node.credentialId() != null) {
            String token = context.getDecryptedCredential(node.credentialId());
            if (token != null && !token.isBlank()) {
                return token;
            }
        }
        throw new NodeExecutionException(
                "Slack Bot Token required. Assign an API_KEY credential containing your xoxb-* token.");
    }

    private String getRequiredString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new NodeExecutionException("Required parameter '" + key + "' is missing or empty");
        }
        return value.toString();
    }
}
