package ai.nervemind.plugin.testing;

import ai.nervemind.plugin.api.*;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Mock implementation of {@link PluginContext} for testing.
 * 
 * <p>
 * Provides controllable implementations of all PluginContext methods
 * for use in unit and integration tests.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * MockPluginContext context = new MockPluginContext()
 *         .withLogger(new PrintLogger())
 *         .withDataDirectory(tempDir)
 *         .withSetting("apiKey", "test-key");
 * 
 * // Initialize plugin
 * plugin.init(context);
 * 
 * // Verify interactions
 * assertThat(context.getLogs()).isNotEmpty();
 * }</pre>
 */
public class MockPluginContext implements PluginContext {

    private PluginLogger logger = new PrintLogger();
    private Path dataDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    private Path pluginsDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
    private String instanceId = UUID.randomUUID().toString();
    private Instant startTime = Instant.now();
    private volatile boolean destroyed = false;

    private final Map<String, Object> settings = new ConcurrentHashMap<>();
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Event>> eventHandlers = new ArrayList<>();

    // Event recording
    private final List<Event> publishedEvents = Collections.synchronizedList(new ArrayList<>());

    public MockPluginContext() {
        // Default initialization
    }

    // ========== Builder Methods ==========

    /**
     * Set the logger.
     */
    public MockPluginContext withLogger(PluginLogger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Set the data directory.
     */
    public MockPluginContext withDataDirectory(Path path) {
        this.dataDirectory = path;
        return this;
    }

    /**
     * Set the plugins directory.
     */
    public MockPluginContext withPluginsDirectory(Path path) {
        this.pluginsDirectory = path;
        return this;
    }

    /**
     * Add a setting.
     */
    public MockPluginContext withSetting(String key, Object value) {
        this.settings.put(key, value);
        return this;
    }

    /**
     * Add multiple settings.
     */
    public MockPluginContext withSettings(Map<String, Object> settings) {
        this.settings.putAll(settings);
        return this;
    }

    /**
     * Clear all settings.
     */
    public MockPluginContext clearSettings() {
        this.settings.clear();
        return this;
    }

    // ========== PluginContext Implementation ==========

    @Override
    public PluginLogger getLogger() {
        return logger;
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Object getSetting(String key) {
        return settings.get(key);
    }

    @Override
    public <T> T getSetting(String key, Class<T> type) {
        Object value = settings.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    public String getSetting(String key, String defaultValue) {
        Object value = settings.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    @Override
    public void removeSetting(String key) {
        settings.remove(key);
    }

    @Override
    public boolean isSettingDefined(String key) {
        return settings.containsKey(key);
    }

    @Override
    public Map<String, Object> getAllSettings() {
        return new HashMap<>(settings);
    }

    @Override
    public void publishEvent(Event event) {
        publishedEvents.add(event);
        for (Consumer<Event> handler : eventHandlers) {
            handler.accept(event);
        }
    }

    @Override
    public void subscribeToEvents(Consumer<Event> handler) {
        eventHandlers.add(handler);
    }

    @Override
    public void destroy() {
        this.destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    // ========== Test Helper Methods ==========

    /**
     * Get all logged messages.
     */
    public List<String> getLogs() {
        if (logger instanceof PrintLogger) {
            return ((PrintLogger) logger).getLogs();
        }
        return new ArrayList<>();
    }

    /**
     * Clear all logs.
     */
    public MockPluginContext clearLogs() {
        if (logger instanceof PrintLogger) {
            ((PrintLogger) logger).clear();
        }
        return this;
    }

    /**
     * Get all published events.
     */
    public List<Event> getPublishedEvents() {
        return new ArrayList<>(publishedEvents);
    }

    /**
     * Clear all published events.
     */
    public MockPluginContext clearEvents() {
        publishedEvents.clear();
        return this;
    }

    /**
     * Check if a specific event was published.
     */
    public boolean hasPublishedEvent(EventType type) {
        return publishedEvents.stream()
                .anyMatch(e -> e.getType() == type);
    }

    /**
     * Get the first event of a specific type.
     */
    public Optional<Event> findFirstEvent(EventType type) {
        return publishedEvents.stream()
                .filter(e -> e.getType() == type)
                .findFirst();
    }

    /**
     * Check if plugin was destroyed.
     */
    public boolean wasDestroyed() {
        return destroyed;
    }

    /**
     * Reset the mock context for reuse.
     */
    public MockPluginContext reset() {
        destroyed = false;
        settings.clear();
        publishedEvents.clear();
        clearLogs();
        return this;
    }

    // ========== Internal Classes ==========

    /**
     * Simple print logger that captures logs for testing.
     */
    public static class PrintLogger implements PluginLogger {
        private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
        private volatile String lastMessage;
        private volatile String lastLevel;

        @Override
        public void trace(String message) {
            log("TRACE", message);
        }

        @Override
        public void trace(String format, Object... args) {
            log("TRACE", String.format(format, args));
        }

        @Override
        public void debug(String message) {
            log("DEBUG", message);
        }

        @Override
        public void debug(String format, Object... args) {
            log("DEBUG", String.format(format, args));
        }

        @Override
        public void info(String message) {
            log("INFO", message);
        }

        @Override
        public void info(String format, Object... args) {
            log("INFO", String.format(format, args));
        }

        @Override
        public void warn(String message) {
            log("WARN", message);
        }

        @Override
        public void warn(String format, Object... args) {
            log("WARN", String.format(format, args));
        }

        @Override
        public void error(String message) {
            log("ERROR", message);
        }

        @Override
        public void error(String format, Object... args) {
            log("ERROR", String.format(format, args));
        }

        @Override
        public void error(String message, Throwable throwable) {
            log("ERROR", message + " - " + throwable.getMessage());
        }

        private void log(String level, String message) {
            lastLevel = level;
            lastMessage = message;
            logs.add(level + ": " + message);
            System.out.println("[" + level + "] " + message);
        }

        public List<String> getLogs() {
            return new ArrayList<>(logs);
        }

        public void clear() {
            logs.clear();
            lastMessage = null;
            lastLevel = null;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public String getLastLevel() {
            return lastLevel;
        }
    }
}
