/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UndoRedoManager")
class UndoRedoManagerTest {

    @Test
    @DisplayName("execute should push undo stack and clear redo stack")
    void executeShouldPushUndoAndClearRedo() {
        UndoRedoManager manager = new UndoRedoManager();
        TestCommand one = new TestCommand("one");
        TestCommand two = new TestCommand("two");

        manager.executeCommand(one);
        manager.undo();
        assertThat(manager.canRedo()).isTrue();

        manager.executeCommand(two);

        assertThat(one.executeCalls).isEqualTo(1);
        assertThat(two.executeCalls).isEqualTo(1);
        assertThat(manager.getUndoCount()).isEqualTo(1);
        assertThat(manager.getRedoCount()).isZero();
        assertThat(manager.getUndoDescription()).isEqualTo("two");
        assertThat(manager.getRedoDescription()).isNull();
    }

    @Test
    @DisplayName("undo and redo should move command between stacks")
    void undoAndRedoShouldMoveCommandBetweenStacks() {
        UndoRedoManager manager = new UndoRedoManager();
        TestCommand cmd = new TestCommand("move");

        manager.executeCommand(cmd);
        manager.undo();

        assertThat(cmd.undoCalls).isEqualTo(1);
        assertThat(manager.getUndoCount()).isZero();
        assertThat(manager.getRedoCount()).isEqualTo(1);
        assertThat(manager.getRedoDescription()).isEqualTo("move");

        manager.redo();

        assertThat(cmd.executeCalls).isEqualTo(2);
        assertThat(manager.getUndoCount()).isEqualTo(1);
        assertThat(manager.getRedoCount()).isZero();
        assertThat(manager.getUndoDescription()).isEqualTo("move");
    }

    @Test
    @DisplayName("undo and redo should be no-op when no history exists")
    void undoAndRedoShouldBeNoOpWhenNoHistoryExists() {
        UndoRedoManager manager = new UndoRedoManager();

        manager.undo();
        manager.redo();

        assertThat(manager.canUndo()).isFalse();
        assertThat(manager.canRedo()).isFalse();
        assertThat(manager.getUndoDescription()).isNull();
        assertThat(manager.getRedoDescription()).isNull();
    }

    @Test
    @DisplayName("clear should reset both stacks and descriptions")
    void clearShouldResetBothStacksAndDescriptions() {
        UndoRedoManager manager = new UndoRedoManager();
        manager.executeCommand(new TestCommand("one"));
        manager.executeCommand(new TestCommand("two"));

        manager.clear();

        assertThat(manager.getUndoCount()).isZero();
        assertThat(manager.getRedoCount()).isZero();
        assertThat(manager.getUndoDescription()).isNull();
        assertThat(manager.getRedoDescription()).isNull();
    }

    @Test
    @DisplayName("history should be trimmed to max size")
    void historyShouldBeTrimmedToMaxSize() {
        UndoRedoManager manager = new UndoRedoManager();
        List<TestCommand> commands = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            TestCommand cmd = new TestCommand("cmd-" + i);
            commands.add(cmd);
            manager.executeCommand(cmd);
        }

        assertThat(manager.getUndoCount()).isEqualTo(50);
        assertThat(manager.getUndoDescription()).isEqualTo("cmd-59");

        while (manager.canUndo()) {
            manager.undo();
        }

        for (int i = 0; i < 10; i++) {
            assertThat(commands.get(i).undoCalls)
                    .as("Command dropped from history should not be undoable: cmd-%d", i)
                    .isZero();
        }
        for (int i = 10; i < 60; i++) {
            assertThat(commands.get(i).undoCalls)
                    .as("Command retained in history should be undoable: cmd-%d", i)
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("state callback should be invoked on each state-changing operation")
    void stateCallbackShouldBeInvokedOnEachStateChangeOperation() {
        UndoRedoManager manager = new UndoRedoManager();
        AtomicInteger callbackCount = new AtomicInteger();
        manager.setOnStateChanged(callbackCount::incrementAndGet);

        manager.executeCommand(new TestCommand("one"));
        manager.undo();
        manager.redo();
        manager.clear();

        assertThat(callbackCount.get()).isEqualTo(4);
    }

    private static final class TestCommand implements CanvasCommand {
        private final String description;
        private int executeCalls;
        private int undoCalls;

        private TestCommand(String description) {
            this.description = description;
        }

        @Override
        public void execute() {
            executeCalls++;
        }

        @Override
        public void undo() {
            undoCalls++;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}
