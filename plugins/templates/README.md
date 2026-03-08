# NerveMind Plugin Templates

A collection of production-ready templates for creating NerveMind plugins.

## Available Templates

| Template | Description | Complexity | Features |
|----------|-------------|------------|----------|
| [hello-world](hello-world/) | Simple action plugin | Beginner | Basic handle, properties, validation |
| [trigger](trigger/) | CRON schedule trigger | Intermediate | Trigger, scheduling, event subscriptions |
| [advanced-multi](advanced-multi/) | Multi-handle processor | Advanced | Multiple handles, events, shared state |

### 1. Choose a Template

**## Quick Start

Beginner**: Start with `hello-world` to understand basic plugin structure.

**Intermediate**: Use `trigger` to learn about triggers and event subscriptions.

**Advanced**: Try `advanced-multi` for a full-featured multi-handle plugin.

### 2. Customize

Edit `gradle.properties` to set your plugin metadata:

```properties
pluginId=com.example.my-plugin
pluginName=My Plugin
pluginDescription=Description of my plugin
pluginProvider=Your Name
nervemindVersion=1.0.0
```

### 3. Build

```bash
cd <template-name>
./gradlew build
./gradlew installPlugin  # Installs to NerveMind plugins directory
```

## Template Comparison

### hello-world
```
Handles: 1
Events: No
State: No
File I/O: No
```

### trigger
```
Handles: 1
Events: Yes (application lifecycle)
State: No
File I/O: No
```

### advanced-multi
```
Handles: 3
Events: Yes (application + workflow)
State: Yes (shared across handles)
File I/O: Yes (JSON, CSV, Text)
```

## Creating Your Own

1. Copy a template that matches your needs
2. Update `gradle.properties` with your plugin info
3. Modify the Java class for your functionality
4. Update the service registration file if needed
5. Build and test

## Best Practices

- Use meaningful plugin IDs (reverse domain notation)
- Implement proper validation for all handles
- Subscribe to lifecycle events for cleanup
- Use shared state for cross-handle communication
- Document your handles with help text and schema

## Next Steps

- Add more templates (webhook, database, AI, etc.)
- Create plugin SDK for testing
- Build CLI tools for scaffolding
- Set up plugin marketplace
