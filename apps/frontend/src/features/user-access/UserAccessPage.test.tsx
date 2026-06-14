import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Component as UserAccessPage } from "./UserAccessPage";
import {
  changeUserRole,
  changeUserStatus,
  createUser,
  getApplications,
  getUser,
  getUserApplicationAccess,
  getUsers,
  replaceUserApplicationAccess,
  softDeleteUser,
  updateUser
} from "./user-access-api";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";

vi.mock("./user-access-api", async importOriginal => {
  const actual = await importOriginal<typeof import("./user-access-api")>();
  return {
    ...actual,
    getUsers: vi.fn(),
    getUser: vi.fn(),
    createUser: vi.fn(),
    changeUserRole: vi.fn(),
    changeUserStatus: vi.fn(),
    updateUser: vi.fn(),
    softDeleteUser: vi.fn(),
    getApplications: vi.fn(),
    getUserApplicationAccess: vi.fn(),
    replaceUserApplicationAccess: vi.fn()
  };
});

const admin = {
  id: "00000000-0000-0000-0000-000000000001",
  email: "admin@example.com",
  displayName: "System Admin",
  role: "ADMIN",
  status: "ACTIVE",
  lastLoginAt: "2026-06-10T10:00:00Z",
  createdAt: "2026-06-01T10:00:00Z"
};

const engineer = {
  id: "00000000-0000-0000-0000-000000000002",
  email: "engineer@example.com",
  displayName: "Platform Engineer",
  role: "ENGINEER",
  status: "DISABLED",
  createdAt: "2026-06-02T10:00:00Z"
};

function userPage(users = [admin, engineer]) {
  return {
    users,
    page: 0,
    size: 20,
    totalElements: users.length,
    totalPages: users.length > 0 ? 1 : 0
  };
}

function renderPage() {
  return render(
    <PageHeaderProvider>
      <UserAccessPage />
    </PageHeaderProvider>
  );
}

describe("UserAccessPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getUsers).mockResolvedValue(userPage());
    vi.mocked(getUser).mockResolvedValue(engineer);
    vi.mocked(createUser).mockResolvedValue(engineer);
    vi.mocked(updateUser).mockResolvedValue({
      ...engineer,
      displayName: "Updated Engineer"
    });
    vi.mocked(softDeleteUser).mockResolvedValue({
      ...engineer,
      status: "DELETED"
    });
    vi.mocked(changeUserRole).mockResolvedValue({
      ...engineer,
      role: "ADMIN"
    });
    vi.mocked(changeUserStatus).mockResolvedValue({
      ...engineer,
      status: "ACTIVE"
    });
    vi.mocked(getApplications).mockResolvedValue([
      {
        id: "00000000-0000-0000-0000-000000000101",
        name: "gateway",
        displayName: "API Gateway",
        status: "ACTIVE"
      },
      {
        id: "00000000-0000-0000-0000-000000000102",
        name: "billing",
        displayName: "Billing Service",
        status: "ACTIVE"
      }
    ]);
    vi.mocked(getUserApplicationAccess).mockResolvedValue([
      {
        userId: engineer.id,
        applicationId: "00000000-0000-0000-0000-000000000101",
        applicationName: "gateway",
        applicationDisplayName: "API Gateway",
        accessLevel: "VIEW"
      }
    ]);
    vi.mocked(replaceUserApplicationAccess).mockResolvedValue([]);
  });

  it("loads users and filters by identity", async () => {
    const user = userEvent.setup();
    vi.mocked(getUsers).mockImplementation(async query => {
      if (query?.search?.includes("admin@example")) {
        return userPage([admin]);
      }
      return userPage();
    });
    renderPage();

    expect(await screen.findByText("System Admin")).toBeInTheDocument();
    expect(screen.getByText("Platform Engineer")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Search users"), "admin@example");

    await waitFor(() =>
      expect(getUsers).toHaveBeenLastCalledWith(
        expect.objectContaining({ search: "admin@example" })
      )
    );
    expect(await screen.findByText("System Admin")).toBeInTheDocument();
    expect(screen.queryByText("Platform Engineer")).not.toBeInTheDocument();
  });

  it("creates a user and adds it to the table", async () => {
    const user = userEvent.setup();
    vi.mocked(getUsers).mockResolvedValue(userPage([admin]));
    renderPage();

    await screen.findByText("System Admin");
    await user.click(screen.getByRole("button", { name: "Create user" }));
    const dialog = screen.getByRole("dialog");
    await user.type(within(dialog).getByLabelText("Display name"), "Platform Engineer");
    await user.type(within(dialog).getByLabelText("Email"), "engineer@example.com");
    await user.type(
      within(dialog).getByLabelText("Temporary password"),
      "secret123"
    );
    await user.click(within(dialog).getByRole("button", { name: "Create user" }));

    await waitFor(() =>
      expect(createUser).toHaveBeenCalledWith({
        displayName: "Platform Engineer",
        email: "engineer@example.com",
        password: "secret123",
        role: "ENGINEER"
      })
    );
    expect(await screen.findByText("Platform Engineer")).toBeInTheDocument();
  });

  it("changes a user role and status from the details dialog", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Platform Engineer");
    const detailButtons = screen.getAllByRole("button", {
      name: "View details"
    });
    await user.click(detailButtons[1]);
    await screen.findByText("Application access");
    expect(await screen.findByText("API Gateway")).toBeInTheDocument();
    expect(screen.getByText("VIEW")).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("User role"), "ADMIN");
    await user.click(screen.getByRole("button", { name: "Save role" }));
    await waitFor(() =>
      expect(changeUserRole).toHaveBeenCalledWith(engineer.id, "ADMIN")
    );

    await user.click(screen.getByRole("button", { name: "Activate" }));
    await waitFor(() =>
      expect(changeUserStatus).toHaveBeenCalledWith(engineer.id, "ACTIVE")
    );
  });

  it("confirms before downgrading an admin to engineer", async () => {
    const user = userEvent.setup();
    vi.mocked(getUser).mockResolvedValue(admin);
    vi.mocked(changeUserRole).mockResolvedValue({
      ...admin,
      role: "ENGINEER"
    });
    renderPage();

    await screen.findByText("System Admin");
    await user.click(screen.getAllByRole("button", { name: "View details" })[0]);
    await screen.findByText("Application access");

    await user.selectOptions(screen.getByLabelText("User role"), "ENGINEER");
    await user.click(screen.getByRole("button", { name: "Save role" }));

    expect(changeUserRole).not.toHaveBeenCalled();
    expect(
      await screen.findByText("Downgrade administrator?")
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: "Downgrade to engineer" })
    );
    await waitFor(() =>
      expect(changeUserRole).toHaveBeenCalledWith(admin.id, "ENGINEER")
    );
  });

  it("updates and soft deletes a user from the details dialog", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Platform Engineer");
    await user.click(screen.getAllByRole("button", { name: "View details" })[1]);
    await screen.findByText("Application access");

    await user.click(screen.getByRole("button", { name: "Edit user" }));
    await user.clear(screen.getByLabelText("Display name"));
    await user.type(screen.getByLabelText("Display name"), "Updated Engineer");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() =>
      expect(updateUser).toHaveBeenCalledWith(engineer.id, {
        displayName: "Updated Engineer",
        email: "engineer@example.com"
      })
    );

    await user.click(screen.getByRole("button", { name: "Delete user" }));
    expect(await screen.findByText("Delete this user?")).toBeInTheDocument();
    const confirmDialog = screen.getAllByRole("dialog").at(-1)!;
    await user.click(
      within(confirmDialog).getByRole("button", { name: "Delete user" })
    );

    await waitFor(() => expect(softDeleteUser).toHaveBeenCalledWith(engineer.id));
  });

  it("replaces application access with selected grants", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Platform Engineer");
    const accessButtons = screen.getAllByRole("button", {
      name: "Manage access"
    });
    await user.click(accessButtons[1]);

    await screen.findByText("API Gateway");
    await user.selectOptions(
      screen.getByLabelText("Access for API Gateway"),
      "MANAGE"
    );
    await user.selectOptions(
      screen.getByLabelText("Access for Billing Service"),
      "VIEW"
    );
    await user.click(screen.getByRole("button", { name: "Save access" }));

    await waitFor(() =>
      expect(replaceUserApplicationAccess).toHaveBeenCalledWith(engineer.id, [
        {
          applicationId: "00000000-0000-0000-0000-000000000101",
          accessLevel: "MANAGE"
        },
        {
          applicationId: "00000000-0000-0000-0000-000000000102",
          accessLevel: "VIEW"
        }
      ])
    );
  });
});
