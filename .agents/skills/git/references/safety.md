# Git Safety Rules

## Classify Changes

Only clear task-related files may be staged.

- **Include:** direct task files; related tests, docs, configuration,
  migrations, or schemas; tracked generated artifacts required by the
  established project workflow.
- **Exclude:** other tasks; personal/editor files; build, dependency, cache,
  log, temporary, or backup output; unrelated formatting/refactoring; secrets
  and local credentials.
- **Uncertain:** do not stage; explain the uncertainty without exposing
  sensitive values; ask the user.

Do not assume pre-staged files belong to the task. If they include unrelated
or uncertain paths, stop and ask without unstaging them.

## Review Before Staging

Semantically review the complete task-related diff for:

- Debug output, debugger calls, or temporary logging.
- Passwords, keys, tokens, credentials, private keys, connection strings, or
  secret files.
- Build, dependency, cache, log, temporary, editor, or backup artifacts.
- Dead or commented-out code.
- Accidental large or binary files.
- Unrelated behavior, formatting, or refactoring.

Pattern matches are evidence, not proof. Redact secret values in reports. A
safety or scope finding stops the workflow before commit; report the file and
location but do not fix task files in this skill.

## Stop Before Commit

Stop for an invalid worktree, detached HEAD, unknown task scope, uncertain
file relationship, pre-staged unrelated changes, unresolved safety finding,
empty staged diff, inseparable logical tasks, or commit failure.

Multiple tasks may be split only when their boundaries and path groups are
clear. Otherwise ask the user.

## Stop Before Push

Stop when post-commit approval is missing or declined, the branch changed or
cannot be determined, a commit hook changed the reviewed path set, no
upstream or `origin` exists, force/branch change/conflict resolution would be
required, or push fails.

Approval from an earlier request does not carry to a new commit. A later push
request must re-check the branch and commit state. Decline leaves the commit
local.

## Never

Never edit task files, silently unstage user changes, create an empty commit,
amend, reset, checkout, clean, rebase, merge, cherry-pick, change branches,
force push, push tags, create a release or pull request, or use destructive
recovery.
