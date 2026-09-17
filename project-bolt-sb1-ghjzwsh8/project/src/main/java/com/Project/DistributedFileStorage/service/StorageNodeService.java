package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.dto.StorageNodeResponse;
import com.Project.DistributedFileStorage.entity.StorageNode;
import com.Project.DistributedFileStorage.repository.StorageNodeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages storage node lifecycle and selection.
 * On startup, initializes default nodes for each enabled provider.
 *
 * The selection algorithm picks the active node with the highest available space.
 * This is designed to be extensible — a load-balancing or round-robin strategy
 * can replace the current "most free space" algorithm later.
 */
@Service
public class StorageNodeService {

    private final StorageNodeRepository storageNodeRepository;
    private final LocalStorageService localStorageService;
    private final S3StorageService s3StorageService;
    private final GCSStorageService gcsStorageService;

    @Value("${storage.enabled-providers:LOCAL}")
    private String enabledProviders;

    public StorageNodeService(StorageNodeRepository storageNodeRepository,
                              LocalStorageService localStorageService,
                              S3StorageService s3StorageService,
                              GCSStorageService gcsStorageService) {
        this.storageNodeRepository = storageNodeRepository;
        this.localStorageService = localStorageService;
        this.s3StorageService = s3StorageService;
        this.gcsStorageService = gcsStorageService;
    }

    /**
     * Initialize default storage nodes on startup for each enabled provider.
     */
    @PostConstruct
    @Transactional
    public void initializeNodes() {
        String[] providers = enabledProviders.split(",");
        for (String provider : providers) {
            String type = provider.trim().toUpperCase();
            switch (type) {
                case "LOCAL" -> ensureNode("Local-Node-1", "LOCAL", "./uploads", 10_000_000_000L);
                case "S3" -> ensureNode("AWS-S3-Node-1", "S3", "s3.amazonaws.com", null);
                case "GCS" -> ensureNode("GCS-Node-1", "GCS", "storage.googleapis.com", null);
            }
        }
    }

    private void ensureNode(String nodeName, String storageType, String endpoint, Long availableSpace) {
        if (storageNodeRepository.findByNodeName(nodeName).isEmpty()) {
            StorageNode node = StorageNode.builder()
                    .nodeName(nodeName)
                    .storageType(storageType)
                    .endpoint(endpoint)
                    .availableSpace(availableSpace)
                    .usedSpace(0L)
                    .status("ACTIVE")
                    .build();
            storageNodeRepository.save(node);
        }
    }

    /**
     * Selects the best storage node for a new file.
     * Algorithm: pick the active node with the highest available space.
     * For cloud nodes (S3, GCS) with null available space, treat as unlimited (Long.MAX_VALUE).
     *
     * @return the selected storage node
     */
    @Transactional
    public StorageNode selectStorageNode() {
        List<StorageNode> activeNodes = storageNodeRepository.findByStatus("ACTIVE");
        if (activeNodes.isEmpty()) {
            throw new IllegalStateException("No active storage nodes available");
        }

        return activeNodes.stream()
                .max((a, b) -> {
                    long spaceA = a.getAvailableSpace() != null ? a.getAvailableSpace() : Long.MAX_VALUE;
                    long spaceB = b.getAvailableSpace() != null ? b.getAvailableSpace() : Long.MAX_VALUE;
                    return Long.compare(spaceA, spaceB);
                })
                .orElseThrow(() -> new IllegalStateException("No active storage nodes available"));
    }

    /**
     * Select a replica node that is different from the primary node's type.
     */
    @Transactional
    public Optional<StorageNode> selectReplicaNode(String primaryStorageType) {
        List<StorageNode> activeNodes = storageNodeRepository.findByStatus("ACTIVE");
        return activeNodes.stream()
                .filter(node -> !node.getStorageType().equals(primaryStorageType))
                .filter(node -> {
                    if (node.getAvailableSpace() == null) return true;
                    return node.getAvailableSpace() > 0;
                })
                .findFirst();
    }

    /**
     * Get the StorageService implementation for a given storage type.
     */
    public StorageService getStorageService(String storageType) {
        return switch (storageType) {
            case "LOCAL" -> localStorageService;
            case "S3" -> s3StorageService;
            case "GCS" -> gcsStorageService;
            default -> throw new IllegalArgumentException("Unknown storage type: " + storageType);
        };
    }

    /**
     * Update used space on a node after an upload or delete.
     */
    @Transactional
    public void updateUsedSpace(Long nodeId, long deltaBytes) {
        StorageNode node = storageNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalStateException("Storage node not found: " + nodeId));
        long newUsed = (node.getUsedSpace() != null ? node.getUsedSpace() : 0L) + deltaBytes;
        if (newUsed < 0) newUsed = 0;
        node.setUsedSpace(newUsed);
        if (node.getAvailableSpace() != null) {
            // Only adjust available for nodes with finite space (local)
            node.setAvailableSpace(Math.max(0, node.getAvailableSpace() - deltaBytes));
        }
        storageNodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    public List<StorageNodeResponse> getAllNodes() {
        return storageNodeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StorageNodeResponse> getActiveNodes() {
        return storageNodeRepository.findByStatus("ACTIVE").stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private StorageNodeResponse toResponse(StorageNode node) {
        return StorageNodeResponse.builder()
                .id(node.getId())
                .nodeName(node.getNodeName())
                .storageType(node.getStorageType())
                .endpoint(node.getEndpoint())
                .availableSpace(node.getAvailableSpace())
                .usedSpace(node.getUsedSpace())
                .status(node.getStatus())
                .createdAt(node.getCreatedAt())
                .build();
    }
}
