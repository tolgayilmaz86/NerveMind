/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.canvas;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.kordamp.ikonli.javafx.FontIcon;

import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel;
import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel.PaletteCategory;
import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel.PaletteItem;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller/Component for the Node Palette.
 *
 * <p>
 * This custom component displays available node types organized by category.
 * It supports:
 * <ul>
 * <li>Search/filter across all node types</li>
 * <li>Collapsible category sections</li>
 * <li>Double-click to add node at canvas center</li>
 * <li>Drag-drop to add node at specific position</li>
 * </ul>
 *
 * <p>
 * Uses the fx:root pattern and loads NodePalette.fxml.
 */
public class NodePaletteComponent extends VBox {

    private final NodePaletteViewModel viewModel;

    // FXML components
    @FXML
    private TextField searchField;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox categoriesContainer;

    @FXML
    private ScrollPane filteredScrollPane;

    @FXML
    private VBox filteredContainer;

    @FXML
    private VBox emptyState;

    // Drag callback for when node is dropped on canvas
    private BiConsumer<PaletteItem, Point2D> onNodeDragComplete;

    /**
     * Creates a new NodePaletteComponent with a default ViewModel.
     */
    public NodePaletteComponent() {
        this(new NodePaletteViewModel());
    }

    /**
     * Creates a new NodePaletteComponent with the specified ViewModel.
     *
     * @param viewModel The view model to use
     */
    public NodePaletteComponent(NodePaletteViewModel viewModel) {
        this.viewModel = viewModel;
        loadFxml();
        setupBindings();
        buildCategories();
    }

    private void loadFxml() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ai/nervemind/ui/view/canvas/NodePalette.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load NodePalette.fxml", e);
        }
    }

    private void setupBindings() {
        // Bind search field to viewmodel
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());

        // Listen for search changes to toggle filtered view
        viewModel.searchTextProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSearching = newVal != null && !newVal.isBlank();
            toggleFilteredView(isSearching);
            if (isSearching) {
                rebuildFilteredItems();
            }
        });

        // Listen for filtered items changes
        viewModel.getFilteredItems().addListener((ListChangeListener<PaletteItem>) change -> {
            if (viewModel.getSearchText() != null && !viewModel.getSearchText().isBlank()) {
                rebuildFilteredItems();
            }
        });

        // Listen for category changes
        viewModel.getCategories().addListener((ListChangeListener<PaletteCategory>) change -> buildCategories());
    }

    private void toggleFilteredView(boolean showFiltered) {
        scrollPane.setVisible(!showFiltered);
        scrollPane.setManaged(!showFiltered);
        filteredScrollPane.setVisible(showFiltered);
        filteredScrollPane.setManaged(showFiltered);

        // Handle empty state
        boolean isEmpty = showFiltered && viewModel.getFilteredItems().isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        if (isEmpty) {
            filteredScrollPane.setVisible(false);
            filteredScrollPane.setManaged(false);
        }
    }

    private void buildCategories() {
        categoriesContainer.getChildren().clear();

        for (PaletteCategory category : viewModel.getCategories()) {
            TitledPane section = createCategorySection(category);
            categoriesContainer.getChildren().add(section);
        }
    }

    private TitledPane createCategorySection(PaletteCategory category) {
        VBox content = new VBox(2);
        content.setPadding(Insets.EMPTY);

        for (PaletteItem item : category.items()) {
            HBox itemView = createPaletteItemView(item);
            content.getChildren().add(itemView);
        }

        TitledPane section = new TitledPane(category.name(), content);
        section.setCollapsible(true);
        section.setExpanded(category.expanded());
        section.getStyleClass().add("palette-section");

        // Add category-specific class for styling
        String categoryClass = "palette-category--" + category.name().toLowerCase().replace(" ", "-");
        section.getStyleClass().add(categoryClass);

        // Track expanded state
        section.expandedProperty()
                .addListener((obs, oldVal, newVal) -> viewModel.setCategoryExpanded(category.name(), newVal));

        return section;
    }

    private void rebuildFilteredItems() {
        filteredContainer.getChildren().clear();

        if (viewModel.getFilteredItems().isEmpty()) {
            toggleFilteredView(true); // Will show empty state
            return;
        }

        for (PaletteItem item : viewModel.getFilteredItems()) {
            HBox itemView = createPaletteItemView(item);
            filteredContainer.getChildren().add(itemView);
        }
    }

    private HBox createPaletteItemView(PaletteItem item) {
        HBox itemBox = new HBox(10);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setPadding(new Insets(8, 12, 8, 12));
        itemBox.getStyleClass().add("palette-item");

        // Icon container with category color
        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("palette-item__icon");
        iconContainer.getStyleClass().add(getIconStyleClass(item.category()));

        FontIcon icon = new FontIcon(item.iconLiteral());
        icon.setIconSize(18);
        icon.getStyleClass().add("palette-item__icon-font");
        iconContainer.getChildren().add(icon);

        // Name label
        Label nameLabel = new Label(item.name());
        nameLabel.getStyleClass().add("palette-item__name");

        itemBox.getChildren().addAll(iconContainer, nameLabel);

        // Double-click to add at center
        itemBox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                viewModel.onItemDoubleClicked(item);
            }
        });

        // Drag detection
        itemBox.setOnDragDetected(e -> {
            itemBox.startFullDrag();
            itemBox.getStyleClass().add("palette-item--dragging");
            viewModel.onItemDragStarted(item);
            e.consume();
        });

        // Reset style on drag done
        itemBox.setOnMouseReleased(e -> {
            itemBox.getStyleClass().remove("palette-item--dragging");

            // Check if dropped outside palette (onto canvas)
            if (onNodeDragComplete != null && e.getSceneX() > getWidth()) {
                Point2D dropPoint = new Point2D(e.getSceneX(), e.getSceneY());
                onNodeDragComplete.accept(item, dropPoint);
            }
            e.consume();
        });

        return itemBox;
    }

    private String getIconStyleClass(String category) {
        return switch (category.toLowerCase()) {
            case "triggers" -> "palette-item__icon--trigger";
            case "actions" -> "palette-item__icon--action";
            case "flow control" -> "palette-item__icon--condition";
            case "ai" -> "palette-item__icon--ai";
            case "data" -> "palette-item__icon--data";
            case "advanced" -> "palette-item__icon--advanced";
            default -> "palette-item__icon--action";
        };
    }

    // ===== Public API =====

    /**
     * Gets the underlying ViewModel.
     * 
     * @return the underlying ViewModel
     */
    public NodePaletteViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Adds a plugin node to the palette.
     *
     * @param name        The display name
     * @param nodeType    The internal type
     * @param iconLiteral The icon literal (or null for default)
     * @param isTrigger   True if trigger, false if action
     */
    public void addPluginNode(String name, String nodeType, String iconLiteral, boolean isTrigger) {
        viewModel.addPluginNode(name, nodeType, iconLiteral, isTrigger);
        buildCategories();
    }

    /**
     * Removes a plugin node from the palette.
     *
     * @param nodeType The node type to remove
     */
    public void removePluginNode(String nodeType) {
        viewModel.removePluginNode(nodeType);
        buildCategories();
    }

    /**
     * Refreshes the palette (rebuilds categories).
     */
    public void refresh() {
        viewModel.refresh();
        buildCategories();
    }

    /**
     * Sets the callback for when a node is double-clicked.
     *
     * @param callback Receives (nodeType, name)
     */
    public void setOnNodeDoubleClick(BiConsumer<String, String> callback) {
        viewModel.setOnNodeDoubleClick(callback);
    }

    /**
     * Sets the callback for when a node drag starts.
     *
     * @param callback Receives (nodeType, name)
     */
    public void setOnNodeDragStart(BiConsumer<String, String> callback) {
        viewModel.setOnNodeDragStart(callback);
    }

    /**
     * Sets the callback for when a node drag completes (dropped on canvas).
     *
     * @param callback Receives (PaletteItem, Point2D dropPosition)
     */
    public void setOnNodeDragComplete(BiConsumer<PaletteItem, Point2D> callback) {
        this.onNodeDragComplete = callback;
    }

    /**
     * Clears the search field.
     */
    public void clearSearch() {
        searchField.clear();
    }
}
