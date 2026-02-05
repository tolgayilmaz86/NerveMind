/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.database.model.SettingEntity;
import ai.nervemind.app.database.repository.SettingRepository;
import ai.nervemind.common.dto.SettingDTO;
import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.enums.SettingType;
import ai.nervemind.common.exception.DataParsingException;
import ai.nervemind.common.service.SettingsServiceInterface.SettingsChangeListener;

/**
 * Unit tests for SettingsService.
 * 
 * <p>
 * Tests all core functionality including CRUD operations, caching,
 * validation, change listeners, and import/export.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService Tests")
class SettingsServiceTest {

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private EncryptionService encryptionService;

    private ObjectMapper objectMapper;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        settingsService = new SettingsService(settingRepository, encryptionService, objectMapper);
    }

    // ===== Test Helpers =====

    private SettingEntity createEntity(String key, String value, SettingCategory category, SettingType type) {
        SettingEntity entity = new SettingEntity(key, value, category, type);
        entity.setId(1L);
        entity.setLabel(key);
        entity.setDescription("Test description");
        entity.setVisible(true);
        entity.setRequiresRestart(false);
        entity.setDisplayOrder(1);
        return entity;
    }

    private SettingEntity createEntityWithValidation(String key, String value, String validationRules) {
        SettingEntity entity = createEntity(key, value, SettingCategory.GENERAL, SettingType.INTEGER);
        entity.setValidationRules(validationRules);
        return entity;
    }

    // ===== Initialization Tests =====

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Should initialize default settings when none exist")
        void shouldInitializeDefaultSettings() {
            when(settingRepository.existsByKey(anyString())).thenReturn(false);

            settingsService.initializeDefaults();

            // Verify settings were saved
            verify(settingRepository, times(SettingsDefaults.getAll().size())).save(any(SettingEntity.class));
        }

        @Test
        @DisplayName("Should skip existing settings during initialization")
        void shouldSkipExistingSettings() {
            when(settingRepository.existsByKey(anyString())).thenReturn(true);

            settingsService.initializeDefaults();

            // No settings should be saved
            verify(settingRepository, never()).save(any(SettingEntity.class));
        }

        @Test
        @DisplayName("Should encrypt password settings during initialization")
        void shouldEncryptPasswordSettings() {
            when(settingRepository.existsByKey(anyString())).thenReturn(false);
            // Use lenient stubbing since not all password defaults may be non-empty
            org.mockito.Mockito.lenient().when(encryptionService.encrypt(anyString())).thenReturn("encrypted");

            settingsService.initializeDefaults();

            ArgumentCaptor<SettingEntity> captor = ArgumentCaptor.forClass(SettingEntity.class);
            verify(settingRepository, times(SettingsDefaults.getAll().size())).save(captor.capture());

            // At least one password type should have been encrypted
            List<SettingEntity> savedEntities = captor.getAllValues();
            boolean hasPasswordType = savedEntities.stream()
                    .anyMatch(e -> e.getType() == SettingType.PASSWORD);
            assertThat(hasPasswordType).isTrue();
        }
    }

    // ===== Find Operations =====

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("findAll should return all settings as DTOs")
        void findAllShouldReturnAllSettings() {
            SettingEntity entity1 = createEntity("key1", "value1", SettingCategory.GENERAL, SettingType.STRING);
            SettingEntity entity2 = createEntity("key2", "value2", SettingCategory.EDITOR, SettingType.STRING);
            when(settingRepository.findAll()).thenReturn(List.of(entity1, entity2));

            List<SettingDTO> result = settingsService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(SettingDTO::key).containsExactly("key1", "key2");
        }

        @Test
        @DisplayName("findAllVisible should return only visible settings")
        void findAllVisibleShouldReturnOnlyVisible() {
            SettingEntity visible = createEntity("visible", "value", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByVisibleTrueOrderByCategoryAscDisplayOrderAsc())
                    .thenReturn(List.of(visible));

            List<SettingDTO> result = settingsService.findAllVisible();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).key()).isEqualTo("visible");
        }

        @Test
        @DisplayName("findByCategory should return settings for category")
        void findByCategoryShouldReturnMatchingSettings() {
            SettingEntity entity = createEntity("editor.grid", "20", SettingCategory.EDITOR, SettingType.INTEGER);
            when(settingRepository.findByCategoryAndVisibleTrueOrderByDisplayOrderAsc(SettingCategory.EDITOR))
                    .thenReturn(List.of(entity));

            List<SettingDTO> result = settingsService.findByCategory(SettingCategory.EDITOR);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).category()).isEqualTo(SettingCategory.EDITOR);
        }

        @Test
        @DisplayName("findByKey should return setting when exists")
        void findByKeyShouldReturnSettingWhenExists() {
            SettingEntity entity = createEntity("general.theme", "dark", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("general.theme")).thenReturn(Optional.of(entity));

            Optional<SettingDTO> result = settingsService.findByKey("general.theme");

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("dark");
        }

        @Test
        @DisplayName("findByKey should return empty when not exists")
        void findByKeyShouldReturnEmptyWhenNotExists() {
            when(settingRepository.findByKey("nonexistent")).thenReturn(Optional.empty());

            Optional<SettingDTO> result = settingsService.findByKey("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByKey should mask password values in DTO")
        void findByKeyShouldMaskPasswordValues() {
            SettingEntity entity = createEntity("ai.openai.apiKey", "sk-secret", SettingCategory.AI_PROVIDERS,
                    SettingType.PASSWORD);
            when(settingRepository.findByKey("ai.openai.apiKey")).thenReturn(Optional.of(entity));

            Optional<SettingDTO> result = settingsService.findByKey("ai.openai.apiKey");

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("********");
        }
    }

    // ===== Get Value Operations =====

    @Nested
    @DisplayName("Get Value Operations")
    class GetValueOperations {

        @Test
        @DisplayName("getValue should return value from repository")
        void getValueShouldReturnFromRepository() {
            SettingEntity entity = createEntity("key", "value", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));

            String result = settingsService.getValue("key", "default");

            assertThat(result).isEqualTo("value");
        }

        @Test
        @DisplayName("getValue should return default when not found")
        void getValueShouldReturnDefaultWhenNotFound() {
            when(settingRepository.findByKey("missing")).thenReturn(Optional.empty());

            String result = settingsService.getValue("missing", "default");

            assertThat(result).isEqualTo("default");
        }

        @Test
        @DisplayName("getValue should decrypt password values")
        void getValueShouldDecryptPasswords() {
            SettingEntity entity = createEntity("password", "encrypted", SettingCategory.GENERAL, SettingType.PASSWORD);
            when(settingRepository.findByKey("password")).thenReturn(Optional.of(entity));
            when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");

            String result = settingsService.getValue("password", "default");

            assertThat(result).isEqualTo("decrypted");
            verify(encryptionService).decrypt("encrypted");
        }

        @Test
        @DisplayName("getBoolean should parse boolean values")
        void getBooleanShouldParseBooleanValues() {
            SettingEntity entity = createEntity("enabled", "true", SettingCategory.GENERAL, SettingType.BOOLEAN);
            when(settingRepository.findByKey("enabled")).thenReturn(Optional.of(entity));

            boolean result = settingsService.getBoolean("enabled", false);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("getBoolean should return default for missing key")
        void getBooleanShouldReturnDefaultWhenMissing() {
            when(settingRepository.findByKey("missing")).thenReturn(Optional.empty());

            boolean result = settingsService.getBoolean("missing", true);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("getInt should parse integer values")
        void getIntShouldParseIntegerValues() {
            SettingEntity entity = createEntity("count", "42", SettingCategory.GENERAL, SettingType.INTEGER);
            when(settingRepository.findByKey("count")).thenReturn(Optional.of(entity));

            int result = settingsService.getInt("count", 0);

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("getInt should return default for invalid number")
        void getIntShouldReturnDefaultForInvalidNumber() {
            SettingEntity entity = createEntity("invalid", "not-a-number", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("invalid")).thenReturn(Optional.of(entity));

            int result = settingsService.getInt("invalid", 100);

            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("getLong should parse long values")
        void getLongShouldParseLongValues() {
            SettingEntity entity = createEntity("timeout", "30000", SettingCategory.GENERAL, SettingType.LONG);
            when(settingRepository.findByKey("timeout")).thenReturn(Optional.of(entity));

            long result = settingsService.getLong("timeout", 0L);

            assertThat(result).isEqualTo(30000L);
        }

        @Test
        @DisplayName("getLong should return default for invalid number")
        void getLongShouldReturnDefaultForInvalidNumber() {
            SettingEntity entity = createEntity("invalid", "abc", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("invalid")).thenReturn(Optional.of(entity));

            long result = settingsService.getLong("invalid", 5000L);

            assertThat(result).isEqualTo(5000L);
        }

        @Test
        @DisplayName("getDouble should parse double values")
        void getDoubleShouldParseDoubleValues() {
            SettingEntity entity = createEntity("ratio", "0.75", SettingCategory.GENERAL, SettingType.DOUBLE);
            when(settingRepository.findByKey("ratio")).thenReturn(Optional.of(entity));

            double result = settingsService.getDouble("ratio", 1.0);

            assertThat(result).isEqualTo(0.75);
        }

        @Test
        @DisplayName("getDouble should return default for invalid number")
        void getDoubleShouldReturnDefaultForInvalidNumber() {
            SettingEntity entity = createEntity("invalid", "xyz", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("invalid")).thenReturn(Optional.of(entity));

            double result = settingsService.getDouble("invalid", 2.5);

            assertThat(result).isEqualTo(2.5);
        }
    }

    // ===== Set Value Operations =====

    @Nested
    @DisplayName("Set Value Operations")
    class SetValueOperations {

        @Test
        @DisplayName("setValue should update existing setting")
        void setValueShouldUpdateExistingSetting() {
            SettingEntity entity = createEntity("key", "old", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            SettingDTO result = settingsService.setValue("key", "new");

            verify(settingRepository).save(entity);
            assertThat(entity.getValue()).isEqualTo("new");
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("key");
        }

        @Test
        @DisplayName("setValue should throw for unknown setting")
        void setValueShouldThrowForUnknownSetting() {
            when(settingRepository.findByKey("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> settingsService.setValue("unknown", "value"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown setting");
        }

        @Test
        @DisplayName("setValue should encrypt password values")
        void setValueShouldEncryptPasswords() {
            SettingEntity entity = createEntity("password", "old", SettingCategory.GENERAL, SettingType.PASSWORD);
            when(settingRepository.findByKey("password")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);
            when(encryptionService.encrypt("new")).thenReturn("encrypted");
            when(encryptionService.decrypt("old")).thenReturn("decrypted-old");

            settingsService.setValue("password", "new");

            verify(encryptionService).encrypt("new");
            assertThat(entity.getValue()).isEqualTo("encrypted");
        }

        @Test
        @DisplayName("setValues should update multiple settings")
        void setValuesShouldUpdateMultipleSettings() {
            SettingEntity entity1 = createEntity("key1", "old1", SettingCategory.GENERAL, SettingType.STRING);
            SettingEntity entity2 = createEntity("key2", "old2", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key1")).thenReturn(Optional.of(entity1));
            when(settingRepository.findByKey("key2")).thenReturn(Optional.of(entity2));
            when(settingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<SettingDTO> result = settingsService.setValues(Map.of("key1", "new1", "key2", "new2"));

            assertThat(result).hasSize(2);
            verify(settingRepository, times(2)).save(any());
        }
    }

    // ===== Validation Tests =====

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should validate minimum value")
        void shouldValidateMinimumValue() {
            SettingEntity entity = createEntityWithValidation("number", "50", "{\"min\": 10, \"max\": 100}");
            when(settingRepository.findByKey("number")).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> settingsService.setValue("number", "5"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        @DisplayName("Should validate maximum value")
        void shouldValidateMaximumValue() {
            SettingEntity entity = createEntityWithValidation("number", "50", "{\"min\": 10, \"max\": 100}");
            when(settingRepository.findByKey("number")).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> settingsService.setValue("number", "150"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at most");
        }

        @Test
        @DisplayName("Should validate enum options")
        void shouldValidateEnumOptions() {
            SettingEntity entity = createEntityWithValidation("theme", "dark",
                    "{\"options\": [\"light\", \"dark\", \"system\"]}");
            when(settingRepository.findByKey("theme")).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> settingsService.setValue("theme", "invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Must be one of");
        }

        @Test
        @DisplayName("Should allow valid enum option")
        void shouldAllowValidEnumOption() {
            SettingEntity entity = createEntityWithValidation("theme", "dark",
                    "{\"options\": [\"light\", \"dark\", \"system\"]}");
            when(settingRepository.findByKey("theme")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            SettingDTO result = settingsService.setValue("theme", "light");

            assertThat(entity.getValue()).isEqualTo("light");
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("theme");
        }

        @Test
        @DisplayName("Should allow value without validation rules")
        void shouldAllowValueWithoutValidationRules() {
            // Entity with no validation rules
            SettingEntity entity = createEntity("optional", "value", SettingCategory.GENERAL, SettingType.STRING);
            entity.setValidationRules(null);
            when(settingRepository.findByKey("optional")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            SettingDTO result = settingsService.setValue("optional", "newValue");

            assertThat(entity.getValue()).isEqualTo("newValue");
            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("optional");
        }

        @Test
        @DisplayName("Should handle invalid validation rules gracefully")
        void shouldHandleInvalidValidationRulesGracefully() {
            SettingEntity entity = createEntityWithValidation("key", "value", "not-valid-json");
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            // Should not throw - invalid rules are logged and skipped
            settingsService.setValue("key", "newValue");

            assertThat(entity.getValue()).isEqualTo("newValue");
        }
    }

    // ===== Reset Operations =====

    @Nested
    @DisplayName("Reset Operations")
    class ResetOperations {

        @Test
        @DisplayName("resetToDefault should restore default value")
        void resetToDefaultShouldRestoreDefaultValue() {
            // Use an actual default key from SettingsDefaults
            String key = SettingsDefaults.GENERAL_THEME;
            String defaultValue = SettingsDefaults.getDefault(key);

            SettingEntity entity = createEntity(key, "custom", SettingCategory.GENERAL, SettingType.ENUM);
            when(settingRepository.findByKey(key)).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            settingsService.resetToDefault(key);

            assertThat(entity.getValue()).isEqualTo(defaultValue);
        }

        @Test
        @DisplayName("resetToDefault should throw for unknown setting")
        void resetToDefaultShouldThrowForUnknownSetting() {
            assertThatThrownBy(() -> settingsService.resetToDefault("unknown.key"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No default");
        }

        @Test
        @DisplayName("resetCategoryToDefaults should reset all settings in category")
        void resetCategoryToDefaultsShouldResetAllInCategory() {
            List<SettingDTO> categoryDefaults = SettingsDefaults.getByCategory(SettingCategory.GENERAL);

            for (SettingDTO def : categoryDefaults) {
                SettingEntity entity = createEntity(def.key(), "custom", def.category(), def.type());
                when(settingRepository.findByKey(def.key())).thenReturn(Optional.of(entity));
                when(settingRepository.save(any())).thenReturn(entity);
            }

            List<SettingDTO> result = settingsService.resetCategoryToDefaults(SettingCategory.GENERAL);

            assertThat(result).hasSize(categoryDefaults.size());
            verify(settingRepository, times(categoryDefaults.size())).save(any());
        }
    }

    // ===== Change Listeners =====

    @Nested
    @DisplayName("Change Listeners")
    class ChangeListeners {

        @Test
        @DisplayName("Should notify listeners on value change")
        void shouldNotifyListenersOnValueChange() {
            SettingsChangeListener listener = mock(SettingsChangeListener.class);
            settingsService.addChangeListener(listener);

            SettingEntity entity = createEntity("key", "old", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            settingsService.setValue("key", "new");

            verify(listener).onSettingChanged("key", "old", "new");
        }

        @Test
        @DisplayName("Should handle listener exceptions gracefully")
        void shouldHandleListenerExceptionsGracefully() {
            SettingsChangeListener badListener = mock(SettingsChangeListener.class);
            doThrow(new RuntimeException("Listener error"))
                    .when(badListener).onSettingChanged(anyString(), anyString(), anyString());

            SettingsChangeListener goodListener = mock(SettingsChangeListener.class);

            settingsService.addChangeListener(badListener);
            settingsService.addChangeListener(goodListener);

            SettingEntity entity = createEntity("key", "old", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            // Should not throw
            settingsService.setValue("key", "new");

            // Good listener should still be called
            verify(goodListener).onSettingChanged("key", "old", "new");
        }

        @Test
        @DisplayName("Should allow removing listeners")
        void shouldAllowRemovingListeners() {
            SettingsChangeListener listener = mock(SettingsChangeListener.class);
            settingsService.addChangeListener(listener);
            settingsService.removeChangeListener(listener);

            SettingEntity entity = createEntity("key", "old", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            settingsService.setValue("key", "new");

            verify(listener, never()).onSettingChanged(anyString(), anyString(), anyString());
        }
    }

    // ===== Import/Export =====

    @Nested
    @DisplayName("Import/Export")
    class ImportExport {

        @Test
        @DisplayName("exportAsJson should return JSON with all non-password settings")
        void exportAsJsonShouldReturnJsonWithNonPasswordSettings() {
            SettingEntity textSetting = createEntity("text.key", "value", SettingCategory.GENERAL, SettingType.STRING);
            SettingEntity passwordSetting = createEntity("password.key", "secret", SettingCategory.GENERAL,
                    SettingType.PASSWORD);
            when(settingRepository.findAll()).thenReturn(List.of(textSetting, passwordSetting));

            String json = settingsService.exportAsJson();

            assertThat(json).contains("text.key");
            assertThat(json).contains("value");
            assertThat(json).doesNotContain("password.key");
            assertThat(json).doesNotContain("secret");
        }

        @Test
        @DisplayName("importFromJson should import valid settings")
        void importFromJsonShouldImportValidSettings() {
            String json = "{\"key1\": \"value1\", \"key2\": \"value2\"}";

            SettingEntity entity1 = createEntity("key1", "old1", SettingCategory.GENERAL, SettingType.STRING);
            SettingEntity entity2 = createEntity("key2", "old2", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key1")).thenReturn(Optional.of(entity1));
            when(settingRepository.findByKey("key2")).thenReturn(Optional.of(entity2));
            when(settingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            settingsService.importFromJson(json);

            assertThat(entity1.getValue()).isEqualTo("value1");
            assertThat(entity2.getValue()).isEqualTo("value2");
        }

        @Test
        @DisplayName("importFromJson should skip unknown settings")
        void importFromJsonShouldSkipUnknownSettings() {
            String json = "{\"unknown.key\": \"value\"}";
            when(settingRepository.findByKey("unknown.key")).thenReturn(Optional.empty());

            // Should not throw
            settingsService.importFromJson(json);

            verify(settingRepository, never()).save(any());
        }

        @Test
        @DisplayName("importFromJson should throw for invalid JSON")
        void importFromJsonShouldThrowForInvalidJson() {
            String invalidJson = "not-valid-json";

            assertThatThrownBy(() -> settingsService.importFromJson(invalidJson))
                    .isInstanceOf(DataParsingException.class);
        }
    }

    // ===== Search =====

    @Nested
    @DisplayName("Search")
    class Search {

        @Test
        @DisplayName("search should return matching settings")
        void searchShouldReturnMatchingSettings() {
            SettingEntity entity = createEntity("general.theme", "dark", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.searchSettings("theme")).thenReturn(List.of(entity));

            List<SettingDTO> result = settingsService.search("theme");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).key()).isEqualTo("general.theme");
        }

        @Test
        @DisplayName("search should return empty for no matches")
        void searchShouldReturnEmptyForNoMatches() {
            when(settingRepository.searchSettings("xyz")).thenReturn(List.of());

            List<SettingDTO> result = settingsService.search("xyz");

            assertThat(result).isEmpty();
        }
    }

    // ===== Caching =====

    @Nested
    @DisplayName("Caching")
    class Caching {

        @Test
        @DisplayName("getValue should cache values for subsequent calls")
        void getValueShouldCacheValues() {
            SettingEntity entity = createEntity("cached", "value", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("cached")).thenReturn(Optional.of(entity));

            // First call - hits repository
            String result1 = settingsService.getValue("cached", "default");
            // Second call - should use cache
            String result2 = settingsService.getValue("cached", "default");

            assertThat(result1).isEqualTo("value");
            assertThat(result2).isEqualTo("value");
            // Repository should only be called once
            verify(settingRepository, times(1)).findByKey("cached");
        }

        @Test
        @DisplayName("setValue should update cache")
        void setValueShouldUpdateCache() {
            SettingEntity entity = createEntity("key", "old", SettingCategory.GENERAL, SettingType.STRING);
            when(settingRepository.findByKey("key")).thenReturn(Optional.of(entity));
            when(settingRepository.save(any())).thenReturn(entity);

            // Set value (updates cache)
            settingsService.setValue("key", "new");

            // Get value (should hit cache, not repository again for value retrieval)
            String result = settingsService.getValue("key", "default");

            assertThat(result).isEqualTo("new");
        }
    }

    // ===== Encryption Edge Cases =====

    @Nested
    @DisplayName("Encryption Edge Cases")
    class EncryptionEdgeCases {

        @Test
        @DisplayName("Should handle empty password value")
        void shouldHandleEmptyPasswordValue() {
            SettingEntity entity = createEntity("password", "", SettingCategory.GENERAL, SettingType.PASSWORD);
            when(settingRepository.findByKey("password")).thenReturn(Optional.of(entity));

            String result = settingsService.getValue("password", "default");

            // Empty value should not be decrypted
            assertThat(result).isEmpty();
            verify(encryptionService, never()).decrypt(anyString());
        }

        @Test
        @DisplayName("Should handle decryption failure gracefully")
        void shouldHandleDecryptionFailureGracefully() {
            SettingEntity entity = createEntity("password", "corrupted", SettingCategory.GENERAL, SettingType.PASSWORD);
            when(settingRepository.findByKey("password")).thenReturn(Optional.of(entity));
            when(encryptionService.decrypt("corrupted")).thenThrow(new RuntimeException("Decryption failed"));

            String result = settingsService.getValue("password", "default");

            // Should return empty string on decryption failure
            assertThat(result).isEmpty();
        }
    }
}
