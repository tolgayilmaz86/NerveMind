package ai.nervemind.app.executor.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.app.service.SettingsService;
import ai.nervemind.common.domain.Node;

/**
 * External Python execution strategy using subprocess.
 * 
 * <p>
 * Executes Python code using the user's installed Python interpreter via
 * subprocess. This provides full access to the Python ecosystem including
 * pip-installed packages.
 * </p>
 * 
 * <h2>Features</h2>
 * <ul>
 * <li>Full Python standard library access</li>
 * <li>pip packages available</li>
 * <li>Any Python version supported (3.8+)</li>
 * <li>Virtual environment support</li>
 * </ul>
 * 
 * <h2>Limitations</h2>
 * <ul>
 * <li>Requires Python to be installed</li>
 * <li>Slower startup than embedded mode</li>
 * <li>Less sandboxed (can access filesystem)</li>
 * </ul>
 * 
 * <h2>Available Globals</h2>
 * <ul>
 * <li><code>input</code> / <code>$input</code> - Data from previous nodes
 * (dict)</li>
 * <li><code>node</code> / <code>$node</code> - Current node parameters
 * (dict)</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * import json
 * 
 * # Access input data
 * items = input.get('items', [])
 * 
 * # Use any Python feature
 * result = {
 *     'count': len(items),
 *     'processed': [x.upper() for x in items]
 * }
 * return result
 * }</pre>
 */
@Component
public class ExternalPythonExecutionStrategy implements ScriptExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(ExternalPythonExecutionStrategy.class);
    private static final String LANGUAGE_ID = "python-external";

    private final ObjectMapper objectMapper;
    private final SettingsService settingsService;

    // Cache availability check result
    private Boolean available = null;
    private String availabilityMessage = "";
    private Path pythonPath = null;
    private String pythonVersion = null;

    /**
     * Creates a new external Python execution strategy.
     *
     * @param objectMapper    the ObjectMapper for JSON
     *                        serialization/deserialization
     * @param settingsService the settings service for configuration access
     */
    public ExternalPythonExecutionStrategy(ObjectMapper objectMapper, SettingsService settingsService) {
        this.objectMapper = objectMapper;
        this.settingsService = settingsService;
    }

    @Override
    public Map<String, Object> execute(String code, Map<String, Object> input,
            Node node, ExecutionService.ExecutionContext context) throws ScriptExecutionException {

        if (code == null || code.isBlank()) {
            return new HashMap<>(input);
        }

        // Ensure Python is available
        if (!isAvailable()) {
            throw new ScriptExecutionException(
                    "External Python not available: " + availabilityMessage,
                    null, "python-external");
        }

        Path tempDir = null;
        try {
            // Create temp directory for execution
            tempDir = Files.createTempDirectory("nervemind-python-");
            Path inputFile = tempDir.resolve("input.json");
            Path nodeFile = tempDir.resolve("node.json");
            Path outputFile = tempDir.resolve("output.json");
            Path scriptFile = tempDir.resolve("script.py");

            // Write input data as JSON
            objectMapper.writeValue(inputFile.toFile(), input);
            objectMapper.writeValue(nodeFile.toFile(), node.parameters());

            // Create wrapper script
            String wrapper = createWrapperScript(code, inputFile, nodeFile, outputFile);
            Files.writeString(scriptFile, wrapper, StandardCharsets.UTF_8);

            // Get timeout from settings
            long timeout = settingsService.getLong(SettingsDefaults.PYTHON_TIMEOUT, 60000L);

            // Execute Python
            ProcessBuilder pb = new ProcessBuilder(getPythonCommand(), scriptFile.toString());
            pb.directory(tempDir.toFile());
            pb.environment().putAll(getEnvironment());
            pb.redirectErrorStream(false);

            log.debug("Executing external Python: {} {}", getPythonCommand(), scriptFile);

            Process process = pb.start();

            // Read stdout and stderr in separate threads to prevent blocking
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Error reading stdout", e);
                }
            });

            Thread stderrThread = Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    log.warn("Error reading stderr", e);
                }
            });

            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ScriptExecutionException(
                        "Python execution timed out after " + timeout + "ms. "
                                + "Check for infinite loops or increase timeout in Settings → Python.",
                        null, "python-external", code, null);
            }

            // Wait for output readers to finish
            stdoutThread.join(1000);
            stderrThread.join(1000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMessage = stderr.toString().trim();
                if (errorMessage.isEmpty()) {
                    errorMessage = stdout.toString().trim();
                }
                Integer lineNumber = extractLineNumber(errorMessage);
                throw new ScriptExecutionException(
                        "Python script error: " + errorMessage,
                        null, "python-external", code, lineNumber);
            }

            // Read output from file
            if (Files.exists(outputFile)) {
                Map<String, Object> output = new HashMap<>(input);
                Map<String, Object> result = objectMapper.readValue(
                        outputFile.toFile(), new TypeReference<Map<String, Object>>() {
                        });
                output.putAll(result);
                return output;
            } else {
                // No output file means script didn't return anything
                return new HashMap<>(input);
            }

        } catch (ScriptExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ScriptExecutionException(
                    "External Python execution failed: " + e.getMessage(),
                    e, "python-external");
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    deleteRecursively(tempDir);
                } catch (IOException e) {
                    log.warn("Failed to clean up temp directory: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * Create the Python wrapper script that handles I/O and captures return
     * values.
     */
    private String createWrapperScript(String userCode, Path inputFile, Path nodeFile, Path outputFile) {
        String indentedCode = userCode.lines()
                .map(line -> "        " + line)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Escape Windows paths
        String inputPath = inputFile.toString().replace("\\", "\\\\");
        String nodePath = nodeFile.toString().replace("\\", "\\\\");
        String outputPath = outputFile.toString().replace("\\", "\\\\");

        return """
                import json
                import sys
                import traceback

                # Load input data
                with open('%s', 'r', encoding='utf-8') as f:
                    input_data = json.load(f)

                # Load node parameters
                with open('%s', 'r', encoding='utf-8') as f:
                    node_data = json.load(f)

                # Make input available as global variables
                input = input_data
                globals()['$input'] = input_data
                node = node_data
                globals()['$node'] = node_data

                # Execute user code
                def __workflow_main__():
                    try:
                %s
                    except Exception as e:
                        # Re-raise to preserve stack trace
                        raise

                try:
                    __result__ = __workflow_main__()

                    # Determine output
                    if __result__ is not None:
                        if isinstance(__result__, dict):
                            output = __result__
                        else:
                            output = {'result': __result__}
                    else:
                        # Collect any global variables defined by user code
                        output = {}
                        for key, value in list(globals().items()):
                            if (not key.startswith('_') and
                                key not in ['input', 'node', 'json', 'sys', 'traceback',
                                           'input_data', 'node_data', '__workflow_main__', '__result__']):
                                try:
                                    json.dumps(value)  # Check if serializable
                                    output[key] = value
                                except (TypeError, ValueError):
                                    pass

                    # Write output
                    with open('%s', 'w', encoding='utf-8') as f:
                        json.dump(output, f, default=str)

                except Exception:
                    traceback.print_exc()
                    sys.exit(1)
                """.formatted(inputPath, nodePath, indentedCode, outputPath);
    }

    /**
     * Get the Python command to use.
     */
    private String getPythonCommand() {
        if (pythonPath != null) {
            return pythonPath.toString();
        }

        // Check settings first
        String configuredPath = settingsService.getValue(SettingsDefaults.PYTHON_EXTERNAL_PATH, "");
        if (!configuredPath.isBlank()) {
            return configuredPath;
        }

        // Use auto-detected path
        Optional<Path> detected = detectPythonPath();
        return detected.map(Path::toString).orElse("python");
    }

    /**
     * Get environment variables for subprocess.
     */
    private Map<String, String> getEnvironment() {
        Map<String, String> env = new HashMap<>(System.getenv());

        // Add venv activation if configured
        String venvPath = settingsService.getValue(SettingsDefaults.PYTHON_VENV_PATH, "");
        if (!venvPath.isBlank()) {
            Path venv = Path.of(venvPath);
            if (Files.isDirectory(venv)) {
                // Activate virtual environment
                Path binDir = isWindows() ? venv.resolve("Scripts") : venv.resolve("bin");
                String pathSeparator = isWindows() ? ";" : ":";
                env.put("PATH", binDir.toString() + pathSeparator + env.getOrDefault("PATH", ""));
                env.put("VIRTUAL_ENV", venv.toString());
                env.remove("PYTHONHOME");
            }
        }

        // Ensure UTF-8 encoding
        env.put("PYTHONIOENCODING", "utf-8");

        return env;
    }

    /**
     * Extract line number from Python error message.
     */
    private Integer extractLineNumber(String error) {
        // Look for "line X" pattern in traceback
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("line (\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(error);
        if (matcher.find()) {
            try {
                // Subtract wrapper lines to get user code line
                int rawLine = Integer.parseInt(matcher.group(1));
                // The wrapper adds ~30 lines before user code
                return Math.max(1, rawLine - 30);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Recursively delete a directory.
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    @Override
    public String getLanguageId() {
        return LANGUAGE_ID;
    }

    @Override
    public String getDisplayName() {
        if (pythonVersion != null) {
            return "Python " + pythonVersion + " (External)";
        }
        return "Python (External)";
    }

    @Override
    public boolean isAvailable() {
        if (available == null) {
            checkAvailability();
        }
        return available;
    }

    @Override
    public String getAvailabilityInfo() {
        if (available == null) {
            checkAvailability();
        }
        return availabilityMessage;
    }

    private synchronized void checkAvailability() {
        if (available != null) {
            return;
        }

        try {
            // First check configured path
            String configuredPath = settingsService.getValue(SettingsDefaults.PYTHON_EXTERNAL_PATH, "");
            if (!configuredPath.isBlank()) {
                Path path = Path.of(configuredPath);
                if (Files.isExecutable(path)) {
                    pythonPath = path;
                    pythonVersion = getPythonVersion(path);
                    available = pythonVersion != null;
                    availabilityMessage = available
                            ? "Python " + pythonVersion + " at " + path
                            : "Configured Python path is not executable";
                    return;
                }
            }

            // Auto-detect Python
            Optional<Path> detected = detectPythonPath();
            if (detected.isPresent()) {
                pythonPath = detected.get();
                pythonVersion = getPythonVersion(pythonPath);
                available = pythonVersion != null;
                availabilityMessage = available
                        ? "Python " + pythonVersion + " at " + pythonPath
                        : "Found Python but version check failed";
            } else {
                available = false;
                availabilityMessage = "Python not found. Install Python 3.8+ or configure path in Settings → Python.";
            }

            if (available) {
                log.info("External Python available: {}", availabilityMessage);
            } else {
                log.info("External Python not available: {}", availabilityMessage);
            }

        } catch (Exception e) {
            available = false;
            availabilityMessage = "Error checking Python: " + e.getMessage();
            log.warn("Error checking Python availability", e);
        }
    }

    /**
     * Detect Python installation path.
     */
    private Optional<Path> detectPythonPath() {
        List<String> candidates = new ArrayList<>();

        if (isWindows()) {
            // Check common Windows locations
            candidates.add("python");
            candidates.add("python3");
            candidates.add("py");

            // Common install paths
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                candidates.add(localAppData + "\\Programs\\Python\\Python312\\python.exe");
                candidates.add(localAppData + "\\Programs\\Python\\Python311\\python.exe");
                candidates.add(localAppData + "\\Programs\\Python\\Python310\\python.exe");
                candidates.add(localAppData + "\\Programs\\Python\\Python39\\python.exe");
            }

            candidates.add("C:\\Python312\\python.exe");
            candidates.add("C:\\Python311\\python.exe");
            candidates.add("C:\\Python310\\python.exe");
            candidates.add("C:\\Python39\\python.exe");

        } else {
            // Unix-like systems
            candidates.add("python3");
            candidates.add("python");
            candidates.add("/usr/bin/python3");
            candidates.add("/usr/local/bin/python3");
            candidates.add("/opt/homebrew/bin/python3");
        }

        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                pb.redirectErrorStream(true);
                Process process = pb.start();

                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String output = reader.readLine();
                        if (output != null && output.startsWith("Python 3.")) {
                            // Verify it's at least Python 3.8
                            String version = output.replace("Python ", "");
                            String[] parts = version.split("\\.");
                            if (parts.length >= 2) {
                                int minor = Integer.parseInt(parts[1]);
                                if (minor >= 8) {
                                    return Optional.of(Path.of(candidate));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Continue to next candidate
            }
        }

        return Optional.empty();
    }

    /**
     * Get Python version string.
     */
    private String getPythonVersion(Path pythonPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonPath.toString(), "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            if (process.waitFor(5, TimeUnit.SECONDS)) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String output = reader.readLine();
                    if (output != null && output.startsWith("Python ")) {
                        return output.replace("Python ", "");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error getting Python version", e);
        }
        return null;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
