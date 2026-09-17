package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.config.StorageConfig;
import com.Project.DistributedFileStorage.exception.StorageException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem storage implementation.
 * Files are stored under the configured local storage directory,
 * using UUID-prefixed filenames to avoid collisions and path traversal.
 */
@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private final StorageConfig storageConfig;

    public LocalStorageService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    @Override
    public String uploadFile(MultipartFile file, String storageKey) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Cannot upload an empty file");
            }

            // Resolve the destination path and normalize it to prevent path traversal
            Path destination = storageConfig.getRootPath()
                    .resolve(StringUtils.cleanPath(storageKey))
                    .normalize();

            // Security check: ensure the resolved path is still inside the upload directory
            if (!destination.startsWith(storageConfig.getRootPath())) {
                throw new StorageException("Path traversal detected: " + storageKey);
            }

            // Copy the file bytes to the destination
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return destination.toString();
        } catch (IOException ex) {
            throw new StorageException("Failed to store file locally: " + storageKey, ex);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();

            // Security: prevent path traversal when downloading
            if (!filePath.startsWith(storageConfig.getRootPath())) {
                throw new StorageException("Path traversal detected: " + storagePath);
            }

            if (!Files.exists(filePath)) {
                throw new StorageException("File not found on local storage: " + storagePath);
            }

            return Files.newInputStream(filePath);
        } catch (IOException ex) {
            throw new StorageException("Failed to read file from local storage: " + storagePath, ex);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).normalize();

            if (!filePath.startsWith(storageConfig.getRootPath())) {
                throw new StorageException("Path traversal detected: " + storagePath);
            }

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new StorageException("Failed to delete file from local storage: " + storagePath, ex);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        Path filePath = Paths.get(storagePath).normalize();
        return Files.exists(filePath) && filePath.startsWith(storageConfig.getRootPath());
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }
}
