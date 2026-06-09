import { createRef, useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation
} from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import MobileSidebarDrawer from "@/shared/layouts/MobileSidebarDrawer";
import Sidebar from "@/shared/layouts/Sidebar";

function LocationValue() {
  return <output data-testid="location">{useLocation().pathname}</output>;
}

function renderSidebar({
  role = "ADMIN",
  path = "/",
  badgeCounts
}: {
  role?: string;
  path?: string;
  badgeCounts?: Record<string, number>;
} = {}) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Sidebar badgeCounts={badgeCounts} role={role} />
      <Routes>
        <Route path="*" element={<LocationValue />} />
      </Routes>
    </MemoryRouter>
  );
}

function DrawerHarness({ onClose = vi.fn() }: { onClose?: () => void }) {
  const [open, setOpen] = useState(true);
  const toggleRef = createRef<HTMLButtonElement>();

  return (
    <MemoryRouter>
      <button ref={toggleRef} type="button">
        Toggle navigation
      </button>
      <MobileSidebarDrawer
        onClose={() => {
          setOpen(false);
          onClose();
        }}
        open={open}
        returnFocusRef={toggleRef}
      >
        <Sidebar
          onNavigate={() => setOpen(false)}
          onRequestClose={() => setOpen(false)}
          role="ADMIN"
          showCloseButton
        />
      </MobileSidebarDrawer>
    </MemoryRouter>
  );
}

describe("Sidebar", () => {
  it("shows all groups to Admin and hides Administration from other roles", () => {
    const { rerender } = renderSidebar();

    expect(screen.getByText("Administration")).toBeInTheDocument();
    expect(screen.getByText("Users & Access")).toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <Sidebar role="ENGINEER" />
      </MemoryRouter>
    );
    expect(screen.queryByText("Administration")).not.toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <Sidebar role="UNKNOWN" />
      </MemoryRouter>
    );
    expect(screen.queryByText("Administration")).not.toBeInTheDocument();
  });

  it("marks the current implemented route active", () => {
    renderSidebar({ path: "/logs" });

    expect(screen.getByRole("link", { name: "Live Logs" })).toHaveAttribute(
      "aria-current",
      "page"
    );
    expect(screen.getByRole("link", { name: "Overview" })).not.toHaveAttribute(
      "aria-current"
    );
  });

  it("navigates implemented items and keeps unavailable items inert", async () => {
    const user = userEvent.setup();
    renderSidebar({ path: "/logs" });

    await user.click(screen.getByRole("button", { name: /log search/i }));
    expect(screen.getByTestId("location")).toHaveTextContent("/logs");
    expect(
      screen.getByRole("button", {
        name: "Log Search, currently unavailable"
      })
    ).toHaveTextContent("Log Search");

    await user.click(screen.getByRole("link", { name: "Overview" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/");
  });

  it("formats alert badges without reserving an empty badge", () => {
    const { rerender } = renderSidebar({
      badgeCounts: { alerts: 0 }
    });
    expect(screen.queryByLabelText("0 alerts")).not.toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <Sidebar badgeCounts={{ alerts: 4 }} role="ADMIN" />
      </MemoryRouter>
    );
    expect(screen.getByText("4")).toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <Sidebar badgeCounts={{ alerts: 100 }} role="ADMIN" />
      </MemoryRouter>
    );
    expect(screen.getByText("99+")).toBeInTheDocument();
  });
});

describe("MobileSidebarDrawer", () => {
  it("owns a single application-sidebar id", () => {
    render(<DrawerHarness />);
    expect(document.querySelectorAll("#application-sidebar")).toHaveLength(1);
  });

  it("closes with Escape, backdrop, and close button", async () => {
    const onClose = vi.fn();
    const first = render(<DrawerHarness onClose={onClose} />);

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
    first.unmount();

    const second = render(<DrawerHarness onClose={onClose} />);
    fireEvent.click(screen.getByTestId("sidebar-backdrop"));
    expect(onClose).toHaveBeenCalledTimes(2);
    second.unmount();

    render(<DrawerHarness onClose={onClose} />);
    await userEvent.click(
      screen.getByRole("button", { name: "Close navigation" })
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("traps focus and restores it to the toggle", async () => {
    const user = userEvent.setup();
    render(<DrawerHarness />);

    const closeButton = screen.getByRole("button", {
      name: "Close navigation"
    });
    expect(closeButton).toHaveFocus();

    await user.tab({ shift: true });
    expect(screen.getByRole("button", { name: /settings/i })).toHaveFocus();

    await user.click(closeButton);
    expect(screen.getByRole("button", { name: "Toggle navigation" })).toHaveFocus();
  });
});
