<template>
  <div class="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900">
    <AppHeader />
    <main class="flex-1 container mx-auto px-4 py-6">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>