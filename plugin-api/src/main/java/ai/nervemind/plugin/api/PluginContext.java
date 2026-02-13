package ai.nervemind.plugin.api;

import java.util.Optional;
import java.util.Properties;

/**
 * Context provided to plugins during their lifecycle (init/destroy).
 * 
 * <p>
 * PluginContext gives plugins access to application services, configuration,
 * other plugins, and system events. It is passed to the
 * {@link PluginProvider#init(PluginContext)}
 * method when a plugin is loaded.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * public class MyPlugin implements PluginProvider {
 *     private CredentialService credentialService;
 * 
 *     @Override
 *     public void init(PluginContext context) {
 *         // Get application services
 *         credentialService = context.getService(CredentialService.class);
 * 
 *         // Get persistent configuration
 *         Properties config = context.getPersistentConfig();
 *         String apiKey = config.getProperty("apiKey");
 * 
 *         // Get logger
 *         Logger log = context.getLogger();
 *         log.info("Plugin initialized");
 * 
 *         // Access other plugins
 *         context.getPlugin(HttpClientPlugin.class)
 *                 .ifPresent(http -> {
 *                     // Use HTTP client plugin
 *                 });
 *     }
 * }
 * }</pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#init(PluginContext)
 * @see ExecutionContext For runtime execution context
 */
public interface PluginContext {

    /**
     * Gets an application service by type.
     * 
     * <p>
     * Available services depend on the application configuration.
     * Common services include:
     * </p>
     * <ul>
     * <li>CredentialService - For accessing stored credentials</li>
     * <li>VariableService - For workflow variables</li>
     * <li>SettingsService - For application settings</li>
     * <li>WorkflowService - For workflow operations</li>
     * </ul>
     * 
     * @param <T>          the service type
     * @param serviceClass the class of the service to retrieve
     * @return the service instance, or null if not available
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Gets a logger for this plugin.
     * 
     * <p>
     * The logger is pre-configured with the plugin's node type as the name.
     * Log messages will appear in the application logs with the plugin identifier.
     * </p>
     * 
     * @return a logger instance
     */
    java.util.logging.Logger getLogger();

    /**
     * Gets another plugin instance by type.
     * 
     * <p>
     * This allows plugins to use functionality from other plugins.
     * The target plugin must be enabled and loaded.
     * </p>
     * 
     * <p>
     * <b>Example: Using another plugin</b>
     * </p>
     * 
     * <pre>{@code
     * @Override
     * public void init(PluginContext context) {
     *     Optional<HttpClientPlugin> httpPlugin = context.getPlugin(HttpClientPlugin.class);
     * 
     *     if (httpPlugin.isPresent()) {
     *         HttpClient client = httpPlugin.get().createClient();
     *         // Use the HTTP client
     *     }
     * }
     * }</pre>
     * 
     * @param <T>         the plugin type
     * @param pluginClass the class of the plugin to retrieve
     * @return an Optional containing the plugin if available, empty otherwise
     */
    <T extends PluginProvider> Optional<T> getPlugin(Class<T> pluginClass);

    /**
     * Gets the persistent configuration for this plugin.
     * 
     * <p>
     * Persistent configuration is stored across application restarts.
     * Use this for API keys, connection strings, and other settings
     * that should be remembered.
     * </p>
     * 
     * <p>
     * <b>Example: Reading configuration</b>
     * </p>
     * 
     * <pre>{@code
     * @Override
     * public void init(PluginContext context) {
     *     Properties config = context.getPersistentConfig();
     *     String apiKey = config.getProperty("apiKey");
     *     String baseUrl = config.getProperty("baseUrl", "https://api.example.com");
     * 
     *     if (apiKey == null || apiKey.isBlank()) {
     *         context.getLogger().warning("API key not configured");
     *     }
     * }
     * }</pre>
     * 
     * @return persistent configuration properties, never null
     * @see #savePersistentConfig(Properties)
     */
    Properties getPersistentConfig();

    /**
     * Saves persistent configuration for this plugin.
     * 
     * <p>
     * Changes are persisted immediately and will be available
     * after application restart.
     * </p>
     * 
     * <p>
     * <b>Example: Saving configuration</b>
     * </p>
     * 
     * <pre>{@code
     * public class MyPlugin implements PluginProvider {
     * 
     *     @Override
     *     public void init(PluginContext context) {
     *         Properties config = context.getPersistentConfig();
     * 
     *         // Update configuration
     *         config.setProperty("lastRun", java.time.Instant.now().toString());
     * 
     *         // Persist changes
     *         context.savePersistentConfig(config);
     *     }
     * }
     * }</pre>
     * 
     * @param config the configuration to save
     * @throws IllegalArgumentException if config is null
     * @see #getPersistentConfig()
     */
    void savePersistentConfig(Properties config);

    /**
     * Registers an event handler for system events.
     * 
     * <p>
     * Plugins can react to various events such as:
     * </p>
     * <ul>
     * <li>Workflow started/finished</li>
     * <li>Execution completed</li>
     * <li>Node execution events</li>
     * <li>Application lifecycle events</li>
     * </ul>
     * 
     * <p>
     * <b>Example: Listening to events</b>
     * </p>
     * 
     * <pre>{@code
     * @Override
     * public void init(PluginContext context) {
     *     context.registerEventHandler(event -> {
     *         if (event instanceof WorkflowCompletedEvent) {
     *             WorkflowCompletedEvent e = (WorkflowCompletedEvent) event;
     *             context.getLogger().info("Workflow completed: " + e.getWorkflowId());
     *         }
     *     });
     * }
     * }</pre>
     * 
     * @param handler the event handler to register
     * @see EventHandler
     * @see EventType
     */
    void registerEventHandler(EventHandler handler);

    /**
     * Gets the plugin's metadata.
     * 
     * @return plugin information
     */
    PluginInfo getPluginInfo();

    /**
     * Checks if the plugin is running in development mode.
     * 
     * @return true if in development mode, false otherwise
     */
    boolean isDevelopmentMode();
}
