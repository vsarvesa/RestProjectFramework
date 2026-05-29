package com.framework.request;

import com.framework.auth.OAuth2TokenManager;
import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import com.framework.logging.FrameworkLogger;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.Logger;

/**
 * BaseRequest
 * ===========
 * The abstract parent for all API request classes.
 *
 * RESPONSIBILITY:
 * ---------------
 * Builds a fully-configured RestAssured RequestSpecification that is shared
 * by every API call in the framework:
 *   - Base URI from config.properties
 *   - Bearer token from OAuth2TokenManager (thread-local, safe for parallel runs)
 *   - Content-Type and Accept headers
 *   - Allure filter for automatic request/response capture in reports
 *   - Connection + read timeouts
 *   - Request/response logging to Log4j2
 *
 * OOP DESIGN:
 * -----------
 * This class is ABSTRACT — it cannot be instantiated directly.
 * Concrete subclasses (e.g., UserRequest, OrderRequest) extend this class
 * and add their endpoint-specific methods.
 *
 * HOW TO CREATE A CONCRETE REQUEST CLASS:
 * ----------------------------------------
 *   public class UserRequest extends BaseRequest {
 *
 *       public UserRequest() {
 *           super();  // Sets up base spec
 *       }
 *
 *       public Response createUser(String payload) {
 *           return given(requestSpec)
 *               .body(payload)
 *               .when()
 *               .post("/users")
 *               .then()
 *               .extract().response();
 *       }
 *   }
 */
public abstract class BaseRequest {

    private static final Logger logger = FrameworkLogger.getLogger(BaseRequest.class);

    /**
     * The fully-configured RestAssured request specification.
     * Protected so subclasses can access it directly.
     *
     * Each subclass instance gets its own spec — safe for parallel runs.
     */
    protected final RequestSpecification requestSpec;

    /**
     * Constructor — builds the base request specification.
     * Called automatically by each subclass via super().
     */
    protected BaseRequest() {
        this.requestSpec = buildBaseRequestSpec();
        logger.debug("[Thread: {}] BaseRequest spec created for: {}",
                     Thread.currentThread().getName(), this.getClass().getSimpleName());
    }

    // ----------------------------------------------------------
    // Protected Helpers — available to all subclasses
    // ----------------------------------------------------------

    /**
     * Returns the base request specification for use in subclass methods.
     * Subclasses should call: given(requestSpec).body(payload).when().post(endpoint)
     *
     * @return the configured RequestSpecification
     */
    protected RequestSpecification getRequestSpec() {
        return requestSpec;
    }

    // ----------------------------------------------------------
    // Private: Build the Specification
    // ----------------------------------------------------------

    /**
     * Builds and returns the fully configured RequestSpecification.
     *
     * Why RequestSpecBuilder?
     * - Allows building the spec independently of RestAssured.given()
     * - The built spec can be reused across multiple requests
     * - Thread-safe: each instance builds its own spec
     */
    private RequestSpecification buildBaseRequestSpec() {
        ConfigManager config = ConfigManager.getInstance();

        // Fetch Bearer token for the current thread
        String bearerToken = OAuth2TokenManager.getInstance().getBearerToken();

        return new RequestSpecBuilder()
            // Base URI — all endpoint paths are relative to this
            .setBaseUri(config.getBaseUrl())

            // OAuth 2.0 Bearer token authorization header
            .addHeader(FrameworkConstants.HEADER_AUTHORIZATION, bearerToken)

            // Correlation trace ID for logs / tracing
            .addHeader("X-Trace-Id", com.framework.utils.ThreadLocalManager.getTraceId())

            // Standard headers
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)

            // Allure filter — captures request & response in Allure report
            .addFilter(new AllureRestAssured()
                .setRequestTemplate("http-request.ftl")
                .setResponseTemplate("http-response.ftl"))

            // Log request details (method, URI, headers, body) for debugging
            .log(LogDetail.ALL)

            // Connection timeout
            .setConfig(io.restassured.config.RestAssuredConfig.config()
                .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout",
                              config.getIntProperty("http.connect.timeout",
                                                    FrameworkConstants.DEFAULT_CONNECT_TIMEOUT_MS))
                    .setParam("http.socket.timeout",
                              config.getIntProperty("http.read.timeout",
                                                    FrameworkConstants.DEFAULT_READ_TIMEOUT_MS))))
            .build();
    }
}
