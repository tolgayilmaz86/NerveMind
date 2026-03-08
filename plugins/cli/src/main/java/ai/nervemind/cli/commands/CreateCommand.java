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
 * Creates a new NerveMind plugin from a template.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>{@code
 * nervemind create [OPTIONS] <plugin-name>
 * }</pre>
 * 
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>--template, -t: Template to use (hello-world, trigger,
 * advanced-multi)</li>
 * <li>--id, -i: Plugin ID (e.g., com.example.myplugin)</li>
 * <li>--name, -n: Display name</li>
 * <li>--description, -d: Plugin description</li>
 * <li>--author, -a: Author name</li>
 * <li>--force, -f: Overwrite existing directory</li>
 * <li>--interactive, -i: Interactive mode (prompt for values)</li>
 * </ul>
 * 
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>{@code
 * # Interactive mode
 * nervemind create my-plugin
 * 
 * # Non-interactive with options
 * nervemind create my-plugin --id=com.example.myplugin --name="My Plugin"
 * 
 * # Specific template
 * nervemind create webhook-plugin --template=trigger
 * }</pre>
 */
@Command(name = "create", description = "Create a new NerveMind plugin from template", alias = { "new",
        "init" }, sortOptions = false)
public class CreateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Name of the plugin to create", paramLabel = "<plugin-name>")
    private String pluginName;

    @Option(names = { "--template",
            "-t" }, description = "Template to use (hello-world, trigger, advanced-multi)", defaultValue = "hello-world")
    private String template = "hello-world";

    @Option(names = { "--id", "-i" }, description = "Plugin ID (e.g., com.example.myplugin)")
    private String pluginId;

    @Option(names = { "--name", "-n" }, description = "Display name for the plugin")
    private String displayName;

    @Option(names = { "--description", "-d" }, description = "Description of the plugin")
    private String description;

    @Option(names = { "--author", "-a" }, description = "Author name", defaultValue = "NerveMind Developer")
    private String author = "NerveMind Developer";

    @Option(names = { "--force", "-f" }, description = "Overwrite existing directory")
    private boolean force = false;

    @Option(names = { "--interactive",
            "-I" }, description = "Run in interactive mode (prompt for values)", arity = "0..0")
    private boolean interactive = false;

    @Option(names = { "--output-dir", "-o" }, description = "Output directory for the plugin", defaultValue = ".")
    private String outputDir = ".";

    @Option(names = { "--nervemind-version" }, description = "NerveMind version requirement", defaultValue = "1.0.0")
    private String nervemindVersion = "1.0.0";

    // Available templates
    private static final Map<String, String> TEMPLATE_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        TEMPLATE_DESCRIPTIONS.put("hello-world", "Simple action plugin (Beginner)");
        TEMPLATE_DESCRIPTIONS.put("trigger", "CRON schedule trigger (Intermediate)");
        TEMPLATE_DESCRIPTIONS.put("advanced-multi", "Multi-handle processor (Advanced)");
    }

    @Override
    public Integer call() throws Exception {
        // Determine if interactive mode
        if (interactive || (pluginId == null && !CommandLine.Help.defaultValueProvider
                .isExplicitlySet("id"))) {
            return interactiveCreate();
        }

        return nonInteractiveCreate();
    }

    /**
     * Interactive creation with prompts.
     */
    private int interactiveCreate() throws IOException {
        System.out.println();
        System.out.println(NerveMindCli.Colors.header("NerveMind Plugin Creator"));
        System.out.println(NerveMindCli.Colors.header("=".repeat(40)));
        System.out.println();

        // Select template
        System.out.println("Choose a template:");
        int idx = 1;
        for (Map.Entry<String, String> entry : TEMPLATE_DESCRIPTIONS.entrySet()) {
            System.out.printf("  %d. %s (%s)%n", idx++, entry.getValue(), entry.getKey());
        }
        System.out.printf("  0. Custom (select features yourself)%n");

        int templateChoice = promptInt("Template", 1, TEMPLATE_DESCRIPTIONS.size());
        if (templateChoice == 0) {
            template = "custom";
        } else {
            template = (String) TEMPLATE_DESCRIPTIONS.keySet().toArray()[templateChoice - 1];
        }

        // Plugin name
        if (pluginName == null || pluginName.isBlank()) {
            pluginName = prompt("Plugin name", "my-plugin");
        }

        // Plugin ID
        if (pluginId == null) {
            String defaultId = "com.example." + pluginName.toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-");
            pluginId = prompt("Plugin ID", defaultId);
        }

        // Display name
        if (displayName == null) {
            displayName = prompt("Display name", capitalize(pluginName));
        }

        // Description
        if (description == null) {
            description = prompt("Description", "A NerveMind plugin");
        }

        // Author
        if (author == null) {
            author = prompt("Author", "NerveMind Developer");
        }

        return createPlugin();
    }

    /**
     * Non-interactive creation using provided options.
     */
    private int nonInteractiveCreate() throws IOException {
        // Set defaults for missing values
        if (pluginId == null) {
            pluginId = "com.example." + pluginName.toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-");
        }

        if (displayName == null) {
            displayName = capitalize(pluginName);
        }

        if (description == null) {
            description = "A NerveMind plugin";
        }

        return createPlugin();
    }

    /**
     * Create the plugin from template.
     */
    private int createPlugin() throws IOException {
        // Find template directory
        Path templateDir = findTemplate(template);
        if (templateDir == null) {
            System.err.println(NerveMindCli.Colors.error(
                    "Template not found: " + template + "\n" +
                            "Available templates: " + String.join(", ", TEMPLATE_DESCRIPTIONS.keySet())));
            return 1;
        }

        // Create output directory
        Path outputPath = Paths.get(outputDir, pluginName);
        if (Files.exists(outputPath) && !force) {
            System.err.println(NerveMindCli.Colors.error(
                    "Directory already exists: " + outputPath + "\n" +
                            "Use --force to overwrite."));
            return 1;
        }

        // Copy and process template
        Map<String, String> context = new LinkedHashMap<>();
        context.put("pluginName", pluginName);
        context.put("pluginId", pluginId);
        context.put("pluginNameCamel", toCamelCase(pluginName));
        context.put("pluginNamePascal", toPascalCase(pluginName));
        context.put("displayName", displayName);
        context.put("description", description);
        context.put("author", author);
        context.put("nervemindVersion", nervemindVersion);
        context.put("year", String.valueOf(java.time.Year.now().getValue()));

        copyTemplate(templateDir, outputPath, context);

        // Print success message
        System.out.println();
        System.out.println(NerveMindCli.Colors.success("Plugin created successfully!"));
        System.out.println();
        System.out.println("Location: " + outputPath.toAbsolutePath());
        System.out.println("Template: " + template);
        System.out.println();
        System.out.println("Next steps:");
        System.out.println("  1. cd " + outputPath);
        System.out.println("  2. ./gradlew build");
        System.out.println("  3. ./gradlew installPlugin");

        return 0;
    }

    /**
     * Copy template files and process placeholders.
     */
    private void copyTemplate(Path templateDir, Path outputPath, Map<String, String> context)
            throws IOException {

        Files.walk(templateDir)
                .filter(path -> !path.equals(templateDir))
                .forEach(source -> {
                    try {
                        Path relative = templateDir.relativize(source);
                        Path target = outputPath.resolve(relative);

                        // Skip hidden files
                        if (relative.getFileName().toString().startsWith(".")) {
                            return;
                        }

                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                            return;
                        }

                        // Read and process file
                        String content = Files.readString(source);
                        for (Map.Entry<String, String> entry : context.entrySet()) {
                            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
                        }

                        // Handle .java file naming
                        String filename = target.getFileName().toString();
                        if (filename.endsWith(".java.template")) {
                            filename = filename.replace(".java.template", ".java");
                            filename = filename.replace("HelloWorld", context.get("pluginNamePascal"));
                            target = target.getParent().resolve(filename);
                        }

                        Files.writeString(target, content);

                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    /**
     * Find template directory.
     */
    private Path findTemplate(String templateName) {
        // Search in multiple locations
        Path[] searchPaths = {
                Paths.get(System.getProperty("user.dir"), "plugins", "templates", templateName),
                Paths.get(System.getProperty("user.dir"), "templates", templateName),
                Paths.get(System.getProperty("user.dir"), "..", "templates", templateName),
                Paths.get(System.getProperty("user.dir"), "..", "..", "templates", templateName),
        };

        for (Path path : searchPaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                return path;
            }
        }

        // Check classpath
        try {
            var stream = getClass().getResourceAsStream("/templates/" + templateName);
            if (stream != null) {
                stream.close();
                return Paths.get("/templates/" + templateName);
            }
        } catch (IOException e) {
            // Ignore
        }

        return null;
    }

    /**
     * Prompt for a string value.
     */
    private String prompt(String label, String defaultValue) {
        System.out.print(label + (defaultValue != null ? " [" + defaultValue + "]" : "") + ": ");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String value = reader.readLine();
            return (value != null && !value.isBlank()) ? value : defaultValue;
        } catch (IOException e) {
            return defaultValue;
        }
    }

    /**
     * Prompt for an integer value.
     */
    private int promptInt(String label, int min, int max) {
        while (true) {
            String value = prompt(label, null);
            try {
                int num = Integer.parseInt(value);
                if (num >= min && num <= max) {
                    return num;
                }
                System.out.println("Please enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
    }

    /**
     * Capitalize first letter.
     */
    private String capitalize(String str) {
        if (str == null || str.isBlank())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Convert to camelCase.
     */
    private String toCamelCase(String str) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : str.toCharArray()) {
            if (c == '-' || c == '_' || c == ' ') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Convert to PascalCase.
     */
    private String toPascalCase(String str) {
        String camel = toCamelCase(str);
        return capitalize(camel);
    }
}
