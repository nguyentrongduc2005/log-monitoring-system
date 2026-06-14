import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as RetentionPage } from "./RetentionPage";
import { getRetentionJobs, saveRetentionJobs } from "./retention-adapter";
import type { RetentionJob } from "./retention-types";

vi.mock("./retention-adapter", () => ({
  getRetentionJobs: vi.fn(),
  saveRetentionJobs: vi.fn()
}));

const retentionJobs: RetentionJob[] = [
  {
    id: "ret-info",
    logLevel: "INFO",
    label: "INFO logs",
    description: "Routine application logs and request traces.",
    retentionDays: 30,
    action: "DELETE",
    minDays: 7,
    maxDays: 365,
    storageTb: 2.1,
    storagePercent: 45,
    projectedDeletionTbPerMonth: 0.72,
    compressionSavingsGbPerMonth: 120,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Purged 1.1TB of expired INFO logs from production cluster.",
      occurredAt: "2026-06-11T01:00:00Z"
    }
  },
  {
    id: "ret-warn",
    logLevel: "WARN",
    label: "WARN logs",
    description: "Potentially degraded behavior.",
    retentionDays: 90,
    action: "COMPRESS",
    minDays: 14,
    maxDays: 365,
    storageTb: 1.5,
    storagePercent: 32,
    projectedDeletionTbPerMonth: 0.18,
    compressionSavingsGbPerMonth: 240,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "WARNING",
      message: "Compression ratio fell below 20%.",
      occurredAt: "2026-06-10T22:00:00Z"
    }
  },
  {
    id: "ret-error",
    logLevel: "ERROR",
    label: "ERROR logs",
    description: "Application errors retained longer.",
    retentionDays: 180,
    action: "ARCHIVE",
    minDays: 30,
    maxDays: 365,
    storageTb: 0.6,
    storagePercent: 16,
    projectedDeletionTbPerMonth: 0.3,
    compressionSavingsGbPerMonth: 60,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Archived 240GB of ERROR logs.",
      occurredAt: "2026-06-10T19:00:00Z"
    }
  },
  {
    id: "ret-critical",
    logLevel: "CRITICAL",
    label: "CRITICAL logs",
    description: "High-severity failures kept longest.",
    retentionDays: 365,
    action: "ARCHIVE",
    minDays: 90,
    maxDays: 730,
    storageTb: 0.3,
    storagePercent: 7,
    projectedDeletionTbPerMonth: 0.05,
    compressionSavingsGbPerMonth: 30,
    nextRunAt: "2026-06-12T04:00:00Z",
    recentOperation: {
      status: "SUCCESS",
      message: "Archived 80GB of CRITICAL logs.",
      occurredAt: "2026-06-10T18:30:00Z"
    }
  }
];

function renderPage() {
  return render(
    <PageHeaderProvider>
      <RetentionPage />
    </PageHeaderProvider>
  );
}

describe("RetentionPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getRetentionJobs).mockResolvedValue(retentionJobs);
    vi.mocked(saveRetentionJobs).mockResolvedValue(retentionJobs);
  });

  it("loads the default retention jobs", async () => {
    renderPage();

    expect(await screen.findAllByText("INFO logs")).toHaveLength(2);
    expect(screen.getAllByText("WARN logs")).toHaveLength(2);
    expect(screen.getAllByText("ERROR logs")).toHaveLength(2);
    expect(screen.getAllByText("CRITICAL logs")).toHaveLength(2);
    expect(screen.getByText("Storage Distribution")).toBeInTheDocument();
  });

  it("saves updated retention settings for existing jobs", async () => {
    const user = userEvent.setup();
    renderPage();

    const infoSlider = await screen.findByLabelText("INFO logs retention days");
    fireEvent.change(infoSlider, { target: { value: "21" } });
    await user.click(screen.getAllByRole("radio", { name: "Compress" })[0]);
    await user.click(screen.getByRole("button", { name: "Save Changes" }));

    await waitFor(() =>
      expect(saveRetentionJobs).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({
            id: "ret-info",
            retentionDays: 21,
            action: "COMPRESS"
          }),
          expect.objectContaining({ id: "ret-warn" }),
          expect.objectContaining({ id: "ret-error" }),
          expect.objectContaining({ id: "ret-critical" })
        ])
      )
    );
  });
});
