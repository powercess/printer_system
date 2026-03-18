// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: "2025-07-15",
  devtools: { enabled: true },

  modules: [
    "@nuxt/eslint",
    "@nuxt/image",
    "@nuxt/test-utils",
    "@nuxt/ui",
    "@pinia/nuxt",
  ],
  css: ["assets/css/main.css"],

  imports: {
    dirs: ["stores", "composables", "api", "types"],
  },

  runtimeConfig: {
    public: {
      apiBase: "http://localhost:8080",
    },
  },

  pinia: {
    storesDirs: ["./stores/**"],
  },

  alias: {
    "~/types": "./types",
    "~/api": "./api",
    "~/stores": "./stores",
    "~/composables": "./composables",
  },

  typescript: {
    strict: true,
  },
});
