/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for CredentialEditViewModel.
 */
@DisplayName("CredentialEditViewModel")
class CredentialEditViewModelTest extends ViewModelTestBase {

    private static final String TEST_NAME = "Test Credential";
    private static final String TEST_DATA = "secret-data";

    @Nested
    @DisplayName("Create Mode")
    class CreateMode {

        @Test
        @DisplayName("Should initialize in create mode when no existing credential")
        void shouldInitializeInCreateMode() {
            var viewModel = new CredentialEditViewModel(null);

            assertThat(viewModel.isEditMode()).isFalse();
            assertThat(viewModel.getName()).isEmpty();
            assertThat(viewModel.getType()).isEqualTo(CredentialType.API_KEY);
            assertThat(viewModel.getData()).isEmpty();
            assertThat(viewModel.getExistingCredential()).isNull();
        }

        @Test
        @DisplayName("Should be invalid when name is empty")
        void shouldBeInvalidWhenNameEmpty() {
            var viewModel = new CredentialEditViewModel(null);
            viewModel.setData(TEST_DATA);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("Should be invalid when data is empty in create mode")
        void shouldBeInvalidWhenDataEmpty() {
            var viewModel = new CredentialEditViewModel(null);
            viewModel.setName(TEST_NAME);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("Should be valid when name and data are provided")
        void shouldBeValidWithNameAndData() {
            var viewModel = new CredentialEditViewModel(null);
            viewModel.setName(TEST_NAME);
            viewModel.setData(TEST_DATA);

            assertThat(viewModel.isFormValid()).isTrue();
            assertThat(viewModel.validate()).isTrue();
        }

        @Test
        @DisplayName("Should build result with form values")
        void shouldBuildResultWithFormValues() {
            var viewModel = new CredentialEditViewModel(null);
            viewModel.setName(TEST_NAME);
            viewModel.setType(CredentialType.HTTP_BEARER);
            viewModel.setData(TEST_DATA);

            viewModel.confirm();
            var result = viewModel.getResult();

            assertThat(result).isNotNull();
            assertThat(result.dto().name()).isEqualTo(TEST_NAME);
            assertThat(result.dto().type()).isEqualTo(CredentialType.HTTP_BEARER);
            assertThat(result.data()).isEqualTo(TEST_DATA);
        }
    }

    @Nested
    @DisplayName("Edit Mode")
    class EditMode {

        private CredentialDTO createExisting() {
            return new CredentialDTO(
                    1L,
                    "Existing Credential",
                    CredentialType.OAUTH2,
                    Instant.now(),
                    null);
        }

        @Test
        @DisplayName("Should initialize in edit mode with existing credential")
        void shouldInitializeInEditMode() {
            var existing = createExisting();
            var viewModel = new CredentialEditViewModel(existing);

            assertThat(viewModel.isEditMode()).isTrue();
            assertThat(viewModel.getName()).isEqualTo("Existing Credential");
            assertThat(viewModel.getType()).isEqualTo(CredentialType.OAUTH2);
            assertThat(viewModel.getExistingCredential()).isEqualTo(existing);
        }

        @Test
        @DisplayName("Should be valid in edit mode without data (keeps existing)")
        void shouldBeValidInEditModeWithoutData() {
            var existing = createExisting();
            var viewModel = new CredentialEditViewModel(existing);

            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("Should preserve existing ID in result")
        void shouldPreserveExistingIdInResult() {
            var existing = createExisting();
            var viewModel = new CredentialEditViewModel(existing);
            viewModel.setName("Updated Name");

            viewModel.confirm();
            var result = viewModel.getResult();

            assertThat(result.dto().id()).isEqualTo(1L);
            assertThat(result.dto().name()).isEqualTo("Updated Name");
        }
    }

    @Nested
    @DisplayName("Show Data Toggle")
    class ShowDataToggle {

        @Test
        @DisplayName("Should toggle show data property")
        void shouldToggleShowData() {
            var viewModel = new CredentialEditViewModel(null);

            assertThat(viewModel.isShowData()).isFalse();

            viewModel.setShowData(true);
            assertThat(viewModel.isShowData()).isTrue();

            viewModel.setShowData(false);
            assertThat(viewModel.isShowData()).isFalse();
        }
    }

    @Nested
    @DisplayName("Type Selection")
    class TypeSelection {

        @Test
        @DisplayName("Should allow changing credential type")
        void shouldAllowChangingType() {
            var viewModel = new CredentialEditViewModel(null);

            viewModel.setType(CredentialType.HTTP_BASIC);
            assertThat(viewModel.getType()).isEqualTo(CredentialType.HTTP_BASIC);

            viewModel.setType(CredentialType.CUSTOM_HEADER);
            assertThat(viewModel.getType()).isEqualTo(CredentialType.CUSTOM_HEADER);
        }
    }
}
