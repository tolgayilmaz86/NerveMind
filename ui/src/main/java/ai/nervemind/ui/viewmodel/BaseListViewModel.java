package ai.nervemind.ui.viewmodel;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Base class for ViewModels that manage a list of items with selection.
 * 
 * <p>
 * Provides:
 * <ul>
 * <li>Observable list of items</li>
 * <li>Selected item tracking</li>
 * <li>Common list operations (add, remove, clear)</li>
 * </ul>
 * 
 * @param <T> the type of items in the list
 */
public abstract class BaseListViewModel<T> extends BaseViewModel {

    /**
     * Default constructor for BaseListViewModel.
     */
    protected BaseListViewModel() {
        // Base initialization
    }

    private final ListProperty<T> items = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();

    /**
     * The observable list of items.
     * UI can bind TableView/ListView items to this property.
     * 
     * @return the items ListProperty
     */
    public ListProperty<T> itemsProperty() {
        return items;
    }

    public ObservableList<T> getItems() {
        return items.get();
    }

    protected void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    /**
     * The currently selected item.
     * UI can bind to selection model.
     * 
     * @return the selectedItem ObjectProperty
     */
    public ObjectProperty<T> selectedItemProperty() {
        return selectedItem;
    }

    public T getSelectedItem() {
        return selectedItem.get();
    }

    public void setSelectedItem(T item) {
        this.selectedItem.set(item);
    }

    /**
     * Checks if an item is currently selected.
     * 
     * @return true if an item is selected
     */
    public boolean hasSelection() {
        return getSelectedItem() != null;
    }

    /**
     * Adds an item to the list.
     * 
     * @param item the item to add
     */
    public void addItem(T item) {
        if (item != null) {
            items.add(item);
            markDirty();
        }
    }

    /**
     * Removes an item from the list.
     * 
     * @param item the item to remove
     * @return true if the item was removed
     */
    public boolean removeItem(T item) {
        if (item != null && items.remove(item)) {
            if (item.equals(getSelectedItem())) {
                setSelectedItem(null);
            }
            markDirty();
            return true;
        }
        return false;
    }

    /**
     * Removes the currently selected item.
     * 
     * @return true if an item was removed
     */
    public boolean removeSelectedItem() {
        return removeItem(getSelectedItem());
    }

    /**
     * Clears all items from the list.
     */
    public void clearItems() {
        items.clear();
        setSelectedItem(null);
        markDirty();
    }

    /**
     * Refreshes the list by reloading from the data source.
     * Implementations should call the appropriate service method.
     */
    public abstract void refresh();

    /**
     * Gets the count of items in the list.
     * 
     * @return the number of items
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Checks if the list is empty.
     * 
     * @return true if the list contains no items
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
