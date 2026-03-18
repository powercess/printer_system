// Guest middleware - redirects to home if already authenticated

import { useAuthStore } from "../../stores/auth";

export default defineNuxtRouteMiddleware(() => {
  // Skip on server side to avoid hydration mismatch
  if (import.meta.server) {
    return;
  }

  const authStore = useAuthStore();

  if (authStore.isLoggedIn) {
    return navigateTo("/");
  }
});