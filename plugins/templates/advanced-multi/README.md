# Advanced Multi-Handle Plugin Template

An advanced NerveMind plugin template demonstrating multiple handles, event subscriptions, and shared state.

## Features

- **Multi-handle design**: Transform, validate, and file output in one plugin
- **Event subscriptions**: Listen to application and workflow events
- **Shared state**: Track statistics across handle executions
- **File operations**: Write to files with multiple formats
- **Complex validation**: Nested field validation with patterns

## Quick Start

```bash
cd advanced-multi
./gradlew build
./gradlew installPlugin
```

## Handles

### 1. Data Transform
Maps input fields to output fields using dot-notation paths.

```json
{
  "type": "com.example.advanced-data-processor",
  "name": "Transform User",
  "handle": "data-transform",
  "config": {
    "mappings": {
      "fullName": "user.name",
      "emailAddress": "contact.email"
    }
  }
}
```

### 2. Data Validate
Validates input against required fields and patterns.

```json
{
  "type": "com.example.advanced-data-processor",
  "name": "Validate Input",
  "handle": "data-validate",
  "config": {
    "requiredFields": ["email", "firstName"],
    "emailPattern": "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
  }
}
```

### 3. File Output
Writes data to files in JSON, CSV, or text format.

```json
{
  "type": "com.example.advanced-data-processor",
  "name": "Save Results",
  "handle": "file-output",
  "config": {
    "outputPath": "results/output.json",
    "outputFormat": "json",
    "overwrite": true
  }
}
```

## Project Structure

```
advanced-multi/
├── build.gradle
├── gradle.properties
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── AdvancedDataProcessorPlugin.java
        └── resources/
            └── META-INF/
                └── services/
                    └── ai.nervemind.plugin.api.PluginProvider
```

## Next Steps

- Add more transformation functions (filter, map, reduce)
- Implement batch processing
- Add streaming file output
- Support more file formats (XML, YAML)

## Resources

- [Plugin Documentation](https://docs.nervemind.ai/plugins/)
- [Event System Guide](https://docs.nervemind.ai/plugins/events/)
