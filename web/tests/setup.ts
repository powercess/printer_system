// 测试环境设置
import { vi } from 'vitest'

// Mock Nuxt app module - must be before any imports
vi.mock('nuxt/app', () => ({
  useRuntimeConfig: () => ({
    public: {
      apiBase: 'http://localhost:8080',
    },
  }),
  useToast: () => ({
    add: vi.fn(),
  }),
  navigateTo: vi.fn(),
  useNuxtApp: () => ({
    provide: {},
  }),
  defineNuxtConfig: (config: any) => config,
}))

// Mock #app alias (same as nuxt/app)
vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: {
      apiBase: 'http://localhost:8080',
    },
  }),
  useToast: () => ({
    add: vi.fn(),
  }),
  navigateTo: vi.fn(),
  useNuxtApp: () => ({
    provide: {},
  }),
  defineNuxtConfig: (config: any) => config,
}))

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value
    },
    removeItem: (key: string) => {
      delete store[key]
    },
    clear: () => {
      store = {}
    },
  }
})()

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
})

// Mock import.meta
vi.stubGlobal('import.meta', {
  client: true,
  server: false,
})