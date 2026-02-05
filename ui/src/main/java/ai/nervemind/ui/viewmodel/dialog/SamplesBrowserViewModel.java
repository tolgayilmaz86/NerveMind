/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import java.util.List;
import java.util.function.Consumer;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.SampleWorkflow.Difficulty;
import ai.nervemind.ui.viewmodel.BaseViewModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Samples Browser dialog.
 * 
 * <p>
 * Manages sample workflow filtering, selection, and guide navigation.
 * Does not depend on any JavaFX UI classes - only uses javafx.beans and
 * javafx.collections.
 */
public class SamplesBrowserViewModel extends BaseViewModel {

    private static final String ALL_CATEGORIES = "All Categories";
    private static final String ALL_LANGUAGES = "All Languages";
    private static final String ALL_LEVELS = "All Levels";

    // Data
    private final ObservableList<SampleWorkflow> allSamples = FXCollections.observableArrayList();
    private final ObservableList<SampleWorkflow> filteredSamples = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList();
    private final ObservableList<String> languages = FXCollections.observableArrayList();
    private final ObservableList<String> difficulties = FXCollections.observableArrayList();

    // Filter state
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final StringProperty selectedCategory = new SimpleStringProperty();
    private final StringProperty selectedLanguage = new SimpleStringProperty();
    private final StringProperty selectedDifficulty = new SimpleStringProperty();

    // Selection state
    private final ObjectProperty<SampleWorkflow> selectedSample = new SimpleObjectProperty<>();
    private final BooleanProperty hasSelection = new SimpleBooleanProperty(false);
    private final BooleanProperty selectedHasGuide = new SimpleBooleanProperty(false);

    // Status
    private final StringProperty statusText = new SimpleStringProperty("");

    // Callbacks
    private Consumer<SampleWorkflow> onImport;
    private Consumer<SampleWorkflow> onViewGuide;

    /**
     * Creates a new SamplesBrowserViewModel.
     * 
     * @param samples    list of all available samples
     * @param categories list of unique categories
     * @param languages  list of unique languages
     */
    public SamplesBrowserViewModel(
            List<SampleWorkflow> samples,
            List<String> categories,
            List<String> languages) {

        this.allSamples.setAll(samples);
        this.filteredSamples.setAll(samples);

        // Setup category filter options
        this.categories.add(ALL_CATEGORIES);
        this.categories.addAll(categories);
        this.selectedCategory.set(ALL_CATEGORIES);

        // Setup language filter options
        this.languages.add(ALL_LANGUAGES);
        this.languages.addAll(languages);
        this.selectedLanguage.set(ALL_LANGUAGES);

        // Setup difficulty filter options
        this.difficulties.addAll(
                ALL_LEVELS,
                "⭐ Beginner",
                "⭐⭐ Intermediate",
                "⭐⭐⭐ Advanced");
        this.selectedDifficulty.set(ALL_LEVELS);

        // Bind selection state
        selectedSample.addListener((obs, oldVal, newVal) -> {
            hasSelection.set(newVal != null);
            selectedHasGuide.set(newVal != null
                    && newVal.guide() != null
                    && newVal.guide().steps() != null
                    && !newVal.guide().steps().isEmpty());
        });

        // Setup filter listeners
        searchQuery.addListener((obs, oldVal, newVal) -> applyFilters());
        selectedCategory.addListener((obs, oldVal, newVal) -> applyFilters());
        selectedLanguage.addListener((obs, oldVal, newVal) -> applyFilters());
        selectedDifficulty.addListener((obs, oldVal, newVal) -> applyFilters());

        updateStatusText();
    }

    // ===== Properties =====

    /**
     * All samples (unfiltered).
     * 
     * @return the list of all samples
     */
    public ObservableList<SampleWorkflow> getAllSamples() {
        return allSamples;
    }

    /**
     * Filtered samples list.
     * 
     * @return the list of filtered samples
     */
    public ObservableList<SampleWorkflow> getFilteredSamples() {
        return filteredSamples;
    }

    /**
     * Available categories for filter.
     * 
     * @return the list of available categories
     */
    public ObservableList<String> getCategories() {
        return categories;
    }

    /**
     * Available languages for filter.
     * 
     * @return the list of available languages
     */
    public ObservableList<String> getLanguages() {
        return languages;
    }

    /**
     * Available difficulty levels for filter.
     * 
     * @return the list of available difficulty levels
     */
    public ObservableList<String> getDifficulties() {
        return difficulties;
    }

    /**
     * Search query text.
     * 
     * @return the search query property
     */
    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    /**
     * Selected category filter.
     * 
     * @return the selected category property
     */
    public StringProperty selectedCategoryProperty() {
        return selectedCategory;
    }

    /**
     * Selected language filter.
     * 
     * @return the selected language property
     */
    public StringProperty selectedLanguageProperty() {
        return selectedLanguage;
    }

    /**
     * Selected difficulty filter.
     * 
     * @return the selected difficulty property
     */
    public StringProperty selectedDifficultyProperty() {
        return selectedDifficulty;
    }

    /**
     * Currently selected sample.
     * 
     * @return the selected sample property
     */
    public ObjectProperty<SampleWorkflow> selectedSampleProperty() {
        return selectedSample;
    }

    /**
     * Gets the currently selected sample.
     * 
     * @return the selected sample
     */
    public SampleWorkflow getSelectedSample() {
        return selectedSample.get();
    }

    /**
     * Whether a sample is selected.
     */
    public ReadOnlyBooleanProperty hasSelectionProperty() {
        return hasSelection;
    }

    /**
     * Whether selected sample has a guide.
     */
    public ReadOnlyBooleanProperty selectedHasGuideProperty() {
        return selectedHasGuide;
    }

    /**
     * Status text showing filter results.
     */
    public ReadOnlyStringProperty statusTextProperty() {
        return statusText;
    }

    // ===== Actions =====

    /**
     * Set callback for import action.
     */
    public void setOnImport(Consumer<SampleWorkflow> callback) {
        this.onImport = callback;
    }

    /**
     * Set callback for view guide action.
     */
    public void setOnViewGuide(Consumer<SampleWorkflow> callback) {
        this.onViewGuide = callback;
    }

    /**
     * Import the selected sample.
     */
    public void importSelected() {
        SampleWorkflow sample = selectedSample.get();
        if (sample != null && onImport != null) {
            onImport.accept(sample);
        }
    }

    /**
     * View guide for the selected sample.
     */
    public void viewGuide() {
        SampleWorkflow sample = selectedSample.get();
        if (sample != null && selectedHasGuide.get() && onViewGuide != null) {
            onViewGuide.accept(sample);
        }
    }

    /**
     * Clear all filters.
     */
    public void clearFilters() {
        searchQuery.set("");
        selectedCategory.set(ALL_CATEGORIES);
        selectedLanguage.set(ALL_LANGUAGES);
        selectedDifficulty.set(ALL_LEVELS);
    }

    // ===== Private Methods =====

    private void applyFilters() {
        String query = searchQuery.get();
        String category = selectedCategory.get();
        String language = selectedLanguage.get();
        String difficulty = selectedDifficulty.get();

        // Parse category
        String categoryFilter = null;
        if (category != null && !category.startsWith("All ")) {
            categoryFilter = category;
        }

        // Parse language
        String languageFilter = null;
        if (language != null && !language.startsWith("All ")) {
            languageFilter = language;
        }

        // Parse difficulty
        Difficulty difficultyFilter = parseDifficulty(difficulty);

        // Apply filters
        final String fCategory = categoryFilter;
        final String fLanguage = languageFilter;
        final Difficulty fDifficulty = difficultyFilter;

        List<SampleWorkflow> filtered = allSamples.stream()
                .filter(s -> matchesQuery(s, query))
                .filter(s -> fCategory == null || s.category().equalsIgnoreCase(fCategory))
                .filter(s -> fLanguage == null || s.language().equalsIgnoreCase(fLanguage))
                .filter(s -> fDifficulty == null || s.difficulty() == fDifficulty)
                .toList();

        filteredSamples.setAll(filtered);
        updateStatusText();
        markDirty();
    }

    private Difficulty parseDifficulty(String difficulty) {
        if (difficulty == null || difficulty.equals(ALL_LEVELS)) {
            return null;
        }
        if (difficulty.contains("Beginner")) {
            return Difficulty.BEGINNER;
        } else if (difficulty.contains("Intermediate")) {
            return Difficulty.INTERMEDIATE;
        } else if (difficulty.contains("Advanced")) {
            return Difficulty.ADVANCED;
        }
        return null;
    }

    private boolean matchesQuery(SampleWorkflow sample, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();

        // Check name
        if (sample.name() != null && sample.name().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check description
        if (sample.description() != null && sample.description().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check tags
        if (sample.tags() != null) {
            for (String tag : sample.tags()) {
                if (tag.toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        }

        // Check category
        return sample.category() != null && sample.category().toLowerCase().contains(lowerQuery);
    }

    private void updateStatusText() {
        int showing = filteredSamples.size();
        int total = allSamples.size();
        if (showing == total) {
            statusText.set("Showing " + total + " samples");
        } else {
            statusText.set("Showing " + showing + " of " + total + " samples");
        }
    }
}
