package ai.nervemind.app.executor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.condition.EnabledIf;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for Embedded Python (GraalPy) execution strategy.
 * 
 * <p>
 * These tests verify that Python code executes correctly using the GraalPy
 * embedded interpreter. Tests are conditionally enabled based on GraalPy
 * availability.
 * </p>
 */
@DisplayName("EmbeddedPythonExecutionStrategy")
class EmbeddedPythonExecutionStrategyTest {

    private EmbeddedPythonExecutionStrategy strategy;
    private ExecutionService.ExecutionContext context;

    @BeforeEach
    void setUp() {
        strategy = new EmbeddedPythonExecutionStrategy();
        context = createMockContext();
    }

    @Nested
    @DisplayName("Availability Check")
    class AvailabilityCheck {

        @Test
        @DisplayName("should report availability status")
        void checkAvailability() {
            // This test just verifies the method doesn't throw
            boolean available = strategy.isAvailable();
            String info = strategy.getAvailabilityInfo();

            assertNotNull(info);
            if (available) {
                assertTrue(info.contains("GraalPy") || info.isEmpty());
            }
        }

        @Test
        @DisplayName("should return correct language ID")
        void languageId() {
            assertEquals("python", strategy.getLanguageId());
        }

        @Test
        @DisplayName("should return display name containing GraalPy")
        void displayName() {
            assertTrue(strategy.getDisplayName().contains("GraalPy"));
        }
    }

    @Nested
    @DisplayName("Basic Execution")
    @EnabledIf("ai.nervemind.app.executor.script.EmbeddedPythonExecutionStrategyTest#isGraalPyAvailable")
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
            String code = "return 42";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(42L, result.get("result"));
        }

        @Test
        @DisplayName("should execute and return dictionary")
        void returnDict() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'name': 'test', 'count': 5}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("test", result.get("name"));
            assertEquals(5L, result.get("count"));
        }

        @Test
        @DisplayName("should preserve input data in output")
        void preserveInput() {
            Map<String, Object> input = Map.of("existing", "data");
            String code = "return {'new_key': 'new_value'}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("data", result.get("existing"));
            assertEquals("new_value", result.get("new_key"));
        }
    }

    @Nested
    @DisplayName("Input Bindings")
    @EnabledIf("ai.nervemind.app.executor.script.EmbeddedPythonExecutionStrategyTest#isGraalPyAvailable")
    class InputBindings {

        @Test
        @DisplayName("should access input via input dict")
        void accessInput() {
            Map<String, Object> input = Map.of("value", 10L);
            String code = "return {'doubled': input.get('value', 0) * 2}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(20L, result.get("doubled"));
        }

        @Test
        @DisplayName("should access nested input data")
        void accessNestedInput() {
            Map<String, Object> input = Map.of(
                    "user", Map.of("name", "Alice", "age", 30L));
            String code = "return {'greeting': 'Hello, ' + input['user']['name']}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("Hello, Alice", result.get("greeting"));
        }

        @Test
        @DisplayName("should handle missing input keys gracefully")
        void missingInputKey() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'value': input.get('missing', 'default')}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("default", result.get("value"));
        }
    }

    @Nested
    @DisplayName("Data Type Handling")
    @EnabledIf("ai.nervemind.app.executor.script.EmbeddedPythonExecutionStrategyTest#isGraalPyAvailable")
    class DataTypeHandling {

        @Test
        @DisplayName("should handle lists")
        void handleLists() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'items': [1, 2, 3]}";
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
            String code = "return {'flag': True, 'other': False}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(true, result.get("flag"));
            assertEquals(false, result.get("other"));
        }

        @Test
        @DisplayName("should handle None as null")
        void handleNone() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'empty': None}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertNull(result.get("empty"));
        }

        @Test
        @DisplayName("should handle floating point numbers")
        void handleFloats() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'pi': 3.14159}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(3.14159, (Double) result.get("pi"), 0.00001);
        }

        @Test
        @DisplayName("should handle strings")
        void handleStrings() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'message': 'Hello, Python!'}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("Hello, Python!", result.get("message"));
        }
    }

    @Nested
    @DisplayName("Python Features")
    @EnabledIf("ai.nervemind.app.executor.script.EmbeddedPythonExecutionStrategyTest#isGraalPyAvailable")
    class PythonFeatures {

        @Test
        @DisplayName("should support list comprehensions")
        void listComprehension() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {'squares': [x**2 for x in range(5)]}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            @SuppressWarnings("unchecked")
            List<Object> squares = (List<Object>) result.get("squares");
            assertEquals(5, squares.size());
            assertEquals(0L, squares.get(0));
            assertEquals(16L, squares.get(4));
        }

        @Test
        @DisplayName("should support dictionary comprehensions")
        void dictComprehension() {
            Map<String, Object> input = new HashMap<>();
            String code = "return {str(i): i*2 for i in range(3)}";
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals(0L, result.get("0"));
            assertEquals(2L, result.get("1"));
            assertEquals(4L, result.get("2"));
        }

        @Test
        @DisplayName("should support string operations")
        void stringOperations() {
            Map<String, Object> input = Map.of("text", "hello world");
            String code = """
                    text = input.get('text', '')
                    return {
                        'upper': text.upper(),
                        'words': text.split(),
                        'length': len(text)
                    }
                    """;
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            assertEquals("HELLO WORLD", result.get("upper"));
            assertEquals(11L, result.get("length"));
        }

        @Test
        @DisplayName("should support math operations")
        void mathOperations() {
            Map<String, Object> input = new HashMap<>();
            String code = """
                    import math
                    return {
                        'sqrt': math.sqrt(16),
                        'pow': math.pow(2, 10),
                        'floor': math.floor(3.7)
                    }
                    """;
            Node node = createNode(code);

            Map<String, Object> result = strategy.execute(code, input, node, context);

            // Math functions may return Long if value fits, or Double
            assertEquals(4.0, ((Number) result.get("sqrt")).doubleValue(), 0.001);
            assertEquals(1024.0, ((Number) result.get("pow")).doubleValue(), 0.001);
            assertEquals(3L, ((Number) result.get("floor")).longValue());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    @EnabledIf("ai.nervemind.app.executor.script.EmbeddedPythonExecutionStrategyTest#isGraalPyAvailable")
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

            assertEquals("python", ex.getLanguage());
        }

        @Test
        @DisplayName("should throw ScriptExecutionException for NameError")
        void nameError() {
            Map<String, Object> input = new HashMap<>();
            String code = "return undefined_variable";
            Node node = createNode(code);

            ScriptExecutionException ex = assertThrows(
                    ScriptExecutionException.class,
                    () -> strategy.execute(code, input, node, context));

            assertEquals("python", ex.getLanguage());
            assertTrue(ex.getMessage().toLowerCase().contains("name")
                    || ex.getMessage().toLowerCase().contains("undefined"));
        }

        @Test
        @DisplayName("should throw ScriptExecutionException for TypeError")
        void typeError() {
            Map<String, Object> input = new HashMap<>();
            String code = "return 'string' + 42"; // TypeError in Python
            Node node = createNode(code);

            ScriptExecutionException ex = assertThrows(
                    ScriptExecutionException.class,
                    () -> strategy.execute(code, input, node, context));

            assertEquals("python", ex.getLanguage());
        }
    }

    // Helper methods

    private Node createNode(String code) {
        return new Node(
                UUID.randomUUID().toString(),
                "code",
                "Test Code",
                new Node.Position(0, 0),
                Map.of("code", code, "language", "python"),
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

    /**
     * Check if GraalPy is available for conditional test execution.
     */
    static boolean isGraalPyAvailable() {
        try {
            EmbeddedPythonExecutionStrategy strategy = new EmbeddedPythonExecutionStrategy();
            return strategy.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
