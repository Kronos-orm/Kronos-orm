# 7. Output Organization (Expanded)

This chapter clarifies how output paths and package names are resolved.

## Output path

- Each table produces one Kotlin file at: `<output.targetDir>/<ClassName>.kt`.
- `output.targetDir` can be absolute or relative to the current working directory where your script/test runs.

## Package name resolution

- If `output.packageName` is provided, use it.
- Else attempt to infer from `targetDir` by locating the segment after `main/kotlin/` and converting slashes to dots.
- If inference fails, fallback to `com.kotlinorm.orm.table`.

## Determinism considerations

- Keep `targetDir` stable across environments if you commit generated code.
- For per-build generation, target a build directory (e.g., `build/generated/kotlin/main`).
