package ai.nervemind.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.nervemind.common.enums.PluginType;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.plugin.api.ActionProvider;
import ai.nervemind.plugin.api.NodeCategory;
import ai.nervemind.plugin.api.NodeDescriptor;
import ai.nervemind.plugin.api.PluginProvider;
import ai.nervemind.plugin.api.PropertyDefinition;
import ai.nervemind.plugin.api.TriggerProvider;
import jakarta.annotation.PostConstruct;

/**
 * Spring-managed service that discovers and manages plugins using Java's
 * ServiceLoader mechanism.
 * 
 * <p>
 * This service is the core implementation of the NerveMind plugin system. It
 * handles the complete
 * plugin lifecycle from discovery to enabling/disabling at runtime.
 * </p>
 * 
 * <h2>Plugin Discovery Sources</h2>
 * <p>
 * Plugins are discovered from two sources during application startup:
 * </p>
 * <ol>
 * <li><strong>Classpath plugins</strong> - Bundled JAR dependencies discovered
 * via
 * {@link ServiceLoader}. These are declared in {@code build.gradle} as runtime
 * dependencies.</li>
 * <li><strong>External plugins</strong> - JAR files placed in the
 * {@code plugins/} directory,
 * loaded dynamically via {@link PluginLoader}.</li>
 * </ol>
 * <p>
 * <strong>Architecture</strong>
 * </p>
 * 
 * <pre>
 *                          ┌──────────────────────────────────┐
 *                          │         PluginService            │
 *                          │   (Spring @Service singleton)    │
 *                          └──────────────────────────────────┘
 *                                         │
 *              ┌──────────────────────────┼──────────────────────────┐
 *              │                          │                          │
 *              ▼                          ▼                          ▼
 *   ┌────────────────────┐    ┌────────────────────┐    ┌────────────────────┐
 *   │   ServiceLoader    │    │    PluginLoader    │    │  SettingsService   │
 *   │ (Classpath JARs)   │    │ (External JARs)    │    │   (Persistence)    │
 *   └────────────────────┘    └────────────────────┘    └────────────────────┘
 * </pre>
 * 
 * <p>
 * <strong>Plugin State Management</strong>
 * </p>
 * <p>
 * The service maintains two categories of plugin registries:
 * </p>
 * <ul>
 * <li><strong>Discovered plugins</strong> ({@code discoveredTriggers},
 * {@code discoveredActions})
 * - All plugins found during discovery, regardless of enabled state.</li>
 * <li><strong>Enabled plugins</strong> ({@code enabledTriggers},
 * {@code enabledActions})
 * - Only plugins that are currently enabled and available for use.</li>
 * </ul>
 * <p>
 * <strong>Thread Safety</strong>
 * </p>
 * <p>
 * This service is designed for single-threaded access from the JavaFX
 * Application Thread.
 * The internal maps are not synchronized; all UI interactions should occur on
 * the FX thread.
 * </p>
 * 
 * <p>
 * <strong>Usage Example</strong>
 * </p>
 * 
 * <pre>{@code
 * @Autowired
 * private PluginService pluginService;
 * 
 * // Get all available (enabled) triggers
 * Collection<TriggerProvider> triggers = pluginService.getTriggerProviders();
 * 
 * // Get a specific trigger's executor for workflow execution
 * Optional<NodeExecutor> executor = pluginService.getExecutor("ai.nervemind.plugin.filewatcher");
 * executor.ifPresent(exec -> exec.execute(context));
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginServiceInterface Contract interface in common module
 * @see PluginLoader External JAR loading component
 * @see TriggerProvider Interface for trigger plugins
 * @see ActionProvider Interface for action plugins
 * @see ai.nervemind.ui.view.dialog.PluginManagerController UI for managing
 *      plugins
 */
@Service
public class PluginService implements PluginServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    /**
     * Settings key prefix for storing enabled plugin IDs.
     * <p>
     * The value is stored as a comma-separated list of plugin IDs.
     * </p>
     */
    private static final String ENABLED_PLUGINS_KEY = "plugins.enabled";

    /**
     * Injected settings service for persisting enabled plugin states.
     * 
     * @see SettingsServiceInterface
     */
    private final SettingsServiceInterface settingsService;

    /**
     * Injected plugin loader for external JAR discovery.
     * 
     * @see PluginLoader
     */
    private final PluginLoader pluginLoader;

    // ========================================================================================
    // Plugin Registries
    // ========================================================================================

    /**
     * Registry of all discovered PluginProvider implementations (new unified API).
     * <p>
     * Key: plugin node type (e.g., "com.example.myplugin.mynode")
     * </p>
     * <p>
     * Value: the PluginProvider implementation
     * </p>
     */
    private final Map<String, PluginProvider> discoveredPluginProviders = new HashMap<>();

    /**
     * Registry of currently enabled PluginProvider implementations.
     * <p>
     * Updated when plugins are enabled/disabled via the UI.
     * </p>
     */
    private final Map<String, PluginProvider> enabledPluginProviders = new HashMap<>();

    /**
     * Registry of all discovered trigger plugins (enabled and disabled).
     * <p>
     * Key: plugin node type (e.g., "ai.nervemind.plugin.filewatcher")
     * </p>
     * <p>
     * Value: the TriggerProvider implementation
     * </p>
     */
    private final Map<String, TriggerProvider> discoveredTriggers = new HashMap<>();

    /**
     * Registry of all discovered action plugins (enabled and disabled).
     * <p>
     * Key: plugin node type
     * </p>
     * <p>
     * Value: the ActionProvider implementation
     * </p>
     */
    private final Map<String, ActionProvider> discoveredActions = new HashMap<>();

    /**
     * Registry of currently enabled trigger plugins only.
     * <p>
     * Updated when plugins are enabled/disabled via the UI.
     * </p>
     */
    private final Map<String, TriggerProvider> enabledTriggers = new HashMap<>();

    /**
     * Registry of currently enabled action plugins only.
     * <p>
     * Updated when plugins are enabled/disabled via the UI.
     * </p>
     */
    private final Map<String, ActionProvider> enabledActions = new HashMap<>();

    /**
     * Combined registry of all enabled nodes (triggers + actions + pluginProviders)
     * for quick lookup.
     * <p>
     * Used by {@link #getNode(String)} and {@link #getProperties(String)}.
     * </p>
     */
    private final Map<String, NodeDescriptor> enabledNodes = new HashMap<>();

    /**
     * Set of plugin IDs that are currently enabled.
     * <p>
     * This set is persisted to settings and loaded on startup.
     * </p>
     */
    private final Set<String> enabledPluginIds = new HashSet<>();

    /**
     * Constructs the PluginService with required dependencies.
     * 
     * <p>
     * This constructor is called by Spring's dependency injection. The actual
     * plugin discovery occurs in {@link #discoverPlugins()} which is annotated
     * with {@link PostConstruct}.
     * </p>
     * 
     * @param settingsService service for persisting plugin enabled states
     * @param pluginLoader    loader for external plugin JARs
     */
    public PluginService(SettingsServiceInterface settingsService, PluginLoader pluginLoader) {
        this.settingsService = settingsService;
        this.pluginLoader = pluginLoader;
    }

    /**
     * Discovers and registers all available plugins at application startup.
     * 
     * <p>
     * This method is automatically called by Spring after dependency injection
     * via the {@link PostConstruct} annotation. It performs a complete plugin
     * discovery from all sources.
     * </p>
     * 
     * <p>
     * <strong>Discovery Order</strong>
     * </p>
     * <ol>
     * <li><strong>Classpath plugins</strong> - Bundled plugins are discovered
     * first</li>
     * <li><strong>External plugins</strong> - Plugins from the {@code plugins/}
     * directory
     * are loaded and may override classpath versions</li>
     * <li><strong>Enable state</strong> - Previously enabled plugins are restored
     * from settings</li>
     * </ol>
     * 
     * <p>
     * After this method completes, use {@link #getAllDiscoveredPlugins()} to get
     * the complete list of available plugins, or {@link #getTriggerProviders()} and
     * {@link #getActionProviders()} to get only enabled plugins.
     * </p>
     * 
     * @see #reloadPlugins() To re-discover plugins at runtime
     */
    @PostConstruct
    public void discoverPlugins() {
        log.info("🔌 Discovering plugins...");

        // 1. Discover plugins from classpath (bundled with application)
        discoverClasspathPlugins();

        // 2. Discover plugins from external JAR files (via PluginLoader)
        discoverExternalPlugins();

        // 3. Load enabled plugins from settings
        loadEnabledPlugins();

        log.info("✅ Plugin discovery complete. Found {} triggers, {} actions, {} plugin providers. {} enabled.",
                discoveredTriggers.size(), discoveredActions.size(),
                discoveredPluginProviders.size(), enabledPluginIds.size());
    }

    private void discoverClasspathPlugins() {
        // Discover new unified PluginProvider implementations from classpath
        ServiceLoader<PluginProvider> pluginProviders = ServiceLoader.load(PluginProvider.class);
        for (PluginProvider provider : pluginProviders) {
            discoveredPluginProviders.put(provider.getNodeType(), provider);
            log.info("  📦 Classpath plugin: {} ({}) [{}]",
                    provider.getDisplayName(), provider.getNodeType(),
                    provider.isTrigger() ? "trigger" : "action");
        }

        // Legacy: Discover Trigger Providers from classpath
        ServiceLoader<TriggerProvider> triggers = ServiceLoader.load(TriggerProvider.class);
        for (TriggerProvider provider : triggers) {
            discoveredTriggers.put(provider.getNodeType(), provider);
            log.info("  📦 Classpath trigger: {} ({})",
                    provider.getDisplayName(), provider.getNodeType());
        }

        // Discover Action Providers from classpath
        ServiceLoader<ActionProvider> actions = ServiceLoader.load(ActionProvider.class);
        for (ActionProvider provider : actions) {
            discoveredActions.put(provider.getNodeType(), provider);
            log.info("  📦 Classpath action: {} ({})",
                    provider.getDisplayName(), provider.getNodeType());
        }
    }

    private void discoverExternalPlugins() {
        // Add PluginProvider implementations from external plugin JARs
        for (PluginProvider provider : pluginLoader.getLoadedPluginProviders()) {
            String nodeType = provider.getNodeType();
            if (discoveredPluginProviders.containsKey(nodeType)) {
                log.warn("  ⚠ External plugin {} overrides classpath version", nodeType);
            }
            discoveredPluginProviders.put(nodeType, provider);
            log.info("  📦 External plugin: {} ({}) [{}]",
                    provider.getDisplayName(), nodeType,
                    provider.isTrigger() ? "trigger" : "action");
        }

        // Legacy: Add triggers from external plugin JARs
        for (TriggerProvider provider : pluginLoader.getLoadedTriggers()) {
            String nodeType = provider.getNodeType();
            if (discoveredTriggers.containsKey(nodeType)) {
                log.warn("  ⚠ External trigger {} overrides classpath version", nodeType);
            }
            discoveredTriggers.put(nodeType, provider);
            log.info("  📦 External trigger: {} ({})",
                    provider.getDisplayName(), nodeType);
        }

        // Add actions from external plugin JARs
        for (ActionProvider provider : pluginLoader.getLoadedActions()) {
            String nodeType = provider.getNodeType();
            if (discoveredActions.containsKey(nodeType)) {
                log.warn("  ⚠ External action {} overrides classpath version", nodeType);
            }
            discoveredActions.put(nodeType, provider);
            log.info("  📦 External action: {} ({})",
                    provider.getDisplayName(), nodeType);
        }
    }

    private void loadEnabledPlugins() {
        // Get enabled plugin IDs from settings (comma-separated)
        String enabledStr = settingsService.getValue(ENABLED_PLUGINS_KEY, "");

        enabledPluginIds.clear();
        enabledPluginProviders.clear();
        enabledTriggers.clear();
        enabledActions.clear();
        enabledNodes.clear();

        if (!enabledStr.isBlank()) {
            Arrays.stream(enabledStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(enabledPluginIds::add);
        }

        // Register enabled plugins
        for (String pluginId : enabledPluginIds) {
            // Check new unified PluginProvider first
            PluginProvider pluginProvider = discoveredPluginProviders.get(pluginId);
            if (pluginProvider != null) {
                enabledPluginProviders.put(pluginId, pluginProvider);
                enabledNodes.put(pluginId, new PluginProviderAdapter(pluginProvider));
                log.info("  ✅ Enabled plugin: {} [{}]",
                        pluginProvider.getDisplayName(),
                        pluginProvider.isTrigger() ? "trigger" : "action");
                continue; // Don't also check legacy providers for same ID
            }

            // Legacy: Check TriggerProvider
            TriggerProvider trigger = discoveredTriggers.get(pluginId);
            if (trigger != null) {
                enabledTriggers.put(pluginId, trigger);
                enabledNodes.put(pluginId, trigger);
                log.info("  ✅ Enabled trigger: {}", trigger.getDisplayName());
            }

            // Legacy: Check ActionProvider
            ActionProvider action = discoveredActions.get(pluginId);
            if (action != null) {
                enabledActions.put(pluginId, action);
                enabledNodes.put(pluginId, action);
                log.info("  ✅ Enabled action: {}", action.getDisplayName());
            }
        }
    }

    @Override
    public List<PluginServiceInterface.PluginInfo> getAllDiscoveredPlugins() {
        List<PluginServiceInterface.PluginInfo> plugins = new ArrayList<>();

        // Add new unified PluginProvider implementations
        for (PluginProvider provider : discoveredPluginProviders.values()) {
            plugins.add(new PluginServiceInterface.PluginInfo(
                    provider.getNodeType(),
                    provider.getDisplayName(),
                    provider.getDescription(),
                    provider.getVersion(),
                    provider.isTrigger() ? PluginType.TRIGGER : PluginType.ACTION,
                    isPluginEnabled(provider.getNodeType()),
                    provider.getIconName(),
                    mapCategory(provider.getCategory()),
                    provider.getSubtitle(),
                    provider.getHelpText()));
        }

        // Legacy: Add TriggerProvider implementations (excluding those also in
        // PluginProvider)
        for (TriggerProvider provider : discoveredTriggers.values()) {
            if (!discoveredPluginProviders.containsKey(provider.getNodeType())) {
                plugins.add(new PluginServiceInterface.PluginInfo(
                        provider.getNodeType(),
                        provider.getDisplayName(),
                        provider.getDescription(),
                        provider.getVersion(),
                        PluginType.TRIGGER,
                        isPluginEnabled(provider.getNodeType()),
                        provider.getIconName(),
                        ai.nervemind.common.enums.NodeCategory.TRIGGER,
                        deriveSubtitle(provider.getDisplayName()),
                        provider.getDescription()));
            }
        }

        // Legacy: Add ActionProvider implementations (excluding those also in
        // PluginProvider)
        for (ActionProvider provider : discoveredActions.values()) {
            if (!discoveredPluginProviders.containsKey(provider.getNodeType())) {
                plugins.add(new PluginServiceInterface.PluginInfo(
                        provider.getNodeType(),
                        provider.getDisplayName(),
                        provider.getDescription(),
                        provider.getVersion(),
                        PluginType.ACTION,
                        isPluginEnabled(provider.getNodeType()),
                        provider.getIconName(),
                        mapCategory(provider.getCategory()),
                        deriveSubtitle(provider.getDisplayName()),
                        provider.getDescription()));
            }
        }

        return plugins;
    }

    /**
     * Map plugin API NodeCategory to common NodeCategory.
     */
    private ai.nervemind.common.enums.NodeCategory mapCategory(ai.nervemind.plugin.api.NodeCategory category) {
        if (category == null) {
            return ai.nervemind.common.enums.NodeCategory.UTILITY;
        }
        return switch (category) {
            case TRIGGER -> ai.nervemind.common.enums.NodeCategory.TRIGGER;
            case ACTION -> ai.nervemind.common.enums.NodeCategory.ACTION;
            case FLOW_CONTROL -> ai.nervemind.common.enums.NodeCategory.FLOW;
            case DATA -> ai.nervemind.common.enums.NodeCategory.DATA;
            case AI -> ai.nervemind.common.enums.NodeCategory.AI;
            case INTEGRATION -> ai.nervemind.common.enums.NodeCategory.INTEGRATION;
            case UTILITY -> ai.nervemind.common.enums.NodeCategory.UTILITY;
        };
    }

    /**
     * Derive a subtitle from a display name (last word, lowercased).
     */
    private String deriveSubtitle(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] words = displayName.split("\\s+");
        return words[words.length - 1].toLowerCase();
    }

    /**
     * Check if a plugin is enabled.
     */
    public boolean isPluginEnabled(String pluginId) {
        return enabledPluginIds.contains(pluginId);
    }

    /**
     * Enable a plugin.
     */
    public void enablePlugin(String pluginId) {
        if (!enabledPluginIds.contains(pluginId)) {
            enabledPluginIds.add(pluginId);
            saveEnabledPlugins();
            loadEnabledPlugins();
            log.info("Plugin enabled: {}", pluginId);
        }
    }

    /**
     * Disable a plugin.
     */
    public void disablePlugin(String pluginId) {
        if (enabledPluginIds.remove(pluginId)) {
            saveEnabledPlugins();
            loadEnabledPlugins();
            log.info("Plugin disabled: {}", pluginId);
        }
    }

    /**
     * Set enabled state for a plugin.
     */
    public void setPluginEnabled(String pluginId, boolean enabled) {
        if (enabled) {
            enablePlugin(pluginId);
        } else {
            disablePlugin(pluginId);
        }
    }

    private void saveEnabledPlugins() {
        String enabledStr = String.join(",", enabledPluginIds);
        settingsService.setValue(ENABLED_PLUGINS_KEY, enabledStr);
    }

    // ===== Original query methods (now return only ENABLED plugins) =====

    /**
     * Gets all enabled trigger providers.
     *
     * @return an unmodifiable collection of enabled trigger providers
     */
    public Collection<TriggerProvider> getTriggerProviders() {
        return Collections.unmodifiableCollection(enabledTriggers.values());
    }

    /**
     * Gets all enabled action providers.
     *
     * @return an unmodifiable collection of enabled action providers
     */
    public Collection<ActionProvider> getActionProviders() {
        return Collections.unmodifiableCollection(enabledActions.values());
    }

    /**
     * Gets all enabled node descriptors.
     *
     * @return an unmodifiable collection of enabled node descriptors
     */
    public Collection<NodeDescriptor> getAllNodes() {
        return Collections.unmodifiableCollection(enabledNodes.values());
    }

    /**
     * Gets the trigger provider for the specified node type.
     *
     * @param nodeType the node type to look up
     * @return an Optional containing the trigger provider if found
     */
    public Optional<TriggerProvider> getTrigger(String nodeType) {
        return Optional.ofNullable(enabledTriggers.get(nodeType));
    }

    /**
     * Gets the action provider for the specified node type.
     *
     * @param nodeType the node type to look up
     * @return an Optional containing the action provider if found
     */
    public Optional<ActionProvider> getAction(String nodeType) {
        return Optional.ofNullable(enabledActions.get(nodeType));
    }

    /**
     * Gets the node descriptor for the specified node type.
     *
     * @param nodeType the node type to look up
     * @return an Optional containing the node descriptor if found
     */
    public Optional<NodeDescriptor> getNode(String nodeType) {
        return Optional.ofNullable(enabledNodes.get(nodeType));
    }

    /**
     * Gets the executor for the specified node type.
     *
     * @param nodeType the node type to look up
     * @return an Optional containing the node executor if found
     */
    public Optional<ai.nervemind.plugin.api.NodeExecutor> getExecutor(String nodeType) {
        // Check new unified PluginProvider first
        PluginProvider pluginProvider = enabledPluginProviders.get(nodeType);
        if (pluginProvider != null) {
            return Optional.of(new PluginProviderExecutorAdapter(pluginProvider));
        }

        // Legacy: Check TriggerProvider
        TriggerProvider trigger = enabledTriggers.get(nodeType);
        if (trigger != null) {
            return Optional.of(trigger.getExecutor());
        }

        // Legacy: Check ActionProvider
        ActionProvider action = enabledActions.get(nodeType);
        if (action != null) {
            return Optional.of(action.getExecutor());
        }

        return Optional.empty();
    }

    /**
     * Gets the property definitions for the specified node type.
     *
     * @param nodeType the node type to look up
     * @return a list of property definitions for the node type
     */
    public List<PropertyDefinition> getProperties(String nodeType) {
        NodeDescriptor descriptor = enabledNodes.get(nodeType);
        if (descriptor != null) {
            return descriptor.getProperties();
        }
        return List.of();
    }

    /**
     * Checks if the specified node type requires a background service.
     *
     * @param nodeType the node type to check
     * @return true if the node type requires a background service
     */
    public boolean requiresBackgroundService(String nodeType) {
        // Check new unified PluginProvider first
        PluginProvider pluginProvider = enabledPluginProviders.get(nodeType);
        if (pluginProvider != null) {
            return pluginProvider.isTrigger() && pluginProvider.requiresBackgroundService();
        }

        // Legacy: Check TriggerProvider
        TriggerProvider trigger = enabledTriggers.get(nodeType);
        return trigger != null && trigger.requiresBackgroundService();
    }

    /**
     * Gets all node descriptors grouped by category.
     *
     * @return a map of node categories to lists of node descriptors
     */
    public Map<NodeCategory, List<NodeDescriptor>> getNodesByCategory() {
        Map<NodeCategory, List<NodeDescriptor>> result = new EnumMap<>(NodeCategory.class);

        for (NodeDescriptor node : enabledNodes.values()) {
            result.computeIfAbsent(node.getCategory(), k -> new ArrayList<>()).add(node);
        }

        return result;
    }

    /**
     * Reload all plugins (classpath + external JARs).
     * Useful after adding new plugin JARs to the plugins directory.
     */
    public void reloadPlugins() {
        log.info("Reloading plugins...");

        // Clear current state
        discoveredPluginProviders.clear();
        discoveredTriggers.clear();
        discoveredActions.clear();
        enabledPluginProviders.clear();
        enabledTriggers.clear();
        enabledActions.clear();
        enabledNodes.clear();

        // Reload external plugins
        pluginLoader.reloadPlugins();

        // Rediscover all plugins
        discoverPlugins();
    }

    /**
     * Get information about loaded plugin JARs.
     *
     * @return a list of information about loaded plugin JARs
     */
    public List<PluginLoader.PluginJarInfo> getLoadedPluginJars() {
        return pluginLoader.getLoadedPluginJars();
    }

    /**
     * Get the plugins directory path.
     *
     * @return the path to the plugins directory
     */
    public String getPluginsDirectory() {
        return pluginLoader.getPluginsDirectory();
    }

    /**
     * Get all enabled PluginProvider implementations.
     * 
     * @return unmodifiable collection of enabled plugin providers
     */
    public Collection<PluginProvider> getPluginProviders() {
        return Collections.unmodifiableCollection(enabledPluginProviders.values());
    }

    /**
     * Get a specific PluginProvider by node type.
     * 
     * @param nodeType the node type identifier
     * @return Optional containing the provider, or empty if not found
     */
    public Optional<PluginProvider> getPluginProvider(String nodeType) {
        return Optional.ofNullable(enabledPluginProviders.get(nodeType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPluginProviderInstance(String pluginId) {
        return enabledPluginProviders.get(pluginId);
    }

    /**
     * Gets the handle definitions for a node type.
     * 
     * <p>
     * If the node type is from a PluginProvider, returns its custom handle
     * definitions.
     * Otherwise, returns the default handles (1 input left, 1 output right).
     * </p>
     * 
     * @param nodeType the node type identifier
     * @return list of handle info records
     */
    @Override
    public List<HandleInfo> getHandleDefinitions(String nodeType) {
        // Check if this is a PluginProvider with custom handles
        PluginProvider provider = enabledPluginProviders.get(nodeType);
        if (provider != null) {
            List<ai.nervemind.plugin.api.HandleDefinition> handles = provider.getHandles();
            if (handles != null && !handles.isEmpty()) {
                return handles.stream()
                        .map(h -> new HandleInfo(
                                h.id(),
                                h.type() == ai.nervemind.plugin.api.HandleType.INPUT
                                        ? HandleInfo.Type.INPUT
                                        : HandleInfo.Type.OUTPUT,
                                switch (h.position()) {
                                    case LEFT -> HandleInfo.Position.LEFT;
                                    case RIGHT -> HandleInfo.Position.RIGHT;
                                    case TOP -> HandleInfo.Position.TOP;
                                    case BOTTOM -> HandleInfo.Position.BOTTOM;
                                },
                                h.label()))
                        .toList();
            }
        }

        // Check for built-in nodes with multiple handles
        return getBuiltInHandles(nodeType);
    }

    /**
     * Get built-in node handle definitions.
     * Supports multiple outputs for conditional nodes like If and Switch.
     */
    private List<HandleInfo> getBuiltInHandles(String nodeType) {
        return switch (nodeType) {
            // Conditional nodes have multiple outputs
            case "if" -> List.of(
                    new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                    new HandleInfo("true", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "True"),
                    new HandleInfo("false", HandleInfo.Type.OUTPUT, HandleInfo.Position.BOTTOM, "False"));

            case "switch" -> List.of(
                    new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                    new HandleInfo("default", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "Default"),
                    new HandleInfo("case1", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "Case 1"),
                    new HandleInfo("case2", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "Case 2"),
                    new HandleInfo("case3", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "Case 3"));

            case "tryCatch" -> List.of(
                    new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                    new HandleInfo("success", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, "Success"),
                    new HandleInfo("error", HandleInfo.Type.OUTPUT, HandleInfo.Position.BOTTOM, "Error"));

            // Merge node has multiple inputs
            case "merge" -> List.of(
                    new HandleInfo("input1", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, "Input 1"),
                    new HandleInfo("input2", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, "Input 2"),
                    new HandleInfo("out", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, null));

            // Default: standard single input/output
            default -> List.of(
                    new HandleInfo("in", HandleInfo.Type.INPUT, HandleInfo.Position.LEFT, null),
                    new HandleInfo("out", HandleInfo.Type.OUTPUT, HandleInfo.Position.RIGHT, null));
        };
    }

    /**
     * Gets a custom node view from a PluginProvider, if available.
     * 
     * @param nodeType    the node type identifier
     * @param nodeId      the instance node ID
     * @param displayName the display name of the node
     * @param settings    the node's current settings
     * @param selected    whether the node is selected
     * @param executing   whether the node is executing
     * @param error       whether the node has an error
     * @return the custom JavaFX node, or null to use default rendering
     */
    @Override
    public Object getCustomNodeView(String nodeType, String nodeId, String displayName,
            java.util.Map<String, Object> settings, boolean selected,
            boolean executing, boolean error) {
        PluginProvider provider = enabledPluginProviders.get(nodeType);
        if (provider != null) {
            ai.nervemind.plugin.api.NodeViewContext context = new ai.nervemind.plugin.api.NodeViewContext(
                    nodeId, nodeType, displayName, settings,
                    80.0, 80.0, selected, executing, error);
            return provider.createNodeView(context);
        }
        return null;
    }

    // ========================================================================================
    // UI Extension Methods
    // ========================================================================================

    /**
     * Gets all menu contributions from enabled plugins.
     * 
     * <p>
     * Aggregates menu contributions from all currently enabled PluginProvider
     * implementations. Each contribution is tagged with the source plugin ID.
     * </p>
     * 
     * @return list of menu contributions from all enabled plugins
     */
    @Override
    public List<MenuContribution> getAllMenuContributions() {
        List<MenuContribution> allContributions = new ArrayList<>();

        log.info("getAllMenuContributions: enabledPluginProviders has {} entries: {}",
                enabledPluginProviders.size(), enabledPluginProviders.keySet());

        for (PluginProvider provider : enabledPluginProviders.values()) {
            String pluginId = provider.getNodeType();
            List<ai.nervemind.plugin.api.MenuContribution> contributions = provider.getMenuContributions();

            log.info("Plugin {} has {} menu contributions", pluginId, contributions.size());
            for (ai.nervemind.plugin.api.MenuContribution c : contributions) {
                log.info("  - {} at location {}", c.label(), c.location());
            }

            for (ai.nervemind.plugin.api.MenuContribution contrib : contributions) {
                allContributions.add(convertMenuContribution(pluginId, contrib));
            }
        }

        log.info("Collected {} menu contributions from {} enabled plugins",
                allContributions.size(), enabledPluginProviders.size());
        return allContributions;
    }

    /**
     * Converts a plugin API MenuContribution to the service interface version.
     */
    private MenuContribution convertMenuContribution(String pluginId,
            ai.nervemind.plugin.api.MenuContribution apiContrib) {

        // Convert children recursively
        List<MenuContribution> children = new ArrayList<>();
        for (ai.nervemind.plugin.api.MenuContribution child : apiContrib.children()) {
            children.add(convertMenuContribution(pluginId, child));
        }

        // Convert MenuLocation enum
        MenuLocation location = convertMenuLocation(apiContrib.location());

        // Convert contextAction if present
        ContextMenuAction contextAction = null;
        if (apiContrib.contextAction() != null) {
            ai.nervemind.plugin.api.ContextMenuAction apiAction = apiContrib.contextAction();
            contextAction = apiAction::execute;
        }

        return new MenuContribution(
                pluginId,
                location,
                apiContrib.label(),
                apiContrib.iconName(),
                apiContrib.action(),
                contextAction,
                children,
                apiContrib.isSeparator());
    }

    /**
     * Converts the plugin API MenuLocation to the service interface version.
     */
    private MenuLocation convertMenuLocation(ai.nervemind.plugin.api.MenuLocation apiLocation) {
        return switch (apiLocation) {
            case FILE -> MenuLocation.FILE;
            case EDIT -> MenuLocation.EDIT;
            case WORKFLOW -> MenuLocation.WORKFLOW;
            case TOOLS -> MenuLocation.TOOLS;
            case HELP -> MenuLocation.HELP;
            case CANVAS_CONTEXT -> MenuLocation.CANVAS_CONTEXT;
            case NODE_CONTEXT -> MenuLocation.NODE_CONTEXT;
            case CONNECTION_CONTEXT -> MenuLocation.CONNECTION_CONTEXT;
        };
    }

    /**
     * Gets all side panel contributions from enabled plugins.
     * 
     * <p>
     * Aggregates side panel contributions from all currently enabled PluginProvider
     * implementations. Each contribution is tagged with the source plugin ID.
     * </p>
     * 
     * @return list of side panel contributions from all enabled plugins
     */
    @Override
    public List<SidePanelContribution> getAllSidePanelContributions() {
        List<SidePanelContribution> allPanels = new ArrayList<>();

        for (PluginProvider provider : enabledPluginProviders.values()) {
            String pluginId = provider.getNodeType();
            java.util.Optional<ai.nervemind.plugin.api.SidePanelContribution> panel = provider.getSidePanel();

            panel.ifPresent(apiPanel -> allPanels.add(convertSidePanelContribution(pluginId, apiPanel)));
        }

        log.debug("Collected {} side panel contributions from {} enabled plugins",
                allPanels.size(), enabledPluginProviders.size());
        return allPanels;
    }

    /**
     * Converts a plugin API SidePanelContribution to the service interface version.
     */
    private SidePanelContribution convertSidePanelContribution(String pluginId,
            ai.nervemind.plugin.api.SidePanelContribution apiPanel) {

        // Convert PanelPosition enum
        PanelPosition position = convertPanelPosition(apiPanel.position());

        return new SidePanelContribution(
                pluginId,
                apiPanel.id(),
                apiPanel.title(),
                apiPanel.iconName(),
                position,
                apiPanel.preferredWidth(),
                apiPanel.contentSupplier());
    }

    /**
     * Converts the plugin API PanelPosition to the service interface version.
     */
    private PanelPosition convertPanelPosition(ai.nervemind.plugin.api.PanelPosition apiPosition) {
        return switch (apiPosition) {
            case LEFT -> PanelPosition.LEFT;
            case RIGHT -> PanelPosition.RIGHT;
            case BOTTOM -> PanelPosition.BOTTOM;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyConnectionCreated(Object connectionContext) {
        if (connectionContext instanceof ai.nervemind.plugin.api.ConnectionContext ctx) {
            for (PluginProvider provider : enabledPluginProviders.values()) {
                try {
                    provider.onConnectionCreated(ctx);
                } catch (Exception e) {
                    log.warn("Plugin {} threw exception in onConnectionCreated: {}",
                            provider.getNodeType(), e.getMessage());
                }
            }
        } else {
            log.warn("notifyConnectionCreated called with invalid context type: {}",
                    connectionContext != null ? connectionContext.getClass().getName() : "null");
        }
    }

    // ========================================================================================
    // Adapter Classes
    // ========================================================================================

    /**
     * Adapter that wraps a PluginProvider to implement NodeDescriptor.
     * This allows PluginProvider instances to be stored in the enabledNodes map.
     */
    private static class PluginProviderAdapter implements NodeDescriptor {
        private final PluginProvider provider;

        PluginProviderAdapter(PluginProvider provider) {
            this.provider = provider;
        }

        @Override
        public String getNodeType() {
            return provider.getNodeType();
        }

        @Override
        public String getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public String getDescription() {
            return provider.getDescription();
        }

        @Override
        public NodeCategory getCategory() {
            return provider.getCategory();
        }

        @Override
        public List<PropertyDefinition> getProperties() {
            return provider.getProperties();
        }

        @Override
        public String getIconName() {
            return provider.getIconName();
        }

        @Override
        public String getVersion() {
            return provider.getVersion();
        }
    }

    /**
     * Adapter that wraps a PluginProvider to implement NodeExecutor.
     * This allows PluginProvider's execute() method to be called through the
     * NodeExecutor interface.
     */
    private static class PluginProviderExecutorAdapter implements ai.nervemind.plugin.api.NodeExecutor {
        private final PluginProvider provider;

        PluginProviderExecutorAdapter(PluginProvider provider) {
            this.provider = provider;
        }

        @Override
        public java.util.Map<String, Object> execute(ai.nervemind.plugin.api.ExecutionContext context)
                throws ai.nervemind.plugin.api.NodeExecutionException {
            return provider.execute(context);
        }

        @Override
        public ai.nervemind.plugin.api.ValidationResult validate(java.util.Map<String, Object> settings) {
            return provider.validate(settings);
        }

        @Override
        public void cancel() {
            provider.cancel();
        }
    }
}
