package com.Project.DistributedFileStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning file metadata to the client.
 * Does not expose the entity directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private String id;
    private String filename;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
    private String storageType;
    private String storageNode;
    private String checksum;
    private String status;
}
