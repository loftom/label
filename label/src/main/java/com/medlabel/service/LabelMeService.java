package com.medlabel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class LabelMeService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Integer> countLabels(String jsonBody) throws IOException {
        Map<String, Integer> counts = new HashMap<>();
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode shapes = root.get("shapes");
        if (shapes != null && shapes.isArray()) {
            for (JsonNode shape : shapes) {
                String label = shape.hasNonNull("label") ? shape.get("label").asText() : "unknown";
                counts.put(label, counts.getOrDefault(label, 0) + 1);
            }
        }
        return counts;
    }

    public String summarize(Map<String, Integer> counts) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append(entry.getKey()).append(":").append(entry.getValue());
        }
        return builder.toString();
    }
}
