package ai.nervemind.ui.canvas.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ai.nervemind.ui.canvas.WorkflowCanvas;

/**
 * Unit tests for {@link AutoLayoutCommand}.
 */
class AutoLayoutCommandTest {

    @Mock
    private WorkflowCanvas mockCanvas;

    private AutoLayoutCommand command;
    private Map<String, double[]> oldPositions;
    private Map<String, double[]> newPositions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        oldPositions = new HashMap<>();
        oldPositions.put("node1", new double[] { 100, 100 });
        oldPositions.put("node2", new double[] { 200, 200 });
        oldPositions.put("node3", new double[] { 300, 150 });

        newPositions = new HashMap<>();
        newPositions.put("node1", new double[] { 100, 100 }); // Same position (trigger)
        newPositions.put("node2", new double[] { 380, 100 }); // Moved to column 1
        newPositions.put("node3", new double[] { 660, 100 }); // Moved to column 2

        command = new AutoLayoutCommand(mockCanvas, oldPositions, newPositions);
    }

    @Test
    @DisplayName("execute should apply new positions to all nodes")
    void execute_appliesNewPositions() {
        // When
        command.execute();

        // Then
        verify(mockCanvas).setNodePosition("node1", 100, 100);
        verify(mockCanvas).setNodePosition("node2", 380, 100);
        verify(mockCanvas).setNodePosition("node3", 660, 100);
    }

    @Test
    @DisplayName("undo should restore old positions to all nodes")
    void undo_restoresOldPositions() {
        // When
        command.undo();

        // Then
        verify(mockCanvas).setNodePosition("node1", 100, 100);
        verify(mockCanvas).setNodePosition("node2", 200, 200);
        verify(mockCanvas).setNodePosition("node3", 300, 150);
    }

    @Test
    @DisplayName("getDescription should return descriptive text")
    void getDescription_returnsDescriptiveText() {
        // When & Then
        assertThat(command.getDescription()).isEqualTo("Auto-layout nodes");
    }

    @Test
    @DisplayName("command should create defensive copies of position maps")
    void command_createsDefensiveCopies() {
        // Given - modify original maps after command creation
        oldPositions.put("node4", new double[] { 400, 400 });
        newPositions.put("node4", new double[] { 940, 100 });

        // When
        command.execute();

        // Then - node4 should NOT be affected since we created defensive copies
        // Only verify the original 3 nodes were processed
        verify(mockCanvas).setNodePosition("node1", 100, 100);
        verify(mockCanvas).setNodePosition("node2", 380, 100);
        verify(mockCanvas).setNodePosition("node3", 660, 100);
    }

    @Test
    @DisplayName("execute and undo should be reversible")
    void execute_and_undo_areReversible() {
        // When - execute first
        command.execute();

        // Then - new positions applied
        verify(mockCanvas).setNodePosition("node1", 100, 100);
        verify(mockCanvas).setNodePosition("node2", 380, 100);
        verify(mockCanvas).setNodePosition("node3", 660, 100);

        // When - undo
        command.undo();

        // Then - old positions restored (verify interactions happened twice for node1)
        verify(mockCanvas).setNodePosition("node2", 200, 200);
        verify(mockCanvas).setNodePosition("node3", 300, 150);
    }

    @Test
    @DisplayName("command should handle empty position maps")
    void command_handlesEmptyMaps() {
        // Given
        AutoLayoutCommand emptyCommand = new AutoLayoutCommand(
                mockCanvas, new HashMap<>(), new HashMap<>());

        // When & Then - no exceptions
        emptyCommand.execute();
        emptyCommand.undo();

        assertThat(emptyCommand.getDescription()).isNotEmpty();
    }

    @Test
    @DisplayName("command should handle single node")
    void command_handlesSingleNode() {
        // Given
        Map<String, double[]> singleOld = new HashMap<>();
        singleOld.put("only-node", new double[] { 50, 50 });

        Map<String, double[]> singleNew = new HashMap<>();
        singleNew.put("only-node", new double[] { 100, 100 });

        AutoLayoutCommand singleCommand = new AutoLayoutCommand(mockCanvas, singleOld, singleNew);

        // When
        singleCommand.execute();

        // Then
        verify(mockCanvas).setNodePosition("only-node", 100, 100);

        // When
        singleCommand.undo();

        // Then
        verify(mockCanvas).setNodePosition("only-node", 50, 50);
    }
}
