import { MetricCard } from "@/shared/components/management-ui";
import type { AlertRule } from "../alert-rules-types";

export default function AlertRuleSummary({ rules }: { rules: AlertRule[] }) {
  const active = rules.filter(rule => rule.status === "ACTIVE").length;
  const disabled = rules.length - active;
  const targets = rules.reduce((total, rule) => total + rule.deliveryTargets.length, 0);

  return (
    <section className="grid gap-3 md:grid-cols-3">
      <MetricCard label="Active rules" tone="success" value={active} />
      <MetricCard label="Delivery targets" tone="primary" value={targets} />
      <MetricCard label="Disabled rules" tone="muted" value={disabled} />
    </section>
  );
}
