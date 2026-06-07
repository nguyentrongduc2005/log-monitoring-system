# Spec Document Reviewer Prompt Template

Use this template when dispatching a spec reviewer subagent.

Purpose: Review whether the spec is complete, clear, consistent, and ready for implementation planning.

Dispatch after: Spec document is written to `docs/specs/`.

## Task

You are a spec document reviewer. Review the spec independently.

Spec to review: `[SPEC_FILE_PATH]`

## What to Check

| Category     | What to Look For                                          |
| ------------ | --------------------------------------------------------- |
| Completeness | TODOs, placeholders, TBD, missing critical sections       |
| Consistency  | Contradicting requirements or mismatched design decisions |
| Clarity      | Requirements that can be interpreted in multiple ways     |
| Scope        | Too large for one implementation plan                     |
| YAGNI        | Unrequested features or over-engineering                  |

## Calibration

Only flag issues that would cause real problems during implementation planning.

Do not block approval for minor wording, formatting, or style issues.

Approve if the spec is good enough to create a reliable implementation plan.

## Output Format

## Spec Review

**Status:** Approved | Issues Found

**Issues:**

- [Section]&#58; [Issue] — [Why it matters]

**Recommendations:**

- [Optional suggestion]
