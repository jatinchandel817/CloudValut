package com.Project.DistributedFileStorage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a file stored in the distributed storage system.
 * Stores metadata only — the actual file bytes live in a storage provider.
 */
@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    /** UUID-prefixed physical filename on the storage provider, e.g. "8f3a2c..._resume.pdf" */
    @Column(name = "filename", nullable = false)
    private String filename;

    /** Original filename as uploaded by the user */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /** Full path/key where the file is stored on the provider */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /** Which provider: LOCAL, S3, GCS */
    @Column(name = "storage_type", nullable = false)
    private String storageType;

    /** Human-readable name of the storage node used */
    @Column(name = "storage_node")
    private String storageNode;

    /** SHA-256 checksum of the file content for integrity verification */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /** ACTIVE, DELETED, REPLICATED, FAILED */
    @Column(name = "status", nullable = false)
    private String status;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FileStorageMapping> storageMappings = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
