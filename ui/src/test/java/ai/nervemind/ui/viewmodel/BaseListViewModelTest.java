/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javafx.collections.ListChangeListener;

/**
 * Unit tests for BaseListViewModel.
 * 
 * <p>
 * Verifies list management functionality: items, selection, add/remove.
 */
class BaseListViewModelTest extends ViewModelTestBase {

    private TestListViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new TestListViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("Should have empty items list")
        void shouldHaveEmptyItemsList() {
            assertTrue(viewModel.getItems().isEmpty());
            assertEquals(0, viewModel.getItemCount());
        }

        @Test
        @DisplayName("Should have no selection")
        void shouldHaveNoSelection() {
            assertNull(viewModel.getSelectedItem());
            assertFalse(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should be empty")
        void shouldBeEmpty() {
            assertTrue(viewModel.isEmpty());
        }
    }

    @Nested
    @DisplayName("Item Management")
    class ItemManagement {

        @Test
        @DisplayName("Should add item to list")
        void shouldAddItemToList() {
            viewModel.addItem("Item1");

            assertEquals(1, viewModel.getItemCount());
            assertEquals("Item1", viewModel.getItems().get(0));
            assertTrue(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should not add null item")
        void shouldNotAddNullItem() {
            viewModel.addItem(null);

            assertTrue(viewModel.isEmpty());
            assertFalse(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should remove item from list")
        void shouldRemoveItemFromList() {
            viewModel.getItems().addAll(Arrays.asList("Item1", "Item2", "Item3"));

            boolean removed = viewModel.removeItem("Item2");

            assertTrue(removed);
            assertEquals(2, viewModel.getItemCount());
            assertFalse(viewModel.getItems().contains("Item2"));
            assertTrue(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should return false when removing non-existent item")
        void shouldReturnFalseWhenRemovingNonExistentItem() {
            viewModel.getItems().add("Item1");

            boolean removed = viewModel.removeItem("NonExistent");

            assertFalse(removed);
            assertEquals(1, viewModel.getItemCount());
        }

        @Test
        @DisplayName("Should clear all items")
        void shouldClearAllItems() {
            viewModel.getItems().addAll(Arrays.asList("Item1", "Item2", "Item3"));
            viewModel.setSelectedItem("Item2");

            viewModel.clearItems();

            assertTrue(viewModel.isEmpty());
            assertNull(viewModel.getSelectedItem());
            assertTrue(viewModel.isDirty());
        }
    }

    @Nested
    @DisplayName("Selection")
    class Selection {

        @BeforeEach
        void setUpItems() {
            viewModel.getItems().addAll(Arrays.asList("Item1", "Item2", "Item3"));
        }

        @Test
        @DisplayName("Should set selected item")
        void shouldSetSelectedItem() {
            viewModel.setSelectedItem("Item2");

            assertEquals("Item2", viewModel.getSelectedItem());
            assertTrue(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should clear selection when set to null")
        void shouldClearSelectionWhenSetToNull() {
            viewModel.setSelectedItem("Item1");
            viewModel.setSelectedItem(null);

            assertNull(viewModel.getSelectedItem());
            assertFalse(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should clear selection when selected item is removed")
        void shouldClearSelectionWhenSelectedItemIsRemoved() {
            viewModel.setSelectedItem("Item2");

            viewModel.removeItem("Item2");

            assertNull(viewModel.getSelectedItem());
            assertFalse(viewModel.hasSelection());
        }

        @Test
        @DisplayName("Should remove selected item")
        void shouldRemoveSelectedItem() {
            viewModel.setSelectedItem("Item2");

            boolean removed = viewModel.removeSelectedItem();

            assertTrue(removed);
            assertEquals(2, viewModel.getItemCount());
            assertNull(viewModel.getSelectedItem());
        }

        @Test
        @DisplayName("Should return false when removing with no selection")
        void shouldReturnFalseWhenRemovingWithNoSelection() {
            boolean removed = viewModel.removeSelectedItem();

            assertFalse(removed);
            assertEquals(3, viewModel.getItemCount());
        }
    }

    @Nested
    @DisplayName("Observable List Changes")
    class ObservableListChanges {

        @Test
        @DisplayName("Should notify listeners on item add")
        void shouldNotifyListenersOnItemAdd() {
            AtomicInteger changeCount = new AtomicInteger(0);
            viewModel.getItems().addListener((ListChangeListener<String>) c -> changeCount.incrementAndGet());

            viewModel.addItem("Item1");
            viewModel.addItem("Item2");

            assertEquals(2, changeCount.get());
        }

        @Test
        @DisplayName("Should notify listeners on item remove")
        void shouldNotifyListenersOnItemRemove() {
            viewModel.getItems().addAll(Arrays.asList("Item1", "Item2"));

            AtomicInteger changeCount = new AtomicInteger(0);
            viewModel.getItems().addListener((ListChangeListener<String>) c -> changeCount.incrementAndGet());

            viewModel.removeItem("Item1");

            assertEquals(1, changeCount.get());
        }
    }

    @Nested
    @DisplayName("Property Binding")
    class PropertyBinding {

        @Test
        @DisplayName("Should expose items property for binding")
        void shouldExposeItemsPropertyForBinding() {
            assertNotNull(viewModel.itemsProperty());
            assertEquals(viewModel.getItems(), viewModel.itemsProperty().get());
        }

        @Test
        @DisplayName("Should expose selected item property for binding")
        void shouldExposeSelectedItemPropertyForBinding() {
            assertNotNull(viewModel.selectedItemProperty());

            viewModel.setSelectedItem("Test");
            assertEquals("Test", viewModel.selectedItemProperty().get());
        }
    }

    /**
     * Concrete implementation of BaseListViewModel for testing.
     */
    private static class TestListViewModel extends BaseListViewModel<String> {

        @Override
        public void refresh() {
            // No-op for tests
        }
    }
}
