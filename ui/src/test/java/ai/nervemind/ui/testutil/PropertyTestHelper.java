/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * Utility class for testing JavaFX property bindings and changes.
 * 
 * <p>
 * Provides helpers to track property changes, verify bindings,
 * and assert observable list modifications.
 */
public final class PropertyTestHelper {

    private PropertyTestHelper() {
        // Utility class
    }

    /**
     * Creates a change tracker for a property.
     * 
     * @param <T>      The property value type
     * @param property The property to track
     * @return A tracker that records all changes
     */
    public static <T> ChangeTracker<T> trackChanges(ReadOnlyProperty<T> property) {
        ChangeTracker<T> tracker = new ChangeTracker<>();
        property.addListener(tracker::recordChange);
        return tracker;
    }

    /**
     * Creates a list change tracker for an observable list.
     * 
     * @param <T>  The list element type
     * @param list The list to track
     * @return A tracker that records all list modifications
     */
    public static <T> ListChangeTracker<T> trackListChanges(ObservableList<T> list) {
        ListChangeTracker<T> tracker = new ListChangeTracker<>();
        list.addListener(tracker);
        return tracker;
    }

    /**
     * Verifies that a property is bound to another property.
     * 
     * @param <T>        The property value type
     * @param source     The source property
     * @param target     The target property (should be bound to source)
     * @param testValues Values to test the binding with
     * @return true if all values propagate correctly
     */
    @SafeVarargs
    public static <T> boolean verifyBinding(
            Property<T> source,
            ReadOnlyProperty<T> target,
            T... testValues) {
        for (T value : testValues) {
            source.setValue(value);
            if (!value.equals(target.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tracks changes to a property value.
     * 
     * @param <T> The property value type
     */
    public static class ChangeTracker<T> {
        private final List<ChangeRecord<T>> changes = new ArrayList<>();
        private final AtomicInteger changeCount = new AtomicInteger(0);

        void recordChange(ObservableValue<? extends T> obs, T oldValue, T newValue) {
            changes.add(new ChangeRecord<>(oldValue, newValue, changeCount.incrementAndGet()));
        }

        public int getChangeCount() {
            return changeCount.get();
        }

        public List<ChangeRecord<T>> getChanges() {
            return new ArrayList<>(changes);
        }

        public ChangeRecord<T> getLastChange() {
            return changes.isEmpty() ? null : changes.get(changes.size() - 1);
        }

        public T getLastOldValue() {
            ChangeRecord<T> last = getLastChange();
            return last != null ? last.oldValue() : null;
        }

        public T getLastNewValue() {
            ChangeRecord<T> last = getLastChange();
            return last != null ? last.newValue() : null;
        }

        public void reset() {
            changes.clear();
            changeCount.set(0);
        }

        public boolean hasChanges() {
            return !changes.isEmpty();
        }
    }

    /**
     * Record of a single property change.
     * 
     * @param <T> The property value type
     */
    public record ChangeRecord<T>(T oldValue, T newValue, int sequence) {
    }

    /**
     * Tracks changes to an observable list.
     * 
     * @param <T> The list element type
     */
    public static class ListChangeTracker<T> implements ListChangeListener<T> {
        private final List<ListModification<T>> modifications = new ArrayList<>();
        private final AtomicInteger modificationCount = new AtomicInteger(0);

        @Override
        public void onChanged(Change<? extends T> c) {
            while (c.next()) {
                if (c.wasAdded()) {
                    modifications.add(new ListModification<>(
                            ModificationType.ADDED,
                            List.copyOf(c.getAddedSubList()),
                            c.getFrom()));
                }
                if (c.wasRemoved()) {
                    modifications.add(new ListModification<>(
                            ModificationType.REMOVED,
                            List.copyOf(c.getRemoved()),
                            c.getFrom()));
                }
                modificationCount.incrementAndGet();
            }
        }

        public int getModificationCount() {
            return modificationCount.get();
        }

        public List<ListModification<T>> getModifications() {
            return new ArrayList<>(modifications);
        }

        public int getAddCount() {
            return (int) modifications.stream()
                    .filter(m -> m.type() == ModificationType.ADDED)
                    .count();
        }

        public int getRemoveCount() {
            return (int) modifications.stream()
                    .filter(m -> m.type() == ModificationType.REMOVED)
                    .count();
        }

        public void reset() {
            modifications.clear();
            modificationCount.set(0);
        }
    }

    /**
     * Type of list modification.
     */
    public enum ModificationType {
        ADDED, REMOVED, UPDATED, PERMUTED
    }

    /**
     * Record of a list modification.
     * 
     * @param <T> The list element type
     */
    public record ListModification<T>(
            ModificationType type,
            List<T> elements,
            int position) {
    }
}
