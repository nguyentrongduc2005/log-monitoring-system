import { type ReactNode } from "react";
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Skeleton
} from "@/shared/components/ui";
import { cn } from "@/shared/lib/utils";

type StatusTone = "primary" | "success" | "warning" | "error" | "muted";

const statusClasses: Record<StatusTone, string> = {
  primary: "border-primary/25 bg-primary/10 text-primary-hover",
  success: "border-success/25 bg-success/10 text-success",
  warning: "border-warning/25 bg-warning/10 text-warning",
  error: "border-error/25 bg-error/10 text-error",
  muted: "border-border bg-surface-raised text-muted"
};

export function PageShell({
  children,
  className
}: {
  children: ReactNode;
  className?: string;
}) {
  return <div className={cn("space-y-5", className)}>{children}</div>;
}

export function PageSection({
  children,
  className
}: {
  children: ReactNode;
  className?: string;
}) {
  return <Card className={cn("overflow-hidden", className)}>{children}</Card>;
}

export function SectionHeader({
  actions,
  description,
  title
}: {
  actions?: ReactNode;
  description?: ReactNode;
  title: ReactNode;
}) {
  return (
    <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0">
        <CardTitle>{title}</CardTitle>
        {description ? <CardDescription>{description}</CardDescription> : null}
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
    </CardHeader>
  );
}

export function FilterBar({
  children,
  className
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("grid gap-3 border-b border-border bg-surface-raised/35 p-3", className)}>
      {children}
    </div>
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
          <linearGradient id={`grad-tile-${tone}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={activeColor.stop} stopOpacity="0.25" />
            <stop offset="100%" stopColor={activeColor.stop} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path
          d={paths[tone]}
          fill={`url(#grad-tile-${tone})`}
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

export function MetricTile({
  label,
  value,
  description,
  tone = "primary"
}: {
  label: string;
  value: ReactNode;
  description?: ReactNode;
  tone?: StatusTone;
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
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted/70">{label}</p>
        <p className={cn("mt-2 text-2xl font-bold leading-none tracking-tight", valueColor)}>{value}</p>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 w-full">
        {description ? (
          <p className="text-[11px] text-muted/80 truncate flex-1">{description}</p>
        ) : <div className="flex-1" />}
        {isNumeric ? <Sparkline tone={tone} /> : null}
      </div>
    </Card>
  );
}

export function StatusPill({
  children,
  tone = "muted"
}: {
  children: ReactNode;
  tone?: StatusTone;
}) {
  return <Badge className={statusClasses[tone]} variant="outline">{children}</Badge>;
}

export function LoadingRows({ count = 3 }: { count?: number }) {
  return (
    <CardContent className="space-y-2">
      {Array.from({ length: count }, (_, index) => (
        <Skeleton className="h-12" key={index} />
      ))}
    </CardContent>
  );
}

export function EmptyState({
  description,
  title
}: {
  description?: ReactNode;
  title: ReactNode;
}) {
  return (
    <div className="px-5 py-10 text-center">
      <p className="text-sm font-medium text-text">{title}</p>
      {description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}
    </div>
  );
}

export function ErrorState({
  message,
  title = "Unable to load data"
}: {
  message: ReactNode;
  title?: ReactNode;
}) {
  return (
    <div className="p-4">
      <Alert variant="destructive">
        <AlertTitle>{title}</AlertTitle>
        <AlertDescription>{message}</AlertDescription>
      </Alert>
    </div>
  );
}
