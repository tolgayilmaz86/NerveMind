package ai.nervemind.ui.node;

import org.kordamp.ikonli.Ikon;

import ai.nervemind.common.enums.NodeCategory;

/**
 * Descriptor for a node type, containing metadata for UI presentation.
 * 
 * <p>
 * This interface provides a unified way to describe both built-in nodes
 * and plugin-provided nodes, ensuring consistent handling throughout the
 * application.
 * </p>
 * 
 * <h2>Usage</h2>
 * <ul>
 * <li>Built-in nodes: Implemented by {@link BuiltInNodeType} enum</li>
 * <li>Plugin nodes: Wrapper implementation for
 * {@link ai.nervemind.common.service.PluginServiceInterface.PluginInfo}</li>
 * </ul>
 */
public interface NodeTypeDescriptor {
    /**
     * Get the unique identifier for this node type.
     * 
     * <p>
     * This ID is used in workflow definitions and must match the executor
     * registration.
     * </p>
     * 
     * @return unique node type identifier (e.g., "manualTrigger", "httpRequest")
     */
    String getId();

    /**
     * Get the human-readable display name for this node type.
     * 
     * @return display name shown in the palette and node editor (e.g., "Manual
     *         Trigger")
     */
    String getDisplayName();

    /**
     * Get the icon to display for this node type.
     * 
     * @return icon from MaterialDesign icon set
     */
    Ikon getIcon();

    /**
     * Get the category this node belongs to.
     * 
     * @return category for palette organization
     */
    NodeCategory getCategory();

    /**
     * Check if this is a plugin-provided node.
     * 
     * @return true if this is a plugin node, false for built-in nodes
     */
    default boolean isPlugin() {
        return false;
    }

    /**
     * Get a short subtitle displayed below the node name.
     * 
     * @return short subtitle text (e.g., "trigger", "HTTP", "condition")
     */
    default String getSubtitle() {
        return "";
    }

    /**
     * Get a short help text describing what this node does.
     * 
     * @return brief description of the node's purpose and usage
     */
    default String getHelpText() {
        return "Configure this node's behavior.";
    }

    /**
     * Get the accent color gradient for this node type in the properties panel.
     * 
     * @return CSS linear-gradient string
     */
    default String getAccentColor() {
        // Default blue gradient
        return "linear-gradient(to right, #4a9eff 0%, #6bb3ff 100%)";
    }

    /**
     * Get the icon identifier for this node type in the properties panel.
     * 
     * @return icon identifier (e.g., "mdi2c-cog")
     */
    default String getPropertiesIcon() {
        // Default cog icon
        return "mdi2c-cog";
    }
}
