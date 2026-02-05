/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;

import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.service.PluginServiceInterface;
import ai.nervemind.ui.viewmodel.dialog.SettingsDialogViewModel;
import ai.nervemind.ui.viewmodel.dialog.SettingsDialogViewModel.CategoryItem;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * Controller for the Settings dialog FXML.
 * 
 * <p>
 * Binds the FXML view to the {@link SettingsDialogViewModel}.
 */
public class SettingsDialogController {

    private static final String STYLE_CLASS_TEXT_FIELD = "settings-dialog__text-field";

    private SettingsDialogViewModel viewModel;
    private Stage dialogStage;
    private final Map<String, Node> controlMap = new HashMap<>();

    // Sidebar
    @FXML
    private ListView<CategoryItem> categoryList;

    // Header
    @FXML
    private HBox categoryHeader;

    @FXML
    private FontIcon categoryIcon;

    @FXML
    private Label categoryTitle;

    @FXML
    private Label categoryDescription;

    // Content
    @FXML
    private ScrollPane settingsScrollPane;

    @FXML
    private VBox settingsContainer;

    // Footer
    @FXML
    private Button resetButton;

    @FXML
    private Button applyButton;

    @FXML
    private Button cancelButton;

    /**
     * Initialize the controller with its view model.
     * 
     * @param viewModel   the ViewModel
     * @param dialogStage the dialog stage
     */
    public void initialize(SettingsDialogViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupCategorySidebar();
        setupBindings();

        // Select first category
        if (!viewModel.getCategories().isEmpty()) {
            categoryList.getSelectionModel().selectFirst();
        }
    }

    private void setupCategorySidebar() {
        categoryList.setItems(viewModel.getCategories());
        categoryList.setCellFactory(lv -> new CategoryListCell());

        // Bind selection to ViewModel
        categoryList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    viewModel.setSelectedCategory(newVal);
                    if (newVal != null) {
                        updateCategoryContent(newVal);
                    }
                });
    }

    private void setupBindings() {
        // Disable apply button when no changes
        applyButton.disableProperty().bind(viewModel.hasChangesProperty().not());
    }

    private void updateCategoryContent(CategoryItem category) {
        // Update header
        categoryIcon.setIconLiteral(category.iconLiteral());
        categoryTitle.setText(category.name());
        categoryDescription.setText(category.description());

        // Clear and rebuild settings
        settingsContainer.getChildren().clear();
        controlMap.clear();

        // Special handling for PLUGINS category - show checkboxes for each plugin
        if (category.category() == SettingCategory.PLUGINS) {
            buildPluginCheckboxes();
            return;
        }

        // Group settings by section
        List<SettingDTO> settings = viewModel.getCurrentSettings();
        Map<String, VBox> sections = new HashMap<>();

        for (SettingDTO setting : settings) {
            String sectionName = SettingsDialogViewModel.extractSection(setting.key());
            VBox section = sections.computeIfAbsent(sectionName, name -> {
                VBox box = createSection(name);
                settingsContainer.getChildren().add(box);
                return box;
            });

            Node control = createSettingControl(setting);
            section.getChildren().add(control);
            controlMap.put(setting.key(), control);
        }
    }

    /**
     * Builds the plugin enable/disable checkboxes for the PLUGINS category.
     */
    private void buildPluginCheckboxes() {
        List<PluginServiceInterface.PluginInfo> plugins = viewModel.getAllDiscoveredPlugins();

        if (plugins.isEmpty()) {
            Label noPlugins = new Label("No plugins discovered. Place plugin JAR files in the 'plugins' folder.");
            noPlugins.getStyleClass().add("settings-dialog__setting-description");
            noPlugins.setWrapText(true);
            settingsContainer.getChildren().add(noPlugins);
            return;
        }

        VBox section = createSection("Discovered Plugins");
        settingsContainer.getChildren().add(section);

        for (PluginServiceInterface.PluginInfo plugin : plugins) {
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("settings-dialog__setting-row");

            // Plugin info (left side)
            VBox infoBox = new VBox(2);
            infoBox.setMinWidth(300);
            infoBox.setMaxWidth(400);

            HBox nameRow = new HBox(8);
            nameRow.setAlignment(Pos.CENTER_LEFT);

            FontIcon pluginIcon = new FontIcon(MaterialDesignP.PUZZLE);
            pluginIcon.setIconSize(16);

            Label nameLabel = new Label(plugin.name());
            nameLabel.getStyleClass().add("settings-dialog__setting-label");

            Label versionLabel = new Label("v" + plugin.version());
            versionLabel.getStyleClass().add("settings-dialog__setting-description");
            versionLabel.setStyle("-fx-font-size: 11px;");

            Label typeLabel = new Label("[" + (plugin.isTrigger() ? "trigger" : "action") + "]");
            typeLabel.getStyleClass().add("settings-dialog__setting-description");
            typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

            nameRow.getChildren().addAll(pluginIcon, nameLabel, versionLabel, typeLabel);

            Label descLabel = new Label(plugin.description());
            descLabel.getStyleClass().add("settings-dialog__setting-description");
            descLabel.setWrapText(true);

            infoBox.getChildren().addAll(nameRow, descLabel);
            row.getChildren().add(infoBox);

            // Spacer
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);

            // Enable checkbox (right side)
            CheckBox enableCheckbox = new CheckBox("Enabled");
            enableCheckbox.setSelected(viewModel.isPluginEnabled(plugin.id()));
            enableCheckbox.getStyleClass().add("settings-dialog__toggle");
            enableCheckbox.selectedProperty()
                    .addListener((obs, oldVal, newVal) -> viewModel.setPluginEnabled(plugin.id(), newVal));
            row.getChildren().add(enableCheckbox);

            section.getChildren().add(row);
        }
    }

    private VBox createSection(String sectionKey) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label(SettingsDialogViewModel.formatSectionName(sectionKey));
        titleLabel.getStyleClass().add("settings-dialog__section-title");

        Separator separator = new Separator();
        separator.getStyleClass().add("settings-dialog__section-separator");

        section.getChildren().addAll(titleLabel, separator);
        return section;
    }

    private Node createSettingControl(SettingDTO setting) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("settings-dialog__setting-row");

        // Label
        VBox labelBox = new VBox(2);
        labelBox.setMinWidth(200);
        labelBox.setMaxWidth(250);

        String labelText = setting.label() != null ? setting.label()
                : SettingsDialogViewModel.formatKeyToLabel(setting.key());
        Label label = new Label(labelText);
        label.getStyleClass().add("settings-dialog__setting-label");

        if (setting.requiresRestart()) {
            label.setText(label.getText() + " ⟳");
            label.setTooltip(new Tooltip("Requires restart to apply"));
        }

        labelBox.getChildren().add(label);

        if (setting.description() != null && !setting.description().isEmpty()) {
            Label desc = new Label(setting.description());
            desc.getStyleClass().add("settings-dialog__setting-description");
            desc.setWrapText(true);
            labelBox.getChildren().add(desc);
        }

        row.getChildren().add(labelBox);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);

        // Control
        Node control = createControl(setting);
        row.getChildren().add(control);

        return row;
    }

    private Node createControl(SettingDTO setting) {
        return switch (setting.type()) {
            case BOOLEAN -> createToggle(setting);
            case INTEGER, LONG -> createNumberField(setting);
            case DOUBLE -> createDecimalField(setting);
            case PASSWORD -> createPasswordField(setting);
            case PATH -> createPathField(setting);
            case ENUM -> createDropdown(setting);
            default -> createTextField(setting);
        };
    }

    private CheckBox createToggle(SettingDTO setting) {
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(Boolean.parseBoolean(viewModel.getCurrentValue(setting)));
        checkBox.getStyleClass().add("settings-dialog__toggle");
        checkBox.selectedProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), String.valueOf(val)));
        return checkBox;
    }

    private Spinner<Integer> createNumberField(SettingDTO setting) {
        int min = 0;
        int max = Integer.MAX_VALUE;
        int value = 0;
        try {
            value = Integer.parseInt(viewModel.getCurrentValue(setting));
        } catch (Exception _) {
            // Ignore
        }

        // Parse validation rules
        if (setting.validationRules() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> rules = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readValue(setting.validationRules(), Map.class);
                if (rules.containsKey("min"))
                    min = ((Number) rules.get("min")).intValue();
                if (rules.containsKey("max"))
                    max = ((Number) rules.get("max")).intValue();
            } catch (Exception _) {
                // Ignore
            }
        }

        Spinner<Integer> spinner = new Spinner<>(min, max, value);
        spinner.setPrefWidth(120);
        spinner.setEditable(true);
        spinner.getStyleClass().add("settings-dialog__spinner");
        spinner.valueProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), String.valueOf(val)));
        return spinner;
    }

    private Spinner<Double> createDecimalField(SettingDTO setting) {
        double value = 0;
        try {
            value = Double.parseDouble(viewModel.getCurrentValue(setting));
        } catch (Exception _) {
            // Ignore
        }

        Spinner<Double> spinner = new Spinner<>(0.0, Double.MAX_VALUE, value, 0.1);
        spinner.setPrefWidth(120);
        spinner.setEditable(true);
        spinner.getStyleClass().add("settings-dialog__spinner");
        spinner.valueProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), String.valueOf(val)));
        return spinner;
    }

    private HBox createPasswordField(SettingDTO setting) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        PasswordField passwordField = new PasswordField();
        passwordField.setText(viewModel.getCurrentValue(setting));
        passwordField.setPrefWidth(200);
        passwordField.getStyleClass().add("settings-dialog__password-field");
        passwordField.textProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), val));

        TextField visibleField = new TextField();
        visibleField.setPrefWidth(200);
        visibleField.getStyleClass().add(STYLE_CLASS_TEXT_FIELD);
        visibleField.setVisible(false);
        visibleField.setManaged(false);
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());

        Button toggleBtn = new Button();
        FontIcon eyeIcon = new FontIcon("mdi2e-eye");
        toggleBtn.setGraphic(eyeIcon);
        toggleBtn.getStyleClass().add("settings-dialog__toggle-btn");
        toggleBtn.setOnAction(e -> {
            boolean show = !visibleField.isVisible();
            visibleField.setVisible(show);
            visibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            eyeIcon.setIconLiteral(show ? "mdi2e-eye-off" : "mdi2e-eye");
        });

        box.getChildren().addAll(passwordField, visibleField, toggleBtn);
        return box;
    }

    private HBox createPathField(SettingDTO setting) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        TextField textField = new TextField(viewModel.getCurrentValue(setting));
        textField.setPrefWidth(200);
        textField.getStyleClass().add(STYLE_CLASS_TEXT_FIELD);
        textField.textProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), val));

        Button browseBtn = new Button();
        FontIcon folderIcon = new FontIcon("mdi2f-folder-open");
        browseBtn.setGraphic(folderIcon);
        browseBtn.getStyleClass().add("settings-dialog__browse-btn");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory");
            if (textField.getText() != null && !textField.getText().isEmpty()) {
                java.io.File current = new java.io.File(textField.getText());
                if (current.exists()) {
                    chooser.setInitialDirectory(current);
                }
            }
            java.io.File selected = chooser.showDialog(dialogStage);
            if (selected != null) {
                textField.setText(selected.getAbsolutePath());
            }
        });

        box.getChildren().addAll(textField, browseBtn);
        return box;
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> createDropdown(SettingDTO setting) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(150);
        comboBox.getStyleClass().add("settings-dialog__combobox");

        // Parse options from validation rules
        if (setting.validationRules() != null) {
            try {
                Map<String, Object> rules = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                        .readValue(setting.validationRules(), Map.class);
                if (rules.containsKey("options")) {
                    List<String> options = (List<String>) rules.get("options");
                    comboBox.getItems().addAll(options);
                }
            } catch (Exception _) {
                // Ignore
            }
        }

        comboBox.setValue(viewModel.getCurrentValue(setting));
        comboBox.valueProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), val));
        return comboBox;
    }

    private TextField createTextField(SettingDTO setting) {
        TextField textField = new TextField(viewModel.getCurrentValue(setting));
        textField.setPrefWidth(200);
        textField.getStyleClass().add(STYLE_CLASS_TEXT_FIELD);
        textField.textProperty().addListener(
                (obs, old, val) -> viewModel.recordChange(setting.key(), val));
        return textField;
    }

    // ===== Actions =====

    @FXML
    private void onResetDefaults() {
        CategoryItem current = viewModel.getSelectedCategory();
        if (current == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Settings");
        confirm.setHeaderText("Reset " + current.name() + " to defaults?");
        confirm.setContentText("This will restore all settings in this category to their default values.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && viewModel.resetCategoryToDefaults()) {
                updateCategoryContent(current);
            }
        });
    }

    @FXML
    private void onApply() {
        if (viewModel.saveChanges()) {
            dialogStage.close();
        }
    }

    @FXML
    private void onCancel() {
        if (viewModel.hasChanges()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Discard Changes");
            confirm.setHeaderText("Discard unsaved changes?");
            confirm.setContentText("You have unsaved changes. Do you want to discard them?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    viewModel.discardChanges();
                    dialogStage.close();
                }
            });
        } else {
            dialogStage.close();
        }
    }

    // ===== Inner Classes =====

    /**
     * List cell for category items in the sidebar.
     */
    private static class CategoryListCell extends ListCell<CategoryItem> {
        private final HBox content = new HBox(12);
        private final FontIcon icon = new FontIcon();
        private final Label label = new Label();

        CategoryListCell() {
            content.setAlignment(Pos.CENTER_LEFT);
            content.getStyleClass().add("settings-dialog__category-cell");
            icon.setIconSize(18);
            icon.getStyleClass().add("settings-dialog__category-cell-icon");
            label.getStyleClass().add("settings-dialog__category-cell-label");
            content.getChildren().addAll(icon, label);
        }

        @Override
        protected void updateItem(CategoryItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                icon.setIconLiteral(item.iconLiteral());
                label.setText(item.name());
                setGraphic(content);
            }
        }
    }
}
