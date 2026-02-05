/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.canvas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.exception.UiInitializationException;
import ai.nervemind.ui.canvas.NodeView;
import ai.nervemind.ui.component.ExpressionEditorComponent;
import ai.nervemind.ui.viewmodel.canvas.NodePropertiesViewModel;
import ai.nervemind.ui.viewmodel.canvas.NodePropertiesViewModel.ParameterDefinition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Controller/Component for the NodePropertiesPanel.
 *
 * <p>
 * This is a custom component that extends VBox and loads its FXML using the
 * fx:root pattern.
 * It provides a panel for editing node properties with:
 * <ul>
 * <li>Header with node type icon and title</li>
 * <li>Common fields (name, disabled, notes)</li>
 * <li>Dynamic parameters section based on node type</li>
 * <li>Action buttons (Advanced, Reset, Apply)</li>
 * </ul>
 *
 * <p>
 * This component delegates business logic to {@link NodePropertiesViewModel}.
 */
public class NodePropertiesPanelComponent extends VBox {

    private final NodePropertiesViewModel viewModel;

    // FXML Components - Header
    @FXML
    private HBox header;

    @FXML
    private VBox iconContainer;

    @FXML
    private FontIcon nodeTypeIcon;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button helpButton;

    @FXML
    private Button closeButton;

    @FXML
    private Region accentBar;

    // FXML Components - Common Fields
    @FXML
    private TextField nameField;

    @FXML
    private CheckBox disabledCheckbox;

    @FXML
    private TextArea notesArea;

    // FXML Components - Parameters
    @FXML
    private TitledPane parametersPane;

    @FXML
    private VBox parametersContainer;

    // FXML Components - Actions
    @FXML
    private HBox actionsContainer;

    @FXML
    private Button advancedButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button applyButton;

    // Dynamic field references for extracting values
    private final Map<String, Object> parameterFields = new HashMap<>();

    /**
     * Creates a new NodePropertiesPanelComponent.
     */
    public NodePropertiesPanelComponent() {
        this.viewModel = new NodePropertiesViewModel();
        loadFxml();
        setupBindings();
    }

    /**
     * Creates a new NodePropertiesPanelComponent with an existing ViewModel.
     *
     * @param viewModel the ViewModel to use
     */
    public NodePropertiesPanelComponent(NodePropertiesViewModel viewModel) {
        this.viewModel = viewModel;
        loadFxml();
        setupBindings();
    }

    /**
     * Loads the FXML file and sets this component as both root and controller.
     */
    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ai/nervemind/ui/view/canvas/NodePropertiesPanel.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new UiInitializationException("Failed to load NodePropertiesPanel FXML", "NodePropertiesPanel", e);
        }
    }

    /**
     * Sets up bindings between view and ViewModel.
     */
    private void setupBindings() {
        // Bind visibility
        visibleProperty().bind(viewModel.visibleProperty());
        managedProperty().bind(viewModel.visibleProperty());

        // Bind title labels
        titleLabel.textProperty().bind(viewModel.nameProperty());
        subtitleLabel.textProperty().bind(viewModel.nodeTypeLabelProperty());

        // Bind common fields bidirectionally
        nameField.textProperty().bindBidirectional(viewModel.nameProperty());
        notesArea.textProperty().bindBidirectional(viewModel.notesProperty());
        disabledCheckbox.selectedProperty().bindBidirectional(viewModel.disabledProperty());

        // Listen for accent color changes
        viewModel.accentColorProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                accentBar.setStyle("-fx-background-color: " + newVal + ";");
            }
        });

        // Listen for icon changes
        viewModel.nodeTypeIconProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nodeTypeIcon.setIconLiteral(newVal);
            }
        });

        // Listen for parameter definitions to rebuild UI
        viewModel.getParameterDefinitions().addListener(
                (javafx.collections.ListChangeListener<ParameterDefinition>) change -> rebuildParametersUI());
    }

    /**
     * Rebuilds the parameters UI based on ViewModel definitions.
     */
    private void rebuildParametersUI() {
        parametersContainer.getChildren().clear();
        parameterFields.clear();

        for (ParameterDefinition def : viewModel.getParameterDefinitions()) {
            switch (def.type()) {
                case TEXT_FIELD -> addTextField(def);
                case TEXT_AREA -> addTextArea(def);
                case COMBO_BOX -> addComboBox(def);
                case SPINNER -> addSpinner(def);
                case CHECK_BOX -> addCheckBox(def);
                case HINT -> addHint(def);
                case EXPRESSION -> addExpressionField(def);
            }
        }
    }

    private void addTextField(ParameterDefinition def) {
        Label label = new Label(def.label());
        label.getStyleClass().add("property-label");

        TextField field = new TextField(def.value());
        if (def.placeholder() != null) {
            field.setPromptText(def.placeholder());
        }
        field.textProperty().addListener((obs, oldVal, newVal) -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), field);
        parametersContainer.getChildren().addAll(label, field);
    }

    private void addTextArea(ParameterDefinition def) {
        Label label = new Label(def.label());
        label.getStyleClass().add("property-label");

        TextArea area = new TextArea(def.value());
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.textProperty().addListener((obs, oldVal, newVal) -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), area);
        parametersContainer.getChildren().addAll(label, area);
    }

    private void addComboBox(ParameterDefinition def) {
        Label label = new Label(def.label());
        label.getStyleClass().add("property-label");

        ComboBox<String> combo = new ComboBox<>();
        if (def.options() != null) {
            combo.getItems().addAll(def.options());
        }
        combo.setValue(def.value());
        combo.valueProperty().addListener((obs, oldVal, newVal) -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), combo);
        parametersContainer.getChildren().addAll(label, combo);
    }

    private void addSpinner(ParameterDefinition def) {
        Label label = new Label(def.label());
        label.getStyleClass().add("property-label");

        int value = 0;
        try {
            value = Integer.parseInt(def.value());
        } catch (NumberFormatException _) {
            // Ignore and use default
        }

        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                def.min(), def.max(), value));
        spinner.setEditable(true);
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), spinner);
        parametersContainer.getChildren().addAll(label, spinner);
    }

    private void addCheckBox(ParameterDefinition def) {
        CheckBox cb = new CheckBox(def.label());
        cb.setSelected(Boolean.parseBoolean(def.value()));
        cb.getStyleClass().add("property-checkbox");
        cb.selectedProperty().addListener((obs, oldVal, newVal) -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), cb);
        parametersContainer.getChildren().add(cb);
    }

    private void addHint(ParameterDefinition def) {
        Label hint = new Label(def.label());
        hint.getStyleClass().add("property-hint");
        hint.setWrapText(true);
        parametersContainer.getChildren().add(hint);
    }

    private void addExpressionField(ParameterDefinition def) {
        Label label = new Label(def.label());
        label.getStyleClass().add("property-label");

        ExpressionEditorComponent editor = new ExpressionEditorComponent();
        editor.setExpression(def.value() != null ? def.value() : "");
        editor.setPrefRowCount(3);

        if (def.placeholder() != null) {
            editor.setPromptText(def.placeholder());
        } else {
            editor.setPromptText("Enter expression... (use ${variable} for variables)");
        }

        // If options contain variable names, set them for autocomplete
        if (def.options() != null && def.options().length > 0) {
            editor.setAvailableVariables(java.util.List.of(def.options()));
        }

        editor.setOnExpressionChange(newVal -> viewModel.setParameter(def.key(), newVal));

        parameterFields.put(def.key(), editor);
        parametersContainer.getChildren().addAll(label, editor);
    }

    // ===== FXML Event Handlers =====

    @FXML
    private void onShowHelp() {
        viewModel.showHelp();
    }

    @FXML
    private void onClose() {
        viewModel.hide();
    }

    @FXML
    private void onShowAdvanced() {
        viewModel.showAdvancedEditor();
    }

    @FXML
    private void onReset() {
        viewModel.reset();
    }

    @FXML
    private void onApply() {
        viewModel.applyChanges();
    }

    // ===== Public API =====

    /**
     * Show the panel and load properties for the given node.
     *
     * @param node the node to edit
     */
    public void show(Node node) {
        viewModel.show(node);
    }

    /**
     * Show the panel and load properties for the given NodeView.
     * This is a convenience overload for compatibility with WorkflowCanvas.
     *
     * @param nodeView the NodeView containing the node to edit
     */
    public void show(NodeView nodeView) {
        if (nodeView != null) {
            viewModel.show(nodeView.getNode());
        }
    }

    /**
     * Hide the panel.
     */
    public void hide() {
        viewModel.hide();
    }

    /**
     * Check if panel is currently visible.
     *
     * @return true if visible
     */
    public boolean isShowing() {
        return viewModel.isVisible();
    }

    /**
     * Gets the underlying ViewModel.
     *
     * @return the ViewModel
     */
    public NodePropertiesViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the callback for when changes are applied.
     *
     * @param callback receives (nodeId, updatedNode)
     */
    public void setOnApplyChanges(java.util.function.BiConsumer<String, Node> callback) {
        viewModel.setOnApplyChanges(callback);
    }

    /**
     * Sets the callback for when the panel is closed.
     *
     * @param callback the callback
     */
    public void setOnClose(Runnable callback) {
        viewModel.setOnClose(callback);
    }

    /**
     * Sets the callback for showing help.
     *
     * @param callback receives node type
     */
    public void setOnShowHelp(java.util.function.Consumer<String> callback) {
        viewModel.setOnShowHelp(callback);
    }

    /**
     * Sets the callback for showing the advanced editor.
     *
     * @param callback receives node id
     */
    public void setOnShowAdvancedEditor(java.util.function.Consumer<String> callback) {
        viewModel.setOnShowAdvancedEditor(callback);
    }

    /**
     * Sets the callback for property changes.
     *
     * @param callback the callback
     */
    public void setOnPropertyChanged(Runnable callback) {
        viewModel.setOnPropertyChanged(callback);
    }
}
