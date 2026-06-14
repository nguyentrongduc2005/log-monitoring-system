import {
  managementButtonClass,
  managementTableHeaderClass,
  managementTableRowClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type {
  Application,
  ApplicationStatus
} from "../application-types";
import { formatDate, statusTone } from "./application-ui";

type ApplicationsTableProps = {
  applications: Application[];
  error: string | null;
  loading: boolean;
  selectedApplication: Application | null;
  onEdit: (application: Application) => void;
  onManageKeys: (application: Application) => void;
  onToggleStatus: (application: Application, status: ApplicationStatus) => void;
};

export default function ApplicationsTable({
  applications,
  error,
  loading,
  selectedApplication,
  onEdit,
  onManageKeys,
  onToggleStatus
}: ApplicationsTableProps) {
  if (loading) {
    return <div className="px-4 py-8 text-sm text-muted">Loading applications...</div>;
  }

  if (error) {
    return (
      <div className="p-4">
        <p className="rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
          {error}
        </p>
      </div>
    );
  }

  return (
    <div className="shell-scrollbar overflow-x-auto">
      <table className="w-full min-w-[920px] text-left text-sm">
        <thead className={managementTableHeaderClass}>
          <tr>
            <th className="px-4 py-2 font-medium">Application</th>
            <th className="px-4 py-2 font-medium">Status</th>
            <th className="px-4 py-2 font-medium">Created</th>
            <th className="px-4 py-2 font-medium">Updated</th>
            <th className="px-4 py-2 text-right font-medium">Actions</th>
          </tr>
        </thead>
        <tbody>
          {applications.map(application => (
            <tr
              className={`${managementTableRowClass} ${
                selectedApplication?.id === application.id ? "bg-primary/5" : ""
              }`}
              key={application.id ?? application.name}
            >
              <td className="px-4 py-3">
                <p className="font-semibold text-text">
                  {application.displayName || application.name}
                </p>
                <p className="mt-0.5 font-mono text-xs text-muted">
                  {application.name}
                </p>
                {application.description ? (
                  <p className="mt-2 line-clamp-2 max-w-2xl text-xs leading-5 text-muted">
                    {application.description}
                  </p>
                ) : null}
              </td>
              <td className="px-4 py-3">
                <StatusBadge tone={statusTone(application.status)}>
                  {application.status || "UNKNOWN"}
                </StatusBadge>
              </td>
              <td className="px-4 py-3 text-muted">
                {formatDate(application.createdAt)}
              </td>
              <td className="px-4 py-3 text-muted">
                {formatDate(application.updatedAt)}
              </td>
              <td className="px-4 py-3">
                <div className="flex justify-end gap-2">
                  <button
                    className={managementButtonClass}
                    onClick={() => onManageKeys(application)}
                    type="button"
                  >
                    View keys
                  </button>
                  <button
                    className="rounded-md bg-primary/15 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/25"
                    onClick={() => onEdit(application)}
                    type="button"
                  >
                    Edit
                  </button>
                  <button
                    className="rounded-md border border-warning/40 px-3 py-2 text-xs font-medium text-warning transition hover:bg-warning/10"
                    onClick={() =>
                      onToggleStatus(
                        application,
                        application.status === "ACTIVE" ? "INACTIVE" : "ACTIVE"
                      )
                    }
                    type="button"
                  >
                    {application.status === "ACTIVE" ? "Deactivate" : "Activate"}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {applications.length === 0 ? (
        <div className="px-5 py-10 text-center">
          <p className="font-medium text-text">No applications found</p>
          <p className="mt-1 text-sm text-muted">
            Create an app or try another search.
          </p>
        </div>
      ) : null}
    </div>
  );
}

