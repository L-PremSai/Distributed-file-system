package com.medicalstorage.exception;

/**
 * Thrown for invalid file uploads (empty file, unsupported MIME type, etc.)
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
