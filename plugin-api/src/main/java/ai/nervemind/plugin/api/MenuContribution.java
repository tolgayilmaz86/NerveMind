package ai.nervemind.plugin.api;

import java.util.List;

/**
 * Defines a menu item contribution from a plugin.
 * 
 * <p>
 * Plugins can contribute menu items to various locations in the application.
 * Each contribution specifies where it should appear, its label, icon, and
 * the action to perform when clicked.
 * </p>
 * 
 * <h2>Menu Locations</h2>
 * <ul>
 * <li>{@link MenuLocation#FILE} - File menu</li>
 * <li>{@link MenuLocation#EDIT} - Edit menu</li>
 * <li>{@link MenuLocation#WORKFLOW} - Workflow menu</li>
 * <li>{@link MenuLocation#TOOLS} - Tools menu</li>
 * <li>{@link MenuLocation#HELP} - Help menu</li>
 * <li>{@link MenuLocation#CANVAS_CONTEXT} - Right-click context menu on
 * canvas</li>
 * <li>{@link MenuLocation#NODE_CONTEXT} - Right-click context menu on a
 * node</li>
 * <li>{@link MenuLocation#CONNECTION_CONTEXT} - Right-click context menu on a
 * connection</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * @Override
 * public List<MenuContribution> getMenuContributions() {
 *     return List.of(
 *             MenuContribution.item(
 *                     MenuLocation.TOOLS,
 *                     "My Plugin Tool",
 *                     "WRENCH",
 *                     () -> showToolDialog()),
 *             MenuContribution.separator(MenuLocation.TOOLS),
 *             MenuContribution.contextItem(
 *                     MenuLocation.CONNECTION_CONTEXT,
 *                     "Add Label",
 *                     "LABEL",
 *                     context -> showLabelDialog(context.get("connectionId"))));
 * }
 * }</pre>
 * 
 * @param location      where in the menu structure this item should appear
 * @param label         the text to display (null for separators)
 * @param iconName      Material Design icon name, or null for no icon
 * @param action        the action to run when clicked (null for
 *                      context/submenus/separators)
 * @param contextAction the context-aware action for context menus (null for
 *                      regular items)
 * @param children      child menu items for submenus (empty for regular items)
 * @param isSeparator   whether this is a separator line
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#getMenuContributions()
 * @see MenuLocation
 * @see ContextMenuAction
 */
public record MenuContribution(
        MenuLocation location,
        String label,
        String iconName,
        Runnable action,
        ContextMenuAction contextAction,
        List<MenuContribution> children,
        boolean isSeparator) {

    /**
     * Creates a simple menu item with an action.
     * 
     * @param location where to place the menu item
     * @param label    the menu item text
     * @param iconName optional icon name (null for no icon)
     * @param action   the action to perform when clicked
     * @return a new menu item contribution
     */
    public static MenuContribution item(MenuLocation location, String label,
            String iconName, Runnable action) {
        return new MenuContribution(location, label, iconName, action, null, List.of(), false);
    }

    /**
     * Creates a context-aware menu item for context menus.
     * 
     * <p>
     * Use this for menu items in CONNECTION_CONTEXT or NODE_CONTEXT locations
     * where the action needs to know what element was right-clicked.
     * </p>
     * 
     * @param location      where to place the menu item (typically
     *                      CONNECTION_CONTEXT or NODE_CONTEXT)
     * @param label         the menu item text
     * @param iconName      optional icon name (null for no icon)
     * @param contextAction the context-aware action that receives element
     *                      information
     * @return a new context-aware menu item contribution
     * @see ContextMenuAction
     */
    public static MenuContribution contextItem(MenuLocation location, String label,
            String iconName, ContextMenuAction contextAction) {
        return new MenuContribution(location, label, iconName, null, contextAction, List.of(), false);
    }

    /**
     * Creates a submenu with child items.
     * 
     * @param location where to place the submenu
     * @param label    the submenu text
     * @param iconName optional icon name (null for no icon)
     * @param children the child menu items
     * @return a new submenu contribution
     */
    public static MenuContribution submenu(MenuLocation location, String label,
            String iconName, List<MenuContribution> children) {
        return new MenuContribution(location, label, iconName, null, null, children, false);
    }

    /**
     * Creates a separator line in the menu.
     * 
     * @param location where to place the separator
     * @return a new separator contribution
     */
    public static MenuContribution separator(MenuLocation location) {
        return new MenuContribution(location, null, null, null, null, List.of(), true);
    }

    /**
     * Checks if this contribution has child items (is a submenu).
     * 
     * @return true if this is a submenu
     */
    public boolean isSubmenu() {
        return !children.isEmpty();
    }

    /**
     * Checks if this is an actionable item (not separator or submenu).
     * 
     * @return true if this is a clickable item
     */
    public boolean isActionItem() {
        return !isSeparator && (action != null || contextAction != null) && children.isEmpty();
    }

    /**
     * Checks if this is a context-aware action item.
     * 
     * @return true if this has a context action
     */
    public boolean isContextActionItem() {
        return contextAction != null;
    }
}
