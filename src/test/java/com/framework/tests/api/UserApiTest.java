package com.framework.tests.api;

import com.framework.pojo.UserResponse;
import com.framework.request.PayloadManager;
import com.framework.request.UserRequest;
import com.framework.response.BaseResponse;
import com.framework.response.ResponseValidator;
import com.framework.tests.base.BaseTest;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * UserApiTest
 * ===========
 * Demonstrates how to write tests using the REST Assured framework.
 *
 * WHAT THIS SHOWS:
 * ----------------
 * 1. How to extend BaseTest (handles OAuth2 token setup automatically)
 * 2. How to use UserRequest (POM for APIs)
 * 3. How to use PayloadManager (JSON file + customization)
 * 4. How to use ResponseValidator (fluent assertions)
 * 5. How to do DB validation after an API call
 * 6. How to use @DataProvider for data-driven testing
 * 7. How to use Allure annotations for rich reporting
 *
 * PARALLEL EXECUTION:
 * -------------------
 * These tests run in parallel as configured in testng.xml.
 * Each test method runs on its own thread with its own token (ThreadLocal).
 *
 * RETRY:
 * ------
 * RetryAnalyzer is applied globally via RetryListener in testng.xml.
 * Tests are automatically retried on 429/500/502/503/504 errors.
 */
@Epic("User Management API")
@Feature("User CRUD Operations")
public class UserApiTest extends BaseTest {

    // The request object — created fresh in @BeforeMethod
    // Each thread gets its own UserRequest instance (not shared)
    private UserRequest userRequest;

    // ----------------------------------------------------------
    // Setup for this test class
    // ----------------------------------------------------------

    /**
     * Creates a new UserRequest for each test method.
     * Called after BaseTest.setUp() (parent @BeforeMethod runs first).
     * At this point, the OAuth2 token is already in ThreadLocal.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUpUserTest() {
        userRequest = new UserRequest();
    }

    // ----------------------------------------------------------
    // GET Tests
    // ----------------------------------------------------------

    @Test(description = "Verify GET all users returns 200 and a non-empty list")
    @Story("Get All Users")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sends GET /users and verifies status code is 200 and response is non-null.")
    public void testGetAllUsers() {
        // Step 1: Call the API
        BaseResponse response = userRequest.getAllUsers();

        // Step 2: Validate the response using the fluent validator
        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertSuccessful()
                .assertJsonContentType()
                .assertResponseTimeBelow(3000);
    }

    @Test(description = "Verify GET /users/{id} returns correct user details")
    @Story("Get User By ID")
    @Severity(SeverityLevel.CRITICAL)
    public void testGetUserById() {
        String userId = "usr-001";

        // Step 1: Call the API
        BaseResponse response = userRequest.getUser(userId);

        // Step 2: Validate response
        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("id", userId)
                .assertFieldNotNull("email")
                .assertFieldNotNull("firstName");

        // Step 3: Deserialize to POJO for stronger type-safe assertions
        UserResponse user = response.as(UserResponse.class);
        assert user.getId().equals(userId) : "User ID mismatch in POJO";

        // Step 4: DB Validation — verify the same user exists in MSSQL
        Map<String, Object> dbUser = getMssqlExecutor().fetchOne(
            "SELECT id, email, first_name FROM dbo.Users WHERE user_id = ?", userId
        );
        assert dbUser != null : "User not found in MSSQL database!";
        assert dbUser.get("email").equals(user.getEmail()) :
            "Email mismatch: API returned " + user.getEmail() + " but DB has " + dbUser.get("email");
    }

    @Test(description = "Verify GET /users/{id} returns 404 for non-existent user")
    @Story("Get User By ID - Negative")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUserByIdNotFound() {
        BaseResponse response = userRequest.getUser("non-existent-id-999");

        ResponseValidator.validate(response)
                .assertStatusCode(404);
    }

    // ----------------------------------------------------------
    // POST Tests
    // ----------------------------------------------------------

    @Test(description = "Verify POST /users creates a user and returns 201")
    @Story("Create User")
    @Severity(SeverityLevel.BLOCKER)
    public void testCreateUser() {
        // Step 1: Load JSON payload and customize it for this test
        // The template file lives at: src/test/resources/payloads/create_user.json
        String payload = PayloadManager.load("create_user.json")
                .customize(body -> {
                    body.put("email", "john.doe." + System.currentTimeMillis() + "@example.com");
                    body.put("firstName", "John");
                    body.put("lastName", "Doe");
                    body.put("age", 28);
                    body.put("role", "USER");
                })
                .getPayload();

        // Step 2: Call the API
        BaseResponse response = userRequest.createUser(payload);

        // Step 3: Validate response
        ResponseValidator.validate(response)
                .assertStatusCode(201)
                .assertFieldNotNull("id")
                .assertFieldEquals("firstName", "John")
                .assertJsonContentType();

        // Step 4: Extract the created user's ID from the response
        String createdUserId = response.getStringValue("id");

        // Step 5: DB Validation — confirm record was created in Oracle
        boolean existsInOracle = getOracleExecutor().exists(
            "SELECT 1 FROM USERS WHERE USER_ID = ?", createdUserId
        );
        assert existsInOracle : "Created user " + createdUserId + " not found in Oracle DB!";
    }

    // ----------------------------------------------------------
    // PUT / PATCH Tests
    // ----------------------------------------------------------

    @Test(description = "Verify PUT /users/{id} updates user and returns 200")
    @Story("Update User")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateUser() {
        String userId = "usr-001";

        // Build the update payload
        String payload = PayloadManager.load("update_user.json")
                .customize(body -> {
                    body.put("firstName", "UpdatedFirstName");
                    body.put("age", 35);
                })
                .getPayload();

        BaseResponse response = userRequest.updateUser(userId, payload);

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("id", userId)
                .assertFieldEquals("firstName", "UpdatedFirstName");
    }

    // ----------------------------------------------------------
    // DELETE Tests
    // ----------------------------------------------------------

    @Test(description = "Verify DELETE /users/{id} deletes the user and returns 204")
    @Story("Delete User")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteUser() {
        String userId = "usr-to-delete";

        BaseResponse response = userRequest.deleteUser(userId);

        ResponseValidator.validate(response)
                .assertStatusCode(204);

        // DB Validation — confirm the record is removed from MSSQL
        boolean stillExists = getMssqlExecutor().exists(
            "SELECT 1 FROM dbo.Users WHERE user_id = ? AND is_deleted = 0", userId
        );
        assert !stillExists : "Deleted user " + userId + " still appears active in MSSQL!";
    }

    // ----------------------------------------------------------
    // Data-Driven Tests using @DataProvider
    // ----------------------------------------------------------

    /**
     * Provides test data for data-driven testing.
     * Each Object[] represents one test case: { userId, expectedEmail }
     */
    @DataProvider(name = "userIdProvider", parallel = true)
    public Object[][] userIdProvider() {
        return new Object[][] {
            { "usr-001", "user1@example.com" },
            { "usr-002", "user2@example.com" },
            { "usr-003", "user3@example.com" },
        };
    }

    @Test(
        dataProvider  = "userIdProvider",
        description   = "Verify user details for multiple users (data-driven)",
        threadPoolSize = 3
    )
    @Story("Get User By ID - Data Driven")
    @Severity(SeverityLevel.NORMAL)
    public void testGetMultipleUsers(String userId, String expectedEmail) {
        BaseResponse response = userRequest.getUser(userId);

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("id", userId)
                .assertFieldEquals("email", expectedEmail);
    }

    // ----------------------------------------------------------
    // JSON Schema Validation Test
    // ----------------------------------------------------------

    @Test(description = "Verify GET /users/{id} response matches JSON schema")
    @Story("Schema Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUserSchemaValidation() {
        BaseResponse response = userRequest.getUser("usr-001");

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                // Schema file location: src/test/resources/schemas/user_response_schema.json
                .assertSchemaMatches("user_response_schema.json");
    }
}
