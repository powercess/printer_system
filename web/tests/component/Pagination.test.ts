import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { ref, computed } from "vue";
import Pagination from "../../app/components/common/Pagination.vue";

// Mock Vue composables
vi.stubGlobal("ref", ref);
vi.stubGlobal("computed", computed);

// Stub UButton component
const UButton = {
  name: "UButton",
  template: '<button class="u-button-stub" :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
  props: ["color", "variant", "size", "icon", "disabled"],
  emits: ["click"],
};

describe("Pagination", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const mountPagination = (props: { currentPage: number; totalPages: number }) => {
    return mount(Pagination, {
      props,
      global: {
        stubs: {
          UButton,
        },
      },
    });
  };

  it("renders correct number of page buttons when totalPages <= 7", () => {
    const wrapper = mountPagination({
      currentPage: 1,
      totalPages: 5,
    });

    const html = wrapper.html();
    // Check that page numbers 1-5 are rendered
    expect(html).toContain("1");
    expect(html).toContain("2");
    expect(html).toContain("3");
    expect(html).toContain("4");
    expect(html).toContain("5");
  });

  it("disables previous button on first page", () => {
    const wrapper = mountPagination({
      currentPage: 1,
      totalPages: 10,
    });

    const buttons = wrapper.findAll("button");
    const prevButton = buttons[0];
    expect(prevButton.attributes("disabled")).toBeDefined();
  });

  it("disables next button on last page", () => {
    const wrapper = mountPagination({
      currentPage: 10,
      totalPages: 10,
    });

    const buttons = wrapper.findAll("button");
    const nextButton = buttons[buttons.length - 1];
    expect(nextButton.attributes("disabled")).toBeDefined();
  });

  it("emits change event when clicking page button", async () => {
    const wrapper = mountPagination({
      currentPage: 1,
      totalPages: 5,
    });

    // Find page 2 button (after prev and page 1)
    const buttons = wrapper.findAll("button");
    // Click the page 2 button
    await buttons[2].trigger("click");

    expect(wrapper.emitted("update:currentPage")).toBeTruthy();
    expect(wrapper.emitted("update:currentPage")![0]).toEqual([2]);
    expect(wrapper.emitted("change")).toBeTruthy();
    expect(wrapper.emitted("change")![0]).toEqual([2]);
  });

  it("shows ellipsis when totalPages > 7 and currentPage is in middle", () => {
    const wrapper = mountPagination({
      currentPage: 5,
      totalPages: 10,
    });

    const html = wrapper.html();
    expect(html).toContain("...");
  });

  it("does not emit when clicking disabled prev button", async () => {
    const wrapper = mountPagination({
      currentPage: 1,
      totalPages: 5,
    });

    const buttons = wrapper.findAll("button");
    await buttons[0].trigger("click");

    expect(wrapper.emitted("change")).toBeFalsy();
  });

  it("shows correct pages when currentPage is near start", () => {
    const wrapper = mountPagination({
      currentPage: 2,
      totalPages: 10,
    });

    const html = wrapper.html();
    // Should show pages 1, 2, 3, 4, 5, ..., 10
    expect(html).toContain("1");
    expect(html).toContain("2");
    expect(html).toContain("...");
    expect(html).toContain("10");
  });

  it("shows correct pages when currentPage is near end", () => {
    const wrapper = mountPagination({
      currentPage: 9,
      totalPages: 10,
    });

    const html = wrapper.html();
    // Should show pages 1, ..., 6, 7, 8, 9, 10
    expect(html).toContain("1");
    expect(html).toContain("...");
    expect(html).toContain("10");
  });
});