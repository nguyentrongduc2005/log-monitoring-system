import type { ReactNode } from "react";

type Tone = "primary" | "success" | "warning" | "error" | "muted";

const toneClasses: Record<Tone, string> = {
  primary: "bg-primary/10 text-primary ring-primary/20",
  success: "bg-success/10 text-success ring-success/20",
  warning: "bg-warning/10 text-warning ring-warning/20",
  error: "bg-error/10 text-error ring-error/20",
  muted: "bg-surface-raised text-muted ring-border"
};

export function StatusBadge({
  children,
  tone = "muted"
}: {
  children: ReactNode;
  tone?: Tone;
}) {
  return (
    <span
      className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-semibold ring-1 ${toneClasses[tone]}`}
    >
      {children}
    </span>
  );
}

export function MetricCard({
  label,
  value,
  tone = "primary"
}: {
  label: string;
  value: ReactNode;
  tone?: Tone;
}) {
  const valueColor = {
    primary: "text-primary",
    success: "text-success",
    warning: "text-warning",
    error: "text-error",
    muted: "text-text"
  }[tone];

  return (
    <div className="rounded-lg border border-border bg-surface p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-muted">
        {label}
      </p>
      <p className={`mt-2 text-2xl font-semibold ${valueColor}`}>{value}</p>
    </div>
  );
}

export const managementPanelClass =
  "overflow-hidden rounded-lg border border-border bg-surface";

export const managementInputClass =
  "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-text outline-none placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20";

export const managementButtonClass =
  "inline-flex min-h-9 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary disabled:opacity-50";

export const managementPrimaryButtonClass =
  "inline-flex min-h-9 items-center justify-center rounded-md bg-primary px-3 text-sm font-semibold text-black transition hover:bg-primary-hover disabled:opacity-50";

export const managementTableHeaderClass =
  "border-b border-border bg-surface-raised/60 text-xs uppercase tracking-wide text-muted";

export const managementTableRowClass =
  "border-b border-border transition last:border-0 odd:bg-background/20 hover:bg-surface-raised/40";

