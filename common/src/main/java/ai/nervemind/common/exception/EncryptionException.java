/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.common.exception;

/**
 * Exception thrown when encryption or decryption operations fail.
 * This includes failures with credential encryption, key generation,
 * and secure data handling.
 */
public class EncryptionException extends NerveMindException {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new EncryptionException.
     *
     * @param message the exception message
     */
    public EncryptionException(String message) {
        super(message);
    }

    /**
     * Create a new EncryptionException with cause.
     *
     * @param message the exception message
     * @param cause   the underlying cause
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
