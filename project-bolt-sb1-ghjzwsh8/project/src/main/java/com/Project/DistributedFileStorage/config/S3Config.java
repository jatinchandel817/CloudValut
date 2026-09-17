package com.Project.DistributedFileStorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 configuration.
 * Creates an S3Client bean using the default AWS credential chain
 * (environment variables, system properties, or IAM role).
 *
 * Credentials are NEVER hardcoded — they come from the environment.
 */
@Configuration
public class S3Config {

    @Value("${aws.region:}")
    private String region;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    /**
     * Creates the S3Client bean. Only instantiated if the region is configured,
     * so the app can run without AWS credentials in local-only mode.
     */
    @Bean
    public S3Client s3Client() {
        if (region == null || region.isBlank()) {
            return null;
        }
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getRegion() {
        return region;
    }

    public boolean isConfigured() {
        return region != null && !region.isBlank()
                && bucketName != null && !bucketName.isBlank();
    }
}
