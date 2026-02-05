/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.viewmodel.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.enums.SettingType;
import ai.nervemind.common.service.SettingsServiceInterface;
import ai.nervemind.ui.viewmodel.dialog.SettingsDialogViewModel.CategoryItem;

/**
 * Unit tests for {@link SettingsDialogViewModel}.
 * Tests all ViewModel logic without requiring JavaFX runtime.
 */
@ExtendWith(MockitoExtension.class)
class SettingsDialogViewModelTest {

    @Mock
    private SettingsServiceInterface settingsService;

    private SettingsDialogViewModel viewModel;

    // Test data
    private SettingDTO booleanSetting;
    private SettingDTO stringSetting;
    private SettingDTO hiddenSetting;

    @BeforeEach
    void setUp() {
        viewModel = new SettingsDialogViewModel(settingsService, null);

        booleanSetting = new SettingDTO(
                1L,
                "general.auto_save",
                "true",
                SettingCategory.GENERAL,
                SettingType.BOOLEAN,
                "Auto Save",
                "Automatically save workflows",
                true,
                false,
                0,
                null,
                Instant.now(),
                Instant.now());

        stringSetting = new SettingDTO(
                2L,
                "general.data_dir",
                "/home/user/data",
                SettingCategory.GENERAL,
                SettingType.PATH,
                "Data Directory",
                "Location for data files",
                true,
                false,
                1,
                null,
                Instant.now(),
                Instant.now());

        hiddenSetting = new SettingDTO(
                3L,
                "general.internal_id",
                "abc123",
                SettingCategory.GENERAL,
                SettingType.STRING,
                "Internal ID",
                null,
                false, // Not visible
                false,
                2,
                null,
                Instant.now(),
                Instant.now());
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with categories")
        void shouldInitializeWithCategories() {
            assertFalse(viewModel.getCategories().isEmpty());
            assertEquals(11, viewModel.getCategories().size());
        }

        @Test
        @DisplayName("Should have General as first category")
        void shouldHaveGeneralAsFirstCategory() {
            CategoryItem first = viewModel.getCategories().get(0);
            assertEquals("General", first.name());
            assertEquals(SettingCategory.GENERAL, first.category());
        }

        @Test
        @DisplayName("Should initialize without changes")
        void shouldInitializeWithoutChanges() {
            assertFalse(viewModel.hasChanges());
            assertTrue(viewModel.getPendingChanges().isEmpty());
        }

        @Test
        @DisplayName("Should not have selected category initially")
        void shouldNotHaveSelectedCategoryInitially() {
            assertNull(viewModel.getSelectedCategory());
        }
    }

    @Nested
    @DisplayName("Category Selection Tests")
    class CategorySelectionTests {

        @Test
        @DisplayName("Should load settings when category selected")
        void shouldLoadSettingsWhenCategorySelected() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, stringSetting, hiddenSetting));

            CategoryItem general = viewModel.getCategories().get(0);
            viewModel.setSelectedCategory(general);

            // Should filter out hidden settings
            assertEquals(2, viewModel.getCurrentSettings().size());
            verify(settingsService).findByCategory(SettingCategory.GENERAL);
        }

        @Test
        @DisplayName("Should filter out non-visible settings")
        void shouldFilterOutNonVisibleSettings() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, hiddenSetting));

            CategoryItem general = viewModel.getCategories().get(0);
            viewModel.setSelectedCategory(general);

            assertEquals(1, viewModel.getCurrentSettings().size());
            assertTrue(viewModel.getCurrentSettings().stream()
                    .allMatch(SettingDTO::visible));
        }
    }

    @Nested
    @DisplayName("Change Tracking Tests")
    class ChangeTrackingTests {

        @BeforeEach
        void loadCategory() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, stringSetting));
            viewModel.setSelectedCategory(viewModel.getCategories().get(0));
        }

        @Test
        @DisplayName("Should track recorded changes")
        void shouldTrackRecordedChanges() {
            assertFalse(viewModel.hasChanges());

            viewModel.recordChange("general.auto_save", "false");

            assertTrue(viewModel.hasChanges());
            assertEquals("false", viewModel.getPendingChanges().get("general.auto_save"));
        }

        @Test
        @DisplayName("Should return pending value over original")
        void shouldReturnPendingValueOverOriginal() {
            viewModel.recordChange("general.auto_save", "false");

            assertEquals("false", viewModel.getCurrentValue(booleanSetting));
        }

        @Test
        @DisplayName("Should return original value when no pending change")
        void shouldReturnOriginalValueWhenNoPendingChange() {
            assertEquals("true", viewModel.getCurrentValue(booleanSetting));
        }

        @Test
        @DisplayName("Should mark as dirty when changes recorded")
        void shouldMarkAsDirtyWhenChangesRecorded() {
            assertFalse(viewModel.isDirty());

            viewModel.recordChange("general.auto_save", "false");

            assertTrue(viewModel.isDirty());
        }
    }

    @Nested
    @DisplayName("Save Changes Tests")
    class SaveChangesTests {

        @BeforeEach
        void loadCategory() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, stringSetting));
            viewModel.setSelectedCategory(viewModel.getCategories().get(0));
        }

        @Test
        @DisplayName("Should save all pending changes")
        void shouldSaveAllPendingChanges() {
            viewModel.recordChange("general.auto_save", "false");
            viewModel.recordChange("general.data_dir", "/new/path");

            boolean result = viewModel.saveChanges();

            assertTrue(result);
            verify(settingsService).setValue("general.auto_save", "false");
            verify(settingsService).setValue("general.data_dir", "/new/path");
        }

        @Test
        @DisplayName("Should clear pending changes after save")
        void shouldClearPendingChangesAfterSave() {
            viewModel.recordChange("general.auto_save", "false");

            viewModel.saveChanges();

            assertTrue(viewModel.getPendingChanges().isEmpty());
            assertFalse(viewModel.hasChanges());
        }

        @Test
        @DisplayName("Should return true when no changes to save")
        void shouldReturnTrueWhenNoChangesToSave() {
            assertTrue(viewModel.saveChanges());
        }

        @Test
        @DisplayName("Should handle save error")
        void shouldHandleSaveError() {
            viewModel.recordChange("general.auto_save", "false");
            doThrow(new RuntimeException("DB error"))
                    .when(settingsService).setValue(anyString(), anyString());

            boolean result = viewModel.saveChanges();

            assertFalse(result);
            assertTrue(viewModel.getErrorMessage().contains("Failed to save settings"));
        }
    }

    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {

        @BeforeEach
        void loadCategory() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, stringSetting));
            viewModel.setSelectedCategory(viewModel.getCategories().get(0));
        }

        @Test
        @DisplayName("Should reset single setting")
        void shouldResetSingleSetting() {
            boolean result = viewModel.resetSetting("general.auto_save");

            assertTrue(result);
            verify(settingsService).resetToDefault("general.auto_save");
        }

        @Test
        @DisplayName("Should remove pending change when reset")
        void shouldRemovePendingChangeWhenReset() {
            viewModel.recordChange("general.auto_save", "false");
            assertTrue(viewModel.hasChanges());

            viewModel.resetSetting("general.auto_save");

            assertFalse(viewModel.getPendingChanges().containsKey("general.auto_save"));
        }

        @Test
        @DisplayName("Should reset all settings in category")
        void shouldResetAllSettingsInCategory() {
            boolean result = viewModel.resetCategoryToDefaults();

            assertTrue(result);
            verify(settingsService).resetToDefault("general.auto_save");
            verify(settingsService).resetToDefault("general.data_dir");
        }

        @Test
        @DisplayName("Should not reset when no category selected")
        void shouldNotResetWhenNoCategorySelected() {
            viewModel = new SettingsDialogViewModel(settingsService, null);

            boolean result = viewModel.resetCategoryToDefaults();

            assertFalse(result);
            verify(settingsService, never()).resetToDefault(anyString());
        }
    }

    @Nested
    @DisplayName("Discard Changes Tests")
    class DiscardChangesTests {

        @BeforeEach
        void loadCategory() {
            when(settingsService.findByCategory(SettingCategory.GENERAL))
                    .thenReturn(List.of(booleanSetting, stringSetting));
            viewModel.setSelectedCategory(viewModel.getCategories().get(0));
        }

        @Test
        @DisplayName("Should discard all pending changes")
        void shouldDiscardAllPendingChanges() {
            viewModel.recordChange("general.auto_save", "false");
            viewModel.recordChange("general.data_dir", "/new/path");
            assertTrue(viewModel.hasChanges());

            viewModel.discardChanges();

            assertFalse(viewModel.hasChanges());
            assertTrue(viewModel.getPendingChanges().isEmpty());
        }

        @Test
        @DisplayName("Should clear dirty flag on discard")
        void shouldClearDirtyFlagOnDiscard() {
            viewModel.recordChange("general.auto_save", "false");
            assertTrue(viewModel.isDirty());

            viewModel.discardChanges();

            assertFalse(viewModel.isDirty());
        }
    }

    @Nested
    @DisplayName("Static Helper Tests")
    class StaticHelperTests {

        @Test
        @DisplayName("Should format key to label")
        void shouldFormatKeyToLabel() {
            assertEquals("Api Key", SettingsDialogViewModel.formatKeyToLabel("ai.openai.api_key"));
            assertEquals("Auto Save", SettingsDialogViewModel.formatKeyToLabel("general.auto_save"));
            assertEquals("Max Retries", SettingsDialogViewModel.formatKeyToLabel("http.max_retries"));
        }

        @Test
        @DisplayName("Should extract section from key")
        void shouldExtractSectionFromKey() {
            assertEquals("openai", SettingsDialogViewModel.extractSection("ai.openai.api_key"));
            assertEquals("general", SettingsDialogViewModel.extractSection("auto_save")); // No second dot
        }

        @Test
        @DisplayName("Should format section name")
        void shouldFormatSectionName() {
            assertEquals("OpenAI", SettingsDialogViewModel.formatSectionName("openai"));
            assertEquals("Anthropic", SettingsDialogViewModel.formatSectionName("anthropic"));
            assertEquals("Azure OpenAI", SettingsDialogViewModel.formatSectionName("azure"));
            assertEquals("Custom section", SettingsDialogViewModel.formatSectionName("custom_section"));
        }
    }
}
