package com.framework.db;

/**
 * DBType
 * ======
 * Enum representing the supported database types in this framework.
 *
 * CURRENT SUPPORTED DATABASES:
 * - MSSQL  → Microsoft SQL Server
 * - ORACLE → Oracle Database
 *
 * HOW TO USE:
 * -----------
 *   // Get the MSSQL executor
 *   DBQueryExecutor executor = DBConnectionManager.getInstance().getExecutor(DBType.MSSQL);
 *
 *   // Get the Oracle executor
 *   DBQueryExecutor executor = DBConnectionManager.getInstance().getExecutor(DBType.ORACLE);
 */
public enum DBType {

    /**
     * Microsoft SQL Server
     * Uses JDBC URL format: jdbc:sqlserver://host:port;databaseName=DB
     */
    MSSQL("MSSQL (Microsoft SQL Server)"),

    /**
     * Oracle Database
     * Uses JDBC URL format: jdbc:oracle:thin:@//host:port/serviceName
     */
    ORACLE("Oracle Database");

    private final String displayName;

    DBType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable name of this database type.
     * Used in log messages and exception messages.
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
