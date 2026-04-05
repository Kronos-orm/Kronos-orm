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
为Kotlin设计的现代ORM框架。
</h3>
<div align="center">

Kronos 是为 Kotlin 设计的现代 ORM 框架，基于编译器插件开发，**适用于后台和移动应用程序，支持多数据库。功能强大、高性能、且简单易用**。

基于KCP实现的表达式树解析和协程，Kronos拥有**强大的性能、表现力和简洁、语义化的写法**，**使操作数据库变得更加简单**。

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
[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v0.0.8-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

[官网](https://www.kotlinorm.com) | [中文文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start) | [Discord](https://discord.gg/Vn8EzxRX) | [QQ群](https://qm.qq.com/q/V821JzFUcM)
</div>

--------

# 重新定义 Kotlin ORM

❓ 为什么选择 Kronos？

- 充分利用 JVM 生态（驱动、日志、连接池等），并逐步面向和支持 Kotlin 多平台。
- 编译器插件驱动，协程友好，核心路径零反射，性能强大且稳定。
- Kotlin 原生语法的表达式 DSL：`==`、`>`、`<`、`in` 等（而非 `.eq`、`.gt`、`.lt`），强类型约束。
- 开箱即用：事务、跨库查询、表/索引/备注创建与同步、序列化/反序列化、无外键的强力级联（一对一/一对多/多对多）。
- 内置：逻辑删除、乐观锁、创建/更新时间，且均可自定义。
- 轻松集成任意技术栈（Spring、Ktor、Vert.x、Solon、Android），下文提供示例仓库。
- **基于命名参数的原生SQL数据库操作**。
- 编译期 Map 映射与元信息 API，KPojo 转换零反射、近乎零开销。

## 开发者文档

- 更完整的架构图、模块内部与高阶指南请见[开发者文档入口](./develop-docs/README-zh_CN.md)。
- 适合阅读的人群：
  - 贡献者 / 维护者：希望扩展或修改 Kronos。
  - 高阶使用者：需要理解内部机制与设计取舍。
  - 与三方集成 / 插件作者：编写日志、JDBC、代码生成、DSL、函数/方言、事件拦截等扩展，或将 Kronos 集成到你的平台中。

-------

# 简单示例

下面是一些最简单的示例，说明如何使用 Kronos-ORM 进行数据库操作。

```kotlin
// 定义一个扩展 KPojo 的数据类
data class User(
    @PrimaryKey(identity = true) val id: Long? = null,
    val username: String? = null,
    val age: Int? = null
) : KPojo

//我们可以使用dataSource.table.createTable<User>()来创建表，
//或dataSource.table.syncTable<User>()来同步表结构。
dataSource.table.createTable<User>()

// 向`user`表插入一条记录(username = "test", age = 18), 并获取插入的记录的id
// 可以使用 List<User>.insert().execute() 来批量插入记录
val id = User(username = "test", age = 18).insert().execute().lastInsertId

// 查询`user`表中`age`为18的记录
val listOfUser = User(age = 18).select().queryList()

// 更新`user`表中`id`为id的记录的`age`字段为19
User(id = id, age = 19).update().by { it.id }.execute()

// 删除`user`表中`id`为id的记录
User(id = id).delete().execute()
```

## 🖥 JDK、Kotlin 和构建工具

- **JDK 8+**
- **Kotlin 2.3.0+**
- **Maven 3.6.3+** or **Gradle 6.8.3+**

> 请确保用于 IDE 的 kotlin 插件支持 kotlin 2.3.0 或更高版本。
>
> 如果您在 **Intellij IDEA** 中构建失败，并使用 **Maven** 构建、
> 请尝试启用以下设置: **Settings / Build, Execution, Deployment / Build Tools / Maven / Runner /
Delegate IDE build/run actions to Maven**.
-------

## 📦 安装

<summary>

### Gradle(kts)

</summary>

<details>

```kotlin
plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.0.7"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.7")
}
```

</details>

<summary>

### Gradle(groovy)

</summary>

<details>

```groovy
plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '0.0.7'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:0.0.7'
}
```

</details>

<summary>

### Maven

</summary>

<details>

```xml

<project>
    <dependencies>
        <dependency>
            <groupId>com.kotlinorm</groupId>
            <artifactId>kronos-core</artifactId>
            <version>0.0.7</version>
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
                        <version>0.0.7</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

## 🗄️ 支持的数据库

Kronos-ORM 支持多种数据库：

- MySQL（MySQL）
- PostgreSQL（PostgreSQL）
- SQLite（SQLite）
- Oracle（甲骨文数据库）
- Microsoft SQL Server（SQL Server）
- DB2（IBM DB2）
- Sybase（Sybase）
- H2（H2 Database）
- OceanBase（OceanBase）
- DM8（达梦数据库）
- GaussDB（华为 GaussDB）

> 说明：
> - 不同方言的特性覆盖可能略有差异；核心 DSL、UPSERT/锁等语法尽量提供跨库兼容能力。
> - 集成测试模块目前主要以 MySQL、PostgreSQL 为示例以便 CI 运行。

## 🚀 快速入门

### 🔗 连接数据库

您可以通过以下方式设置默认数据源

```kotlin
Kronos.init {
    dataSource = { KronosBasicWrapper(SomeDataSource()) }
    // 具体数据源的示例：
    //dataSource = { KronosBasicWrapper(MysqlDataSource("jdbc:mysql://localhost:3306/test", "root", "***")) }
    //dataSource = { KronosBasicWrapper(HikariDataSource().apply { jdbcUrl = "jdbc:mysql://localhost:3306/test" ... }) }
    //dataSource = { KronosBasicWrapper(BasicDataSource().apply { url = "jdbc:sqlite:path/to/db" ... }) }
}
```

有关连接数据库和使用动态数据源或多个数据源的更多详情，请参阅
文档](https://kotlinorm.com/#/documentation/zh-CN/database/connect-to-db)。

### 🎨 表模型定义（通过注解）

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null,// 主键和自动递增
    @Necessary val name: String? = null, // 电影名称
    val directorId: Long? = null, // 导演 ID
    @Cascade(["directorId"], ["id"])
    val director: Director? = null, // 级联关系：一对多
    val relations: List<MovieActorRelation>? = null, // 级联关系：多对多
    @Serialize
    val type: List<String>? = null, // 从字符串反序列化
    @Column("movie_summary")
    val summary: String? = null, // 电影摘要，列别名
    @Version val version: Long? = null, // 乐观锁的版本
    @LogicDelete val deleted: Boolean? = null, // 逻辑删除
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @UpdateTime val updateTime: String? = null, // 更新时间，日期格式化
    @CreateTime val createTime: LocalDateTime? = null // 创建时间
) : KPojo { // KPojo 是一个标记接口，用于标记实体类
    var actors: List<Actor>? by manyToMany(::relations) // 使用 Kotlin 委托简化多对多关系的操作
}
```

### 🎨 表模型定义(Dsl 风格)

即将推出...[#10](https://github.com/Kronos-orm/Kronos-orm/issues/10)

------

### 🔨 Table Operation

```kotlin
// 表是否存在
dataSource.table.exists<Movie>()

// 创建表
dataSource.table.createTable<Movie>()

// 删除表
dataSource.table.dropTable<Movie>()
// 或者
dataSource.table.dropTable("tb_movie")

// 同步表结构
dataSource.table.syncTable<Movie>()
```

------

### 📜 Query

```kotlin
// 一个简单的查询，包括指定字段、条件、排序、分组、去重
val listOfUser: List<User> = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// 带有分页和总数查询
val (total, list) = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.username like "a%" }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
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

// 使用原生 SQL + 命名参数 查询
val result: Map<String, Any> = dataSource.query("select * from tb_user where id = :id", mapOf("id" to 1))
```

### ➕ Insert

```kotlin
// 插入单个实体
user.insert().execute()

// 插入多个实体
listOfUser.insert().execute()
```

### ✏️ Update

```kotlin
// 使用set函数更新
user.update()
    .set {
        it.username = "123"
        it.score += 10
    }
    .by { it.id }
    .execute()

// 根据某些条件更新，更新的数据来自传入的记录
user.update { it.username + it.gender }
    .by { it.id }
    .execute()
```

### 🔄 Upsert

```kotlin
// 在某些列上执行插入或更新
user.upsert { it.username }
    .on { it.id }
//  .lock(PessimisticLock.X) // 您可以指定锁的类型，默认使用悲观锁
    .execute()

// 只有在冲突时才更新
user.upsert { it.username }
    .onConflict() // 跨数据库的 UPSERT 兼容写法
    .execute()
```

### 🗑 Delete

```kotlin
// 根据某些条件删除行
user.delete()
    .where { it.id == 1 }
    .execute()
```

------

### 📝 动态创建`KPojo` & `KPojo` 与 Map互转 & 获取表名、列等信息

- Kronos通过编译期操作，为`KPojo`提供了根据类型参数动态创建实例的能力，这样就可以轻松地创建实例了。
- Kronos提供函数轻松用于`KPojo`与`Map`之间的互相转换。
- Kronos为继承了`KPojo`接口的类提供了包括 `kronosTable` 和 `kronosColumns` 等许多函数，用于获取表名和列等元信息。

```kotlin
val instance = Movie::class.newInstance() // -> Movie() ,通过引用动态创建 KPojo 实例，不使用反射
fun <T: KPojo> dynamic(kClass: KClass<T>): T = kClass.newInstance() // 通过引用动态创建 KPojo 实例，不使用反射

val movie = Movie(1)
val dataMap: Map<String, Any?> = movie.toDataMap() // Map("id" = 1), 不使用反射
val movie2: Movie = dataMap.mapperTo<Movie>() // 或 dataMap.mapperTo(Movie::class), 不使用反射

val tableName = movie.kronosTable() // "tb_movie", 不使用反射
val columns = movie.kronosColumns() // [Field(id), Field(name), ...], 不使用反射
// 实际上，我们提供了更多的功能，如获取表的创建时间、更新时间、字段设置的逻辑删除等。

instance["fieldName"] // 通过字段名获取字段值, 不使用反射
instance["fieldName"] = "value" // 通过字段名设置字段值, 不使用反射
```

------

## 🛠️ 与 Spring 或其他框架集成

有关更多信息，请参阅以下示例项目：

- [SpringBoot 示例项目](https://github.com/Kronos-orm/kronos-example-spring-boot)
- [Ktor 示例项目](https://github.com/Kronos-orm/kronos-example-ktor)
- [Vert.x 示例项目](https://github.com/Kronos-orm/kronos-example-vertx)
- [Solon 示例项目](https://github.com/Kronos-orm/kronos-example-solon)
- [Android 示例项目](https://github.com/Kronos-orm/kronos-example-android)

## 📚 官方文档

如需了解更多信息，请访问 [官方网站](https://www.kotlinorm.com) 或 
[文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start)。

## 📜 License

Kronos-ORM 遵循 Apache 2.0 许可发布。

## 🤝 Contributing

更多信息请参阅 [CONTRIBUTING.md](https://github.com/Kronos-orm/Kronos-orm/blob/main/CONTRIBUTING.md)。

## Contributors

我们对所有已经为 Kronos-ORM 做出贡献的个人表示感谢！

<a href="https://github.com/Kronos-orm/Kronos-orm/graphs/contributors">
  <img alt="contributors" src="https://contrib.rocks/image?repo=Kronos-orm/Kronos-orm"/>
</a>

-------------------

如果你对 Kronos-ORM 感兴趣，欢迎加入我们的社区！

[![discord](https://img.shields.io/badge/Discord-Kronos_ORM-blue?logo=discord&logoColor=white)](https://discord.gg/Vn8EzxRX)

[![QQ群](https://img.shields.io/badge/QQ%E7%BE%A4-991790071-red?logo=qq)](https://qm.qq.com/q/V821JzFUcM)

![QRCode](https://github.com/Kronos-orm/Kronos-orm/blob/main/assets/invitation_qrcode.jpg?raw=true)

**如果您喜欢 Kronos-ORM，请给我们一个Star ⭐️，谢谢！**
