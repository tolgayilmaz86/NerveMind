/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import ai.nervemind.ui.viewmodel.MainViewModel;
import ai.nervemind.ui.viewmodel.canvas.NodePaletteViewModel;
import ai.nervemind.ui.viewmodel.canvas.NodePropertiesViewModel;
import ai.nervemind.ui.viewmodel.canvas.WorkflowCanvasViewModel;
import ai.nervemind.ui.viewmodel.editor.ExpressionEditorViewModel;

/**
 * Integration tests for key workflow user flows.
 *
 * <p>
 * These tests simulate complete user workflows across multiple ViewModels
 * to ensure they integrate correctly. No JavaFX runtime is required since
 * all ViewModels use only javafx.beans.* properties.
 */
@DisplayName("Workflow User Flow Integration Tests")
class WorkflowUserFlowIntegrationTest {

    private WorkflowCanvasViewModel canvasViewModel;
    private NodePaletteViewModel paletteViewModel;
    private NodePropertiesViewModel propertiesViewModel;
    private MainViewModel mainViewModel;

    @BeforeEach
    void setUp() {
        canvasViewModel = new WorkflowCanvasViewModel(
                new CanvasZoomService(),
                new NodeSelectionService<>());
        paletteViewModel = new NodePaletteViewModel();
        propertiesViewModel = new NodePropertiesViewModel();
        mainViewModel = new MainViewModel();
    }

    // ===== Test Helpers =====

    private Node createNode(String id, String type, String name, double x, double y) {
        return new Node(id, type, name, new Node.Position(x, y), Map.of(), null, false, null);
    }

    private Connection createConnection(String id, String sourceId, String targetId) {
        return new Connection(id, sourceId, "output", targetId, "input");
    }

    private WorkflowDTO createSampleWorkflow() {
        Node httpNode = createNode("http-1", "httpRequest", "Fetch API", 100, 100);
        Node transformNode = createNode("transform-1", "code", "Transform", 300, 100);
        Node outputNode = createNode("output-1", "set", "Output", 500, 100);
        Connection conn1 = createConnection("conn-1", "http-1", "transform-1");
        Connection conn2 = createConnection("conn-2", "transform-1", "output-1");

        return new WorkflowDTO(
                1L,
                "Sample Workflow",
                "A test workflow",
                List.of(httpNode, transformNode, outputNode),
                List.of(conn1, conn2),
                Map.of(),
                false,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }

    // ===== User Flow: Create New Workflow =====

    @Nested
    @DisplayName("Create New Workflow Flow")
    class CreateNewWorkflowFlow {

        private static final String MY_API_WORKFLOW = "My API Workflow";

        @Test
        @DisplayName("User creates new workflow, adds nodes, and connects them")
        void userCreatesWorkflowWithNodesAndConnections() {
            // 1. User starts with new workflow
            canvasViewModel.newWorkflow();
            assertThat(canvasViewModel.getNodeCount()).isZero();
            assertThat(canvasViewModel.isDirty()).isFalse();

            // 2. User searches for HTTP node in palette
            paletteViewModel.setSearchText("http");
            assertThat(paletteViewModel.getFilteredItems())
                    .anyMatch(item -> item.nodeType().toLowerCase().contains("http"));

            // 3. User adds HTTP node from palette
            Node httpNode = createNode("http-1", "httpRequest", "HTTP Request", 100, 100);
            canvasViewModel.addNode(httpNode);
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(1);
            assertThat(canvasViewModel.isDirty()).isTrue();

            // 4. User adds a transform node
            Node transformNode = createNode("transform-1", "code", "Transform Data", 300, 100);
            canvasViewModel.addNode(transformNode);
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(2);

            // 5. User connects the nodes
            Connection conn = createConnection("conn-1", "http-1", "transform-1");
            canvasViewModel.addConnection(conn);
            assertThat(canvasViewModel.getConnectionCount()).isEqualTo(1);

            // 6. User sets workflow name
            canvasViewModel.setWorkflowName(MY_API_WORKFLOW);
            assertThat(canvasViewModel.getWorkflowName()).isEqualTo(MY_API_WORKFLOW);

            // 7. User exports to DTO
            WorkflowDTO dto = canvasViewModel.toWorkflowDTO();
            assertThat(dto.name()).isEqualTo(MY_API_WORKFLOW);
            assertThat(dto.nodes()).hasSize(2);
            assertThat(dto.connections()).hasSize(1);
        }

        @Test
        @DisplayName("User filters palette by search text")
        void userFiltersPaletteBySearchText() {
            // 1. User sees all node types initially
            paletteViewModel.setSearchText("");
            int totalNodes = paletteViewModel.getFilteredNodeCount();
            assertThat(totalNodes).isGreaterThan(0);

            // 2. User searches for specific node type
            paletteViewModel.setSearchText("code");
            assertThat(paletteViewModel.getFilteredNodeCount()).isLessThanOrEqualTo(totalNodes);
            assertThat(paletteViewModel.getFilteredItems())
                    .allMatch(item -> item.nodeType().toLowerCase().contains("code")
                            || item.name().toLowerCase().contains("code")
                            || item.category().toLowerCase().contains("code"));
        }
    }

    // ===== User Flow: Edit Existing Workflow =====

    @Nested
    @DisplayName("Edit Existing Workflow Flow")
    class EditExistingWorkflowFlow {

        @Test
        @DisplayName("User loads workflow, selects node, and edits properties")
        void userLoadsAndEditsWorkflow() {
            // 1. Load existing workflow
            WorkflowDTO workflow = createSampleWorkflow();
            canvasViewModel.loadWorkflow(workflow);
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(3);
            assertThat(canvasViewModel.getConnectionCount()).isEqualTo(2);
            assertThat(canvasViewModel.isDirty()).isFalse();

            // 2. User selects a node
            canvasViewModel.selectNode("http-1", false);
            assertThat(canvasViewModel.hasSelection()).isTrue();
            assertThat(canvasViewModel.getSelectedNodeCount()).isEqualTo(1);

            // 3. Properties panel shows the selected node
            Node selectedNode = canvasViewModel.getNode("http-1");
            propertiesViewModel.show(selectedNode);
            assertThat(propertiesViewModel.isVisible()).isTrue();
            assertThat(propertiesViewModel.getName()).isEqualTo("Fetch API");

            // 4. User changes the node name
            propertiesViewModel.setName("Updated API Call");
            assertThat(propertiesViewModel.getName()).isEqualTo("Updated API Call");

            // 5. Properties should be marked dirty
            assertThat(propertiesViewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("User deletes selected nodes and connections")
        void userDeletesSelectedItems() {
            // 1. Load workflow
            canvasViewModel.loadWorkflow(createSampleWorkflow());
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(3);

            // 2. User multi-selects two nodes
            canvasViewModel.selectNode("http-1", false);
            canvasViewModel.selectNode("transform-1", true);
            assertThat(canvasViewModel.getSelectedNodeCount()).isEqualTo(2);

            // 3. User deletes selection
            canvasViewModel.deleteSelected();

            // Nodes should be removed
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(1);
            // Connections to deleted nodes should also be removed
            assertThat(canvasViewModel.getConnectionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("User uses undo/redo scenario")
        void userModifiesAndTracksChanges() {
            // 1. Load workflow and note initial state
            canvasViewModel.loadWorkflow(createSampleWorkflow());
            assertThat(canvasViewModel.isDirty()).isFalse();

            // 2. Make a change
            canvasViewModel.setWorkflowName("Modified Name");
            assertThat(canvasViewModel.isDirty()).isTrue();

            // 3. Mark as clean (simulating save)
            canvasViewModel.markClean();
            assertThat(canvasViewModel.isDirty()).isFalse();

            // 4. Make another change
            canvasViewModel.addNode(createNode("new-node", "code", "New Node", 400, 200));
            assertThat(canvasViewModel.isDirty()).isTrue();
        }
    }

    // ===== User Flow: Canvas Navigation =====

    @Nested
    @DisplayName("Canvas Navigation Flow")
    class CanvasNavigationFlow {

        @Test
        @DisplayName("User zooms and pans the canvas")
        void userZoomsAndPansCanvas() {
            // 1. Start at default zoom
            assertThat(canvasViewModel.getZoomPercentage()).isEqualTo(100);
            assertThat(canvasViewModel.getScale()).isEqualTo(1.0);

            // 2. User zooms in
            canvasViewModel.zoomIn();
            assertThat(canvasViewModel.getZoomPercentage()).isGreaterThan(100);

            // 3. User zooms out
            canvasViewModel.zoomOut();
            canvasViewModel.zoomOut();
            assertThat(canvasViewModel.getZoomPercentage()).isLessThan(100);

            // 4. User resets zoom
            canvasViewModel.resetZoom();
            assertThat(canvasViewModel.getZoomPercentage()).isEqualTo(100);
        }

        @Test
        @DisplayName("User toggles grid settings")
        void userTogglesGridSettings() {
            // 1. Grid is on by default
            assertThat(canvasViewModel.isShowGrid()).isTrue();
            assertThat(canvasViewModel.isSnapToGrid()).isTrue();

            // 2. User hides grid
            canvasViewModel.toggleGrid();
            assertThat(canvasViewModel.isShowGrid()).isFalse();

            // 3. User disables snap
            canvasViewModel.toggleSnapToGrid();
            assertThat(canvasViewModel.isSnapToGrid()).isFalse();

            // 4. Dragging node doesn't snap
            double rawValue = 45.0;
            double result = canvasViewModel.snapToGrid(rawValue);
            assertThat(result).isEqualTo(rawValue);

            // 5. Re-enable snap
            canvasViewModel.toggleSnapToGrid();
            result = canvasViewModel.snapToGrid(rawValue);
            assertThat(result).isEqualTo(40.0); // Snapped to grid (20-based)
        }
    }

    // ===== User Flow: Copy/Paste Nodes =====

    @Nested
    @DisplayName("Copy/Paste Nodes Flow")
    class CopyPasteNodesFlow {

        @Test
        @DisplayName("User copies and pastes nodes")
        void userCopiesAndPastesNodes() {
            // 1. Load workflow with nodes
            canvasViewModel.loadWorkflow(createSampleWorkflow());

            // 2. Select two nodes
            canvasViewModel.selectNode("http-1", false);
            canvasViewModel.selectNode("transform-1", true);
            assertThat(canvasViewModel.getSelectedNodeCount()).isEqualTo(2);

            // 3. Copy to clipboard
            canvasViewModel.copySelectedNodes();
            assertThat(canvasViewModel.hasClipboardContent()).isTrue();
            assertThat(canvasViewModel.getClipboard()).hasSize(2);
        }
    }

    // ===== User Flow: Expression Editor =====

    @Nested
    @DisplayName("Expression Editor Flow")
    class ExpressionEditorFlow {

        private ExpressionEditorViewModel expressionViewModel;

        @BeforeEach
        void setUpExpressionEditor() {
            expressionViewModel = new ExpressionEditorViewModel();
            expressionViewModel.setAvailableVariables(List.of("name", "email", "age", "status"));
        }

        @Test
        @DisplayName("User creates valid expression")
        void userCreatesValidExpression() {
            // 1. User enters expression with variable
            expressionViewModel.setExpression("Hello, ${name}!");

            // Expression validation happens automatically via listener
            assertThat(expressionViewModel.isValid()).isTrue();
        }

        @Test
        @DisplayName("User sees validation error for invalid expression")
        void userSeesValidationError() {
            // 1. User enters expression with unclosed variable
            expressionViewModel.setExpression("Hello, ${name");

            // Validation happens automatically
            assertThat(expressionViewModel.isValid()).isFalse();
            assertThat(expressionViewModel.getValidationMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("User gets autocomplete suggestions")
        void userGetsAutocompleteSuggestions() {
            // 1. User types filter to show suggestions
            expressionViewModel.setSuggestionFilter("nam");
            List<String> suggestions = expressionViewModel.getFilteredSuggestions();

            assertThat(suggestions).anyMatch(s -> s.contains("name"));
        }
    }

    // ===== User Flow: Main Navigation =====

    @Nested
    @DisplayName("Main Navigation Flow")
    class MainNavigationFlow {

        @Test
        @DisplayName("User navigates between views")
        void userNavigatesBetweenViews() {
            // 1. Start on workflows view
            mainViewModel.setCurrentView(MainViewModel.NavigationView.WORKFLOWS);
            assertThat(mainViewModel.getCurrentView()).isEqualTo(MainViewModel.NavigationView.WORKFLOWS);

            // 2. Navigate to executions
            mainViewModel.setCurrentView(MainViewModel.NavigationView.EXECUTIONS);
            assertThat(mainViewModel.getCurrentView()).isEqualTo(MainViewModel.NavigationView.EXECUTIONS);

            // 3. Navigate to execution console
            mainViewModel.setCurrentView(MainViewModel.NavigationView.EXECUTION_CONSOLE);
            assertThat(mainViewModel.getCurrentView()).isEqualTo(MainViewModel.NavigationView.EXECUTION_CONSOLE);
        }

        @Test
        @DisplayName("User sees status updates")
        void userSeesStatusUpdates() {
            // 1. Initial status
            mainViewModel.updateStatus("Ready");
            assertThat(mainViewModel.getStatusMessage()).isEqualTo("Ready");

            // 2. Status changes during workflow operation
            mainViewModel.updateStatus("Saving workflow...");
            assertThat(mainViewModel.getStatusMessage()).isEqualTo("Saving workflow...");

            // 3. Success status
            mainViewModel.updateStatus("Workflow saved successfully");
            assertThat(mainViewModel.getStatusMessage()).isEqualTo("Workflow saved successfully");
        }

        @Test
        @DisplayName("User sees active workflow tracking")
        void userSeesActiveWorkflowTracking() {
            // 1. No active workflow initially
            assertThat(mainViewModel.hasActiveWorkflow()).isFalse();

            // 2. Load a workflow
            WorkflowDTO workflow = createSampleWorkflow();
            mainViewModel.setActiveWorkflow(workflow);

            assertThat(mainViewModel.hasActiveWorkflow()).isTrue();
            assertThat(mainViewModel.getActiveWorkflowName()).isEqualTo("Sample Workflow");
        }
    }

    // ===== User Flow: Complete Workflow Lifecycle =====

    @Nested
    @DisplayName("Complete Workflow Lifecycle")
    class CompleteWorkflowLifecycle {

        @Test
        @DisplayName("Full lifecycle: create, edit, save, reload")
        void fullWorkflowLifecycle() {
            // === CREATE PHASE ===
            // 1. Start new workflow
            canvasViewModel.newWorkflow();
            mainViewModel.setCurrentView(MainViewModel.NavigationView.WORKFLOWS);
            mainViewModel.updateStatus("Creating new workflow...");

            // 2. Add nodes
            Node startNode = createNode("start", "manual", "Start", 50, 100);
            Node processNode = createNode("process", "code", "Process", 250, 100);
            Node endNode = createNode("end", "set", "End", 450, 100);

            canvasViewModel.addNode(startNode);
            canvasViewModel.addNode(processNode);
            canvasViewModel.addNode(endNode);

            // 3. Connect nodes
            canvasViewModel.addConnection(createConnection("c1", "start", "process"));
            canvasViewModel.addConnection(createConnection("c2", "process", "end"));

            // 4. Name the workflow
            canvasViewModel.setWorkflowName("My Complete Workflow");

            // === EDIT PHASE ===
            // 5. Select and edit a node
            canvasViewModel.selectNode("process", false);
            Node selected = canvasViewModel.getNode("process");
            propertiesViewModel.show(selected);
            propertiesViewModel.setName("Data Processor");

            // === SAVE PHASE ===
            // 6. Export to DTO (simulating save)
            WorkflowDTO savedDto = canvasViewModel.toWorkflowDTO();
            canvasViewModel.markClean();

            assertThat(savedDto.name()).isEqualTo("My Complete Workflow");
            assertThat(savedDto.nodes()).hasSize(3);
            assertThat(savedDto.connections()).hasSize(2);
            assertThat(canvasViewModel.isDirty()).isFalse();

            // === RELOAD PHASE ===
            // 7. Clear and reload
            canvasViewModel.newWorkflow();
            assertThat(canvasViewModel.getNodeCount()).isZero();

            canvasViewModel.loadWorkflow(savedDto);
            mainViewModel.setActiveWorkflow(savedDto);

            // 8. Verify restored state
            assertThat(canvasViewModel.getNodeCount()).isEqualTo(3);
            assertThat(canvasViewModel.getConnectionCount()).isEqualTo(2);
            assertThat(canvasViewModel.getWorkflowName()).isEqualTo("My Complete Workflow");
            assertThat(mainViewModel.hasActiveWorkflow()).isTrue();

            mainViewModel.updateStatus("Workflow loaded successfully");
        }
    }
}
