# Task 1: Lock the Public Opt-In Contract

Progress: 100%
Status: Done

## Goal

Define and publish one lightweight user-facing opt-in marker that acknowledges deterministic duplicate projection naming and selected-field replacement in Context.

## Current State

- The final marker is `com.kotlinorm.annotations.UnsafeProjectionOverride`.
- `kronos-core` exposes the binary-retained, error-level `UnsafeProjectionOverride` marker.
- Its current message covers both `_N` suffix allocation and selected-field Context replacement, and recommends explicit aliases.

## Follow-Up Work

- Added `UnsafeProjectionOverride` in the existing `com.kotlinorm.annotations` package with no compiler-plugin dependency.
- Expanded the contract from the original alias-only proposal to the unified projection-conflict mechanism.

## Acceptance

- User source can import the marker from the supported Kronos annotation package.
- The marker is `@RequiresOptIn(Level.ERROR, ...)`, not a warning or ordinary suppression.
- The marker declaration compiles with the repository's Kotlin version and supports the intended expression/function/class/file opt-in forms.
- No compiler plugin class is required on the user's annotation compile classpath.
- The message covers duplicate preservation and Context replacement without making ordinary non-conflicting aliases experimental.

## Verification Record

- `./gradlew :kronos-core:compileKotlin`: pass via targeted compiler-plugin test dependency build.
- `@OptIn(UnsafeProjectionOverride::class)` is exercised by `projectionAliasOptInScopes.kt` and `selectedAliasOverrideType.kt`.
