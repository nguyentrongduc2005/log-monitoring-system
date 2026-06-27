import { NavLink } from "react-router-dom";
import { AppIcon } from "@/shared/components/AppIcon";
import {
  formatNavigationBadge,
  type NavigationItem,
} from "@/shared/layouts/navigation";

type SidebarItemProps = {
  item: NavigationItem;
  badgeCount?: number;
  onNavigate?: () => void;
};

const itemClasses =
  "group relative flex min-h-9 w-full items-center gap-2.5 rounded-md border px-2.5 py-2 text-left text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2]/70";

function ItemContent({
  item,
  badgeCount,
}: Pick<SidebarItemProps, "item" | "badgeCount">) {
  const badge = formatNavigationBadge(badgeCount);

  return (
    <>
      <AppIcon className="shrink-0 text-current" name={item.icon} size={18} />
      <span className="min-w-0 flex-1 truncate leading-5">{item.label}</span>
      {badge ? (
        <span className="min-w-5 rounded-full border border-[#5e6ad2]/30 bg-[#5e6ad2]/10 px-1.5 py-0.5 text-center text-[10px] font-semibold leading-4 text-[#828fff]">
          {badge}
        </span>
      ) : null}
    </>
  );
}

export default function SidebarItem({
  item,
  badgeCount,
  onNavigate,
}: SidebarItemProps) {
  if (item.to) {
    return (
      <NavLink
        className={({ isActive }) =>
          `${itemClasses} ${
            isActive
              ? "border-[#5e6ad2]/45 bg-[#5e6ad2]/10 font-medium text-[#f7f8f8]"
              : "border-transparent text-[#8a8f98] hover:border-[#34343a] hover:bg-[#18191a] hover:text-[#f7f8f8]"
          }`
        }
        end={item.to === "/" || item.to === "/logs"}
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
      className={`${itemClasses} border-transparent text-[#62666d] hover:border-[#34343a] hover:bg-[#18191a] hover:text-[#8a8f98]`}
      type="button"
    >
      <ItemContent badgeCount={badgeCount} item={item} />
    </button>
  );
}
