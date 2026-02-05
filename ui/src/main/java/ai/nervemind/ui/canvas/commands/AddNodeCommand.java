package ai.nervemind.ui.canvas.commands;

import ai.nervemind.common.domain.Node;
import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for adding a node to the canvas.
 */
public class AddNodeCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final String nodeType;
    private final double x;
    private final double y;
    private String createdNodeId;

    /**
     * Creates a new command to add a node.
     *
     * @param canvas   The workflow canvas
     * @param nodeType The type of node to create
     * @param x        The X coordinate
     * @param y        The Y coordinate
     */
    public AddNodeCommand(WorkflowCanvas canvas, String nodeType, double x, double y) {
        this.canvas = canvas;
        this.nodeType = nodeType;
        this.x = x;
        this.y = y;
    }

    /**
     * Executes the command by creating the node on the canvas.
     */
    @Override
    public void execute() {
        Node node = canvas.createNodeInternal(nodeType, x, y);
        if (node != null) {
            createdNodeId = node.id();
        }
    }

    /**
     * Undoes the command by removing the created node.
     */
    @Override
    public void undo() {
        if (createdNodeId != null) {
            canvas.deleteNodeInternal(createdNodeId);
        }
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Add " + nodeType.replaceAll("([A-Z])", " $1").trim();
    }
}
