# Hello World Plugin Template

A simple NerveMind plugin template that returns a greeting message.

## Quick Start

### Prerequisites

- Java 21+
- Gradle 9.0+
- NerveMind 1.0.0+

### Building

```bash
# Build the plugin
./gradlew build

# Install to NerveMind plugins directory
./gradlew installPlugin
```

### Development

```bash
# Run tests
./gradlew test

# Build with documentation
./gradlew javadoc

# Create distribution JAR
./gradlew jar
```

## Project Structure

```
hello-world/
├── build.gradle              # Build configuration
├── gradle.properties         # Plugin metadata
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── HelloWorldPlugin.java  # Main plugin class
        └── resources/
            └── META-INF/
                └── services/
                    └── ai.nervemind.plugin.api.PluginProvider  # Service registration
```

## Customizing

Edit `gradle.properties` to customize plugin metadata:

```properties
pluginId=com.example.helloworld
pluginName=Hello World
pluginDescription=Your description here
```

## Publishing

1. Build the JAR: `./gradlew jar`
2. The JAR will be at `build/libs/hello-world-1.0.0.jar`
3. Copy to your NerveMind `plugins/` directory

## Features

- ✅ Simple action plugin
- ✅ String properties
- ✅ Validation
- ✅ Logging
- ✅ Help text
- ✅ Documentation

## Next Steps

- Add more properties
- Implement custom handles
- Add event subscriptions
- Create sample workflows

## Resources

- [Plugin Documentation](https://docs.nervemind.ai/plugins/)
- [API Reference](https://docs.nervemind.ai/api/)
- [Discord Community](https://discord.gg/nervemind)
