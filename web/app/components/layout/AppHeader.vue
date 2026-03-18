<template>
  <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
    <div class="container mx-auto px-4">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <NuxtLink to="/" class="flex items-center space-x-2" @click="handleLogoClick">
          <UIcon name="i-heroicons-printer" class="w-8 h-8 text-primary" />
          <span class="text-xl font-bold text-gray-900 dark:text-white">
            自助打印
          </span>
        </NuxtLink>

        <!-- Navigation -->
        <nav class="hidden md:flex items-center space-x-6">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary transition-colors"
            :class="{ 'text-primary font-medium': isActive(item.to) }"
            @click="handleNavClick(item)"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>

        <!-- User Menu -->
        <div class="flex items-center space-x-4">
          <template v-if="isLoggedIn">
            <div class="flex items-center space-x-3">
              <UBadge color="primary" variant="subtle">
                余额: ¥{{ (balance ?? 0).toFixed(2) }}
              </UBadge>
              <UDropdownMenu :items="userMenuItems">
                <UButton
                  color="neutral"
                  variant="ghost"
                  trailing-icon="i-heroicons-chevron-down"
                >
                  {{ username }}
                </UButton>
              </UDropdownMenu>
            </div>
          </template>
          <template v-else>
            <UButton to="/login" color="primary" variant="soft" @click="handleLoginClick">
              登录
            </UButton>
            <UButton to="/register" color="primary" @click="handleRegisterClick">
              注册
            </UButton>
          </template>

          <!-- Mobile Menu -->
          <UButton
            class="md:hidden"
            color="neutral"
            variant="ghost"
            icon="i-heroicons-bars-3"
            @click="toggleMobileMenu"
          />
        </div>
      </div>

      <!-- Mobile Navigation -->
      <div v-if="mobileMenuOpen" class="md:hidden py-4 border-t border-gray-200 dark:border-gray-700">
        <nav class="flex flex-col space-y-2">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="px-3 py-2 rounded-md text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
            @click="handleMobileNavClick(item)"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useAuthStore } from "../../../stores/auth";
import { useUserStore } from "../../../stores/user";
import { useLogger } from "../../../utils/logger";

const logger = useLogger("AppHeader");

const route = useRoute();
const authStore = useAuthStore();
const userStore = useUserStore();
const mobileMenuOpen = ref(false);

const isLoggedIn = computed(() => authStore.isLoggedIn);
const username = computed(() => userStore.username);
const balance = computed(() => userStore.balance);

const navItems = computed(() => {
  const items = [
    { to: "/", label: "打印" },
    { to: "/files", label: "文件管理" },
    { to: "/orders", label: "订单记录" },
    { to: "/wallet", label: "钱包" },
    { to: "/community", label: "社区" },
  ];

  if (userStore.isAdmin) {
    items.push({ to: "/admin", label: "管理后台" });
  }

  logger.trace("导航菜单项", { items: items.map(i => i.label), isAdmin: userStore.isAdmin });

  return items;
});

const userMenuItems = [
  [
    {
      label: "个人中心",
      icon: "i-heroicons-user",
      onSelect: () => {
        logger.info("用户菜单选择", { item: "个人中心" });
        logger.debug("导航到个人中心");
        navigateTo("/wallet");
      },
    },
  ],
  [
    {
      label: "退出登录",
      icon: "i-heroicons-arrow-right-on-rectangle",
      onSelect: () => {
        logger.info("用户退出登录", { username: userStore.username });
        authStore.logout();
      },
    },
  ],
];

const handleLogoClick = () => {
  logger.debug("点击 Logo");
};

const handleNavClick = (item: { to: string; label: string }) => {
  logger.info("导航点击", { to: item.to, label: item.label });
};

const handleMobileNavClick = (item: { to: string; label: string }) => {
  logger.info("移动端导航点击", { to: item.to, label: item.label });
  mobileMenuOpen.value = false;
};

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value;
  logger.debug("切换移动菜单", { open: mobileMenuOpen.value });
};

const handleLoginClick = () => {
  logger.info("点击登录按钮");
};

const handleRegisterClick = () => {
  logger.info("点击注册按钮");
};

const isActive = (path: string) => {
  if (path === "/") {
    return route.path === "/";
  }
  return route.path.startsWith(path);
};

// 监听路由变化
watch(() => route.path, (newPath, oldPath) => {
  logger.trace("路由变化", { from: oldPath, to: newPath });
});

onMounted(() => {
  logger.debug("AppHeader 已挂载", {
    isLoggedIn: isLoggedIn.value,
    username: username.value,
    isAdmin: userStore.isAdmin,
  });
});
</script>