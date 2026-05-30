<template>
  <div class="assistant-root">
    <transition name="assistant-panel">
      <section v-if="open" class="assistant-panel">
        <header class="assistant-head">
          <div>
            <strong>AI 小助手</strong>
            <span>混合智能助手</span>
          </div>
          <el-button circle link :icon="'Close'" @click="open = false"/>
        </header>

        <div ref="messagesBox" class="assistant-messages">
          <div
              v-for="(item, index) in messages"
              :key="index"
              class="assistant-message"
              :class="'is-' + item.role"
          >
            <div class="message-bubble">
              <p>{{ item.text }}</p>
              <div v-if="item.results && item.results.length" class="result-list">
                <button
                    v-for="result in item.results"
                    :key="result.module + result.title"
                    type="button"
                    class="result-card"
                    @click="goRoute(result.route)"
                >
                  <strong>{{ result.title }}</strong>
                  <span>{{ result.description }}</span>
                </button>
              </div>
              <div v-if="item.actions && item.actions.length" class="action-list">
                <el-button
                    v-for="(action, actionIndex) in item.actions"
                    :key="action.type + action.route + actionIndex"
                    size="small"
                    type="primary"
                    plain
                    @click="runAction(action)"
                >
                  {{ action.label }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="assistant-suggestions">
          <button v-for="tip in tips" :key="tip" type="button" @click="send(tip)">{{ tip }}</button>
        </div>

        <footer class="assistant-input">
          <el-input
              v-model="draft"
              placeholder="例如：查张三 / 新增公告：今晚停水"
              clearable
              :disabled="loading"
              @keyup.enter="send()"
          />
          <el-button type="primary" :loading="loading" :icon="'Promotion'" @click="send()">发送</el-button>
        </footer>
      </section>
    </transition>

    <button class="assistant-fab" type="button" @click="open = !open">
      <el-icon :size="24"><service/></el-icon>
      <span v-if="!open">AI</span>
    </button>
  </div>
</template>

<script>
import {ElMessage} from "element-plus";
import {executeAssistant} from "@/utils/assistant";
import {emitAssistantAction} from "@/utils/assistantBridge";

export default {
  name: "AssistantWidget",
  data() {
    return {
      open: false,
      loading: false,
      draft: "",
      tips: ["查张三", "3号楼空床位", "最近晚归告警", "新增公告：今晚停水"],
      messages: [
        {
          role: "assistant",
          text: "我可以帮你快速查询、跳转页面，或生成需要你确认保存的表单草稿。",
          results: [],
          actions: [],
        },
      ],
    };
  },
  methods: {
    async send(preset) {
      const text = (preset || this.draft || "").trim();
      if (!text || this.loading) {
        return;
      }
      this.draft = "";
      this.messages.push({role: "user", text});
      this.loading = true;
      this.scrollBottom();
      try {
        const res = await executeAssistant({
          message: text,
          currentPath: this.$route.path,
          pageContext: {routeName: this.$route.name},
        });
        if (res.code === "0") {
          const data = res.data || {};
          this.messages.push({
            role: "assistant",
            text: data.message || "已处理。",
            results: data.results || [],
            actions: data.actions || [],
          });
        } else {
          this.messages.push({role: "assistant", text: res.msg || "助手暂时不可用。"});
        }
      } catch (error) {
        this.messages.push({role: "assistant", text: "请求失败，请稍后再试。"});
      } finally {
        this.loading = false;
        this.scrollBottom();
      }
    },
    async runAction(action) {
      if (!action || !action.route) {
        return;
      }
      if (action.type === "navigate") {
        await this.goRoute(action.route);
        return;
      }
      await this.goRoute(action.route);
      this.$nextTick(() => {
        setTimeout(() => {
          const handled = emitAssistantAction(action.route, action);
          if (!handled) {
            ElMessage({message: "已打开页面，请在当前页面手动处理。", type: "info"});
          }
        }, 120);
      });
    },
    async goRoute(route) {
      if (route && this.$route.path !== route) {
        await this.$router.push(route);
      }
    },
    scrollBottom() {
      this.$nextTick(() => {
        const box = this.$refs.messagesBox;
        if (box) {
          box.scrollTop = box.scrollHeight;
        }
      });
    },
  },
};
</script>

<style scoped>
.assistant-root {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 3000;
}

.assistant-fab {
  width: 58px;
  height: 58px;
  border: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, #23b7a4, #4e9df6);
  color: #fff;
  box-shadow: 0 14px 34px rgba(35, 183, 164, .34);
  display: grid;
  place-items: center;
  cursor: pointer;
}

.assistant-fab span {
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
}

.assistant-panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: min(390px, calc(100vw - 28px));
  height: min(620px, calc(100vh - 110px));
  background: rgba(255, 255, 255, .96);
  border: 1px solid rgba(85,145,158,.18);
  border-radius: 8px;
  box-shadow: 0 22px 54px rgba(20,83,92,.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(16px);
}

.assistant-head {
  height: 58px;
  padding: 10px 12px 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(180deg, rgba(234,249,245,.95), rgba(255,255,255,.92));
}

.assistant-head strong,
.assistant-head span {
  display: block;
}

.assistant-head strong {
  color: var(--text-title);
  font-size: 15px;
}

.assistant-head span {
  color: var(--text-muted);
  font-size: 12px;
}

.assistant-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assistant-message {
  display: flex;
}

.assistant-message.is-user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 88%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f7fcff;
  color: #111;
  font-size: 13px;
  line-height: 1.55;
  border: 1px solid rgba(85,145,158,.12);
}

.is-user .message-bubble {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.message-bubble p {
  margin: 0;
}

.result-list,
.action-list {
  margin-top: 9px;
}

.result-list {
  display: grid;
  gap: 7px;
}

.result-card {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(35,183,164,.18);
  background: rgba(255,255,255,.92);
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
}

.result-card strong,
.result-card span {
  display: block;
}

.result-card strong {
  color: #111;
  font-size: 13px;
}

.result-card span {
  color: #111;
  font-size: 12px;
  margin-top: 2px;
}

.action-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  align-items: center;
}

.action-list :deep(.el-button) {
  width: 100%;
  min-width: 0;
  margin-left: 0 !important;
  justify-content: center;
  color: #111 !important;
}

.action-list :deep(.el-button + .el-button) {
  margin-left: 0 !important;
}

.assistant-suggestions {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 8px 12px;
  border-top: 1px solid rgba(85,145,158,.10);
}

.assistant-suggestions button {
  flex: 0 0 auto;
  border: 1px solid rgba(35,183,164,.22);
  background: rgba(35,183,164,.08);
  color: #0b756a;
  border-radius: 6px;
  padding: 5px 8px;
  font-size: 12px;
  cursor: pointer;
}

.assistant-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--border-light);
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition: opacity .18s var(--ease), transform .18s var(--ease);
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
