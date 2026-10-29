{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Kotlin Multiplatform

For shared models, Native targets and version-catalog configuration, see {{ $.keyword("getting-started/kmp", ["Kotlin Multiplatform"]) }}.

## Runtime and compiler plugin

Add `kronos-core` for the ORM runtime and the Kronos compiler plugin for compile-time DSL support.

Use `{{ $.kronosVersion() }}` for copyable dependency examples. Use the snapshot version only when you are testing unreleased source changes.

## Kotlin Toolchain

Create a JVM application with the [Kotlin Toolchain wrapper](https://kotlin-toolchain.org/latest/cli/provisioning/). Add these two files at the project root and place Kotlin sources in `src/`.

```yaml group="Toolchain JVM" name="module.yaml"
product: jvm/app

dependencies:
  - com.kotlinorm:kronos-core:{{ $.kronosVersion() }}

settings:
  kotlin:
    version: 2.4.21
    compileIncrementally: false
    freeCompilerArgs:
      - -Xcollection-literals
      - -Xcontext-sensitive-resolution
    compilerPlugins:
      - id: kronos-compiler-plugin
        dependency: com.kotlinorm:kronos-compiler-plugin:{{ $.kronosVersion() }}
        options:
          generated-provider-id: com.example:app:main
          generated-provider-fq-name: com.kotlinorm.generated.factory.AppGeneratedTypeProvider
```

The registration file must contain the same name as `generated-provider-fq-name`, so the JVM loads entity and projection metadata from this module.

```text group="Toolchain JVM" name="resources/META-INF/services/com.kotlinorm.utils.GeneratedTypeProvider"
com.kotlinorm.generated.factory.AppGeneratedTypeProvider
```

After defining `main()` in `src/Main.kt`, run:

```bash group="Toolchain run" name="shell"
./kotlin run
```

For modules, test source sets and KMP, see {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }} and {{ $.keyword("getting-started/kmp", ["Kotlin Multiplatform"]) }}.

## Gradle

```kotlin group="Gradle" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.21"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcollection-literals", "-Xcontext-sensitive-resolution")
    }
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

```groovy group="Gradle" name="build.gradle" icon="gradle"
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.4.21'
    id 'com.kotlinorm.kronos-gradle-plugin' version '{{ $.kronosVersion() }}'
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcollection-literals", "-Xcontext-sensitive-resolution")
    }
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:{{ $.kronosVersion() }}'
}
```

## Maven

```xml group="Maven" name="pom.xml" icon="maven"
<project>
    <properties>
        <kotlin.version>2.4.21</kotlin.version>
        <kronos.version>{{ $.kronosVersion() }}</kronos.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>${kronos.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <extensions>true</extensions>
                <configuration>
                    <args>
                        <arg>-Xcollection-literals</arg>
                        <arg>-Xcontext-sensitive-resolution</arg>
                    </args>
                    <compilerPlugins>
                        <plugin>kronos-maven-plugin</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.kotlinorm</groupId>
                        <artifactId>kronos-maven-plugin</artifactId>
                        <version>${kronos.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

## JDBC wrapper

Add `kronos-jdbc-wrapper` when the application uses JDBC `DataSource` objects directly. The wrapper executes generated SQL and maps rows back to KPojo objects.

```yaml group="JDBC wrapper" name="Kotlin Toolchain"
dependencies:
  - com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}
```

```kotlin group="JDBC wrapper" name="Gradle" icon="gradlekts"
dependencies {
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
}
```

```xml group="JDBC wrapper" name="Maven" icon="maven"
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-jdbc-wrapper</artifactId>
    <version>${kronos.version}</version>
</dependency>
```

For connection setup, see {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }}. For Spring JDBC or other frameworks, see {{ $.keyword("database/custom-wrapper", ["Custom Wrapper"]) }}.

## Android SQLite

For Android/JVM and Android `SQLiteDatabase` setup, use the dedicated {{ $.keyword("database/android-sqlite", ["Android SQLite"]) }} chapter. It covers dependencies, the wrapper, transactions, logging, and the complete example.

## Database driver and pool

Choose JDBC driver and connection-pool versions that match your database server and JDK. The `<latest-stable>` placeholders below represent the current stable third-party versions for your runtime environment.

```yaml group="Driver" name="Kotlin Toolchain"
dependencies:
  - org.apache.commons:commons-dbcp2:<latest-stable>
  - com.mysql:mysql-connector-j:<latest-stable>
```

```kotlin group="Driver" name="MySQL example" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

## Minimum environment

- JDK 8 or later.
- Kotlin 2.4.21 or later.
- Kotlin Toolchain 0.13.0, Maven 3.9+, or a Gradle version supported by Kotlin 2.4.21.

> **Warning**
> Make sure the Kotlin plugin used by your IDE supports Kotlin 2.4.21 or later.

Next, follow {{ $.keyword("getting-started/first-query", ["First Query"]) }} for the smallest model, table creation, insert, and select flow.
