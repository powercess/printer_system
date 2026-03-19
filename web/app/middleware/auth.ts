// Authentication middleware - redirects to login if not authenticated

import { useAuthStore } from "../../stores/auth";
import { createMiddlewareLogger } from "../../utils/logger";

const log = createMiddlewareLogger("auth");

export default defineNuxtRouteMiddleware((to) => {
  const fromPath = (to as { from?: { path?: string } }).from?.path || "未知";
  log.execute(fromPath, to.path);

  // Skip on server side to avoid hydration mismatch
  if (import.meta.server) {
    log.trace("服务端跳过中间件执行");
    return;
  }

  const authStore = useAuthStore();

  log.debug("检查认证状态", {
    isAuthenticated: authStore.isAuthenticated,
    hasToken: !!authStore.token,
    isLoggedIn: authStore.isLoggedIn,
  });

  if (!authStore.isLoggedIn) {
    log.redirect(to.path, "/login", "用户未登录");
    return navigateTo("/login");
  }

  log.allow(to.path);
});