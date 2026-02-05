/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.ui.viewmodel.canvas.ConnectionViewModel.ConnectionState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConnectionViewModel.
 */
@DisplayName("ConnectionViewModel")
class ConnectionViewModelTest {

    private ConnectionViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ConnectionViewModel();
    }

    /**
     * Helper to create a Connection with default outputs/inputs.
     */
    private Connection createConnection(String id, String sourceNodeId, String targetNodeId) {
        return new Connection(id, sourceNodeId, "main", targetNodeId, "main");
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create empty ViewModel")
        void shouldCreateEmptyViewModel() {
            assertNull(viewModel.getConnectionId());
            assertNull(viewModel.getSourceNodeId());
            assertNull(viewModel.getTargetNodeId());
            assertEquals(0.0, viewModel.getSourceX());
            assertEquals(0.0, viewModel.getSourceY());
            assertEquals(0.0, viewModel.getTargetX());
            assertEquals(0.0, viewModel.getTargetY());
            assertFalse(viewModel.isSelected());
            assertEquals(ConnectionState.IDLE, viewModel.getState());
        }

        @Test
        @DisplayName("Should create from domain Connection")
        void shouldCreateFromDomainConnection() {
            Connection connection = createConnection("conn-1", "node-a", "node-b");
            ConnectionViewModel vm = new ConnectionViewModel(connection);

            assertEquals("conn-1", vm.getConnectionId());
            assertEquals("node-a", vm.getSourceNodeId());
            assertEquals("node-b", vm.getTargetNodeId());
        }

        @Test
        @DisplayName("Should create with coordinates")
        void shouldCreateWithCoordinates() {
            ConnectionViewModel vm = new ConnectionViewModel("node-a", "node-b", 100, 200, 300, 400);

            assertEquals("node-a", vm.getSourceNodeId());
            assertEquals("node-b", vm.getTargetNodeId());
            assertEquals(100.0, vm.getSourceX());
            assertEquals(200.0, vm.getSourceY());
            assertEquals(300.0, vm.getTargetX());
            assertEquals(400.0, vm.getTargetY());
            assertEquals("node-a->node-b", vm.getConnectionId());
        }
    }

    @Nested
    @DisplayName("Loading from Connection")
    class LoadingTests {

        @Test
        @DisplayName("Should load state from domain Connection")
        void shouldLoadStateFromConnection() {
            Connection connection = createConnection("conn-1", "node-a", "node-b");

            viewModel.loadFromConnection(connection);

            assertEquals("conn-1", viewModel.getConnectionId());
            assertEquals("node-a", viewModel.getSourceNodeId());
            assertEquals("node-b", viewModel.getTargetNodeId());
            assertEquals(connection, viewModel.getConnection());
        }

        @Test
        @DisplayName("Should reset state when loading")
        void shouldResetStateWhenLoading() {
            viewModel.setState(ConnectionState.ACTIVE);
            viewModel.setSelected(true);

            Connection connection = createConnection("conn-1", "node-a", "node-b");
            viewModel.loadFromConnection(connection);

            assertEquals(ConnectionState.IDLE, viewModel.getState());
            assertFalse(viewModel.isSelected());
        }

        @Test
        @DisplayName("Should handle null connection")
        void shouldHandleNullConnection() {
            viewModel.loadFromConnection(createConnection("conn-1", "a", "b"));
            viewModel.loadFromConnection(null);

            assertNull(viewModel.getConnectionId());
            assertNull(viewModel.getConnection());
        }
    }

    @Nested
    @DisplayName("Converting to Connection")
    class ToConnectionTests {

        @Test
        @DisplayName("Should create Connection from ViewModel state")
        void shouldCreateConnectionFromState() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            Connection result = viewModel.toConnection();

            assertEquals("node-a->node-b", result.id());
            assertEquals("node-a", result.sourceNodeId());
            assertEquals("node-b", result.targetNodeId());
        }

        @Test
        @DisplayName("Should preserve original connection ID")
        void shouldPreserveOriginalConnectionId() {
            Connection original = createConnection("custom-id", "node-a", "node-b");
            viewModel.loadFromConnection(original);

            Connection result = viewModel.toConnection();

            assertEquals("custom-id", result.id());
        }
    }

    @Nested
    @DisplayName("Endpoint Updates")
    class EndpointTests {

        @Test
        @DisplayName("Should update all endpoints")
        void shouldUpdateAllEndpoints() {
            viewModel.updateEndpoints(100, 200, 300, 400);

            assertEquals(100.0, viewModel.getSourceX());
            assertEquals(200.0, viewModel.getSourceY());
            assertEquals(300.0, viewModel.getTargetX());
            assertEquals(400.0, viewModel.getTargetY());
        }

        @Test
        @DisplayName("Should update source endpoint only")
        void shouldUpdateSourceEndpointOnly() {
            viewModel.updateEndpoints(0, 0, 300, 400);
            viewModel.updateSourceEndpoint(100, 200);

            assertEquals(100.0, viewModel.getSourceX());
            assertEquals(200.0, viewModel.getSourceY());
            assertEquals(300.0, viewModel.getTargetX());
            assertEquals(400.0, viewModel.getTargetY());
        }

        @Test
        @DisplayName("Should update target endpoint only")
        void shouldUpdateTargetEndpointOnly() {
            viewModel.updateEndpoints(100, 200, 0, 0);
            viewModel.updateTargetEndpoint(300, 400);

            assertEquals(100.0, viewModel.getSourceX());
            assertEquals(200.0, viewModel.getSourceY());
            assertEquals(300.0, viewModel.getTargetX());
            assertEquals(400.0, viewModel.getTargetY());
        }

        @Test
        @DisplayName("Should calculate control points on endpoint change")
        void shouldCalculateControlPointsOnEndpointChange() {
            viewModel.updateEndpoints(0, 100, 200, 100);

            // Control points should be set
            assertTrue(viewModel.getControlX1() > 0);
            assertEquals(100.0, viewModel.getControlY1());
            assertTrue(viewModel.getControlX2() < 200);
            assertEquals(100.0, viewModel.getControlY2());
        }
    }

    @Nested
    @DisplayName("Control Points")
    class ControlPointTests {

        @Test
        @DisplayName("Should calculate horizontal control points for horizontal connections")
        void shouldCalculateHorizontalControlPoints() {
            viewModel.updateEndpoints(0, 100, 200, 100);

            // Source control point should be to the right of source
            assertTrue(viewModel.getControlX1() > 0);
            assertEquals(100.0, viewModel.getControlY1());

            // Target control point should be to the left of target
            assertTrue(viewModel.getControlX2() < 200);
            assertEquals(100.0, viewModel.getControlY2());
        }

        @Test
        @DisplayName("Should use minimum offset for close connections")
        void shouldUseMinimumOffsetForCloseConnections() {
            viewModel.updateEndpoints(0, 0, 50, 0);

            // Even for short distance, control points should have offset
            assertTrue(viewModel.getControlX1() >= 50);
        }
    }

    @Nested
    @DisplayName("Geometry Helpers")
    class GeometryTests {

        @Test
        @DisplayName("Should calculate length")
        void shouldCalculateLength() {
            viewModel.updateEndpoints(0, 0, 300, 400);

            // 3-4-5 triangle scaled: sqrt(300^2 + 400^2) = 500
            assertEquals(500.0, viewModel.getLength(), 0.001);
        }

        @Test
        @DisplayName("Should calculate midpoint")
        void shouldCalculateMidpoint() {
            viewModel.updateEndpoints(100, 200, 300, 400);

            assertEquals(200.0, viewModel.getMidpointX());
            assertEquals(300.0, viewModel.getMidpointY());
        }

        @Test
        @DisplayName("Should detect point near connection")
        void shouldDetectPointNearConnection() {
            viewModel.updateEndpoints(0, 100, 200, 100);

            // Point on the line
            assertTrue(viewModel.isNearPoint(100, 100, 10));

            // Point far from the line
            assertFalse(viewModel.isNearPoint(100, 200, 10));
        }

        @Test
        @DisplayName("Should reject points outside bounding box")
        void shouldRejectPointsOutsideBoundingBox() {
            viewModel.updateEndpoints(100, 100, 200, 200);

            // Far outside bounding box
            assertFalse(viewModel.isNearPoint(0, 0, 10));
            assertFalse(viewModel.isNearPoint(500, 500, 10));
        }
    }

    @Nested
    @DisplayName("State Management")
    class StateTests {

        @Test
        @DisplayName("Should update state")
        void shouldUpdateState() {
            viewModel.setState(ConnectionState.ACTIVE);
            assertEquals(ConnectionState.ACTIVE, viewModel.getState());

            viewModel.setState(ConnectionState.SUCCESS);
            assertEquals(ConnectionState.SUCCESS, viewModel.getState());

            viewModel.setState(ConnectionState.ERROR);
            assertEquals(ConnectionState.ERROR, viewModel.getState());
        }

        @Test
        @DisplayName("Should reset state")
        void shouldResetState() {
            viewModel.setState(ConnectionState.ACTIVE);
            viewModel.resetState();

            assertEquals(ConnectionState.IDLE, viewModel.getState());
        }
    }

    @Nested
    @DisplayName("Selection State")
    class SelectionTests {

        @Test
        @DisplayName("Should update selection")
        void shouldUpdateSelection() {
            viewModel.setSelected(true);
            assertTrue(viewModel.isSelected());

            viewModel.setSelected(false);
            assertFalse(viewModel.isSelected());
        }

        @Test
        @DisplayName("Should notify on selection")
        void shouldNotifyOnSelection() {
            AtomicBoolean notified = new AtomicBoolean(false);
            viewModel.setOnSelected(() -> notified.set(true));

            viewModel.setSelected(true);

            assertTrue(notified.get());
        }

        @Test
        @DisplayName("Should update hover state")
        void shouldUpdateHoverState() {
            viewModel.setHovered(true);
            assertTrue(viewModel.isHovered());

            viewModel.setHovered(false);
            assertFalse(viewModel.isHovered());
        }
    }

    @Nested
    @DisplayName("Node Involvement Check")
    class InvolvesNodeTests {

        @Test
        @DisplayName("Should detect source node")
        void shouldDetectSourceNode() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertTrue(viewModel.involvesNode("node-a"));
        }

        @Test
        @DisplayName("Should detect target node")
        void shouldDetectTargetNode() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertTrue(viewModel.involvesNode("node-b"));
        }

        @Test
        @DisplayName("Should not detect unrelated node")
        void shouldNotDetectUnrelatedNode() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertFalse(viewModel.involvesNode("node-c"));
        }

        @Test
        @DisplayName("Should handle null node ID")
        void shouldHandleNullNodeId() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertFalse(viewModel.involvesNode(null));
        }
    }

    @Nested
    @DisplayName("Clear State")
    class ClearTests {

        @Test
        @DisplayName("Should clear all state")
        void shouldClearAllState() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 100, 200, 300, 400);
            viewModel.setSelected(true);
            viewModel.setState(ConnectionState.ACTIVE);

            viewModel.clear();

            assertNull(viewModel.getConnectionId());
            assertNull(viewModel.getSourceNodeId());
            assertNull(viewModel.getTargetNodeId());
            assertEquals(0.0, viewModel.getSourceX());
            assertEquals(0.0, viewModel.getSourceY());
            assertEquals(0.0, viewModel.getTargetX());
            assertEquals(0.0, viewModel.getTargetY());
            assertFalse(viewModel.isSelected());
            assertEquals(ConnectionState.IDLE, viewModel.getState());
        }
    }

    @Nested
    @DisplayName("Deletion Callback")
    class DeletionTests {

        @Test
        @DisplayName("Should notify on deletion")
        void shouldNotifyOnDeletion() {
            AtomicBoolean notified = new AtomicBoolean(false);
            viewModel.setOnDeleted(() -> notified.set(true));

            viewModel.notifyDeleted();

            assertTrue(notified.get());
        }

        @Test
        @DisplayName("Should handle no deletion callback")
        void shouldHandleNoDeletionCallback() {
            assertDoesNotThrow(() -> viewModel.notifyDeleted());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when IDs match")
        void shouldBeEqualWhenIdsMatch() {
            ConnectionViewModel vm1 = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);
            ConnectionViewModel vm2 = new ConnectionViewModel("node-a", "node-b", 100, 200, 300, 400);

            assertEquals(vm1, vm2);
            assertEquals(vm1.hashCode(), vm2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when IDs differ")
        void shouldNotBeEqualWhenIdsDiffer() {
            ConnectionViewModel vm1 = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);
            ConnectionViewModel vm2 = new ConnectionViewModel("node-b", "node-c", 0, 0, 0, 0);

            assertNotEquals(vm1, vm2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            ConnectionViewModel vm = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertEquals(vm, vm);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            ConnectionViewModel vm = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertNotEquals(null, vm);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            ConnectionViewModel vm = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);

            assertNotEquals("string", vm);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            viewModel = new ConnectionViewModel("node-a", "node-b", 0, 0, 0, 0);
            viewModel.setState(ConnectionState.ACTIVE);

            String str = viewModel.toString();

            assertTrue(str.contains("node-a->node-b"));
            assertTrue(str.contains("node-a"));
            assertTrue(str.contains("node-b"));
            assertTrue(str.contains("ACTIVE"));
        }
    }
}
