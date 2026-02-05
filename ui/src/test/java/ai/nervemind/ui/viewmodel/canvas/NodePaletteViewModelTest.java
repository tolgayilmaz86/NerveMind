/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;
import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel.PaletteCategory;
import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel.PaletteItem;

/**
 * Unit tests for NodePaletteViewModel.
 *
 * <p>
 * Tests cover:
 * <ul>
 * <li>Initial state and built-in nodes</li>
 * <li>Category management</li>
 * <li>Search/filter functionality</li>
 * <li>Plugin node management</li>
 * <li>Callback invocations</li>
 * </ul>
 */
class NodePaletteViewModelTest extends ViewModelTestBase {

    private NodePaletteViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new NodePaletteViewModel();
    }

    @Nested
    class InitialState {

        @Test
        void shouldHaveEmptySearchText() {
            assertEquals("", viewModel.getSearchText());
        }

        @Test
        void shouldHaveBuiltInCategories() {
            List<String> categoryNames = viewModel.getCategories().stream()
                    .map(PaletteCategory::name)
                    .toList();

            assertTrue(categoryNames.contains("Triggers"), "Should have Triggers category");
            assertTrue(categoryNames.contains("Actions"), "Should have Actions category");
            assertTrue(categoryNames.contains("Flow Control"), "Should have Flow Control category");
            assertTrue(categoryNames.contains("Data"), "Should have Data category");
            assertTrue(categoryNames.contains("AI"), "Should have AI category");
            assertTrue(categoryNames.contains("Advanced"), "Should have Advanced category");
        }

        @Test
        void shouldHaveBuiltInNodes() {
            // Triggers
            assertTrue(viewModel.hasNodeType("manualTrigger"));
            assertTrue(viewModel.hasNodeType("scheduleTrigger"));
            assertTrue(viewModel.hasNodeType("webhookTrigger"));

            // Actions
            assertTrue(viewModel.hasNodeType("httpRequest"));
            assertTrue(viewModel.hasNodeType("code"));
            assertTrue(viewModel.hasNodeType("executeCommand"));

            // Flow Control
            assertTrue(viewModel.hasNodeType("if"));
            assertTrue(viewModel.hasNodeType("switch"));
            assertTrue(viewModel.hasNodeType("merge"));
            assertTrue(viewModel.hasNodeType("loop"));

            // Data
            assertTrue(viewModel.hasNodeType("set"));
            assertTrue(viewModel.hasNodeType("filter"));
            assertTrue(viewModel.hasNodeType("sort"));

            // AI
            assertTrue(viewModel.hasNodeType("llmChat"));
            assertTrue(viewModel.hasNodeType("textClassifier"));
            assertTrue(viewModel.hasNodeType("embedding"));
            assertTrue(viewModel.hasNodeType("rag"));

            // Advanced
            assertTrue(viewModel.hasNodeType("subworkflow"));
            assertTrue(viewModel.hasNodeType("parallel"));
            assertTrue(viewModel.hasNodeType("tryCatch"));
            assertTrue(viewModel.hasNodeType("retry"));
            assertTrue(viewModel.hasNodeType("rate_limit"));
        }

        @Test
        void shouldHaveAllItemsInFilteredListInitially() {
            assertEquals(viewModel.getTotalNodeCount(), viewModel.getFilteredNodeCount());
        }

        @Test
        void shouldHaveNoSelectedItem() {
            assertNull(viewModel.getSelectedItem());
        }
    }

    @Nested
    class CategoryManagement {

        @Test
        void shouldPreserveCategoryOrder() {
            List<String> categoryNames = viewModel.getCategories().stream()
                    .map(PaletteCategory::name)
                    .toList();

            // Expected order
            int triggersIndex = categoryNames.indexOf("Triggers");
            int actionsIndex = categoryNames.indexOf("Actions");
            int flowControlIndex = categoryNames.indexOf("Flow Control");
            int dataIndex = categoryNames.indexOf("Data");
            int aiIndex = categoryNames.indexOf("AI");
            int advancedIndex = categoryNames.indexOf("Advanced");

            assertTrue(triggersIndex < actionsIndex, "Triggers should come before Actions");
            assertTrue(actionsIndex < flowControlIndex, "Actions should come before Flow Control");
            assertTrue(flowControlIndex < dataIndex, "Flow Control should come before Data");
            assertTrue(dataIndex < aiIndex, "Data should come before AI");
            assertTrue(aiIndex < advancedIndex, "AI should come before Advanced");
        }

        @Test
        void shouldHaveExpandedCategoriesByDefault() {
            for (PaletteCategory category : viewModel.getCategories()) {
                assertTrue(category.expanded(), "Category '" + category.name() + "' should be expanded by default");
            }
        }

        @Test
        void shouldUpdateCategoryExpandedState() {
            viewModel.setCategoryExpanded("Triggers", false);

            PaletteCategory triggers = viewModel.getCategories().stream()
                    .filter(c -> c.name().equals("Triggers"))
                    .findFirst()
                    .orElseThrow();

            assertFalse(triggers.expanded(), "Triggers should be collapsed");
        }

        @Test
        void shouldGetNodeTypesInCategory() {
            List<String> triggerTypes = viewModel.getNodeTypesInCategory("Triggers");

            assertTrue(triggerTypes.contains("manualTrigger"));
            assertTrue(triggerTypes.contains("scheduleTrigger"));
            assertTrue(triggerTypes.contains("webhookTrigger"));
            assertFalse(triggerTypes.contains("httpRequest"), "httpRequest should not be in Triggers");
        }
    }

    @Nested
    class SearchAndFilter {

        @Test
        void shouldFilterByNodeName() {
            viewModel.setSearchText("HTTP");

            List<String> filteredTypes = viewModel.getFilteredItems().stream()
                    .map(PaletteItem::nodeType)
                    .toList();

            assertTrue(filteredTypes.contains("httpRequest"));
            assertEquals(1, filteredTypes.size());
        }

        @Test
        void shouldFilterCaseInsensitive() {
            viewModel.setSearchText("http");

            List<String> filteredTypes = viewModel.getFilteredItems().stream()
                    .map(PaletteItem::nodeType)
                    .toList();

            assertTrue(filteredTypes.contains("httpRequest"));
        }

        @Test
        void shouldFilterByNodeType() {
            viewModel.setSearchText("llm");

            List<String> filteredTypes = viewModel.getFilteredItems().stream()
                    .map(PaletteItem::nodeType)
                    .toList();

            assertTrue(filteredTypes.contains("llmChat"));
        }

        @Test
        void shouldFilterByCategory() {
            viewModel.setSearchText("ai");

            List<String> filteredTypes = viewModel.getFilteredItems().stream()
                    .map(PaletteItem::nodeType)
                    .toList();

            // Should include all AI category nodes
            assertTrue(filteredTypes.contains("llmChat"));
            assertTrue(filteredTypes.contains("textClassifier"));
            assertTrue(filteredTypes.contains("embedding"));
            assertTrue(filteredTypes.contains("rag"));
        }

        @Test
        void shouldReturnEmptyListForNoMatch() {
            viewModel.setSearchText("xyznonexistent");

            assertTrue(viewModel.getFilteredItems().isEmpty());
            assertEquals(0, viewModel.getFilteredNodeCount());
        }

        @Test
        void shouldShowAllNodesWhenSearchCleared() {
            viewModel.setSearchText("http");
            assertEquals(1, viewModel.getFilteredNodeCount());

            viewModel.setSearchText("");
            assertEquals(viewModel.getTotalNodeCount(), viewModel.getFilteredNodeCount());
        }

        @Test
        void shouldShowAllNodesForBlankSearch() {
            viewModel.setSearchText("   ");

            assertEquals(viewModel.getTotalNodeCount(), viewModel.getFilteredNodeCount());
        }

        @Test
        void shouldMatchPartialNames() {
            viewModel.setSearchText("trig");

            List<String> filteredNames = viewModel.getFilteredItems().stream()
                    .map(PaletteItem::name)
                    .toList();

            assertTrue(filteredNames.stream().anyMatch(n -> n.contains("Trigger")));
        }
    }

    @Nested
    class PluginNodeManagement {

        @Test
        void shouldAddPluginTrigger() {
            int originalCount = viewModel.getTotalNodeCount();

            viewModel.addPluginNode("File Watcher", "fileWatcher", "mdmz-file_eye", true);

            assertEquals(originalCount + 1, viewModel.getTotalNodeCount());
            assertTrue(viewModel.hasNodeType("fileWatcher"));
        }

        @Test
        void shouldAddPluginAction() {
            int originalCount = viewModel.getTotalNodeCount();

            viewModel.addPluginNode("Send Email", "sendEmail", "mdmz-email", false);

            assertEquals(originalCount + 1, viewModel.getTotalNodeCount());
            assertTrue(viewModel.hasNodeType("sendEmail"));
        }

        @Test
        void shouldRemovePluginNode() {
            viewModel.addPluginNode("Test Plugin", "testPlugin", null, false);
            assertTrue(viewModel.hasNodeType("testPlugin"));

            viewModel.removePluginNode("testPlugin");

            assertFalse(viewModel.hasNodeType("testPlugin"));
        }

        @Test
        void shouldUpdateFilteredListAfterAddingPlugin() {
            viewModel.setSearchText("custom");
            int initialFiltered = viewModel.getFilteredNodeCount();

            viewModel.addPluginNode("Custom Plugin", "customPlugin", null, false);

            assertEquals(initialFiltered + 1, viewModel.getFilteredNodeCount());
        }

        @Test
        void shouldResetToBuiltInNodes() {
            viewModel.addPluginNode("Plugin1", "plugin1", null, true);
            viewModel.addPluginNode("Plugin2", "plugin2", null, false);
            int countWithPlugins = viewModel.getTotalNodeCount();

            viewModel.reset();

            assertTrue(viewModel.getTotalNodeCount() < countWithPlugins);
            assertFalse(viewModel.hasNodeType("plugin1"));
            assertFalse(viewModel.hasNodeType("plugin2"));
            assertTrue(viewModel.hasNodeType("httpRequest"), "Should still have built-in nodes");
        }
    }

    @Nested
    class Callbacks {

        @Test
        void shouldInvokeDoubleClickCallback() {
            AtomicReference<String> capturedType = new AtomicReference<>();
            AtomicReference<String> capturedName = new AtomicReference<>();

            viewModel.setOnNodeDoubleClick((type, name) -> {
                capturedType.set(type);
                capturedName.set(name);
            });

            PaletteItem item = new PaletteItem("HTTP Request", "httpRequest", "mdmz-web", "Actions");
            viewModel.onItemDoubleClicked(item);

            assertEquals("httpRequest", capturedType.get());
            assertEquals("HTTP Request", capturedName.get());
        }

        @Test
        void shouldNotFailWhenDoubleClickCallbackIsNull() {
            viewModel.setOnNodeDoubleClick(null);
            PaletteItem item = new PaletteItem("HTTP Request", "httpRequest", "mdmz-web", "Actions");

            assertDoesNotThrow(() -> viewModel.onItemDoubleClicked(item));
        }

        @Test
        void shouldInvokeDragStartCallback() {
            AtomicReference<String> capturedType = new AtomicReference<>();
            AtomicReference<String> capturedName = new AtomicReference<>();

            viewModel.setOnNodeDragStart((type, name) -> {
                capturedType.set(type);
                capturedName.set(name);
            });

            PaletteItem item = new PaletteItem("If", "if", "mdmz-call_split", "Flow Control");
            viewModel.onItemDragStarted(item);

            assertEquals("if", capturedType.get());
            assertEquals("If", capturedName.get());
        }

        @Test
        void shouldNotFailWhenDragStartCallbackIsNull() {
            viewModel.setOnNodeDragStart(null);
            PaletteItem item = new PaletteItem("If", "if", "mdmz-call_split", "Flow Control");

            assertDoesNotThrow(() -> viewModel.onItemDragStarted(item));
        }

        @Test
        void shouldNotInvokeCallbackForNullItem() {
            AtomicBoolean called = new AtomicBoolean(false);
            viewModel.setOnNodeDoubleClick((type, name) -> called.set(true));

            viewModel.onItemDoubleClicked(null);

            assertFalse(called.get(), "Should not invoke callback for null item");
        }
    }

    @Nested
    class Selection {

        @Test
        void shouldSetSelectedItem() {
            PaletteItem item = new PaletteItem("Code", "code", "mdmz-code", "Actions");

            viewModel.setSelectedItem(item);

            assertEquals(item, viewModel.getSelectedItem());
        }

        @Test
        void shouldClearSelectedItem() {
            PaletteItem item = new PaletteItem("Code", "code", "mdmz-code", "Actions");
            viewModel.setSelectedItem(item);

            viewModel.setSelectedItem(null);

            assertNull(viewModel.getSelectedItem());
        }

        @Test
        void shouldSupportPropertyBinding() {
            AtomicReference<PaletteItem> capturedItem = new AtomicReference<>();
            viewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> capturedItem.set(newVal));

            PaletteItem item = new PaletteItem("Loop", "loop", "mdmz-repeat", "Flow Control");
            viewModel.setSelectedItem(item);

            assertEquals(item, capturedItem.get());
        }
    }

    @Nested
    class PaletteItemRecord {

        @Test
        void shouldCreatePaletteItem() {
            PaletteItem item = new PaletteItem("Test Node", "testNode", "mdmz-test", "TestCategory");

            assertEquals("Test Node", item.name());
            assertEquals("testNode", item.nodeType());
            assertEquals("mdmz-test", item.iconLiteral());
            assertEquals("TestCategory", item.category());
        }

        @Test
        void shouldBeEqualForSameValues() {
            PaletteItem item1 = new PaletteItem("Name", "type", "icon", "cat");
            PaletteItem item2 = new PaletteItem("Name", "type", "icon", "cat");

            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
        }
    }

    @Nested
    class PaletteCategoryRecord {

        @Test
        void shouldCreatePaletteCategory() {
            List<PaletteItem> items = List.of(
                    new PaletteItem("Item1", "item1", "icon1", "Cat"),
                    new PaletteItem("Item2", "item2", "icon2", "Cat"));

            PaletteCategory category = new PaletteCategory("Cat", true, items);

            assertEquals("Cat", category.name());
            assertTrue(category.expanded());
            assertEquals(2, category.items().size());
        }
    }

    @Nested
    class RefreshBehavior {

        @Test
        void shouldRebuildCategoriesOnRefresh() {
            viewModel.addPluginNode("New Plugin", "newPlugin", null, true);
            int categoriesCount = viewModel.getCategories().size();

            viewModel.refresh();

            assertEquals(categoriesCount, viewModel.getCategories().size());
            assertTrue(viewModel.hasNodeType("newPlugin"));
        }
    }
}
