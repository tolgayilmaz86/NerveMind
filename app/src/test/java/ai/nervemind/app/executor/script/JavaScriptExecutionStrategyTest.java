package ai.nervemind.app.executor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for JavaScript execution strategy.
 */
@DisplayName("JavaScriptExecutionStrategy")
class JavaScriptExecutionStrategyTest {

    private JavaScriptExecutionStrategy strategy;
    private ExecutionService.ExecutionContext context;

    @BeforeEach
    void setUp() {
        strategy = new JavaScriptExecutionStrategy();
        context = createMockContext();
    }

    @Nested
    @DisplayName("Basic Execution")
    class BasicExecution {

        @Test
        @DisplayName("should return input unchanged for blank code")
        void blankCode() {
            Map<String, Object> input = Map.of("key", "value");
            Node node = createNode("");

            Map<String, Object> result = strategy.execute("", input, node, context);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("should execute simple return statement")
        void simpleReturn() {
            Map<String, Object> input = new HashMap<>();
            Node node = createNode("return 42;");

            Map<String, Object> result = strategy.execute("return 42;", input, node, context);

            assertEquals(42L, result.get("result"));
        }

        @Test
        @DisplayName("should execute and return object")
        void returnObject() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { name: 'test', count: 5 };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("test", result.get("name"));
            assertEquals(5L, result.get("count"));
        }

        @Test
        @DisplayName("should preserve input data in output")
        void preserveInput() {
            Map<String, Object> input = Map.of("existing", "data");
            String code = "return { newKey: 'newValue' };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("data", result.get("existing"));
            assertEquals("newValue", result.get("newKey"));
        }
    }

    @Nested
    @DisplayName("Input Bindings")
    class InputBindings {

        @Test
        @DisplayName("should access input via $input")
        void accessInputWithDollar() {
            Map<String, Object> input = Map.of("value", 10);
            String code = "return { doubled: $input.value * 2 };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(20L, result.get("doubled"));
        }

        @Test
        @DisplayName("should access input via input alias")
        void accessInputWithoutDollar() {
            Map<String, Object> input = Map.of("value", 10);
            String code = "return { doubled: input.value * 2 };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(20L, result.get("doubled"));
        }

        @Test
        @DisplayName("should access nested input data")
        void accessNestedInput() {
            Map<String, Object> nested = new HashMap<>();
            nested.put("inner", "value");
            Map<String, Object> input = new HashMap<>();
            input.put("outer", nested);
            String code = "return { result: $input.outer.inner };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("value", result.get("result"));
        }
    }

    @Nested
    @DisplayName("Data Type Handling")
    class DataTypeHandling {

        @Test
        @DisplayName("should handle arrays")
        void handleArrays() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { items: [1, 2, 3] };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertTrue(result.get("items") instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) result.get("items");
            assertEquals(3, items.size());
        }

        @Test
        @DisplayName("should handle booleans")
        void handleBooleans() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { flag: true, other: false };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(true, result.get("flag"));
            assertEquals(false, result.get("other"));
        }

        @Test
        @DisplayName("should handle null values")
        void handleNull() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { empty: null };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertNull(result.get("empty"));
        }

        @Test
        @DisplayName("should handle floating point numbers")
        void handleFloats() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { pi: 3.14159 };";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(3.14159, (Double) result.get("pi"), 0.00001);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should throw ScriptExecutionException for syntax errors")
        void syntaxError() {
            Map<String, Object> input = new HashMap<>();
            String code = "return { invalid syntax";
            Node node = createNode(code);

            ScriptExecutionException ex = assertThrows(
                    ScriptExecutionException.class,
                    () -> strategy.execute(code, input, node, context));

            assertEquals("javascript", ex.getLanguage());
        }

        @Test
        @DisplayName("should throw ScriptExecutionException for runtime errors")
        void runtimeError() {
            Map<String, Object> input = new HashMap<>();
            String code = "return undefinedVariable.property;";
            Node node = createNode(code);

            ScriptExecutionException ex = assertThrows(
                    ScriptExecutionException.class,
                    () -> strategy.execute(code, input, node, context));

            assertEquals("javascript", ex.getLanguage());
        }
    }

    @Nested
    @DisplayName("Strategy Metadata")
    class StrategyMetadata {

        @Test
        @DisplayName("should return correct language ID")
        void languageId() {
            assertEquals("javascript", strategy.getLanguageId());
        }

        @Test
        @DisplayName("should return display name containing GraalJS")
        void displayName() {
            assertTrue(strategy.getDisplayName().contains("GraalJS"));
        }

        @Test
        @DisplayName("should be available")
        void isAvailable() {
            assertTrue(strategy.isAvailable());
        }
    }

    // Helper methods

    private Node createNode(String code) {
        return new Node(
                UUID.randomUUID().toString(),
                "code",
                "Test Code",
                new Node.Position(0, 0),
                Map.of("code", code),
                null,
                false,
                null);
    }

    private ExecutionService.ExecutionContext createMockContext() {
        return new ExecutionService.ExecutionContext(
                1L,
                null,
                Map.of(),
                null,
                new ExecutionLogger());
    }
}
