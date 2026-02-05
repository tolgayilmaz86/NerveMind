package ai.nervemind.ui.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Base class for all ViewModels in the application.
 * 
 * <p>
 * Provides common functionality:
 * <ul>
 * <li>Loading state management</li>
 * <li>Error message handling</li>
 * <li>Dirty/modified state tracking</li>
 * </ul>
 * 
 * <p>
 * <strong>IMPORTANT:</strong> ViewModels must NOT import javafx.scene.*
 * classes.
 * Only javafx.beans.* imports are allowed. This ensures ViewModels are testable
 * without the JavaFX runtime.
 */
public abstract class BaseViewModel {

    /**
     * Default constructor for BaseViewModel.
     */
    protected BaseViewModel() {
        // Base initialization
    }

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty hasError = new SimpleBooleanProperty(false);

    /**
     * Indicates whether the ViewModel is currently loading data.
     * UI can bind to this to show loading indicators.
     * 
     * @return the loading BooleanProperty
     */
    public BooleanProperty loadingProperty() {
        return loading;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    /**
     * Indicates whether the ViewModel has unsaved changes.
     * UI can bind to this to enable/disable save buttons or show warnings.
     * 
     * @return the dirty BooleanProperty
     */
    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }

    /**
     * Marks the ViewModel as modified. Call this when data changes.
     */
    protected void markDirty() {
        setDirty(true);
    }

    /**
     * Clears the dirty flag. Call this after saving.
     */
    protected void clearDirty() {
        setDirty(false);
    }

    /**
     * The current error message, if any.
     * UI can bind to this to display error alerts or messages.
     * 
     * @return the error message StringProperty
     */
    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    /**
     * Sets an error message and updates the hasError flag.
     * 
     * @param message the error message, or null to clear
     */
    public void setErrorMessage(String message) {
        this.errorMessage.set(message);
        this.hasError.set(message != null && !message.isBlank());
    }

    /**
     * Clears any error state.
     */
    public void clearError() {
        setErrorMessage(null);
    }

    /**
     * Indicates whether the ViewModel currently has an error.
     * UI can bind to this to show/hide error indicators.
     * 
     * @return the hasError BooleanProperty
     */
    public BooleanProperty hasErrorProperty() {
        return hasError;
    }

    /**
     * Checks if the ViewModel has an error.
     * 
     * @return true if the ViewModel currently has an error
     */
    public boolean hasError() {
        return hasError.get();
    }

    /**
     * Called when the ViewModel is being initialized.
     * Override to load initial data.
     */
    public void initialize() {
        // Default implementation does nothing
    }

    /**
     * Called when the ViewModel is being destroyed/closed.
     * Override to clean up resources.
     */
    public void dispose() {
        // Default implementation does nothing
    }

    /**
     * Executes an action with loading state management.
     * Sets loading=true before, loading=false after, and handles errors.
     * 
     * @param action the action to execute
     */
    protected void withLoading(Runnable action) {
        try {
            setLoading(true);
            clearError();
            action.run();
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        } finally {
            setLoading(false);
        }
    }

    /**
     * Executes an action and marks the ViewModel as dirty on success.
     * 
     * @param action the action to execute
     */
    protected void withDirtyTracking(Runnable action) {
        try {
            action.run();
            markDirty();
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
    }
}
