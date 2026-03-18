// Admin middleware - requires admin role

import { useAuthStore } from "../../stores/auth";
import { useUserStore } from "../../stores/user";

export default defineNuxtRouteMiddleware(() => {
  // Skip on server side to avoid hydration mismatch
  if (import.meta.server) {
    return;
  }

  const authStore = useAuthStore();
  const userStore = useUserStore();

  if (!authStore.isLoggedIn) {
    return navigateTo("/login");
  }

  if (!userStore.isAdmin) {
    return navigateTo("/");
  }
});