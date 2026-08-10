package com.xiaou.system.agent.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Stable SHA-256 fingerprint for a registered tool and filtered input.
 */
public final class AgentTaskFingerprint {

    private AgentTaskFingerprint() {
    }

    public static String of(String toolName, Map<String, Object> input, ObjectMapper objectMapper) {
        try {
            JsonNode canonicalInput = canonicalize(objectMapper.valueToTree(input == null ? Map.of() : input), objectMapper);
            String payload = normalize(toolName) + "\n" + objectMapper.writeValueAsString(canonicalInput);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("无法生成任务步骤指纹", e);
        }
    }

    private static JsonNode canonicalize(JsonNode node, ObjectMapper objectMapper) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                result.add(canonicalize(item, objectMapper));
            }
            return result;
        }
        ObjectNode result = objectMapper.createObjectNode();
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        names.sort(Comparator.naturalOrder());
        for (String name : names) {
            result.set(name, canonicalize(node.get(name), objectMapper));
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
