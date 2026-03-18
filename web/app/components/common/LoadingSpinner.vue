<template>
  <div class="flex items-center justify-center">
    <div
      class="animate-spin rounded-full border-4 border-primary border-t-transparent"
      :class="sizeClasses[size ?? 'md']"
    />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: "sm" | "md" | "lg";
}>(), {
  size: "md",
});

const sizeClasses: Record<"sm" | "md" | "lg", string> = {
  sm: "w-4 h-4",
  md: "w-8 h-8",
  lg: "w-12 h-12",
};
</script>