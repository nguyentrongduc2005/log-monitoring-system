import type { ReactNode } from "react";
import { Badge, Card } from "@/shared/components/ui";
import { cn } from "@/shared/lib/utils";

type Tone = "primary" | "success" | "warning" | "error" | "muted";

const toneClasses: Record<Tone, string> = {
  primary: "border-primary/25 bg-primary/10 text-primary-hover",
  success: "border-success/25 bg-success/10 text-success",
  warning: "border-warning/25 bg-warning/10 text-warning",
  error: "border-error/25 bg-error/10 text-error",
  muted: "border-border bg-surface-raised text-muted"
};

export function StatusBadge({
  children,
  tone = "muted"
}: {
  children: ReactNode;
  tone?: Tone;
}) {
  return (
    <Badge className={`py-1 font-semibold ${toneClasses[tone]}`} variant="outline">
      {children}
    </Badge>
  );
}

function Sparkline({ tone }: { tone: "primary" | "success" | "warning" | "error" | "muted" }) {
  const paths = {
    primary: "M 0 15 Q 15 5, 30 18 T 60 12 T 90 20 T 120 10 L 120 25 L 0 25 Z",
    success: "M 0 18 Q 15 22, 30 12 T 60 8 T 90 15 T 120 4 L 120 25 L 0 25 Z",
    warning: "M 0 10 Q 15 8, 30 18 T 60 12 T 90 22 T 120 15 L 120 25 L 0 25 Z",
    error: "M 0 22 Q 15 15, 30 8 T 60 18 T 90 4 T 120 20 L 120 25 L 0 25 Z",
    muted: "M 0 12 Q 15 18, 30 10 T 60 15 T 90 8 T 120 12 L 120 25 L 0 25 Z"
  };

  const strokePaths = {
    primary: "M 0 15 Q 15 5, 30 18 T 60 12 T 90 20 T 120 10",
    success: "M 0 18 Q 15 22, 30 12 T 60 8 T 90 15 T 120 4",
    warning: "M 0 10 Q 15 8, 30 18 T 60 12 T 90 22 T 120 15",
    error: "M 0 22 Q 15 15, 30 8 T 60 18 T 90 4 T 120 20",
    muted: "M 0 12 Q 15 18, 30 10 T 60 15 T 90 8 T 120 12"
  };

  const colors = {
    primary: { stroke: "#5e6ad2", stop: "#5e6ad2" },
    success: { stroke: "#27a644", stop: "#27a644" },
    warning: { stroke: "#f59e0b", stop: "#f59e0b" },
    error: { stroke: "#ef4444", stop: "#ef4444" },
    muted: { stroke: "#8a8f98", stop: "#8a8f98" }
  };

  const activeColor = colors[tone];

  return (
    <div className="h-5 w-16 opacity-75 shrink-0 self-end">
      <svg className="h-full w-full" viewBox="0 0 120 25" preserveAspectRatio="none">
        <defs>
          <linearGradient id={`grad-mgmt-${tone}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={activeColor.stop} stopOpacity="0.25" />
            <stop offset="100%" stopColor={activeColor.stop} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path
          d={paths[tone]}
          fill={`url(#grad-mgmt-${tone})`}
        />
        <path
          d={strokePaths[tone]}
          fill="none"
          stroke={activeColor.stroke}
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
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
    primary: "text-primary-hover",
    success: "text-success",
    warning: "text-warning",
    error: "text-error",
    muted: "text-text"
  }[tone];

  const isNumeric = typeof value === "number" || (typeof value === "string" && !isNaN(Number(value)));

  return (
    <Card className="p-4 flex flex-col justify-between min-h-[105px]">
      <div className="w-full">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted/70">
          {label}
        </p>
        <p className={cn("mt-2 text-2xl font-bold leading-none tracking-tight", valueColor)}>{value}</p>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 w-full">
        <div className="flex-1" />
        {isNumeric ? <Sparkline tone={tone} /> : null}
      </div>
    </Card>
  );
}

export const managementPanelClass =
  "overflow-hidden rounded-lg border border-border bg-surface";

export const managementInputClass =
  "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-text outline-none placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20";

export const managementButtonClass =
  "inline-flex min-h-9 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary disabled:opacity-50";

export const managementPrimaryButtonClass =
  "inline-flex min-h-9 items-center justify-center rounded-md bg-primary px-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover disabled:opacity-50";

export const managementTableHeaderClass =
  "border-b border-border bg-surface-raised/60 text-xs uppercase tracking-wide text-muted";

export const managementTableRowClass =
  "border-b border-border transition last:border-0 odd:bg-background/20 hover:bg-surface-raised/40";
