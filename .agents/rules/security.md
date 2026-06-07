# Security Rules

Apply to every change.

## Secrets

- Load secrets from environment variables or approved secret management.
- Never commit `.env`, `.env.local`, credentials, tokens, private keys, or API
  keys.
- Never log passwords, complete tokens, raw API keys, or connection strings
  containing credentials.
- Example values must be clearly fake.

## Access

Authentication and application-level authorization are planned but not yet
implemented.

- Enforce future authentication and authorization server-side.
- Deny protected access by default.
- Do not trust client-provided identity, role, application ownership, or
  permission claims.
- Store only hashed API-key material when API keys are implemented.

## Input And Output

- Validate all HTTP, event, and WebSocket input.
- Use structured APIs and parameterized queries.
- Do not expose stack traces, internal paths, or security decisions.
- Apply request and batch limits before production ingestion.

## Log Data

Treat ingested logs as potentially sensitive.

- Define redaction before accepting credentials, tokens, personal data, or
  complete request bodies.
- Do not copy sensitive payloads into application logs, errors, alerts, or
  test fixtures.
- Define retention and deletion behavior before production use.

## Dependencies And Configuration

- Use supported dependency versions.
- Review new dependencies for maintenance, license, and vulnerabilities.
- Keep production configuration fail-safe; do not rely on development
  passwords or permissive defaults.
- Report when security scanning or security tests are unavailable.
