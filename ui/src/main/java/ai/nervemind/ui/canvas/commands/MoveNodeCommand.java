package ai.nervemind.ui.canvas.commands;

import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for moving a node on the canvas.
 */
public class MoveNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeId;
    private final double oldX;
    private final double oldY;
    private final double newX;
    private final double newY;

    /**
     * Creates a new command to move a node.
     *
     * @param canvas The workflow canvas
     * @param nodeId The ID of the node to move
     * @param oldX   The original X coordinate
     * @param oldY   The original Y coordinate
     * @param newX   The new X coordinate
     * @param newY   The new Y coordinate
     */
    public MoveNodeCommand(WorkflowCanvas canvas, String nodeId,
            double oldX, double oldY, double newX, double newY) {
        this.canvas = canvas;
        this.nodeId = nodeId;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    /**
     * Executes the command by moving the node to the new position.
     */
    @Override
    public void execute() {
        canvas.setNodePosition(nodeId, newX, newY);
    }

    /**
     * Undoes the command by moving the node back to the old position.
     */
    @Override
    public void undo() {
        canvas.setNodePosition(nodeId, oldX, oldY);
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Move node";
    }
}
