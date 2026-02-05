package ai.nervemind.app.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.nervemind.plugin.api.ActionProvider;
import ai.nervemind.plugin.api.PluginProvider;
import ai.nervemind.plugin.api.TriggerProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Loads plugins from JAR files in the plugins directory.
 * 
 * <p>
 * Plugins are discovered using Java's ServiceLoader mechanism. Each plugin JAR
 * must contain a META-INF/services file that lists the provider
 * implementations.
 * 
 * <p>
 * Plugin developers should:
 * <ol>
 * <li>Add dependency on nervemind-plugin-api</li>
 * <li>Implement TriggerProvider or ActionProvider</li>
 * <li>Create META-INF/services/ai.nervemind.plugin.api.TriggerProvider (or
 * ActionProvider)</li>
 * <li>Build a JAR and drop it in the plugins/ directory</li>
 * </ol>
 */
@Component
public class PluginLoader {

    /**
     * Default constructor.
     */
    public PluginLoader() {
        // Default constructor
    }

    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    @Value("${nervemind.plugins.directory:plugins}")
    private String pluginsDirectory;

    @Value("${nervemind.plugins.enabled:true}")
    private boolean pluginsEnabled;

    private final List<URLClassLoader> pluginClassLoaders = new ArrayList<>();
    private final List<PluginJarInfo> loadedPluginJars = new ArrayList<>();
    private final List<TriggerProvider> loadedTriggers = new ArrayList<>();
    private final List<ActionProvider> loadedActions = new ArrayList<>();
    private final List<PluginProvider> loadedPluginProviders = new ArrayList<>();

    /**
     * Initializes the plugin loader after construction.
     * Loads all available plugins if plugin loading is enabled.
     */
    @PostConstruct
    public void initialize() {
        if (!pluginsEnabled) {
            log.info("Plugin loading is disabled via configuration");
            return;
        }
        loadAllPlugins();
    }

    /**
     * Cleans up plugin resources before shutdown.
     * Closes all plugin class loaders.
     */
    @PreDestroy
    public void cleanup() {
        for (URLClassLoader classLoader : pluginClassLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close plugin class loader", e);
            }
        }
        pluginClassLoaders.clear();
        loadedPluginJars.clear();
        loadedTriggers.clear();
        loadedActions.clear();
        loadedPluginProviders.clear();
    }

    /**
     * Load all plugins from the plugins directory.
     */
    public void loadAllPlugins() {
        Path pluginsPath = Path.of(pluginsDirectory);

        // Create plugins directory if it doesn't exist
        if (!Files.exists(pluginsPath)) {
            try {
                Files.createDirectories(pluginsPath);
                log.info("Created plugins directory: {}", pluginsPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to create plugins directory: {}", pluginsPath, e);
                return;
            }
        }

        if (!Files.isDirectory(pluginsPath)) {
            log.warn("Plugins path is not a directory: {}", pluginsPath);
            return;
        }

        log.info("🔌 Loading plugins from: {}", pluginsPath.toAbsolutePath());

        // Find all JAR files in the plugins directory
        List<Path> jarFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsPath, "*.jar")) {
            for (Path jar : stream) {
                jarFiles.add(jar);
            }
        } catch (IOException e) {
            log.error("Failed to scan plugins directory", e);
            return;
        }

        if (jarFiles.isEmpty()) {
            log.info("No plugin JARs found in {}", pluginsPath);
            return;
        }

        // Load each plugin JAR
        for (Path jarPath : jarFiles) {
            loadPluginJar(jarPath);
        }

        log.info("✅ Plugin loading complete. Loaded {} triggers, {} actions from {} JARs",
                loadedTriggers.size(), loadedActions.size(), loadedPluginJars.size());
    }

    /**
     * Load a single plugin JAR file.
     */
    private void loadPluginJar(Path jarPath) {
        log.info("  📦 Loading plugin: {}", jarPath.getFileName());

        try {
            // Read plugin metadata from JAR manifest
            PluginJarInfo jarInfo = readPluginManifest(jarPath);

            // Create a class loader for this plugin
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    getClass().getClassLoader());
            pluginClassLoaders.add(classLoader);

            // Use ServiceLoader to discover providers in this JAR
            int triggersFound = 0;
            int actionsFound = 0;
            int pluginProvidersFound = 0;

            // Load new unified PluginProvider implementations
            ServiceLoader<PluginProvider> pluginProviders = ServiceLoader.load(
                    PluginProvider.class, classLoader);
            for (PluginProvider provider : pluginProviders) {
                loadedPluginProviders.add(provider);
                pluginProvidersFound++;
                log.info("     ✓ Plugin: {} ({}) [{}]",
                        provider.getDisplayName(), provider.getNodeType(),
                        provider.isTrigger() ? "trigger" : "action");
            }

            // Legacy: Load TriggerProvider implementations
            ServiceLoader<TriggerProvider> triggers = ServiceLoader.load(
                    TriggerProvider.class, classLoader);
            for (TriggerProvider provider : triggers) {
                loadedTriggers.add(provider);
                triggersFound++;
                log.info("     ✓ Trigger: {} ({})",
                        provider.getDisplayName(), provider.getNodeType());
            }

            ServiceLoader<ActionProvider> actions = ServiceLoader.load(
                    ActionProvider.class, classLoader);
            for (ActionProvider provider : actions) {
                loadedActions.add(provider);
                actionsFound++;
                log.info("     ✓ Action: {} ({})",
                        provider.getDisplayName(), provider.getNodeType());
            }

            if (triggersFound == 0 && actionsFound == 0 && pluginProvidersFound == 0) {
                log.warn("     ⚠ No providers found in JAR. Check META-INF/services files.");
            }

            jarInfo = new PluginJarInfo(
                    jarInfo.pluginId(),
                    jarInfo.pluginName(),
                    jarInfo.pluginVersion(),
                    jarInfo.description(),
                    jarPath,
                    triggersFound,
                    actionsFound,
                    pluginProvidersFound);
            loadedPluginJars.add(jarInfo);

        } catch (Exception e) {
            log.error("     ❌ Failed to load plugin: {}", jarPath.getFileName(), e);
        }
    }

    /**
     * Read plugin metadata from JAR manifest.
     */
    private PluginJarInfo readPluginManifest(Path jarPath) {
        String pluginId = jarPath.getFileName().toString().replace(".jar", "");
        String pluginName = pluginId;
        String pluginVersion = "unknown";
        String description = "";

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                pluginId = attrs.getValue("Plugin-Id");
                if (pluginId == null) {
                    pluginId = jarPath.getFileName().toString().replace(".jar", "");
                }
                pluginName = attrs.getValue("Plugin-Name");
                if (pluginName == null) {
                    pluginName = pluginId;
                }
                pluginVersion = attrs.getValue("Plugin-Version");
                if (pluginVersion == null) {
                    pluginVersion = "1.0.0";
                }
                description = attrs.getValue("Plugin-Description");
                if (description == null) {
                    description = "";
                }
            }
        } catch (IOException _) {
            log.warn("Could not read manifest from {}", jarPath.getFileName());
        }

        return new PluginJarInfo(pluginId, pluginName, pluginVersion, description, jarPath, 0, 0, 0);
    }

    /**
     * Reload all plugins (useful for development).
     */
    public void reloadPlugins() {
        cleanup();
        loadAllPlugins();
    }

    // ===== Getters =====

    /**
     * Returns an unmodifiable list of all loaded plugin providers.
     *
     * @return list of loaded plugin providers
     */
    public List<PluginProvider> getLoadedPluginProviders() {
        return List.copyOf(loadedPluginProviders);
    }

    /**
     * Returns an unmodifiable list of all loaded trigger providers.
     *
     * @return list of loaded trigger providers
     */
    public List<TriggerProvider> getLoadedTriggers() {
        return List.copyOf(loadedTriggers);
    }

    /**
     * Returns an unmodifiable list of all loaded action providers.
     *
     * @return list of loaded action providers
     */
    public List<ActionProvider> getLoadedActions() {
        return List.copyOf(loadedActions);
    }

    /**
     * Returns an unmodifiable list of information about loaded plugin JARs.
     *
     * @return list of loaded plugin JAR information
     */
    public List<PluginJarInfo> getLoadedPluginJars() {
        return List.copyOf(loadedPluginJars);
    }

    /**
     * Returns whether plugin loading is enabled.
     *
     * @return true if plugins are enabled, false otherwise
     */
    public boolean isPluginsEnabled() {
        return pluginsEnabled;
    }

    /**
     * Returns the directory path where plugins are loaded from.
     *
     * @return the plugins directory path
     */
    public String getPluginsDirectory() {
        return pluginsDirectory;
    }

    /**
     * Information about a loaded plugin JAR.
     * 
     * @param pluginId            unique identifier of the plugin
     * @param pluginName          display name of the plugin
     * @param pluginVersion       version string
     * @param description         plugin description
     * @param jarPath             path to the JAR file
     * @param triggerCount        number of triggers found in JAR
     * @param actionCount         number of actions found in JAR
     * @param pluginProviderCount number of unified plugin providers found in JAR
     */
    public record PluginJarInfo(
            String pluginId,
            String pluginName,
            String pluginVersion,
            String description,
            Path jarPath,
            int triggerCount,
            int actionCount,
            int pluginProviderCount) {
    }
}
