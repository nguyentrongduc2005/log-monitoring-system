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

const connectionDotColors: Record<LiveConnectionState, string> = {
  live: "bg-success animate-live",
  paused: "bg-warning",
  connecting: "bg-primary animate-pulse",
  reconnecting: "bg-primary animate-pulse",
  disconnected: "bg-muted",
  error: "bg-error animate-pulse",
};

const connectionStateContainers: Record<LiveConnectionState, string> = {
  live: "border-success/30 bg-success/5 text-success",
  paused: "border-warning/30 bg-warning/5 text-warning",
  connecting: "border-primary/30 bg-primary/5 text-primary",
  reconnecting: "border-primary/30 bg-primary/5 text-primary",
  disconnected: "border-border bg-surface text-muted",
  error: "border-error/30 bg-error/5 text-error",
};

type LiveLogsToolbarProps = {
  connectionState: LiveConnectionState;
  paused: boolean;
  buffered: number;
  dropped: number;
  stickyToLatest: boolean;
  wrapLines: boolean;
  onFollowLatest: () => void;
  onTogglePause: () => void;
  onToggleWrap: () => void;
  onClear: () => void;
};

export default function LiveLogsToolbar({
  connectionState,
  paused,
  buffered,
  dropped,
  stickyToLatest,
  wrapLines,
  onFollowLatest,
  onTogglePause,
  onToggleWrap,
  onClear
}: LiveLogsToolbarProps) {
  const effectiveState = paused ? "paused" : connectionState;

  return (
    <div className="flex flex-wrap items-center justify-end gap-2">
      <span
        className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${connectionStateContainers[effectiveState]}`}
      >
        <span className={`size-1.5 rounded-full ${connectionDotColors[effectiveState]}`} />
        {getConnectionLabel(effectiveState)}
      </span>
      {buffered > 0 ? (
        <span className="rounded-full bg-surface-raised px-2.5 py-0.5 text-xs font-medium text-text border border-border">
          Buffered: {buffered}
        </span>
      ) : null}
      {dropped > 0 ? (
        <span className="rounded-full bg-error/15 px-2.5 py-0.5 text-xs font-medium text-error border border-error/20">
          Dropped: {dropped}
        </span>
      ) : null}
      <button
        className={`inline-flex h-8 items-center justify-center rounded-md border px-3 text-xs font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 ${
          stickyToLatest
            ? "border-success/40 bg-success/5 text-success hover:border-success"
            : "border-border bg-surface-raised text-text hover:border-primary hover:text-primary"
        }`}
        disabled={stickyToLatest}
        onClick={onFollowLatest}
        type="button"
      >
        {stickyToLatest ? "Following" : "Jump to latest"}
      </button>
      <button
        className="inline-flex h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-xs font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
        onClick={onTogglePause}
        type="button"
      >
        {paused ? "Resume" : "Pause"}
      </button>
      <button
        className={`inline-flex h-8 items-center justify-center rounded-md border px-3 text-xs font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 ${
          wrapLines
            ? "border-primary/50 bg-primary/10 text-primary"
            : "border-border bg-surface-raised text-text hover:border-primary hover:text-primary"
        }`}
        onClick={onToggleWrap}
        type="button"
      >
        Wrap
      </button>
      <button
        className="inline-flex h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-xs font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
        onClick={onClear}
        type="button"
      >
        Clear
      </button>
    </div>
  );
}
