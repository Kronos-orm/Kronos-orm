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

[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-%237f52ff.svg?logo=kotlin)](http://kotlinlang.org)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/Heapy/awesome-kotlin)
[![License](https://img.shields.io/:license-apache_2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/eb64ca7cb21949c4a469c8a4bc095938)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-compiler-plugin.svg)](https://coverage.kotlinorm.com/kronos-compiler-plugin)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-core.svg)](https://coverage.kotlinorm.com/kronos-core)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-codegen.svg)](https://coverage.kotlinorm.com/kronos-codegen)

[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/detekt.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/detekt.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml)

[![Maven Central](https://img.shields.io/maven-central/v/com.kotlinorm/kronos-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.kotlinorm)
[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v0.1.0--SNAPSHOT-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

[Official Website](https://www.kotlinorm.com) | [Documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start) | [Discord](https://discord.gg/Vn8EzxRX) | [QQ群](https://qm.qq.com/q/V821JzFUcM)
</div>

--------

## Why Kronos

- **Zero reflection** — Compiler plugin generates all mapping code at build time. No runtime overhead, no GC pressure.
- **Kotlin-native DSL** — Write conditions with `==`, `>`, `<`, `in`, `like` directly. No `.eq()`, `.gt()`, `.lt()` boilerplate.
- **Multi-database** — MySQL, PostgreSQL, SQLite, Oracle, SQL Server, DB2, H2, OceanBase, DM8, GaussDB and more.
- **Full-featured** — Transactions, cascading (1:1 / 1:N / M:N), schema sync, serialization, cross-database joins.
- **Built-in strategies** — Logical deletion, optimistic lock, create/update timestamps. All configurable.
- **Framework agnostic** — Spring Boot, Ktor, Vert.x, Solon, Android. Plug in anywhere.
- **Coroutine-friendly** — Designed for structured concurrency from the ground up.
- **Compile-time mappers** — KPojo to/from Map conversion with near-zero overhead.

## For Contributors

AI coding assistants can leverage the skill guide under `.claude/skills/` for architecture and workflow context.

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

val listOfUser = User(age = 18).select().queryList()

User(id = id, age = 19).update().by { it.id }.execute()

User(id = id).delete().execute()
```

## Requirements

| Dependency | Version |
|------------|---------|
| JDK | 8+ |
| Kotlin | 2.3.0+ |
| Gradle | 6.8.3+ |
| Maven | 3.6.3+ |

> Ensure your IDE's Kotlin plugin supports Kotlin 2.3.0 or higher.
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)

--------

## Installation

<details>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
plugins {
<<<<<<< HEAD
  id("com.kotlinorm.kronos-gradle-plugin") version "0.0.7"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.7")
=======
    id("com.kotlinorm.kronos-gradle-plugin") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.1.0-SNAPSHOT")
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)
}
```

</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
plugins {
<<<<<<< HEAD
  id 'com.kotlinorm.kronos-gradle-plugin' version '0.0.7'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:0.0.7'
=======
    id 'com.kotlinorm.kronos-gradle-plugin' version '0.1.0-SNAPSHOT'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:0.1.0-SNAPSHOT'
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)
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
<<<<<<< HEAD
            <version>0.0.7</version>
=======
            <version>0.1.0-SNAPSHOT</version>
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)
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
<<<<<<< HEAD
                        <version>0.0.7</version>
=======
                        <version>0.1.0-SNAPSHOT</version>
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

## Supported Databases

MySQL | PostgreSQL | SQLite | Oracle | SQL Server | DB2 | Sybase | H2 | OceanBase | DM8 | GaussDB

> The core DSL provides cross-database compatibility. Integration tests currently focus on MySQL and PostgreSQL.

--------

## Usage

### Connect to Database

```kotlin
Kronos.init {
    dataSource = { KronosBasicWrapper(SomeDataSource()) }
}
```

See [Connect to Database](https://kotlinorm.com/#/documentation/en/database/connect-to-db) for dynamic / multiple data sources.

### Model Definition

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null,
    @Necessary val name: String? = null,
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
    .select { it.id + it.username }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// Pagination
val (total, list) = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.username like "a%" }
    .page(1, 10)
    .withTotal()
    .queryList()

// Join
val listOfMap = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId && user.id == role.userId }
    select {
        user.id + user.username + relation.id.as_("relationId") +
                role.role + f.count(1).as_("count")
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

user.update { it.username + it.gender }
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
| **Cursor / Windsurf** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| **Claude Code** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .claude/skills/kronos-orm-guide && rm -rf .claude/skills/kronos-orm-guide/.git` |
| **Codex** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .codex/kronos-orm-guide && rm -rf .codex/kronos-orm-guide/.git` |
| **OpenCode** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .opencode/skills/kronos-orm-guide && rm -rf .opencode/skills/kronos-orm-guide/.git` |
| **Kiro** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .kiro/skills/kronos-orm-guide && rm -rf .kiro/skills/kronos-orm-guide/.git` |
| **Generic** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .llm/kronos-orm-guide && rm -rf .llm/kronos-orm-guide/.git` |

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
