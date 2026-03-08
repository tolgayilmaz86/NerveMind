package ai.nervemind.cli.commands;

import ai.nervemind.cli.NerveMindCli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Installs a NerveMind plugin.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind install [OPTIONS] <plugin-jar>
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--dir, -d: Target plugins directory</li>
 * <li>--enable, -e: Enable plugin after install</li>
 * <li>--backup: Backup existing plugin version</li>
 * </ul>
 */
@Command(name = "install", description = "Install a NerveMind plugin")
public class InstallCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to plugin JAR file", paramLabel = "<plugin-jar>")
    private Path jarPath;

    @Option(names = { "--dir", "-d" }, description = "Target plugins directory", defaultValue = "./plugins")
    private String pluginsDir = "./plugins";

    @Option(names = { "--enable", "-e" }, description = "Enable plugin after install")
    private boolean enableAfter = false;

    @Option(names = { "--backup", "-b" }, description = "Backup existing plugin version")
    private boolean backup = true;

    @Option(names = { "--force", "-f" }, description = "Overwrite existing plugin")
    private boolean force = false;

    @Override
    public Integer call() throws Exception {
        // Validate JAR
        if (!Files.exists(jarPath)) {
            System.err.println(NerveMindCli.Colors.error(
                    "JAR file not found: " + jarPath));
            return 1;
        }

        if (!jarPath.toString().endsWith(".jar")) {
            System.err.println(NerveMindCli.Colors.error(
                    "File must be a JAR: " + jarPath));
            return 1;
        }

        // Extract plugin info from JAR
        PluginInfo info = extractPluginInfo(jarPath);
        if (info == null) {
            System.err.println(NerveMindCli.Colors.error(
                    "Invalid plugin JAR: missing manifest or service registration"));
            return 1;
        }

        System.out.println(NerveMindCli.Colors.header("Installing Plugin"));
        System.out.println("  Name: " + info.name);
        System.out.println("  ID: " + info.id);
        System.out.println("  Version: " + info.version);
        System.out.println();

        // Create plugins directory
        Path targetDir = Paths.get(pluginsDir);
        Files.createDirectories(targetDir);

        // Check for existing plugin
        Path existingPlugin = targetDir.resolve(jarPath.getFileName());
        if (Files.exists(existingPlugin)) {
            if (!force) {
                System.err.println(NerveMindCli.Colors.warning(
                        "Plugin already installed: " + existingPlugin + "\n" +
                                "Use --force to overwrite."));
                return 1;
            }

            if (backup) {
                Path backupPath = targetDir.resolve(
                        jarPath.getFileName().toString().replace(".jar", ".bak"));
                Files.move(existingPlugin, backupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Backed up existing plugin to: " + backupPath);
            }
        }

        // Copy JAR
        Path targetPath = targetDir.resolve(jarPath.getFileName());
        Files.copy(jarPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println(NerveMindCli.Colors.success(
                "Plugin installed to: " + targetPath.toAbsolutePath()));

        // Enable if requested
        if (enableAfter) {
            return enablePlugin(targetPath);
        }

        return 0;
    }

    /**
     * Extract plugin info from JAR manifest.
     */
    private PluginInfo extractPluginInfo(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // Get manifest
            JarEntry manifest = jar.getJarEntry("META-INF/MANIFEST.MF");
            if (manifest == null) {
                return null;
            }

            Properties props = new Properties();
            try (InputStream is = jar.getInputStream(manifest)) {
                props.load(is);
            }

            String id = props.getProperty("Plugin-Id");
            String name = props.getProperty("Plugin-Name");
            String version = props.getProperty("Plugin-Version", "unknown");

            if (id == null || name == null) {
                return null;
            }

            return new PluginInfo(id, name, version);

        } catch (IOException e) {
            System.err.println(NerveMindCli.Colors.error(
                    "Failed to read JAR: " + e.getMessage()));
            return null;
        }
    }

    /**
     * Enable a plugin by creating an enabled marker.
     */
    private int enablePlugin(Path jarPath) {
        Path pluginsDir = jarPath.getParent();
        String pluginName = jarPath.getFileName().toString().replace(".jar", "");
        Path enabledMarker = pluginsDir.resolve(pluginName + ".enabled");

        try {
            Files.writeString(enabledMarker, "");
            System.out.println(NerveMindCli.Colors.success(
                    "Plugin enabled: " + enabledMarker));
            return 0;
        } catch (IOException e) {
            System.err.println(NerveMindCli.Colors.error(
                    "Failed to enable plugin: " + e.getMessage()));
            return 1;
        }
    }

    /**
     * Plugin info record.
     */
    private record PluginInfo(String id, String name, String version) {
    }
}
