package com.example.springboot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantResult {
    private String module;
    private String title;
    private String description;
    private String route;
    private Map<String, Object> data = new LinkedHashMap<>();
}
