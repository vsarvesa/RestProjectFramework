package com.framework.response;

import com.framework.constants.FrameworkConstants;
import com.framework.logging.FrameworkLogger;
import io.qameta.allure.Step;
import io.restassured.module.jsv.JsonSchemaValidator;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * ResponseValidator
 * =================
 * Provides fluent, reusable assertion methods for API responses.
 *
 * DESIGN PATTERN: Method Chaining (Fluent Interface)
 * ---------------------------------------------------
 * Each assertion method validates a condition and returns `this`,
 * allowing assertions to be chained together in a readable way:
 *
 *   ResponseValidator.validate(response)
 *       .assertStatusCode(200)
 *       .assertFieldEquals("data.name", "John")
 *       .assertFieldNotNull("data.id")
 *       .assertResponseTimeBelow(2000)
 *       .assertSchemaMatches("user_response_schema.json");
 *
 * WHY USE THIS INSTEAD OF PLAIN TESTNG ASSERTS?
 * -----------------------------------------------
 * - Keeps test methods short and readable
 * - Automatically adds @Step annotations → appears in Allure report
 * - Generates meaningful failure messages
 * - Centralizes assertion logic in one place
 */
public class ResponseValidator {

    private static final Logger logger = FrameworkLogger.getLogger(ResponseValidator.class);

    /** The response being validated */
    private final BaseResponse response;

    /**
     * Private constructor — use the static factory method validate()
     */
    private ResponseValidator(BaseResponse response) {
        this.response = response;
    }

    /**
     * Creates a ResponseValidator for the given response.
     * Start your assertion chain with this method.
     *
     * @param response the BaseResponse to validate
     * @return a ResponseValidator ready for assertion chaining
     */
    public static ResponseValidator validate(BaseResponse response) {
        return new ResponseValidator(response);
    }

    // ----------------------------------------------------------
    // Status Code Assertions
    // ----------------------------------------------------------

    /**
     * Asserts that the HTTP status code matches the expected value.
     *
     * @param expectedStatusCode expected HTTP status code (e.g., 200)
     * @return this validator (for method chaining)
     */
    @Step("Assert status code is {expectedStatusCode}")
    public ResponseValidator assertStatusCode(int expectedStatusCode) {
        int actual = response.getStatusCode();
        logger.info("Asserting status code: expected={}, actual={}", expectedStatusCode, actual);

        if (actual != expectedStatusCode) {
            org.testng.Reporter.getCurrentTestResult().setAttribute("lastResponseStatusCode", actual);
        }

        Assert.assertEquals(actual, expectedStatusCode,
            "Status code mismatch. Expected: " + expectedStatusCode + ", Actual: " + actual +
            "\nResponse Body: " + response.getBodyAsString()
        );
        return this;
    }

    /**
     * Asserts that the HTTP status code is in the 2xx success range.
     *
     * @return this validator (for method chaining)
     */
    @Step("Assert response is successful (2xx)")
    public ResponseValidator assertSuccessful() {
        if (!response.isSuccessful()) {
            org.testng.Reporter.getCurrentTestResult().setAttribute("lastResponseStatusCode", response.getStatusCode());
        }
        
        Assert.assertTrue(response.isSuccessful(),
            "Expected a 2xx status code but got: " + response.getStatusCode() +
            "\nResponse Body: " + response.getBodyAsString()
        );
        return this;
    }

    // ----------------------------------------------------------
    // Response Body Assertions
    // ----------------------------------------------------------

    /**
     * Asserts that the value at the given JsonPath equals the expected string.
     *
     * @param jsonPath      JsonPath expression (e.g., "data.name")
     * @param expectedValue expected string value
     * @return this validator (for method chaining)
     */
    @Step("Assert {jsonPath} equals '{expectedValue}'")
    public ResponseValidator assertFieldEquals(String jsonPath, String expectedValue) {
        String actual = response.getStringValue(jsonPath);
        logger.info("Asserting '{}': expected='{}', actual='{}'", jsonPath, expectedValue, actual);

        Assert.assertEquals(actual, expectedValue,
            "Field mismatch at JsonPath '" + jsonPath + "'. " +
            "Expected: '" + expectedValue + "', Actual: '" + actual + "'"
        );
        return this;
    }

    /**
     * Asserts that the integer value at the given JsonPath equals the expected value.
     *
     * @param jsonPath      JsonPath expression
     * @param expectedValue expected integer value
     * @return this validator (for method chaining)
     */
    @Step("Assert {jsonPath} equals {expectedValue}")
    public ResponseValidator assertFieldEquals(String jsonPath, int expectedValue) {
        int actual = response.getIntValue(jsonPath);
        logger.info("Asserting '{}': expected={}, actual={}", jsonPath, expectedValue, actual);

        Assert.assertEquals(actual, expectedValue,
            "Field mismatch at JsonPath '" + jsonPath + "'. " +
            "Expected: " + expectedValue + ", Actual: " + actual
        );
        return this;
    }

    /**
     * Asserts that the value at the given JsonPath is not null and not empty.
     *
     * @param jsonPath JsonPath expression
     * @return this validator (for method chaining)
     */
    @Step("Assert {jsonPath} is not null or empty")
    public ResponseValidator assertFieldNotNull(String jsonPath) {
        Object actual = response.getJsonPathValue(jsonPath);
        logger.info("Asserting '{}' is not null: actual='{}'", jsonPath, actual);

        Assert.assertNotNull(actual,
            "Expected field at JsonPath '" + jsonPath + "' to be non-null, but it was null."
        );
        return this;
    }

    /**
     * Asserts that the response body contains a specific string.
     *
     * @param text the text that should be present in the response body
     * @return this validator (for method chaining)
     */
    @Step("Assert response body contains '{text}'")
    public ResponseValidator assertBodyContains(String text) {
        String body = response.getBodyAsString();
        Assert.assertTrue(body.contains(text),
            "Expected response body to contain: '" + text + "'\nActual body: " + body
        );
        return this;
    }

    // ----------------------------------------------------------
    // Header Assertions
    // ----------------------------------------------------------

    /**
     * Asserts that a specific response header exists and equals the expected value.
     *
     * @param headerName    the header name
     * @param expectedValue the expected header value
     * @return this validator (for method chaining)
     */
    @Step("Assert header '{headerName}' equals '{expectedValue}'")
    public ResponseValidator assertHeader(String headerName, String expectedValue) {
        String actual = response.getHeader(headerName);
        Assert.assertEquals(actual, expectedValue,
            "Header '" + headerName + "' mismatch. Expected: '" + expectedValue + "', Actual: '" + actual + "'"
        );
        return this;
    }

    /**
     * Asserts that the Content-Type header is application/json.
     *
     * @return this validator (for method chaining)
     */
    @Step("Assert Content-Type is application/json")
    public ResponseValidator assertJsonContentType() {
        String contentType = response.getHeader("Content-Type");
        Assert.assertTrue(contentType != null && contentType.contains("application/json"),
            "Expected Content-Type to contain 'application/json' but got: " + contentType
        );
        return this;
    }

    // ----------------------------------------------------------
    // Performance Assertions
    // ----------------------------------------------------------

    /**
     * Asserts that the response time is within an acceptable limit.
     *
     * @param maxMilliseconds the maximum allowed response time in milliseconds
     * @return this validator (for method chaining)
     */
    @Step("Assert response time is below {maxMilliseconds}ms")
    public ResponseValidator assertResponseTimeBelow(long maxMilliseconds) {
        long actual = response.getResponseTimeMs();
        logger.info("Response time check: actual={}ms, max={}ms", actual, maxMilliseconds);

        Assert.assertTrue(actual <= maxMilliseconds,
            "Response time exceeded limit. Expected: <=" + maxMilliseconds + "ms, Actual: " + actual + "ms"
        );
        return this;
    }

    // ----------------------------------------------------------
    // JSON Schema Validation
    // ----------------------------------------------------------

    /**
     * Validates the response body against a JSON schema file.
     *
     * The schema file must be in: src/test/resources/schemas/
     *
     * @param schemaFileName the schema file name (e.g., "user_response_schema.json")
     * @return this validator (for method chaining)
     */
    @Step("Assert response matches JSON schema '{schemaFileName}'")
    public ResponseValidator assertSchemaMatches(String schemaFileName) {
        String schemaPath = FrameworkConstants.SCHEMAS_DIR + schemaFileName;
        logger.info("Validating response against JSON schema: {}", schemaPath);

        try {
            response.getRawResponse().then()
                .assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaPath));
        } catch (AssertionError e) {
            Assert.fail("JSON schema validation failed for schema '" + schemaFileName + "': " + e.getMessage());
        }
        return this;
    }

    // ----------------------------------------------------------
    // Access to Response (if needed for custom assertions)
    // ----------------------------------------------------------

    /**
     * Returns the underlying BaseResponse for additional custom assertions.
     *
     * @return the BaseResponse object
     */
    public BaseResponse getResponse() {
        return response;
    }
}
