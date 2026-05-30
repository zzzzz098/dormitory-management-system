package com.example.springboot.service.impl;

import com.example.springboot.service.AssistantLlmClient;
import com.example.springboot.service.AssistantService;
import com.example.springboot.service.dto.AssistantAction;
import com.example.springboot.service.dto.AssistantLlmRequest;
import com.example.springboot.service.dto.AssistantLlmResponse;
import com.example.springboot.service.dto.AssistantRequest;
import com.example.springboot.service.dto.AssistantResponse;
import com.example.springboot.service.dto.AssistantResult;
import com.example.springboot.service.dto.AssistantSuggestedAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AssistantServiceImpl implements AssistantService {

    private static final String ADMIN = "admin";
    private static final String DORM_MANAGER = "dormManager";
    private static final String STUDENT = "stu";

    private final List<Capability> capabilities = buildCapabilities();
    private final AssistantLlmClient llmClient;

    public AssistantServiceImpl() {
        this(new DisabledAssistantLlmClient());
    }

    @Autowired
    public AssistantServiceImpl(AssistantLlmClient llmClient) {
        this.llmClient = llmClient == null ? new DisabledAssistantLlmClient() : llmClient;
    }

    @Override
    public AssistantResponse execute(AssistantRequest request, HttpSession session) {
        String message = normalize(request == null ? "" : request.getMessage());
        AssistantResponse response = new AssistantResponse();
        String role = role(session);

        if (message.isEmpty()) {
            response.setIntentType("help");
            response.setMessage("\u53ef\u4ee5\u8bd5\u8bd5\uff1a\u67e5\u5f20\u4e09\u30013\u53f7\u697c\u7a7a\u5e8a\u4f4d\u3001\u6700\u8fd1\u665a\u5f52\u544a\u8b66\u3001\u65b0\u589e\u516c\u544a\uff1a\u4eca\u665a\u505c\u6c34\u3002");
            response.setActions(suggestionActions(role));
            return response;
        }
        if (isDeleteCommand(message)) {
            response.setIntentType("blocked");
            response.setMessage("\u5220\u9664\u64cd\u4f5c\u98ce\u9669\u8f83\u9ad8\uff0c\u8bf7\u5728\u5bf9\u5e94\u9875\u9762\u4e2d\u624b\u52a8\u5220\u9664\u3002");
            return response;
        }

        Intent intent = detectIntent(message);
        List<Capability> matched = matchCapabilities(message, role, intent);
        if (!matched.isEmpty()) {
            if ("draft".equals(intent.type)) {
                buildDraftResponse(response, message, intent, matched.get(0));
            } else {
                buildQueryResponse(response, message, intent, matched);
            }
            return response;
        }

        return buildLlmResponse(request, message, role);
    }

    private AssistantResponse buildLlmResponse(AssistantRequest request, String message, String role) {
        AssistantLlmRequest llmRequest = new AssistantLlmRequest(
                message,
                role,
                request == null ? "" : normalize(request.getCurrentPath()),
                allowedCapabilitySummaries(role)
        );
        AssistantLlmResponse llmResponse = llmClient.complete(llmRequest);
        AssistantResponse response = new AssistantResponse();
        response.setIntentType("llm");
        response.setMessage(llmResponse == null || normalize(llmResponse.getMessage()).isEmpty()
                ? "\u5927\u6a21\u578b\u6682\u65f6\u6ca1\u6709\u8fd4\u56de\u5185\u5bb9\u3002"
                : llmResponse.getMessage());

        if (llmResponse != null) {
            for (AssistantSuggestedAction suggestion : llmResponse.getSuggestedActions()) {
                addValidatedSuggestion(response, suggestion, role);
            }
        }
        if (!response.getActions().isEmpty()) {
            response.setIntentType("llm_suggested_action");
        }
        return response;
    }

    private void addValidatedSuggestion(AssistantResponse response, AssistantSuggestedAction suggestion, String role) {
        if (suggestion == null || suggestion.getType() == null || suggestion.getModuleKey() == null) {
            return;
        }
        String type = suggestion.getType();
        if (!"navigate".equals(type) && !"setSearch".equals(type) && !"openDraft".equals(type)) {
            return;
        }
        Capability capability = capabilityByKey(suggestion.getModuleKey());
        if (capability == null || !capability.roles.contains(role)) {
            return;
        }
        if ("setSearch".equals(type) && !capability.searchable) {
            return;
        }
        if ("openDraft".equals(type) && !capability.draftable) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (suggestion.getPayload() != null) {
            payload.putAll(suggestion.getPayload());
        }
        AssistantAction action = action(type, labelFor(type, capability), capability, payload);
        response.getActions().add(action);
        response.getResults().add(new AssistantResult(
                capability.name,
                capability.name + "\u5efa\u8bae",
                "\u5927\u6a21\u578b\u5efa\u8bae\u7684\u64cd\u4f5c\u5df2\u901a\u8fc7\u6743\u9650\u6821\u9a8c\u3002",
                capability.route,
                new LinkedHashMap<>(action.getPayload())
        ));
    }

    private String labelFor(String type, Capability capability) {
        if ("setSearch".equals(type)) {
            return "\u641c\u7d22" + capability.name;
        }
        if ("openDraft".equals(type)) {
            return "\u751f\u6210\u8349\u7a3f";
        }
        return "\u6253\u5f00" + capability.name;
    }

    private void buildQueryResponse(AssistantResponse response, String message, Intent intent, List<Capability> matched) {
        response.setIntentType("query");
        String keyword = extractQueryKeyword(message, intent);
        for (Capability capability : matched) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("search", keyword);
            data.put("moduleKey", capability.key);
            response.getResults().add(new AssistantResult(
                    capability.name,
                    capability.name + "\u67e5\u8be2",
                    keyword.isEmpty() ? "\u6253\u5f00" + capability.name + "\u9875\u9762\u67e5\u770b\u6570\u636e" : "\u5728" + capability.name + "\u4e2d\u641c\u7d22\uff1a" + keyword,
                    capability.route,
                    data
            ));
            response.getActions().add(action("navigate", "\u6253\u5f00" + capability.name, capability, Collections.emptyMap()));
            if (capability.searchable) {
                response.getActions().add(action("setSearch", "\u641c\u7d22" + capability.name, capability, mapOf("search", keyword)));
            }
        }
        response.setMessage("\u4e3a\u4f60\u627e\u5230 " + matched.size() + " \u4e2a\u53ef\u7528\u6a21\u5757\uff0c\u5df2\u51c6\u5907\u597d\u8df3\u8f6c\u548c\u5feb\u901f\u641c\u7d22\u3002");
    }

    private void buildDraftResponse(AssistantResponse response, String message, Intent intent, Capability capability) {
        response.setIntentType("draft");
        Map<String, Object> draft = extractDraftPayload(message, capability, intent);
        response.getResults().add(new AssistantResult(
                capability.name,
                capability.name + "\u8349\u7a3f",
                "\u5df2\u6839\u636e\u4f60\u7684\u63cf\u8ff0\u751f\u6210\u8349\u7a3f\uff0c\u8bf7\u5230\u9875\u9762\u786e\u8ba4\u540e\u518d\u4fdd\u5b58\u3002",
                capability.route,
                draft
        ));
        response.getActions().add(action("navigate", "\u6253\u5f00" + capability.name, capability, Collections.emptyMap()));
        response.getActions().add(action("openDraft", "\u751f\u6210\u8349\u7a3f", capability, draft));
        response.setMessage("\u5df2\u51c6\u5907" + capability.name + "\u8349\u7a3f\uff0c\u4e0d\u4f1a\u81ea\u52a8\u4fdd\u5b58\uff0c\u8bf7\u786e\u8ba4\u540e\u624b\u52a8\u63d0\u4ea4\u3002");
    }

    private List<Capability> matchCapabilities(String message, String role, Intent intent) {
        List<Capability> matched = new ArrayList<>();
        for (Capability capability : capabilities) {
            if (!capability.roles.contains(role)) {
                continue;
            }
            if ("draft".equals(intent.type) && !capability.draftable) {
                continue;
            }
            if (capability.matches(message) || capability.key.equals(intent.targetKey)) {
                matched.add(capability);
            }
        }
        if (matched.isEmpty() && "query".equals(intent.type) && intent.localCommand) {
            for (Capability capability : capabilities) {
                if (capability.roles.contains(role) && capability.searchable) {
                    matched.add(capability);
                }
            }
        }
        return matched.size() > 4 ? matched.subList(0, 4) : matched;
    }

    private Intent detectIntent(String message) {
        boolean draft = message.contains("\u65b0\u589e") || message.contains("\u6dfb\u52a0") || message.contains("\u7533\u8bf7") || message.contains("\u767b\u8bb0");
        boolean query = message.startsWith("\u67e5") || message.startsWith("\u67e5\u8be2") || message.startsWith("\u641c") || message.contains("\u6700\u8fd1") || message.contains("\u6700\u65b0");
        return new Intent(draft ? "draft" : "query", detectTargetKey(message), draft || query);
    }

    private String detectTargetKey(String message) {
        for (Capability capability : capabilities) {
            if (capability.matches(message)) {
                return capability.key;
            }
        }
        return "";
    }

    private Map<String, Object> extractDraftPayload(String message, Capability capability, Intent intent) {
        String content = stripDraftPrefix(message);
        Map<String, Object> payload = new LinkedHashMap<>();
        if ("notice".equals(capability.key)) {
            payload.put("title", content);
            payload.put("content", content);
        } else if ("repair".equals(capability.key) || "applyRepair".equals(capability.key)) {
            payload.put("title", content);
            payload.put("content", content);
            Integer room = firstNumber(message);
            if (room != null) {
                payload.put("dormRoomId", room);
            }
        } else if ("forum".equals(capability.key) || "forumManage".equals(capability.key)) {
            payload.put("title", content);
            payload.put("content", content);
        } else if ("access".equals(capability.key) || "visitor".equals(capability.key)) {
            payload.put("remark", content);
            Integer room = firstNumber(message);
            if (room != null) {
                payload.put("dormRoomId", room);
            }
        } else {
            payload.put("keyword", content);
        }
        payload.put("moduleKey", capability.key);
        payload.put("intentTarget", intent.targetKey);
        return payload;
    }

    private String stripDraftPrefix(String message) {
        String result = message.replaceFirst("^(\u8bf7\u5e2e\u6211|\u5e2e\u6211|\u6211\u8981|\u6211\u60f3)?(\u65b0\u589e|\u6dfb\u52a0|\u7533\u8bf7|\u767b\u8bb0)", "");
        int colon = Math.max(result.lastIndexOf("\uff1a"), result.lastIndexOf(":"));
        if (colon >= 0 && colon + 1 < result.length()) {
            result = result.substring(colon + 1);
        }
        return result.trim().isEmpty() ? message.trim() : result.trim();
    }

    private String extractQueryKeyword(String message, Intent intent) {
        String result = message.replaceFirst("^(\u5e2e\u6211|\u8bf7\u5e2e\u6211)?(\u67e5\u8be2|\u67e5\u627e|\u641c\u7d22|\u67e5|\u770b\u4e00\u4e0b|\u770b\u770b)", "");
        result = result.replaceFirst("^(\u67e5\u8be2|\u67e5\u627e|\u641c\u7d22|\u67e5|\u770b\u4e00\u4e0b|\u770b\u770b)", "");
        for (Capability capability : capabilities) {
            for (String keyword : capability.keywords) {
                if (!keyword.equals(result)) {
                    result = result.replace(keyword, "");
                }
            }
        }
        result = result.replace("\u6700\u8fd1", "").replace("\u6700\u65b0", "").trim();
        return result.isEmpty() ? message.trim() : result;
    }

    private boolean isDeleteCommand(String message) {
        return message.contains("\u5220\u9664") || message.contains("\u79fb\u9664") || message.toLowerCase(Locale.ROOT).contains("delete");
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim();
    }

    private String role(HttpSession session) {
        Object identity = session == null ? null : session.getAttribute("Identity");
        return identity == null ? "" : identity.toString();
    }

    private AssistantAction action(String type, String label, Capability capability, Map<String, Object> payload) {
        Map<String, Object> actionPayload = new LinkedHashMap<>(payload);
        actionPayload.putIfAbsent("moduleKey", capability.key);
        return new AssistantAction(type, label, capability.name, capability.route, actionPayload);
    }

    private List<AssistantAction> suggestionActions(String role) {
        List<AssistantAction> actions = new ArrayList<>();
        for (Capability capability : capabilities) {
            if (capability.roles.contains(role)) {
                actions.add(action("navigate", "\u6253\u5f00" + capability.name, capability, Collections.emptyMap()));
            }
            if (actions.size() == 3) {
                break;
            }
        }
        return actions;
    }

    private List<Map<String, Object>> allowedCapabilitySummaries(String role) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Capability capability : capabilities) {
            if (!capability.roles.contains(role)) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("moduleKey", capability.key);
            summary.put("name", capability.name);
            summary.put("route", capability.route);
            summary.put("searchable", capability.searchable);
            summary.put("draftable", capability.draftable);
            summaries.add(summary);
        }
        return summaries;
    }

    private Capability capabilityByKey(String key) {
        for (Capability capability : capabilities) {
            if (capability.key.equals(key)) {
                return capability;
            }
        }
        return null;
    }

    private Integer firstNumber(String message) {
        String digits = message.replaceAll("^.*?(\\d+).*$", "$1");
        if (digits.equals(message)) {
            return null;
        }
        try {
            return Integer.valueOf(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private List<Capability> buildCapabilities() {
        List<Capability> list = new ArrayList<>();
        Set<String> managerRoles = setOf(ADMIN, DORM_MANAGER);
        list.add(new Capability("student", "\u5b66\u751f\u4fe1\u606f", "/stuInfo", setOf(ADMIN), true, true, "\u5b66\u751f", "\u5b66\u53f7", "\u59d3\u540d", "\u5f20\u4e09"));
        list.add(new Capability("dormManager", "\u5bbf\u7ba1\u4fe1\u606f", "/dormManagerInfo", setOf(ADMIN), true, true, "\u5bbf\u7ba1", "\u7ba1\u7406\u5458"));
        list.add(new Capability("building", "\u697c\u5b87\u4fe1\u606f", "/buildingInfo", managerRoles, true, true, "\u697c\u5b87", "\u697c\u680b", "\u53f7\u697c", "\u5bbf\u820d\u697c"));
        list.add(new Capability("room", "\u623f\u95f4\u4fe1\u606f", "/roomInfo", managerRoles, true, true, "\u623f\u95f4", "\u5bbf\u820d", "\u7a7a\u5e8a", "\u5e8a\u4f4d", "\u53f7\u697c"));
        list.add(new Capability("utility", "\u6c34\u7535\u7ba1\u7406", "/utilityInfo", managerRoles, true, false, "\u6c34\u7535", "\u7528\u7535", "\u7528\u6c34", "\u7535\u8d39", "\u544a\u8b66"));
        list.add(new Capability("access", "\u51fa\u5165\u7ba1\u7406", "/accessInfo", managerRoles, true, true, "\u51fa\u5165", "\u665a\u5f52", "\u8fd4\u6821", "\u5927\u4ef6", "\u7269\u54c1"));
        list.add(new Capability("notice", "\u516c\u544a\u4fe1\u606f", "/noticeInfo", setOf(ADMIN), true, true, "\u516c\u544a", "\u901a\u77e5"));
        list.add(new Capability("forumManage", "\u5e16\u5b50\u7ba1\u7406", "/forumManageInfo", managerRoles, true, true, "\u5e16\u5b50", "\u8bba\u575b", "\u793e\u533a"));
        list.add(new Capability("adjustRoom", "\u8c03\u5bbf\u7533\u8bf7", "/adjustRoomInfo", managerRoles, true, true, "\u8c03\u5bbf", "\u6362\u5bbf", "\u6362\u623f"));
        list.add(new Capability("repair", "\u62a5\u4fee\u4fe1\u606f", "/repairInfo", managerRoles, true, true, "\u62a5\u4fee", "\u7ef4\u4fee", "\u4fee\u7406"));
        list.add(new Capability("visitor", "\u8bbf\u5ba2\u7ba1\u7406", "/visitorInfo", managerRoles, true, true, "\u8bbf\u5ba2", "\u6765\u8bbf"));
        list.add(new Capability("self", "\u4e2a\u4eba\u4fe1\u606f", "/selfInfo", setOf(ADMIN, DORM_MANAGER, STUDENT), false, false, "\u4e2a\u4eba", "\u8d44\u6599", "\u8d26\u6237"));
        list.add(new Capability("myRoom", "\u6211\u7684\u5bbf\u820d", "/myRoomInfo", setOf(STUDENT), false, false, "\u6211\u7684\u5bbf\u820d", "\u5bbf\u820d"));
        list.add(new Capability("forum", "\u6821\u56ed\u793e\u533a", "/forumInfo", setOf(STUDENT), true, true, "\u8bba\u575b", "\u793e\u533a", "\u5e16\u5b50"));
        list.add(new Capability("applyRepair", "\u62a5\u4fee\u7533\u8bf7", "/applyRepairInfo", setOf(STUDENT), true, true, "\u62a5\u4fee", "\u7ef4\u4fee", "\u4fee\u7406"));
        list.add(new Capability("applyChangeRoom", "\u8c03\u5bbf\u7533\u8bf7", "/applyChangeRoom", setOf(STUDENT), true, true, "\u8c03\u5bbf", "\u6362\u5bbf", "\u6362\u623f"));
        return list;
    }

    private Set<String> setOf(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static class Intent {
        private final String type;
        private final String targetKey;
        private final boolean localCommand;

        private Intent(String type, String targetKey, boolean localCommand) {
            this.type = type;
            this.targetKey = targetKey;
            this.localCommand = localCommand;
        }
    }

    private static class Capability {
        private final String key;
        private final String name;
        private final String route;
        private final Set<String> roles;
        private final boolean searchable;
        private final boolean draftable;
        private final List<String> keywords;

        private Capability(String key, String name, String route, Set<String> roles, boolean searchable, boolean draftable, String... keywords) {
            this.key = key;
            this.name = name;
            this.route = route;
            this.roles = roles;
            this.searchable = searchable;
            this.draftable = draftable;
            this.keywords = Arrays.asList(keywords);
        }

        private boolean matches(String message) {
            for (String keyword : keywords) {
                if (message.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class DisabledAssistantLlmClient implements AssistantLlmClient {
        @Override
        public AssistantLlmResponse complete(AssistantLlmRequest request) {
            return new AssistantLlmResponse("\u672a\u914d\u7f6e LLM_API_KEY\uff0c\u65e0\u6cd5\u8c03\u7528\u5927\u6a21\u578b\u3002", Collections.emptyList());
        }
    }
}
