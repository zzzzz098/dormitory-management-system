package com.example.springboot.service.impl;

import com.example.springboot.entity.Admin;
import com.example.springboot.service.AssistantLlmClient;
import com.example.springboot.service.AssistantService;
import com.example.springboot.service.dto.AssistantAction;
import com.example.springboot.service.dto.AssistantLlmRequest;
import com.example.springboot.service.dto.AssistantLlmResponse;
import com.example.springboot.service.dto.AssistantRequest;
import com.example.springboot.service.dto.AssistantResponse;
import com.example.springboot.service.dto.AssistantSuggestedAction;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantServiceImplTest {

    @Test
    void studentCommandsOnlyReturnStudentAllowedModules() {
        AssistantService service = new AssistantServiceImpl();
        AssistantResponse response = service.execute(request("\u67e5\u5f20\u4e09", "/home"), session("stu"));

        assertThat(response.getActions())
                .extracting(AssistantAction::getRoute)
                .contains("/forumInfo")
                .doesNotContain("/stuInfo", "/visitorInfo", "/accessInfo");
    }

    @Test
    void queryCommandReturnsNavigationAndSearchActions() {
        AssistantService service = new AssistantServiceImpl();
        AssistantResponse response = service.execute(request("\u67e5\u5f20\u4e09", "/home"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("query");
        assertThat(response.getActions())
                .anySatisfy(action -> {
                    assertThat(action.getType()).isEqualTo("navigate");
                    assertThat(action.getRoute()).isEqualTo("/stuInfo");
                })
                .anySatisfy(action -> {
                    assertThat(action.getType()).isEqualTo("setSearch");
                    assertThat(action.getRoute()).isEqualTo("/stuInfo");
                    assertThat(action.getPayload()).containsEntry("search", "\u5f20\u4e09");
                });
        assertThat(response.getResults()).isNotEmpty();
    }

    @Test
    void draftCommandCreatesDraftActionWithoutSaveAction() {
        AssistantService service = new AssistantServiceImpl();
        AssistantResponse response = service.execute(request("\u65b0\u589e\u516c\u544a\uff1a\u4eca\u665a\u505c\u6c34", "/home"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("draft");
        assertThat(response.getActions())
                .anySatisfy(action -> {
                    assertThat(action.getType()).isEqualTo("openDraft");
                    assertThat(action.getRoute()).isEqualTo("/noticeInfo");
                    assertThat(action.getPayload()).containsEntry("title", "\u4eca\u665a\u505c\u6c34");
                });
        assertThat(response.getActions()).extracting(AssistantAction::getType).doesNotContain("save", "delete");
    }

    @Test
    void deleteCommandDoesNotCreateExecutableActions() {
        AssistantService service = new AssistantServiceImpl();
        AssistantResponse response = service.execute(request("\u5220\u9664\u5f20\u4e09", "/stuInfo"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("blocked");
        assertThat(response.getActions()).isEmpty();
        assertThat(response.getMessage()).contains("\u624b\u52a8");
    }

    @Test
    void knownCommandDoesNotCallLlmFallback() {
        RecordingLlmClient llmClient = new RecordingLlmClient("unused");
        AssistantService service = new AssistantServiceImpl(llmClient);

        AssistantResponse response = service.execute(request("\u67e5\u5f20\u4e09", "/home"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("query");
        assertThat(llmClient.callCount).isZero();
    }

    @Test
    void unknownCommandCallsLlmFallback() {
        RecordingLlmClient llmClient = new RecordingLlmClient("\u8fd9\u662f\u5927\u6a21\u578b\u56de\u590d\u3002");
        AssistantService service = new AssistantServiceImpl(llmClient);

        AssistantResponse response = service.execute(request("summarize dorm safety notes", "/home"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("llm");
        assertThat(response.getMessage()).isEqualTo("\u8fd9\u662f\u5927\u6a21\u578b\u56de\u590d\u3002");
        assertThat(llmClient.callCount).isEqualTo(1);
        assertThat(llmClient.lastRequest.getAllowedCapabilities()).isNotEmpty();
    }

    @Test
    void llmSuggestedActionsAreValidatedByRoleAndType() {
        RecordingLlmClient llmClient = new RecordingLlmClient("\u6211\u5efa\u8bae\u6253\u5f00\u53ef\u7528\u6a21\u5757\u3002");
        llmClient.addSuggestion("setSearch", "forum", mapOf("search", "lost card"));
        llmClient.addSuggestion("setSearch", "student", mapOf("search", "\u5f20\u4e09"));
        llmClient.addSuggestion("delete", "forum", mapOf("id", 1));
        AssistantService service = new AssistantServiceImpl(llmClient);

        AssistantResponse response = service.execute(request("find lost card maybe", "/forumInfo"), session("stu"));

        assertThat(response.getIntentType()).isEqualTo("llm_suggested_action");
        assertThat(response.getActions())
                .extracting(AssistantAction::getRoute)
                .contains("/forumInfo")
                .doesNotContain("/stuInfo");
        assertThat(response.getActions()).extracting(AssistantAction::getType).doesNotContain("delete", "save");
    }

    @Test
    void disabledLlmClientReturnsDegradedMessageWithoutActions() {
        AssistantService service = new AssistantServiceImpl(new DisabledAssistantLlmClient());

        AssistantResponse response = service.execute(request("summarize dorm safety notes", "/home"), session("admin"));

        assertThat(response.getIntentType()).isEqualTo("llm");
        assertThat(response.getMessage()).contains("LLM_API_KEY");
        assertThat(response.getActions()).isEmpty();
    }

    private AssistantRequest request(String message, String currentPath) {
        AssistantRequest request = new AssistantRequest();
        request.setMessage(message);
        request.setCurrentPath(currentPath);
        return request;
    }

    private MockHttpSession session(String identity) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("Identity", identity);
        session.setAttribute("User", new Admin());
        return session;
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private static class RecordingLlmClient implements AssistantLlmClient {
        private final AssistantLlmResponse response = new AssistantLlmResponse();
        private int callCount = 0;
        private AssistantLlmRequest lastRequest;

        private RecordingLlmClient(String message) {
            response.setMessage(message);
        }

        @Override
        public AssistantLlmResponse complete(AssistantLlmRequest request) {
            callCount++;
            lastRequest = request;
            return response;
        }

        private void addSuggestion(String type, String moduleKey, Map<String, Object> payload) {
            response.getSuggestedActions().add(new AssistantSuggestedAction(type, moduleKey, payload));
        }
    }

    private static class DisabledAssistantLlmClient implements AssistantLlmClient {
        @Override
        public AssistantLlmResponse complete(AssistantLlmRequest request) {
            return new AssistantLlmResponse("\u672a\u914d\u7f6e LLM_API_KEY\uff0c\u65e0\u6cd5\u8c03\u7528\u5927\u6a21\u578b\u3002", Collections.emptyList());
        }
    }
}
