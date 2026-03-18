import { config } from "@vue/test-utils";
import { vi } from "vitest";
import { defineComponent, h, ref, computed, nextTick } from "vue";

// Create named stub components
const UButtonStub = defineComponent({
  name: "UButton",
  props: ["color", "variant", "icon", "size", "disabled", "to", "trailingIcon"],
  emits: ["click"],
  setup(props, { slots, emit }) {
    return () =>
      h(
        "button",
        { disabled: props.disabled, onClick: () => emit("click") },
        slots.default?.(),
      );
  },
});

// Stub Nuxt components
config.global.stubs = {
  NuxtLink: {
    props: ["to"],
    template: '<a :href="to"><slot /></a>',
  },
  UIcon: {
    props: ["name"],
    template: '<span class="icon"><slot /></span>',
  },
  UButton: UButtonStub,
  UBadge: {
    props: ["color", "variant"],
    template: '<span class="badge"><slot /></span>',
  },
  UDropdownMenu: {
    props: ["items"],
    template: '<div class="dropdown"><slot /></div>',
    emits: ["select"],
  },
};

// Mock Nuxt composables
config.global.mocks = {
  $route: {
    path: "/",
  },
  $router: {
    push: vi.fn(),
  },
};

// Provide Vue's ref, computed, nextTick as Nuxt auto-imports
vi.stubGlobal("ref", ref);
vi.stubGlobal("computed", computed);
vi.stubGlobal("nextTick", nextTick);
vi.stubGlobal("useRoute", () => ({ path: "/" }));
vi.stubGlobal("navigateTo", vi.fn());
vi.stubGlobal("useRouter", () => ({ push: vi.fn() }));

// Mock import.meta
vi.stubGlobal("import.meta", {
  client: true,
  server: false,
});

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(global, "localStorage", {
  value: localStorageMock,
});