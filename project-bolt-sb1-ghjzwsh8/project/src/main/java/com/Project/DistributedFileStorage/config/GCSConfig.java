package com.Project.DistributedFileStorage.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Storage configuration.
 * Creates a GCS Storage bean using Application Default Credentials (ADC).
 *
 * Credentials are NEVER hardcoded — they come from the environment
 * (GOOGLE_APPLICATION_CREDENTIALS env var or gcloud auth).
 */
@Configuration
public class GCSConfig {

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.bucket-name:}")
    private String bucketName;

    @Bean
    public Storage gcsStorage() {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    public String getProjectId() {
        return projectId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public boolean isConfigured() {
        return projectId != null && !projectId.isBlank()
                && bucketName != null && !bucketName.isBlank();
    }
}
