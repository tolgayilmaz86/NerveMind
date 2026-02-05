/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.dto.VariableDTO;
import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for VariableEditViewModel.
 */
@DisplayName("VariableEditViewModel")
class VariableEditViewModelTest extends ViewModelTestBase {

    private static final String TEST_NAME = "testVariable";
    private static final String TEST_VALUE = "test-value";
    private static final Long TEST_WORKFLOW_ID = 1L;

    @Nested
    @DisplayName("Create Mode")
    class CreateMode {

        @Test
        @DisplayName("Should initialize in create mode when no existing variable")
        void shouldInitializeInCreateMode() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            assertThat(viewModel.isEditMode()).isFalse();
            assertThat(viewModel.getName()).isEmpty();
            assertThat(viewModel.getType()).isEqualTo(VariableType.STRING);
            assertThat(viewModel.getScope()).isEqualTo(VariableScope.GLOBAL);
            assertThat(viewModel.getValue()).isEmpty();
            assertThat(viewModel.getDescription()).isEmpty();
            assertThat(viewModel.isScopeEditable()).isTrue();
        }

        @Test
        @DisplayName("Should be invalid when name is empty")
        void shouldBeInvalidWhenNameEmpty() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);
            viewModel.setValue(TEST_VALUE);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("Should be valid when name and value are provided")
        void shouldBeValidWithNameAndValue() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);
            viewModel.setName(TEST_NAME);
            viewModel.setValue(TEST_VALUE);

            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("Should allow changing variable type")
        void shouldAllowChangingType() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            viewModel.setType(VariableType.NUMBER);
            assertThat(viewModel.getType()).isEqualTo(VariableType.NUMBER);

            viewModel.setType(VariableType.BOOLEAN);
            assertThat(viewModel.getType()).isEqualTo(VariableType.BOOLEAN);

            viewModel.setType(VariableType.SECRET);
            assertThat(viewModel.getType()).isEqualTo(VariableType.SECRET);
        }

        @Test
        @DisplayName("Should allow changing scope in create mode")
        void shouldAllowChangingScope() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            viewModel.setScope(VariableScope.WORKFLOW);
            assertThat(viewModel.getScope()).isEqualTo(VariableScope.WORKFLOW);

            viewModel.setScope(VariableScope.EXECUTION);
            assertThat(viewModel.getScope()).isEqualTo(VariableScope.EXECUTION);
        }
    }

    @Nested
    @DisplayName("Edit Mode")
    class EditMode {

        private VariableDTO createExisting() {
            return new VariableDTO(
                    1L,
                    "existingVar",
                    "42",
                    VariableType.NUMBER,
                    VariableScope.WORKFLOW,
                    TEST_WORKFLOW_ID,
                    "Existing description",
                    null,
                    null);
        }

        @Test
        @DisplayName("Should initialize in edit mode with existing variable")
        void shouldInitializeInEditMode() {
            var existing = createExisting();
            var viewModel = new VariableEditViewModel(existing, TEST_WORKFLOW_ID);

            assertThat(viewModel.isEditMode()).isTrue();
            assertThat(viewModel.getName()).isEqualTo("existingVar");
            assertThat(viewModel.getType()).isEqualTo(VariableType.NUMBER);
            assertThat(viewModel.getScope()).isEqualTo(VariableScope.WORKFLOW);
            assertThat(viewModel.getValue()).isEqualTo("42");
            assertThat(viewModel.getDescription()).isEqualTo("Existing description");
        }

        @Test
        @DisplayName("Should not be scope editable in edit mode")
        void shouldNotBeScopeEditableInEditMode() {
            var existing = createExisting();
            var viewModel = new VariableEditViewModel(existing, TEST_WORKFLOW_ID);

            assertThat(viewModel.isScopeEditable()).isFalse();
        }

        @Test
        @DisplayName("Should not load value for SECRET type variables")
        void shouldNotLoadValueForSecretType() {
            var secretVar = new VariableDTO(
                    2L,
                    "secretVar",
                    "should-not-be-visible",
                    VariableType.SECRET,
                    VariableScope.GLOBAL,
                    null,
                    null,
                    null,
                    null);
            var viewModel = new VariableEditViewModel(secretVar, TEST_WORKFLOW_ID);

            assertThat(viewModel.getValue()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should validate form correctly")
        void shouldValidateFormCorrectly() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            assertThat(viewModel.validate()).isFalse();

            viewModel.setName(TEST_NAME);
            viewModel.setValue(TEST_VALUE);
            assertThat(viewModel.validate()).isTrue();
        }

        @Test
        @DisplayName("Should update validation on property changes")
        void shouldUpdateValidationOnPropertyChanges() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            assertThat(viewModel.isFormValid()).isFalse();

            viewModel.setName(TEST_NAME);
            viewModel.setValue(TEST_VALUE);
            assertThat(viewModel.isFormValid()).isTrue();

            viewModel.setName("");
            assertThat(viewModel.isFormValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Description")
    class Description {

        @Test
        @DisplayName("Should handle description property")
        void shouldHandleDescription() {
            var viewModel = new VariableEditViewModel(null, TEST_WORKFLOW_ID);

            viewModel.setDescription("Test description");
            assertThat(viewModel.getDescription()).isEqualTo("Test description");
        }

        @Test
        @DisplayName("Should handle null description in existing variable")
        void shouldHandleNullDescription() {
            var existing = new VariableDTO(
                    1L, "var", "value", VariableType.STRING,
                    VariableScope.GLOBAL, null, null, null, null);
            var viewModel = new VariableEditViewModel(existing, TEST_WORKFLOW_ID);

            assertThat(viewModel.getDescription()).isEmpty();
        }
    }
}
