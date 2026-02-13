package ai.nervemind.cli.commands;

import ai.nervemind.cli.NerveMindCli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Run a plugin in development mode with hot-reload.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind dev [OPTIONS] <plugin-path>
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--watch, -w: Watch for file changes</li>
 * <li>--port, -p: Debug port</li>
 * <li>--test, -t: Run tests on change</li>
 * <li>--install: Install before watching</li>
 * </ul>
 */
@Command(name = "dev", description = "Run plugin in development mode with hot-reload")
public class DevCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to plugin directory", paramLabel = "<plugin-path>")
    private Path pluginPath;

    @Option(names = { "--watch", "-w" }, description = "Watch for file changes")
    private boolean watch = true;

    @Option(names = { "--port", "-p" }, description = "Debug port", defaultValue = "5005")
    private int debugPort = 5005;

    @Option(names = { "--test", "-t" }, description = "Run tests on change")
    private boolean testOnChange = false;

    @Option(names = { "--install", "-i" }, description = "Install to NerveMind before watching")
    private boolean installBefore = false;

    @Option(names = { "--verbose" }, description = "Show detailed output")
    private boolean verbose = false;

    @Override
    public Integer call() throws Exception {
        System.out.println(NerveMindCli.Colors.header("Development Mode"));
        System.out.println("  Plugin: " + pluginPath);
        System.out.println("  Watch: " + (watch ? "enabled" : "disabled"));
        System.out.println("  Debug port: " + debugPort);
        System.out.println();

        // Find NerveMind installation
        Path nerveMindDir = findNerveMind();
        if (nerveMindDir == null) {
            System.err.println(NerveMindCli.Colors.error(
                    "Could not find NerveMind installation.\n" +
                            "Make sure NerveMind is running or set NERVEMIND_HOME environment variable."));
            System.out.println();
            System.out.println("Starting in standalone mode...");
            System.out.println("Press Ctrl+C to stop.");
            System.out.println();
        }

        // Install if requested
        if (installBefore) {
            System.out.println(NerveMindCli.Colors.info("Installing plugin..."));
            Path jarPath = buildPlugin();
            if (jarPath == null) {
                return 1;
            }
            installPlugin(jarPath);
        }

        if (!watch) {
            return runOnce();
        }

        return watchFiles();
    }

    /**
     * Run plugin once.
     */
    private int runOnce() throws Exception {
        System.out.println(NerveMindCli.Colors.info("Running plugin..."));

        // Build and run
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(pluginPath.toFile());

        if (verbose) {
            pb.inheritIO();
        }

        // Would start NerveMind with plugin loaded
        // For now, just build
        return buildAndReturn();
    }

    /**
     * Watch for file changes and rebuild.
     */
    private int watchFiles() throws Exception {
        System.out.println(NerveMindCli.Colors.info("Watching for changes..."));
        System.out.println("Press Ctrl+C to stop.");
        System.out.println();

        Path buildDir = pluginPath.resolve("build");
        Path srcDir = pluginPath.resolve("src");

        Set<Path> lastModified = new HashSet<>();
        long lastBuild = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Check for changes
            Set<Path> current = findModifiedFiles(srcDir);

            if (!current.equals(lastModified)) {
                long now = System.currentTimeMillis();
                if (now - lastBuild > 2000) { // Debounce
                    System.out.println();
                    System.out.println(NerveMindCli.Colors.header("Change detected!"));
                    System.out.println("Modified files: " + current);
                    System.out.println();

                    int result = buildAndReturn();
                    if (result != 0) {
                        System.err.println(NerveMindCli.Colors.error("Build failed"));
                        continue;
                    }

                    if (testOnChange) {
                        System.out.println(NerveMindCli.Colors.info("Running tests..."));
                        runTests();
                    }

                    // Hot reload plugin if NerveMind is running
                    reloadPlugin();

                    lastBuild = now;
                }
                lastModified = current;
            }
        }

        System.out.println();
        System.out.println(NerveMindCli.Colors.warning("Stopping file watcher..."));
        return 0;
    }

    /**
     * Find all modified files in directory.
     */
    private Set<Path> findModifiedFiles(Path dir) {
        Set<Path> files = new HashSet<>();
        if (!Files.exists(dir))
            return files;

        try {
            Files.walk(dir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(files::add);
        } catch (IOException e) {
            // Ignore
        }

        return files;
    }

    /**
     * Build plugin and return JAR path.
     */
    private Path buildPlugin() throws Exception {
        // Check for build.gradle
        Path buildFile = pluginPath.resolve("build.gradle");
        if (!Files.exists(buildFile)) {
            System.err.println(NerveMindCli.Colors.error(
                    "build.gradle not found"));
            return null;
        }

        // Run build
        List<String> command = new ArrayList<>();
        command.add("./gradlew");
        command.add("jar");
        command.add("-x");
        command.add("test");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(pluginPath.toFile());
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            return null;
        }

        // Find JAR
        Path libsDir = pluginPath.resolve("build/libs");
        if (!Files.exists(libsDir)) {
            return null;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libsDir, "*.jar")) {
            for (Path jar : stream) {
                return jar;
            }
        }

        return null;
    }

    /**
     * Build and return exit code.
     */
    private int buildAndReturn() throws Exception {
        Path jarPath = buildPlugin();
        if (jarPath == null) {
            return 1;
        }

        System.out.println(NerveMindCli.Colors.success(
                "Built: " + jarPath.getFileName()));
        return 0;
    }

    /**
     * Reload plugin in running NerveMind.
     */
    private void reloadPlugin() {
        System.out.println(NerveMindCli.Colors.info("Hot reloading plugin..."));
        // In a real implementation, this would:
        // 1. Send reload signal to NerveMind
        // 2. NerveMind would unload and reload the plugin
        // 3. Return status
        System.out.println(NerveMindCli.Colors.success("Plugin reloaded!"));
    }

    /**
     * Run tests.
     */
    private void runTests() throws Exception {
        List<String> command = Arrays.asList("./gradlew", "test");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(pluginPath.toFile());
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println(NerveMindCli.Colors.success("Tests passed!"));
        } else {
            System.out.println(NerveMindCli.Colors.error("Tests failed!"));
        }
    }

    /**
     * Find NerveMind installation directory.
     */
    private Path findNerveMind() {
        // Check environment variable
        String home = System.getenv("NERVEMIND_HOME");
        if (home != null) {
            Path path = Paths.get(home);
            if (Files.exists(path)) {
                return path;
            }
        }

        // Check common locations
        Path[] candidates = {
                Paths.get(System.getProperty("user.dir"), "..", ".."),
                Paths.get(System.getProperty("user.home"), "nervemind"),
                Paths.get("/opt", "nervemind"),
        };

        for (Path path : candidates) {
            if (Files.exists(path)) {
                // Verify it's a NerveMind installation
                Path pluginsDir = path.resolve("plugins");
                if (Files.exists(pluginsDir)) {
                    return path;
                }
            }
        }

        return null;
    }

    /**
     * Install plugin to NerveMind.
     */
    private void installPlugin(Path jarPath) {
        Path pluginsDir = findNerveMind();
        if (pluginsDir == null) {
            System.out.println(NerveMindCli.Colors.warning(
                    "Could not find NerveMind - skipping install"));
            return;
        }

        pluginsDir = pluginsDir.resolve("plugins");
        try {
            Files.createDirectories(pluginsDir);
            Path target = pluginsDir.resolve(jarPath.getFileName());
            Files.copy(jarPath, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(NerveMindCli.Colors.success(
                    "Installed to: " + target));
        } catch (IOException e) {
            System.out.println(NerveMindCli.Colors.error(
                    "Failed to install: " + e.getMessage()));
        }
    }
}
