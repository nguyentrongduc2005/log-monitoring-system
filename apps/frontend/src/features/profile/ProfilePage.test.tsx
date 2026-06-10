import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthContext } from "@/features/auth/auth-context";
import type { AuthContextValue } from "@/features/auth/auth-context";
import { Component as ProfilePage } from "@/features/profile/ProfilePage";
import {
  changePassword,
  getProfile,
  updateProfile
} from "@/features/profile/profile-api";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";

vi.mock("@/features/profile/profile-api", () => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  changePassword: vi.fn()
}));

const profile = {
  id: "00000000-0000-0000-0000-000000000001",
  email: "engineer@example.com",
  displayName: "Engineer",
  role: "ENGINEER",
  status: "ACTIVE",
  lastLoginAt: "2026-06-09T10:00:00Z",
  createdAt: "2026-06-01T09:00:00Z",
  updatedAt: "2026-06-08T11:00:00Z"
};

function renderProfilePage(
  authOverrides: Partial<AuthContextValue> = {}
) {
  const authValue: AuthContextValue = {
    session: {
      accessToken: "token",
      refreshToken: "refresh",
      user: profile
    },
    isInitializing: false,
    login: vi.fn(),
    logout: vi.fn(),
    updateSessionUser: vi.fn(),
    ...authOverrides
  };

  render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter>
        <PageHeaderProvider>
          <ProfilePage />
        </PageHeaderProvider>
      </MemoryRouter>
    </AuthContext.Provider>
  );

  return authValue;
}

describe("ProfilePage", () => {
  beforeEach(() => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    vi.mocked(updateProfile).mockResolvedValue(profile);
    vi.mocked(changePassword).mockResolvedValue(undefined);
  });

  it("renders profile identity and metadata", async () => {
    renderProfilePage();

    expect(await screen.findByText("Engineer")).toBeInTheDocument();
    expect(screen.getByText("engineer@example.com")).toBeInTheDocument();
    expect(screen.getAllByText("ENGINEER")).not.toHaveLength(0);
    expect(screen.getAllByText("ACTIVE")).not.toHaveLength(0);
    expect(
      screen.getByText("00000000-0000-0000-0000-000000000001")
    ).toBeInTheDocument();
    expect(screen.getByText(/9 Jun 2026/i)).toBeInTheDocument();
    expect(screen.getByText(/1 Jun 2026/i)).toBeInTheDocument();
    expect(screen.getByText(/8 Jun 2026/i)).toBeInTheDocument();
  });

  it("renders Not available for missing date fields", async () => {
    vi.mocked(getProfile).mockResolvedValue({
      ...profile,
      lastLoginAt: undefined,
      createdAt: undefined,
      updatedAt: undefined
    });

    renderProfilePage();

    await screen.findByText("Engineer");
    expect(screen.getAllByText("Not available")).toHaveLength(3);
  });

  it("validates display name and email before saving", async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await screen.findByText("Engineer");
    await user.clear(screen.getByLabelText("Display name"));
    await user.clear(screen.getByLabelText("Email"));
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    expect(screen.getByText("Email and display name are required.")).toBeInTheDocument();
    expect(updateProfile).not.toHaveBeenCalled();
  });

  it("submits profile updates, refreshes visible identity, and updates auth session", async () => {
    const user = userEvent.setup();
    const updateSessionUser = vi.fn();
    vi.mocked(updateProfile).mockResolvedValue({
      ...profile,
      displayName: "New Engineer",
      email: "new@example.com"
    });

    renderProfilePage({ updateSessionUser });

    await screen.findByText("Engineer");
    await user.clear(screen.getByLabelText("Display name"));
    await user.type(screen.getByLabelText("Display name"), "New Engineer");
    await user.clear(screen.getByLabelText("Email"));
    await user.type(screen.getByLabelText("Email"), "new@example.com");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    await waitFor(() =>
      expect(updateProfile).toHaveBeenCalledWith({
        displayName: "New Engineer",
        email: "new@example.com"
      })
    );
    expect(updateSessionUser).toHaveBeenCalledWith({
      ...profile,
      displayName: "New Engineer",
      email: "new@example.com"
    });
    expect(await screen.findByText("New Engineer")).toBeInTheDocument();
    expect(screen.getByText("new@example.com")).toBeInTheDocument();
  });

  it("validates password confirmation before API call", async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await screen.findByText("Engineer");
    await user.type(screen.getByLabelText("Current password"), "old-password");
    await user.type(screen.getByLabelText("New password"), "new-password");
    await user.type(
      screen.getByLabelText("Confirm new password"),
      "different-password"
    );
    await user.click(screen.getByRole("button", { name: "Change password" }));

    expect(screen.getByText("Password confirmation does not match.")).toBeInTheDocument();
    expect(changePassword).not.toHaveBeenCalled();
  });

  it("changes password and clears password fields after success", async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await screen.findByText("Engineer");
    const currentPassword = screen.getByLabelText("Current password");
    const newPassword = screen.getByLabelText("New password");
    const confirmPassword = screen.getByLabelText("Confirm new password");

    await user.type(currentPassword, "old-password");
    await user.type(newPassword, "new-password");
    await user.type(confirmPassword, "new-password");
    await user.click(screen.getByRole("button", { name: "Change password" }));

    await waitFor(() =>
      expect(changePassword).toHaveBeenCalledWith({
        oldPassword: "old-password",
        newPassword: "new-password"
      })
    );
    expect(currentPassword).toHaveValue("");
    expect(newPassword).toHaveValue("");
    expect(confirmPassword).toHaveValue("");
  });

  it("renders a retryable generic error when loading fails", async () => {
    vi.mocked(getProfile).mockRejectedValue(new Error("boom"));
    renderProfilePage();

    expect(
      await screen.findByText("Unable to load profile right now. Please try again.")
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("renders a permission-safe message for forbidden profile access", async () => {
    vi.mocked(getProfile).mockRejectedValue({
      isAxiosError: true,
      response: { status: 403 }
    });

    renderProfilePage();

    expect(
      await screen.findByText("You do not have permission to view this profile.")
    ).toBeInTheDocument();
  });
});
