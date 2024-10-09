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

Kronos是一个为Kotlin设计的现代ORM框架，**适用于后端和移动应用，支持多数据库, 高性能**。

KCP实现的表达式树分析支持以及Kotlin本身泛型、高阶函数、扩展函数等语法特性，让Kronos拥有**强大的表现力和简洁、语义化的写法**，**使操作数据库变得更加简单**。

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1839b25a691b49ee93a0ace2a6255cb0)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![build](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml)
[![Maven central](https://img.shields.io/maven-central/v/com.kotoframework/koto-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.kotoframework%22)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.kotoframework/koto-core.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/kotoframework/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[官网](https://www.kotlinorm.com) | [中文文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start)
</div>

--------

# 重新定义 Kotlin ORM

❓ **为什么选择 Kronos?**

* 可以使用jvm平台的**全部生态和资源**，如**数据库驱动和日志框架**等，未来可能支持kotlin多平台
* **Kotlin 编译器插件和协程驱动**，显著**减少反射的使用**，提供**高性能**的数据库操作体验
* 支持大多数**主流数据库**且支持自由地通过插件**添加数据库扩展**
* **书写简洁、富有表现力，支持 Kotlin 原生语法** `==`、`>`、`<`、`in` 等，而不是 .eq、.gt、.lt 等
* 强类型检查
* 支持**事务**、**无外键复杂级联操作（一对一，一对多，多对多）**、**序列化反序列化**、**跨数据库查询**、**数据库表/索引/备注创建和结构同步**等功能
* 支持**逻辑删除**、**乐观锁**、**创建时间**、**更新时间**，且支持灵活的自定义设置
* 轻松与任何第三方框架集成
* **基于命名参数的原生SQL数据库操作**
* 通过编译期操作，支持轻松将**数据实体类转换为Map或从Map转换为数据实体类**，并且**无反射，近乎零开销**
* 数据类就可以当作数据库表模型，**显著减少额外的类定义**

-------

## 🖥 JDK、Kotlin 和构建工具

- **JDK 8+**
- **Kotlin 2.0.0+**
- **Maven 3.6.3+** or **Gradle 6.8.3+**

> 请确保用于 IDE 的 kotlin 插件支持 kotlin 2.0.0 或更高版本。
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
    id("com.kotlinorm.kronos-gradle-plugin") version "2.0.0"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:2.0.0")
}
```

</details>

<summary>

### Gradle(groovy)

</summary>

<details>

```groovy
plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '2.0.0'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:2.0.0'
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
            <version>2.0.0</version>
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
                        <version>2.0.0</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

## 🚀 快速入门

### 🔗 连接数据库

您可以通过以下方式设置默认数据源

```kotlin
Kronos.dataSource = { KronosBasicWrapper(SomeDataSource()) }
// 具体数据源的示例：
//Kronos.dataSource = { KronosBasicWrapper(MysqlDataSource("jdbc:mysql://localhost:3306/test", "root", "***")) }
//Kronos.dataSource = { KronosBasicWrapper(HikariDataSource().apply { jdbcUrl = "jdbc:mysql://localhost:3306/test" ... }) }
//Kronos.dataSource = { KronosBasicWrapper(BasicDataSource().apply { url = "jdbc:sqlite:path/to/db" ... }) }
```

有关连接数据库和使用动态数据源或多个数据源的更多详情，请参阅
文档](https://kotlinorm.com/#/documentation/zh-CN/database/connect-to-db)。

### 🎨 Table Model Definition(Annotation Style)

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null,// 主键和自动递增
    val name: String? = null, // 电影名称
    val directorId: Long? = null, // 导演 ID
    @Cascade(["directorId"], ["id"])
    val director: Director? = null, // 级联关系：一对多
    val relations: List<MovieActorRelation>? = null, // 级联关系：多对多
    @Serializable
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

### 🎨 Table Model Definition(Dsl 风格)

即将推出...

------------

### 📝 将 `KPojo` 转换为 Map 或将 Map 转换为 `KPojo`

将 `KPojo` 转换为 Map 或从 Map 转换为 `KPojo` 都很容易，而且可以通过
kronosTable` 和 `kronosColumns` 方法获取表名和列信息。

```kotlin
val movie = Movie(1)
val dataMap: Map<String, Any?> = movie.toDataMap() // Map("id" = 1)
val movie2: Movie = dataMap.mapperTo<Movie>() // or dataMap.mapperTo(Movie::class)
val tableName = movie.kronosTable() // "tb_movie", 没有使用反射
val columns = movie.kronosColumns() // [Field(id), Field(name), ...], 没有使用反射
```

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
        user.id + user.username + relation.id.`as`("relationId") +
                role.role + "count(1) as count"
    }
    where { user.id < 10 }
}.query()
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
    .onConfict() // 我们实现了不同数据库的兼容
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

## 🛠️ 与 Spring 或其他框架集成

有关更多信息，请参阅以下示例项目：

[SpringBoot 示例项目](https://github.com/Kronos-orm/kronos-spring-demo)

## 📚 官方文档

如需了解更多信息，请访问 [官方网站](https://www.kotlinorm.com) 或
文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start)。

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

**如果您喜欢 Kronos-ORM，请给我们一个Star ⭐️，谢谢！**
