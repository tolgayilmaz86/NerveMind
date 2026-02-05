package ai.nervemind.common.enums;

/**
 * Categories for organizing application settings in the Settings UI.
 * 
 * <p>
 * Each category groups related settings together for easier navigation.
 * Settings within a category are displayed as a section in the settings panel.
 * </p>
 * 
 * <h2>Settings UI Layout</h2>
 * 
 * <pre>
 * ┌─────────────────────┬──────────────────────────┐
 * │ Categories          │ Settings                 │
 * ├─────────────────────┼──────────────────────────┤
 * │ ► General           │  Language: [English ▼]   │
 * │   Editor            │  Theme: [Nord Dark ▼]    │
 * │   Execution         │  Auto-save: [✓]          │
 * │   AI Providers      │  ...                     │
 * │   ...               │                          │
 * └─────────────────────┴──────────────────────────┘
 * </pre>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see ai.nervemind.common.service.SettingsServiceInterface Settings service
 * @see ai.nervemind.common.dto.SettingDTO Setting data transfer object
 */
public enum SettingCategory {
    /** General application settings (language, theme, startup behavior) */
    GENERAL,

    /** Workflow editor settings (grid, snap, zoom) */
    EDITOR,

    /** Workflow execution settings (timeouts, retries, concurrency) */
    EXECUTION,

    /** AI/LLM provider settings (API keys, models, endpoints) */
    AI_PROVIDERS,

    /** HTTP and network settings (proxies, timeouts, certificates) */
    HTTP_NETWORK,

    /** Database and storage settings (path, backup) */
    DATABASE_STORAGE,

    /** Webhook server settings (port, SSL, authentication) */
    WEBHOOK_SERVER,

    /** Notification settings (email, Slack, toast) */
    NOTIFICATIONS,

    /** Python scripting settings (execution mode, paths, timeout) */
    PYTHON,

    /** Plugin management settings (enabled plugins) */
    PLUGINS,

    /** Advanced/developer settings (debug logging, experimental features) */
    ADVANCED
}
