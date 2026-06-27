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
      className="flex h-full w-full flex-col border-r border-[#23252a] bg-[#0f1011] text-[#f7f8f8]"
      id={id}
    >
      <div className="flex h-14 shrink-0 items-center gap-3 border-b border-[#23252a] px-3">
        <div
          aria-hidden="true"
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md border border-primary/30 bg-primary/10 font-mono text-sm font-semibold text-primary-hover"
        >
          &gt;_
        </div>
        <div className="min-w-0 flex-1">
          <span className="block truncate text-sm font-semibold tracking-[-0.1px]">
            LogPulse
          </span>
          <span className="block truncate text-[11px] text-[#62666d]">
            AI log monitoring
          </span>
        </div>
        {showCloseButton ? (
          <button
            aria-label="Close navigation"
            className="rounded-md border border-transparent p-2 text-[#8a8f98] hover:border-[#34343a] hover:bg-[#18191a] hover:text-[#f7f8f8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2]/70"
            onClick={onRequestClose}
            type="button"
          >
            <AppIcon name="close" />
          </button>
        ) : null}
      </div>
      <nav
        aria-label="Primary navigation"
        className="shell-scrollbar flex-1 space-y-5 overflow-y-auto px-2.5 py-3"
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
