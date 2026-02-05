package ai.nervemind.ui.canvas.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;

/**
 * Unit tests for {@link AutoLayoutEngine}.
 */
class AutoLayoutEngineTest {

    private AutoLayoutEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AutoLayoutEngine();
    }

    @Nested
    @DisplayName("Empty and simple workflows")
    class EmptyAndSimpleWorkflows {

        @Test
        @DisplayName("should return empty map for empty workflow")
        void emptyWorkflow() {
            WorkflowDTO workflow = createWorkflow(List.of(), List.of());

            Map<String, double[]> positions = engine.computeLayout(workflow);

            assertThat(positions).isEmpty();
        }

        @Test
        @DisplayName("should position single node at start position")
        void singleNode() {
            Node node = createNode("node1", "Trigger");
            WorkflowDTO workflow = createWorkflow(List.of(node), List.of());

            Map<String, double[]> positions = engine.computeLayout(workflow);

            assertThat(positions).containsKey("node1");
            double[] pos = positions.get("node1");
            assertThat(pos[0]).isEqualTo(100); // Default startX
            assertThat(pos[1]).isEqualTo(100); // Default startY
        }

        @Test
        @DisplayName("should position two connected nodes in different columns")
        void twoConnectedNodes() {
            Node trigger = createNode("trigger", "Manual Trigger");
            Node action = createNode("action", "HTTP Request");
            Connection conn = createConnection("conn1", "trigger", "action");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, action), List.of(conn));

            Map<String, double[]> positions = engine.computeLayout(workflow);

            assertThat(positions).hasSize(2);
            double[] triggerPos = positions.get("trigger");
            double[] actionPos = positions.get("action");

            // Trigger should be in column 0, action in column 1
            assertThat(triggerPos[0]).isLessThan(actionPos[0]);
            // Both should have same Y since centered
            assertThat(triggerPos[1]).isEqualTo(actionPos[1]);
        }
    }

    @Nested
    @DisplayName("Column assignment")
    class ColumnAssignment {

        @Test
        @DisplayName("should assign columns based on BFS depth")
        void linearChain() {
            // Create a chain: A -> B -> C -> D
            Node a = createNode("a", "Trigger");
            Node b = createNode("b", "Node B");
            Node c = createNode("c", "Node C");
            Node d = createNode("d", "Node D");
            List<Connection> connections = List.of(
                    createConnection("c1", "a", "b"),
                    createConnection("c2", "b", "c"),
                    createConnection("c3", "c", "d"));
            WorkflowDTO workflow = createWorkflow(List.of(a, b, c, d), connections);

            Map<String, double[]> positions = engine.computeLayout(workflow);

            // Each node should be in a separate column, left to right
            double xA = positions.get("a")[0];
            double xB = positions.get("b")[0];
            double xC = positions.get("c")[0];
            double xD = positions.get("d")[0];

            assertThat(xA).isLessThan(xB);
            assertThat(xB).isLessThan(xC);
            assertThat(xC).isLessThan(xD);
        }

        @Test
        @DisplayName("should use maximum depth for nodes with multiple incoming paths")
        void diamondPattern() {
            // Diamond: A -> B, A -> C, B -> D, C -> D
            Node a = createNode("a", "Trigger");
            Node b = createNode("b", "Node B");
            Node c = createNode("c", "Node C");
            Node d = createNode("d", "Node D");
            List<Connection> connections = List.of(
                    createConnection("c1", "a", "b"),
                    createConnection("c2", "a", "c"),
                    createConnection("c3", "b", "d"),
                    createConnection("c4", "c", "d"));
            WorkflowDTO workflow = createWorkflow(List.of(a, b, c, d), connections);

            Map<String, double[]> positions = engine.computeLayout(workflow);

            // D should be at column 2 (A=0, B/C=1, D=2)
            double xA = positions.get("a")[0];
            double xB = positions.get("b")[0];
            double xC = positions.get("c")[0];
            double xD = positions.get("d")[0];

            assertThat(xA).isLessThan(xB); // A is before B
            assertThat(xB).isEqualTo(xC); // B and C in same column
            assertThat(xD).isGreaterThan(xB); // D is after B and C
        }
    }

    @Nested
    @DisplayName("Disconnected nodes")
    class DisconnectedNodes {

        @Test
        @DisplayName("should treat isolated nodes as triggers (column 0)")
        void isolatedNodesAreTriggers() {
            // Isolated nodes (no connections at all) are considered trigger nodes
            // since they have no incoming connections
            Node trigger = createNode("trigger", "Trigger");
            Node action = createNode("action", "Action");
            Node isolated = createNode("isolated", "Isolated");
            Connection conn = createConnection("c1", "trigger", "action");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, action, isolated), List.of(conn));

            Map<String, double[]> positions = engine.computeLayout(workflow);

            double xTrigger = positions.get("trigger")[0];
            double xAction = positions.get("action")[0];
            double xIsolated = positions.get("isolated")[0];

            // Isolated node is in column 0 with trigger (both have no incoming)
            assertThat(xIsolated).isEqualTo(xTrigger);
            // Action is in column 1
            assertThat(xAction).isGreaterThan(xTrigger);
        }

        @Test
        @DisplayName("should group multiple isolated nodes in same column")
        void multipleIsolatedNodes() {
            Node trigger = createNode("trigger", "Trigger");
            Node iso1 = createNode("iso1", "Isolated 1");
            Node iso2 = createNode("iso2", "Isolated 2");
            // No connections - all are trigger nodes
            WorkflowDTO workflow = createWorkflow(List.of(trigger, iso1, iso2), List.of());

            Map<String, double[]> positions = engine.computeLayout(workflow);

            double xTrigger = positions.get("trigger")[0];
            double xIso1 = positions.get("iso1")[0];
            double xIso2 = positions.get("iso2")[0];

            // All nodes in same column (column 0)
            assertThat(xIso1).isEqualTo(xTrigger);
            assertThat(xIso2).isEqualTo(xTrigger);
        }

        @Test
        @DisplayName("should handle truly disconnected subgraph")
        void disconnectedSubgraph() {
            // Main flow: trigger1 -> action1
            // Separate flow: trigger2 -> action2 (no connection to main)
            Node trigger1 = createNode("trigger1", "Trigger 1");
            Node action1 = createNode("action1", "Action 1");
            Node trigger2 = createNode("trigger2", "Trigger 2");
            Node action2 = createNode("action2", "Action 2");
            List<Connection> connections = List.of(
                    createConnection("c1", "trigger1", "action1"),
                    createConnection("c2", "trigger2", "action2"));
            WorkflowDTO workflow = createWorkflow(
                    List.of(trigger1, action1, trigger2, action2), connections);

            Map<String, double[]> positions = engine.computeLayout(workflow);

            // Both trigger nodes in column 0
            assertThat(positions.get("trigger1")[0]).isEqualTo(positions.get("trigger2")[0]);
            // Both action nodes in column 1
            assertThat(positions.get("action1")[0]).isEqualTo(positions.get("action2")[0]);
            // Actions after triggers
            assertThat(positions.get("action1")[0]).isGreaterThan(positions.get("trigger1")[0]);
        }
    }

    @Nested
    @DisplayName("Vertical centering")
    class VerticalCentering {

        @Test
        @DisplayName("should center single-node columns vertically")
        void centersSingleNodeColumns() {
            // Create: trigger -> (node1, node2) pattern (1 to 2)
            Node trigger = createNode("trigger", "Trigger");
            Node node1 = createNode("node1", "Node 1");
            Node node2 = createNode("node2", "Node 2");
            List<Connection> connections = List.of(
                    createConnection("c1", "trigger", "node1"),
                    createConnection("c2", "trigger", "node2"));
            WorkflowDTO workflow = createWorkflow(List.of(trigger, node1, node2), connections);

            Map<String, double[]> positions = engine.computeLayout(workflow);

            double yTrigger = positions.get("trigger")[1];
            double yNode1 = positions.get("node1")[1];
            double yNode2 = positions.get("node2")[1];

            // Trigger should be centered between node1 and node2
            double avgY = (yNode1 + yNode2) / 2;
            assertThat(yTrigger).isEqualTo(avgY);
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("should use custom configuration")
        void customConfig() {
            LayoutConfig config = LayoutConfig.builder()
                    .startX(200)
                    .startY(150)
                    .baseXSpacing(300)
                    .build();
            AutoLayoutEngine customEngine = new AutoLayoutEngine(config);

            Node node = createNode("node1", "Trigger");
            WorkflowDTO workflow = createWorkflow(List.of(node), List.of());

            Map<String, double[]> positions = customEngine.computeLayout(workflow);

            double[] pos = positions.get("node1");
            assertThat(pos[0]).isEqualTo(200);
            assertThat(pos[1]).isEqualTo(150);
        }

        @Test
        @DisplayName("should use compact config for dense layouts")
        void compactConfig() {
            AutoLayoutEngine compactEngine = new AutoLayoutEngine(LayoutConfig.compact());

            Node trigger = createNode("trigger", "Trigger");
            Node action = createNode("action", "Action");
            Connection conn = createConnection("c1", "trigger", "action");
            WorkflowDTO workflow = createWorkflow(List.of(trigger, action), List.of(conn));

            Map<String, double[]> positions = compactEngine.computeLayout(workflow);

            double xSpacing = positions.get("action")[0] - positions.get("trigger")[0];
            // Compact config uses 220 x-spacing
            assertThat(xSpacing).isEqualTo(220);
        }
    }

    @Nested
    @DisplayName("Cycle handling")
    class CycleHandling {

        @Test
        @DisplayName("should handle simple cycle without infinite loop")
        void simpleCycle() {
            // A -> B -> C -> A (cycle back to A)
            Node a = createNode("a", "Node A");
            Node b = createNode("b", "Node B");
            Node c = createNode("c", "Node C");
            List<Connection> connections = List.of(
                    createConnection("c1", "a", "b"),
                    createConnection("c2", "b", "c"),
                    createConnection("c3", "c", "a")); // Cycle
            WorkflowDTO workflow = createWorkflow(List.of(a, b, c), connections);

            // Should complete without hanging
            Map<String, double[]> positions = engine.computeLayout(workflow);

            assertThat(positions).hasSize(3);
        }
    }

    // ===== Helper methods =====

    private Node createNode(String id, String name) {
        return new Node(id, "test-type", name, new Node.Position(0, 0), Map.of(), null, false, null);
    }

    private Connection createConnection(String id, String sourceId, String targetId) {
        return Connection.simple(id, sourceId, targetId);
    }

    private WorkflowDTO createWorkflow(List<Node> nodes, List<Connection> connections) {
        return new WorkflowDTO(
                null,
                "Test Workflow",
                "Test",
                new ArrayList<>(nodes),
                new ArrayList<>(connections),
                Map.of(),
                false,
                TriggerType.MANUAL,
                null,
                Instant.now(),
                Instant.now(),
                null,
                1);
    }
}
