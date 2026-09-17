package com.Project.DistributedFileStorage.exception;

/**
 * Thrown when an uploaded file is invalid — empty, has a path-traversal
 * filename, or is an unsupported type.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
