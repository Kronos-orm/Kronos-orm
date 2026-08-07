# macOS Version Bump Script Must Use POSIX Whitespace Matching

## Symptom

`bash .github/scripts/bump-version.sh set <version>` printed the requested
version on macOS but left `publishing.gradle.kts` and `KronosGradlePlugin.kt`
unchanged.

## Cause

BSD `sed` does not interpret `\\s` as a whitespace character in extended
regular expressions. The replacement expressions therefore matched no version
assignment and still exited successfully.

## Resolution

Use `[[:space:]]*` in both the read and replacement expressions in
`.github/scripts/bump-version.sh`. This syntax works with BSD and GNU `sed`.

## Verification

```bash
bash .github/scripts/bump-version.sh set 0.3.1
rg -n 'project\\.version|version\\s*=' \
  build-logic/src/main/kotlin/publishing.gradle.kts \
  kronos-gradle-plugin/src/main/kotlin/com/kotlinorm/compiler/plugin/KronosGradlePlugin.kt
```

The command must report the requested version in both files.
