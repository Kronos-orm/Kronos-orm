{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` strictly adheres to the Semantic Versioning 2.0.0 specification.

- **Current Version:** `0.0.4`

## Update Logs

### 0.0.1

- The first version of Kronos was released, providing all the features described in the ORM documentation.
- Upgraded the minimum supported Kotlin version to `2.1.0`.

### 0.0.2

- 🐛 Fix the problem that `@CreateTime`, `@Update` table-level annotation parameter `enable` is not effective when
  `false`.
  Fix global table create time, update time, logical delete field settings so that they only take effect when KPojo
  contains the field. ([#92](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 Optimize instantiation performance by removing the use of `map` as an intermediate variable when
  converting ([#94](https://github.com/Kronos-orm/Kronos-orm/pull/92)).
- 💪 Add default data source transaction creation function
  `fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?)` ([#94](https://github.com/Kronos-orm/Kronos-orm/pull/95))
- 💪Added cache support for insert statements, 20% performance improvement, see kronos-benchmark for detailed performance
  data ([#100](https://github.com/Kronos-orm/Kronos-orm/pull/100))
- Add `scale` property for `Field`, which is used to specify the scale of the decimal type, and refactor default mapping from kotlin type to Column type, change `java.math.BigDecimal` -> `decimal`(from `NUMERIC`), `kotlin.ByteArray` -> `BLOB`(from `BINARY`) ([#106](https://github.com/Kronos-orm/Kronos-orm/pull/106))

> **Note**
> The current version is in the development stage and does not guarantee backward compatibility.
>
> If you are interested, you can use the latest version for testing.

## Use Snapshots Version

[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v0.0.5--SNAPSHOT-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

Add maven repository for your project:

```groovy name="gradle(groovy)" icon="gradle" group="dependency"
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            url "https://central.sonatype.com/repository/maven-snapshots/"
        }
    }
}

// build.gradle
repositories {
    mavenCentral()
    maven {
        url "https://central.sonatype.com/repository/maven-snapshots/"
    }
}

plugins {
    id 'kronos-gradle-plugin' version '0.0.5-SNAPSHOT'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:0.0.5-SNAPSHOT"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:0.0.5-SNAPSHOT"
}
```

```groovy name="gradle(kts)" icon="gradlekts" group="dependency"
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        maven {
            name = "Maven Central Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// build.gradle.kts
repositories {
    mavenCentral()
    maven {
        name = "Maven Central Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

plugins {
    id("kronos-gradle-plugin") version "0.0.5-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.5-SNAPSHOT")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.0.5-SNAPSHOT")
}
```

```xml name="maven" icon="maven" group="dependency"
<project>
  <repositories>
    <repository>
      <id>maven-center-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-core</artifactId>
      <version>0.0.5-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.5-SNAPSHOT</version>
    </dependency>
  </dependencies>

  <pluginRepositories>
    <pluginRepository>
      <id>central-portal-snapshots</id>
      <url>https://central.sonatype.com/repository/maven-snapshots/</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>

  <plugins>
    <plugin>
      <groupId>org.jetbrains.kotlin</groupId>
      <artifactId>kotlin-maven-plugin</artifactId>
      <extensions>true</extensions>
      <configuration>
        <compilerPlugins>
          <plugin>all-open</plugin>
          <plugin>kronos-maven-plugin</plugin>
        </compilerPlugins>
      </configuration>
      <dependencies>
        <dependency>
          <groupId>org.jetbrains.kotlin</groupId>
          <artifactId>kotlin-maven-allopen</artifactId>
          <version>${kotlin.version}</version>
        </dependency>
        <dependency>
          <groupId>com.kotlinorm</groupId>
          <artifactId>kronos-maven-plugin</artifactId>
          <version>${kronos.version}</version>
        </dependency>
      </dependencies>
    </plugin>
  </plugins>
</project>
```
