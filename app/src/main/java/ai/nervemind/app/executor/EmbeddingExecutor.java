package ai.nervemind.app.executor;

import java.io.IOException;
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
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "embedding" node type - generates vector embeddings for
 * text.
 *
 * <p>
 * Creates dense vector representations of text using embedding models from
 * various
 * providers. Essential for semantic search, similarity matching, and building
 * RAG
 * (Retrieval-Augmented Generation) pipelines.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Embedding node configuration parameters</caption>
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
 * <td>"openai", "ollama", "azure", "cohere"</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>String</td>
 * <td>provider-specific</td>
 * <td>Embedding model name</td>
 * </tr>
 * <tr>
 * <td>apiKey</td>
 * <td>String</td>
 * <td>from settings</td>
 * <td>API key for the provider</td>
 * </tr>
 * <tr>
 * <td>baseUrl</td>
 * <td>String</td>
 * <td>provider default</td>
 * <td>Custom base URL</td>
 * </tr>
 * <tr>
 * <td>input</td>
 * <td>String/List</td>
 * <td>-</td>
 * <td>Text(s) to embed (supports ${interpolation})</td>
 * </tr>
 * <tr>
 * <td>dimensions</td>
 * <td>Integer</td>
 * <td>model default</td>
 * <td>Output dimensions (for dimension reduction)</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Integer</td>
 * <td>60</td>
 * <td>Request timeout in seconds</td>
 * </tr>
 * </table>
 *
 * <h2>Supported Models</h2>
 * <ul>
 * <li><strong>OpenAI:</strong> text-embedding-3-small, text-embedding-3-large,
 * text-embedding-ada-002</li>
 * <li><strong>Ollama:</strong> nomic-embed-text, mxbai-embed-large,
 * all-minilm</li>
 * <li><strong>Cohere:</strong> embed-english-v3.0, embed-multilingual-v3.0</li>
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
 * <td>embedding</td>
 * <td>List&lt;Double&gt;</td>
 * <td>Vector embedding (or list of vectors for batch)</td>
 * </tr>
 * <tr>
 * <td>dimensions</td>
 * <td>Integer</td>
 * <td>Number of dimensions in the vector</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>String</td>
 * <td>Model that was used</td>
 * </tr>
 * <tr>
 * <td>usage</td>
 * <td>Map</td>
 * <td>Token usage information</td>
 * </tr>
 * </table>
 *
 * <h2>Batch Processing</h2>
 * <p>
 * When {@code input} is a list of strings, embeddings are generated for each
 * item in a single API call (if supported by provider), improving efficiency.
 * </p>
 *
 * @see RagExecutor For using embeddings in retrieval-augmented generation
 * @see LlmChatExecutor For chat completions using LLMs
 */
@Component
public class EmbeddingExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final HttpClient httpClient;

    /**
     * Default constructor for EmbeddingExecutor.
     * Initializes the HttpClient with a 30-second connection timeout.
     */
    public EmbeddingExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input);
        String model = interpolate((String) params.getOrDefault("model", getDefaultModel(provider)), input);
        String apiKey = interpolate((String) params.get("apiKey"), input);
        String baseUrl = interpolate((String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input);
        int timeout = ((Number) params.getOrDefault("timeout", 60)).intValue();
        Integer dimensions = params.containsKey("dimensions") ? ((Number) params.get("dimensions")).intValue() : null;

        // Get input text(s)
        Object inputText = params.get("input");
        List<String> texts = new ArrayList<>();

        if (inputText instanceof String s) {
            texts.add(interpolate(s, input));
        } else if (inputText instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    texts.add(interpolate(s, input));
                }
            }
        }

        if (texts.isEmpty()) {
            // Try to get from input data
            Object dataInput = input.get("text");
            if (dataInput instanceof String s) {
                texts.add(s);
            }
        }

        if (texts.isEmpty()) {
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "No input text provided for embedding");
            return output;
        }

        try {
            Map<String, Object> result = switch (provider.toLowerCase()) {
                case "ollama" -> callOllama(baseUrl, model, texts, timeout);
                case "cohere" -> callCohere(baseUrl, apiKey, model, texts, timeout);
                default -> callOpenAI(baseUrl, apiKey, model, texts, dimensions, timeout);
            };

            Map<String, Object> output = new HashMap<>(input);
            output.putAll(result);
            output.put("provider", provider);
            output.put("model", model);
            output.put("success", true);
            output.put("inputCount", texts.size());

            return output;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Embedding generation interrupted: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", "Embedding generation interrupted");
            return output;
        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage(), e);
            Map<String, Object> output = new HashMap<>(input);
            output.put("success", false);
            output.put("error", e.getMessage());
            return output;
        }
    }

    private String getDefaultModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> "nomic-embed-text";
            case "cohere" -> "embed-english-v3.0";
            default -> "text-embedding-3-small";
        };
    }

    private String getDefaultBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> "http://localhost:11434/api";
            case "cohere" -> "https://api.cohere.ai/v1";
            default -> "https://api.openai.com/v1";
        };
    }

    private Map<String, Object> callOpenAI(String baseUrl, String apiKey, String model,
            List<String> texts, Integer dimensions,
            int timeout) throws IOException, InterruptedException {
        String url = baseUrl + "/embeddings";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", texts.size() == 1 ? texts.get(0) : texts);

        if (dimensions != null) {
            requestBody.put("dimensions", dimensions);
        }

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        return parseOpenAIResponse(response.body());
    }

    private Map<String, Object> callOllama(String baseUrl, String model,
            List<String> texts, int timeout) throws IOException, InterruptedException {
        // Ollama embeddings endpoint
        String url = baseUrl + "/embeddings";

        List<List<Double>> allEmbeddings = new ArrayList<>();

        // Ollama typically processes one at a time
        for (String text : texts) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", text);

            String jsonBody = toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Ollama API error: " + response.statusCode() + " - " + response.body());
            }

            Map<String, Object> parsed = fromJson(response.body());
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) parsed.get("embedding");
            allEmbeddings.add(embedding);
        }

        Map<String, Object> result = new HashMap<>();
        if (allEmbeddings.size() == 1) {
            result.put("embedding", allEmbeddings.get(0));
            result.put("dimensions", allEmbeddings.get(0).size());
        } else {
            result.put("embeddings", allEmbeddings);
            result.put("dimensions", allEmbeddings.get(0).size());
        }

        return result;
    }

    private Map<String, Object> callCohere(String baseUrl, String apiKey, String model,
            List<String> texts, int timeout) throws IOException, InterruptedException {
        String url = baseUrl + "/embed";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("texts", texts);
        requestBody.put("input_type", "search_document"); // or "search_query" for queries

        String jsonBody = toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Cohere API error: " + response.statusCode() + " - " + response.body());
        }

        return parseCohereResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOpenAIResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.get("data");
        if (data != null && !data.isEmpty()) {
            if (data.size() == 1) {
                List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                result.put("embedding", embedding);
                result.put("dimensions", embedding.size());
            } else {
                List<List<Double>> embeddings = new ArrayList<>();
                for (Map<String, Object> item : data) {
                    embeddings.add((List<Double>) item.get("embedding"));
                }
                result.put("embeddings", embeddings);
                result.put("dimensions", embeddings.get(0).size());
            }
        }

        Map<String, Object> usage = (Map<String, Object>) parsed.get("usage");
        if (usage != null) {
            result.put("usage", Map.of(
                    "promptTokens", usage.get("prompt_tokens"),
                    "totalTokens", usage.get("total_tokens")));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCohereResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<List<Double>> embeddings = (List<List<Double>>) parsed.get("embeddings");
        if (embeddings != null && !embeddings.isEmpty()) {
            if (embeddings.size() == 1) {
                result.put("embedding", embeddings.get(0));
            } else {
                result.put("embeddings", embeddings);
            }
            result.put("dimensions", embeddings.get(0).size());
        }

        return result;
    }

    private String interpolate(String text, Map<String, Object> data) {
        if (text == null)
            return null;

        Matcher matcher = INTERPOLATION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getNestedValue(data, path);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty())
            return null;

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }
            if (current == null)
                return null;
        }

        return current;
    }

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
        if (value == null)
            return "null";
        if (value instanceof String s)
            return "\"" + escapeJson(s) + "\"";
        if (value instanceof Number || value instanceof Boolean)
            return value.toString();
        if (value instanceof List<?> list) {
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
        }
        if (value instanceof Map<?, ?> map) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    /**
     * @return the node type identifier ("embedding")
     */
    @Override
    public String getNodeType() {
        return "embedding";
    }
}
