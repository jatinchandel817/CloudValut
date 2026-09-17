package com.Project.DistributedFileStorage.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Abstraction over a storage provider (Local, S3, GCS, or any future provider).
 * Each implementation handles the physical storage of file bytes.
 *
 * This interface is the core of the storage abstraction layer.
 * To add a new provider, implement this interface and register it.
 */
public interface StorageService {

    /**
     * Upload a file to this storage provider.
     *
     * @param file         the multipart file from the client
     * @param storageKey   the UUID-prefixed unique filename to use for storage
     * @return the full path/key where the file was stored
     */
    String uploadFile(MultipartFile file, String storageKey);

    /**
     * Download a file as an InputStream from this storage provider.
     *
     * @param storagePath the path/key returned by uploadFile
     * @return InputStream of the file content
     */
    InputStream downloadFile(String storagePath);

    /**
     * Delete a file from this storage provider.
     *
     * @param storagePath the path/key returned by uploadFile
     */
    void deleteFile(String storagePath);

    /**
     * Check whether a file exists at the given path.
     *
     * @param storagePath the path/key to check
     * @return true if the file exists
     */
    boolean fileExists(String storagePath);

    /**
     * Return the storage type identifier for this provider.
     * @return "LOCAL", "S3", or "GCS"
     */
    String getStorageType();
}
