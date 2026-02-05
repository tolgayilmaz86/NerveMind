package ai.nervemind.plugin.api;

import java.util.function.Supplier;

/**
 * Defines a side panel contribution from a plugin.
 * 
 * <p>
 * Plugins can contribute custom side panels to the NerveMind workspace.
 * Side panels appear in a collapsible area beside the main canvas and can
 * provide additional tools, information, or controls.
 * </p>
 * 
 * <p>
 * <strong>Note:</strong> The content supplier returns {@code Object} to avoid
 * a direct dependency on JavaFX in the plugin API. Implementations should
 * return
 * a {@code javafx.scene.Node} instance.
 * </p>
 * 
 * <h2>Panel Positions</h2>
 * <ul>
 * <li>{@link PanelPosition#LEFT} - Left side of the canvas</li>
 * <li>{@link PanelPosition#RIGHT} - Right side of the canvas</li>
 * <li>{@link PanelPosition#BOTTOM} - Bottom of the canvas</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * @Override
 * public Optional<SidePanelContribution> getSidePanel() {
 *         return Optional.of(new SidePanelContribution(
 *                         "my-plugin-panel",
 *                         "My Plugin Tools",
 *                         "TOOLBOX",
 *                         PanelPosition.RIGHT,
 *                         300,
 *                         () -> createToolsPanel() // Returns javafx.scene.Node
 *         ));
 * }
 * 
 * private Node createToolsPanel() {
 *         VBox panel = new VBox(10);
 *         panel.getChildren().addAll(
 *                         new Label("Plugin Tools"),
 *                         new Button("Tool 1"),
 *                         new Button("Tool 2"));
 *         return panel;
 * }
 * }</pre>
 * 
 * @param id              unique identifier for this panel
 * @param title           the display title shown in the panel header
 * @param iconName        Material Design icon name for the panel tab
 * @param position        where the panel should appear
 * @param preferredWidth  preferred width in pixels
 * @param contentSupplier factory function that creates the panel content
 *                        (javafx.scene.Node)
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginProvider#getSidePanel()
 * @see PanelPosition
 */
public record SidePanelContribution(
                String id,
                String title,
                String iconName,
                PanelPosition position,
                int preferredWidth,
                Supplier<Object> contentSupplier) {

        /**
         * Creates a right-side panel with default width.
         * 
         * @param id       unique panel identifier
         * @param title    panel title
         * @param iconName icon name
         * @param supplier content factory (should return javafx.scene.Node)
         * @return a new side panel contribution
         */
        public static SidePanelContribution rightPanel(String id, String title,
                        String iconName, Supplier<Object> supplier) {
                return new SidePanelContribution(id, title, iconName,
                                PanelPosition.RIGHT, 300, supplier);
        }

        /**
         * Creates a left-side panel with default width.
         * 
         * @param id       unique panel identifier
         * @param title    panel title
         * @param iconName icon name
         * @param supplier content factory (should return javafx.scene.Node)
         * @return a new side panel contribution
         */
        public static SidePanelContribution leftPanel(String id, String title,
                        String iconName, Supplier<Object> supplier) {
                return new SidePanelContribution(id, title, iconName,
                                PanelPosition.LEFT, 250, supplier);
        }

        /**
         * Creates a bottom panel with default height (width is treated as height).
         * 
         * @param id       unique panel identifier
         * @param title    panel title
         * @param iconName icon name
         * @param supplier content factory (should return javafx.scene.Node)
         * @return a new side panel contribution
         */
        public static SidePanelContribution bottomPanel(String id, String title,
                        String iconName, Supplier<Object> supplier) {
                return new SidePanelContribution(id, title, iconName,
                                PanelPosition.BOTTOM, 200, supplier);
        }
}
