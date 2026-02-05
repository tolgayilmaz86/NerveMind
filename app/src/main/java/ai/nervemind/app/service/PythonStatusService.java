/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.nervemind.app.executor.script.ScriptExecutionStrategy;
import ai.nervemind.common.service.PythonStatusServiceInterface;

/**
 * Service for providing Python environment status information to the UI.
 * 
 * <p>
 * Aggregates status from all Python execution strategies and provides
 * formatted information for display in the status bar and settings panels.
 * </p>
 * 
 * <h2>Status Bar Display</h2>
 * <ul>
 * <li>🐍 GraalPy 24.1 - embedded mode active</li>
 * <li>🐍 Python 3.12 - external mode active</li>
 * <li>🐍 Python 3.12 (venv) - external with virtual environment</li>
 * <li>⚠️ Python N/A - no Python available</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@Service
public class PythonStatusService implements PythonStatusServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(PythonStatusService.class);

    private final SettingsService settingsService;
    private final List<ScriptExecutionStrategy> strategies;

    // Cached status
    private String lastHealthCheckResult = "Not checked";

    /**
     * Creates a new Python status service.
     *
     * @param settingsService the settings service for configuration access
     * @param strategies      the list of available Python execution strategies
     */
    public PythonStatusService(SettingsService settingsService, List<ScriptExecutionStrategy> strategies) {
        this.settingsService = settingsService;
        this.strategies = strategies;
        log.info("PythonStatusService initialized with {} strategies", strategies.size());
    }

    @Override
    public String getExecutionMode() {
        return settingsService.getValue(SettingsDefaults.PYTHON_EXECUTION_MODE, "embedded");
    }

    @Override
    public String getActiveEnvironmentName() {
        String mode = getExecutionMode();

        switch (mode.toLowerCase()) {
            case "external":
                return getExternalEnvironmentStatus();

            case "auto":
                return getAutoEnvironmentStatus();

            case "embedded":
            default:
                return getEmbeddedEnvironmentStatus();
        }
    }

    private String getExternalEnvironmentStatus() {
        ScriptExecutionStrategy external = findStrategy("python-external");
        if (external != null && external.isAvailable()) {
            return formatExternalEnvironmentString(getExternalPythonVersion(), "");
        }
        return "External Python Not Available";
    }

    private String getAutoEnvironmentStatus() {
        ScriptExecutionStrategy autoExternal = findStrategy("python-external");
        if (autoExternal != null && autoExternal.isAvailable()) {
            return formatExternalEnvironmentString(getExternalPythonVersion(), " [auto]");
        }
        ScriptExecutionStrategy embedded = findStrategy("python");
        if (embedded != null && embedded.isAvailable()) {
            return formatEmbeddedEnvironmentString(getGraalPyVersion(), " [auto]");
        }
        return "No Python Available";
    }

    private String getEmbeddedEnvironmentStatus() {
        ScriptExecutionStrategy embeddedStrategy = findStrategy("python");
        if (embeddedStrategy != null && embeddedStrategy.isAvailable()) {
            return formatEmbeddedEnvironmentString(getGraalPyVersion(), "");
        }
        return "GraalPy Not Available";
    }

    private String formatExternalEnvironmentString(String version, String suffix) {
        String ver = version != null ? version : "?";
        String base = "Python " + ver;
        if (isVenvActive()) {
            base += " (venv)";
        }
        return base + suffix;
    }

    private String formatEmbeddedEnvironmentString(String version, String suffix) {
        String ver = version != null ? version : "?";
        return "GraalPy " + ver + suffix;
    }

    @Override
    public String getStatusBarText() {
        if (!isPythonAvailable()) {
            return "⚠️ Python N/A";
        }

        String mode = getExecutionMode();
        switch (mode.toLowerCase()) {
            case "external":
                return getExternalStatusBarText();

            case "auto":
                return getAutoStatusBarText();

            case "embedded":
            default:
                return getEmbeddedStatusBarText();
        }
    }

    private String getExternalStatusBarText() {
        ScriptExecutionStrategy external = findStrategy("python-external");
        if (external != null && external.isAvailable()) {
            String version = getExternalPythonVersion();
            String shortVersion = version != null ? shortenVersion(version) : "?";
            if (isVenvActive()) {
                return "🐍 Py " + shortVersion + " (venv)";
            }
            return "🐍 Py " + shortVersion;
        }
        return "⚠️ External N/A";
    }

    private String getAutoStatusBarText() {
        ScriptExecutionStrategy autoExternal = findStrategy("python-external");
        if (autoExternal != null && autoExternal.isAvailable()) {
            String version = getExternalPythonVersion();
            String shortVersion = version != null ? shortenVersion(version) : "?";
            return "🐍 Py " + shortVersion;
        }
        ScriptExecutionStrategy embedded = findStrategy("python");
        if (embedded != null && embedded.isAvailable()) {
            return "🐍 GraalPy";
        }
        return "⚠️ Python N/A";
    }

    private String getEmbeddedStatusBarText() {
        ScriptExecutionStrategy embeddedStrategy = findStrategy("python");
        if (embeddedStrategy != null && embeddedStrategy.isAvailable()) {
            return "🐍 GraalPy";
        }
        return "⚠️ GraalPy N/A";
    }

    @Override
    public boolean isPythonAvailable() {
        return isEmbeddedAvailable() || isExternalAvailable();
    }

    @Override
    public boolean isEmbeddedAvailable() {
        ScriptExecutionStrategy embedded = findStrategy("python");
        return embedded != null && embedded.isAvailable();
    }

    @Override
    public boolean isExternalAvailable() {
        ScriptExecutionStrategy external = findStrategy("python-external");
        return external != null && external.isAvailable();
    }

    @Override
    public boolean isVenvActive() {
        String venvPath = settingsService.getValue(SettingsDefaults.PYTHON_VENV_PATH, "");
        return venvPath != null && !venvPath.isBlank();
    }

    @Override
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Python Environment Status\n");
        sb.append("═════════════════════════\n\n");

        // Current mode
        sb.append("Mode: ").append(getExecutionMode().toUpperCase()).append("\n\n");

        // Embedded (GraalPy)
        sb.append("Embedded (GraalPy):\n");
        ScriptExecutionStrategy embedded = findStrategy("python");
        if (embedded != null) {
            sb.append("  Status: ").append(embedded.isAvailable() ? "✓ Available" : "✗ Not Available").append("\n");
            sb.append("  Info: ").append(embedded.getAvailabilityInfo()).append("\n");
        } else {
            sb.append("  Status: ✗ Strategy not found\n");
        }
        sb.append("\n");

        // External Python
        sb.append("External Python:\n");
        ScriptExecutionStrategy external = findStrategy("python-external");
        if (external != null) {
            sb.append("  Status: ").append(external.isAvailable() ? "✓ Available" : "✗ Not Available").append("\n");
            sb.append("  Info: ").append(external.getAvailabilityInfo()).append("\n");
            String path = getExternalPythonPath();
            if (path != null && !path.isBlank()) {
                sb.append("  Path: ").append(path).append("\n");
            }
        } else {
            sb.append("  Status: ✗ Strategy not found\n");
        }
        sb.append("\n");

        // Virtual Environment
        sb.append("Virtual Environment:\n");
        String venvPath = settingsService.getValue(SettingsDefaults.PYTHON_VENV_PATH, "");
        if (venvPath != null && !venvPath.isBlank()) {
            sb.append("  Status: ✓ Active\n");
            sb.append("  Path: ").append(venvPath).append("\n");
        } else {
            sb.append("  Status: Not configured\n");
        }
        sb.append("\n");

        // Active environment
        sb.append("Active: ").append(getActiveEnvironmentName()).append("\n");

        return sb.toString();
    }

    @Override
    public String getExternalPythonPath() {
        // First check configured path
        String configuredPath = settingsService.getValue(SettingsDefaults.PYTHON_EXTERNAL_PATH, "");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return configuredPath;
        }

        // Get from strategy availability info (contains detected path)
        ScriptExecutionStrategy external = findStrategy("python-external");
        if (external != null && external.isAvailable()) {
            String info = external.getAvailabilityInfo();
            // Parse "Python X.Y.Z at /path/to/python"
            if (info.contains(" at ")) {
                return info.substring(info.indexOf(" at ") + 4).trim();
            }
        }
        return null;
    }

    @Override
    public String getGraalPyVersion() {
        ScriptExecutionStrategy embedded = findStrategy("python");
        if (embedded != null && embedded.isAvailable()) {
            String info = embedded.getAvailabilityInfo();
            // Parse "GraalPy X.Y.Z available"
            if (info.startsWith("GraalPy ")) {
                String version = info.substring(8);
                int spaceIdx = version.indexOf(' ');
                if (spaceIdx > 0) {
                    return version.substring(0, spaceIdx);
                }
                return version;
            }
        }
        return null;
    }

    @Override
    public String getExternalPythonVersion() {
        ScriptExecutionStrategy external = findStrategy("python-external");
        if (external != null && external.isAvailable()) {
            String displayName = external.getDisplayName();
            // Parse "Python X.Y.Z (External)"
            if (displayName.startsWith("Python ")) {
                String version = displayName.substring(7);
                int spaceIdx = version.indexOf(' ');
                if (spaceIdx > 0) {
                    return version.substring(0, spaceIdx);
                }
                return version;
            }
        }
        return null;
    }

    @Override
    public boolean runHealthCheck() {
        try {
            String mode = getExecutionMode();
            ScriptExecutionStrategy strategy;

            switch (mode.toLowerCase()) {
                case "external":
                    strategy = findStrategy("python-external");
                    break;
                case "auto":
                    strategy = findStrategy("python-external");
                    if (strategy == null || !strategy.isAvailable()) {
                        strategy = findStrategy("python");
                    }
                    break;
                case "embedded":
                default:
                    strategy = findStrategy("python");
            }

            if (strategy == null) {
                lastHealthCheckResult = "No Python strategy available";
                return false;
            }

            if (!strategy.isAvailable()) {
                lastHealthCheckResult = "Python not available: " + strategy.getAvailabilityInfo();
                return false;
            }

            lastHealthCheckResult = "✓ Python is healthy (" + strategy.getDisplayName() + ")";
            return true;

        } catch (Exception e) {
            lastHealthCheckResult = "Health check failed: " + e.getMessage();
            log.warn("Python health check failed", e);
            return false;
        }
    }

    @Override
    public String getLastHealthCheckResult() {
        return lastHealthCheckResult;
    }

    @Override
    public void refreshStatus() {
        log.info("Refreshing Python status...");

        // Force strategies to re-check availability
        for (ScriptExecutionStrategy strategy : strategies) {
            if (strategy.getLanguageId().contains("python")) {
                // Strategies cache their availability, so we just trigger a check
                strategy.isAvailable();
                log.debug("Refreshed status for {}: {}", strategy.getLanguageId(),
                        strategy.isAvailable() ? "available" : "not available");
            }
        }

        // Run health check
        runHealthCheck();
    }

    /**
     * Find a strategy by language ID.
     */
    private ScriptExecutionStrategy findStrategy(String languageId) {
        for (ScriptExecutionStrategy strategy : strategies) {
            if (strategy.getLanguageId().equals(languageId)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * Shorten a version string for status bar display.
     * "3.12.1" -> "3.12"
     */
    private String shortenVersion(String version) {
        if (version == null) {
            return "?";
        }
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return version;
    }
}
