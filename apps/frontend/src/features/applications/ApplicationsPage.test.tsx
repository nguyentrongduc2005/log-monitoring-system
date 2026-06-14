import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as ApplicationsPage } from "./ApplicationsPage";
import {
  changeApplicationStatus,
  createApiKey,
  createApplication,
  getApplicationApiKeys,
  getApplications,
  revokeApiKey,
  rotateApiKey,
  updateApplication
} from "./application-api";

vi.mock("./application-api", async importOriginal => {
  const actual = await importOriginal<typeof import("./application-api")>();
  return {
    ...actual,
    getApplications: vi.fn(),
    createApplication: vi.fn(),
    updateApplication: vi.fn(),
    changeApplicationStatus: vi.fn(),
    getApplicationApiKeys: vi.fn(),
    createApiKey: vi.fn(),
    rotateApiKey: vi.fn(),
    revokeApiKey: vi.fn()
  };
});

const billingApp = {
  id: "00000000-0000-0000-0000-000000000101",
  name: "billing-service",
  displayName: "Billing Service",
  description: "Handles invoices and payments.",
  status: "ACTIVE",
  createdAt: "2026-06-01T10:00:00Z",
  updatedAt: "2026-06-01T10:00:00Z"
};

const gatewayApp = {
  id: "00000000-0000-0000-0000-000000000102",
  name: "api-gateway",
  displayName: "API Gateway",
  status: "INACTIVE",
  createdAt: "2026-06-02T10:00:00Z"
};

const activeKey = {
  id: "00000000-0000-0000-0000-000000000201",
  applicationId: billingApp.id,
  name: "Production key",
  keyPrefix: "lms_live_prod",
  status: "ACTIVE",
  createdAt: "2026-06-03T10:00:00Z"
};

function renderPage() {
  return render(
    <PageHeaderProvider>
      <ApplicationsPage />
    </PageHeaderProvider>
  );
}

describe("ApplicationsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getApplications).mockResolvedValue([billingApp, gatewayApp]);
    vi.mocked(getApplicationApiKeys).mockResolvedValue([activeKey]);
    vi.mocked(createApplication).mockResolvedValue({
      id: "00000000-0000-0000-0000-000000000103",
      name: "orders",
      displayName: "Orders",
      status: "ACTIVE"
    });
    vi.mocked(updateApplication).mockResolvedValue({
      ...billingApp,
      displayName: "Billing Platform"
    });
    vi.mocked(changeApplicationStatus).mockResolvedValue({
      ...billingApp,
      status: "INACTIVE"
    });
    vi.mocked(createApiKey).mockResolvedValue({
      id: "00000000-0000-0000-0000-000000000202",
      applicationId: billingApp.id,
      name: "New key",
      keyPrefix: "lms_live_new",
      rawApiKey: "lms_live_new.raw-secret",
      status: "ACTIVE"
    });
    vi.mocked(rotateApiKey).mockResolvedValue({
      id: "00000000-0000-0000-0000-000000000203",
      applicationId: billingApp.id,
      name: "Production key",
      keyPrefix: "lms_live_rotated",
      rawApiKey: "lms_live_rotated.raw-secret",
      status: "ACTIVE"
    });
    vi.mocked(revokeApiKey).mockResolvedValue();
  });

  it("loads applications and selected app API keys", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Billing Service")).toBeInTheDocument();
    expect(screen.getByText("API Gateway")).toBeInTheDocument();
    expect(getApplicationApiKeys).toHaveBeenCalledWith(billingApp.id);

    await user.click(screen.getByRole("button", { name: "API keys" }));
    expect(await screen.findByText("Production key")).toBeInTheDocument();
  });

  it("creates and edits applications", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Billing Service");
    await user.click(screen.getByRole("button", { name: "Create app" }));
    let dialog = screen.getByRole("dialog");
    await user.type(within(dialog).getByLabelText("Application name"), "orders");
    await user.type(within(dialog).getByLabelText("Display name"), "Orders");
    await user.click(within(dialog).getByRole("button", { name: "Create app" }));

    await waitFor(() =>
      expect(createApplication).toHaveBeenCalledWith({
        name: "orders",
        displayName: "Orders",
        description: undefined
      })
    );

    await user.click(screen.getAllByRole("button", { name: "Edit" })[1]);
    dialog = screen.getByRole("dialog");
    await user.clear(within(dialog).getByLabelText("Display name"));
    await user.type(
      within(dialog).getByLabelText("Display name"),
      "Billing Platform"
    );
    await user.click(
      within(dialog).getByRole("button", { name: "Save changes" })
    );

    await waitFor(() =>
      expect(updateApplication).toHaveBeenCalledWith(billingApp.id, {
        name: "billing-service",
        displayName: "Billing Platform",
        description: "Handles invoices and payments."
      })
    );
  });

  it("creates, rotates, and revokes API keys", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Billing Service");
    await user.click(screen.getByRole("button", { name: "API keys" }));
    await screen.findByText("Production key");
    await user.click(screen.getByRole("button", { name: "Create API key" }));
    let dialog = screen.getByRole("dialog");
    await user.type(within(dialog).getByLabelText("Key name"), "New key");
    await user.click(
      within(dialog).getByRole("button", { name: "Create API key" })
    );

    await waitFor(() =>
      expect(createApiKey).toHaveBeenCalledWith(billingApp.id, {
        name: "New key",
        expiresAt: undefined
      })
    );
    expect(await screen.findByText("lms_live_new.raw-secret")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "I copied this key" }));

    await user.click(screen.getByRole("button", { name: "Rotate" }));
    dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Rotate key" }));
    await waitFor(() =>
      expect(rotateApiKey).toHaveBeenCalledWith(billingApp.id, activeKey.id)
    );
    expect(
      await screen.findByText("lms_live_rotated.raw-secret")
    ).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "I copied this key" }));

    await user.click(screen.getByRole("button", { name: "Revoke" }));
    dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Revoke key" }));
    await waitFor(() =>
      expect(revokeApiKey).toHaveBeenCalledWith(billingApp.id, activeKey.id)
    );
  });

  it("confirms before changing application status", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Billing Service");
    await user.click(screen.getByRole("button", { name: "Deactivate" }));
    const dialog = screen.getByRole("dialog");
    await user.click(
      within(dialog).getByRole("button", { name: "Deactivate app" })
    );

    await waitFor(() =>
      expect(changeApplicationStatus).toHaveBeenCalledWith(
        billingApp.id,
        "INACTIVE"
      )
    );
  });
});
