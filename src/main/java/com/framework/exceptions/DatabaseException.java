package com.framework.exceptions;

/**
 * DatabaseException
 * =================
 * Thrown when a database operation fails.
 *
 * WHEN IS THIS THROWN?
 * --------------------
 * - Failed to acquire a connection from the pool
 * - SQL query execution fails
 * - Result set parsing fails
 * - Connection pool initialization fails
 *
 * EXTRA CONTEXT:
 * --------------
 * Carries the SQL state code and the original SQL query
 * to aid in debugging.
 *
 * EXAMPLE:
 * --------
 *   throw new DatabaseException(
 *       "Failed to execute query",
 *       "SELECT * FROM users WHERE id = ?",
 *       "42S02",   // SQL state: table not found
 *       originalSqlException
 *   );
 */
public class DatabaseException extends FrameworkException {

    /**
     * The SQL query that caused the failure.
     * May be null if the error happened before a query was executed.
     */
    private final String failedQuery;

    /**
     * SQL state code from the underlying SQLException.
     * Standard 5-character code (e.g., "42S02" = table not found).
     * May be null if not applicable.
     */
    private final String sqlState;

    /**
     * Creates a DatabaseException with message only.
     * Use for connection pool or setup failures.
     *
     * @param message explanation of the failure
     */
    public DatabaseException(String message) {
        super(message);
        this.failedQuery = null;
        this.sqlState    = null;
    }

    /**
     * Creates a DatabaseException wrapping a root cause.
     *
     * @param message explanation of the failure
     * @param cause   the underlying exception (e.g., SQLException)
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.failedQuery = null;
        this.sqlState    = null;
    }

    /**
     * Creates a DatabaseException with full SQL context.
     *
     * @param message      explanation of the failure
     * @param failedQuery  the SQL query that was being executed
     * @param sqlState     SQL state code from the SQLException
     * @param cause        the original SQLException
     */
    public DatabaseException(String message, String failedQuery, String sqlState, Throwable cause) {
        super(message + " | Query=[" + failedQuery + "] | SQLState=" + sqlState, cause);
        this.failedQuery = failedQuery;
        this.sqlState    = sqlState;
    }

    /**
     * Returns the SQL query that failed.
     * May be null if error occurred before query execution.
     */
    public String getFailedQuery() {
        return failedQuery;
    }

    /**
     * Returns the SQL state code.
     * May be null if not applicable.
     */
    public String getSqlState() {
        return sqlState;
    }
}
