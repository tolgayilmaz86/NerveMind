package ai.nervemind.app.database.model;

import java.time.Instant;

import ai.nervemind.common.enums.SettingCategory;
import ai.nervemind.common.enums.SettingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * JPA Entity for Application Settings.
 * Stores user preferences and application configuration as key-value pairs.
 * Sensitive values (type=PASSWORD) are stored encrypted.
 */
@Entity
@Table(name = "settings", indexes = {
        @Index(name = "idx_settings_key", columnList = "setting_key", unique = true),
        @Index(name = "idx_settings_category", columnList = "category")
})
public class SettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique key identifying the setting (e.g., "general.theme",
     * "ai.openai.apiKey").
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 255)
    private String key;

    /**
     * The setting value (stored as string, converted based on type).
     * For PASSWORD type, this is encrypted.
     */
    @Column(name = "setting_value", columnDefinition = "CLOB")
    private String value;

    /**
     * Category for organizing settings in the UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingCategory category;

    /**
     * Data type for proper parsing and validation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false)
    private SettingType type;

    /**
     * Human-readable label for display.
     */
    @Column(length = 255)
    private String label;

    /**
     * Description/help text for the setting.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this setting is visible in the UI.
     */
    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    /**
     * Whether this setting requires application restart.
     */
    @Column(name = "requires_restart", nullable = false)
    private boolean requiresRestart = false;

    /**
     * Order for display within category (lower = first).
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Validation rules as JSON (e.g., {"min": 1, "max": 100}).
     */
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    /**
     * Default constructor for JPA entity instantiation.
     * Required for JPA proxy creation and deserialization.
     */
    protected SettingEntity() {
    }

    /**
     * Creates a new setting entity with the specified parameters.
     * Timestamps are automatically set to current time.
     *
     * @param key      the unique setting key identifier
     * @param value    the setting value as a string
     * @param category the category this setting belongs to
     * @param type     the data type of the setting value
     */
    public SettingEntity(String key, String value, SettingCategory category, SettingType type) {
        this.key = key;
        this.value = value;
        this.category = category;
        this.type = type;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Lifecycle callback executed before the entity is persisted.
     * Initializes timestamps if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    /**
     * Lifecycle callback executed before the entity is updated.
     * Updates the last modified timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    /**
     * Gets the unique identifier of this setting.
     *
     * @return the setting ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this setting.
     * This method is primarily used by JPA and should not be called directly.
     *
     * @param id the setting ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the unique key identifier for this setting.
     *
     * @return the setting key (e.g., "general.theme")
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the unique key identifier for this setting.
     *
     * @param key the setting key to set (e.g., "general.theme")
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the value of this setting as a string.
     * For PASSWORD type settings, this returns the encrypted value.
     *
     * @return the setting value as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this setting.
     * For PASSWORD type settings, the value should be pre-encrypted.
     *
     * @param value the setting value to set as a string
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the category this setting belongs to.
     *
     * @return the setting category
     */
    public SettingCategory getCategory() {
        return category;
    }

    /**
     * Sets the category this setting belongs to.
     *
     * @param category the setting category to set
     */
    public void setCategory(SettingCategory category) {
        this.category = category;
    }

    /**
     * Gets the data type of this setting's value.
     *
     * @return the setting type
     */
    public SettingType getType() {
        return type;
    }

    /**
     * Sets the data type of this setting's value.
     *
     * @param type the setting type to set
     */
    public void setType(SettingType type) {
        this.type = type;
    }

    /**
     * Gets the human-readable label for this setting.
     *
     * @return the setting label for UI display
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the human-readable label for this setting.
     *
     * @param label the setting label for UI display
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the description of this setting.
     *
     * @return the setting description explaining its purpose
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this setting.
     *
     * @param description the setting description explaining its purpose
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if this setting should be visible in the UI.
     *
     * @return true if the setting should be displayed in the UI, false otherwise
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether this setting should be visible in the UI.
     *
     * @param visible true if the setting should be displayed in the UI, false
     *                otherwise
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Checks if changing this setting requires an application restart.
     *
     * @return true if the application needs to be restarted after changing this
     *         setting
     */
    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    /**
     * Sets whether changing this setting requires an application restart.
     *
     * @param requiresRestart true if the application needs to be restarted after
     *                        changing this setting
     */
    public void setRequiresRestart(boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    /**
     * Gets the display order for this setting within its category.
     *
     * @return the display order (lower numbers appear first), or null for default
     *         ordering
     */
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Sets the display order for this setting within its category.
     *
     * @param displayOrder the display order (lower numbers appear first), or null
     *                     for default ordering
     */
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Gets the validation rules for this setting's value.
     *
     * @return the validation rules as a string, or null if no validation is
     *         required
     */
    public String getValidationRules() {
        return validationRules;
    }

    /**
     * Sets the validation rules for this setting's value.
     *
     * @param validationRules the validation rules as a string, or null if no
     *                        validation is required
     */
    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    /**
     * Gets the timestamp when this setting was first created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this setting was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
