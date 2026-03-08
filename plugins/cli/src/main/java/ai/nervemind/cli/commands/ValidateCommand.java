package ai.nervemind.cli.commands;

import ai.nervemind.cli.NerveMindCli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validates a NerveMind plugin.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind validate [OPTIONS] <plugin-path>
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--strict: Enable strict validation</li>
 * <li>--quick: Quick validation (skip JAR inspection)</li>
 * </ul>
 */
@Command(name = "validate", description = "Validate a NerveMind plugin")
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to plugin directory or JAR file", paramLabel = "<plugin-path>")
    private Path pluginPath;

    @Option(names = { "--strict" }, description = "Enable strict validation")
    private boolean strict = false;

    @Option(names = { "--quick" }, description = "Quick validation (skip JAR inspection)")
    private boolean quick = false;

    @Option(names = { "--json" }, description = "Output in JSON format")
    private boolean jsonOutput = false;

    @Override
    public Integer call() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();

        // Check path exists
        if (!Files.exists(pluginPath)) {
            System.err.println(NerveMindCli.Colors.error("Path does not exist: " + pluginPath));
            return 1;
        }

        // Determine if JAR or directory
        boolean isJar = pluginPath.toString().endsWith(".jar");

        if (isJar) {
            validateJar(pluginPath, errors, warnings, info);
        } else {
            validateDirectory(pluginPath, errors, warnings, info);
        }

        // Output results
        if (jsonOutput) {
            outputJson(errors, warnings, info);
        } else {
            outputText(errors, warnings, info);
        }

        return errors.isEmpty() ? 0 : 1;
    }

    /**
     * Validate a JAR file.
     */
    private void validateJar(Path jarPath, List<String> errors,
            List<String> warnings, List<String> info) {
        info.add("Validating JAR: " + jarPath.getFileName());

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // Check for manifest
            JarEntry manifest = jar.getJarEntry("META-INF/MANIFEST.MF");
            if (manifest == null) {
                errors.add("Missing MANIFEST.MF");
            } else {
                InputStream is = jar.getInputStream(manifest);
                Properties manifestProps = new Properties();
                manifestProps.load(is);
                is.close();

                validateManifest(manifestProps, errors, warnings, info);
            }

            // Check for service file
            JarEntry serviceEntry = jar.getJarEntry(
                    "META-INF/services/ai.nervemind.plugin.api.PluginProvider");
            if (serviceEntry == null) {
                warnings.add("Missing service registration (META-INF/services)");
                warnings.add("Plugin will not be auto-discovered");
            } else {
                info.add("Service registration found");

                // Validate PluginProvider implementation
                if (!quick) {
                    validateServiceImplementation(jar, serviceEntry, errors, warnings, info);
                }
            }

            // Check for required files
            checkJarEntry(jar, "build.gradle", info, warnings, "Build file not found");
            checkJarEntry(jar, "gradle.properties", info, warnings, "gradle.properties not found");

        } catch (IOException e) {
            errors.add("Failed to read JAR: " + e.getMessage());
        }
    }

    /**
     * Validate a plugin directory.
     */
    private void validateDirectory(Path dir, List<String> errors,
            List<String> warnings, List<String> info) {
        info.add("Validating directory: " + dir);

        // Check for required files
        checkFile(dir.resolve("build.gradle"), info, errors, "build.gradle not found");
        checkFile(dir.resolve("gradle.properties"), info, errors, "gradle.properties not found");

        // Check for source directory
        Path srcDir = dir.resolve("src/main/java");
        if (!Files.exists(srcDir)) {
            errors.add("Source directory not found: " + srcDir);
        } else {
            info.add("Source directory found");
            findAndValidatePlugin(dir, errors, warnings, info);
        }

        // Check for service registration
        Path serviceFile = dir.resolve(
                "src/main/resources/META-INF/services/ai.nervemind.plugin.api.PluginProvider");
        if (Files.exists(serviceFile)) {
            info.add("Service registration found");
            validateServiceFile(serviceFile, errors, warnings, info);
        } else {
            warnings.add("Service registration not found");
        }
    }

    /**
     * Validate manifest properties.
     */
    private void validateManifest(Properties props, List<String> errors,
            List<String> warnings, List<String> info) {
        String[] required = { "Plugin-Id", "Plugin-Name", "Plugin-Version" };
        String[] optional = { "Plugin-Description", "Plugin-Provider", "NerveMind-Version" };

        for (String key : required) {
            if (!props.containsKey(key)) {
                errors.add("Missing required manifest property: " + key);
            } else {
                info.add(key + ": " + props.getProperty(key));
            }
        }

        for (String key : optional) {
            if (props.containsKey(key)) {
                info.add(key + ": " + props.getProperty(key));
            }
        }

        // Validate version format
        String version = props.getProperty("Plugin-Version");
        if (version != null && !version.matches("^\\d+\\.\\d+\\.\\d+.*$")) {
            warnings.add("Version does not follow semantic versioning: " + version);
        }
    }

    /**
     * Find and validate plugin Java files.
     */
    private void findAndValidatePlugin(Path dir, List<String> errors,
            List<String> warnings, List<String> info) {
        try {
            Files.walk(dir.resolve("src/main/java"))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(javaFile -> validateJavaFile(javaFile, errors, warnings, info));
        } catch (IOException e) {
            errors.add("Failed to scan source files: " + e.getMessage());
        }
    }

    /**
     * Validate a Java source file.
     */
    private void validateJavaFile(Path javaFile, List<String> errors,
            List<String> warnings, List<String> info) {
        try {
            String content = Files.readString(javaFile);

            // Check for PluginProvider implementation
            if (content.contains("implements PluginProvider")) {
                info.add("Found PluginProvider implementation: " + javaFile.getFileName());

                // Check for required methods
                checkMethod(content, "getId()", "Missing getId() method", errors);
                checkMethod(content, "getName()", "Missing getName() method", errors);
                checkMethod(content, "getDescription()", "Missing getDescription() method", errors);
                checkMethod(content, "getVersion()", "Missing getVersion() method", errors);
                checkMethod(content, "getHandles()", "Missing getHandles() method", errors);

                // Check for deprecated methods
                if (content.contains("getNodeType()")) {
                    warnings.add("Using deprecated getNodeType() method - use getId() instead");
                }
                if (content.contains("getDisplayName()")) {
                    warnings.add("Using deprecated getDisplayName() method - use getName() instead");
                }
            }

        } catch (IOException e) {
            errors.add("Failed to read Java file: " + e.getMessage());
        }
    }

    /**
     * Validate service file content.
     */
    private void validateServiceFile(Path serviceFile, List<String> errors,
            List<String> warnings, List<String> info) {
        try {
            String content = Files.readString(serviceFile).trim();
            if (content.isEmpty()) {
                errors.add("Service file is empty");
                return;
            }

            String[] classes = content.split("\\s+");
            info.add("Service class: " + classes[0]);

            // Check if class exists
            String className = classes[0];
            Path classFile = serviceFile.getParent()
                    .getParent() // services
                    .getParent() // resources
                    .resolve("java")
                    .resolve(className.replace('.', '/') + ".java");

            if (!Files.exists(classFile)) {
                warnings.add("Source file not found for service class (may be in JAR)");
            }

        } catch (IOException e) {
            errors.add("Failed to read service file: " + e.getMessage());
        }
    }

    /**
     * Validate service implementation in JAR.
     */
    private void validateServiceImplementation(JarFile jar, JarEntry serviceEntry,
            List<String> errors, List<String> warnings,
            List<String> info) {
        try {
            InputStream is = jar.getInputStream(serviceEntry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String className = reader.readLine().trim();
            reader.close();

            info.add("Service class: " + className);

            // Would need to load class to validate further
            // For now, just check the class is referenced
            if (className.isEmpty()) {
                errors.add("Service file is empty");
            }

        } catch (IOException e) {
            errors.add("Failed to read service entry: " + e.getMessage());
        }
    }

    /**
     * Check if JAR contains entry.
     */
    private void checkJarEntry(JarFile jar, String path, List<String> info,
            List<String> warnings, String missingMsg) {
        JarEntry entry = jar.getJarEntry(path);
        if (entry != null) {
            info.add("Found: " + path);
        } else {
            warnings.add(missingMsg);
        }
    }

    /**
     * Check if file exists.
     */
    private void checkFile(Path path, List<String> info, List<String> errors, String missingMsg) {
        if (Files.exists(path)) {
            info.add("Found: " + path.getFileName());
        } else {
            errors.add(missingMsg);
        }
    }

    /**
     * Check if method exists in content.
     */
    private void checkMethod(String content, String method, String msg, List<String> errors) {
        if (!content.contains(method)) {
            errors.add(msg);
        }
    }

    /**
     * Output results in JSON format.
     */
    private void outputJson(List<String> errors, List<String> warnings, List<String> info) {
        System.out.println("{");
        System.out.println("  \"valid\": " + errors.isEmpty() + ",");
        System.out.println("  \"errors\": " + formatAsJsonArray(errors) + ",");
        System.out.println("  \"warnings\": " + formatAsJsonArray(warnings) + ",");
        System.out.println("  \"info\": " + formatAsJsonArray(info));
        System.out.println("}");
    }

    /**
     * Format list as JSON array.
     */
    private String formatAsJsonArray(List<String> list) {
        if (list.isEmpty())
            return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
     * Output results in text format.
     */
    private void outputText(List<String> errors, List<String> warnings, List<String> info) {
        System.out.println();

        if (errors.isEmpty()) {
            System.out.println(NerveMindCli.Colors.success("✓ Plugin validation passed"));
        } else {
            System.out.println(NerveMindCli.Colors.error("✗ Plugin validation failed"));
            System.out.println();
            System.out.println(NerveMindCli.Colors.header("Errors:"));
            for (String error : errors) {
                System.out.println("  " + NerveMindCli.Colors.error("• ") + error);
            }
        }

        if (!warnings.isEmpty()) {
            System.out.println();
            System.out.println(NerveMindCli.Colors.header("Warnings:"));
            for (String warning : warnings) {
                System.out.println("  " + NerveMindCli.Colors.warning("• ") + warning);
            }
        }

        if (!info.isEmpty()) {
            System.out.println();
            System.out.println(NerveMindCli.Colors.header("Info:"));
            for (String i : info) {
                System.out.println("  " + NerveMindCli.Colors.info("• ") + i);
            }
        }
    }
}
