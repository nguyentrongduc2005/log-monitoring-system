import { useEffect, useMemo, useState } from "react";
import {
  getApplications,
  getUserAccessError,
  getUserApplicationAccess,
  replaceUserApplicationAccess
} from "@/features/user-access/user-access-api";
import type {
  AccessSelection,
  Application,
  User
} from "@/features/user-access/user-access-types";
import DialogShell from "./DialogShell";

type AccessDialogProps = {
  user: User;
  onClose: () => void;
};

export default function AccessDialog({ user, onClose }: AccessDialogProps) {
  const [applications, setApplications] = useState<Application[]>([]);
  const [selections, setSelections] = useState<Record<string, AccessSelection>>(
    {}
  );
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(Boolean(user.id));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(
    user.id ? null : "This user does not have a valid identifier."
  );
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (!user.id) {
      return;
    }

    let active = true;
    Promise.all([getApplications(), getUserApplicationAccess(user.id)])
      .then(([nextApplications, nextAccesses]) => {
        if (!active) {
          return;
        }

        const nextSelections: Record<string, AccessSelection> = {};
        for (const application of nextApplications) {
          if (application.id) {
            nextSelections[application.id] = "NONE";
          }
        }
        for (const access of nextAccesses) {
          if (
            access.applicationId &&
            (access.accessLevel === "VIEW" || access.accessLevel === "MANAGE")
          ) {
            nextSelections[access.applicationId] = access.accessLevel;
          }
        }

        setApplications(nextApplications);
        setSelections(nextSelections);
      })
      .catch(loadError => {
        if (active) {
          setError(
            getUserAccessError(loadError, "Unable to load application access.")
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [user.id]);

  const visibleApplications = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    if (!normalizedSearch) {
      return applications;
    }

    return applications.filter(application =>
      `${application.name ?? ""} ${application.displayName ?? ""}`
        .toLowerCase()
        .includes(normalizedSearch)
    );
  }, [applications, search]);

  const selectedGrantCount = Object.values(selections).filter(
    access => access === "VIEW" || access === "MANAGE"
  ).length;

  async function saveAccess() {
    if (!user.id) {
      return;
    }

    const grants = Object.entries(selections)
      .filter((entry): entry is [string, "VIEW" | "MANAGE"] =>
        entry[1] === "VIEW" || entry[1] === "MANAGE"
      )
      .map(([applicationId, accessLevel]) => ({
        applicationId,
        accessLevel
      }));

    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await replaceUserApplicationAccess(user.id, grants);
      setSuccess("Application access updated.");
    } catch (saveError) {
      setError(
        getUserAccessError(saveError, "Unable to update application access.")
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <DialogShell
      description={`Control which applications ${user.displayName || user.email} can access and what they can do.`}
      eyebrow="Access control"
      footer={
        <>
          <button
            className="min-h-11 rounded-xl border border-border px-5 text-sm font-medium text-text transition hover:bg-surface-raised"
            disabled={saving}
            onClick={onClose}
            type="button"
          >
            Close
          </button>
          <button
            className="min-h-11 rounded-xl bg-primary px-5 text-sm font-semibold text-black transition hover:bg-primary-hover disabled:opacity-50"
            disabled={loading || saving || !user.id}
            onClick={() => void saveAccess()}
            type="button"
          >
            {saving ? "Saving..." : "Save access"}
          </button>
        </>
      }
      onClose={onClose}
      size="xl"
      title="Manage application access"
    >
      {loading ? (
        <div className="space-y-3">
          <div className="h-12 animate-pulse rounded-xl bg-surface-raised" />
          <div className="h-20 animate-pulse rounded-xl bg-surface-raised" />
          <div className="h-20 animate-pulse rounded-xl bg-surface-raised" />
          <div className="h-20 animate-pulse rounded-xl bg-surface-raised" />
        </div>
      ) : null}

      {!loading ? (
        <div className="space-y-5">
          <section className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-xl border border-border bg-background p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-muted">
                Applications
              </p>
              <p className="mt-2 text-2xl font-semibold text-text">
                {applications.length}
              </p>
            </div>
            <div className="rounded-xl border border-border bg-background p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-muted">
                Assigned
              </p>
              <p className="mt-2 text-2xl font-semibold text-primary">
                {selectedGrantCount}
              </p>
            </div>
            <div className="rounded-xl border border-border bg-background p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-muted">
                Unassigned
              </p>
              <p className="mt-2 text-2xl font-semibold text-muted">
                {applications.length - selectedGrantCount}
              </p>
            </div>
          </section>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-sm font-semibold text-text">Application permissions</h3>
              <p className="mt-1 text-xs text-muted">
                View allows read-only access. Manage allows operational changes.
              </p>
            </div>
            <input
              aria-label="Search applications"
              className="min-h-11 w-full rounded-xl border border-border bg-background px-3.5 text-sm text-text outline-none placeholder:text-muted/70 hover:border-muted/60 focus:border-primary focus:ring-2 focus:ring-primary/20 sm:max-w-xs"
              onChange={event => setSearch(event.target.value)}
              placeholder="Search applications..."
              value={search}
            />
          </div>

          <div className="overflow-hidden rounded-2xl border border-border bg-background">
            <div className="hidden grid-cols-[minmax(0,1fr)_13rem] border-b border-border bg-surface-raised/60 px-5 py-3 text-xs font-semibold uppercase tracking-wide text-muted sm:grid">
              <span>Application</span>
              <span>Permission</span>
            </div>
            {visibleApplications.map(application => {
              if (!application.id) {
                return null;
              }

              return (
                <div
                  className="grid gap-3 border-b border-border px-5 py-4 transition last:border-0 hover:bg-surface-raised/35 sm:grid-cols-[minmax(0,1fr)_13rem] sm:items-center"
                  key={application.id}
                >
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-medium text-text">
                        {application.displayName || application.name}
                      </p>
                      {application.status !== "ACTIVE" ? (
                        <span className="rounded-full bg-warning/15 px-2 py-0.5 text-xs font-medium text-warning">
                          {application.status}
                        </span>
                      ) : null}
                    </div>
                    <p className="mt-1 font-mono text-xs text-muted">
                      {application.name}
                    </p>
                  </div>
                  <select
                    aria-label={`Access for ${application.displayName || application.name}`}
                    className={`min-h-11 rounded-xl border px-3.5 text-sm font-medium outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 ${
                      selections[application.id] === "MANAGE"
                        ? "border-primary/40 bg-primary/10 text-primary"
                        : selections[application.id] === "VIEW"
                          ? "border-success/40 bg-success/10 text-success"
                          : "border-border bg-surface text-muted"
                    }`}
                    disabled={saving}
                    onChange={event =>
                      setSelections(current => ({
                        ...current,
                        [application.id as string]: event.target
                          .value as AccessSelection
                      }))
                    }
                    value={selections[application.id] ?? "NONE"}
                  >
                    <option value="NONE">No access</option>
                    <option value="VIEW">View</option>
                    <option value="MANAGE">Manage</option>
                  </select>
                </div>
              );
            })}

            {visibleApplications.length === 0 ? (
              <p className="px-4 py-14 text-center text-sm text-muted">
                No applications match this search.
              </p>
            ) : null}
          </div>

          <div className="flex gap-3 rounded-xl border border-primary/20 bg-primary/5 px-4 py-3">
            <span
              aria-hidden="true"
              className="grid size-5 shrink-0 place-items-center rounded-full bg-primary/15 text-xs font-bold text-primary"
            >
              i
            </span>
            <p className="text-xs leading-5 text-muted">
              Saving replaces the user&apos;s complete application grant list.
              Applications set to No access are removed immediately.
            </p>
          </div>

          {success ? (
            <p className="rounded-xl border border-success/30 bg-success/10 px-4 py-3 text-sm text-success">
              {success}
            </p>
          ) : null}
          {error ? (
            <p className="rounded-xl border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
              {error}
            </p>
          ) : null}
        </div>
      ) : null}
    </DialogShell>
  );
}
