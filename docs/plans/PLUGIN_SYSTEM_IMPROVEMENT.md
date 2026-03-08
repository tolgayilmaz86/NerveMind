# NerveMind Plugin System Enhancement Plan

## Executive Summary

This plan prioritizes **developer experience** to encourage third-party plugin development. Before building a marketplace, we need:
1. **Comprehensive plugin API documentation**
2. **Step-by-step guides**
3. **Plugin templates/samples**
4. **CLI tools for developers**
5. **Testing framework**

---

## Phase 1: Plugin API Enhancement & Documentation [Priority: HIGHEST]

### 1.1 Complete Plugin API Documentation

**Location:** `docs/plugins/`

**Files to Create:**
```
docs/plugins/
├── README.md                          # Getting started guide
├── architecture.md                    # Plugin architecture overview
├── api/
│   ├── PluginProvider.md             # Complete interface docs
│   ├── ExecutionContext.md           # Runtime context docs
│   ├── PropertyDefinition.md         # Property system docs
│   ├── HandleDefinition.md           # Custom handles docs
│   ├── NodeExecutor.md               # Execution docs
│   └── ExecutionContext.md           # Context API docs
├── guides/
│   ├── creating-your-first-plugin.md
│   ├── defining-properties.md
│   ├── custom-handles.md
│   ├── handling-errors.md
│   ├── testing-plugins.md
│   ├── debugging-tips.md
│   └── best-practices.md
├── examples/
│   ├── simple-action-plugin.md
│   ├── trigger-plugin.md
│   ├── multi-handle-plugin.md
│   └── custom-ui-plugin.md
└── faq.md
```

### 1.2 Plugin API Enhancements

**Current Gaps to Address:**

| Gap | Issue | Solution |
|-----|-------|----------|
| No typed connections | Generic data flow | Add `DataType` system |
| No lifecycle hooks | Can't init/shutdown | Add `init()`/`destroy()` |
| No dependencies | Can't use other plugins | Add `getDependencies()` |
| No events | Can't react to events | Add `EventPublisher` |
| No configuration UI | Manual config only | Add `createConfigUI()` |

**Enhanced PluginProvider Interface:**
```java
public interface PluginProvider {
    
    // === EXISTING (keep) ===
    String getNodeType();
    String getDisplayName();
    String getDescription();
    NodeCategory getCategory();
    List<PropertyDefinition> getProperties();
    Map<String, Object> execute(ExecutionContext context);
    List<HandleDefinition> getHandles();
    
    // === NEW ===
    
    /**
     * Optional initialization - called when plugin loads
     */
    default void init(PluginContext context) {}
    
    /**
     * Optional cleanup - called when plugin unloads
     */
    default void destroy() {}
    
    /**
     * Declare dependencies on other plugins
     */
    default List<PluginDependency> getDependencies() {
        return List.of();
    }
    
    /**
     * Subscribe to system events
     */
    default List<EventSubscription> getEventSubscriptions() {
        return List.of();
    }
    
    /**
     * Create custom configuration UI
     */
    default Node createConfigUI(ConfigContext context) {
        return null;
    }
    
    /**
     * Plugin icon override
     */
    default String getIconName() {
        return "PUZZLE";
    }
    
    /**
     * Help text (Markdown supported)
     */
    default String getHelpText() {
        return getDescription();
    }
}
```

### 1.3 PluginContext API
```java
/**
 * Context provided during plugin initialization/destruction.
 */
public interface PluginContext {
    
    /**
     * Get a service from the application
     */
    <T> T getService(Class<T> serviceClass);
    
    /**
     * Get plugin configuration
     */
    Properties getConfig();
    
    /**
     * Get plugin logger
     */
    Logger getLogger();
    
    /**
     * Register event handler
     */
    void registerEventHandler(EventHandler handler);
    
    /**
     * Get another plugin instance
     */
    Optional<PluginProvider> getPlugin(String pluginId);
}
```

---

## Phase 2: Plugin Templates & Samples [Priority: HIGH]

### 2.1 Template Repository

**Location:** `plugins/templates/`

**Templates to Create:**
```
plugins/templates/
├── template-simple-action/
│   ├── build.gradle
│   ├── src/main/java/com/example/SimpleActionPlugin.java
│   ├── src/main/resources/META-INF/services/
│   │   └── ai.nervemind.plugin.api.PluginProvider
│   └── README.md
│
├── template-trigger/
│   ├── build.gradle
│   ├── src/main/java/com/example/TriggerPlugin.java
│   └── README.md
│
├── template-multi-handle/
│   ├── build.gradle
│   ├── src/main/java/com/example/MultiHandlePlugin.java
│   └── README.md
│
├── template-custom-ui/
│   ├── build.gradle
│   ├── src/main/java/com/example/CustomUIPlugin.java
│   └── README.md
│
└── template-advanced/
    ├── build.gradle
    ├── src/main/java/com/example/AdvancedPlugin.java
    └── README.md
```

### 2.2 Sample Plugins in Repository

**Location:** `plugins/samples/`

**Sample Plugins:**
| Plugin | Complexity | Features Demonstrated |
|--------|------------|----------------------|
| `hello-world` | Beginner | Basic action, properties |
| `weather-fetcher` | Beginner | HTTP request, API integration |
| `file-processor` | Intermediate | File I/O, triggers |
| `data-transformer` | Intermediate | Data transformation, merging |
| `ai-assistant` | Advanced | AI integration, chat |
| `webhook-handler` | Advanced | HTTP triggers, responses |

### 2.3 Sample Plugin: Hello World
```java
package com.example.helloworld;

import ai.nervemind.plugin.api.*;
import java.util.List;
import java.util.Map;

/**
 * A simple plugin that returns a greeting message.
 * 
 * This is a beginner-friendly example of a PluginProvider implementation.
 */
public class HelloWorldPlugin implements PluginProvider {
    
    @Override
    public String getNodeType() {
        return "com.example.helloworld";
    }
    
    @Override
    public String getDisplayName() {
        return "Hello World";
    }
    
    @Override
    public String getDescription() {
        return "Returns a personalized greeting message";
    }
    
    @Override
    public NodeCategory getCategory() {
        return NodeCategory.UTILITY;
    }
    
    @Override
    public List<PropertyDefinition> getProperties() {
        return List.of(
            PropertyDefinition.requiredString(
                "name", 
                "Your Name", 
                "Enter your name"
            ),
            PropertyDefinition.optionalString(
                "greeting", 
                "Greeting", 
                "Hello", 
                "Custom greeting prefix"
            )
        );
    }
    
    @Override
    public Map<String, Object> execute(ExecutionContext context) {
        String name = (String) context.getNodeSettings().get("name");
        String greeting = (String) context.getNodeSettings()
            .getOrDefault("greeting", "Hello");
        
        String message = greeting + ", " + name + "!";
        
        context.logInfo("Generated greeting: " + message);
        
        return Map.of(
            "message", message,
            "name", name,
            "timestamp", System.currentTimeMillis()
        );
    }
}
```

---

## Phase 3: Developer CLI Tools [Priority: HIGH]

### 3.1 CLI Commands

**New Module:** `nervemind-cli/`

**Commands:**
```bash
# Create a new plugin from template
nervemind plugin create my-plugin --template hello-world

# Build and package plugin
nervemind plugin build

# Run tests
nervemind plugin test

# Validate plugin structure
nervemind plugin validate

# Package as distribution JAR
nervemind plugin package

# Generate documentation
nervemind plugin docs

# Initialize development environment
nervemind plugin init

# Install plugin to local NerveMind
nervemind plugin install

# List installed plugins
nervemind plugin list

# Run plugin in dev mode
nervemind plugin dev
```

### 3.2 Plugin Create Wizard
```bash
$ nervemind plugin create my-plugin

Welcome to the NerveMind Plugin Creator!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

? Choose a template:
  1. Hello World (Beginner)
  2. Trigger Plugin (Beginner)
  3. Multi-Handle Plugin (Intermediate)
  4. Custom UI Plugin (Advanced)
  5. Custom (select features yourself)

> (Select template)

? Enter your plugin ID (e.g., com.example.myplugin):
> com.example.myplugin

? Enter a display name:
> My Plugin

? Enter description:
> A description of what my plugin does

? Choose properties:
  [*] String input
  [*] Number input
  [ ] Boolean toggle
  [ ] File picker
  [ ] Custom type

? Would you like to add custom handles? (y/N)
> N

✅ Plugin created: my-plugin/

Next steps:
  1. cd my-plugin
  2. ./gradlew build
  3. ./gradlew install
```

### 3.3 Gradle Plugin for NerveMind

**Location:** `plugins/gradle-plugin/`

**Build script integration:**
```groovy
// build.gradle
plugins {
    id 'ai.nervemind.plugin' version '1.0.0'
}

nervemindPlugin {
    pluginId = 'com.example.myplugin'
    displayName = 'My Plugin'
    category = 'UTILITY'
    
    // Auto-generate service file
    generateServiceFile = true
}
```

---

## Phase 4: Testing Framework [Priority: MEDIUM]

### 4.1 Plugin Testing SDK

**Location:** `plugin-testing/`

**Files:**
```
plugin-testing/
├── src/main/java/ai/nervemind/plugin/testing/
│   ├── PluginTestRunner.java
│   ├── MockExecutionContext.java
│   ├── MockPluginContext.java
│   ├── TestWorkflowBuilder.java
│   ├── AssertionHelpers.java
│   ├── TestDataFactory.java
│   └── junit5/
│       ├── PluginTest.java
│       ├── Extension.java
│       └── annotations/
│           ├── WithMockContext.java
│           └── WithWorkflow.java
└── README.md
```

### 4.2 Usage Example
```java
import ai.nervemind.plugin.testing.*;

@PluginTest(
    plugin = HelloWorldPlugin.class,
    templates = {"hello-world", "weather-fetcher"}
)
class HelloWorldPluginTest {
    
    private MockExecutionContext context;
    
    @BeforeEach
    void setup() {
        context = new MockExecutionContext()
            .withSetting("name", "World")
            .withSetting("greeting", "Hi");
    }
    
    @Test
    void shouldReturnGreeting() {
        HelloWorldPlugin plugin = new HelloWorldPlugin();
        
        Map<String, Object> result = plugin.execute(context);
        
        assertThat(result.get("message")).isEqualTo("Hi, World!");
    }
    
    @Test
    void shouldIncludeTimestamp() {
        HelloWorldPlugin plugin = new HelloWorldPlugin();
        
        Map<String, Object> result = plugin.execute(context);
        
        assertThat(result).containsKey("timestamp");
        assertThat(result.get("timestamp"))
            .isInstanceOf(Long.class);
    }
    
    @Test
    void shouldLogMessage() {
        HelloWorldPlugin plugin = new HelloWorldPlugin();
        
        plugin.execute(context);
        
        assertThat(context.getLogs())
            .anyMatch(log -> log.contains("Generated greeting"));
    }
}
```

---

## Phase 5: In-App Plugin Developer Features [Priority: MEDIUM]

### 5.1 Plugin Development Panel

**Location:** `ui/src/main/java/ai/nervemind/ui/`

**UI Components:**
```
├── PluginDevelopmentPanel.java      # Main development panel
├── TemplateBrowser.java             # Browse/install templates
├── PluginEditor.java               # Simple code editor
├── TestRunnerPanel.java            # Run tests UI
├── DocumentationViewer.java        # Docs browser
└── SamplesBrowser.java             # Sample workflows
```

**Features:**
- Browse and install templates
- Create new plugin from template
- Run tests with visual feedback
- View API documentation inline
- Access sample workflows

### 5.2 Plugin Debug Mode
```java
// In Development mode, plugins can:
// - Reload without restart
// - Use hot-swap for code changes
// - Access extended logging
// - Debug visualizations

public class DevModeService {
    
    public void enableHotSwap(String pluginId) {
        // Watch plugin class files
        // Reload on change
    }
    
    public void showExecutionTrace(String nodeId) {
        // Visualize data flow through nodes
    }
    
    public void enableDebugLogging(String pluginId) {
        // Add verbose logging
    }
}
```

---

## Phase 6: Plugin Marketplace [Priority: LOWER]

After developer experience is solid, build the marketplace:

### 6.1 Marketplace Stack (Your Setup)
```
Frontend:     Next.js (already have)
Hosting:      Vercel (free tier)
Database:     Supabase (free tier)
Storage:      AWS S3 (free tier)
Auth:         GitHub OAuth (free)
```

### 6.2 Marketplace Features
- Browse/search plugins
- One-click install to NerveMind
- Ratings & reviews
- Plugin versioning
- Update notifications

---

## Implementation Order

```
Phase 1: Weeks 1-4
  ├── 1.1 Plugin API documentation
  ├── 1.2 Plugin API enhancements
  └── 1.3 PluginContext API

Phase 2: Weeks 3-6 (overlap with Phase 1)
  ├── 2.1 Template repository
  └── 2.2 Sample plugins

Phase 3: Weeks 5-8
  ├── 3.1 CLI commands
  ├── 3.2 Plugin create wizard
  └── 3.3 Gradle plugin

Phase 4: Weeks 7-10
  └── 4.1 Testing framework SDK

Phase 5: Weeks 9-12
  └── 5.1 In-app developer panel

Phase 6: Weeks 13+
  └── 6.1 Marketplace (after DE is solid)
```

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Plugin documentation pages | 50+ |
| Template plugins | 5+ |
| Sample plugins | 10+ |
| CLI commands | 10+ |
| Developer satisfaction | > 4.5/5 |
| Time to first plugin | < 30 minutes |

---

## Developer Journey

```
1. Discovery
   ↓
   Visit docs.nervemind.ai/plugins
   ↓
2. Quick Start
   ↓
   Read "Creating Your First Plugin" (5 min)
   ↓
3. Template
   ↓
   Run: nervemind plugin create my-plugin
   ↓
4. Development
   ↓
   Edit code with IDE
   Use: nervemind plugin test
   ↓
5. Distribution
   ↓
   Publish to marketplace (future)
```

---

## Backward Compatibility

- All existing plugins continue to work
- New API additions are optional
- Deprecation warnings for old APIs
- Migration guide provided
