/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.executor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionLogger;
import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.NodeExecutionException;

/**
 * Node executor for reading and writing CSV files.
 *
 * <p>
 * Supports reading CSV files into structured row data, writing structured
 * data to CSV files, and parsing CSV content from string input. Uses only
 * built-in Java I/O — no external dependencies required.
 * </p>
 *
 * <h2>Node Configuration</h2>
 * <table border="1">
 * <caption>CSV node parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>operation</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>READ or WRITE</td>
 * </tr>
 * <tr>
 * <td>filePath</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>Path to CSV file</td>
 * </tr>
 * <tr>
 * <td>delimiter</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Column delimiter (default: comma)</td>
 * </tr>
 * <tr>
 * <td>hasHeader</td>
 * <td>Boolean</td>
 * <td>No</td>
 * <td>First row is header (default: true)</td>
 * </tr>
 * <tr>
 * <td>content</td>
 * <td>String</td>
 * <td>No</td>
 * <td>CSV string to parse (alternative to filePath for READ)</td>
 * </tr>
 * <tr>
 * <td>data</td>
 * <td>String</td>
 * <td>No</td>
 * <td>JSON rows to write (WRITE operation)</td>
 * </tr>
 * <tr>
 * <td>columns</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Comma-separated column names for WRITE header</td>
 * </tr>
 * </table>
 *
 * <h2>Output Data</h2>
 * <ul>
 * <li><strong>rows</strong> - List of row maps (READ) keyed by column name</li>
 * <li><strong>columns</strong> - List of column names</li>
 * <li><strong>rowCount</strong> - Number of rows</li>
 * <li><strong>success</strong> - Boolean</li>
 * <li><strong>filePath</strong> - Written file path (WRITE only)</li>
 * </ul>
 *
 * @since 1.1.0
 * @see NodeExecutor
 */
@Component
public class CsvExecutor implements NodeExecutor {

    private static final int MAX_ROWS = 50_000;

    /**
     * Default constructor.
     */
    public CsvExecutor() {
        // Default constructor
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String operation = ((String) params.getOrDefault("operation", "READ")).toUpperCase();
        String delimiter = (String) params.getOrDefault("delimiter", ",");
        boolean hasHeader = extractBoolean(params.get("hasHeader"), true);

        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG,
                "CSV: operation=" + operation + ", delimiter='" + delimiter + "'",
                Map.of());

        return switch (operation) {
            case "READ" -> executeRead(params, input, delimiter, hasHeader, context);
            case "WRITE" -> executeWrite(params, input, delimiter, hasHeader, context);
            default -> throw new NodeExecutionException(
                    "Unsupported CSV operation: " + operation + ". Use READ or WRITE.");
        };
    }

    @Override
    public String getNodeType() {
        return "csv";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeRead(Map<String, Object> params,
            Map<String, Object> input, String delimiter, boolean hasHeader,
            ExecutionService.ExecutionContext context) {
        String content = (String) params.getOrDefault("content", "");
        String filePath = (String) params.getOrDefault("filePath", "");

        BufferedReader reader;
        try {
            if (!content.isBlank()) {
                reader = new BufferedReader(new StringReader(content));
            } else if (!filePath.isBlank()) {
                Path path = Path.of(filePath).toAbsolutePath().normalize();
                reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            } else {
                // Try to read from input data
                Object inputContent = input.get("body");
                if (inputContent == null) {
                    inputContent = input.get("content");
                }
                if (inputContent == null) {
                    throw new NodeExecutionException(
                            "No CSV source specified. Provide 'filePath', 'content', or pass data via input.");
                }
                reader = new BufferedReader(new StringReader(inputContent.toString()));
            }
        } catch (IOException e) {
            throw new NodeExecutionException("Failed to open CSV source: " + e.getMessage(), e);
        }

        try (reader) {
            List<String[]> rawRows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && rawRows.size() < MAX_ROWS) {
                rawRows.add(parseCsvLine(line, delimiter));
            }

            if (rawRows.isEmpty()) {
                Map<String, Object> output = new HashMap<>();
                output.put("rows", List.of());
                output.put("columns", List.of());
                output.put("rowCount", 0);
                output.put("success", true);
                return output;
            }

            List<String> columns;
            int dataStart;
            if (hasHeader) {
                columns = List.of(rawRows.getFirst());
                dataStart = 1;
            } else {
                // Generate column names: col0, col1, col2...
                int colCount = rawRows.getFirst().length;
                columns = new ArrayList<>(colCount);
                for (int i = 0; i < colCount; i++) {
                    columns.add("col" + i);
                }
                dataStart = 0;
            }

            List<Map<String, Object>> rows = new ArrayList<>(rawRows.size());
            for (int i = dataStart; i < rawRows.size(); i++) {
                String[] values = rawRows.get(i);
                Map<String, Object> row = new LinkedHashMap<>(columns.size());
                for (int j = 0; j < columns.size(); j++) {
                    row.put(columns.get(j), j < values.length ? values[j] : "");
                }
                rows.add(row);
            }

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "CSV READ: " + rows.size() + " rows, " + columns.size() + " columns",
                    Map.of());

            Map<String, Object> output = new HashMap<>();
            output.put("rows", rows);
            output.put("columns", columns);
            output.put("rowCount", rows.size());
            output.put("success", true);
            return output;

        } catch (IOException e) {
            throw new NodeExecutionException("Failed to read CSV: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeWrite(Map<String, Object> params,
            Map<String, Object> input, String delimiter, boolean hasHeader,
            ExecutionService.ExecutionContext context) {
        String filePath = (String) params.getOrDefault("filePath", "");
        String columnsParam = (String) params.getOrDefault("columns", "");

        // Get rows from input or params
        Object dataSource = input.getOrDefault("rows", params.get("data"));
        if (dataSource == null) {
            throw new NodeExecutionException("No data to write. Provide rows via input or 'data' parameter.");
        }

        List<Map<String, Object>> rows;
        if (dataSource instanceof List<?> list) {
            rows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add((Map<String, Object>) map);
                }
            }
        } else {
            throw new NodeExecutionException("Data must be a list of row objects.");
        }

        // Determine columns
        List<String> columns;
        if (!columnsParam.isBlank()) {
            columns = List.of(columnsParam.split(","));
        } else if (!rows.isEmpty()) {
            columns = new ArrayList<>(rows.getFirst().keySet());
        } else {
            columns = List.of();
        }

        try {
            StringWriter sw = new StringWriter();
            BufferedWriter writer = new BufferedWriter(sw);

            // Write header
            if (hasHeader && !columns.isEmpty()) {
                writer.write(toCsvLine(columns.stream().map(Object::toString).toArray(String[]::new), delimiter));
                writer.newLine();
            }

            // Write data rows
            for (Map<String, Object> row : rows) {
                String[] values = new String[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    Object val = row.get(columns.get(i));
                    values[i] = val != null ? val.toString() : "";
                }
                writer.write(toCsvLine(values, delimiter));
                writer.newLine();
            }
            writer.flush();

            String csvContent = sw.toString();

            // Write to file if path specified
            if (!filePath.isBlank()) {
                Path path = Path.of(filePath).toAbsolutePath().normalize();
                Files.writeString(path, csvContent, StandardCharsets.UTF_8);
            }

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "CSV WRITE: " + rows.size() + " rows, " + columns.size() + " columns",
                    Map.of());

            Map<String, Object> output = new HashMap<>();
            output.put("content", csvContent);
            output.put("rowCount", rows.size());
            output.put("columns", columns);
            output.put("success", true);
            if (!filePath.isBlank()) {
                output.put("filePath", filePath);
            }
            return output;

        } catch (IOException e) {
            throw new NodeExecutionException("Failed to write CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a single CSV line respecting quoted fields.
     */
    private String[] parseCsvLine(String line, String delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char delim = delimiter.isEmpty() ? ',' : delimiter.charAt(0);

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // Check for escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delim) {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());

        return fields.toArray(String[]::new);
    }

    /**
     * Converts values to a CSV line with proper quoting.
     */
    private String toCsvLine(String[] values, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            String val = values[i];
            if (val.contains(delimiter) || val.contains("\"") || val.contains("\n")) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val);
            }
        }
        return sb.toString();
    }

    private boolean extractBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
