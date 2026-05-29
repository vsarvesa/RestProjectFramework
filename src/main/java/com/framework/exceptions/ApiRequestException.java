package com.framework.exceptions;

/**
 * ApiRequestException
 * ===================
 * Thrown when an API call encounters an unrecoverable failure.
 *
 * WHEN IS THIS THROWN?
 * --------------------
 * - Network connectivity issues (e.g., host unreachable)
 * - All retry attempts exhausted and final response is still an error
 * - Response body cannot be parsed
 *
 * EXTRA CONTEXT:
 * --------------
 * Carries the HTTP status code and raw response body so callers
 * can log meaningful error messages.
 *
 * EXAMPLE:
 * --------
 *   throw new ApiRequestException(
 *       "All 3 retries exhausted. Endpoint: POST /users",
 *       503,
 *       responseBody
 *   );
 */
public class ApiRequestException extends FrameworkException {

    /**
     * HTTP status code of the failing response.
     * -1 if the request never reached the server (e.g., network error).
     */
    private final int statusCode;

    /**
     * Raw response body of the failing response.
     * May be empty if the request never reached the server.
     */
    private final String responseBody;

    /**
     * Creates an ApiRequestException with message only (no HTTP details).
     * Use this for pre-request failures (e.g., missing payload file).
     *
     * @param message explanation of the failure
     */
    public ApiRequestException(String message) {
        super(message);
        this.statusCode   = -1;
        this.responseBody = "";
    }

    /**
     * Creates an ApiRequestException with full HTTP context.
     *
     * @param message      explanation of the failure
     * @param statusCode   HTTP status code of the failed response
     * @param responseBody raw response body string
     */
    public ApiRequestException(String message, int statusCode, String responseBody) {
        super(message + " | StatusCode=" + statusCode + " | Body=" + responseBody);
        this.statusCode   = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates an ApiRequestException wrapping a lower-level cause.
     *
     * @param message explanation of the failure
     * @param cause   the underlying exception (e.g., SocketException)
     */
    public ApiRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode   = -1;
        this.responseBody = "";
    }

    /**
     * Returns the HTTP status code of the failed response.
     * Returns -1 if the request never reached the server.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw response body of the failed response.
     */
    public String getResponseBody() {
        return responseBody;
    }
}
