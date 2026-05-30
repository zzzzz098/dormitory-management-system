package com.example.springboot.service.impl;

import com.example.springboot.service.AssistantLlmClient;
import com.example.springboot.service.dto.AssistantLlmRequest;
import com.example.springboot.service.dto.AssistantLlmResponse;
import com.example.springboot.service.dto.AssistantSuggestedAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleAssistantLlmClient implements AssistantLlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${LLM_API_KEY:}")
    private String apiKey;

    @Value("${LLM_BASE_URL:https://api.openai.com}")
    private String baseUrl;

    @Value("${LLM_MODEL:gpt-4o-mini}")
    private String model;

    @Override
    public AssistantLlmResponse complete(AssistantLlmRequest request) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new AssistantLlmResponse("\u672a\u914d\u7f6e LLM_API_KEY\uff0c\u65e0\u6cd5\u8c03\u7528\u5927\u6a21\u578b\u3002", new ArrayList<>());
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("temperature", 0.2);
            body.put("messages", messages(request));

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint(), new HttpEntity<>(body, headers), Map.class);
            Object content = extractContent(response.getBody());
            if (content == null) {
                return new AssistantLlmResponse("\u5927\u6a21\u578b\u6682\u65f6\u6ca1\u6709\u8fd4\u56de\u5185\u5bb9\u3002", new ArrayList<>());
            }
            return parseContent(content.toString());
        } catch (Exception e) {
            return new AssistantLlmResponse("\u5927\u6a21\u578b\u8bf7\u6c42\u5931\u8d25\uff1a" + e.getMessage(), new ArrayList<>());
        }
    }

    private String endpoint() {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }

    private List<Map<String, String>> messages(AssistantLlmRequest request) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system",
                "You are an assistant inside a dormitory management system. "
                        + "Reply in Chinese. Return only compact JSON with keys: message and suggestedActions. "
                        + "suggestedActions is an array. Each item may use type navigate, setSearch, or openDraft only. "
                        + "Each item must include moduleKey and payload. Never suggest delete or save. "
                        + "Allowed modules: " + objectMapper.writeValueAsString(request.getAllowedCapabilities())));
        messages.add(message("user", request.getMessage()));
        return messages;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    @SuppressWarnings("rawtypes")
    private Object extractContent(Map body) {
        if (body == null) {
            return null;
        }
        Object choices = body.get("choices");
        if (!(choices instanceof List) || ((List) choices).isEmpty()) {
            return null;
        }
        Object first = ((List) choices).get(0);
        if (!(first instanceof Map)) {
            return null;
        }
        Object message = ((Map) first).get("message");
        if (!(message instanceof Map)) {
            return null;
        }
        return ((Map) message).get("content");
    }

    private AssistantLlmResponse parseContent(String content) throws Exception {
        String json = stripCodeFence(content);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception ignored) {
            return new AssistantLlmResponse(content, new ArrayList<>());
        }
        AssistantLlmResponse response = new AssistantLlmResponse();
        response.setMessage(root.path("message").asText(content));
        JsonNode actions = root.path("suggestedActions");
        if (actions.isArray()) {
            for (JsonNode node : actions) {
                AssistantSuggestedAction action = new AssistantSuggestedAction();
                action.setType(node.path("type").asText(""));
                action.setModuleKey(node.path("moduleKey").asText(""));
                action.setPayload(payloadMap(node.path("payload")));
                response.getSuggestedActions().add(action);
            }
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadMap(JsonNode payloadNode) {
        if (payloadNode == null || !payloadNode.isObject()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> payload = objectMapper.convertValue(payloadNode, Map.class);
        return payload == null ? new LinkedHashMap<>() : payload;
    }

    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                return trimmed.substring(firstBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
