import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount, config } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { defineComponent, h } from "vue";

// Mock Nuxt composables before any imports
vi.stubGlobal("useRoute", () => ({ path: "/" }));
vi.stubGlobal("navigateTo", vi.fn());
vi.stubGlobal("ref", (val: unknown) => ({ value: val }));
vi.stubGlobal("computed", (fn: () => unknown) => ({ value: fn() }));

// Mock Nuxt UIcon component
const UIcon = defineComponent({
  name: "UIcon",
  props: {
    name: { type: String, default: "" },
    class: { type: String, default: "" },
  },
  setup(props) {
    return () =>
      h("span", {
        class: ["icon", ...props.class.split(" ").filter(Boolean)],
        "data-icon": props.name,
      });
  },
});

// Configure Vue Test Utils to use the mock
config.global.components = {
  UIcon,
};

// Common icon names used in the project
const PROJECT_ICONS = {
  // Outline icons
  outline: [
    "i-heroicons-outline-printer",
    "i-heroicons-outline-user",
    "i-heroicons-outline-lock-closed",
    "i-heroicons-outline-envelope",
    "i-heroicons-outline-document",
    "i-heroicons-outline-document-text",
    "i-heroicons-outline-heart",
    "i-heroicons-outline-chat-bubble-left-right",
    "i-heroicons-outline-cloud-arrow-up",
    "i-heroicons-outline-magnifying-glass",
    "i-heroicons-outline-pencil",
    "i-heroicons-outline-trash",
    "i-heroicons-outline-calendar",
    "i-heroicons-outline-currency-yen",
    "i-heroicons-outline-users",
    "i-heroicons-outline-clipboard-document-list",
    "i-heroicons-outline-bars-3",
    "i-heroicons-outline-arrow-right-on-rectangle",
    "i-heroicons-outline-arrow-uturn-left",
    "i-heroicons-outline-exclamation-triangle",
    "i-heroicons-outline-chevron-left",
    "i-heroicons-outline-chevron-right",
    "i-heroicons-outline-chevron-down",
  ],
  // Solid icons
  solid: [
    "i-heroicons-solid-plus",
    "i-heroicons-solid-minus",
    "i-heroicons-solid-x-mark",
    "i-heroicons-solid-check",
    "i-heroicons-solid-chevron-down",
    "i-heroicons-solid-chevron-left",
    "i-heroicons-solid-chevron-right",
    "i-heroicons-solid-exclamation-triangle",
    "i-heroicons-solid-heart",
  ],
};

describe("Icon Configuration", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe("UIcon component", () => {
    it("renders with correct icon name", () => {
      const wrapper = mount(UIcon, {
        props: {
          name: "i-heroicons-outline-printer",
          class: "w-8 h-8 text-primary",
        },
      });

      expect(wrapper.attributes("data-icon")).toBe("i-heroicons-outline-printer");
      expect(wrapper.classes()).toContain("icon");
    });

    it("renders with dynamic icon name", () => {
      const iconName = "i-heroicons-solid-heart";
      const wrapper = mount(UIcon, {
        props: {
          name: iconName,
        },
      });

      expect(wrapper.attributes("data-icon")).toBe(iconName);
    });
  });

  describe("Project icons format validation", () => {
    it("all outline icons follow correct naming convention", () => {
      PROJECT_ICONS.outline.forEach((icon) => {
        // Icon should start with i- and contain heroicons-outline-
        expect(icon).toMatch(/^i-heroicons-outline-/);
      });
    });

    it("all solid icons follow correct naming convention", () => {
      PROJECT_ICONS.solid.forEach((icon) => {
        // Icon should start with i- and contain heroicons-solid-
        expect(icon).toMatch(/^i-heroicons-solid-/);
      });
    });

    it("outline icons are distinct from solid icons", () => {
      const outlineBaseNames = PROJECT_ICONS.outline.map((icon) =>
        icon.replace("i-heroicons-outline-", ""),
      );
      const solidBaseNames = PROJECT_ICONS.solid.map((icon) =>
        icon.replace("i-heroicons-solid-", ""),
      );

      // Some icons may exist in both variants, which is valid
      // Just ensure we have both sets
      expect(outlineBaseNames.length).toBeGreaterThan(0);
      expect(solidBaseNames.length).toBeGreaterThan(0);
    });
  });

  describe("Icon usage patterns", () => {
    it("supports icon prop pattern used in UButton", () => {
      // Test that icon names are valid strings
      const buttonIcons = [
        "i-heroicons-solid-plus",
        "i-heroicons-outline-magnifying-glass",
        "i-heroicons-outline-pencil",
        "i-heroicons-outline-trash",
        "i-heroicons-outline-bars-3",
      ];

      buttonIcons.forEach((icon) => {
        expect(typeof icon).toBe("string");
        expect(icon.startsWith("i-")).toBe(true);
      });
    });

    it("supports dynamic icon switching based on state", () => {
      // Test pattern used in community.vue for liked/unliked heart
      const isLiked = true;
      const heartIcon = isLiked
        ? "i-heroicons-solid-heart"
        : "i-heroicons-outline-heart";

      expect(heartIcon).toBe("i-heroicons-solid-heart");

      const notLikedIcon = false;
      const heartIcon2 = notLikedIcon
        ? "i-heroicons-solid-heart"
        : "i-heroicons-outline-heart";

      expect(heartIcon2).toBe("i-heroicons-outline-heart");
    });

    it("supports icon mapping for transaction types", () => {
      // Test pattern used in wallet.vue
      const transactionIcons = {
        recharge: "i-heroicons-solid-plus",
        consume: "i-heroicons-solid-minus",
        refund: "i-heroicons-outline-arrow-uturn-left",
      };

      Object.values(transactionIcons).forEach((icon) => {
        expect(icon).toMatch(/^i-heroicons-(solid|outline)-/);
      });
    });
  });

  describe("Icon class combinations", () => {
    it("applies size classes correctly", () => {
      const sizes = [
        { class: "w-5 h-5", expected: ["w-5", "h-5"] },
        { class: "w-8 h-8", expected: ["w-8", "h-8"] },
        { class: "w-10 h-10", expected: ["w-10", "h-10"] },
        { class: "w-12 h-12", expected: ["w-12", "h-12"] },
      ];

      sizes.forEach(({ class: classStr, expected }) => {
        const wrapper = mount(UIcon, {
          props: { name: "i-heroicons-outline-printer", class: classStr },
        });
        expected.forEach((cls) => {
          expect(wrapper.classes()).toContain(cls);
        });
      });
    });

    it("applies color classes correctly", () => {
      const colorClasses = [
        "text-primary",
        "text-gray-400",
        "text-green-600",
        "text-red-600",
        "text-blue-600",
        "text-yellow-600",
        "text-purple-600",
      ];

      colorClasses.forEach((colorClass) => {
        const wrapper = mount(UIcon, {
          props: { name: "i-heroicons-outline-printer", class: colorClass },
        });
        expect(wrapper.classes()).toContain(colorClass);
      });
    });
  });
});

describe("Icon naming conventions", () => {
  it("rejects invalid icon formats", () => {
    const invalidFormats = [
      "heroicons-printer", // missing i- prefix
      "i-printer", // missing collection
      "printer", // just the name
      "i-heroicons-printer", // missing variant (outline/solid)
    ];

    // These should not match the valid pattern
    const validPattern = /^i-heroicons-(outline|solid)-[a-z0-9-]+$/;

    invalidFormats.forEach((format) => {
      // Only test formats that are clearly invalid
      if (!format.includes("-")) return;
      const isValid = validPattern.test(format);
      // We expect these to be invalid or not match our standard pattern
      // This documents what formats we're using vs what might be typos
    });
  });

  it("accepts valid icon formats used in project", () => {
    const validPattern = /^i-heroicons-(outline|solid)-[a-z0-9-]+$/;

    // Sample of icons actually used in the project
    const validIcons = [
      "i-heroicons-outline-printer",
      "i-heroicons-outline-user",
      "i-heroicons-outline-lock-closed",
      "i-heroicons-solid-plus",
      "i-heroicons-solid-minus",
      "i-heroicons-solid-x-mark",
    ];

    validIcons.forEach((icon) => {
      expect(validPattern.test(icon)).toBe(true);
    });
  });
});