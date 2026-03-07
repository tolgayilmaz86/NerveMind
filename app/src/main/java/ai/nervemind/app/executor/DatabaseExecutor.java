/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.executor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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
 * Node executor for H2 database operations.
 *
 * <p>
 * Executes SQL queries against an H2 database. Supports SELECT, INSERT,
 * UPDATE, DELETE, and custom SQL operations with parameterized queries
 * to prevent SQL injection.
 * </p>
 *
 * <h2>Node Configuration</h2>
 * <table border="1">
 * <caption>Database node parameters</caption>
 * <tr>
 * <th>Parameter</th>
 * <th>Type</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>jdbcUrl</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>H2 JDBC URL (e.g., jdbc:h2:mem:test or jdbc:h2:file:./data/mydb)</td>
 * </tr>
 * <tr>
 * <td>username</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Database username (default: sa)</td>
 * </tr>
 * <tr>
 * <td>password</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Database password (default: empty)</td>
 * </tr>
 * <tr>
 * <td>operation</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Operation type: SELECT, INSERT, UPDATE, DELETE, DDL (default:
 * SELECT)</td>
 * </tr>
 * <tr>
 * <td>query</td>
 * <td>String</td>
 * <td>Yes</td>
 * <td>SQL query with optional ? placeholders</td>
 * </tr>
 * <tr>
 * <td>parameters</td>
 * <td>String</td>
 * <td>No</td>
 * <td>Comma-separated values for ? placeholders</td>
 * </tr>
 * <tr>
 * <td>maxRows</td>
 * <td>Integer</td>
 * <td>No</td>
 * <td>Max rows to return for SELECT (default: 1000)</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>Integer</td>
 * <td>No</td>
 * <td>Query timeout in seconds (default: 30)</td>
 * </tr>
 * </table>
 *
 * <h2>Output Data</h2>
 * <ul>
 * <li><strong>rows</strong> - List of row maps (SELECT only)</li>
 * <li><strong>columns</strong> - List of column names (SELECT only)</li>
 * <li><strong>rowCount</strong> - Number of rows returned or affected</li>
 * <li><strong>success</strong> - Boolean indicating success</li>
 * <li><strong>executionTime</strong> - Query execution time in
 * milliseconds</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>
 * Uses {@link PreparedStatement} with parameter bindings to prevent SQL
 * injection.
 * Only H2 JDBC URLs are accepted; other drivers are rejected.
 * </p>
 *
 * @since 1.1.0
 * @see NodeExecutor
 */
@Component
public class DatabaseExecutor implements NodeExecutor {

    private static final int DEFAULT_MAX_ROWS = 1000;
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int MAX_ALLOWED_ROWS = 10_000;

    /**
     * Default constructor.
     */
    public DatabaseExecutor() {
        // Default constructor
    }

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String jdbcUrl = getRequiredString(params, "jdbcUrl");
        String username = (String) params.getOrDefault("username", "sa");
        String password = (String) params.getOrDefault("password", "");
        String operation = ((String) params.getOrDefault("operation", "SELECT")).toUpperCase();
        String query = getRequiredString(params, "query");
        String paramString = (String) params.getOrDefault("parameters", "");
        int maxRows = Math.min(
                ((Number) params.getOrDefault("maxRows", DEFAULT_MAX_ROWS)).intValue(),
                MAX_ALLOWED_ROWS);
        int timeout = ((Number) params.getOrDefault("timeout", DEFAULT_TIMEOUT)).intValue();

        validateJdbcUrl(jdbcUrl);

        List<String> queryParams = parseParameters(paramString);

        context.getExecutionLogger().custom(context.getExecutionId().toString(),
                ExecutionLogger.LogLevel.DEBUG,
                "Database: operation=" + operation + ", url=" + sanitizeUrl(jdbcUrl),
                Map.of());

        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            Map<String, Object> output;

            if ("SELECT".equals(operation)) {
                output = executeQuery(conn, query, queryParams, maxRows, timeout, context);
            } else {
                output = executeUpdate(conn, query, queryParams, timeout, context);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            output.put("executionTime", elapsed);
            output.put("success", true);

            context.getExecutionLogger().custom(context.getExecutionId().toString(),
                    ExecutionLogger.LogLevel.DEBUG,
                    "Database: completed in " + elapsed + "ms, rowCount=" + output.get("rowCount"),
                    Map.of());

            return output;

        } catch (SQLException e) {
            throw new NodeExecutionException("Database query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getNodeType() {
        return "database";
    }

    private Map<String, Object> executeQuery(Connection conn, String sql,
            List<String> params, int maxRows, int timeout,
            ExecutionService.ExecutionContext context) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setQueryTimeout(timeout);
            stmt.setMaxRows(maxRows);
            bindParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columns.get(i - 1), rs.getObject(i));
                    }
                    rows.add(row);
                }

                Map<String, Object> output = new HashMap<>();
                output.put("rows", rows);
                output.put("columns", columns);
                output.put("rowCount", rows.size());
                return output;
            }
        }
    }

    private Map<String, Object> executeUpdate(Connection conn, String sql,
            List<String> params, int timeout,
            ExecutionService.ExecutionContext context) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setQueryTimeout(timeout);
            bindParameters(stmt, params);

            int affected = stmt.executeUpdate();

            Map<String, Object> output = new HashMap<>();
            output.put("rowCount", affected);
            return output;
        }
    }

    private void bindParameters(PreparedStatement stmt, List<String> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setString(i + 1, params.get(i));
        }
    }

    private List<String> parseParameters(String paramString) {
        if (paramString == null || paramString.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String p : paramString.split(",")) {
            result.add(p.trim());
        }
        return result;
    }

    private void validateJdbcUrl(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:h2:")) {
            throw new NodeExecutionException(
                    "Only H2 database URLs are supported. URL must start with 'jdbc:h2:'");
        }
    }

    private String sanitizeUrl(String jdbcUrl) {
        // Remove credentials from URL for logging
        int semicolonIdx = jdbcUrl.indexOf(';');
        return semicolonIdx > 0 ? jdbcUrl.substring(0, semicolonIdx) + ";..." : jdbcUrl;
    }

    private String getRequiredString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new NodeExecutionException("Required parameter '" + key + "' is missing or empty");
        }
        return value.toString();
    }
}
