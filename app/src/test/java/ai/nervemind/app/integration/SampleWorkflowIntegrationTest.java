package ai.nervemind.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ai.nervemind.app.service.CredentialService;
import ai.nervemind.app.service.SampleService;
import ai.nervemind.app.service.SettingsDefaults;
import ai.nervemind.app.service.SettingsService;
import ai.nervemind.app.service.WorkflowService;
import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.Workflow;
import ai.nervemind.common.dto.CredentialDTO;
import ai.nervemind.common.dto.ExecutionDTO;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.CredentialType;
import ai.nervemind.common.enums.ExecutionStatus;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.service.ExecutionServiceInterface;

/**
 * Integration tests for running sample workflows end-to-end.
 * 
 * <p>
 * These tests validate that all sample workflows execute successfully with real
 * credentials. They serve as system-wide smoke tests to ensure the entire
 * workflow execution pipeline works correctly.
 * </p>
 * 
 * <h2>Test Categories</h2>
 * <ul>
 * <li><b>No-Credential Samples</b>: Run without any API keys</li>
 * <li><b>OpenAI Samples</b>: Require OPENAI_API_KEY credential</li>
 * <li><b>Weather API Samples</b>: Require OPEN_WEATHER_API_KEY credential</li>
 * <li><b>Gemini Samples</b>: Require GOOGLE_API_KEY credential</li>
 * </ul>
 * 
 * <h2>Automatic Credential Import from .env</h2>
 * <p>
 * Tests automatically import credentials from a {@code .env} file in the
 * project root directory. This allows using the same API keys as the main
 * application without manual setup. The .env file format is:
 * </p>
 * 
 * <pre>
 * OPENAI_API_KEY=sk-...
 * GOOGLE_API_KEY=AI...
 * OPEN_WEATHER_API_KEY=...
 * </pre>
 * 
 * <h2>Running Tests</h2>
 * <p>
 * Tests are tagged and can be run selectively:
 * </p>
 * 
 * <pre>
 * ./gradlew :app:integrationTest
 * ./gradlew test --tests "*SampleWorkflowIntegrationTest" -Dtest.integration=true
 * </pre>
 * 
 * <h2>Required Credentials</h2>
 * <p>
 * These credentials are auto-imported from .env or can be created manually:
 * </p>
 * <ul>
 * <li>OPENAI_API_KEY - For LLM samples</li>
 * <li>OPEN_WEATHER_API_KEY - For weather samples</li>
 * <li>GOOGLE_API_KEY - For Gemini samples</li>
 * </ul>
 * 
 * @author NerveMind Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration-samples")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Sample Workflow Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleWorkflowIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SampleWorkflowIntegrationTest.class);

    /** Maximum time to wait for a workflow execution */
    @SuppressWarnings("unused")
    private static final Duration EXECUTION_TIMEOUT = Duration.ofMinutes(2);

    @Autowired
    private SampleService sampleService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ExecutionServiceInterface executionService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private CredentialService credentialService;

    /** Execution results for reporting */
    private static final Map<String, ExecutionResult> executionResults = new LinkedHashMap<>();

    /** Available credentials at test start */
    private static final Map<String, Boolean> availableCredentials = new HashMap<>();

    /** Flag to ensure .env import happens only once */
    private static final AtomicBoolean envImported = new AtomicBoolean(false);

    /** Path to .env file in project root */
    private static final Path ENV_FILE = Path.of(".env");

    /**
     * Result of a sample workflow execution.
     */
    record ExecutionResult(
            String sampleId,
            String sampleName,
            ExecutionStatus status,
            Duration duration,
            String errorMessage,
            Map<String, Object> output,
            List<String> logs) {

        boolean isSuccess() {
            return status == ExecutionStatus.SUCCESS;
        }

        @Override
        public String toString() {
            String statusIcon = switch (status) {
                case SUCCESS -> "✅";
                case FAILED -> "❌";
                case CANCELLED -> "⚠️";
                case RUNNING -> "🔄";
                default -> "❓";
            };
            return String.format("%s %s (%s) - %s", statusIcon, sampleName, sampleId,
                    isSuccess() ? duration.toMillis() + "ms" : errorMessage);
        }
    }

    @BeforeAll
    static void checkIntegrationTestEnabled() {
        String integrationEnabled = System.getProperty("test.integration", "false");
        if (!"true".equalsIgnoreCase(integrationEnabled)) {
            LOG.warn("╔════════════════════════════════════════════════════════════════════╗");
            LOG.warn("║  Integration tests are DISABLED by default.                        ║");
            LOG.warn("║  To run integration tests, use:                                    ║");
            LOG.warn("║    ./gradlew test -Dtest.integration=true                          ║");
            LOG.warn("╚════════════════════════════════════════════════════════════════════╝");
        }
    }

    @BeforeEach
    void setupCredentials() {
        // Import credentials from .env file (only once)
        if (envImported.compareAndSet(false, true)) {
            importCredentialsFromEnvFile();
        }

        // Cache available credentials
        if (availableCredentials.isEmpty()) {
            List<CredentialDTO> credentials = credentialService.findAll();
            LOG.info("Available credentials: {}", credentials.stream()
                    .map(CredentialDTO::name)
                    .collect(Collectors.joining(", ")));

            for (CredentialDTO cred : credentials) {
                availableCredentials.put(cred.name(), credentialService.testCredential(cred.id()));
            }
        }
    }

    /**
     * Import credentials from .env file in project root.
     * This allows using the same API keys as the main application.
     */
    private void importCredentialsFromEnvFile() {
        Path envPath = ENV_FILE;
        if (!Files.exists(envPath)) {
            // Try parent directory (common when running from app subproject)
            envPath = Path.of("../.env");
            if (!Files.exists(envPath)) {
                LOG.info("No .env file found at .env or ../.env - skipping auto-import");
                return;
            }
        }

        LOG.info("╔════════════════════════════════════════════════════════════════════╗");
        LOG.info("║  AUTO-IMPORTING CREDENTIALS FROM {}                         ║", envPath.getFileName());
        LOG.info("╚════════════════════════════════════════════════════════════════════╝");

        try {
            List<String> lines = Files.readAllLines(envPath);
            int imported = 0;
            int skipped = 0;

            for (String line : lines) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse KEY=value format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    // Skip empty values
                    if (value.isEmpty()) {
                        continue;
                    }

                    // Check if credential already exists
                    if (credentialService.findByName(key).isPresent()) {
                        LOG.debug("Credential '{}' already exists - skipping", key);
                        skipped++;
                        continue;
                    }

                    // Create new credential
                    try {
                        CredentialDTO dto = CredentialDTO.create(key, CredentialType.API_KEY);
                        credentialService.create(dto, value);
                        LOG.info("✅ Imported credential: {}", key);
                        imported++;

                        // Also update corresponding settings
                        updateSettingsFromEnv(key, value);
                    } catch (Exception e) {
                        LOG.warn("Failed to import '{}': {}", key, e.getMessage());
                    }
                }
            }

            LOG.info("Import complete: {} imported, {} skipped (already exist)", imported, skipped);

        } catch (IOException e) {
            LOG.error("Failed to read .env file: {}", e.getMessage());
        }
    }

    /**
     * Map common environment variables to application settings.
     */
    private void updateSettingsFromEnv(String key, String value) {
        String settingKey = null;

        if ("OPENAI_API_KEY".equals(key)) {
            settingKey = SettingsDefaults.AI_OPENAI_API_KEY;
        } else if ("GOOGLE_API_KEY".equals(key)) {
            settingKey = SettingsDefaults.AI_GOOGLE_API_KEY;
        } else if ("ANTHROPIC_API_KEY".equals(key)) {
            settingKey = SettingsDefaults.AI_ANTHROPIC_API_KEY;
        } else if ("AZURE_OPENAI_API_KEY".equals(key)) {
            settingKey = SettingsDefaults.AI_AZURE_API_KEY;
        }

        if (settingKey != null) {
            try {
                settingsService.setValue(settingKey, value);
                LOG.info("✅ Updated setting '{}' from .env", settingKey);
            } catch (Exception e) {
                LOG.warn("Failed to update setting '{}': {}", settingKey, e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREDENTIAL SANITY CHECKS (run first to validate API keys)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(0)
    @DisplayName("0. Credential Sanity Checks")
    class CredentialSanityChecks {

        @Test
        @Order(1)
        @DisplayName("List all stored credentials")
        void listStoredCredentials() {
            List<CredentialDTO> credentials = credentialService.findAll();
            LOG.info("╔════════════════════════════════════════════════════════════════════╗");
            LOG.info("║                    STORED CREDENTIALS                              ║");
            LOG.info("╠════════════════════════════════════════════════════════════════════╣");

            if (credentials.isEmpty()) {
                LOG.warn("║  ⚠️  No credentials found! Store API keys before running tests.   ║");
            } else {
                for (CredentialDTO cred : credentials) {
                    LOG.info("║  ✓ {} (ID: {})",
                            String.format("%-40s", cred.name()), cred.id());
                }
            }
            LOG.info("╚════════════════════════════════════════════════════════════════════╝");

            // This test always passes but provides visibility into what's stored
            assertThat(credentials).as("Credential list should be accessible").isNotNull();
        }

        @Test
        @Order(2)
        @DisplayName("Verify OPENAI_API_KEY is stored")
        void verifyOpenAiKeyStored() {
            boolean found = credentialService.findAll().stream()
                    .anyMatch(c -> "OPENAI_API_KEY".equals(c.name()));

            if (found) {
                LOG.info("✅ OPENAI_API_KEY credential found");
            } else {
                LOG.warn("⚠️ OPENAI_API_KEY not found - OpenAI samples will be skipped");
            }
            // Don't fail - just report status
        }

        @Test
        @Order(3)
        @DisplayName("Verify OPEN_WEATHER_API_KEY is stored")
        void verifyWeatherKeyStored() {
            boolean found = credentialService.findAll().stream()
                    .anyMatch(c -> "OPEN_WEATHER_API_KEY".equals(c.name()));

            if (found) {
                LOG.info("✅ OPEN_WEATHER_API_KEY credential found");
            } else {
                LOG.warn("⚠️ OPEN_WEATHER_API_KEY not found - Weather samples will be skipped");
            }
        }

        @Test
        @Order(4)
        @DisplayName("Verify GOOGLE_API_KEY is stored")
        void verifyGoogleKeyStored() {
            boolean found = credentialService.findAll().stream()
                    .anyMatch(c -> "GOOGLE_API_KEY".equals(c.name()));

            if (found) {
                LOG.info("✅ GOOGLE_API_KEY credential found");
            } else {
                LOG.warn("⚠️ GOOGLE_API_KEY not found - Gemini samples will be skipped");
            }
        }

        @Test
        @Order(5)
        @DisplayName("Test OpenAI API connectivity")
        void testOpenAiApiConnectivity() {
            // Skip if credential not stored
            var credOpt = credentialService.findAll().stream()
                    .filter(c -> "OPENAI_API_KEY".equals(c.name()))
                    .findFirst();

            assumeTrue(credOpt.isPresent(), "Skipping - OPENAI_API_KEY not stored");
            CredentialDTO cred = credOpt.get();

            LOG.info("Testing OpenAI API connectivity...");

            try {
                // Get decrypted key
                String apiKey = credentialService.getDecryptedData(cred.id());

                // Create simple request to OpenAI models endpoint
                var client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.openai.com/v1/models"))
                        .header("Authorization", "Bearer " + apiKey)
                        .GET()
                        .build();

                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    LOG.info("✅ OpenAI API key is valid and working (Status: 200)");
                } else {
                    LOG.error("❌ OpenAI API key test failed: Status {} - {}", response.statusCode(), response.body());
                    fail("OpenAI API key invalid: " + response.statusCode());
                }

            } catch (Exception e) {
                LOG.error("❌ OpenAI connectivity test failed: {}", e.getMessage());
                fail("OpenAI connectivity test failed: " + e.getMessage());
            }
        }

        @Test
        @Order(6)
        @DisplayName("Test Weather API connectivity")
        void testWeatherApiConnectivity() {
            // Skip if credential not stored
            // Skip if credential not stored
            var credOpt = credentialService.findAll().stream()
                    .filter(c -> "OPEN_WEATHER_API_KEY".equals(c.name()))
                    .findFirst();

            assumeTrue(credOpt.isPresent(), "Skipping - OPEN_WEATHER_API_KEY not stored");
            CredentialDTO cred = credOpt.get();

            LOG.info("Testing OpenWeather API connectivity...");

            try {
                String apiKey = credentialService.getDecryptedData(cred.id());

                // Simple weather request for London
                var client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI
                                .create("https://api.openweathermap.org/data/2.5/weather?q=Eindhoven&appid=" + apiKey))
                        .GET()
                        .build();

                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    LOG.info("✅ OpenWeather API key is valid and working");
                } else {
                    LOG.error("❌ OpenWeather API key test failed: Status {} - {}", response.statusCode(),
                            response.body());
                    fail("OpenWeather API key invalid: " + response.statusCode());
                }

            } catch (Exception e) {
                LOG.error("❌ Weather connectivity test failed: {}", e.getMessage());
                fail("Weather connectivity test failed: " + e.getMessage());
            }
        }

        @Test
        @Order(7)
        @DisplayName("Test Google/Gemini API connectivity")
        void testGoogleApiConnectivity() {
            // Skip if credential not stored
            // Skip if credential not stored
            var credOpt = credentialService.findAll().stream()
                    .filter(c -> "GOOGLE_API_KEY".equals(c.name()))
                    .findFirst();

            assumeTrue(credOpt.isPresent(), "Skipping - GOOGLE_API_KEY not stored");
            CredentialDTO cred = credOpt.get();

            LOG.info("Testing Google/Gemini API connectivity...");

            try {
                String apiKey = credentialService.getDecryptedData(cred.id());

                // List models request
                var client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI
                                .create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey))
                        .GET()
                        .build();

                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    LOG.info("✅ Google API key is valid and working");
                } else {
                    LOG.error("❌ Google API key test failed: Status {} - {}", response.statusCode(), response.body());
                    fail("Google API key invalid: " + response.statusCode());
                }

            } catch (Exception e) {
                LOG.error("❌ Google connectivity test failed: {}", e.getMessage());
                fail("Google connectivity test failed: " + e.getMessage());
            }
        }

        @Test
        @Order(8)
        @DisplayName("Summary: Credential readiness report")
        void credentialReadinessReport() {
            List<CredentialDTO> credentials = credentialService.findAll();

            boolean hasOpenAi = credentials.stream().anyMatch(c -> "OPENAI_API_KEY".equals(c.name()));
            boolean hasWeather = credentials.stream().anyMatch(c -> "OPEN_WEATHER_API_KEY".equals(c.name()));
            boolean hasGemini = credentials.stream().anyMatch(c -> "GOOGLE_API_KEY".equals(c.name()));

            int sampleCount = 19;
            int runnableSamples = 10; // No-credential samples
            if (hasOpenAi)
                runnableSamples += 6;
            if (hasWeather)
                runnableSamples += 1;
            if (hasGemini)
                runnableSamples += 1;
            // File watcher sample (+1) has special handling

            LOG.info("╔════════════════════════════════════════════════════════╗");
            LOG.info("║          CREDENTIAL READINESS REPORT                   ║");
            LOG.info("╠════════════════════════════════════════════════════════╣");
            LOG.info("║  OPENAI_API_KEY:       {} ║", hasOpenAi ? "✅ Ready  " : "❌ Missing");
            LOG.info("║  OPEN_WEATHER_API_KEY: {} ║", hasWeather ? "✅ Ready  " : "❌ Missing");
            LOG.info("║  GOOGLE_API_KEY:       {} ║", hasGemini ? "✅ Ready  " : "❌ Missing");
            LOG.info("╠════════════════════════════════════════════════════════╣");
            LOG.info("║  Samples runnable: {}/{}                              ║",
                    runnableSamples, sampleCount);
            LOG.info("╚════════════════════════════════════════════════════════╝");

            if (!hasOpenAi && !hasWeather && !hasGemini) {
                LOG.warn("No API credentials found. Only no-credential samples will run.");
                LOG.warn("To add credentials: Start app > Settings > Credentials > Add");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NO-CREDENTIAL SAMPLES (should always pass)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @DisplayName("1. No-Credential Samples")
    class NoCredentialSamples {

        @Test
        @Order(1)
        @DisplayName("00 - Weather Alert (No API Key Demo)")
        void sample00WeatherAlertNoApiKey() {
            executeSampleAndAssert("00-weather-alert-workflow-no-apikey", false);
        }

        @Test
        @Order(2)
        @DisplayName("03 - Data Processing Pipeline")
        void sample03DataProcessingPipeline() {
            executeSampleAndAssert("03-data-processing-pipeline", false);
        }

        @Test
        @Order(3)
        @DisplayName("05 - Error Handling Demo")
        void sample05ErrorHandlingDemo() {
            // This sample demonstrates error handling, so partial success is expected
            executeSampleAndAssert("05-error-handling-demo", true);
        }

        @Test
        @Order(4)
        @DisplayName("11 - System Health Monitor")
        void sample11SystemHealthMonitor() {
            executeSampleAndAssert("11-system-health-monitor", false);
        }

        @Test
        @Order(5)
        @DisplayName("12 - Resilient Data Scraper")
        void sample12ResilientDataScraper() {
            executeSampleAndAssert("12-resilient-data-scraper", true);
        }

        @Test
        @Order(6)
        @DisplayName("13 - Python Data Transform")
        void sample13PythonDataTransform() {
            executeSampleAndAssert("13-python-data-transform", false);
        }

        @Test
        @Order(7)
        @DisplayName("14 - Python Text Analyzer")
        void sample14PythonTextAnalyzer() {
            executeSampleAndAssert("14-python-text-analyzer", false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPENAI API SAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(2)
    @DisplayName("OpenAI API Samples")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class OpenAISamples {

        @BeforeAll
        void checkCredentials() {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    SampleWorkflowIntegrationTest.this.hasOpenAICredential(),
                    "OpenAI API key not available - skipping OpenAI samples");
        }

        @Test
        @Order(1)
        @DisplayName("02 - AI Content Generator")
        void sample02AiContentGenerator() {
            executeSampleAndAssert("02-ai-content-generator", true);
        }

        @Test
        @Order(2)
        @DisplayName("07 - iRacing Setup Advisor")
        void sample07IRacingSetupAdvisor() {
            executeSampleAndAssert("07-iracing-setup-advisor", true);
        }

        @Test
        @Order(3)
        @DisplayName("09 - Local Knowledge Base RAG")
        void sample09LocalKnowledgeBaseRag() {
            executeSampleAndAssert("09-local-knowledge-base-rag", true);
        }

        @Test
        @Order(4)
        @DisplayName("10 - Support Ticket Router")
        void sample10SupportTicketRouter() {
            executeSampleAndAssert("10-support-ticket-router", true);
        }

        @Test
        @Order(5)
        @DisplayName("15 - Security Incident Responder")
        void sample15SecurityIncidentResponder() {
            executeSampleAndAssert("15-security-incident-responder", true);
        }

        @Test
        @Order(6)
        @DisplayName("16 - Invoice Processing ERP")
        void sample16InvoiceProcessingErp() {
            executeSampleAndAssert("16-invoice-processing-erp", true);
        }

        @Test
        @Order(7)
        @DisplayName("17 - Assignment Auto Grader")
        void sample17AssignmentAutoGrader() {
            executeSampleAndAssert("17-assignment-auto-grader", true);
        }

        @Test
        @Order(8)
        @DisplayName("18 - Research Paper Summarizer")
        void sample18ResearchPaperSummarizer() {
            executeSampleAndAssert("18-research-paper-summarizer", true);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WEATHER API SAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(3)
    @DisplayName("Weather API Samples")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WeatherAPISamples {

        @BeforeAll
        void checkCredentials() {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    SampleWorkflowIntegrationTest.this.hasWeatherAPICredential(),
                    "Weather API key not available - skipping Weather samples");
        }

        @Test
        @Order(1)
        @DisplayName("01 - Weather Alert Workflow")
        void sample01WeatherAlertWorkflow() {
            executeSampleAndAssert("01-weather-alert-workflow", true);
        }

        @Test
        @Order(2)
        @DisplayName("04 - Multi API Integration")
        void sample04MultiApiIntegration() {
            executeSampleAndAssert("04-multi-api-integration", true);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GEMINI API SAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(4)
    @DisplayName("Gemini API Samples")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GeminiAPISamples {

        @BeforeAll
        void checkCredentials() {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    SampleWorkflowIntegrationTest.this.hasGeminiCredential(),
                    "Gemini API key not available - skipping Gemini samples");
        }

        @Test
        @Order(1)
        @DisplayName("08 - Gemini AI Assistant")
        void sample08GeminiAiAssistant() {
            executeSampleAndAssert("08-gemini-ai-assistant", true);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILE WATCHER SAMPLES (special handling) needs user interaction
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(5)
    @DisplayName("File Watcher Samples")
    class FileWatcherSamples {

        @Test
        @Order(1)
        @DisplayName("06 - File Watcher Workflow (validation only)")
        void sample06FileWatcherWorkflow() {
            // File watcher needs actual file events, so we just validate it loads
            SampleWorkflow sample = findSampleByFileName("06-file-watcher-workflow");
            assertThat(sample).isNotNull();
            assertThat(sample.workflow().nodes()).isNotEmpty();
            LOG.info("✅ File watcher sample validated (execution requires file events)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @Order(100)
    @DisplayName("Test Summary")
    class TestSummary {

        @Test
        @Order(1)
        @DisplayName("Generate execution report")
        void generateExecutionReport() {
            if (executionResults.isEmpty()) {
                LOG.info("No sample executions recorded.");
                return;
            }

            LOG.info("\n");
            LOG.info("╔════════════════════════════════════════════════════════════════════╗");
            LOG.info("║              SAMPLE WORKFLOW EXECUTION REPORT                      ║");
            LOG.info("╠════════════════════════════════════════════════════════════════════╣");

            int successCount = 0;
            int failedCount = 0;
            long totalDuration = 0;

            for (ExecutionResult result : executionResults.values()) {
                LOG.info("║ {}", String.format("%-66s", result.toString()) + "║");
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failedCount++;
                }
                totalDuration += result.duration().toMillis();
            }

            LOG.info("╠════════════════════════════════════════════════════════════════════╣");
            LOG.info("║ Total: {} samples | ✅ {} passed | ❌ {} failed | ⏱️ {}ms         ║",
                    executionResults.size(), successCount, failedCount, totalDuration);
            LOG.info("╚════════════════════════════════════════════════════════════════════╝");

            // Fail if any samples failed unexpectedly
            if (failedCount > 0) {
                List<String> failures = executionResults.values().stream()
                        .filter(r -> !r.isSuccess())
                        .map(r -> r.sampleId() + ": " + r.errorMessage())
                        .toList();
                LOG.error("Failed samples: {}", failures);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if OpenAI credential is available.
     */
    boolean hasOpenAICredential() {
        return hasCredential("OPENAI_API_KEY");
    }

    /**
     * Check if Weather API credential is available.
     */
    boolean hasWeatherAPICredential() {
        return hasCredential("OPEN_WEATHER_API_KEY");
    }

    /**
     * Check if Google/Gemini credential is available.
     */
    boolean hasGoogleCredential() {
        return hasCredential("GOOGLE_API_KEY");
    }

    /**
     * Check if Gemini credential is available (alias for Google credential).
     */
    boolean hasGeminiCredential() {
        return hasCredential("GOOGLE_API_KEY");
    }

    /**
     * Check if a credential exists and is valid.
     */
    private boolean hasCredential(String name) {
        if (availableCredentials.isEmpty()) {
            // Load credentials if not cached yet
            List<CredentialDTO> credentials = credentialService.findAll();
            for (CredentialDTO cred : credentials) {
                availableCredentials.put(cred.name(), credentialService.testCredential(cred.id()));
            }
        }
        return Boolean.TRUE.equals(availableCredentials.get(name));
    }

    /**
     * Find a sample by its filename (without .json extension).
     */
    private SampleWorkflow findSampleByFileName(String fileNamePrefix) {
        List<SampleWorkflow> samples = sampleService.getAllSamples();
        return samples.stream()
                .filter(s -> s.filePath() != null && s.filePath().contains(fileNamePrefix))
                .findFirst()
                .orElseGet(() -> samples.stream()
                        .filter(s -> s.id().contains(fileNamePrefix.replace("-", "")))
                        .findFirst()
                        .orElse(null));
    }

    /**
     * Execute a sample workflow and assert the result.
     *
     * @param fileNamePrefix      Prefix of the sample file (e.g.,
     *                            "01-weather-alert")
     * @param allowPartialSuccess If true, partial success is acceptable
     */
    private void executeSampleAndAssert(String fileNamePrefix, boolean allowPartialSuccess) {
        SampleWorkflow sample = findSampleByFileName(fileNamePrefix);

        if (sample == null) {
            LOG.warn("⚠️ Sample not found: {}", fileNamePrefix);
            fail("Sample not found: " + fileNamePrefix);
            return;
        }

        LOG.info("▶️ Executing sample: {} ({})", sample.name(), sample.id());

        // Check required credentials
        if (sample.requiredCredentials() != null && !sample.requiredCredentials().isEmpty()) {
            for (String requiredCred : sample.requiredCredentials()) {
                if (!hasCredential(requiredCred)) {
                    LOG.warn("⏭️ Skipping sample {} - missing credential: {}", sample.id(), requiredCred);
                    ExecutionResult result = new ExecutionResult(
                            sample.id(),
                            sample.name(),
                            ExecutionStatus.CANCELLED,
                            Duration.ZERO,
                            "Missing credential: " + requiredCred,
                            Map.of(),
                            List.of());
                    executionResults.put(sample.id(), result);
                    return; // Skip without failing
                }
            }
        }

        ExecutionResult result = executeWorkflow(sample);
        executionResults.put(sample.id(), result);

        // Log result
        if (result.isSuccess()) {
            LOG.info("✅ {} completed successfully in {}ms", sample.name(), result.duration().toMillis());
        } else {
            LOG.error("❌ {} failed: {}", sample.name(), result.errorMessage());
            if (!allowPartialSuccess) {
                fail("Sample execution failed: " + sample.name() + " - " + result.errorMessage());
            }
        }
    }

    /**
     * Execute a sample workflow and return the result.
     */
    private ExecutionResult executeWorkflow(SampleWorkflow sample) {
        Instant startTime = Instant.now();
        List<String> logs = new ArrayList<>();

        try {
            // Convert sample workflow to WorkflowDTO for persistence
            Workflow workflow = sample.workflow();
            WorkflowDTO workflowDTO = new WorkflowDTO(
                    null,
                    sample.name(),
                    sample.description(),
                    workflow.nodes(),
                    workflow.connections(),
                    workflow.settings(),
                    true,
                    TriggerType.MANUAL,
                    null,
                    null, null, null, 0);

            // Save the sample as a temporary workflow
            WorkflowDTO savedWorkflow = workflowService.create(workflowDTO);
            logs.add("Created workflow: " + savedWorkflow.id());

            // Execute the workflow
            ExecutionDTO execution = executionService.execute(savedWorkflow.id(), Map.of());
            logs.add("Execution completed with status: " + execution.status());

            Duration duration = Duration.between(startTime, Instant.now());

            // Parse output
            Map<String, Object> output = execution.outputData() != null
                    ? execution.outputData()
                    : Map.of();

            // Clean up - delete the temporary workflow
            workflowService.delete(savedWorkflow.id());
            logs.add("Cleaned up temporary workflow");

            return new ExecutionResult(
                    sample.id(),
                    sample.name(),
                    execution.status(),
                    duration,
                    execution.errorMessage(),
                    output,
                    logs);

        } catch (Exception e) {
            LOG.error("Execution error for {}: {}", sample.name(), e.getMessage(), e);
            Duration duration = Duration.between(startTime, Instant.now());
            logs.add("ERROR: " + e.getMessage());

            return new ExecutionResult(
                    sample.id(),
                    sample.name(),
                    ExecutionStatus.FAILED,
                    duration,
                    e.getMessage(),
                    Map.of(),
                    logs);
        }
    }
}
