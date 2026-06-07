---
name: git
description: "Use after completed work to review task-related changes, stage and commit one logical task automatically, then request approval before optionally pushing the current branch. Do not use for implementation, unrelated files, branch/history changes, or force push."
---

# Preparing and Publishing Git Changes

Workflow: `Inspect → Review → Stage → Commit → Report → Approval → Push`

This skill performs Git workflow only. It does not implement, fix, document,
release, or modify task files.

<HARD-GATE>
Require a valid worktree and attached branch. Never edit task files; stage
unrelated or uncertain changes; commit secrets, debug code, or artifacts;
silently unstage user changes; create empty commits; rewrite history; change
branches; or force push.

Push only after reporting the new local commit and receiving explicit approval
for that commit. Follow `references/safety.md` for detailed classification,
review, stop, and prohibition rules.
</HARD-GATE>

## Inspect

Run from the skill directory:

```bash
scripts/inspect.sh
git diff
git diff --staged
```

Stop when the script fails. Read both full diffs; the script provides evidence
but does not replace semantic review. Record pre-staged changes separately and
do not assume they belong to the task.

## Review

Read `references/safety.md` now. Determine task scope from the conversation,
approved plan, completion report, or explicit file list.

Classify every changed file. Continue only with clear task-related files.
Uncertain relationships require user input. Any unresolved safety or scope
finding stops before commit.

## Stage

Stage only reviewed paths:

```bash
git add -- <explicit-task-paths>
git status --short
git diff --staged --check
git diff --staged
```

Require a non-empty staged diff containing one logical task and exactly the
reviewed paths. Preserve unrelated changes. Never silently unstage pre-staged
files; ask when their relationship is unclear.

## Commit

Derive a Conventional Commit from staged intent:

```text
<type>(<optional-scope>): <imperative summary>
```

Use `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, or
`perf`. Use a stable scope only and avoid vague summaries.

Record the current branch and reviewed staged paths, then commit automatically:

```bash
git commit -m "<message>"
scripts/verify-commit.sh <recorded-branch> -- <reviewed-paths...>
```

Verification returns hash, subject, branch, and committed paths. On failure or
path mutation, stop before push; never amend, reset, or rewrite the commit.

## Report And Approval

Report the hash, subject, branch, committed paths, and review result; only then
ask for explicit approval to push. Earlier approval does not carry forward. A
decline leaves the commit local; a later request must re-check branch/commit.

## Push

After approval, re-check:

```bash
git branch --show-current
git status --short --branch
git rev-parse --abbrev-ref --symbolic-full-name @{u}
```

Stop if the branch differs from the recorded branch. If upstream exists:

```bash
git push
```

Without upstream, verify `git remote get-url origin`, then:

```bash
git push -u origin <current-branch>
```

Missing `origin`, force, branch change, conflict resolution, or push failure
is a blocker; report it without destructive recovery.

## Final Output

Report committed files; hash/message; branch/review result; approval/push
status and command/remote when pushed; remaining unrelated changes or blockers.
