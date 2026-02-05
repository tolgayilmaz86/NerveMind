/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.common.domain.SampleWorkflow.Guide;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.ui.service.CanvasZoomService;
import ai.nervemind.ui.service.NodeSelectionService;
import ai.nervemind.ui.viewmodel.MainViewModel;
import ai.nervemind.ui.viewmodel.canvas.WorkflowCanvasViewModel;
import ai.nervemind.ui.viewmodel.dialog.GuideViewViewModel;
import ai.nervemind.ui.viewmodel.dialog.SamplesBrowserViewModel;

/**
 * Integration tests for the samples browser and workflow guide user flows.
 *
 * <p>
 * Tests the interaction between SamplesBrowserViewModel, GuideViewViewModel,
 * and WorkflowCanvasViewModel when browsing and importing sample workflows.
 */
@DisplayName("Samples Browser Flow Integration Tests")
class SamplesBrowserFlowIntegrationTest {

    private SamplesBrowserViewModel browserViewModel;
    private WorkflowCanvasViewModel canvasViewModel;
    private MainViewModel mainViewModel;

    private List<SampleWorkflow> testSamples;

    @BeforeEach
    void setUp() {
        testSamples = createSampleWorkflows();
        browserViewModel = new SamplesBrowserViewModel(
                testSamples,
                List.of("Basics", "Integration", "AI"),
                List.of("English", "Spanish"));
        canvasViewModel = new WorkflowCanvasViewModel(
                new CanvasZoomService(),
                new NodeSelectionService<>());
        mainViewModel = new MainViewModel();
    }

    private List<SampleWorkflow> createSampleWorkflows() {
        // Sample without guide
        SampleWorkflow simpleWorkflow = new SampleWorkflow(
                "sample1",
                "Hello World",
                "A simple hello world workflow",
                "Basics",
                Difficulty.BEGINNER,
                "English",
                List.of("beginner", "intro"),
                null, null, null, null, null, null, null);

        // Sample with guide
        SampleWorkflow guidedWorkflow = new SampleWorkflow(
                "sample2",
                "API Integration",
                "Complex API integration workflow",
                "Integration",
                Difficulty.ADVANCED,
                "English",
                List.of("api", "advanced"),
                null, null,
                new Guide(List.of(
                        new GuideStep("Getting Started", "Welcome to the guide", null, null),
                        new GuideStep("Configuration", "Configure your API key", List.of("api-node"), null),
                        new GuideStep("Testing", "Test your workflow", null, "workflow.run()"))),
                null, null, null, null);

        // Sample in Spanish
        SampleWorkflow spanishWorkflow = new SampleWorkflow(
                "sample3",
                "Spanish Sample",
                "Sample in Spanish",
                "Basics",
                Difficulty.INTERMEDIATE,
                "Spanish",
                List.of("spanish"),
                null, null, null, null, null, null, null);

        return List.of(simpleWorkflow, guidedWorkflow, spanishWorkflow);
    }

    // ===== User Flow: Browse Samples =====

    @Nested
    @DisplayName("Browse Samples Flow")
    class BrowseSamplesFlow {

        @Test
        @DisplayName("User browses all available samples")
        void userBrowsesAllSamples() {
            // All samples are visible
            assertThat(browserViewModel.getFilteredSamples()).hasSize(3);

            // No selection initially
            assertThat(browserViewModel.hasSelectionProperty().get()).isFalse();
        }

        @Test
        @DisplayName("User filters samples by category")
        void userFiltersByCategory() {
            // User selects Integration category
            browserViewModel.selectedCategoryProperty().set("Integration");

            // Only Integration samples shown (API Integration)
            assertThat(browserViewModel.getFilteredSamples()).hasSize(1);
            assertThat(browserViewModel.getFilteredSamples().getFirst().name()).isEqualTo("API Integration");
        }

        @Test
        @DisplayName("User filters samples by difficulty")
        void userFiltersByDifficulty() {
            // User selects beginner difficulty
            browserViewModel.selectedDifficultyProperty().set("⭐ Beginner");

            // Only beginner samples shown
            assertThat(browserViewModel.getFilteredSamples()).hasSize(1);
            assertThat(browserViewModel.getFilteredSamples().getFirst().name()).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("User filters samples by language")
        void userFiltersByLanguage() {
            // User selects Spanish
            browserViewModel.selectedLanguageProperty().set("Spanish");

            // Only Spanish samples shown
            assertThat(browserViewModel.getFilteredSamples()).hasSize(1);
            assertThat(browserViewModel.getFilteredSamples().getFirst().name()).isEqualTo("Spanish Sample");
        }

        @Test
        @DisplayName("User searches samples by text")
        void userSearchesSamples() {
            // User searches for "api"
            browserViewModel.searchQueryProperty().set("api");

            // API-related samples shown
            assertThat(browserViewModel.getFilteredSamples()).hasSize(1);
            assertThat(browserViewModel.getFilteredSamples().getFirst().id()).isEqualTo("sample2");
        }

        @Test
        @DisplayName("User clears all filters")
        void userClearsFilters() {
            // Apply filters
            browserViewModel.selectedCategoryProperty().set("Integration");
            assertThat(browserViewModel.getFilteredSamples()).hasSize(1);

            // Clear filters
            browserViewModel.clearFilters();

            // All samples visible again
            assertThat(browserViewModel.getFilteredSamples()).hasSize(3);
        }
    }

    // ===== User Flow: Select and Preview Sample =====

    @Nested
    @DisplayName("Select and Preview Sample Flow")
    class SelectAndPreviewFlow {

        @Test
        @DisplayName("User selects sample to see details")
        void userSelectsSample() {
            // User selects a sample
            SampleWorkflow sample = browserViewModel.getFilteredSamples().getFirst();
            browserViewModel.selectedSampleProperty().set(sample);

            // Sample is selected
            assertThat(browserViewModel.hasSelectionProperty().get()).isTrue();
            assertThat(browserViewModel.getSelectedSample()).isEqualTo(sample);
        }

        @Test
        @DisplayName("User sees sample has guide indicator")
        void userSeesSampleHasGuide() {
            // API Integration has a guide
            SampleWorkflow guidedSample = browserViewModel.getFilteredSamples().stream()
                    .filter(s -> s.id().equals("sample2"))
                    .findFirst()
                    .orElseThrow();

            browserViewModel.selectedSampleProperty().set(guidedSample);

            assertThat(browserViewModel.selectedHasGuideProperty().get()).isTrue();
        }

        @Test
        @DisplayName("User sees sample without guide")
        void userSeesSampleWithoutGuide() {
            // Hello World has no guide
            SampleWorkflow simpleSample = browserViewModel.getFilteredSamples().stream()
                    .filter(s -> s.id().equals("sample1"))
                    .findFirst()
                    .orElseThrow();

            browserViewModel.selectedSampleProperty().set(simpleSample);

            assertThat(browserViewModel.selectedHasGuideProperty().get()).isFalse();
        }
    }

    // ===== User Flow: Import Sample Workflow =====

    @Nested
    @DisplayName("Import Sample Workflow Flow")
    class ImportSampleWorkflowFlow {

        @Test
        @DisplayName("User imports sample and workflow loads on canvas")
        void userImportsSample() {
            // 1. User selects a sample
            SampleWorkflow sample = browserViewModel.getFilteredSamples().getFirst();
            browserViewModel.selectedSampleProperty().set(sample);

            // 2. User imports sample (simulating the workflow load)
            WorkflowDTO workflow = createSampleWorkflowDTO("Hello World");
            canvasViewModel.loadWorkflow(workflow);
            mainViewModel.setActiveWorkflow(workflow);

            // 3. Workflow is loaded
            assertThat(canvasViewModel.getNodes()).isNotEmpty();
            assertThat(mainViewModel.getActiveWorkflow()).isNotNull();
            assertThat(mainViewModel.getActiveWorkflow().name()).isEqualTo("Hello World");
        }

        private WorkflowDTO createSampleWorkflowDTO(String name) {
            Node startNode = new Node("start-1", "trigger", "Start",
                    new Node.Position(100, 100), Map.of(), null, false, null);
            Node actionNode = new Node("action-1", "code", "Action",
                    new Node.Position(300, 100), Map.of(), null, false, null);

            return new WorkflowDTO(1L, name, "Sample workflow",
                    List.of(startNode, actionNode),
                    List.of(new Connection("c1", "start-1", "output", "action-1", "input")),
                    Map.of(), false, TriggerType.MANUAL, null,
                    Instant.now(), Instant.now(), null, 1);
        }
    }

    // ===== User Flow: Follow Workflow Guide =====

    @Nested
    @DisplayName("Follow Workflow Guide Flow")
    class FollowWorkflowGuideFlow {

        @Test
        @DisplayName("User follows guide from start to finish")
        void userFollowsGuide() {
            // 1. Get sample with guide
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.id().equals("sample2"))
                    .findFirst()
                    .orElseThrow();

            // 2. Create guide view model
            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // 3. Guide starts at first step
            assertThat(guideViewModel.getCurrentStepIndex()).isZero();
            assertThat(guideViewModel.getStepTitle()).isEqualTo("Step 1: Getting Started");
            assertThat(guideViewModel.getSteps()).hasSize(3);

            // 4. User clicks next
            guideViewModel.navigateNext();
            assertThat(guideViewModel.getCurrentStepIndex()).isEqualTo(1);
            assertThat(guideViewModel.getStepTitle()).isEqualTo("Step 2: Configuration");

            // 5. User continues
            guideViewModel.navigateNext();
            assertThat(guideViewModel.getCurrentStepIndex()).isEqualTo(2);
            assertThat(guideViewModel.getStepTitle()).isEqualTo("Step 3: Testing");

            // 6. User is at last step
            assertThat(guideViewModel.canNavigateNext()).isFalse();
        }

        @Test
        @DisplayName("User navigates back and forth in guide")
        void userNavigatesBackAndForth() {
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // Start at first step
            assertThat(guideViewModel.canNavigatePrevious()).isFalse();
            assertThat(guideViewModel.canNavigateNext()).isTrue();

            // Go to middle
            guideViewModel.navigateNext();
            assertThat(guideViewModel.canNavigatePrevious()).isTrue();
            assertThat(guideViewModel.canNavigateNext()).isTrue();

            // Go back
            guideViewModel.navigatePrevious();
            assertThat(guideViewModel.getCurrentStepIndex()).isZero();
        }

        @Test
        @DisplayName("User jumps to specific step")
        void userJumpsToStep() {
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // Jump to last step
            guideViewModel.navigateToStep(2);
            assertThat(guideViewModel.getCurrentStepIndex()).isEqualTo(2);

            // Jump back to first
            guideViewModel.navigateToStep(0);
            assertThat(guideViewModel.getCurrentStepIndex()).isZero();
        }
    }

    // ===== User Flow: Guide with Code Snippets =====

    @Nested
    @DisplayName("Guide with Code Snippets Flow")
    class GuideWithCodeSnippetsFlow {

        @Test
        @DisplayName("Guide step shows code snippet")
        void guideShowsCodeSnippet() {
            // Get sample with guide (last step has code snippet)
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // Navigate to step with code snippet
            guideViewModel.navigateToStep(2);

            assertThat(guideViewModel.hasCodeSnippet()).isTrue();
            assertThat(guideViewModel.getStepCodeSnippet()).isEqualTo("workflow.run()");
        }

        @Test
        @DisplayName("Guide step without code snippet")
        void guideStepWithoutCodeSnippet() {
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // First step has no code snippet
            assertThat(guideViewModel.hasCodeSnippet()).isFalse();
        }
    }

    // ===== User Flow: Guide with Node Highlighting =====

    @Nested
    @DisplayName("Guide with Node Highlighting Flow")
    class GuideWithNodeHighlightingFlow {

        @Test
        @DisplayName("Guide step highlights relevant nodes")
        void guideHighlightsNodes() {
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // Navigate to step with node highlighting
            guideViewModel.navigateToStep(1); // Configuration step

            assertThat(guideViewModel.hasHighlightNodes()).isTrue();
        }

        @Test
        @DisplayName("Guide step without node highlighting")
        void guideStepWithoutHighlighting() {
            SampleWorkflow guidedSample = testSamples.stream()
                    .filter(s -> s.guide() != null)
                    .findFirst()
                    .orElseThrow();

            GuideViewViewModel guideViewModel = new GuideViewViewModel(guidedSample);

            // First step has no highlighting
            assertThat(guideViewModel.hasHighlightNodes()).isFalse();
        }
    }
}
