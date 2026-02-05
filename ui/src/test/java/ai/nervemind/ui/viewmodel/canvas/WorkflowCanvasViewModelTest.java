/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

/**
 * Unit tests for WorkflowCanvasViewModel.
 */
@DisplayName("WorkflowCanvasViewModel")
class WorkflowCanvasViewModelTest {

    private WorkflowCanvasViewModel viewModel;
    private CanvasZoomService zoomService;
    private NodeSelectionService<String, String> selectionService;

    @BeforeEach
    void setUp() {
        zoomService = new CanvasZoomService();
        selectionService = new NodeSelectionService<>();
        viewModel = new WorkflowCanvasViewModel(zoomService, selectionService);
    }

    // ===== Test Helpers =====

    private Node createTestNode(String id, String type) {
        return new Node(id, type, type + " Node", new Node.Position(100.0, 100.0), Map.of(), null, false, null);
    }

    private Connection createTestConnection(String id, String sourceId, String targetId) {
        return new Connection(id, sourceId, "output", targetId, "input");
    }

    private WorkflowDTO createTestWorkflow() {
        Node node1 = createTestNode("node-1", "http");
        Node node2 = createTestNode("node-2", "transform");
        Connection conn = createTestConnection("conn-1", "node-1", "node-2");

        return new WorkflowDTO(
                1L,
                "Test Workflow",
                "Test Description",
                List.of(node1, node2),
                List.of(conn),
                Map.of(),
                true,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }

    // ===== Initialization Tests =====

    @Nested
    @DisplayName("Initialization")
    class InitializationTests {

        @Test
        @DisplayName("should have default workflow name")
        void shouldHaveDefaultWorkflowName() {
            assertThat(viewModel.getWorkflowName()).isEqualTo("New Workflow");
        }

        @Test
        @DisplayName("should start with empty nodes")
        void shouldStartWithEmptyNodes() {
            assertThat(viewModel.getNodeCount()).isZero();
            assertThat(viewModel.getNodes()).isEmpty();
        }

        @Test
        @DisplayName("should start with empty connections")
        void shouldStartWithEmptyConnections() {
            assertThat(viewModel.getConnectionCount()).isZero();
            assertThat(viewModel.getConnections()).isEmpty();
        }

        @Test
        @DisplayName("should start with grid enabled")
        void shouldStartWithGridEnabled() {
            assertThat(viewModel.isShowGrid()).isTrue();
            assertThat(viewModel.isSnapToGrid()).isTrue();
        }

        @Test
        @DisplayName("should start as clean (not dirty)")
        void shouldStartClean() {
            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should start not executing")
        void shouldStartNotExecuting() {
            assertThat(viewModel.isExecuting()).isFalse();
        }

        @Test
        @DisplayName("should start with no selection")
        void shouldStartWithNoSelection() {
            assertThat(viewModel.hasSelection()).isFalse();
            assertThat(viewModel.getSelectedNodeCount()).isZero();
        }
    }

    // ===== Workflow Operations Tests =====

    @Nested
    @DisplayName("Workflow Operations")
    class WorkflowOperationsTests {

        @Test
        @DisplayName("should load workflow")
        void shouldLoadWorkflow() {
            WorkflowDTO workflow = createTestWorkflow();

            viewModel.loadWorkflow(workflow);

            assertThat(viewModel.getWorkflowName()).isEqualTo("Test Workflow");
            assertThat(viewModel.getWorkflowDescription()).isEqualTo("Test Description");
            assertThat(viewModel.isWorkflowActive()).isTrue();
            assertThat(viewModel.getNodeCount()).isEqualTo(2);
            assertThat(viewModel.getConnectionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should clear dirty flag after loading workflow")
        void shouldClearDirtyAfterLoad() {
            viewModel.markDirty();
            assertThat(viewModel.isDirty()).isTrue();

            viewModel.loadWorkflow(createTestWorkflow());

            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should create new workflow")
        void shouldCreateNewWorkflow() {
            viewModel.loadWorkflow(createTestWorkflow());
            assertThat(viewModel.getNodeCount()).isEqualTo(2);

            viewModel.newWorkflow();

            assertThat(viewModel.getWorkflowName()).isEqualTo("New Workflow");
            assertThat(viewModel.getWorkflowDescription()).isEmpty();
            assertThat(viewModel.isWorkflowActive()).isFalse();
            assertThat(viewModel.getNodeCount()).isZero();
            assertThat(viewModel.getConnectionCount()).isZero();
            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should fire workflow changed callback")
        void shouldFireWorkflowChangedCallback() {
            AtomicBoolean called = new AtomicBoolean(false);
            viewModel.setOnWorkflowChanged(() -> called.set(true));

            viewModel.loadWorkflow(createTestWorkflow());

            assertThat(called.get()).isTrue();
        }

        @Test
        @DisplayName("should convert to WorkflowDTO")
        void shouldConvertToWorkflowDTO() {
            viewModel.loadWorkflow(createTestWorkflow());
            viewModel.setWorkflowName("Updated Name");
            viewModel.setWorkflowDescription("Updated Description");

            WorkflowDTO result = viewModel.toWorkflowDTO();

            assertThat(result.name()).isEqualTo("Updated Name");
            assertThat(result.description()).isEqualTo("Updated Description");
            assertThat(result.nodes()).hasSize(2);
            assertThat(result.connections()).hasSize(1);
        }
    }

    // ===== Node Operations Tests =====

    @Nested
    @DisplayName("Node Operations")
    class NodeOperationsTests {

        @Test
        @DisplayName("should add node")
        void shouldAddNode() {
            Node node = createTestNode("test-node", "http");

            viewModel.addNode(node);

            assertThat(viewModel.getNodeCount()).isEqualTo(1);
            assertThat(viewModel.getNode("test-node")).isEqualTo(node);
            assertThat(viewModel.hasNode("test-node")).isTrue();
        }

        @Test
        @DisplayName("should mark dirty when adding node")
        void shouldMarkDirtyWhenAddingNode() {
            viewModel.markClean();

            viewModel.addNode(createTestNode("test-node", "http"));

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should fire nodes changed callback when adding")
        void shouldFireNodesChangedWhenAdding() {
            AtomicInteger callCount = new AtomicInteger(0);
            viewModel.setOnNodesChanged(callCount::incrementAndGet);

            viewModel.addNode(createTestNode("node-1", "http"));
            viewModel.addNode(createTestNode("node-2", "transform"));

            assertThat(callCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("should remove node")
        void shouldRemoveNode() {
            viewModel.addNode(createTestNode("test-node", "http"));
            assertThat(viewModel.hasNode("test-node")).isTrue();

            Node removed = viewModel.removeNode("test-node");

            assertThat(removed).isNotNull();
            assertThat(viewModel.hasNode("test-node")).isFalse();
            assertThat(viewModel.getNodeCount()).isZero();
        }

        @Test
        @DisplayName("should remove associated connections when removing node")
        void shouldRemoveAssociatedConnections() {
            viewModel.addNode(createTestNode("node-1", "http"));
            viewModel.addNode(createTestNode("node-2", "transform"));
            viewModel.addConnection(createTestConnection("conn-1", "node-1", "node-2"));
            assertThat(viewModel.getConnectionCount()).isEqualTo(1);

            viewModel.removeNode("node-1");

            assertThat(viewModel.getConnectionCount()).isZero();
        }

        @Test
        @DisplayName("should update node")
        void shouldUpdateNode() {
            viewModel.addNode(createTestNode("test-node", "http"));
            Node updated = new Node("test-node", "http", "Updated Name", new Node.Position(200.0, 200.0), Map.of(),
                    null, false, null);

            viewModel.updateNode("test-node", updated);

            assertThat(viewModel.getNode("test-node").name()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("should return null for non-existent node")
        void shouldReturnNullForNonExistentNode() {
            assertThat(viewModel.getNode("non-existent")).isNull();
            assertThat(viewModel.hasNode("non-existent")).isFalse();
        }
    }

    // ===== Connection Operations Tests =====

    @Nested
    @DisplayName("Connection Operations")
    class ConnectionOperationsTests {

        @BeforeEach
        void setUpNodes() {
            viewModel.addNode(createTestNode("node-1", "http"));
            viewModel.addNode(createTestNode("node-2", "transform"));
            viewModel.addNode(createTestNode("node-3", "output"));
        }

        @Test
        @DisplayName("should add connection")
        void shouldAddConnection() {
            Connection conn = createTestConnection("conn-1", "node-1", "node-2");

            viewModel.addConnection(conn);

            assertThat(viewModel.getConnectionCount()).isEqualTo(1);
            assertThat(viewModel.getConnection("conn-1")).isEqualTo(conn);
        }

        @Test
        @DisplayName("should fire connections changed callback")
        void shouldFireConnectionsChangedCallback() {
            AtomicBoolean called = new AtomicBoolean(false);
            viewModel.setOnConnectionsChanged(() -> called.set(true));

            viewModel.addConnection(createTestConnection("conn-1", "node-1", "node-2"));

            assertThat(called.get()).isTrue();
        }

        @Test
        @DisplayName("should remove connection")
        void shouldRemoveConnection() {
            viewModel.addConnection(createTestConnection("conn-1", "node-1", "node-2"));

            Connection removed = viewModel.removeConnection("conn-1");

            assertThat(removed).isNotNull();
            assertThat(viewModel.getConnectionCount()).isZero();
        }

        @Test
        @DisplayName("should find outgoing connections for node")
        void shouldFindOutgoingConnections() {
            viewModel.addConnection(createTestConnection("conn-1", "node-1", "node-2"));
            viewModel.addConnection(createTestConnection("conn-2", "node-1", "node-3"));
            viewModel.addConnection(createTestConnection("conn-3", "node-2", "node-3"));

            List<Connection> outgoing = viewModel.getConnectionsForNode("node-1", true);

            assertThat(outgoing).hasSize(2);
        }

        @Test
        @DisplayName("should find incoming connections for node")
        void shouldFindIncomingConnections() {
            viewModel.addConnection(createTestConnection("conn-1", "node-1", "node-2"));
            viewModel.addConnection(createTestConnection("conn-2", "node-1", "node-3"));
            viewModel.addConnection(createTestConnection("conn-3", "node-2", "node-3"));

            List<Connection> incoming = viewModel.getConnectionsForNode("node-3", false);

            assertThat(incoming).hasSize(2);
        }
    }

    // ===== Selection Tests =====

    @Nested
    @DisplayName("Selection")
    class SelectionTests {

        @BeforeEach
        void setUpNodes() {
            viewModel.addNode(createTestNode("node-1", "http"));
            viewModel.addNode(createTestNode("node-2", "transform"));
        }

        @Test
        @DisplayName("should select node")
        void shouldSelectNode() {
            viewModel.selectNode("node-1", false);

            assertThat(viewModel.hasSelection()).isTrue();
            assertThat(viewModel.getSelectedNodeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should multi-select nodes")
        void shouldMultiSelectNodes() {
            viewModel.selectNode("node-1", false);
            viewModel.selectNode("node-2", true);

            assertThat(viewModel.getSelectedNodeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should deselect all")
        void shouldDeselectAll() {
            viewModel.selectNode("node-1", false);
            viewModel.selectNode("node-2", true);

            viewModel.deselectAll();

            assertThat(viewModel.hasSelection()).isFalse();
            assertThat(viewModel.getSelectedNodeCount()).isZero();
        }

        @Test
        @DisplayName("should delete selected nodes")
        void shouldDeleteSelectedNodes() {
            viewModel.selectNode("node-1", false);
            viewModel.selectNode("node-2", true);

            viewModel.deleteSelected();

            assertThat(viewModel.getNodeCount()).isZero();
            assertThat(viewModel.hasSelection()).isFalse();
        }
    }

    // ===== Zoom Tests =====

    @Nested
    @DisplayName("Zoom")
    class ZoomTests {

        @Test
        @DisplayName("should zoom in")
        void shouldZoomIn() {
            int initialZoom = viewModel.getZoomPercentage();

            viewModel.zoomIn();

            assertThat(viewModel.getZoomPercentage()).isGreaterThan(initialZoom);
        }

        @Test
        @DisplayName("should zoom out")
        void shouldZoomOut() {
            viewModel.zoomIn();
            int afterZoomIn = viewModel.getZoomPercentage();

            viewModel.zoomOut();

            assertThat(viewModel.getZoomPercentage()).isLessThan(afterZoomIn);
        }

        @Test
        @DisplayName("should reset zoom")
        void shouldResetZoom() {
            viewModel.zoomIn();
            viewModel.zoomIn();

            viewModel.resetZoom();

            assertThat(viewModel.getZoomPercentage()).isEqualTo(100);
        }

        @Test
        @DisplayName("should provide scale property")
        void shouldProvideScaleProperty() {
            assertThat(viewModel.scaleProperty()).isNotNull();
            assertThat(viewModel.getScale()).isEqualTo(1.0);
        }
    }

    // ===== Grid Tests =====

    @Nested
    @DisplayName("Grid")
    class GridTests {

        @Test
        @DisplayName("should toggle grid visibility")
        void shouldToggleGridVisibility() {
            assertThat(viewModel.isShowGrid()).isTrue();

            viewModel.toggleGrid();

            assertThat(viewModel.isShowGrid()).isFalse();
        }

        @Test
        @DisplayName("should toggle snap to grid")
        void shouldToggleSnapToGrid() {
            assertThat(viewModel.isSnapToGrid()).isTrue();

            viewModel.toggleSnapToGrid();

            assertThat(viewModel.isSnapToGrid()).isFalse();
        }

        @Test
        @DisplayName("should snap value to grid when enabled")
        void shouldSnapToGridWhenEnabled() {
            double result = viewModel.snapToGrid(25.0);

            assertThat(result).isEqualTo(20.0); // Grid size is 20
        }

        @Test
        @DisplayName("should not snap when disabled")
        void shouldNotSnapWhenDisabled() {
            viewModel.toggleSnapToGrid();

            double result = viewModel.snapToGrid(25.0);

            assertThat(result).isEqualTo(25.0);
        }
    }

    // ===== Clipboard Tests =====

    @Nested
    @DisplayName("Clipboard")
    class ClipboardTests {

        @BeforeEach
        void setUpNodes() {
            viewModel.addNode(createTestNode("node-1", "http"));
            viewModel.addNode(createTestNode("node-2", "transform"));
        }

        @Test
        @DisplayName("should start with empty clipboard")
        void shouldStartWithEmptyClipboard() {
            assertThat(viewModel.hasClipboardContent()).isFalse();
            assertThat(viewModel.getClipboard()).isEmpty();
        }

        @Test
        @DisplayName("should copy selected nodes to clipboard")
        void shouldCopySelectedNodes() {
            viewModel.selectNode("node-1", false);
            viewModel.selectNode("node-2", true);

            viewModel.copySelectedNodes();

            assertThat(viewModel.hasClipboardContent()).isTrue();
            assertThat(viewModel.getClipboard()).hasSize(2);
        }
    }

    // ===== State Tests =====

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        @DisplayName("should set workflow name and mark dirty")
        void shouldSetWorkflowNameAndMarkDirty() {
            viewModel.markClean();

            viewModel.setWorkflowName("New Name");

            assertThat(viewModel.getWorkflowName()).isEqualTo("New Name");
            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should set workflow description and mark dirty")
        void shouldSetWorkflowDescriptionAndMarkDirty() {
            viewModel.markClean();

            viewModel.setWorkflowDescription("New Description");

            assertThat(viewModel.getWorkflowDescription()).isEqualTo("New Description");
            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should set workflow active and mark dirty")
        void shouldSetWorkflowActiveAndMarkDirty() {
            viewModel.markClean();

            viewModel.setWorkflowActive(true);

            assertThat(viewModel.isWorkflowActive()).isTrue();
            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should manage executing state")
        void shouldManageExecutingState() {
            assertThat(viewModel.isExecuting()).isFalse();

            viewModel.setExecuting(true);

            assertThat(viewModel.isExecuting()).isTrue();
        }

        @Test
        @DisplayName("should manage status message")
        void shouldManageStatusMessage() {
            viewModel.setStatusMessage("Test status");

            assertThat(viewModel.getStatusMessage()).isEqualTo("Test status");
        }

        @Test
        @DisplayName("should mark clean")
        void shouldMarkClean() {
            viewModel.addNode(createTestNode("test", "http"));
            assertThat(viewModel.isDirty()).isTrue();

            viewModel.markClean();

            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    // ===== Services Access Tests =====

    @Nested
    @DisplayName("Services Access")
    class ServicesAccessTests {

        @Test
        @DisplayName("should provide zoom service")
        void shouldProvideZoomService() {
            assertThat(viewModel.getZoomService()).isSameAs(zoomService);
        }

        @Test
        @DisplayName("should provide selection service")
        void shouldProvideSelectionService() {
            assertThat(viewModel.getSelectionService()).isSameAs(selectionService);
        }
    }
}
