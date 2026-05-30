package com.example.springboot.service;

import com.example.springboot.service.dto.AssistantRequest;
import com.example.springboot.service.dto.AssistantResponse;

import javax.servlet.http.HttpSession;

public interface AssistantService {
    AssistantResponse execute(AssistantRequest request, HttpSession session);
}
