package ai.nervemind.app.executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.app.service.ExecutionService;
import ai.nervemind.app.service.NodeExecutor;
import ai.nervemind.common.domain.Node;

/**
 * Executor for the "executeCommand" node type - runs shell commands on the host
 * system.
 *
 * <p>Executes operating system commands with support for multiple shells,
 * argument
 * interpolation, environment variables, and output capture. Use with caution as
 * this allows arbitrary command execution.</p>
 *
 * <h2>Node Parameters</h2>
 * <table border="1">
 * <caption>Execute command node configuration parameters</caption>
 * <tr><th>Parameter</th><th>Type</th><th>Default</th><th>Description</th></tr>
 * <tr><td>command</td><td>String</td><td>-</td><td><strong>Required.</strong>
 * Command to execute</td></tr>
 * <tr><td>arguments</td><td>List</td><td>[]</td><td>Arguments to pass to
 * command</td></tr>
 * <tr><td>workingDirectory</td><td>String</td><td>system temp</td><td>Working
 * directory</td></tr>
 * <tr><td>timeout</td><td>Integer</td><td>300</td><td>Timeout in
 * seconds</td></tr>
 * <tr><td>shell</td><td>String</td><td>auto</td><td>"cmd", "powershell",
 * "bash", "sh"</td></tr>
 * <tr><td>environment</td><td>Map</td><td>{}</td><td>Environment variables to
 * set</td></tr>
 * <tr><td>captureOutput</td><td>Boolean</td><td>true</td><td>Capture
 * stdout/stderr</td></tr>
 * <tr><td>failOnError</td><td>Boolean</td><td>true</td><td>Fail if exit code is
 * non-zero</td></tr>
 * </table>
 *
 * <h2>Variable Interpolation</h2>
 * <p>Commands and arguments support <code>${variableName}</code> syntax:</p>
 * <pre>{@code
 * {
 * "command": "curl ${url}",
 * "arguments": ["-o", "${outputFile}"]
 * }
 * }</pre>
 *
 * <h2>Shell Detection</h2>
 * <ul>
 * <li><strong>Windows:</strong> Defaults to "cmd" (cmd.exe /c)</li>
 * <li><strong>Linux/Mac:</strong> Defaults to "sh" (sh -c)</li>
 * <li><strong>powershell:</strong> Uses PowerShell with -NoProfile
 * -NonInteractive</li>
 * <li><strong>bash:</strong> Uses bash -c</li>
 * </ul>
 *
 * <h2>Output Data</h2>
 * <table border="1">
 * <caption>Output keys added by this executor</caption>
 * <tr><th>Key</th><th>Type</th><th>Description</th></tr>
 * <tr><td>exitCode</td><td>Integer</td><td>Process exit code (0 =
 * success)</td></tr>
 * <tr><td>stdout</td><td>String</td><td>Standard output captured from
 * process</td></tr>
 * <tr><td>stderr</td><td>String</td><td>Standard error captured from
 * process</td></tr>
 * <tr><td>success</td><td>Boolean</td><td>True if exit code is 0</td></tr>
 * <tr><td>timedOut</td><td>Boolean</td><td>True if command exceeded
 * timeout</td></tr>
 * <tr><td>durationMs</td><td>Long</td><td>Execution time in
 * milliseconds</td></tr>
 * </table>
 *
 * <h2>Security Warning</h2>
 * <p><strong>CAUTION:</strong> This executor runs arbitrary commands on the
 * host system.
 * Never use untrusted input in commands. Consider sandboxing in production.</p>
 *
 * @see CodeExecutor For sandboxed code execution
 */
@Component
public class ExecuteCommandExecutor implements NodeExecutor {

    /**
     * Default constructor for ExecuteCommandExecutor.
     */
    public ExecuteCommandExecutor() {
        // Required for Spring component scanning
    }

    private static final Logger log = LoggerFactory.getLogger(ExecuteCommandExecutor.class);

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private static final String ERROR_KEY = "error";

    private static final String SUCCESS_KEY = "success";

    private static final String EXIT_CODE_KEY = "exitCode";

    private static final String STDOUT_KEY = "stdout";

    private static final String STDERR_KEY = "stderr";

    @Override
    public Map<String, Object> execute(Node node, Map<String, Object> input,
            ExecutionService.ExecutionContext context) {
        Map<String, Object> params = node.parameters();

        String command = interpolate((String) params.get("command"), input);
        if (command == null || command.isBlank()) {
            return Map.of(SUCCESS_KEY, false, ERROR_KEY, "Command is required");
        }

        @SuppressWarnings("unchecked")
        List<String> arguments = (List<String>) params.getOrDefault("arguments", List.of());
        String workingDirectory = (String) params.get("workingDirectory");
        int timeout = ((Number) params.getOrDefault("timeout", 300)).intValue();
        String shell = (String) params.get("shell");
        @SuppressWarnings("unchecked")
        Map<String, String> environment = (Map<String, String>) params.getOrDefault("environment", Map.of());
        boolean captureOutput = (Boolean) params.getOrDefault("captureOutput", true);
        boolean failOnError = (Boolean) params.getOrDefault("failOnError", true);

        // Interpolate arguments
        List<String> interpolatedArgs = arguments.stream()
                .map(arg -> interpolate(arg, input))
                .toList();

        try {
            ProcessResult result = executeCommand(command, interpolatedArgs, workingDirectory,
                    timeout, shell, environment, captureOutput);

            Map<String, Object> output = new HashMap<>(input);
            output.put(EXIT_CODE_KEY, result.exitCode);
            output.put(STDOUT_KEY, result.stdout);
            output.put(STDERR_KEY, result.stderr);
            output.put(SUCCESS_KEY, result.exitCode == 0);
            output.put("timedOut", result.timedOut);
            output.put("executedCommand", command);
            output.put("executedArguments", interpolatedArgs);
            output.put("durationMs", result.durationMs);

            if (failOnError && result.exitCode != 0 && !result.timedOut) {
                output.put(ERROR_KEY, "Command exited with code " + result.exitCode + ": " + result.stderr);
            }

            if (result.timedOut) {
                output.put(ERROR_KEY, "Command timed out after " + timeout + " seconds");
            }

            return output;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Command execution interrupted: {}", command, e);
            Map<String, Object> output = new HashMap<>(input);
            output.put(SUCCESS_KEY, false);
            output.put(ERROR_KEY, "Command execution was interrupted");
            output.put(EXIT_CODE_KEY, -1);
            output.put(STDOUT_KEY, "");
            output.put(STDERR_KEY, "");
            return output;
        } catch (IOException e) {
            log.error("Failed to execute command: {}", command, e);
            Map<String, Object> output = new HashMap<>(input);
            output.put(SUCCESS_KEY, false);
            output.put(ERROR_KEY, e.getMessage());
            output.put(EXIT_CODE_KEY, -1);
            output.put(STDOUT_KEY, "");
            output.put(STDERR_KEY, "");
            return output;
        }
    }

    private ProcessResult executeCommand(String command, List<String> arguments,
            String workingDirectory, int timeout, String shell,
            Map<String, String> environment, boolean captureOutput)
            throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        List<String> commandList = buildCommandList(command, arguments, shell);
        ProcessBuilder pb = configureProcessBuilder(commandList, workingDirectory, environment, captureOutput);
        Process process = pb.start();

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        boolean timedOut = captureOutput
                ? executeWithOutputCapture(process, timeout, stdoutBuilder, stderrBuilder)
                : waitForProcess(process, timeout);

        if (timedOut) {
            return new ProcessResult(-1, "", "Process timed out", true,
                    System.currentTimeMillis() - startTime);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        return new ProcessResult(
                process.exitValue(),
                stdoutBuilder.toString().trim(),
                stderrBuilder.toString().trim(),
                false,
                durationMs);
    }

    private ProcessBuilder configureProcessBuilder(List<String> commandList, String workingDirectory,
            Map<String, String> environment, boolean captureOutput) {
        ProcessBuilder pb = new ProcessBuilder(commandList);
        setWorkingDirectory(pb, workingDirectory);
        pb.environment().putAll(environment);
        if (captureOutput) {
            pb.redirectErrorStream(false);
        }
        return pb;
    }

    private void setWorkingDirectory(ProcessBuilder pb, String workingDirectory) {
        if (workingDirectory == null || workingDirectory.isBlank()) {
            return;
        }
        File workDir = new File(workingDirectory);
        if (workDir.exists() && workDir.isDirectory()) {
            pb.directory(workDir);
        }
    }

    private boolean executeWithOutputCapture(Process process, int timeout,
            StringBuilder stdoutBuilder, StringBuilder stderrBuilder) throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> stdoutFuture = executor
                    .submit(() -> readStream(process.getInputStream(), stdoutBuilder, STDOUT_KEY));
            Future<?> stderrFuture = executor
                    .submit(() -> readStream(process.getErrorStream(), stderrBuilder, STDERR_KEY));
            executor.shutdown();

            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return true;
            }

            waitForOutputReaders(stdoutFuture, stderrFuture);
        }
        return false;
    }

    private void readStream(java.io.InputStream inputStream, StringBuilder builder, String streamName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            log.debug("Error reading {}", streamName, e);
        }
    }

    private void waitForOutputReaders(Future<?> stdoutFuture, Future<?> stderrFuture) {
        try {
            stdoutFuture.get(5, TimeUnit.SECONDS);
            stderrFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            log.debug("Error waiting for output readers", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean waitForProcess(Process process, int timeout) throws InterruptedException {
        boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            return true;
        }
        return false;
    }

    private List<String> buildCommandList(String command, List<String> arguments, String shell) {
        List<String> commandList = new ArrayList<>();

        // Determine shell to use
        String effectiveShell = shell;
        if (effectiveShell == null || effectiveShell.isBlank()) {
            effectiveShell = detectShell();
        }

        // Build full command string
        StringBuilder fullCommand = new StringBuilder(command);
        for (String arg : arguments) {
            fullCommand.append(" ").append(escapeArgument(arg, effectiveShell));
        }

        switch (effectiveShell.toLowerCase()) {
            case "cmd" -> {
                commandList.add("cmd.exe");
                commandList.add("/c");
                commandList.add(fullCommand.toString());
            }
            case "powershell" -> {
                commandList.add("powershell.exe");
                commandList.add("-NoProfile");
                commandList.add("-NonInteractive");
                commandList.add("-Command");
                commandList.add(fullCommand.toString());
            }
            case "bash" -> {
                commandList.add("bash");
                commandList.add("-c");
                commandList.add(fullCommand.toString());
            }
            case "sh" -> {
                commandList.add("sh");
                commandList.add("-c");
                commandList.add(fullCommand.toString());
            }
            default -> {
                // Direct command execution
                commandList.add(command);
                commandList.addAll(arguments);
            }
        }

        return commandList;
    }

    private String detectShell() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("windows")) {
            return "cmd";
        } else {
            return "sh";
        }
    }

    private String escapeArgument(String arg, String shell) {
        if (arg == null || arg.isEmpty()) {
            return "\"\"";
        }

        // Check if escaping is needed
        if (!arg.contains(" ") && !arg.contains("\"") && !arg.contains("'")
                && !arg.contains("&") && !arg.contains("|") && !arg.contains("<")
                && !arg.contains(">")) {
            return arg;
        }

        // Escape based on shell
        if ("powershell".equalsIgnoreCase(shell)) {
            return "'" + arg.replace("'", "''") + "'";
        } else if ("cmd".equalsIgnoreCase(shell)) {
            return "\"" + arg.replace("\"", "\\\"") + "\"";
        } else {
            // Bash/sh
            return "'" + arg.replace("'", "'\\''") + "'";
        }
    }

    private String interpolate(String text, Map<String, Object> data) {
        if (text == null) {
            return null;
        }

        Matcher matcher = INTERPOLATION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = getNestedValue(data, path);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    @Override
    /** @return "executeCommand" */
    public String getNodeType() {
        return "executeCommand";
    }

    /**
     * Internal record to store process execution results.
     * 
     * @param exitCode   the return code from the process
     * @param stdout     captured standard output
     * @param stderr     captured standard error
     * @param timedOut   true if the process was killed due to timeout
     * @param durationMs total execution time in milliseconds
     */
    private record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long durationMs) {
    }
}
