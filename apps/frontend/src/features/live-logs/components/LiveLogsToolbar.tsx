import type { LiveConnectionState } from "@/features/live-logs/live-logs-types";

function getConnectionLabel(state: LiveConnectionState) {
  switch (state) {
    case "live":
      return "Live";
    case "paused":
      return "Paused";
    case "connecting":
      return "Connecting";
    case "reconnecting":
      return "Reconnecting";
    case "disconnected":
      return "Disconnected";
    case "error":
      return "Error";
  }
}

const stateClasses: Record<LiveConnectionState, string> = {
  live: "bg-success/15 text-success",
  paused: "bg-warning/15 text-warning",
  connecting: "bg-primary/15 text-primary",
  reconnecting: "bg-primary/15 text-primary",
  disconnected: "bg-surface-raised text-muted",
  error: "bg-error/15 text-error"
};

type LiveLogsToolbarProps = {
  connectionState: LiveConnectionState;
  paused: boolean;
  buffered: number;
  dropped: number;
  onTogglePause: () => void;
  onClear: () => void;
};

export default function LiveLogsToolbar({
  connectionState,
  paused,
  buffered,
  dropped,
  onTogglePause,
  onClear
}: LiveLogsToolbarProps) {
  const effectiveState = paused ? "paused" : connectionState;

  return (
    <div className="flex flex-wrap items-center justify-end gap-2">
      <span
        className={`rounded-full px-3 py-1 text-xs font-semibold ${stateClasses[effectiveState]}`}
      >
        {getConnectionLabel(effectiveState)}
      </span>
      {buffered > 0 ? (
        <span className="rounded-full bg-surface-raised px-3 py-1 text-xs font-medium text-text">
          Buffered: {buffered}
        </span>
      ) : null}
      {dropped > 0 ? (
        <span className="rounded-full bg-error/15 px-3 py-1 text-xs font-medium text-error">
          Dropped: {dropped}
        </span>
      ) : null}
      <button
        className="inline-flex min-h-10 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
        onClick={onTogglePause}
        type="button"
      >
        {paused ? "Resume" : "Pause"}
      </button>
      <button
        className="inline-flex min-h-10 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
        onClick={onClear}
        type="button"
      >
        Clear
      </button>
    </div>
  );
}
