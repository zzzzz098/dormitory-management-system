package com.example.springboot.service;

import com.example.springboot.service.dto.AssistantLlmRequest;
import com.example.springboot.service.dto.AssistantLlmResponse;

public interface AssistantLlmClient {
    AssistantLlmResponse complete(AssistantLlmRequest request);
}
