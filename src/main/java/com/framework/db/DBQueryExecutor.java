package com.framework.db;

import com.framework.exceptions.DatabaseException;
import com.framework.logging.FrameworkLogger;
import io.qameta.allure.Step;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * DBQueryExecutor
 * ===============
 * Provides reusable, high-level database query methods for test validation.
 *
 * DESIGN:
 * -------
 * - Obtained via: DBConnectionManager.getInstance().getExecutor(DBType.MSSQL)
 * - Each method handles its own Connection lifecycle (open → use → close)
 * - Uses PreparedStatement for ALL queries (prevents SQL injection)
 * - All exceptions are wrapped in DatabaseException with full SQL context
 *
 * AVAILABLE METHODS:
 * ------------------
 *   fetchAll(sql, params)     → List of rows, each row is a Map<columnName, value>
 *   fetchOne(sql, params)     → Single row as Map<columnName, value>
 *   fetchScalar(sql, type)    → Single value (e.g., COUNT, MAX, a specific column)
 *   executeUpdate(sql, params)→ Number of rows affected (INSERT/UPDATE/DELETE)
 *   exists(sql, params)       → true if at least one row is returned
 *
 * HOW TO USE:
 * -----------
 *   // Get executor for MSSQL
 *   DBQueryExecutor mssql = DBConnectionManager.getInstance().getExecutor(DBType.MSSQL);
 *
 *   // Fetch all matching rows
 *   List<Map<String, Object>> users = mssql.fetchAll(
 *       "SELECT id, name, email FROM users WHERE status = ?", "active"
 *   );
 *
 *   // Fetch a single row
 *   Map<String, Object> user = mssql.fetchOne(
 *       "SELECT * FROM users WHERE id = ?", 123
 *   );
 *   String name = (String) user.get("name");
 *
 *   // Fetch a single value (scalar)
 *   int count = mssql.fetchScalar("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
 *
 *   // Check if a record exists
 *   boolean exists = mssql.exists("SELECT 1 FROM users WHERE email = ?", "test@example.com");
 *
 *   // Execute an UPDATE
 *   int rowsAffected = mssql.executeUpdate("UPDATE users SET status = ? WHERE id = ?", "inactive", 123);
 */
public class DBQueryExecutor {

    private static final Logger logger = FrameworkLogger.getLogger(DBQueryExecutor.class);

    private final DBConnectionManager connectionManager;
    private final DBType dbType;

    /**
     * Package-private constructor — created by DBConnectionManager.getExecutor()
     *
     * @param connectionManager the connection manager to get connections from
     * @param dbType            which database this executor targets
     */
    DBQueryExecutor(DBConnectionManager connectionManager, DBType dbType) {
        this.connectionManager = connectionManager;
        this.dbType            = dbType;
    }

    // ----------------------------------------------------------
    // fetchAll — Returns multiple rows
    // ----------------------------------------------------------

    /**
     * Executes a SELECT query and returns ALL matching rows.
     *
     * Each row is returned as a Map where:
     *   - Key   = column name (as defined in the database)
     *   - Value = column value (Java type matches SQL type, e.g., String, Integer, Date)
     *
     * @param sql    the SQL SELECT query (use ? for parameters)
     * @param params the parameter values to bind to each ? placeholder
     * @return list of rows, empty list if no rows found
     * @throws DatabaseException if the query fails
     */
    @Step("DB [{dbType}] fetchAll: {sql}")
    public List<Map<String, Object>> fetchAll(String sql, Object... params) {
        logger.info("[{}] Executing fetchAll: {}", dbType, sql);

        try (Connection conn = connectionManager.getConnection(dbType);
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>(); // LinkedHashMap preserves column order
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            logger.info("[{}] fetchAll returned {} rows.", dbType, rows.size());
            return rows;

        } catch (SQLException e) {
            throw new DatabaseException("fetchAll failed on " + dbType.getDisplayName(),
                                        sql, e.getSQLState(), e);
        }
    }

    // ----------------------------------------------------------
    // fetchOne — Returns a single row
    // ----------------------------------------------------------

    /**
     * Executes a SELECT query and returns the FIRST matching row.
     * Returns null if no rows are found.
     *
     * RECOMMENDATION: Add a LIMIT 1 (MSSQL: TOP 1) to your query when you
     * know only one row is expected — improves performance.
     *
     * @param sql    the SQL SELECT query (use ? for parameters)
     * @param params the parameter values
     * @return first matching row as Map, or null if no rows found
     * @throws DatabaseException if the query fails
     */
    @Step("DB [{dbType}] fetchOne: {sql}")
    public Map<String, Object> fetchOne(String sql, Object... params) {
        logger.info("[{}] Executing fetchOne: {}", dbType, sql);

        List<Map<String, Object>> rows = fetchAll(sql, params);

        if (rows.isEmpty()) {
            logger.warn("[{}] fetchOne returned no rows for query: {}", dbType, sql);
            return null;
        }

        if (rows.size() > 1) {
            logger.warn("[{}] fetchOne: query returned {} rows, using first row only. " +
                        "Consider adding TOP 1 / LIMIT 1 to your query.", dbType, rows.size());
        }

        return rows.get(0);
    }

    // ----------------------------------------------------------
    // fetchScalar — Returns a single value
    // ----------------------------------------------------------

    /**
     * Executes a SELECT query and returns the value of the FIRST column
     * of the FIRST row, cast to the specified type.
     *
     * COMMON USE CASES:
     *   - Count: fetchScalar("SELECT COUNT(*) FROM orders", Integer.class)
     *   - Max ID: fetchScalar("SELECT MAX(id) FROM users", Long.class)
     *   - Single column: fetchScalar("SELECT name FROM users WHERE id = ?", String.class, 123)
     *
     * @param sql    the SQL SELECT query (use ? for parameters)
     * @param type   the expected return type (String.class, Integer.class, Long.class, etc.)
     * @param params the parameter values
     * @param <T>    the expected return type
     * @return the scalar value or null if no rows found
     * @throws DatabaseException if the query fails or type cast fails
     */
    @SuppressWarnings("unchecked")
    @Step("DB [{dbType}] fetchScalar: {sql}")
    public <T> T fetchScalar(String sql, Class<T> type, Object... params) {
        logger.info("[{}] Executing fetchScalar (type={}): {}", dbType, type.getSimpleName(), sql);

        try (Connection conn = connectionManager.getConnection(dbType);
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Object value = rs.getObject(1);
                if (value == null) {
                    return null;
                }
                // Handle common type conversions
                return convertToType(value, type);
            }

            logger.warn("[{}] fetchScalar returned no rows for query: {}", dbType, sql);
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("fetchScalar failed on " + dbType.getDisplayName(),
                                        sql, e.getSQLState(), e);
        }
    }

    // ----------------------------------------------------------
    // executeUpdate — INSERT / UPDATE / DELETE
    // ----------------------------------------------------------

    /**
     * Executes an INSERT, UPDATE, or DELETE statement.
     *
     * @param sql    the SQL DML statement (use ? for parameters)
     * @param params the parameter values
     * @return the number of rows affected
     * @throws DatabaseException if the statement fails
     */
    @Step("DB [{dbType}] executeUpdate: {sql}")
    public int executeUpdate(String sql, Object... params) {
        logger.info("[{}] Executing update: {}", dbType, sql);

        try (Connection conn = connectionManager.getConnection(dbType);
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {

            int rowsAffected = stmt.executeUpdate();
            logger.info("[{}] Update affected {} row(s).", dbType, rowsAffected);
            return rowsAffected;

        } catch (SQLException e) {
            throw new DatabaseException("executeUpdate failed on " + dbType.getDisplayName(),
                                        sql, e.getSQLState(), e);
        }
    }

    // ----------------------------------------------------------
    // exists — Check if a record exists
    // ----------------------------------------------------------

    /**
     * Checks whether at least one row matches the given query.
     *
     * RECOMMENDED QUERY FORMAT:
     *   "SELECT 1 FROM table WHERE condition = ?"
     *
     * @param sql    the SQL query (use ? for parameters)
     * @param params the parameter values
     * @return true if at least one row is returned, false otherwise
     * @throws DatabaseException if the query fails
     */
    @Step("DB [{dbType}] exists check: {sql}")
    public boolean exists(String sql, Object... params) {
        logger.info("[{}] Checking existence: {}", dbType, sql);

        try (Connection conn = connectionManager.getConnection(dbType);
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            boolean found = rs.next();
            logger.info("[{}] Existence check result: {}", dbType, found);
            return found;

        } catch (SQLException e) {
            throw new DatabaseException("exists check failed on " + dbType.getDisplayName(),
                                        sql, e.getSQLState(), e);
        }
    }

    // ----------------------------------------------------------
    // fetchColumn — Extract a specific column from all rows
    // ----------------------------------------------------------

    /**
     * Executes a SELECT query and returns the values of a specific column
     * from all matching rows as a List.
     *
     * USEFUL FOR:
     *   List<String> emails = executor.fetchColumn(
     *       "SELECT email FROM users WHERE status = ?", "email", "active"
     *   );
     *
     * @param sql        the SQL SELECT query
     * @param columnName the name of the column to extract
     * @param params     the parameter values
     * @param <T>        the expected value type
     * @return list of values from the specified column
     */
    @SuppressWarnings("unchecked")
    @Step("DB [{dbType}] fetchColumn '{columnName}': {sql}")
    public <T> List<T> fetchColumn(String sql, String columnName, Object... params) {
        List<Map<String, Object>> rows = fetchAll(sql, params);
        List<T> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            values.add((T) row.get(columnName));
        }
        return values;
    }

    // ----------------------------------------------------------
    // Private Helpers
    // ----------------------------------------------------------

    /**
     * Creates a PreparedStatement and binds all parameters.
     *
     * @param conn   the JDBC connection
     * @param sql    the SQL with ? placeholders
     * @param params the values to bind
     * @return the populated PreparedStatement
     * @throws SQLException if statement creation or binding fails
     */
    private PreparedStatement prepareStatement(Connection conn, String sql, Object[] params)
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
        return stmt;
    }

    /**
     * Converts a database value to the specified Java type.
     * Handles common cases like Number → Integer/Long/Double.
     *
     * @param value the value from the ResultSet
     * @param type  the target type
     * @param <T>   generic type
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    private <T> T convertToType(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (type == Integer.class && value instanceof Number n) {
            return (T) Integer.valueOf(n.intValue());
        }
        if (type == Long.class && value instanceof Number n) {
            return (T) Long.valueOf(n.longValue());
        }
        if (type == Double.class && value instanceof Number n) {
            return (T) Double.valueOf(n.doubleValue());
        }
        if (type == String.class) {
            return (T) String.valueOf(value);
        }
        // Fallback — attempt direct cast
        return type.cast(value);
    }
}
