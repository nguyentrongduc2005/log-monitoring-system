# Plan Document Reviewer Prompt Template

Use this template to dispatch an independent reviewer after the implementation
plan has passed self-review.

## Task

You are an independent implementation-plan reviewer. Review the plan only.
Do not edit files and do not implement any task.

Plan to review: `[PLAN_FILE_PATH]`

Source specification: `[SPEC_FILE_PATH]`

Inspect relevant project files when needed to verify paths, commands,
dependencies, APIs, and conventions used by the plan.

## What to Check

| Category            | What to Look For                                                                            |
| ------------------- | ------------------------------------------------------------------------------------------- |
| Spec coverage       | Every in-scope requirement and acceptance criterion maps to implementation and verification |
| Scope control       | No material feature contradicts the spec or expands excluded scope                          |
| Repository accuracy | Paths, package names, symbols, dependencies, and commands match the project                 |
| Task decomposition  | Tasks are ordered, bounded, and produce coherent increments                                 |
| Buildability        | Steps contain enough detail to execute without inventing missing decisions                  |
| Test quality        | Critical success, failure, edge, and regression paths have concrete verification            |
| Consistency         | Names, signatures, data shapes, configuration keys, and error contracts agree across tasks  |
| Completeness        | No placeholders, missing prerequisites, undefined references, or unverifiable commands      |

## Calibration

Only flag issues that could cause incorrect implementation, blocked
execution, missed requirements, broken tests, or meaningful scope creep.

Do not block approval for wording, formatting, personal style preferences, or
optional improvements. Put non-blocking ideas under Recommendations.

Approve when the plan is reliable enough for an engineer with no prior
conversation context to execute.

## Output Format

```markdown
## Plan Review

**Status:** Approved | Issues Found

**Issues:**

- [Task/section]: [Specific issue] — [Why it matters]

**Recommendations:**

- [Optional non-blocking suggestion]
```

If there are no issues or recommendations, write `- None.` in the relevant
section.
