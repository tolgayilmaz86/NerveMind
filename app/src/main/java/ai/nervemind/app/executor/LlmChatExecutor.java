package ai.nervemind.app.executor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Executor for the "llmChat" node type - sends messages to LLM providers.
 *
 * <p>
 * Enables AI-powered workflows by interacting with various Large Language Model
 * providers. Supports multiple providers with a unified interface, template
 * interpolation
 * for dynamic prompts, and structured response handling.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>LLM Chat node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>provider</td>
 * <td>String</td>
 * <td>"openai"</td>
 * <td>"openai", "anthropic", "ollama", "azure", "google", "custom"</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>String</td>
 * <td>provider-specific</td>
 * <td>Model name (e.g., "gpt-4", "claude-3-opus")</td>
 * </tr>
 * <tr>
 * <td>apiKey</td>
 * <td>String</td>
 * <td>from settings</td>
 * <td>API key or ${credential.name} reference</td>
 * </tr>
 * <tr>
 * <td>baseUrl</td>
 * <td>String</td>
 * <td>provider default</td>
 * <td>Custom base URL for API</td>
 * </tr>
 * <tr>
 * <td>messages</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Array of {role, content} message objects</td>
 * </tr>
 * <tr>
 * <td>systemPrompt</td>
 * <td>String</td>
 * <td>-</td>
 * <td>System message prepended to conversation</td>
 * </tr>
 * <tr>
 * <td>prompt</td>
 * <td>String</td>
 * <td>-</td>
 * <td>Single user message (alternative to messages)</td>
 * </tr>
 * <tr>
 * <td>temperature</td>
 * <td>Double</td>
 * <td>0.7</td>
 * <td>Sampling temperature (0.0-2.0)</td>
 * </tr>
 * <tr>
 * <td>maxTokens</td>
 * <td>Integer</td>
 * <td>1024</td>
 * <td>Maximum tokens in response</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Integer</td>
 * <td>120</td>
 * <td>Request timeout in seconds</td>
 * </tr>
 * <tr>
 * <td>responseFormat</td>
 * <td>String</td>
 * <td>"text"</td>
 * <td>"text" or "json"</td>
 * </tr>
 * </table>
 *
 * <h2>Supported Providers</h2>
 * <ul>
 * <li><strong>openai</strong> - OpenAI GPT models (GPT-4, GPT-3.5-turbo)</li>
 * <li><strong>anthropic</strong> - Anthropic Claude models</li>
 * <li><strong>ollama</strong> - Local Ollama server (Llama, Mistral, etc.)</li>
 * <li><strong>azure</strong> - Azure OpenAI Service</li>
 * <li><strong>google</strong> - Google Gemini models</li>
 * <li><strong>custom</strong> - OpenAI-compatible custom endpoints</li>
 * </ul>
 *
 * <h2>Message Format</h2>
 * 
 * <pre>{@code
 * "messages": [
 *   { "role": "system", "content": "You are a helpful assistant." },
 *   { "role": "user", "content": "Hello!" },
 *   { "role": "assistant", "content": "Hi! How can I help?" },
 *   { "role": "user", "content": "What's 2+2?" }
 * ]
 * }</pre>
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
 * <td>response</td>
 * <td>String</td>
 * <td>The LLM's text response</td>
 * </tr>
 * <tr>
 * <td>usage</td>
 * <td>Map</td>
 * <td>Token usage info (promptTokens, completionTokens, totalTokens)</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>String</td>
 * <td>Model that was used</td>
 * </tr>
 * <tr>
 * <td>finishReason</td>
 * <td>String</td>
 * <td>Why response ended (stop, length, etc.)</td>
 * </tr>
 * </table>
 *
 * <h2>API Key Resolution</h2>
 * <p>
 * API keys are resolved in this order:
 * </p>
 * <ol>
 * <li>Explicit {@code apiKey} parameter</li>
 * <li>Credential reference: ${credential.OPENAI_API_KEY}</li>
 * <li>Settings service: stored API keys per provider</li>
 * </ol>
 *
 * @see EmbeddingExecutor For generating vector embeddings
 * @see RagExecutor For retrieval-augmented generation
 * @see TextClassifierExecutor For LLM-based classification
 */
@Component
public class LlmChatExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(LlmChatExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final Map<String, String> DEFAULT_BASE_URLS = Map.of(
            "openai", "https://api.openai.com/v1",
            "anthropic", "https://api.anthropic.com/v1",
            "ollama", "http://localhost:11434/api",
            "azure", "", // Requires custom configuration
            "google", "https://generativelanguage.googleapis.com/v1beta");

    private final HttpClient httpClient;
    private final SettingsServiceInterface settingsService;

    /**
     * Creates a new LLM chat executor with configured HTTP client.
     *
     * @param settingsService the settings service for configuration access
     */
    public LlmChatExecutor(SettingsServiceInterface settingsService) {
        this.settingsService = settingsService;
        int connectTimeout = settingsService.getInt(SettingsDefaults.HTTP_CONNECT_TIMEOUT, 30);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input, context);
        String model = interpolate((String) params.getOrDefault("model", getDefaultModel(provider)), input, context);
        String apiKey = interpolate((String) params.get("apiKey"), input, context);
        String baseUrl = interpolate(
                (String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input, context);

        // Fall back to settings for API key if not specified in node params
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = getApiKeyFromSettings(provider);
        }
        String systemPrompt = interpolate((String) params.get("systemPrompt"), input, context);
        String prompt = interpolate((String) params.get("prompt"), input, context);
        double temperature = ((Number) params.getOrDefault("temperature", 0.7)).doubleValue();
        int maxTokens = ((Number) params.getOrDefault("maxTokens", 1024)).intValue();
        int timeout = ((Number) params.getOrDefault("timeout", 120)).intValue();
        String responseFormat = (String) params.getOrDefault("responseFormat", "text");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) params.get("messages");

        try {
            // Build messages list
            List<Map<String, String>> chatMessages = buildMessages(systemPrompt, prompt, messages, input, context);

            // Call the appropriate provider
            Map<String, Object> result = switch (provider.toLowerCase()) {
                case "openai" -> callOpenAI(new LlmRequest(
                        baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout, responseFormat));
                case "anthropic" ->
                    callAnthropic(baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout);
                case "ollama" -> callOllama(baseUrl, model, chatMessages, temperature, maxTokens, timeout);
                case "azure" -> callAzure(new LlmRequest(
                        baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout, responseFormat));
                case "google", "gemini" ->
                    callGemini(baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout);
                default -> callOpenAI(new LlmRequest(
                        baseUrl, apiKey, model, chatMessages, temperature, maxTokens, timeout, responseFormat));
            };

            Map<String, Object> output = new HashMap<>(input);
            output.putAll(result);
            output.put("provider", provider);
            output.put("model", model);
            output.put("success", true);

            return output;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("LLM chat interrupted", e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Execution interrupted");
            output.put("provider", provider);
            output.put("model", model);
            return output;
        } catch (Exception e) {
            log.error("LLM chat failed: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            output.put("provider", provider);
            output.put("model", model);
            return output;
        }
    }

    private String getDefaultModel(String provider) {
        // Try to get model from settings first
        String settingsModel = switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_DEFAULT_MODEL, null);
            case "ollama" -> settingsService.getValue(SettingsDefaults.AI_OLLAMA_DEFAULT_MODEL, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_DEPLOYMENT, null);
            case "google", "gemini" -> settingsService.getValue(SettingsDefaults.AI_GOOGLE_DEFAULT_MODEL, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_DEFAULT_MODEL, null);
        };

        if (settingsModel != null && !settingsModel.isBlank()) {
            return settingsModel;
        }

        // Fall back to hardcoded defaults
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "ollama" -> "llama3.2";
            case "azure" -> "gpt-4";
            case "google", "gemini" -> "gemini-1.5-pro";
            default -> "gpt-4o";
        };
    }

    /**
     * Gets the API key from settings based on provider.
     */
    private String getApiKeyFromSettings(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_API_KEY, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_API_KEY, null);
            case "google", "gemini" -> settingsService.getValue(SettingsDefaults.AI_GOOGLE_API_KEY, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_API_KEY, null);
        };
    }

    /**
     * Gets the default base URL for a provider, checking settings first.
     */
    private String getDefaultBaseUrl(String provider) {
        String settingsUrl = switch (provider.toLowerCase()) {
            case "anthropic" -> settingsService.getValue(SettingsDefaults.AI_ANTHROPIC_BASE_URL, null);
            case "ollama" -> settingsService.getValue(SettingsDefaults.AI_OLLAMA_BASE_URL, null);
            case "azure" -> settingsService.getValue(SettingsDefaults.AI_AZURE_ENDPOINT, null);
            case "google", "gemini" -> settingsService.getValue(SettingsDefaults.AI_GOOGLE_BASE_URL, null);
            default -> settingsService.getValue(SettingsDefaults.AI_OPENAI_BASE_URL, null);
        };

        if (settingsUrl != null && !settingsUrl.isBlank()) {
            return settingsUrl;
        }

        return DEFAULT_BASE_URLS.getOrDefault(provider.toLowerCase(), "");
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String prompt,
            List<Map<String, Object>> messages,
            Map<String, Object> input, ExecutionService.ExecutionContext context) {
        List<Map<String, String>> chatMessages = new ArrayList<>();

        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            chatMessages.add(Map.of("role", "system", "content", systemPrompt));
        }

        // Add messages array if provided
        if (messages != null && !messages.isEmpty()) {
            for (Map<String, Object> msg : messages) {
                String role = (String) msg.getOrDefault("role", "user");
                String content = interpolate((String) msg.get("content"), input, context);
                if (content != null) {
                    chatMessages.add(Map.of("role", role, "content", content));
                }
            }
        }

        // Add single prompt if provided (and no messages)
        if (prompt != null && !prompt.isBlank() && (messages == null || messages.isEmpty())) {
            chatMessages.add(Map.of("role", "user", "content", prompt));
        }

        return chatMessages;
    }

    private record LlmRequest(
            String baseUrl,
            String apiKey,
            String model,
            List<Map<String, String>> messages,
            double temperature,
            int maxTokens,
            int timeout,
            String responseFormat) {
    }

    private static class LlmProviderException extends RuntimeException {
        public LlmProviderException(String message) {
            super(message);
        }
    }

    private Map<String, Object> callOpenAI(LlmRequest request) throws java.io.IOException, InterruptedException {
        String url = request.baseUrl() + "/chat/completions";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", request.model());
        requestBody.put("messages", request.messages());
        requestBody.put("temperature", request.temperature());
        requestBody.put("max_tokens", request.maxTokens());

        if ("json".equals(request.responseFormat())) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        String jsonBody = toJson(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + request.apiKey())
                .timeout(Duration.ofSeconds(request.timeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new LlmProviderException(
                    "OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOpenAIResponse(response.body());
    }

    private Map<String, Object> callAnthropic(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws java.io.IOException, InterruptedException {
        String url = baseUrl + "/messages";

        // Anthropic requires system message separately
        String systemMessage = null;
        List<Map<String, String>> userMessages = new ArrayList<>();

        for (Map<String, String> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemMessage = msg.get("content");
            } else {
                userMessages.add(msg);
            }
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", userMessages);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        if (systemMessage != null) {
            requestBody.put("system", systemMessage);
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new LlmProviderException(
                    "Anthropic API error: " + response.statusCode() + " - " + response.body());
        }

        return parseAnthropicResponse(response.body());
    }

    private Map<String, Object> callOllama(String baseUrl, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws java.io.IOException, InterruptedException {
        String url = baseUrl + "/chat";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of(
                "temperature", temperature,
                "num_predict", maxTokens));

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new LlmProviderException("Ollama API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOllamaResponse(response.body());
    }

    private Map<String, Object> callAzure(LlmRequest request) throws java.io.IOException, InterruptedException {
        // Azure OpenAI uses a different URL pattern
        String url = request.baseUrl() + "/openai/deployments/" + request.model()
                + "/chat/completions?api-version=2024-02-15-preview";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("messages", request.messages());
        requestBody.put("temperature", request.temperature());
        requestBody.put("max_tokens", request.maxTokens());

        if ("json".equals(request.responseFormat())) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        String jsonBody = toJson(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("api-key", request.apiKey())
                .timeout(Duration.ofSeconds(request.timeout()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new LlmProviderException(
                    "Azure OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOpenAIResponse(response.body());
    }

    /**
     * Call Google Gemini API.
     * Gemini uses a different message format than OpenAI:
     * - Contents array with parts
     * - System instruction is separate
     * - API key passed via query parameter or header
     */
    private Map<String, Object> callGemini(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws java.io.IOException, InterruptedException {

        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        // Convert messages to Gemini format
        // Gemini uses: {"contents": [{"role": "user", "parts": [{"text": "..."}]}]}
        String systemInstruction = null;
        List<Map<String, Object>> contents = new ArrayList<>();

        for (Map<String, String> msg : messages) {
            String role = msg.get("role");
            String content = msg.get("content");

            if ("system".equals(role)) {
                // System messages become systemInstruction
                systemInstruction = content;
            } else {
                // Convert role: "assistant" -> "model" for Gemini
                String geminiRole = "assistant".equals(role) ? "model" : "user";
                Map<String, Object> geminiMessage = new LinkedHashMap<>();
                geminiMessage.put("role", geminiRole);
                geminiMessage.put("parts", List.of(Map.of("text", content)));
                contents.add(geminiMessage);
            }
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", contents);

        // Add system instruction if present
        if (systemInstruction != null) {
            requestBody.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))));
        }

        // Generation config
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        requestBody.put("generationConfig", generationConfig);

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new LlmProviderException(
                    "Google Gemini API error: " + response.statusCode() + " - " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGeminiResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        // Gemini response format:
        // {"candidates": [{"content": {"parts": [{"text": "..."}]}, "finishReason":
        // "STOP"}]}
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) parsed.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");

            if (content != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    StringBuilder responseText = new StringBuilder();
                    for (Map<String, Object> part : parts) {
                        Object text = part.get("text");
                        if (text != null) {
                            responseText.append(text);
                        }
                    }
                    result.put("response", responseText.toString());
                }
            }

            // Map Gemini finish reason to standard format
            String finishReason = (String) candidate.get("finishReason");
            result.put("finishReason", finishReason != null ? finishReason.toLowerCase() : "unknown");
        }

        // Parse usage metadata
        Map<String, Object> usageMetadata = (Map<String, Object>) parsed.get("usageMetadata");
        if (usageMetadata != null) {
            int promptTokens = ((Number) usageMetadata.getOrDefault("promptTokenCount", 0)).intValue();
            int completionTokens = ((Number) usageMetadata.getOrDefault("candidatesTokenCount", 0)).intValue();
            result.put("usage", Map.of(
                    "promptTokens", promptTokens,
                    "completionTokens", completionTokens,
                    "totalTokens", promptTokens + completionTokens));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");

            result.put("response", message.get("content"));
            result.put("finishReason", choice.get("finish_reason"));
        }

        Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
        if (usage != null) {
            result.put("usage", Map.of(
                    "promptTokens", usage.get("prompt_tokens"),
                    "completionTokens", usage.get("completion_tokens"),
                    "totalTokens", usage.get("total_tokens")));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
        if (content != null && !content.isEmpty()) {
            StringBuilder responseText = new StringBuilder();
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type"))) {
                    responseText.append(block.get("text"));
                }
            }
            result.put("response", responseText.toString());
        }

        result.put("finishReason", parsed.get("stop_reason"));

        Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
        if (usage != null) {
            result.put("usage", Map.of(
                    "promptTokens", usage.get("input_tokens"),
                    "completionTokens", usage.get("output_tokens"),
                    "totalTokens", ((Number) usage.get("input_tokens")).intValue() +
                            ((Number) usage.get("output_tokens")).intValue()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOllamaResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> message = (Map<String, Object>) parsed.get("message");
        if (message != null) {
            result.put("response", message.get("content"));
        }

        result.put("finishReason", parsed.get("done") != null && (Boolean) parsed.get("done") ? "stop" : "unknown");

        // Ollama provides different usage metrics
        if (parsed.containsKey("eval_count")) {
            result.put("usage", Map.of(
                    "promptTokens", parsed.getOrDefault("prompt_eval_count", 0),
                    "completionTokens", parsed.getOrDefault("eval_count", 0),
                    "totalTokens", ((Number) parsed.getOrDefault("prompt_eval_count", 0)).intValue() +
                            ((Number) parsed.getOrDefault("eval_count", 0)).intValue()));
        }

        return result;
    }

    private String interpolate(String text, Map<String, Object> data, ExecutionService.ExecutionContext context) {
        if (text == null) {
            return null;
        }

        Matcher matcher = INTERPOLATION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);

            // Check for credential reference first
            if (path.startsWith("credential.") && context != null) {
                String credentialName = path.substring("credential.".length());
                String credentialValue = context.getDecryptedCredentialByName(credentialName);
                if (credentialValue != null) {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(credentialValue));
                    continue;
                }
            }

            Object value = getNestedValue(data, path);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
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

    // Simple JSON serialization (production should use Jackson)
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first)
                sb.append(",");
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }

        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append(valueToJson(item));
            }
            sb.append("]");
            return sb.toString();
        } else if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typedMap = (Map<String, Object>) map;
            return toJson(typedMap);
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Simple JSON parsing (production should use Jackson)
    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        // Use a simple approach - delegate to the Jackson ObjectMapper if available
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new LlmProviderException("Failed to parse JSON: " + e.getMessage());
        }
    }

    @Override
    public String getNodeType() {
        return "llmChat";
    }
}
