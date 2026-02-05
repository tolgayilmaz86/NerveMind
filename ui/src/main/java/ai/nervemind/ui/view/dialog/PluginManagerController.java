/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import ai.nervemind.common.service.PluginServiceInterface.PluginInfo;
import ai.nervemind.ui.viewmodel.dialog.PluginManagerViewModel;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX controller for the Plugin Manager dialog.
 * 
 * <p>
 * This controller manages the Plugin Manager UI, which allows users to view,
 * enable, and disable plugins at runtime. It follows the MVVM pattern,
 * delegating
 * business logic to {@link PluginManagerViewModel}.
 * </p>
 * 
 * <h2>MVVM Architecture</h2>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                              Plugin Manager                                 │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────┐ │
 * │  │ PluginManagerDialog │───▶│ PluginManagerController│───▶│ PluginManagerVM│ │
 * │  │      (FXML)         │    │     (Controller)    │    │  (ViewModel)    │ │
 * │  └─────────────────────┘    └─────────────────────┘    └─────────────────┘ │
 * │                                                               │             │
 * │                                                               ▼             │
 * │                                                        ┌─────────────────┐ │
 * │                                                        │ PluginService   │ │
 * │                                                        │    (Model)      │ │
 * │                                                        └─────────────────┘ │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Key Features</h2>
 * <ul>
 * <li><strong>Plugin cards</strong> - Visual cards showing plugin name,
 * description,
 * version, and enabled state</li>
 * <li><strong>Toggle enabled state</strong> - Click button to enable/disable
 * plugins</li>
 * <li><strong>Search/filter</strong> - Filter plugins by name (bound to
 * ViewModel)</li>
 * <li><strong>Reload</strong> - Refresh plugin list after adding new JARs</li>
 * <li><strong>Open folder</strong> - Open the plugins directory in file
 * explorer</li>
 * </ul>
 * 
 * <h2>CSS Classes</h2>
 * <p>
 * The dialog uses BEM-style CSS classes defined in
 * {@code plugin-manager-dialog.css}:
 * </p>
 * <ul>
 * <li>{@code plugin-manager-dialog__card} - Plugin card container</li>
 * <li>{@code plugin-manager-dialog__card--enabled} - Enabled plugin
 * styling</li>
 * <li>{@code plugin-manager-dialog__enable-button--enabled} - Green enabled
 * button</li>
 * <li>{@code plugin-manager-dialog__enable-button--disabled} - Gray disabled
 * button</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <p>
 * This controller is instantiated by
 * {@link DialogFactory#showPluginManager(javafx.stage.Window,
 * ai.nervemind.common.service.PluginServiceInterface)}. Do not instantiate
 * directly.
 * </p>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see PluginManagerViewModel ViewModel containing plugin business logic
 * @see DialogFactory Factory that creates and shows this dialog
 * @see ai.nervemind.common.service.PluginServiceInterface Plugin service
 *      interface
 */
public class PluginManagerController {

    // CSS class constants to avoid duplication
    private static final String CSS_CARD_ICON_ENABLED = "plugin-manager-dialog__card-icon--enabled";
    private static final String CSS_CARD_ICON_DISABLED = "plugin-manager-dialog__card-icon--disabled";
    private static final String CSS_CARD_NAME_DISABLED = "plugin-manager-dialog__card-name--disabled";
    private static final String CSS_ENABLE_BTN_ENABLED = "plugin-manager-dialog__enable-button--enabled";
    private static final String CSS_ENABLE_BTN_DISABLED = "plugin-manager-dialog__enable-button--disabled";
    private static final String CSS_CARD_ENABLED = "plugin-manager-dialog__card--enabled";
    private static final String CSS_CARD_HOVER = "plugin-manager-dialog__card--hover";

    private PluginManagerViewModel viewModel;
    private Stage dialogStage;

    @FXML
    private TextField searchField;

    @FXML
    private Button reloadButton;

    @FXML
    private Button openFolderButton;

    @FXML
    private VBox triggerSection;

    @FXML
    private VBox actionSection;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Label statusLabel;

    /**
     * Initialize the controller with its view model.
     * 
     * @param viewModel   the ViewModel
     * @param dialogStage the dialog stage
     */
    public void initialize(PluginManagerViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupBindings();
        setupPluginLists();
    }

    private void setupBindings() {
        // Search field binding
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        // Status label
        statusLabel.textProperty().bind(viewModel.statusTextProperty());
    }

    private void setupPluginLists() {
        // Initial population
        populateTriggerSection();
        populateActionSection();

        // Listen for changes
        viewModel.getTriggerPlugins().addListener((ListChangeListener<PluginInfo>) c -> populateTriggerSection());
        viewModel.getActionPlugins().addListener((ListChangeListener<PluginInfo>) c -> populateActionSection());
    }

    private void populateTriggerSection() {
        triggerSection.getChildren().clear();

        if (viewModel.getTriggerPlugins().isEmpty()) {
            Label emptyLabel = new Label("No trigger plugins found");
            emptyLabel.getStyleClass().add("plugin-manager-dialog__empty-label");
            triggerSection.getChildren().add(emptyLabel);
        } else {
            for (PluginInfo plugin : viewModel.getTriggerPlugins()) {
                triggerSection.getChildren().add(createPluginCard(plugin));
            }
        }
    }

    private void populateActionSection() {
        actionSection.getChildren().clear();

        if (viewModel.getActionPlugins().isEmpty()) {
            Label emptyLabel = new Label("No action plugins found");
            emptyLabel.getStyleClass().add("plugin-manager-dialog__empty-label");
            actionSection.getChildren().add(emptyLabel);
        } else {
            for (PluginInfo plugin : viewModel.getActionPlugins()) {
                actionSection.getChildren().add(createPluginCard(plugin));
            }
        }
    }

    private HBox createPluginCard(PluginInfo plugin) {
        // Track enabled state for dynamic updates
        final boolean[] isEnabled = { plugin.enabled() };
        boolean isTrigger = viewModel.isTriggerPlugin(plugin);

        // Icon based on type
        FontIcon icon = new FontIcon(isTrigger ? MaterialDesignP.PLAY_CIRCLE : MaterialDesignC.COG);
        icon.setIconSize(28);
        icon.getStyleClass().add(isEnabled[0] ? CSS_CARD_ICON_ENABLED : CSS_CARD_ICON_DISABLED);

        // Name
        Label nameLabel = new Label(plugin.name());
        nameLabel.getStyleClass().add("plugin-manager-dialog__card-name");
        if (!isEnabled[0]) {
            nameLabel.getStyleClass().add(CSS_CARD_NAME_DISABLED);
        }

        // Description
        Label descLabel = new Label(plugin.description());
        descLabel.getStyleClass().add("plugin-manager-dialog__card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(380);

        // Badges row
        Label typeLabel = new Label(plugin.pluginType().getDisplayName());
        typeLabel.getStyleClass().add(isTrigger
                ? "plugin-manager-dialog__badge--trigger"
                : "plugin-manager-dialog__badge--action");

        Label versionLabel = new Label("v" + plugin.version());
        versionLabel.getStyleClass().add("plugin-manager-dialog__badge--version");

        // Source indicator
        boolean isExternal = viewModel.isExternalPlugin(plugin);
        Label sourceLabel = new Label(isExternal ? "JAR" : "Built-in");
        sourceLabel.getStyleClass().add(isExternal
                ? "plugin-manager-dialog__badge--external"
                : "plugin-manager-dialog__badge--builtin");
        sourceLabel.setTooltip(new Tooltip(
                isExternal ? "Loaded from plugins folder" : "Bundled with application"));

        // Node type ID
        Label idLabel = new Label(plugin.id());
        idLabel.getStyleClass().add("plugin-manager-dialog__card-id");

        HBox badges = new HBox(10, typeLabel, versionLabel, sourceLabel);
        badges.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(4, nameLabel, descLabel, badges, idLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Enable/Disable button
        Button enableBtn = new Button(isEnabled[0] ? "Enabled" : "Disabled");
        enableBtn.setPrefWidth(85);
        enableBtn.setMnemonicParsing(false);
        enableBtn.getStyleClass().add(isEnabled[0] ? CSS_ENABLE_BTN_ENABLED : CSS_ENABLE_BTN_DISABLED);

        // Card layout
        HBox card = new HBox(15, icon, textBox, enableBtn);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("plugin-manager-dialog__card");
        if (isEnabled[0]) {
            card.getStyleClass().add(CSS_CARD_ENABLED);
        }

        // Hover effect
        setupCardHoverEffect(card);

        // Button click - toggle enabled state
        enableBtn.setOnAction(e -> togglePluginState(isEnabled, enableBtn, icon, nameLabel, card, plugin));

        return card;
    }

    private void setupCardHoverEffect(HBox card) {
        card.setOnMouseEntered(e -> {
            if (!card.getStyleClass().contains(CSS_CARD_HOVER)) {
                card.getStyleClass().add(CSS_CARD_HOVER);
            }
        });
        card.setOnMouseExited(e -> card.getStyleClass().remove(CSS_CARD_HOVER));
    }

    private void togglePluginState(boolean[] isEnabled, Button enableBtn, FontIcon icon,
            Label nameLabel, HBox card, PluginInfo plugin) {
        boolean newState = !isEnabled[0];
        isEnabled[0] = newState;

        // Update button
        enableBtn.setText(newState ? "Enabled" : "Disabled");
        enableBtn.getStyleClass().removeAll(CSS_ENABLE_BTN_ENABLED, CSS_ENABLE_BTN_DISABLED);
        enableBtn.getStyleClass().add(newState ? CSS_ENABLE_BTN_ENABLED : CSS_ENABLE_BTN_DISABLED);

        // Update icon
        icon.getStyleClass().removeAll(CSS_CARD_ICON_ENABLED, CSS_CARD_ICON_DISABLED);
        icon.getStyleClass().add(newState ? CSS_CARD_ICON_ENABLED : CSS_CARD_ICON_DISABLED);

        // Update name and card
        if (newState) {
            nameLabel.getStyleClass().remove(CSS_CARD_NAME_DISABLED);
            card.getStyleClass().add(CSS_CARD_ENABLED);
        } else {
            nameLabel.getStyleClass().add(CSS_CARD_NAME_DISABLED);
            card.getStyleClass().remove(CSS_CARD_ENABLED);
        }

        // Update service
        viewModel.setPluginEnabled(plugin.id(), newState);
    }

    @FXML
    private void onReload() {
        viewModel.reloadPlugins();
    }

    @FXML
    private void onOpenFolder() {
        try {
            String path = viewModel.getPluginsFolderPath();
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                // Use full path from WINDIR to avoid PATH injection
                String windir = System.getenv("WINDIR");
                if (windir == null) {
                    windir = "C:\\Windows";
                }
                pb = new ProcessBuilder(windir + "\\explorer.exe", path);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("/usr/bin/open", path);
            } else {
                pb = new ProcessBuilder("/usr/bin/xdg-open", path);
            }
            pb.start();
        } catch (Exception _) {
            // Error opening folder is non-critical
        }
    }

    @FXML
    private void onClose() {
        dialogStage.close();
    }
}
