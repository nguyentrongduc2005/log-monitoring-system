# API Design Rules

Apply when creating or changing HTTP, event, WebSocket, or public application
contracts.

## Contract

- HTTP APIs use Spring MVC and OpenAPI.
- The intended REST namespace is `/api/v1`.
- OpenAPI source is Spring controllers and DTOs.
- Generated artifacts are `docs/api/openapi.json` and
  `apps/frontend/src/api/generated/api-types.ts`.
- Regenerate with `make api` while the backend is running.
- Never edit generated contracts manually.

## HTTP

- Use resource-oriented, consistently pluralized paths.
- Validate requests at the boundary.
- Define required, optional, nullable, and default behavior explicitly.
- Use DTOs; never expose persistence entities.
- Use ISO 8601 timestamps with an explicit timezone.
- Keep error responses stable and do not expose stack traces.
- Use standard statuses: `400` validation, `401` unauthenticated, `403`
  forbidden, `404` missing resource, `409` conflict, and `500` unexpected
  failure.

Do not invent pagination, envelopes, identifier formats, request-size limits,
or error codes without an approved contract.

## Events And Live Delivery

- Version event payloads when compatibility matters.
- Define producer, consumer, topic, key, ordering, retry, and DLQ behavior.
- Consumers must be safe against duplicate delivery.
- Keep WebSocket/STOMP payloads aligned with backend DTOs.
- Do not treat target topic names beyond evidenced names such as `logs.raw` as
  implemented configuration.

## Compatibility And Security

- Prefer additive changes.
- Require approval for breaking contracts.
- Authenticate and authorize protected operations server-side.
- Never return credentials, API-key values, or internal security state.
- Update API documentation and generated types with contract changes.
