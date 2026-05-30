import request from "@/utils/request";

export function executeAssistant(payload) {
    return request.post("/assistant/execute", payload);
}
