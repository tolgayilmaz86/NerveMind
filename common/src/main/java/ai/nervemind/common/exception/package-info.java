/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */

/**
 * Custom exceptions for the NerveMind application.
 *
 * <p>
 * This package contains a hierarchy of domain-specific exceptions:
 * <ul>
 * <li>{@link ai.nervemind.common.exception.NerveMindException} - Base exception
 * for
 * all NerveMind exceptions</li>
 * <li>{@link ai.nervemind.common.exception.NodeExecutionException} - Workflow
 * node execution failures</li>
 * <li>{@link ai.nervemind.common.exception.ApiException} - External API call
 * failures</li>
 * <li>{@link ai.nervemind.common.exception.EncryptionException} -
 * Encryption/decryption failures</li>
 * <li>{@link ai.nervemind.common.exception.DataParsingException} - Data
 * serialization/parsing failures</li>
 * <li>{@link ai.nervemind.common.exception.UiInitializationException} - UI
 * component initialization failures</li>
 * </ul>
 *
 * <p>
 * All exceptions extend {@link java.lang.RuntimeException} via
 * {@link ai.nervemind.common.exception.NerveMindException}
 * to avoid cluttering method signatures with checked exceptions while still
 * providing semantic meaning.
 */
package ai.nervemind.common.exception;
