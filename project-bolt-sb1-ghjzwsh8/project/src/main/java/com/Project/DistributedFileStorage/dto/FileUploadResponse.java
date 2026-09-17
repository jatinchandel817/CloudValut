package com.Project.DistributedFileStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO returned after a file upload completes.
 * Includes replication status information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {
    private boolean success;
    private String message;
    private String fileId;
    private String filename;
    private String originalName;
    private Long fileSize;
    private String fileType;
    private String storageType;
    private String storageNode;
    private String checksum;
    private String status;
    private String replicationStatus;
    private LocalDateTime uploadedAt;
}
