// Initialize stores with persisted data on client-side

export default defineNuxtPlugin(() => {
  if (import.meta.client) {
    const authStore = useAuthStore();
    const userStore = useUserStore();

    authStore.init();
    userStore.init();
  }
});