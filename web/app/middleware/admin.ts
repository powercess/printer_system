// Admin middleware - requires admin role

import { useAuthStore } from "../../stores/auth";
import { useUserStore } from "../../stores/user";
import { createMiddlewareLogger } from "../../utils/logger";

const log = createMiddlewareLogger("admin");

export default defineNuxtRouteMiddleware((to) => {
  log.execute(to.from?.path || "未知", to.path);

  // Skip on server side to avoid hydration mismatch
  if (import.meta.server) {
    log.trace("服务端跳过中间件执行");
    return;
  }

  const authStore = useAuthStore();
  const userStore = useUserStore();

  log.debug("检查认证和管理员状态", {
    isAuthenticated: authStore.isAuthenticated,
    hasToken: !!authStore.token,
    isLoggedIn: authStore.isLoggedIn,
    isAdmin: userStore.isAdmin,
    username: userStore.username,
  });

  if (!authStore.isLoggedIn) {
    log.redirect(to.path, "/login", "用户未登录");
    return navigateTo("/login");
  }

  if (!userStore.isAdmin) {
    log.redirect(to.path, "/", "用户不是管理员");
    return navigateTo("/");
  }

  log.allow(to.path);
});