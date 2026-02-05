package ai.nervemind.app.database.model;

import java.time.Instant;

import ai.nervemind.common.enums.CredentialType;
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

/**
 * JPA Entity for Credentials.
 * Credential data is stored encrypted.
 */
@Entity
@Table(name = "credentials")
public class CredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialType type;

    @Column(name = "data_encrypted", columnDefinition = "CLOB", nullable = false)
    private String dataEncrypted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor for JPA.
     */
    protected CredentialEntity() {
    }

    /**
     * Creates a new CredentialEntity.
     * 
     * @param name          the display name of the credential
     * @param type          the type of credential
     * @param dataEncrypted the encrypted credential data
     */
    public CredentialEntity(String name, CredentialType type, String dataEncrypted) {
        this.name = name;
        this.type = type;
        this.dataEncrypted = dataEncrypted;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Pre-persist callback to set timestamps.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    /**
     * Pre-update callback to update timestamps.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    /**
     * Gets the credential ID.
     * 
     * @return the credential ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the credential ID.
     * 
     * @param id the credential ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the credential name.
     * 
     * @return the credential name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the credential name.
     * 
     * @param name the credential name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the credential type.
     * 
     * @return the credential type
     */
    public CredentialType getType() {
        return type;
    }

    /**
     * Sets the credential type.
     * 
     * @param type the credential type to set
     */
    public void setType(CredentialType type) {
        this.type = type;
    }

    /**
     * Gets the encrypted credential data.
     * 
     * @return the encrypted credential data
     */
    public String getDataEncrypted() {
        return dataEncrypted;
    }

    /**
     * Sets the encrypted credential data.
     * 
     * @param dataEncrypted the encrypted credential data to set
     */
    public void setDataEncrypted(String dataEncrypted) {
        this.dataEncrypted = dataEncrypted;
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
     * Gets the last update timestamp.
     * 
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     * 
     * @param updatedAt the last update timestamp to set
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
