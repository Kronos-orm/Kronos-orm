<p align="center">
    <a href="https://www.kotlinorm.com">
        <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" alt="logo" height="256" width="256">
    </a>
</p>

---------

<h1 align="center">
    Kronos-ORM
</h1>

English | [简体中文](https://github.com/Kronos-orm/Kronos-orm/blob/main/README-zh_CN.MD)

<h3 align="center">
A modern ORM framework designed for Kotlin.
</h3>
<div align="center">

Kronos is a compiler-plugin-powered ORM framework for Kotlin. Zero reflection, strong typing, multi-database support. Built for both server-side and mobile applications.

[![Kotlin](https://img.shields.io/badge/kotlin-2.4.0-%237f52ff.svg?logo=kotlin)](http://kotlinlang.org)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/Heapy/awesome-kotlin)
[![License](https://img.shields.io/:license-apache_2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/eb64ca7cb21949c4a469c8a4bc095938)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-compiler-plugin.svg)](https://coverage.kotlinorm.com/kronos-compiler-plugin)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-core.svg)](https://coverage.kotlinorm.com/kronos-core)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-syntax.svg)](https://coverage.kotlinorm.com/kronos-syntax)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-codegen.svg)](https://coverage.kotlinorm.com/kronos-codegen)

[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/detekt.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/detekt.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-syntax-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-syntax-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml)

[![Maven Central](https://img.shields.io/maven-central/v/com.kotlinorm/kronos-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.kotlinorm)
[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v0.1.1--SNAPSHOT-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

[Official Website](https://www.kotlinorm.com) | [Documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start) | [Discord](https://discord.gg/Vn8EzxRX) | [QQ群](https://qm.qq.com/q/V821JzFUcM)
</div>

--------

## Why Kronos

- **Zero reflection** — Compiler plugin generates all mapping code at build time. No runtime overhead, no GC pressure.
- **Kotlin-native DSL** — Write conditions with `==`, `>`, `<`, `in`, `like` directly. No `.eq()`, `.gt()`, `.lt()` boilerplate.
- **Multi-database** — Built-in dialects for MySQL, PostgreSQL, SQLite, SQL Server and Oracle.
- **Full-featured** — Transactions, cascading (1:1 / 1:N / M:N), schema sync, serialization, cross-database joins.
- **Built-in strategies** — Logical deletion, optimistic lock, create/update timestamps. All configurable.
- **Framework agnostic** — Spring Boot, Ktor, Vert.x, Solon, Android. Plug in anywhere.
- **Coroutine-friendly** — Designed for structured concurrency from the ground up.
- **Compile-time mappers** — KPojo to/from Map conversion with near-zero overhead.

## For Contributors

AI coding assistants can leverage the skill guide under `.agents/skills/` for architecture and workflow context.

--------

## Quick Example

```kotlin
data class User(
    @PrimaryKey(identity = true) val id: Long? = null,
    val username: String? = null,
    val age: Int? = null
) : KPojo

dataSource.table.createTable<User>()

val id = User(username = "test", age = 18).insert().execute().lastInsertId

val listOfUser = User().select().where { it.age == 18 }.queryList()

User(id = id, age = 19).update().by { it.id }.execute()

User(id = id).delete().by { it.id }.execute()
```

## Projection and Subquery Example

Kronos adds compiler-generated projection types to Kotlin queries, so selected aliases can be consumed by the next query layer.

```kotlin
val rankedOrders = Order()
    .select {
        [
            it.id,
            it.userId,
            it.amount,
            f.rowNumber()
                .over {
                    partitionBy(it.userId)
                    orderBy(it.createTime.desc())
                }
                .alias("rn")
        ]
    }

val latestOrders = rankedOrders
    .select { [it.id, it.userId, it.amount] }
    .where { it.rn == 1 }
    .queryList()

Order()
    .select { [it.userId, f.count(it.id).alias("orderCount")] }
    .groupBy { it.userId }
    .insert<UserOrderSummary> { [it.userId, it.orderCount] }
    .execute()

val activeUsers = User()
    .select { [it.id, it.name, it.status] }
    .where { it.status == ACTIVE }

dataSource.table.createTable(UserArchive(), activeUsers)

val currentUsers = User().select { [it.id, it.name] }.where { it.enabled == true }
val archivedUsers = ArchivedUser().select { [it.id, it.name] }

union(currentUsers, archivedUsers)
    .select { [it.id, it.name] }
    .queryList()
```

## Requirements

| Dependency | Version |
|------------|---------|
| JDK | 8+ |
| Kotlin | 2.4.0+ |
| Gradle | A version supported by Kotlin 2.4.0 |
| Maven | 3.9+ |

> Ensure your IDE's Kotlin plugin supports Kotlin 2.4.0 or higher.

## Kotlin Compatibility

| Kronos version | Supported Kotlin version | Notes |
|----------------|--------------------------|-------|
| `0.1.1` | `2.4.0+` | Recommended user documentation version |
| `0.1.0` | `2.3.0+` | Stable release line based on Kotlin 2.3.0 |
| `0.0.7` | `2.3.0+` | Upgraded Gradle and Kotlin in the 0.0.7 release |
| `0.0.6` | `2.2.21+` | Added `kotlin.time.Instant` support |
| `0.0.2` - `0.0.5` | `2.2.0+` | Early development releases before the 2.2.21 upgrade |
| `0.0.1` | `2.1.0+` | Initial public release line |

--------

## Installation

<details>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.1.1"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.1.1")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.1.1")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '0.1.1'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:0.1.1'
    implementation 'com.kotlinorm:kronos-jdbc-wrapper:0.1.1'
    implementation 'com.mysql:mysql-connector-j:<latest-stable>'
}
```

</details>

<details>
<summary><b>Maven</b></summary>

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>0.1.1</version>
        </dependency>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-jdbc-wrapper</artifactId>
            <version>0.1.1</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql-connector-j.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
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
                        <version>0.1.1</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

## Supported Databases

MySQL | PostgreSQL | SQLite | SQL Server | Oracle

> Kronos selects SQL rendering and table-structure statements from the active database dialect.

--------

## Usage

### Connect to Database

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper

Kronos.dataSource = { KronosJdbcWrapper(SomeDataSource()) }
```

`KronosJdbcWrapper` can also receive `databaseType = DBType.Mysql` and a configuration block for JDBC statement settings, warning handling, parameter binding, and result mapping. See [Connect to Database](https://kotlinorm.com/#/documentation/en/database/connect-to-db) for dynamic / multiple data sources.

### Model Definition

```kotlin
@Table("tb_movie")
@TableIndex("idx_name", ["name"], "UNIQUE", "BTREE")
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null,
    @NonNull val name: String? = null,
    val directorId: Long? = null,
    @Cascade(["directorId"], ["id"])
    val director: Director? = null,
    val relations: List<MovieActorRelation>? = null,
    @Serialize
    val type: List<String>? = null,
    @Column("movie_summary")
    val summary: String? = null,
    @Version val version: Long? = null,
    @LogicDelete val deleted: Boolean? = null,
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @UpdateTime val updateTime: String? = null,
    @CreateTime val createTime: LocalDateTime? = null
) : KPojo {
    var actors: List<Actor>? by manyToMany(::relations)
}
```

### Table Operations

```kotlin
dataSource.table.exists<Movie>()
dataSource.table.createTable<Movie>()
dataSource.table.syncTable<Movie>()
dataSource.table.dropTable<Movie>()
```

### Query

```kotlin
val listOfUser: List<User> = User()
    .select { [it.id, it.username] }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// Pagination
val (total, list) = User()
    .select { [it.id, it.username] }
    .where { it.id < 10 && it.username like "a%" }
    .page(1, 10)
    .withTotal()
    .queryList()

// Join
val listOfMap = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId && user.id == role.userId }
    select {
        [user.id, user.username, relation.id.alias("relationId"),
                role.role, f.count(1).alias("count")]
    }
    where { user.id < 10 }
}.query()

// Native SQL with named parameters
val result = dataSource.query("select * from tb_user where id = :id", mapOf("id" to 1))
```

### Insert

```kotlin
user.insert().execute()
listOfUser.insert().execute()
```

### Update

```kotlin
user.update()
    .set {
        it.username = "123"
        it.score += 10
    }
    .by { it.id }
    .execute()

user.update { [it.username, it.gender] }
    .by { it.id }
    .execute()
```

### Upsert

```kotlin
user.upsert { it.username }
    .on { it.id }
    .execute()

user.upsert { it.username }
    .onConflict()
    .execute()
```

### Delete

```kotlin
user.delete()
    .where { it.id == 1 }
    .execute()
```

--------

### KPojo Utilities

```kotlin
val instance = Movie::class.newInstance()

val dataMap: Map<String, Any?> = movie.toDataMap()
val movie2: Movie = dataMap.mapperTo<Movie>()

val tableName = movie.__tableName // readable and writable
val columns = movie.kronosColumns()

instance["fieldName"]
instance["fieldName"] = "value"
```

All operations are resolved at compile time. No reflection.

--------

## Framework Integration

| Framework | Example |
|-----------|---------|
| Spring Boot | [kronos-example-spring-boot](https://github.com/Kronos-orm/kronos-example-spring-boot) |
| Ktor | [kronos-example-ktor](https://github.com/Kronos-orm/kronos-example-ktor) |
| Vert.x | [kronos-example-vertx](https://github.com/Kronos-orm/kronos-example-vertx) |
| Solon | [kronos-example-solon](https://github.com/Kronos-orm/kronos-example-solon) |
| Android | [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android) |

## AI Coding Assistant Integration

Kronos provides an AI skill guide that helps coding assistants understand and generate Kronos code. Install it with a single command:

| Tool | Command |
|------|---------|
| **Cursor** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| **Default / Generic** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .agents/skills/kronos-orm-guide && rm -rf .agents/skills/kronos-orm-guide/.git` |

Use the default command for Windsurf and other tools that read agent skills from `.agents/skills/`.

--------

## Documentation

For full documentation, visit the [official website](https://www.kotlinorm.com) or the [documentation portal](https://kotlinorm.com/#/documentation/en/getting-started/quick-start).

## License

Kronos-ORM is released under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.html).

## Contributing

See [CONTRIBUTING.md](https://github.com/Kronos-orm/Kronos-orm/blob/main/CONTRIBUTING.md).

## Contributors

<a href="https://github.com/Kronos-orm/Kronos-orm/graphs/contributors">
  <img alt="contributors" src="https://contrib.rocks/image?repo=Kronos-orm/Kronos-orm"/>
</a>

-------------------

[![discord](https://img.shields.io/badge/Discord-Kronos_ORM-blue?logo=discord&logoColor=white)](https://discord.gg/Vn8EzxRX)
[![QQ群](https://img.shields.io/badge/QQ%E7%BE%A4-991790071-red?logo=qq)](https://qm.qq.com/q/V821JzFUcM)

![QRCode](https://github.com/Kronos-orm/Kronos-orm/blob/main/assets/invitation_qrcode.jpg?raw=true)
