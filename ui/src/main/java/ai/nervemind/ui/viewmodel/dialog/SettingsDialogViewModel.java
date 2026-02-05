/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;
import java.util.Map;

import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * ViewModel for the Settings dialog.
 * 
 * <p>
 * Manages settings categories, loading settings, and tracking pending changes.
 * Does not depend on any JavaFX UI classes - only uses javafx.beans and
 * javafx.collections.
 */
public class SettingsDialogViewModel extends BaseViewModel {

    private final SettingsServiceInterface settingsService;
    private final PluginServiceInterface pluginService;

    private final ObservableList<CategoryItem> categories = FXCollections.observableArrayList();
    private final ObjectProperty<CategoryItem> selectedCategory = new SimpleObjectProperty<>();
    private final ObservableList<SettingDTO> currentSettings = FXCollections.observableArrayList();
    private final ObservableMap<String, String> pendingChanges = FXCollections.observableHashMap();
    private final BooleanProperty hasChanges = new SimpleBooleanProperty(false);

    /**
     * Creates a new SettingsDialogViewModel.
     * 
     * @param settingsService the service for settings operations
     * @param pluginService   the service for plugin operations (can be null)
     */
    public SettingsDialogViewModel(SettingsServiceInterface settingsService,
            PluginServiceInterface pluginService) {
        this.settingsService = settingsService;
        this.pluginService = pluginService;

        initializeCategories();

        // Track changes
        pendingChanges.addListener((javafx.collections.MapChangeListener<String, String>) change -> {
            hasChanges.set(!pendingChanges.isEmpty());
            markDirty();
        });

        // Load settings when category changes
        selectedCategory.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadSettingsForCategory(newVal.category());
            }
        });
    }

    private void initializeCategories() {
        categories.addAll(
                new CategoryItem("General", "mdi2c-cog", SettingCategory.GENERAL,
                        "Application behavior and appearance"),
                new CategoryItem("Editor", "mdi2p-pencil", SettingCategory.EDITOR,
                        "Canvas and workflow editor settings"),
                new CategoryItem("Execution", "mdi2p-play-circle", SettingCategory.EXECUTION,
                        "Workflow execution parameters"),
                new CategoryItem("AI Providers", "mdi2r-robot", SettingCategory.AI_PROVIDERS,
                        "Configure AI service connections"),
                new CategoryItem("HTTP & Network", "mdi2w-web", SettingCategory.HTTP_NETWORK,
                        "HTTP client and proxy settings"),
                new CategoryItem("Database & Storage", "mdi2d-database", SettingCategory.DATABASE_STORAGE,
                        "Data persistence and backups"),
                new CategoryItem("Webhook & Server", "mdi2s-server", SettingCategory.WEBHOOK_SERVER,
                        "HTTP server and webhook configuration"),
                new CategoryItem("Notifications", "mdi2b-bell", SettingCategory.NOTIFICATIONS,
                        "Alerts and notification preferences"),
                new CategoryItem("Python Scripting", "mdi2l-language-python", SettingCategory.PYTHON,
                        "Python execution mode and interpreter settings"),
                new CategoryItem("Plugins", "mdi2p-puzzle", SettingCategory.PLUGINS,
                        "Plugin management and enabled plugins"),
                new CategoryItem("Advanced", "mdi2c-code-tags", SettingCategory.ADVANCED,
                        "Developer options and diagnostics"));
    }

    // ===== Properties =====

    /**
     * All available setting categories.
     */
    public ObservableList<CategoryItem> getCategories() {
        return categories;
    }

    /**
     * Currently selected category.
     */
    public ObjectProperty<CategoryItem> selectedCategoryProperty() {
        return selectedCategory;
    }

    public CategoryItem getSelectedCategory() {
        return selectedCategory.get();
    }

    public void setSelectedCategory(CategoryItem category) {
        selectedCategory.set(category);
    }

    /**
     * Settings for the currently selected category.
     */
    public ObservableList<SettingDTO> getCurrentSettings() {
        return currentSettings;
    }

    /**
     * Map of pending changes (key -> new value).
     */
    public ObservableMap<String, String> getPendingChanges() {
        return pendingChanges;
    }

    /**
     * Whether there are unsaved changes.
     */
    public BooleanProperty hasChangesProperty() {
        return hasChanges;
    }

    public boolean hasChanges() {
        return hasChanges.get();
    }

    // ===== Actions =====

    /**
     * Load settings for a specific category.
     * 
     * @param category the category to load
     */
    public void loadSettingsForCategory(SettingCategory category) {
        setLoading(true);
        try {
            List<SettingDTO> settings = settingsService.findByCategory(category);
            // Filter to only visible settings
            List<SettingDTO> visibleSettings = settings.stream()
                    .filter(SettingDTO::visible)
                    .toList();
            currentSettings.setAll(visibleSettings);
            clearError();
        } catch (Exception e) {
            setErrorMessage("Failed to load settings: " + e.getMessage());
        } finally {
            setLoading(false);
        }
    }

    /**
     * Record a setting change.
     * 
     * @param key   the setting key
     * @param value the new value
     */
    public void recordChange(String key, String value) {
        pendingChanges.put(key, value);
    }

    /**
     * Save all pending changes.
     * 
     * @return true if all changes were saved successfully
     */
    public boolean saveChanges() {
        if (pendingChanges.isEmpty()) {
            return true;
        }

        setLoading(true);
        try {
            for (Map.Entry<String, String> entry : pendingChanges.entrySet()) {
                settingsService.setValue(entry.getKey(), entry.getValue());
            }
            pendingChanges.clear();
            clearDirty();
            clearError();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to save settings: " + e.getMessage());
            return false;
        } finally {
            setLoading(false);
        }
    }

    /**
     * Reset a setting to its default value.
     * 
     * @param key the setting key
     * @return true if successful
     */
    public boolean resetSetting(String key) {
        try {
            settingsService.resetToDefault(key);
            // Reload current category
            CategoryItem current = selectedCategory.get();
            if (current != null) {
                loadSettingsForCategory(current.category());
            }
            pendingChanges.remove(key);
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to reset setting: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reset all settings in the current category to defaults.
     * 
     * @return true if successful
     */
    public boolean resetCategoryToDefaults() {
        CategoryItem current = selectedCategory.get();
        if (current == null) {
            return false;
        }

        try {
            for (SettingDTO setting : currentSettings) {
                settingsService.resetToDefault(setting.key());
            }
            // Reload
            loadSettingsForCategory(current.category());
            // Clear pending changes for this category
            for (SettingDTO setting : currentSettings) {
                pendingChanges.remove(setting.key());
            }
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to reset settings: " + e.getMessage());
            return false;
        }
    }

    // ===== Plugin Management =====

    /**
     * Gets all discovered plugins with their current enabled state.
     * 
     * @return list of plugin info, or empty list if plugin service is not available
     */
    public List<PluginServiceInterface.PluginInfo> getAllDiscoveredPlugins() {
        if (pluginService == null) {
            return List.of();
        }
        return pluginService.getAllDiscoveredPlugins();
    }

    /**
     * Enable or disable a plugin.
     * 
     * @param nodeType the plugin node type identifier
     * @param enabled  true to enable, false to disable
     */
    public void setPluginEnabled(String nodeType, boolean enabled) {
        if (pluginService == null) {
            return;
        }
        try {
            if (enabled) {
                pluginService.enablePlugin(nodeType);
            } else {
                pluginService.disablePlugin(nodeType);
            }
            markDirty();
        } catch (Exception e) {
            setErrorMessage("Failed to " + (enabled ? "enable" : "disable") + " plugin: " + e.getMessage());
        }
    }

    /**
     * Check if a plugin is enabled.
     * 
     * @param nodeType the plugin node type identifier
     * @return true if enabled
     */
    public boolean isPluginEnabled(String nodeType) {
        if (pluginService == null) {
            return false;
        }
        return pluginService.isPluginEnabled(nodeType);
    }

    /**
     * Discard all pending changes.
     */
    public void discardChanges() {
        pendingChanges.clear();
        clearDirty();
        // Reload current category to restore original values
        CategoryItem current = selectedCategory.get();
        if (current != null) {
            loadSettingsForCategory(current.category());
        }
    }

    /**
     * Get the current value for a setting (pending or original).
     * 
     * @param setting the setting
     * @return the current value
     */
    public String getCurrentValue(SettingDTO setting) {
        if (pendingChanges.containsKey(setting.key())) {
            return pendingChanges.get(setting.key());
        }
        return setting.value();
    }

    // ===== Static Helpers =====

    /**
     * Format a setting key into a human-readable label.
     * 
     * @param key the setting key (e.g., "ai.openai.api_key")
     * @return formatted label (e.g., "Api Key")
     */
    public static String formatKeyToLabel(String key) {
        String[] parts = key.split("\\.");
        String lastPart = parts[parts.length - 1];
        // Convert snake_case to Title Case
        String[] words = lastPart.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Extract section name from a setting key.
     * 
     * @param key the setting key (e.g., "ai.openai.api_key")
     * @return section name (e.g., "openai")
     */
    public static String extractSection(String key) {
        String[] parts = key.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "general";
    }

    /**
     * Format section key into display name.
     * 
     * @param sectionKey the section key
     * @return formatted name
     */
    public static String formatSectionName(String sectionKey) {
        return switch (sectionKey.toLowerCase()) {
            case "openai" -> "OpenAI";
            case "anthropic" -> "Anthropic";
            case "ollama" -> "Ollama";
            case "azure" -> "Azure OpenAI";
            case "google" -> "Google Gemini";
            case "smtp" -> "SMTP Settings";
            default -> {
                String result = sectionKey.substring(0, 1).toUpperCase() +
                        sectionKey.substring(1).replace("_", " ");
                yield result;
            }
        };
    }

    // ===== Inner Classes =====

    /**
     * Represents a settings category for the sidebar.
     *
     * @param name        Display name of the category
     * @param iconLiteral Ikonli literal for the category icon
     * @param category    The underlying enum category
     * @param description Brief explanation of what settings are in this category
     */
    public record CategoryItem(
            String name,
            String iconLiteral,
            SettingCategory category,
            String description) {
    }
}
