<p align="center">
    <a href="https://www.kotlinorm.com">
        <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" alt="logo" height="256" width="256">
    </a>
</p>

---------

<h1 align="center">
    Kronos-ORM
</h1>

[English](https://github.com/Kronos-orm/Kronos-orm/blob/main/README.MD) | 简体中文

<h3 align="center">
为 Kotlin 设计的现代 ORM 框架。
</h3>
<div align="center">

Kronos 是基于编译器插件的 Kotlin ORM 框架。零反射、强类型、多数据库支持，适用于服务端和移动端应用。

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

[官网](https://www.kotlinorm.com) | [中文文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start) | [Discord](https://discord.gg/Vn8EzxRX) | [QQ群](https://qm.qq.com/q/V821JzFUcM)
</div>

--------

## 为什么选择 Kronos

- **零反射** — 编译器插件在构建期生成所有映射代码，无运行时开销，无 GC 压力。
- **Kotlin 原生 DSL** — 直接使用 `==`、`>`、`<`、`in`、`like` 编写条件，无需 `.eq()`、`.gt()`、`.lt()`。
- **多数据库** — MySQL、PostgreSQL、SQLite、Oracle、SQL Server、DB2、H2、OceanBase、DM8、GaussDB 等。
- **功能完备** — 事务、级联（1:1 / 1:N / M:N）、表结构同步、序列化、跨库查询。
- **内置策略** — 逻辑删除、乐观锁、创建/更新时间戳，均可自定义。
- **框架无关** — Spring Boot、Ktor、Vert.x、Solon、Android，轻易集成。
- **协程友好** — 从底层为结构化并发设计。
- **编译期映射** — KPojo 与 Map 互转，近乎零开销。

## 贡献者指引

AI 编程助手可参考 `.claude/skills/` 下的技能指南获取架构与工作流上下文。

--------

## 快速示例

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

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 8+ |
| Kotlin | 2.3.0+ |
| Gradle | 6.8.3+ |
| Maven | 3.6.3+ |

> 请确保 IDE 的 Kotlin 插件支持 Kotlin 2.3.0 或更高版本。
>>>>>>> 9037e6de0 (docs: rewrite READMEs, add AI skill guide, and update submodule docs)

--------

## 安装

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

## 支持的数据库

MySQL | PostgreSQL | SQLite | Oracle | SQL Server | DB2 | Sybase | H2 | OceanBase | DM8 | GaussDB

> 核心 DSL 提供跨数据库兼容性，集成测试目前以 MySQL 和 PostgreSQL 为主。

--------

## 使用

### 连接数据库

```kotlin
Kronos.init {
    dataSource = { KronosBasicWrapper(SomeDataSource()) }
}
```

动态数据源和多数据源配置请参阅[连接数据库](https://kotlinorm.com/#/documentation/zh-CN/database/connect-to-db)。

### 模型定义

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

### 表操作

```kotlin
dataSource.table.exists<Movie>()
dataSource.table.createTable<Movie>()
dataSource.table.syncTable<Movie>()
dataSource.table.dropTable<Movie>()
```

### 查询

```kotlin
val listOfUser: List<User> = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// 分页
val (total, list) = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.username like "a%" }
    .page(1, 10)
    .withTotal()
    .queryList()

// 联表查询
val listOfMap = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId && user.id == role.userId }
    select {
        user.id + user.username + relation.id.as_("relationId") +
                role.role + f.count(1).as_("count")
    }
    where { user.id < 10 }
}.query()

// 命名参数原生 SQL
val result = dataSource.query("select * from tb_user where id = :id", mapOf("id" to 1))
```

### 插入

```kotlin
user.insert().execute()
listOfUser.insert().execute()
```

### 更新

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

### 删除

```kotlin
user.delete()
    .where { it.id == 1 }
    .execute()
```

--------

### KPojo 工具方法

```kotlin
val instance = Movie::class.newInstance()

val dataMap: Map<String, Any?> = movie.toDataMap()
val movie2: Movie = dataMap.mapperTo<Movie>()

val tableName = movie.__tableName // 可读写
val columns = movie.kronosColumns()

instance["fieldName"]
instance["fieldName"] = "value"
```

所有操作在编译期完成，无反射。

--------

## 框架集成

| 框架 | 示例 |
|------|------|
| Spring Boot | [kronos-example-spring-boot](https://github.com/Kronos-orm/kronos-example-spring-boot) |
| Ktor | [kronos-example-ktor](https://github.com/Kronos-orm/kronos-example-ktor) |
| Vert.x | [kronos-example-vertx](https://github.com/Kronos-orm/kronos-example-vertx) |
| Solon | [kronos-example-solon](https://github.com/Kronos-orm/kronos-example-solon) |
| Android | [kronos-example-android](https://github.com/Kronos-orm/kronos-example-android) |

## AI 编程助手集成

Kronos 提供 AI 技能指南，帮助编程助手理解和生成 Kronos 代码。一条命令即可安装：

| 工具 | 命令 |
|------|------|
| **Cursor / Windsurf** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .cursor/skills/kronos-orm-guide && rm -rf .cursor/skills/kronos-orm-guide/.git` |
| **Claude Code** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .claude/skills/kronos-orm-guide && rm -rf .claude/skills/kronos-orm-guide/.git` |
| **Codex** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .codex/kronos-orm-guide && rm -rf .codex/kronos-orm-guide/.git` |
| **OpenCode** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .opencode/skills/kronos-orm-guide && rm -rf .opencode/skills/kronos-orm-guide/.git` |
| **Kiro** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .kiro/skills/kronos-orm-guide && rm -rf .kiro/skills/kronos-orm-guide/.git` |
| **通用** | `git clone -b release/llm --depth 1 https://github.com/Kronos-orm/Kronos-orm.git .llm/kronos-orm-guide && rm -rf .llm/kronos-orm-guide/.git` |

--------

## 文档

完整文档请访问[官方网站](https://www.kotlinorm.com)或[文档中心](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start)。

## 许可证

Kronos-ORM 遵循 [Apache 2.0 许可证](https://www.apache.org/licenses/LICENSE-2.0.html)发布。

## 贡献

参阅 [CONTRIBUTING.md](https://github.com/Kronos-orm/Kronos-orm/blob/main/CONTRIBUTING.md)。

## 贡献者

<a href="https://github.com/Kronos-orm/Kronos-orm/graphs/contributors">
  <img alt="contributors" src="https://contrib.rocks/image?repo=Kronos-orm/Kronos-orm"/>
</a>

-------------------

[![discord](https://img.shields.io/badge/Discord-Kronos_ORM-blue?logo=discord&logoColor=white)](https://discord.gg/Vn8EzxRX)
[![QQ群](https://img.shields.io/badge/QQ%E7%BE%A4-991790071-red?logo=qq)](https://qm.qq.com/q/V821JzFUcM)

![QRCode](https://github.com/Kronos-orm/Kronos-orm/blob/main/assets/invitation_qrcode.jpg?raw=true)
