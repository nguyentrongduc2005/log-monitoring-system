#!/usr/bin/env bash
set -euo pipefail

usage() {
  printf '%s\n' \
    "usage: verify-commit.sh <expected-branch> -- <reviewed-path>..." >&2
  exit 2
}

[[ $# -ge 3 ]] || usage
expected_branch="$1"
[[ "$2" == "--" ]] || usage
shift 2
reviewed_paths=("$@")
[[ ${#reviewed_paths[@]} -gt 0 ]] || usage

for path in "${reviewed_paths[@]}"; do
  if [[ "$path" == *$'\n'* ]]; then
    printf '%s\n' "error: reviewed paths containing newlines are unsupported" >&2
    exit 2
  fi
done

if [[ "$(git rev-parse --is-inside-work-tree 2>/dev/null || true)" != "true" ]]; then
  printf '%s\n' "error: not inside a Git worktree" >&2
  exit 1
fi

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  printf '%s\n' "error: detached HEAD" >&2
  exit 1
fi
if [[ "$branch" != "$expected_branch" ]]; then
  printf 'error: branch mismatch: expected %s, got %s\n' \
    "$expected_branch" "$branch" >&2
  exit 1
fi

hash="$(git rev-parse HEAD)"
subject="$(git show -s --format=%s HEAD)"
mapfile -d '' committed_paths < <(
  git diff-tree --root --no-commit-id --name-only -r -z HEAD
)

for path in "${committed_paths[@]}"; do
  if [[ "$path" == *$'\n'* ]]; then
    printf '%s\n' "error: committed paths containing newlines are unsupported" >&2
    exit 1
  fi
done

mapfile -t reviewed_sorted < <(
  printf '%s\n' "${reviewed_paths[@]}" | LC_ALL=C sort -u
)
mapfile -t committed_sorted < <(
  printf '%s\n' "${committed_paths[@]}" | LC_ALL=C sort -u
)

missing=()
unexpected=()
for path in "${reviewed_sorted[@]}"; do
  found=false
  for committed in "${committed_sorted[@]}"; do
    [[ "$path" == "$committed" ]] && found=true && break
  done
  "$found" || missing+=("$path")
done
for path in "${committed_sorted[@]}"; do
  found=false
  for reviewed in "${reviewed_sorted[@]}"; do
    [[ "$path" == "$reviewed" ]] && found=true && break
  done
  "$found" || unexpected+=("$path")
done

if (( ${#missing[@]} > 0 || ${#unexpected[@]} > 0 )); then
  if (( ${#missing[@]} > 0 )); then
    printf '%s\n' "missing-reviewed-paths" >&2
    printf '%s\n' "${missing[@]}" >&2
  fi
  if (( ${#unexpected[@]} > 0 )); then
    printf '%s\n' "unexpected-committed-paths" >&2
    printf '%s\n' "${unexpected[@]}" >&2
  fi
  exit 1
fi

printf 'hash\n%s\n' "$hash"
printf 'subject\n%s\n' "$subject"
printf 'branch\n%s\n' "$branch"
printf '%s\n' "committed-paths"
printf '%s\n' "${committed_sorted[@]}"
