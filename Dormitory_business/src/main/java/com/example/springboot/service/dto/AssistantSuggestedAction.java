package com.example.springboot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantSuggestedAction {
    private String type;
    private String moduleKey;
    private Map<String, Object> payload = new LinkedHashMap<>();
}
