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
    private final Node prebuiltNode;
    private final String nodeId;

    /**
     * Creates a new command to add a node by type and position.
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
        this.prebuiltNode = null;
        this.nodeId = java.util.UUID.randomUUID().toString();
    }

    /**
     * Creates a new command to add a pre-built node (for paste/duplicate).
     *
     * @param canvas The workflow canvas
     * @param node   The fully constructed node to add
     */
    public AddNodeCommand(WorkflowCanvas canvas, Node node) {
        this.canvas = canvas;
        this.prebuiltNode = node;
        this.nodeType = node.type();
        this.x = node.position().x();
        this.y = node.position().y();
        this.nodeId = node.id();
    }

    /**
     * Executes the command by creating the node on the canvas.
     */
    @Override
    public void execute() {
        if (prebuiltNode != null) {
            canvas.restoreNode(prebuiltNode);
        } else {
            canvas.createNodeInternal(nodeId, nodeType, x, y);
        }
    }

    /**
     * Undoes the command by removing the created node.
     */
    @Override
    public void undo() {
        canvas.deleteNodeInternal(nodeId);
    }

    /**
     * Gets the ID of the node that was created by this command.
     *
     * @return the created node ID
     */
    public String getCreatedNodeId() {
        return nodeId;
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        if (prebuiltNode != null) {
            return "Add " + prebuiltNode.name();
        }
        return "Add " + nodeType.replaceAll("([A-Z])", " $1").trim();
    }
}
