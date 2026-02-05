package ai.nervemind.plugin.api;

/**
 * Specifies the location where a menu contribution should be placed.
 * 
 * <p>
 * Menu items can be contributed to the main menu bar or to context menus.
 * Use these locations to specify where plugin menu items should appear.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see MenuContribution
 */
public enum MenuLocation {

    /**
     * The File menu in the main menu bar.
     * Contains file operations like New, Open, Save, Export.
     */
    FILE,

    /**
     * The Edit menu in the main menu bar.
     * Contains editing operations like Undo, Redo, Cut, Copy, Paste.
     */
    EDIT,

    /**
     * The Workflow menu in the main menu bar.
     * Contains workflow operations like Run, Debug, Validate.
     */
    WORKFLOW,

    /**
     * The Tools menu in the main menu bar.
     * Recommended location for plugin-specific tools and utilities.
     */
    TOOLS,

    /**
     * The Help menu in the main menu bar.
     * Contains help and documentation links.
     */
    HELP,

    /**
     * The context menu that appears when right-clicking on the canvas.
     * Shows options for canvas-level operations.
     */
    CANVAS_CONTEXT,

    /**
     * The context menu that appears when right-clicking on a node.
     * Shows options for node-level operations.
     */
    NODE_CONTEXT,

    /**
     * The context menu that appears when right-clicking on a connection.
     * Shows options for connection-level operations like labeling or styling.
     */
    CONNECTION_CONTEXT
}
