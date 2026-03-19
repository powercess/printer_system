import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import AppFooter from "../../app/components/layout/AppFooter.vue";

// Stub components
const UIcon = {
  name: "UIcon",
  template: '<span class="icon-stub"><slot /></span>',
  props: ["name", "class"],
};

describe("AppFooter", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const mountAppFooter = () => {
    return mount(AppFooter, {
      global: {
        stubs: {
          UIcon,
        },
      },
    });
  };

  it("renders brand name", () => {
    const wrapper = mountAppFooter();

    const html = wrapper.html();
    expect(html).toContain("Powercess 自助打印系统");
  });

  it("renders footer links", () => {
    const wrapper = mountAppFooter();

    const html = wrapper.html();
    expect(html).toContain("关于我们");
    expect(html).toContain("使用条款");
    expect(html).toContain("隐私政策");
    expect(html).toContain("联系客服");
  });

  it("displays current year in copyright", () => {
    const wrapper = mountAppFooter();

    const currentYear = new Date().getFullYear().toString();
    const html = wrapper.html();
    expect(html).toContain(currentYear);
  });

  it("has correct footer structure", () => {
    const wrapper = mountAppFooter();

    const footer = wrapper.find("footer");
    expect(footer.exists()).toBe(true);

    // Check for container
    const container = footer.find(".container");
    expect(container.exists()).toBe(true);
  });
});