# Shared Application Layout Implementation Plan

**Source spec:** `docs/specs/shared-application-layout-design.md`

**Goal:** Implement one responsive, role-aware dark application shell for all
authenticated frontend routes.

**Architecture:** Nest `AppLayout` beneath `ProtectedRoute` so authentication
remains the outer boundary and all authenticated feature pages share one shell.
Use typed navigation configuration for role filtering, a small page-header
context for feature-owned titles/actions, and Tailwind CSS v4 tokens for the
visual system. Implement mobile drawer focus behavior locally without adding a
general UI framework.

**Tech stack:** React 19, TypeScript 5.9, React Router 7, Tailwind CSS 4, Vite
8, Vitest, Testing Library, jsdom.

---

## File Map

- Modify: `apps/frontend/package.json` — add test scripts and frontend test
  dependencies.
- Modify: `apps/frontend/package-lock.json` — lock the installed test
  dependencies.
- Create: `apps/frontend/vitest.config.ts` — configure jsdom, aliases, and
  shared test setup.
- Create: `apps/frontend/src/test/setup.ts` — install DOM matchers and browser
  API shims used by layout tests.
- Modify: `apps/frontend/src/styles/global.css` — align Tailwind v4 theme
  tokens and shared base behavior with the approved dark shell.
- Create: `apps/frontend/public/logpulse-logo.png` — runtime copy of the
  approved transparent product logo.
- Create: `apps/frontend/src/shared/components/AppIcon.tsx` — dependency-free
  decorative SVG icons used by navigation and shell controls.
- Create: `apps/frontend/src/shared/layouts/navigation.ts` — typed navigation
  groups, role filtering, implemented route metadata, and badge formatting.
- Create: `apps/frontend/src/shared/layouts/navigation.test.ts` — navigation
  role, route, and badge behavior.
- Create: `apps/frontend/src/shared/layouts/page-header-context.tsx` —
  portal-based feature contract for supplying title and optional header
  actions without effect-driven provider updates.
- Create: `apps/frontend/src/shared/layouts/page-header-context.test.tsx` —
  context registration and cleanup behavior.
- Create: `apps/frontend/src/shared/layouts/SidebarItem.tsx` — semantic
  implemented/unimplemented navigation item rendering.
- Create: `apps/frontend/src/shared/layouts/SidebarSection.tsx` — always-open
  navigation group rendering.
- Create: `apps/frontend/src/shared/layouts/Sidebar.tsx` — branding,
  role-filtered navigation, badges, and desktop/mobile navigation callbacks.
- Create: `apps/frontend/src/shared/layouts/MobileSidebarDrawer.tsx` —
  accessible overlay, focus trap, Escape, and backdrop dismissal.
- Create: `apps/frontend/src/shared/layouts/Sidebar.test.tsx` — navigation
  visibility, active state, inert items, badge, and drawer interaction tests.
- Create: `apps/frontend/src/shared/layouts/UserMenu.tsx` — avatar identity,
  role label, account menu, and logout.
- Create: `apps/frontend/src/shared/layouts/Topbar.tsx` — sidebar toggle, page
  title/actions, and account placement.
- Create: `apps/frontend/src/shared/layouts/Topbar.test.tsx` — title/actions,
  identity, menu, and logout behavior.
- Modify: `apps/frontend/src/shared/layouts/AppLayout.tsx` — compose responsive
  shell state, sidebar, topbar, page-header provider, and routed outlet.
- Create: `apps/frontend/src/shared/layouts/AppLayout.test.tsx` — shell
  composition, responsive defaults, toggling, resize behavior, and layout
  exclusions.
- Modify: `apps/frontend/src/app/router.tsx` — nest current authenticated
  routes under `AppLayout`.
- Modify: `apps/frontend/src/features/dashboard/DashboardPage.tsx` — register
  the `Overview` page title while leaving page content otherwise unchanged.
- Modify: `apps/frontend/src/features/live-logs/LiveLogsPage.tsx` — register the
  `Live Logs` page title while leaving page content otherwise unchanged.

## Task 1: Establish Frontend Layout Test Infrastructure

**Files:**

- Modify: `apps/frontend/package.json`
- Modify: `apps/frontend/package-lock.json`
- Create: `apps/frontend/vitest.config.ts`
- Create: `apps/frontend/src/test/setup.ts`

- [x] **Step 1: Install the verified test stack**

Run from `apps/frontend`:

```sh
npm install --save-dev vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

Expected: `package.json` and `package-lock.json` contain the five development
dependencies and npm completes without peer-dependency errors against the
existing React 19 and Vite 8 installation.

- [x] **Step 2: Add repeatable test scripts**

Add these scripts to `apps/frontend/package.json`:

```json
{
  "test": "vitest",
  "test:run": "vitest run"
}
```

Keep the existing `api:generate`, `dev`, `build`, `lint`, and `preview`
scripts unchanged.

- [x] **Step 3: Configure Vitest**

Create `apps/frontend/vitest.config.ts`:

```ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: true
  }
});
```

- [x] **Step 4: Add shared DOM setup and a controllable `matchMedia` shim**

Create `apps/frontend/src/test/setup.ts` with:

```ts
import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

afterEach(() => cleanup());

Object.defineProperty(window, "matchMedia", {
  configurable: true,
  writable: true,
  value: (query: string): MediaQueryList => ({
    matches: query === "(min-width: 768px)",
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => true
  })
});
```

Individual responsive tests may replace this property with a listener-aware
mock and must restore it after the test.

- [x] **Step 5: Verify the test runner and existing quality gates**

Run:

```sh
npm run test:run -- --passWithNoTests
npm run lint
npm run build
```

Expected: all three commands pass. The first reports no test files yet rather
than failing.

## Task 2: Add Dark Theme Tokens, Product Asset, Icons, and Navigation Model

**Files:**

- Modify: `apps/frontend/src/styles/global.css`
- Create: `apps/frontend/public/logpulse-logo.png`
- Create: `apps/frontend/src/shared/components/AppIcon.tsx`
- Create: `apps/frontend/src/shared/layouts/navigation.ts`
- Create: `apps/frontend/src/shared/layouts/navigation.test.ts`

- [x] **Step 1: Add focused failing tests for navigation behavior**

Create `apps/frontend/src/shared/layouts/navigation.test.ts` covering:

```ts
import { describe, expect, it } from "vitest";
import {
  formatNavigationBadge,
  getNavigationGroups,
  navigationGroups
} from "@/shared/layouts/navigation";

describe("navigation", () => {
  it("shows every group to an admin", () => {
    expect(getNavigationGroups("ADMIN").map((group) => group.label)).toEqual([
      "Monitoring",
      "Analytics",
      "Resources",
      "Administration"
    ]);
  });

  it("hides administration from an engineer or unknown role", () => {
    expect(getNavigationGroups("ENGINEER").map((group) => group.label)).toEqual([
      "Monitoring",
      "Analytics",
      "Resources"
    ]);
    expect(getNavigationGroups(undefined).some(
      (group) => group.label === "Administration"
    )).toBe(false);
  });

  it("defines only Overview and Live Logs as implemented routes", () => {
    const implemented = navigationGroups
      .flatMap((group) => group.items)
      .filter((item) => item.to !== undefined)
      .map((item) => [item.label, item.to]);

    expect(implemented).toEqual([
      ["Overview", "/"],
      ["Live Logs", "/logs"]
    ]);
  });

  it.each([
    [undefined, null],
    [0, null],
    [1, "1"],
    [99, "99"],
    [100, "99+"]
  ])("formats badge count %s as %s", (count, expected) => {
    expect(formatNavigationBadge(count)).toBe(expected);
  });
});
```

- [x] **Step 2: Verify the expected failure**

Run:

```sh
npm run test:run -- src/shared/layouts/navigation.test.ts
```

Expected: FAIL because `navigation.ts` does not exist.

- [x] **Step 3: Define the typed navigation configuration**

Create `apps/frontend/src/shared/layouts/navigation.ts` with:

- `UserRole = "ADMIN" | "ENGINEER"`.
- Stable item IDs:
  `overview`, `live-logs`, `log-search`, `alerts`, `incidents`,
  `application-health`, `ai-insights`, `applications`, `users-access`,
  `alert-rules`, `notification-channels`, `retention-policies`,
  `system-operations`, and `settings`.
- `NavigationItem` containing `id`, `label`, `icon`, and optional `to`.
- `NavigationGroup` containing `label`, optional `roles`, and `items`.
- The exact four approved groups and English labels from the source spec.
- Only `Overview` (`/`) and `Live Logs` (`/logs`) receive `to` values.
- Administration receives `roles: ["ADMIN"]`.
- `getNavigationGroups(role)` filters unauthorized groups without mutating the
  exported configuration.
- `formatNavigationBadge(count)` returns `null` for missing/non-positive
  values, the decimal value for `1..99`, and `99+` for `>=100`.

Use `as const satisfies readonly NavigationGroup[]` so labels and IDs remain
type-safe without widening the configuration.

- [x] **Step 4: Add dependency-free shell icons**

Create `apps/frontend/src/shared/components/AppIcon.tsx`:

- Export an `AppIconName` union matching every navigation icon plus
  `menu`, `close`, `chevron-down`, and `logout`.
- Render a `20x20` inline `<svg>` using `currentColor`, `fill="none"`,
  `strokeWidth={1.75}`, rounded line caps, and icon-specific `<path>` or
  `<circle>` nodes.
- Accept `name`, optional `className`, and optional `size`.
- Set `aria-hidden="true"` and `focusable="false"` because every icon has a
  visible or separately accessible label.
- Do not add an icon package.

- [x] **Step 5: Copy the approved logo into the Vite public root**

Run from the repository root:

```sh
cp docs/assets/logo-transparent.png apps/frontend/public/logpulse-logo.png
```

Expected: `apps/frontend/public/logpulse-logo.png` is a valid transparent PNG.
The implementation must reference it as `/logpulse-logo.png`; do not import
files from `docs/` at runtime.

- [x] **Step 6: Align Tailwind v4 tokens and global base styles**

Update the existing `@theme` block in
`apps/frontend/src/styles/global.css`:

```css
@theme {
  --color-background: #0b0d0f;
  --color-sidebar: #0d0e0f;
  --color-header: #121415;
  --color-surface: #1a1c1d;
  --color-surface-raised: #202326;
  --color-border: #2d3139;
  --color-primary: #3b82f6;
  --color-primary-hover: #60a5fa;
  --color-text: #e5e7eb;
  --color-muted: #9ca3af;
  --color-success: #22c55e;
  --color-warning: #f59e0b;
  --color-error: #ef4444;
  --font-sans: "Inter", "Segoe UI", sans-serif;
  --font-mono: "JetBrains Mono", Consolas, monospace;
}
```

Also:

- set `html` and `body` to the approved background;
- give `body` `min-h-screen`, `text-text`, and `overflow-hidden`;
- set `#root` to `min-height: 100svh`;
- include `button`, `input`, and `a` font/tap behavior already present;
- add a thin dark scrollbar style used by sidebar/main overflow;
- add a `prefers-reduced-motion: reduce` rule that disables nonessential
  transition duration;
- do not add a dotted/grid page background.

- [x] **Step 7: Verify the foundation**

Run:

```sh
npm run test:run -- src/shared/layouts/navigation.test.ts
npm run lint
npm run build
```

Expected: navigation tests pass; lint and production build pass.

## Task 3: Add the Feature-Owned Page Header Slot Contract

**Files:**

- Create: `apps/frontend/src/shared/layouts/page-header-context.tsx`
- Create: `apps/frontend/src/shared/layouts/page-header-context.test.tsx`

- [x] **Step 1: Add failing tests for declarative title/action slots**

Create `apps/frontend/src/shared/layouts/page-header-context.test.tsx` with a
test harness that renders `PageHeaderProvider`, two slot hosts, and a routed
child rendering `PageHeader`.

Cover:

- empty slot hosts render no page-owned content;
- `<PageHeader title="Overview" />` portals the title into the title host;
- passing a button portals it into the actions host;
- unmounting `PageHeader` removes both portals so content does not leak to the
  next route;
- rendering `PageHeader` outside `PageHeaderProvider` throws a clear error.

- [x] **Step 2: Verify the expected failure**

Run:

```sh
npm run test:run -- src/shared/layouts/page-header-context.test.tsx
```

Expected: FAIL because the page-header context does not exist.

- [x] **Step 3: Implement portal targets and declarative page metadata**

Create `apps/frontend/src/shared/layouts/page-header-context.tsx` exporting:

```ts
export type PageHeaderSlots = {
  titleTarget: HTMLElement | null;
  actionsTarget: HTMLElement | null;
  setTitleTarget: (element: HTMLElement | null) => void;
  setActionsTarget: (element: HTMLElement | null) => void;
};

export function PageHeaderProvider({ children }: PropsWithChildren): JSX.Element;
export function usePageHeaderSlots(): PageHeaderSlots;
export function PageHeader(props: {
  title: string;
  actions?: ReactNode;
}): ReactNode;
```

Implementation requirements:

- provider owns the current title/actions host elements, initially `null`;
- provider exposes stable callback refs named `setTitleTarget` and
  `setActionsTarget` for `Topbar` to attach to its two host elements;
- `usePageHeaderSlots` returns the current targets/callback refs and throws a
  clear error outside the provider;
- `PageHeader` uses `createPortal` from `react-dom` to render its title and
  optional actions directly into available targets;
- when a target is not mounted yet, `PageHeader` returns no portal for that
  target; the provider rerender after callback-ref attachment makes the target
  available;
- portal unmounting automatically removes stale route content;
- do not register page metadata by calling a React state setter from
  `useEffect` or `useLayoutEffect`;
- memoize the context value so unrelated shell state does not recreate it.

- [x] **Step 4: Verify the context**

Run:

```sh
npm run test:run -- src/shared/layouts/page-header-context.test.tsx
npm run lint
```

Expected: tests and lint pass.

## Task 4: Implement Role-Aware Sidebar and Accessible Mobile Drawer

**Files:**

- Create: `apps/frontend/src/shared/layouts/SidebarItem.tsx`
- Create: `apps/frontend/src/shared/layouts/SidebarSection.tsx`
- Create: `apps/frontend/src/shared/layouts/Sidebar.tsx`
- Create: `apps/frontend/src/shared/layouts/MobileSidebarDrawer.tsx`
- Create: `apps/frontend/src/shared/layouts/Sidebar.test.tsx`

- [x] **Step 1: Add failing sidebar and drawer tests**

Create `apps/frontend/src/shared/layouts/Sidebar.test.tsx` using
`MemoryRouter` and Testing Library. Cover:

- Admin sees `Monitoring`, `Analytics`, `Resources`, and `Administration`.
- Engineer does not see `Administration` or any of its items.
- Unknown role also hides Administration.
- `/logs` gives `Live Logs` `aria-current="page"` while `Overview` is not
  active.
- Clicking implemented `Overview` navigates to `/`.
- Clicking unimplemented `Log Search` leaves location and active item
  unchanged.
- Unimplemented items are buttons whose accessible name includes
  `currently unavailable`, while visible text remains only the approved label.
- Alerts badge is absent for `0`, shows `4`, and shows `99+` for `100`.
- Mobile drawer closes through Escape, backdrop, explicit close button, and
  an implemented navigation selection.
- An open mobile drawer contains exactly one element with
  `id="application-sidebar"`.
- Focus moves into the open drawer, Tab/Shift+Tab wrap between its first and
  last controls, and closing restores focus to the supplied toggle button.

- [x] **Step 2: Verify the expected failure**

Run:

```sh
npm run test:run -- src/shared/layouts/Sidebar.test.tsx
```

Expected: FAIL because sidebar components do not exist.

- [x] **Step 3: Implement semantic navigation items**

Create `SidebarItem.tsx` with these props:

```ts
type SidebarItemProps = {
  item: NavigationItem;
  badgeCount?: number;
  onNavigate?: () => void;
};
```

Behavior:

- implemented items render `NavLink`;
- active links receive `aria-current` from React Router and approved blue
  text/background/2px left marker classes;
- unimplemented items render `<button type="button">`;
- their `aria-label` is `${label}, currently unavailable`;
- button click performs no state or route change;
- both variants render the same visible icon, label, height, hover, and focus
  treatment;
- badge output comes only from `formatNavigationBadge`.

Create `SidebarSection.tsx` to render an always-visible uppercase/muted group
heading and its item list. Do not add accordion state.

- [x] **Step 4: Implement the shared sidebar**

Create `Sidebar.tsx` with:

```ts
type SidebarProps = {
  id?: string;
  role?: string;
  badgeCounts?: Partial<Record<NavigationItemId, number>>;
  onNavigate?: () => void;
  onRequestClose?: () => void;
  showCloseButton?: boolean;
};
```

Requirements:

- render `/logpulse-logo.png` in a `32x32` `object-contain` box and adjacent
  `LogPulse` text;
- pass the optional `id` prop to the root `<aside>` and do not create a
  hard-coded ID inside `Sidebar`;
- do not render subtitle, version, account identity, system status, search,
  theme controls, or notification bell;
- normalize role by passing only exact `ADMIN`/`ENGINEER` values to
  `getNavigationGroups`; all other values are unprivileged;
- render every approved group expanded;
- allow sidebar body scrolling without moving the brand block;
- invoke `onNavigate` only after an implemented link selection;
- render an accessible close button only when `showCloseButton` is true.

- [x] **Step 5: Implement the mobile drawer**

Create `MobileSidebarDrawer.tsx` with:

```ts
type MobileSidebarDrawerProps = {
  open: boolean;
  onClose: () => void;
  returnFocusRef: RefObject<HTMLButtonElement | null>;
  children: ReactNode;
};
```

Requirements:

- render nothing when closed;
- render a fixed backdrop and a `role="dialog"` panel with
  `aria-modal="true"` and `aria-label="Application navigation"`;
- assign `id="application-sidebar"` to the drawer panel; desktop and mobile
  sidebar instances are mutually exclusive, and the nested mobile `Sidebar`
  receives no `id`, so the document never contains duplicate IDs;
- panel width is `min(280px, calc(100vw - 32px))`;
- clicking backdrop or pressing Escape closes it;
- move focus to the first focusable drawer control on open;
- trap Tab and Shift+Tab within focusable controls;
- restore focus to `returnFocusRef.current` after close;
- lock body overflow while open and restore the previous value on cleanup;
- use `md:hidden` so this overlay cannot remain visually active on desktop.

- [x] **Step 6: Verify sidebar behavior**

Run:

```sh
npm run test:run -- src/shared/layouts/Sidebar.test.tsx
npm run lint
npm run build
```

Expected: all sidebar/drawer tests pass and production checks remain green.

## Task 5: Implement the Header and Account Menu

**Files:**

- Create: `apps/frontend/src/shared/layouts/UserMenu.tsx`
- Create: `apps/frontend/src/shared/layouts/Topbar.tsx`
- Create: `apps/frontend/src/shared/layouts/Topbar.test.tsx`

- [x] **Step 1: Add failing header and account tests**

Create `apps/frontend/src/shared/layouts/Topbar.test.tsx`. Cover:

- title and supplied page action render;
- sidebar toggle has an accessible label and correct `aria-expanded`;
- avatar fallback uses initials from display name, or email when display name
  is absent;
- full display name and normalized `Admin`/`Engineer` role render on desktop;
- long names use truncation classes and preserve the full value through
  `title`;
- account trigger opens a menu and Escape closes it;
- clicking outside closes the menu;
- logout calls the supplied callback once;
- forbidden shared controls/text are absent: search, notifications, theme
  switch, breadcrumbs, version, and system status.

- [x] **Step 2: Verify the expected failure**

Run:

```sh
npm run test:run -- src/shared/layouts/Topbar.test.tsx
```

Expected: FAIL because `Topbar` and `UserMenu` do not exist.

- [x] **Step 3: Implement the account menu**

Create `UserMenu.tsx` with:

```ts
type UserMenuProps = {
  displayName?: string;
  email?: string;
  role?: string;
  onLogout: () => void;
};
```

Requirements:

- visible fallback name order: trimmed `displayName`, then `email`, then
  `Account`;
- derive up to two uppercase initials from the fallback name;
- display role as `Admin`, `Engineer`, or `User` for unknown/missing values;
- desktop trigger shows avatar, truncated name, role, and chevron;
- narrow screens retain the avatar trigger and expose full identity inside the
  menu;
- trigger uses `aria-haspopup="menu"` and `aria-expanded`;
- menu closes on Escape, outside pointer interaction, or logout;
- logout item has `role="menuitem"` and calls the existing auth logout
  callback;
- restore focus to the trigger when the menu closes by keyboard.

- [x] **Step 4: Implement the topbar**

Create `Topbar.tsx` with:

```ts
type TopbarProps = {
  sidebarOpen: boolean;
  onToggleSidebar: () => void;
  user: UserResponse;
  onLogout: () => void;
  toggleRef: RefObject<HTMLButtonElement | null>;
};
```

At the top of `Topbar.tsx`, define the generated schema alias explicitly:

```ts
import type { components } from "@/api/generated/api-types";

type UserResponse = components["schemas"]["UserResponse"];
```

Requirements:

- fixed `64px` desktop height and compact mobile spacing;
- call `usePageHeaderSlots()` to obtain `setTitleTarget` and
  `setActionsTarget`;
- left side contains only toggle and an empty title host connected to
  `setTitleTarget`;
- an empty actions host connected to `setActionsTarget` renders beside it and
  may wrap on narrow screens;
- right side contains only `UserMenu`;
- toggle exposes `aria-expanded`, `aria-controls="application-sidebar"`, and
  `Show navigation`/`Hide navigation` label;
- do not add search, breadcrumb, notification bell, theme switch, version, or
  system-health/realtime state.

- [x] **Step 5: Verify header behavior**

Run:

```sh
npm run test:run -- src/shared/layouts/Topbar.test.tsx
npm run lint
npm run build
```

Expected: header/account tests, lint, and build pass.

## Task 6: Compose the Responsive App Shell and Wire Authenticated Routes

**Files:**

- Modify: `apps/frontend/src/shared/layouts/AppLayout.tsx`
- Create: `apps/frontend/src/shared/layouts/AppLayout.test.tsx`
- Modify: `apps/frontend/src/app/router.tsx`
- Modify: `apps/frontend/src/features/dashboard/DashboardPage.tsx`
- Modify: `apps/frontend/src/features/live-logs/LiveLogsPage.tsx`

- [x] **Step 1: Add failing app-shell tests**

Create `apps/frontend/src/shared/layouts/AppLayout.test.tsx` with helpers that:

- provide a mock `AuthContext` session containing a typed user;
- render `AppLayout` in a `MemoryRouter` route tree with child routes;
- replace `matchMedia` with a listener-aware mock for desktop/mobile cases.

Cover:

- desktop initial render shows the sidebar and applies the `240px` content
  offset state;
- desktop toggle hides sidebar entirely and removes the offset; toggling again
  restores it;
- a fresh remount starts desktop open, proving hidden state is not persisted;
- mobile initial render has no open drawer;
- mobile toggle opens the drawer and leaves the content behind the backdrop;
- resizing mobile to desktop closes the drawer and restores the desktop
  sidebar open;
- Admin and Engineer sessions produce the correct group visibility;
- routed child content renders in the full-width main region;
- registered page title and actions appear in `Topbar`;
- logout clears the session through the supplied auth callback;
- shell markup contains one `header`, one `nav`, and one `main`, and does not
  contain the excluded global controls.

- [x] **Step 2: Verify the expected failure**

Run:

```sh
npm run test:run -- src/shared/layouts/AppLayout.test.tsx
```

Expected: FAIL because `AppLayout.tsx` is empty.

- [x] **Step 3: Compose `AppLayout`**

Implement `AppLayout.tsx`:

- read `session` and `logout` through `useAuth`;
- initialize `desktopSidebarOpen` from
  `window.matchMedia("(min-width: 768px)").matches`;
- initialize mobile drawer closed;
- subscribe to the same media query:
  - entering desktop closes mobile drawer and opens desktop sidebar;
  - entering mobile closes desktop sidebar and mobile drawer;
- do not read or write `localStorage`;
- maintain a ref to the topbar toggle for drawer focus restoration;
- wrap shell consumers in `PageHeaderProvider`;
- render desktop `Sidebar` with `id="application-sidebar"` in a fixed `240px`
  column only when open;
- render `MobileSidebarDrawer` only for the mobile state;
- render the nested mobile `Sidebar` without an `id` because the drawer panel
  owns `id="application-sidebar"`;
- render `Topbar` and `main` with matching responsive left offsets;
- use `transition-[padding-left] duration-200` and respect the global
  reduced-motion override;
- set `main` to full width, `min-w-0`, vertical overflow, and responsive
  padding `12px / 16px / 24px`;
- render `<Outlet />` inside `main`;
- pass no badge counts until alert/incident feature data exists;
- do not infer realtime or infrastructure state.

Use an internal child such as `AppLayoutContent` beneath
`PageHeaderProvider`, because `Topbar` must consume the provider's title and
actions host callbacks while routed pages portal content into those hosts.

- [x] **Step 4: Nest the authenticated router under the layout**

Update `apps/frontend/src/app/router.tsx` to this structure:

```tsx
{
  element: <ProtectedRoute />,
  children: [
    {
      element: <AppLayout />,
      children: [
        {
          path: "/",
          lazy: () => import("@/features/dashboard/DashboardPage")
        },
        {
          path: "/logs",
          lazy: () => import("@/features/live-logs/LiveLogsPage")
        }
      ]
    }
  ]
}
```

Import `AppLayout` from `@/shared/layouts/AppLayout`. Remove the commented-out
route block rather than carrying obsolete placeholders into the new route
tree. Do not create routes for unimplemented sidebar items.

- [x] **Step 5: Register current page titles**

Update `DashboardPage.tsx`:

```tsx
import { PageHeader } from "@/shared/layouts/page-header-context";

export function Component() {
  return (
    <>
      <PageHeader title="Overview" />
      <h1>Dashboard</h1>
    </>
  );
}
```

Update `LiveLogsPage.tsx` equivalently with
`<PageHeader title="Live Logs" />`.
Do not redesign either page's feature content in this task.

- [x] **Step 6: Verify the complete shell**

Run:

```sh
npm run test:run
npm run lint
npm run build
```

Expected: every layout/navigation test passes, ESLint reports no errors, and
Vite produces the production build.

## Task 7: Perform Browser-Level Responsive and Accessibility Verification

**Files:**

- No production file changes expected.
- Modify only a task-owned layout/test file if verification exposes a defect
  in the approved behavior.

- [x] **Step 1: Start the existing frontend development server**

Run from `apps/frontend`:

```sh
npm run dev
```

Expected: Vite serves the app at `http://localhost:5173`. Use an authenticated
local session or the existing login flow; backend services are required only
to obtain/restore a real session.

- [ ] **Step 2: Verify desktop at approximately `1440px` and `1024px`**

Confirm:

- sidebar starts open at `240px`;
- sidebar pushes header and content and never covers them;
- toggle hides it completely and content fills the viewport;
- refresh restores the sidebar open;
- all groups stay expanded;
- Admin sees Administration and Engineer does not;
- Overview and Live Logs navigate and show active state;
- unimplemented items look normal but do nothing;
- long navigation content scrolls within the sidebar;
- no dotted background, search, bell, theme switch, breadcrumb, version, or
  global system status is present.

- [ ] **Step 3: Verify mobile at approximately `390px` and the `768px` boundary**

Confirm:

- drawer starts closed below `768px`;
- toggle opens a drawer no wider than `280px`;
- drawer overlays rather than pushes mobile content;
- Escape, backdrop, close button, and implemented navigation close it;
- focus stays inside while open and returns to the toggle after close;
- resizing from mobile to desktop restores the open pushed sidebar;
- title truncation, compact account trigger, and wrapped page actions do not
  overlap.

- [ ] **Step 4: Verify account and keyboard behavior**

Confirm:

- avatar, name, and role appear at desktop width;
- full identity is available from the compact mobile menu;
- Tab reaches toggle, navigation, page actions, and account menu in logical
  order;
- focus indicators are visible;
- logout returns to the existing protected login behavior;
- reduced-motion emulation removes or shortens shell transitions.

- [x] **Step 5: Run final automated regression checks**

Stop the development server, then run:

```sh
npm run test:run
npm run lint
npm run build
```

Expected: all commands pass after browser verification.

## Final Verification

Run from `apps/frontend`:

```sh
npm run test:run
npm run lint
npm run build
```

Expected:

- Vitest passes navigation, header context, sidebar/drawer, topbar/account, and
  composed shell tests.
- ESLint reports no errors.
- TypeScript and Vite complete a production build.
- Existing `/` and `/logs` authenticated routes render through `AppLayout`.

Manual verification matrix:

| Viewport | Role | Expected shell |
| --- | --- | --- |
| `1440px` | Admin | Open pushed sidebar with all four groups |
| `1024px` | Engineer | Open pushed sidebar without Administration |
| `768px` | Admin | Desktop pushed sidebar behavior |
| `390px` | Engineer | Closed-by-default overlay drawer |

No Kafka, Redis, ClickHouse, WebSocket, or Telegram service is required for
automated layout tests. A running backend is needed only for manual login with
a real account.

## Spec Coverage

| Spec requirement / acceptance criterion | Planned implementation | Verification |
| --- | --- | --- |
| One reusable authenticated `AppLayout` | Task 6 route nesting and shell composition | Task 6 app-shell tests and final build |
| Tailwind v4 dark palette and tonal surfaces | Task 2 global theme tokens | Task 2 build and Task 7 visual checks |
| Product logo and `LogPulse`, no subtitle/version | Task 2 asset; Task 4 sidebar | Sidebar tests and Task 7 desktop check |
| Desktop `240px` sidebar pushes content | Task 6 responsive composition | AppLayout tests and Task 7 desktop matrix |
| Manual hide, no icon-only rail, no persistence | Tasks 4 and 6 | Toggle/remount tests and desktop refresh check |
| Mobile accessible drawer below `768px` | Tasks 4 and 6 | Drawer focus/dismissal tests and mobile check |
| Always-expanded English navigation groups | Tasks 2 and 4 | Navigation/sidebar tests |
| Admin sees Administration; Engineer/unknown does not | Tasks 2, 4, and 6 | Navigation, Sidebar, and AppLayout tests |
| Existing routes navigate and become active | Tasks 2, 4, and 6 | Sidebar route tests and final route check |
| Unimplemented modules remain visible and inert | Tasks 2 and 4 | Sidebar mouse/keyboard tests |
| Alerts/Incidents badge slot capped at `99+` | Tasks 2 and 4 | Navigation formatting and Sidebar tests |
| Header contains only toggle, title/actions, account | Tasks 3 and 5 | Topbar tests and Task 7 visual checks |
| No search, bell, theme, breadcrumb, version, health | Tasks 4–6 explicit exclusions | Topbar/AppLayout absence assertions |
| Avatar, display name, role, logout at header right | Task 5 | Topbar/UserMenu tests and keyboard check |
| Full-width main content with responsive padding | Task 6 | AppLayout state tests and viewport checks |
| Page-owned title and action extension point | Tasks 3 and 6 | Context, Topbar, and AppLayout tests |
| API keys remain under Application detail | Navigation model omits API Key item | Navigation configuration test |
| Feature-local realtime and System Operations health | Tasks 4–6 add no global status | Absence assertions and visual checks |
| Backend remains authorization boundary | Role filtering is presentation-only; no API changes | File map/scope review and unchanged backend |
| Existing auth and route behavior remains functional | Task 6 preserves `ProtectedRoute` and `useAuth` | AppLayout logout test and final route check |
