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
import java.util.stream.Collectors;

/**
 * Builds a NerveMind plugin.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind build [OPTIONS] <plugin-path>
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--clean: Clean before building</li>
 * <li>--test: Run tests</li>
 * <li>--jar: Create JAR file</li>
 * <li>--install: Install to NerveMind after building</li>
 * <li>--parallel: Parallel build</li>
 * </ul>
 */
@Command(name = "build", description = "Build a NerveMind plugin")
public class BuildCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to plugin directory", paramLabel = "<plugin-path>")
    private Path pluginPath;

    @Option(names = { "--clean" }, description = "Clean before building")
    private boolean clean = false;

    @Option(names = { "--test", "-t" }, description = "Run tests")
    private boolean runTests = false;

    @Option(names = { "--jar" }, description = "Create JAR file")
    private boolean createJar = true;

    @Option(names = { "--install", "-i" }, description = "Install to NerveMind after building")
    private boolean installAfter = false;

    @Option(names = { "--parallel", "-p" }, description = "Enable parallel compilation")
    private boolean parallel = false;

    @Option(names = { "--verbose" }, description = "Show build output")
    private boolean verbose = false;

    @Option(names = { "--nerve-mind-dir" }, description = "NerveMind installation directory")
    private String nerveMindDir;

    @Override
    public Integer call() throws Exception {
        // Find build.gradle
        Path buildFile = pluginPath.resolve("build.gradle");
        if (!Files.exists(buildFile)) {
            System.err.println(NerveMindCli.Colors.error(
                    "build.gradle not found in: " + pluginPath));
            return 1;
        }

        // Check if gradlew exists
        Path gradlew = pluginPath.resolve(
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? "gradlew.bat"
                        : "gradlew");

        boolean useWrapper = Files.exists(gradlew);

        // Build command
        List<String> command = new ArrayList<>();
        if (useWrapper) {
            command.add(gradlew.toString());
        } else {
            command.add("gradle");
        }

        // Add tasks
        if (clean) {
            command.add("clean");
        }

        command.add("build");

        // Add options
        if (!runTests) {
            command.add("-x");
            command.add("test");
        }

        if (verbose) {
            command.add("--info");
        }

        // Execute build
        System.out.println(NerveMindCli.Colors.header("Building plugin..."));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(pluginPath.toFile());
        pb.inheritIO();

        if (verbose) {
            pb.redirectErrorStream(true);
        }

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println(NerveMindCli.Colors.error(
                        "Build failed with exit code: " + exitCode));
                return exitCode;
            }

            System.out.println(NerveMindCli.Colors.success("Build successful!"));

            // Find and list built JAR
            if (createJar) {
                Path jarPath = findBuiltJar(pluginPath);
                if (jarPath != null) {
                    System.out.println();
                    System.out.println("JAR created: " + jarPath.toAbsolutePath());

                    // Install if requested
                    if (installAfter) {
                        return installPlugin(jarPath);
                    }
                }
            }

            return 0;

        } catch (IOException | InterruptedException e) {
            System.err.println(NerveMindCli.Colors.error(
                    "Build failed: " + e.getMessage()));
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }
    }

    /**
     * Find the built JAR file.
     */
    private Path findBuiltJar(Path pluginPath) {
        Path buildDir = pluginPath.resolve("build/libs");

        if (!Files.exists(buildDir)) {
            return null;
        }

        try {
            return Files.list(buildDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Install plugin to NerveMind.
     */
    private int installPlugin(Path jarPath) {
        Path targetDir;

        if (nerveMindDir != null) {
            targetDir = Paths.get(nerveMindDir, "plugins");
        } else {
            // Try to find NerveMind
            targetDir = Paths.get(System.getProperty("user.dir"), "plugins");
            if (!Files.exists(targetDir)) {
                targetDir = Paths.get(System.getProperty("user.dir"), "..", "plugins");
                if (!Files.exists(targetDir)) {
                    System.err.println(NerveMindCli.Colors.warning(
                            "Could not find NerveMind plugins directory.\n" +
                                    "Use --nerve-mind-dir to specify the location."));
                    return 0;
                }
            }
        }

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(jarPath.getFileName());
            Files.copy(jarPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(NerveMindCli.Colors.success(
                    "Plugin installed to: " + targetPath.toAbsolutePath()));
            return 0;

        } catch (IOException e) {
            System.err.println(NerveMindCli.Colors.error(
                    "Failed to install plugin: " + e.getMessage()));
            return 1;
        }
    }
}
