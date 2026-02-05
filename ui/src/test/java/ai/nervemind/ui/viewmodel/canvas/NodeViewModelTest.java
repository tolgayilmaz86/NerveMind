/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.canvas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Node;
import ai.nervemind.common.domain.Node.Position;
import ai.nervemind.ui.viewmodel.canvas.NodeViewModel.ExecutionState;
import ai.nervemind.ui.viewmodel.canvas.NodeViewModel.NodeCategory;

/**
 * Unit tests for NodeViewModel.
 */
@DisplayName("NodeViewModel")
class NodeViewModelTest {

    private NodeViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new NodeViewModel();
    }

    private Node createTestNode(String id, String type, String name) {
        return new Node(
                id,
                type,
                name,
                new Position(100, 200),
                Map.of("key1", "value1"),
                null, // credentialId
                false,
                "Test notes");
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create empty ViewModel")
        void shouldCreateEmptyViewModel() {
            assertNull(viewModel.getNodeId());
            assertNull(viewModel.getNodeType());
            assertNull(viewModel.getName());
            assertEquals(0.0, viewModel.getLayoutX());
            assertEquals(0.0, viewModel.getLayoutY());
            assertFalse(viewModel.isSelected());
            assertEquals(ExecutionState.IDLE, viewModel.getExecutionState());
        }

        @Test
        @DisplayName("Should create ViewModel from Node")
        void shouldCreateViewModelFromNode() {
            Node node = createTestNode("node-1", "httpRequest", "Test Node");
            NodeViewModel vm = new NodeViewModel(node);

            assertEquals("node-1", vm.getNodeId());
            assertEquals("httpRequest", vm.getNodeType());
            assertEquals("Test Node", vm.getName());
            assertEquals(100.0, vm.getLayoutX());
            assertEquals(200.0, vm.getLayoutY());
            assertFalse(vm.isDisabled());
            assertEquals("Test notes", vm.getNotes());
            assertEquals(NodeCategory.ACTION, vm.getCategory());
        }
    }

    @Nested
    @DisplayName("Loading from Node")
    class LoadingTests {

        @Test
        @DisplayName("Should load state from domain Node")
        void shouldLoadStateFromNode() {
            Node node = createTestNode("node-1", "llmChat", "AI Chat");

            viewModel.loadFromNode(node);

            assertEquals("node-1", viewModel.getNodeId());
            assertEquals("llmChat", viewModel.getNodeType());
            assertEquals("AI Chat", viewModel.getName());
            assertEquals(100.0, viewModel.getLayoutX());
            assertEquals(200.0, viewModel.getLayoutY());
            assertEquals(NodeCategory.AI, viewModel.getCategory());
        }

        @Test
        @DisplayName("Should reset execution state when loading")
        void shouldResetExecutionStateWhenLoading() {
            viewModel.setExecutionState(ExecutionState.RUNNING);
            viewModel.setSelected(true);

            Node node = createTestNode("node-1", "code", "Script");
            viewModel.loadFromNode(node);

            assertEquals(ExecutionState.IDLE, viewModel.getExecutionState());
            assertFalse(viewModel.isSelected());
        }

        @Test
        @DisplayName("Should handle null node")
        void shouldHandleNullNode() {
            viewModel.loadFromNode(createTestNode("node-1", "code", "Script"));
            viewModel.loadFromNode(null);

            assertNull(viewModel.getNodeId());
            assertNull(viewModel.getNode());
        }

        @Test
        @DisplayName("Should handle null parameters")
        void shouldHandleNullParameters() {
            Node node = new Node("id", "type", "name", new Position(0, 0), null, null, false, null);
            viewModel.loadFromNode(node);

            assertNotNull(viewModel.getParameters());
            assertTrue(viewModel.getParameters().isEmpty());
        }
    }

    @Nested
    @DisplayName("Converting to Node")
    class ToNodeTests {

        @Test
        @DisplayName("Should create Node from ViewModel state")
        void shouldCreateNodeFromState() {
            Node original = createTestNode("node-1", "httpRequest", "Request");
            viewModel.loadFromNode(original);

            viewModel.setName("Updated Name");
            viewModel.setPosition(300, 400);
            viewModel.setDisabled(true);

            Node result = viewModel.toNode();

            assertEquals("node-1", result.id());
            assertEquals("httpRequest", result.type());
            assertEquals("Updated Name", result.name());
            assertEquals(300.0, result.position().x());
            assertEquals(400.0, result.position().y());
            assertTrue(result.disabled());
        }

        @Test
        @DisplayName("Should return null when no original node")
        void shouldReturnNullWhenNoOriginalNode() {
            assertNull(viewModel.toNode());
        }
    }

    @Nested
    @DisplayName("Position Methods")
    class PositionTests {

        @Test
        @DisplayName("Should update position")
        void shouldUpdatePosition() {
            viewModel.setPosition(150, 250);

            assertEquals(150.0, viewModel.getLayoutX());
            assertEquals(250.0, viewModel.getLayoutY());
        }

        @Test
        @DisplayName("Should snap to grid")
        void shouldSnapToGrid() {
            viewModel.setPosition(123, 267);
            viewModel.snapToGrid(20.0);

            assertEquals(120.0, viewModel.getLayoutX());
            assertEquals(260.0, viewModel.getLayoutY());
        }

        @Test
        @DisplayName("Should calculate center coordinates")
        void shouldCalculateCenterCoordinates() {
            viewModel.setPosition(100, 200);

            assertEquals(100 + NodeViewModel.NODE_SIZE / 2, viewModel.getCenterX());
            assertEquals(200 + NodeViewModel.NODE_SIZE / 2, viewModel.getCenterY());
        }

        @Test
        @DisplayName("Should calculate input handle coordinates")
        void shouldCalculateInputHandleCoordinates() {
            viewModel.setPosition(100, 200);

            assertEquals(100 - NodeViewModel.HANDLE_RADIUS, viewModel.getInputX());
            assertEquals(200 + NodeViewModel.NODE_SIZE / 2, viewModel.getInputY());
        }

        @Test
        @DisplayName("Should calculate output handle coordinates")
        void shouldCalculateOutputHandleCoordinates() {
            viewModel.setPosition(100, 200);

            assertEquals(100 + NodeViewModel.NODE_SIZE + NodeViewModel.HANDLE_RADIUS, viewModel.getOutputX());
            assertEquals(200 + NodeViewModel.NODE_SIZE / 2, viewModel.getOutputY());
        }

        @Test
        @DisplayName("Should notify position changes")
        void shouldNotifyPositionChanges() {
            AtomicInteger callCount = new AtomicInteger(0);
            viewModel.setOnPositionChanged(callCount::incrementAndGet);

            viewModel.setLayoutX(100);
            viewModel.setLayoutY(200);

            assertEquals(2, callCount.get());
        }
    }

    @Nested
    @DisplayName("Execution State")
    class ExecutionStateTests {

        @Test
        @DisplayName("Should set execution state")
        void shouldSetExecutionState() {
            viewModel.setExecutionState(ExecutionState.RUNNING);

            assertEquals(ExecutionState.RUNNING, viewModel.getExecutionState());
        }

        @Test
        @DisplayName("Should set execution state with error message")
        void shouldSetExecutionStateWithErrorMessage() {
            viewModel.setExecutionState(ExecutionState.ERROR, "Connection failed");

            assertEquals(ExecutionState.ERROR, viewModel.getExecutionState());
            assertEquals("Connection failed", viewModel.getErrorMessage());
            assertTrue(viewModel.hasError());
        }

        @Test
        @DisplayName("Should clear error message for non-error states")
        void shouldClearErrorMessageForNonErrorStates() {
            viewModel.setExecutionState(ExecutionState.ERROR, "Some error");
            viewModel.setExecutionState(ExecutionState.SUCCESS, "Completed");

            assertEquals(ExecutionState.SUCCESS, viewModel.getExecutionState());
            assertNull(viewModel.getErrorMessage());
            assertFalse(viewModel.hasError());
        }

        @Test
        @DisplayName("Should reset execution state")
        void shouldResetExecutionState() {
            viewModel.setExecutionState(ExecutionState.ERROR, "Error");
            viewModel.resetExecutionState();

            assertEquals(ExecutionState.IDLE, viewModel.getExecutionState());
            assertNull(viewModel.getErrorMessage());
        }

        @Test
        @DisplayName("Should detect executing states")
        void shouldDetectExecutingStates() {
            viewModel.setExecutionState(ExecutionState.IDLE);
            assertFalse(viewModel.isExecuting());

            viewModel.setExecutionState(ExecutionState.QUEUED);
            assertTrue(viewModel.isExecuting());

            viewModel.setExecutionState(ExecutionState.RUNNING);
            assertTrue(viewModel.isExecuting());

            viewModel.setExecutionState(ExecutionState.SUCCESS);
            assertFalse(viewModel.isExecuting());
        }

        @Test
        @DisplayName("Should notify state changes")
        void shouldNotifyStateChanges() {
            AtomicBoolean notified = new AtomicBoolean(false);
            viewModel.setOnStateChanged(() -> notified.set(true));

            viewModel.setExecutionState(ExecutionState.RUNNING);

            assertTrue(notified.get());
        }
    }

    @Nested
    @DisplayName("Selection State")
    class SelectionStateTests {

        @Test
        @DisplayName("Should update selection state")
        void shouldUpdateSelectionState() {
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
        @DisplayName("Should not notify when deselecting")
        void shouldNotNotifyWhenDeselecting() {
            AtomicBoolean notified = new AtomicBoolean(false);
            viewModel.setOnSelected(() -> notified.set(true));

            viewModel.setSelected(false);

            assertFalse(notified.get());
        }
    }

    @Nested
    @DisplayName("Type Category Mapping")
    class TypeCategoryTests {

        @Test
        @DisplayName("Should map trigger types to TRIGGER category")
        void shouldMapTriggerTypes() {
            assertEquals(NodeCategory.TRIGGER, NodeViewModel.getCategoryForType("manualTrigger"));
            assertEquals(NodeCategory.TRIGGER, NodeViewModel.getCategoryForType("scheduleTrigger"));
            assertEquals(NodeCategory.TRIGGER, NodeViewModel.getCategoryForType("webhookTrigger"));
        }

        @Test
        @DisplayName("Should map action types to ACTION category")
        void shouldMapActionTypes() {
            assertEquals(NodeCategory.ACTION, NodeViewModel.getCategoryForType("httpRequest"));
            assertEquals(NodeCategory.ACTION, NodeViewModel.getCategoryForType("code"));
            assertEquals(NodeCategory.ACTION, NodeViewModel.getCategoryForType("executeCommand"));
        }

        @Test
        @DisplayName("Should map flow types to FLOW category")
        void shouldMapFlowTypes() {
            assertEquals(NodeCategory.FLOW, NodeViewModel.getCategoryForType("if"));
            assertEquals(NodeCategory.FLOW, NodeViewModel.getCategoryForType("switch"));
            assertEquals(NodeCategory.FLOW, NodeViewModel.getCategoryForType("merge"));
            assertEquals(NodeCategory.FLOW, NodeViewModel.getCategoryForType("loop"));
        }

        @Test
        @DisplayName("Should map data types to DATA category")
        void shouldMapDataTypes() {
            assertEquals(NodeCategory.DATA, NodeViewModel.getCategoryForType("set"));
            assertEquals(NodeCategory.DATA, NodeViewModel.getCategoryForType("filter"));
            assertEquals(NodeCategory.DATA, NodeViewModel.getCategoryForType("sort"));
        }

        @Test
        @DisplayName("Should map AI types to AI category")
        void shouldMapAITypes() {
            assertEquals(NodeCategory.AI, NodeViewModel.getCategoryForType("llmChat"));
            assertEquals(NodeCategory.AI, NodeViewModel.getCategoryForType("textClassifier"));
            assertEquals(NodeCategory.AI, NodeViewModel.getCategoryForType("embedding"));
            assertEquals(NodeCategory.AI, NodeViewModel.getCategoryForType("rag"));
        }

        @Test
        @DisplayName("Should map unknown types to DEFAULT category")
        void shouldMapUnknownTypes() {
            assertEquals(NodeCategory.DEFAULT, NodeViewModel.getCategoryForType("unknown"));
            assertEquals(NodeCategory.DEFAULT, NodeViewModel.getCategoryForType(null));
        }
    }

    @Nested
    @DisplayName("Subtitle Generation")
    class SubtitleTests {

        @Test
        @DisplayName("Should generate correct subtitles")
        void shouldGenerateCorrectSubtitles() {
            assertEquals("Trigger", NodeViewModel.getSubtitleForType("manualTrigger"));
            assertEquals("HTTP", NodeViewModel.getSubtitleForType("httpRequest"));
            assertEquals("Script", NodeViewModel.getSubtitleForType("code"));
            assertEquals("Condition", NodeViewModel.getSubtitleForType("if"));
            assertEquals("AI Chat", NodeViewModel.getSubtitleForType("llmChat"));
        }

        @Test
        @DisplayName("Should format unknown types as labels")
        void shouldFormatUnknownTypesAsLabels() {
            assertEquals("Custom Node", NodeViewModel.getSubtitleForType("customNode"));
            assertEquals("My Type", NodeViewModel.getSubtitleForType("myType"));
        }

        @Test
        @DisplayName("Should handle null and empty types")
        void shouldHandleNullAndEmptyTypes() {
            assertEquals("", NodeViewModel.getSubtitleForType(null));
            assertEquals("", NodeViewModel.getSubtitleForType(""));
        }
    }

    @Nested
    @DisplayName("Border Color")
    class BorderColorTests {

        @Test
        @DisplayName("Should return correct colors for categories")
        void shouldReturnCorrectColorsForCategories() {
            assertEquals("#40c057", NodeViewModel.getBorderColorForCategory(NodeCategory.TRIGGER));
            assertEquals("#4a9eff", NodeViewModel.getBorderColorForCategory(NodeCategory.ACTION));
            assertEquals("#be4bdb", NodeViewModel.getBorderColorForCategory(NodeCategory.FLOW));
            assertEquals("#fab005", NodeViewModel.getBorderColorForCategory(NodeCategory.DATA));
            assertEquals("#ff6b6b", NodeViewModel.getBorderColorForCategory(NodeCategory.AI));
            assertEquals("#404040", NodeViewModel.getBorderColorForCategory(NodeCategory.DEFAULT));
            assertEquals("#404040", NodeViewModel.getBorderColorForCategory(null));
        }
    }

    @Nested
    @DisplayName("Visual State Properties")
    class VisualStateTests {

        @Test
        @DisplayName("Should update hovered state")
        void shouldUpdateHoveredState() {
            viewModel.setHovered(true);
            assertTrue(viewModel.isHovered());

            viewModel.setHovered(false);
            assertFalse(viewModel.isHovered());
        }

        @Test
        @DisplayName("Should update target highlighted state")
        void shouldUpdateTargetHighlightedState() {
            viewModel.setTargetHighlighted(true);
            assertTrue(viewModel.isTargetHighlighted());

            viewModel.setTargetHighlighted(false);
            assertFalse(viewModel.isTargetHighlighted());
        }

        @Test
        @DisplayName("Should update custom icon")
        void shouldUpdateCustomIcon() {
            viewModel.setCustomIcon("mdi-star");
            assertEquals("mdi-star", viewModel.getCustomIcon());
        }

        @Test
        @DisplayName("Should update notes")
        void shouldUpdateNotes() {
            viewModel.setNotes("Some notes");
            assertEquals("Some notes", viewModel.getNotes());
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
    @DisplayName("Clear State")
    class ClearTests {

        @Test
        @DisplayName("Should clear all state")
        void shouldClearAllState() {
            viewModel.loadFromNode(createTestNode("node-1", "code", "Test"));
            viewModel.setSelected(true);
            viewModel.setExecutionState(ExecutionState.RUNNING);
            viewModel.setHovered(true);

            viewModel.clear();

            assertNull(viewModel.getNodeId());
            assertNull(viewModel.getNode());
            assertNull(viewModel.getName());
            assertEquals(0.0, viewModel.getLayoutX());
            assertEquals(0.0, viewModel.getLayoutY());
            assertFalse(viewModel.isSelected());
            assertFalse(viewModel.isHovered());
            assertEquals(ExecutionState.IDLE, viewModel.getExecutionState());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            viewModel.loadFromNode(createTestNode("node-1", "httpRequest", "My Node"));
            viewModel.setSelected(true);
            viewModel.setExecutionState(ExecutionState.RUNNING);

            String str = viewModel.toString();

            assertTrue(str.contains("node-1"));
            assertTrue(str.contains("httpRequest"));
            assertTrue(str.contains("My Node"));
            assertTrue(str.contains("selected=true"));
            assertTrue(str.contains("RUNNING"));
        }
    }
}
