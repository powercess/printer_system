import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Pagination from "../../app/components/common/Pagination.vue";

describe("Pagination", () => {
  it("renders correct number of page buttons when totalPages <= 7", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 1,
        totalPages: 5,
      },
    });

    // Should show all 5 pages + prev/next buttons
    const buttons = wrapper.findAllComponents({ name: "UButton" });
    // 5 page buttons + 2 navigation buttons (prev/next) = 7
    expect(buttons.length).toBe(7);
  });

  it("disables previous button on first page", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 1,
        totalPages: 10,
      },
    });

    const buttons = wrapper.findAllComponents({ name: "UButton" });
    const prevButton = buttons[0];
    expect(prevButton.props("disabled")).toBe(true);
  });

  it("disables next button on last page", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 10,
        totalPages: 10,
      },
    });

    const buttons = wrapper.findAllComponents({ name: "UButton" });
    const nextButton = buttons[buttons.length - 1];
    expect(nextButton.props("disabled")).toBe(true);
  });

  it("emits change event when clicking page button", async () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 1,
        totalPages: 5,
      },
    });

    const buttons = wrapper.findAllComponents({ name: "UButton" });
    // Click page 2 (index 2 because index 0 is prev, index 1 is page 1)
    await buttons[2].trigger("click");

    expect(wrapper.emitted("update:currentPage")).toBeTruthy();
    expect(wrapper.emitted("update:currentPage")![0]).toEqual([2]);
    expect(wrapper.emitted("change")).toBeTruthy();
    expect(wrapper.emitted("change")![0]).toEqual([2]);
  });

  it("shows ellipsis when totalPages > 7 and currentPage is in middle", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 5,
        totalPages: 10,
      },
    });

    const html = wrapper.html();
    expect(html).toContain("...");
  });

  it("does not emit when clicking disabled prev button", async () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 1,
        totalPages: 5,
      },
    });

    const buttons = wrapper.findAllComponents({ name: "UButton" });
    await buttons[0].trigger("click");

    expect(wrapper.emitted("change")).toBeFalsy();
  });

  it("shows correct pages when currentPage is near start", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 2,
        totalPages: 10,
      },
    });

    const html = wrapper.html();
    // Should show pages 1, 2, 3, 4, 5, ..., 10
    expect(html).toContain("1");
    expect(html).toContain("2");
    expect(html).toContain("...");
    expect(html).toContain("10");
  });

  it("shows correct pages when currentPage is near end", () => {
    const wrapper = mount(Pagination, {
      props: {
        currentPage: 9,
        totalPages: 10,
      },
    });

    const html = wrapper.html();
    // Should show pages 1, ..., 6, 7, 8, 9, 10
    expect(html).toContain("1");
    expect(html).toContain("...");
    expect(html).toContain("10");
  });
});