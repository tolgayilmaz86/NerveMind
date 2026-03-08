package ai.nervemind.cli;

import ai.nervemind.cli.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * NerveMind CLI - Developer tools for plugin development.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind [GLOBAL OPTIONS] <command> [COMMAND OPTIONS]
 * }</pre>
 * 
 * <p>
 * Global Options:
 * </p>
 * <ul>
 * <li>--verbose, -v: Enable verbose output</li>
 * <li>--help, -h: Show help</li>
 * <li>--version: Show version</li>
 * </ul>
 * 
 * <p>
 * Commands:
 * </p>
 * <ul>
 * <li>create - Create a new plugin from template</li>
 * <li>validate - Validate plugin configuration</li>
 * <li>build - Build the plugin</li>
 * <li>install - Install plugin to local NerveMind</li>
 * <li>list - List installed plugins</li>
 * <li>dev - Run plugin in development mode</li>
 * </ul>
 */
@Command(name = "nervemind", description = "NerveMind CLI - Developer tools for plugin development", version = "1.0.0", subcommands = {
        CreateCommand.class,
        ValidateCommand.class,
        BuildCommand.class,
        InstallCommand.class,
        ListCommand.class,
        DevCommand.class
}, mixinStandardHelpOptions = true)
public class NerveMindCli implements Runnable {

    @CommandLine.Option(names = { "--verbose", "-v" }, description = "Enable verbose output")
    private boolean verbose = false;

    @CommandLine.Option(names = { "--quiet", "-q" }, description = "Suppress non-essential output")
    private boolean quiet = false;

    @CommandLine.Option(names = {
            "--plugins-dir" }, description = "Path to NerveMind plugins directory", defaultValue = "./plugins")
    private String pluginsDir = "./plugins";

    @CommandLine.ArgGroup(exclusive = false)
    private VersionOption versionOption;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new NerveMindCli());
        commandLine.setExecutionStrategy(new CommandLine.RunAll());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // Main command does nothing, just shows help
        if (!verbose && !quiet) {
            System.out.println("NerveMind CLI v1.0.0");
            System.out.println("Use 'nervemind --help' for available commands.");
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    /**
     * Version option for --version flag.
     */
    static class VersionOption {
        @Option(names = { "--version" }, description = "Show version", help = true)
        boolean version;
    }

    /**
     * ANSI colors for terminal output.
     */
    public static class Colors {
        public static final String RESET = "\u001B[0m";
        public static final String RED = "\u001B[31m";
        public static final String GREEN = "\u001B[32m";
        public static final String YELLOW = "\u001B[33m";
        public static final String BLUE = "\u001B[34m";
        public static final String CYAN = "\u001B[36m";
        public static final String BOLD = "\u001B[1m";

        public static String success(String msg) {
            return GREEN + msg + RESET;
        }

        public static String error(String msg) {
            return RED + msg + RESET;
        }

        public static String warning(String msg) {
            return YELLOW + msg + RESET;
        }

        public static String info(String msg) {
            return CYAN + msg + RESET;
        }

        public static String header(String msg) {
            return BOLD + msg + RESET;
        }
    }

    /**
     * Print a success message.
     */
    public void printSuccess(String message) {
        if (!quiet) {
            System.out.println(Colors.success("[OK] ") + message);
        }
    }

    /**
     * Print an error message.
     */
    public void printError(String message) {
        System.out.println(Colors.error("[ERROR] ") + message);
    }

    /**
     * Print a warning message.
     */
    public void printWarning(String message) {
        System.out.println(Colors.warning("[WARN] ") + message);
    }

    /**
     * Print an info message.
     */
    public void printInfo(String message) {
        if (!quiet && verbose) {
            System.out.println(Colors.info("[INFO] ") + message);
        }
    }

    /**
     * Print a header message.
     */
    public void printHeader(String message) {
        if (!quiet) {
            System.out.println();
            System.out.println(Colors.header(message));
            System.out.println("=".repeat(message.length()));
        }
    }
}
