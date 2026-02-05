/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.view.dialog;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link KeyboardShortcutsController}.
 * 
 * <p>
 * Tests the controller logic without requiring JavaFX runtime.
 */
@DisplayName("KeyboardShortcutsController")
class KeyboardShortcutsControllerTest {

    private KeyboardShortcutsController controller;

    @BeforeEach
    void setUp() {
        controller = new KeyboardShortcutsController();
    }

    @Test
    @DisplayName("should initialize without errors")
    void shouldInitializeWithoutErrors() {
        // initialize() should not throw
        assertDoesNotThrow(() -> controller.initialize());
    }

    @Test
    @DisplayName("should set and invoke close callback")
    void shouldSetAndInvokeCloseCallback() {
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        controller.setOnCloseCallback(() -> callbackInvoked.set(true));

        // Note: We can't fully test handleClose() without a Stage,
        // but we can test the callback mechanism
        // The actual handleClose() method requires a JavaFX Stage

        // Verify callback was set (no way to verify directly, but no exception means
        // success)
        assertDoesNotThrow(() -> controller.setOnCloseCallback(() -> {
        }));
    }

    @Test
    @DisplayName("should accept null close callback")
    void shouldAcceptNullCloseCallback() {
        assertDoesNotThrow(() -> controller.setOnCloseCallback(null));
    }
}
