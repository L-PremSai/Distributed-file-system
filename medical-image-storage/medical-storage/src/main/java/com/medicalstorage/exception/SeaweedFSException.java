package com.medicalstorage.exception;

/**
 * Thrown when communication with SeaweedFS fails (assign, upload, delete).
 */
public class SeaweedFSException extends RuntimeException {

    public SeaweedFSException(String message) {
        super(message);
    }

    public SeaweedFSException(String message, Throwable cause) {
        super(message, cause);
    }
}
