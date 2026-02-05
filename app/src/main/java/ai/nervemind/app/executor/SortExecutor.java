package ai.nervemind.app.executor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "sort" node type - sorts items in an array.
 *
 * <p>
 * Orders array elements based on configurable criteria including sort field,
 * direction, and data type. Supports auto-detection of value types for
 * intelligent sorting behavior.
 * </p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Sort node configuration parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>inputField</td>
 * <td>String</td>
 * <td>"items"</td>
 * <td>Field containing the array to sort</td>
 * </tr>
 * <tr>
 * <td>outputField</td>
 * <td>String</td>
 * <td>"sorted"</td>
 * <td>Field to store sorted results</td>
 * </tr>
 * <tr>
 * <td>sortBy</td>
 * <td>String</td>
 * <td>null</td>
 * <td>Field path within each item (null = sort by item value)</td>
 * </tr>
 * <tr>
 * <td>direction</td>
 * <td>String</td>
 * <td>"asc"</td>
 * <td>"asc" for ascending, "desc" for descending</td>
 * </tr>
 * <tr>
 * <td>sortType</td>
 * <td>String</td>
 * <td>auto</td>
 * <td>"string", "number", or "date" (auto-detects if not specified)</td>
 * </tr>
 * <tr>
 * <td>nullsFirst</td>
 * <td>Boolean</td>
 * <td>false</td>
 * <td>true to put null values first</td>
 * </tr>
 * </table>
 *
 * <h2>Sort Types</h2>
 * <ul>
 * <li><strong>number</strong> - Numeric comparison (parses strings to
 * double)</li>
 * <li><strong>string</strong> - Case-insensitive alphabetical comparison</li>
 * <li><strong>date</strong> - ISO date format comparison (YYYY-MM-DD)</li>
 * <li><strong>auto</strong> - Detects type based on actual values</li>
 * </ul>
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
 * <td>[outputField]</td>
 * <td>List</td>
 * <td>Sorted array</td>
 * </tr>
 * <tr>
 * <td>_sortedCount</td>
 * <td>Integer</td>
 * <td>Number of items sorted</td>
 * </tr>
 * <tr>
 * <td>_sortedBy</td>
 * <td>String</td>
 * <td>Field used for sorting</td>
 * </tr>
 * <tr>
 * <td>_sortDirection</td>
 * <td>String</td>
 * <td>"asc" or "desc"</td>
 * </tr>
 * </table>
 *
 * /**
 * Executor for sorting arrays based on specified criteria.
 * Supports sorting by number, date, and string values with ascending/descending
 * order.
 *
 * @see FilterExecutor For filtering arrays
 */
@Component
public class SortExecutor implements NodeExecutor {

    // Default constructor for Spring
    /**
     * Default constructor for Spring.
     */
    public SortExecutor() {
    }

    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_DATE = "date";
    private static final String TYPE_STRING = "string";

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String inputField = (String) params.getOrDefault("inputField", "items");
        String outputField = (String) params.getOrDefault("outputField", "sorted");
        String sortBy = (String) params.get("sortBy");
        String direction = (String) params.getOrDefault("direction", "asc");
        String sortType = (String) params.get("sortType"); // null means auto-detect
        boolean nullsFirst = (Boolean) params.getOrDefault("nullsFirst", false);

        // Get the input array
        Object inputData = getNestedValue(input, inputField);

        Map<String, Object> output = new HashMap<>(input);

        if (!(inputData instanceof List<?>)) {
            output.put(outputField, List.of());
            output.put("_sortedCount", 0);
            output.put("_error", "Input field '" + inputField + "' is not an array");
            return output;
        }

        List<?> items = (List<?>) inputData;
        List<Object> sorted = new ArrayList<>(items);

        boolean ascending = "asc".equalsIgnoreCase(direction);

        Comparator<Object> comparator = createComparator(sortBy, sortType, ascending, nullsFirst);
        sorted.sort(comparator);

        output.put(outputField, sorted);
        output.put("_sortedCount", sorted.size());
        output.put("_sortedBy", sortBy != null ? sortBy : "value");
        output.put("_sortDirection", direction);

        return output;
    }

    private Comparator<Object> createComparator(String sortBy, String sortType, boolean ascending, boolean nullsFirst) {
        Comparator<Object> comparator = (a, b) -> {
            Object valueA = extractValue(a, sortBy);
            Object valueB = extractValue(b, sortBy);

            int nullComparison = compareNulls(valueA, valueB, nullsFirst);
            if (nullComparison != Integer.MIN_VALUE) {
                return nullComparison;
            }

            String effectiveType = sortType != null ? sortType : detectType(valueA, valueB);
            return compareByType(valueA, valueB, effectiveType);
        };

        return ascending ? comparator : comparator.reversed();
    }

    private Object extractValue(Object item, String sortBy) {
        return sortBy != null ? getNestedValue(item, sortBy) : item;
    }

    private int compareNulls(Object a, Object b, boolean nullsFirst) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return nullsFirst ? -1 : 1;
        }
        if (b == null) {
            return nullsFirst ? 1 : -1;
        }
        return Integer.MIN_VALUE; // Sentinel value indicating no nulls
    }

    private int compareByType(Object a, Object b, String type) {
        return switch (type.toLowerCase()) {
            case TYPE_NUMBER -> compareAsNumbers(a, b);
            case TYPE_DATE -> compareAsDates(a, b);
            default -> compareAsStrings(a, b);
        };
    }

    private String detectType(Object a, Object b) {
        if (a instanceof Number || b instanceof Number) {
            return TYPE_NUMBER;
        }

        String strA = a.toString();
        String strB = b.toString();

        // Check if both look like numbers
        if (looksLikeNumber(strA) && looksLikeNumber(strB)) {
            return TYPE_NUMBER;
        }

        // Check if both look like dates (ISO format)
        if (looksLikeDate(strA) && looksLikeDate(strB)) {
            return TYPE_DATE;
        }

        return TYPE_STRING;
    }

    private boolean looksLikeNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    private boolean looksLikeDate(String s) {
        // Check for ISO date format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS
        return s.matches("\\d{4}-\\d{2}-\\d{2}.*");
    }

    private int compareAsNumbers(Object a, Object b) {
        try {
            double numA = a instanceof Number n ? n.doubleValue() : Double.parseDouble(a.toString());
            double numB = b instanceof Number n ? n.doubleValue() : Double.parseDouble(b.toString());
            return Double.compare(numA, numB);
        } catch (NumberFormatException _) {
            return compareAsStrings(a, b);
        }
    }

    private int compareAsDates(Object a, Object b) {
        // For dates in ISO format, string comparison works
        return a.toString().compareTo(b.toString());
    }

    private int compareAsStrings(Object a, Object b) {
        return a.toString().compareToIgnoreCase(b.toString());
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object data, String path) {
        if (path == null || path.isEmpty() || data == null) {
            return data;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    @Override
    public String getNodeType() {
        return "sort";
    }
}
