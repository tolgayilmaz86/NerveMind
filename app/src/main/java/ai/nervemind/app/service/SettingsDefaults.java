package ai.nervemind.app.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.enums.SettingType;

/**
 * Default settings definitions for the application.
 * Contains all setting keys, default values, and metadata.
 */
public final class SettingsDefaults {

        private SettingsDefaults() {
                // Utility class
        }

        // ========================================
        // Setting Key Constants
        // ========================================

        // General Settings
        /** Setting key for the application theme (light/dark). */
        public static final String GENERAL_THEME = "general.theme";
        /** Setting key for the application language/locale. */
        public static final String GENERAL_LANGUAGE = "general.language";
        /** Setting key for enabling/disabling auto-save functionality. */
        public static final String GENERAL_AUTO_SAVE = "general.autoSave";
        /** Setting key for auto-save interval in minutes. */
        public static final String GENERAL_AUTO_SAVE_INTERVAL = "general.autoSaveInterval";
        /** Setting key for showing/hiding the welcome screen. */
        public static final String GENERAL_SHOW_WELCOME = "general.showWelcome";
        /** Setting key for maximum number of recent items to display. */
        public static final String GENERAL_RECENT_LIMIT = "general.recentLimit";
        /** Setting key for showing confirmation dialog on delete operations. */
        public static final String GENERAL_CONFIRM_DELETE = "general.confirmDelete";
        /** Setting key for enabling/disabling automatic update checks. */
        public static final String GENERAL_CHECK_UPDATES = "general.checkUpdates";

        // Editor Settings
        /** Setting key for grid size in the editor. */
        public static final String EDITOR_GRID_SIZE = "editor.gridSize";
        /** Setting key for snap-to-grid functionality. */
        public static final String EDITOR_SNAP_TO_GRID = "editor.snapToGrid";
        /** Setting key for showing/hiding the grid. */
        public static final String EDITOR_SHOW_GRID = "editor.showGrid";
        /** Setting key for showing/hiding the minimap. */
        public static final String EDITOR_SHOW_MINIMAP = "editor.showMinimap";
        /** Setting key for default zoom level. */
        public static final String EDITOR_DEFAULT_ZOOM = "editor.defaultZoom";
        /** Setting key for animation speed. */
        public static final String EDITOR_ANIMATION_SPEED = "editor.animationSpeed";
        /** Setting key for node spacing. */
        public static final String EDITOR_NODE_SPACING = "editor.nodeSpacing";
        /** Setting key for connection style. */
        public static final String EDITOR_CONNECTION_STYLE = "editor.connectionStyle";

        // Execution Settings
        /** Setting key for default execution timeout. */
        public static final String EXECUTION_DEFAULT_TIMEOUT = "execution.defaultTimeout";
        /** Setting key for maximum parallel executions. */
        public static final String EXECUTION_MAX_PARALLEL = "execution.maxParallel";
        /** Setting key for retry attempts. */
        public static final String EXECUTION_RETRY_ATTEMPTS = "execution.retryAttempts";
        /** Setting key for retry delay. */
        public static final String EXECUTION_RETRY_DELAY = "execution.retryDelay";
        /** Setting key for execution log level. */
        public static final String EXECUTION_LOG_LEVEL = "execution.logLevel";
        /** Setting key for execution history limit. */
        public static final String EXECUTION_HISTORY_LIMIT = "execution.historyLimit";
        /** Setting key for execution history retention. */
        public static final String EXECUTION_HISTORY_RETENTION = "execution.historyRetention";
        /** Setting key for showing execution console. */
        public static final String EXECUTION_SHOW_CONSOLE = "execution.showConsole";

        // AI Provider Settings - OpenAI
        /** Setting key for OpenAI API key. */
        public static final String AI_OPENAI_API_KEY = "ai.openai.apiKey";
        /** Setting key for OpenAI organization ID. */
        public static final String AI_OPENAI_ORG_ID = "ai.openai.orgId";
        /** Setting key for OpenAI base URL. */
        public static final String AI_OPENAI_BASE_URL = "ai.openai.baseUrl";
        /** Setting key for OpenAI default model. */
        public static final String AI_OPENAI_DEFAULT_MODEL = "ai.openai.defaultModel";

        // AI Provider Settings - Anthropic
        /** Setting key for Anthropic API key. */
        public static final String AI_ANTHROPIC_API_KEY = "ai.anthropic.apiKey";
        /** Setting key for Anthropic base URL. */
        public static final String AI_ANTHROPIC_BASE_URL = "ai.anthropic.baseUrl";
        /** Setting key for Anthropic default model. */
        public static final String AI_ANTHROPIC_DEFAULT_MODEL = "ai.anthropic.defaultModel";

        // AI Provider Settings - Ollama
        /** Setting key for Ollama base URL. */
        public static final String AI_OLLAMA_BASE_URL = "ai.ollama.baseUrl";
        /** Setting key for Ollama default model. */
        public static final String AI_OLLAMA_DEFAULT_MODEL = "ai.ollama.defaultModel";

        // AI Provider Settings - Azure OpenAI
        /** Setting key for Azure OpenAI API key. */
        public static final String AI_AZURE_API_KEY = "ai.azure.apiKey";
        /** Setting key for Azure OpenAI endpoint. */
        public static final String AI_AZURE_ENDPOINT = "ai.azure.endpoint";
        /** Setting key for Azure OpenAI deployment. */
        public static final String AI_AZURE_DEPLOYMENT = "ai.azure.deployment";
        /** Setting key for Azure OpenAI API version. */
        public static final String AI_AZURE_API_VERSION = "ai.azure.apiVersion";

        // AI Provider Settings - Google Gemini
        /** Setting key for Google Gemini API key. */
        public static final String AI_GOOGLE_API_KEY = "ai.google.apiKey";
        /** Setting key for Google Gemini base URL. */
        public static final String AI_GOOGLE_BASE_URL = "ai.google.baseUrl";
        /** Setting key for Google Gemini default model. */
        public static final String AI_GOOGLE_DEFAULT_MODEL = "ai.google.defaultModel";

        // HTTP/Network Settings
        /** Setting key for HTTP User-Agent header. */
        public static final String HTTP_USER_AGENT = "http.userAgent";
        /** Setting key for HTTP connection timeout in milliseconds. */
        public static final String HTTP_CONNECT_TIMEOUT = "http.connectTimeout";
        /** Setting key for HTTP read timeout in milliseconds. */
        public static final String HTTP_READ_TIMEOUT = "http.readTimeout";
        /** Setting key for enabling/disabling automatic redirect following. */
        public static final String HTTP_FOLLOW_REDIRECTS = "http.followRedirects";
        /** Setting key for maximum number of redirects to follow. */
        public static final String HTTP_MAX_REDIRECTS = "http.maxRedirects";
        /** Setting key for enabling/disabling HTTP proxy. */
        public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled";
        /** Setting key for HTTP proxy host. */
        public static final String HTTP_PROXY_HOST = "http.proxy.host";
        /** Setting key for HTTP proxy port. */
        public static final String HTTP_PROXY_PORT = "http.proxy.port";
        /** Setting key for HTTP proxy authentication type. */
        public static final String HTTP_PROXY_AUTH = "http.proxy.auth";
        /** Setting key for HTTP proxy username. */
        public static final String HTTP_PROXY_USERNAME = "http.proxy.username";
        /** Setting key for HTTP proxy password. */
        public static final String HTTP_PROXY_PASSWORD = "http.proxy.password";

        // Database & Storage Settings
        /** Setting key for the database file path. */
        public static final String STORAGE_DATABASE_PATH = "storage.databasePath";
        /** Setting key for enabling/disabling automatic backups. */
        public static final String STORAGE_BACKUP_ENABLED = "storage.backup.enabled";
        /** Setting key for backup interval in hours. */
        public static final String STORAGE_BACKUP_INTERVAL = "storage.backup.interval";
        /** Setting key for number of backup files to retain. */
        public static final String STORAGE_BACKUP_RETENTION = "storage.backup.retention";
        /** Setting key for backup directory path. */
        public static final String STORAGE_BACKUP_PATH = "storage.backup.path";

        // Webhook & Server Settings
        /** Setting key for server port number. */
        public static final String SERVER_PORT = "server.port";
        /** Setting key for base URL for webhook endpoints. */
        public static final String SERVER_WEBHOOK_BASE_URL = "server.webhookBaseUrl";
        /** Setting key for enabling/disabling webhook functionality. */
        public static final String SERVER_WEBHOOKS_ENABLED = "server.webhooksEnabled";
        /** Setting key for webhook signature verification secret. */
        public static final String SERVER_WEBHOOK_SECRET = "server.webhookSecret";
        /** Setting key for enabling/disabling CORS support. */
        public static final String SERVER_CORS_ENABLED = "server.cors.enabled";
        /** Setting key for allowed CORS origins (comma-separated). */
        public static final String SERVER_CORS_ORIGINS = "server.cors.origins";

        // Notification Settings
        /** Setting key for enabling/disabling notifications. */
        public static final String NOTIFY_ENABLED = "notify.enabled";
        /** Setting key for sending notifications on successful operations. */
        public static final String NOTIFY_ON_SUCCESS = "notify.onSuccess";
        /** Setting key for sending notifications on failed operations. */
        public static final String NOTIFY_ON_FAILURE = "notify.onFailure";
        /** Setting key for enabling/disabling notification sounds. */
        public static final String NOTIFY_SOUND_ENABLED = "notify.soundEnabled";
        /** Setting key for enabling/disabling email notifications. */
        public static final String NOTIFY_EMAIL_ENABLED = "notify.email.enabled";
        /** Setting key for SMTP host for email notifications. */
        public static final String NOTIFY_EMAIL_SMTP_HOST = "notify.email.smtpHost";
        /** Setting key for SMTP port for email notifications. */
        public static final String NOTIFY_EMAIL_SMTP_PORT = "notify.email.smtpPort";
        /** Setting key for sender email address for notifications. */
        public static final String NOTIFY_EMAIL_FROM = "notify.email.from";
        /** Setting key for recipient email address for notifications. */
        public static final String NOTIFY_EMAIL_TO = "notify.email.to";

        // Python Scripting Settings
        /** Setting key for Python script execution mode (sandboxed/external). */
        public static final String PYTHON_EXECUTION_MODE = "python.executionMode";
        /** Setting key for external Python interpreter path. */
        public static final String PYTHON_EXTERNAL_PATH = "python.externalPath";
        /** Setting key for Python virtual environment path. */
        public static final String PYTHON_VENV_PATH = "python.venvPath";
        /** Setting key for Python script execution timeout in seconds. */
        public static final String PYTHON_TIMEOUT = "python.timeout";
        /** Setting key for allowing network access in Python scripts. */
        public static final String PYTHON_ALLOW_NETWORK = "python.allowNetwork";
        /** Setting key for allowing file system access in Python scripts. */
        public static final String PYTHON_ALLOW_FILE_ACCESS = "python.allowFileAccess";
        /** Setting key for working directory for Python script execution. */
        public static final String PYTHON_WORKING_DIR = "python.workingDirectory";

        // Advanced Settings
        /** Setting key for enabling/disabling developer mode features. */
        public static final String ADVANCED_DEV_MODE = "advanced.devMode";
        /** Setting key for showing/hiding internal node types in the UI. */
        public static final String ADVANCED_SHOW_INTERNAL_NODES = "advanced.showInternalNodes";
        /** Setting key for script execution timeout in milliseconds. */
        public static final String ADVANCED_SCRIPT_TIMEOUT = "advanced.scriptTimeout";
        /** Setting key for maximum expression evaluation depth. */
        public static final String ADVANCED_MAX_EXPRESSION_DEPTH = "advanced.maxExpressionDepth";
        /** Setting key for enabling/disabling telemetry collection. */
        public static final String ADVANCED_TELEMETRY = "advanced.telemetry";
        /** Setting key for custom log file path. */
        public static final String ADVANCED_LOG_PATH = "advanced.logPath";
        /** Setting key for maximum log file size in MB. */
        public static final String ADVANCED_LOG_MAX_SIZE = "advanced.logMaxSize";
        /** Setting key for log rotation strategy. */
        public static final String ADVANCED_LOG_ROTATION = "advanced.logRotation";

        // Plugin Settings
        /** Setting key for enabling/disabling plugin system. */
        public static final String PLUGINS_ENABLED = "plugins.enabled";

        // ========================================
        // Default Settings List
        // ========================================

        private static final List<SettingDTO> DEFAULTS = new ArrayList<>();

        static {
                int order;

                // --- General Settings ---
                order = 0;
                DEFAULTS.add(setting(GENERAL_THEME, "nord-dark", SettingCategory.GENERAL, SettingType.ENUM,
                                "Theme", "Application color theme", order++, false,
                                "{\"options\":[\"nord-dark\",\"nord-light\",\"system\"]}"));
                DEFAULTS.add(setting(GENERAL_LANGUAGE, "en", SettingCategory.GENERAL, SettingType.ENUM,
                                "Language", "UI language", order++, true,
                                "{\"options\":[\"en\",\"de\",\"fr\",\"es\"]}"));
                DEFAULTS.add(setting(GENERAL_AUTO_SAVE, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                                "Auto-save", "Automatically save workflows", order++, false, null));
                DEFAULTS.add(setting(GENERAL_AUTO_SAVE_INTERVAL, "30", SettingCategory.GENERAL, SettingType.INTEGER,
                                "Auto-save interval", "Seconds between auto-saves", order++, false,
                                "{\"min\":5,\"max\":300}"));
                DEFAULTS.add(setting(GENERAL_SHOW_WELCOME, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                                "Show welcome screen", "Show welcome screen on startup", order++, false, null));
                DEFAULTS.add(setting(GENERAL_RECENT_LIMIT, "10", SettingCategory.GENERAL, SettingType.INTEGER,
                                "Recent workflows limit", "Maximum recent workflows to show", order++, false,
                                "{\"min\":1,\"max\":50}"));
                DEFAULTS.add(setting(GENERAL_CONFIRM_DELETE, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                                "Confirm on delete", "Ask before deleting workflows", order++, false, null));
                DEFAULTS.add(setting(GENERAL_CHECK_UPDATES, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                                "Check for updates", "Automatically check for updates", order++, false, null));

                // --- Editor Settings ---
                order = 0;
                DEFAULTS.add(setting(EDITOR_GRID_SIZE, "20", SettingCategory.EDITOR, SettingType.INTEGER,
                                "Grid size", "Canvas grid size in pixels", order++, false, "{\"min\":10,\"max\":100}"));
                DEFAULTS.add(setting(EDITOR_SNAP_TO_GRID, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                                "Snap to grid", "Snap nodes to grid when moving", order++, false, null));
                DEFAULTS.add(setting(EDITOR_SHOW_GRID, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                                "Show grid", "Display grid lines on canvas", order++, false, null));
                DEFAULTS.add(setting(EDITOR_SHOW_MINIMAP, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                                "Show minimap", "Show minimap in canvas corner", order++, false, null));
                DEFAULTS.add(setting(EDITOR_DEFAULT_ZOOM, "100", SettingCategory.EDITOR, SettingType.INTEGER,
                                "Default zoom", "Initial zoom level (%)", order++, false, "{\"min\":25,\"max\":200}"));
                DEFAULTS.add(setting(EDITOR_ANIMATION_SPEED, "normal", SettingCategory.EDITOR, SettingType.ENUM,
                                "Animation speed", "UI animation speed", order++, false,
                                "{\"options\":[\"slow\",\"normal\",\"fast\",\"none\"]}"));
                DEFAULTS.add(setting(EDITOR_NODE_SPACING, "50", SettingCategory.EDITOR, SettingType.INTEGER,
                                "Node spacing", "Default spacing for auto-layout", order++, false,
                                "{\"min\":20,\"max\":200}"));
                DEFAULTS.add(setting(EDITOR_CONNECTION_STYLE, "bezier", SettingCategory.EDITOR, SettingType.ENUM,
                                "Connection style", "Line style for connections", order++, false,
                                "{\"options\":[\"bezier\",\"straight\",\"step\"]}"));

                // --- Execution Settings ---
                order = 0;
                DEFAULTS.add(setting(EXECUTION_DEFAULT_TIMEOUT, "30000", SettingCategory.EXECUTION, SettingType.LONG,
                                "Default timeout", "Default node timeout in ms", order++, false,
                                "{\"min\":1000,\"max\":300000}"));
                DEFAULTS.add(setting(EXECUTION_MAX_PARALLEL, "10", SettingCategory.EXECUTION, SettingType.INTEGER,
                                "Max parallel nodes", "Maximum concurrent node executions", order++, false,
                                "{\"min\":1,\"max\":100}"));
                DEFAULTS.add(setting(EXECUTION_RETRY_ATTEMPTS, "3", SettingCategory.EXECUTION, SettingType.INTEGER,
                                "Retry attempts", "Default retry count for failed nodes", order++, false,
                                "{\"min\":0,\"max\":10}"));
                DEFAULTS.add(setting(EXECUTION_RETRY_DELAY, "1000", SettingCategory.EXECUTION, SettingType.LONG,
                                "Retry delay", "Default delay between retries (ms)", order++, false,
                                "{\"min\":100,\"max\":60000}"));
                DEFAULTS.add(setting(EXECUTION_LOG_LEVEL, "DEBUG", SettingCategory.EXECUTION, SettingType.ENUM,
                                "Log level", "Minimum execution log level", order++, false,
                                "{\"options\":[\"TRACE\",\"DEBUG\",\"INFO\",\"WARN\",\"ERROR\"]}"));
                DEFAULTS.add(setting(EXECUTION_HISTORY_LIMIT, "100", SettingCategory.EXECUTION, SettingType.INTEGER,
                                "History limit", "Max executions per workflow", order++, false,
                                "{\"min\":10,\"max\":1000}"));
                DEFAULTS.add(setting(EXECUTION_HISTORY_RETENTION, "30", SettingCategory.EXECUTION, SettingType.INTEGER,
                                "History retention", "Days to keep execution history", order++, false,
                                "{\"min\":1,\"max\":365}"));
                DEFAULTS.add(setting(EXECUTION_SHOW_CONSOLE, "false", SettingCategory.EXECUTION, SettingType.BOOLEAN,
                                "Show console on run", "Auto-open console when running", order++, false, null));

                // --- AI Provider Settings ---
                order = 0;
                // OpenAI
                DEFAULTS.add(setting(AI_OPENAI_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                                "OpenAI API Key", "Your OpenAI API key", order++, false, null));
                DEFAULTS.add(setting(AI_OPENAI_ORG_ID, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                                "OpenAI Organization ID", "Optional organization ID", order++, false, null));
                DEFAULTS.add(setting(AI_OPENAI_BASE_URL, "https://api.openai.com/v1", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "OpenAI Base URL", "API endpoint (for proxies)", order++, false, null));
                DEFAULTS.add(setting(AI_OPENAI_DEFAULT_MODEL, "gpt-4o", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "OpenAI Default Model", "Default model for LLM nodes", order++, false, null));
                // Anthropic
                DEFAULTS.add(setting(AI_ANTHROPIC_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                                "Anthropic API Key", "Your Anthropic API key", order++, false, null));
                DEFAULTS.add(setting(AI_ANTHROPIC_BASE_URL, "https://api.anthropic.com", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "Anthropic Base URL", "API endpoint", order++, false, null));
                DEFAULTS.add(setting(AI_ANTHROPIC_DEFAULT_MODEL, "claude-sonnet-4-20250514",
                                SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "Anthropic Default Model", "Default model", order++, false, null));
                // Ollama
                DEFAULTS.add(
                                setting(AI_OLLAMA_BASE_URL, "http://localhost:11434", SettingCategory.AI_PROVIDERS,
                                                SettingType.STRING,
                                                "Ollama Base URL", "Ollama server URL", order++, false, null));
                DEFAULTS.add(setting(AI_OLLAMA_DEFAULT_MODEL, "llama3.2", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "Ollama Default Model", "Default local model", order++, false, null));
                // Azure OpenAI
                DEFAULTS.add(setting(AI_AZURE_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                                "Azure API Key", "Azure OpenAI API key", order++, false, null));
                DEFAULTS.add(setting(AI_AZURE_ENDPOINT, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                                "Azure Endpoint", "Azure OpenAI endpoint URL", order++, false, null));
                DEFAULTS.add(setting(AI_AZURE_DEPLOYMENT, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                                "Azure Deployment", "Model deployment name", order++, false, null));
                DEFAULTS.add(setting(AI_AZURE_API_VERSION, "2024-02-15", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "Azure API Version", "API version string", order++, false, null));
                // Google Gemini
                DEFAULTS.add(setting(AI_GOOGLE_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                                "Google API Key", "Your Google AI API key", order++, false, null));
                DEFAULTS.add(setting(AI_GOOGLE_BASE_URL, "https://generativelanguage.googleapis.com/v1beta",
                                SettingCategory.AI_PROVIDERS, SettingType.STRING,
                                "Google Base URL", "Gemini API endpoint", order++, false, null));
                DEFAULTS.add(setting(AI_GOOGLE_DEFAULT_MODEL, "gemini-1.5-pro", SettingCategory.AI_PROVIDERS,
                                SettingType.STRING,
                                "Google Default Model", "Default Gemini model", order++, false, null));

                // --- HTTP/Network Settings ---
                order = 0;
                DEFAULTS.add(setting(HTTP_USER_AGENT, "NerveMind/1.0", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                                "User Agent", "HTTP User-Agent header", order++, false, null));
                DEFAULTS.add(setting(HTTP_CONNECT_TIMEOUT, "10000", SettingCategory.HTTP_NETWORK, SettingType.LONG,
                                "Connection timeout", "Connection timeout (ms)", order++, false,
                                "{\"min\":1000,\"max\":60000}"));
                DEFAULTS.add(setting(HTTP_READ_TIMEOUT, "30000", SettingCategory.HTTP_NETWORK, SettingType.LONG,
                                "Read timeout", "Read timeout (ms)", order++, false, "{\"min\":1000,\"max\":300000}"));
                DEFAULTS.add(setting(HTTP_FOLLOW_REDIRECTS, "true", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                                "Follow redirects", "Automatically follow redirects", order++, false, null));
                DEFAULTS.add(setting(HTTP_MAX_REDIRECTS, "5", SettingCategory.HTTP_NETWORK, SettingType.INTEGER,
                                "Max redirects", "Maximum redirect hops", order++, false, "{\"min\":1,\"max\":20}"));
                DEFAULTS.add(setting(HTTP_PROXY_ENABLED, "false", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                                "Use proxy", "Use proxy for HTTP requests", order++, false, null));
                DEFAULTS.add(setting(HTTP_PROXY_HOST, "", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                                "Proxy host", "Proxy server hostname", order++, false, null));
                DEFAULTS.add(setting(HTTP_PROXY_PORT, "8080", SettingCategory.HTTP_NETWORK, SettingType.INTEGER,
                                "Proxy port", "Proxy server port", order++, false, "{\"min\":1,\"max\":65535}"));
                DEFAULTS.add(setting(HTTP_PROXY_AUTH, "false", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                                "Proxy authentication", "Proxy requires authentication", order++, false, null));
                DEFAULTS.add(setting(HTTP_PROXY_USERNAME, "", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                                "Proxy username", "Proxy username", order++, false, null));
                DEFAULTS.add(setting(HTTP_PROXY_PASSWORD, "", SettingCategory.HTTP_NETWORK, SettingType.PASSWORD,
                                "Proxy password", "Proxy password", order++, false, null));

                // --- Database & Storage Settings ---
                order = 0;
                DEFAULTS.add(setting(STORAGE_DATABASE_PATH, "./data", SettingCategory.DATABASE_STORAGE,
                                SettingType.PATH,
                                "Database location", "Data directory path", order++, true, null));
                DEFAULTS.add(setting(STORAGE_BACKUP_ENABLED, "false", SettingCategory.DATABASE_STORAGE,
                                SettingType.BOOLEAN,
                                "Automatic backups", "Enable automatic backups", order++, false, null));
                DEFAULTS.add(setting(STORAGE_BACKUP_INTERVAL, "daily", SettingCategory.DATABASE_STORAGE,
                                SettingType.ENUM,
                                "Backup interval", "Backup frequency", order++, false,
                                "{\"options\":[\"hourly\",\"daily\",\"weekly\"]}"));
                DEFAULTS.add(setting(STORAGE_BACKUP_RETENTION, "7", SettingCategory.DATABASE_STORAGE,
                                SettingType.INTEGER,
                                "Backup retention", "Days to keep backups", order++, false, "{\"min\":1,\"max\":365}"));
                DEFAULTS.add(setting(STORAGE_BACKUP_PATH, "./backups", SettingCategory.DATABASE_STORAGE,
                                SettingType.PATH,
                                "Backup location", "Backup directory", order++, false, null));

                // --- Webhook & Server Settings ---
                order = 0;
                DEFAULTS.add(setting(SERVER_PORT, "8080", SettingCategory.WEBHOOK_SERVER, SettingType.INTEGER,
                                "Server port", "HTTP server port", order++, true, "{\"min\":1,\"max\":65535}"));
                DEFAULTS.add(setting(SERVER_WEBHOOK_BASE_URL, "http://localhost:8080", SettingCategory.WEBHOOK_SERVER,
                                SettingType.STRING,
                                "Webhook base URL", "External webhook URL", order++, false, null));
                DEFAULTS.add(setting(SERVER_WEBHOOKS_ENABLED, "true", SettingCategory.WEBHOOK_SERVER,
                                SettingType.BOOLEAN,
                                "Enable webhooks", "Accept incoming webhooks", order++, false, null));
                DEFAULTS.add(setting(SERVER_WEBHOOK_SECRET, "", SettingCategory.WEBHOOK_SERVER, SettingType.PASSWORD,
                                "Webhook secret", "HMAC secret for validation", order++, false, null));
                DEFAULTS.add(setting(SERVER_CORS_ENABLED, "false", SettingCategory.WEBHOOK_SERVER, SettingType.BOOLEAN,
                                "Enable CORS", "Allow cross-origin requests", order++, false, null));
                DEFAULTS.add(setting(SERVER_CORS_ORIGINS, "*", SettingCategory.WEBHOOK_SERVER, SettingType.STRING,
                                "CORS origins", "Allowed origins (comma-separated)", order++, false, null));

                // --- Notification Settings ---
                order = 0;
                DEFAULTS.add(setting(NOTIFY_ENABLED, "true", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                                "Enable notifications", "Show system notifications", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_ON_SUCCESS, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                                "Notify on success", "Notify when workflow completes", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_ON_FAILURE, "true", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                                "Notify on failure", "Notify when workflow fails", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_SOUND_ENABLED, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                                "Sound enabled", "Play sound with notifications", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_EMAIL_ENABLED, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                                "Email notifications", "Send email notifications", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_EMAIL_SMTP_HOST, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                                "SMTP server", "SMTP server address", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_EMAIL_SMTP_PORT, "587", SettingCategory.NOTIFICATIONS, SettingType.INTEGER,
                                "SMTP port", "SMTP server port", order++, false, "{\"min\":1,\"max\":65535}"));
                DEFAULTS.add(setting(NOTIFY_EMAIL_FROM, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                                "From address", "Sender email address", order++, false, null));
                DEFAULTS.add(setting(NOTIFY_EMAIL_TO, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                                "To address", "Recipient email address", order++, false, null));

                // --- Python Scripting Settings ---
                order = 0;
                DEFAULTS.add(setting(PYTHON_EXECUTION_MODE, "embedded", SettingCategory.PYTHON, SettingType.ENUM,
                                "Execution mode",
                                "embedded: GraalPy (no install), external: system Python, auto: try both",
                                order++, false,
                                "{\"options\":[\"embedded\",\"external\",\"auto\"]}"));
                DEFAULTS.add(setting(PYTHON_EXTERNAL_PATH, "", SettingCategory.PYTHON, SettingType.PATH,
                                "Python interpreter",
                                "Path to external Python interpreter (leave empty for auto-detect)",
                                order++, false, null));
                DEFAULTS.add(setting(PYTHON_VENV_PATH, "", SettingCategory.PYTHON, SettingType.PATH,
                                "Virtual environment",
                                "Path to Python virtual environment (for pip packages)",
                                order++, false, null));
                DEFAULTS.add(setting(PYTHON_TIMEOUT, "60000", SettingCategory.PYTHON, SettingType.LONG,
                                "Script timeout",
                                "Maximum Python script execution time (milliseconds)",
                                order++, false,
                                "{\"min\":1000,\"max\":600000}"));
                DEFAULTS.add(setting(PYTHON_ALLOW_NETWORK, "true", SettingCategory.PYTHON, SettingType.BOOLEAN,
                                "Allow network access",
                                "Allow Python scripts to make network requests (external mode only)",
                                order++, false, null));
                DEFAULTS.add(setting(PYTHON_ALLOW_FILE_ACCESS, "false", SettingCategory.PYTHON, SettingType.BOOLEAN,
                                "Allow file access",
                                "Allow Python scripts to read/write files (external mode only)",
                                order++, false, null));
                DEFAULTS.add(setting(PYTHON_WORKING_DIR, "", SettingCategory.PYTHON, SettingType.PATH,
                                "Working directory",
                                "Working directory for Python scripts (leave empty for temp)",
                                order++, false, null));

                // --- Plugin Settings ---
                order = 0;
                DEFAULTS.add(setting(PLUGINS_ENABLED, "", SettingCategory.PLUGINS, SettingType.STRING,
                                "Enabled plugins",
                                "Comma-separated list of enabled plugin IDs (managed by Plugin Manager)",
                                order++, false, null));

                // --- Advanced Settings ---
                order = 0;
                DEFAULTS.add(setting(ADVANCED_DEV_MODE, "false", SettingCategory.ADVANCED, SettingType.BOOLEAN,
                                "Developer mode", "Enable debug features", order++, false, null));
                DEFAULTS.add(setting(ADVANCED_SHOW_INTERNAL_NODES, "false", SettingCategory.ADVANCED,
                                SettingType.BOOLEAN,
                                "Show internal nodes", "Show system node types", order++, false, null));
                DEFAULTS.add(setting(ADVANCED_SCRIPT_TIMEOUT, "60000", SettingCategory.ADVANCED, SettingType.LONG,
                                "Script timeout", "Code node timeout (ms)", order++, false,
                                "{\"min\":1000,\"max\":600000}"));
                DEFAULTS.add(setting(ADVANCED_MAX_EXPRESSION_DEPTH, "10", SettingCategory.ADVANCED, SettingType.INTEGER,
                                "Max expression depth", "Max nested expressions", order++, false,
                                "{\"min\":1,\"max\":50}"));
                DEFAULTS.add(setting(ADVANCED_TELEMETRY, "false", SettingCategory.ADVANCED, SettingType.BOOLEAN,
                                "Telemetry", "Anonymous usage statistics", order++, false, null));
                DEFAULTS.add(setting(ADVANCED_LOG_PATH, "./logs", SettingCategory.ADVANCED, SettingType.PATH,
                                "Log file location", "Log file directory", order++, true, null));
                DEFAULTS.add(setting(ADVANCED_LOG_MAX_SIZE, "10", SettingCategory.ADVANCED, SettingType.INTEGER,
                                "Max log file size", "Max log file size (MB)", order++, false,
                                "{\"min\":1,\"max\":100}"));
                DEFAULTS.add(setting(ADVANCED_LOG_ROTATION, "5", SettingCategory.ADVANCED, SettingType.INTEGER,
                                "Log rotation", "Number of log files to keep", order++, false,
                                "{\"min\":1,\"max\":20}"));
        }

        /**
         * Helper method to create a SettingDTO.
         */
        private static SettingDTO setting(String key, String value, SettingCategory category, SettingType type,
                        String label, String description, int order, boolean requiresRestart, String validation) {
                return SettingDTO.full(key, value, category, type, label, description, true, requiresRestart, order,
                                validation);
        }

        /**
         * Get all default settings.
         * 
         * @return the list of all default settings
         */
        public static List<SettingDTO> getAll() {
                return Collections.unmodifiableList(DEFAULTS);
        }

        /**
         * Get default settings by category.
         * 
         * @param category the category to filter by
         * @return the list of default settings for the category
         */
        public static List<SettingDTO> getByCategory(SettingCategory category) {
                return DEFAULTS.stream()
                                .filter(s -> s.category() == category)
                                .toList();
        }

        /**
         * Get default value for a setting key.
         * 
         * @param key the setting key
         * @return the default value for the key, or null if not found
         */
        public static String getDefault(String key) {
                return DEFAULTS.stream()
                                .filter(s -> s.key().equals(key))
                                .findFirst()
                                .map(SettingDTO::value)
                                .orElse(null);
        }

        /**
         * Get setting definition by key.
         * 
         * @param key the setting key
         * @return the setting definition, or null if not found
         */
        public static SettingDTO getDefinition(String key) {
                return DEFAULTS.stream()
                                .filter(s -> s.key().equals(key))
                                .findFirst()
                                .orElse(null);
        }

        /**
         * Get all settings as a map of key -> default value.
         * 
         * @return the map of setting keys to default values
         */
        public static Map<String, String> asMap() {
                return DEFAULTS.stream()
                                .collect(Collectors.toMap(SettingDTO::key, SettingDTO::value));
        }

        /**
         * Get all setting keys.
         * 
         * @return the list of all setting keys
         */
        public static List<String> getAllKeys() {
                return DEFAULTS.stream()
                                .map(SettingDTO::key)
                                .toList();
        }
}
