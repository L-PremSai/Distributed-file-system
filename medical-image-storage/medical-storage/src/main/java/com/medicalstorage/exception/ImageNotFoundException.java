package com.medicalstorage.exception;

/**
 * Thrown when a requested medical image record does not exist in the database.
 */
public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(Long id) {
        super("Medical image not found with id: " + id);
    }

    public ImageNotFoundException(String message) {
        super(message);
    }
}
