import { NavLink } from "react-router-dom";
import { AppIcon } from "@/shared/components/AppIcon";
import {
  formatNavigationBadge,
  type NavigationItem
} from "@/shared/layouts/navigation";

type SidebarItemProps = {
  item: NavigationItem;
  badgeCount?: number;
  onNavigate?: () => void;
};

const itemClasses =
  "group relative flex min-h-10 w-full items-center gap-3 rounded-md border-l-2 px-3 py-2 text-left text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70";

function ItemContent({
  item,
  badgeCount
}: Pick<SidebarItemProps, "item" | "badgeCount">) {
  const badge = formatNavigationBadge(badgeCount);

  return (
    <>
      <AppIcon
        className="shrink-0 text-current"
        name={item.icon}
        size={19}
      />
      <span className="min-w-0 flex-1 truncate">{item.label}</span>
      {badge ? (
        <span className="min-w-5 rounded-full bg-primary/15 px-1.5 py-0.5 text-center text-[10px] font-semibold leading-4 text-primary">
          {badge}
        </span>
      ) : null}
    </>
  );
}

export default function SidebarItem({
  item,
  badgeCount,
  onNavigate
}: SidebarItemProps) {
  if (item.to) {
    return (
      <NavLink
        className={({ isActive }) =>
          `${itemClasses} ${
            isActive
              ? "border-primary bg-primary/10 font-medium text-primary"
              : "border-transparent text-muted hover:bg-surface-raised hover:text-text"
          }`
        }
        end={item.to === "/"}
        onClick={onNavigate}
        to={item.to}
      >
        <ItemContent badgeCount={badgeCount} item={item} />
      </NavLink>
    );
  }

  return (
    <button
      aria-label={`${item.label}, currently unavailable`}
      className={`${itemClasses} border-transparent text-muted hover:bg-surface-raised hover:text-text`}
      type="button"
    >
      <ItemContent badgeCount={badgeCount} item={item} />
    </button>
  );
}
