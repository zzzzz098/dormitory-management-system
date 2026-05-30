const listeners = {};

export function onAssistantAction(route, handler) {
    if (!listeners[route]) {
        listeners[route] = new Set();
    }
    listeners[route].add(handler);
    return () => listeners[route] && listeners[route].delete(handler);
}

export function emitAssistantAction(route, action) {
    const routeListeners = listeners[route];
    if (!routeListeners || routeListeners.size === 0) {
        return false;
    }
    routeListeners.forEach((handler) => handler(action));
    return true;
}

export function installAssistantBridge(app) {
    app.mixin({
        mounted() {
            const path = this.$route && this.$route.path;
            if (!path || this.$options.name === "AssistantWidget") {
                return;
            }
            const canSearch = Object.prototype.hasOwnProperty.call(this.$data || {}, "search") && typeof this.load === "function";
            const canDraft = Object.prototype.hasOwnProperty.call(this.$data || {}, "form") && typeof this.add === "function";
            const hasCustomDraft = typeof this.assistantOpenDraft === "function";
            if (!canSearch && !canDraft && !hasCustomDraft) {
                return;
            }
            this.__assistantOff = onAssistantAction(path, (action) => {
                const payload = action.payload || {};
                if (action.type === "setSearch" && canSearch) {
                    this.search = payload.search || "";
                    this.currentPage = 1;
                    this.load();
                }
                if (action.type === "openDraft") {
                    if (hasCustomDraft) {
                        this.assistantOpenDraft(payload);
                        return;
                    }
                    if (canDraft) {
                        this.add();
                        this.$nextTick(() => {
                            this.form = Object.assign({}, this.form || {}, payload);
                        });
                    }
                }
            });
        },
        beforeUnmount() {
            if (this.__assistantOff) {
                this.__assistantOff();
                this.__assistantOff = null;
            }
        },
    });
}
