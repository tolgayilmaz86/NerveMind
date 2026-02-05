/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.common.service.CredentialServiceInterface;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Credential Manager dialog.
 * 
 * <p>
 * Manages the list of credentials and CRUD operations.
 * Does not depend on any JavaFX UI classes - only uses javafx.beans and
 * javafx.collections.
 */
public class CredentialManagerViewModel extends BaseViewModel {

    /** Golden yellow color for all credential types. */
    public static final String TYPE_COLOR = "#fbbf24";

    private final CredentialServiceInterface credentialService;

    private final ObservableList<CredentialDTO> credentials = FXCollections.observableArrayList();
    private final ObjectProperty<CredentialDTO> selectedCredential = new SimpleObjectProperty<>();
    private final BooleanProperty hasSelection = new SimpleBooleanProperty(false);

    /**
     * Creates a new CredentialManagerViewModel.
     * 
     * @param credentialService the service for credential operations
     */
    public CredentialManagerViewModel(CredentialServiceInterface credentialService) {
        this.credentialService = credentialService;

        // Track selection
        selectedCredential.addListener((obs, oldVal, newVal) -> hasSelection.set(newVal != null));
    }

    // ===== Properties =====

    /**
     * Gets the list of credentials.
     * 
     * @return the list of all credentials
     */
    public ObservableList<CredentialDTO> getCredentials() {
        return credentials;
    }

    /**
     * Gets the selected credential property.
     * 
     * @return the currently selected credential property
     */
    public ObjectProperty<CredentialDTO> selectedCredentialProperty() {
        return selectedCredential;
    }

    /**
     * Gets the currently selected credential.
     * 
     * @return the currently selected credential
     */
    public CredentialDTO getSelectedCredential() {
        return selectedCredential.get();
    }

    /**
     * Sets the currently selected credential.
     * 
     * @param credential the credential to select
     */
    public void setSelectedCredential(CredentialDTO credential) {
        selectedCredential.set(credential);
    }

    /**
     * Gets the selection state property.
     * 
     * @return the selection state property
     */
    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection;
    }

    /**
     * Checks if a credential is selected.
     * 
     * @return true if a credential is selected
     */
    public boolean hasSelection() {
        return hasSelection.get();
    }

    // ===== Actions =====

    /**
     * Refresh the credential list from the service.
     */
    public void refreshCredentials() {
        setLoading(true);
        try {
            List<CredentialDTO> creds = credentialService.findAll();
            credentials.setAll(creds);
            clearError();
        } catch (Exception e) {
            setErrorMessage("Failed to load credentials: " + e.getMessage());
        } finally {
            setLoading(false);
        }
    }

    /**
     * Add a new credential.
     * 
     * @param dto  the credential metadata
     * @param data the sensitive data (password/key)
     * @return true if successful
     */
    public boolean addCredential(CredentialDTO dto, String data) {
        try {
            CredentialDTO created = credentialService.create(dto, data);
            credentials.add(created);
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to create credential: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update an existing credential.
     * 
     * @param dto  the credential metadata
     * @param data the new sensitive data (null to keep existing)
     * @return true if successful
     */
    public boolean updateCredential(CredentialDTO dto, String data) {
        try {
            CredentialDTO updated = credentialService.update(dto.id(), dto, data);
            int index = findCredentialIndex(dto.id());
            if (index >= 0) {
                credentials.set(index, updated);
            }
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to update credential: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete the currently selected credential.
     * 
     * @return true if successful
     */
    public boolean deleteSelectedCredential() {
        CredentialDTO selected = selectedCredential.get();
        if (selected == null) {
            return false;
        }

        try {
            credentialService.delete(selected.id());
            credentials.remove(selected);
            selectedCredential.set(null);
            markDirty();
            return true;
        } catch (Exception e) {
            setErrorMessage("Failed to delete credential: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a credential name is unique.
     * 
     * @param name      the name to check
     * @param excludeId ID to exclude (for updates)
     * @return true if the name is unique
     */
    public boolean isNameUnique(String name, Long excludeId) {
        return credentials.stream()
                .filter(c -> !c.id().equals(excludeId))
                .noneMatch(c -> c.name().equalsIgnoreCase(name));
    }

    // ===== Private Methods =====

    private int findCredentialIndex(Long id) {
        for (int i = 0; i < credentials.size(); i++) {
            if (credentials.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    // ===== Static Helpers =====

    /**
     * Get display icon literal for a credential type.
     * 
     * @param type the credential type
     * @return the icon literal
     */
    public static String getTypeIconLiteral(CredentialType type) {
        return switch (type) {
            case API_KEY -> "mdi2k-key";
            case HTTP_BASIC -> "mdi2a-account-key";
            case HTTP_BEARER -> "mdi2s-shield-key";
            case OAUTH2 -> "mdi2s-shield-account";
            case CUSTOM_HEADER -> "mdi2c-code-tags";
        };
    }

}
