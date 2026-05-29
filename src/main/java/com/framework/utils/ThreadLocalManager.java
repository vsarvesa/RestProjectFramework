package com.framework.utils;

import com.framework.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;

/**
 * ThreadLocalManager
 * ==================
 * Stores per-thread data so parallel tests do not interfere with each other.
 *
 * WHY THREAD LOCAL?
 * -----------------
 * When 5 threads run tests simultaneously:
 * - Thread 1's token must NOT leak into Thread 2's request.
 * - Thread 1's test name must NOT appear in Thread 2's logs.
 *
 * ThreadLocal gives each thread its own private copy of a variable.
 *
 * HOW TO USE:
 * -----------
 *   // Store a value (called once per test method, usually in @BeforeMethod)
 *   ThreadLocalManager.setAccessToken("Bearer abc123");
 *
 *   // Retrieve a value (called from any class in the same thread)
 *   String token = ThreadLocalManager.getAccessToken();
 *
 *   // IMPORTANT: Always clean up after each test to prevent memory leaks
 *   ThreadLocalManager.clear();  // call this in @AfterMethod
 */
public final class ThreadLocalManager {

    private static final Logger logger = FrameworkLogger.getLogger(ThreadLocalManager.class);

    // Private constructor — utility class
    private ThreadLocalManager() {
        throw new UnsupportedOperationException("ThreadLocalManager is a utility class.");
    }

    // ----------------------------------------------------------
    // Thread-Local Storage Slots
    // ----------------------------------------------------------

    /** Stores the OAuth 2.0 access token for the current thread */
    private static final ThreadLocal<String> ACCESS_TOKEN = new ThreadLocal<>();

    /** Stores the current test method name (useful for logging & Allure) */
    private static final ThreadLocal<String> TEST_NAME = new ThreadLocal<>();

    /** Stores the unique trace ID for the current test run (for log correlation) */
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /** Stores an arbitrary key-value context for the current thread */
    private static final ThreadLocal<java.util.Map<String, String>> CONTEXT =
        ThreadLocal.withInitial(java.util.HashMap::new);

    // ----------------------------------------------------------
    // Access Token
    // ----------------------------------------------------------

    /**
     * Stores the OAuth2 access token for the current thread.
     *
     * @param token the full Bearer token string (e.g., "Bearer eyJ...")
     */
    public static void setAccessToken(String token) {
        ACCESS_TOKEN.set(token);
        logger.debug("[Thread: {}] Access token stored in ThreadLocal.", Thread.currentThread().getName());
    }

    /**
     * Returns the OAuth2 access token for the current thread.
     *
     * @return the Bearer token or null if not set
     */
    public static String getAccessToken() {
        return ACCESS_TOKEN.get();
    }

    // ----------------------------------------------------------
    // Test Name
    // ----------------------------------------------------------

    /**
     * Stores the name of the currently running test.
     *
     * @param testName test method name (e.g., "testCreateUser")
     */
    public static void setTestName(String testName) {
        TEST_NAME.set(testName);
    }

    /**
     * Returns the currently running test name for this thread.
     *
     * @return test name or null if not set
     */
    public static String getTestName() {
        return TEST_NAME.get();
    }

    // ----------------------------------------------------------
    // Trace ID
    // ----------------------------------------------------------

    /**
     * Stores the trace ID for the current test.
     * Also pushes it to Log4j2 ThreadContext (MDC) so it appears in all log lines.
     */
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
        org.apache.logging.log4j.ThreadContext.put("traceId", traceId);
    }

    /**
     * Returns the trace ID for the current thread.
     */
    public static String getTraceId() {
        return TRACE_ID.get();
    }

    // ----------------------------------------------------------
    // Generic Context Map
    // ----------------------------------------------------------

    /**
     * Stores an arbitrary key-value pair in the thread's context map.
     * Useful for passing data between test steps within the same thread.
     *
     * @param key   context key (e.g., "userId", "orderId")
     * @param value context value
     */
    public static void setContext(String key, String value) {
        CONTEXT.get().put(key, value);
    }

    /**
     * Retrieves a value from the thread's context map.
     *
     * @param key context key
     * @return the stored value or null if not set
     */
    public static String getContext(String key) {
        return CONTEXT.get().get(key);
    }

    // ----------------------------------------------------------
    // Cleanup (CRITICAL — must call after each test)
    // ----------------------------------------------------------

    /**
     * Clears ALL ThreadLocal data for the current thread.
     *
     * IMPORTANT: Call this in @AfterMethod in BaseTest.
     * Failure to call this causes:
     *  - Memory leaks (especially in thread pool environments)
     *  - Stale data bleeding into the next test on the same thread
     */
    public static void clear() {
        ACCESS_TOKEN.remove();
        TEST_NAME.remove();
        TRACE_ID.remove();
        CONTEXT.remove();
        org.apache.logging.log4j.ThreadContext.remove("traceId");
        logger.debug("[Thread: {}] ThreadLocal data cleared.", Thread.currentThread().getName());
    }
}
