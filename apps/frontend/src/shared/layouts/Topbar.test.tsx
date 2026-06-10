import { createRef } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation
} from "react-router-dom";
import {
  PageHeader,
  PageHeaderProvider
} from "@/shared/layouts/page-header-context";
import Topbar from "@/shared/layouts/Topbar";

function LocationProbe() {
  const location = useLocation();
  return <p data-testid="location-probe">{location.pathname}</p>;
}

function renderTopbar({
  displayName = "Ada Lovelace",
  email = "ada@logpulse.dev",
  role = "ADMIN",
  onLogout = vi.fn()
}: {
  displayName?: string;
  email?: string;
  role?: string;
  onLogout?: () => void;
} = {}) {
  const toggleRef = createRef<HTMLButtonElement>();

  render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route
          path="*"
          element={
            <PageHeaderProvider>
              <Topbar
                onLogout={onLogout}
                onToggleSidebar={vi.fn()}
                sidebarOpen
                toggleRef={toggleRef}
                user={{ displayName, email, role }}
              />
              <PageHeader
                actions={<button type="button">Create Application</button>}
                title="Overview"
              />
              <LocationProbe />
            </PageHeaderProvider>
          }
        />
      </Routes>
    </MemoryRouter>
  );

  return { onLogout };
}

describe("Topbar", () => {
  it("renders page title, actions, toggle state, and account identity", () => {
    renderTopbar();

    expect(screen.getByText("Overview")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Create Application" })
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Hide navigation" })).toHaveAttribute(
      "aria-expanded",
      "true"
    );
    expect(screen.getByText("Ada Lovelace")).toHaveAttribute(
      "title",
      "Ada Lovelace"
    );
    expect(screen.getByText("Admin")).toBeInTheDocument();
    expect(screen.getByText("AL")).toBeInTheDocument();
  });

  it("falls back to email initials and a neutral role", () => {
    renderTopbar({ displayName: " ", role: "UNKNOWN" });

    expect(screen.getByText("A")).toBeInTheDocument();
    expect(screen.getByText("User")).toBeInTheDocument();
  });

  it("opens and dismisses the account menu and invokes logout", async () => {
    const user = userEvent.setup();
    const onLogout = vi.fn();
    renderTopbar({ onLogout });

    const trigger = screen.getByRole("button", { name: /open account menu/i });
    await user.click(trigger);
    expect(screen.getByRole("menu")).toBeInTheDocument();

    await user.keyboard("{Escape}");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();

    await user.click(trigger);
    await user.click(document.body);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    await user.click(trigger);
    await user.click(screen.getByRole("menuitem", { name: "Log out" }));
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it("navigates to the profile route from the account menu", async () => {
    const user = userEvent.setup();
    renderTopbar();

    await user.click(screen.getByRole("button", { name: /open account menu/i }));
    await user.click(screen.getByRole("menuitem", { name: "Profile" }));

    expect(screen.getByTestId("location-probe")).toHaveTextContent("/profile");
  });

  it("does not render excluded global controls", () => {
    renderTopbar();

    expect(screen.queryByRole("search")).not.toBeInTheDocument();
    expect(screen.queryByText(/system online|degraded|disconnected/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/notification|theme/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/version/i)).not.toBeInTheDocument();
  });
});
