package ai.nervemind.ui.canvas.commands;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for editing a node's properties (name, parameters, icon, disabled
 * state, etc.).
 * Captures the full node state before and after the edit so undo restores the
 * previous state.
 */
public class EditNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final Node oldNode;
    private final Node newNode;
    private final String description;

    /**
     * Creates a new edit-node command.
     *
     * @param canvas      the workflow canvas
     * @param oldNode     the node state before editing
     * @param newNode     the node state after editing
     * @param description human-readable description of the edit
     */
    public EditNodeCommand(WorkflowCanvas canvas, Node oldNode, Node newNode, String description) {
        this.canvas = canvas;
        this.oldNode = oldNode;
        this.newNode = newNode;
        this.description = description;
    }

    @Override
    public void execute() {
        canvas.applyNodeState(newNode);
    }

    @Override
    public void undo() {
        canvas.applyNodeState(oldNode);
    }

    @Override
    public String getDescription() {
        return description;
    }
}
