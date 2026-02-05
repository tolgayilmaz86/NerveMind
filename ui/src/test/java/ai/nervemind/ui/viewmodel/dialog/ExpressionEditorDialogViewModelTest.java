/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for ExpressionEditorDialogViewModel.
 */
@DisplayName("ExpressionEditorDialogViewModel")
class ExpressionEditorDialogViewModelTest extends ViewModelTestBase {

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should initialize with empty expression")
        void shouldInitializeWithEmptyExpression() {
            var viewModel = new ExpressionEditorDialogViewModel();

            assertThat(viewModel.getExpression()).isEmpty();
            assertThat(viewModel.getPreviewText()).isEmpty();
        }

        @Test
        @DisplayName("Should initialize with provided variables")
        void shouldInitializeWithProvidedVariables() {
            var variables = List.of("name", "email", "count");
            var viewModel = new ExpressionEditorDialogViewModel(variables);

            assertThat(viewModel.getAvailableVariables()).containsExactlyElementsOf(variables);
        }

        @Test
        @DisplayName("Should handle null variables list")
        void shouldHandleNullVariables() {
            var viewModel = new ExpressionEditorDialogViewModel(null);

            assertThat(viewModel.getAvailableVariables()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Expression Property")
    class ExpressionProperty {

        @Test
        @DisplayName("Should update expression property")
        void shouldUpdateExpressionProperty() {
            var viewModel = new ExpressionEditorDialogViewModel();

            viewModel.setExpression("Hello ${name}");
            assertThat(viewModel.getExpression()).isEqualTo("Hello ${name}");
        }

        @Test
        @DisplayName("Should allow empty expression")
        void shouldAllowEmptyExpression() {
            var viewModel = new ExpressionEditorDialogViewModel();
            viewModel.setExpression("test");
            viewModel.setExpression("");

            assertThat(viewModel.getExpression()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Preview Generation")
    class PreviewGeneration {

        @Test
        @DisplayName("Should generate preview for expression with variables")
        void shouldGeneratePreviewWithVariables() {
            var viewModel = new ExpressionEditorDialogViewModel();
            viewModel.setExpression("Hello ${name}, your count is ${count}");

            String preview = viewModel.getPreviewText();
            assertThat(preview).contains("Expression:");
            assertThat(preview).contains("Variables used:");
            assertThat(preview).contains("name");
            assertThat(preview).contains("count");
        }

        @Test
        @DisplayName("Should show empty preview for blank expression")
        void shouldShowEmptyPreviewForBlankExpression() {
            var viewModel = new ExpressionEditorDialogViewModel();
            viewModel.setExpression("");

            assertThat(viewModel.getPreviewText()).isEmpty();
        }

        @Test
        @DisplayName("Should update preview on expression change")
        void shouldUpdatePreviewOnExpressionChange() {
            var viewModel = new ExpressionEditorDialogViewModel();

            viewModel.setExpression("${var1}");
            String preview1 = viewModel.getPreviewText();

            viewModel.setExpression("${var2}");
            String preview2 = viewModel.getPreviewText();

            assertThat(preview1).isNotEqualTo(preview2);
            assertThat(preview2).contains("var2");
        }

        @Test
        @DisplayName("Should handle expression without variables")
        void shouldHandleExpressionWithoutVariables() {
            var viewModel = new ExpressionEditorDialogViewModel();
            viewModel.setExpression("Plain text without variables");

            String preview = viewModel.getPreviewText();
            assertThat(preview).contains("Expression:");
            assertThat(preview).doesNotContain("Variables used:");
        }
    }

    @Nested
    @DisplayName("Available Variables")
    class AvailableVariables {

        @Test
        @DisplayName("Should update available variables")
        void shouldUpdateAvailableVariables() {
            var viewModel = new ExpressionEditorDialogViewModel();

            viewModel.setAvailableVariables(List.of("a", "b", "c"));
            assertThat(viewModel.getAvailableVariables()).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("Should clear variables when set to null")
        void shouldClearVariablesWhenSetToNull() {
            var viewModel = new ExpressionEditorDialogViewModel(List.of("x", "y"));

            viewModel.setAvailableVariables(null);
            assertThat(viewModel.getAvailableVariables()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should always be valid (expressions can be empty)")
        void shouldAlwaysBeValid() {
            var viewModel = new ExpressionEditorDialogViewModel();

            assertThat(viewModel.validate()).isTrue();
            assertThat(viewModel.isValid()).isTrue();

            viewModel.setExpression("${var}");
            assertThat(viewModel.validate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Result Building")
    class ResultBuilding {

        @Test
        @DisplayName("Should build result with expression")
        void shouldBuildResultWithExpression() {
            var viewModel = new ExpressionEditorDialogViewModel();
            viewModel.setExpression("Hello ${world}");

            viewModel.confirm();

            assertThat(viewModel.getResult()).isEqualTo("Hello ${world}");
        }

        @Test
        @DisplayName("Should build result with empty expression")
        void shouldBuildResultWithEmptyExpression() {
            var viewModel = new ExpressionEditorDialogViewModel();

            viewModel.confirm();

            assertThat(viewModel.getResult()).isEmpty();
        }
    }
}
