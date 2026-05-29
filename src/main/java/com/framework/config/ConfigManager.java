package com.framework.config;

import com.framework.exceptions.FrameworkException;
import com.framework.logging.FrameworkLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager
 * =============
 * Reads all configuration from src/main/resources/config.properties.
 *
 * DESIGN DECISIONS:
 * -----------------
 * 1. Singleton pattern using an enum — thread-safe by JVM guarantee.
 * 2. System property ALWAYS overrides the file value:
 *       mvn test -Dbase.url=https://staging.api.com
 *    This makes it easy to run tests against different environments in CI/CD.
 * 3. Sensitive values (passwords, secrets) are masked in logs.
 *
 * HOW TO USE:
 * -----------
 *    String baseUrl = ConfigManager.getInstance().getBaseUrl();
 *    String clientId = ConfigManager.getInstance().getProperty("client.id");
 */
public enum ConfigManager {

    INSTANCE; // Singleton enum — only one instance exists throughout the JVM

    private static final String CONFIG_FILE = "config.properties";
    private final Properties properties;

    /**
     * Constructor — runs once when the enum constant is first accessed.
     * Loads config.properties from the classpath.
     */
    ConfigManager() {
        properties = new Properties();
        loadProperties();
    }

    // ----------------------------------------------------------
    // Public API
    // ----------------------------------------------------------

    /**
     * Returns the singleton instance.
     *
     * Usage: ConfigManager.getInstance().getBaseUrl()
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a property value.
     * System property takes priority over config.properties value.
     *
     * @param key the property key (e.g., "base.url")
     * @return the property value
     * @throws FrameworkException if the key does not exist
     */
    public String getProperty(String key) {
        // Check system property first (allows CI/CD override)
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new FrameworkException(
                "Missing required configuration key: '" + key + "'. " +
                "Please add it to config.properties or pass it as a system property."
            );
        }
        return value.trim();
    }

    /**
     * Returns a property value with a default fallback.
     * Does NOT throw an exception if the key is missing.
     *
     * @param key          the property key
     * @param defaultValue fallback value if key is not found
     * @return the property value or the default
     */
    public String getProperty(String key, String defaultValue) {
        try {
            return getProperty(key);
        } catch (FrameworkException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a property value parsed as an integer.
     *
     * @param key          the property key
     * @param defaultValue fallback if key is missing or not a number
     * @return integer value
     */
    public int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (FrameworkException | NumberFormatException e) {
            return defaultValue;
        }
    }

    // ----------------------------------------------------------
    // Convenience Getters (avoid repeating key strings in tests)
    // ----------------------------------------------------------

    public String getBaseUrl() {
        return getProperty("base.url");
    }

    public String getAuthUrl() {
        return getProperty("auth.url");
    }

    public String getClientId() {
        return getProperty("client.id");
    }

    public String getClientSecret() {
        return getProperty("client.secret");
    }

    public String getAuthUsername() {
        return getProperty("auth.username", "");
    }

    public String getAuthPassword() {
        return getProperty("auth.password", "");
    }

    public String getGrantType() {
        return getProperty("auth.grant.type", "client_credentials");
    }

    public String getAuthScope() {
        return getProperty("auth.scope", "");
    }

    public String getMssqlUrl() {
        return getProperty("mssql.url");
    }

    public String getMssqlUsername() {
        return getProperty("mssql.username");
    }

    public String getMssqlPassword() {
        return getProperty("mssql.password");
    }

    public String getOracleUrl() {
        return getProperty("oracle.url");
    }

    public String getOracleUsername() {
        return getProperty("oracle.username");
    }

    public String getOraclePassword() {
        return getProperty("oracle.password");
    }

    public int getRetryMaxCount() {
        return getIntProperty("retry.max.count", 3);
    }

    public int getRetryInitialDelayMs() {
        return getIntProperty("retry.initial.delay.ms", 1000);
    }

    // ----------------------------------------------------------
    // Private Helpers
    // ----------------------------------------------------------

    private void loadProperties() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new FrameworkException(
                    "Cannot find '" + CONFIG_FILE + "' in classpath. " +
                    "Ensure it exists at src/main/resources/" + CONFIG_FILE
                );
            }
            properties.load(inputStream);
            FrameworkLogger.getLogger(ConfigManager.class)
                           .info("Configuration loaded successfully from '{}'", CONFIG_FILE);
        } catch (IOException e) {
            throw new FrameworkException("Failed to load configuration file: " + CONFIG_FILE, e);
        }
    }
}
