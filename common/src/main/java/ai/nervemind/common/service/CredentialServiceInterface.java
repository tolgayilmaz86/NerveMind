package ai.nervemind.common.service;

import java.util.List;
import java.util.Optional;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;

/**
 * Service interface for secure credential management.
 * 
 * <p>
 * This interface provides operations for storing, retrieving, and managing
 * sensitive credentials like API keys, tokens, and passwords. All sensitive
 * data is encrypted at rest using AES-256 encryption.
 * </p>
 * 
 * <h2>Security Model</h2>
 * <ul>
 * <li><strong>Encryption</strong> - All credential data is encrypted before
 * storage</li>
 * <li><strong>DTO Redaction</strong> - DTOs never contain sensitive data (only
 * metadata)</li>
 * <li><strong>Decryption on Demand</strong> - Data is only decrypted during
 * workflow execution</li>
 * <li><strong>Audit Trail</strong> - All access to credentials is logged</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * // Store a new API key
 * CredentialDTO dto = CredentialDTO.create("OpenAI Key", CredentialType.API_KEY);
 * credentialService.create(dto, "sk-abc123...");
 * 
 * // List all credentials (no sensitive data exposed)
 * List&lt;CredentialDTO&gt; creds = credentialService.findAll();
 * 
 * // Get decrypted value during execution
 * String apiKey = credentialService.getDecryptedData(credentialId);
 * </pre>
 * 
 * <h2>Supported Credential Types</h2>
 * <ul>
 * <li>{@link CredentialType#API_KEY} - Simple API key authentication</li>
 * <li>{@link CredentialType#HTTP_BASIC} - Username and password</li>
 * <li>{@link CredentialType#HTTP_BEARER} - Bearer token</li>
 * <li>{@link CredentialType#OAUTH2} - OAuth 2.0 tokens</li>
 * <li>{@link CredentialType#CUSTOM_HEADER} - Custom header values</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 * @see CredentialDTO Data transfer object for credentials
 * @see CredentialType Supported credential types
 */
public interface CredentialServiceInterface {

    /**
     * Get all credentials (without sensitive data).
     * 
     * @return list of all credentials
     */
    List<CredentialDTO> findAll();

    /**
     * Find credential by ID.
     * 
     * @param id the credential ID
     * @return optional containing the credential if found
     */
    Optional<CredentialDTO> findById(Long id);

    /**
     * Find credential by name.
     * 
     * @param name the credential name
     * @return optional containing the credential if found
     */
    Optional<CredentialDTO> findByName(String name);

    /**
     * Find credentials by type.
     * 
     * @param type the credential type
     * @return list of credentials matching the type
     */
    List<CredentialDTO> findByType(CredentialType type);

    /**
     * Create a new credential.
     * 
     * @param dto  The credential metadata
     * @param data The sensitive credential data (will be encrypted)
     * @return The created credential (without sensitive data)
     */
    CredentialDTO create(CredentialDTO dto, String data);

    /**
     * Update an existing credential.
     * 
     * @param id   The credential ID
     * @param dto  The updated metadata
     * @param data The new sensitive data (null to keep existing)
     * @return The updated credential
     */
    CredentialDTO update(Long id, CredentialDTO dto, String data);

    /**
     * Delete a credential.
     * 
     * @param id the credential ID to delete
     */
    void delete(Long id);

    /**
     * Get decrypted credential data for use in node execution.
     * This method should only be called during workflow execution.
     * 
     * @param id The credential ID
     * @return The decrypted credential data
     */
    String getDecryptedData(Long id);

    /**
     * Test if a credential is valid (e.g., by making a test API call).
     * 
     * @param id The credential ID
     * @return true if the credential is valid
     */
    boolean testCredential(Long id);
}
