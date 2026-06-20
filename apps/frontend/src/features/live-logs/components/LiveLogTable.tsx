import { useEffect, useLayoutEffect, useRef } from "react";
import type { ReactNode } from "react";
import type { LiveLogEntry } from "@/features/live-logs/live-logs-types";

const levelClasses = {
  INFO: "border-primary text-primary",
  WARN: "border-warning text-warning",
  ERROR: "border-error text-error",
  CRITICAL: "border-error bg-error/15 text-error"
} as const;

export default function LiveLogTable({
  entries,
  keyword,
  onSelect,
  onStickyChange,
  expanded,
  selectedEntryId,
  stickyToLatest,
  wrapLines
}: {
  entries: LiveLogEntry[];
  keyword: string;
  onSelect: (entry: LiveLogEntry) => void;
  onStickyChange: (sticky: boolean) => void;
  expanded: boolean;
  selectedEntryId?: string;
  stickyToLatest: boolean;
  wrapLines: boolean;
}) {
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const programmaticScrollRef = useRef(false);
  const userScrollIntentRef = useRef(false);
  const userScrollTimeoutRef = useRef<number | null>(null);
  const latestEntryId = entries.at(-1)?.id;

  useEffect(() => {
    return () => {
      if (userScrollTimeoutRef.current !== null) {
        window.clearTimeout(userScrollTimeoutRef.current);
      }
    };
  }, []);

  useLayoutEffect(() => {
    const container = scrollContainerRef.current;
    if (!container || !stickyToLatest) {
      return;
    }

    programmaticScrollRef.current = true;
    container.scrollTop = container.scrollHeight;
    requestAnimationFrame(() => {
      container.scrollTop = container.scrollHeight;
      requestAnimationFrame(() => {
        programmaticScrollRef.current = false;
      });
    });
  }, [expanded, latestEntryId, stickyToLatest]);

  function handleScroll() {
    if (programmaticScrollRef.current || !userScrollIntentRef.current) {
      return;
    }

    const container = scrollContainerRef.current;
    if (!container) {
      return;
    }

    const distanceFromBottom =
      container.scrollHeight - container.scrollTop - container.clientHeight;
    onStickyChange(distanceFromBottom < 24);
  }

  function markUserScrollIntent() {
    userScrollIntentRef.current = true;
    if (userScrollTimeoutRef.current !== null) {
      window.clearTimeout(userScrollTimeoutRef.current);
    }
    userScrollTimeoutRef.current = window.setTimeout(() => {
      userScrollIntentRef.current = false;
      userScrollTimeoutRef.current = null;
    }, 250);
  }

  if (entries.length === 0) {
    return (
      <p className="rounded-lg border border-border bg-background p-4 font-mono text-sm text-muted">
        No logs match the current filters.
      </p>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-border bg-background shadow-inner">
      <div className="flex items-center justify-between border-b border-border bg-surface px-3 py-2 font-mono text-[11px] uppercase text-muted">
        <span>Time / Level / App / Message</span>
        <span>{entries.length} visible</span>
      </div>
      <div
        ref={scrollContainerRef}
        onKeyDown={markUserScrollIntent}
        onPointerDown={markUserScrollIntent}
        onScroll={handleScroll}
        onTouchStart={markUserScrollIntent}
        onWheel={markUserScrollIntent}
        className={`bg-[#080a0c] overflow-auto [overflow-anchor:none] shell-scrollbar ${
          expanded ? "max-h-[72vh]" : "max-h-[48vh]"
        }`}
      >
        <div role="table" aria-label="Live log stream">
          <div role="rowgroup">
            <div
              className="sticky top-0 z-10 grid min-w-[980px] grid-cols-[11rem_5.5rem_12rem_minmax(24rem,1fr)_12rem] border-b border-border bg-surface px-3 py-1.5 font-mono text-[11px] uppercase text-muted"
              role="row"
            >
              <span role="columnheader">Timestamp</span>
              <span role="columnheader">Level</span>
              <span role="columnheader">Application</span>
              <span role="columnheader">Message</span>
              <span role="columnheader">Trace</span>
            </div>
          </div>
          <div className="font-mono text-xs" role="rowgroup">
            {entries.map((entry) => (
              <button
                className={`grid min-w-[980px] w-full grid-cols-[11rem_5.5rem_12rem_minmax(24rem,1fr)_12rem] items-start border-l-2 border-b border-border/50 px-3 py-1.5 text-left transition hover:bg-surface-raised ${
                  selectedEntryId === entry.id
                    ? "border-l-primary bg-primary/10"
                    : "border-l-transparent odd:bg-surface/15"
                }`}
                key={entry.id}
                onClick={() => onSelect(entry)}
                role="row"
                type="button"
              >
                <span className="whitespace-nowrap text-muted" role="cell">
                  {entry.timestamp}
                </span>
                <span role="cell">
                  <span
                    className={`inline-flex min-w-16 justify-center rounded border px-1.5 py-0.5 text-[11px] font-semibold ${levelClasses[entry.level]}`}
                  >
                    {entry.level}
                  </span>
                </span>
                <span className="truncate pr-4 text-text" role="cell">
                  {entry.applicationName}
                </span>
                <span
                  className={`pr-4 leading-5 text-text ${
                    wrapLines ? "whitespace-pre-wrap break-words" : "truncate"
                  }`}
                  role="cell"
                >
                  {highlightKeyword(entry.message, keyword)}
                </span>
                <span className="truncate text-muted" role="cell">
                  {entry.traceId || "n/a"}
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function highlightKeyword(message: string, keyword: string) {
  const normalizedKeyword = keyword.trim();
  if (!normalizedKeyword) {
    return message;
  }

  const lowerMessage = message.toLowerCase();
  const lowerKeyword = normalizedKeyword.toLowerCase();
  const parts: ReactNode[] = [];
  let cursor = 0;
  let matchIndex = lowerMessage.indexOf(lowerKeyword);

  while (matchIndex !== -1) {
    if (matchIndex > cursor) {
      parts.push(message.slice(cursor, matchIndex));
    }

    const end = matchIndex + normalizedKeyword.length;
    parts.push(
      <mark className="rounded bg-warning/25 px-0.5 text-warning" key={matchIndex}>
        {message.slice(matchIndex, end)}
      </mark>
    );
    cursor = end;
    matchIndex = lowerMessage.indexOf(lowerKeyword, cursor);
  }

  if (cursor < message.length) {
    parts.push(message.slice(cursor));
  }

  return parts;
}
