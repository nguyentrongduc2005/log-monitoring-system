import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  MemoryRouter,
  Route,
  Routes
} from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthContext } from "@/features/auth/auth-context";
import type { AuthContextValue } from "@/features/auth/auth-context";
import AppLayout from "@/shared/layouts/AppLayout";
import { PageHeader } from "@/shared/layouts/page-header-context";

function createMatchMedia(desktop: boolean) {
  let matches = desktop;
  const listeners = new Set<(event: MediaQueryListEvent) => void>();
  const mediaQuery = {
    get matches() {
      return matches;
    },
    media: "(min-width: 768px)",
    onchange: null,
    addEventListener: (_type: string, listener: (event: MediaQueryListEvent) => void) =>
      listeners.add(listener),
    removeEventListener: (
      _type: string,
      listener: (event: MediaQueryListEvent) => void
    ) => listeners.delete(listener),
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => true
  } as MediaQueryList;

  return {
    matchMedia: () => mediaQuery,
    setDesktop(next: boolean) {
      matches = next;
      listeners.forEach((listener) =>
        listener({ matches: next, media: mediaQuery.media } as MediaQueryListEvent)
      );
    }
  };
}

function TestPage() {
  return (
    <>
      <PageHeader
        actions={<button type="button">Page action</button>}
        title="Overview"
      />
      <p>Routed content</p>
    </>
  );
}

function renderLayout({
  role = "ADMIN",
  desktop = true,
  logout = vi.fn()
}: {
  role?: string;
  desktop?: boolean;
  logout?: () => void;
} = {}) {
  const media = createMatchMedia(desktop);
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: media.matchMedia
  });

  const authValue: AuthContextValue = {
    session: {
      accessToken: "token",
      refreshToken: "refresh",
      user: {
        displayName: "Ada Lovelace",
        email: "ada@logpulse.dev",
        role
      }
    },
    isInitializing: false,
    login: vi.fn(),
    logout,
    updateSessionUser: vi.fn()
  };

  const result = render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<TestPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );

  return { ...result, logout, media };
}

afterEach(() => {
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: (query: string): MediaQueryList =>
      ({
        matches: query === "(min-width: 768px)",
        media: query,
        onchange: null,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        addListener: () => undefined,
        removeListener: () => undefined,
        dispatchEvent: () => true
      }) as MediaQueryList
  });
});

describe("AppLayout", () => {
  it("starts desktop open, pushes content, and resets open after remount", async () => {
    const user = userEvent.setup();
    const first = renderLayout();

    expect(document.querySelector("#application-sidebar")).toBeInTheDocument();
    expect(screen.getByTestId("app-content-column")).toHaveClass("md:pl-60");

    await user.click(screen.getByRole("button", { name: "Hide navigation" }));
    expect(document.querySelector("#application-sidebar")).not.toBeInTheDocument();
    expect(screen.getByTestId("app-content-column")).not.toHaveClass("md:pl-60");
    first.unmount();

    renderLayout();
    expect(document.querySelector("#application-sidebar")).toBeInTheDocument();
  });

  it("starts mobile closed, opens as a drawer, and restores desktop open on resize", async () => {
    const user = userEvent.setup();
    const { media } = renderLayout({ desktop: false });

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Show navigation" }));
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    act(() => media.setDesktop(true));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(document.querySelector("#application-sidebar")).toBeInTheDocument();
    expect(screen.getByTestId("app-content-column")).toHaveClass("md:pl-60");
  });

  it("filters navigation by role and renders routed title, action, and content", () => {
    renderLayout({ role: "ENGINEER" });

    expect(screen.queryByText("Administration")).not.toBeInTheDocument();
    expect(screen.getByTitle("Overview")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Page action" })).toBeInTheDocument();
    expect(screen.getByText("Routed content")).toBeInTheDocument();
    expect(screen.getAllByRole("navigation")).toHaveLength(1);
    expect(screen.getAllByRole("main")).toHaveLength(1);
    expect(screen.getAllByRole("banner")).toHaveLength(1);
  });

  it("logs out through the header account menu and excludes global controls", async () => {
    const user = userEvent.setup();
    const logout = vi.fn();
    renderLayout({ logout });

    await user.click(screen.getByRole("button", { name: "Open account menu" }));
    await user.click(screen.getByRole("menuitem", { name: "Log out" }));
    expect(logout).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("search")).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^notifications?$/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/theme/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/system online|degraded|disconnected/i)).not.toBeInTheDocument();
  });
});
