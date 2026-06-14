import { MetricCard } from "@/shared/components/management-ui";
import type { AlertRule } from "../alert-rules-types";

export default function AlertRuleSummary({ rules }: { rules: AlertRule[] }) {
  const activeRules = rules.filter(rule => rule.status === "RUNNING").length;
  const mutedRules = rules.filter(rule => rule.status === "MUTED").length;
  const notifications = rules.reduce((sum, rule) => sum + rule.triggered24h, 0);
  const breaches = rules.reduce((sum, rule) => sum + rule.breached24h, 0);

  return (
    <section className="grid gap-3 md:grid-cols-3">
      <MetricCard label="Active rules" tone="success" value={activeRules} />
      <MetricCard
        label="Notifications 24h"
        tone="primary"
        value={notifications.toLocaleString()}
      />
      <MetricCard label="Muted rules" tone="muted" value={mutedRules} />
      <p className="sr-only">{breaches} elevated values breached in 24h.</p>
    </section>
  );
}
