# REST Assured API Test Framework

A production-grade API test automation framework built with:
- **Java 21** (Amazon Corretto 21)
- **Maven** build tool
- **TestNG 7.9** test runner + parallel execution
- **REST Assured 5.4** HTTP client
- **OAuth 2.0** authentication (client_credentials + password grants)
- **Allure 2.27** reporting
- **HikariCP** connection pools for **MSSQL** and **Oracle** databases
- **Log4j2** structured logging
- **Jackson** JSON serialization/deserialization

---

## Project Structure

```
src/
├── main/java/com/framework/
│   ├── auth/          → OAuth2TokenManager (thread-safe token cache)
│   ├── config/        → ConfigManager (reads config.properties)
│   ├── constants/     → FrameworkConstants (status codes, headers, etc.)
│   ├── db/            → DBConnectionManager, DBQueryExecutor, DBType
│   ├── exceptions/    → FrameworkException hierarchy
│   ├── logging/       → FrameworkLogger (Log4j2 wrapper)
│   ├── pojo/          → Request & Response POJOs
│   ├── request/       → BaseRequest, RequestBuilder, PayloadManager, *Request classes
│   ├── response/      → BaseResponse, ResponseValidator (fluent assertions)
│   ├── retry/         → RetryAnalyzer, RetryListener (429/500/502/503/504)
│   └── utils/         → JsonUtils, ThreadLocalManager
└── main/resources/
    ├── config.properties   → All configuration (URLs, credentials, DB, retry)
    └── log4j2.xml          → Logging configuration

src/
├── test/java/com/framework/tests/
│   ├── base/          → BaseTest (TestNG lifecycle, DB helpers)
│   └── api/           → *ApiTest classes (your actual tests)
└── test/resources/
    ├── allure.properties
    ├── payloads/       → JSON payload template files
    └── schemas/        → JSON Schema files for validation
```

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | **21** (Amazon Corretto 21 recommended) |
| Maven | 3.8+ |
| Allure CLI | 2.27+ (for generating reports) |

---

## Configuration

Edit `src/main/resources/config.properties`:

```properties
# API endpoints
base.url=https://api.your-server.com
auth.url=https://auth.your-server.com/oauth/token

# OAuth 2.0 credentials
client.id=your_client_id
client.secret=your_client_secret

# Grant type: client_credentials OR password
auth.grant.type=client_credentials

# For password grant only
auth.username=your_username
auth.password=your_password

# MSSQL
mssql.url=jdbc:sqlserver://localhost:1433;databaseName=TestDB;encrypt=true;trustServerCertificate=true
mssql.username=sa
mssql.password=YourPassword

# Oracle
oracle.url=jdbc:oracle:thin:@//localhost:1521/ORCLPDB1
oracle.username=system
oracle.password=YourPassword
```

> **Tip:** Any property can be overridden via system property:
> ```
> mvn test -Dbase.url=https://staging.api.com
> ```

---

## Running Tests

### Set JAVA_HOME to JDK 21 first
```powershell
$env:JAVA_HOME = "C:\Users\sarve\.jdks\corretto-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Run all tests (parallel, 5 threads)
```bash
mvn clean test
```

### Run a specific test class
```bash
mvn test -Dtest=UserApiTest
```

### Run with a different environment
```bash
mvn test -Dbase.url=https://staging.api.com -Dauth.url=https://staging-auth.com/token
```

### Generate Allure Report
```bash
mvn allure:serve
```

---

## Creating a New API Test

### 1. Add a JSON payload template
`src/test/resources/payloads/create_order.json`
```json
{
  "productId": "prod-001",
  "quantity": 1,
  "currency": "USD"
}
```

### 2. Create Request/Response POJOs
```java
// src/main/java/com/framework/pojo/CreateOrderRequest.java
// src/main/java/com/framework/pojo/OrderResponse.java
```

### 3. Create a Request class (POM for APIs)
```java
public class OrderRequest extends BaseRequest {

    public BaseResponse createOrder(String payload) {
        return RequestBuilder.create()
            .withSpec(getRequestSpec())
            .withEndpoint("/orders")
            .withPayload(payload)
            .post();
    }

    public BaseResponse getOrder(String orderId) {
        return RequestBuilder.create()
            .withSpec(getRequestSpec())
            .withEndpoint("/orders/{id}")
            .withPathParam("id", orderId)
            .get();
    }
}
```

### 4. Write the Test
```java
@Epic("Order Management")
@Feature("Create Order")
public class OrderApiTest extends BaseTest {

    private OrderRequest orderRequest;

    @BeforeMethod
    public void setup() {
        orderRequest = new OrderRequest();
    }

    @Test
    public void testCreateOrder() {
        // Load and customize payload
        String payload = PayloadManager.load("create_order.json")
            .customize(body -> {
                body.put("productId", "prod-999");
                body.put("quantity", 3);
            })
            .getPayload();

        // Make the API call
        BaseResponse response = orderRequest.createOrder(payload);

        // Validate the response
        ResponseValidator.validate(response)
            .assertStatusCode(201)
            .assertFieldNotNull("orderId")
            .assertResponseTimeBelow(3000);

        // DB Validation — check MSSQL
        String orderId = response.getStringValue("orderId");
        boolean existsInDb = getMssqlExecutor().exists(
            "SELECT 1 FROM dbo.Orders WHERE order_id = ?", orderId
        );
        assert existsInDb : "Order not found in database!";
    }
}
```

---

## DB Validation Reference

```java
// MSSQL — fetch all rows
List<Map<String, Object>> rows = getMssqlExecutor().fetchAll(
    "SELECT * FROM dbo.Users WHERE status = ?", "active"
);

// MSSQL — fetch single row
Map<String, Object> row = getMssqlExecutor().fetchOne(
    "SELECT * FROM dbo.Users WHERE user_id = ?", userId
);

// Oracle — fetch scalar value
int count = getOracleExecutor().fetchScalar(
    "SELECT COUNT(*) FROM ORDERS WHERE STATUS = ?", Integer.class, "PENDING"
);

// Oracle — check existence
boolean exists = getOracleExecutor().exists(
    "SELECT 1 FROM USERS WHERE EMAIL = ?", email
);

// Execute UPDATE / DELETE
int affected = getMssqlExecutor().executeUpdate(
    "UPDATE dbo.Users SET status = ? WHERE user_id = ?", "inactive", userId
);
```

---

## Retry Mechanism

Automatically applied to all tests via `testng.xml` `RetryListener`.

| Status Code | Description | Retried? |
|---|---|---|
| 429 | Too Many Requests | ✅ |
| 500 | Internal Server Error | ✅ |
| 502 | Bad Gateway | ✅ |
| 503 | Service Unavailable | ✅ |
| 504 | Gateway Timeout | ✅ |

Configurable in `config.properties`:
```properties
retry.max.count=3
retry.initial.delay.ms=1000    # Doubles each retry: 1s → 2s → 4s
```

---

## Logs

| Log File | Content |
|---|---|
| `target/logs/framework.log` | All framework logs |
| `target/logs/api-calls.log` | Detailed request/response logs |
| Console | INFO level summary |

---

## Exception Hierarchy

```
FrameworkException (RuntimeException)
├── AuthenticationException  — OAuth2 token failures
├── ApiRequestException      — HTTP call failures (carries status code + body)
└── DatabaseException        — DB failures (carries SQL state + query)
```
