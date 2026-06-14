import {
  managementButtonClass,
  managementTableHeaderClass,
  managementTableRowClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type { ApiKey, Application } from "../application-types";
import { formatDate, statusTone } from "./application-ui";

type ApiKeysPanelProps = {
  apiKeys: ApiKey[];
  error: string | null;
  loading: boolean;
  selectedApplication: Application | null;
  onRevoke: (apiKey: ApiKey) => void;
  onRotate: (apiKey: ApiKey) => void;
};

export default function ApiKeysPanel({
  apiKeys,
  error,
  loading,
  selectedApplication,
  onRevoke,
  onRotate
}: ApiKeysPanelProps) {
  return (
    <div className="space-y-4 p-4">
      {selectedApplication ? (
        <section className="rounded-lg border border-border bg-background p-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-primary">
                Selected application
              </p>
              <h2 className="mt-2 text-xl font-semibold text-text">
                {selectedApplication.displayName || selectedApplication.name}
              </h2>
              <p className="mt-1 font-mono text-xs text-muted">
                {selectedApplication.name}
              </p>
              <p className="mt-3 max-w-3xl text-sm leading-6 text-muted">
                {selectedApplication.description || "No description provided."}
              </p>
            </div>
            <StatusBadge tone={statusTone(selectedApplication.status)}>
              {selectedApplication.status}
            </StatusBadge>
          </div>
        </section>
      ) : (
        <div className="rounded-lg border border-dashed border-border px-4 py-10 text-center">
          <p className="font-medium text-text">No application selected</p>
          <p className="mt-1 text-sm text-muted">
            Create an application to start managing API keys.
          </p>
        </div>
      )}

      {loading ? (
        <p className="rounded-lg border border-border bg-background px-4 py-8 text-sm text-muted">
          Loading API keys...
        </p>
      ) : null}

      {error ? (
        <p className="rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
          {error}
        </p>
      ) : null}

      {!loading && apiKeys.length > 0 ? (
        <div className="shell-scrollbar overflow-x-auto rounded-lg border border-border bg-background">
          <table className="w-full min-w-[900px] text-left text-sm">
            <thead className={managementTableHeaderClass}>
              <tr>
                <th className="px-4 py-2 font-medium">Key</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">Created</th>
                <th className="px-4 py-2 font-medium">Expires</th>
                <th className="px-4 py-2 font-medium">Last used</th>
                <th className="px-4 py-2 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {apiKeys.map(apiKey => (
                <tr
                  className={managementTableRowClass}
                  key={apiKey.id ?? apiKey.keyPrefix}
                >
                  <td className="px-4 py-3">
                    <p className="font-medium text-text">{apiKey.name}</p>
                    <p className="mt-1 font-mono text-xs text-muted">
                      {apiKey.keyPrefix}
                    </p>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge tone={statusTone(apiKey.status)}>
                      {apiKey.status}
                    </StatusBadge>
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(apiKey.createdAt)}
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(apiKey.expiresAt)}
                  </td>
                  <td className="px-4 py-3 text-muted">
                    {formatDate(apiKey.lastUsedAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <button
                        className={`${managementButtonClass} disabled:opacity-40`}
                        disabled={apiKey.status !== "ACTIVE"}
                        onClick={() => onRotate(apiKey)}
                        type="button"
                      >
                        Rotate
                      </button>
                      <button
                        className="rounded-md border border-error/40 px-3 py-2 text-xs font-medium text-error transition hover:bg-error/10 disabled:opacity-40"
                        disabled={apiKey.status !== "ACTIVE"}
                        onClick={() => onRevoke(apiKey)}
                        type="button"
                      >
                        Revoke
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {!loading && !error && apiKeys.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted">
          No API keys have been created for this application yet.
        </p>
      ) : null}
    </div>
  );
}
