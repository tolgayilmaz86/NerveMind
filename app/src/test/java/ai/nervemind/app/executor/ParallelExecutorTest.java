package ai.nervemind.app.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutorRegistry;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for ParallelExecutor.
 * 
 * <p>
 * Tests parallel branch execution with both fan-out mode (branches as number)
 * and inline mode (branches as list of operation definitions).
 * </p>
 */
@DisplayName("ParallelExecutor")
class ParallelExecutorTest {

    private ParallelExecutor parallelExecutor;
    private ExecutionService.ExecutionContext mockContext;
    private NodeExecutorRegistry mockRegistry;

    @BeforeEach
    void setUp() {
        mockRegistry = mock(NodeExecutorRegistry.class);
        parallelExecutor = new ParallelExecutor(mockRegistry);
        mockContext = new ExecutionService.ExecutionContext(
                1L,
                null,
                Map.of(),
                null,
                new ExecutionLogger());
    }

    private Node createParallelNode(Map<String, Object> params) {
        return new Node(
                "parallel-1",
                "parallel",
                "Parallel Test",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    @Nested
    @DisplayName("Node Type")
    class NodeType {

        @Test
        @DisplayName("should return 'parallel' as node type")
        void shouldReturnParallelNodeType() {
            assertThat(parallelExecutor.getNodeType()).isEqualTo("parallel");
        }
    }

    @Nested
    @DisplayName("Fan-out Mode")
    class FanOutMode {

        @Test
        @DisplayName("should handle branches as number (fan-out mode)")
        void shouldHandleBranchesAsNumber() {
            // branches as Number means fan-out mode - pass through input
            Map<String, Object> params = new HashMap<>();
            params.put("branches", 3);
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of("data", "test value");
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            // In fan-out mode, should include input data and metadata
            assertThat(result).containsKey("data");
            assertThat(result.get("data")).isEqualTo("test value");
        }

        @Test
        @DisplayName("should handle null branches (defaults to fan-out)")
        void shouldHandleNullBranches() {
            Map<String, Object> params = new HashMap<>();
            // No branches key at all
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of("value", 42);
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            // Should pass through
            assertThat(result).containsKey("value");
            assertThat(result.get("value")).isEqualTo(42);
        }

        @Test
        @DisplayName("should handle Integer branches value")
        void shouldHandleIntegerBranches() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", Integer.valueOf(5));
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of("key", "value");
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            // Should not throw ClassCastException
            assertThat(result).isNotNull();
            assertThat(result).containsKey("key");
        }

        @Test
        @DisplayName("should handle Long branches value")
        void shouldHandleLongBranches() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", Long.valueOf(3));
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of("test", true);
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle Double branches value")
        void shouldHandleDoubleBranches() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", 3.0);
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of();
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Inline Mode with Empty Branches")
    class InlineModeEmptyBranches {

        @Test
        @DisplayName("should handle empty branches list")
        void shouldHandleEmptyBranchesList() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", List.of());
            Node node = createParallelNode(params);

            Map<String, Object> input = Map.of("passthrough", "data");
            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            // Empty list should result in pass-through
            assertThat(result).containsKey("passthrough");
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("should return error for invalid branches type (string)")
        void shouldReturnErrorForStringBranches() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", "invalid");
            Node node = createParallelNode(params);

            Map<String, Object> result = parallelExecutor.execute(node, Map.of(), mockContext);

            assertThat(result).containsKey("error");
            assertThat(result.get("error").toString()).contains("Invalid branches configuration");
        }

        @Test
        @DisplayName("should return error for invalid branches type (boolean)")
        void shouldReturnErrorForBooleanBranches() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", true);
            Node node = createParallelNode(params);

            Map<String, Object> result = parallelExecutor.execute(node, Map.of(), mockContext);

            assertThat(result).containsKey("error");
        }
    }

    @Nested
    @DisplayName("Result Structure")
    class ResultStructure {

        @Test
        @DisplayName("should include branch execution metadata in fan-out mode")
        void shouldIncludeBranchMetadataFanOut() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", 2);
            Node node = createParallelNode(params);

            Map<String, Object> result = parallelExecutor.execute(node, Map.of("input", "data"), mockContext);

            // Fan-out mode returns input with metadata
            assertThat(result).isNotNull();
            // Should have preserved input
            assertThat(result).containsKey("input");
        }

        @Test
        @DisplayName("should preserve input data in result")
        void shouldPreserveInputData() {
            Map<String, Object> params = new HashMap<>();
            params.put("branches", 3);
            Node node = createParallelNode(params);

            Map<String, Object> input = new HashMap<>();
            input.put("originalKey", "originalValue");
            input.put("number", 42);

            Map<String, Object> result = parallelExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("originalKey", "originalValue");
            assertThat(result).containsEntry("number", 42);
        }
    }
}
