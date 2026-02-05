package ai.nervemind.common.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;

/**
 * Service interface for managing application settings and user preferences.
 * 
 * <p>
 * This interface provides a type-safe API for reading and writing application
 * settings. Settings are organized by category and can be observed for changes.
 * </p>
 * 
 * <h2>Setting Categories</h2>
 * <ul>
 * <li><strong>GENERAL</strong> - Application-wide settings (language,
 * theme)</li>
 * <li><strong>EXECUTION</strong> - Workflow execution settings (timeouts,
 * retries)</li>
 * <li><strong>APPEARANCE</strong> - UI customization (colors, fonts)</li>
 * <li><strong>ADVANCED</strong> - Developer/power-user settings</li>
 * <li><strong>PLUGINS</strong> - Plugin enable/disable states</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * @Autowired
 * private SettingsServiceInterface settingsService;
 * 
 * // Read a string setting
 * String theme = settingsService.getValue("appearance.theme", "dark");
 * 
 * // Read typed settings
 * int timeout = settingsService.getInt("execution.timeout", 30);
 * boolean debug = settingsService.getBoolean("advanced.debug", false);
 * 
 * // Write a setting (persisted immediately)
 * settingsService.setValue("appearance.theme", "light");
 * 
 * // Listen for changes
 * settingsService.addChangeListener((key, oldVal, newVal) -> {
 *     if (key.equals("appearance.theme")) {
 *         applyTheme(newVal);
 *     }
 * });
 * }</pre>
 * 
 * <h2>Persistence</h2>
 * <p>
 * Settings are persisted to the database immediately when set. Default values
 * are defined in code and initialized on first startup.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see SettingDTO Data transfer object for settings
 * @see SettingCategory Categories for organizing settings
 */
public interface SettingsServiceInterface {

    /**
     * Get all settings.
     * 
     * @return list of all settings
     */
    List<SettingDTO> findAll();

    /**
     * Get all visible settings.
     * 
     * @return list of visible settings
     */
    List<SettingDTO> findAllVisible();

    /**
     * Get settings by category.
     * 
     * @param category the category to filter by
     * @return list of settings in the category
     */
    List<SettingDTO> findByCategory(SettingCategory category);

    /**
     * Get a single setting by key.
     * 
     * @param key the setting key
     * @return optional containing the setting if found
     */
    Optional<SettingDTO> findByKey(String key);

    /**
     * Get setting value by key, or default if not found.
     * 
     * @param key          the setting key
     * @param defaultValue value to return if key not fixed
     * @return the setting value or default
     */
    String getValue(String key, String defaultValue);

    /**
     * Get setting value as boolean.
     * 
     * @param key          the setting key
     * @param defaultValue value to return if key not fixed
     * @return the setting value as boolean
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Get setting value as integer.
     * 
     * @param key          the setting key
     * @param defaultValue value to return if key not fixed
     * @return the setting value as int
     */
    int getInt(String key, int defaultValue);

    /**
     * Get setting value as long.
     * 
     * @param key          the setting key
     * @param defaultValue value to return if key not fixed
     * @return the setting value as long
     */
    long getLong(String key, long defaultValue);

    /**
     * Get setting value as double.
     * 
     * @param key          the setting key
     * @param defaultValue value to return if key not fixed
     * @return the setting value as double
     */
    double getDouble(String key, double defaultValue);

    /**
     * Set a single setting value.
     * 
     * @param key   the setting key
     * @param value the new string value
     * @return the updated setting
     */
    SettingDTO setValue(String key, String value);

    /**
     * Set multiple settings at once.
     * 
     * @param settings map of keys to new values
     * @return list of updated settings
     */
    List<SettingDTO> setValues(Map<String, String> settings);

    /**
     * Reset a setting to its default value.
     * 
     * @param key the setting key to reset
     * @return the reset setting
     */
    SettingDTO resetToDefault(String key);

    /**
     * Reset all settings in a category to defaults.
     * 
     * @param category the category to reset
     * @return list of reset settings
     */
    List<SettingDTO> resetCategoryToDefaults(SettingCategory category);

    /**
     * Reset all settings to defaults.
     */
    void resetAllToDefaults();

    /**
     * Search settings by query.
     * 
     * @param query the search query
     * @return list of matching settings
     */
    List<SettingDTO> search(String query);

    /**
     * Export all settings as JSON.
     * 
     * @return JSON string containing all settings
     */
    String exportAsJson();

    /**
     * Import settings from JSON.
     * 
     * @param json JSON string containing settings to import
     */
    void importFromJson(String json);

    /**
     * Initialize default settings (run on startup).
     */
    void initializeDefaults();

    /**
     * Add a listener for setting changes.
     * 
     * @param listener the change listener to add
     */
    void addChangeListener(SettingsChangeListener listener);

    /**
     * Remove a change listener.
     * 
     * @param listener the change listener to remove
     */
    void removeChangeListener(SettingsChangeListener listener);

    /**
     * Listener interface for setting changes.
     */
    interface SettingsChangeListener {
        /**
         * Handle a setting update.
         *
         * @param key      the setting key that changed
         * @param oldValue previous value, may be null
         * @param newValue new value after change
         */
        void onSettingChanged(String key, String oldValue, String newValue);
    }
}
