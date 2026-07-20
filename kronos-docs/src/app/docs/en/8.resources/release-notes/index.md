{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

`Kronos` strictly adheres to the Semantic Versioning 2.0.0 specification.

- **Current Version:** `{{ $.kronosVersion() }}`

## Update Logs

### 0.2.4

- 🐛 Fix generated projection typing for identity-source selections and preserve source metadata, including `@Serialize`, across aliased, generated, window-derived, and nested projections ([#246](https://github.com/Kronos-orm/Kronos-orm/issues/246)).
- 🐛 Fix ordinary Kotlin `if` / `when` condition lowering so only the selected SQL branch and its parameters are emitted; make `contains`, `startsWith`, and `endsWith` escape literal LIKE wildcards ([#249](https://github.com/Kronos-orm/Kronos-orm/issues/249)).
- 🐛 Fix self-joins, repeated-table joins, correlated subqueries, and nested query layers by binding fields to runtime source identities and renaming colliding parameters ([#252](https://github.com/Kronos-orm/Kronos-orm/issues/252)).
- 🐛 Fix named-parameter binding so collections expand only at explicit list occurrences, while `ByteArray` and other array values remain single JDBC parameters; add JDBC type hints for null temporal and binary values ([#251](https://github.com/Kronos-orm/Kronos-orm/issues/251)).
- 🐛 Make cursor pagination deterministic by appending a primary or unique-key tie-breaker, carrying hidden cursor fields for map results, and preserving rows tied on a non-unique order value. Also fix `limit(0)` ([#248](https://github.com/Kronos-orm/Kronos-orm/issues/248)).
- 🐛 Fix upsert conflict-target inference for generated custom primary keys and replace aggregate existence probes with `SELECT 1`, allowing PostgreSQL row locking ([#247](https://github.com/Kronos-orm/Kronos-orm/issues/247)).
- 🐛 Improve temporal conversion, DDL/default rendering, boolean predicates, CREATE TABLE AS SELECT, schema synchronization, codegen data-source setters, and mutation SQL across MySQL, PostgreSQL, SQLite, SQL Server, and Oracle ([#250](https://github.com/Kronos-orm/Kronos-orm/issues/250)).

#### Upgrade notes

- Replace condition-level `ifNoValue(...)` calls with ordinary Kotlin control flow. Use `.takeIf(...)` to omit a predicate, or `if` / `else` with `true.asSql()` / `false.asSql()` for an explicit fallback.
- Cursor pagination now requires a primary key or unique index when the requested ordering is not already unique. Map results hide automatically added tie-breaker columns; typed projections must select every cursor field needed to construct the next token.

### 0.2.3

- ✨ Use typed cursor pagination through `cursor(pageSize, after)`, returning `CursorResult(hasNext, nextCursor, records)` as an execution-only stage.
- ✨ Use `page(...).withTotal().toList()` / `toMapList()` for named `PageResult(total, records, totalPages, pageIndex, pageSize)` values; offset pages remain selectable.
- 🐛 Fix derived-source and window-alias projection SQL so logical output names such as `rn`, `userName`, and `createTime` remain available in later query layers.
- 🧩 Improve the IDEA plugin projection experience, including generated projection completion for `it.` and safer handling of IntelliJ cancellation exceptions.
- 📚 Refresh README, documentation macros, release snippets, and AI skill guidance for `0.2.3`.

### 0.2.2

- ✨ Add metadata-backed KPojo runtime properties and refresh the public metadata guidance for generated column access ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240)).
- 🐛 Fix compiler/core DSL handling for null-safe conditions, Elvis/selectable set values, generated keys, cascade and strategy edge cases, and DDL metadata casing ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240)).
- 🐛 Publish `kronos-syntax` as an API dependency of `kronos-core` so downstream projects can compile DSL calls that expose syntax types without declaring `kronos-syntax` directly ([#241](https://github.com/Kronos-orm/Kronos-orm/pull/241)).
- 🎨 Refresh documentation UI details, including the dark-theme language switcher, logo assets, and responsive docs layout ([#238](https://github.com/Kronos-orm/Kronos-orm/pull/238), [#239](https://github.com/Kronos-orm/Kronos-orm/pull/239), [#240](https://github.com/Kronos-orm/Kronos-orm/pull/240)).
- 🔧 Add example-project smoke tests for Ktor, Spring Boot, and Solon, publish current artifacts to Maven local in CI, skip signing for local release-version publishing, and update examples for the current metadata API ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240), [#241](https://github.com/Kronos-orm/Kronos-orm/pull/241)).
- 💪 Expand compiler, core, integration, and example coverage for projection, condition, set, utility, SQL Server, and strategy edge cases ([#240](https://github.com/Kronos-orm/Kronos-orm/pull/240)).
- 📦 Prepare the stable `0.2.2` release and update install snippets, documentation macros, plugin README files, and AI skill guidance.

### 0.2.1

- ✨ Support `it - [it.id, it.age]` inside projection `[]`, including mixed projection lists such as `[it - [it.id, it.age], it.id.alias("sourceId")]`.
- 🔧 Make the release workflow's next SNAPSHOT PR step idempotent so reruns skip an already-open version PR instead of failing on an existing branch.
- 📚 Refresh README, documentation macros, user-facing examples, and AI skill references for `0.2.1`.

### 0.2.0

> **Warning**
> `0.2.0` contains source- and binary-incompatible public API changes. Recompile the project after upgrading and update call sites and custom integrations as described below.

#### Breaking API changes

Query terminal operations now use collection-conversion and single-row terminology consistently:

| `0.1.x` | `0.2.0` |
| --- | --- |
| `query()` | `toMapList()` / `toList<Map<String, Any?>>()` |
| `queryList<T>()` | `toList<T>()` |
| `queryMap()` | `toMap()` / `first<Map<String, Any?>>()` |
| `queryMapOrNull()` | `toMapOrNull()` / `firstOrNull<Map<String, Any?>>()` / `first<Map<String, Any?>?>()` |
| `queryOne<T>()` | `first<T>()` |
| `queryOneOrNull<T>()` | `firstOrNull<T>()` / `first<T?>()` |
| `PagedClause.query()` | `PagedClause.toMapList()` |
| `PagedClause.queryList()` | `PagedClause.toList()` |

`toMapList()`, `toMap()`, and `toMapOrNull()` are convenience methods for the corresponding generic forms; in general, `firstOrNull<T>()` is also equivalent to `first<T?>()`. Map query results now use `Map<String, Any?>`, preserving selected SQL `NULL` values. `QueryType` is reduced to `ToMapList`, `ToList`, `ToMap`, and `First`; projects that inspect query events must update their enum branches. Raw SQL extensions now come from `com.kotlinorm.database.SqlExecutor`, and the former `SqlHandler` object has been removed.

The built-in JDBC wrapper is now consistently exposed as `KronosJdbcWrapper`. Projects upgrading from older releases that still use the legacy `KronosBasicWrapper` must update both the class and package name:

```kotlin
// Legacy API
import com.kotlinorm.KronosBasicWrapper
val wrapper = KronosBasicWrapper(dataSource)

// 0.2.0
import com.kotlinorm.wrappers.KronosJdbcWrapper
val wrapper = KronosJdbcWrapper(dataSource)
```

Custom `KronosDataSourceWrapper` implementations must replace `forList`, `forMap`, and `forObject` with the following two query entry points. Read the result mapping target from `task.targetType`:

```kotlin
override fun toList(task: KAtomicQueryTask): List<Any?>
override fun first(task: KAtomicQueryTask): Any?
```

Query tasks and the conversion pipeline now carry complete Kotlin `KType` metadata:

- `KAtomicQueryTask` requires `targetType: KType`; direct `KronosAtomicQueryTask` construction must provide `typeOf<T>()`.
- `KronosSerializeProcessor` now defines `serialize(obj, kType)` and `deserialize(serializedStr, kType)`. The untyped serializer and the `KClass` deserializer have been removed.
- `ValueTransformer`, `TransformerManager.getValueTransformed`, `getTypeSafeValue`, `getSafeValue`, and `TransformerSafeValue` now accept `KType`. String type names and `superTypes` have been removed, and the runtime value class parameter is consistently named `sourceValueClass`.
- The `Field` constructor replaces `cascadeIsCollectionOrArray`, `kClass`, and `superTypes` parameters with `kType`; `kClass`, `elementKType`, and `cascadeIsCollectionOrArray` are now derived lazily from the declaration type.

#### Features and fixes

- ✨ Preserve complete Kotlin `KType` metadata across fields, query tasks, serializers, and value transformers, including nested generic collections such as `List<List<String>>` ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 Fix Kotlinx Serialization deserialization for generic collection and data-class fields, and preserve selected `null` values in map and scalar query results ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 Fix generated projection types for `select { it }`, `select { [it] }`, KPojo-minus projections, and mixed full-row plus alias projections using `[]` ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 🐛 Fix SQLite UNION rendering, SQLite schema synchronization, default string values, and other ORM edge cases found by expanded compiler, core, and integration tests ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))
- 💪 Expand projection, serialization, nullable-result, schema-sync, and cross-database regression coverage; update the README, user docs, AI skill guide, Gradle plugin, and IDEA plugin for `0.2.0` ([#232](https://github.com/Kronos-orm/Kronos-orm/pull/232))

### 0.1.2

- 🐛 Fix `syncTable()` schema diffing for custom string primary keys so database metadata primary keys and KPojo `@PrimaryKey(custom = true)` definitions are treated as the same database primary-key mode ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 🐛 Fix SQLite repeated schema sync after `ADD COLUMN` by ignoring column-order-only diffs that SQLite cannot apply with `ALTER COLUMN` ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 💪 Add cross-database integration regression coverage for create V1 table, sync V2 with a new `age` column, verify primary-key metadata, and run a second idempotent sync across MySQL, PostgreSQL, SQLite, SQL Server, and Oracle ([#229](https://github.com/Kronos-orm/Kronos-orm/pull/229))
- 💪 Simplify AI skill installation docs and keep Claude, Codex, Cursor, and generic agent installation paths on the `main` branch ([#228](https://github.com/Kronos-orm/Kronos-orm/pull/228))
- 🔧 Fix docs deployment on newer JDKs and bootstrap Java for the docs deployment workflow ([#226](https://github.com/Kronos-orm/Kronos-orm/pull/226), [#227](https://github.com/Kronos-orm/Kronos-orm/pull/227))

### 0.1.1

- ✨ Add projection and subquery DSL coverage for generated result-row classes, scalar subqueries, predicate subqueries, INSERT SELECT, and window-function aliases ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- ✨ Add user-facing IDEA plugin documentation for generated KPojo shapes, projection receivers, editor diagnostics, and plugin installation from GitHub Release artifacts ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 💪 Reorganize user documentation around getting started, mapping, query, mutation, database, configuration, advanced topics, and resources; refresh examples for table mapping, functions, projection, dialect behavior, and codegen workflows ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 💪 Improve release automation so formal releases publish Maven artifacts, build JVM jars and the IDEA plugin zip, attach them to a GitHub Release, and generate release notes automatically ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 🐛 Fix compiler-plugin type-parameter handling for `SelectFrom10` through `SelectFrom16` query functions and add official compiler tests for query return types and projection behavior ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))
- 🔧 Use the official Gradle distribution URL for all wrappers to avoid CI failures caused by mirror download timeouts ([#222](https://github.com/Kronos-orm/Kronos-orm/pull/222))

### 0.1.0

- ✨ Release `0.1.0` with core module, build configuration, and README version reference updates ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204), [#207](https://github.com/Kronos-orm/Kronos-orm/pull/207), [#208](https://github.com/Kronos-orm/Kronos-orm/pull/208))
- ✨ Add the K2 compiler plugin implementation, replace the old compiler plugin implementation, and publish the K2 plugin as the only main compiler plugin ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- ✨ Add type-safe SQL AST and dialect renderers covering select, insert, update, delete, upsert, DDL, union, and function rendering ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- ✨ Add transaction DSL support for isolation levels, timeout, savepoints, and nested transaction connection reuse; add ThreadLocal transaction connection sharing to the JDBC wrapper ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 💪 Improve cascade select, cross-module test infrastructure, codegen tests, core ORM/dialect function tests, and coverage CI ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 💪 Redesign the docs homepage and documentation UI with theme sync, collapsible sidebar, copy-as-markdown, footer, AI docs chapter, and module READMEs ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 🐛 Fix upsert `criteriaParams` injection, `.set {}` assignment, logic-delete existence checks, PostgreSQL `ANY(ARRAY[...])` type mismatches, and PostgreSQL concurrent index creation inside transactions ([#204](https://github.com/Kronos-orm/Kronos-orm/pull/204))
- 🔧 Update `__tableName` property documentation and plugin configuration, and adjust detekt suppressions ([#206](https://github.com/Kronos-orm/Kronos-orm/pull/206))

### 0.0.7

- 🔧 Upgrade Gradle to 9.4.1 and Kotlin to 2.3.0 ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 🐛 Fix PostgreSQL identity primary key to use BIGSERIAL for BIGINT columns ([#201](https://github.com/Kronos-orm/Kronos-orm/pull/201))
- 💪 Refactor table name and comment from functions to properties (`__tableName`, `__tableComment`) ([#198](https://github.com/Kronos-orm/Kronos-orm/pull/198))
- 🔧 Fix bump-version.sh sed syntax for Linux CI environments ([#197](https://github.com/Kronos-orm/Kronos-orm/pull/197))

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

[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v{{ $.kronosSnapshotBadgeVersion() }}-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

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
    id 'com.kotlinorm.kronos-gradle-plugin' version '{{ $.kronosSnapshotVersion() }}'
}

dependencies {
    implementation "com.kotlinorm:kronos-core:{{ $.kronosSnapshotVersion() }}"
    implementation "com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosSnapshotVersion() }}"
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
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosSnapshotVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosSnapshotVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosSnapshotVersion() }}")
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
      <version>{{ $.kronosSnapshotVersion() }}</version>
    </dependency>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-jdbc-wrapper</artifactId>
      <version>{{ $.kronosSnapshotVersion() }}</version>
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
