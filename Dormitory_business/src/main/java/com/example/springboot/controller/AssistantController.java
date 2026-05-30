package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.service.AssistantService;
import com.example.springboot.service.dto.AssistantRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Resource
    private AssistantService assistantService;

    @PostMapping("/execute")
    public Result<?> execute(@RequestBody AssistantRequest request, HttpSession session) {
        return Result.success(assistantService.execute(request, session));
    }
}
