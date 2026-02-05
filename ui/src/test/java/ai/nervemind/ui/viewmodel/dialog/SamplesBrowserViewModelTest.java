/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.common.domain.SampleWorkflow.Guide;
import ai.nervemind.common.domain.SampleWorkflow.GuideStep;
import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for SamplesBrowserViewModel.
 */
@DisplayName("SamplesBrowserViewModel")
class SamplesBrowserViewModelTest extends ViewModelTestBase {

    private SamplesBrowserViewModel viewModel;

    private static final SampleWorkflow BEGINNER_SAMPLE = new SampleWorkflow(
            "sample1",
            "Hello World",
            "A simple hello world workflow",
            "Basics",
            Difficulty.BEGINNER,
            "English",
            List.of("beginner", "intro"),
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    private static final SampleWorkflow ADVANCED_SAMPLE = new SampleWorkflow(
            "sample2",
            "API Integration",
            "Complex API integration workflow",
            "Integration",
            Difficulty.ADVANCED,
            "English",
            List.of("api", "advanced"),
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    private static final SampleWorkflow SAMPLE_WITH_GUIDE = new SampleWorkflow(
            "sample3",
            "With Guide",
            "Sample with step-by-step guide",
            "Basics",
            Difficulty.INTERMEDIATE,
            "Spanish",
            List.of("guide"),
            null,
            null,
            new Guide(List.of(
                    new GuideStep("Step 1", "First step", null, null))),
            null,
            null,
            null,
            null);

    @BeforeEach
    void setUp() {
        viewModel = new SamplesBrowserViewModel(
                List.of(BEGINNER_SAMPLE, ADVANCED_SAMPLE, SAMPLE_WITH_GUIDE),
                List.of("Basics", "Integration"),
                List.of("English", "Spanish"));
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should load all samples")
        void shouldLoadAllSamples() {
            assertThat(viewModel.getAllSamples()).hasSize(3);
            assertThat(viewModel.getFilteredSamples()).hasSize(3);
        }

        @Test
        @DisplayName("Should setup categories with All option")
        void shouldSetupCategoriesWithAllOption() {
            assertThat(viewModel.getCategories()).contains("All Categories", "Basics", "Integration");
            assertThat(viewModel.selectedCategoryProperty().get()).isEqualTo("All Categories");
        }

        @Test
        @DisplayName("Should setup languages with All option")
        void shouldSetupLanguagesWithAllOption() {
            assertThat(viewModel.getLanguages()).contains("All Languages", "English", "Spanish");
            assertThat(viewModel.selectedLanguageProperty().get()).isEqualTo("All Languages");
        }

        @Test
        @DisplayName("Should setup difficulty levels")
        void shouldSetupDifficultyLevels() {
            assertThat(viewModel.getDifficulties()).isNotEmpty();
            assertThat(viewModel.getDifficulties()).contains("All Levels");
        }

        @Test
        @DisplayName("Should have no selection initially")
        void shouldHaveNoSelectionInitially() {
            assertThat(viewModel.getSelectedSample()).isNull();
            assertThat(viewModel.hasSelectionProperty().get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Search Filtering")
    class SearchFiltering {

        @Test
        @DisplayName("Should filter by search query")
        void shouldFilterBySearchQuery() {
            viewModel.searchQueryProperty().set("Hello");

            assertThat(viewModel.getFilteredSamples())
                    .hasSize(1)
                    .first()
                    .extracting(SampleWorkflow::name)
                    .isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should search in name and description")
        void shouldSearchInNameAndDescription() {
            viewModel.searchQueryProperty().set("API");

            assertThat(viewModel.getFilteredSamples())
                    .hasSize(1)
                    .first()
                    .extracting(SampleWorkflow::name)
                    .isEqualTo("API Integration");
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            viewModel.searchQueryProperty().set("HELLO");
            int upperCaseCount = viewModel.getFilteredSamples().size();

            viewModel.searchQueryProperty().set("hello");
            int lowerCaseCount = viewModel.getFilteredSamples().size();

            assertThat(upperCaseCount).isEqualTo(lowerCaseCount);
        }

        @Test
        @DisplayName("Should show all when search cleared")
        void shouldShowAllWhenSearchCleared() {
            viewModel.searchQueryProperty().set("Hello");
            viewModel.searchQueryProperty().set("");

            assertThat(viewModel.getFilteredSamples()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Category Filtering")
    class CategoryFiltering {

        @Test
        @DisplayName("Should filter by category")
        void shouldFilterByCategory() {
            viewModel.selectedCategoryProperty().set("Basics");

            assertThat(viewModel.getFilteredSamples()).hasSize(2);
        }

        @Test
        @DisplayName("Should show all with All Categories")
        void shouldShowAllWithAllCategories() {
            viewModel.selectedCategoryProperty().set("Basics");
            viewModel.selectedCategoryProperty().set("All Categories");

            assertThat(viewModel.getFilteredSamples()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Language Filtering")
    class LanguageFiltering {

        @Test
        @DisplayName("Should filter by language")
        void shouldFilterByLanguage() {
            viewModel.selectedLanguageProperty().set("Spanish");

            assertThat(viewModel.getFilteredSamples())
                    .hasSize(1)
                    .first()
                    .extracting(SampleWorkflow::name)
                    .isEqualTo("With Guide");
        }
    }

    @Nested
    @DisplayName("Combined Filtering")
    class CombinedFiltering {

        @Test
        @DisplayName("Should combine multiple filters")
        void shouldCombineMultipleFilters() {
            viewModel.selectedCategoryProperty().set("Basics");
            viewModel.selectedLanguageProperty().set("English");

            assertThat(viewModel.getFilteredSamples())
                    .hasSize(1)
                    .first()
                    .extracting(SampleWorkflow::name)
                    .isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should combine search and category filters")
        void shouldCombineSearchAndCategoryFilters() {
            viewModel.selectedCategoryProperty().set("Basics");
            viewModel.searchQueryProperty().set("Guide");

            assertThat(viewModel.getFilteredSamples()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Selection")
    class Selection {

        @Test
        @DisplayName("Should update selection state")
        void shouldUpdateSelectionState() {
            viewModel.selectedSampleProperty().set(BEGINNER_SAMPLE);

            assertThat(viewModel.hasSelectionProperty().get()).isTrue();
            assertThat(viewModel.getSelectedSample()).isEqualTo(BEGINNER_SAMPLE);
        }

        @Test
        @DisplayName("Should detect if sample has guide")
        void shouldDetectIfSampleHasGuide() {
            viewModel.selectedSampleProperty().set(BEGINNER_SAMPLE);
            assertThat(viewModel.selectedHasGuideProperty().get()).isFalse();

            viewModel.selectedSampleProperty().set(SAMPLE_WITH_GUIDE);
            assertThat(viewModel.selectedHasGuideProperty().get()).isTrue();
        }

        @Test
        @DisplayName("Should clear selection when set to null")
        void shouldClearSelectionWhenSetToNull() {
            viewModel.selectedSampleProperty().set(BEGINNER_SAMPLE);
            viewModel.selectedSampleProperty().set(null);

            assertThat(viewModel.hasSelectionProperty().get()).isFalse();
        }
    }
}
