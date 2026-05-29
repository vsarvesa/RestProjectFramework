package com.framework.retry;

import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import com.framework.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer
 * =============
 * Automatically retries failed API tests when the failure is due to
 * transient HTTP errors.
 *
 * RETRYABLE STATUS CODES:
 * -----------------------
 *   429 — Too Many Requests  (rate limiting)
 *   500 — Internal Server Error
 *   502 — Bad Gateway
 *   503 — Service Unavailable
 *   504 — Gateway Timeout
 *
 * HOW RETRIES WORK:
 * -----------------
 * 1. TestNG calls retry() after each test failure.
 * 2. If the failed response status code is retryable AND max retries not reached:
 *    → Returns true (retry the test)
 * 3. Between retries, an exponential backoff delay is applied:
 *    Retry 1: wait 1 second
 *    Retry 2: wait 2 seconds
 *    Retry 3: wait 4 seconds
 * 4. If max retries are exhausted → Returns false (mark test as FAILED)
 *
 * HOW TO APPLY:
 * -------------
 * Option A — Per test method:
 *   @Test(retryAnalyzer = RetryAnalyzer.class)
 *   public void testCreateUser() { ... }
 *
 * Option B — Automatically via RetryListener (applied in testng.xml):
 *   <listener class-name="com.framework.retry.RetryListener"/>
 *   This automatically attaches RetryAnalyzer to ALL test methods.
 *
 * THREAD SAFETY:
 * --------------
 * TestNG creates a NEW RetryAnalyzer instance per test method.
 * The retryCount field is instance-scoped → no shared state between threads.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = FrameworkLogger.getLogger(RetryAnalyzer.class);

    /** Current retry attempt for this test method (0-based) */
    private int retryCount = 0;

    /** Maximum retries — read from config.properties */
    private final int maxRetryCount = ConfigManager.getInstance().getRetryMaxCount();

    /** Initial delay between retries in milliseconds */
    private final int initialDelayMs = ConfigManager.getInstance().getRetryInitialDelayMs();

    /**
     * Called by TestNG after each test failure.
     *
     * @param result the result of the failed test
     * @return true → retry the test, false → mark as failed
     */
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount && isRetryableFailure(result)) {
            retryCount++;
            int delayMs = calculateDelay(retryCount);

            logger.warn("[Thread: {}] Test '{}' failed. Retry attempt {}/{} in {}ms...",
                        Thread.currentThread().getName(),
                        result.getName(),
                        retryCount,
                        maxRetryCount,
                        delayMs);

            applyDelay(delayMs);
            return true; // → retry
        }

        if (retryCount > 0) {
            logger.error("[Thread: {}] Test '{}' failed after {} retries. Marking as FAILED.",
                         Thread.currentThread().getName(),
                         result.getName(),
                         retryCount);
        }

        return false; // → do not retry
    }

    // ----------------------------------------------------------
    // Private Helpers
    // ----------------------------------------------------------

    /**
     * Determines whether the test failure is due to a retryable HTTP status code.
     *
     * HOW IT WORKS:
     * The test result's throwable message is checked for the HTTP status code
     * pattern. If the test used ResponseValidator.assertStatusCode() or
     * plain TestNG Assert.assertEquals(), the failure message contains
     * "Status code mismatch" or "expected [200] but found [503]".
     *
     * We also check the ITestResult attributes map — BaseTest stores the last
     * response status code there for this exact purpose.
     *
     * @param result the failed test result
     * @return true if the failure should trigger a retry
     */
    private boolean isRetryableFailure(ITestResult result) {
        // Check the stored status code attribute (set by ResponseValidator)
        Object statusCodeAttr = result.getAttribute("lastResponseStatusCode");
        if (statusCodeAttr instanceof Integer statusCode) {
            return isRetryableStatusCode(statusCode);
        }

        // If no status code context is found, check for network-level exceptions
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            if (throwable instanceof java.net.ConnectException ||
                throwable instanceof java.net.SocketTimeoutException ||
                throwable instanceof java.net.UnknownHostException) {
                logger.debug("Retryable network exception found: {}", throwable.getClass().getSimpleName());
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a specific HTTP status code is in the retryable list.
     *
     * @param statusCode the HTTP status code to check
     * @return true if this code should trigger a retry
     */
    private boolean isRetryableStatusCode(int statusCode) {
        for (int retryableCode : FrameworkConstants.RETRYABLE_STATUS_CODES) {
            if (statusCode == retryableCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates exponential backoff delay.
     *
     * Formula: initialDelay * 2^(retryAttempt - 1)
     * Example with initialDelay=1000ms:
     *   Retry 1 → 1000ms (1s)
     *   Retry 2 → 2000ms (2s)
     *   Retry 3 → 4000ms (4s)
     *
     * @param retryAttempt the current retry attempt (1-based)
     * @return delay in milliseconds
     */
    private int calculateDelay(int retryAttempt) {
        return initialDelayMs * (int) Math.pow(2, retryAttempt - 1);
    }

    /**
     * Pauses the current thread for the specified duration.
     *
     * @param delayMs delay in milliseconds
     */
    private void applyDelay(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Retry delay interrupted for thread: {}", Thread.currentThread().getName());
        }
    }
}
