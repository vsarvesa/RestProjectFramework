package com.framework.response;

import com.framework.logging.FrameworkLogger;
import com.framework.utils.JsonUtils;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

/**
 * BaseResponse
 * ============
 * A wrapper around RestAssured's Response object.
 *
 * WHY WRAP?
 * ---------
 * - Provides typed convenience methods (getStatusCode, as, getHeader)
 * - Hides RestAssured-specific API from test code
 * - Allows response time tracking and logging in one place
 * - Makes it easy to swap the HTTP client in the future without changing tests
 *
 * HOW TO USE:
 * -----------
 *   BaseResponse response = userRequest.getUser("123");
 *
 *   // Check status code
 *   int statusCode = response.getStatusCode(); // → 200
 *
 *   // Deserialize body to POJO
 *   UserResponse user = response.as(UserResponse.class);
 *
 *   // Read a specific field via JsonPath
 *   String name = response.getJsonPathValue("data.name");
 *
 *   // Get the raw body as a string
 *   String body = response.getBodyAsString();
 */
public class BaseResponse {

    private static final Logger logger = FrameworkLogger.getLogger(BaseResponse.class);

    /** The underlying RestAssured response — not exposed directly to tests */
    private final Response response;

    /**
     * Constructor — wraps a RestAssured Response.
     * Called by RequestBuilder after every HTTP call.
     *
     * @param response the raw RestAssured response
     */
    public BaseResponse(Response response) {
        this.response = response;
        logResponseSummary();
    }

    // ----------------------------------------------------------
    // Status Code
    // ----------------------------------------------------------

    /**
     * Returns the HTTP status code of the response.
     * Example: 200, 201, 404, 500
     *
     * @return HTTP status code as int
     */
    public int getStatusCode() {
        return response.statusCode();
    }

    /**
     * Returns true if the status code is 2xx (success).
     *
     * @return true if successful (200–299)
     */
    public boolean isSuccessful() {
        int status = response.statusCode();
        return status >= 200 && status < 300;
    }

    // ----------------------------------------------------------
    // Response Body
    // ----------------------------------------------------------

    /**
     * Returns the entire response body as a plain String.
     *
     * @return response body string
     */
    public String getBodyAsString() {
        return response.body().asString();
    }

    /**
     * Deserializes the response body JSON into the specified POJO class.
     *
     * Example:
     *   UserResponse user = response.as(UserResponse.class);
     *
     * @param clazz the target class for deserialization
     * @param <T>   generic type parameter
     * @return the deserialized object
     */
    public <T> T as(Class<T> clazz) {
        return JsonUtils.fromJson(getBodyAsString(), clazz);
    }

    // ----------------------------------------------------------
    // JsonPath Access
    // ----------------------------------------------------------

    /**
     * Reads a specific field from the response body using JsonPath syntax.
     *
     * JsonPath examples:
     *   "id"               → top-level field
     *   "data.name"        → nested field
     *   "data[0].id"       → first item in an array
     *   "data.findAll{it.active == true}[0].name" → conditional
     *
     * @param path JsonPath expression
     * @param <T>  expected return type
     * @return the value at the given path
     */
    public <T> T getJsonPathValue(String path) {
        return response.jsonPath().get(path);
    }

    /**
     * Reads a String field from the response body using JsonPath.
     *
     * @param path JsonPath expression
     * @return value as String
     */
    public String getStringValue(String path) {
        return response.jsonPath().getString(path);
    }

    /**
     * Reads an integer field from the response body using JsonPath.
     *
     * @param path JsonPath expression
     * @return value as int
     */
    public int getIntValue(String path) {
        return response.jsonPath().getInt(path);
    }

    // ----------------------------------------------------------
    // Headers
    // ----------------------------------------------------------

    /**
     * Returns the value of a specific response header.
     *
     * @param headerName the header name (case-insensitive)
     * @return header value or null if not present
     */
    public String getHeader(String headerName) {
        return response.header(headerName);
    }

    // ----------------------------------------------------------
    // Response Time
    // ----------------------------------------------------------

    /**
     * Returns the total time taken for the request in milliseconds.
     *
     * @return response time in milliseconds
     */
    public long getResponseTimeMs() {
        return response.time();
    }

    // ----------------------------------------------------------
    // Raw Response Access
    // ----------------------------------------------------------

    /**
     * Returns the underlying RestAssured Response.
     * Use only when you need RestAssured-specific functionality
     * not exposed by BaseResponse.
     *
     * @return the raw RestAssured Response
     */
    public Response getRawResponse() {
        return response;
    }

    // ----------------------------------------------------------
    // Private Helpers
    // ----------------------------------------------------------

    /**
     * Logs a one-line summary of the response (status + time).
     * Full body is logged at DEBUG level only.
     */
    private void logResponseSummary() {
        logger.info("[Thread: {}] Response: Status={}, Time={}ms, ContentType={}",
                    Thread.currentThread().getName(),
                    response.statusCode(),
                    response.time(),
                    response.contentType());
        logger.debug("[Thread: {}] Response Body: {}",
                     Thread.currentThread().getName(),
                     response.body().asString());
    }
}
