# 13. Contributing (Expanded)

## Areas to improve
- Template ergonomics and DSL readability.
- Type mapping gaps for less common SQL types.
- Additional strategies (e.g., soft delete variants, audit fields).
- Better diagnostics for DataSource reflection issues.

## Coding guidelines
- Keep algorithms deterministic and side-effects explicit.
- Strongly type inputs and fail fast with actionable messages.
- Write tests under `kronos-testing` and prefer asserting exact output lines for stability.

## PR checklist
- Include docs updates when changing APIs/behaviors.
- Run tests and static checks (detekt).
- Add examples when introducing new features.
