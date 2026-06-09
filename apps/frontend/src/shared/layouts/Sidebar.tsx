import { AppIcon } from "@/shared/components/AppIcon";
import SidebarSection from "@/shared/layouts/SidebarSection";
import {
  getNavigationGroups,
  type NavigationItemId,
  type UserRole
} from "@/shared/layouts/navigation";

type SidebarProps = {
  id?: string;
  role?: string;
  badgeCounts?: Partial<Record<NavigationItemId, number>>;
  onNavigate?: () => void;
  onRequestClose?: () => void;
  showCloseButton?: boolean;
};

function normalizeRole(role?: string): UserRole | undefined {
  return role === "ADMIN" || role === "ENGINEER" ? role : undefined;
}

export default function Sidebar({
  id,
  role,
  badgeCounts,
  onNavigate,
  onRequestClose,
  showCloseButton = false
}: SidebarProps) {
  const groups = getNavigationGroups(normalizeRole(role));

  return (
    <aside
      className="flex h-full w-full flex-col border-r border-border bg-sidebar text-text"
      id={id}
    >
      <div className="flex h-16 shrink-0 items-center gap-3 border-b border-border px-4">
        <img
          alt=""
          className="h-8 w-8 shrink-0 object-contain"
          src="/logpulse-logo.png"
        />
        <span className="min-w-0 flex-1 truncate text-base font-semibold tracking-tight">
          LogPulse
        </span>
        {showCloseButton ? (
          <button
            aria-label="Close navigation"
            className="rounded-md p-2 text-muted hover:bg-surface-raised hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onRequestClose}
            type="button"
          >
            <AppIcon name="close" />
          </button>
        ) : null}
      </div>
      <nav
        aria-label="Primary navigation"
        className="shell-scrollbar flex-1 space-y-5 overflow-y-auto px-2 py-4"
      >
        {groups.map((group) => (
          <SidebarSection
            badgeCounts={badgeCounts}
            group={group}
            key={group.label}
            onNavigate={onNavigate}
          />
        ))}
      </nav>
    </aside>
  );
}
