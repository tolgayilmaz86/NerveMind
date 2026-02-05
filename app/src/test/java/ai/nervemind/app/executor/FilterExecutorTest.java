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

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.common.domain.Node;

/**
 * Unit tests for FilterExecutor.
 * 
 * <p>
 * Tests array filtering capabilities with various conditions
 * and combination modes (AND/OR).
 * </p>
 */
class FilterExecutorTest {

        private FilterExecutor filterExecutor;
        private ExecutionService.ExecutionContext mockContext;

        @BeforeEach
        void setUp() {
                filterExecutor = new FilterExecutor();
                mockContext = mock(ExecutionService.ExecutionContext.class);
        }

        private Node createFilterNode(String inputField, String outputField,
                        List<Map<String, Object>> conditions, String combineWith, boolean keepMatching) {
                Map<String, Object> params = new HashMap<>();
                params.put("inputField", inputField);
                params.put("outputField", outputField);
                params.put("conditions", conditions);
                params.put("combineWith", combineWith);
                params.put("keepMatching", keepMatching);

                return new Node(
                                "filter-1",
                                "filter",
                                "Filter Items",
                                new Node.Position(100.0, 100.0),
                                params,
                                null,
                                false,
                                null);
        }

        private Map<String, Object> condition(String field, String operator, Object value) {
                Map<String, Object> cond = new HashMap<>();
                cond.put("field", field);
                cond.put("operator", operator);
                cond.put("value", value);
                return cond;
        }

        @Nested
        @DisplayName("Basic Filtering")
        class BasicFiltering {

                @Test
                @DisplayName("Should filter items with equals condition")
                void shouldFilterWithEquals() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "Alice", "status", "active"),
                                        Map.of("name", "Bob", "status", "inactive"),
                                        Map.of("name", "Charlie", "status", "active"));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("status", "equals", "active"));

                        Node node = createFilterNode("items", "filtered", conditions, "and", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("filtered");

                        assertThat(filtered).hasSize(2);
                        assertThat(filtered).extracting("name").containsExactly("Alice", "Charlie");
                        assertThat(result)
                                        .containsEntry("_filteredCount", 2)
                                        .containsEntry("_originalCount", 3)
                                        .containsEntry("_removedCount", 1);
                }

                @Test
                @DisplayName("Should filter items with greaterThan condition")
                void shouldFilterWithGreaterThan() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "A", "price", 50),
                                        Map.of("name", "B", "price", 150),
                                        Map.of("name", "C", "price", 200));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("price", "gt", 100)); // Use "gt" not "greaterThan"

                        Node node = createFilterNode("items", "expensive", conditions, "and", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("expensive");

                        assertThat(filtered).hasSize(2);
                        assertThat(filtered).extracting("name").containsExactly("B", "C");
                }

                @Test
                @DisplayName("Should filter items with contains condition")
                void shouldFilterWithContains() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "Apple iPhone"),
                                        Map.of("name", "Samsung Galaxy"),
                                        Map.of("name", "Apple iPad"));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("name", "contains", "Apple"));

                        Node node = createFilterNode("items", "appleProducts", conditions, "and", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("appleProducts");

                        assertThat(filtered).hasSize(2);
                }
        }

        @Nested
        @DisplayName("Condition Combining")
        class ConditionCombining {

                @Test
                @DisplayName("Should combine conditions with AND")
                void shouldCombineWithAnd() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("status", "active", "price", 50),
                                        Map.of("status", "active", "price", 150),
                                        Map.of("status", "inactive", "price", 150));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("status", "equals", "active"),
                                        condition("price", "gt", 100)); // Use "gt" not "greaterThan"

                        Node node = createFilterNode("items", "filtered", conditions, "and", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("filtered");

                        // Only the second item matches BOTH conditions
                        assertThat(filtered)
                                        .hasSize(1)
                                        .first()
                                        .satisfies(item -> assertThat(item)
                                                        .containsEntry("price", 150)
                                                        .containsEntry("status", "active"));
                }

                @Test
                @DisplayName("Should combine conditions with OR")
                void shouldCombineWithOr() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("status", "active", "priority", "low"),
                                        Map.of("status", "inactive", "priority", "high"),
                                        Map.of("status", "inactive", "priority", "low"));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("status", "equals", "active"),
                                        condition("priority", "equals", "high"));

                        Node node = createFilterNode("items", "filtered", conditions, "or", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("filtered");

                        // First two items match at least one condition
                        assertThat(filtered).hasSize(2);
                }
        }

        @Nested
        @DisplayName("Keep Matching Mode")
        class KeepMatchingMode {

                @Test
                @DisplayName("Should exclude matching items when keepMatching is false")
                void shouldExcludeMatchingItems() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "Keep1", "remove", false),
                                        Map.of("name", "Remove", "remove", true),
                                        Map.of("name", "Keep2", "remove", false));

                        List<Map<String, Object>> conditions = List.of(
                                        condition("remove", "equals", true));

                        // keepMatching = false means EXCLUDE matching items
                        Node node = createFilterNode("items", "kept", conditions, "and", false);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> filtered = (List<Map<String, Object>>) result.get("kept");

                        assertThat(filtered).hasSize(2);
                        assertThat(filtered).extracting("name").containsExactly("Keep1", "Keep2");
                }
        }

        @Nested
        @DisplayName("Edge Cases")
        class EdgeCases {

                @Test
                @DisplayName("Should handle empty items list")
                void shouldHandleEmptyList() {
                        List<Map<String, Object>> conditions = List.of(
                                        condition("status", "equals", "active"));

                        Node node = createFilterNode("items", "filtered", conditions, "and", true);
                        Map<String, Object> input = Map.of("items", List.of());

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        List<?> filtered = (List<?>) result.get("filtered");

                        assertThat(filtered).isEmpty();
                        assertThat(result)
                                        .containsEntry("_filteredCount", 0)
                                        .containsEntry("_originalCount", 0);
                }

                @Test
                @DisplayName("Should handle no conditions (pass all)")
                void shouldHandleNoConditions() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "A"),
                                        Map.of("name", "B"));

                        Node node = createFilterNode("items", "filtered", List.of(), "and", true);
                        Map<String, Object> input = Map.of("items", items);

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        List<?> filtered = (List<?>) result.get("filtered");

                        assertThat(filtered).hasSize(2);
                }

                @Test
                @DisplayName("Should preserve original input data")
                void shouldPreserveInputData() {
                        List<Map<String, Object>> items = List.of(
                                        Map.of("name", "A"));

                        Node node = createFilterNode("items", "filtered", List.of(), "and", true);
                        Map<String, Object> input = new HashMap<>();
                        input.put("items", items);
                        input.put("other", "data");

                        Map<String, Object> result = filterExecutor.execute(node, input, mockContext);

                        assertThat(result)
                                        .containsEntry("other", "data")
                                        .containsKey("items");
                }
        }
}
