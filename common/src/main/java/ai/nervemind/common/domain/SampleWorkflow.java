package ai.nervemind.common.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents a sample workflow with rich metadata for the samples browser.
 * 
 * <p>
 * Sample workflows are pre-built examples that help users learn NerveMind.
 * Each sample includes the workflow definition, metadata for discovery, and
 * an optional step-by-step tutorial guide.
 * </p>
 * 
 * <h2>Sample JSON Structure</h2>
 * 
 * <pre>
 * {
 *   "id": "hello-world",
 *   "name": "Hello World",
 *   "description": "A simple workflow...",
 *   "category": "Getting Started",
 *   "difficulty": "beginner",
 *   "language": "javascript",
 *   "tags": ["tutorial", "basic"],
 *   "guide": {
 *     "steps": [
 *       { "title": "Step 1", "content": "..." }
 *     ]
 *   },
 *   "workflow": {
 *     "name": "Hello World",
 *     "nodes": [...],
 *     "connections": [...]
 *   }
 * }
 * </pre>
 * 
 * @param id                   Unique sample identifier
 * @param name                 Display name
 * @param description          Sample description
 * @param category             Category for grouping (e.g., "AI", "Data")
 * @param difficulty           Difficulty level (BEGINNER, INTERMEDIATE,
 *                             ADVANCED)
 * @param language             Primary scripting language used
 * @param tags                 Searchable tags
 * @param author               Sample author
 * @param version              Sample version
 * @param guide                Step-by-step tutorial guide
 * @param workflow             The workflow definition
 * @param requiredCredentials  Credentials needed to run this sample
 * @param environmentVariables Environment variables needed
 * @param filePath             Path to source JSON file
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see Difficulty Sample difficulty levels
 * @see Guide Tutorial guide structure
 */
public record SampleWorkflow(
        String id,
        String name,
        String description,
        String category,
        Difficulty difficulty,
        String language,
        List<String> tags,
        String author,
        String version,
        Guide guide,
        Workflow workflow,
        List<String> requiredCredentials,
        List<String> environmentVariables,
        String filePath // Path to source file for reference
) {

    /**
     * Difficulty levels for samples.
     */
    public enum Difficulty {
        /** Beginner level. */
        BEGINNER("⭐", "Beginner"),
        /** Intermediate level. */
        INTERMEDIATE("⭐⭐", "Intermediate"),
        /** Advanced level. */
        ADVANCED("⭐⭐⭐", "Advanced");

        private final String stars;
        private final String label;

        /**
         * @param stars the star rating string
         * @param label the display label
         */
        Difficulty(String stars, String label) {
            this.stars = stars;
            this.label = label;
        }

        /**
         * Gets the star rating string.
         * 
         * @return the star rating string
         */
        public String getStars() {
            return stars;
        }

        /**
         * Gets the display label.
         * 
         * @return the display label
         */
        public String getLabel() {
            return label;
        }

        /**
         * Converts a string value to a Difficulty level.
         * 
         * @param value the difficulty string
         * @return the matching Difficulty level, or BEGINNER if not found
         */
        public static Difficulty fromString(String value) {
            if (value == null) {
                return BEGINNER;
            }
            return switch (value.toLowerCase()) {
                case "intermediate" -> INTERMEDIATE;
                case "advanced" -> ADVANCED;
                default -> BEGINNER;
            };
        }
    }

    /**
     * Guide structure for step-by-step tutorials.
     * 
     * @param steps the list of tutorial steps
     */
    public record Guide(
            List<GuideStep> steps) {
        /**
         * Creates an empty tutorial guide.
         * 
         * @return an empty tutorial guide
         */
        public static Guide empty() {
            return new Guide(List.of());
        }
    }

    /**
     * Individual step in a guide.
     * 
     * @param title          the step title
     * @param content        the step content (markdown)
     * @param highlightNodes list of node IDs to highlight
     * @param codeSnippet    optional code snippet to display
     */
    public record GuideStep(
            String title,
            String content,
            List<String> highlightNodes,
            String codeSnippet) {
        /**
         * Constructs a new GuideStep with title and content.
         * 
         * @param title   the step title
         * @param content the step content
         */
        public GuideStep(String title, String content) {
            this(title, content, List.of(), null);
        }
    }

    /**
     * Create a SampleWorkflow from a workflow with minimal metadata.
     * Used when a sample JSON doesn't have full metadata.
     * 
     * @param workflow the workflow to convert
     * @param filePath the path to the source file
     * @return a new SampleWorkflow instance
     */
    public static SampleWorkflow fromWorkflow(Workflow workflow, String filePath) {
        String id = workflow.id() != null ? workflow.id() : generateId(filePath);
        return new SampleWorkflow(
                id,
                workflow.name(),
                workflow.description(),
                "General",
                Difficulty.BEGINNER,
                detectLanguage(workflow),
                List.of(),
                "NerveMind Team",
                "1.0.0",
                Guide.empty(),
                workflow,
                List.of(),
                List.of(),
                filePath);
    }

    /**
     * Create a SampleWorkflow from a full sample JSON structure.
     * 
     * @param map      the map containing sample data
     * @param filePath the path to the source file
     * @return a new SampleWorkflow instance
     */
    @SuppressWarnings("unchecked")
    public static SampleWorkflow fromMap(Map<String, Object> map, String filePath) {
        // Extract workflow data (can be nested under "workflow" or at root level)
        Map<String, Object> workflowData;
        if (map.containsKey("workflow")) {
            workflowData = (Map<String, Object>) map.get("workflow");
        } else {
            workflowData = map; // Legacy format: workflow data at root
        }

        // Parse the workflow
        Workflow workflow = parseWorkflow(workflowData);

        // Extract metadata (with defaults)
        String id = getString(map, "id", generateId(filePath));
        String name = getString(map, "name", workflow.name());
        String description = getString(map, "description", workflow.description());
        String category = getString(map, "category", "General");
        Difficulty difficulty = Difficulty.fromString(getString(map, "difficulty", "beginner"));
        String language = getString(map, "language", detectLanguage(workflow));
        List<String> tags = getStringList(map, "tags");
        String author = getString(map, "author", "NerveMind Team");
        String version = getString(map, "version", "1.0.0");
        Guide guide = parseGuide(map.get("guide"));
        List<String> requiredCredentials = getStringList(map, "requiredCredentials");
        List<String> environmentVariables = getStringList(map, "environmentVariables");

        return new SampleWorkflow(
                id, name, description, category, difficulty, language,
                tags, author, version, guide, workflow,
                requiredCredentials, environmentVariables, filePath);
    }

    private static String generateId(String filePath) {
        if (filePath == null) {
            return "sample-" + System.currentTimeMillis();
        }
        // Extract filename without extension
        String filename = filePath.substring(filePath.lastIndexOf('/') + 1)
                .replace(".json", "")
                .replace("\\", "/");
        return "sample-" + filename;
    }

    private static String detectLanguage(Workflow workflow) {
        if (workflow == null || workflow.nodes() == null) {
            return "javascript";
        }
        // Check code nodes for language
        for (Node node : workflow.nodes()) {
            if ("code".equals(node.type()) && node.parameters() != null) {
                Object lang = node.parameters().get("language");
                if (lang != null) {
                    return lang.toString().toLowerCase();
                }
            }
        }
        return "javascript";
    }

    @SuppressWarnings("unchecked")
    private static Workflow parseWorkflow(Map<String, Object> data) {
        // Use existing workflow parsing logic
        String id = getString(data, "id", null);
        String name = getString(data, "name", "Untitled");
        String description = getString(data, "description", "");

        List<Node> nodes = ((List<Map<String, Object>>) data.getOrDefault("nodes", List.of()))
                .stream()
                .map(SampleWorkflow::parseNode)
                .toList();

        List<Connection> connections = ((List<Map<String, Object>>) data.getOrDefault("connections", List.of()))
                .stream()
                .map(SampleWorkflow::parseConnection)
                .toList();

        Map<String, Object> settings = (Map<String, Object>) data.getOrDefault("settings", Map.of());

        return new Workflow(id, name, description, nodes, connections, settings);
    }

    @SuppressWarnings("unchecked")
    private static Node parseNode(Map<String, Object> data) {
        Map<String, Object> position = (Map<String, Object>) data.getOrDefault("position", Map.of("x", 0.0, "y", 0.0));
        return new Node(
                getString(data, "id", "node"),
                getString(data, "type", "unknown"),
                getString(data, "name", "Node"),
                new Node.Position(
                        ((Number) position.getOrDefault("x", 0)).doubleValue(),
                        ((Number) position.getOrDefault("y", 0)).doubleValue()),
                (Map<String, Object>) data.getOrDefault("parameters", Map.of()),
                null, // credentialId
                Boolean.TRUE.equals(data.get("disabled")),
                getString(data, "notes", null));
    }

    private static Connection parseConnection(Map<String, Object> data) {
        return new Connection(
                getString(data, "id", null),
                getString(data, "sourceNodeId", ""),
                getString(data, "sourceOutput", "main"),
                getString(data, "targetNodeId", ""),
                getString(data, "targetInput", "main"));
    }

    @SuppressWarnings("unchecked")
    private static Guide parseGuide(Object guideData) {
        if (guideData == null) {
            return Guide.empty();
        }
        if (guideData instanceof Map<?, ?> guideMap) {
            Object stepsObj = guideMap.get("steps");
            if (stepsObj == null) {
                return Guide.empty();
            }
            List<Map<String, Object>> stepsData = (List<Map<String, Object>>) stepsObj;

            List<GuideStep> steps = stepsData.stream()
                    .map(stepData -> new GuideStep(
                            getString(stepData, "title", "Step"),
                            getString(stepData, "content", ""),
                            getStringList(stepData, "highlightNodes"),
                            getString(stepData, "codeSnippet", null)))
                    .toList();

            return new Guide(steps);
        }
        return Guide.empty();
    }

    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }
}
