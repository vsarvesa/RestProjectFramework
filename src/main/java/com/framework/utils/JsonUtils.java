package com.framework.utils;

import com.framework.exceptions.ApiRequestException;
import com.framework.logging.FrameworkLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * JsonUtils
 * =========
 * Reusable Jackson helpers for working with JSON throughout the framework.
 *
 * RESPONSIBILITIES:
 * -----------------
 * - Parse JSON strings to typed objects
 * - Serialize objects to JSON strings
 * - Deep-copy ObjectNode for independent mutation
 *
 * HOW TO USE:
 * -----------
 *   // Deserialize a JSON response body to a POJO
 *   UserResponse user = JsonUtils.fromJson(responseBody, UserResponse.class);
 *
 *   // Serialize a POJO to a JSON string
 *   String json = JsonUtils.toJson(createUserRequest);
 *
 *   // Parse a JSON string to a mutable ObjectNode (for payload customization)
 *   ObjectNode node = JsonUtils.toObjectNode(jsonString);
 *   node.put("email", "new@example.com");
 */
public final class JsonUtils {

    private static final Logger logger = FrameworkLogger.getLogger(JsonUtils.class);

    /**
     * Shared ObjectMapper instance.
     * ObjectMapper is thread-safe after configuration — safe to share.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Private constructor — utility class
    private JsonUtils() {
        throw new UnsupportedOperationException("JsonUtils is a utility class.");
    }

    /**
     * Deserializes a JSON string into a Java object of the specified type.
     *
     * @param json  the JSON string (e.g., response body)
     * @param clazz the target class (e.g., UserResponse.class)
     * @param <T>   generic return type
     * @return the deserialized object
     * @throws ApiRequestException if parsing fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new ApiRequestException(
                "Failed to parse JSON to " + clazz.getSimpleName() + ". JSON: " + json, e
            );
        }
    }

    /**
     * Serializes a Java object to a JSON string.
     *
     * @param object the object to serialize
     * @return JSON string representation
     * @throws ApiRequestException if serialization fails
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new ApiRequestException(
                "Failed to serialize object of type: " + object.getClass().getSimpleName(), e
            );
        }
    }

    /**
     * Parses a JSON string into a mutable ObjectNode.
     * Use this when you need to customize specific fields of a JSON payload.
     *
     * @param json the JSON string to parse
     * @return mutable ObjectNode
     * @throws ApiRequestException if JSON is invalid
     */
    public static ObjectNode toObjectNode(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            if (!node.isObject()) {
                throw new ApiRequestException(
                    "Expected a JSON object but got: " + node.getNodeType()
                );
            }
            return (ObjectNode) node;
        } catch (IOException e) {
            throw new ApiRequestException("Failed to parse JSON string to ObjectNode. JSON: " + json, e);
        }
    }

    /**
     * Reads a JSON file from the classpath and returns it as an ObjectNode.
     * Used by PayloadManager to load payload template files.
     *
     * @param classpathPath path relative to the classpath root (e.g., "payloads/create_user.json")
     * @return mutable ObjectNode loaded from the file
     * @throws ApiRequestException if the file is not found or is invalid JSON
     */
    public static ObjectNode loadFromClasspath(String classpathPath) {
        try (InputStream is = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new ApiRequestException(
                    "Payload file not found on classpath: '" + classpathPath + "'. " +
                    "Ensure the file exists under src/test/resources/" + classpathPath
                );
            }
            JsonNode node = OBJECT_MAPPER.readTree(is);
            if (!node.isObject()) {
                throw new ApiRequestException(
                    "Payload file '" + classpathPath + "' must be a JSON object, got: " + node.getNodeType()
                );
            }
            logger.debug("Loaded JSON payload from classpath: {}", classpathPath);
            return (ObjectNode) node;
        } catch (IOException e) {
            throw new ApiRequestException(
                "Failed to read payload file from classpath: " + classpathPath, e
            );
        }
    }

    /**
     * Returns the shared ObjectMapper for advanced usage.
     * ObjectMapper is thread-safe — do not reconfigure it at runtime.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
