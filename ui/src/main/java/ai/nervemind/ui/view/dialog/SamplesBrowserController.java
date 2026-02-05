/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.ui.viewmodel.dialog.SamplesBrowserViewModel;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for the Samples Browser dialog FXML.
 * 
 * <p>
 * Binds the FXML view to the {@link SamplesBrowserViewModel}.
 */
public class SamplesBrowserController {

    /**
     * Creates a new SamplesBrowserController.
     */
    public SamplesBrowserController() {
        // Default constructor for FXML loading
    }

    private SamplesBrowserViewModel viewModel;
    private Stage dialogStage;
    private SampleWorkflow result;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> languageFilter;

    @FXML
    private ComboBox<String> difficultyFilter;

    @FXML
    private ListView<SampleWorkflow> sampleListView;

    @FXML
    private Label statusLabel;

    @FXML
    private Button viewGuideButton;

    @FXML
    private Button importButton;

    @FXML
    private Button closeButton;

    /**
     * Initialize the controller with its view model.
     * 
     * @param viewModel   the ViewModel
     * @param dialogStage the dialog stage
     */
    public void initialize(SamplesBrowserViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;

        setupBindings();
        setupListView();
        setupActions();
    }

    private void setupBindings() {
        // Search field binding
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        // Filter combo boxes
        categoryFilter.setItems(viewModel.getCategories());
        categoryFilter.valueProperty().bindBidirectional(viewModel.selectedCategoryProperty());

        languageFilter.setItems(viewModel.getLanguages());
        languageFilter.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

        difficultyFilter.setItems(viewModel.getDifficulties());
        difficultyFilter.valueProperty().bindBidirectional(viewModel.selectedDifficultyProperty());

        // Status label
        statusLabel.textProperty().bind(viewModel.statusTextProperty());

        // Button states
        importButton.disableProperty().bind(viewModel.hasSelectionProperty().not());
        viewGuideButton.disableProperty().bind(viewModel.selectedHasGuideProperty().not());
    }

    private void setupListView() {
        sampleListView.setItems(viewModel.getFilteredSamples());
        sampleListView.setCellFactory(lv -> new SampleListCell());

        // Selection binding
        sampleListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> viewModel.selectedSampleProperty().set(newVal));

        // Double-click to import
        sampleListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && viewModel.getSelectedSample() != null) {
                onImport();
            }
        });
    }

    private void setupActions() {
        viewModel.setOnImport(sample -> {
            this.result = sample;
            dialogStage.close();
        });
    }

    @FXML
    private void onViewGuide() {
        // Close the samples browser so the guide can interact with the canvas
        dialogStage.close();
        viewModel.viewGuide();
    }

    @FXML
    private void onImport() {
        viewModel.importSelected();
    }

    @FXML
    private void onClose() {
        this.result = null;
        dialogStage.close();
    }

    /**
     * Get the result of the dialog.
     * 
     * @return the selected sample workflow
     */
    public SampleWorkflow getResult() {
        return result;
    }

    /**
     * Get the view model to access the selected sample for guide viewing.
     * 
     * @return the samples browser view model
     */
    public SamplesBrowserViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Custom list cell for displaying sample information.
     */
    private static class SampleListCell extends ListCell<SampleWorkflow> {

        public SampleListCell() {
            getStyleClass().add("samples-browser-dialog__cell");

            // Selection listener
            selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.TRUE.equals(newVal)) {
                    getStyleClass().remove("samples-browser-dialog__cell--hover");
                    if (!getStyleClass().contains("samples-browser-dialog__cell--selected")) {
                        getStyleClass().add("samples-browser-dialog__cell--selected");
                    }
                } else {
                    getStyleClass().remove("samples-browser-dialog__cell--selected");
                }
            });

            // Hover effects
            setOnMouseEntered(e -> {
                if (!isSelected() && !isEmpty()
                        && !getStyleClass().contains("samples-browser-dialog__cell--hover")) {
                    getStyleClass().add("samples-browser-dialog__cell--hover");
                }
            });
            setOnMouseExited(e -> {
                if (!isSelected()) {
                    getStyleClass().remove("samples-browser-dialog__cell--hover");
                }
            });
        }

        @Override
        protected void updateItem(SampleWorkflow sample, boolean empty) {
            super.updateItem(sample, empty);

            if (empty || sample == null) {
                setGraphic(null);
                setText(null);
                getStyleClass().removeAll("samples-browser-dialog__cell--hover",
                        "samples-browser-dialog__cell--selected");
                return;
            }

            // Build cell content
            VBox cellContent = new VBox(6);
            cellContent.setPadding(new Insets(4));

            // Title row with icon and difficulty
            HBox titleRow = new HBox(10);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            // Language icon
            FontIcon langIcon = getLanguageIcon(sample.language());
            langIcon.setIconSize(18);
            langIcon.setIconColor(Color.web("#71717a"));

            Label nameLabel = new Label(sample.name());
            nameLabel.getStyleClass().add("samples-browser-dialog__cell-name");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label diffLabel = new Label(sample.difficulty() != null ? sample.difficulty().getStars() : "");
            diffLabel.getStyleClass().add("samples-browser-dialog__cell-difficulty");
            if (sample.difficulty() != null) {
                Tooltip.install(diffLabel, new Tooltip(sample.difficulty().getLabel()));
            }

            titleRow.getChildren().addAll(langIcon, nameLabel, diffLabel);

            // Description
            Label descLabel = new Label(truncate(sample.description(), 100));
            descLabel.getStyleClass().add("samples-browser-dialog__cell-description");
            descLabel.setWrapText(true);

            // Tags row
            FlowPane tagsPane = new FlowPane(6, 4);
            if (sample.tags() != null && !sample.tags().isEmpty()) {
                for (String tag : sample.tags()) {
                    Label tagLabel = new Label(tag);
                    tagLabel.getStyleClass().add("samples-browser-dialog__tag");
                    tagsPane.getChildren().add(tagLabel);
                }
            }

            // Category label
            if (sample.category() != null) {
                Label categoryLabel = new Label(sample.category());
                categoryLabel.getStyleClass().add("samples-browser-dialog__category-tag");
                tagsPane.getChildren().add(0, categoryLabel);
            }

            cellContent.getChildren().addAll(titleRow, descLabel, tagsPane);

            setGraphic(cellContent);
            setText(null);
        }

        private FontIcon getLanguageIcon(String language) {
            if (language == null) {
                return FontIcon.of(MaterialDesignC.CODE_TAGS, 18);
            }
            return switch (language.toLowerCase()) {
                case "python" -> FontIcon.of(MaterialDesignS.SNAKE, 18);
                case "javascript", "js" -> FontIcon.of(MaterialDesignS.SCRIPT, 18);
                default -> FontIcon.of(MaterialDesignC.CODE_TAGS, 18);
            };
        }

        private String truncate(String text, int maxLength) {
            if (text == null) {
                return "";
            }
            if (text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 3) + "...";
        }
    }
}
