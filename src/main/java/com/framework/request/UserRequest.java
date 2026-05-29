package com.framework.request;

import com.framework.response.BaseResponse;
import io.qameta.allure.Step;

/**
 * UserRequest
 * ===========
 * Concrete request class for the /users API endpoint.
 *
 * DESIGN PATTERN: Page Object Model (POM) adapted for APIs
 * --------------------------------------------------------
 * This class is the API equivalent of a Page Object.
 * It encapsulates every HTTP operation for the /users endpoint so that:
 *   - Tests call readable, named methods: getUser("123"), createUser(payload)
 *   - Tests have zero knowledge of HTTP verbs, endpoint paths, or RestAssured
 *   - Any endpoint change is fixed in ONE place, not across every test
 *
 * ALL LOGGING IS HANDLED INTERNALLY:
 * ------------------------------------
 * Request logging (URL, headers, body, status, timing) is done automatically
 * inside RequestBuilder and BaseRequest. No logger is needed here.
 *
 * HOW TO USE IN TESTS:
 * --------------------
 *   UserRequest userRequest = new UserRequest();
 *
 *   // GET /users/123
 *   BaseResponse response = userRequest.getUser("123");
 *
 *   // POST /users with a customised payload
 *   String payload = PayloadManager.load("create_user.json")
 *       .customize(body -> body.put("email", "test@example.com"))
 *       .getPayload();
 *   BaseResponse response = userRequest.createUser(payload);
 *
 * ADDING NEW ENDPOINTS:
 * ---------------------
 * Follow the same pattern — one method per operation.
 *
 *   public BaseResponse searchUsers(String query) {
 *       return RequestBuilder.create()
 *           .withSpec(getRequestSpec())
 *           .withEndpoint("/users/search")
 *           .withQueryParam("q", query)
 *           .get();
 *   }
 */
public class UserRequest extends BaseRequest {

    private static final String USERS_ENDPOINT      = "/users";
    private static final String USER_BY_ID_ENDPOINT = "/users/{id}";

    /**
     * Constructs a UserRequest.
     * Calls super() → BaseRequest sets up the full RestAssured spec:
     *   - Bearer token from ThreadLocal (set by BaseTest.setUp)
     *   - Base URI from config.properties
     *   - Content-Type: application/json
     *   - Allure request/response capture filter
     *   - Connect + read timeouts
     */
    public UserRequest() {
        super();
    }

    // ----------------------------------------------------------
    // GET
    // ----------------------------------------------------------

    /**
     * Retrieves all users.
     *
     * HTTP: GET /users
     *
     * @return BaseResponse — check status with ResponseValidator
     */
    @Step("GET all users")
    public BaseResponse getAllUsers() {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USERS_ENDPOINT)
                .get();
    }

    /**
     * Retrieves a single user by their ID.
     *
     * HTTP: GET /users/{id}
     *
     * @param userId the user ID to fetch (e.g., "usr-001")
     * @return BaseResponse — deserialise with response.as(UserResponse.class)
     */
    @Step("GET user by id={userId}")
    public BaseResponse getUser(String userId) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USER_BY_ID_ENDPOINT)
                .withPathParam("id", userId)
                .get();
    }

    /**
     * Retrieves users filtered by status and role.
     *
     * HTTP: GET /users?status={status}&role={role}
     *
     * @param status filter value for status field (e.g., "active")
     * @param role   filter value for role field (e.g., "ADMIN")
     * @return BaseResponse containing filtered user list
     */
    @Step("GET users filtered — status={status}, role={role}")
    public BaseResponse getUsersByFilter(String status, String role) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USERS_ENDPOINT)
                .withQueryParam("status", status)
                .withQueryParam("role", role)
                .get();
    }

    // ----------------------------------------------------------
    // POST
    // ----------------------------------------------------------

    /**
     * Creates a new user.
     *
     * HTTP: POST /users
     *
     * @param payload JSON request body — build with PayloadManager
     * @return BaseResponse — typically 201 Created with new user in body
     */
    @Step("POST create user")
    public BaseResponse createUser(String payload) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USERS_ENDPOINT)
                .withPayload(payload)
                .post();
    }

    // ----------------------------------------------------------
    // PUT
    // ----------------------------------------------------------

    /**
     * Fully updates an existing user.
     *
     * HTTP: PUT /users/{id}
     *
     * @param userId  the ID of the user to update
     * @param payload JSON body with all updated fields
     * @return BaseResponse — typically 200 OK with updated user
     */
    @Step("PUT update user id={userId}")
    public BaseResponse updateUser(String userId, String payload) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USER_BY_ID_ENDPOINT)
                .withPathParam("id", userId)
                .withPayload(payload)
                .put();
    }

    // ----------------------------------------------------------
    // PATCH
    // ----------------------------------------------------------

    /**
     * Partially updates an existing user.
     *
     * HTTP: PATCH /users/{id}
     *
     * @param userId  the ID of the user to patch
     * @param payload JSON body containing only the fields to change
     * @return BaseResponse — typically 200 OK with patched user
     */
    @Step("PATCH partial update user id={userId}")
    public BaseResponse patchUser(String userId, String payload) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USER_BY_ID_ENDPOINT)
                .withPathParam("id", userId)
                .withPayload(payload)
                .patch();
    }

    // ----------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------

    /**
     * Deletes a user by ID.
     *
     * HTTP: DELETE /users/{id}
     *
     * @param userId the ID of the user to delete
     * @return BaseResponse — typically 204 No Content
     */
    @Step("DELETE user id={userId}")
    public BaseResponse deleteUser(String userId) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(USER_BY_ID_ENDPOINT)
                .withPathParam("id", userId)
                .delete();
    }
}
