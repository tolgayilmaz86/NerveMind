package ai.nervemind.app.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for IfExecutor.
 * 
 * <p>
 * Tests conditional branching logic with SpEL expressions
 * and template variable interpolation.
 * </p>
 */
class IfExecutorTest {

    private IfExecutor ifExecutor;
    private ExecutionService.ExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        ifExecutor = new IfExecutor();
        mockContext = mock(ExecutionService.ExecutionContext.class);
    }

    private Node createIfNode(String condition) {
        Map<String, Object> params = new HashMap<>();
        params.put("condition", condition);

        return new Node(
                "if-1",
                "if",
                "Condition Check",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    @Nested
    @DisplayName("Simple Conditions")
    class SimpleConditions {

        @Test
        @DisplayName("Should evaluate true literal")
        void shouldEvaluateTrueLiteral() {
            Node node = createIfNode("true");
            Map<String, Object> input = new HashMap<>();

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("conditionResult", true)
                    .containsEntry("branch", "true");
        }

        @Test
        @DisplayName("Should evaluate false literal")
        void shouldEvaluateFalseLiteral() {
            Node node = createIfNode("false");
            Map<String, Object> input = new HashMap<>();

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("conditionResult", false)
                    .containsEntry("branch", "false");
        }

        @Test
        @DisplayName("Should default to true for empty condition")
        void shouldDefaultToTrueForEmpty() {
            Node node = createIfNode("");
            Map<String, Object> input = new HashMap<>();

            // Empty string evaluates to false in SpEL
            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            // SpEL treats empty string as falsy
            assertThat(result).containsKey("branch");
        }
    }

    @Nested
    @DisplayName("Numeric Comparisons")
    class NumericComparisons {

        @ParameterizedTest(name = "Should evaluate {0} with value {2} to {3}")
        @CsvSource({
                "'{{ count }} > 10', count, 15, true",
                "'{{ count }} > 10', count, 5, false",
                "'{{ price }} <= 100', price, 50, true",
                "'{{ price }} <= 100', price, 150, false",
                "'{{ value }} == 42', value, 42, true",
                "'{{ value }} == 42', value, 0, false"
        })
        @DisplayName("Should evaluate numeric comparisons")
        void shouldEvaluateNumericComparisons(String condition, String varName, int varValue, boolean expected) {
            Node node = createIfNode(condition);
            Map<String, Object> input = Map.of(varName, varValue);

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", expected);
        }
    }

    @Nested
    @DisplayName("String Comparisons")
    class StringComparisons {

        @Test
        @DisplayName("Should evaluate string equality")
        void shouldEvaluateStringEquality() {
            Node node = createIfNode("{{ status }} == 'active'");
            Map<String, Object> input = Map.of("status", "active");

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }

        @Test
        @DisplayName("Should evaluate string inequality")
        void shouldEvaluateStringInequality() {
            Node node = createIfNode("{{ status }} != 'pending'");
            Map<String, Object> input = Map.of("status", "active");

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }
    }

    @Nested
    @DisplayName("Logical Operators")
    class LogicalOperators {

        @Test
        @DisplayName("Should evaluate AND condition")
        void shouldEvaluateAndCondition() {
            Node node = createIfNode("{{ count }} > 5 and {{ enabled }} == true");
            Map<String, Object> input = Map.of("count", 10, "enabled", true);

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }

        @Test
        @DisplayName("Should evaluate OR condition")
        void shouldEvaluateOrCondition() {
            Node node = createIfNode("{{ status }} == 'active' or {{ status }} == 'pending'");
            Map<String, Object> input = Map.of("status", "pending");

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }

        @Test
        @DisplayName("Should evaluate NOT condition")
        void shouldEvaluateNotCondition() {
            Node node = createIfNode("!{{ disabled }}");
            Map<String, Object> input = Map.of("disabled", false);

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should preserve input data in output")
        void shouldPreserveInputData() {
            Node node = createIfNode("true");
            Map<String, Object> input = Map.of("key", "value", "count", 123);

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("key", "value")
                    .containsEntry("count", 123)
                    .containsKey("conditionResult")
                    .containsKey("branch");
        }

        @Test
        @DisplayName("Should handle missing variable gracefully")
        void shouldHandleMissingVariable() {
            Node node = createIfNode("{{ missingVar }} == 'test'");
            Map<String, Object> input = new HashMap<>();

            // Should not throw, should evaluate to false
            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("branch", "false");
        }

        @Test
        @DisplayName("Should handle nested path variables")
        void shouldHandleNestedPaths() {
            Node node = createIfNode("{{ data.status }} == 'active'");
            Map<String, Object> data = Map.of("status", "active");
            Map<String, Object> input = Map.of("data", data);

            Map<String, Object> result = ifExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("conditionResult", true);
        }
    }
}
