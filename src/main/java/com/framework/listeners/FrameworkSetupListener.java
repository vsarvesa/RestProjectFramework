package com.framework.listeners;

import com.framework.db.DBConnectionManager;
import com.framework.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * FrameworkSetupListener
 * ======================
 * A TestNG ISuiteListener that handles all one-time framework
 * infrastructure setup and teardown invisibly.
 *
 * WHY THIS EXISTS:
 * ----------------
 * Previously, @BeforeSuite / @AfterSuite in BaseTest were doing:
 *   - DB connection pool teardown
 *   - Banner logging
 *
 * Moving this into a listener means:
 *   1. BaseTest has zero infrastructure concern — it's purely about test lifecycle
 *   2. The DB pool is ALWAYS closed after the suite, even if BaseTest is not used
 *   3. No logger visible inside BaseTest or any test class
 *
 * REGISTRATION:
 * -------------
 * In testng.xml:
 *   <listener class-name="com.framework.listeners.FrameworkSetupListener"/>
 */
public class FrameworkSetupListener implements ISuiteListener {

    private static final Logger logger = FrameworkLogger.getLogger(FrameworkSetupListener.class);

    /**
     * Called once before any test in the suite starts.
     * Good place for any global pre-suite setup that should not be in BaseTest.
     */
    @Override
    public void onStart(ISuite suite) {
        logger.info("FrameworkSetupListener: Suite '{}' initializing...", suite.getName());
    }

    /**
     * Called once after ALL tests in the suite have finished.
     * Closes all HikariCP connection pools for MSSQL and Oracle.
     */
    @Override
    public void onFinish(ISuite suite) {
        logger.info("FrameworkSetupListener: Closing all database connection pools...");
        try {
            DBConnectionManager.getInstance().closeAll();
            logger.info("FrameworkSetupListener: Database connection pools closed successfully.");
        } catch (Exception e) {
            logger.error("FrameworkSetupListener: Error closing DB connection pools: {}", e.getMessage(), e);
        }
    }
}
