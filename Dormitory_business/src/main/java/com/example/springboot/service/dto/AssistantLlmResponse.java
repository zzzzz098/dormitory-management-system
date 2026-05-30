package com.example.springboot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantLlmResponse {
    private String message;
    private List<AssistantSuggestedAction> suggestedActions = new ArrayList<>();
}
