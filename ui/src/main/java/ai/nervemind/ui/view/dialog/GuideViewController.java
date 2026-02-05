/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.ui.viewmodel.dialog.GuideViewViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * Controller for the Guide View dialog FXML.
 */
public class GuideViewController {

    @FXML
    private ListView<GuideStep> stepListView;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox contentPane;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label stepCounter;
    @FXML
    private Button backButton;
    @FXML
    private Button highlightButton;
    @FXML
    private Button importButton;
    @FXML
    private Button closeButton;

    private GuideViewViewModel viewModel;
    private Stage dialogStage;
    private boolean applied = false;
    private Consumer<SampleWorkflow> onImportCallback;
    private Runnable onBackCallback;

    /**
     * Default constructor for FXML loading.
     */
    public GuideViewController() {
        // Default constructor for FXML
    }

    /**
     * Initializes the controller after FXML loading.
     * Sets up the step list view and binds UI elements.
     */
    @FXML
    public void initialize() {
        // Setup list cell factory
        stepListView.setCellFactory(lv -> new StepListCell());
    }

    /**
     * Initialize the controller with ViewModel and Stage.
     *
     * @param viewModel the guide view model
     * @param stage     the dialog stage
     */
    public void initialize(GuideViewViewModel viewModel, Stage stage) {
        this.viewModel = viewModel;
        this.dialogStage = stage;

        stage.setTitle("📖 Guide: " + viewModel.getSample().name());

        bindViewModel();
    }

    private void bindViewModel() {
        // Bind step list
        stepListView.setItems(viewModel.getSteps());

        // Bind navigation buttons
        prevButton.disableProperty().bind(viewModel.canNavigatePreviousProperty().not());
        nextButton.disableProperty().bind(viewModel.canNavigateNextProperty().not());

        // Bind step counter
        stepCounter.textProperty().bind(viewModel.stepCounterTextProperty());

        // Listen for step changes to update content
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> updateStepContent());

        // Listen for list selection changes - use Platform.runLater to ensure selection
        // is updated
        stepListView.setOnMouseClicked(event -> Platform.runLater(() -> {
            int selectedIndex = stepListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex != viewModel.getCurrentStepIndex()) {
                viewModel.navigateToStep(selectedIndex);
            }
        }));

        // Sync list selection with current step (for prev/next buttons)
        viewModel.currentStepIndexProperty().addListener((obs, oldVal, newVal) -> {
            int newIndex = newVal.intValue();
            if (stepListView.getSelectionModel().getSelectedIndex() != newIndex) {
                stepListView.getSelectionModel().select(newIndex);
            }
        });

        // Initial content update
        updateStepContent();
        if (!viewModel.getSteps().isEmpty()) {
            stepListView.getSelectionModel().select(0);
        }
    }

    private void updateStepContent() {
        contentPane.getChildren().clear();

        // Step title
        Label titleLabel = new Label(viewModel.getStepTitle());
        titleLabel.getStyleClass().add("guide-view-dialog__step-title-label");
        contentPane.getChildren().add(titleLabel);

        // Separator
        Region separator = new Region();
        separator.getStyleClass().add("guide-view-dialog__separator");
        separator.setPrefHeight(1);
        separator.setMinHeight(1);
        separator.setMaxHeight(1);
        contentPane.getChildren().add(separator);

        // Content
        String content = viewModel.getStepContent();
        if (content != null && !content.isEmpty()) {
            VBox contentBox = parseMarkdownContent(content);
            contentPane.getChildren().add(contentBox);
        }

        // Code snippet
        if (viewModel.hasCodeSnippet()) {
            Label codeLabel = new Label("Code Example:");
            codeLabel.getStyleClass().add("guide-view-dialog__code-label");

            TextArea codeArea = new TextArea(viewModel.getStepCodeSnippet());
            codeArea.setEditable(false);
            codeArea.setWrapText(true);
            codeArea.getStyleClass().add("guide-view-dialog__code-area");
            int lines = viewModel.getStepCodeSnippet().split("\n").length + 1;
            codeArea.setPrefRowCount(Math.min(15, lines));

            contentPane.getChildren().addAll(codeLabel, codeArea);
        }

        // Highlight nodes indicator
        if (viewModel.hasHighlightNodes()) {
            HBox highlightInfo = new HBox(8);
            highlightInfo.setAlignment(Pos.CENTER_LEFT);
            highlightInfo.setPadding(new Insets(10, 0, 0, 0));

            FontIcon icon = FontIcon.of(MaterialDesignI.INFORMATION_OUTLINE, 16, Color.web("#60a5fa"));
            Label label = new Label(viewModel.getHighlightNodesText());
            label.getStyleClass().add("guide-view-dialog__highlight-info");

            highlightInfo.getChildren().addAll(icon, label);
            contentPane.getChildren().add(highlightInfo);
        }

        // Scroll to top
        scrollPane.setVvalue(0);
    }

    @FXML
    private void handlePrevious() {
        viewModel.navigatePrevious();
    }

    @FXML
    private void handleNext() {
        viewModel.navigateNext();
    }

    @FXML
    private void handleHighlight() {
        viewModel.highlightCurrentStepNodes();
    }

    @FXML
    private void handleImport() {
        applied = true;
        // Trigger the import callback directly
        if (onImportCallback != null) {
            onImportCallback.accept(viewModel.getSample());
        }
        // Update button to show success without closing the dialog
        importButton.setText("✓ Imported!");
        importButton.setDisable(true);
        importButton.getStyleClass().remove("dialog__button-secondary");
        importButton.getStyleClass().add("dialog__button-success");
    }

    @FXML
    private void handleBack() {
        dialogStage.close();
        if (onBackCallback != null) {
            onBackCallback.run();
        }
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }

    /**
     * Set the callback for importing the workflow.
     *
     * @param callback the import callback
     */
    public void setOnImport(Consumer<SampleWorkflow> callback) {
        this.onImportCallback = callback;
    }

    /**
     * Set the callback for going back to samples browser.
     *
     * @param callback the back callback
     */
    public void setOnBack(Runnable callback) {
        this.onBackCallback = callback;
    }

    /**
     * Checks if the guide was applied (workflow imported).
     *
     * @return true if applied
     */
    public boolean wasApplied() {
        return applied;
    }

    /**
     * Gets the result of the guide dialog.
     *
     * @return the sample workflow if applied, null otherwise
     */
    public SampleWorkflow getResult() {
        return applied ? viewModel.getSample() : null;
    }

    /**
     * Custom list cell for step navigation.
     */
    private class StepListCell extends ListCell<GuideStep> {

        private final HBox cell;
        private final Label indicator;
        private final Label titleLabel;

        public StepListCell() {
            cell = new HBox(10);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8));

            indicator = new Label();
            indicator.setMinSize(24, 24);
            indicator.setMaxSize(24, 24);
            indicator.setAlignment(Pos.CENTER);
            indicator.getStyleClass().add("guide-view-dialog__step-indicator");

            titleLabel = new Label();
            titleLabel.getStyleClass().add("guide-view-dialog__step-title");
            titleLabel.setWrapText(true);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            cell.getChildren().addAll(indicator, titleLabel);

            selectedProperty().addListener((obs, wasSelected, isNowSelected) -> updateSelectionStyle(isNowSelected));
        }

        private void updateSelectionStyle(boolean selected) {
            indicator.getStyleClass().remove("guide-view-dialog__step-indicator--selected");
            if (selected) {
                indicator.getStyleClass().add("guide-view-dialog__step-indicator--selected");
            }
        }

        @Override
        protected void updateItem(GuideStep step, boolean empty) {
            super.updateItem(step, empty);

            if (empty || step == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            indicator.setText(String.valueOf(getIndex() + 1));
            titleLabel.setText(step.title());
            setGraphic(cell);
            setText(null);

            updateSelectionStyle(isSelected());
        }
    }

    /**
     * Parse simple markdown content and convert to JavaFX nodes.
     * Supports: **bold**, `code`, bullet lists, and tables.
     */
    private VBox parseMarkdownContent(String content) {
        VBox container = new VBox(8);
        container.setPadding(new Insets(10, 0, 10, 0));

        String[] lines = content.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();

            // Empty line - just increment and move on
            if (line.isEmpty()) {
                i++;
            }
            // Check for table
            else if (i + 1 < lines.length && lines[i + 1].trim().matches("^\\|[-:\\s|]+\\|$")) {
                // Table detected
                VBox table = parseTable(lines, i);
                container.getChildren().add(table);
                // Skip table lines
                i++;
                while (i < lines.length && lines[i].trim().startsWith("|")) {
                    i++;
                }
            }
            // Bullet list
            else if (line.startsWith("- ") || line.startsWith("* ")) {
                HBox bulletLine = new HBox(5);
                bulletLine.setPadding(new Insets(2, 0, 2, 15));

                Text bullet = new Text("• ");
                TextFlow textFlow = parseLineToTextFlow(line.substring(2));
                bulletLine.getChildren().addAll(bullet, textFlow);
                container.getChildren().add(bulletLine);
                i++;
            }
            // Regular paragraph
            else {
                TextFlow textFlow = parseLineToTextFlow(line);
                textFlow.setPadding(new Insets(2, 0, 2, 0));
                textFlow.setLineSpacing(4);
                container.getChildren().add(textFlow);
                i++;
            }
        }

        return container;
    }

    /**
     * Parse a single line of text with inline markdown and return a TextFlow.
     */
    private TextFlow parseLineToTextFlow(String line) {
        TextFlow flow = new TextFlow();
        StringBuilder current = new StringBuilder();
        int i = 0;

        while (i < line.length()) {
            int newIndex = tryParseBold(line, i, current, flow);
            if (newIndex == i) {
                newIndex = tryParseCode(line, i, current, flow);
            }
            if (newIndex == i) {
                current.append(line.charAt(i));
                i++;
            } else {
                i = newIndex;
            }
        }

        // Add remaining text
        if (!current.isEmpty()) {
            flow.getChildren().add(new Text(current.toString()));
        }

        return flow;
    }

    /**
     * Try to parse bold text (**text**) at the current position.
     * Returns the new index if parsed, or the same index if not.
     */
    private int tryParseBold(String line, int i, StringBuilder current, TextFlow flow) {
        if (i >= line.length() - 1 || line.charAt(i) != '*' || line.charAt(i + 1) != '*') {
            return i;
        }

        int end = line.indexOf("**", i + 2);
        if (end == -1) {
            return i;
        }

        flushCurrentText(current, flow);
        Text boldText = new Text(line.substring(i + 2, end));
        boldText.setFont(Font.font(boldText.getFont().getFamily(), FontWeight.BOLD, boldText.getFont().getSize()));
        flow.getChildren().add(boldText);
        return end + 2;
    }

    /**
     * Try to parse inline code (`text`) at the current position.
     * Returns the new index if parsed, or the same index if not.
     */
    private int tryParseCode(String line, int i, StringBuilder current, TextFlow flow) {
        if (line.charAt(i) != '`') {
            return i;
        }

        int end = line.indexOf('`', i + 1);
        if (end == -1) {
            return i;
        }

        flushCurrentText(current, flow);
        Text codeText = new Text(line.substring(i + 1, end));
        codeText.setFont(Font.font("monospace", codeText.getFont().getSize()));
        codeText.setFill(Color.web("#88c0d0"));
        flow.getChildren().add(codeText);
        return end + 1;
    }

    /**
     * Add accumulated text to the flow and clear the buffer.
     */
    private void flushCurrentText(StringBuilder current, TextFlow flow) {
        if (!current.isEmpty()) {
            flow.getChildren().add(new Text(current.toString()));
            current.setLength(0);
        }
    }

    /**
     * Parse a markdown table into a JavaFX VBox.
     */
    private VBox parseTable(String[] lines, int startIndex) {
        VBox table = new VBox(0);
        table.getStyleClass().add("guide-view-dialog__table");
        table.setPadding(new Insets(10, 0, 10, 0));

        // Parse header
        String headerLine = lines[startIndex].trim();
        String[] headers = parseTableRow(headerLine);

        // Create header row
        HBox headerRow = new HBox(0);
        headerRow.getStyleClass().add("guide-view-dialog__table-header-row");
        for (String header : headers) {
            Label headerCell = new Label(header.trim());
            headerCell.getStyleClass().add("guide-view-dialog__table-header-cell");
            headerCell.setMinWidth(150);
            headerCell.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(headerCell, Priority.ALWAYS);
            headerRow.getChildren().add(headerCell);
        }
        table.getChildren().add(headerRow);

        // Skip separator line (e.g., |-------|---------|)
        int i = startIndex + 2;

        // Parse data rows
        while (i < lines.length && lines[i].trim().startsWith("|")) {
            String[] cells = parseTableRow(lines[i].trim());
            HBox dataRow = new HBox(0);
            dataRow.getStyleClass().add("guide-view-dialog__table-data-row");

            for (String cell : cells) {
                Label cellLabel = new Label(cell.trim());
                cellLabel.getStyleClass().add("guide-view-dialog__table-data-cell");
                cellLabel.setMinWidth(150);
                cellLabel.setMaxWidth(Double.MAX_VALUE);
                cellLabel.setWrapText(true);
                HBox.setHgrow(cellLabel, Priority.ALWAYS);
                dataRow.getChildren().add(cellLabel);
            }
            table.getChildren().add(dataRow);
            i++;
        }

        return table;
    }

    /**
     * Parse a table row (split by |).
     */
    private String[] parseTableRow(String line) {
        // Remove leading/trailing |
        if (line.startsWith("|")) {
            line = line.substring(1);
        }
        if (line.endsWith("|")) {
            line = line.substring(0, line.length() - 1);
        }
        return line.split("\\|");
    }
}
