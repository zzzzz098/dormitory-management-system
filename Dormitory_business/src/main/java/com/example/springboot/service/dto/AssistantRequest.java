package com.example.springboot.service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AssistantRequest {
    private String message;
    private String currentPath;
    private Map<String, Object> pageContext;
}
