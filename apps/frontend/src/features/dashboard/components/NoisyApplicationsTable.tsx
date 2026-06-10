import type { NoisyApplication } from "@/features/dashboard/overview-types";

export default function NoisyApplicationsTable({
  applications,
  authorizedApplications
}: {
  applications: NoisyApplication[];
  authorizedApplications: number;
}) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-text">Noisy applications</h2>
          <p className="mt-1 text-sm text-muted">
            Fast read on which services are generating the heaviest error load.
          </p>
        </div>
      </div>

      {applications.length === 0 && authorizedApplications > 0 ? (
        <p className="mt-5 rounded-xl border border-border bg-surface-raised p-4 text-sm text-muted">
          No noisy applications in the current window.
        </p>
      ) : (
        <div className="mt-5 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="text-xs uppercase tracking-[0.16em] text-muted/70">
              <tr>
                <th className="pb-3">Application</th>
                <th className="pb-3">Environment</th>
                <th className="pb-3">Total logs</th>
                <th className="pb-3">Errors</th>
                <th className="pb-3">Critical</th>
                <th className="pb-3">Error rate</th>
                <th className="pb-3">Last seen</th>
              </tr>
            </thead>
            <tbody>
              {applications.map((application) => (
                <tr className="border-t border-border" key={application.id}>
                  <td className="py-3 font-medium text-text">{application.name}</td>
                  <td className="py-3 text-muted">{application.environment}</td>
                  <td className="py-3 text-text">{application.totalLogs}</td>
                  <td className="py-3 text-warning">{application.errorCount}</td>
                  <td className="py-3 text-error">{application.criticalCount}</td>
                  <td className="py-3 text-text">{application.errorRate}</td>
                  <td className="py-3 text-muted">{application.lastSeen}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
