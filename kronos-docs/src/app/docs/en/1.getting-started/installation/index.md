{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Runtime and compiler plugin

Add `kronos-core` for the ORM runtime and the Kronos compiler plugin for compile-time DSL support.

Use `{{ $.kronosVersion() }}` for copyable dependency examples. Use the snapshot version only when you are testing unreleased source changes.

```kotlin group="Gradle" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
}
```

```groovy group="Gradle" name="build.gradle" icon="gradle"
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.4.0'
    id 'com.kotlinorm.kronos-gradle-plugin' version '{{ $.kronosVersion() }}'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:{{ $.kronosVersion() }}'
}
```

```xml group="Maven" name="pom.xml" icon="maven"
<project>
    <properties>
        <kotlin.version>2.4.0</kotlin.version>
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

```kotlin group="Driver" name="MySQL example" icon="gradlekts"
dependencies {
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

## Minimum environment

- JDK 8 or later.
- Kotlin 2.4.0 or later.
- Maven 3.9+ or a Gradle version supported by Kotlin 2.4.0.

> **Warning**
> Make sure the Kotlin plugin used by your IDE supports Kotlin 2.4.0 or later.

Next, follow {{ $.keyword("getting-started/first-query", ["First Query"]) }} for the smallest model, table creation, insert, and select flow.
