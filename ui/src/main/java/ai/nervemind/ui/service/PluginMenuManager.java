/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.MenuContribution;
import ai.nervemind.common.service.PluginServiceInterface.MenuLocation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Manages plugin menu contributions and converts them to JavaFX menu items.
 * 
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Collecting menu contributions from all enabled plugins</li>
 * <li>Converting contribution records to JavaFX MenuItem instances</li>
 * <li>Adding plugin items to the appropriate menus in the application</li>
 * </ul>
 * <p>
 * Menus include:
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
@Component
public class PluginMenuManager {

    private static final Logger log = LoggerFactory.getLogger(PluginMenuManager.class);

    private final PluginServiceInterface pluginService;

    // Cached menu references for dynamic updates
    private final Map<MenuLocation, Menu> menuMap = new EnumMap<>(MenuLocation.class);
    private final Map<MenuLocation, ContextMenu> contextMenuMap = new EnumMap<>(MenuLocation.class);

    // Track added items for cleanup
    private final List<MenuItem> pluginMenuItems = new ArrayList<>();

    /**
     * Creates a new PluginMenuManager.
     * 
     * @param pluginService the plugin service for getting contributions
     */
    public PluginMenuManager(PluginServiceInterface pluginService) {
        this.pluginService = pluginService;
    }

    /**
     * Registers a menu bar menu for receiving plugin contributions.
     * 
     * @param location the menu location (FILE, EDIT, etc.)
     * @param menu     the JavaFX Menu to add items to
     */
    public void registerMenu(MenuLocation location, Menu menu) {
        menuMap.put(location, menu);
        log.debug("Registered menu for location: {}", location);
    }

    /**
     * Registers a context menu for receiving plugin contributions.
     * 
     * @param location the context menu location (CANVAS_CONTEXT, NODE_CONTEXT)
     * @param menu     the JavaFX ContextMenu to add items to
     */
    public void registerContextMenu(MenuLocation location, ContextMenu menu) {
        contextMenuMap.put(location, menu);
        log.debug("Registered context menu for location: {}", location);
    }

    /**
     * Refreshes all plugin menu contributions.
     * 
     * <p>
     * This method clears existing plugin menu items and re-adds them
     * from the current list of enabled plugins. Call this when plugins
     * are enabled or disabled.
     * </p>
     */
    public void refreshPluginMenus() {
        log.info("Refreshing plugin menu contributions");

        // Remove existing plugin items
        clearPluginMenuItems();

        // Get all contributions from enabled plugins
        List<MenuContribution> contributions = pluginService.getAllMenuContributions();

        // Group by location
        Map<MenuLocation, List<MenuContribution>> byLocation = new EnumMap<>(MenuLocation.class);
        for (MenuContribution contrib : contributions) {
            byLocation.computeIfAbsent(contrib.location(), k -> new ArrayList<>()).add(contrib);
        }

        // Add to each registered menu
        for (Map.Entry<MenuLocation, List<MenuContribution>> entry : byLocation.entrySet()) {
            MenuLocation location = entry.getKey();
            List<MenuContribution> items = entry.getValue();

            if (menuMap.containsKey(location)) {
                addContributionsToMenu(menuMap.get(location), items);
            } else if (contextMenuMap.containsKey(location)) {
                addContributionsToContextMenu(contextMenuMap.get(location), items);
            } else {
                log.warn("No registered menu for location: {}", location);
            }
        }

        log.info("Added {} plugin menu items to {} locations",
                pluginMenuItems.size(), byLocation.size());
    }

    /**
     * Clears all plugin-contributed menu items.
     */
    private void clearPluginMenuItems() {
        for (MenuItem item : pluginMenuItems) {
            if (item.getParentMenu() != null) {
                item.getParentMenu().getItems().remove(item);
            } else if (item.getParentPopup() != null) {
                // Item is in a ContextMenu
                (item.getParentPopup()).getItems().remove(item);
            }
        }
        pluginMenuItems.clear();
    }

    /**
     * Adds contributions to a Menu (from menu bar).
     */
    private void addContributionsToMenu(Menu menu, List<MenuContribution> contributions) {
        // Add separator before plugin items if menu is not empty
        if (!menu.getItems().isEmpty() && !contributions.isEmpty()) {
            SeparatorMenuItem separator = new SeparatorMenuItem();
            separator.setId("plugin-separator");
            menu.getItems().add(separator);
            pluginMenuItems.add(separator);
        }

        for (MenuContribution contrib : contributions) {
            MenuItem item = createMenuItem(contrib);
            menu.getItems().add(item);
            pluginMenuItems.add(item);
        }
    }

    /**
     * Adds contributions to a ContextMenu.
     */
    private void addContributionsToContextMenu(ContextMenu menu, List<MenuContribution> contributions) {
        // Add separator before plugin items if menu is not empty
        if (!menu.getItems().isEmpty() && !contributions.isEmpty()) {
            SeparatorMenuItem separator = new SeparatorMenuItem();
            separator.setId("plugin-separator");
            menu.getItems().add(separator);
            pluginMenuItems.add(separator);
        }

        for (MenuContribution contrib : contributions) {
            MenuItem item = createMenuItem(contrib);
            menu.getItems().add(item);
            pluginMenuItems.add(item);
        }
    }

    /**
     * Creates a JavaFX MenuItem from a MenuContribution.
     */
    private MenuItem createMenuItem(MenuContribution contrib) {
        return createMenuItem(contrib, null);
    }

    /**
     * Creates a JavaFX MenuItem from a MenuContribution with optional context.
     * 
     * @param contrib the menu contribution
     * @param context context data for context-aware actions (may be null)
     * @return the created MenuItem
     */
    private MenuItem createMenuItem(MenuContribution contrib, Map<String, Object> context) {
        if (contrib.isSeparator()) {
            return new SeparatorMenuItem();
        }

        if (contrib.isSubmenu()) {
            return createSubmenu(contrib, context);
        }

        // Regular menu item
        MenuItem item = new MenuItem(contrib.label());
        item.setId("plugin-" + contrib.pluginId() + "-" + contrib.label().toLowerCase().replace(" ", "-"));

        if (contrib.iconName() != null) {
            item.setGraphic(createIcon(contrib.iconName()));
        }

        setMenuItemAction(item, contrib, context);

        return item;
    }

    /**
     * Creates a submenu from a MenuContribution.
     */
    private Menu createSubmenu(MenuContribution contrib, Map<String, Object> context) {
        Menu submenu = new Menu(contrib.label());
        if (contrib.iconName() != null) {
            submenu.setGraphic(createIcon(contrib.iconName()));
        }
        for (MenuContribution child : contrib.children()) {
            submenu.getItems().add(createMenuItem(child, context));
        }
        return submenu;
    }

    /**
     * Sets the action handler for a menu item based on the contribution.
     */
    private void setMenuItemAction(MenuItem item, MenuContribution contrib, Map<String, Object> context) {
        if (contrib.contextAction() != null && context != null) {
            // Filter out null values since Map.copyOf doesn't allow nulls
            final Map<String, Object> contextCopy = context.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey, Map.Entry::getValue));
            item.setOnAction(e -> {
                try {
                    contrib.contextAction().execute(contextCopy);
                } catch (Exception ex) {
                    log.error("Error executing plugin context menu action: {} - {}",
                            contrib.pluginId(), contrib.label(), ex);
                }
            });
        } else if (contrib.action() != null) {
            item.setOnAction(e -> {
                try {
                    contrib.action().run();
                } catch (Exception ex) {
                    log.error("Error executing plugin menu action: {} - {}",
                            contrib.pluginId(), contrib.label(), ex);
                }
            });
        }
    }

    /**
     * Gets menu items for a specific location with context.
     * 
     * <p>
     * This method is useful for dynamically building context menus where the
     * plugin menu items need context information (like connection ID, node ID,
     * etc.)
     * </p>
     * 
     * @param location the menu location to get items for
     * @param context  context data passed to context-aware actions
     * @return list of menu items to add to the context menu
     */
    public List<MenuItem> getMenuItemsForLocation(MenuLocation location, Map<String, Object> context) {
        List<MenuItem> items = new ArrayList<>();

        List<MenuContribution> allContributions = pluginService.getAllMenuContributions();
        log.info("getMenuItemsForLocation: {} - Total contributions: {}, looking for location: {}",
                location, allContributions.size(), location);

        List<MenuContribution> contributions = allContributions
                .stream()
                .filter(c -> c.location() == location)
                .toList();

        log.info("getMenuItemsForLocation: Found {} contributions for location {}",
                contributions.size(), location);

        if (!contributions.isEmpty()) {
            // Add separator before plugin items
            items.add(new SeparatorMenuItem());

            for (MenuContribution contrib : contributions) {
                log.info("Adding menu item: {} for location {}", contrib.label(), location);
                items.add(createMenuItem(contrib, context));
            }
        }

        return items;
    }

    /**
     * Creates a FontIcon from an icon name.
     * 
     * <p>
     * Supports Material Design icon names in UPPER_SNAKE_CASE format.
     * </p>
     */
    private FontIcon createIcon(String iconName) {
        try {
            Ikon ikon = resolveIcon(iconName);
            FontIcon icon = new FontIcon(ikon);
            icon.setIconSize(16);
            return icon;
        } catch (Exception _) {
            log.debug("Could not resolve icon: {}", iconName);
            return new FontIcon(MaterialDesignP.PUZZLE);
        }
    }

    /**
     * Resolves an icon name to an Ikon instance.
     * 
     * <p>
     * Searches through Material Design icon sets to find a match.
     * </p>
     */
    private Ikon resolveIcon(String iconName) {
        // Try common icon sets based on first letter
        String normalized = iconName.toUpperCase().replace("-", "_").replace(" ", "_");

        // Try to find by prefix
        char first = normalized.charAt(0);
        return switch (first) {
            case 'A' -> tryEnum(MaterialDesignA.class, normalized);
            case 'C' -> tryEnum(MaterialDesignC.class, normalized);
            case 'F' -> tryEnum(MaterialDesignF.class, normalized);
            case 'P' -> tryEnum(MaterialDesignP.class, normalized);
            case 'S' -> tryEnum(MaterialDesignS.class, normalized);
            case 'T' -> tryEnum(MaterialDesignT.class, normalized);
            case 'W' -> tryEnum(MaterialDesignW.class, normalized);
            default -> MaterialDesignP.PUZZLE;
        };
    }

    /**
     * Tries to resolve an enum constant by name.
     */
    private <E extends Enum<E> & Ikon> Ikon tryEnum(Class<E> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            return MaterialDesignP.PUZZLE;
        }
    }

    /**
     * Gets the count of registered menu locations.
     * 
     * @return the number of registered menus and context menus
     */
    public int getRegisteredMenuCount() {
        return menuMap.size() + contextMenuMap.size();
    }

    /**
     * Gets the count of currently added plugin menu items.
     * 
     * @return the number of plugin menu items
     */
    public int getPluginMenuItemCount() {
        return pluginMenuItems.size();
    }
}
