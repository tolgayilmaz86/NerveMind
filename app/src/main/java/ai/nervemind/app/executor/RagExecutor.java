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
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "rag" node type - implements Retrieval-Augmented Generation.
 *
 * <p>
 * Combines document retrieval with Large Language Models to answer questions
 * based
 * on specific context. This node orchestrates the full RAG pipeline: embedding
 * generation
 * for the query, vector similarity search against documents, and LLM generation
 * with
 * retrieved context.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>RAG node configuration parameters</caption>
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
 * <td>LLM provider ("openai", "anthropic", "ollama")</td>
 * </tr>
 * <tr>
 * <td>chatModel</td>
 * <td>String</td>
 * <td>auto</td>
 * <td>Model for answer generation (e.g., "gpt-4o")</td>
 * </tr>
 * <tr>
 * <td>embeddingModel</td>
 * <td>String</td>
 * <td>auto</td>
 * <td>Model for embedding generation</td>
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
 * <td>Custom base URL for API</td>
 * </tr>
 * <tr>
 * <td>query</td>
 * <td>String</td>
 * <td>-</td>
 * <td>User's question/query (supports interpolation)</td>
 * </tr>
 * <tr>
 * <td>documents</td>
 * <td>List</td>
 * <td>[]</td>
 * <td>Documents to search (content + metadata)</td>
 * </tr>
 * <tr>
 * <td>topK</td>
 * <td>Integer</td>
 * <td>3</td>
 * <td>Number of documents to retrieve</td>
 * </tr>
 * <tr>
 * <td>includeContext</td>
 * <td>Boolean</td>
 * <td>true</td>
 * <td>Include retrieved text in output</td>
 * </tr>
 * </table>
 *
 * <h2>Pipeline Steps</h2>
 * <ol>
 * <li><strong>Embedding:</strong> Generates vector for the user
 * <code>query</code></li>
 * <li><strong>Indexing:</strong> Generates embeddings for
 * <code>documents</code> (if missing)</li>
 * <li><strong>Retrieval:</strong> Finds top K documents using cosine
 * similarity</li>
 * <li><strong>Generation:</strong> Sends query + retrieved context to LLM</li>
 * </ol>
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
 * <td>The LLM's generated answer</td>
 * </tr>
 * <tr>
 * <td>context</td>
 * <td>List</td>
 * <td>Retrieved documents (if includeContext=true)</td>
 * </tr>
 * <tr>
 * <td>sources</td>
 * <td>List</td>
 * <td>Metadata from retrieved documents</td>
 * </tr>
 * <tr>
 * <td>documentsRetrieved</td>
 * <td>Integer</td>
 * <td>Number of documents found</td>
 * </tr>
 * </table>
 *
 * @see EmbeddingExecutor For standalone embedding generation
 * @see LlmChatExecutor For direct chat without retrieval
 */
@Component
public class RagExecutor implements NodeExecutor {
    private static final Logger log = LoggerFactory.getLogger(RagExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final String DEFAULT_RAG_PROMPT = """
            You are a helpful assistant that answers questions based on the provided context.

            Use ONLY the information from the context below to answer the question.
            If the answer cannot be found in the context, say "I don't have enough information to answer that question."

            Context:
            {context}

            Please provide a clear and concise answer based on the context above.
            """;

    private final HttpClient httpClient;

    /**
     * Creates a new RAG executor with default HTTP client configuration.
     */
    public RagExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        RagParameters ragParams = extractParameters(node.parameters(), input);

        String validationError = validateParameters(ragParams);
        if (validationError != null) {
            return createErrorOutput(input, validationError);
        }

        try {
            return executeRagPipeline(ragParams, input);
        } catch (Exception e) {
            log.error("RAG execution failed: {}", e.getMessage(), e);
            return createErrorOutput(input, e.getMessage());
        }
    }

    private record RagParameters(
            String provider, String chatModel, String embeddingModel, String apiKey,
            String baseUrl, String query, String systemPrompt, int topK,
            double temperature, int maxTokens, int timeout, boolean includeContext,
            List<Map<String, Object>> documents) {
    }

    private RagParameters extractParameters(Map<String, Object> params, Map<String, Object> input) {
        String provider = interpolate((String) params.getOrDefault("provider", "openai"), input);
        String chatModel = interpolate((String) params.getOrDefault("chatModel", getChatModel(provider)), input);
        String embeddingModel = interpolate((String) params.getOrDefault("embeddingModel", getEmbeddingModel(provider)),
                input);
        String apiKey = interpolate((String) params.get("apiKey"), input);
        String baseUrl = interpolate((String) params.getOrDefault("baseUrl", getDefaultBaseUrl(provider)), input);
        String query = resolveQuery(params, input);
        String systemPrompt = interpolate((String) params.getOrDefault("systemPrompt", DEFAULT_RAG_PROMPT), input);
        int topK = ((Number) params.getOrDefault("topK", 3)).intValue();
        double temperature = ((Number) params.getOrDefault("temperature", 0.7)).doubleValue();
        int maxTokens = ((Number) params.getOrDefault("maxTokens", 1024)).intValue();
        int timeout = ((Number) params.getOrDefault("timeout", 120)).intValue();
        boolean includeContext = (Boolean) params.getOrDefault("includeContext", true);
        List<Map<String, Object>> documents = resolveDocuments(params, input);

        return new RagParameters(provider, chatModel, embeddingModel, apiKey, baseUrl, query,
                systemPrompt, topK, temperature, maxTokens, timeout, includeContext, documents);
    }

    private String resolveQuery(Map<String, Object> params, Map<String, Object> input) {
        String query = interpolate((String) params.get("query"), input);
        if (query == null || query.isBlank()) {
            query = (String) input.get("query");
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveDocuments(Map<String, Object> params, Map<String, Object> input) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) params.getOrDefault("documents", List.of());
        if (documents.isEmpty() && input.containsKey("documents")) {
            List<Map<String, Object>> inputDocs = (List<Map<String, Object>>) input.get("documents");
            documents = inputDocs != null ? inputDocs : List.of();
        }
        return documents;
    }

    private String validateParameters(RagParameters params) {
        if (params.query() == null || params.query().isBlank()) {
            return "Query is required";
        }
        if (params.documents().isEmpty()) {
            return "Documents are required for RAG";
        }
        return null;
    }

    private Map<String, Object> createErrorOutput(Map<String, Object> input, String error) {
        Map<String, Object> output = new HashMap<>(input);
        output.put("success", false);
        output.put("error", error);
        return output;
    }

    private Map<String, Object> executeRagPipeline(RagParameters params, Map<String, Object> input) throws Exception {
        List<Double> queryEmbedding = generateEmbedding(params.provider(), params.baseUrl(), params.apiKey(),
                params.embeddingModel(), params.query(), params.timeout());

        List<Map<String, Object>> documentsWithEmbeddings = ensureEmbeddings(
                params.documents(), params.provider(), params.baseUrl(), params.apiKey(),
                params.embeddingModel(), params.timeout());

        List<Map<String, Object>> retrievedDocs = retrieveTopK(queryEmbedding, documentsWithEmbeddings, params.topK());

        String contextText = buildContextText(retrievedDocs);
        String enhancedSystemPrompt = params.systemPrompt().replace("{context}", contextText);

        Map<String, Object> llmResult = callLlm(params.provider(), params.baseUrl(), params.apiKey(),
                params.chatModel(), enhancedSystemPrompt, params.query(),
                params.temperature(), params.maxTokens(), params.timeout());

        return buildSuccessOutput(input, params, llmResult, retrievedDocs);
    }

    private Map<String, Object> buildSuccessOutput(Map<String, Object> input, RagParameters params,
            Map<String, Object> llmResult, List<Map<String, Object>> retrievedDocs) {
        Map<String, Object> output = new HashMap<>(input);
        output.put("response", llmResult.get("response"));
        output.put("provider", params.provider());
        output.put("chatModel", params.chatModel());
        output.put("embeddingModel", params.embeddingModel());
        output.put("success", true);
        output.put("documentsRetrieved", retrievedDocs.size());

        if (params.includeContext()) {
            addContextToOutput(output, retrievedDocs);
        }

        if (llmResult.containsKey("usage")) {
            output.put("usage", llmResult.get("usage"));
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    private void addContextToOutput(Map<String, Object> output, List<Map<String, Object>> retrievedDocs) {
        output.put("context", retrievedDocs);

        List<Map<String, Object>> sources = new ArrayList<>();
        for (Map<String, Object> doc : retrievedDocs) {
            if (doc.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                sources.add(metadata);
            }
        }
        if (!sources.isEmpty()) {
            output.put("sources", sources);
        }
    }

    private String getChatModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "claude-3-5-sonnet-20241022";
            case "ollama" -> "llama3.2";
            default -> "gpt-4o";
        };
    }

    private String getEmbeddingModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> "nomic-embed-text";
            default -> "text-embedding-3-small";
        };
    }

    private String getDefaultBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "anthropic" -> "https://api.anthropic.com/v1";
            case "ollama" -> "http://localhost:11434/api";
            default -> "https://api.openai.com/v1";
        };
    }

    private List<Double> generateEmbedding(String provider, String baseUrl, String apiKey,
            String model, String text, int timeout) throws Exception {
        if ("ollama".equalsIgnoreCase(provider)) {
            return generateOllamaEmbedding(baseUrl, model, text, timeout);
        } else {
            return generateOpenAIEmbedding(baseUrl, apiKey, model, text, timeout);
        }
    }

    private List<Double> generateOpenAIEmbedding(String baseUrl, String apiKey, String model,
            String text, int timeout) throws Exception {
        String url = baseUrl + "/embeddings";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", text);

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
            throw new RuntimeException("OpenAI embedding error: " + response.statusCode());
        }

        Map<String, Object> parsed = fromJson(response.body());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) parsed.get("data");

        if (data != null && !data.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            return embedding;
        }

        throw new RuntimeException("No embedding returned");
    }

    private List<Double> generateOllamaEmbedding(String baseUrl, String model,
            String text, int timeout) throws Exception {
        String url = baseUrl + "/embeddings";

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
            throw new RuntimeException("Ollama embedding error: " + response.statusCode());
        }

        Map<String, Object> parsed = fromJson(response.body());
        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) parsed.get("embedding");

        return embedding;
    }

    private List<Map<String, Object>> ensureEmbeddings(List<Map<String, Object>> documents,
            String provider, String baseUrl,
            String apiKey, String model,
            int timeout) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            Map<String, Object> docCopy = new HashMap<>(doc);

            if (!doc.containsKey("embedding") || doc.get("embedding") == null) {
                String content = (String) doc.get("content");
                if (content != null && !content.isBlank()) {
                    List<Double> embedding = generateEmbedding(provider, baseUrl, apiKey,
                            model, content, timeout);
                    docCopy.put("embedding", embedding);
                }
            }

            result.add(docCopy);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> retrieveTopK(List<Double> queryEmbedding,
            List<Map<String, Object>> documents,
            int topK) {
        // Calculate similarity scores
        List<Map.Entry<Map<String, Object>, Double>> scored = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            List<Double> docEmbedding = (List<Double>) doc.get("embedding");
            if (docEmbedding != null) {
                double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                scored.add(Map.entry(doc, similarity));
            }
        }

        // Sort by similarity (descending)
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Return top K
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            Map<String, Object> doc = new HashMap<>(scored.get(i).getKey());
            doc.put("similarity", scored.get(i).getValue());
            // Remove embedding from output to save space
            doc.remove("embedding");
            result.add(doc);
        }

        return result;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String buildContextText(List<Map<String, Object>> documents) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Map<String, Object> doc = documents.get(i);
            sb.append("--- Document ").append(i + 1).append(" ---\n");
            sb.append(doc.get("content")).append("\n\n");
        }

        return sb.toString();
    }

    private Map<String, Object> callLlm(String provider, String baseUrl, String apiKey,
            String model, String systemPrompt, String userMessage,
            double temperature, int maxTokens, int timeout) throws Exception {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage));

        return switch (provider.toLowerCase()) {
            case "anthropic" -> callAnthropic(baseUrl, apiKey, model, messages, temperature, maxTokens, timeout);
            case "ollama" -> callOllama(baseUrl, model, messages, temperature, maxTokens, timeout);
            default -> callOpenAI(baseUrl, apiKey, model, messages, temperature, maxTokens, timeout);
        };
    }

    private Map<String, Object> callOpenAI(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
        String url = baseUrl + "/chat/completions";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

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
            throw new RuntimeException("OpenAI API error: " + response.statusCode());
        }

        return parseOpenAIResponse(response.body());
    }

    private Map<String, Object> callAnthropic(String baseUrl, String apiKey, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
        String url = baseUrl + "/messages";

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
            throw new RuntimeException("Anthropic API error: " + response.statusCode());
        }

        return parseAnthropicResponse(response.body());
    }

    private Map<String, Object> callOllama(String baseUrl, String model,
            List<Map<String, String>> messages, double temperature,
            int maxTokens, int timeout) throws Exception {
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
            throw new RuntimeException("Ollama API error: " + response.statusCode());
        }

        return parseOllamaResponse(response.body());
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
        }

        if (parsed.containsKey("usage")) {
            result.put("usage", parsed.get("usage"));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnthropicResponse(String json) {
        Map<String, Object> parsed = fromJson(json);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> content = (List<Map<String, Object>>) parsed.get("content");
        if (content != null && !content.isEmpty()) {
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type"))) {
                    result.put("response", block.get("text"));
                    break;
                }
            }
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String getNodeType() {
        return "rag";
    }
}
