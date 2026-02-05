package ai.nervemind.app.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.common.domain.SampleWorkflow;
import ai.nervemind.common.domain.Workflow;
import ai.nervemind.common.service.SampleServiceInterface;
import jakarta.annotation.PostConstruct;

/**
 * Service for loading, searching, and managing sample workflows.
 * Samples are loaded from classpath resources and user's samples directory.
 */
@Service
public class SampleService implements SampleServiceInterface {

    private static final Logger LOG = LoggerFactory.getLogger(SampleService.class);

    private final ObjectMapper objectMapper;
    private final List<SampleWorkflow> samples = new CopyOnWriteArrayList<>();

    @Value("${nervemind.samples.directory:samples}")
    private String userSamplesDirectory;

    /**
     * Creates a new SampleService with the given ObjectMapper.
     *
     * @param objectMapper the ObjectMapper for JSON processing
     */
    public SampleService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads sample workflows from both user directory and classpath.
     * User samples take priority over bundled samples.
     */
    @PostConstruct
    public void loadSamples() {
        samples.clear();

        // Load user samples from external directory (High priority)
        loadUserSamples();

        // Load bundled samples from classpath (Low priority - defaults)
        loadClasspathSamples();

        LOG.info("Loaded {} sample workflows", samples.size());
    }

    /**
     * Reload all samples (e.g., after user adds new samples).
     */
    @Override
    public void reloadSamples() {
        loadSamples();
    }

    /**
     * Get all available samples.
     */
    @Override
    public List<SampleWorkflow> getAllSamples() {
        return List.copyOf(samples);
    }

    /**
     * Search samples with filters.
     *
     * @param query      Search query (matches name, description, tags)
     * @param category   Filter by category (null for all)
     * @param language   Filter by language (null for all)
     * @param difficulty Filter by difficulty (null for all)
     * @return Filtered list of samples
     */
    @Override
    public List<SampleWorkflow> searchSamples(
            String query,
            String category,
            String language,
            SampleWorkflow.Difficulty difficulty) {

        return samples.stream()
                .filter(s -> matchesQuery(s, query))
                .filter(s -> category == null || category.isEmpty() || s.category().equalsIgnoreCase(category))
                .filter(s -> language == null || language.isEmpty() || s.language().equalsIgnoreCase(language))
                .filter(s -> difficulty == null || s.difficulty() == difficulty)
                .sorted(Comparator.comparing(SampleWorkflow::name))
                .toList();
    }

    /**
     * Get unique categories from all samples.
     */
    @Override
    public List<String> getCategories() {
        return samples.stream()
                .map(SampleWorkflow::category)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Get unique languages from all samples.
     */
    @Override
    public List<String> getLanguages() {
        return samples.stream()
                .map(SampleWorkflow::language)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Find a sample by ID.
     */
    @Override
    public Optional<SampleWorkflow> findById(String sampleId) {
        return samples.stream()
                .filter(s -> s.id().equals(sampleId))
                .findFirst();
    }

    /**
     * Get the workflow from a sample (for importing).
     */
    @Override
    public Workflow getWorkflowFromSample(String sampleId) {
        return findById(sampleId)
                .map(SampleWorkflow::workflow)
                .orElseThrow(() -> new IllegalArgumentException("Sample not found: " + sampleId));
    }

    /**
     * Get samples by category.
     *
     * @param category the category to filter by
     * @return list of samples in the specified category, sorted by difficulty
     */
    public List<SampleWorkflow> getSamplesByCategory(String category) {
        return samples.stream()
                .filter(s -> s.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(s -> s.difficulty().ordinal()))
                .toList();
    }

    private boolean matchesQuery(SampleWorkflow sample, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();

        // Check name
        if (sample.name() != null && sample.name().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check description
        if (sample.description() != null && sample.description().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Check tags
        if (sample.tags() != null) {
            for (String tag : sample.tags()) {
                if (tag.toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        }

        // Check category
        return sample.category() != null && sample.category().toLowerCase().contains(lowerQuery);
    }

    private void loadClasspathSamples() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:samples/*.json");

            for (Resource resource : resources) {
                loadSampleFromResource(resource);
            }
        } catch (IOException e) {
            LOG.warn("Failed to scan classpath for samples", e);
        }
    }

    private void loadSampleFromResource(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> data = objectMapper.readValue(is, new TypeReference<>() {
            });
            String filename = resource.getFilename();
            SampleWorkflow sample = SampleWorkflow.fromMap(data, "classpath:samples/" + filename);

            // Only add if not already present (User samples take precedence)
            boolean exists = samples.stream().anyMatch(s -> s.id().equals(sample.id()));
            if (!exists) {
                samples.add(sample);
                LOG.debug("Loaded sample from classpath: {}", sample.name());
            }
        } catch (Exception e) {
            LOG.warn("Failed to load sample from classpath: {}", resource.getFilename(), e);
        }
    }

    private void loadUserSamples() {
        Path samplesDir = Path.of(userSamplesDirectory);
        if (!Files.exists(samplesDir)) {
            LOG.debug("User samples directory does not exist: {}", samplesDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(samplesDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::loadSampleFromPath);
        } catch (IOException e) {
            LOG.warn("Failed to scan user samples directory: {}", samplesDir, e);
        }
    }

    private void loadSampleFromPath(Path path) {
        try {
            Map<String, Object> data = objectMapper.readValue(path.toFile(), new TypeReference<>() {
            });
            SampleWorkflow sample = SampleWorkflow.fromMap(data, path.toString());

            // Check for duplicates by ID
            boolean exists = samples.stream().anyMatch(s -> s.id().equals(sample.id()));
            if (!exists) {
                samples.add(sample);
                LOG.debug("Loaded sample from file: {}", sample.name());
            }
        } catch (Exception e) {
            LOG.warn("Failed to load sample from file: {}", path, e);
        }
    }

    /**
     * Get sample count by difficulty level.
     *
     * @return map of difficulty levels to sample counts
     */
    public Map<SampleWorkflow.Difficulty, Long> getSampleCountByDifficulty() {
        return samples.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SampleWorkflow::difficulty,
                        java.util.stream.Collectors.counting()));
    }

    /**
     * Get featured/recommended samples (e.g., for home screen).
     */
    @Override
    public List<SampleWorkflow> getFeaturedSamples(int limit) {
        // Return beginner samples first, then by name
        return samples.stream()
                .sorted(Comparator
                        .comparing((SampleWorkflow s) -> s.difficulty().ordinal())
                        .thenComparing(SampleWorkflow::name))
                .limit(limit)
                .toList();
    }
}
