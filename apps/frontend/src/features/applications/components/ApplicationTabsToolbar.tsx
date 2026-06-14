import {
  managementInputClass,
  managementPrimaryButtonClass
} from "@/shared/components/management-ui";
import type { Application } from "../application-types";

export type ApplicationTab = "applications" | "api-keys";

type ApplicationTabsToolbarProps = {
  activeTab: ApplicationTab;
  applications: Application[];
  search: string;
  selectedApplication: Application | null;
  onCreateKey: () => void;
  onSearchChange: (value: string) => void;
  onSelectApplication: (id: string | null) => void;
  onTabChange: (tab: ApplicationTab) => void;
};

export default function ApplicationTabsToolbar({
  activeTab,
  applications,
  search,
  selectedApplication,
  onCreateKey,
  onSearchChange,
  onSelectApplication,
  onTabChange
}: ApplicationTabsToolbarProps) {
  return (
    <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-3 lg:flex-row lg:items-center lg:justify-between">
      <div className="inline-flex rounded-md border border-border bg-background p-1">
        <button
          className={`rounded px-3 py-2 text-sm font-semibold transition ${
            activeTab === "applications"
              ? "bg-primary text-black"
              : "text-muted hover:text-text"
          }`}
          onClick={() => onTabChange("applications")}
          type="button"
        >
          Applications
        </button>
        <button
          className={`rounded px-3 py-2 text-sm font-semibold transition ${
            activeTab === "api-keys"
              ? "bg-primary text-black"
              : "text-muted hover:text-text"
          }`}
          onClick={() => onTabChange("api-keys")}
          type="button"
        >
          API keys
        </button>
      </div>

      {activeTab === "applications" ? (
        <label className="block w-full lg:max-w-md">
          <span className="sr-only">Search applications</span>
          <input
            aria-label="Search applications"
            className={managementInputClass}
            onChange={event => onSearchChange(event.target.value)}
            placeholder="Search application name, description, or status..."
            value={search}
          />
        </label>
      ) : null}

      {activeTab === "api-keys" ? (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <label className="block min-w-72">
            <span className="sr-only">Select application</span>
            <select
              aria-label="Select application"
              className={managementInputClass}
              onChange={event => onSelectApplication(event.target.value || null)}
              value={selectedApplication?.id ?? ""}
            >
              {applications.map(application => (
                <option
                  key={application.id ?? application.name}
                  value={application.id}
                >
                  {application.displayName || application.name}
                </option>
              ))}
            </select>
          </label>
          <button
            className={managementPrimaryButtonClass}
            disabled={
              !selectedApplication || selectedApplication.status !== "ACTIVE"
            }
            onClick={onCreateKey}
            type="button"
          >
            Create API key
          </button>
        </div>
      ) : null}
    </div>
  );
}

