package ai.nervemind.plugin.testing;

import ai.nervemind.plugin.api.PluginProvider;
import ai.nervemind.plugin.testing.junit5.PluginTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Sample test demonstrating the Testing Framework SDK.
 */
@PluginTest(plugin = SampleHelloWorldPlugin.class, autoInit = true, autoDestroy = true)
class HelloWorldPluginTest {

    private SampleHelloWorldPlugin plugin;
    private MockPluginContext pluginContext;
    private MockExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        executionContext = new MockExecutionContext()
                .withInput("name", "World")
                .withVariable("counter", 0);
    }

    @Test
    void shouldReturnGreeting() {
        Map<String, Object> result = plugin.execute(
                Map.of("name", "World"),
                executionContext.getInputs(),
                executionContext);

        assertThat(result).containsKey("message");
        assertThat(result.get("message")).isEqualTo("Hello, World!");
    }

    @Test
    void shouldIncludeTimestamp() {
        long before = System.currentTimeMillis();

        Map<String, Object> result = plugin.execute(
                Map.of("name", "Test"),
                executionContext.getInputs(),
                executionContext);

        long after = System.currentTimeMillis();

        assertThat(result).containsKey("timestamp");
        Object timestamp = result.get("timestamp");
        assertThat(timestamp).isInstanceOf(Long.class);

        long ts = (Long) timestamp;
        assertThat(ts).isBetween(before, after);
    }

    @Test
    void shouldLogMessage() {
        plugin.execute(
                Map.of("name", "Logger Test"),
                executionContext.getInputs(),
                executionContext);

        List<String> logs = pluginContext.getLogs();
        assertThat(logs).anyMatch(log -> log.contains("Generated greeting"));
    }

    @Test
    void shouldTrackExecution() {
        assertThat(executionContext.getExecutionCount()).isEqualTo(0);

        plugin.execute(
                Map.of("name", "Count Test"),
                executionContext.getInputs(),
                executionContext);

        assertThat(executionContext.getExecutionCount()).isEqualTo(1);
    }

    // ========== Sample Plugin for Testing ==========

    static class SampleHelloWorldPlugin implements PluginProvider {

        private MockPluginContext context;

        @Override
        public String getId() {
            return "com.example.helloworld";
        }

        @Override
        public String getName() {
            return "Hello World";
        }

        @Override
        public String getDescription() {
            return "Returns a greeting message";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public String getAuthor() {
            return "Test";
        }

        @Override
        public List<PluginHandle> getHandles() {
            return List.of(
                    new PluginHandle(
                            "hello",
                            "Say Hello",
                            "Returns a greeting",
                            ai.nervemind.common.enums.NodeCategory.UTILITY,
                            null,
                            this::execute,
                            this::validate,
                            () -> "Enter a name to greet",
                            () -> Map.of("type", "object", "properties", Map.of(
                                    "name", Map.of("type", "string", "description", "Name to greet")))));
        }

        @Override
        public void init(ai.nervemind.plugin.api.PluginContext pluginContext) {
            this.context = (MockPluginContext) pluginContext;
            this.context.getLogger().info("Hello World plugin initialized");
        }

        @Override
        public void destroy() {
            this.context.getLogger().info("Hello World plugin destroyed");
        }

        private Map<String, Object> validate(Map<String, Object> config) {
            return Map.of();
        }

        private Map<String, Object> execute(
                Map<String, Object> config,
                Map<String, Object> inputs,
                Map<String, Object> context) {

            String name = config.getOrDefault("name", "World").toString();

            Map<String, Object> result = Map.of(
                    "message", "Hello, " + name + "!",
                    "timestamp", System.currentTimeMillis());

            this.context.getLogger().info("Generated greeting for: " + name);

            return result;
        }
    }
}
