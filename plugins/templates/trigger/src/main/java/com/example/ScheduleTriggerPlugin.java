package com.example;

import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.plugin.api.*;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

/**
 * A trigger plugin that fires workflows on a configurable schedule.
 * 
 * <p>This plugin demonstrates:</p>
 * <ul>
 * <li>Creating a trigger with CRON-like scheduling</li>
 * <li>Using {@link PluginContext} for scheduling</li>
 * <li>Trigger lifecycle management</li>
 * <li>Configuration validation</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * {
 * "type": "com.example.schedule-trigger",
 * "name": "Daily Morning Trigger",
 * "config": {
 * "cronExpression": "0 8 * * *",
 * "timezone": "Europe/Amsterdam",
 * "enabled": true
 * }
 * }
 * }</pre>
 * 
 * <h2>Configuration</h2>
 * <table border="1">
 * <tr><th>Property</th><th>Type</th><th>Required</th><th>Default</th><th>Description</th></tr>
 * <tr><td>cronExpression</td><td>String</td><td>Yes</td><td>-</td><td>CRON
 * expression (minute hour day month weekday)</td></tr>
 * <tr><td>timezone</td><td>String</td><td>No</td><td>UTC</td><td>Timezone for
 * scheduling</td></tr>
 * <tr><td>enabled</td><td>Boolean</td><td>No</td><td>true</td><td>Whether the
 * trigger starts enabled</td></tr>
 * </table>
 */
public class ScheduleTriggerPlugin implements PluginProvider {

    private static final String CRON_EXPRESSION_KEY = "cronExpression";
    private static final String TIMEZONE_KEY = "timezone";
    private static final String ENABLED_KEY = "enabled";

    private PluginContext context;
    private ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> activeTriggers = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    @Override
    public String getId() {
        return "com.example.schedule-trigger";
    }

    @Override
    public String getName() {
        return "Schedule Trigger";
    }

    @Override
    public String getDescription() {
        return "Triggers workflows on a configurable schedule using CRON expressions";
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
        return List.of(); // No dependencies
    }

    @Override
    public List<EventSubscription> getEventSubscriptions() {
        // Subscribe to application shutdown to clean up
        return List.of(
                new EventSubscription(EventType.APPLICATION_SHUTTING_DOWN, event -> {
                    destroy();
                }));
    }

    @Override
    public List<PluginHandle> getHandles() {
        return List.of(
                new PluginHandle(
                        "schedule-trigger",
                        "Schedule Trigger",
                        "Fires on a configurable schedule",
                        NodeCategory.TRIGGER,
                        TriggerType.CUSTOM,
                        this::execute,
                        this::validate,
                        this::getHelpText,
                        this::getSchema));
    }

    @Override
    public void init(PluginContext pluginContext) {
        this.context = pluginContext;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.running = true;
        context.getLogger().info("Schedule Trigger plugin initialized");
    }

    @Override
    public void destroy() {
        this.running = false;

        // Cancel all active triggers
        activeTriggers.values().forEach(future -> future.cancel(false));
        activeTriggers.clear();

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        context.getLogger().info("Schedule Trigger plugin destroyed");
    }

    /**
     * Validates the trigger configuration.
     * 
     * @param config The configuration to validate
     * @return Validation result with any errors
     */
    private Map<String, Object> validate(Map<String, Object> config) {
        Map<String, Object> errors = new LinkedHashMap<>();

        String cronExpression = getString(config, CRON_EXPRESSION_KEY);
        if (cronExpression == null || cronExpression.isBlank()) {
            errors.put(CRON_EXPRESSION_KEY, "CRON expression is required");
        } else if (!isValidCron(cronExpression)) {
            errors.put(CRON_EXPRESSION_KEY, "Invalid CRON expression format");
        }

        String timezone = getString(config, TIMEZONE_KEY);
        if (timezone != null) {
            try {
                ZoneId.of(timezone);
            } catch (Exception e) {
                errors.put(TIMEZONE_KEY, "Invalid timezone: " + timezone);
            }
        }

        return errors;
    }

    /**
     * Executes the trigger logic.
     * 
     * @param config  The trigger configuration
     * @param inputs  Input context (empty for triggers)
     * @param context Execution context
     * @return Trigger result with outputs
     */
    private Map<String, Object> execute(
            Map<String, Object> config,
            Map<String, Object> inputs,
            Map<String, Object> context) {

        String cronExpression = getString(config, CRON_EXPRESSION_KEY);
        String timezone = getString(config, TIMEZONE_KEY, "UTC");
        boolean enabled = getBoolean(config, ENABLED_KEY, true);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("triggeredAt", Instant.now().toString());
        outputs.put("cronExpression", cronExpression);
        outputs.put("timezone", timezone);

        // Log trigger event
        this.context.getLogger().info(
                "Schedule trigger fired: expression={}, timezone={}",
                cronExpression, timezone);

        return outputs;
    }

    /**
     * Returns the help text for this trigger.
     */
    private String getHelpText() {
        return """
                Schedule Trigger Help
                ====================

                A trigger that fires workflows based on a CRON expression.

                CRON Format: minute hour day-of-month month day-of-week
                Example: "0 8 * * *" fires at 8:00 AM every day
                Example: "30 9 * * 1" fires at 9:30 AM every Monday

                Special Characters:
                * - any value
                , - list separator
                / - increments

                Timezones:
                Use standard IANA timezone IDs (e.g., "Europe/Amsterdam", "America/New_York")
                """;
    }

    /**
     * Returns the JSON schema for configuration.
     */
    private Map<String, Object> getSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> cronSchema = new LinkedHashMap<>();
        cronSchema.put("type", "string");
        cronSchema.put("pattern",
                "^(\\*|[0-9,\\/\\-]+)\\s+(\\*|[0-9,\\/\\-]+)\\s+(\\*|[0-9,\\/\\-]+)\\s+(\\*|[0-9,\\/\\-]+)\\s+(\\*|[0-9,\\/\\-]+)$");
        cronSchema.put("description", "CRON expression (minute hour day month weekday)");
        properties.put(CRON_EXPRESSION_KEY, cronSchema);

        Map<String, Object> tzSchema = new LinkedHashMap<>();
        tzSchema.put("type", "string");
        tzSchema.put("description", "Timezone ID");
        tzSchema.put("default", "UTC");
        properties.put(TIMEZONE_KEY, tzSchema);

        Map<String, Object> enabledSchema = new LinkedHashMap<>();
        enabledSchema.put("type", "boolean");
        enabledSchema.put("description", "Enable or disable the trigger");
        enabledSchema.put("default", true);
        properties.put(ENABLED_KEY, enabledSchema);

        schema.put("properties", properties);
        schema.put("required", List.of(CRON_EXPRESSION_KEY));

        return schema;
    }

    // Helper methods

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

    /**
     * Validates a basic CRON expression.
     * 
     * <p>
     * Note: This is a simplified validator. For production use,
     * consider using a library like quartz-scheduler.
     * </p>
     */
    private boolean isValidCron(String cron) {
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 5) {
            return false;
        }

        // Validate each part (simplified)
        for (int i = 0; i < 5; i++) {
            if (!isValidCronPart(parts[i], i)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidCronPart(String part, int field) {
        // Simplified validation - allows *, numbers, ranges, lists, increments
        return part.matches("^(\\*|[0-9]+)([\\-,\\/][0-9]+)*$");
    }
}
