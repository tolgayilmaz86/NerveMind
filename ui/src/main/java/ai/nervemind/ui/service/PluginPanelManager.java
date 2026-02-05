/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.common.service.PluginServiceInterface.PanelPosition;
import ai.nervemind.common.service.PluginServiceInterface.SidePanelContribution;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Manages plugin side panel contributions and their lifecycle.
 * 
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Collecting side panel contributions from all enabled plugins</li>
 * <li>Creating JavaFX containers for plugin panels</li>
 * <li>Managing panel visibility and collapse state</li>
 * <li>Integrating panels into the main application layout</li>
 * </ul>
 * <p>
 * Panels include:
 * </p>
 * 
 * <h2>Panel Integration</h2>
 * <p>
 * Plugin panels are displayed in accordion-style containers on the left,
 * right, or bottom of the main canvas area. Each panel has:
 * <ul>
 * <li>A header with title and icon</li>
 * <li>A collapsible content area</li>
 * <li>A close button to hide the panel</li>
 * </ul>
 * <h2>Usage</h2>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
@Component
public class PluginPanelManager {

    private static final Logger log = LoggerFactory.getLogger(PluginPanelManager.class);

    private final PluginServiceInterface pluginService;

    // Panel containers by position
    private final Map<PanelPosition, VBox> panelContainers = new EnumMap<>(PanelPosition.class);

    // Currently loaded panels
    private final Map<String, PanelEntry> loadedPanels = new HashMap<>();

    // Toggle buttons in sidebar (for showing/hiding panels)
    private final Map<String, ToggleButton> panelToggleButtons = new HashMap<>();

    // Callback for when panels change
    private Consumer<PanelPosition> panelChangeCallback;

    /**
     * Creates a new PluginPanelManager.
     * 
     * @param pluginService the plugin service for getting contributions
     */
    public PluginPanelManager(PluginServiceInterface pluginService) {
        this.pluginService = pluginService;
    }

    /**
     * Sets the callback to be invoked when panels are added or removed.
     * 
     * @param callback the callback function receiving the affected position
     */
    public void setOnPanelChange(Consumer<PanelPosition> callback) {
        this.panelChangeCallback = callback;
    }

    /**
     * Registers a container for a specific panel position.
     * 
     * @param position  the panel position (LEFT, RIGHT, BOTTOM)
     * @param container the VBox container to hold panels
     */
    public void registerPanelContainer(PanelPosition position, VBox container) {
        panelContainers.put(position, container);
        log.debug("Registered panel container for position: {}", position);
    }

    /**
     * Creates the panel containers for all positions.
     * 
     * <p>
     * Returns a map of position to VBox container that can be added to the
     * application layout.
     * </p>
     * 
     * @return map of panel containers by position
     */
    public Map<PanelPosition, VBox> createPanelContainers() {
        Map<PanelPosition, VBox> containers = new EnumMap<>(PanelPosition.class);

        for (PanelPosition position : PanelPosition.values()) {
            VBox container = createPanelContainer(position);
            containers.put(position, container);
            panelContainers.put(position, container);
        }

        return containers;
    }

    /**
     * Creates a container for panels at a specific position.
     */
    private VBox createPanelContainer(PanelPosition position) {
        VBox container = new VBox(5);
        container.getStyleClass().add("plugin-panel-container");
        container.setPadding(new Insets(5));

        // Set preferred size based on position
        if (position == PanelPosition.BOTTOM) {
            container.setPrefHeight(200);
            container.setMaxHeight(300);
        } else {
            container.setPrefWidth(300);
            container.setMaxWidth(400);
        }

        // Initially hidden
        container.setVisible(false);
        container.setManaged(false);

        return container;
    }

    /**
     * Refreshes all plugin panel contributions.
     * 
     * <p>
     * This method clears existing panels and recreates them from the
     * current list of enabled plugins. Call this when plugins are
     * enabled or disabled.
     * </p>
     */
    public void refreshPluginPanels() {
        log.info("Refreshing plugin panel contributions");

        // Clear existing panels
        clearPluginPanels();

        // Get all contributions from enabled plugins
        List<SidePanelContribution> contributions = pluginService.getAllSidePanelContributions();

        // Create panels for each contribution
        for (SidePanelContribution contrib : contributions) {
            try {
                createPanel(contrib);
            } catch (Exception e) {
                log.error("Failed to create panel '{}' from plugin '{}': {}",
                        contrib.id(), contrib.pluginId(), e.getMessage(), e);
            }
        }

        // Update container visibility
        updateContainerVisibility();

        log.info("Loaded {} plugin panels", loadedPanels.size());
    }

    /**
     * Clears all plugin-contributed panels.
     */
    private void clearPluginPanels() {
        for (PanelEntry entry : loadedPanels.values()) {
            VBox container = panelContainers.get(entry.position);
            if (container != null) {
                container.getChildren().remove(entry.panelNode);
            }
        }
        loadedPanels.clear();
        panelToggleButtons.clear();
    }

    /**
     * Creates a panel from a contribution.
     */
    private void createPanel(SidePanelContribution contrib) {
        // Create the panel content
        Object content = contrib.contentSupplier().get();
        if (!(content instanceof Node contentNode)) {
            log.warn("Panel '{}' did not return a valid Node", contrib.id());
            return;
        }

        // Create the titled pane wrapper
        TitledPane titledPane = new TitledPane();
        titledPane.setText(contrib.title());
        titledPane.setExpanded(true);
        titledPane.setAnimated(true);
        titledPane.getStyleClass().add("plugin-panel");
        titledPane.setId("plugin-panel-" + contrib.id());

        // Add icon to title
        if (contrib.iconName() != null) {
            FontIcon icon = createIcon(contrib.iconName());
            titledPane.setGraphic(icon);
        }

        // Wrap content in scroll pane
        ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("plugin-panel-content");

        titledPane.setContent(scrollPane);

        // Add to container
        VBox container = panelContainers.get(contrib.position());
        if (container != null) {
            container.getChildren().add(titledPane);
            VBox.setVgrow(titledPane, Priority.ALWAYS);
        }

        // Track the panel
        loadedPanels.put(contrib.id(), new PanelEntry(
                contrib.id(),
                contrib.pluginId(),
                contrib.position(),
                titledPane,
                true));

        log.debug("Created plugin panel '{}' at position {}",
                contrib.id(), contrib.position());
    }

    /**
     * Updates the visibility of panel containers based on their content.
     */
    private void updateContainerVisibility() {
        for (Map.Entry<PanelPosition, VBox> entry : panelContainers.entrySet()) {
            PanelPosition position = entry.getKey();
            VBox container = entry.getValue();

            boolean hasVisiblePanels = loadedPanels.values().stream()
                    .anyMatch(p -> p.position == position && p.visible);

            container.setVisible(hasVisiblePanels);
            container.setManaged(hasVisiblePanels);

            if (panelChangeCallback != null) {
                panelChangeCallback.accept(position);
            }
        }
    }

    /**
     * Shows a specific panel.
     * 
     * @param panelId the panel ID to show
     */
    public void showPanel(String panelId) {
        PanelEntry entry = loadedPanels.get(panelId);
        if (entry != null) {
            entry.visible = true;
            entry.panelNode.setVisible(true);
            entry.panelNode.setManaged(true);
            updateContainerVisibility();
        }
    }

    /**
     * Hides a specific panel.
     * 
     * @param panelId the panel ID to hide
     */
    public void hidePanel(String panelId) {
        PanelEntry entry = loadedPanels.get(panelId);
        if (entry != null) {
            entry.visible = false;
            entry.panelNode.setVisible(false);
            entry.panelNode.setManaged(false);
            updateContainerVisibility();
        }
    }

    /**
     * Toggles the visibility of a panel.
     * 
     * @param panelId the panel ID to toggle
     */
    public void togglePanel(String panelId) {
        PanelEntry entry = loadedPanels.get(panelId);
        if (entry != null) {
            if (entry.visible) {
                hidePanel(panelId);
            } else {
                showPanel(panelId);
            }
        }
    }

    /**
     * Gets the panel container for a specific position.
     * 
     * @param position the panel position
     * @return the container, or null if not registered
     */
    public VBox getContainer(PanelPosition position) {
        return panelContainers.get(position);
    }

    /**
     * Gets all loaded panel IDs.
     * 
     * @return list of panel IDs
     */
    public List<String> getPanelIds() {
        return new ArrayList<>(loadedPanels.keySet());
    }

    /**
     * Gets information about a specific panel.
     * 
     * @param panelId the panel ID
     * @return panel entry, or null if not found
     */
    public PanelEntry getPanel(String panelId) {
        return loadedPanels.get(panelId);
    }

    /**
     * Gets the count of loaded panels.
     * 
     * @return the number of loaded panels
     */
    public int getPanelCount() {
        return loadedPanels.size();
    }

    /**
     * Creates toggle buttons for the sidebar to show/hide panels.
     * 
     * @return list of toggle buttons for plugin panels
     */
    public List<ToggleButton> createSidebarToggleButtons() {
        List<ToggleButton> buttons = new ArrayList<>();

        for (PanelEntry entry : loadedPanels.values()) {
            ToggleButton button = new ToggleButton();
            button.setId("toggle-panel-" + entry.id);
            button.setSelected(entry.visible);
            button.getStyleClass().add("sidebar-button");
            button.setTooltip(new Tooltip(entry.id));

            // Create icon
            FontIcon icon = new FontIcon(MaterialDesignP.PUZZLE_OUTLINE);
            icon.setIconSize(20);
            button.setGraphic(icon);

            // Handle toggle
            button.setOnAction(e -> togglePanel(entry.id));

            buttons.add(button);
            panelToggleButtons.put(entry.id, button);
        }

        return buttons;
    }

    /**
     * Creates a FontIcon from an icon name.
     */
    private FontIcon createIcon(String iconName) {
        try {
            // Simple icon resolution - expand as needed
            FontIcon icon = new FontIcon(MaterialDesignP.PUZZLE);
            icon.setIconSize(16);
            return icon;
        } catch (Exception _) {
            return new FontIcon(MaterialDesignP.PUZZLE);
        }
    }

    /**
     * Entry representing a loaded panel.
     */
    public static class PanelEntry {
        /** Unique identifier for the panel. */
        public final String id;
        /** ID of the plugin that owns this panel. */
        public final String pluginId;
        /** Position where the panel should be displayed. */
        public final PanelPosition position;
        /** The UI component representing the panel. */
        public final TitledPane panelNode;
        /** Whether the panel is currently visible. */
        public boolean visible;

        PanelEntry(String id, String pluginId, PanelPosition position,
                TitledPane panelNode, boolean visible) {
            this.id = id;
            this.pluginId = pluginId;
            this.position = position;
            this.panelNode = panelNode;
            this.visible = visible;
        }
    }
}
