package ai.nervemind.plugin.api;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.enums.TriggerType;

/**
 * Represents a handle (node type) provided by a plugin.
 * 
 * <p>
 * Each handle defines a node that can be used in workflows. A plugin can
 * provide
 * multiple handles, each representing a different operation or trigger type.
 * </p>
 * 
 * <h2>Handle Structure</h2>
 * <p>
 * A handle consists of:
 * </p>
 * <ul>
 * <li><b>id</b> - Unique identifier for this handle within the plugin</li>
 * <li><b>name</b> - Display name shown in the workflow editor</li>
 * <li><b>description</b> - Brief description of what this handle does</li>
 * <li><b>category</b> - UI category for grouping (ACTION, TRIGGER, DATA,
 * etc.)</li>
 * <li><b>triggerType</b> - Type of trigger (null for actions)</li>
 * <li><b>executor</b> - Function that executes the node logic</li>
 * <li><b>schema</b> - JSON Schema for configuration validation</li>
 * <li><b>helpText</b> - Help documentation for users</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * public List<PluginHandle> getHandles() {
 *         return List.of(
 *                         new PluginHandle(
 *                                         "process", // handle ID
 *                                         "Process Data", // display name
 *                                         "Processes input data", // description
 *                                         NodeCategory.UTILITY, // category
 *                                         null, // trigger type (null for actions)
 *                                         this::execute, // executor function
 *                                         this::getSchema, // schema supplier
 *                                         () -> "Enter data to process" // help text
 *                         ));
 * }
 * 
 * private Map<String, Object> execute(
 *                 Map<String, Object> config, // node configuration
 *                 Map<String, Object> inputs) { // inputs from previous nodes
 *         String input = (String) config.get("input");
 *         return Map.of("result", "Processed: " + input);
 * }
 * 
 * private Map<String, Object> getSchema() {
 *         return Map.of(
 *                         "type", "object",
 *                         "properties", Map.of(
 *                                         "input", Map.of(
 *                                                         "type", "string",
 *                                                         "description", "Input value to process")),
 *                         "required", List.of("input"));
 * }
 * }</pre>
 * 
 * @param id          unique identifier for this handle (combined with plugin ID
 *                    to form node type)
 * @param name        display name shown in the workflow editor UI
 * @param description brief description of what this handle does
 * @param category    UI category for grouping in the node palette
 * @param triggerType type of trigger (WEBHOOK, SCHEDULE, FILE, etc.), or null
 *                    for actions
 * @param executor    function that executes the node logic, receiving config
 *                    and inputs
 * @param schema      supplier that returns JSON Schema for configuration
 *                    validation
 * @param helpText    supplier that returns help documentation in Markdown
 *                    format
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#getHandles()
 * @see NodeCategory
 * @see TriggerType
 */
public record PluginHandle(
                /** Unique identifier for this handle within the plugin. */
                String id,

                /** Display name shown in the workflow editor UI. */
                String name,

                /** Brief description of what this handle does. */
                String description,

                /** UI category for grouping in the node palette. */
                NodeCategory category,

                /** Type of trigger (WEBHOOK, SCHEDULE, FILE, etc.), or null for actions. */
                TriggerType triggerType,

                /**
                 * Function that executes the node logic.
                 * 
                 * <p>
                 * Receives two maps:
                 * </p>
                 * <ul>
                 * <li>First map: Node configuration from the workflow</li>
                 * <li>Second map: Inputs from connected previous nodes</li>
                 * </ul>
                 * <p>
                 * Returns a map with the execution results.
                 * </p>
                 */
                BiFunction<Map<String, Object>, Map<String, Object>, Map<String, Object>> executor,

                /**
                 * Supplier that returns JSON Schema for configuration validation.
                 * 
                 * <p>
                 * The schema defines the structure and constraints for the node's
                 * configuration properties.
                 * </p>
                 */
                Supplier<Map<String, Object>> schema,

                /**
                 * Supplier that returns help documentation in Markdown format.
                 * 
                 * <p>
                 * Displayed when users request help for this node type.
                 * </p>
                 */
                Supplier<String> helpText) {
}
