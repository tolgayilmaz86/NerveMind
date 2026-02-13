package com.example;

import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.plugin.api.*;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Advanced multi-handle plugin demonstrating:
 * - Multiple action handles (transform, validate, output)
 * - Shared state across handles
 * - Event subscriptions
 * - Complex validation
 * - File operations via PluginContext
 * 
 * <h2>Handles Provided</h2>
 * <ul>
 * <li><b>data-transform</b> - Transform data using mapping rules</li>
 * <li><b>data-validate</b> - Validate data against schema</li>
 * <li><b>file-output</b> - Write data to files</li>
 * </ul>
 */
public class AdvancedDataProcessorPlugin implements PluginProvider {

    // Configuration keys
    private static final String MAPPINGS_KEY = "mappings";
    private static final String REQUIRED_FIELDS_KEY = "requiredFields";
    private static final String EMAIL_PATTERN_KEY = "emailPattern";
    private static final String OUTPUT_PATH_KEY = "outputPath";
    private static final String OUTPUT_FORMAT_KEY = "outputFormat";
    private static final String OVERWRITE_KEY = "overwrite";

    private PluginContext context;
    private final Map<String, Object> sharedState = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    @Override
    public String getId() {
        return "com.example.advanced-data-processor";
    }

    @Override
    public String getName() {
        return "Advanced Data Processor";
    }

    @Override
    public String getDescription() {
        return "Multi-handle plugin for data transformation, validation, and file output";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "NerveMind Examples";
    }

    @Override
    public List<PluginDependency> getDependencies() {
        return List.of();
    }

    @Override
    public List<EventSubscription> getEventSubscriptions() {
        return List.of(
                new EventSubscription(EventType.APPLICATION_STARTING, event -> {
                    context.getLogger().info("Data Processor plugin starting");
                    sharedState.put("startTime", Instant.now().toString());
                }),
                new EventSubscription(EventType.APPLICATION_SHUTTING_DOWN, event -> {
                    context.getLogger().info("Data Processor plugin shutting down");
                    sharedState.clear();
                }),
                new EventSubscription(EventType.WORKFLOW_COMPLETED, event -> {
                    Map<String, Object> data = event.getData();
                    if (data != null) {
                        long processed = sharedState.merge("processedCount", 1L, Long::sum);
                        context.getLogger().debug("Workflow completed, processed: {}", processed);
                    }
                }));
    }

    @Override
    public List<PluginHandle> getHandles() {
        return List.of(
                new PluginHandle(
                        "data-transform",
                        "Data Transform",
                        "Transform data using field mappings",
                        NodeCategory.ACTION,
                        null,
                        this::executeTransform,
                        this::validateTransform,
                        this::getTransformHelp,
                        this::getTransformSchema),
                new PluginHandle(
                        "data-validate",
                        "Data Validate",
                        "Validate data against schema and rules",
                        NodeCategory.ACTION,
                        null,
                        this::executeValidate,
                        this::validateValidate,
                        this::getValidateHelp,
                        this::getValidateSchema),
                new PluginHandle(
                        "file-output",
                        "File Output",
                        "Write data to files",
                        NodeCategory.ACTION,
                        null,
                        this::executeOutput,
                        this::validateOutput,
                        this::getOutputHelp,
                        this::getOutputSchema));
    }

    @Override
    public void init(PluginContext pluginContext) {
        this.context = pluginContext;
        this.running = true;
        context.getLogger().info("Advanced Data Processor plugin initialized");
    }

    @Override
    public void destroy() {
        this.running = false;
        sharedState.clear();
        context.getLogger().info("Advanced Data Processor plugin destroyed");
    }

    // ========== TRANSFORM HANDLE ==========

    private Map<String, Object> validateTransform(Map<String, Object> config) {
        Map<String, Object> errors = new LinkedHashMap<>();

        Object mappings = config.get(MAPPINGS_KEY);
        if (mappings == null || !(mappings instanceof Map)) {
            errors.put(MAPPINGS_KEY, "Mappings must be an object");
        }

        return errors;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeTransform(
            Map<String, Object> config,
            Map<String, Object> inputs,
            Map<String, Object> context) {

        Map<String, Object> mappings = (Map<String, Object>) config.get(MAPPINGS_KEY);
        Map<String, Object> output = new LinkedHashMap<>();

        context.getLogger().debug("Applying {} field mappings", mappings.size());

        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            String targetField = entry.getKey();
            String sourcePath = entry.getValue().toString();
            Object value = resolvePath(inputs, sourcePath);
            setNestedValue(output, targetField, value);
        }

        output.put("_transformed", true);
        output.put("_originalKeys", inputs.keySet().toString());

        sharedState.merge("transformCount", 1L, Long::sum);

        return output;
    }

    private String getTransformHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Data Transform Help\n");
        sb.append("====================\n\n");
        sb.append("Maps input fields to output fields using dot-notation paths.\n\n");
        sb.append("Example mappings:\n");
        sb.append("{ \"fullName\": \"user.name\", \"emailAddress\": \"contact.email\" }\n\n");
        sb.append("Input: {\"user\": {\"name\": \"John\"}, \"contact\": {\"email\": \"john@example.com\"}}\n");
        sb.append("Output: {\"fullName\": \"John\", \"emailAddress\": \"john@example.com\"}");
        return sb.toString();
    }

    private Map<String, Object> getTransformSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> mappingsSchema = new LinkedHashMap<>();
        mappingsSchema.put("type", "object");
        mappingsSchema.put("description", "Field mappings (targetField: sourcePath)");
        properties.put(MAPPINGS_KEY, mappingsSchema);

        schema.put("properties", properties);
        schema.put("required", List.of(MAPPINGS_KEY));

        return schema;
    }

    // ========== VALIDATE HANDLE ==========

    private Map<String, Object> validateValidate(Map<String, Object> config) {
        Map<String, Object> errors = new LinkedHashMap<>();

        Object requiredFields = config.get(REQUIRED_FIELDS_KEY);
        if (requiredFields != null && !(requiredFields instanceof List)) {
            errors.put(REQUIRED_FIELDS_KEY, "Required fields must be a list");
        }

        return errors;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeValidate(
            Map<String, Object> config,
            Map<String, Object> inputs,
            Map<String, Object> context) {

        Map<String, Object> output = new LinkedHashMap<>();
        List<String> requiredFields = (List<String>) config.getOrDefault(REQUIRED_FIELDS_KEY, List.of());
        String emailPattern = getString(config, EMAIL_PATTERN_KEY);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int validatedCount = 0;

        for (String field : requiredFields) {
            if (!hasNestedValue(inputs, field)) {
                errors.add("Missing required field: " + field);
            } else {
                validatedCount++;
            }
        }

        if (emailPattern != null && inputs.containsKey("email")) {
            String email = inputs.get("email").toString();
            if (!email.matches(emailPattern)) {
                errors.add("Email format invalid: " + email);
            } else {
                validatedCount++;
            }
        }

        for (Object key : inputs.keySet()) {
            Object value = inputs.get(key);
            if (value == null || value.toString().isBlank()) {
                warnings.add("Empty optional field: " + key);
            }
        }

        output.put("valid", errors.isEmpty());
        output.put("errors", errors);
        output.put("warnings", warnings);
        output.put("validatedFields", validatedCount);
        output.put("totalRequiredFields", requiredFields.size());

        this.context.getLogger().debug(
                "Validation complete: valid={}, errors={}, warnings={}",
                errors.isEmpty(), errors.size(), warnings.size());

        return output;
    }

    private String getValidateHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Data Validate Help\n");
        sb.append("==================\n\n");
        sb.append("Validates input data against required fields and patterns.\n\n");
        sb.append("Configuration:\n");
        sb.append("- requiredFields: List of field paths that must exist\n");
        sb.append("- emailPattern: Optional regex pattern for email validation\n\n");
        sb.append("Output:\n");
        sb.append("- valid: true/false\n");
        sb.append("- errors: List of validation errors\n");
        sb.append("- warnings: List of warnings");
        return sb.toString();
    }

    private Map<String, Object> getValidateSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> requiredSchema = new LinkedHashMap<>();
        requiredSchema.put("type", "array");
        requiredSchema.put("items", Map.of("type", "string"));
        requiredSchema.put("description", "List of required field paths");
        properties.put(REQUIRED_FIELDS_KEY, requiredSchema);

        Map<String, Object> emailSchema = new LinkedHashMap<>();
        emailSchema.put("type", "string");
        emailSchema.put("description", "Regex pattern for email validation");
        properties.put(EMAIL_PATTERN_KEY, emailSchema);

        schema.put("properties", properties);

        return schema;
    }

    // ========== FILE OUTPUT HANDLE ==========

    private Map<String, Object> validateOutput(Map<String, Object> config) {
        Map<String, Object> errors = new LinkedHashMap<>();

        String outputPath = getString(config, OUTPUT_PATH_KEY);
        if (outputPath == null || outputPath.isBlank()) {
            errors.put(OUTPUT_PATH_KEY, "Output path is required");
        }

        return errors;
    }

    private Map<String, Object> executeOutput(
            Map<String, Object> config,
            Map<String, Object> inputs,
            Map<String, Object> context) throws Exception {

        String outputPath = getString(config, OUTPUT_PATH_KEY);
        String format = getString(config, OUTPUT_FORMAT_KEY, "json");
        boolean overwrite = getBoolean(config, OVERWRITE_KEY, false);

        Map<String, Object> output = new LinkedHashMap<>();

        Path targetPath;
        if (outputPath.startsWith("~")) {
            targetPath = Paths.get(System.getProperty("user.home"), outputPath.substring(2));
        } else if (Paths.get(outputPath).isAbsolute()) {
            targetPath = Paths.get(outputPath);
        } else {
            targetPath = this.context.getDataDirectory().resolve(outputPath);
        }

        Files.createDirectories(targetPath.getParent());

        if (Files.exists(targetPath) && !overwrite) {
            output.put("skipped", true);
            output.put("message", "File already exists: " + targetPath);
            this.context.getLogger().warn("File already exists: {}", targetPath);
            return output;
        }

        String content;
        switch (format.toLowerCase()) {
            case "json":
                content = toJson(inputs);
                break;
            case "csv":
                content = toCsv(inputs);
                break;
            case "text":
                content = toText(inputs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Files.writeString(targetPath, content);

        output.put("written", true);
        output.put("path", targetPath.toString());
        output.put("format", format);
        output.put("bytes", content.getBytes().length);

        this.context.getLogger().info("Written {} bytes to {}", content.getBytes().length, targetPath);

        sharedState.merge("filesWritten", 1L, Long::sum);

        return output;
    }

    private String getOutputHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("File Output Help\n");
        sb.append("================\n\n");
        sb.append("Writes data to files in various formats.\n\n");
        sb.append("Configuration:\n");
        sb.append("- outputPath: Target file path\n");
        sb.append("- outputFormat: json, csv, or text\n");
        sb.append("- overwrite: true to overwrite existing files");
        return sb.toString();
    }

    private Map<String, Object> getOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> pathSchema = new LinkedHashMap<>();
        pathSchema.put("type", "string");
        pathSchema.put("description", "Output file path");
        properties.put(OUTPUT_PATH_KEY, pathSchema);

        Map<String, Object> formatSchema = new LinkedHashMap<>();
        formatSchema.put("type", "string");
        formatSchema.put("enum", List.of("json", "csv", "text"));
        formatSchema.put("default", "json");
        properties.put(OUTPUT_FORMAT_KEY, formatSchema);

        Map<String, Object> overwriteSchema = new LinkedHashMap<>();
        overwriteSchema.put("type", "boolean");
        overwriteSchema.put("default", false);
        properties.put(OVERWRITE_KEY, overwriteSchema);

        schema.put("properties", properties);
        schema.put("required", List.of(OUTPUT_PATH_KEY));

        return schema;
    }

    // ========== HELPER METHODS ==========

    private String getString(Map<String, Object> map, String key) {
        return getString(map, key, null);
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private Object resolvePath(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    private boolean hasNestedValue(Map<String, Object> root, String path) {
        return resolvePath(root, path) != null;
    }

    private void setNestedValue(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }
            Object next = current.get(part);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return;
            }
        }

        current.put(parts[parts.length - 1], value);
    }

    private String toJson(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (i > 0)
                sb.append(",\n");
            sb.append("  \"").append(escapeJson(entry.getKey())).append("\": ");
            sb.append(serializeValue(entry.getValue()));
            i++;
        }
        sb.append("\n}");
        return sb.toString();
    }

    private String toCsv(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", data.keySet())).append("\n");
        sb.append(String.join(",", data.values().stream()
                .map(v -> "\"" + escapeCsv(v.toString()) + "\"")
                .toArray(String[]::new)));
        return sb.toString();
    }

    private String toText(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String serializeValue(Object value) {
        if (value == null)
            return "null";
        if (value instanceof String)
            return "\"" + escapeJson(value.toString()) + "\"";
        if (value instanceof Map || value instanceof Collection)
            return toJson((Map<String, Object>) value);
        return value.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeCsv(String s) {
        return s.replace("\"", "\"\"");
    }
}
