import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AppHeader from "../../app/components/layout/AppHeader.vue";

// Mock stores
vi.mock("../../stores/auth", () => ({
  useAuthStore: vi.fn(() => ({
    isLoggedIn: false,
    logout: vi.fn(),
  })),
}));

vi.mock("../../stores/user", () => ({
  useUserStore: vi.fn(() => ({
    username: "",
    balance: 0,
    isAdmin: false,
  })),
}));

// Mock Nuxt composables
vi.stubGlobal("useRoute", () => ({ path: "/" }));
vi.stubGlobal("navigateTo", vi.fn());

describe("AppHeader", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it("renders logo and brand name", () => {
    const wrapper = mount(AppHeader);

    const html = wrapper.html();
    expect(html).toContain("自助打印");
  });

  it("shows login and register buttons when not logged in", () => {
    const wrapper = mount(AppHeader);

    const html = wrapper.html();
    expect(html).toContain("登录");
    expect(html).toContain("注册");
  });

  it("renders navigation items", () => {
    const wrapper = mount(AppHeader);

    const html = wrapper.html();
    expect(html).toContain("打印");
    expect(html).toContain("文件管理");
    expect(html).toContain("订单记录");
    expect(html).toContain("钱包");
    expect(html).toContain("社区");
  });

  it("has mobile menu toggle button", () => {
    const wrapper = mount(AppHeader);

    const buttons = wrapper.findAllComponents({ name: "UButton" });
    const mobileMenuButton = buttons.find(
      (btn) => btn.props("icon") === "i-heroicons-bars-3",
    );
    expect(mobileMenuButton).toBeDefined();
  });
});