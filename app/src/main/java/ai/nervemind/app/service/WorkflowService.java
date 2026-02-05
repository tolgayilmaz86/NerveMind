package ai.nervemind.app.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nervemind.app.database.model.WorkflowEntity;
import ai.nervemind.app.database.repository.WorkflowRepository;
import ai.nervemind.common.domain.Connection;
import ai.nervemind.common.domain.Node;
import ai.nervemind.common.dto.WorkflowDTO;
import ai.nervemind.common.enums.TriggerType;
import ai.nervemind.common.exception.DataParsingException;
import ai.nervemind.common.service.WorkflowServiceInterface;

/**
 * Service for managing workflows.
 */
@Service
@Transactional
public class WorkflowService implements WorkflowServiceInterface {

    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<FileWatcherService> fileWatcherServiceProvider;

    /**
     * Creates a new WorkflowService with the required dependencies.
     *
     * @param workflowRepository         the repository for workflow data access
     * @param objectMapper               the object mapper for JSON serialization
     * @param fileWatcherServiceProvider provider for file watcher service
     */
    public WorkflowService(WorkflowRepository workflowRepository, ObjectMapper objectMapper,
            ObjectProvider<FileWatcherService> fileWatcherServiceProvider) {
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
        this.fileWatcherServiceProvider = fileWatcherServiceProvider;
    }

    @Override
    public List<WorkflowDTO> findAll() {
        return workflowRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Optional<WorkflowDTO> findById(Long id) {
        return workflowRepository.findById(id)
                .map(this::toDTO);
    }

    /**
     * Find a workflow by its exact name.
     *
     * @param name the exact name to search for
     * @return an Optional containing the workflow DTO if found
     */
    public Optional<WorkflowDTO> findByName(String name) {
        return workflowRepository.findByName(name)
                .map(this::toDTO);
    }

    @Override
    public List<WorkflowDTO> findByTriggerType(TriggerType triggerType) {
        return workflowRepository.findByTriggerType(triggerType).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Find all active scheduled workflows.
     *
     * @return a list of active scheduled workflow DTOs
     */
    public List<WorkflowDTO> findActiveScheduledWorkflows() {
        return workflowRepository.findActiveScheduledWorkflows().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public WorkflowDTO create(WorkflowDTO dto) {
        WorkflowEntity entity = toEntity(dto);
        // ID is auto-generated, createdAt/updatedAt handled by @PrePersist
        WorkflowEntity saved = workflowRepository.save(entity);
        WorkflowDTO created = toDTO(saved);
        notifyWatcher(created);
        return created;
    }

    @Override
    public WorkflowDTO update(WorkflowDTO dto) {
        if (dto.id() == null) {
            throw new IllegalArgumentException("Workflow ID cannot be null for update");
        }
        WorkflowEntity existing = workflowRepository.findById(dto.id())
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + dto.id()));

        existing.setName(dto.name());
        existing.setDescription(dto.description());
        existing.setNodesJson(serializeNodes(dto.nodes()));
        existing.setConnectionsJson(serializeConnections(dto.connections()));
        existing.setSettingsJson(serializeSettings(dto.settings()));
        existing.setActive(dto.isActive());
        existing.setTriggerType(dto.triggerType());
        existing.setCronExpression(dto.cronExpression());
        // updatedAt handled by @PreUpdate

        WorkflowEntity saved = workflowRepository.save(existing);
        WorkflowDTO updated = toDTO(saved);
        notifyWatcher(updated);
        return updated;
    }

    @Override
    public void delete(Long id) {
        workflowRepository.deleteById(id);
        fileWatcherServiceProvider.ifAvailable(watcher -> watcher.unregisterWorkflow(id));
    }

    @Override
    public WorkflowDTO duplicate(Long id) {
        WorkflowDTO original = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));

        WorkflowDTO copy = new WorkflowDTO(
                null,
                original.name() + " (Copy)",
                original.description(),
                original.nodes(),
                original.connections(),
                original.settings(),
                false,
                original.triggerType(),
                original.cronExpression(),
                null, null, null, 0);

        return create(copy);
    }

    @Override
    public void setActive(Long id, boolean active) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        entity.setActive(active);
        entity.setActive(active);
        WorkflowEntity saved = workflowRepository.save(entity);
        notifyWatcher(toDTO(saved));
    }

    private void notifyWatcher(WorkflowDTO workflow) {
        fileWatcherServiceProvider.ifAvailable(watcher -> watcher.registerWorkflow(workflow));
    }

    private WorkflowDTO toDTO(WorkflowEntity entity) {
        List<Node> nodes = parseNodes(entity.getNodesJson());
        List<Connection> connections = parseConnections(entity.getConnectionsJson());
        Map<String, Object> settings = parseSettings(entity.getSettingsJson());

        return new WorkflowDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                nodes,
                connections,
                settings,
                entity.isActive(),
                entity.getTriggerType(),
                entity.getCronExpression(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastExecuted(),
                entity.getVersion());
    }

    private WorkflowEntity toEntity(WorkflowDTO dto) {
        WorkflowEntity entity = new WorkflowEntity(dto.name());
        entity.setDescription(dto.description());
        entity.setNodesJson(serializeNodes(dto.nodes()));
        entity.setConnectionsJson(serializeConnections(dto.connections()));
        entity.setSettingsJson(serializeSettings(dto.settings()));
        entity.setActive(dto.isActive());
        entity.setTriggerType(dto.triggerType());
        entity.setCronExpression(dto.cronExpression());
        return entity;
    }

    private List<Node> parseNodes(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to parse nodes JSON", "nodes", e);
        }
    }

    private List<Connection> parseConnections(String json) {
        if (json == null || json.isBlank())
            return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to parse connections JSON", "connections", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSettings(String json) {
        if (json == null || json.isBlank())
            return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to parse settings JSON", "settings", e);
        }
    }

    private String serializeNodes(List<Node> nodes) {
        if (nodes == null)
            return "[]";
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to serialize nodes", "nodes", e);
        }
    }

    private String serializeConnections(List<Connection> connections) {
        if (connections == null)
            return "[]";
        try {
            return objectMapper.writeValueAsString(connections);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to serialize connections", "connections", e);
        }
    }

    private String serializeSettings(Map<String, Object> settings) {
        if (settings == null)
            return null;
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            throw new DataParsingException("Failed to serialize settings", "settings", e);
        }
    }
}
