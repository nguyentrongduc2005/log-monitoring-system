import { describe, expect, it } from "vitest";
import {
  formatNavigationBadge,
  getNavigationGroups,
  navigationGroups
} from "@/shared/layouts/navigation";
import type { NavigationItem } from "@/shared/layouts/navigation";

describe("navigation", () => {
  it("shows every group to an admin", () => {
    expect(getNavigationGroups("ADMIN").map((group) => group.label)).toEqual([
      "Monitoring",
      "Analytics",
      "Resources",
      "Administration"
    ]);
  });

  it("hides administration from an engineer or unknown role", () => {
    expect(getNavigationGroups("ENGINEER").map((group) => group.label)).toEqual([
      "Monitoring",
      "Analytics",
      "Resources"
    ]);
    expect(
      getNavigationGroups(undefined).some(
        (group) => group.label === "Administration"
      )
    ).toBe(false);
  });

  it("defines only Overview and Live Logs as implemented routes", () => {
    const implemented = navigationGroups
      .flatMap<NavigationItem>((group) => [...group.items])
      .filter((item) => item.to !== undefined)
      .map((item) => [item.label, item.to]);

    expect(implemented).toEqual([
      ["Overview", "/"],
      ["Live Logs", "/logs"]
    ]);
  });

  it.each([
    [undefined, null],
    [0, null],
    [1, "1"],
    [99, "99"],
    [100, "99+"]
  ])("formats badge count %s as %s", (count, expected) => {
    expect(formatNavigationBadge(count)).toBe(expected);
  });
});
