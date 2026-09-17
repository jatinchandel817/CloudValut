package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.dto.FileResponse;
import com.Project.DistributedFileStorage.dto.FileUploadResponse;
import com.Project.DistributedFileStorage.dto.StorageStatusResponse;
import com.Project.DistributedFileStorage.entity.File;
import com.Project.DistributedFileStorage.entity.FileStorageMapping;
import com.Project.DistributedFileStorage.entity.StorageNode;
import com.Project.DistributedFileStorage.exception.FileNotFoundException;
import com.Project.DistributedFileStorage.exception.InvalidFileException;
import com.Project.DistributedFileStorage.exception.StorageException;
import com.Project.DistributedFileStorage.repository.FileRepository;
import com.Project.DistributedFileStorage.repository.FileStorageMappingRepository;
import com.Project.DistributedFileStorage.repository.StorageNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for file operations.
 * Handles upload (with storage node selection + optional replication),
 * download, delete, search, and metadata retrieval.
 *
 * Flow:
 *   Controller → FileService → StorageNodeService → StorageService → Provider
 *                                   ↓
 *                              Database (metadata)
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final FileStorageMappingRepository mappingRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final StorageNodeService storageNodeService;

    public FileService(FileRepository fileRepository,
                       FileStorageMappingRepository mappingRepository,
                       StorageNodeRepository storageNodeRepository,
                       StorageNodeService storageNodeService) {
        this.fileRepository = fileRepository;
        this.mappingRepository = mappingRepository;
        this.storageNodeRepository = storageNodeRepository;
        this.storageNodeService = storageNodeService;
    }

    // ============================================================
    //  UPLOAD
    // ============================================================

    /**
     * Upload a file to the distributed storage system.
     *
     * Steps:
     *   1. Validate the file
     *   2. Generate a unique storage key (UUID + original filename)
     *   3. Calculate SHA-256 checksum
     *   4. Select a storage node
     *   5. Upload the file to the selected provider
     *   6. Save metadata to the database
     *   7. Save the primary storage mapping
     *   8. Attempt replication to a second node (best-effort)
     *   9. Return the upload response
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file) {
        validateFile(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed");
        String storageKey = UUID.randomUUID().toString() + "_" + originalName;
        String checksum = calculateChecksum(file);

        // Select the best storage node
        StorageNode selectedNode = storageNodeService.selectStorageNode();
        StorageService storageService = storageNodeService.getStorageService(selectedNode.getStorageType());

        // Upload to the primary node
        String storagePath = storageService.uploadFile(file, storageKey);

        // Save file metadata
        File fileEntity = File.builder()
                .filename(storageKey)
                .originalName(originalName)
                .filePath(storagePath)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .storageType(selectedNode.getStorageType())
                .storageNode(selectedNode.getNodeName())
                .checksum(checksum)
                .status("ACTIVE")
                .build();
        fileRepository.save(fileEntity);

        // Save primary mapping
        FileStorageMapping primaryMapping = FileStorageMapping.builder()
                .file(fileEntity)
                .storageNode(selectedNode)
                .storagePath(storagePath)
                .replicationStatus("PRIMARY")
                .build();
        mappingRepository.save(primaryMapping);

        // Update used space on the node
        storageNodeService.updateUsedSpace(selectedNode.getId(), file.getSize());

        // Attempt replication (best-effort — does not fail the upload)
        String replicationStatus = "NONE";
        try {
            replicationStatus = replicateFile(file, storageKey, fileEntity, selectedNode);
        } catch (Exception ex) {
            log.warn("Replication failed for file {}: {}", fileEntity.getId(), ex.getMessage());
            replicationStatus = "FAILED";
        }

        return FileUploadResponse.builder()
                .success(true)
                .message("File uploaded successfully")
                .fileId(fileEntity.getId())
                .filename(storageKey)
                .originalName(originalName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .storageType(selectedNode.getStorageType())
                .storageNode(selectedNode.getNodeName())
                .checksum(checksum)
                .status("ACTIVE")
                .replicationStatus(replicationStatus)
                .uploadedAt(fileEntity.getUploadedAt())
                .build();
    }

    /**
     * Best-effort replication: copies the file to a second storage node
     * of a different type. If replication fails, the upload still succeeds.
     */
    private String replicateFile(MultipartFile file, String storageKey,
                                 File fileEntity, StorageNode primaryNode) {
        var replicaNodeOpt = storageNodeService.selectReplicaNode(primaryNode.getStorageType());
        if (replicaNodeOpt.isEmpty()) {
            return "NONE";
        }

        StorageNode replicaNode = replicaNodeOpt.get();
        StorageService replicaService = storageNodeService.getStorageService(replicaNode.getStorageType());
        String replicaPath = replicaService.uploadFile(file, storageKey);

        FileStorageMapping replicaMapping = FileStorageMapping.builder()
                .file(fileEntity)
                .storageNode(replicaNode)
                .storagePath(replicaPath)
                .replicationStatus("REPLICA")
                .build();
        mappingRepository.save(replicaMapping);

        storageNodeService.updateUsedSpace(replicaNode.getId(), file.getSize());

        return "REPLICATED";
    }

    // ============================================================
    //  DOWNLOAD
    // ============================================================

    /**
     * Download a file by its ID. Returns a Resource for streaming.
     */
    @Transactional
    public Resource downloadFile(String fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if ("DELETED".equals(file.getStatus())) {
            throw new FileNotFoundException("File has been deleted: " + fileId);
        }

        StorageService storageService = storageNodeService.getStorageService(file.getStorageType());
        InputStream inputStream = storageService.downloadFile(file.getFilePath());

        return new InputStreamResource(inputStream);
    }

    /**
     * Get the original filename for a file (for Content-Disposition header).
     */
    @Transactional(readOnly = true)
    public String getOriginalFilename(String fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));
        return file.getOriginalName();
    }

    /**
     * Get the content type for a file.
     */
    @Transactional(readOnly = true)
    public String getContentType(String fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));
        return file.getFileType() != null ? file.getFileType() : "application/octet-stream";
    }

    // ============================================================
    //  DELETE
    // ============================================================

    /**
     * Delete a file from storage and mark it as deleted in the database.
     * Removes the file from all storage nodes (primary + replicas).
     */
    @Transactional
    public void deleteFile(String fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        // Delete from all mapped storage nodes
        List<FileStorageMapping> mappings = mappingRepository.findByFileId(fileId);
        for (FileStorageMapping mapping : mappings) {
            try {
                StorageService storageService = storageNodeService
                        .getStorageService(mapping.getStorageNode().getStorageType());
                storageService.deleteFile(mapping.getStoragePath());
                storageNodeService.updateUsedSpace(mapping.getStorageNode().getId(), -file.getFileSize());
            } catch (Exception ex) {
                log.warn("Failed to delete file from {}: {}",
                        mapping.getStorageNode().getNodeName(), ex.getMessage());
            }
        }

        file.setStatus("DELETED");
        fileRepository.save(file);
    }

    // ============================================================
    //  METADATA & SEARCH
    // ============================================================

    @Transactional(readOnly = true)
    public List<FileResponse> getAllFiles() {
        return fileRepository.findAll().stream()
                .filter(f -> !"DELETED".equals(f.getStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileResponse getFileById(String id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + id));
        return toResponse(file);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> searchFiles(String name) {
        return fileRepository.searchByOriginalName(name).stream()
                .filter(f -> !"DELETED".equals(f.getStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    //  STORAGE STATUS
    // ============================================================

    @Transactional(readOnly = true)
    public StorageStatusResponse getStorageStatus() {
        List<FileResponse> allFiles = getAllFiles();

        long totalFiles = allFiles.size();
        long totalStorageUsed = fileRepository.sumTotalFileSize() != null ? fileRepository.sumTotalFileSize() : 0L;
        long localFiles = fileRepository.countByStorageType("LOCAL");
        long localStorageUsed = fileRepository.sumFileSizeByStorageType("LOCAL") != null
                ? fileRepository.sumFileSizeByStorageType("LOCAL") : 0L;
        long s3Files = fileRepository.countByStorageType("S3");
        long s3StorageUsed = fileRepository.sumFileSizeByStorageType("S3") != null
                ? fileRepository.sumFileSizeByStorageType("S3") : 0L;
        long gcsFiles = fileRepository.countByStorageType("GCS");
        long gcsStorageUsed = fileRepository.sumFileSizeByStorageType("GCS") != null
                ? fileRepository.sumFileSizeByStorageType("GCS") : 0L;

        return StorageStatusResponse.builder()
                .totalFiles(totalFiles)
                .totalStorageUsed(totalStorageUsed)
                .localFiles(localFiles)
                .localStorageUsed(localStorageUsed)
                .s3Files(s3Files)
                .s3StorageUsed(s3StorageUsed)
                .gcsFiles(gcsFiles)
                .gcsStorageUsed(gcsStorageUsed)
                .nodes(storageNodeService.getAllNodes())
                .build();
    }

    // ============================================================
    //  HELPERS
    // ============================================================

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("No file provided");
        }
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidFileException("File has no name");
        }
        // Reject path traversal attempts
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new InvalidFileException("Invalid filename: " + originalName);
        }
    }

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            log.warn("Failed to calculate checksum: {}", ex.getMessage());
            return null;
        }
    }

    private FileResponse toResponse(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .originalName(file.getOriginalName())
                .filePath(file.getFilePath())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .uploadedAt(file.getUploadedAt())
                .storageType(file.getStorageType())
                .storageNode(file.getStorageNode())
                .checksum(file.getChecksum())
                .status(file.getStatus())
                .build();
    }
}
