/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Node Palette component.
 *
 * <p>
 * Manages:
 * <ul>
 * <li>Node categories and items within each category</li>
 * <li>Search/filter functionality across all nodes</li>
 * <li>Selection and drag initiation callbacks</li>
 * </ul>
 *
 * <p>
 * <strong>IMPORTANT:</strong> This ViewModel does NOT import javafx.scene.*
 * classes.
 * Only javafx.beans.* and javafx.collections.* imports are allowed.
 */
public class NodePaletteViewModel extends BaseViewModel {

    // ===== Node Item Model =====

    /**
     * Represents a single palette item (a node type that can be dragged onto the
     * canvas).
     *
     * @param name        The display name of the node
     * @param nodeType    The internal node type identifier (e.g., "httpRequest",
     *                    "if")
     * @param iconLiteral The icon literal string for FontIcon (e.g.,
     *                    "mdomz-play_circle")
     * @param category    The category this node belongs to
     */
    public record PaletteItem(String name, String nodeType, String iconLiteral, String category) {
    }

    /**
     * Represents a category of nodes in the palette.
     *
     * @param name     The display name of the category
     * @param expanded Whether the category is currently expanded
     * @param items    The items within this category
     */
    public record PaletteCategory(String name, boolean expanded, List<PaletteItem> items) {
    }

    // ===== Properties =====

    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObservableList<PaletteCategory> categories = FXCollections.observableArrayList();
    private final ObservableList<PaletteItem> filteredItems = FXCollections.observableArrayList();
    private final ObjectProperty<PaletteItem> selectedItem = new SimpleObjectProperty<>();

    // Master list of all items (not filtered)
    private final List<PaletteItem> allItems = new ArrayList<>();

    // Callbacks
    private BiConsumer<String, String> onNodeDoubleClick; // (nodeType, name) -> void
    private BiConsumer<String, String> onNodeDragStart; // (nodeType, name) -> void

    // ===== Constructor =====

    /**
     * Creates a new NodePaletteViewModel with default categories.
     */
    public NodePaletteViewModel() {
        // Initialize with built-in node types
        initializeBuiltInNodes();

        // Initialize filtered items with all items
        applyFilter();

        // Setup search filtering
        searchText.addListener((obs, oldVal, newVal) -> applyFilter());
    }

    // ===== Initialization =====

    private void initializeBuiltInNodes() {
        // Triggers
        addItem("Manual Trigger", "manualTrigger", "mdmz-play_circle", "Triggers");
        addItem("Schedule", "scheduleTrigger", "mdmz-clock_loader_40", "Triggers");
        addItem("Webhook", "webhookTrigger", "mdmz-webhook", "Triggers");

        // Actions
        addItem("HTTP Request", "httpRequest", "mdmz-web", "Actions");
        addItem("Code", "code", "mdmz-code", "Actions");
        addItem("Execute Command", "executeCommand", "mdomz-terminal", "Actions");

        // Flow Control
        addItem("If", "if", "mdmz-call_split", "Flow Control");
        addItem("Switch", "switch", "mdmz-swap_horiz", "Flow Control");
        addItem("Merge", "merge", "mdmz-call_merge", "Flow Control");
        addItem("Loop", "loop", "mdmz-repeat", "Flow Control");

        // Data
        addItem("Set", "set", "mdmz-edit", "Data");
        addItem("Filter", "filter", "mdomz-filter_alt", "Data");
        addItem("Sort", "sort", "mdmz-sort", "Data");

        // AI
        addItem("LLM Chat", "llmChat", "mdomz-smart_toy", "AI");
        addItem("Text Classifier", "textClassifier", "mdomz-label", "AI");
        addItem("Embedding", "embedding", "mdmz-vector_line", "AI");
        addItem("RAG", "rag", "mdmz-book_4_spark", "AI");

        // Advanced
        addItem("Subworkflow", "subworkflow", "mdmz-account_tree", "Advanced");
        addItem("Parallel", "parallel", "mdomz-view_week", "Advanced");
        addItem("Try/Catch", "tryCatch", "mdmz-shield_with_heart", "Advanced");
        addItem("Retry", "retry", "mdmz-refresh", "Advanced");
        addItem("Rate Limit", "rate_limit", "mdmz-speed", "Advanced");

        // Build categories from items
        rebuildCategories();
    }

    // ===== Public API =====

    /**
     * Adds a plugin node to the palette.
     *
     * @param name        The display name
     * @param nodeType    The internal type identifier
     * @param iconLiteral The icon literal
     * @param isTrigger   True if this is a trigger, false if an action
     */
    public void addPluginNode(String name, String nodeType, String iconLiteral, boolean isTrigger) {
        String category = isTrigger ? "Triggers" : "Actions";
        addItem(name, nodeType, iconLiteral != null ? iconLiteral : "mdmz-view_module", category);
        rebuildCategories();
        applyFilter();
    }

    /**
     * Removes a plugin node from the palette.
     *
     * @param nodeType The node type to remove
     */
    public void removePluginNode(String nodeType) {
        allItems.removeIf(item -> item.nodeType().equals(nodeType));
        rebuildCategories();
        applyFilter();
    }

    /**
     * Refreshes the palette, rebuilding categories. Call this after bulk changes.
     */
    public void refresh() {
        rebuildCategories();
        applyFilter();
    }

    /**
     * Clears all nodes and resets to built-in nodes only.
     */
    public void reset() {
        allItems.clear();
        initializeBuiltInNodes();
    }

    /**
     * Called when a node item is double-clicked. Notifies the callback to add the
     * node at center.
     *
     * @param item The item that was double-clicked
     */
    public void onItemDoubleClicked(PaletteItem item) {
        if (item != null && onNodeDoubleClick != null) {
            onNodeDoubleClick.accept(item.nodeType(), item.name());
        }
    }

    /**
     * Called when drag is initiated on a node item.
     *
     * @param item The item being dragged
     */
    public void onItemDragStarted(PaletteItem item) {
        if (item != null && onNodeDragStart != null) {
            onNodeDragStart.accept(item.nodeType(), item.name());
        }
    }

    /**
     * Sets the category expanded state.
     *
     * @param categoryName The category name
     * @param expanded     True to expand, false to collapse
     */
    public void setCategoryExpanded(String categoryName, boolean expanded) {
        for (int i = 0; i < categories.size(); i++) {
            PaletteCategory cat = categories.get(i);
            if (cat.name().equals(categoryName)) {
                categories.set(i, new PaletteCategory(cat.name(), expanded, cat.items()));
                break;
            }
        }
    }

    // ===== Internal Methods =====

    private void addItem(String name, String nodeType, String iconLiteral, String category) {
        allItems.add(new PaletteItem(name, nodeType, iconLiteral, category));
    }

    private void rebuildCategories() {
        // Group items by category, preserving order
        List<String> categoryOrder = List.of("Triggers", "Actions", "Flow Control", "Data", "AI", "Advanced");
        List<PaletteCategory> newCategories = new ArrayList<>();

        for (String catName : categoryOrder) {
            List<PaletteItem> itemsInCategory = allItems.stream()
                    .filter(item -> item.category().equals(catName))
                    .toList();
            if (!itemsInCategory.isEmpty()) {
                // Preserve expanded state if category already exists
                boolean expanded = categories.stream()
                        .filter(c -> c.name().equals(catName))
                        .findFirst()
                        .map(PaletteCategory::expanded)
                        .orElse(true);
                newCategories.add(new PaletteCategory(catName, expanded, itemsInCategory));
            }
        }

        // Add any categories not in the predefined order
        allItems.stream()
                .map(PaletteItem::category)
                .distinct()
                .filter(cat -> !categoryOrder.contains(cat))
                .forEach(cat -> {
                    List<PaletteItem> items = allItems.stream()
                            .filter(item -> item.category().equals(cat))
                            .toList();
                    if (!items.isEmpty()) {
                        newCategories.add(new PaletteCategory(cat, true, items));
                    }
                });

        categories.setAll(newCategories);
    }

    private void applyFilter() {
        String search = searchText.get();
        if (search == null || search.isBlank()) {
            filteredItems.setAll(allItems);
        } else {
            String lowerSearch = search.toLowerCase(Locale.ROOT);
            filteredItems.setAll(allItems.stream()
                    .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(lowerSearch)
                            || item.nodeType().toLowerCase(Locale.ROOT).contains(lowerSearch)
                            || item.category().toLowerCase(Locale.ROOT).contains(lowerSearch))
                    .toList());
        }
    }

    // ===== Property Accessors =====

    /**
     * Gets the search text property.
     * 
     * @return the search text property
     */
    public StringProperty searchTextProperty() {
        return searchText;
    }

    /**
     * Gets the search text.
     * 
     * @return the search text
     */
    public String getSearchText() {
        return searchText.get();
    }

    /**
     * Sets the search text.
     * 
     * @param text the search text to set
     */
    public void setSearchText(String text) {
        searchText.set(text);
    }

    /**
     * Gets the categories list.
     * 
     * @return the categories list
     */
    public ObservableList<PaletteCategory> getCategories() {
        return categories;
    }

    /**
     * Gets the filtered items list.
     * 
     * @return the filtered items list
     */
    public ObservableList<PaletteItem> getFilteredItems() {
        return filteredItems;
    }

    /**
     * Gets the selected item property.
     * 
     * @return the selected item property
     */
    public ObjectProperty<PaletteItem> selectedItemProperty() {
        return selectedItem;
    }

    /**
     * Gets the selected item.
     * 
     * @return the selected item
     */
    public PaletteItem getSelectedItem() {
        return selectedItem.get();
    }

    /**
     * Sets the selected item.
     * 
     * @param item the item to select
     */
    public void setSelectedItem(PaletteItem item) {
        selectedItem.set(item);
    }

    // ===== Callback Setters =====

    /**
     * Sets the callback for double-click on a palette item.
     *
     * @param callback Receives (nodeType, name)
     */
    public void setOnNodeDoubleClick(BiConsumer<String, String> callback) {
        this.onNodeDoubleClick = callback;
    }

    /**
     * Sets the callback for drag start on a palette item.
     *
     * @param callback Receives (nodeType, name)
     */
    public void setOnNodeDragStart(BiConsumer<String, String> callback) {
        this.onNodeDragStart = callback;
    }

    // ===== Utility Methods =====

    /**
     * Gets the total number of nodes in the palette.
     *
     * @return the total number of nodes
     */
    public int getTotalNodeCount() {
        return allItems.size();
    }

    /**
     * Gets the number of nodes visible after filtering.
     *
     * @return the number of filtered nodes
     */
    public int getFilteredNodeCount() {
        return filteredItems.size();
    }

    /**
     * Checks if a specific node type exists in the palette.
     *
     * @param nodeType the node type to check for
     * @return true if the node type exists
     */
    public boolean hasNodeType(String nodeType) {
        return allItems.stream().anyMatch(item -> item.nodeType().equals(nodeType));
    }

    /**
     * Gets all node types in a specific category.
     *
     * @param category the category name
     * @return list of node types in the category
     */
    public List<String> getNodeTypesInCategory(String category) {
        return allItems.stream()
                .filter(item -> item.category().equals(category))
                .map(PaletteItem::nodeType)
                .toList();
    }
}
