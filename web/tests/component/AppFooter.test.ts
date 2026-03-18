import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import AppFooter from "../../app/components/layout/AppFooter.vue";

describe("AppFooter", () => {
  it("renders brand name", () => {
    const wrapper = mount(AppFooter);

    const html = wrapper.html();
    expect(html).toContain("Powercess 自助打印系统");
  });

  it("renders footer links", () => {
    const wrapper = mount(AppFooter);

    const html = wrapper.html();
    expect(html).toContain("关于我们");
    expect(html).toContain("使用条款");
    expect(html).toContain("隐私政策");
    expect(html).toContain("联系客服");
  });

  it("displays current year in copyright", () => {
    const wrapper = mount(AppFooter);

    const currentYear = new Date().getFullYear().toString();
    const html = wrapper.html();
    expect(html).toContain(currentYear);
  });

  it("has correct footer structure", () => {
    const wrapper = mount(AppFooter);

    const footer = wrapper.find("footer");
    expect(footer.exists()).toBe(true);

    // Check for container
    const container = footer.find(".container");
    expect(container.exists()).toBe(true);
  });
});