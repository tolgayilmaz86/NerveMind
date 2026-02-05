package ai.nervemind.app.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Node executor for HTTP Request operations.
 * 
 * <p>
 * This executor makes HTTP calls to external APIs and services. It supports
 * all standard HTTP methods, custom headers, authentication, and template
 * interpolation for dynamic values.
 * </p>
 * 
 * <h2>Node Configuration</h2>
 * <table border="1">
 * <caption>HTTP Request node parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>url</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>The URL to call (supports {{var}} interpolation)</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>String</td>
 * <td>No</td>
 * <td>HTTP method: GET, POST, PUT, DELETE, etc. (default: GET)</td>
 * </tr>
 * <tr>
 * <td>headers</td>
 * <td>Map</td>
 * <td>No</td>
 * <td>Custom HTTP headers</td>
 * </tr>
 * <tr>
 * <td>body</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Request body (supports interpolation)</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Integer</td>
 * <td>No</td>
 * <td>Timeout in seconds (default from settings)</td>
 * </tr>
 * </table>
 * 
 * <h2>Output Data</h2>
 * <p>
 * The executor outputs a Map with the following keys:
 * </p>
 * <ul>
 * <li><strong>statusCode</strong> - HTTP status code (e.g., 200, 404)</li>
 * <li><strong>body</strong> - Response body as string</li>
 * <li><strong>headers</strong> - Response headers as Map</li>
 * <li><strong>success</strong> - Boolean, true if status is 2xx</li>
 * <li><strong>json</strong> - Response body (if detected as JSON)</li>
 * </ul>
 * 
 * <h2>Template Interpolation</h2>
 * <p>
 * URL and body support <code>{{variableName}}</code> syntax for dynamic values:
 * </p>
 * <ul>
 * <li>Variables from input data (previous node output)</li>
 * <li>Workflow settings</li>
 * <li>Stored credentials (referenced by name)</li>
 * </ul>
 * 
 * <h2>Authentication</h2>
 * <p>
 * For authenticated requests, either:
 * </p>
 * <ul>
 * <li>Assign a credential to the node (adds Bearer token header)</li>
 * <li>Use <code>{{credentialName}}</code> in headers/URL for manual auth</li>
 * </ul>
 * 
 * <h2>Performance</h2>
 * <p>
 * Uses virtual threads (Project Loom) for efficient non-blocking I/O.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ai.nervemind.app.service.NodeExecutor Base executor interface
 */
@Component
public class HttpRequestExecutor implements NodeExecutor {

    private final HttpClient httpClient;
    private final int defaultTimeout;

    /**
     * Creates a new HTTP request executor with configured timeouts.
     *
     * @param settingsService the settings service for timeout configuration
     */
    public HttpRequestExecutor(SettingsServiceInterface settingsService) {
        int connectTimeout = settingsService.getInt(SettingsDefaults.HTTP_CONNECT_TIMEOUT, 30);
        this.defaultTimeout = settingsService.getInt(SettingsDefaults.HTTP_READ_TIMEOUT, 30);

        // Configure HttpClient to use virtual threads for async operations
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .executor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        // Combine input data with workflow settings for template interpolation
        Map<String, Object> templateData = new HashMap<>(input);
        if (context.getWorkflow().settings() != null) {
            templateData.putAll(context.getWorkflow().settings());
        }

        String url = interpolate((String) params.getOrDefault("url", ""), templateData, context);
        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG, "HTTP Request URL: " + url, Map.of());
        String method = (String) params.getOrDefault("method", "GET");
        Map<String, String> headers = (Map<String, String>) params.getOrDefault("headers", Map.of());
        String body = interpolate((String) params.getOrDefault("body", ""), templateData, context);
        int timeout = (int) params.getOrDefault("timeout", defaultTimeout);

        try {
            // Build request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            // Add headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), interpolate(header.getValue(), templateData, context));
            }

            // Add authentication if credential is specified
            if (node.credentialId() != null) {
                String credential = context.getDecryptedCredential(node.credentialId());
                requestBuilder.header("Authorization", "Bearer " + credential);
            }

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = body.isBlank()
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);

            requestBuilder.method(method.toUpperCase(), bodyPublisher);

            // Execute request
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG, "HTTP Response: status=" + response.statusCode() +
                            ", body_length=" + (response.body() != null ? response.body().length() : 0),
                    Map.of());

            if (response.body() != null && !response.body().isEmpty()) {
                String preview = response.body().length() > 200 ? response.body().substring(0, 200) + "..."
                        : response.body();
                context.getExecutionLogger().custom(context.getExecutionId().toString(),
                        ExecutionLogger.LogLevel.TRACE, "HTTP Response Body: " + preview, Map.of());
            }

            // Build output
            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.statusCode());
            output.put("body", response.body());
            output.put("headers", response.headers().map());
            output.put("success", response.statusCode() >= 200 && response.statusCode() < 300);

            // Add JSON response if detected
            addJsonResponseIfValid(output, response.body());

            return output;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeExecutionException("HTTP request interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new NodeExecutionException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Simple template interpolation for {{ variable }} syntax.
     * Supports workflow settings and credentials by name.
     */
    private String interpolate(String template, Map<String, Object> data, ExecutionService.ExecutionContext context) {
        if (template == null)
            return "";

        String result = template;

        // First pass: replace credential references like {{credentialName}}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            // Check if this is a credential name
            String credentialValue = context.getDecryptedCredentialByName(varName);
            if (credentialValue != null) {
                context.getExecutionLogger().custom(context.getExecutionId().toString(),
                        ExecutionLogger.LogLevel.DEBUG,
                        "Found credential '" + varName + "', value length: " + credentialValue.length(), Map.of());
                matcher.appendReplacement(sb, credentialValue);
            } else {
                // Check if this variable exists in the data map (input + settings)
                Object dataValue = getNestedValue(data, varName);
                if (dataValue != null) {
                    String value = dataValue.toString();
                    context.getExecutionLogger().custom(context.getExecutionId().toString(),
                            ExecutionLogger.LogLevel.DEBUG,
                            "Found variable '" + varName + "' in data map, value length: " + value.length(), Map.of());
                    matcher.appendReplacement(sb, value);
                } else {
                    context.getExecutionLogger().custom(context.getExecutionId().toString(),
                            ExecutionLogger.LogLevel.DEBUG,
                            "Variable '" + varName + "' not found in credentials or data map", Map.of());
                    // Throw an error for missing required variables instead of leaving template
                    // syntax
                    throw new NodeExecutionException(
                            "Variable '" + varName + "' not found. Create a credential with this name.");
                }
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Adds JSON response to output if the response body appears to be valid JSON.
     * Simple detection - in production, use proper JSON parsing library.
     */
    private void addJsonResponseIfValid(Map<String, Object> output, String responseBody) {
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            String trimmed = responseBody.trim();
            // Basic JSON detection: starts with { or [ and ends with } or ]
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                output.put("json", responseBody);
            }
        }
    }

    @Override
    public String getNodeType() {
        return "httpRequest";
    }
}
