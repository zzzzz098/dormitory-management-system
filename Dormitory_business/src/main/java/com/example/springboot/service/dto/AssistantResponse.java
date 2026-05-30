package com.example.springboot.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssistantResponse {
    private String message;
    private String intentType;
    private List<AssistantResult> results = new ArrayList<>();
    private List<AssistantAction> actions = new ArrayList<>();
}
