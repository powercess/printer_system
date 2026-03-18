import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "jsdom",
    include: ["tests/component/**/*.test.ts"],
    setupFiles: ["tests/component-setup.ts"],
    globals: true,
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "./app"),
      "~": resolve(__dirname, "."),
    },
  },
});