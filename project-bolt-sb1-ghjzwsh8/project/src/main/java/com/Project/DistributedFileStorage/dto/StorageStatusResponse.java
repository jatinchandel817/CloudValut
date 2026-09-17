package com.Project.DistributedFileStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for returning the overall storage system status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageStatusResponse {
    private long totalFiles;
    private long totalStorageUsed;
    private long localFiles;
    private long localStorageUsed;
    private long s3Files;
    private long s3StorageUsed;
    private long gcsFiles;
    private long gcsStorageUsed;
    private List<StorageNodeResponse> nodes;
}
