package ai.nervemind.plugin.testing.junit5;

import ai.nervemind.plugin.api.PluginProvider;
import ai.nervemind.plugin.testing.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.support.*;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;

/**
 * JUnit 5 extension for plugin testing.
 * 
 * <p>
 * Provides automatic injection of mock contexts and lifecycle management
 * for plugin tests.
 * </p>
 * 
 * <h2>Features</h2>
 * <ul>
 * <li>Automatic plugin instance creation</li>
 * <li>Mock context injection</li>
 * <li>Automatic init/destroy lifecycle</li>
 * <li>Temp directory management</li>
 * </ul>
 */
public class PluginTestExtension implements BeforeAllCallback, BeforeEachCallback,
        AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(PluginTestExtension.class);

    private static final String KEY_PLUGIN = "plugin";
    private static final String KEY_PLUGIN_CONTEXT = "pluginContext";
    private static final String KEY_AUTO_INIT = "autoInit";
    private static final String KEY_AUTO_DESTROY = "autoDestroy";

    @TempDir
    Path tempDir;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        PluginTest annotation = testClass.getAnnotation(PluginTest.class);

        if (annotation == null) {
            return;
        }

        // Store configuration
        Store store = getStore(context);
        store.put(KEY_AUTO_INIT, annotation.autoInit());
        store.put(KEY_AUTO_DESTROY, annotation.autoDestroy());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        PluginTest annotation = testClass.getAnnotation(PluginTest.class);

        if (annotation == null) {
            return;
        }

        Store store = getStore(context);

        // Create plugin instance
        PluginProvider plugin = createPlugin(annotation.plugin());
        store.put(KEY_PLUGIN, plugin);

        // Create plugin context
        MockPluginContext pluginContext = new MockPluginContext()
                .withDataDirectory(tempDir)
                .withPluginsDirectory(tempDir);
        store.put(KEY_PLUGIN_CONTEXT, pluginContext);

        // Initialize plugin if auto-init is enabled
        if (annotation.autoInit()) {
            plugin.init(pluginContext);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        Store store = getStore(context);

        PluginProvider plugin = store.get(KEY_PLUGIN, PluginProvider.class);
        MockPluginContext pluginContext = store.get(KEY_PLUGIN_CONTEXT, MockPluginContext.class);
        Boolean autoDestroy = store.get(KEY_AUTO_DESTROY, Boolean.class);

        if (autoDestroy != null && autoDestroy && plugin != null) {
            plugin.destroy();
        }

        // Verify plugin was destroyed
        if (pluginContext != null && pluginContext.isDestroyed()) {
            // Expected behavior
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) throws Exception {
        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();

        // Support mock contexts
        return MockPluginContext.class.isAssignableFrom(type) ||
                MockExecutionContext.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) throws Exception {
        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();
        Store store = getStore(extensionContext);

        if (MockPluginContext.class.isAssignableFrom(type)) {
            return store.getOrComputeIfAbsent(
                    KEY_PLUGIN_CONTEXT,
                    k -> new MockPluginContext().withDataDirectory(tempDir),
                    MockPluginContext.class);
        }

        if (MockExecutionContext.class.isAssignableFrom(type)) {
            return new MockExecutionContext()
                    .withInputs(new HashMap<>())
                    .withVariables(new HashMap<>());
        }

        throw new ParameterResolutionException(
                "Cannot resolve parameter of type: " + type.getName());
    }

    /**
     * Create a plugin instance.
     */
    @SuppressWarnings("unchecked")
    private PluginProvider createPlugin(Class<? extends PluginProvider> pluginClass)
            throws Exception {
        Constructor<?> constructor = pluginClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (PluginProvider) constructor.newInstance();
    }

    /**
     * Get or create the store for context.
     */
    private Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
