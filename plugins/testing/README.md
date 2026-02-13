# NerveMind Plugin Testing SDK

Testing utilities for NerveMind plugins.

## Installation

Add to your plugin's `build.gradle`:

```groovy
dependencies {
    testImplementation 'ai.nervemind:plugin-testing:1.0.0'
}
```

## Usage

### Basic Test

```java
import ai.nervemind.plugin.testing.*;
import ai.nervemind.plugin.testing.junit5.*;

@PluginTest(plugin = MyPlugin.class)
class MyPluginTest {
    
    private MyPlugin plugin;
    private MockPluginContext pluginContext;
    private MockExecutionContext executionContext;
    
    @BeforeEach
    void setUp() {
        plugin = new MyPlugin();
        pluginContext = new MockPluginContext()
            .withSetting("apiKey", "test-key");
        plugin.init(pluginContext);
        
        executionContext = new MockExecutionContext()
            .withInput("name", "Test");
    }
    
    @Test
    void shouldExecute() {
        Map<String, Object> result = plugin.execute(
            Map.of("name", "Test"),
            executionContext.getInputs(),
            executionContext
        );
        
        assertThat(result).containsKey("output");
    }
}
```

### MockPluginContext

Provides a controllable implementation of `PluginContext`:

```java
MockPluginContext context = new MockPluginContext()
    .withLogger(new PrintLogger())
    .withDataDirectory(tempDir)
    .withSetting("key", "value");
```

### MockExecutionContext

Provides test inputs, outputs, and variables:

```java
MockExecutionContext context = new MockExecutionContext()
    .withInput("name", "World")
    .withVariable("counter", 0)
    .withWorkflowId("test-workflow");
```

### Assertions

Fluent assertions for plugin testing:

```java
PluginAssertions.assertThat(result)
    .containsKey("message")
    .containsEntry("status", "success")
    .value("count").isInstanceOf(Number.class);
```

### Test Data Factory

Generate test data:

```java
// User data
Map<String, Object> user = TestDataFactory.createUser()
    .withName("John")
    .withEmail("john@example.com")
    .build();

// Random config
Map<String, Object> config = TestDataFactory.randomConfig()
    .withString("apiKey")
    .withNumber("timeout")
    .build();

// File content
Map<String, Object> file = TestDataFactory.file("test.json")
    .withJsonContent(data)
    .withMimeType("application/json")
    .build();
```

## Running Tests

```bash
./gradlew test
```

## Best Practices

- Use `@PluginTest` for automatic lifecycle management
- Reset contexts between tests with `context.reset()`
- Use `PluginAssertions` for readable tests
- Create reusable test data factories for complex scenarios
