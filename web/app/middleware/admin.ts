// Admin middleware - requires admin role

import { useAuthStore } from "../../stores/auth";
import { useUserStore } from "../../stores/user";

export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore();
  const userStore = useUserStore();

  if (!authStore.isLoggedIn) {
    return navigateTo("/login");
  }

  if (!userStore.isAdmin) {
    return navigateTo("/");
  }
});