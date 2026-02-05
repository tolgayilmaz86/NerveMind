/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.service;

/**
 * Interface for Python environment status information.
 * 
 * <p>
 * Provides information about the current Python execution environment
 * for display in the UI status bar and settings panels:
 * <ul>
 * <li>Current execution mode (embedded/external/auto)</li>
 * <li>Python version and availability</li>
 * <li>Virtual environment status</li>
 * <li>Health check and diagnostics</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public interface PythonStatusServiceInterface {

    // ========== Status Information ==========

    /**
     * Get the current Python execution mode setting.
     *
     * @return "embedded", "external", or "auto"
     */
    String getExecutionMode();

    /**
     * Get the display name for the current active Python environment.
     * 
     * <p>
     * Examples:
     * <ul>
     * <li>"GraalPy 24.1" - embedded mode</li>
     * <li>"Python 3.12.1" - external mode</li>
     * <li>"Python 3.12.1 (venv)" - external with virtual environment</li>
     * <li>"Not Available" - no Python found</li>
     * </ul>
     *
     * @return human-readable Python environment description
     */
    String getActiveEnvironmentName();

    /**
     * Get a short status text suitable for status bar display.
     * 
     * <p>
     * Format: "🐍 GraalPy" or "🐍 Python 3.12" or "⚠️ Python N/A"
     *
     * @return short status text with emoji indicator
     */
    String getStatusBarText();

    /**
     * Check if any Python environment is available.
     *
     * @return true if at least one Python strategy is available
     */
    boolean isPythonAvailable();

    /**
     * Check if the embedded GraalPy is available.
     *
     * @return true if GraalPy can execute Python code
     */
    boolean isEmbeddedAvailable();

    /**
     * Check if external Python is available.
     *
     * @return true if system Python can be found and executed
     */
    boolean isExternalAvailable();

    /**
     * Check if a virtual environment is active (external mode only).
     *
     * @return true if running in a venv
     */
    boolean isVenvActive();

    // ========== Detailed Information ==========

    /**
     * Get detailed availability information.
     * 
     * <p>
     * Returns a multi-line string with:
     * <ul>
     * <li>Embedded status and version</li>
     * <li>External status, path, and version</li>
     * <li>Virtual environment details if active</li>
     * </ul>
     *
     * @return detailed status information
     */
    String getDetailedInfo();

    /**
     * Get the path to the external Python interpreter.
     *
     * @return path string, or null if not configured/detected
     */
    String getExternalPythonPath();

    /**
     * Get the GraalPy version if available.
     *
     * @return version string like "24.1.0", or null if not available
     */
    String getGraalPyVersion();

    /**
     * Get the external Python version if available.
     *
     * @return version string like "3.12.1", or null if not available
     */
    String getExternalPythonVersion();

    // ========== Diagnostics ==========

    /**
     * Run a health check on the Python environment.
     * 
     * <p>
     * This performs a quick test execution to verify the environment
     * is working correctly.
     *
     * @return true if the health check passes
     */
    boolean runHealthCheck();

    /**
     * Get the result of the last health check.
     *
     * @return diagnostic message from last health check
     */
    String getLastHealthCheckResult();

    /**
     * Force refresh of cached availability information.
     * 
     * <p>
     * Call this after changing Python settings or installing Python.
     */
    void refreshStatus();
}
