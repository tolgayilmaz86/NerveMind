package ai.nervemind.cli.commands;

import ai.nervemind.cli.NerveMindCli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Lists installed NerveMind plugins.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind list [OPTIONS]
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--dir, -d: Plugins directory</li>
 * <li>--json: Output in JSON format</li>
 * <li>--verbose: Show detailed information</li>
 * </ul>
 */
@Command(name = "list", alias = { "ls", "installed" }, description = "List installed NerveMind plugins")
public class ListCommand implements Callable<Integer> {

    @Option(names = { "--dir", "-d" }, description = "Plugins directory", defaultValue = "./plugins")
    private String pluginsDir = "./plugins";

    @Option(names = { "--json" }, description = "Output in JSON format")
    private boolean jsonOutput = false;

    @Option(names = { "--verbose", "-v" }, description = "Show detailed information")
    private boolean verbose = false;

    @Option(names = { "--enabled-only" }, description = "Show only enabled plugins")
    private boolean enabledOnly = false;

    @Override
    public Integer call() throws Exception {
        Path pluginsPath = Paths.get(pluginsDir);

        if (!Files.exists(pluginsPath)) {
            System.out.println(NerveMindCli.Colors.warning(
                    "Plugins directory not found: " + pluginsPath));
            return 0;
        }

        // Find all JAR files
        List<PluginEntry> plugins = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsPath, "*.jar")) {
            for (Path jarPath : stream) {
                PluginEntry entry = readPluginInfo(jarPath);
                if (entry != null) {
                    // Check if enabled
                    Path enabledMarker = pluginsPath.resolve(
                            jarPath.getFileName().toString().replace(".jar", ".enabled"));
                    entry.enabled = Files.exists(enabledMarker);

                    if (!enabledOnly || entry.enabled) {
                        plugins.add(entry);
                    }
                }
            }
        }

        // Output results
        if (jsonOutput) {
            outputJson(plugins);
        } else {
            outputText(plugins);
        }

        return 0;
    }

    /**
     * Read plugin info from JAR manifest.
     */
    private PluginEntry readPluginInfo(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return new PluginEntry(
                        jarPath.getFileName().toString(),
                        "Unknown",
                        "Unknown",
                        "Unknown",
                        false);
            }

            Properties props = new Properties();
            props.load(jar.getInputStream(manifest.getMainAttributes()));

            String id = props.getProperty("Plugin-Id", "unknown");
            String name = props.getProperty("Plugin-Name", "Unknown");
            String version = props.getProperty("Plugin-Version", "unknown");
            String description = props.getProperty("Plugin-Description", "");

            return new PluginEntry(id, name, version, description, false);

        } catch (IOException e) {
            return new PluginEntry(
                    jarPath.getFileName().toString(),
                    "Error reading plugin",
                    "",
                    "",
                    false);
        }
    }

    /**
     * Output in JSON format.
     */
    private void outputJson(List<PluginEntry> plugins) {
        System.out.println("{");
        System.out.println("  \"count\": " + plugins.size() + ",");
        System.out.println("  \"plugins\": [");

        for (int i = 0; i < plugins.size(); i++) {
            PluginEntry p = plugins.get(i);
            System.out.println("    {");
            System.out.println("      \"id\": \"" + escapeJson(p.id) + "\",");
            System.out.println("      \"name\": \"" + escapeJson(p.name) + "\",");
            System.out.println("      \"version\": \"" + escapeJson(p.version) + "\",");
            System.out.println("      \"enabled\": " + p.enabled + ",");
            System.out.println("      \"file\": \"" + escapeJson(p.file) + "\"");
            System.out.print("    }");
            if (i < plugins.size() - 1)
                System.out.println(",");
            else
                System.out.println();
        }

        System.out.println("  ]");
        System.out.println("}");
    }

    /**
     * Output in text format.
     */
    private void outputText(List<PluginEntry> plugins) {
        System.out.println();
        System.out.println(NerveMindCli.Colors.header("Installed Plugins"));
        System.out.println(NerveMindCli.Colors.header("=".repeat(40)));
        System.out.println();

        if (plugins.isEmpty()) {
            System.out.println("No plugins installed.");
            System.out.println();
            System.out.println("Install plugins with:");
            System.out.println("  nervemind install <plugin.jar>");
            return;
        }

        // Calculate column widths
        int nameWidth = 20;
        int versionWidth = 12;
        int statusWidth = 10;

        for (PluginEntry p : plugins) {
            nameWidth = Math.max(nameWidth, p.name.length());
            versionWidth = Math.max(versionWidth, p.version.length());
        }

        // Header
        String format = "  %-" + nameWidth + "s  %-" + versionWidth + "s  %-" + statusWidth + "s";
        System.out.printf(format, "Name", "Version", "Status");
        System.out.println();
        System.out.println("-".repeat(nameWidth) + "  " + "-".repeat(versionWidth) + "  " + "-".repeat(statusWidth));

        // Rows
        for (PluginEntry p : plugins) {
            String status = p.enabled
                    ? NerveMindCli.Colors.success("[ENABLED]")
                    : NerveMindCli.Colors.warning("[DISABLED]");
            System.out.printf(format,
                    truncate(p.name, nameWidth),
                    truncate(p.version, versionWidth),
                    status);
            System.out.println();
        }

        System.out.println();
        System.out.println("Total: " + plugins.size() + " plugin(s)");

        if (verbose) {
            System.out.println();
            System.out.println(NerveMindCli.Colors.header("Details:"));
            for (PluginEntry p : plugins) {
                System.out.println();
                System.out.println("  " + NerveMindCli.Colors.info(p.name));
                System.out.println("    ID: " + p.id);
                System.out.println("    Version: " + p.version);
                System.out.println("    File: " + p.file);
                System.out.println("    Status: " + (p.enabled ? "Enabled" : "Disabled"));
            }
        }
    }

    /**
     * Escape string for JSON.
     */
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Truncate string to max length.
     */
    private String truncate(String s, int max) {
        if (s.length() <= max)
            return s;
        return s.substring(0, max - 3) + "...";
    }

    /**
     * Plugin entry record.
     */
    private record PluginEntry(
            String id,
            String name,
            String version,
            String description,
            boolean enabled) {
        PluginEntry(String file, String name, String version, String desc, boolean enabled) {
            this(file, name, version, desc, enabled);
        }
    }
}
