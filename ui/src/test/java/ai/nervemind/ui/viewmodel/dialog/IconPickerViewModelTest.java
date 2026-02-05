/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.nervemind.ui.viewmodel.ViewModelTestBase;

/**
 * Unit tests for IconPickerViewModel.
 */
@DisplayName("IconPickerViewModel")
class IconPickerViewModelTest extends ViewModelTestBase {

    private IconPickerViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new IconPickerViewModel(null);
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should have icons loaded on initialization")
        void shouldHaveIconsLoaded() {
            assertThat(viewModel.getFilteredIcons()).isNotEmpty();
        }

        @Test
        @DisplayName("Should have categories available")
        void shouldHaveCategoriesAvailable() {
            assertThat(viewModel.getCategories()).isNotEmpty();
            assertThat(viewModel.getCategories()).contains("All");
        }

        @Test
        @DisplayName("Should have All category selected by default")
        void shouldHaveAllCategorySelected() {
            assertThat(viewModel.selectedCategoryProperty().get()).isEqualTo("All");
        }

        @Test
        @DisplayName("Should have no selection initially when no current icon")
        void shouldHaveNoSelectionInitially() {
            assertThat(viewModel.selectedIconCodeProperty().get()).isNull();
        }

        @Test
        @DisplayName("Should pre-select current icon if provided")
        void shouldPreSelectCurrentIcon() {
            // Get a valid icon code from the list
            var firstIcon = viewModel.getFilteredIcons().get(0);
            var viewModelWithIcon = new IconPickerViewModel(firstIcon.code());

            assertThat(viewModelWithIcon.selectedIconCodeProperty().get())
                    .isEqualTo(firstIcon.code());
        }
    }

    @Nested
    @DisplayName("Search Filtering")
    class SearchFiltering {

        @Test
        @DisplayName("Should filter icons by search query")
        void shouldFilterIconsBySearchQuery() {
            int totalCount = viewModel.getFilteredIcons().size();

            viewModel.searchQueryProperty().set("robot");
            int filteredCount = viewModel.getFilteredIcons().size();

            assertThat(filteredCount).isLessThan(totalCount);
            assertThat(filteredCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            viewModel.searchQueryProperty().set("ROBOT");
            int upperCaseCount = viewModel.getFilteredIcons().size();

            viewModel.searchQueryProperty().set("robot");
            int lowerCaseCount = viewModel.getFilteredIcons().size();

            assertThat(upperCaseCount).isEqualTo(lowerCaseCount);
        }

        @Test
        @DisplayName("Should show all icons when search is cleared")
        void shouldShowAllIconsWhenSearchCleared() {
            int totalCount = viewModel.getFilteredIcons().size();

            viewModel.searchQueryProperty().set("robot");
            viewModel.searchQueryProperty().set("");

            assertThat(viewModel.getFilteredIcons().size()).isEqualTo(totalCount);
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResults() {
            viewModel.searchQueryProperty().set("zzzznonexistentzzzz");

            assertThat(viewModel.getFilteredIcons()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Category Filtering")
    class CategoryFiltering {

        @Test
        @DisplayName("Should filter icons by category")
        void shouldFilterIconsByCategory() {
            int totalCount = viewModel.getFilteredIcons().size();

            viewModel.selectedCategoryProperty().set("Material Design");
            int materialCount = viewModel.getFilteredIcons().size();

            assertThat(materialCount).isLessThan(totalCount);
            assertThat(materialCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should show all icons when All category selected")
        void shouldShowAllIconsWhenAllCategorySelected() {
            int totalCount = viewModel.getFilteredIcons().size();

            viewModel.selectedCategoryProperty().set("Material Design");
            viewModel.selectedCategoryProperty().set("All");

            assertThat(viewModel.getFilteredIcons().size()).isEqualTo(totalCount);
        }

        @Test
        @DisplayName("Should combine category and search filters")
        void shouldCombineCategoryAndSearchFilters() {
            viewModel.selectedCategoryProperty().set("Material Design");
            int categoryCount = viewModel.getFilteredIcons().size();

            viewModel.searchQueryProperty().set("robot");
            int combinedCount = viewModel.getFilteredIcons().size();

            assertThat(combinedCount).isLessThanOrEqualTo(categoryCount);
        }
    }

    @Nested
    @DisplayName("Icon Selection")
    class IconSelection {

        @Test
        @DisplayName("Should select icon entry")
        void shouldSelectIconEntry() {
            var firstIcon = viewModel.getFilteredIcons().get(0);
            viewModel.selectIcon(firstIcon);

            assertThat(viewModel.selectedIconCodeProperty().get())
                    .isEqualTo(firstIcon.code());
        }

        @Test
        @DisplayName("Should reset selection")
        void shouldResetSelection() {
            var firstIcon = viewModel.getFilteredIcons().get(0);
            viewModel.selectIcon(firstIcon);
            viewModel.resetSelection();

            assertThat(viewModel.selectedIconCodeProperty().get()).isNull();
            assertThat(viewModel.selectedIconLabelProperty().get())
                    .contains("default");
        }

        @Test
        @DisplayName("Should get result")
        void shouldGetResult() {
            var firstIcon = viewModel.getFilteredIcons().get(0);
            viewModel.selectIcon(firstIcon);

            assertThat(viewModel.getResult()).isEqualTo(firstIcon.code());
        }

        @Test
        @DisplayName("Should return null result when reset")
        void shouldReturnNullResultWhenReset() {
            var firstIcon = viewModel.getFilteredIcons().get(0);
            viewModel.selectIcon(firstIcon);
            viewModel.resetSelection();

            assertThat(viewModel.getResult()).isNull();
        }
    }

    @Nested
    @DisplayName("Original Icon")
    class OriginalIcon {

        @Test
        @DisplayName("Should store original icon")
        void shouldStoreOriginalIcon() {
            var firstIcon = viewModel.getFilteredIcons().get(0);
            var viewModelWithIcon = new IconPickerViewModel(firstIcon.code());

            assertThat(viewModelWithIcon.getOriginalIcon()).isEqualTo(firstIcon.code());
        }

        @Test
        @DisplayName("Should be null when no original icon")
        void shouldBeNullWhenNoOriginalIcon() {
            assertThat(viewModel.getOriginalIcon()).isNull();
        }
    }
}
