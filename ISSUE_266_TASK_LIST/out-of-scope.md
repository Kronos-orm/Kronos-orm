# Out Of Scope

- Making every alias operation experimental or requiring opt-in for non-colliding aliases.
- Adding a custom suppression annotation, a user-facing `.overrideAlias(...)` DSL modifier, or manual Kotlin parent-scope traversal.
- Changing same-layer receiver semantics so selected aliases become available to `where`, `groupBy`, or `having`.
- Supporting arbitrary runtime type coercion, reflection-based mapping, or blanket `Any?` generated fields.
- Adding user-managed SQL table aliases for ordinary, derived, or nested JOIN sources.
- Preserving the old statement-style JOIN API through deprecated forwarding overloads.
- Redesigning scalar-subquery, aggregate, window, or insert-select syntax beyond the naming/composition work required here.
- Changing project versions or publishing a release from this task-list update. Task 10 prepares and verifies compatibility; release execution remains a separate explicit action.
- Running the documentation build locally; the user has stated documentation verification may be left to PR CI.
