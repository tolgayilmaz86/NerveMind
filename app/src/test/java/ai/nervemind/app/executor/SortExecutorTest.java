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
 * Unit tests for SortExecutor.
 * 
 * <p>
 * Tests array sorting capabilities with various sort directions,
 * types, and field paths.
 * </p>
 */
class SortExecutorTest {

    private SortExecutor sortExecutor;
    private ExecutionService.ExecutionContext mockContext;

    @BeforeEach
    void setUp() {
        sortExecutor = new SortExecutor();
        mockContext = mock(ExecutionService.ExecutionContext.class);
    }

    private Node createSortNode(String inputField, String outputField,
            String sortBy, String direction, String sortType) {
        Map<String, Object> params = new HashMap<>();
        params.put("inputField", inputField);
        params.put("outputField", outputField);
        if (sortBy != null) {
            params.put("sortBy", sortBy);
        }
        params.put("direction", direction);
        if (sortType != null) {
            params.put("sortType", sortType);
        }

        return new Node(
                "sort-1",
                "sort",
                "Sort Items",
                new Node.Position(100.0, 100.0),
                params,
                null,
                false,
                null);
    }

    @Nested
    @DisplayName("Numeric Sorting")
    class NumericSorting {

        @Test
        @DisplayName("Should sort numbers ascending")
        void shouldSortNumbersAscending() {
            List<Map<String, Object>> items = List.of(
                    Map.of("name", "C", "value", 30),
                    Map.of("name", "A", "value", 10),
                    Map.of("name", "B", "value", 20));

            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            assertThat(sorted).extracting("value").containsExactly(10, 20, 30);
            assertThat(sorted).extracting("name").containsExactly("A", "B", "C");
        }

        @Test
        @DisplayName("Should sort numbers descending")
        void shouldSortNumbersDescending() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", 10),
                    Map.of("value", 30),
                    Map.of("value", 20));

            Node node = createSortNode("items", "sorted", "value", "desc", "number");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            assertThat(sorted).extracting("value").containsExactly(30, 20, 10);
        }

        @Test
        @DisplayName("Should handle numeric strings")
        void shouldHandleNumericStrings() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", "30"),
                    Map.of("value", "10"),
                    Map.of("value", "20"));

            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            // When sorting as numbers, "10" < "20" < "30"
            assertThat(sorted).extracting("value")
                    .containsExactly("10", "20", "30");
        }
    }

    @Nested
    @DisplayName("String Sorting")
    class StringSorting {

        @Test
        @DisplayName("Should sort strings alphabetically ascending")
        void shouldSortStringsAscending() {
            List<Map<String, Object>> items = List.of(
                    Map.of("name", "Charlie"),
                    Map.of("name", "Alice"),
                    Map.of("name", "Bob"));

            Node node = createSortNode("items", "sorted", "name", "asc", "string");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            assertThat(sorted).extracting("name").containsExactly("Alice", "Bob", "Charlie");
        }

        @Test
        @DisplayName("Should sort strings case-insensitively")
        void shouldSortStringsIgnoreCase() {
            List<Map<String, Object>> items = List.of(
                    Map.of("name", "CHARLIE"),
                    Map.of("name", "alice"),
                    Map.of("name", "Bob"));

            Node node = createSortNode("items", "sorted", "name", "asc", "string");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            // Case-insensitive: alice < Bob < CHARLIE
            assertThat(sorted).extracting("name").containsExactly("alice", "Bob", "CHARLIE");
        }

        @Test
        @DisplayName("Should sort strings descending")
        void shouldSortStringsDescending() {
            List<Map<String, Object>> items = List.of(
                    Map.of("name", "Alice"),
                    Map.of("name", "Charlie"),
                    Map.of("name", "Bob"));

            Node node = createSortNode("items", "sorted", "name", "desc", "string");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");

            assertThat(sorted).extracting("name").containsExactly("Charlie", "Bob", "Alice");
        }
    }

    @Nested
    @DisplayName("Default Field Names")
    class DefaultFieldNames {

        @Test
        @DisplayName("Should use default input field 'items'")
        void shouldUseDefaultInputField() {
            List<Map<String, Object>> items = List.of(
                    Map.of("v", 2),
                    Map.of("v", 1));

            Map<String, Object> params = new HashMap<>();
            params.put("sortBy", "v");
            params.put("direction", "asc");
            // No inputField specified - should default to "items"

            Node node = new Node(
                    "sort-1", "sort", "Sort",
                    new Node.Position(100.0, 100.0), params,
                    null, false, null);
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            // Should have output in default field "sorted"
            assertThat(result).containsKey("sorted");
        }

        @Test
        @DisplayName("Should use custom field names")
        void shouldUseCustomFieldNames() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", 2),
                    Map.of("value", 1));

            Node node = createSortNode("myInput", "myOutput", "value", "asc", "number");
            Map<String, Object> input = Map.of("myInput", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            assertThat(result).containsKey("myOutput");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("myOutput");
            assertThat(sorted).extracting("value").containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("Metadata Output")
    class MetadataOutput {

        @Test
        @DisplayName("Should include sorting metadata")
        void shouldIncludeSortingMetadata() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", 1),
                    Map.of("value", 2),
                    Map.of("value", 3));

            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            assertThat(result)
                    .containsEntry("_sortedCount", 3)
                    .containsEntry("_sortedBy", "value")
                    .containsEntry("_sortDirection", "asc");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = Map.of("items", List.of());

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            List<?> sorted = (List<?>) result.get("sorted");
            assertThat(sorted).isEmpty();
            assertThat(result).containsEntry("_sortedCount", 0);
        }

        @Test
        @DisplayName("Should handle single item list")
        void shouldHandleSingleItem() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", 42));

            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = Map.of("items", items);

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sorted = (List<Map<String, Object>>) result.get("sorted");
            assertThat(sorted)
                    .hasSize(1)
                    .first()
                    .satisfies(item -> assertThat(item).containsEntry("value", 42));
        }

        @Test
        @DisplayName("Should preserve original input data")
        void shouldPreserveInputData() {
            List<Map<String, Object>> items = List.of(
                    Map.of("value", 1));

            Node node = createSortNode("items", "sorted", "value", "asc", "number");
            Map<String, Object> input = new HashMap<>();
            input.put("items", items);
            input.put("metadata", "preserved");

            Map<String, Object> result = sortExecutor.execute(node, input, mockContext);

            assertThat(result).containsEntry("metadata", "preserved");
        }
    }
}
