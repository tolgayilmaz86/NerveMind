package ai.nervemind.ui.canvas.commands;

import java.util.Map;

import ai.nervemind.ui.canvas.CanvasCommand;
import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Command for auto-layout operation that allows undo/redo of all node
 * positions.
 */
public class AutoLayoutCommand implements CanvasCommand {

    private final WorkflowCanvas canvas;
    private final Map<String, double[]> oldPositions;
    private final Map<String, double[]> newPositions;

    /**
     * Create an auto-layout command.
     *
     * @param canvas       the workflow canvas
     * @param oldPositions map of nodeId to [x, y] positions before layout
     * @param newPositions map of nodeId to [x, y] positions after layout
     */
    public AutoLayoutCommand(WorkflowCanvas canvas,
            Map<String, double[]> oldPositions,
            Map<String, double[]> newPositions) {
        this.canvas = canvas;
        this.oldPositions = Map.copyOf(oldPositions);
        this.newPositions = Map.copyOf(newPositions);
    }

    /**
     * Executes the command by applying the new node positions.
     */
    @Override
    public void execute() {
        applyPositions(newPositions);
    }

    /**
     * Undoes the command by restoring the old node positions.
     */
    @Override
    public void undo() {
        applyPositions(oldPositions);
    }

    private void applyPositions(Map<String, double[]> positions) {
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            String nodeId = entry.getKey();
            double[] pos = entry.getValue();
            canvas.setNodePosition(nodeId, pos[0], pos[1]);
        }
    }

    /**
     * Gets a readable description of the command.
     *
     * @return The command description
     */
    @Override
    public String getDescription() {
        return "Auto-layout nodes";
    }
}
