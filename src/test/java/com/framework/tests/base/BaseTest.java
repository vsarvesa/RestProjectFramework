package com.framework.tests.base;

import com.framework.auth.OAuth2TokenManager;
import com.framework.db.DBConnectionManager;
import com.framework.db.DBQueryExecutor;
import com.framework.db.DBType;
import com.framework.utils.ThreadLocalManager;
import io.qameta.allure.Step;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * BaseTest
 * ========
 * The parent class for ALL test classes in the framework.
 *
 * WHAT EVERY TEST GETS AUTOMATICALLY (just by extending BaseTest):
 * ----------------------------------------------------------------
 * ✅ OAuth2 access token fetched before each test (ThreadLocal, parallel-safe)
 * ✅ ThreadLocal cleared after each test (prevents memory leaks)
 * ✅ getMssqlExecutor() — query MSSQL inside any @Test method
 * ✅ getOracleExecutor() — query Oracle inside any @Test method
 *
 * WHAT YOU DON'T NEED TO DO IN YOUR TEST CLASS:
 * ----------------------------------------------
 * ✗ No logger declarations
 * ✗ No log statements
 * ✗ No token setup code
 * ✗ No ThreadLocal management
 * ✗ No DB pool lifecycle management
 *
 * All of the above is handled internally by the framework listeners
 * (FrameworkLoggingListener, FrameworkSetupListener) registered in testng.xml.
 *
 * HOW TO WRITE A TEST CLASS:
 * --------------------------
 *   public class UserApiTest extends BaseTest {
 *
 *       private UserRequest userRequest;
 *
 *       @BeforeMethod(alwaysRun = true)
 *       public void setUpTest() {
 *           userRequest = new UserRequest(); // token already available via ThreadLocal
 *       }
 *
 *       @Test
 *       public void testGetUser() {
 *           BaseResponse response = userRequest.getUser("usr-001");
 *           ResponseValidator.validate(response).assertStatusCode(200);
 *       }
 *   }
 *
 * NOTE on @BeforeMethod ordering:
 * --------------------------------
 * TestNG guarantees the parent @BeforeMethod (here) runs FIRST.
 * Your subclass @BeforeMethod runs after — so the token is already set
 * when you create request objects like `new UserRequest()`.
 */
public class BaseTest {

    // ----------------------------------------------------------
    // @BeforeMethod — Runs before EACH @Test method automatically
    // ----------------------------------------------------------

    /**
     * Initialises the test context for the current thread.
     *
     * What it does internally (invisible to test writers):
     *   1. Stores the test method name in ThreadLocal (framework logging uses it)
     *   2. Fetches the OAuth2 token for this thread (cached, thread-safe)
     *
     * @param result injected by TestNG — provides the current test method context
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        ThreadLocalManager.setTestName(result.getMethod().getMethodName());
        ThreadLocalManager.setTraceId(java.util.UUID.randomUUID().toString());
        OAuth2TokenManager.getInstance().initializeToken();
    }

    // ----------------------------------------------------------
    // @AfterMethod — Runs after EACH @Test method automatically
    // ----------------------------------------------------------

    /**
     * Cleans up the test context for the current thread.
     * Always runs even if the test fails.
     *
     * What it does internally:
     *   Removes all ThreadLocal values (token, test name, context map)
     *   to prevent memory leaks in the thread pool.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        ThreadLocalManager.clear();
    }

    // ----------------------------------------------------------
    // Protected DB Helpers — available to all test subclasses
    // ----------------------------------------------------------

    /**
     * Returns a ready-to-use executor for the MSSQL (SQL Server) database.
     *
     * Call this inside any @Test method to validate data in MSSQL.
     *
     * Usage examples:
     * <pre>
     *   // Fetch a row and check a field
     *   Map&lt;String, Object&gt; row = getMssqlExecutor()
     *       .fetchOne("SELECT status FROM dbo.Users WHERE user_id = ?", userId);
     *   assertEquals(row.get("status"), "ACTIVE");
     *
     *   // Count rows
     *   int count = getMssqlExecutor()
     *       .fetchScalar("SELECT COUNT(*) FROM dbo.Orders WHERE user_id = ?",
     *                    Integer.class, userId);
     *
     *   // Existence check
     *   boolean exists = getMssqlExecutor()
     *       .exists("SELECT 1 FROM dbo.Users WHERE email = ?", email);
     * </pre>
     *
     * @return DBQueryExecutor backed by the MSSQL connection pool
     */
    @Step("Query MSSQL database")
    protected DBQueryExecutor getMssqlExecutor() {
        return DBConnectionManager.getInstance().getExecutor(DBType.MSSQL);
    }

    /**
     * Returns a ready-to-use executor for the Oracle database.
     *
     * Call this inside any @Test method to validate data in Oracle.
     *
     * Usage examples:
     * <pre>
     *   // Fetch all orders
     *   List&lt;Map&lt;String, Object&gt;&gt; orders = getOracleExecutor()
     *       .fetchAll("SELECT * FROM ORDERS WHERE USER_ID = ?", userId);
     *
     *   // Scalar query
     *   int count = getOracleExecutor()
     *       .fetchScalar("SELECT COUNT(*) FROM ORDERS WHERE STATUS = ?",
     *                    Integer.class, "PENDING");
     * </pre>
     *
     * @return DBQueryExecutor backed by the Oracle connection pool
     */
    @Step("Query Oracle database")
    protected DBQueryExecutor getOracleExecutor() {
        return DBConnectionManager.getInstance().getExecutor(DBType.ORACLE);
    }
}
