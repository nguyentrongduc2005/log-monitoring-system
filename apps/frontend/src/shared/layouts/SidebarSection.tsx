import SidebarItem from "@/shared/layouts/SidebarItem";
import type {
  NavigationGroup,
  NavigationItemId
} from "@/shared/layouts/navigation";

type SidebarSectionProps = {
  group: NavigationGroup;
  badgeCounts?: Partial<Record<NavigationItemId, number>>;
  onNavigate?: () => void;
};

export default function SidebarSection({
  group,
  badgeCounts,
  onNavigate
}: SidebarSectionProps) {
  return (
    <section aria-labelledby={`sidebar-${group.label.toLowerCase()}`}>
      <h2
        className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-muted/70"
        id={`sidebar-${group.label.toLowerCase()}`}
      >
        {group.label}
      </h2>
      <div className="space-y-0.5">
        {group.items.map((item) => (
          <SidebarItem
            badgeCount={badgeCounts?.[item.id]}
            item={item}
            key={item.id}
            onNavigate={onNavigate}
          />
        ))}
      </div>
    </section>
  );
}
