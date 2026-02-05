/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BaseDialogViewModel.
 * 
 * <p>
 * Verifies dialog-specific functionality: validation, confirmation,
 * and result handling.
 */
class BaseDialogViewModelTest extends ViewModelTestBase {

    private TestDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new TestDialogViewModel();
    }

    @Test
    void testInitialState() {
        assertFalse(viewModel.confirmedProperty().get(), "Should not be confirmed initially");
        assertTrue(viewModel.validProperty().get(), "Should be valid initially (no validators)");
        assertNull(viewModel.getResult(), "Result should be null initially");
    }

    @Test
    void testConfirmWithValidInput() {
        viewModel.setValidationResult(true);

        boolean confirmed = viewModel.confirm();

        assertTrue(confirmed);
        assertTrue(viewModel.confirmedProperty().get());
        assertNotNull(viewModel.getResult());
    }

    @Test
    void testConfirmWithInvalidInput() {
        viewModel.setValidationResult(false);

        boolean confirmed = viewModel.confirm();

        assertFalse(confirmed);
        assertFalse(viewModel.confirmedProperty().get());
        assertNull(viewModel.getResult());
    }

    @Test
    void testCancel() {
        viewModel.setValidationResult(true);
        viewModel.confirm(); // First confirm

        viewModel.cancel();

        assertFalse(viewModel.confirmedProperty().get());
        // Note: result is not cleared by cancel(), use reset() for that
    }

    @Test
    void testValidateTriggersValidation() {
        PropertyChangeTracker<Boolean> tracker = new PropertyChangeTracker<>();
        viewModel.validProperty().addListener(tracker::onChanged);

        viewModel.setValidationResult(false);
        viewModel.validate();

        assertFalse(viewModel.validProperty().get());
    }

    @Test
    void testBuildResultOnlyCalledWhenValid() {
        viewModel.setValidationResult(false);
        viewModel.confirm();

        assertEquals(0, viewModel.getBuildResultCallCount(),
                "buildResult should not be called when invalid");

        viewModel.setValidationResult(true);
        viewModel.confirm();

        assertEquals(1, viewModel.getBuildResultCallCount(),
                "buildResult should be called once when valid");
    }

    @Test
    void testOnValidatedCallbackCalled() {
        viewModel.validate();

        assertTrue(viewModel.isOnValidatedCalled(),
                "onValidated should be called after validation");
    }

    /**
     * Concrete implementation of BaseDialogViewModel for testing.
     */
    private static class TestDialogViewModel extends BaseDialogViewModel<String> {
        private boolean validationResult = true;
        private int buildResultCallCount = 0;
        private boolean onValidatedCalled = false;

        public void setValidationResult(boolean result) {
            this.validationResult = result;
        }

        public int getBuildResultCallCount() {
            return buildResultCallCount;
        }

        public boolean isOnValidatedCalled() {
            return onValidatedCalled;
        }

        @Override
        public boolean validate() {
            setValid(validationResult);
            onValidatedCalled = true;
            return validationResult;
        }

        @Override
        protected void buildResult() {
            buildResultCallCount++;
            setResult("TestResult");
        }
    }
}
