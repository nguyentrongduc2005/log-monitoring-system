# Live Log WebSocket Integration

This document describes how the frontend should integrate with the backend
Live Log WebSocket stream.

## Scope

Live Log supports these filters:

- Application
- Level
- Keyword

Only Application affects WebSocket subscriptions and backend authorization.
Level and Keyword are not backend subscription filters in the current
implementation; they should be applied locally in the frontend against the
received live log buffer.

The backend publishes each live log message once to the application topic. With
the current Spring simple broker setup, adding `level` or `keyword` headers to a
`SUBSCRIBE` frame would not filter messages per subscriber. Backend-side
Level/Keyword filtering would require a different delivery model, such as
per-session queues or a custom subscription registry.

## Authentication

The WebSocket endpoint is:

```text
/ws
```

The HTTP handshake is allowed without an HTTP `Authorization` header so browser
clients can open the socket. Authentication is required in the STOMP `CONNECT`
frame.

The frontend must send the access token in the STOMP native header:

```text
Authorization: Bearer <accessToken>
```

Example with `@stomp/stompjs`:

```ts
const client = new Client({
  brokerURL: import.meta.env.VITE_WS_URL,
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

If the token is missing, invalid, expired, or revoked, the backend rejects the
STOMP connection.

## Live Log Topic

Live logs are published per application:

```text
/topic/applications/{applicationId}/logs
```

Example:

```text
/topic/applications/00000000-0000-0000-0000-000000000101/logs
```

The backend authorizes every subscription to this topic pattern. A user can
subscribe only when they can view that application. Admin users can view all
applications through the existing application access policy.

## Application Filter

The frontend should load visible applications through the existing authenticated
HTTP endpoint:

```text
GET /api/v1/applications/me
```

Use the returned application ids as the only selectable Application filter
options.

Subscription behavior:

- `All applications`: subscribe to every visible application topic.
- One selected application: unsubscribe from old live log topics and subscribe
  only to `/topic/applications/{applicationId}/logs`.
- No visible applications: do not subscribe to any live log topic.

Do not trust frontend filtering for authorization. The backend rejects direct
subscriptions to unauthorized application ids.

## Level Filter

Level is a local frontend filter over the live log buffer.

Supported values:

```text
ALL, INFO, WARN, ERROR, CRITICAL
```

Behavior:

- `ALL`: show every received log.
- Other values: show only logs where `message.level` equals the selected value.

Changing Level should not reconnect the WebSocket and should not resubscribe.
Do not send Level as a STOMP subscription header for the current backend
contract.

## Keyword Filter

Keyword is a local frontend filter over the live log buffer.

Recommended behavior:

- Trim whitespace.
- Match case-insensitively.
- Search in `message.message`.
- Empty keyword means no keyword filtering.

Changing Keyword should not reconnect the WebSocket and should not resubscribe.
Do not send Keyword as a STOMP subscription header for the current backend
contract.

## Message Shape

The current backend live log message contains:

```ts
type LiveLogMessage = {
  eventId: string;
  ingestionId: string;
  applicationId: string;
  applicationName: string;
  applicationDisplayName: string | null;
  level: "INFO" | "WARN" | "ERROR" | "CRITICAL" | string;
  message: string;
  traceId: string | null;
  logTimestamp: string;
  processedAt: string;
};
```

Use `eventId` as the preferred row id. Use `applicationDisplayName` for display
when it is present, otherwise fall back to `applicationName`.

## Recommended Frontend Flow

1. Login and keep the access token from the normal auth flow.
2. Call `GET /api/v1/applications/me`.
3. Create the STOMP client with `Authorization: Bearer <accessToken>` in
   `connectHeaders`.
4. On connect, subscribe according to the selected Application filter.
5. Store received messages in a bounded local buffer.
6. Render `buffer -> application filter -> level filter -> keyword filter`.
7. When Application changes, resubscribe to the matching live log topic set.
8. When Level or Keyword changes, only recompute visible rows locally.

## Failure Handling

The frontend should treat WebSocket close or STOMP error during connect as an
authentication or connectivity failure. If the access token has expired, refresh
the HTTP auth session first, then reconnect using the new access token.

If a subscription is rejected, assume the selected application is no longer
authorized for the user. Refresh `GET /api/v1/applications/me` and rebuild the
Application filter options.
