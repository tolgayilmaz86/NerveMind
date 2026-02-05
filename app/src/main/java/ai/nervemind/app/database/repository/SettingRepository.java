package ai.nervemind.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.nervemind.app.database.model.SettingEntity;
import ai.nervemind.common.enums.SettingCategory;

/**
 * Repository for Setting entities.
 */
@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, Long> {

    /**
     * Find setting by unique key.
     *
     * @param key the setting key to search for
     * @return optional containing the setting if found
     */
    Optional<SettingEntity> findByKey(String key);

    /**
     * Find all settings in a category.
     *
     * @param category the category to filter by
     * @return list of settings in the category, ordered by display order
     */
    List<SettingEntity> findByCategoryOrderByDisplayOrderAsc(SettingCategory category);

    /**
     * Find all visible settings in a category.
     *
     * @param category the category to filter by
     * @return list of visible settings in the category, ordered by display order
     */
    List<SettingEntity> findByCategoryAndVisibleTrueOrderByDisplayOrderAsc(SettingCategory category);

    /**
     * Find all visible settings.
     *
     * @return list of all visible settings, ordered by category then display order
     */
    List<SettingEntity> findByVisibleTrueOrderByCategoryAscDisplayOrderAsc();

    /**
     * Find settings by key prefix (e.g., "ai.openai." for all OpenAI settings).
     *
     * @param prefix the key prefix to search for
     * @return list of settings with keys starting with the prefix
     */
    @Query("SELECT s FROM SettingEntity s WHERE s.key LIKE :prefix% ORDER BY s.displayOrder ASC")
    List<SettingEntity> findByKeyPrefix(String prefix);

    /**
     * Delete setting by key.
     *
     * @param key the key of the setting to delete
     */
    void deleteByKey(String key);

    /**
     * Check if setting exists by key.
     *
     * @param key the key to check for existence
     * @return true if a setting with the key exists
     */
    boolean existsByKey(String key);

    /**
     * Find settings requiring restart.
     *
     * @return list of settings that require application restart when changed
     */
    List<SettingEntity> findByRequiresRestartTrue();

    /**
     * Search settings by key or label.
     *
     * @param query the search query to match against keys and labels
     * @return list of settings matching the search query
     */
    @Query("SELECT s FROM SettingEntity s WHERE s.visible = true AND " +
            "(LOWER(s.key) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.label) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<SettingEntity> searchSettings(String query);
}
