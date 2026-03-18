// Initialize stores with persisted data on client-side

import { useLogger } from "../../utils/logger";

const log = useLogger("Plugin:store-init");

export default defineNuxtPlugin(() => {
  log.info("开始初始化 Store 插件");

  if (import.meta.client) {
    log.debug("客户端环境，初始化 stores");
    const authStore = useAuthStore();
    const userStore = useUserStore();

    log.time("stores-init");

    authStore.init();
    userStore.init();

    log.timeEnd("stores-init");
    log.success("Stores 初始化完成", {
      isAuthenticated: authStore.isAuthenticated,
      hasUser: !!userStore.user,
    });
  } else {
    log.debug("服务端环境，跳过 stores 初始化");
  }
});