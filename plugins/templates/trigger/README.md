# Schedule Trigger Plugin Template

A trigger plugin template for NerveMind that fires workflows on a configurable CRON schedule.

## Features

- ✅ CRON expression support (minute hour day month weekday)
- ✅ Timezone configuration
- ✅ Enable/disable toggle
- ✅ Event subscriptions
- ✅ Lifecycle management
- ✅ Comprehensive validation
- ✅ Help text and schema

## Quick Start

### Prerequisites

- Java 21+
- Gradle 9.0+
- NerveMind 1.0.0+

### Building

```bash
cd trigger
./gradlew build
./gradlew installPlugin
```

### Usage in Workflow

```json
{
  "type": "com.example.schedule-trigger",
  "name": "Daily Morning Trigger",
  "config": {
    "cronExpression": "0 8 * * *",
    "timezone": "Europe/Amsterdam",
    "enabled": true
  }
}
```

## CRON Examples

| Expression | Description |
|------------|-------------|
| `0 8 * * *` | Every day at 8:00 AM |
| `30 9 * * 1` | Every Monday at 9:30 AM |
| `0 0 1 * *` | First day of every month at midnight |
| `*/15 * * * *` | Every 15 minutes |

## Project Structure

```
trigger/
├── build.gradle              # Build configuration
├── gradle.properties         # Plugin metadata
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── ScheduleTriggerPlugin.java  # Main plugin class
        └── resources/
            └── META-INF/
                └── services/
                    └── ai.nervemind.plugin.api.PluginProvider
```

## Customizing

Edit `gradle.properties` and `ScheduleTriggerPlugin.java` to customize.

## Next Steps

- Add more CRON fields (seconds, year)
- Implement persistent trigger state
- Add trigger history tracking
- Support multiple schedules per trigger

## Resources

- [Plugin Documentation](https://docs.nervemind.ai/plugins/)
- [CRON Expression Tutorial](https://crontab.guru/)
