package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.config.GCSConfig;
import com.Project.DistributedFileStorage.exception.StorageException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Google Cloud Storage implementation.
 * Uses the official Google Cloud Storage Java SDK.
 */
@Service("gcsStorageService")
public class GCSStorageService implements StorageService {

    private final Storage gcsStorage;
    private final GCSConfig gcsConfig;

    public GCSStorageService(Storage gcsStorage, GCSConfig gcsConfig) {
        this.gcsStorage = gcsStorage;
        this.gcsConfig = gcsConfig;
    }

    @Override
    public String uploadFile(MultipartFile file, String storageKey) {
        if (gcsStorage == null || !gcsConfig.isConfigured()) {
            throw new StorageException("Google Cloud Storage is not configured");
        }
        try {
            BlobId blobId = BlobId.of(gcsConfig.getBucketName(), storageKey);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            gcsStorage.createFrom(blobInfo, file.getInputStream(), file.getSize());

            return storageKey;
        } catch (Exception ex) {
            throw new StorageException("Failed to upload file to GCS: " + storageKey, ex);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        if (gcsStorage == null || !gcsConfig.isConfigured()) {
            throw new StorageException("Google Cloud Storage is not configured");
        }
        try {
            BlobId blobId = BlobId.of(gcsConfig.getBucketName(), storagePath);
            Blob blob = gcsStorage.get(blobId);
            if (blob == null) {
                throw new StorageException("File not found in GCS: " + storagePath);
            }
            return blob.reader();
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from GCS: " + storagePath, ex);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        if (gcsStorage == null || !gcsConfig.isConfigured()) {
            throw new StorageException("Google Cloud Storage is not configured");
        }
        try {
            BlobId blobId = BlobId.of(gcsConfig.getBucketName(), storagePath);
            gcsStorage.delete(blobId);
        } catch (Exception ex) {
            throw new StorageException("Failed to delete file from GCS: " + storagePath, ex);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        if (gcsStorage == null || !gcsConfig.isConfigured()) {
            return false;
        }
        try {
            BlobId blobId = BlobId.of(gcsConfig.getBucketName(), storagePath);
            Blob blob = gcsStorage.get(blobId);
            return blob != null && blob.exists();
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "GCS";
    }
}
