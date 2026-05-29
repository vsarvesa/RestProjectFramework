package com.framework.auth;

import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import com.framework.exceptions.AuthenticationException;
import com.framework.logging.FrameworkLogger;
import com.framework.utils.ThreadLocalManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth2TokenManager
 * ==================
 * Handles OAuth 2.0 token acquisition for two grant types:
 *   1. client_credentials — machine-to-machine (no user context)
 *   2. password           — user-level token (requires username/password)
 *
 * THREAD SAFETY DESIGN:
 * ----------------------
 * - Each thread stores its own token in ThreadLocal.
 * - A ReentrantLock prevents multiple threads from fetching a token
 *   simultaneously when the cache is empty/expired (prevents thundering herd).
 * - Token expiry is tracked via expiresAt timestamp.
 *
 * HOW TO USE:
 * -----------
 *   // Fetch token and store in ThreadLocal for this thread
 *   OAuth2TokenManager.getInstance().initializeToken();
 *
 *   // Retrieve the Bearer token string (e.g., "Bearer eyJ...")
 *   String token = OAuth2TokenManager.getInstance().getBearerToken();
 */
public class OAuth2TokenManager {

    private static final Logger logger = FrameworkLogger.getLogger(OAuth2TokenManager.class);

    // ----------------------------------------------------------
    // Singleton (enum-based, thread-safe)
    // ----------------------------------------------------------
    private static final OAuth2TokenManager INSTANCE = new OAuth2TokenManager();

    private OAuth2TokenManager() {}

    public static OAuth2TokenManager getInstance() {
        return INSTANCE;
    }

    // ----------------------------------------------------------
    // Shared token cache (all threads share the same token if not expired)
    // This avoids hammering the auth server with one request per thread.
    // ----------------------------------------------------------
    // ----------------------------------------------------------
    // Shared token cache (isolated per client identity)
    // ----------------------------------------------------------
    private static class CachedToken {
        final String token;
        final Instant expiresAt;
        
        CachedToken(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }
        
        boolean isValid() {
            return Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(expiresAt);
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, CachedToken> tokenCache = 
        new java.util.concurrent.ConcurrentHashMap<>();

    /** Lock ensures only one thread fetches a new token at a time */
    private final ReentrantLock tokenLock = new ReentrantLock();

    /** Buffer in seconds — refresh token 30 seconds before actual expiry */
    private static final long EXPIRY_BUFFER_SECONDS = 30;

    /** Generates a unique cache key based on the credentials used */
    private String getCacheKey() {
        ConfigManager config = ConfigManager.getInstance();
        return config.getClientId() + ":" + config.getAuthUsername();
    }

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    /**
     * Initializes the token for the current thread.
     * Fetches a new token if the cache is empty or expired,
     * then stores the "Bearer <token>" string in ThreadLocal.
     *
     * Called once per test thread in BaseTest.@BeforeMethod.
     */
    public void initializeToken() {
        String token = fetchOrGetCachedToken();
        ThreadLocalManager.setAccessToken(FrameworkConstants.BEARER_PREFIX + token);
        logger.info("[Thread: {}] OAuth2 token initialized successfully.",
                    Thread.currentThread().getName());
    }

    /**
     * Returns the full Bearer token string for the current thread.
     * Format: "Bearer eyJhbGciOi..."
     *
     * @return Bearer token string
     * @throws AuthenticationException if token is not initialized for this thread
     */
    public String getBearerToken() {
        String token = ThreadLocalManager.getAccessToken();
        if (token == null) {
            throw new AuthenticationException(
                "No access token found for thread: " + Thread.currentThread().getName() +
                ". Ensure initializeToken() was called in @BeforeMethod."
            );
        }
        return token;
    }

    /**
     * Forces a new token fetch, ignoring the cache.
     * Useful when a test receives a 401 and needs to refresh.
     */
    public void forceRefreshToken() {
        logger.info("[Thread: {}] Forcing token refresh...", Thread.currentThread().getName());
        tokenLock.lock();
        try {
            String key = getCacheKey();
            tokenCache.remove(key); // Invalidate cache for this user identity
            CachedToken cached = fetchToken();
            tokenCache.put(key, cached);
            ThreadLocalManager.setAccessToken(FrameworkConstants.BEARER_PREFIX + cached.token);
        } finally {
            tokenLock.unlock();
        }
    }

    // ----------------------------------------------------------
    // Private: Token Cache Logic
    // ----------------------------------------------------------

    /**
     * Returns the cached token if still valid, otherwise fetches a new one.
     * Thread-safe via ReentrantLock.
     */
    private String fetchOrGetCachedToken() {
        String key = getCacheKey();
        
        // Fast path: token is valid, no lock needed
        CachedToken cached = tokenCache.get(key);
        if (cached != null && cached.isValid()) {
            logger.debug("Using cached OAuth2 token for identity: {}", key);
            return cached.token;
        }

        // Slow path: acquire lock and re-check (double-checked locking)
        tokenLock.lock();
        try {
            cached = tokenCache.get(key);
            if (cached != null && cached.isValid()) {
                logger.debug("Using cached OAuth2 token after lock for identity: {}", key);
                return cached.token;
            }
            
            cached = fetchToken();
            tokenCache.put(key, cached);
            return cached.token;
        } finally {
            tokenLock.unlock();
        }
    }

    // ----------------------------------------------------------
    // Private: Actual Token Fetch
    // ----------------------------------------------------------

    /**
     * Fetches a new token from the auth server based on the configured grant type.
     *
     * @return CachedToken containing raw access token and expiry timestamp
     * @throws AuthenticationException if the token fetch fails
     */
    private CachedToken fetchToken() {
        ConfigManager config    = ConfigManager.getInstance();
        String grantType        = config.getGrantType();
        String authUrl          = config.getAuthUrl();

        logger.info("Fetching new OAuth2 token using grant_type='{}' from: {}",
                    grantType, authUrl);

        Map<String, String> formParams = buildFormParams(config, grantType);

        try {
            Response response = RestAssured
                .given()
                    .contentType(FrameworkConstants.CONTENT_TYPE_FORM)
                    .formParams(formParams)
                    // Logging intentionally omitted — avoid logging sensitive credentials
                .when()
                    .post(authUrl)
                .then()
                    .extract().response();

            validateTokenResponse(response, authUrl);

            String accessToken = response.jsonPath().getString(FrameworkConstants.OAUTH_ACCESS_TOKEN);
            int expiresIn      = response.jsonPath().getInt(FrameworkConstants.OAUTH_EXPIRES_IN);
            Instant expiry     = Instant.now().plusSeconds(expiresIn);

            logger.info("OAuth2 token acquired successfully. Expires in {} seconds.", expiresIn);
            return new CachedToken(accessToken, expiry);

        } catch (AuthenticationException e) {
            throw e; // re-throw as-is
        } catch (Exception e) {
            throw new AuthenticationException(
                "Unexpected error while fetching OAuth2 token from: " + authUrl, e
            );
        }
    }

    /**
     * Builds the form parameters map based on grant type.
     */
    private Map<String, String> buildFormParams(ConfigManager config, String grantType) {
        Map<String, String> params = new HashMap<>();

        if (FrameworkConstants.OAUTH_CLIENT_CREDENTIALS.equals(grantType)) {
            // client_credentials: only needs client_id + client_secret
            params.put(FrameworkConstants.OAUTH_GRANT_TYPE,    FrameworkConstants.OAUTH_CLIENT_CREDENTIALS);
            params.put(FrameworkConstants.OAUTH_CLIENT_ID,     config.getClientId());
            params.put(FrameworkConstants.OAUTH_CLIENT_SECRET, config.getClientSecret());

        } else if (FrameworkConstants.OAUTH_PASSWORD.equals(grantType)) {
            // password: needs client_id + client_secret + username + password
            params.put(FrameworkConstants.OAUTH_GRANT_TYPE,    FrameworkConstants.OAUTH_PASSWORD);
            params.put(FrameworkConstants.OAUTH_CLIENT_ID,     config.getClientId());
            params.put(FrameworkConstants.OAUTH_CLIENT_SECRET, config.getClientSecret());
            params.put(FrameworkConstants.OAUTH_USERNAME,      config.getAuthUsername());
            params.put("password",                             config.getAuthPassword());

        } else {
            throw new AuthenticationException(
                "Unsupported OAuth2 grant type: '" + grantType + "'. " +
                "Supported values: 'client_credentials', 'password'. " +
                "Check auth.grant.type in config.properties."
            );
        }

        // Optional scope
        String scope = config.getAuthScope();
        if (scope != null && !scope.isBlank()) {
            params.put(FrameworkConstants.OAUTH_SCOPE, scope);
        }

        logger.debug("OAuth2 form params prepared (credentials masked): grant_type={}, client_id={}",
                     grantType, FrameworkLogger.maskSensitive(config.getClientId()));
        return params;
    }

    /**
     * Validates the token response from the auth server.
     *
     * @throws AuthenticationException if response is not 2xx or missing access_token
     */
    private void validateTokenResponse(Response response, String authUrl) {
        int statusCode = response.statusCode();

        if (statusCode < 200 || statusCode >= 300) {
            throw new AuthenticationException(
                "OAuth2 token request failed. " +
                "URL: " + authUrl +
                " | Status: " + statusCode +
                " | Response: " + response.body().asString()
            );
        }

        String accessToken = response.jsonPath().getString(FrameworkConstants.OAUTH_ACCESS_TOKEN);
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException(
                "OAuth2 token response did not contain 'access_token'. " +
                "Response body: " + response.body().asString()
            );
        }
    }
}
