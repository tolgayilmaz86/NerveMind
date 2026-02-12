package ai.nervemind.ui.canvas.commands;

import java.util.List;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Composite command that groups multiple sub-commands into a single undoable
 * operation.
 * Used for bulk delete, paste, cut, etc. where multiple nodes/connections
 * are affected at once.
 */
public class CompositeCommand implements CanvasCommand {

    private final List<CanvasCommand> commands;
    private final String description;

    /**
     * Creates a composite command.
     *
     * @param commands    the sub-commands to execute in order
     * @param description human-readable description
     */
    public CompositeCommand(List<CanvasCommand> commands, String description) {
        this.commands = List.copyOf(commands);
        this.description = description;
    }

    @Override
    public void execute() {
        for (CanvasCommand command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    // ===== Factory methods for common composite operations =====

    /**
     * Creates a composite command to delete multiple nodes and their connections.
     *
     * @param canvas      the canvas
     * @param nodes       nodes to delete
     * @param connections connections to delete (those involving the deleted nodes)
     * @return the composite command
     */
    public static CompositeCommand deleteMultiple(
            WorkflowCanvas canvas, List<Node> nodes, List<Connection> connections) {

        List<CanvasCommand> cmds = new java.util.ArrayList<>();

        // Delete connections first, then nodes (reverse order for undo)
        for (Connection conn : connections) {
            cmds.add(new DeleteConnectionCommand(canvas, conn.id()));
        }
        for (Node node : nodes) {
            cmds.add(new DeleteNodeCommand(canvas, node.id()));
        }

        int count = nodes.size();
        String desc = "Delete " + count + " node" + (count != 1 ? "s" : "");
        return new CompositeCommand(cmds, desc);
    }

    /**
     * Creates a composite command to paste multiple nodes.
     *
     * @param canvas the canvas
     * @param nodes  the nodes being pasted (with their new IDs already assigned)
     * @return the composite command
     */
    public static CompositeCommand pasteMultiple(WorkflowCanvas canvas, List<Node> nodes) {
        List<CanvasCommand> cmds = new java.util.ArrayList<>();
        for (Node node : nodes) {
            cmds.add(new AddNodeCommand(canvas, node));
        }
        int count = nodes.size();
        String desc = "Paste " + count + " node" + (count != 1 ? "s" : "");
        return new CompositeCommand(cmds, desc);
    }
}
