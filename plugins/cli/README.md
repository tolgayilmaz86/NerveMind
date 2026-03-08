# NerveMind CLI

Developer tools for NerveMind plugin development.

## Installation

### Option 1: Build from Source

```bash
cd plugins/cli
./gradlew shadowJar
```

The CLI JAR will be at `build/libs/nervemind-cli.jar`.

### Option 2: Use Gradle Plugin

Add to your plugin's `build.gradle`:

```groovy
plugins {
    id 'ai.nervemind.cli' version '1.0.0'
}
```

## Usage

```bash
java -jar nervemind-cli.jar <command> [options]
```

### Commands

| Command | Description |
|---------|-------------|
| [create](#create) | Create a new plugin from template |
| [validate](#validate) | Validate a plugin configuration |
| [build](#build) | Build a plugin |
| [install](#install) | Install a plugin to NerveMind |
| [list](#list) | List installed plugins |
| [dev](#dev) | Run plugin in development mode |

### Global Options

| Option | Description |
|--------|-------------|
| `--verbose, -v` | Enable verbose output |
| `--quiet, -q` | Suppress non-essential output |
| `--help, -h` | Show help |
| `--version` | Show version |

---

## Create Command

Create a new plugin from a template.

```bash
nervemind create [OPTIONS] <plugin-name>
```

### Options

| Option | Description |
|--------|-------------|
| `--template, -t` | Template to use (hello-world, trigger, advanced-multi) |
| `--id, -i` | Plugin ID (e.g., com.example.myplugin) |
| `--name, -n` | Display name |
| `--description, -d` | Plugin description |
| `--author, -a` | Author name |
| `--force, -f` | Overwrite existing directory |
| `--interactive, -I` | Interactive mode |
| `--output-dir, -o` | Output directory |

### Examples

```bash
# Interactive mode
nervemind create my-plugin

# Non-interactive with options
nervemind create webhook-plugin --id=com.example.webhook --name="Webhook Handler"

# Specific template
nervemind create my-trigger --template=trigger
```

---

## Validate Command

Validate a NerveMind plugin.

```bash
nervemind validate [OPTIONS] <plugin-path>
```

### Options

| Option | Description |
|--------|-------------|
| `--strict` | Enable strict validation |
| `--quick` | Quick validation (skip JAR inspection) |
| `--json` | Output in JSON format |

### Examples

```bash
# Validate plugin directory
nervemind validate ./my-plugin

# Validate JAR file
nervemind validate ./build/libs/my-plugin-1.0.0.jar

# JSON output
nervemind validate ./my-plugin --json
```

---

## Build Command

Build a NerveMind plugin.

```bash
nervemind build [OPTIONS] <plugin-path>
```

### Options

| Option | Description |
|--------|-------------|
| `--clean` | Clean before building |
| `--test, -t` | Run tests |
| `--install, -i` | Install after building |
| `--parallel, -p` | Enable parallel compilation |
| `--verbose` | Show build output |

### Examples

```bash
# Basic build
nervemind build ./my-plugin

# Build with tests
nervemind build ./my-plugin --test

# Build and install
nervemind build ./my-plugin --install
```

---

## Install Command

Install a NerveMind plugin.

```bash
nervemind install [OPTIONS] <plugin-jar>
```

### Options

| Option | Description |
|--------|-------------|
| `--dir, -d` | Target plugins directory |
| `--enable, -e` | Enable plugin after install |
| `--backup, -b` | Backup existing plugin |
| `--force, -f` | Overwrite existing |

### Examples

```bash
# Install plugin
nervemind install ./build/libs/my-plugin-1.0.0.jar

# Install and enable
nervemind install ./my-plugin.jar --enable

# Custom plugins directory
nervemind install ./my-plugin.jar --dir=/opt/nervemind/plugins
```

---

## List Command

List installed plugins.

```bash
nervemind list [OPTIONS]
```

### Options

| Option | Description |
|--------|-------------|
| `--dir, -d` | Plugins directory |
| `--json` | Output in JSON format |
| `--verbose, -v` | Show detailed information |
| `--enabled-only` | Show only enabled plugins |

### Examples

```bash
# List all plugins
nervemind list

# Verbose output
nervemind list --verbose

# JSON format
nervemind list --json

# Only enabled
nervemind list --enabled-only
```

---

## Dev Command

Run a plugin in development mode with hot-reload.

```bash
nervemind dev [OPTIONS] <plugin-path>
```

### Options

| Option | Description |
|--------|-------------|
| `--watch, -w` | Watch for file changes |
| `--port, -p` | Debug port |
| `--test, -t` | Run tests on change |
| `--install, -i` | Install before watching |
| `--verbose` | Show detailed output |

### Examples

```bash
# Start development mode
nervemind dev ./my-plugin

# Watch with auto-test
nervemind dev ./my-plugin --test

# Custom debug port
nervemind dev ./my-plugin --port=5006
```

---

## Templates

### Available Templates

| Template | Description | Complexity |
|----------|-------------|------------|
| `hello-world` | Simple action plugin | Beginner |
| `trigger` | CRON schedule trigger | Intermediate |
| `advanced-multi` | Multi-handle processor | Advanced |

### Creating from Template

```bash
nervemind create my-plugin --template=hello-world
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `NERVEMIND_HOME` | NerveMind installation directory |
| `NERVEMIND_PLUGINS` | Default plugins directory |

---

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Creating Shadow JAR

```bash
./gradlew shadowJar
```

---

## License

Apache License 2.0
