package com.intelliguard.exception;

/**
 * Thrown when a transaction ID is not found in the database.
 * Extends RuntimeException so we don't need try-catch everywhere —
 * the GlobalExceptionHandler catches it automatically.
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}