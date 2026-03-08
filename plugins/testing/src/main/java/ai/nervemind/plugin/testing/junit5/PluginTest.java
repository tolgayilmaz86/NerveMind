package ai.nervemind.plugin.testing.junit5;

import ai.nervemind.plugin.api.PluginProvider;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Annotation to mark a plugin test class.
 * 
 * <p>
 * Provides automatic initialization of mock contexts and plugin instances
 * for JUnit 5 tests.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;PluginTest(plugin = HelloWorldPlugin.class, templates = { "hello-world", "weather-fetcher" })
 *     class HelloWorldPluginTest {
 * 
 *         private HelloWorldPlugin plugin;
 *         private MockPluginContext pluginContext;
 *         private MockExecutionContext executionContext;
 * 
 *         &#64;BeforeEach
 *         void setUp() {
 *             plugin = new HelloWorldPlugin();
 *             pluginContext = new MockPluginContext()
 *                     .withSetting("greeting", "Hello");
 *             plugin.init(pluginContext);
 * 
 *             executionContext = new MockExecutionContext()
 *                     .withInput("name", "World");
 *         }
 * 
 *         @Test
 *         void shouldReturnGreeting() {
 *             Map<String, Object> result = plugin.execute(
 *                     Map.of("name", "World"),
 *                     executionContext.getInputs(),
 *                     executionContext);
 * 
 *             assertThat(result.get("message")).isEqualTo("Hello, World!");
 *         }
 *     }
 * }
 * </pre>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(PluginTestExtension.class)
public @interface PluginTest {

    /**
     * The plugin class under test.
     */
    Class<? extends PluginProvider> plugin();

    /**
     * Template names to include in classpath for testing.
     */
    String[] templates() default {};

    /**
     * Whether to initialize the plugin automatically.
     */
    boolean autoInit() default true;

    /**
     * Whether to destroy the plugin after each test.
     */
    boolean autoDestroy() default true;
}
