package com.framework.db;

import com.framework.config.ConfigManager;
import com.framework.exceptions.DatabaseException;
import com.framework.logging.FrameworkLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBConnectionManager
 * ===================
 * Manages HikariCP connection pools for MSSQL and Oracle databases.
 *
 * DESIGN: Singleton using enum (thread-safe, initialized once by JVM)
 *
 * WHY HIKARICP?
 * -------------
 * - Fastest JDBC connection pool available for Java
 * - Thread-safe: handles concurrent requests from parallel tests
 * - Automatically validates and recycles stale connections
 * - Configurable pool size, timeout, and idle settings
 *
 * HOW IT WORKS:
 * -------------
 * - Two separate DataSource pools are created at startup:
 *     1. mssqlDataSource → pool for SQL Server
 *     2. oracleDataSource → pool for Oracle
 * - getConnection(DBType) returns a Connection from the appropriate pool
 * - Connections MUST be returned to the pool after use (use try-with-resources)
 *
 * HOW TO USE:
 * -----------
 *   // Get a connection (always use try-with-resources!)
 *   try (Connection conn = DBConnectionManager.getInstance().getConnection(DBType.MSSQL)) {
 *       // use connection
 *   }
 *   // → Connection is automatically returned to pool when try block exits
 *
 * NOTE: You typically don't call getConnection() directly.
 *       Use DBQueryExecutor which handles connections for you.
 */
public enum DBConnectionManager {

    INSTANCE;

    private static final Logger logger = FrameworkLogger.getLogger(DBConnectionManager.class);

    // HikariCP DataSources (connection pools) — one per database
    private HikariDataSource mssqlDataSource;
    private HikariDataSource oracleDataSource;

    // Tracks whether each pool has been initialized
    private volatile boolean mssqlInitialized  = false;
    private volatile boolean oracleInitialized = false;

    // Locks to prevent race conditions during lazy initialization
    private final Object mssqlLock  = new Object();
    private final Object oracleLock = new Object();

    /**
     * Returns the singleton instance.
     */
    public static DBConnectionManager getInstance() {
        return INSTANCE;
    }

    // ----------------------------------------------------------
    // Public: Get Connection
    // ----------------------------------------------------------

    /**
     * Returns a JDBC Connection from the appropriate pool for the given DBType.
     *
     * IMPORTANT: Always use try-with-resources:
     *   try (Connection conn = DBConnectionManager.getInstance().getConnection(DBType.MSSQL)) {
     *       ...
     *   }
     *
     * @param dbType the database to connect to (MSSQL or ORACLE)
     * @return a live JDBC Connection from the pool
     * @throws DatabaseException if a connection cannot be obtained
     */
    public Connection getConnection(DBType dbType) {
        try {
            return switch (dbType) {
                case MSSQL  -> getMssqlConnection();
                case ORACLE -> getOracleConnection();
            };
        } catch (SQLException e) {
            throw new DatabaseException(
                "Failed to obtain connection from " + dbType.getDisplayName() + " pool.",
                null,
                e.getSQLState(),
                e
            );
        }
    }

    /**
     * Returns a DBQueryExecutor backed by the specified database.
     * This is the preferred way to run queries — DBQueryExecutor handles
     * connection lifecycle and exception wrapping automatically.
     *
     * @param dbType the database to query (MSSQL or ORACLE)
     * @return a DBQueryExecutor ready to run queries
     */
    public DBQueryExecutor getExecutor(DBType dbType) {
        return new DBQueryExecutor(this, dbType);
    }

    // ----------------------------------------------------------
    // Public: Cleanup (call in test suite teardown)
    // ----------------------------------------------------------

    /**
     * Closes both connection pools and releases all resources.
     * Call this once in @AfterSuite in BaseTest.
     */
    public void closeAll() {
        closeMssqlPool();
        closeOraclePool();
    }

    // ----------------------------------------------------------
    // Private: MSSQL Pool Management
    // ----------------------------------------------------------

    private Connection getMssqlConnection() throws SQLException {
        if (!mssqlInitialized) {
            synchronized (mssqlLock) {
                if (!mssqlInitialized) {
                    mssqlDataSource = createMssqlPool();
                    mssqlInitialized = true;
                }
            }
        }
        return mssqlDataSource.getConnection();
    }

    private HikariDataSource createMssqlPool() {
        ConfigManager config = ConfigManager.getInstance();
        String url = config.getMssqlUrl();
        if (url == null || url.isBlank()) {
            throw new org.testng.SkipException(
                "MSSQL DB is not configured in config.properties. Skipping this test."
            );
        }
        logger.info("Initializing MSSQL connection pool...");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getMssqlUrl());
        hikariConfig.setUsername(config.getMssqlUsername());
        hikariConfig.setPassword(config.getMssqlPassword());

        // Pool sizing
        hikariConfig.setMinimumIdle(
            config.getIntProperty("mssql.pool.min-idle", 2));
        hikariConfig.setMaximumPoolSize(
            config.getIntProperty("mssql.pool.max-pool-size", 10));
        hikariConfig.setConnectionTimeout(
            config.getIntProperty("mssql.pool.connection-timeout", 30_000));

        // Pool name for HikariCP logs
        hikariConfig.setPoolName("MSSQL-Pool");

        // Validate connection before returning from pool
        hikariConfig.setConnectionTestQuery("SELECT 1");

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        logger.info("MSSQL connection pool initialized. URL: {}",
                    config.getMssqlUrl());
        return ds;
    }

    private void closeMssqlPool() {
        if (mssqlDataSource != null && !mssqlDataSource.isClosed()) {
            mssqlDataSource.close();
            logger.info("MSSQL connection pool closed.");
        }
    }

    // ----------------------------------------------------------
    // Private: Oracle Pool Management
    // ----------------------------------------------------------

    private Connection getOracleConnection() throws SQLException {
        if (!oracleInitialized) {
            synchronized (oracleLock) {
                if (!oracleInitialized) {
                    oracleDataSource = createOraclePool();
                    oracleInitialized = true;
                }
            }
        }
        return oracleDataSource.getConnection();
    }

    private HikariDataSource createOraclePool() {
        ConfigManager config = ConfigManager.getInstance();
        String url = config.getOracleUrl();
        if (url == null || url.isBlank()) {
            throw new org.testng.SkipException(
                "Oracle DB is not configured in config.properties. Skipping this test."
            );
        }
        logger.info("Initializing Oracle connection pool...");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getOracleUrl());
        hikariConfig.setUsername(config.getOracleUsername());
        hikariConfig.setPassword(config.getOraclePassword());

        // Pool sizing
        hikariConfig.setMinimumIdle(
            config.getIntProperty("oracle.pool.min-idle", 2));
        hikariConfig.setMaximumPoolSize(
            config.getIntProperty("oracle.pool.max-pool-size", 10));
        hikariConfig.setConnectionTimeout(
            config.getIntProperty("oracle.pool.connection-timeout", 30_000));

        // Pool name for HikariCP logs
        hikariConfig.setPoolName("Oracle-Pool");

        // Oracle-specific connection validation query
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM DUAL");

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        logger.info("Oracle connection pool initialized. URL: {}",
                    config.getOracleUrl());
        return ds;
    }

    private void closeOraclePool() {
        if (oracleDataSource != null && !oracleDataSource.isClosed()) {
            oracleDataSource.close();
            logger.info("Oracle connection pool closed.");
        }
    }
}
