package ai.nervemind.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.enums.CredentialType;
import jakarta.annotation.PostConstruct;

/**
 * Initializes credentials from .env file on application startup.
 * Automatically creates credentials for any API keys found in the .env file.
 */
@Component
public class CredentialInitializer {

    private static final Logger log = LoggerFactory.getLogger(CredentialInitializer.class);

    private final CredentialService credentialService;

    /**
     * Creates a new CredentialInitializer.
     * 
     * @param credentialService the service used to manage credentials
     */
    public CredentialInitializer(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    /**
     * Initializes the credentials from the .env file.
     * This method is called automatically by Spring after dependency injection.
     */
    @PostConstruct
    public void init() {
        loadCredentialsFromEnvFile();
    }

    /**
     * Load credentials from .env file in the project root.
     */
    private void loadCredentialsFromEnvFile() {
        // Try multiple possible locations for the .env file
        Path[] possiblePaths = {
                Paths.get(".env"), // Current directory
                Paths.get("..", ".env"), // Parent directory
                Paths.get(System.getProperty("user.dir"), ".env"), // User working directory
                Paths.get(System.getProperty("user.home"), ".env") // User home (fallback)
        };

        Path envFile = null;
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                envFile = path;
                break;
            }
        }

        if (envFile == null) {
            log.info("🔍 No .env file found in any of the expected locations, skipping credential initialization");
            return;
        }

        log.info("🔐 Found .env file at: {}", envFile.toAbsolutePath());

        try {
            log.info("🔐 Loading credentials from .env file...");

            Map<String, String> envVars = parseEnvFile(envFile);
            int created = 0;

            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Process only non-empty values for credentials that don't exist yet
                if (value != null && !value.trim().isEmpty()) {
                    if (credentialService.findByName(key).isPresent()) {
                        log.debug("Credential '{}' already exists, skipping", key);
                    } else if (createCredential(key, value)) {
                        // Create new credential
                        created++;
                    }
                }
            }

            if (created > 0) {
                log.info("🎉 Successfully created {} credentials from .env file", created);
            } else {
                log.info("ℹ️  No new credentials created (all already exist or .env is empty)");
            }

        } catch (Exception e) {
            log.error("❌ Failed to load credentials from .env file: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a single credential from key-value pair.
     * 
     * @param key   the credential name
     * @param value the credential value
     * @return true if credential was created successfully, false otherwise
     */
    private boolean createCredential(String key, String value) {
        try {
            CredentialDTO credential = CredentialDTO.create(key, CredentialType.API_KEY);
            credentialService.create(credential, value);
            log.info("✅ Created credential: {}", key);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to create credential '{}': {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Parse .env file into key-value pairs.
     * Supports basic .env format: KEY=value
     * Ignores comments (#) and empty lines.
     */
    private Map<String, String> parseEnvFile(Path envFile) throws IOException {
        Map<String, String> envVars = new HashMap<>();

        try (var lines = Files.lines(envFile)) {
            lines.forEach(line -> {
                String trimmedLine = line.trim();

                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    return;
                }

                // Parse KEY=value format
                int equalsIndex = trimmedLine.indexOf('=');
                if (equalsIndex > 0) {
                    String key = trimmedLine.substring(0, equalsIndex).trim();
                    String value = trimmedLine.substring(equalsIndex + 1).trim();

                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    envVars.put(key, value);
                }
            });
        }

        return envVars;
    }
}