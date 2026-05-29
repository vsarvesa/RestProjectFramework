package com.framework.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CreateUserRequest
 * =================
 * POJO representing the request body for creating a new user.
 *
 * This is a SAMPLE POJO — replace with your actual API request fields.
 *
 * PATTERN:
 * --------
 * Each API endpoint that accepts a request body should have its own
 * request POJO in this package. Name them clearly:
 *   - CreateUserRequest    → POST /users
 *   - UpdateUserRequest    → PUT /users/{id}
 *   - CreateOrderRequest   → POST /orders
 *
 * HOW TO USE:
 * -----------
 *   // Build the request POJO
 *   CreateUserRequest request = new CreateUserRequest.Builder()
 *       .firstName("John")
 *       .lastName("Doe")
 *       .email("john.doe@example.com")
 *       .age(30)
 *       .build();
 *
 *   // Serialize to JSON string
 *   String payload = JsonUtils.toJson(request);
 *
 *   // OR use PayloadManager for file-based payloads (preferred):
 *   String payload = PayloadManager.load("create_user.json")
 *       .customize(body -> body.put("email", "john@example.com"))
 *       .getPayload();
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserRequest {

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("age")
    private int age;

    @JsonProperty("role")
    private String role;

    // ----------------------------------------------------------
    // Default constructor (required by Jackson)
    // ----------------------------------------------------------
    public CreateUserRequest() {}

    // Private constructor — used only by the Builder
    private CreateUserRequest(Builder builder) {
        this.firstName = builder.firstName;
        this.lastName  = builder.lastName;
        this.email     = builder.email;
        this.age       = builder.age;
        this.role      = builder.role;
    }

    // ----------------------------------------------------------
    // Getters
    // ----------------------------------------------------------

    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getEmail()     { return email; }
    public int    getAge()       { return age; }
    public String getRole()      { return role; }

    // ----------------------------------------------------------
    // Setters
    // ----------------------------------------------------------

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName)   { this.lastName = lastName; }
    public void setEmail(String email)         { this.email = email; }
    public void setAge(int age)                { this.age = age; }
    public void setRole(String role)           { this.role = role; }

    // ----------------------------------------------------------
    // Builder — for readable test code
    // ----------------------------------------------------------

    /**
     * Builder for CreateUserRequest.
     *
     * WHY USE A BUILDER?
     * ------------------
     * Instead of a constructor with 5+ parameters (hard to read),
     * the builder makes it obvious which field is which:
     *
     *   new CreateUserRequest("John", "Doe", "john@example.com", 30, "ADMIN")
     *   // vs.
     *   new CreateUserRequest.Builder().firstName("John").email("john@example.com").build()
     */
    public static class Builder {

        private String firstName;
        private String lastName;
        private String email;
        private int    age;
        private String role;

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        /**
         * Creates the immutable CreateUserRequest object.
         *
         * @return configured CreateUserRequest
         */
        public CreateUserRequest build() {
            return new CreateUserRequest(this);
        }
    }

    @Override
    public String toString() {
        return "CreateUserRequest{" +
               "firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", email='" + email + '\'' +
               ", age=" + age +
               ", role='" + role + '\'' +
               '}';
    }
}
