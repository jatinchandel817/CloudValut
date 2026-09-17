package com.Project.DistributedFileStorage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a logical storage node in the distributed system.
 * A node maps to one storage provider: LOCAL, S3, or GCS.
 */
@Entity
@Table(name = "storage_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true)
    private String nodeName;

    /** LOCAL, S3, GCS */
    @Column(name = "storage_type", nullable = false)
    private String storageType;

    /** Endpoint URL or local path */
    @Column(name = "endpoint")
    private String endpoint;

    /** Total available space in bytes (null = unlimited, e.g. cloud) */
    @Column(name = "available_space")
    private Long availableSpace;

    @Column(name = "used_space")
    @Builder.Default
    private Long usedSpace = 0L;

    /** ACTIVE, INACTIVE, OFFLINE */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
