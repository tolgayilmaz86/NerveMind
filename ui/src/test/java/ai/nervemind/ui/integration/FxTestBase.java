/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;

import javafx.application.Platform;

/**
 * Base class for JavaFX integration tests that require the FX toolkit.
 * 
 * <p>
 * Initializes the JavaFX Platform before tests run. Use this base class
 * when testing code that requires actual JavaFX UI components (not just
 * properties from javafx.beans.*).
 * 
 * <p>
 * For ViewModel tests, prefer
 * {@link ai.nervemind.ui.viewmodel.ViewModelTestBase}
 * which doesn't require the JavaFX runtime.
 * 
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * class MyViewIntegrationTest extends FxTestBase {
 * 
 *     @Test
 *     void testButtonClick() throws Exception {
 *         runOnFxThread(() -> {
 *             Button button = new Button("Click me");
 *             button.fire();
 *             // assertions...
 *         });
 *     }
 * }
 * }</pre>
 */
public abstract class FxTestBase {

    private static boolean fxInitialized = false;

    /**
     * Initialize the JavaFX Platform once for all tests.
     * This is idempotent - calling multiple times is safe.
     */
    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!fxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);

            // Try to start the platform
            try {
                Platform.startup(() -> {
                    fxInitialized = true;
                    latch.countDown();
                });
            } catch (IllegalStateException _) {
                // Platform already initialized
                fxInitialized = true;
                latch.countDown();
            }

            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX Platform failed to initialize");
            }
        }
    }

    /**
     * Run code on the JavaFX Application Thread and wait for completion.
     * 
     * @param runnable The code to run on the FX thread
     * @throws InterruptedException if the wait is interrupted
     */
    protected void runOnFxThread(Runnable runnable) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("FX thread operation timed out");
            }
        }
    }

    /**
     * Run code on the JavaFX Application Thread, wait for completion,
     * and return a result.
     * 
     * @param <T>      The result type
     * @param callable The code to run
     * @return The result from the callable
     * @throws Exception if the callable throws or timeout occurs
     */
    protected <T> T runOnFxThreadAndWait(java.util.concurrent.Callable<T> callable)
            throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }

        CountDownLatch latch = new CountDownLatch(1);
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        Exception[] error = new Exception[1];

        Platform.runLater(() -> {
            try {
                result[0] = callable.call();
            } catch (Exception e) {
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX thread operation timed out");
        }

        if (error[0] != null) {
            throw error[0];
        }

        return result[0];
    }

    /**
     * Wait for the JavaFX event queue to empty.
     * Useful when you need to wait for UI updates to complete.
     * 
     * @throws InterruptedException if the wait is interrupted
     */
    protected void waitForFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("FX event queue wait timed out");
        }
    }
}
