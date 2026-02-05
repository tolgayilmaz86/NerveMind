package ai.nervemind.app.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.nervemind.app.database.model.CredentialEntity;
import ai.nervemind.common.enums.CredentialType;

/**
 * Repository for Credential entities.
 */
@Repository
public interface CredentialRepository extends JpaRepository<CredentialEntity, Long> {

    /**
     * Find credential by name.
     *
     * @param name The unique name of the credential
     * @return an Optional containing the entity if found
     */
    Optional<CredentialEntity> findByName(String name);

    /**
     * Find credentials by type.
     *
     * @param type The type of credentials to search for
     * @return a list of matching credentials
     */
    List<CredentialEntity> findByType(CredentialType type);

    /**
     * Find credentials by name containing (search).
     *
     * @param name The name fragment to search for
     * @return a list of matching credentials
     */
    List<CredentialEntity> findByNameContainingIgnoreCase(String name);

    /**
     * Check if credential with name exists.
     *
     * @param name The name to check
     * @return true if a credential with this name exists
     */
    boolean existsByName(String name);
}
