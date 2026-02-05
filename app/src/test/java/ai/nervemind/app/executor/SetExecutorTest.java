package ai.nervemind.app.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for SetExecutor.
 * 
 * <p>
 * Tests data setting and transformation capabilities without
 * requiring Spring context or external services.
 * </p>
 */
class SetExecutorTest {

    private SetExecutor setExecutor;
    private ExecutionService.ExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        setExecutor = new SetExecutor();
        mockContext = mock(ExecutionService.ExecutionContext.class);
    }

    private Node createSetNode(Map<String, Object> values, boolean keepOnlySet) {
        Map<String, Object> params = new HashMap<>();
        params.put("values", values);
        params.put("keepOnlySet", keepOnlySet);

        return new Node(
                "set-1",
                "set",
                "Set Values",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    @Nested
    @DisplayName("Basic Value Setting")
    class BasicValueSetting {

        @Test
        @DisplayName("Should set static values")
        void shouldSetStaticValues() {
            Map<String, Object> values = Map.of(
                    "status", "active",
                    "count", 42);
            Node node = createSetNode(values, false);
            Map<String, Object> input = new HashMap<>();

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("status", "active")
                    .containsEntry("count", 42);
        }

        @Test
        @DisplayName("Should merge with existing input when keepOnlySet is false")
        void shouldMergeWithInputWhenNotKeepOnly() {
            Map<String, Object> values = Map.of("newKey", "newValue");
            Node node = createSetNode(values, false);
            Map<String, Object> input = new HashMap<>();
            input.put("existingKey", "existingValue");

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("existingKey", "existingValue")
                    .containsEntry("newKey", "newValue");
        }

        @Test
        @DisplayName("Should only keep set values when keepOnlySet is true")
        void shouldOnlyKeepSetValuesWhenTrue() {
            Map<String, Object> values = Map.of("onlyThis", "value");
            Node node = createSetNode(values, true);
            Map<String, Object> input = new HashMap<>();
            input.put("existingKey", "existingValue");

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("onlyThis", "value")
                    .doesNotContainKey("existingKey");
        }
    }

    @Nested
    @DisplayName("Expression References")
    class ExpressionReferences {

        @Test
        @DisplayName("Should resolve $input.field references")
        void shouldResolveInputFieldReferences() {
            Map<String, Object> values = new HashMap<>();
            values.put("userName", "$input.user");
            Node node = createSetNode(values, false);
            Map<String, Object> input = Map.of("user", "John Doe");

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("userName", "John Doe");
        }

        @Test
        @DisplayName("Should resolve nested $input.path references")
        void shouldResolveNestedReferences() {
            Map<String, Object> values = new HashMap<>();
            values.put("cityName", "$input.address.city");
            Node node = createSetNode(values, false);
            Map<String, Object> address = Map.of("city", "New York", "zip", "10001");
            Map<String, Object> input = Map.of("address", address);

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("cityName", "New York");
        }

        @Test
        @DisplayName("Should handle missing $input references gracefully")
        void shouldHandleMissingReferences() {
            Map<String, Object> values = new HashMap<>();
            values.put("missing", "$input.nonExistent");
            Node node = createSetNode(values, false);
            Map<String, Object> input = new HashMap<>();

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("missing", null);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty values map")
        void shouldHandleEmptyValues() {
            Node node = createSetNode(Map.of(), false);
            Map<String, Object> input = Map.of("existing", "data");

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("existing", "data");
        }

        @Test
        @DisplayName("Should override existing values")
        void shouldOverrideExistingValues() {
            Map<String, Object> values = Map.of("status", "updated");
            Node node = createSetNode(values, false);
            Map<String, Object> input = new HashMap<>();
            input.put("status", "original");

            Map<String, Object> result = setExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("status", "updated");
        }

        @Test
        @DisplayName("Should handle null input gracefully")
        void shouldHandleNullInput() {
            Map<String, Object> values = Map.of("key", "value");
            Node node = createSetNode(values, true);

            // With keepOnlySet=true, null input should work
            Map<String, Object> result = setExecutor.execute(node, new HashMap<>(), mockContext);

            assertThat(result).containsEntry("key", "value");
        }
    }
}
