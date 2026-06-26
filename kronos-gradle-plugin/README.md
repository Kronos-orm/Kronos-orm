# kronos-gradle-plugin

Gradle plugin that wires the Kronos compiler plugin into Kotlin compilation.

## Usage

```kotlin
plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.1.0-SNAPSHOT"
}
```

## How It Works

Implements `KotlinCompilerPluginSupportPlugin`. Registers the `kronos-compiler-plugin` artifact as a compiler plugin dependency, making all IR transformations (KPojo augmentation, DSL parsing) happen automatically during compilation.

## Build Note

This is an **included build** (not a regular subproject) — configured via `includeBuild("kronos-gradle-plugin")` in root `settings.gradle.kts`.

## Version Sync

The version in `KronosGradlePlugin.kt` must stay in sync with `publishing.gradle.kts`. Use `.github/scripts/bump-version.sh` to update both.
