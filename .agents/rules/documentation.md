# Documentation Rules

Apply when a change affects behavior, architecture, APIs, data,
configuration, setup, or developer workflows.

## Sources

- `README.md`: project introduction, setup, commands, and roadmap
- `docs/api/README.md`: target API behavior and MVP defaults
- `docs/api/openapi.json`: generated API contract
- `.agents/context/`: concise agent orientation
- `DETAI.md`: authoritative assignment scope

`docs/module-requirement.md` and `docs/store-requirement.md` are temporary
planning drafts. Update or review them only when the user explicitly requests
those files; do not treat them as official sources.

Implementation and executable configuration take precedence over roadmap text.
Do not document planned behavior as implemented.

## Updates

- Update architecture docs for boundaries, ownership, integrations, or data
  flow.
- Update API docs and generated types for contract changes.
- Update database docs for schemas, indexes, constraints, or retention.
- Update README for setup, environment, ports, or common commands.
- Keep context files concise and avoid duplicating detailed documentation.

## Style

- Write concise English unless the target document already uses Vietnamese.
- Use exact, verified commands and paths.
- State prerequisites, defaults, and implementation status.
- Use fake credentials in examples.
- Remove placeholders and empty sections.
- Keep Mermaid diagrams and terminology consistent with code.
- Clearly label differences between current implementation and the approved
  Modular Monolith target.
- Use `module` for internal business boundaries; do not describe modules as
  independently deployed services.
- State that backend modules use PostgreSQL for transactional/configuration
  data and ClickHouse for logs/analytics.
- State that Kafka is mandatory for the raw-log processing pipeline and Redis
  is mandatory for alert deduplication.
- Do not apply backend module package rules to the existing frontend
  architecture.

## Review

Check links, commands, paths, examples, generated artifacts, stale claims, and
secrets. Report behavior that cannot be verified rather than guessing.
