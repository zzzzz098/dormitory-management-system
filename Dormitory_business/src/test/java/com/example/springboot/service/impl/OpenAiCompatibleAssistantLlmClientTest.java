package com.example.springboot.service.impl;

import com.example.springboot.service.dto.AssistantLlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleAssistantLlmClientTest {

    @Test
    void parseContentTreatsEmptyStringPayloadAsEmptyObject() throws Exception {
        OpenAiCompatibleAssistantLlmClient client = new OpenAiCompatibleAssistantLlmClient();
        String content = "{\"message\":\"ok\",\"suggestedActions\":[{\"type\":\"navigate\",\"moduleKey\":\"student\",\"payload\":\"\"}]}";

        AssistantLlmResponse response = ReflectionTestUtils.invokeMethod(client, "parseContent", content);

        assertThat(response.getMessage()).isEqualTo("ok");
        assertThat(response.getSuggestedActions()).hasSize(1);
        assertThat(response.getSuggestedActions().get(0).getPayload()).isEmpty();
    }

    @Test
    void parseContentFallsBackToPlainTextWhenModelDoesNotReturnJson() throws Exception {
        OpenAiCompatibleAssistantLlmClient client = new OpenAiCompatibleAssistantLlmClient();

        AssistantLlmResponse response = ReflectionTestUtils.invokeMethod(client, "parseContent", "您好，我可以帮您处理宿舍问题。");

        assertThat(response.getMessage()).isEqualTo("您好，我可以帮您处理宿舍问题。");
        assertThat(response.getSuggestedActions()).isEmpty();
    }
}
