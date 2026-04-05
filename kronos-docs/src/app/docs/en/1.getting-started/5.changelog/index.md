{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` strictly adheres to the Semantic Versioning 2.0.0 specification.

- **Current Version:** `0.0.6`

## Update Logs

### 0.0.6

- ✨ Update Kotlin version to `2.2.21`, add support for `kotlin.time.Instant` ([#191](https://github.com/Kronos-orm/Kronos-orm/pull/191))
- ✨ Add patch functionality to select clause ([#192](https://github.com/Kronos-orm/Kronos-orm/pull/192))
- ✨ Add `takeIf` condition support for conditional query handling ([#178](https://github.com/Kronos-orm/Kronos-orm/pull/178))
- 💪 Enhance upsert logic with null value handling and logic delete strategy ([#188](https://github.com/Kronos-orm/Kronos-orm/pull/188))
- 🐛 Fix criteria missing when join with on clause ([#194](https://github.com/Kronos-orm/Kronos-orm/pull/194))
- 🐛 Fix `java.sql.Date` conversion logic and add test case for `LocalDate` type parsing ([#185](https://github.com/Kronos-orm/Kronos-orm/pull/185))
- 🐛 Fix jdbc wrapper `getObject(position)` NPE ([#184](https://github.com/Kronos-orm/Kronos-orm/pull/184))
- 🐛 Handle empty default values in column definitions ([#175](https://github.com/Kronos-orm/Kronos-orm/pull/175))

### 0.0.5

- ✨ Add kClass attribute to DeleteClauseInfo and InsertClauseInfo ([#152](https://github.com/Kronos-orm/Kronos-orm/pull/152))
- 💪 Optimize query parameter processing, fix invalid parameter mapping ([#150](https://github.com/Kronos-orm/Kronos-orm/pull/150))

### 0.0.4

- ✨ Add default boolean value handling method `getDefaultBoolean` ([#147](https://github.com/Kronos-orm/Kronos-orm/pull/147))
- 💪 Modify cache implementation to use existing instance instead of creating new instance each time ([#145](https://github.com/Kronos-orm/Kronos-orm/pull/145))
- 🐛 Ensure table names are properly quoted in PostgreSQL SQL statements ([#142](https://github.com/Kronos-orm/Kronos-orm/pull/142))

### 0.0.3

- ✨ Add new CodeGen module for code generation ([#130](https://github.com/Kronos-orm/Kronos-orm/pull/130))
- ✨ Add DataGuardPlugin to prevent deletion, update, and truncation operations on tables
- ✨ Add LastInsertIdPlugin to support retrieving the last inserted ID
- ✨ Add task event hooks support with QueryEvent and ActionEvent ([#123](https://github.com/Kronos-orm/Kronos-orm/pull/123))
- ✨ Add cache and identity configuration ([#122](https://github.com/Kronos-orm/Kronos-orm/pull/122))
- ✨ Add named SQL parse cache ([#116](https://github.com/Kronos-orm/Kronos-orm/pull/116))
- 💪 Refactor jdbc wrapper ([#117](https://github.com/Kronos-orm/Kronos-orm/pull/117))
- 💪 Refactor LastInsertIdPlugin enable logic using plugin registration/unregistration ([#125](https://github.com/Kronos-orm/Kronos-orm/pull/125))
- 💪 Update logging DSL ([#137](https://github.com/Kronos-orm/Kronos-orm/pull/137))
- 🐛 Fix null value handling logic in InsertClause to adapt SpringData
- 🐛 Fix field index in coverage calculation ([#133](https://github.com/Kronos-orm/Kronos-orm/pull/133))
- 🔧 Remove KronosKClassMapperTransformer ([#131](https://github.com/Kronos-orm/Kronos-orm/pull/131))

### 0.0.2

- 🐛 Fix the problem that `@CreateTime`, `@Update` table-level annotation parameter `enable` is not effective when
  `false`.
  Fix global table create time, update time, logical delete field settings so that they only take effect when KPojo
  contains the field. ([#92](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 Optimize instantiation performance by removing the use of `map` as an intermediate variable when
  converting ([#94](https://github.com/Kronos-orm/Kronos-orm/pull/92))
- 💪 Add default data source transaction creation function
  `fun transact(wrapper: KronosDataSourceWrapper? = null, block: () -> Any?)` ([#94](https://github.com/Kronos-orm/Kronos-orm/pull/95))
- 💪 Add cache support for insert statements, 20% performance improvement, see kronos-benchmark for detailed performance
  data ([#100](https://github.com/Kronos-orm/Kronos-orm/pull/100))
- ✨ Add `scale` property for `Field`, which is used to specify the scale of the decimal type, and refactor default mapping from kotlin type to Column type, change `java.math.BigDecimal` -> `decimal`(from `NUMERIC`), `kotlin.ByteArray` -> `BLOB`(from `BINARY`) ([#106](https://github.com/Kronos-orm/Kronos-orm/pull/106))

### 0.0.1

- The first version of Kronos was released, providing all the features described in the ORM documentation.
- Upgraded the minimum supported Kotlin version to `2.1.0`.

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
    id 'kronos-gradle-plugin' version '0.0.7-SNAPSHOT'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:0.0.7-SNAPSHOT"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:0.0.7-SNAPSHOT"
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
    id("kronos-gradle-plugin") version "0.0.7-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.7-SNAPSHOT")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.0.7-SNAPSHOT")
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
      <version>0.0.7-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>0.0.7-SNAPSHOT</version>
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
