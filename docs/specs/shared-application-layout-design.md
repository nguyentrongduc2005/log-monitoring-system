# Shared Application Layout Design

## Problem

The frontend has authenticated routes and a dark visual foundation, but
`AppLayout.tsx` is empty and each Stitch screen repeats its own sidebar and
header. The repeated screen shells are not fully consistent: navigation
grouping varies, the header duplicates sidebar navigation, global search and
system status are shown without supporting product contracts, and role-based
visibility is not defined as a shared rule.

The application needs one reusable shell for all authenticated pages. This
specification covers only that shared shell, not the content of individual
feature pages.

## Goal

Create a compact, dark, desktop-first application layout that:

- gives every authenticated page a consistent sidebar, header, and content
  canvas;
- keeps the complete product navigation visible when allowed by role;
- uses the available screen width effectively for logs, tables, and charts;
- supports deliberate sidebar hiding without collapsing navigation to icons;
- provides clear extension points for page titles and page-specific actions;
- follows the existing React, React Router, TypeScript, and Tailwind CSS v4
  stack.

## Scope

### In scope

- Authenticated application shell.
- Desktop sidebar and mobile sidebar drawer.
- Header with page title, page actions, and account menu.
- Navigation grouping, labels, active state, badges, and role visibility.
- Responsive layout behavior.
- Dark layout tokens and shared interaction states.
- Layout-level loading and accessibility requirements.

### Out of scope

- Login page redesign.
- Content, filters, cards, tables, charts, drawers, and forms within feature
  pages.
- Feature route implementation beyond existing routes.
- Backend authorization changes.
- Application access assignment UI.
- Global search or command palette.
- Notification center or notification bell.
- Light theme and theme switching.
- Global system-health calculation.
- Realtime connection status outside pages that consume realtime data.
- Automatic sidebar mini/collapsed mode.

## Requirements

### Product language and branding

- All shared layout labels use English.
- The product name is `LogPulse`.
- The sidebar brand area displays the existing product logo and `LogPulse`.
- Do not show the `Observability Platform` subtitle or application version.

### Visual direction

- Use a dark control-center style optimized for dense operational data.
- Prefer tonal surfaces and low-contrast borders over shadows and glow.
- Do not use the dotted blueprint background from the current Stitch
  dashboard.
- Use Inter for navigation and interface text.
- Reserve JetBrains Mono for technical values rendered by feature pages.
- Use blue for active and interactive states.
- Reserve green, amber, and red for semantic state rather than decoration.
- Keep shared control radii between 4px and 8px.

Recommended shared tokens:

| Purpose | Value |
| --- | --- |
| Application background | `#0B0D0F` |
| Sidebar | `#0D0E0F` |
| Header | `#121415` |
| Raised surface | `#1A1C1D` |
| Border | `#2D3139` |
| Primary action/active | `#3B82F6` |
| Primary text | `#E5E7EB` |
| Secondary text | `#9CA3AF` |

These values should be expressed as Tailwind v4 theme tokens rather than
repeated arbitrary values in components.

## Proposed Design

### Application shell

The authenticated shell has three regions:

1. A left sidebar.
2. A top header aligned with the content area.
3. A full-width main content canvas below the header.

On desktop, opening the sidebar occupies layout width and pushes both the
header and main content to the right. Hiding it restores the full viewport to
the content. The sidebar never collapses to an icon-only rail.

The main canvas is full width. The shell does not impose a global maximum
content width because log streams, tables, and charts benefit from the
available space. Individual pages may constrain narrow forms locally.

Suggested dimensions:

| Element | Desktop | Tablet/mobile |
| --- | --- | --- |
| Sidebar width | `240px` | Drawer up to `280px` |
| Header height | `64px` | `56px` to `64px` |
| Content padding | `24px` | `16px`, then `12px` on narrow screens |

### Sidebar behavior

- At widths of `768px` and above, the sidebar is open by default on every
  fresh page load.
- The open/hidden choice is not persisted across reloads.
- A hamburger control toggles the sidebar.
- On desktop, toggling changes the document layout and never covers content.
- Below `768px`, the sidebar becomes an overlay drawer with a backdrop.
- Opening the drawer traps focus; Escape and backdrop activation close it.
- Navigation groups are always expanded and do not use accordions.
- The sidebar itself scrolls when its content exceeds viewport height.
- The account profile is not duplicated in the sidebar.

### Navigation information architecture

Navigation is intentionally shallow. The sidebar shows product modules only;
subfeatures such as API keys and application access remain inside their parent
feature pages.

#### Monitoring

- Overview
- Live Logs
- Log Search
- Alerts
- Incidents

#### Analytics

- Application Health
- AI Insights

#### Resources

- Applications

Application API keys belong to:

`Applications -> Application detail -> API Keys`

They do not receive a separate sidebar item.

#### Administration

- Users & Access
- Alert Rules
- Notification Channels
- Retention Policies
- System Operations
- Settings

The `Administration` group is visible only to `ADMIN`.

### Role and permission visibility

The backend currently defines `ADMIN` and `ENGINEER`.

| Navigation area | ADMIN | ENGINEER |
| --- | --- | --- |
| Monitoring | Visible | Visible |
| Analytics | Visible | Visible |
| Resources | Visible | Visible |
| Administration | Visible | Hidden |

An Engineer sees only data for applications authorized by the backend.
Application-specific `VIEW` and `MANAGE` permissions affect feature-page
actions and data, not the presence of the top-level `Applications` item.

Unauthorized navigation and actions are hidden rather than displayed as
locked. Frontend visibility is a usability rule only; backend authorization
remains mandatory.

### Implemented and unimplemented navigation

- Existing routes use normal React Router links.
- A matching route receives the active state.
- Product modules that do not yet have routes still appear as normal-looking
  navigation items.
- Activating an unimplemented item does nothing: it must not navigate, change
  the active item, show a placeholder page, or display a `Coming soon` label.
- Unimplemented items must use button semantics, not fake links with `href`.
- Their accessible label indicates that the destination is currently
  unavailable, even though no extra visual badge is shown.

### Navigation item states

- Default: muted foreground.
- Hover: subtle raised background and brighter foreground.
- Active: blue foreground, low-opacity blue background, and a 2px blue left
  indicator.
- Focus-visible: clear keyboard focus ring.
- Disabled/unimplemented activation: no route or content change.

`Alerts` and `Incidents` may display count badges supplied by their feature
data:

- Hide the badge when the count is `0`.
- Display values from `1` through `99`.
- Display `99+` for values of `100` or more.
- The shell defines the badge slot and formatting but does not fetch counts.

### Header

The header contains:

- the sidebar toggle;
- the current page title;
- an optional page-actions region;
- the current user's avatar, display name, and role;
- an account menu containing at least logout.

The header does not contain:

- breadcrumb navigation;
- global search;
- duplicated `Dashboard / Logs / Infrastructure` navigation;
- notification bell;
- light/dark theme switch;
- application version;
- global `System Online`, `Degraded`, or `Disconnected` status.

The page title comes from route metadata or the page's layout contract. The
page-actions region accepts feature-owned controls such as `Create
Application`, `Pause Live`, or `Export`; the shared layout does not define
their behavior.

The account area is placed at the far right and shows avatar, display name,
and `Admin` or `Engineer`. On narrow screens, the textual identity may be
visually reduced while remaining available in the account menu.

### Realtime and system status

- Pages using realtime data, such as Live Logs, own and display their
  connection state locally.
- Kafka, Redis, ClickHouse, PostgreSQL, and backend health belong in System
  Operations.
- The shared header must not infer or display aggregate health without a
  backend health contract.

## Component Boundaries

The implementation should keep responsibilities small:

| Component | Responsibility |
| --- | --- |
| `AppLayout` | Compose shell state, header, sidebar, and routed page outlet |
| `Sidebar` | Render branding, role-filtered groups, and navigation |
| `SidebarSection` | Render one always-open navigation group |
| `SidebarItem` | Render link/button state, icon, active marker, and badge |
| `Topbar` | Render toggle, page title/actions, and account area |
| `UserMenu` | Render identity and account actions |
| `PageHeaderContext` or equivalent | Allow a route to supply title and actions |
| `MobileSidebarDrawer` | Manage backdrop, focus, and mobile dismissal |

Do not add a general component framework as part of this work. Reuse simple
components only where the shared shell has actual duplication.

## Data Flow

1. `ProtectedRoute` confirms that an authenticated session exists.
2. The authenticated route tree renders `AppLayout`.
3. `AppLayout` reads the current user and role from the existing auth context.
4. Navigation configuration is filtered by role.
5. React Router location determines the active implemented item.
6. Route metadata or a page-header contract provides the current title and
   optional actions.
7. Feature-owned alert and incident counts may populate badge slots; absence
   of count data renders no badge.
8. Logout uses the existing auth context and returns the user to the login
   route through existing protection behavior.

No layout decision grants access to backend data.

## Responsive Behavior

### Desktop, `>= 768px`

- Sidebar starts open at `240px`.
- Sidebar pushes header and content.
- Manual toggle hides or restores the full sidebar.
- Main content keeps a stable minimum width and uses horizontal overflow where
  a feature table requires it.

### Mobile, `< 768px`

- Sidebar is closed on initial render and opens as a drawer.
- The drawer overlays content because pushing a narrow viewport would make the
  page unusable.
- A backdrop, Escape key, navigation, or explicit close control dismisses it.
- Header retains the toggle, page title, and compact account trigger.
- Page actions may wrap below the title when space is insufficient.

## Edge Cases

- Unknown or missing user role: render no privileged Administration items.
- Long display name: truncate in the header and expose the full value in the
  account menu or title attribute.
- Long page title: truncate on one line on narrow screens.
- More than `99` alerts or incidents: render `99+`.
- Badge data unavailable or loading: render no badge and avoid layout shift.
- Direct navigation to a protected but unauthorized route: backend and route
  guards reject access; hiding sidebar items is not sufficient.
- Unimplemented item activated by mouse or keyboard: remain on the current
  route without visual active-state changes.
- Mobile drawer open during viewport resize to desktop: close drawer and
  restore desktop sidebar state to open for that render.
- Reduced-motion preference: remove or shorten sidebar and menu transitions.

## Accessibility

- Use semantic `nav`, `header`, `main`, buttons, and links.
- Add `aria-current="page"` to the active route.
- The sidebar toggle exposes `aria-expanded` and an accessible label.
- Icons are decorative when a visible label exists.
- Mobile drawer focus is trapped and restored to the toggle when closed.
- All controls are reachable by keyboard.
- Text and active states meet WCAG AA contrast.
- Color is not the sole active-state or badge indicator.
- Touch targets are at least 40px high where practical.

## Testing Notes

### Component tests

- Render correct navigation groups for `ADMIN`.
- Hide Administration for `ENGINEER`.
- Mark existing routes active from the current location.
- Confirm unimplemented items do not navigate.
- Format badges as hidden, exact count, and `99+`.
- Toggle desktop sidebar and verify the content offset changes.
- Open and close the mobile drawer by toggle, Escape, backdrop, and navigation.
- Render page title and page-owned actions.
- Invoke logout through the account menu.

### Integration checks

- Authenticated routes render within `AppLayout`.
- Existing `/` and `/logs` routes continue to work.
- Reloading a desktop page starts with the sidebar open.
- Engineer and Admin sessions receive the expected shared navigation.
- Keyboard focus order remains logical in both desktop and mobile layouts.

### Visual checks

- Verify at approximately `1440px`, `1024px`, `768px`, and `390px`.
- Confirm log/table pages can consume full width.
- Confirm long names, titles, and `99+` badges do not break alignment.
- Confirm no content is covered by the desktop sidebar.

## Acceptance Criteria

- Every authenticated page can render inside one reusable `AppLayout`.
- The layout uses Tailwind CSS v4 theme tokens and a consistent dark palette.
- Desktop sidebar opens at `240px`, pushes content, and can be hidden manually.
- A reload restores the desktop sidebar to open rather than remembering the
  previous hidden state.
- Mobile uses an accessible drawer below `768px`.
- Navigation groups are always expanded and use the approved English labels.
- Admin sees all groups; Engineer never sees Administration.
- Existing routes navigate and show active state.
- Unimplemented modules remain visible but activation causes no route or UI
  change.
- Header shows only toggle, page title/actions, and account identity/menu.
- Header contains no global search, notification bell, theme switch,
  breadcrumb, version, or aggregate system status.
- Alerts and Incidents support count badges capped visually at `99+`.
- Application API key management remains a subfeature of Application detail.
- Realtime status is owned by relevant feature pages; infrastructure status is
  owned by System Operations.
- Existing authentication and route behavior remain functional.
