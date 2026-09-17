package com.Project.DistributedFileStorage.controller;

import com.Project.DistributedFileStorage.dto.FileResponse;
import com.Project.DistributedFileStorage.dto.FileUploadResponse;
import com.Project.DistributedFileStorage.dto.StorageStatusResponse;
import com.Project.DistributedFileStorage.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for file operations.
 * Kept thin — all business logic is in FileService.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResponse response = fileService.uploadFile(file);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> getAllFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(@PathVariable String id) {
        return ResponseEntity.ok(fileService.getFileById(id));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {
        Resource resource = fileService.downloadFile(id);
        String filename = fileService.getOriginalFilename(id);
        String contentType = fileService.getContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File deleted successfully",
                "fileId", id
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileResponse>> searchFiles(@RequestParam("name") String name) {
        return ResponseEntity.ok(fileService.searchFiles(name));
    }

    @GetMapping("/status")
    public ResponseEntity<StorageStatusResponse> getStorageStatus() {
        return ResponseEntity.ok(fileService.getStorageStatus());
    }
}
