/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.common.service.CredentialServiceInterface;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import ai.nervemind.ui.viewmodel.canvas.NodePropertiesViewModel;
import ai.nervemind.ui.viewmodel.canvas.WorkflowCanvasViewModel;
import ai.nervemind.ui.viewmodel.dialog.CredentialEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.CredentialManagerViewModel;

/**
 * Integration tests for credential management user flows.
 * 
 * <p>
 * Tests the interaction between CredentialManagerViewModel,
 * CredentialEditViewModel, and how credentials are used in workflow nodes.
 */
@DisplayName("Credential Management Flow Integration Tests")
class CredentialFlowIntegrationTest {

    private CredentialServiceInterface credentialService;
    private CredentialManagerViewModel managerViewModel;
    private WorkflowCanvasViewModel canvasViewModel;
    private NodePropertiesViewModel propertiesViewModel;

    private List<CredentialDTO> mockCredentials;

    @BeforeEach
    void setUp() {
        credentialService = mock(CredentialServiceInterface.class);

        // Setup mock credentials
        mockCredentials = new ArrayList<>();
        mockCredentials.add(new CredentialDTO(1L, "GitHub API", CredentialType.HTTP_BEARER, null, null));
        mockCredentials.add(new CredentialDTO(2L, "Database", CredentialType.HTTP_BASIC, null, null));
        mockCredentials.add(new CredentialDTO(3L, "AWS", CredentialType.API_KEY, null, null));

        when(credentialService.findAll()).thenReturn(mockCredentials);

        managerViewModel = new CredentialManagerViewModel(credentialService);
        canvasViewModel = new WorkflowCanvasViewModel(new CanvasZoomService(), new NodeSelectionService<>());
        propertiesViewModel = new NodePropertiesViewModel();
    }

    // ===== User Flow: Browse and Select Credentials =====

    @Nested
    @DisplayName("Browse Credentials Flow")
    class BrowseCredentialsFlow {

        @Test
        @DisplayName("User opens credential manager and browses credentials")
        void userBrowsesCredentials() {
            // 1. User opens credential manager
            managerViewModel.refreshCredentials();

            // 2. Credentials are loaded
            assertThat(managerViewModel.getCredentials()).hasSize(3);

            // 3. User selects a credential
            CredentialDTO github = managerViewModel.getCredentials().get(0);
            managerViewModel.setSelectedCredential(github);

            // 4. Selection is tracked
            assertThat(managerViewModel.hasSelection()).isTrue();
            assertThat(managerViewModel.getSelectedCredential().name()).isEqualTo("GitHub API");
        }

        @Test
        @DisplayName("User filters credentials by name")
        void userFiltersCredentialsByName() {
            managerViewModel.refreshCredentials();

            // Filter to find credentials containing "API"
            List<CredentialDTO> apiCredentials = managerViewModel.getCredentials().stream()
                    .filter(c -> c.name().toLowerCase().contains("api"))
                    .toList();

            assertThat(apiCredentials)
                    .hasSize(1)
                    .first()
                    .extracting(CredentialDTO::name)
                    .isEqualTo("GitHub API");
        }
    }

    // ===== User Flow: Create New Credential =====

    @Nested
    @DisplayName("Create Credential Flow")
    class CreateCredentialFlow {

        @Test
        @DisplayName("User creates new HTTP Bearer credential")
        void userCreatesHttpBearerCredential() {
            // 1. User opens create dialog (null = create mode)
            var editViewModel = new CredentialEditViewModel(null);

            // 2. User fills in credential details
            editViewModel.setName("New API Token");
            editViewModel.setType(CredentialType.HTTP_BEARER);
            editViewModel.setData("my-secret-token-12345");

            // 3. User confirms the dialog
            assertThat(editViewModel.validate()).isTrue();
            editViewModel.confirm();

            // 4. Result is available
            var result = editViewModel.getResult();
            assertThat(result).isNotNull();
            assertThat(result.dto().name()).isEqualTo("New API Token");
            assertThat(result.dto().type()).isEqualTo(CredentialType.HTTP_BEARER);
            assertThat(result.data()).isEqualTo("my-secret-token-12345");
        }

        @Test
        @DisplayName("User sees validation error for empty name")
        void userSeesValidationErrorForEmptyName() {
            var editViewModel = new CredentialEditViewModel(null);

            // User tries to confirm without entering name
            editViewModel.setType(CredentialType.API_KEY);
            editViewModel.setData("some-key");

            assertThat(editViewModel.validate()).isFalse();
        }

        @Test
        @DisplayName("User creates different credential types")
        void userCreatesDifferentCredentialTypes() {
            // Basic Auth
            var basicAuth = new CredentialEditViewModel(null);
            basicAuth.setName("DB Connection");
            basicAuth.setType(CredentialType.HTTP_BASIC);
            basicAuth.setData("user:password");
            assertThat(basicAuth.validate()).isTrue();

            // API Key
            var apiKey = new CredentialEditViewModel(null);
            apiKey.setName("Weather API");
            apiKey.setType(CredentialType.API_KEY);
            apiKey.setData("abc123xyz");
            assertThat(apiKey.validate()).isTrue();
        }
    }

    // ===== User Flow: Edit Existing Credential =====

    @Nested
    @DisplayName("Edit Credential Flow")
    class EditCredentialFlow {

        @Test
        @DisplayName("User edits existing credential")
        void userEditsExistingCredential() {
            // 1. User selects credential to edit
            managerViewModel.refreshCredentials();
            CredentialDTO existing = managerViewModel.getCredentials().get(0);

            // 2. User opens edit dialog
            var editViewModel = new CredentialEditViewModel(existing);

            // 3. Fields are pre-populated
            assertThat(editViewModel.isEditMode()).isTrue();
            assertThat(editViewModel.getName()).isEqualTo("GitHub API");
            assertThat(editViewModel.getType()).isEqualTo(CredentialType.HTTP_BEARER);

            // 4. User modifies the name
            editViewModel.setName("GitHub API v2");

            // 5. User confirms
            assertThat(editViewModel.validate()).isTrue();
            editViewModel.confirm();

            // 6. Result contains updated credential
            var result = editViewModel.getResult();
            assertThat(result.dto().name()).isEqualTo("GitHub API v2");
            assertThat(result.dto().id()).isEqualTo(1L); // Preserves original ID
        }
    }

    // ===== User Flow: Credential with Workflow Node =====

    @Nested
    @DisplayName("Credential with Node Flow")
    class CredentialWithNodeFlow {

        @Test
        @DisplayName("User assigns credential to HTTP node")
        void userAssignsCredentialToHttpNode() {
            // 1. Load credentials
            managerViewModel.refreshCredentials();

            // 2. Add HTTP node to canvas
            Node httpNode = new Node(
                    "http-1",
                    "httpRequest",
                    "GitHub API Call",
                    new Node.Position(100, 100),
                    Map.of("url", "https://api.github.com/user"),
                    null, // No credential yet
                    false,
                    null);
            canvasViewModel.addNode(httpNode);

            // 3. User selects the node
            canvasViewModel.selectNode("http-1", false);
            Node selected = canvasViewModel.getNode("http-1");
            propertiesViewModel.show(selected);

            // 4. User selects credential from list
            CredentialDTO githubCredential = managerViewModel.getCredentials().get(0);

            // 5. User assigns credential to node (simulated via properties update)
            propertiesViewModel.setParameter("credentialId", githubCredential.id());
            assertThat(propertiesViewModel.isDirty()).isTrue();

            // 6. Properties show the credential is assigned
            assertThat(propertiesViewModel.getParameter("credentialId")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Complete flow: Create credential and use in new workflow")
        void completeCredentialWorkflowFlow() {
            // === CREATE CREDENTIAL ===
            var editViewModel = new CredentialEditViewModel(null);
            editViewModel.setName("OpenAI API");
            editViewModel.setType(CredentialType.HTTP_BEARER);
            editViewModel.setData("sk-test-key");
            editViewModel.confirm();

            var newCredential = editViewModel.getResult();
            assertThat(newCredential).isNotNull();

            // Simulate saving and refreshing (in real app, service would persist)
            CredentialDTO savedCredential = new CredentialDTO(
                    4L,
                    newCredential.dto().name(),
                    newCredential.dto().type(),
                    null, null);
            mockCredentials.add(savedCredential);
            when(credentialService.findAll()).thenReturn(mockCredentials);

            // === CREATE WORKFLOW WITH NODE ===
            canvasViewModel.newWorkflow();
            canvasViewModel.setWorkflowName("AI Chat Bot");

            // Add AI node
            Node aiNode = new Node(
                    "ai-1",
                    "openai",
                    "Chat Completion",
                    new Node.Position(100, 100),
                    Map.of(
                            "model", "gpt-4",
                            "credentialId", savedCredential.id()),
                    savedCredential.id(),
                    false,
                    null);
            canvasViewModel.addNode(aiNode);

            // === VERIFY ===
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(1);
            Node loaded = canvasViewModel.getNode("ai-1");
            assertThat(loaded.credentialId()).isEqualTo(4L);
        }
    }

    // ===== Error Handling =====

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Handles service error gracefully")
        void handlesServiceErrorGracefully() {
            when(credentialService.findAll()).thenThrow(new RuntimeException("Connection failed"));

            managerViewModel.refreshCredentials();

            // Should have error message set
            assertThat(managerViewModel.hasError()).isTrue();
            assertThat(managerViewModel.getErrorMessage()).contains("Connection failed");
        }
    }
}
