#!/usr/bin/env bash
set -euo pipefail

if [[ "$(git rev-parse --is-inside-work-tree 2>/dev/null || true)" != "true" ]]; then
  printf '%s\n' "error: not inside a Git worktree" >&2
  exit 1
fi

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  printf '%s\n' "error: detached HEAD" >&2
  exit 1
fi

printf 'branch\n%s\n' "$branch"
printf '%s\n' "status"
git status --short --branch
printf '%s\n' "unstaged-check"
git diff --check
printf '%s\n' "staged-check"
git diff --staged --check
