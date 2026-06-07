# Documentation Rules

Apply when a change affects behavior, architecture, APIs, data,
configuration, setup, or developer workflows.

## Sources

- `README.md`: project introduction, setup, commands, and roadmap
- `docs/architecture.md`: detailed architecture when created
- `docs/api.md`: API behavior when created
- `docs/database.md`: storage design when created
- `docs/api/openapi.json`: generated API contract
- `.agents/context/`: concise agent orientation

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

## Review

Check links, commands, paths, examples, generated artifacts, stale claims, and
secrets. Report behavior that cannot be verified rather than guessing.
