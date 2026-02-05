/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for WorkflowSettingsViewModel.
 */
@DisplayName("WorkflowSettingsViewModel")
class WorkflowSettingsViewModelTest extends ViewModelTestBase {

    private static final String TEST_NAME = "Test Workflow";
    private static final String TEST_DESCRIPTION = "A test workflow description";

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should initialize with provided values")
        void shouldInitializeWithProvidedValues() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, TEST_DESCRIPTION, true);

            assertThat(viewModel.getName()).isEqualTo(TEST_NAME);
            assertThat(viewModel.getDescription()).isEqualTo(TEST_DESCRIPTION);
            assertThat(viewModel.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should handle null name")
        void shouldHandleNullName() {
            var viewModel = new WorkflowSettingsViewModel(null, TEST_DESCRIPTION, true);

            assertThat(viewModel.getName()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null description")
        void shouldHandleNullDescription() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, null, true);

            assertThat(viewModel.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("Should initialize with inactive state")
        void shouldInitializeWithInactiveState() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, TEST_DESCRIPTION, false);

            assertThat(viewModel.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("Should update name property")
        void shouldUpdateNameProperty() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, TEST_DESCRIPTION, true);

            viewModel.setName("Updated Name");
            assertThat(viewModel.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should update description property")
        void shouldUpdateDescriptionProperty() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, TEST_DESCRIPTION, true);

            viewModel.setDescription("Updated description");
            assertThat(viewModel.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should toggle active property")
        void shouldToggleActiveProperty() {
            var viewModel = new WorkflowSettingsViewModel(TEST_NAME, TEST_DESCRIPTION, true);

            viewModel.setActive(false);
            assertThat(viewModel.isActive()).isFalse();

            viewModel.setActive(true);
            assertThat(viewModel.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should always be valid (name defaults to Untitled)")
        void shouldAlwaysBeValid() {
            var viewModel = new WorkflowSettingsViewModel("", "", true);

            assertThat(viewModel.validate()).isTrue();
            assertThat(viewModel.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Result Building")
    class ResultBuilding {

        @Test
        @DisplayName("Should build result with trimmed values")
        void shouldBuildResultWithTrimmedValues() {
            var viewModel = new WorkflowSettingsViewModel("  My Workflow  ", "  Description  ", true);

            viewModel.confirm();
            var result = viewModel.getResult();

            assertThat(result.name()).isEqualTo("My Workflow");
            assertThat(result.description()).isEqualTo("Description");
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should default to Untitled Workflow when name is empty")
        void shouldDefaultToUntitledWorkflow() {
            var viewModel = new WorkflowSettingsViewModel("", TEST_DESCRIPTION, true);

            viewModel.confirm();
            var result = viewModel.getResult();

            assertThat(result.name()).isEqualTo("Untitled Workflow");
        }

        @Test
        @DisplayName("Should default to Untitled Workflow when name is only whitespace")
        void shouldDefaultToUntitledWorkflowForWhitespace() {
            var viewModel = new WorkflowSettingsViewModel("   ", TEST_DESCRIPTION, true);

            viewModel.confirm();
            var result = viewModel.getResult();

            assertThat(result.name()).isEqualTo("Untitled Workflow");
        }
    }
}
