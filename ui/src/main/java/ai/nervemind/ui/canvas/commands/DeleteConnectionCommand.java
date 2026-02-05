package ai.nervemind.ui.canvas.commands;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for deleting a connection between nodes.
 */
public class DeleteConnectionCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String connectionId;
    private Connection deletedConnection;

    /**
     * Creates a new command to delete a connection.
     *
     * @param canvas       The workflow canvas
     * @param connectionId The ID of the connection to delete
     */
    public DeleteConnectionCommand(WorkflowCanvas canvas, String connectionId) {
        this.canvas = canvas;
        this.connectionId = connectionId;
    }

    /**
     * Executes the command by removing the connection from the canvas.
     */
    @Override
    public void execute() {
        deletedConnection = canvas.getConnectionById(connectionId);
        if (deletedConnection != null) {
            canvas.deleteConnectionInternal(connectionId);
        }
    }

    /**
     * Undoes the command by restoring the deleted connection.
     */
    @Override
    public void undo() {
        if (deletedConnection != null) {
            canvas.restoreConnection(deletedConnection);
        }
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Delete connection";
    }
}
