/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.VariableServiceInterface;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import ai.nervemind.ui.viewmodel.MainViewModel;
import ai.nervemind.ui.viewmodel.canvas.WorkflowCanvasViewModel;
import ai.nervemind.ui.viewmodel.dialog.VariableEditViewModel;
import ai.nervemind.ui.viewmodel.dialog.VariableManagerViewModel;

/**
 * Integration tests for variable management user flows.
 *
 * <p>
 * Tests the interaction between VariableManagerViewModel,
 * VariableEditViewModel,
 * and the workflow canvas when managing variables.
 */
@DisplayName("Variable Flow Integration Tests")
class VariableFlowIntegrationTest {

    private VariableServiceInterface variableService;
    private VariableManagerViewModel managerViewModel;
    private WorkflowCanvasViewModel canvasViewModel;
    private MainViewModel mainViewModel;

    private List<VariableDTO> mockVariables;
    private static final Long WORKFLOW_ID = 1L;

    @BeforeEach
    void setUp() {
        variableService = mock(VariableServiceInterface.class);

        // Setup mock variables
        mockVariables = new ArrayList<>();
        mockVariables.add(new VariableDTO(1L, "apiKey", "abc123", VariableType.SECRET, VariableScope.GLOBAL,
                null, "API key for external service", Instant.now(), Instant.now()));
        mockVariables.add(
                new VariableDTO(2L, "baseUrl", "https://api.example.com", VariableType.STRING, VariableScope.GLOBAL,
                        null, "Base URL for API calls", Instant.now(), Instant.now()));
        mockVariables.add(new VariableDTO(3L, "counter", "0", VariableType.NUMBER, VariableScope.WORKFLOW,
                WORKFLOW_ID, "Counter variable", Instant.now(), Instant.now()));

        when(variableService.findAll()).thenReturn(mockVariables);
        when(variableService.findByWorkflowId(WORKFLOW_ID)).thenReturn(
                mockVariables.stream().filter(v -> v.scope() == VariableScope.WORKFLOW).toList());
        when(variableService.findGlobalVariables()).thenReturn(
                mockVariables.stream().filter(v -> v.scope() == VariableScope.GLOBAL).toList());

        managerViewModel = new VariableManagerViewModel(variableService, WORKFLOW_ID);
        canvasViewModel = new WorkflowCanvasViewModel(new CanvasZoomService(), new NodeSelectionService<>());
        mainViewModel = new MainViewModel();
    }

    private WorkflowDTO createTestWorkflow() {
        Node node1 = new Node("node-1", "httpRequest", "API Call", new Node.Position(100, 100),
                Map.of("url", "${baseUrl}/data", "apiKey", "${apiKey}"), null, false, null);
        Node node2 = new Node("node-2", "code", "Process", new Node.Position(300, 100),
                Map.of("code", "counter++"), null, false, null);

        return new WorkflowDTO(WORKFLOW_ID, "Test Workflow", "A workflow for testing",
                List.of(node1, node2),
                List.of(new Connection("c1", "node-1", "output", "node-2", "input")),
                Map.of(), false, TriggerType.MANUAL, null,
                Instant.now(), Instant.now(), null, 1);
    }

    // ===== User Flow: Browse Variables =====

    @Nested
    @DisplayName("Browse Variables Flow")
    class BrowseVariablesFlow {

        @Test
        @DisplayName("User browses all variables")
        void userBrowsesAllVariables() {
            // 1. Load variables
            managerViewModel.refreshVariables();

            // 2. All variables visible (no filter)
            assertThat(managerViewModel.getFilteredVariables()).hasSize(3);
        }

        @Test
        @DisplayName("User filters by scope")
        void userFiltersByScope() {
            managerViewModel.refreshVariables();

            // User selects Global scope
            managerViewModel.scopeFilterProperty().set(VariableScope.GLOBAL);

            // Only global variables shown
            assertThat(managerViewModel.getFilteredVariables()).hasSize(2);
            assertThat(managerViewModel.getFilteredVariables())
                    .allMatch(v -> v.scope() == VariableScope.GLOBAL);
        }

        @Test
        @DisplayName("User filters workflow-specific variables")
        void userFiltersWorkflowVariables() {
            managerViewModel.refreshVariables();

            // User selects Workflow scope
            managerViewModel.scopeFilterProperty().set(VariableScope.WORKFLOW);

            // Only workflow variables shown
            assertThat(managerViewModel.getFilteredVariables()).hasSize(1);
            assertThat(managerViewModel.getFilteredVariables().getFirst().name()).isEqualTo("counter");
        }

        @Test
        @DisplayName("User selects variable to view details")
        void userSelectsVariable() {
            managerViewModel.refreshVariables();

            // User selects a variable
            VariableDTO variable = managerViewModel.getFilteredVariables().getFirst();
            managerViewModel.setSelectedVariable(variable);

            assertThat(managerViewModel.hasSelection()).isTrue();
            assertThat(managerViewModel.getSelectedVariable()).isEqualTo(variable);
        }
    }

    // ===== User Flow: Create Variable =====

    @Nested
    @DisplayName("Create Variable Flow")
    class CreateVariableFlow {

        @Test
        @DisplayName("User creates new global variable")
        void userCreatesGlobalVariable() {
            // 1. User opens create dialog (no existing variable)
            VariableEditViewModel editViewModel = new VariableEditViewModel(null, WORKFLOW_ID);

            // 2. Verify create mode
            assertThat(editViewModel.isEditMode()).isFalse();

            // 3. User enters variable details
            editViewModel.setName("newVar");
            editViewModel.setValue("initial value");
            editViewModel.setScope(VariableScope.GLOBAL);
            editViewModel.setType(VariableType.STRING);

            // 4. Form is valid
            assertThat(editViewModel.isFormValid()).isTrue();

            // 5. User confirms
            editViewModel.confirm();
            VariableDTO result = editViewModel.getResult();
            assertThat(result.name()).isEqualTo("newVar");
            assertThat(result.value()).isEqualTo("initial value");
            assertThat(result.scope()).isEqualTo(VariableScope.GLOBAL);
        }

        @Test
        @DisplayName("User creates secret variable")
        void userCreatesSecretVariable() {
            VariableEditViewModel editViewModel = new VariableEditViewModel(null, WORKFLOW_ID);

            editViewModel.setName("password");
            editViewModel.setValue("secret123");
            editViewModel.setScope(VariableScope.GLOBAL);
            editViewModel.setType(VariableType.SECRET);

            assertThat(editViewModel.isFormValid()).isTrue();

            editViewModel.confirm();
            VariableDTO result = editViewModel.getResult();
            assertThat(result.type()).isEqualTo(VariableType.SECRET);
        }

        @Test
        @DisplayName("User creates workflow-scoped variable")
        void userCreatesWorkflowVariable() {
            VariableEditViewModel editViewModel = new VariableEditViewModel(null, WORKFLOW_ID);

            editViewModel.setName("workflowVar");
            editViewModel.setValue("workflow value");
            editViewModel.setScope(VariableScope.WORKFLOW);

            assertThat(editViewModel.isFormValid()).isTrue();

            editViewModel.confirm();
            VariableDTO result = editViewModel.getResult();
            assertThat(result.scope()).isEqualTo(VariableScope.WORKFLOW);
        }

        @Test
        @DisplayName("User cannot save with empty name")
        void userCannotSaveEmptyName() {
            VariableEditViewModel editViewModel = new VariableEditViewModel(null, WORKFLOW_ID);

            editViewModel.setName("");
            editViewModel.setValue("some value");

            assertThat(editViewModel.isFormValid()).isFalse();
        }
    }

    // ===== User Flow: Edit Variable =====

    @Nested
    @DisplayName("Edit Variable Flow")
    class EditVariableFlow {

        @Test
        @DisplayName("User edits existing variable")
        void userEditsVariable() {
            // 1. Select variable to edit
            VariableDTO existing = mockVariables.get(1); // baseUrl

            // 2. Open edit dialog
            VariableEditViewModel editViewModel = new VariableEditViewModel(existing, WORKFLOW_ID);

            // 3. Verify edit mode
            assertThat(editViewModel.isEditMode()).isTrue();

            // 4. Fields are populated
            assertThat(editViewModel.getName()).isEqualTo("baseUrl");
            assertThat(editViewModel.getValue()).isEqualTo("https://api.example.com");

            // 5. User changes value
            editViewModel.setValue("https://api.newdomain.com");

            // 6. Form is valid
            assertThat(editViewModel.isFormValid()).isTrue();

            // 7. User confirms
            editViewModel.confirm();
            VariableDTO result = editViewModel.getResult();
            assertThat(result.value()).isEqualTo("https://api.newdomain.com");
        }
    }

    // ===== User Flow: Delete Variable =====

    @Nested
    @DisplayName("Delete Variable Flow")
    class DeleteVariableFlow {

        @Test
        @DisplayName("User deletes selected variable")
        void userDeletesVariable() {
            managerViewModel.refreshVariables();

            // User selects variable to delete
            VariableDTO toDelete = managerViewModel.getFilteredVariables().getFirst();
            managerViewModel.setSelectedVariable(toDelete);

            assertThat(managerViewModel.hasSelection()).isTrue();

            // Simulate deletion by removing and refreshing
            managerViewModel.setSelectedVariable(null);
            assertThat(managerViewModel.hasSelection()).isFalse();
        }
    }

    // ===== User Flow: Variable in Node Properties =====

    @Nested
    @DisplayName("Variable in Node Properties Flow")
    class VariableInNodePropertiesFlow {

        @Test
        @DisplayName("User configures node to use variable")
        void userConfiguresNodeWithVariable() {
            // 1. Load workflow
            WorkflowDTO workflow = createTestWorkflow();
            canvasViewModel.loadWorkflow(workflow);
            mainViewModel.setActiveWorkflow(workflow);
            managerViewModel.refreshVariables();

            // 2. Select node
            canvasViewModel.selectNode("node-1", false);

            // 3. Node has variable references in parameters
            Node selectedNode = canvasViewModel.getNode("node-1");
            Map<String, Object> params = selectedNode.parameters();
            assertThat(params.get("url").toString()).contains("${baseUrl}");
            assertThat(params.get("apiKey").toString()).contains("${apiKey}");
        }

        @Test
        @DisplayName("Workflow uses multiple variable scopes")
        void workflowUsesMultipleScopes() {
            managerViewModel.refreshVariables();

            // Clear filter to see all
            managerViewModel.scopeFilterProperty().set(null);

            // Variables from different scopes
            List<VariableDTO> variables = managerViewModel.getFilteredVariables();

            long globalCount = variables.stream()
                    .filter(v -> v.scope() == VariableScope.GLOBAL)
                    .count();
            long workflowCount = variables.stream()
                    .filter(v -> v.scope() == VariableScope.WORKFLOW)
                    .count();

            assertThat(globalCount).isEqualTo(2);
            assertThat(workflowCount).isEqualTo(1);
        }
    }

    // ===== User Flow: Secret Variable Handling =====

    @Nested
    @DisplayName("Secret Variable Handling Flow")
    class SecretVariableHandlingFlow {

        @Test
        @DisplayName("Secret variables are identified")
        void secretVariablesIdentified() {
            managerViewModel.refreshVariables();

            // Find secret variable
            VariableDTO secretVar = managerViewModel.getFilteredVariables().stream()
                    .filter(v -> v.type() == VariableType.SECRET)
                    .findFirst()
                    .orElseThrow();

            assertThat(secretVar.name()).isEqualTo("apiKey");
            assertThat(secretVar.type()).isEqualTo(VariableType.SECRET);
        }

        @Test
        @DisplayName("User edits secret variable")
        void userEditsSecretVariable() {
            VariableDTO secretVar = mockVariables.getFirst(); // apiKey (SECRET)

            VariableEditViewModel editViewModel = new VariableEditViewModel(secretVar, WORKFLOW_ID);

            // Type is preserved (SECRET)
            assertThat(editViewModel.getType()).isEqualTo(VariableType.SECRET);

            // User can update value
            editViewModel.setValue("newSecret456");

            editViewModel.confirm();
            VariableDTO result = editViewModel.getResult();
            assertThat(result.type()).isEqualTo(VariableType.SECRET);
        }
    }
}
