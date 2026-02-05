package ai.nervemind.app.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import ai.nervemind.common.dto.VariableDTO.VariableScope;
import ai.nervemind.common.dto.VariableDTO.VariableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entity for storing workflow variables.
 */
@Entity
@Table(name = "variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "var_scope", "workflow_id" })
})
public class VariableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "var_value", columnDefinition = "CLOB")
    private String value;

    @Column(name = "encrypted_value", columnDefinition = "CLOB")
    private String encryptedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "var_type", nullable = false)
    private VariableType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "var_scope", nullable = false)
    private VariableScope scope;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA entity instantiation.
     * Required for JPA proxy creation and deserialization.
     */
    public VariableEntity() {
    }

    /**
     * Creates a new variable entity with the specified parameters.
     * Timestamps are automatically managed by Hibernate.
     *
     * @param name  the unique name of the variable
     * @param value the string value of the variable
     * @param type  the data type of the variable
     * @param scope the scope (global/workflow) of the variable
     */
    public VariableEntity(String name, String value, VariableType type, VariableScope scope) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.scope = scope;
    }

    // Getters and setters

    /**
     * Gets the unique identifier of this variable.
     *
     * @return the variable ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this variable.
     * This method is primarily used by JPA and should not be called directly.
     *
     * @param id the variable ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the unique name identifier for this variable.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name identifier for this variable.
     *
     * @param name the variable name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value of this variable as a string.
     * For SECRET type variables, this returns the plain text value.
     *
     * @return the variable value as a string
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this variable.
     * For SECRET type variables, the value should be plain text and will be
     * encrypted automatically.
     *
     * @param value the variable value to set as a string
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the encrypted value of this variable.
     * Only populated for SECRET type variables.
     *
     * @return the encrypted value, or null if not a secret variable
     */
    public String getEncryptedValue() {
        return encryptedValue;
    }

    /**
     * Sets the encrypted value of this variable.
     * This method is primarily used by the encryption service.
     *
     * @param encryptedValue the encrypted value to set
     */
    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    /**
     * Gets the data type of this variable.
     *
     * @return the variable type
     */
    public VariableType getType() {
        return type;
    }

    /**
     * Sets the data type of this variable.
     *
     * @param type the variable type to set
     */
    public void setType(VariableType type) {
        this.type = type;
    }

    /**
     * Gets the scope of this variable (global or workflow-specific).
     *
     * @return the variable scope
     */
    public VariableScope getScope() {
        return scope;
    }

    /**
     * Sets the scope of this variable.
     *
     * @param scope the variable scope to set
     */
    public void setScope(VariableScope scope) {
        this.scope = scope;
    }

    /**
     * Gets the workflow ID this variable belongs to.
     * Null for global variables.
     *
     * @return the workflow ID, or null for global variables
     */
    public Long getWorkflowId() {
        return workflowId;
    }

    /**
     * Sets the workflow ID this variable belongs to.
     * Set to null for global variables.
     *
     * @param workflowId the workflow ID to set, or null for global variables
     */
    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Gets the description of this variable.
     *
     * @return the variable description explaining its purpose
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this variable.
     *
     * @param description the variable description explaining its purpose
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the timestamp when this variable was first created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of this variable.
     * This method is primarily used by Hibernate and should not be called directly.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the timestamp when this variable was last updated.
     *
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp of this variable.
     * This method is primarily used by Hibernate and should not be called directly.
     *
     * @param updatedAt the last update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Check if this variable stores encrypted data.
     *
     * @return true if this is a SECRET type variable with encrypted data, false
     *         otherwise
     */
    public boolean isEncrypted() {
        return type == VariableType.SECRET && encryptedValue != null;
    }
}
