# 📘 REST Assured Framework — Developer Guide

> A step-by-step guide to onboard any QA engineer and show how to use every feature of the framework from scratch.
> **Note:** This framework runs tests **sequentially** to ensure test data stability and avoid database conflicts.

---

## Table of Contents

1. [Project Setup & Running Tests](#1-project-setup--running-tests)
2. [Framework Architecture at a Glance](#2-framework-architecture-at-a-glance)
3. [Step-by-Step Demo: Rahul Shetty Maps API](#3-step-by-step-demo-rahul-shetty-maps-api)
   - [Step 1 — Create the JSON Payload](#step-1--create-the-json-payload)
   - [Step 2 — Create the POJO (Response)](#step-2--create-the-pojo)
   - [Step 3 — Create the Request Class](#step-3--create-the-request-class)
   - [Step 4 — Create the Test Class (CRUD)](#step-4--create-the-test-class-crud)
4. [Payload Customisation with PayloadManager](#4-payload-customisation)
5. [Response Validation with ResponseValidator](#5-response-validation)
6. [JSON Schema Validation](#6-json-schema-validation)
7. [Database Validation](#7-database-validation)
8. [Data-Driven Tests with @DataProvider](#8-data-driven-tests)
9. [Allure Reporting Annotations](#9-allure-reporting-annotations)
10. [Configuration Override for CI/CD](#10-configuration-override-for-cicd)

---

## 1. Project Setup & Running Tests

### Prerequisites

| Tool | Version | Where |
|---|---|---|
| JDK | **21** | `C:\Users\sarve\.jdks\corretto-21.0.11` |
| Maven | 3.8+ | installed globally |
| Allure CLI | 2.27+ | for viewing reports |

### Set JDK before running (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Users\sarve\.jdks\corretto-21.0.11"
$env:PATH      = "$env:JAVA_HOME\bin;$env:PATH"
```

### Common Maven Commands

```bash
# Run the full suite
mvn clean test

# Run a specific test class
mvn test -Dtest=MapsApiTest

# Generate and open Allure report
mvn allure:serve
```

---

## 2. Framework Architecture at a Glance

```
Your Test (MapsApiTest)
    │
    │  extends
    ▼
BaseTest                          ← Fetches OAuth2 token (if configured), sets Trace ID
    │
    │  creates
    ▼
PlaceRequest extends BaseRequest  ← Injects base URI + headers + trace ID
    │
    │  uses
    ▼
RequestBuilder                    ← Assembles & fires the HTTP call + Auto 401 Retries
    │
    │  returns
    ▼
BaseResponse                      ← Wraps RestAssured Response
    │
    │  validated by
    ▼
ResponseValidator                 ← Fluent assertions (status, fields, schema, time)
```

**What happens automatically (zero code needed in tests):**
- **Token Fetching:** `BaseTest` automatically caches tokens per identity.
- **Trace ID Logging:** Every test gets a unique Trace ID injected into Logs and HTTP headers.
- **Auto-Retry:** 429/5xx status codes are retried automatically. 401 token expiry forces an automatic refresh.
- **Skipping Unconfigured DBs:** If you don't configure Oracle/MSSQL, the framework safely throws `SkipException` instead of crashing.

---

## 3. Step-by-Step Demo: Rahul Shetty Maps API

We will use the **Rahul Shetty Maps API** to demonstrate a full end-to-end framework flow.
Base URL: `https://rahulshettyacademy.com`

---

### Step 1 — Create the JSON Payload

Location: `src/test/resources/payloads/add_place.json`

```json
{
  "location": {
    "lat": -38.383494,
    "lng": 33.427362
  },
  "accuracy": 50,
  "name": "Frontline house",
  "phone_number": "(+91) 983 893 3937",
  "address": "29, side layout, cohen 09",
  "types": [
    "shoe park",
    "shop"
  ],
  "website": "http://google.com",
  "language": "French-IN"
}
```

> **Rule:** This is your template. You don't need to create a new file just to change the `name` or `address`. You will override those dynamically in the test.

---

### Step 2 — Create the POJOs (Request & Response)

Instead of sending raw JSON strings, it is highly recommended to use **Request POJOs** for complex bodies. RestAssured automatically serializes POJOs to JSON.

Location: `src/main/java/com/framework/pojo/AddPlaceRequest.java`

```java
package com.framework.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AddPlaceRequest {
    private Location location;
    private int accuracy;
    private String name;
    private String phone_number;
    private String address;
    private List<String> types;
    private String website;
    private String language;

    // Getters, Setters, and Builder pattern omitted for brevity
    
    public static class Location {
        private double lat;
        private double lng;
        // Getters and Setters
    }
}
```

Location: `src/main/java/com/framework/pojo/AddPlaceResponse.java`

```java
package com.framework.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddPlaceResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("place_id")
    private String placeId;

    public String getStatus() { return status; }
    public String getPlaceId() { return placeId; }
}
```

---

### Step 3 — Create the Request Class

Location: `src/main/java/com/framework/request/PlaceRequest.java`

```java
package com.framework.request;

import com.framework.response.BaseResponse;
import io.qameta.allure.Step;

/**
 * PlaceRequest — Page Object for Rahul Shetty Maps API
 */
public class PlaceRequest extends BaseRequest {

    private static final String ADD_PLACE    = "/maps/api/place/add/json";
    private static final String GET_PLACE    = "/maps/api/place/get/json";
    private static final String UPDATE_PLACE = "/maps/api/place/update/json";
    private static final String DELETE_PLACE = "/maps/api/place/delete/json";

    public PlaceRequest() {
        super();
    }

    @Step("POST Add Place")
    public BaseResponse addPlace(Object payload) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(ADD_PLACE)
                .withQueryParam("key", "qaclick123")
                .withPayload(payload)  // Accepts String or POJO!
                .post();
    }

    @Step("GET Place by ID")
    public BaseResponse getPlace(String placeId) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(GET_PLACE)
                .withQueryParam("key", "qaclick123")
                .withQueryParam("place_id", placeId)
                .get();
    }

    @Step("PUT Update Place")
    public BaseResponse updatePlace(String payload) {
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(UPDATE_PLACE)
                .withQueryParam("key", "qaclick123")
                .withPayload(payload)
                .put();
    }

    @Step("DELETE Place")
    public BaseResponse deletePlace(String placeId) {
        // Rahul Shetty DELETE payload requires { "place_id": "xxx" }
        String payload = "{ \"place_id\": \"" + placeId + "\" }";
        
        return RequestBuilder.create()
                .withSpec(getRequestSpec())
                .withEndpoint(DELETE_PLACE)
                .withQueryParam("key", "qaclick123")
                .withPayload(payload)
                .delete();
    }
}
```

---

### Step 4 — Create the Test Class (CRUD)

Location: `src/test/java/com/framework/tests/api/MapsApiTest.java`

This test class demonstrates End-to-End state passing: Create → Verify → Update → Delete.

```java
package com.framework.tests.api;

import com.framework.pojo.AddPlaceResponse;
import com.framework.request.PlaceRequest;
import com.framework.request.PayloadManager;
import com.framework.response.BaseResponse;
import com.framework.response.ResponseValidator;
import com.framework.tests.base.BaseTest;
import io.qameta.allure.*;
import org.testng.annotations.*;

@Epic("Maps Provider")
@Feature("Place CRUD Operations")
public class MapsApiTest extends BaseTest {

    private PlaceRequest placeRequest;
    private String generatedPlaceId;

    @BeforeMethod(alwaysRun = true)
    public void setUpTest() {
        placeRequest = new PlaceRequest();
    }

    @Test(description = "1. Add a new place", priority = 1)
    @Story("Add Place")
    public void testAddPlace() {
        // 1. Create payload using Request POJO (RestAssured automatically serializes this to JSON)
        AddPlaceRequest.Location loc = new AddPlaceRequest.Location();
        loc.setLat(-38.383494);
        loc.setLng(33.427362);

        AddPlaceRequest payload = new AddPlaceRequest();
        payload.setLocation(loc);
        payload.setAccuracy(50);
        payload.setName("Rahul Shetty Academy");
        payload.setPhone_number("(+91) 983 893 3937");
        payload.setAddress("29, side layout, cohen 09");
        payload.setTypes(java.util.Arrays.asList("shoe park", "shop"));
        payload.setWebsite("http://google.com");
        payload.setLanguage("English");

        // 2. Make API Call
        BaseResponse response = placeRequest.addPlace(payload);

        // 3. Fluent Validation
        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("status", "OK")
                .assertFieldEquals("scope", "APP")
                .assertFieldNotNull("place_id");

        // 4. Save Place ID for downstream tests (Deserializing to POJO)
        AddPlaceResponse pojoResponse = response.as(AddPlaceResponse.class);
        this.generatedPlaceId = pojoResponse.getPlaceId();
    }

    @Test(description = "2. Get the created place", priority = 2, dependsOnMethods = "testAddPlace")
    @Story("Get Place")
    public void testGetPlace() {
        BaseResponse response = placeRequest.getPlace(generatedPlaceId);

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("name", "Rahul Shetty Academy")
                .assertFieldEquals("language", "English");
    }

    @Test(description = "3. Update the place address", priority = 3, dependsOnMethods = "testAddPlace")
    @Story("Update Place")
    public void testUpdatePlace() {
        String updatePayload = "{\n" +
                "\"place_id\":\"" + generatedPlaceId + "\",\n" +
                "\"address\":\"70 Summer walk, USA\",\n" +
                "\"key\":\"qaclick123\"\n" +
                "}";

        BaseResponse response = placeRequest.updatePlace(updatePayload);

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("msg", "Address successfully updated");

        // Verify update persisted
        BaseResponse getResponse = placeRequest.getPlace(generatedPlaceId);
        ResponseValidator.validate(getResponse)
                .assertFieldEquals("address", "70 Summer walk, USA");
    }

    @Test(description = "4. Delete the place", priority = 4, dependsOnMethods = "testAddPlace")
    @Story("Delete Place")
    public void testDeletePlace() {
        BaseResponse response = placeRequest.deletePlace(generatedPlaceId);

        ResponseValidator.validate(response)
                .assertStatusCode(200)
                .assertFieldEquals("status", "OK");

        // Verify it was actually deleted (API returns 404)
        BaseResponse getResponse = placeRequest.getPlace(generatedPlaceId);
        ResponseValidator.validate(getResponse)
                .assertStatusCode(404)
                .assertFieldEquals("msg", "Get operation failed, looks like place_id  doesn't exists");
    }
}
```

---

## 4. Payload Customisation

`PayloadManager` allows you to treat JSON files as templates.

### Load and change fields
```java
String payload = PayloadManager.load("add_place.json")
        .customize(body -> {
            body.put("name", "New Name");
            body.put("accuracy", 100);
        })
        .getPayload();
```

### Change a nested object field
```java
String payload = PayloadManager.load("add_place.json")
        .customize(body -> {
            ObjectNode location = (ObjectNode) body.get("location");
            location.put("lat", 40.7128);
            location.put("lng", -74.0060);
        })
        .getPayload();
```

---

## 5. Response Validation

All assertions are fluent and automatically write to Allure and Log4j2.

```java
ResponseValidator.validate(response)
        .assertStatusCode(200)
        .assertSuccessful()
        .assertFieldEquals("status", "OK")
        .assertFieldNotNull("place_id")
        .assertBodyContains("Address successfully updated")
        .assertHeader("Server", "Apache")
        .assertJsonContentType()
        .assertResponseTimeBelow(2000);
```

---

## 6. JSON Schema Validation

1. Save the schema file in `src/test/resources/schemas/maps_schema.json`
2. Assert in test:

```java
ResponseValidator.validate(response)
        .assertSchemaMatches("maps_schema.json");
```

---

## 7. Database Validation

If you need to connect to MSSQL or Oracle to verify data persistence, use the built-in executors in `BaseTest`. If the DB isn't configured in `config.properties`, the test will cleanly be marked as **SKIPPED**.

```java
// Check existence
boolean exists = getMssqlExecutor().exists(
    "SELECT 1 FROM dbo.Places WHERE place_id = ?", generatedPlaceId
);

// Fetch a single column
String address = getOracleExecutor().fetchScalar(
    "SELECT ADDRESS FROM PLACES WHERE PLACE_ID = ?", String.class, generatedPlaceId
);
```

---

## 8. Data-Driven Tests

Test multiple datasets easily with TestNG DataProviders.

```java
@DataProvider(name = "placeData")
public Object[][] placeData() {
    return new Object[][] {
        { "House 1", "English" },
        { "House 2", "Spanish" }
    };
}

@Test(dataProvider = "placeData")
public void testAddMultiplePlaces(String name, String language) {
    String payload = PayloadManager.load("add_place.json")
        .customize(b -> { b.put("name", name); b.put("language", language); })
        .getPayload();
        
    ResponseValidator.validate(placeRequest.addPlace(payload)).assertStatusCode(200);
}
```

---

## 9. Allure Reporting Annotations

Use these to enrich your test reports:

```java
@Epic("Maps Module")               // Highest level group
@Feature("Add Place Feature")      // Sub-group
@Story("Add Place Validations")    // specific flow
@Severity(SeverityLevel.BLOCKER)   // BLOCKER, CRITICAL, NORMAL, MINOR
@Description("Verifies that placing a map location correctly stores latitude")
```

---

## 10. Configuration Override for CI/CD

Jenkins can easily override which environment the framework runs against using Maven `-D` parameters:

```bash
# Run against Staging
mvn clean test -Dbase.url=https://rahulshettyacademy.com

# Change OAuth Grant Type
mvn clean test -Dauth.grant.type=client_credentials

# Target specific tests
mvn clean test -Dtest=MapsApiTest
```
