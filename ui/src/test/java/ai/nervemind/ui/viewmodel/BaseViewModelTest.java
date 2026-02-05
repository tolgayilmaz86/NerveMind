/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BaseViewModel.
 * 
 * <p>
 * Verifies the base functionality that all ViewModels inherit:
 * loading state, dirty state, error handling.
 */
class BaseViewModelTest extends ViewModelTestBase {

    private static final String SOME_ERROR = "Some error";
    private TestableBaseViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new TestableBaseViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("Should not be loading initially")
        void shouldNotBeLoadingInitially() {
            assertFalse(viewModel.isLoading());
            assertFalse(viewModel.loadingProperty().get());
        }

        @Test
        @DisplayName("Should not be dirty initially")
        void shouldNotBeDirtyInitially() {
            assertFalse(viewModel.isDirty());
            assertFalse(viewModel.dirtyProperty().get());
        }

        @Test
        @DisplayName("Should not have error initially")
        void shouldNotHaveErrorInitially() {
            assertFalse(viewModel.hasError());
            assertFalse(viewModel.hasErrorProperty().get());
            assertNull(viewModel.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Loading State")
    class LoadingState {

        @Test
        @DisplayName("Should track loading state changes")
        void shouldTrackLoadingStateChanges() {
            PropertyChangeTracker<Boolean> tracker = new PropertyChangeTracker<>();
            viewModel.loadingProperty().addListener(tracker::onChanged);

            viewModel.setLoading(true);

            assertTrue(viewModel.isLoading());
            assertEquals(1, tracker.getChangeCount());
            assertFalse(tracker.getLastOldValue());
            assertTrue(tracker.getLastNewValue());
        }

        @Test
        @DisplayName("Should reset loading state")
        void shouldResetLoadingState() {
            viewModel.setLoading(true);
            assertTrue(viewModel.isLoading());

            viewModel.setLoading(false);

            assertFalse(viewModel.isLoading());
        }
    }

    @Nested
    @DisplayName("Dirty State")
    class DirtyState {

        @Test
        @DisplayName("Should mark as dirty")
        void shouldMarkAsDirty() {
            PropertyChangeTracker<Boolean> tracker = new PropertyChangeTracker<>();
            viewModel.dirtyProperty().addListener(tracker::onChanged);

            viewModel.callMarkDirty();

            assertTrue(viewModel.isDirty());
            assertEquals(1, tracker.getChangeCount());
        }

        @Test
        @DisplayName("Should clear dirty state")
        void shouldClearDirtyState() {
            viewModel.callMarkDirty();
            assertTrue(viewModel.isDirty());

            viewModel.callClearDirty();

            assertFalse(viewModel.isDirty());
        }

        @Test
        @DisplayName("Should support direct dirty property setting")
        void shouldSupportDirectDirtyPropertySetting() {
            viewModel.setDirty(true);
            assertTrue(viewModel.isDirty());

            viewModel.setDirty(false);
            assertFalse(viewModel.isDirty());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should set error message")
        void shouldSetErrorMessage() {
            String errorMessage = "Test error message";

            viewModel.setErrorMessage(errorMessage);

            assertTrue(viewModel.hasError());
            assertEquals(errorMessage, viewModel.getErrorMessage());
        }

        @Test
        @DisplayName("Should clear error with clearError")
        void shouldClearErrorWithClearError() {
            viewModel.setErrorMessage(SOME_ERROR);
            assertTrue(viewModel.hasError());

            viewModel.clearError();

            assertFalse(viewModel.hasError());
            assertNull(viewModel.getErrorMessage());
        }

        @Test
        @DisplayName("Should clear error when setting null message")
        void shouldClearErrorWhenSettingNullMessage() {
            viewModel.setErrorMessage(SOME_ERROR);

            viewModel.setErrorMessage(null);

            assertFalse(viewModel.hasError());
            assertNull(viewModel.getErrorMessage());
        }

        @Test
        @DisplayName("Should clear error when setting blank message")
        void shouldClearErrorWhenSettingBlankMessage() {
            viewModel.setErrorMessage(SOME_ERROR);

            viewModel.setErrorMessage("   ");

            assertFalse(viewModel.hasError());
        }

        @Test
        @DisplayName("hasError should reflect error state")
        void hasErrorShouldReflectErrorState() {
            assertFalse(viewModel.hasError());

            viewModel.setErrorMessage("Error");
            assertTrue(viewModel.hasError());

            viewModel.setErrorMessage(null);
            assertFalse(viewModel.hasError());
        }
    }

    @Nested
    @DisplayName("Lifecycle Methods")
    class LifecycleMethods {

        @Test
        @DisplayName("Should call initialize")
        void shouldCallInitialize() {
            viewModel.initialize();

            assertTrue(viewModel.wasInitializeCalled());
        }

        @Test
        @DisplayName("Should call dispose")
        void shouldCallDispose() {
            viewModel.dispose();

            assertTrue(viewModel.wasDisposeCalled());
        }
    }

    @Nested
    @DisplayName("withLoading Helper")
    class WithLoadingHelper {

        @Test
        @DisplayName("Should manage loading state during action")
        void shouldManageLoadingStateDuringAction() {
            boolean[] wasLoadingDuringAction = { false };

            viewModel.callWithLoading(() -> wasLoadingDuringAction[0] = viewModel.isLoading());

            assertTrue(wasLoadingDuringAction[0], "Should be loading during action");
            assertFalse(viewModel.isLoading(), "Should not be loading after action");
        }

        @Test
        @DisplayName("Should clear error before action")
        void shouldClearErrorBeforeAction() {
            viewModel.setErrorMessage("Previous error");

            viewModel.callWithLoading(() -> {
                // Action
            });

            assertFalse(viewModel.hasError());
        }

        @Test
        @DisplayName("Should capture exception and set error message")
        void shouldCaptureExceptionAndSetErrorMessage() {
            viewModel.callWithLoading(() -> {
                throw new RuntimeException("Test exception");
            });

            assertTrue(viewModel.hasError());
            assertEquals("Test exception", viewModel.getErrorMessage());
            assertFalse(viewModel.isLoading(), "Should not be loading after exception");
        }
    }

    /**
     * Concrete implementation of BaseViewModel for testing.
     */
    private static class TestableBaseViewModel extends BaseViewModel {
        private boolean initializeCalled = false;
        private boolean disposeCalled = false;

        @Override
        public void initialize() {
            super.initialize();
            initializeCalled = true;
        }

        @Override
        public void dispose() {
            super.dispose();
            disposeCalled = true;
        }

        // Expose protected methods for testing
        public void callMarkDirty() {
            markDirty();
        }

        public void callClearDirty() {
            clearDirty();
        }

        public void callWithLoading(Runnable action) {
            withLoading(action);
        }

        public boolean wasInitializeCalled() {
            return initializeCalled;
        }

        public boolean wasDisposeCalled() {
            return disposeCalled;
        }
    }
}
