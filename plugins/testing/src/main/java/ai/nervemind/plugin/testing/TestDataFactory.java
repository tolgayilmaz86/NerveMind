package ai.nervemind.plugin.testing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory for creating test data for plugin testing.
 * 
 * <p>
 * Provides convenient methods for generating test inputs, configurations,
 * and expected outputs.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Create sample user data
 * Map<String, Object> user = TestDataFactory.createUser()
 *         .withName("John Doe")
 *         .withEmail("john@example.com")
 *         .build();
 * 
 * // Create random test data
 * Map<String, Object> config = TestDataFactory.randomConfig()
 *         .withString("apiKey")
 *         .withNumber("timeout", 1000)
 *         .build();
 * }</pre>
 */
public class TestDataFactory {

    private static final String[] FIRST_NAMES = {
            "John", "Jane", "Bob", "Alice", "Charlie", "Diana", "Eve", "Frank"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Doe", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller"
    };

    private static final String[] DOMAINS = {
            "example.com", "test.org", "sample.net", "demo.io", "localhost"
    };

    private static final String[] CITIES = {
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
            "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"
    };

    private static final String[] COUNTRIES = {
            "United States", "Canada", "United Kingdom", "Germany", "France",
            "Australia", "Japan", "India", "Brazil", "Mexico"
    };

    // ========== User Data Builder ==========

    /**
     * Create a user data builder.
     */
    public static UserBuilder createUser() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String firstName = randomElement(FIRST_NAMES);
        private String lastName = randomElement(LAST_NAMES);
        private String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + randomElement(DOMAINS);
        private int age = ThreadLocalRandom.current().nextInt(18, 80);
        private String city = randomElement(CITIES);
        private String country = randomElement(COUNTRIES);
        private boolean active = true;
        private Map<String, Object> metadata = new HashMap<>();

        public UserBuilder withFirstName(String name) {
            this.firstName = name;
            return this;
        }

        public UserBuilder withLastName(String name) {
            this.lastName = name;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withAge(int age) {
            this.age = age;
            return this;
        }

        public UserBuilder withCity(String city) {
            this.city = city;
            return this;
        }

        public UserBuilder withCountry(String country) {
            this.country = country;
            return this;
        }

        public UserBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public UserBuilder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("firstName", firstName);
            user.put("lastName", lastName);
            user.put("fullName", firstName + " " + lastName);
            user.put("email", email);
            user.put("age", age);
            user.put("city", city);
            user.put("country", country);
            user.put("active", active);
            user.put("metadata", metadata);
            return user;
        }
    }

    // ========== Random Config Builder ==========

    /**
     * Create a random configuration builder.
     */
    public static ConfigBuilder randomConfig() {
        return new ConfigBuilder();
    }

    public static class ConfigBuilder {
        private final Map<String, Object> config = new LinkedHashMap<>();

        public ConfigBuilder withString(String key) {
            config.put(key, UUID.randomUUID().toString().substring(0, 8));
            return this;
        }

        public ConfigBuilder withString(String key, String value) {
            config.put(key, value);
            return this;
        }

        public ConfigBuilder withNumber(String key) {
            config.put(key, ThreadLocalRandom.current().nextInt(1, 1000));
            return this;
        }

        public ConfigBuilder withNumber(String key, int value) {
            config.put(key, value);
            return this;
        }

        public ConfigBuilder withDouble(String key, double min, double max) {
            config.put(key, ThreadLocalRandom.current().nextDouble(min, max));
            return this;
        }

        public ConfigBuilder withBoolean(String key) {
            config.put(key, ThreadLocalRandom.current().nextBoolean());
            return this;
        }

        public ConfigBuilder withBoolean(String key, boolean value) {
            config.put(key, value);
            return this;
        }

        public ConfigBuilder withList(String key, int size) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(UUID.randomUUID().toString().substring(0, 4));
            }
            config.put(key, list);
            return this;
        }

        public ConfigBuilder withMap(String key) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", UUID.randomUUID().toString());
            map.put("value", ThreadLocalRandom.current().nextInt());
            config.put(key, map);
            return this;
        }

        public ConfigBuilder withNested(String... keys) {
            Map<String, Object> current = config;
            for (int i = 0; i < keys.length - 1; i++) {
                Map<String, Object> nested = new LinkedHashMap<>();
                current.put(keys[i], nested);
                current = nested;
            }
            current.put(keys[keys.length - 1], "value");
            return this;
        }

        public Map<String, Object> build() {
            return new LinkedHashMap<>(config);
        }
    }

    // ========== Workflow Test Data ==========

    /**
     * Create a test workflow configuration.
     */
    public static WorkflowBuilder createWorkflow() {
        return new WorkflowBuilder();
    }

    public static class WorkflowBuilder {
        private String workflowId = "test-workflow-" + UUID.randomUUID().toString().substring(0, 8);
        private String name = "Test Workflow";
        private boolean enabled = true;
        private Map<String, Object> trigger = new HashMap<>();
        private List<Map<String, Object>> nodes = new ArrayList<>();

        public WorkflowBuilder withId(String id) {
            this.workflowId = id;
            return this;
        }

        public WorkflowBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public WorkflowBuilder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public WorkflowBuilder withManualTrigger() {
            this.trigger = Map.of(
                    "type", "manual",
                    "description", "Manual trigger for testing");
            return this;
        }

        public WorkflowBuilder withScheduleTrigger(String cronExpression) {
            this.trigger = Map.of(
                    "type", "schedule",
                    "cronExpression", cronExpression,
                    "enabled", true);
            return this;
        }

        public WorkflowBuilder addNode(Map<String, Object> node) {
            this.nodes.add(node);
            return this;
        }

        public WorkflowBuilder addNode(String type, String name) {
            this.nodes.add(Map.of(
                    "type", type,
                    "name", name,
                    "id", "node-" + UUID.randomUUID().toString().substring(0, 8)));
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> workflow = new LinkedHashMap<>();
            workflow.put("id", workflowId);
            workflow.put("name", name);
            workflow.put("enabled", enabled);
            workflow.put("trigger", trigger);
            workflow.put("nodes", nodes);
            return workflow;
        }
    }

    // ========== API Response Data ==========

    /**
     * Create a mock API response.
     */
    public static ApiResponseBuilder apiResponse() {
        return new ApiResponseBuilder();
    }

    public static class ApiResponseBuilder {
        private int statusCode = 200;
        private String status = "OK";
        private Map<String, Object> body = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private long timestamp = System.currentTimeMillis();

        public ApiResponseBuilder withStatusCode(int code) {
            this.statusCode = code;
            this.status = code == 200 ? "OK"
                    : code == 201 ? "Created"
                            : code == 400 ? "Bad Request"
                                    : code == 401 ? "Unauthorized"
                                            : code == 403 ? "Forbidden"
                                                    : code == 404 ? "Not Found"
                                                            : code == 500 ? "Internal Server Error" : "Error";
            return this;
        }

        public ApiResponseBuilder withBody(Map<String, Object> body) {
            this.body = body;
            return this;
        }

        public ApiResponseBuilder withHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public ApiResponseBuilder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("statusCode", statusCode);
            response.put("status", status);
            response.put("body", body);
            response.put("headers", headers);
            response.put("timestamp", timestamp);
            return response;
        }
    }

    // ========== File Test Data ==========

    /**
     * Create file content for testing.
     */
    public static FileContentBuilder file(String filename) {
        return new FileContentBuilder(filename);
    }

    public static class FileContentBuilder {
        private final String filename;
        private String content = "";
        private String mimeType = "text/plain";
        private long size = 0;
        private Map<String, String> metadata = new HashMap<>();

        public FileContentBuilder(String filename) {
            this.filename = filename;
        }

        public FileContentBuilder withContent(String content) {
            this.content = content;
            this.size = content.length();
            return this;
        }

        public FileContentBuilder withJsonContent(Object data) {
            this.content = data.toString(); // Simplified
            this.size = content.length();
            this.mimeType = "application/json";
            return this;
        }

        public FileContentBuilder withCsvContent(String[][] rows) {
            StringBuilder sb = new StringBuilder();
            for (String[] row : rows) {
                sb.append(String.join(",", row)).append("\n");
            }
            this.content = sb.toString();
            this.size = content.length();
            this.mimeType = "text/csv";
            return this;
        }

        public FileContentBuilder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public FileContentBuilder withMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Map<String, Object> build() {
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("filename", filename);
            file.put("content", content);
            file.put("mimeType", mimeType);
            file.put("size", size);
            file.put("metadata", metadata);
            return file;
        }
    }

    // ========== Helper Methods ==========

    private static <T> T randomElement(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    /**
     * Generate a random email.
     */
    public static String randomEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@" + randomElement(DOMAINS);
    }

    /**
     * Generate a random UUID string.
     */
    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a random timestamp within a range.
     */
    public static long randomTimestamp(Duration minAge, Duration maxAge) {
        long minMillis = minAge.toMillis();
        long maxMillis = maxAge.toMillis();
        return System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(minMillis, maxMillis);
    }

    /**
     * Create a random boolean (weighted towards true).
     */
    public static boolean randomBoolean(double trueProbability) {
        return ThreadLocalRandom.current().nextDouble() < trueProbability;
    }
}
