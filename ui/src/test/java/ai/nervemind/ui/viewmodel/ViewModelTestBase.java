/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Base class for ViewModel unit tests.
 * 
 * <p>
 * Provides common setup for testing ViewModels without JavaFX runtime.
 * ViewModels should only use javafx.beans.* which don't require
 * Platform.startup().
 * 
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *     &#64;code
 *     class MyViewModelTest extends ViewModelTestBase {
 *         private MyViewModel viewModel;
 * 
 *         &#64;BeforeEach
 *         void setUp() {
 *             viewModel = new MyViewModel();
 *         }
 * 
 *         @Test
 *         void testLoadingState() {
 *             assertFalse(viewModel.loadingProperty().get());
 *             viewModel.loadingProperty().set(true);
 *             assertTrue(viewModel.loadingProperty().get());
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class ViewModelTestBase {

    /**
     * Called before each test method.
     * Override in subclasses for test-specific setup.
     */
    @BeforeEach
    protected void baseSetUp() {
        // No JavaFX Platform setup needed for ViewModel tests
        // ViewModels should only use javafx.beans.* which work without runtime
    }

    /**
     * Helper method to create a BooleanProperty for testing.
     * 
     * @param initialValue Initial value
     * @return A new BooleanProperty
     */
    protected BooleanProperty createBooleanProperty(boolean initialValue) {
        return new SimpleBooleanProperty(initialValue);
    }

    /**
     * Helper method to create a StringProperty for testing.
     * 
     * @param initialValue Initial value
     * @return A new StringProperty
     */
    protected StringProperty createStringProperty(String initialValue) {
        return new SimpleStringProperty(initialValue);
    }

    /**
     * Asserts that two properties are bound together.
     * Changes to source should reflect in target.
     * 
     * @param <T>       The property value type
     * @param source    The source property
     * @param target    The target property (bound to source)
     * @param testValue A test value to verify binding
     */
    protected <T> void assertBound(
            javafx.beans.property.Property<T> source,
            javafx.beans.property.ReadOnlyProperty<T> target,
            T testValue) {
        source.setValue(testValue);
        assert target.getValue().equals(testValue)
                : "Properties are not bound. Expected: " + testValue + ", Got: " + target.getValue();
    }

    /**
     * Tracks property change events for verification.
     */
    public static class PropertyChangeTracker<T> {
        private int changeCount = 0;
        private T lastOldValue;
        private T lastNewValue;

        public void onChanged(javafx.beans.value.ObservableValue<? extends T> obs, T oldVal, T newVal) {
            changeCount++;
            lastOldValue = oldVal;
            lastNewValue = newVal;
        }

        public int getChangeCount() {
            return changeCount;
        }

        public T getLastOldValue() {
            return lastOldValue;
        }

        public T getLastNewValue() {
            return lastNewValue;
        }

        public void reset() {
            changeCount = 0;
            lastOldValue = null;
            lastNewValue = null;
        }
    }
}
