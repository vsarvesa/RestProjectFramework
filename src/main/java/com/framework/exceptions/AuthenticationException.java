package com.framework.exceptions;

/**
 * AuthenticationException
 * =======================
 * Thrown when OAuth 2.0 token acquisition fails.
 *
 * WHEN IS THIS THROWN?
 * --------------------
 * - Token endpoint returns non-2xx response
 * - Response body does not contain "access_token"
 * - Token refresh fails
 * - Invalid client credentials or username/password
 *
 * EXAMPLE:
 * --------
 *   throw new AuthenticationException(
 *       "Token request failed. Status: 401. Response: " + responseBody
 *   );
 */
public class AuthenticationException extends FrameworkException {

    /**
     * Creates an AuthenticationException with a descriptive message.
     *
     * @param message explanation of the authentication failure
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates an AuthenticationException wrapping an original cause.
     *
     * @param message explanation of the authentication failure
     * @param cause   the underlying exception (e.g., network error)
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
