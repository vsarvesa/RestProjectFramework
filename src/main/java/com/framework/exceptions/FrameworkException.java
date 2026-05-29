package com.framework.exceptions;

/**
 * FrameworkException
 * ==================
 * The base exception for all custom exceptions in this framework.
 *
 * DESIGN:
 * -------
 * - Extends RuntimeException so tests don't need try-catch everywhere.
 * - All other custom exceptions extend this class.
 * - Provides constructors for: message only, message + cause, cause only.
 *
 * HIERARCHY:
 * ----------
 *   FrameworkException
 *   ├── AuthenticationException  (OAuth2 token failures)
 *   ├── ApiRequestException      (HTTP call failures)
 *   └── DatabaseException        (DB connection / query failures)
 */
public class FrameworkException extends RuntimeException {

    /**
     * Creates a FrameworkException with a descriptive message.
     *
     * @param message explanation of what went wrong
     */
    public FrameworkException(String message) {
        super(message);
    }

    /**
     * Creates a FrameworkException wrapping another exception.
     *
     * @param message explanation of what went wrong
     * @param cause   the original exception that caused this failure
     */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a FrameworkException from a root cause alone.
     *
     * @param cause the original exception
     */
    public FrameworkException(Throwable cause) {
        super(cause);
    }
}
