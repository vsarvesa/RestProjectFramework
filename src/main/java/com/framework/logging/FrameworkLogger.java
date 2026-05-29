package com.framework.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FrameworkLogger
 * ===============
 * A thin wrapper around Log4j2.
 *
 * WHY WRAP LOG4J2?
 * ----------------
 * - Provides a single place to add future cross-cutting concerns
 *   (e.g., masking sensitive data, adding correlation IDs).
 * - Keeps all classes using the same logging approach.
 * - Thread name is automatically included via log4j2.xml pattern.
 *
 * HOW TO USE:
 * -----------
 *   private static final Logger logger = FrameworkLogger.getLogger(MyClass.class);
 *   logger.info("Request sent to: {}", url);
 *   logger.error("Failed with status: {}", statusCode, exception);
 */
public final class FrameworkLogger {

    // Private constructor — utility class, never instantiate
    private FrameworkLogger() {
        throw new UnsupportedOperationException("FrameworkLogger is a utility class.");
    }

    /**
     * Returns a Log4j2 Logger for the given class.
     * The logger name will be the fully-qualified class name,
     * matching the logger hierarchy defined in log4j2.xml.
     *
     * @param clazz the class requesting the logger
     * @return Log4j2 Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    /**
     * Returns a Log4j2 Logger by a custom name.
     * Useful for non-class loggers (e.g., "API-CALLS", "DB-QUERIES").
     *
     * @param name custom logger name
     * @return Log4j2 Logger instance
     */
    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

    /**
     * Masks a sensitive string for safe logging.
     * Shows first 4 characters and replaces the rest with asterisks.
     *
     * Example: maskSensitive("secret123") → "secr*****"
     *
     * @param value the sensitive string to mask
     * @return masked string
     */
    public static String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "*".repeat(value.length() - 4);
    }
}
