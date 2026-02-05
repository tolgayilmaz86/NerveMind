package ai.nervemind.ui.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.Ikon;

import ai.nervemind.common.enums.NodeCategory;
import ai.nervemind.common.service.PluginServiceInterface;

/**
 * Central registry for all node types (built-in and plugins).
 * 
 * <p>
 * This registry provides a unified interface for discovering and accessing
 * node type metadata. It automatically includes all built-in node types and
 * can be updated with plugin-provided nodes.
 * </p>
 * 
 * <h2>Usage</h2>
 * 
 * <pre>
 * NodeTypeRegistry registry = new NodeTypeRegistry();
 * registry.updateFromPluginService(pluginService);
 * 
 * // Get a node type
 * NodeTypeDescriptor type = registry.get("httpRequest");
 * 
 * // Get all nodes in a category
 * List&lt;NodeTypeDescriptor&gt; triggers = registry.getByCategory(NodeCategory.TRIGGER);
 * </pre>
 */
public class NodeTypeRegistry {
    private final Map<String, NodeTypeDescriptor> typeMap = new LinkedHashMap<>();

    /**
     * Create a new registry with all built-in node types.
     */
    public NodeTypeRegistry() {
        // Register all built-in node types
        for (BuiltInNodeType type : BuiltInNodeType.values()) {
            typeMap.put(type.getId(), type);
        }
    }

    /**
     * Get a node type by its ID.
     * 
     * @param id the node type ID
     * @return the node type descriptor, or null if not found
     */
    public NodeTypeDescriptor get(String id) {
        return typeMap.get(id);
    }

    /**
     * Get all node types.
     * 
     * @return unmodifiable collection of all registered node types
     */
    public Collection<NodeTypeDescriptor> getAll() {
        return Collections.unmodifiableCollection(typeMap.values());
    }

    /**
     * Get all node types in a specific category.
     * 
     * @param category the category to filter by
     * @return list of node types in the specified category
     */
    public List<NodeTypeDescriptor> getByCategory(NodeCategory category) {
        return typeMap.values().stream()
                .filter(type -> type.getCategory() == category)
                .toList();
    }

    /**
     * Get all categories that have at least one node type.
     * 
     * @return list of categories with nodes
     */
    public List<NodeCategory> getActiveCategories() {
        return Arrays.stream(NodeCategory.values())
                .filter(category -> typeMap.values().stream()
                        .anyMatch(type -> type.getCategory() == category))
                .toList();
    }

    /**
     * Update the registry with plugin-provided node types.
     * 
     * <p>
     * This method removes all previously registered plugin nodes and
     * adds currently enabled plugins. Should be called when plugin state changes.
     * </p>
     * 
     * @param pluginService the plugin service to query for enabled plugins
     */
    public void updateFromPluginService(PluginServiceInterface pluginService) {
        if (pluginService == null) {
            return;
        }

        // Remove all plugin nodes
        typeMap.entrySet().removeIf(entry -> entry.getValue().isPlugin());

        // Add enabled plugin nodes
        for (PluginServiceInterface.PluginInfo plugin : pluginService.getAllDiscoveredPlugins()) {
            if (plugin.enabled()) {
                PluginNodeType pluginNode = new PluginNodeType(plugin);
                typeMap.put(pluginNode.getId(), pluginNode);
            }
        }
    }

    /**
     * Register a custom node type descriptor.
     * 
     * @param descriptor the node type descriptor to register
     */
    public void register(NodeTypeDescriptor descriptor) {
        typeMap.put(descriptor.getId(), descriptor);
    }

    /**
     * Check if a node type is registered.
     * 
     * @param id the node type ID to check
     * @return true if the node type exists
     */
    public boolean contains(String id) {
        return typeMap.containsKey(id);
    }

    /**
     * Wrapper for plugin-provided nodes to adapt them to NodeTypeDescriptor.
     */
    private static class PluginNodeType implements NodeTypeDescriptor {
        private final PluginServiceInterface.PluginInfo pluginInfo;

        PluginNodeType(PluginServiceInterface.PluginInfo pluginInfo) {
            this.pluginInfo = pluginInfo;
        }

        @Override
        public String getId() {
            return pluginInfo.id();
        }

        @Override
        public String getDisplayName() {
            return pluginInfo.name();
        }

        @Override
        public Ikon getIcon() {
            // Use the icon name from the plugin
            return ai.nervemind.ui.util.IconResolver.resolve(pluginInfo.iconName());
        }

        @Override
        public NodeCategory getCategory() {
            // Use the category from the plugin
            return pluginInfo.category();
        }

        @Override
        public boolean isPlugin() {
            return true;
        }

        @Override
        public String getSubtitle() {
            return pluginInfo.subtitle();
        }

        @Override
        public String getHelpText() {
            return pluginInfo.helpText() != null ? pluginInfo.helpText() : pluginInfo.description();
        }
    }
}
