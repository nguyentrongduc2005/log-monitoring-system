# Operational Pages Design

## Problem

The authenticated frontend shell exists, but the implemented `Overview` and
`Live Logs` routes still render placeholders. The account menu also exposes a
profile entry without a profile route. The project needs these pages to look
and behave like production observability screens while staying honest about the
current backend state: identity endpoints exist, but log search, realtime log
streaming, alerting, and operational metrics are still target contracts unless
source code proves otherwise.

Stitch screens for `Tổng quan hệ thống - LogPulse` and `Live Logs - LogPulse`
are visual references only. They inform density, dark enterprise tone, card
hierarchy, severity coloring, and realtime feel; they do not override project
requirements, implemented APIs, shared shell behavior, or authorization rules.

## Goal

Design three authenticated pages:

- `Overview`: an operational dashboard focused on realtime ingestion demo
  readiness and application error visibility.
- `Live Logs`: a high-density hybrid table/stream for realtime log monitoring
  with fast filters and row details.
- `Profile`: a user account page based only on existing identity contracts.

The design should support early frontend implementation with mock data behind
adapters, then allow HTTP/WebSocket adapters to replace mocks without
rewriting page UI.

## Scope

### In scope

- Page content design for existing `Overview` and `Live Logs` routes.
- A new authenticated `Profile` route reached from the existing account menu.
- Page-level data contracts and adapter boundaries for mock and future real
  data.
- Loading, empty, error, authorization, and degraded states.
- Required authorization alignment for existing self-service identity
  endpoints used by Profile.
- Testing notes for React components and data adapters.

### Out of scope

- Backend implementation for logs, realtime, alerting, analytics, or new
  profile endpoints.
- New device/session APIs, session list UI, IP history, browser history, or
  logout-other-devices behavior.
- A new shared shell, sidebar redesign, or navigation information architecture
  change outside adding the profile destination.
- System Operations detail screens for Kafka, Redis, ClickHouse, PostgreSQL,
  workers, or DLQ management.
- AI analysis UI, retention UI, application management UI, and alert rule UI.

## Requirements

### General

- Use the existing `AppLayout`, `PageHeader`, sidebar, topbar, and auth
  context.
- Do not create a second page shell inside the feature pages.
- Use English labels for product UI, matching the existing shared shell.
- Use Tailwind v4 tokens from the shared layout where possible.
- Use high-density dark enterprise visuals: tonal surfaces, low-contrast
  borders, compact spacing, and clear semantic colors.
- Treat frontend role and permission checks as usability only; backend
  authorization remains mandatory.
- Do not document or display target backend behavior as implemented.

### Data Strategy

- Each page reads data through feature-local adapter functions or hooks.
- Initial implementation may use mock adapters for Overview and Live Logs.
- Future real adapters may call HTTP APIs and WebSocket/STOMP transport.
- Mock data must stay behind adapters and must not be scattered inside visual
  components.
- Identity/Profile uses the implemented identity contract where available:
  `GET /api/v1/users/me`, `PUT /api/v1/users/me`, and
  `PUT /api/v1/users/me/password`.
- Implementation must verify backend authorization allows any authenticated
  user to call self-service endpoints:
  - `GET /api/v1/users/me`
  - `PUT /api/v1/users/me`
  - `PUT /api/v1/users/me/password`
- Admin-only user management routes under `/api/v1/users/{id}` must remain
  admin-only. Self-service profile access must not depend on `ADMIN`.

## Proposed Design

## Overview Page

### Purpose

The Overview page answers two questions quickly:

1. Is the system able to receive and process the realtime ingestion demo
   smoothly?
2. Which applications are currently producing the most serious errors?

Detailed infrastructure inspection belongs in a future System Operations page.
Overview may summarize pipeline state, but it should not become a deep Kafka,
Redis, ClickHouse, or worker admin console.

### Layout

The page uses a responsive dashboard grid inside the existing content canvas.

Top page actions:

- Time range selector, defaulting to `Last 15 minutes`.
- Refresh action for adapter-backed snapshots.

Primary sections:

- KPI row with compact metric cards.
- Pipeline strip showing the log flow at a glance.
- Main log volume and error trend chart.
- Top noisy applications list.
- Recent critical alerts panel.
- Demo readiness panel for the required 500 logs in 2 seconds flow.

### KPI Cards

Cards should show:

- `Logs accepted/min`
- `Error rate`
- `Critical alerts`
- `Active applications`
- `Processing lag`

Each card includes a current value, short label, optional trend, and state
color. Values are adapter-provided. When the real backend is unavailable, the
mock adapter should label data source in development code only, not in product
copy.

### Pipeline Strip

The strip displays the required architecture flow:

```text
Ingestion API -> Kafka logs.raw -> Worker -> ClickHouse
                                      -> WebSocket
                                      -> Alerting / Telegram
```

State values:

- `healthy`
- `degraded`
- `delayed`
- `offline`
- `unknown`

The strip is summary-only. It should not expose provider internals, connection
strings, Kafka broker addresses, Redis keys, or secret-bearing payloads.

### Error Trend Chart

The main chart shows log volume over time, grouped by level:

- `INFO`
- `WARN`
- `ERROR`
- `CRITICAL`

The chart may be a simple accessible SVG or CSS-driven placeholder in the
first implementation. It should not require a heavy charting integration unless
existing dependencies make that practical.

### Top Noisy Applications

Rows include:

- application name
- environment
- total logs
- error count
- critical count
- error rate
- last seen time

For `ENGINEER`, this list must only contain applications authorized by the
backend once real data is connected. The frontend may filter mock data by
session role for demonstration, but it must not be treated as a security
boundary.

### Recent Critical Alerts

Show deduplicated alert summaries:

- severity
- application
- fingerprint or alert key
- compact message
- occurrences
- last seen
- delivery state if available

This panel should reflect the dedup concept without claiming Redis alert
deduplication is implemented when using mock data.

### Demo Readiness Panel

This panel supports the assignment demo:

```text
500 logs in 2 seconds -> accepted without ingestion error -> admin UI remains smooth
```

Suggested values:

- accepted count
- rejected count
- ingest duration
- p95 ingestion acknowledgement latency
- live stream status
- buffered or dropped events count

When the real demo runner does not exist yet, the adapter may return mock
readiness data. The UI copy should avoid saying an actual load test has passed
unless the adapter receives real verification evidence.

## Live Logs Page

### Purpose

Live Logs is the primary realtime monitoring screen. It should feel like a
tailing log stream while preserving table-like scanability and filtering.

The approved display model is hybrid:

- dense table structure for field clarity;
- severity-colored visual rhythm for realtime scanning;
- monospaced message and identifiers;
- row click opens a detail drawer.

### Page Actions

The page header action region may include:

- connection status indicator;
- pause/resume stream button;
- clear visible stream button.

### Connection States

The local realtime status can be:

- `live`
- `paused`
- `connecting`
- `reconnecting`
- `disconnected`
- `error`

Only Live Logs owns this state. The shared topbar must not infer global
platform health from it.

### Filter Bar

Filters:

- application selector
- level selector with `INFO`, `WARN`, `ERROR`, `CRITICAL`
- keyword search
- trace ID

Filter updates should not reload the page. Future WebSocket integration should
send updated subscription filters to the backend. Until then, mock adapters may
filter locally.

### Log Stream

Visible columns:

- timestamp
- application
- level
- message
- trace ID
- source or host when available

Rows use a severity bar:

- `INFO`: muted blue or neutral
- `WARN`: amber
- `ERROR`: red
- `CRITICAL`: strong red

The message and technical identifiers use monospace text. Long messages are
truncated in the stream but visible in the detail drawer.

### Detail Drawer

Selecting a row opens a right-side detail drawer. The drawer shows:

- level
- application
- timestamp
- full message
- trace ID
- event ID
- ingestion ID
- source
- host
- environment
- attributes or metadata as key-value rows

The drawer must not expose secrets or complete sensitive payloads. Future
backend redaction is mandatory before production use; frontend fixtures must
also avoid real credentials or personal data.

### Backpressure And Smoothness

The UI should remain responsive during high log volume.

Implementation should use one of:

- virtualization/windowed rendering;
- capped in-memory visible buffer;
- batched state updates.

The page should display buffered or dropped counts when the client cannot show
every event. This is a frontend delivery indicator, not Kafka loss.

### Empty And Error States

- No logs: show an empty state with filter reset action.
- No authorized applications: explain that no applications are available for
  the current account.
- Disconnected: keep existing logs visible and show reconnect status.
- Error: show a non-secret message and retry action.

## Profile Page

### Purpose

Profile lets the authenticated user view and update identity information that
is already represented by the current identity API. It must not invent device,
session, IP, browser, or audit-history features.

### Route And Entry Point

- Add an authenticated route such as `/profile`.
- The existing account menu item `Hồ sơ cá nhân` should navigate to this route.
- The route renders inside the existing `AppLayout`.

The label may be changed to English, such as `Profile`, to match the shared
layout language.

### Data Source

Profile uses `UserResponse`:

- `id`
- `email`
- `displayName`
- `role`
- `status`
- `lastLoginAt`
- `createdAt`
- `updatedAt`

Supported actions:

- update `email` and `displayName` through `PUT /api/v1/users/me`;
- change password through `PUT /api/v1/users/me/password`.

Implementation must confirm this password endpoint is authorized for every
authenticated account before enabling the form. If the current backend security
configuration treats `/api/v1/users/me/password` as admin-only, the
implementation plan must include the minimal backend authorization alignment
for this existing self-service endpoint.

### Layout

Primary sections:

- identity summary card;
- account metadata card;
- edit profile form;
- change password form;
- role and status explanation panel.

### Identity Summary

Show:

- initials avatar;
- display name;
- email;
- role badge;
- status badge.

### Account Metadata

Show read-only metadata:

- user ID
- last login time
- created time
- updated time

Missing or null dates should render as `Not available` rather than an invalid
date.

### Edit Profile Form

Fields:

- display name
- email

Behavior:

- prefill from loaded profile;
- validate required fields client-side;
- show save, saving, success, and error states;
- update auth session user data after a successful save so the topbar reflects
  the latest display name or email.

### Change Password Form

Fields:

- current password
- new password
- confirm new password

Behavior:

- validate confirmation client-side;
- call the existing password endpoint;
- clear password fields after success;
- show non-secret error messages.

### Role And Status Explanation

Role copy:

- `ADMIN`: can manage platform configuration and view all applications when
  backend authorization allows it.
- `ENGINEER`: can view authorized applications and operational data.

Status copy:

- `ACTIVE`: account can sign in and use authorized features.
- `DISABLED`: account is disabled.
- `LOCKED`: account is locked.

This panel explains current fields only. It must not mention active device
sessions, browser history, IP history, or logout-other-devices controls.

## Data Flow

### Overview

```text
OverviewPage
  -> useOverviewSnapshot(adapter)
  -> OverviewDataAdapter
      -> mock data now
      -> future HTTP metrics/search APIs
```

### Live Logs

```text
LiveLogsPage
  -> useLiveLogs(adapter, filters)
  -> LiveLogsAdapter
      -> mock event stream now
      -> future WebSocket/STOMP subscription and filter update messages
```

### Profile

```text
ProfilePage
  -> profile API adapter
      -> GET /api/v1/users/me
      -> PUT /api/v1/users/me
      -> PUT /api/v1/users/me/password
  -> AuthProvider/session refresh or local session update
```

## Component Boundaries

Suggested frontend structure:

```text
src/features/dashboard/
├── DashboardPage.tsx
├── overview-adapter.ts
├── overview-types.ts
└── components/

src/features/live-logs/
├── LiveLogsPage.tsx
├── live-logs-adapter.ts
├── live-logs-types.ts
└── components/

src/features/profile/
├── ProfilePage.tsx
├── profile-api.ts
└── components/
```

Types that describe feature UI state should stay colocated with the feature.
API DTOs should continue using generated OpenAPI types instead of duplicating
DTOs in a generic `types` directory.

## Edge Cases

- The user has no authorized applications.
- A filter combination returns zero logs.
- Realtime reconnects while filters have changed.
- The stream is paused while new events arrive.
- A log row has a missing trace ID, source, host, or attributes.
- `lastLoginAt`, `createdAt`, or `updatedAt` is absent.
- Profile update succeeds but session cache still has stale user data.
- Password change fails due to invalid current password.
- Password endpoint is misclassified as admin-only by backend security.
- Backend returns `401` and auth interceptors log the user out.
- Backend returns `403`; the page shows a permission message without exposing
  internal authorization rules.

## Testing Notes

Recommended tests:

- Overview renders KPI, pipeline, noisy apps, alerts, and demo readiness from
  adapter data.
- Overview handles loading, empty, and adapter error states.
- Live Logs applies application, level, keyword, and trace ID filters.
- Live Logs pause/resume changes stream state without clearing existing rows.
- Live Logs detail drawer renders full row details and handles missing fields.
- Live Logs renders no-authorized-applications and disconnected states.
- Profile renders all `UserResponse` metadata.
- Profile validates email/display name before save.
- Profile updates visible identity after a successful save.
- Profile validates password confirmation and clears password fields after
  success.
- Account menu navigates to `/profile`.

Manual verification:

- Run frontend tests.
- Run frontend lint.
- Confirm the pages render inside the existing shell at desktop and mobile
  widths.
- Confirm mock data contains no real credentials, raw API keys, or sensitive
  payloads.

## Acceptance Criteria

- Overview focuses on realtime ingestion demo readiness and application error
  visibility.
- Overview does not become a detailed System Operations page.
- Live Logs uses the approved hybrid dense table/stream design.
- Live Logs supports fast filters, pause/resume, connection status, and row
  detail drawer.
- Live Logs is designed for high-volume rendering with a bounded visible
  buffer, virtualization, or batched updates.
- Profile is reachable from the account menu and renders inside `AppLayout`.
- Profile uses only existing `UserResponse` fields for identity, role, status,
  and account timestamps.
- Profile supports editing display name/email and changing password using the
  existing identity contracts.
- No page claims logs/realtime/alerting backend behavior is implemented until
  the source code and generated OpenAPI prove it.
- Mock data is isolated behind adapters and can be replaced by real HTTP or
  WebSocket adapters.
- The design introduces no device/session list, browser history, IP history,
  logout-other-devices control, or unimplemented security audit feature.
