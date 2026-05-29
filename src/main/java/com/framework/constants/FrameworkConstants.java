package com.framework.constants;

/**
 * FrameworkConstants
 * ==================
 * Central place for all constant values used across the framework.
 *
 * WHY: Avoids magic numbers and strings scattered in test code.
 *      Junior QAs can find and change these values in one place.
 */
public final class FrameworkConstants {

    // Private constructor — this class should never be instantiated
    private FrameworkConstants() {
        throw new UnsupportedOperationException("FrameworkConstants is a utility class and cannot be instantiated.");
    }

    // ----------------------------------------------------------
    // HTTP Status Codes
    // ----------------------------------------------------------
    public static final int STATUS_OK             = 200;
    public static final int STATUS_CREATED        = 201;
    public static final int STATUS_NO_CONTENT     = 204;
    public static final int STATUS_BAD_REQUEST    = 400;
    public static final int STATUS_UNAUTHORIZED   = 401;
    public static final int STATUS_FORBIDDEN      = 403;
    public static final int STATUS_NOT_FOUND      = 404;
    public static final int STATUS_TOO_MANY_REQ   = 429;
    public static final int STATUS_SERVER_ERROR   = 500;
    public static final int STATUS_BAD_GATEWAY    = 502;
    public static final int STATUS_UNAVAILABLE    = 503;
    public static final int STATUS_GATEWAY_TIMEOUT= 504;

    // ----------------------------------------------------------
    // Retry — which status codes should trigger a retry
    // ----------------------------------------------------------
    public static final int[] RETRYABLE_STATUS_CODES = {
        STATUS_TOO_MANY_REQ,   // 429 - rate limited
        STATUS_SERVER_ERROR,   // 500 - internal server error
        STATUS_BAD_GATEWAY,    // 502 - bad gateway
        STATUS_UNAVAILABLE,    // 503 - service unavailable
        STATUS_GATEWAY_TIMEOUT // 504 - gateway timeout
    };

    // ----------------------------------------------------------
    // Content Types
    // ----------------------------------------------------------
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    // ----------------------------------------------------------
    // HTTP Headers
    // ----------------------------------------------------------
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE  = "Content-Type";
    public static final String HEADER_ACCEPT        = "Accept";

    // ----------------------------------------------------------
    // OAuth 2.0 Field Names
    // ----------------------------------------------------------
    public static final String OAUTH_GRANT_TYPE         = "grant_type";
    public static final String OAUTH_CLIENT_CREDENTIALS = "client_credentials";
    public static final String OAUTH_PASSWORD           = "password";
    public static final String OAUTH_CLIENT_ID          = "client_id";
    public static final String OAUTH_CLIENT_SECRET      = "client_secret";
    public static final String OAUTH_USERNAME           = "username";
    public static final String OAUTH_SCOPE              = "scope";
    public static final String OAUTH_ACCESS_TOKEN       = "access_token";
    public static final String OAUTH_EXPIRES_IN         = "expires_in";
    public static final String BEARER_PREFIX            = "Bearer ";

    // ----------------------------------------------------------
    // Payload File Paths (relative to src/test/resources)
    // ----------------------------------------------------------
    public static final String PAYLOADS_DIR = "payloads/";

    // ----------------------------------------------------------
    // JSON Schema File Paths (relative to src/test/resources)
    // ----------------------------------------------------------
    public static final String SCHEMAS_DIR = "schemas/";

    // ----------------------------------------------------------
    // Default Timeouts (milliseconds)
    // ----------------------------------------------------------
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    public static final int DEFAULT_READ_TIMEOUT_MS    = 30_000;

    // ----------------------------------------------------------
    // Retry Defaults
    // ----------------------------------------------------------
    public static final int DEFAULT_RETRY_MAX_COUNT     = 3;
    public static final int DEFAULT_RETRY_INITIAL_DELAY = 1_000; // 1 second

    // ----------------------------------------------------------
    // Database Identifiers
    // ----------------------------------------------------------
    public static final String DB_MSSQL  = "MSSQL";
    public static final String DB_ORACLE = "ORACLE";
}
