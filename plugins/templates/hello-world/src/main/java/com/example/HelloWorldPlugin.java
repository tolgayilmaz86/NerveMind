package com.example;

import ai.nervemind.plugin.api.*;
import java.util.List;
import java.util.Map;

/**
 * A simple Hello World plugin for NerveMind.
 * 
 * <p>
 * This plugin demonstrates the basic structure of a PluginProvider
 * implementation.
 * It takes a name as input and returns a greeting message.
 * </p>
 * 
 * <h2>Features Demonstrated</h2>
 * <ul>
 * <li>Basic plugin structure</li>
 * <li>Required interface methods</li>
 * <li>String property definition</li>
 * <li>Simple execution logic</li>
 * <li>Logging</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <ol>
 * <li>Add the plugin JAR to your NerveMind plugins directory</li>
 * <li>The "Hello World" node will appear in the "Utility" category</li>
 * <li>Configure the "Your Name" property</li>
 * <li>Connect to downstream nodes</li>
 * </ol>
 * 
 * @author Your Name
 * @version 1.0.0
 * @since 1.0.0
 */
public class HelloWorldPlugin implements PluginProvider {

    /**
     * Creates a new HelloWorldPlugin instance.
     * Required for ServiceLoader discovery.
     */
    public HelloWorldPlugin() {
        // No initialization needed
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUIRED: Core Node Definition
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns the unique node type identifier.
     * 
     * <p>
     * This ID must be globally unique. Use reverse domain notation
     * (e.g., "com.example.myplugin") to avoid conflicts.
     * </p>
     * 
     * @return the unique node type identifier
     */
    @Override
    public String getNodeType() {
        return "com.example.helloworld";
    }

    /**
     * Returns the display name shown in the UI palette.
     * 
     * @return a short, human-readable name
     */
    @Override
    public String getDisplayName() {
        return "Hello World";
    }

    /**
     * Returns the description shown as a tooltip.
     * 
     * @return a brief description of what the node does
     */
    @Override
    public String getDescription() {
        return "Returns a personalized greeting message";
    }

    /**
     * Returns the category for grouping in the node palette.
     * 
     * @return the node category
     */
    @Override
    public NodeCategory getCategory() {
        return NodeCategory.UTILITY;
    }

    /**
     * Returns the configurable properties for this node.
     * 
     * <p>
     * Properties are displayed in the properties panel when the node
     * is selected. Values are passed to execute() via the context.
     * </p>
     * 
     * @return list of property definitions
     */
    @Override
    public List<PropertyDefinition> getProperties() {
        return List.of(
                PropertyDefinition.requiredString(
                        "name",
                        "Your Name",
                        "Enter your name to personalize the greeting"),
                PropertyDefinition.optionalString(
                        "greeting",
                        "Greeting",
                        "Hello",
                        "Custom greeting prefix (default: Hello)"));
    }

    /**
     * Executes the node logic.
     * 
     * <p>
     * This method is called when the node runs in a workflow.
     * It receives configuration and input data via the context
     * and returns output data for downstream nodes.
     * </p>
     * 
     * @param context the execution context
     * @return output data as a map of key-value pairs
     */
    @Override
    public Map<String, Object> execute(ExecutionContext context) {
        // Get configuration from properties
        String name = (String) context.getNodeSettings().get("name");
        String greeting = (String) context.getNodeSettings()
                .getOrDefault("greeting", "Hello");

        // Validate input
        if (name == null || name.isBlank()) {
            name = "World";
        }

        // Create greeting message
        String message = greeting + ", " + name + "!";

        // Log progress
        context.logInfo("Generated greeting: " + message);

        // Return output data
        return Map.of(
                "message", message,
                "name", name,
                "greeting", greeting,
                "timestamp", System.currentTimeMillis());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Node Metadata
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Returns the icon name for display in the UI.
     * 
     * <p>
     * Uses Material Design Icons names.
     * See: https://materialdesignicons.com/
     * </p>
     * 
     * @return the icon name (default: "PUZZLE")
     */
    @Override
    public String getIconName() {
        return "GREETING_CARD";
    }

    /**
     * Returns the subtitle displayed below the node name.
     * 
     * @return the subtitle text
     */
    @Override
    public String getSubtitle() {
        return "greeting";
    }

    /**
     * Returns extended help text for the help panel.
     * 
     * <p>
     * Markdown formatting is supported.
     * </p>
     * 
     * @return the help text
     */
    @Override
    public String getHelpText() {
        return """
                ## Hello World Plugin

                Returns a personalized greeting message based on your inputs.

                ### Properties

                | Property | Required | Description |
                |----------|----------|-------------|
                | Your Name | Yes | The name to include in the greeting |
                | Greeting | No | Custom greeting prefix (default: "Hello") |

                ### Output

                | Output | Type | Description |
                |--------|------|-------------|
                | message | String | The complete greeting message |
                | name | String | The name used in the greeting |
                | greeting | String | The greeting prefix used |
                | timestamp | Number | Unix timestamp of execution |

                ### Example

                Input:
                - Name: "Alice"
                - Greeting: "Hi"

                Output:
                - message: "Hi, Alice!"
                """;
    }

    /**
     * Returns the plugin version.
     * 
     * @return semantic version string
     */
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPTIONAL: Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates the node's configuration.
     * 
     * @param settings the node's current settings
     * @return validation result
     */
    @Override
    public ValidationResult validate(Map<String, Object> settings) {
        String name = (String) settings.get("name");

        if (name != null && name.length() > 100) {
            return ValidationResult.invalid(
                    "Name must be 100 characters or less");
        }

        return ValidationResult.valid();
    }
}
