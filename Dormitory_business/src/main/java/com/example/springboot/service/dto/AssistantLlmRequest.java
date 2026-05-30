package com.example.springboot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantLlmRequest {
    private String message;
    private String role;
    private String currentPath;
    private List<Map<String, Object>> allowedCapabilities = new ArrayList<>();
}
