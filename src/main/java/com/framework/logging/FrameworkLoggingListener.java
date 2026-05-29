package com.framework.logging;

import org.apache.logging.log4j.Logger;
import org.testng.*;

/**
 * FrameworkLoggingListener
 * ========================
 * A TestNG listener that captures ALL test lifecycle events and logs them
 * internally inside the framework — completely invisible to test writers.
 *
 * WHY THIS EXISTS:
 * ----------------
 * Without this listener, test lifecycle logging (test started, passed, failed)
 * would have to be written inside BaseTest using explicit logger calls.
 * That leaks framework infrastructure into the test layer.
 *
 * With this listener registered in testng.xml, ALL logging happens here
 * automatically. Test classes and BaseTest contain zero logging code.
 *
 * WHAT IT LOGS:
 * -------------
 * - Suite start / end banners
 * - Test start:  [Thread] → Starting: TestClass.methodName
 * - Test pass:   [Thread] → PASSED ✅  TestClass.methodName (Xms)
 * - Test fail:   [Thread] → FAILED ❌  TestClass.methodName | Reason: ...
 * - Test skip:   [Thread] → SKIPPED ⚠️ TestClass.methodName
 * - Retry:       [Thread] → RETRYING  TestClass.methodName (attempt N)
 *
 * REGISTRATION:
 * -------------
 * Add to testng.xml:
 *   <listener class-name="com.framework.logging.FrameworkLoggingListener"/>
 */
public class FrameworkLoggingListener implements ITestListener, ISuiteListener {

    private static final Logger logger = FrameworkLogger.getLogger(FrameworkLoggingListener.class);

    private static final String SEPARATOR = "═".repeat(60);

    // ----------------------------------------------------------
    // ISuiteListener — Suite level
    // ----------------------------------------------------------

    @Override
    public void onStart(ISuite suite) {
        logger.info(SEPARATOR);
        logger.info("  REST ASSURED FRAMEWORK — SUITE STARTED");
        logger.info("  Suite  : {}", suite.getName());
        logger.info("  Threads: parallel={}", suite.getParallel());
        logger.info(SEPARATOR);
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info(SEPARATOR);
        logger.info("  REST ASSURED FRAMEWORK — SUITE FINISHED");
        logger.info("  Suite: {}", suite.getName());
        logger.info(SEPARATOR);
    }

    // ----------------------------------------------------------
    // ITestListener — Test level
    // ----------------------------------------------------------

    @Override
    public void onTestStart(ITestResult result) {
        logger.info("▶ STARTING  [{}/{}] {}",
                    Thread.currentThread().getName(),
                    result.getTestClass().getName(),
                    result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("✅ PASSED    [{}/{}] {} ({}ms)",
                    Thread.currentThread().getName(),
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName(),
                    getDurationMs(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String reason = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Unknown failure";

        logger.error("❌ FAILED    [{}/{}] {} ({}ms) | Reason: {}",
                     Thread.currentThread().getName(),
                     result.getTestClass().getRealClass().getSimpleName(),
                     result.getMethod().getMethodName(),
                     getDurationMs(result),
                     reason);

        // Log the full stack trace at DEBUG level to keep ERROR output clean
        if (result.getThrowable() != null) {
            logger.debug("Stack trace for failed test [{}]:", result.getMethod().getMethodName(),
                         result.getThrowable());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("⚠️  SKIPPED   [{}/{}] {}",
                    Thread.currentThread().getName(),
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        logger.warn("⚠️  WITHIN-THRESHOLD [{}/{}] {}",
                    Thread.currentThread().getName(),
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        logger.error("⏱️  TIMEOUT   [{}/{}] {} after {}ms",
                     Thread.currentThread().getName(),
                     result.getTestClass().getRealClass().getSimpleName(),
                     result.getMethod().getMethodName(),
                     getDurationMs(result));
    }

    // ----------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------

    /**
     * Calculates test duration from ITestResult start/end timestamps.
     */
    private long getDurationMs(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
