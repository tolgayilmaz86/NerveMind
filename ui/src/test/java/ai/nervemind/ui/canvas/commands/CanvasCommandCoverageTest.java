/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

@ExtendWith(MockitoExtension.class)
@DisplayName("Canvas Commands")
class CanvasCommandCoverageTest {

    @Mock
    private WorkflowCanvas canvas;

    @Nested
    @DisplayName("AddNodeCommand")
    class AddNodeCommandTests {

        @Test
        @DisplayName("execute and undo should create then remove generated node")
        void executeAndUndoShouldCreateThenRemoveGeneratedNode() {
            AddNodeCommand command = new AddNodeCommand(canvas, "emailSend", 100.0, 200.0);

            command.execute();
            command.undo();

            String createdNodeId = command.getCreatedNodeId();
            assertThat(createdNodeId).isNotBlank();
            verify(canvas).createNodeInternal(createdNodeId, "emailSend", 100.0, 200.0);
            verify(canvas).deleteNodeInternal(createdNodeId);
            assertThat(command.getDescription()).isEqualTo("Add email Send");
        }

        @Test
        @DisplayName("prebuilt node path should restore and describe by name")
        void prebuiltNodePathShouldRestoreAndDescribeByName() {
            Node node = new Node(
                    "node-1",
                    "httpRequest",
                    "Fetch Users",
                    new Node.Position(10, 20),
                    Map.of(),
                    null,
                    false,
                    null);
            AddNodeCommand command = new AddNodeCommand(canvas, node);

            command.execute();
            command.undo();

            verify(canvas).restoreNode(node);
            verify(canvas).deleteNodeInternal("node-1");
            assertThat(command.getCreatedNodeId()).isEqualTo("node-1");
            assertThat(command.getDescription()).isEqualTo("Add Fetch Users");
        }
    }

    @Nested
    @DisplayName("DeleteNodeCommand")
    class DeleteNodeCommandTests {

        @Test
        @DisplayName("should delete and restore node when present")
        void shouldDeleteAndRestoreNodeWhenPresent() {
            Node node = new Node(
                    "node-2",
                    "code",
                    "Transform",
                    new Node.Position(0, 0),
                    Map.of(),
                    null,
                    false,
                    null);
            when(canvas.getNodeById("node-2")).thenReturn(node);
            DeleteNodeCommand command = new DeleteNodeCommand(canvas, "node-2");

            command.execute();
            command.undo();

            verify(canvas).deleteNodeInternal("node-2");
            verify(canvas).restoreNode(node);
            assertThat(command.getDescription()).isEqualTo("Delete Transform");
        }

        @Test
        @DisplayName("should no-op when node is missing")
        void shouldNoOpWhenNodeIsMissing() {
            when(canvas.getNodeById("missing")).thenReturn(null);
            DeleteNodeCommand command = new DeleteNodeCommand(canvas, "missing");

            command.execute();
            command.undo();

            verify(canvas, never()).deleteNodeInternal(anyString());
            verify(canvas, never()).restoreNode(org.mockito.ArgumentMatchers.any());
            assertThat(command.getDescription()).isEqualTo("Delete node");
        }
    }

    @Test
    @DisplayName("MoveNodeCommand should set new then old position")
    void moveNodeCommandShouldSetNewThenOldPosition() {
        MoveNodeCommand command = new MoveNodeCommand(canvas, "node-3", 5.0, 6.0, 50.0, 60.0);

        command.execute();
        command.undo();

        verify(canvas).setNodePosition("node-3", 50.0, 60.0);
        verify(canvas).setNodePosition("node-3", 5.0, 6.0);
        assertThat(command.getDescription()).isEqualTo("Move node");
    }

    @Test
    @DisplayName("AddConnectionCommand should add and then remove same generated connection")
    void addConnectionCommandShouldAddAndThenRemoveSameGeneratedConnection() {
        AddConnectionCommand command = new AddConnectionCommand(canvas, "source", "out", "target", "in");

        command.execute();
        command.undo();

        org.mockito.ArgumentCaptor<String> idCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(canvas).createConnectionInternal(idCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("source"),
                org.mockito.ArgumentMatchers.eq("out"),
                org.mockito.ArgumentMatchers.eq("target"),
                org.mockito.ArgumentMatchers.eq("in"));

        verify(canvas).deleteConnectionInternal(idCaptor.getValue());
        assertThat(command.getDescription()).isEqualTo("Connect nodes");
    }

    @Nested
    @DisplayName("DeleteConnectionCommand")
    class DeleteConnectionCommandTests {

        @Test
        @DisplayName("should delete and restore connection when present")
        void shouldDeleteAndRestoreConnectionWhenPresent() {
            Connection connection = new Connection("conn-1", "a", "main", "b", "main");
            when(canvas.getConnectionById("conn-1")).thenReturn(connection);
            DeleteConnectionCommand command = new DeleteConnectionCommand(canvas, "conn-1");

            command.execute();
            command.undo();

            verify(canvas).deleteConnectionInternal("conn-1");
            verify(canvas).restoreConnection(connection);
            assertThat(command.getDescription()).isEqualTo("Delete connection");
        }

        @Test
        @DisplayName("should no-op when connection is missing")
        void shouldNoOpWhenConnectionIsMissing() {
            when(canvas.getConnectionById("missing")).thenReturn(null);
            DeleteConnectionCommand command = new DeleteConnectionCommand(canvas, "missing");

            command.execute();
            command.undo();

            verify(canvas, never()).deleteConnectionInternal(anyString());
            verify(canvas, never()).restoreConnection(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("CompositeCommand")
    class CompositeCommandTests {

        @Mock
        private CanvasCommand first;

        @Mock
        private CanvasCommand second;

        @Test
        @DisplayName("execute should run commands in forward order and undo in reverse order")
        void executeShouldRunForwardAndUndoReverse() {
            CompositeCommand command = new CompositeCommand(List.of(first, second), "Bulk action");

            command.execute();
            command.undo();

            InOrder order = inOrder(first, second);
            order.verify(first).execute();
            order.verify(second).execute();
            order.verify(second).undo();
            order.verify(first).undo();
            assertThat(command.getDescription()).isEqualTo("Bulk action");
        }

        @Test
        @DisplayName("deleteMultiple factory should describe plural and trigger delete/restore")
        void deleteMultipleFactoryShouldDescribePluralAndTriggerDeleteRestore() {
            Node n1 = new Node("n1", "code", "N1", new Node.Position(1, 1), Map.of(), null, false, null);
            Node n2 = new Node("n2", "code", "N2", new Node.Position(2, 2), Map.of(), null, false, null);
            Connection c1 = new Connection("c1", "n1", "main", "n2", "main");
            when(canvas.getNodeById("n1")).thenReturn(n1);
            when(canvas.getNodeById("n2")).thenReturn(n2);
            when(canvas.getConnectionById("c1")).thenReturn(c1);

            CompositeCommand command = CompositeCommand.deleteMultiple(canvas, List.of(n1, n2), List.of(c1));

            command.execute();
            command.undo();

            verify(canvas).deleteConnectionInternal("c1");
            verify(canvas).deleteNodeInternal("n1");
            verify(canvas).deleteNodeInternal("n2");
            verify(canvas).restoreNode(n1);
            verify(canvas).restoreNode(n2);
            verify(canvas).restoreConnection(c1);
            assertThat(command.getDescription()).isEqualTo("Delete 2 nodes");
        }

        @Test
        @DisplayName("deleteMultiple factory should use singular description for one node")
        void deleteMultipleFactoryShouldUseSingularDescriptionForOneNode() {
            Node n1 = new Node("n1", "code", "N1", new Node.Position(1, 1), Map.of(), null, false, null);

            CompositeCommand command = CompositeCommand.deleteMultiple(canvas, List.of(n1), List.of());

            assertThat(command.getDescription()).isEqualTo("Delete 1 node");
        }

        @Test
        @DisplayName("pasteMultiple factory should restore nodes then remove them on undo")
        void pasteMultipleFactoryShouldRestoreNodesThenRemoveOnUndo() {
            Node n1 = new Node("p1", "code", "P1", new Node.Position(3, 3), Map.of(), null, false, null);
            Node n2 = new Node("p2", "code", "P2", new Node.Position(4, 4), Map.of(), null, false, null);

            CompositeCommand command = CompositeCommand.pasteMultiple(canvas, List.of(n1, n2));

            command.execute();
            command.undo();

            verify(canvas).restoreNode(n1);
            verify(canvas).restoreNode(n2);
            verify(canvas).deleteNodeInternal("p1");
            verify(canvas).deleteNodeInternal("p2");
            assertThat(command.getDescription()).isEqualTo("Paste 2 nodes");
        }
    }
}
