package ai.nervemind.ui.canvas.commands;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for deleting a node from the canvas.
 */
public class DeleteNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeId;
    private Node deletedNode;

    /**
     * Creates a new command to delete a node.
     *
     * @param canvas The workflow canvas
     * @param nodeId The ID of the node to delete
     */
    public DeleteNodeCommand(WorkflowCanvas canvas, String nodeId) {
        this.canvas = canvas;
        this.nodeId = nodeId;
    }

    /**
     * Executes the command by removing the node from the canvas.
     */
    @Override
    public void execute() {
        deletedNode = canvas.getNodeById(nodeId);
        if (deletedNode != null) {
            canvas.deleteNodeInternal(nodeId);
        }
    }

    /**
     * Undoes the command by restoring the deleted node.
     */
    @Override
    public void undo() {
        if (deletedNode != null) {
            canvas.restoreNode(deletedNode);
        }
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Delete " + (deletedNode != null ? deletedNode.name() : "node");
    }
}
