package ai.nervemind.app.executor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.service.SettingsServiceInterface;

/**
 * Unit tests for HttpRequestExecutor with mocked HTTP responses using WireMock.
 * 
 * <p>
 * Tests HTTP operations (GET, POST, PUT, DELETE) against a mock server
 * to ensure correct request building and response handling.
 * </p>
 */
class HttpRequestExecutorTest {

    private WireMockServer wireMockServer;
    private HttpRequestExecutor httpRequestExecutor;
    private ExecutionService.ExecutionContext mockContext;
    private ExecutionLogger mockLogger;
    private WorkflowDTO mockWorkflow;

    @BeforeEach
    void setUp() {
        // Start WireMock server on a random port
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Create mock settings service
        SettingsServiceInterface mockSettings = mock(SettingsServiceInterface.class);
        when(mockSettings.getInt(anyString(), any(Integer.class))).thenAnswer(invocation -> invocation.getArgument(1));

        httpRequestExecutor = new HttpRequestExecutor(mockSettings);

        // Setup mock context
        mockContext = mock(ExecutionService.ExecutionContext.class);
        mockLogger = mock(ExecutionLogger.class);
        mockWorkflow = mock(WorkflowDTO.class);

        when(mockContext.getExecutionId()).thenReturn(1L);
        when(mockContext.getExecutionLogger()).thenReturn(mockLogger);
        when(mockContext.getWorkflow()).thenReturn(mockWorkflow);
        when(mockWorkflow.settings()).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private Node createHttpNode(String url, String method) {
        return createHttpNode(url, method, null, null);
    }

    private Node createHttpNode(String url, String method, Map<String, String> headers, String body) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", url);
        params.put("method", method);
        if (headers != null) {
            params.put("headers", headers);
        }
        if (body != null) {
            params.put("body", body);
        }
        params.put("timeout", 10);

        return new Node(
                "http-1",
                "httpRequest",
                "HTTP Request",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    @Nested
    @DisplayName("GET Requests")
    class GetRequests {

        @Test
        @DisplayName("Should execute GET request successfully")
        void shouldExecuteGetRequest() {
            // Setup mock response
            wireMockServer.stubFor(get(urlEqualTo("/api/data"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"Hello World\"}")));

            String url = wireMockServer.baseUrl() + "/api/data";
            Node node = createHttpNode(url, "GET");
            Map<String, Object> input = new HashMap<>();

            Map<String, Object> result = httpRequestExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 200)
                    .containsEntry("success", true)
                    .containsKey("json");
            assertThat(result.get("body")).asString().contains("Hello World");
        }

        @Test
        @DisplayName("Should handle 404 response")
        void shouldHandle404Response() {
            wireMockServer.stubFor(get(urlEqualTo("/api/notfound"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withBody("Not Found")));

            String url = wireMockServer.baseUrl() + "/api/notfound";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 404)
                    .containsEntry("success", false);
        }

        @Test
        @DisplayName("Should handle 500 server error")
        void shouldHandle500Error() {
            wireMockServer.stubFor(get(urlEqualTo("/api/error"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            String url = wireMockServer.baseUrl() + "/api/error";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 500)
                    .containsEntry("success", false);
        }
    }

    @Nested
    @DisplayName("POST Requests")
    class PostRequests {

        @Test
        @DisplayName("Should execute POST with JSON body")
        void shouldExecutePostWithJsonBody() {
            wireMockServer.stubFor(post(urlEqualTo("/api/create"))
                    .willReturn(aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\": 123, \"created\": true}")));

            String url = wireMockServer.baseUrl() + "/api/create";
            Map<String, String> headers = Map.of("Content-Type", "application/json");
            String body = "{\"name\": \"Test Item\"}";
            Node node = createHttpNode(url, "POST", headers, body);

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 201)
                    .containsEntry("success", true);

            wireMockServer.verify(postRequestedFor(urlEqualTo("/api/create"))
                    .withHeader("Content-Type", equalTo("application/json")));
        }

        @Test
        @DisplayName("Should handle POST response with array")
        void shouldHandleArrayResponse() {
            wireMockServer.stubFor(post(urlEqualTo("/api/items"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("[{\"id\": 1}, {\"id\": 2}]")));

            String url = wireMockServer.baseUrl() + "/api/items";
            Node node = createHttpNode(url, "POST", null, "{}");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsKey("json");
            assertThat(result.get("json")).asString().startsWith("[");
        }
    }

    @Nested
    @DisplayName("Other HTTP Methods")
    class OtherMethods {

        @Test
        @DisplayName("Should execute PUT request")
        void shouldExecutePutRequest() {
            wireMockServer.stubFor(put(urlEqualTo("/api/update/1"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"updated\": true}")));

            String url = wireMockServer.baseUrl() + "/api/update/1";
            Node node = createHttpNode(url, "PUT", null, "{\"status\": \"active\"}");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 200)
                    .containsEntry("success", true);
        }

        @Test
        @DisplayName("Should execute DELETE request")
        void shouldExecuteDeleteRequest() {
            wireMockServer.stubFor(delete(urlEqualTo("/api/delete/1"))
                    .willReturn(aResponse()
                            .withStatus(204)));

            String url = wireMockServer.baseUrl() + "/api/delete/1";
            Node node = createHttpNode(url, "DELETE");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result)
                    .containsEntry("statusCode", 204)
                    .containsEntry("success", true);
        }
    }

    @Nested
    @DisplayName("Response Headers")
    class ResponseHeaders {

        @Test
        @DisplayName("Should capture response headers")
        void shouldCaptureResponseHeaders() {
            wireMockServer.stubFor(get(urlEqualTo("/api/headers"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("X-Custom-Header", "custom-value")
                            .withHeader("Content-Type", "text/plain")
                            .withBody("OK")));

            String url = wireMockServer.baseUrl() + "/api/headers";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsKey("headers");
            @SuppressWarnings("unchecked")
            Map<String, ?> headers = (Map<String, ?>) result.get("headers");
            assertThat(headers).containsKey("X-Custom-Header");
        }
    }

    @Nested
    @DisplayName("JSON Detection")
    class JsonDetection {

        @Test
        @DisplayName("Should detect JSON object response")
        void shouldDetectJsonObject() {
            wireMockServer.stubFor(get(urlEqualTo("/api/json"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"key\": \"value\"}")));

            String url = wireMockServer.baseUrl() + "/api/json";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsKey("json");
        }

        @Test
        @DisplayName("Should detect JSON array response")
        void shouldDetectJsonArray() {
            wireMockServer.stubFor(get(urlEqualTo("/api/array"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("[1, 2, 3]")));

            String url = wireMockServer.baseUrl() + "/api/array";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsKey("json");
        }

        @Test
        @DisplayName("Should not flag plain text as JSON")
        void shouldNotFlagPlainTextAsJson() {
            wireMockServer.stubFor(get(urlEqualTo("/api/text"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("Hello World")));

            String url = wireMockServer.baseUrl() + "/api/text";
            Node node = createHttpNode(url, "GET");

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).doesNotContainKey("json");
        }
    }

    @Nested
    @DisplayName("Default Method")
    class DefaultMethod {

        @Test
        @DisplayName("Should default to GET when method not specified")
        void shouldDefaultToGet() {
            wireMockServer.stubFor(get(urlEqualTo("/api/default"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("OK")));

            String url = wireMockServer.baseUrl() + "/api/default";

            Map<String, Object> params = new HashMap<>();
            params.put("url", url);
            // No method specified

            Node node = new Node(
                    "http-1",
                    "httpRequest",
                    "HTTP Request",
                    new Node.Position(100.0, 100.0),
                    params,
                    null,
                    false,
                    null);

            Map<String, Object> result = httpRequestExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsEntry("statusCode", 200);
            wireMockServer.verify(getRequestedFor(urlEqualTo("/api/default")));
        }
    }
}
