<template>
  <div class="flex items-center justify-center gap-2">
    <UButton
      :disabled="currentPage <= 1"
      color="neutral"
      variant="ghost"
      icon="i-heroicons-solid-chevron-left"
      @click="goToPage(currentPage - 1)"
    />

    <div class="flex items-center gap-1">
      <template v-for="page in visiblePages" :key="page">
        <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
        <UButton
          v-else
          :color="page === currentPage ? 'primary' : 'neutral'"
          :variant="page === currentPage ? 'solid' : 'ghost'"
          size="sm"
          @click="goToPage(page as number)"
        >
          {{ page }}
        </UButton>
      </template>
    </div>

    <UButton
      :disabled="currentPage >= totalPages"
      color="neutral"
      variant="ghost"
      icon="i-heroicons-solid-chevron-right"
      @click="goToPage(currentPage + 1)"
    />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  currentPage: number;
  totalPages: number;
}>();

const emit = defineEmits<{
  (e: "update:currentPage", page: number): void;
  (e: "change", page: number): void;
}>();

const visiblePages = computed(() => {
  const pages: (number | string)[] = [];
  const { currentPage, totalPages } = props;

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
  } else {
    if (currentPage <= 4) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i);
      }
      pages.push("...");
      pages.push(totalPages);
    } else if (currentPage >= totalPages - 3) {
      pages.push(1);
      pages.push("...");
      for (let i = totalPages - 4; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      pages.push("...");
      for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        pages.push(i);
      }
      pages.push("...");
      pages.push(totalPages);
    }
  }

  return pages;
});

const goToPage = (page: number) => {
  if (page >= 1 && page <= props.totalPages) {
    emit("update:currentPage", page);
    emit("change", page);
  }
};
</script>