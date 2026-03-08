/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.WorkflowCanvas;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditNodeCommand")
class EditNodeCommandTest {

    @Mock
    private WorkflowCanvas canvas;

    @Test
    @DisplayName("execute should apply new node state")
    void executeShouldApplyNewNodeState() {
        Node oldNode = new Node("n1", "code", "Old", new Node.Position(10, 20), Map.of("k", "v1"), null, false,
                null);
        Node newNode = new Node("n1", "code", "New", new Node.Position(30, 40), Map.of("k", "v2"), null, true,
                "notes");

        EditNodeCommand command = new EditNodeCommand(canvas, oldNode, newNode, "Edit node properties");
        command.execute();

        verify(canvas).applyNodeState(newNode);
        assertThat(command.getDescription()).isEqualTo("Edit node properties");
    }

    @Test
    @DisplayName("undo should restore old node state")
    void undoShouldRestoreOldNodeState() {
        Node oldNode = new Node("n1", "code", "Old", new Node.Position(10, 20), Map.of(), null, false, null);
        Node newNode = new Node("n1", "code", "New", new Node.Position(30, 40), Map.of(), null, true, null);

        EditNodeCommand command = new EditNodeCommand(canvas, oldNode, newNode, "Edit node");
        command.undo();

        verify(canvas).applyNodeState(oldNode);
        assertThat(command.getDescription()).isEqualTo("Edit node");
    }
}
