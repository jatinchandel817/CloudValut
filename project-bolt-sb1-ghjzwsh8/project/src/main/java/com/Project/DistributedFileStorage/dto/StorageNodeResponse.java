package com.Project.DistributedFileStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning storage node information to the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageNodeResponse {
    private Long id;
    private String nodeName;
    private String storageType;
    private String endpoint;
    private Long availableSpace;
    private Long usedSpace;
    private String status;
    private LocalDateTime createdAt;
}
