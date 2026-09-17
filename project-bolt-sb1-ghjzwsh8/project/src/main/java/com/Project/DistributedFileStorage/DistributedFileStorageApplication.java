package com.Project.DistributedFileStorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Distributed File Storage application.
 *
 * Spring Boot auto-configuration handles:
 *   - Embedded Tomcat web server
 *   - JPA / Hibernate with the configured datasource
 *   - Multipart file upload support
 *   - Bean validation
 *
 * Run this class to start the application.
 */
@SpringBootApplication
public class DistributedFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedFileStorageApplication.class, args);
    }
}
