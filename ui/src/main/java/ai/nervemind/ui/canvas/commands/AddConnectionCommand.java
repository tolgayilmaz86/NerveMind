package ai.nervemind.ui.canvas.commands;

import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for creating a connection between nodes.
 */
public class AddConnectionCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String sourceNodeId;
    private final String targetNodeId;
    private String createdConnectionId;

    /**
     * Creates a new command to add a connection.
     *
     * @param canvas       The workflow canvas
     * @param sourceNodeId The ID of the source node
     * @param targetNodeId The ID of the target node
     */
    public AddConnectionCommand(WorkflowCanvas canvas, String sourceNodeId, String targetNodeId) {
        this.canvas = canvas;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    /**
     * Executes the command by creating the connection on the canvas.
     */
    @Override
    public void execute() {
        createdConnectionId = canvas.createConnectionInternal(sourceNodeId, targetNodeId);
    }

    /**
     * Undoes the command by removing the created connection.
     */
    @Override
    public void undo() {
        if (createdConnectionId != null) {
            canvas.deleteConnectionInternal(createdConnectionId);
        }
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Connect nodes";
    }
}
