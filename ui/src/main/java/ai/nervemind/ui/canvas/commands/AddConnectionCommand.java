package ai.nervemind.ui.canvas.commands;

import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for creating a connection between nodes.
 */
public class AddConnectionCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String sourceNodeId;
    private final String sourceHandleId;
    private final String targetNodeId;
    private final String targetHandleId;
    private final String connectionId;

    /**
     * Creates a new command to add a connection.
     *
     * @param canvas         The workflow canvas
     * @param sourceNodeId   The ID of the source node
     * @param sourceHandleId The ID of the source handle/port
     * @param targetNodeId   The ID of the target node
     * @param targetHandleId The ID of the target handle/port
     */
    public AddConnectionCommand(WorkflowCanvas canvas, String sourceNodeId, String sourceHandleId,
            String targetNodeId, String targetHandleId) {
        this.canvas = canvas;
        this.sourceNodeId = sourceNodeId;
        this.sourceHandleId = sourceHandleId;
        this.targetNodeId = targetNodeId;
        this.targetHandleId = targetHandleId;
        this.connectionId = java.util.UUID.randomUUID().toString();
    }

    /**
     * Executes the command by creating the connection on the canvas.
     */
    @Override
    public void execute() {
        canvas.createConnectionInternal(connectionId, sourceNodeId, sourceHandleId, targetNodeId,
                targetHandleId);
    }

    /**
     * Undoes the command by removing the created connection.
     */
    @Override
    public void undo() {
        canvas.deleteConnectionInternal(connectionId);
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
