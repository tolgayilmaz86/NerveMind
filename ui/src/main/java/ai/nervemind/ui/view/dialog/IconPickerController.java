/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import org.kordamp.ikonli.javafx.FontIcon;

import ai.nervemind.ui.viewmodel.dialog.IconPickerViewModel;
import ai.nervemind.ui.viewmodel.dialog.IconPickerViewModel.IconEntry;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for the Icon Picker dialog FXML.
 * 
 * <p>
 * Binds the FXML view to the {@link IconPickerViewModel}.
 */
public class IconPickerController {

    private IconPickerViewModel viewModel;
    private Stage dialogStage;
    private String result;
    private boolean applied = false;

    /**
     * Default constructor for FXML loading.
     */
    public IconPickerController() {
        // Default constructor for FXML
    }

    private static final String SELECTED_ICON_BUTTON_STYLE_CLASS = "icon-picker-dialog__icon-button--selected";

    private ToggleButton selectedButton = null;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private FlowPane iconGrid;

    @FXML
    private FontIcon previewIcon;

    @FXML
    private Label selectedLabel;

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
    public void initialize(IconPickerViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupBindings();
        setupIconGrid();
    }

    private void setupBindings() {
        // Search field binding
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        // Category filter
        categoryFilter.setItems(viewModel.getCategories());
        categoryFilter.valueProperty().bindBidirectional(viewModel.selectedCategoryProperty());

        // Selected label
        selectedLabel.textProperty().bind(viewModel.selectedIconLabelProperty());

        // Preview icon - update when selection changes
        viewModel.selectedIconProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewIcon.setIconLiteral(newVal.code());
            } else {
                previewIcon.setIconLiteral(null);
            }
        });

        // Set initial preview if there's a current icon
        if (viewModel.getOriginalIcon() != null) {
            previewIcon.setIconLiteral(viewModel.getOriginalIcon());
        }
    }

    private void setupIconGrid() {
        // Initial population
        populateIconGrid();

        // Listen for changes
        viewModel.getFilteredIcons().addListener((ListChangeListener<IconEntry>) c -> populateIconGrid());
    }

    private void populateIconGrid() {
        iconGrid.getChildren().clear();
        selectedButton = null;

        for (IconEntry entry : viewModel.getFilteredIcons()) {
            ToggleButton button = createIconButton(entry);
            iconGrid.getChildren().add(button);

            // Check if this is the currently selected icon
            if (entry.equals(viewModel.selectedIconProperty().get())) {
                button.setSelected(true);
                button.getStyleClass().add(SELECTED_ICON_BUTTON_STYLE_CLASS);
                selectedButton = button;
            }
        }
    }

    private ToggleButton createIconButton(IconEntry entry) {
        ToggleButton button = new ToggleButton();
        button.setPrefSize(42, 42);
        button.setMinSize(42, 42);
        button.setMaxSize(42, 42);
        button.getStyleClass().add("icon-picker-dialog__icon-button");

        FontIcon icon = new FontIcon(entry.ikon());
        icon.setIconSize(22);
        icon.setIconColor(Color.web("#e5e5e5"));
        button.setGraphic(icon);

        // Tooltip with icon code
        String tooltipText = entry.code()
                .replace("mdi2-", "")
                .replace("fas-", "")
                .replace("far-", "")
                .replace("fab-", "")
                .replace("-", " ");
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("icon-picker-dialog__tooltip");
        Tooltip.install(button, tooltip);

        // Selection handler
        button.setOnAction(e -> {
            // Deselect previous
            if (selectedButton != null && selectedButton != button) {
                selectedButton.setSelected(false);
                selectedButton.getStyleClass().remove(SELECTED_ICON_BUTTON_STYLE_CLASS);
            }

            if (button.isSelected()) {
                selectedButton = button;
                button.getStyleClass().add(SELECTED_ICON_BUTTON_STYLE_CLASS);
                viewModel.selectIcon(entry);
            } else {
                selectedButton = null;
                button.getStyleClass().remove(SELECTED_ICON_BUTTON_STYLE_CLASS);
                viewModel.resetSelection();
            }
        });

        return button;
    }

    @FXML
    private void onReset() {
        if (selectedButton != null) {
            selectedButton.setSelected(false);
            selectedButton.getStyleClass().remove(SELECTED_ICON_BUTTON_STYLE_CLASS);
            selectedButton = null;
        }
        viewModel.resetSelection();
    }

    @FXML
    private void onApply() {
        this.result = viewModel.getResult();
        this.applied = true;
        dialogStage.close();
    }

    @FXML
    private void onCancel() {
        this.result = viewModel.getOriginalIcon();
        this.applied = false;
        dialogStage.close();
    }

    /**
     * Get the result of the dialog.
     * 
     * @return the selected icon code, or null for reset, or original if cancelled
     */
    public String getResult() {
        return result;
    }

    /**
     * Check if Apply was clicked.
     *
     * @return true if applied
     */
    public boolean wasApplied() {
        return applied;
    }
}
