package com.framework.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UserResponse
 * ============
 * POJO representing the response body for a user API endpoint.
 *
 * This is a SAMPLE POJO — replace with your actual API response fields.
 *
 * EXAMPLE JSON RESPONSE:
 * ----------------------
 * {
 *   "id": "usr-001",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "email": "john.doe@example.com",
 *   "age": 30,
 *   "role": "ADMIN",
 *   "active": true,
 *   "createdAt": "2024-01-15T10:30:00Z"
 * }
 *
 * PATTERN:
 * --------
 * Each API endpoint that returns a response body should have its own
 * response POJO in this package. Name them clearly:
 *   - UserResponse        → GET /users/{id}
 *   - UserListResponse    → GET /users (wraps a list)
 *   - OrderResponse       → GET /orders/{id}
 *
 * HOW TO USE:
 * -----------
 *   // Deserialize from BaseResponse
 *   UserResponse user = response.as(UserResponse.class);
 *   System.out.println(user.getEmail());
 *
 *   // Or via ResponseValidator
 *   ResponseValidator.validate(response)
 *       .assertFieldEquals("firstName", "John");
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    @JsonProperty("id")
    private String id;

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

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("createdAt")
    private String createdAt;

    // ----------------------------------------------------------
    // Default constructor (required by Jackson)
    // ----------------------------------------------------------
    public UserResponse() {}

    // ----------------------------------------------------------
    // Getters
    // ----------------------------------------------------------

    public String  getId()        { return id; }
    public String  getFirstName() { return firstName; }
    public String  getLastName()  { return lastName; }
    public String  getEmail()     { return email; }
    public int     getAge()       { return age; }
    public String  getRole()      { return role; }
    public boolean isActive()     { return active; }
    public String  getCreatedAt() { return createdAt; }

    // ----------------------------------------------------------
    // Setters
    // ----------------------------------------------------------

    public void setId(String id)               { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName)   { this.lastName = lastName; }
    public void setEmail(String email)         { this.email = email; }
    public void setAge(int age)                { this.age = age; }
    public void setRole(String role)           { this.role = role; }
    public void setActive(boolean active)      { this.active = active; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "UserResponse{" +
               "id='" + id + '\'' +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", email='" + email + '\'' +
               ", age=" + age +
               ", role='" + role + '\'' +
               ", active=" + active +
               ", createdAt='" + createdAt + '\'' +
               '}';
    }
}
