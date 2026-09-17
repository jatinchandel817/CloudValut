package com.Project.DistributedFileStorage.service;

import com.Project.DistributedFileStorage.config.S3Config;
import com.Project.DistributedFileStorage.exception.StorageException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

/**
 * AWS S3 storage implementation.
 * Uses the AWS SDK v2 S3Client.
 */
@Service("s3StorageService")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Config s3Config;

    public S3StorageService(S3Client s3Client, S3Config s3Config) {
        this.s3Client = s3Client;
        this.s3Config = s3Config;
    }

    @Override
    public String uploadFile(MultipartFile file, String storageKey) {
        if (s3Client == null || !s3Config.isConfigured()) {
            throw new StorageException("AWS S3 is not configured");
        }
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return storageKey;
        } catch (Exception ex) {
            throw new StorageException("Failed to upload file to S3: " + storageKey, ex);
        }
    }

    @Override
    public InputStream downloadFile(String storagePath) {
        if (s3Client == null || !s3Config.isConfigured()) {
            throw new StorageException("AWS S3 is not configured");
        }
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(storagePath)
                    .build();

            return s3Client.getObject(getRequest);
        } catch (NoSuchKeyException ex) {
            throw new StorageException("File not found in S3: " + storagePath);
        } catch (Exception ex) {
            throw new StorageException("Failed to download file from S3: " + storagePath, ex);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        if (s3Client == null || !s3Config.isConfigured()) {
            throw new StorageException("AWS S3 is not configured");
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(storagePath)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception ex) {
            throw new StorageException("Failed to delete file from S3: " + storagePath, ex);
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        if (s3Client == null || !s3Config.isConfigured()) {
            return false;
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(storagePath)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "S3";
    }
}
