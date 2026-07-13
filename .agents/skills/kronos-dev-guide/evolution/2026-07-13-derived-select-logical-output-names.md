# Derived Select Logical Output Names

## Symptom

`DslIntegrationBoxTest.selectNoProjectionNextSource` can appear to fail after projection/source-binding changes with:

```text
expected: <OK> but was: <Fail: selected columns were [id, name]>
```

This happens when a chained select uses a source field whose physical column name differs from its logical property name, for example `@Column("user_name") var name`.

## Cause

A derived select exposes the inner query's output labels to the outer query. Even when the inner query was built without a generated projection DTO, the inner select item for `name` is rendered as the logical output label, so the outer query must reference `q.name`, not `q.user_name`.

Treating no-projection derived selects as if they should keep physical column names makes the outer query reference a column that the derived table does not expose.

## Fix

Keep the runtime source-binding behavior that maps derived source columns to logical output names. The test expectation should assert outer selected columns such as:

```text
[id, name]
```

while the inner query may still select physical columns such as:

```text
[id, user_name, status]
```

## Prevention

When changing `SourceBinding`, `SelectContext`, or `SelectPlanner`, distinguish:

- inner table columns: physical database column names,
- derived table output labels: logical names visible to the next query layer.

For chained selects, assert both inner physical columns and outer logical output names.
