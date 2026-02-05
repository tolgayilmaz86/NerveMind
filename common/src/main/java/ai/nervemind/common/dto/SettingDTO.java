package ai.nervemind.common.dto;

import java.time.Instant;

import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.enums.SettingType;

/**
 * Data Transfer Object for application settings.
 * 
 * <p>
 * Represents a single configurable setting in the application. Settings
 * are organized by category and have a specific data type for validation.
 * </p>
 * 
 * <h2>Setting Lifecycle</h2>
 * <ol>
 * <li>Settings are defined with default values at startup</li>
 * <li>Users can modify settings via the Settings UI</li>
 * <li>Changes trigger {@code SettingsChangeListener} notifications</li>
 * <li>Some settings require application restart to take effect</li>
 * </ol>
 * 
 * @param id              Unique identifier
 * @param key             Setting key (e.g., "execution.timeout")
 * @param value           Current value as string
 * @param category        Category for grouping in UI
 * @param type            Data type for validation and rendering
 * @param label           Display name in UI
 * @param description     Help text shown in UI
 * @param visible         Whether setting is visible in UI
 * @param requiresRestart Whether changes require app restart
 * @param displayOrder    Order within category
 * @param validationRules JSON validation rules (e.g., min/max for numbers)
 * @param createdAt       Creation timestamp
 * @param updatedAt       Last modification timestamp
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see SettingCategory Categories for settings
 * @see SettingType Data types for settings
 */
public record SettingDTO(
        Long id,
        String key,
        String value,
        SettingCategory category,
        SettingType type,
        String label,
        String description,
        boolean visible,
        boolean requiresRestart,
        Integer displayOrder,
        String validationRules,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * Create a minimal SettingDTO for simple key-value updates.
     * 
     * @param key   the setting key
     * @param value the setting value
     * @return a new SettingDTO instance
     */
    public static SettingDTO of(String key, String value) {
        return new SettingDTO(null, key, value, null, null, null, null, true, false, 0, null, null, null);
    }

    /**
     * Create a SettingDTO with category and type.
     * 
     * @param key      the setting key
     * @param value    the setting value
     * @param category the setting category
     * @param type     the setting type
     * @return a new SettingDTO instance
     */
    public static SettingDTO of(String key, String value, SettingCategory category, SettingType type) {
        return new SettingDTO(null, key, value, category, type, null, null, true, false, 0, null, null, null);
    }

    /**
     * Create a full SettingDTO with all metadata.
     * 
     * @param key             the setting key
     * @param value           the setting value
     * @param category        the setting category
     * @param type            the setting type
     * @param label           the display label
     * @param description     the description
     * @param visible         the visibility status
     * @param requiresRestart whether it requires restart
     * @param displayOrder    the display order
     * @param validationRules the validation rules
     * @return a new SettingDTO instance
     */
    public static SettingDTO full(
            String key,
            String value,
            SettingCategory category,
            SettingType type,
            String label,
            String description,
            boolean visible,
            boolean requiresRestart,
            int displayOrder,
            String validationRules) {
        return new SettingDTO(null, key, value, category, type, label, description, visible, requiresRestart,
                displayOrder, validationRules, null, null);
    }

    /**
     * Get the value as boolean.
     * 
     * @return the value parsed as boolean
     */
    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    /**
     * Get the value as integer.
     * 
     * @return the value parsed as an integer
     */
    public int asInt() {
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Get the value as long.
     * 
     * @return the value parsed as a long
     */
    public long asLong() {
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * Get the value as double.
     * 
     * @return the value parsed as a double
     */
    public double asDouble() {
        return value != null ? Double.parseDouble(value) : 0.0;
    }

    /**
     * Get the value as string (or default if null).
     * 
     * @param defaultValue the default value to return if value is null
     * @return the setting value or default
     */
    public String asString(String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
