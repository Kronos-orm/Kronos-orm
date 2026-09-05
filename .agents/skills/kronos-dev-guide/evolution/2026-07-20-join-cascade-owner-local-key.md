# Non-root JOIN Cascade Must Preserve The Owner Local Key

## Symptom

A JOIN projection can select a cascade relation from a non-root owner source, for example `owner.profile`, while the
owner's local key is `profileId`. The relation itself is not a physical SQL column, so a projection that only emits the
visible relation loses the key required to load the cascade. The resulting row may contain a null relation, an incomplete
`__columns` list, or a cascade lookup that cannot be performed.

The failure is easy to miss when the owner is the root JOIN source because root-source fallback can accidentally retain
the key. It is exposed when the owner is the non-root source and the root has a different projection shape.

## Cause

Cascade local/target key metadata belongs to the selected owner source, not necessarily to the JOIN root. Projection
materialization must therefore distinguish the relation field from its hidden local key. Treating the relation as an
ordinary physical field either emits a nonexistent `profile` column or drops the physical `profile_id` key and its
logical output label.

## Fix

- Materialize the non-root owner's cascade relation with its hidden local key in the Selected shape.
- Render the physical key once with its logical label, such as `profile_id AS profileId`; never render the relation
  property as a database column.
- Keep the hidden key in mapped data and `__columns` so cascade mapping can issue the target lookup after the main
  query. The owner source identity must remain intact through JOIN projection lowering and runtime mapping.

## Prevention

For every cascade projection path, include an official compiler box fixture whose owner is a non-root JOIN source and
whose relation has no physical column. Assert all of the following:

- generated SQL selects the physical local key with its logical label (`profile_id AS profileId`);
- generated SQL does not select the relation property (`profile`);
- mapped Selected data retains hidden `profileId` and exposes the expected `__columns` order;
- the cascade query runs exactly once and backfills the relation value.

Do not use a root-only cascade fixture as proof of JOIN source ownership. Keep the local-key assertions beside the
selected receiver/type assertions so a future source fallback cannot silently pass.

Verified with:

```text
./gradlew :kronos-compiler-plugin:test --tests 'com.kotlinorm.compiler.ProjectionBoxTest.joinNonRootCascadeProjection' --no-daemon --max-workers=1 --console=plain
```

The focused regression passed with the real SQL projection `profile_id AS profileId`, no physical `profile` column,
hidden `profileId` retained in the mapped row, and one cascade lookup that backfilled `profile`.
