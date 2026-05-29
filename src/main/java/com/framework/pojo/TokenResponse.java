package com.framework.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TokenResponse
 * =============
 * POJO representing the OAuth 2.0 token endpoint response.
 *
 * EXAMPLE JSON RESPONSE:
 * ----------------------
 * {
 *   "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "token_type": "Bearer",
 *   "expires_in": 3600,
 *   "scope": "read write",
 *   "refresh_token": "def50200..."
 * }
 *
 * ANNOTATIONS:
 * ------------
 * @JsonIgnoreProperties(ignoreUnknown = true)
 *   → If the auth server returns extra fields we don't care about,
 *     Jackson will silently ignore them instead of throwing an error.
 *
 * @JsonProperty("access_token")
 *   → Maps the JSON key "access_token" (snake_case) to the Java
 *     field accessToken (camelCase).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("refresh_token")
    private String refreshToken;

    // ----------------------------------------------------------
    // Default constructor (required by Jackson)
    // ----------------------------------------------------------
    public TokenResponse() {}

    // ----------------------------------------------------------
    // Getters
    // ----------------------------------------------------------

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // ----------------------------------------------------------
    // Setters (needed for Jackson deserialization)
    // ----------------------------------------------------------

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
               "tokenType='" + tokenType + '\'' +
               ", expiresIn=" + expiresIn +
               ", scope='" + scope + '\'' +
               // NOTE: access token is deliberately NOT in toString() to avoid logging it
               '}';
    }
}
