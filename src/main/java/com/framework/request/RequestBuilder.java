package com.framework.request;

import com.framework.constants.FrameworkConstants;
import com.framework.exceptions.ApiRequestException;
import com.framework.logging.FrameworkLogger;
import com.framework.response.BaseResponse;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * RequestBuilder
 * ==============
 * A fluent builder that constructs and executes HTTP requests step by step.
 *
 * DESIGN PATTERN: Builder Pattern
 * --------------------------------
 * Instead of having many overloaded methods for different combinations of
 * path params / query params / headers / body, the builder allows test code
 * to chain only what it needs:
 *
 *   RequestBuilder.create()
 *       .withSpec(requestSpec)           // inject the base spec from BaseRequest
 *       .withEndpoint("/users/{id}")      // endpoint path
 *       .withPathParam("id", "123")       // path parameter
 *       .withQueryParam("active", "true") // query parameter
 *       .withHeader("X-Trace-Id", "abc")  // extra header
 *       .withPayload(payloadJson)         // request body string
 *       .get();                           // execute HTTP GET
 *
 * RETURNED VALUE:
 * ---------------
 * All HTTP methods return a BaseResponse — our wrapper around RestAssured Response.
 *
 * WHY NOT CALL restAssured.given() DIRECTLY IN TESTS?
 * ----------------------------------------------------
 * - Tests would need to repeat headers, base URI, auth, and filter setup.
 * - Centralizing request execution here means any future cross-cutting change
 *   (e.g., adding a new required header) is done in ONE place.
 */
public class RequestBuilder {

    private static final Logger logger = FrameworkLogger.getLogger(RequestBuilder.class);

    // ----------------------------------------------------------
    // Builder State Fields
    // ----------------------------------------------------------
    private RequestSpecification baseSpec;
    private String endpoint;
    private Object payload; // Can be a JSON String or a POJO
    private final Map<String, String> pathParams   = new HashMap<>();
    private final Map<String, String> queryParams  = new HashMap<>();
    private final Map<String, String> extraHeaders = new HashMap<>();
    private final Map<String, String> formParams   = new HashMap<>();

    // Private constructor — use create() factory method
    private RequestBuilder() {}

    /**
     * Creates and returns a new RequestBuilder instance.
     */
    public static RequestBuilder create() {
        return new RequestBuilder();
    }

    // ----------------------------------------------------------
    // Builder Methods (Fluent API)
    // ----------------------------------------------------------

    /**
     * Sets the base RestAssured spec (from BaseRequest or a custom spec).
     * This is required — call this before build() / get() / post() etc.
     */
    public RequestBuilder withSpec(RequestSpecification spec) {
        this.baseSpec = spec;
        return this;
    }

    /**
     * Sets the endpoint path.
     * Path parameters are denoted with {paramName}.
     * Example: "/users/{id}/orders/{orderId}"
     */
    public RequestBuilder withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Adds a path parameter.
     * Example: withPathParam("id", "123") resolves "/users/{id}" → "/users/123"
     */
    public RequestBuilder withPathParam(String name, Object value) {
        pathParams.put(name, String.valueOf(value));
        return this;
    }

    /**
     * Adds a query parameter.
     * Example: withQueryParam("page", "1") appends "?page=1" to the URL
     */
    public RequestBuilder withQueryParam(String name, Object value) {
        queryParams.put(name, String.valueOf(value));
        return this;
    }

    /**
     * Adds a custom HTTP header (in addition to the base headers from the spec).
     */
    public RequestBuilder withHeader(String name, String value) {
        extraHeaders.put(name, value);
        return this;
    }

    /**
     * Sets the request body.
     * Can be a JSON string (from PayloadManager) OR a POJO (which RestAssured will serialize).
     */
    public RequestBuilder withPayload(Object payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Adds a form parameter (for application/x-www-form-urlencoded requests).
     */
    public RequestBuilder withFormParam(String name, String value) {
        formParams.put(name, value);
        return this;
    }

    // ----------------------------------------------------------
    // Terminal Methods: Execute the HTTP Request
    // ----------------------------------------------------------

    /**
     * Executes an HTTP GET request.
     *
     * @return BaseResponse wrapping the RestAssured response
     */
    @Step("GET {0}")
    public BaseResponse get() {
        return executeRequest("GET");
    }

    /**
     * Executes an HTTP POST request.
     *
     * @return BaseResponse wrapping the RestAssured response
     */
    @Step("POST {0}")
    public BaseResponse post() {
        return executeRequest("POST");
    }

    /**
     * Executes an HTTP PUT request.
     *
     * @return BaseResponse wrapping the RestAssured response
     */
    @Step("PUT {0}")
    public BaseResponse put() {
        return executeRequest("PUT");
    }

    /**
     * Executes an HTTP PATCH request.
     *
     * @return BaseResponse wrapping the RestAssured response
     */
    @Step("PATCH {0}")
    public BaseResponse patch() {
        return executeRequest("PATCH");
    }

    /**
     * Executes an HTTP DELETE request.
     *
     * @return BaseResponse wrapping the RestAssured response
     */
    @Step("DELETE {0}")
    public BaseResponse delete() {
        return executeRequest("DELETE");
    }

    // ----------------------------------------------------------
    // Private: Shared Request Execution Logic
    // ----------------------------------------------------------

    /**
     * Builds the full RequestSpecification and executes the HTTP request.
     *
     * @param method the HTTP method string (GET, POST, PUT, PATCH, DELETE)
     * @return BaseResponse wrapping the actual response
     */
    private BaseResponse executeRequest(String method) {
        validateBuilderState();

        logger.info("[Thread: {}] {} {}", Thread.currentThread().getName(), method, endpoint);

        // Start from the base spec provided by BaseRequest
        RequestSpecification spec = RestAssured.given().spec(baseSpec);
        applyBuilderState(spec);

        // Execute the HTTP method
        Response rawResponse = executeHttpMethod(spec, method, endpoint);

        // Auto-retry once if we get a 401 Unauthorized (likely token expired mid-test)
        if (rawResponse.statusCode() == 401) {
            logger.warn("[Thread: {}] Received 401 Unauthorized. Forcing token refresh and retrying...",
                        Thread.currentThread().getName());

            // Force refresh token across the framework
            com.framework.auth.OAuth2TokenManager.getInstance().forceRefreshToken();

            // Rebuild spec and override the old Authorization header with the new token
            RequestSpecification retrySpec = RestAssured.given().spec(baseSpec);
            retrySpec.header(FrameworkConstants.HEADER_AUTHORIZATION,
                             com.framework.auth.OAuth2TokenManager.getInstance().getBearerToken());

            applyBuilderState(retrySpec);
            rawResponse = executeHttpMethod(retrySpec, method, endpoint);
        }

        logger.info("[Thread: {}] Response received: Status={}, Time={}ms",
                    Thread.currentThread().getName(),
                    rawResponse.statusCode(),
                    rawResponse.time());

        return new BaseResponse(rawResponse);
    }

    /**
     * Applies the builder state (params, headers, body) to the given RequestSpecification.
     */
    private void applyBuilderState(RequestSpecification spec) {
        if (!pathParams.isEmpty()) {
            spec.pathParams(pathParams);
        }
        if (!queryParams.isEmpty()) {
            spec.queryParams(queryParams);
        }
        if (!extraHeaders.isEmpty()) {
            spec.headers(extraHeaders);
        }
        if (!formParams.isEmpty()) {
            spec.formParams(formParams);
        }
        if (payload != null) {
            // If it's a String, RestAssured passes it raw.
            // If it's a POJO, RestAssured serializes it to JSON using Jackson.
            if (payload instanceof String s && s.isBlank()) {
                // Do not attach blank string bodies
            } else {
                spec.body(payload);
            }
        }
    }

    /**
     * Executes the RestAssured HTTP method based on the string verb.
     */
    private Response executeHttpMethod(RequestSpecification spec, String method, String endpoint) {
        return switch (method) {
            case "GET"    -> spec.when().get(endpoint);
            case "POST"   -> spec.when().post(endpoint);
            case "PUT"    -> spec.when().put(endpoint);
            case "PATCH"  -> spec.when().patch(endpoint);
            case "DELETE" -> spec.when().delete(endpoint);
            default -> throw new ApiRequestException("Unsupported HTTP method: " + method);
        };
    }

    /**
     * Validates that required builder fields have been set.
     */
    private void validateBuilderState() {
        if (baseSpec == null) {
            throw new ApiRequestException(
                "RequestBuilder: baseSpec is null. Call withSpec(requestSpec) before executing the request."
            );
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new ApiRequestException(
                "RequestBuilder: endpoint is null or blank. Call withEndpoint(\"/path\") before executing."
            );
        }
    }
}
