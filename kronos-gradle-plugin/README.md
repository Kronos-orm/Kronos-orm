# Module kronos-gradle-plugin

Gradle plugin that wires the Kronos compiler plugin into Kotlin compilation.

## Usage

```kotlin
plugins {
    kotlin("jvm") version "2.4.21"
    id("com.kotlinorm.kronos-gradle-plugin") version "0.4.0"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcollection-literals", "-Xcontext-sensitive-resolution")
    }
}
```

Collection literals enable DSL forms such as `select { [it.id, it.name] }`.

## How It Works

Implements `KotlinCompilerPluginSupportPlugin`. Registers the `kronos-compiler-plugin` artifact as a compiler plugin dependency, making all IR transformations (KPojo augmentation, DSL parsing) happen automatically during compilation.

## Build

Kotlin Toolchain builds this JVM library with the Gradle plugin descriptor and compiler version resource from `src/main/resources`. The release script adds the standard Maven plugin marker.

```sh
scripts/run build test -m kronos-gradle-plugin -p jvm
```

Gradle consumption is verified through the [Android](https://github.com/Kronos-orm/kronos-example-android), [Spring Boot](https://github.com/Kronos-orm/kronos-example-spring-boot), [Ktor](https://github.com/Kronos-orm/kronos-example-ktor), and [Vert.x](https://github.com/Kronos-orm/kronos-example-vertx) applications. Their CI builds consume the staged publications. The [Solon](https://github.com/Kronos-orm/kronos-example-solon) and Spring Boot Maven applications verify Maven integration.

## Version Sync

Use `scripts/run version set <version>` to update `libs.versions.toml`, the shared publication template and the compiler version resource together.
