# External Example Metadata API Drift

## Symptom

`Kronos example projects` fails after checking out an external example repository and pointing it at the current local Kronos artifacts. A representative compiler error is an example source still calling the legacy KPojo metadata API, such as `user.kronosColumns()`, after current Kronos has migrated metadata access to properties like `user.__columns`.

## Cause

The examples workflow validates external repositories against the current Kronos checkout. Those repositories can lag behind breaking API migrations in this repository, so the failure may be in the checked-out example source even when this repository contains no stale call sites.

## Fix

Do not restore the legacy compatibility API just to make the example pass. Either update the external example repository, or keep a narrowly scoped workflow preparation step that migrates the checked-out example source to the current API before smoke tests run. The workflow should also scan the checked-out examples and fail clearly if `kronosColumns(` remains.

For the Solon example migration:

```diff
-            "columns" to user.kronosColumns().map {
+            "columns" to user.__columns.map {
```

## Prevention

When adding an external example smoke test, check whether the example source uses generated KPojo metadata members. If current Kronos exposes those members as properties, examples must use `__columns`, `__tableName`, and the other current metadata properties instead of legacy metadata functions.
