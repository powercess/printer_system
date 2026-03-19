<template>
  <footer class="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700">
    <div class="container mx-auto px-4 py-6">
      <div class="flex flex-col md:flex-row items-center justify-between space-y-4 md:space-y-0">
        <div class="flex items-center space-x-2">
          <UIcon name="i-heroicons-outline-printer" class="w-5 h-5 text-primary" />
          <span class="text-gray-600 dark:text-gray-400">
            Powercess 自助打印系统
          </span>
        </div>

        <div class="flex items-center space-x-6 text-sm text-gray-500 dark:text-gray-400">
          <a href="#" class="hover:text-primary transition-colors">关于我们</a>
          <a href="#" class="hover:text-primary transition-colors">使用条款</a>
          <a href="#" class="hover:text-primary transition-colors">隐私政策</a>
          <a href="#" class="hover:text-primary transition-colors">联系客服</a>
        </div>

        <div class="text-sm text-gray-500 dark:text-gray-400">
          &copy; {{ new Date().getFullYear() }} Powercess. All rights reserved.
        </div>
      </div>
    </div>
  </footer>
</template>