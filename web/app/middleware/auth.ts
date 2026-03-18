// Authentication middleware - redirects to login if not authenticated

import { useAuthStore } from "../../stores/auth";

export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore();

  if (!authStore.isLoggedIn) {
    return navigateTo("/login");
  }
});
