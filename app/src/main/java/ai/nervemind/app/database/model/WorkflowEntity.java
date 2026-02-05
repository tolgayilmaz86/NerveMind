package ai.nervemind.app.database.model;

import java.time.Instant;

import ai.nervemind.common.enums.TriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA Entity for Workflow.
 * Stores workflow definition including nodes and connections as JSON.
 */
@Entity
@Table(name = "workflows")
public class WorkflowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "nodes_json", columnDefinition = "CLOB", nullable = false)
    private String nodesJson;

    @Column(name = "connections_json", columnDefinition = "CLOB", nullable = false)
    private String connectionsJson;

    @Column(name = "settings_json", columnDefinition = "CLOB")
    private String settingsJson;

    @Column(name = "is_active")
    private boolean isActive = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType = TriggerType.MANUAL;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_executed")
    private Instant lastExecuted;

    @Version
    private int version = 1;

    // Default constructor for JPA
    /**
     * Default constructor for JPA.
     */
    protected WorkflowEntity() {
    }

    /**
     * Creates a new workflow with the specified name.
     * Initializes with empty nodes and connections arrays.
     *
     * @param name the name of the workflow
     */
    public WorkflowEntity(String name) {
        this.name = name;
        this.nodesJson = "[]";
        this.connectionsJson = "[]";
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
     * Gets the workflow ID.
     * 
     * @return the workflow ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the workflow ID.
     * 
     * @param id the workflow ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the workflow name.
     * 
     * @return the workflow name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the workflow name.
     * 
     * @param name the workflow name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the workflow description.
     * 
     * @return the workflow description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the workflow description.
     * 
     * @param description the workflow description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the serialized nodes JSON.
     * 
     * @return the serialized nodes JSON
     */
    public String getNodesJson() {
        return nodesJson;
    }

    /**
     * Sets the serialized nodes JSON.
     * 
     * @param nodesJson the nodes JSON to set
     */
    public void setNodesJson(String nodesJson) {
        this.nodesJson = nodesJson;
    }

    /**
     * Gets the serialized connections JSON.
     * 
     * @return the serialized connections JSON
     */
    public String getConnectionsJson() {
        return connectionsJson;
    }

    /**
     * Sets the serialized connections JSON.
     * 
     * @param connectionsJson the connections JSON to set
     */
    public void setConnectionsJson(String connectionsJson) {
        this.connectionsJson = connectionsJson;
    }

    /**
     * Gets the serialized settings JSON.
     * 
     * @return the serialized settings JSON
     */
    public String getSettingsJson() {
        return settingsJson;
    }

    /**
     * Sets the serialized settings JSON.
     * 
     * @param settingsJson the settings JSON to set
     */
    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    /**
     * Checks if the workflow is active.
     * 
     * @return true if the workflow is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the active status of the workflow.
     * 
     * @param active the active status to set
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Gets the trigger type.
     * 
     * @return the trigger type
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Sets the trigger type.
     * 
     * @param triggerType the trigger type to set
     */
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * Gets the cron expression.
     * 
     * @return the cron expression
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression.
     * 
     * @param cronExpression the cron expression to set
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Gets the creation timestamp.
     * 
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     * 
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the update timestamp.
     * 
     * @return the update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the update timestamp.
     * 
     * @param updatedAt the update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the last execution timestamp.
     * 
     * @return the last execution timestamp
     */
    public Instant getLastExecuted() {
        return lastExecuted;
    }

    /**
     * Sets the last execution timestamp.
     * 
     * @param lastExecuted the last execution timestamp to set
     */
    public void setLastExecuted(Instant lastExecuted) {
        this.lastExecuted = lastExecuted;
    }

    /**
     * Gets the version.
     * 
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the version.
     * 
     * @param version the version to set
     */
    public void setVersion(int version) {
        this.version = version;
    }
}
