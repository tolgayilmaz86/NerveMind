package ai.nervemind.plugin.api;

/**
 * Defines a sample workflow contribution from a plugin.
 * 
 * <p>
 * Plugins can provide sample workflows to help users understand how to use
 * their node types. Samples appear in the Samples Browser dialog alongside
 * built-in samples, with a badge indicating they come from a plugin.
 * </p>
 * 
 * <h2>Sample Content</h2>
 * <p>
 * Samples can be provided either as:
 * </p>
 * <ul>
 * <li><strong>JSON string:</strong> Direct workflow JSON content</li>
 * <li><strong>Resource path:</strong> Path to a JSON file in the plugin
 * JAR</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * @Override
 * public List<SampleDefinition> getSamples() {
 *     return List.of(
 *             SampleDefinition.fromResource(
 *                     "slack-notification-example",
 *                     "Slack Notification Workflow",
 *                     "Demonstrates sending messages to Slack channels",
 *                     "CHAT",
 *                     SampleDifficulty.BEGINNER,
 *                     "/samples/slack-notification.json"),
 *             SampleDefinition.fromJson(
 *                     "slack-approval-flow",
 *                     "Slack Approval Workflow",
 *                     "Interactive approval workflow using Slack",
 *                     "APPROVAL",
 *                     SampleDifficulty.INTERMEDIATE,
 *                     "{\"nodes\": [...], \"edges\": [...]}"));
 * }
 * }</pre>
 * 
 * @param id           unique identifier for this sample
 * @param name         display name shown in the samples browser
 * @param description  detailed description of what the sample demonstrates
 * @param iconName     Material Design icon name
 * @param difficulty   skill level required for this sample
 * @param contentJson  the workflow JSON content (null if using resourcePath)
 * @param resourcePath path to JSON resource in plugin JAR (null if using
 *                     contentJson)
 * @param tags         optional tags for categorization and search
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#getSamples()
 * @see SampleDifficulty
 */
public record SampleDefinition(
        String id,
        String name,
        String description,
        String iconName,
        SampleDifficulty difficulty,
        String contentJson,
        String resourcePath,
        java.util.List<String> tags) {

    /**
     * Creates a sample definition from inline JSON content.
     * 
     * @param id          unique sample identifier
     * @param name        display name
     * @param description sample description
     * @param iconName    icon name
     * @param difficulty  difficulty level
     * @param json        the workflow JSON content
     * @return a new sample definition
     */
    public static SampleDefinition fromJson(String id, String name, String description,
            String iconName, SampleDifficulty difficulty, String json) {
        return new SampleDefinition(id, name, description, iconName, difficulty,
                json, null, java.util.List.of());
    }

    /**
     * Creates a sample definition from a resource file in the plugin JAR.
     * 
     * @param id           unique sample identifier
     * @param name         display name
     * @param description  sample description
     * @param iconName     icon name
     * @param difficulty   difficulty level
     * @param resourcePath path to the JSON resource (e.g., "/samples/example.json")
     * @return a new sample definition
     */
    public static SampleDefinition fromResource(String id, String name, String description,
            String iconName, SampleDifficulty difficulty, String resourcePath) {
        return new SampleDefinition(id, name, description, iconName, difficulty,
                null, resourcePath, java.util.List.of());
    }

    /**
     * Creates a sample definition with tags.
     * 
     * @param id           unique sample identifier
     * @param name         display name
     * @param description  sample description
     * @param iconName     icon name
     * @param difficulty   difficulty level
     * @param resourcePath path to the JSON resource
     * @param tags         categorization tags
     * @return a new sample definition
     */
    public static SampleDefinition withTags(String id, String name, String description,
            String iconName, SampleDifficulty difficulty, String resourcePath,
            java.util.List<String> tags) {
        return new SampleDefinition(id, name, description, iconName, difficulty,
                null, resourcePath, tags);
    }

    /**
     * Checks if this sample uses inline JSON content.
     * 
     * @return true if content is provided inline
     */
    public boolean hasInlineContent() {
        return contentJson != null && !contentJson.isBlank();
    }

    /**
     * Checks if this sample uses a resource file.
     * 
     * @return true if content is in a resource file
     */
    public boolean hasResourcePath() {
        return resourcePath != null && !resourcePath.isBlank();
    }
}
