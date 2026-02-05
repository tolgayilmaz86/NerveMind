/**
 * Node type registry and descriptor system.
 * 
 * <p>
 * This package provides a unified system for managing both built-in and
 * plugin-provided node types. It replaces the previous approach of scattered
 * string constants with a type-safe, extensible registry pattern.
 * </p>
 * 
 * <h2>Key Classes</h2>
 * <ul>
 * <li>{@link ai.nervemind.ui.node.NodeTypeDescriptor} - Interface for node type
 * metadata</li>
 * <li>{@link ai.nervemind.ui.node.BuiltInNodeType} - Enum of all built-in node
 * types</li>
 * <li>{@link ai.nervemind.ui.node.NodeTypeRegistry} - Central registry for all
 * node types</li>
 * <li>{@link ai.nervemind.common.enums.NodeCategory} - Categories for
 * organizing
 * nodes in the palette</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * // Initialize registry
 * NodeTypeRegistry registry = new NodeTypeRegistry();
 * registry.updateFromPluginService(pluginService);
 * 
 * // Get a specific node type
 * NodeTypeDescriptor httpRequest = registry.get("httpRequest");
 * String displayName = httpRequest.getDisplayName(); // "HTTP Request"
 * Ikon icon = httpRequest.getIcon();
 * 
 * // Get all nodes in a category
 * List&lt;NodeTypeDescriptor&gt; triggers = registry.getByCategory(NodeCategory.TRIGGER);
 * 
 * // Use built-in node type directly
 * NodeTypeDescriptor manualTrigger = BuiltInNodeType.MANUAL_TRIGGER;
 * addNode(manualTrigger.getId(), manualTrigger.getDisplayName());
 * </pre>
 * 
 * <h2>Benefits</h2>
 * <ul>
 * <li><strong>Type Safety</strong> - Compile-time checking instead of
 * string-based errors</li>
 * <li><strong>Consistency</strong> - Unified handling of built-in and plugin
 * nodes</li>
 * <li><strong>Maintainability</strong> - Centralized node metadata instead of
 * scattered constants</li>
 * <li><strong>Extensibility</strong> - Easy to add new node types</li>
 * </ul>
 * 
 * @since 1.0
 */
package ai.nervemind.ui.node;
