package ai.nervemind.app.executor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.app.service.SettingsService;
import ai.nervemind.common.domain.Node;

/**
 * Tests for ExternalPythonExecutionStrategy.
 * 
 * These tests are only run when external Python is available on the system.
 */
@ExtendWith(MockitoExtension.class)
class ExternalPythonExecutionStrategyTest {

    @Mock
    private SettingsService settingsService;

    private ExecutionService.ExecutionContext context;

    private ExternalPythonExecutionStrategy strategy;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        strategy = new ExternalPythonExecutionStrategy(objectMapper, settingsService);

        // Setup mocks - use lenient for settings that may not be called in all tests
        lenient().when(settingsService.getValue(eq(SettingsDefaults.PYTHON_EXTERNAL_PATH), anyString())).thenReturn("");
        lenient().when(settingsService.getValue(eq(SettingsDefaults.PYTHON_VENV_PATH), anyString())).thenReturn("");
        lenient().when(settingsService.getLong(eq(SettingsDefaults.PYTHON_TIMEOUT), anyLong())).thenReturn(60000L);
        lenient().when(settingsService.getValue(eq(SettingsDefaults.PYTHON_EXECUTION_MODE), anyString()))
                .thenReturn("external");

        // Create real context
        context = new ExecutionService.ExecutionContext(
                1L,
                null,
                Map.of(),
                null,
                new ExecutionLogger());
    }

    private Node createTestNode(String code) {
        return new Node(
                "test-node",
                "code",
                "Test Code",
                new Node.Position(0, 0),
                Map.of("code", code, "language", "python"),
                null,
                false,
                null);
    }

    // Helper to check if Python is available
    static boolean isPythonAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return true;
            }
            // Try python3
            pb = new ProcessBuilder("python3", "--version");
            pb.redirectErrorStream(true);
            process = pb.start();
            finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testGetLanguageId() {
        assertEquals("python-external", strategy.getLanguageId());
    }

    @Test
    void testGetDisplayName() {
        String displayName = strategy.getDisplayName();
        assertTrue(displayName.contains("Python") && displayName.contains("External"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testIsAvailable() {
        assertTrue(strategy.isAvailable(), "External Python should be available");
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testSimpleReturn() throws ScriptExecutionException {
        String code = "return {'message': 'Hello from Python!'}";
        Node node = createTestNode(code);

        Map<String, Object> result = strategy.execute(code, new HashMap<>(), node, context);

        assertEquals("Hello from Python!", result.get("message"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testAccessInputData() throws ScriptExecutionException {
        String code = """
                name = input.get('name', 'World')
                return {'greeting': f'Hello, {name}!'}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("name", "NerveMind");

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        assertEquals("Hello, NerveMind!", result.get("greeting"));
        assertEquals("NerveMind", result.get("name")); // Input preserved
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testListProcessing() throws ScriptExecutionException {
        String code = """
                items = input.get('items', [])
                processed = [x.upper() for x in items]
                return {'processed': processed, 'count': len(items)}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("items", List.of("apple", "banana", "cherry"));

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        @SuppressWarnings("unchecked")
        List<String> processed = (List<String>) result.get("processed");
        assertEquals(List.of("APPLE", "BANANA", "CHERRY"), processed);
        assertEquals(3, result.get("count"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testMathOperations() throws ScriptExecutionException {
        String code = """
                import math
                numbers = input.get('numbers', [])
                return {
                    'sum': sum(numbers),
                    'avg': sum(numbers) / len(numbers) if numbers else 0,
                    'sqrt_first': math.sqrt(numbers[0]) if numbers else 0
                }
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("numbers", List.of(16, 25, 36));

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        assertEquals(77, ((Number) result.get("sum")).intValue());
        assertEquals(4.0, ((Number) result.get("sqrt_first")).doubleValue(), 0.001);
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testJsonProcessing() throws ScriptExecutionException {
        String code = """
                import json
                data = input.get('data', {})
                json_str = json.dumps(data, indent=2)
                parsed = json.loads(json_str)
                return {'json_string': json_str, 'roundtrip': parsed}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("data", Map.of("key", "value", "number", 42));

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        assertNotNull(result.get("json_string"));
        assertTrue(result.get("json_string").toString().contains("key"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testRegexProcessing() throws ScriptExecutionException {
        String code = """
                import re
                text = input.get('text', '')
                emails = re.findall(r'[\\w.+-]+@[\\w-]+\\.[\\w.-]+', text)
                return {'emails': emails, 'count': len(emails)}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("text",
                "Contact us at support@example.com or sales@company.org");

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        @SuppressWarnings("unchecked")
        List<String> emails = (List<String>) result.get("emails");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("support@example.com"));
        assertTrue(emails.contains("sales@company.org"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testDatetimeProcessing() throws ScriptExecutionException {
        String code = """
                from datetime import datetime, timedelta
                now = datetime.now()
                tomorrow = now + timedelta(days=1)
                return {
                    'today': now.strftime('%Y-%m-%d'),
                    'tomorrow': tomorrow.strftime('%Y-%m-%d')
                }
                """;
        Node node = createTestNode(code);

        Map<String, Object> result = strategy.execute(code, new HashMap<>(), node, context);

        assertNotNull(result.get("today"));
        assertNotNull(result.get("tomorrow"));
        // Verify date format
        assertTrue(result.get("today").toString().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testEmptyCodeReturnsInput() throws ScriptExecutionException {
        Node node = createTestNode("");
        Map<String, Object> inputData = Map.of("key", "value");

        Map<String, Object> result = strategy.execute("", inputData, node, context);

        assertEquals("value", result.get("key"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testBlankCodeReturnsInput() throws ScriptExecutionException {
        Node node = createTestNode("   ");
        Map<String, Object> inputData = Map.of("key", "value");

        Map<String, Object> result = strategy.execute("   ", inputData, node, context);

        assertEquals("value", result.get("key"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testSyntaxErrorThrowsException() {
        String code = "def broken(:\n    pass";
        Node node = createTestNode(code);

        ScriptExecutionException exception = assertThrows(ScriptExecutionException.class,
                () -> strategy.execute(code, new HashMap<>(), node, context));

        assertTrue(exception.getMessage().toLowerCase().contains("syntax")
                || exception.getMessage().toLowerCase().contains("error"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testRuntimeErrorThrowsException() {
        String code = "x = 1 / 0";
        Node node = createTestNode(code);

        ScriptExecutionException exception = assertThrows(ScriptExecutionException.class,
                () -> strategy.execute(code, new HashMap<>(), node, context));

        assertTrue(exception.getMessage().toLowerCase().contains("division")
                || exception.getMessage().toLowerCase().contains("zero")
                || exception.getMessage().toLowerCase().contains("error"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testCollectionsModule() throws ScriptExecutionException {
        String code = """
                from collections import Counter
                words = input.get('words', [])
                counts = Counter(words)
                return {'counts': dict(counts.most_common(3))}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("words",
                List.of("apple", "banana", "apple", "cherry", "banana", "apple"));

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        @SuppressWarnings("unchecked")
        Map<String, Integer> counts = (Map<String, Integer>) result.get("counts");
        assertEquals(3, counts.get("apple"));
        assertEquals(2, counts.get("banana"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testNestedDataStructures() throws ScriptExecutionException {
        String code = """
                data = input.get('data', {})
                nested = data.get('nested', {})
                value = nested.get('deep', {}).get('value', 'default')
                return {'extracted': value}
                """;
        Node node = createTestNode(code);
        Map<String, Object> inputData = Map.of("data",
                Map.of("nested", Map.of("deep", Map.of("value", "found!"))));

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        assertEquals("found!", result.get("extracted"));
    }

    @Test
    @EnabledIf("isPythonAvailable")
    void testInputPreservedInOutput() throws ScriptExecutionException {
        String code = "return {'new_key': 'new_value'}";
        Node node = createTestNode(code);
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("original", "preserved");

        Map<String, Object> result = strategy.execute(code, inputData, node, context);

        assertEquals("preserved", result.get("original"));
        assertEquals("new_value", result.get("new_key"));
    }
}
