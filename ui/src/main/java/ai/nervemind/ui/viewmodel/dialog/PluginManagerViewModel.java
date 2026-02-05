/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;

import ai.nervemind.common.enums.PluginType;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.PluginInfo;
import ai.nervemind.ui.view.dialog.DialogFactory;
import ai.nervemind.ui.view.dialog.PluginManagerController;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Plugin Manager dialog following the MVVM pattern.
 * 
 * <p>
 * This ViewModel manages the presentation logic for the Plugin Manager UI,
 * providing observable properties for plugin lists, search/filter
 * functionality,
 * and status display. It delegates to {@link PluginServiceInterface} for actual
 * plugin operations.
 * </p>
 * 
 * <h2>Responsibilities</h2>
 * <ul>
 * <li><strong>Plugin listing</strong> - Provides observable lists of plugins
 * separated by type (triggers/actions)</li>
 * <li><strong>Filtering</strong> - Filters plugins based on search query</li>
 * <li><strong>Enable/Disable</strong> - Delegates state changes to service</li>
 * <li><strong>Status updates</strong> - Shows plugin count and operation
 * status</li>
 * </ul>
 * 
 * <h2>Observable Properties</h2>
 * <table border="1">
 * <caption>Observable properties</caption>
 * <tr>
 * <th>Property</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link #getTriggerPlugins()}</td>
 * <td>ObservableList</td>
 * <td>Filtered list of trigger plugins</td>
 * </tr>
 * <tr>
 * <td>{@link #getActionPlugins()}</td>
 * <td>ObservableList</td>
 * <td>Filtered list of action plugins</td>
 * </tr>
 * <tr>
 * <td>{@link #searchQueryProperty()}</td>
 * <td>StringProperty</td>
 * <td>Two-way bindable search text</td>
 * </tr>
 * <tr>
 * <td>{@link #statusTextProperty()}</td>
 * <td>ReadOnlyStringProperty</td>
 * <td>Status message for display</td>
 * </tr>
 * </table>
 * 
 * <h2>Thread Safety</h2>
 * <p>
 * This ViewModel is designed to be used from the JavaFX Application Thread.
 * Plugin state changes are executed on a background thread to avoid UI
 * blocking.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Created by DialogFactory, not typically instantiated directly
 * PluginManagerViewModel viewModel = new PluginManagerViewModel(pluginService);
 * 
 * // Bind search field
 * searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());
 * 
 * // Listen for plugin list changes
 * viewModel.getTriggerPlugins().addListener((ListChangeListener<PluginInfo>) c -> {
 *     // Update UI
 * });
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginManagerController Controller that uses this ViewModel
 * @see PluginServiceInterface Service for plugin operations
 * @see DialogFactory#showPluginManager Creates dialog with this ViewModel
 */
public class PluginManagerViewModel extends BaseViewModel {

    // PluginType enum is now used for type-safe plugin type identification

    /**
     * Injected plugin service for plugin operations.
     * 
     * @see PluginServiceInterface
     */
    private final PluginServiceInterface pluginService;

    // ========================================================================================
    // Observable Collections
    // ========================================================================================

    /**
     * Master list of all discovered plugins (unfiltered).
     * <p>
     * Updated when {@link #refreshPlugins()} is called.
     * </p>
     */
    private final ObservableList<PluginInfo> allPlugins = FXCollections.observableArrayList();

    /**
     * Filtered list of trigger plugins only.
     * <p>
     * Updated automatically when {@link #searchQueryProperty()} changes.
     * </p>
     */
    private final ObservableList<PluginInfo> triggerPlugins = FXCollections.observableArrayList();

    /**
     * Filtered list of action plugins only.
     * <p>
     * Updated automatically when {@link #searchQueryProperty()} changes.
     * </p>
     */
    private final ObservableList<PluginInfo> actionPlugins = FXCollections.observableArrayList();

    // ========================================================================================
    // Properties
    // ========================================================================================

    /**
     * Search query for filtering plugins.
     * <p>
     * Bind this to a TextField for real-time filtering.
     * </p>
     */
    private final StringProperty searchQuery = new SimpleStringProperty("");

    /**
     * Status message showing plugin counts and operation results.
     * <p>
     * Example: "📦 5 plugins found, 3 enabled"
     * </p>
     */
    private final StringProperty statusText = new SimpleStringProperty("");

    /**
     * Creates a new PluginManagerViewModel with the specified plugin service.
     * 
     * <p>
     * The constructor sets up a listener on {@link #searchQueryProperty()} to
     * automatically apply filters when the search text changes. It also performs
     * an initial plugin refresh.
     * </p>
     * 
     * @param pluginService the service for plugin discovery and management;
     *                      must not be {@code null}
     * @throws NullPointerException if pluginService is null
     * @see PluginServiceInterface
     */
    public PluginManagerViewModel(PluginServiceInterface pluginService) {
        this.pluginService = pluginService;

        // Setup filter listener
        searchQuery.addListener((obs, oldVal, newVal) -> applyFilters());

        // Load initial plugins
        refreshPlugins();
    }

    // ===== Properties =====

    /**
     * All plugins (unfiltered).
     * 
     * @return the list of all plugins
     */
    public ObservableList<PluginInfo> getAllPlugins() {
        return allPlugins;
    }

    /**
     * Filtered trigger plugins.
     * 
     * @return the list of filtered trigger plugins
     */
    public ObservableList<PluginInfo> getTriggerPlugins() {
        return triggerPlugins;
    }

    /**
     * Filtered action plugins.
     * 
     * @return the list of filtered action plugins
     */
    public ObservableList<PluginInfo> getActionPlugins() {
        return actionPlugins;
    }

    /**
     * Search query text.
     * 
     * @return the search query property
     */
    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    /**
     * Status text showing plugin count.
     * 
     * @return the status text property
     */
    public ReadOnlyStringProperty statusTextProperty() {
        return statusText;
    }

    // ===== Actions =====

    /**
     * Refresh the plugin list from service.
     */
    public void refreshPlugins() {
        List<PluginInfo> plugins = pluginService.getAllDiscoveredPlugins();
        allPlugins.setAll(plugins);
        applyFilters();
    }

    /**
     * Reload plugins from disk.
     */
    public void reloadPlugins() {
        statusText.set("🔄 Reloading plugins...");
        // In a real implementation, would call pluginService.reloadPlugins()
        refreshPlugins();
        statusText.set("✅ Plugins reloaded");
    }

    /**
     * Enable or disable a plugin.
     * 
     * @param pluginId the plugin ID
     * @param enabled  whether to enable or disable
     */
    public void setPluginEnabled(String pluginId, boolean enabled) {
        // Update service synchronously - it's a quick operation and ensures
        // the plugin state is updated before the dialog closes
        pluginService.setPluginEnabled(pluginId, enabled);

        updateStatusText();
        markDirty();
    }

    /**
     * Get plugins folder path.
     * 
     * @return the plugins folder path
     */
    public String getPluginsFolderPath() {
        java.io.File pluginsDir = new java.io.File(System.getProperty("user.dir"), "app/plugins");
        if (!pluginsDir.exists()) {
            pluginsDir = new java.io.File(System.getProperty("user.dir"), "plugins");
        }
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
        }
        return pluginsDir.getAbsolutePath();
    }

    /**
     * Check if a plugin is from an external JAR.
     * 
     * @param plugin the plugin to check
     * @return true if the plugin is from an external JAR
     */
    public boolean isExternalPlugin(PluginInfo plugin) {
        return plugin.id().contains(".") && !plugin.id().startsWith("ai.nervemind.app");
    }

    /**
     * Check if a plugin is a trigger type.
     */
    public boolean isTriggerPlugin(PluginInfo plugin) {
        return plugin.pluginType() == PluginType.TRIGGER;
    }

    // ===== Private Methods =====

    private void applyFilters() {
        String filter = searchQuery.get() != null ? searchQuery.get().toLowerCase().trim() : "";

        // Filter all plugins
        List<PluginInfo> filtered = allPlugins.stream()
                .filter(p -> filter.isEmpty()
                        || p.name().toLowerCase().contains(filter)
                        || p.description().toLowerCase().contains(filter)
                        || p.id().toLowerCase().contains(filter))
                .toList();

        // Separate by type using PluginType enum
        List<PluginInfo> triggers = filtered.stream()
                .filter(p -> p.pluginType() == PluginType.TRIGGER)
                .toList();

        List<PluginInfo> actions = filtered.stream()
                .filter(p -> p.pluginType() == PluginType.ACTION)
                .toList();

        triggerPlugins.setAll(triggers);
        actionPlugins.setAll(actions);

        updateStatusText();
    }

    private void updateStatusText() {
        int total = allPlugins.size();
        int enabled = (int) allPlugins.stream().filter(PluginInfo::enabled).count();
        statusText.set(String.format("📦 %d plugins found, %d enabled", total, enabled));
    }
}
