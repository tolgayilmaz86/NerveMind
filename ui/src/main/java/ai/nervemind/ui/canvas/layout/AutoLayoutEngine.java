package ai.nervemind.ui.canvas.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;

/**
 * Engine for computing auto-layout positions for workflow nodes.
 * 
 * <p>
 * This class implements an improved layout algorithm with the following
 * features:
 * </p>
 * <ul>
 * <li>BFS-based column assignment with cycle handling (maximum depth)</li>
 * <li>Disconnected nodes placed in a separate column at the end</li>
 * <li>Barycenter heuristic for minimizing edge crossings</li>
 * <li>Vertical centering of columns</li>
 * <li>Dynamic spacing based on node density</li>
 * </ul>
 * 
 * <p>
 * The engine is stateless and can be reused across multiple layout operations.
 * </p>
 */
public class AutoLayoutEngine {

    private final LayoutConfig config;

    /**
     * Create an auto-layout engine with default configuration.
     */
    public AutoLayoutEngine() {
        this(LayoutConfig.defaults());
    }

    /**
     * Create an auto-layout engine with custom configuration.
     *
     * @param config the layout configuration
     */
    public AutoLayoutEngine(LayoutConfig config) {
        this.config = config;
    }

    /**
     * Compute layout positions for all nodes in a workflow.
     *
     * @param workflow the workflow to layout
     * @return map of nodeId to [x, y] position
     */
    public Map<String, double[]> computeLayout(WorkflowDTO workflow) {
        if (workflow.nodes().isEmpty()) {
            return Map.of();
        }

        // Find trigger nodes (nodes with no incoming connections)
        List<Node> triggerNodes = workflow.getTriggerNodes();

        // Calculate node columns using BFS with cycle handling
        Map<String, Integer> nodeColumns = calculateNodeColumns(workflow, triggerNodes);

        // Handle disconnected nodes
        handleDisconnectedNodes(workflow, nodeColumns);

        // Group nodes by column
        Map<Integer, List<Node>> columnGroups = groupNodesByColumn(workflow, nodeColumns);

        // Sort nodes within columns to minimize edge crossings
        sortNodesWithinColumns(workflow, columnGroups);

        // Calculate layout metrics
        LayoutMetrics metrics = calculateMetrics(columnGroups);

        // Compute final positions
        return computePositions(columnGroups, metrics);
    }

    /**
     * Calculate which column each node belongs to using BFS with cycle handling.
     * 
     * <p>
     * Uses maximum depth assignment: if a node is reachable via multiple paths,
     * it gets assigned to the deepest column (longest path from trigger).
     * </p>
     */
    private Map<String, Integer> calculateNodeColumns(WorkflowDTO workflow, List<Node> triggerNodes) {
        Map<String, Integer> nodeColumns = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();

        // Initialize: trigger nodes start at column 0
        for (Node trigger : triggerNodes) {
            nodeColumns.put(trigger.id(), 0);
            queue.add(trigger);
        }

        // BFS with relaxation: update column if we find a longer path
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentColumn = nodeColumns.get(current.id());
            processNodeConnections(workflow, current, currentColumn, nodeColumns, queue);
        }

        return nodeColumns;
    }

    /**
     * Process all outgoing connections from a node and update target columns.
     */
    private void processNodeConnections(WorkflowDTO workflow, Node current, int currentColumn,
            Map<String, Integer> nodeColumns, Queue<Node> queue) {
        for (Connection connection : workflow.connections()) {
            if (connection.sourceNodeId().equals(current.id())) {
                String targetId = connection.targetNodeId();
                int newColumn = currentColumn + 1;

                // Only process if this path gives a deeper column (handles cycles)
                Integer previousColumn = nodeColumns.compute(targetId,
                        (key, oldValue) -> (oldValue == null || oldValue < newColumn) ? newColumn : oldValue);

                if (previousColumn.equals(newColumn)) {
                    Node targetNode = workflow.findNode(targetId);
                    if (targetNode != null) {
                        queue.add(targetNode);
                    }
                }
            }
        }
    }

    /**
     * Handle disconnected nodes by placing them in a separate column at the end.
     */
    private void handleDisconnectedNodes(WorkflowDTO workflow, Map<String, Integer> nodeColumns) {
        int maxColumn = nodeColumns.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);

        int disconnectedColumn = maxColumn + 1;

        for (Node node : workflow.nodes()) {
            if (!nodeColumns.containsKey(node.id())) {
                nodeColumns.put(node.id(), disconnectedColumn);
            }
        }
    }

    /**
     * Group nodes by their calculated column.
     */
    private Map<Integer, List<Node>> groupNodesByColumn(WorkflowDTO workflow, Map<String, Integer> nodeColumns) {
        Map<Integer, List<Node>> columnGroups = new HashMap<>();
        for (Node node : workflow.nodes()) {
            int column = nodeColumns.getOrDefault(node.id(), 0);
            columnGroups.computeIfAbsent(column, k -> new ArrayList<>()).add(node);
        }
        return columnGroups;
    }

    /**
     * Sort nodes within each column to minimize edge crossings using barycenter
     * heuristic.
     */
    private void sortNodesWithinColumns(WorkflowDTO workflow, Map<Integer, List<Node>> columnGroups) {
        Map<String, List<String>> parentMap = buildParentMap(workflow);

        for (int col = 1; col <= columnGroups.size(); col++) {
            List<Node> nodesInColumn = columnGroups.get(col);
            if (nodesInColumn == null || nodesInColumn.size() <= 1) {
                continue;
            }

            List<Node> prevColumn = columnGroups.getOrDefault(col - 1, List.of());
            Map<String, Integer> prevColPositions = new HashMap<>();
            for (int i = 0; i < prevColumn.size(); i++) {
                prevColPositions.put(prevColumn.get(i).id(), i);
            }

            nodesInColumn.sort((a, b) -> {
                double avgA = calculateAverageParentPosition(a.id(), parentMap, prevColPositions);
                double avgB = calculateAverageParentPosition(b.id(), parentMap, prevColPositions);
                return Double.compare(avgA, avgB);
            });
        }
    }

    /**
     * Build a map of nodeId -> list of parent nodeIds.
     */
    private Map<String, List<String>> buildParentMap(WorkflowDTO workflow) {
        Map<String, List<String>> parentMap = new HashMap<>();
        for (Connection conn : workflow.connections()) {
            parentMap.computeIfAbsent(conn.targetNodeId(), k -> new ArrayList<>())
                    .add(conn.sourceNodeId());
        }
        return parentMap;
    }

    /**
     * Calculate average position of parent nodes for barycenter sorting.
     */
    private double calculateAverageParentPosition(String nodeId,
            Map<String, List<String>> parentMap,
            Map<String, Integer> prevColPositions) {
        List<String> parents = parentMap.getOrDefault(nodeId, List.of());
        if (parents.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double sum = 0;
        int count = 0;
        for (String parentId : parents) {
            Integer pos = prevColPositions.get(parentId);
            if (pos != null) {
                sum += pos;
                count++;
            }
        }

        return count > 0 ? sum / count : Double.MAX_VALUE;
    }

    /**
     * Calculate layout metrics based on column structure.
     */
    private LayoutMetrics calculateMetrics(Map<Integer, List<Node>> columnGroups) {
        int maxNodesInColumn = columnGroups.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);

        int totalColumns = columnGroups.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        // Adjust Y spacing for dense columns
        double ySpacing = maxNodesInColumn > config.compactThreshold()
                ? config.compactYSpacing()
                : config.baseYSpacing();

        return new LayoutMetrics(
                config.startX(), config.startY(),
                config.baseXSpacing(), ySpacing,
                maxNodesInColumn, totalColumns);
    }

    /**
     * Compute final positions for all nodes.
     */
    private Map<String, double[]> computePositions(Map<Integer, List<Node>> columnGroups,
            LayoutMetrics metrics) {
        Map<String, double[]> positions = new HashMap<>();
        double maxColumnHeight = metrics.maxNodesInColumn() * metrics.ySpacing();

        for (Map.Entry<Integer, List<Node>> entry : columnGroups.entrySet()) {
            int column = entry.getKey();
            List<Node> nodesInColumn = entry.getValue();

            double columnX = metrics.startX() + column * metrics.xSpacing();
            double columnHeight = nodesInColumn.size() * metrics.ySpacing();
            double columnStartY = metrics.startY() + (maxColumnHeight - columnHeight) / 2;

            double currentY = columnStartY;
            for (Node node : nodesInColumn) {
                positions.put(node.id(), new double[] { columnX, currentY });
                currentY += metrics.ySpacing();
            }
        }

        return positions;
    }

    /**
     * Internal record for layout metrics.
     */
    private record LayoutMetrics(
            double startX, double startY,
            double xSpacing, double ySpacing,
            int maxNodesInColumn, int totalColumns) {
    }
}
