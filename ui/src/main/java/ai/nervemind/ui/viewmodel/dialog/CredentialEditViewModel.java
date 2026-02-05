/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.ui.viewmodel.BaseDialogViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for the Credential Edit dialog.
 * 
 * <p>
 * Manages the state for adding or editing a credential.
 */
public class CredentialEditViewModel extends BaseDialogViewModel<CredentialEditViewModel.CredentialEditResult> {

    private final CredentialDTO existingCredential;
    private final boolean editMode;

    // Form fields
    private final StringProperty name = new SimpleStringProperty("");
    private final ObjectProperty<CredentialType> type = new SimpleObjectProperty<>(CredentialType.API_KEY);
    private final StringProperty data = new SimpleStringProperty("");
    private final BooleanProperty showData = new SimpleBooleanProperty(false);

    // Validation state
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);

    /**
     * Creates a new CredentialEditViewModel.
     * 
     * @param existing the existing credential to edit, or null for a new credential
     */
    public CredentialEditViewModel(CredentialDTO existing) {
        this.existingCredential = existing;
        this.editMode = existing != null;

        if (existing != null) {
            name.set(existing.name());
            type.set(existing.type());
        }

        // Setup validation
        name.addListener((obs, oldVal, newVal) -> updateFormValid());
        data.addListener((obs, oldVal, newVal) -> updateFormValid());
        updateFormValid();
    }

    // ===== Properties =====

    /**
     * Gets the name property.
     * 
     * @return the name property
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Gets the name value.
     * 
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the name value.
     * 
     * @param value the name to set
     */
    public void setName(String value) {
        name.set(value);
    }

    /**
     * Gets the type property.
     * 
     * @return the type property
     */
    public ObjectProperty<CredentialType> typeProperty() {
        return type;
    }

    /**
     * Gets the credential type.
     * 
     * @return the type
     */
    public CredentialType getType() {
        return type.get();
    }

    /**
     * Sets the credential type.
     * 
     * @param value the type to set
     */
    public void setType(CredentialType value) {
        type.set(value);
    }

    /**
     * Gets the data property.
     * 
     * @return the data property
     */
    public StringProperty dataProperty() {
        return data;
    }

    /**
     * Gets the credential data.
     * 
     * @return the data
     */
    public String getData() {
        return data.get();
    }

    /**
     * Sets the credential data.
     * 
     * @param value the data to set
     */
    public void setData(String value) {
        data.set(value);
    }

    /**
     * Gets the show data property.
     * 
     * @return the show data property
     */
    public BooleanProperty showDataProperty() {
        return showData;
    }

    /**
     * Checks if data should be shown.
     * 
     * @return true if data should be shown
     */
    public boolean isShowData() {
        return showData.get();
    }

    /**
     * Sets whether data should be shown.
     * 
     * @param value true to show data
     */
    public void setShowData(boolean value) {
        showData.set(value);
    }

    /**
     * Gets the form validity property.
     * 
     * @return the form validity property
     */
    public ReadOnlyBooleanProperty formValidProperty() {
        return formValid;
    }

    /**
     * Checks if the form is valid.
     * 
     * @return true if the form is valid
     */
    public boolean isFormValid() {
        return formValid.get();
    }

    /**
     * Checks if in edit mode.
     * 
     * @return true if in edit mode
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * Gets the existing credential.
     * 
     * @return the existing credential or null
     */
    public CredentialDTO getExistingCredential() {
        return existingCredential;
    }

    // ===== Validation =====

    private void updateFormValid() {
        boolean nameValid = name.get() != null && !name.get().trim().isEmpty();
        boolean dataValid = editMode || (data.get() != null && !data.get().isEmpty());
        formValid.set(nameValid && dataValid);
    }

    @Override
    public boolean validate() {
        boolean valid = isFormValid();
        setValid(valid);
        return valid;
    }

    @Override
    protected void buildResult() {
        CredentialDTO dto = new CredentialDTO(
                existingCredential != null ? existingCredential.id() : null,
                name.get().trim(),
                type.get(),
                existingCredential != null ? existingCredential.createdAt() : null,
                null);
        String credentialData = data.get();
        setResult(new CredentialEditResult(dto, credentialData.isBlank() ? null : credentialData));
    }

    // ===== Result Record =====

    /**
     * Result record for credential edit dialog.
     *
     * @param dto  The credential metadata (name, type)
     * @param data The sensitive credential data (API key, password, etc.), or null
     *             if unchanged in edit mode
     */
    public record CredentialEditResult(CredentialDTO dto, String data) {
    }
}
