package com.Project.DistributedFileStorage.exception;

/**
 * Thrown when a file is requested that does not exist in the database
 * or on the storage provider.
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);
    }
}
