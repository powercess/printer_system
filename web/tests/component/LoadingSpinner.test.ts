import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import LoadingSpinner from "../../app/components/common/LoadingSpinner.vue";

describe("LoadingSpinner", () => {
  it("renders with default size (md)", () => {
    const wrapper = mount(LoadingSpinner);

    const spinner = wrapper.find("div.animate-spin");
    expect(spinner.exists()).toBe(true);
    expect(spinner.classes()).toContain("w-8");
    expect(spinner.classes()).toContain("h-8");
  });

  it("renders with small size", () => {
    const wrapper = mount(LoadingSpinner, {
      props: { size: "sm" },
    });

    const spinner = wrapper.find("div.animate-spin");
    expect(spinner.classes()).toContain("w-4");
    expect(spinner.classes()).toContain("h-4");
  });

  it("renders with large size", () => {
    const wrapper = mount(LoadingSpinner, {
      props: { size: "lg" },
    });

    const spinner = wrapper.find("div.animate-spin");
    expect(spinner.classes()).toContain("w-12");
    expect(spinner.classes()).toContain("h-12");
  });

  it("has animation classes", () => {
    const wrapper = mount(LoadingSpinner);

    const spinner = wrapper.find("div.animate-spin");
    expect(spinner.classes()).toContain("animate-spin");
    expect(spinner.classes()).toContain("rounded-full");
  });
});