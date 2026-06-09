---
version: "1.0"
name: LogPulse
description: >-
  Enterprise log collection, monitoring, alerting, analytics, retention, and
  AI insight platform

design:
  theme: dark-first
  density: compact
  viewport: 1440px
  grid: 8px

colors:
  background: "#0B1220"
  sidebar: "#091525"
  surface: "#111827"
  surfaceSoft: "#162033"
  surfaceElevated: "#1A2638"
  border: "#273449"
  borderStrong: "#3A4A62"
  primary: "#2563EB"
  primaryHover: "#1D4ED8"
  primarySoft: "#102A4C"
  accent: "#16D9E3"
  text: "#F9FAFB"
  textSecondary: "#CBD5E1"
  textMuted: "#9CA3AF"
  textDisabled: "#64748B"
  info: "#38BDF8"
  warning: "#F59E0B"
  error: "#F43F5E"
  critical: "#DC2626"
  success: "#22C55E"
  neutral: "#64748B"

typography:
  uiFont: Inter
  codeFont: JetBrains Mono
  display:
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1.2
  h1:
    fontSize: 28px
    fontWeight: 700
    lineHeight: 1.25
  h2:
    fontSize: 22px
    fontWeight: 600
    lineHeight: 1.3
  h3:
    fontSize: 16px
    fontWeight: 600
    lineHeight: 1.4
  body:
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.5
  small:
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.4
  label:
    fontSize: 12px
    fontWeight: 600
    lineHeight: 1.3
  code:
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.5

radius:
  sm: 6px
  md: 8px
  lg: 12px
  full: 9999px

spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px

layout:
  sidebarWidth: 240px
  sidebarCollapsedWidth: 72px
  topbarHeight: 64px
  pageMaxWidth: 1920px
  pagePaddingDesktop: 24px
  pagePaddingTablet: 16px
  pagePaddingMobile: 12px
  detailDrawerWidth: 480px
  tableRowCompact: 36px
  tableRowComfortable: 44px
---

# LogPulse Design System

## 1. Product Vision

LogPulse is a complete observability platform for collecting, monitoring,
searching, alerting, analyzing, and retaining application logs.

The product serves:

- DevOps engineers
- Backend engineers
- Site reliability engineers
- System administrators

The UI must feel serious, reliable, technical, and production-ready. It should
look like real enterprise monitoring software rather than a landing page,
futuristic AI demo, game dashboard, or cryptocurrency product.

Useful visual references:

- Grafana Cloud
- Datadog
- New Relic
- Elastic Cloud
- Sentry
- GitHub Enterprise

Do not copy a reference product directly. Use them only to understand the
expected information density, hierarchy, and operational clarity.

## 2. Product Scope

Design all listed capabilities as first-class parts of the complete product:

- Authentication and user profile
- System overview dashboard
- Realtime live log viewer
- Historical log search
- Alert monitoring and deduplication
- Application and API key management
- User roles and application access
- Alert rule configuration
- Telegram notification channels
- Application health analytics
- Retention policy management
- AI log and incident insights
- Platform settings and infrastructure status

Do not label Health Analytics, Retention, or AI Insights as `Beta`, `Bonus`,
`Future`, or `Coming soon`.

The primary operational workflow remains:

```text
Dashboard
  -> Live Logs
  -> Historical Search
  -> Alerts
  -> Applications
  -> Health Analytics
  -> AI Insights
  -> Administration
```

The design must not imply that AI replaces monitoring, alerting, or engineering
judgment. AI analysis is advisory and is not part of the realtime ingestion hot
path.

## 3. Product Context

LogPulse receives logs from external applications through a high-throughput
API. Accepted raw logs are buffered in Kafka, normalized by workers, stored in
ClickHouse, and streamed to the UI through WebSocket.

`ERROR` and `CRITICAL` logs produce alert events. Redis provides an atomic
deduplication lock, PostgreSQL stores alert occurrences and delivery history,
and Telegram is the initial notification channel.

The UI should expose operational signals without requiring users to understand
every internal implementation detail.

Core log fields:

- Application
- Log level: `INFO`, `WARN`, `ERROR`, `CRITICAL`
- Message
- Event timestamp
- Trace ID
- Event ID
- Ingestion ID
- Environment
- Host name
- Source
- Fingerprint
- Attributes

Pipeline states:

- `RAW_RECEIVED`
- `NORMALIZED`
- `STORED`
- `FAILED`

Do not present pipeline state as a searchable property for every historical log
unless the API supports it. Historical search results are stored logs. Failed
processing events belong in an operations or DLQ view.

## 4. User Roles and Permissions

### Admin

An Admin can:

- View all applications and logs
- Manage users and roles
- Grant `VIEW` or `MANAGE` access to applications
- Create and manage applications
- Create and revoke API keys
- Configure alert rules and thresholds
- Configure Telegram notification channels
- Configure retention policies
- View infrastructure and DLQ operational status

### Engineer

An Engineer can:

- View only applications they are authorized to access
- View realtime and historical logs for authorized applications
- View alerts and health analytics for authorized applications
- Manage an application only when granted `MANAGE` access

Frontend visibility does not replace backend authorization. Use disabled or
hidden actions only as a usability aid; never imply that client-side hiding is
the security boundary.

## 5. Visual Direction

The product should communicate:

- Reliability
- System visibility
- Incident awareness
- Realtime activity
- Engineering confidence
- Controlled information density

Use a dark enterprise dashboard palette by default. The background should be
deep navy rather than pure black. Blue is reserved for primary actions and
neutral realtime activity. Cyan connects the UI to the LogPulse logo and is
used sparingly for live signals.

Avoid:

- Neon gradients
- Excessive glow
- Cyberpunk effects
- Heavy glassmorphism
- Decorative sci-fi graphics
- Random floating cards
- Oversized marketing headlines
- Excessive rounded shapes
- Decorative stock photography
- Charts styled as wireframes

Use borders and tonal surface layers instead of heavy shadows. Small shadows
may be used for dropdowns, modals, and drawers where elevation must be clear.

## 6. Color Rules

### Semantic colors

| Meaning | Color | Usage |
| --- | --- | --- |
| Primary | `#2563EB` | Main actions, selected navigation, focus |
| Accent | `#16D9E3` | Live connection, pulse, realtime activity |
| Success | `#22C55E` | Healthy, connected, delivered |
| Info | `#38BDF8` | INFO logs and informational states |
| Warning | `#F59E0B` | WARN logs, degraded state |
| Error | `#F43F5E` | ERROR logs and recoverable failures |
| Critical | `#DC2626` | CRITICAL logs and severe outages |
| Neutral | `#64748B` | Disabled, inactive, unknown |

Never communicate state using color alone. Pair every semantic color with a
label, icon, shape, or status text.

For log rows, use a subtle tinted background only for `ERROR` and `CRITICAL`.
Do not fill an entire page or large card with bright red.

### Light theme

Dark mode is the default, but all components must have a light-theme
equivalent. Recommended light tokens:

```yaml
background: "#F6F8FB"
sidebar: "#FFFFFF"
surface: "#FFFFFF"
surfaceSoft: "#F1F5F9"
border: "#D8E0EA"
text: "#0F172A"
textSecondary: "#334155"
textMuted: "#64748B"
```

Semantic status colors should remain recognizable in both themes.

## 7. Typography

Use Inter for navigation, headings, labels, buttons, badges, application names,
and ordinary table content.

Use JetBrains Mono for:

- Log messages
- Trace IDs
- Event and ingestion IDs
- Timestamps with milliseconds
- JSON payloads
- Error codes
- API key prefixes
- Technical metadata values

A log row should not use monospace for every cell. Application names, severity
badges, status text, and actions remain in Inter to preserve hierarchy.

Headings should be strong but not oversized. Page titles should normally be
`28px` or smaller. Labels may use uppercase only for short technical labels;
do not uppercase long Vietnamese phrases.

## 8. Layout and Navigation

### Desktop

Design desktop-first at `1440px`, while supporting widths up to `1920px`.

Use:

- Fixed left sidebar: `240px`
- Collapsed sidebar: `72px`
- Topbar: `64px`
- Main page padding: `24px`
- Base spacing grid: `8px`
- Maximum common card radius: `12px`

The sidebar contains:

- LogPulse logo and product name
- Tổng quan
- Live Logs
- Tìm kiếm Log
- Cảnh báo
- Ứng dụng
- Phân tích sức khỏe
- AI Insights
- A separated `Quản trị` group
- Người dùng
- Quy tắc cảnh báo
- Kênh thông báo
- Retention Policy
- Vận hành hệ thống
- Cài đặt

Admin-only navigation belongs in the `Quản trị` group. Engineer navigation
must show only authorized areas.

The sidebar footer shows:

- Overall platform status
- Current version
- Collapse control

The topbar contains:

- Breadcrumb
- Global search or command search
- Live update pause/resume control where relevant
- Notification bell with unread count
- Theme switcher
- User avatar, display name, and role

### Tablet and mobile

- Collapse the sidebar automatically below `1200px`.
- Replace it with a drawer below `768px`.
- Stack KPI cards into two columns on tablet and one column on mobile.
- Permit horizontal table scrolling where a card layout would hide technical
  data.
- Keep filters available through a sticky filter button or filter drawer.
- Preserve log level, timestamp, application, and message as the minimum mobile
  log information.

## 9. Interaction and Motion

Animations should explain state changes, not decorate the interface.

Recommended timings:

- Hover and focus transition: `120-160ms`
- Drawer and modal transition: `180-240ms`
- New live log fade: `150ms`
- Healthy live pulse: one subtle cycle every `2s`
- Toast display: `4-6s`, persistent for destructive failures

Respect `prefers-reduced-motion`.

WebSocket reconnect should be represented with:

- Immediate reconnect attempt
- Exponential retry delay: `1s`, `2s`, `4s`, `8s`, maximum `30s`
- Visible `Đang kết nối lại` state
- Manual retry action after repeated failure

Do not animate every incoming log with movement. At high throughput, use a
subtle fade or no row animation to protect readability and performance.

## 10. Shared Components

Create a consistent component system for:

- Primary, secondary, ghost, and danger buttons
- Icon button
- Text input and password input
- Search input
- Select and multi-select
- Date and time range picker
- Checkbox, radio, and switch
- Form validation and helper text
- Tooltip
- Dropdown menu
- Command palette
- Breadcrumb
- Tabs
- Status badge
- Log-level badge
- Connection status
- KPI card with optional sparkline
- Chart card
- Data table
- Pagination
- Side drawer
- Confirmation modal
- Toast notification
- Empty state
- Loading skeleton
- Error state

### Buttons

- Default control height: `40px`
- Compact table action height: `32px`
- Primary: blue background and white text
- Secondary: dark surface with visible border
- Ghost: transparent, used for low-priority actions
- Danger: red only for destructive actions
- Disabled controls must remain readable and clearly inactive

### Inputs

- Default height: `40px`
- Dark surface with subtle border
- Blue focus ring with at least `2px` visible outline
- Validation message appears below the field
- Search inputs include a clear action
- Technical numeric inputs show units such as seconds, days, or events

### Cards

- Default padding: `16px` for dense dashboard cards
- Use `24px` for forms and prominent summary cards
- Separate cards with border and surface tone
- Every chart card supports loading, empty, error, and tooltip states

### Tables

- Sticky header
- Compact row height: `36px`
- Comfortable row height: `44px`
- Visible hover and keyboard focus
- Column sorting only when supported
- Resizable columns for log tables
- Horizontal scrolling without hiding the first key column
- Optional column visibility control
- Pagination for historical data
- Virtualized rendering for live logs

### Drawers and modals

- Log detail drawer: recommended width `480px`
- Large JSON or metadata drawer: up to `640px`
- Modal width: `440-560px`
- Require confirmation for destructive actions
- Do not use a modal where a non-blocking drawer supports investigation better

## 11. Status and Badge System

### Log levels

- `INFO`: blue badge
- `WARN`: amber badge
- `ERROR`: rose-red badge
- `CRITICAL`: dark red badge with stronger emphasis

### Application and account status

- `ACTIVE`: green
- `INACTIVE`: neutral gray
- `DISABLED`: neutral gray
- `LOCKED`: amber or red depending on context
- `REVOKED`: red
- `EXPIRED`: amber

### Infrastructure status

- `HEALTHY`: green
- `DEGRADED`: amber
- `DOWN`: red
- `UNKNOWN`: gray
- `CONNECTING`: blue

### Pipeline status

- `RAW_RECEIVED`: neutral blue
- `NORMALIZED`: cyan
- `STORED`: green
- `FAILED`: red

## 12. Screen Specifications

### 12.1. Sign In

Create a focused login page containing:

- LogPulse logo
- `Đăng nhập vào LogPulse`
- Email
- Password with visibility toggle
- `Ghi nhớ đăng nhập`
- `Quên mật khẩu?`
- Primary `Đăng nhập` action
- Loading state
- Invalid credential state
- Locked or disabled account state

Use a restrained realtime log or pulse visualization as supporting artwork.
Do not use social login or a large marketing carousel.

### 12.2. System Overview Dashboard

Header controls:

- Page title and short context
- Application selector
- Time ranges: `15 phút`, `1 giờ`, `6 giờ`, `24 giờ`, `7 ngày`
- Refresh action
- `Đang cập nhật trực tiếp` connection state

Default dashboard auto-refresh: `15 seconds`. Allow users to pause it.

KPI cards:

- Tổng log tiếp nhận
- Log mỗi giây
- Tỷ lệ lỗi
- Cảnh báo đang hoạt động
- Ứng dụng đang hoạt động
- Kafka consumer lag

Each KPI includes:

- Current value
- Comparison with previous period
- Percentage trend
- Small sparkline where useful
- Tooltip explaining calculation

Charts and panels:

- Log throughput over time
- Stacked log levels over time
- Error rate by application
- Log-level distribution
- Applications requiring attention
- Latest alerts
- Infrastructure health

Infrastructure health includes Kafka, ClickHouse, Redis, PostgreSQL, WebSocket,
backend API, and processing worker. Show useful metrics such as latency,
consumer lag, last successful check, or error rate when available.

Recommended operational thresholds for visual defaults:

- Healthy Kafka consumer lag: below `1,000`
- Warning lag: `1,000-10,000`
- Critical lag: above `10,000`
- Healthy service latency: below `300ms`
- Warning latency: `300-1,000ms`
- Critical latency: above `1,000ms`

These are configurable display defaults, not hard business guarantees.

### 12.3. Live Logs

The Live Logs page is the primary realtime workspace.

Header:

- `Live Logs`
- WebSocket connection badge
- Current events per second
- Pause/resume
- Clear local view
- Auto-scroll toggle
- Compact/comfortable density control
- Buffered event count while paused

Filters:

- Application multi-select
- Log level multi-select
- Environment
- Message search
- Trace ID
- `Chỉ hiển thị lỗi`
- Reset filters

Filtering must update without reloading the page.

The virtualized table contains:

- Timestamp
- Level
- Application
- Environment
- Message
- Trace ID
- Host
- Source
- Row actions

Recommended behavior:

- Keep up to `2,000` visible rows in the local live window
- When paused, buffer up to `5,000` events before warning about dropped local
  display events
- Do not imply that locally dropped display events were lost from Kafka or
  ClickHouse
- Keep the table header and filter bar sticky
- Truncate long messages to one line with an expand action

Clicking a row opens a detail drawer with:

- Full message
- Formatted JSON
- Event ID
- Ingestion ID
- Application
- Level
- Event timestamp with milliseconds and UTC indicator
- Received, normalized, and stored timestamps when available
- Trace ID
- Environment
- Host name
- Source
- Fingerprint
- Attributes
- Copy action for technical values
- `Xem toàn bộ trace`
- `Tìm log tương tự`

States:

- Waiting for first log
- No matching logs
- Paused
- Connected
- Reconnecting
- Disconnected
- High throughput
- Local buffer full

### 12.4. Historical Log Search

Use HTTP search for stored logs rather than WebSocket.

Query controls:

- Required time range
- Application
- Log level
- Environment
- Message
- Trace ID
- Fingerprint
- Search action
- Reset action
- Saved and recent queries

Default time range: last `1 hour`.

Results:

- Total matching result estimate when available
- Query execution time
- Paginated table
- Sort by timestamp
- Table and histogram views
- Export visible result set as JSON or CSV
- Log detail drawer

Recommended default page size: `50`.
Available sizes: `25`, `50`, `100`.

Do not provide raw SQL or ClickHouse expression input.

### 12.5. Alerts

Header:

- Application filter
- Level filter
- Delivery status filter
- Time range
- Realtime update toggle

KPI cards:

- New alerts
- Critical alerts
- Deduplicated occurrences
- Telegram deliveries sent
- Failed deliveries

Alert table:

- Level
- Title
- Application
- Sample message
- Occurrence count
- First seen
- Last seen
- Deduplication state
- Telegram delivery state
- Triggered rule

Make deduplication understandable:

```text
Đã gửi 1 thông báo
Đã gộp thêm 99 lần xuất hiện
Khóa trùng còn 34 giây
```

Alert detail drawer or page:

- Occurrence timeline
- Sample message
- Fingerprint
- Representative event ID
- Triggered rule
- Delivery history
- Related logs
- AI insight when available

Do not add assignment, acknowledgement, resolution, or incident ownership
workflow unless the product requirements are expanded to include those
capabilities.

### 12.6. Applications

Application list columns:

- Display name
- Technical name
- Environment summary
- Status
- Throughput
- Error rate
- Last event
- Engineer count
- Active API key count
- Actions

Application detail tabs:

- Tổng quan
- Logs
- API Keys
- Thành viên
- Alert Rules
- Health

Create and edit form:

- Display name
- Unique technical name
- Description
- Status

API key table:

- Name
- Prefix
- Status
- Expiration
- Last used
- Created by
- Actions

Show a newly generated raw API key exactly once. Use a blocking confirmation:

```text
API key sẽ không thể xem lại sau khi đóng cửa sổ này.
```

Provide copy action and require the user to confirm that the key has been
stored before closing.

### 12.7. Application Health Analytics

Header:

- Application selector
- Time range
- Compare with previous period

KPI cards:

- Health score from `0-100`
- Error rate
- Warning rate
- Log throughput
- Critical events
- Continuous healthy duration

Charts:

- Health score over time
- Hourly error rate
- Application error-rate comparison
- Hour and weekday heatmap
- Top error fingerprints
- Least stable applications

Recommended visual health bands:

- `90-100`: Healthy
- `70-89`: Needs attention
- `0-69`: Unhealthy

The score must show its formula version or an explanatory tooltip. Do not use a
single error rate to claim a root cause.

### 12.8. AI Insights

AI Insights is a first-class page, but its recommendations remain advisory.

Display:

- Natural-language system summary
- Grouped errors by fingerprint
- Category such as Database, Network, Authentication, Timeout, Validation, or
  Unknown
- Possible root cause
- Suggested next checks
- Confidence score
- Analysis timestamp
- Provider/model metadata in a technical details section
- Source fingerprints and related logs
- Helpful/not helpful feedback

Use wording such as `Nguyên nhân có thể` and `Gợi ý kiểm tra`. Never present an
AI hypothesis as a verified fact.

AI analysis should be shown per grouped error or alert, not as a separate
analysis for every individual log.

### 12.9. User Management

Admin page with:

- Search
- Role filter
- Status filter
- User table

Columns:

- Display name
- Email
- Role: `ADMIN` or `ENGINEER`
- Status: `ACTIVE`, `DISABLED`, or `LOCKED`
- Authorized application count
- Last login
- Created date
- Actions

Actions:

- Change role
- Change status
- Grant application access
- Select `VIEW` or `MANAGE`

Use confirmation modals for role changes, account locking, and access removal.

### 12.10. Alert Rules

Rule list columns:

- Rule name
- Application or global scope
- Levels
- Threshold count
- Window
- Deduplication duration
- Notification channel
- Enabled state
- Last triggered
- Actions

Create and edit form:

- Name
- Application or global scope
- Levels
- Threshold count
- Window seconds
- Deduplication seconds
- Notification channel
- Enabled state

Default recommended values:

- Threshold: `1`
- Window: `60 seconds`
- Deduplication duration: `60 seconds`
- Levels: `ERROR` and `CRITICAL`

Show a readable preview:

```text
Khi có ít nhất 1 log ERROR hoặc CRITICAL trong 60 giây,
gửi một cảnh báo và khóa thông báo trùng trong 60 giây.
```

All numeric values must be greater than zero.

### 12.11. Notification Channels

The initial channel type is Telegram.

Channel list:

- Name
- Type
- Application or global scope
- Partially masked Chat ID
- Status
- Last successful delivery
- Delivery success rate
- Actions

Configuration form:

- Name
- Application or global scope
- Chat ID
- Bot token secret reference
- Message template
- Enabled state
- Test delivery action

Never display or store the Telegram bot token as plain text in the design.
Display delivery success, retrying, and failed states.

### 12.12. Retention Policy

Retention is a complete product area.

Policy fields:

- Scope: global or application override
- Log level
- Retention duration
- Action: compress or delete
- Enabled state
- Next scheduled run

Recommended defaults:

- `INFO`: compress or delete after `7 days`
- `WARN`: retain for `30 days`
- `ERROR`: retain for `90 days`
- `CRITICAL`: retain for `180 days`
- Cleanup schedule: daily at `02:00 UTC`

These values must be configurable.

Retention overview:

- Active policies
- Next run
- Last run status
- Rows or partitions processed
- Storage released
- Duration
- Failure details

Provide an estimate action before manual cleanup. Manual cleanup and destructive
policy changes require confirmation.

Do not imply that the application deletes ClickHouse rows one by one. Present
cleanup as partition or TTL-based storage operations.

### 12.13. System Operations

Admin operational view:

- Kafka topics and consumer lag
- Processing throughput
- ClickHouse insert and query latency
- Redis dedup status
- PostgreSQL health
- WebSocket clients
- Telegram delivery health
- DLQ event count

Include a DLQ table with:

- Event ID
- Application
- Failure stage
- Error summary
- Retry count
- First and last failure
- Safe replay action when supported

Clearly separate infrastructure failure from application log severity.

### 12.14. Profile and Settings

Tabs:

- Hồ sơ
- Bảo mật
- Giao diện
- Tùy chọn thông báo

Profile:

- Display name
- Email
- Read-only role

Security:

- Current password
- New password
- Confirm password
- Password strength

Appearance:

- Dark, light, or system theme
- Compact or comfortable density
- Timezone, default `UTC`
- Timestamp format

Recommended timestamp display:

- Tables: `YYYY-MM-DD HH:mm:ss.SSS`
- Detail views: ISO 8601 with timezone
- Allow local timezone display while preserving a visible UTC option

## 13. Global States

Design every major page with:

- Loading skeleton
- Empty state
- No search results
- API failure
- Permission denied
- Session expired
- Rate limit exceeded
- Partial data
- Stale data
- Maintenance state
- 404 page

Operational state examples:

- WebSocket disconnected
- Kafka backlog high
- ClickHouse unavailable
- Redis dedup unavailable
- Telegram delivery failed
- AI analysis failed
- Retention run partially completed

Every error state should explain:

- What happened
- What data may be affected
- Whether retry is automatic
- What action the user can take

## 14. Accessibility

- Meet WCAG AA contrast for text and interactive controls.
- Show a visible keyboard focus state.
- Support keyboard navigation for tables, menus, tabs, dialogs, and drawers.
- Provide text or icon labels in addition to color.
- Use accessible chart legends and non-color distinctions.
- Keep touch targets at least `40x40px`.
- Announce realtime connection changes without announcing every incoming log.
- Trap focus inside modal dialogs and restore focus on close.
- Support reduced motion.

## 15. Charts and Data Visualization

Use ECharts-compatible visual patterns.

- Show units on axes and tooltips.
- Use UTC or the selected timezone consistently.
- Keep legends concise and interactive.
- Provide loading, empty, and error states.
- Avoid more than six strong colors in a single chart.
- Use semantic level colors consistently.
- Prefer line and area charts for throughput.
- Prefer stacked areas or bars for level distribution over time.
- Prefer horizontal bars for application comparisons.
- Use heatmaps only when hour/day patterns are meaningful.
- Avoid 3D charts.

Recommended aggregation:

- Last 15 minutes: 10-second buckets
- Last 1 hour: 1-minute buckets
- Last 6 hours: 5-minute buckets
- Last 24 hours: 15-minute buckets
- Last 7 days: 1-hour buckets

## 16. Realistic Sample Data

Applications:

- `payment-service`
- `authentication-service`
- `order-service`
- `notification-worker`
- `api-gateway`

Environments:

- `production`
- `staging`
- `development`

Log messages:

- INFO: `Payment request processed successfully`
- WARN: `Kafka consumer lag exceeded 500 messages`
- ERROR: `Connection timeout while accessing PostgreSQL`
- CRITICAL: `Payment service circuit breaker is open`

Trace IDs:

- `trc_01JX8K4Q9M6NZ2V7`
- `trc_01JX8M01AT4CR9P3`

Alert example:

```text
Title: PostgreSQL connection timeout
Application: payment-service
Level: ERROR
Occurrences: 100 in 1 minute
Telegram: 1 notification sent
Deduplication: 99 following occurrences grouped
```

Use realistic values and Vietnamese UI text. Do not use Lorem Ipsum.

## 17. Implementation Constraints

The design should be practical to implement with:

- React
- TypeScript
- React Router
- TanStack Query
- STOMP WebSocket
- ECharts
- React Window
- Responsive CSS

Prioritize:

- Reusable feature-based components
- Virtualized live log rendering
- Stable layouts during loading
- Clear API and WebSocket states
- Responsive behavior
- Accessible interactions

Do not design interactions that require unsupported raw SQL, direct database
access, plaintext secret display, or client-side-only authorization.

## 18. Final Design Deliverables

Produce a consistent high-fidelity design for every screen in this document.

For each screen, include:

- Desktop layout
- Responsive behavior
- Loading state
- Empty state
- Error state
- One realistic populated state
- Important modal or drawer
- Relevant role differences

Begin the design set with the most operationally important screens:

1. Sign In
2. System Overview Dashboard
3. Live Logs
4. Log Detail Drawer
5. Historical Log Search
6. Alerts and Alert Detail
7. Applications and API Keys

Then complete Health Analytics, AI Insights, Retention, System Operations, and
all administration screens. This ordering controls design workflow only; all
listed capabilities belong to the completed product.
