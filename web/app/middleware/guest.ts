// Guest middleware - redirects to home if already authenticated

import { useAuthStore } from "../../stores/auth";

export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore();

  if (authStore.isLoggedIn) {
    return navigateTo("/");
  }
});