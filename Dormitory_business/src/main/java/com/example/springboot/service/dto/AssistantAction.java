package com.example.springboot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantAction {
    private String type;
    private String label;
    private String module;
    private String route;
    private Map<String, Object> payload = new LinkedHashMap<>();
}
