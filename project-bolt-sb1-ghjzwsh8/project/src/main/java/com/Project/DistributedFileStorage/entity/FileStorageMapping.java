package com.Project.DistributedFileStorage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Maps a file to the specific storage node where it physically resides.
 * A single file can have multiple mappings — one PRIMARY and zero or more REPLICA copies.
 */
@Entity
@Table(name = "file_storage_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_node_id", nullable = false)
    private StorageNode storageNode;

    /** Path/key where the file is stored on this specific node */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "stored_at", nullable = false, updatable = false)
    private LocalDateTime storedAt;

    /** PRIMARY, REPLICA, FAILED */
    @Column(name = "replication_status", nullable = false)
    @Builder.Default
    private String replicationStatus = "PRIMARY";

    @PrePersist
    void prePersist() {
        if (this.storedAt == null) {
            this.storedAt = LocalDateTime.now();
        }
    }
}
