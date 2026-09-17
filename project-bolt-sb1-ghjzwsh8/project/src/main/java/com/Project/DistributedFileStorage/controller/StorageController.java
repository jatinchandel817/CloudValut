package com.Project.DistributedFileStorage.controller;

import com.Project.DistributedFileStorage.dto.StorageNodeResponse;
import com.Project.DistributedFileStorage.dto.StorageStatusResponse;
import com.Project.DistributedFileStorage.service.FileService;
import com.Project.DistributedFileStorage.service.StorageNodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for storage node information and system status.
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final StorageNodeService storageNodeService;
    private final FileService fileService;

    public StorageController(StorageNodeService storageNodeService, FileService fileService) {
        this.storageNodeService = storageNodeService;
        this.fileService = fileService;
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<StorageNodeResponse>> getStorageNodes() {
        return ResponseEntity.ok(storageNodeService.getAllNodes());
    }

    @GetMapping("/status")
    public ResponseEntity<StorageStatusResponse> getStorageStatus() {
        return ResponseEntity.ok(fileService.getStorageStatus());
    }
}
