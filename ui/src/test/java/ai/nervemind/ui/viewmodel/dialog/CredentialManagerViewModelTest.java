/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.common.service.CredentialServiceInterface;

/**
 * Unit tests for {@link CredentialManagerViewModel}.
 * Tests all ViewModel logic without requiring JavaFX runtime.
 */
@ExtendWith(MockitoExtension.class)
class CredentialManagerViewModelTest {

    @Mock
    private CredentialServiceInterface credentialService;

    private CredentialManagerViewModel viewModel;

    // Test data
    private CredentialDTO apiKeyCredential;
    private CredentialDTO basicAuthCredential;

    @BeforeEach
    void setUp() {
        viewModel = new CredentialManagerViewModel(credentialService);

        apiKeyCredential = new CredentialDTO(
                1L,
                "OpenAI API Key",
                CredentialType.API_KEY,
                Instant.now(),
                null);

        basicAuthCredential = new CredentialDTO(
                2L,
                "GitHub Token",
                CredentialType.HTTP_BEARER,
                Instant.now(),
                null);
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with empty list")
        void shouldInitializeWithEmptyList() {
            assertTrue(viewModel.getCredentials().isEmpty());
            assertNull(viewModel.getSelectedCredential());
            assertFalse(viewModel.hasSelection());
        }
    }

    @Nested
    @DisplayName("Refresh Tests")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh credentials")
        void shouldRefreshCredentials() {
            when(credentialService.findAll()).thenReturn(List.of(apiKeyCredential, basicAuthCredential));

            viewModel.refreshCredentials();

            assertEquals(2, viewModel.getCredentials().size());
            verify(credentialService).findAll();
        }

        @Test
        @DisplayName("Should handle service exception on refresh")
        void shouldHandleServiceExceptionOnRefresh() {
            when(credentialService.findAll())
                    .thenThrow(new RuntimeException("Database error"));

            viewModel.refreshCredentials();

            assertTrue(viewModel.getCredentials().isEmpty());
            assertTrue(viewModel.getErrorMessage().contains("Failed to load credentials"));
        }

        @Test
        @DisplayName("Should clear credentials on refresh")
        void shouldClearCredentialsOnRefresh() {
            // First load
            when(credentialService.findAll()).thenReturn(List.of(apiKeyCredential));
            viewModel.refreshCredentials();
            assertEquals(1, viewModel.getCredentials().size());

            // Second load with empty list
            when(credentialService.findAll()).thenReturn(List.of());
            viewModel.refreshCredentials();
            assertTrue(viewModel.getCredentials().isEmpty());
        }
    }

    @Nested
    @DisplayName("Selection Tests")
    class SelectionTests {

        @BeforeEach
        void loadCredentials() {
            when(credentialService.findAll()).thenReturn(List.of(apiKeyCredential));
            viewModel.refreshCredentials();
        }

        @Test
        @DisplayName("Should track selection state")
        void shouldTrackSelectionState() {
            assertFalse(viewModel.hasSelection());

            viewModel.setSelectedCredential(apiKeyCredential);

            assertTrue(viewModel.hasSelection());
            assertEquals(apiKeyCredential, viewModel.getSelectedCredential());
        }

        @Test
        @DisplayName("Should clear selection when set to null")
        void shouldClearSelection() {
            viewModel.setSelectedCredential(apiKeyCredential);
            viewModel.setSelectedCredential(null);

            assertFalse(viewModel.hasSelection());
            assertNull(viewModel.getSelectedCredential());
        }
    }

    @Nested
    @DisplayName("CRUD Operation Tests")
    class CrudOperationTests {

        @BeforeEach
        void loadCredentials() {
            when(credentialService.findAll()).thenReturn(List.of(apiKeyCredential));
            viewModel.refreshCredentials();
        }

        @Test
        @DisplayName("Should add new credential")
        void shouldAddNewCredential() {
            CredentialDTO newDto = new CredentialDTO(
                    null, "New Credential", CredentialType.API_KEY, null, null);
            CredentialDTO createdDto = new CredentialDTO(
                    10L, "New Credential", CredentialType.API_KEY, Instant.now(), null);

            when(credentialService.create(newDto, "secret-key")).thenReturn(createdDto);

            boolean result = viewModel.addCredential(newDto, "secret-key");

            assertTrue(result);
            assertEquals(2, viewModel.getCredentials().size());
            assertTrue(viewModel.isDirty());
            verify(credentialService).create(newDto, "secret-key");
        }

        @Test
        @DisplayName("Should handle add credential failure")
        void shouldHandleAddCredentialFailure() {
            CredentialDTO newDto = new CredentialDTO(
                    null, "New Credential", CredentialType.API_KEY, null, null);

            when(credentialService.create(newDto, "secret-key"))
                    .thenThrow(new RuntimeException("Duplicate name"));

            boolean result = viewModel.addCredential(newDto, "secret-key");

            assertFalse(result);
            assertEquals(1, viewModel.getCredentials().size());
            assertTrue(viewModel.getErrorMessage().contains("Failed to create credential"));
        }

        @Test
        @DisplayName("Should update existing credential")
        void shouldUpdateExistingCredential() {
            CredentialDTO updatedDto = new CredentialDTO(
                    1L, "Updated Key", CredentialType.API_KEY, apiKeyCredential.createdAt(), Instant.now());

            when(credentialService.update(1L, updatedDto, "new-secret")).thenReturn(updatedDto);

            boolean result = viewModel.updateCredential(updatedDto, "new-secret");

            assertTrue(result);
            assertEquals("Updated Key", viewModel.getCredentials().get(0).name());
            assertTrue(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should update credential without changing secret")
        void shouldUpdateCredentialWithoutChangingSecret() {
            CredentialDTO updatedDto = new CredentialDTO(
                    1L, "Renamed Key", CredentialType.API_KEY, apiKeyCredential.createdAt(), Instant.now());

            when(credentialService.update(1L, updatedDto, null)).thenReturn(updatedDto);

            boolean result = viewModel.updateCredential(updatedDto, null);

            assertTrue(result);
            verify(credentialService).update(1L, updatedDto, null);
        }

        @Test
        @DisplayName("Should delete selected credential")
        void shouldDeleteSelectedCredential() {
            viewModel.setSelectedCredential(apiKeyCredential);

            boolean result = viewModel.deleteSelectedCredential();

            assertTrue(result);
            assertTrue(viewModel.getCredentials().isEmpty());
            assertNull(viewModel.getSelectedCredential());
            assertFalse(viewModel.hasSelection());
            assertTrue(viewModel.isDirty());
            verify(credentialService).delete(1L);
        }

        @Test
        @DisplayName("Should not delete when no selection")
        void shouldNotDeleteWhenNoSelection() {
            boolean result = viewModel.deleteSelectedCredential();

            assertFalse(result);
            assertEquals(1, viewModel.getCredentials().size());
            verify(credentialService, never()).delete(anyLong());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @BeforeEach
        void loadCredentials() {
            when(credentialService.findAll())
                    .thenReturn(Arrays.asList(apiKeyCredential, basicAuthCredential));
            viewModel.refreshCredentials();
        }

        @Test
        @DisplayName("Should detect duplicate name")
        void shouldDetectDuplicateName() {
            assertFalse(viewModel.isNameUnique("OpenAI API Key", null));
            assertFalse(viewModel.isNameUnique("openai api key", null)); // Case-insensitive
        }

        @Test
        @DisplayName("Should allow unique name")
        void shouldAllowUniqueName() {
            assertTrue(viewModel.isNameUnique("New Unique Name", null));
        }

        @Test
        @DisplayName("Should exclude current credential when checking duplicates")
        void shouldExcludeCurrentCredentialWhenCheckingDuplicates() {
            assertTrue(viewModel.isNameUnique("OpenAI API Key", 1L));
            assertFalse(viewModel.isNameUnique("GitHub Token", 1L));
        }
    }

    @Nested
    @DisplayName("Static Helper Tests")
    class StaticHelperTests {

        @Test
        @DisplayName("Should return icon literals for credential types")
        void shouldReturnIconLiteralsForCredentialTypes() {
            assertNotNull(CredentialManagerViewModel.getTypeIconLiteral(CredentialType.API_KEY));
            assertNotNull(CredentialManagerViewModel.getTypeIconLiteral(CredentialType.HTTP_BASIC));
            assertNotNull(CredentialManagerViewModel.getTypeIconLiteral(CredentialType.HTTP_BEARER));
            assertNotNull(CredentialManagerViewModel.getTypeIconLiteral(CredentialType.OAUTH2));
            assertNotNull(CredentialManagerViewModel.getTypeIconLiteral(CredentialType.CUSTOM_HEADER));
        }
    }
}
