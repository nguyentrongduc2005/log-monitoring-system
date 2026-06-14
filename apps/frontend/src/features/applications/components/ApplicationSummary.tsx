import { MetricCard } from "@/shared/components/management-ui";
import type { ApiKey, Application } from "../application-types";

export default function ApplicationSummary({
  apiKeys,
  applications
}: {
  apiKeys: ApiKey[];
  applications: Application[];
}) {
  const activeApplications = applications.filter(
    application => application.status === "ACTIVE"
  ).length;
  const activeKeys = apiKeys.filter(apiKey => apiKey.status === "ACTIVE").length;

  return (
    <section className="grid gap-3 md:grid-cols-3">
      <MetricCard label="Total applications" tone="muted" value={applications.length} />
      <MetricCard
        label="Active applications"
        tone="success"
        value={activeApplications}
      />
      <MetricCard
        label="Active keys for selected app"
        tone="primary"
        value={activeKeys}
      />
    </section>
  );
}

