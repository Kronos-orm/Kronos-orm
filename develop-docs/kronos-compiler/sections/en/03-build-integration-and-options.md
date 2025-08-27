# 3. Build Integration and Plugin Options

This section explains how to enable kronos-compiler-plugin in Gradle/Maven and configure its CLI options.

## 3.1 Gradle (Kotlin DSL)

Prerequisites: Kotlin 1.9+ with K2 enabled (K2 is default or enabled via options).

Example:

```kotlin
plugins {
    kotlin("jvm") version "<your-kotlin-version>"
}

dependencies {
    // Put the compiler plugin on the compiler classpath (compileOnly or kotlinCompilerPluginClasspath)
    compileOnly("io.github.kotlin-orm:kronos-compiler-plugin:<version>")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xplugin=kronos-compiler-plugin",
            "-P",
            "plugin:kronos-compiler-plugin:debug=true",
            "-P",
            "plugin:kronos-compiler-plugin:debug-info-path=build/tmp/kronosIrDebug"
        )
    }
}
```

Notes:
- Plugin ID is `kronos-compiler-plugin`;
- `debug` (optional): when enabled, IR will be dumped into `debug-info-path` at the end of the compilation;
- `debug-info-path` (optional): output path for IR dump, default `build/tmp/kronosIrDebug`.

Depending on Kotlin/Gradle versions, classpath wiring may vary; you can also use the official `kotlinCompilerPluginClasspath` configuration.

## 3.2 Maven

Example:

```xml
<plugin>
  <groupId>org.jetbrains.kotlin</groupId>
  <artifactId>kotlin-maven-plugin</artifactId>
  <version>${kotlin.version}</version>
  <configuration>
    <args>
      <arg>-Xplugin=kronos-compiler-plugin</arg>
      <arg>-P</arg>
      <arg>plugin:kronos-compiler-plugin:debug=true</arg>
      <arg>-P</arg>
      <arg>plugin:kronos-compiler-plugin:debug-info-path=build/tmp/kronosIrDebug</arg>
    </args>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>io.github.kotlin-orm</groupId>
      <artifactId>kronos-compiler-plugin</artifactId>
      <version>${kronos.compiler.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

## 3.3 Option Summary

- `debug` (Boolean, default false): enable IR dump;
- `debug-info-path` (String, default `build/tmp/kronosIrDebug`): directory where IR dump files are written.
