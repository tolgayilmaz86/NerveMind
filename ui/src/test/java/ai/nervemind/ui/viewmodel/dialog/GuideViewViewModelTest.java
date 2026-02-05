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

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.common.domain.SampleWorkflow.Guide;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for GuideViewViewModel.
 */
@DisplayName("GuideViewViewModel")
class GuideViewViewModelTest extends ViewModelTestBase {

    private static final GuideStep STEP_1 = new GuideStep(
            "Getting Started",
            "This is the first step of the guide",
            List.of("node-1", "node-2"),
            "System.out.println(\"Hello\");");

    private static final GuideStep STEP_2 = new GuideStep(
            "Configuration",
            "Configure your workflow settings",
            null,
            null);

    private static final GuideStep STEP_3 = new GuideStep(
            "Final Step",
            "Complete the workflow",
            List.of("node-3"),
            "workflow.run();");

    private static SampleWorkflow createSampleWithGuide() {
        return new SampleWorkflow(
                "test-sample", // id
                "Test Sample", // name
                "A test sample workflow", // description
                "Testing", // category
                Difficulty.BEGINNER, // difficulty
                "English", // language
                List.of("test"), // tags
                null, // author
                null, // version
                new Guide(List.of(STEP_1, STEP_2, STEP_3)), // guide
                null, // workflow
                null, // requiredCredentials
                null, // environmentVariables
                null); // filePath
    }

    private static SampleWorkflow createSampleWithoutGuide() {
        return new SampleWorkflow(
                "no-guide",
                "No Guide Sample",
                "Sample without guide",
                "Testing",
                Difficulty.BEGINNER,
                "English",
                List.of(),
                null,
                null,
                null, // guide is null
                null,
                null,
                null,
                null);
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should load guide steps")
        void shouldLoadGuideSteps() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.getSteps()).hasSize(3);
        }

        @Test
        @DisplayName("Should start at first step")
        void shouldStartAtFirstStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.getCurrentStepIndex()).isZero();
            assertThat(viewModel.getStepTitle()).isEqualTo("Step 1: Getting Started");
        }

        @Test
        @DisplayName("Should display step content")
        void shouldDisplayStepContent() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.getStepContent()).isEqualTo("This is the first step of the guide");
        }

        @Test
        @DisplayName("Should handle sample without guide")
        void shouldHandleSampleWithoutGuide() {
            var viewModel = new GuideViewViewModel(createSampleWithoutGuide());

            assertThat(viewModel.getSteps()).isEmpty();
        }

        @Test
        @DisplayName("Should store sample reference")
        void shouldStoreSampleReference() {
            var sample = createSampleWithGuide();
            var viewModel = new GuideViewViewModel(sample);

            assertThat(viewModel.getSample()).isEqualTo(sample);
        }
    }

    @Nested
    @DisplayName("Navigation State")
    class NavigationState {

        @Test
        @DisplayName("Should not allow previous at first step")
        void shouldNotAllowPreviousAtFirstStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.canNavigatePrevious()).isFalse();
        }

        @Test
        @DisplayName("Should allow next at first step")
        void shouldAllowNextAtFirstStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.canNavigateNext()).isTrue();
        }

        @Test
        @DisplayName("Should not allow next at last step")
        void shouldNotAllowNextAtLastStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());
            viewModel.navigateToStep(2);

            assertThat(viewModel.canNavigateNext()).isFalse();
        }

        @Test
        @DisplayName("Should allow previous at last step")
        void shouldAllowPreviousAtLastStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());
            viewModel.navigateToStep(2);

            assertThat(viewModel.canNavigatePrevious()).isTrue();
        }

        @Test
        @DisplayName("Should display step counter")
        void shouldDisplayStepCounter() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            String counter = viewModel.getStepCounterText();
            assertThat(counter).contains("1");
            assertThat(counter).contains("3");
        }
    }

    @Nested
    @DisplayName("Navigation Actions")
    class NavigationActions {

        @Test
        @DisplayName("Should navigate to next step")
        void shouldNavigateToNextStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            viewModel.navigateNext();

            assertThat(viewModel.getCurrentStepIndex()).isEqualTo(1);
            assertThat(viewModel.getStepTitle()).isEqualTo("Step 2: Configuration");
        }

        @Test
        @DisplayName("Should navigate to previous step")
        void shouldNavigateToPreviousStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());
            viewModel.navigateToStep(2);

            viewModel.navigatePrevious();

            assertThat(viewModel.getCurrentStepIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should navigate to specific step")
        void shouldNavigateToSpecificStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            viewModel.navigateToStep(2);

            assertThat(viewModel.getCurrentStepIndex()).isEqualTo(2);
            assertThat(viewModel.getStepTitle()).isEqualTo("Step 3: Final Step");
        }

        @Test
        @DisplayName("Should ignore invalid step index")
        void shouldIgnoreInvalidStepIndex() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            viewModel.navigateToStep(-1);
            assertThat(viewModel.getCurrentStepIndex()).isZero();

            viewModel.navigateToStep(100);
            assertThat(viewModel.getCurrentStepIndex()).isZero();
        }

        @Test
        @DisplayName("Should not navigate previous at first step")
        void shouldNotNavigatePreviousAtFirstStep() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            viewModel.navigatePrevious();

            assertThat(viewModel.getCurrentStepIndex()).isZero();
        }
    }

    @Nested
    @DisplayName("Code Snippets")
    class CodeSnippets {

        @Test
        @DisplayName("Should detect code snippet presence")
        void shouldDetectCodeSnippetPresence() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.hasCodeSnippet()).isTrue();
            assertThat(viewModel.getStepCodeSnippet()).isEqualTo("System.out.println(\"Hello\");");
        }

        @Test
        @DisplayName("Should handle step without code snippet")
        void shouldHandleStepWithoutCodeSnippet() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());
            viewModel.navigateToStep(1);

            assertThat(viewModel.hasCodeSnippet()).isFalse();
        }
    }

    @Nested
    @DisplayName("Highlight Nodes")
    class HighlightNodes {

        @Test
        @DisplayName("Should detect highlight nodes presence")
        void shouldDetectHighlightNodesPresence() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());

            assertThat(viewModel.hasHighlightNodes()).isTrue();
        }

        @Test
        @DisplayName("Should handle step without highlight nodes")
        void shouldHandleStepWithoutHighlightNodes() {
            var viewModel = new GuideViewViewModel(createSampleWithGuide());
            viewModel.navigateToStep(1);

            assertThat(viewModel.hasHighlightNodes()).isFalse();
        }
    }

    @Nested
    @DisplayName("Result Building")
    class ResultBuilding {

        @Test
        @DisplayName("Should return sample as result")
        void shouldReturnSampleAsResult() {
            var sample = createSampleWithGuide();
            var viewModel = new GuideViewViewModel(sample);

            viewModel.confirm();

            assertThat(viewModel.getResult()).isEqualTo(sample);
        }
    }
}
