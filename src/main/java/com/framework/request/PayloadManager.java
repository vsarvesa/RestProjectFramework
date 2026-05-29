package com.framework.request;

import com.framework.constants.FrameworkConstants;
import com.framework.exceptions.ApiRequestException;
import com.framework.logging.FrameworkLogger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.framework.utils.JsonUtils;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * PayloadManager
 * ==============
 * Loads JSON payload files and allows field-level customization.
 *
 * DESIGN PATTERN:
 * ---------------
 * Uses the Template Method concept:
 *   1. Load a JSON template file from disk (immutable baseline)
 *   2. Apply customizations using a lambda Consumer
 *   3. Return the final JSON string ready for the API request
 *
 * WHY JSON FILES?
 * ---------------
 * - Keeps test code clean and readable (no large JSON strings in code)
 * - Template files serve as living documentation of the API contract
 * - Fields can be customized per-test without duplicating entire payloads
 *
 * HOW TO USE:
 * -----------
 *
 *   // Use the file as-is (no customization)
 *   String payload = PayloadManager.load("create_user.json").getPayload();
 *
 *   // Customize specific fields
 *   String payload = PayloadManager.load("create_user.json")
 *       .customize(body -> {
 *           body.put("email", "test@example.com");
 *           body.put("age", 30);
 *       })
 *       .getPayload();
 *
 *   // Nested field customization
 *   String payload = PayloadManager.load("create_order.json")
 *       .customize(body -> {
 *           body.put("orderId", "ORD-999");
 *           ((ObjectNode) body.get("address")).put("city", "New York");
 *       })
 *       .getPayload();
 */
public class PayloadManager {

    private static final Logger logger = FrameworkLogger.getLogger(PayloadManager.class);

    /**
     * The mutable working copy of the loaded JSON.
     * Each PayloadManager instance has its own copy — thread-safe.
     */
    private final ObjectNode payloadNode;

    /**
     * The file name of the loaded payload (for logging/debugging).
     */
    private final String fileName;

    // Private constructor — use the static factory method load()
    private PayloadManager(ObjectNode payloadNode, String fileName) {
        this.payloadNode = payloadNode;
        this.fileName    = fileName;
    }

    // ----------------------------------------------------------
    // Static Factory: load()
    // ----------------------------------------------------------

    /**
     * Loads a JSON file from the classpath and returns a PayloadManager.
     *
     * The file must be located under: src/test/resources/payloads/
     *
     * @param fileName the file name only (e.g., "create_user.json")
     *                 Do NOT include the "payloads/" prefix.
     * @return PayloadManager instance ready for customization
     * @throws ApiRequestException if the file is not found or is invalid JSON
     */
    public static PayloadManager load(String fileName) {
        String classpathPath = FrameworkConstants.PAYLOADS_DIR + fileName;
        ObjectNode node = JsonUtils.loadFromClasspath(classpathPath);

        // Deep copy so the template is never modified between tests
        ObjectNode copy = node.deepCopy();

        logger.debug("PayloadManager: Loaded '{}' from classpath.", classpathPath);
        return new PayloadManager(copy, fileName);
    }

    /**
     * Creates a PayloadManager from an inline JSON string.
     * Useful when you want to build a payload entirely in code.
     *
     * @param jsonString the JSON string to use as the payload
     * @return PayloadManager instance ready for customization
     */
    public static PayloadManager fromString(String jsonString) {
        ObjectNode node = JsonUtils.toObjectNode(jsonString);
        return new PayloadManager(node, "<inline>");
    }

    // ----------------------------------------------------------
    // Fluent Customization
    // ----------------------------------------------------------

    /**
     * Applies customizations to the loaded payload.
     *
     * The Consumer receives the mutable ObjectNode and can call:
     *   - node.put("key", "value")     → set a string field
     *   - node.put("key", 42)          → set a number field
     *   - node.put("key", true)        → set a boolean field
     *   - node.remove("key")           → remove a field
     *   - node.putNull("key")          → set field to null
     *   - ((ObjectNode) node.get("nested")).put("key", "value") → nested field
     *
     * @param customizer lambda that modifies the payload node
     * @return this PayloadManager instance (for method chaining)
     */
    public PayloadManager customize(Consumer<ObjectNode> customizer) {
        customizer.accept(payloadNode);
        logger.debug("PayloadManager: Customization applied to '{}'.", fileName);
        return this;
    }

    // ----------------------------------------------------------
    // Terminal: getPayload()
    // ----------------------------------------------------------

    /**
     * Returns the final JSON string ready to send as an API request body.
     *
     * @return JSON string representation of the payload
     */
    public String getPayload() {
        String json = JsonUtils.toJson(payloadNode);
        logger.debug("PayloadManager: Final payload for '{}': {}", fileName, json);
        return json;
    }

    /**
     * Returns the raw ObjectNode for direct manipulation.
     * Prefer using customize() and getPayload() instead.
     *
     * @return the mutable ObjectNode
     */
    public ObjectNode getPayloadNode() {
        return payloadNode;
    }
}
