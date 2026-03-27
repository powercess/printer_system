<template>
  <div class="min-h-screen flex flex-col bg-gray-100 dark:bg-gray-900">
    <!-- Global Header -->
    <AppHeader />

    <!-- Main Content with Sidebar -->
    <div class="flex-1 flex">
      <!-- Sidebar -->
      <aside
        class="w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 flex-shrink-0 hidden lg:flex flex-col"
      >
        <!-- Logo -->
        <div class="flex items-center justify-between h-16 px-4 border-b border-gray-200 dark:border-gray-700">
          <NuxtLink to="/admin" class="flex items-center gap-2">
            <UIcon name="i-heroicons-solid-cog" class="w-6 h-6 text-primary" />
            <span class="font-bold">管理后台</span>
          </NuxtLink>
        </div>

        <!-- Navigation -->
        <nav class="flex-1 px-2 py-4 space-y-1 overflow-y-auto">
          <NuxtLink
            v-for="item in menuItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
            :class="isActive(item.to)
              ? 'bg-primary/10 text-primary font-medium'
              : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
          >
            <UIcon :name="item.icon" class="w-5 h-5" />
            <span>{{ item.label }}</span>
            <UBadge v-if="item.badge" :color="item.badgeColor || 'primary'" variant="subtle" size="xs">
              {{ item.badge }}
            </UBadge>
          </NuxtLink>
        </nav>

        <!-- User Info -->
        <div class="p-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <UIcon name="i-heroicons-solid-user" class="w-5 h-5 text-primary" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium truncate">{{ userStore.username }}</p>
              <p class="text-xs text-gray-500">管理员</p>
            </div>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-heroicons-outline-arrow-right-on-rectangle"
              @click="handleLogout"
            />
          </div>
        </div>
      </aside>

      <!-- Mobile Sidebar Overlay -->
      <div
        v-if="sidebarOpen"
        class="fixed inset-0 z-40 bg-black/50 lg:hidden"
        @click="sidebarOpen = false"
      />

      <!-- Mobile Sidebar -->
      <aside
        class="fixed inset-y-0 left-0 z-50 w-64 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 transform transition-transform duration-200 lg:hidden"
        :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
      >
        <!-- Logo -->
        <div class="flex items-center justify-between h-16 px-4 border-b border-gray-200 dark:border-gray-700">
          <NuxtLink to="/admin" class="flex items-center gap-2">
            <UIcon name="i-heroicons-solid-cog" class="w-8 h-8 text-primary" />
            <span class="font-bold text-lg">管理后台</span>
          </NuxtLink>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-outline-x-mark"
            @click="sidebarOpen = false"
          />
        </div>

        <!-- Navigation -->
        <nav class="flex-1 px-2 py-4 space-y-1 overflow-y-auto">
          <NuxtLink
            v-for="item in menuItems"
            :key="item.to"
            :to="item.to"
            class="flex items-center gap-3 px-3 py-2 rounded-lg transition-colors"
            :class="isActive(item.to)
              ? 'bg-primary/10 text-primary font-medium'
              : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'"
            @click="sidebarOpen = false"
          >
            <UIcon :name="item.icon" class="w-5 h-5" />
            <span>{{ item.label }}</span>
            <UBadge v-if="item.badge" :color="item.badgeColor || 'primary'" variant="subtle" size="xs">
              {{ item.badge }}
            </UBadge>
          </NuxtLink>
        </nav>

        <!-- User Info -->
        <div class="p-4 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <UIcon name="i-heroicons-solid-user" class="w-5 h-5 text-primary" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium truncate">{{ userStore.username }}</p>
              <p class="text-xs text-gray-500">管理员</p>
            </div>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-heroicons-outline-arrow-right-on-rectangle"
              @click="handleLogout"
            />
          </div>
        </div>
      </aside>

      <!-- Page Content -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- Top Bar -->
        <header class="sticky top-0 z-30 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center px-4 gap-4">
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-outline-bars-3"
            class="lg:hidden"
            @click="sidebarOpen = true"
          />
          <h1 class="text-lg font-semibold">{{ pageTitle }}</h1>
          <div class="flex-1" />
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-outline-home"
            to="/"
          >
            返回前台
          </UButton>
        </header>

        <!-- Page Content -->
        <main class="flex-1 p-6 overflow-auto">
          <slot />
        </main>
      </div>
    </div>

    <!-- Global Footer -->
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from "../../stores/auth";
import { useUserStore } from "../../stores/user";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const userStore = useUserStore();

const sidebarOpen = ref(false);

const menuItems: Array<{ to: string; label: string; icon: string; badge?: string; badgeColor?: "error" | "info" | "success" | "primary" | "secondary" | "warning" | "neutral" }> = [
  { to: "/admin", label: "控制台", icon: "i-heroicons-outline-home" },
  { to: "/admin/users", label: "用户管理", icon: "i-heroicons-outline-users" },
  { to: "/admin/orders", label: "订单管理", icon: "i-heroicons-outline-clipboard-document-list" },
  { to: "/admin/files", label: "文件管理", icon: "i-heroicons-outline-document" },
  { to: "/admin/printers", label: "打印机管理", icon: "i-heroicons-outline-printer" },
  { to: "/admin/settings", label: "系统设置", icon: "i-heroicons-outline-cog-6-tooth" },
];

const pageTitle = computed(() => {
  const item = menuItems.find(m => m.to === route.path);
  return item?.label || "管理后台";
});

const isActive = (path: string) => {
  if (path === "/admin") {
    return route.path === "/admin";
  }
  return route.path.startsWith(path);
};

const handleLogout = () => {
  authStore.logout();
  router.push("/login");
};
</script>