package ai.nervemind.ui.viewmodel;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Base class for ViewModels that back dialog windows.
 * 
 * <p>
 * Extends {@link BaseViewModel} with dialog-specific functionality:
 * <ul>
 * <li>Result value handling (generic type T)</li>
 * <li>Validation state</li>
 * <li>Confirmation/cancellation tracking</li>
 * </ul>
 * 
 * @param <T> the type of result the dialog produces
 */
public abstract class BaseDialogViewModel<T> extends BaseViewModel {

    /**
     * Default constructor for BaseDialogViewModel.
     */
    protected BaseDialogViewModel() {
        // Base initialization
    }

    private final BooleanProperty valid = new SimpleBooleanProperty(true);
    private final BooleanProperty confirmed = new SimpleBooleanProperty(false);
    private final ObjectProperty<T> result = new SimpleObjectProperty<>();

    /**
     * Indicates whether the current dialog state is valid.
     * UI can bind save/OK buttons to this property.
     *
     * @return The validation property
     */
    public BooleanProperty validProperty() {
        return valid;
    }

    public boolean isValid() {
        return valid.get();
    }

    protected void setValid(boolean valid) {
        this.valid.set(valid);
    }

    /**
     * Indicates whether the user confirmed the dialog (OK/Save vs Cancel).
     *
     * @return The confirmation property
     */
    public BooleanProperty confirmedProperty() {
        return confirmed;
    }

    public boolean isConfirmed() {
        return confirmed.get();
    }

    protected void setConfirmed(boolean confirmed) {
        this.confirmed.set(confirmed);
    }

    /**
     * The result value produced by the dialog.
     *
     * @return The result propery
     */
    public ObjectProperty<T> resultProperty() {
        return result;
    }

    public T getResult() {
        return result.get();
    }

    protected void setResult(T result) {
        this.result.set(result);
    }

    /**
     * Gets the result if the dialog was confirmed, empty otherwise.
     * 
     * @return Optional containing the result if confirmed
     */
    public Optional<T> getConfirmedResult() {
        return isConfirmed() ? Optional.ofNullable(getResult()) : Optional.empty();
    }

    /**
     * Validates the current dialog state.
     * 
     * <p>
     * Implementations should:
     * <ol>
     * <li>Check all required fields</li>
     * <li>Validate field formats/ranges</li>
     * <li>Set error messages for invalid fields</li>
     * <li>Call {@link #setValid(boolean)} with the result</li>
     * </ol>
     * 
     * @return true if validation passes
     */
    public abstract boolean validate();

    /**
     * Called when the user clicks OK/Save/Confirm.
     * 
     * <p>
     * Default implementation validates and sets confirmed=true if valid.
     * Override to add custom confirmation logic.
     * 
     * @return true if confirmation was successful
     */
    public boolean confirm() {
        if (validate()) {
            buildResult();
            setConfirmed(true);
            return true;
        }
        return false;
    }

    /**
     * Called when the user clicks Cancel/Close.
     * 
     * <p>
     * Default implementation sets confirmed=false.
     * Override to add custom cancellation logic (e.g., prompting for unsaved
     * changes).
     */
    public void cancel() {
        setConfirmed(false);
    }

    /**
     * Builds the result object from the current dialog state.
     * Called after successful validation in {@link #confirm()}.
     * 
     * <p>
     * Implementations should construct the result object and call
     * {@link #setResult(Object)}.
     */
    protected abstract void buildResult();

    /**
     * Resets the dialog to its initial state.
     * Override to clear all fields and reset to defaults.
     */
    public void reset() {
        clearError();
        setValid(true);
        setConfirmed(false);
        setResult(null);
        clearDirty();
    }
}
