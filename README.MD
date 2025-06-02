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

Kronos is a modern ORM framework designed for Kotlin based on the compiler plugin, which is **suitable for both backend and mobile applications, support multi-database. Powerful, high performance, easy to use**.

Based on the KCP implementation of expression tree parsing and concatenation, Kronos has **powerful performance, expressiveness, and a concise, semantic writing style**, make the operation of the database has become **more simple**.

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-%237f52ff.svg?logo=kotlin)](http://kotlinlang.org)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/Heapy/awesome-kotlin)
[![License](https://img.shields.io/:license-apache_2.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/eb64ca7cb21949c4a469c8a4bc095938)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-compiler-plugin.svg)](https://coverage.kotlinorm.com/kronos-compiler-plugin)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-core.svg)](https://coverage.kotlinorm.com/kronos-core)
[![Coverage](https://github.com/Kronos-orm/Kronos-orm/raw/coverage/coverage-codegen.svg)](https://coverage.kotlinorm.com/kronos-codegen)

[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/detekt.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-core-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-compiler-plugin-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-codegen-testing.yml)
[![CI](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/kronos-testing.yml)

[![Maven Central](https://img.shields.io/maven-central/v/com.kotlinorm/kronos-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.kotlinorm)
[![Maven Central Snapshots](https://img.shields.io/badge/Maven%20Central%20Snapshots-v0.0.4--SNAPSHOT-blue?link=https%3A%2F%2Fcentral.sonatype.com%2Fservice%2Frest%2Frepository%2Fbrowse%2Fmaven-snapshots%2Fcom%2Fkotlinorm%2F)](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/kotlinorm/)

[Official Website](https://www.kotlinorm.com) | [Documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start) | [Discord](https://discord.gg/Vn8EzxRX) | [QQ群](https://qm.qq.com/q/V821JzFUcM)
</div>

--------
# Re-define KotlinORM

❓ **Why Kronos?**

* Utilizes the **entire ecosystem and resources** of the JVM platform, such as **database drivers and logging frameworks**, with potential future support for Kotlin Multiplatform.
* **Powered by Kotlin compiler plugins and coroutines**, with **NO reflection** used, Kronos delivers unmatched **high-performance** database operations.
* Supports most **mainstream databases** and allows **freely adding database extensions through plugins**.
* **Concise and expressive writing, supporting Kotlin native syntax** `==`, `>`, `<`, `in`, etc., instead of .eq, .gt, .lt, etc.
* Strong type checking.
* Supports **transactions**, **complex cascading operations without foreign keys (one-to-one, one-to-many, many-to-many)**, **serialization and deserialization**, **cross-database queries**, and **database table/index/remarks creation and structure synchronization**, etc.
* Supports **Logical Deletion**, **Optimistic Lock**, **Creation Time**, **Update Time**, and offers flexible customization settings.
* **Easily integrate with any third-party framework** such as `Spring`, `Ktor`, `Vert.x`, `Solon`, etc. See more information in the sample projects plz.
* **Native SQL database manipulation based on named parameters**.
* Supports easy conversion of **data entity classes to Map or from Map to data entity classes** via compile-time operations with **NO reflection, near-zero overhead**.
* Data classes can be treated as database table models, **significantly reducing additional class definitions**.

-------

# Simple Example

Here are some of the simplest examples showing how to use Kronos-ORM for database operations.

```kotlin
// Define a data class extending KPojo
data class User(
    @PrimaryKey(identity = true) val id: Long? = null,
    val username: String? = null,
    val age: Int? = null
) : KPojo

// We can use dataSource.table.createTable<User>() to create the table,
// or dataSource.table.syncTable<User>() to synchronize the table structure.
dataSource.table.createTable<User>()

// Insert a record into table `user` (username = "test", age = 18), and get the id of the inserted record
// we can use List<User>.insert().execute() to batch insert
val id = User(username = "test", age = 18).insert().execute().lastInsertId

// Query records from table `user` where `age` is 18
val listOfUser = User(age = 18).select().queryList()

// Update records in table `user` where `id` is lastInsertId, set `age` to 19
User(id = id, age = 19).update().by { it.id }.execute()

// Delete records from table `user` where `id` is lastInsertId
User(id = id).delete().execute()
```

## 🖥 JDK、Kotlin and Build Tools

- **JDK 8+**
- **Kotlin 2.1.0+**
- **Maven 3.6.3+** or **Gradle 6.8.3+**

> Please make sure your kotlin plugin for ide supports kotlin 2.1.0 or higher.
>
> If you built failed in **Intellij IDEA** and build with **Maven**,
> please try to enable the following setting: **Settings / Build, Execution, Deployment / Build Tools / Maven / Runner / Delegate IDE build/run actions to Maven**.
-------

## 📦  Installation

<summary>

### Gradle(kts)

</summary>

<details>

```kotlin
plugins {
  id("com.kotlinorm.kronos-gradle-plugin") version "0.0.3"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.0.3")
}
```

</details>

<summary>

### Gradle(groovy)

</summary>

<details>

```groovy
plugins {
  id 'com.kotlinorm.kronos-gradle-plugin' version '0.0.3'
}

dependencies {
    implementation 'com.kotlinorm:kronos-core:0.0.3'
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
            <version>0.0.3</version>
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
                        <version>0.0.3</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

## 🚀 Quick Start

### 🔗 Connect to Database

You can set default data source by:

```kotlin
Kronos.init {
    dataSource = { KronosBasicWrapper(SomeDataSource()) }
    // for example:
    //dataSource = { KronosBasicWrapper(MysqlDataSource("jdbc:mysql://localhost:3306/test", "root", "***")) }
    //dataSource = { KronosBasicWrapper(HikariDataSource().apply { jdbcUrl = "jdbc:mysql://localhost:3306/test" ... }) }
    //dataSource = { KronosBasicWrapper(BasicDataSource().apply { url = "jdbc:sqlite:path/to/db" ... }) }
}
```

More details about connecting to the database and use dynamic data source or multiple data sources, please refer to the [docs](https://kotlinorm.com/#/documentation/en/database/connect-to-db).

### 🎨 Table Model Definition(Annotation Style)

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null, // primary key and auto increment
    @Necessary val name: String? = null, // movie name
    val directorId: Long? = null, // director id
    @Cascade(["directorId"], ["id"])
    val director: Director? = null, // cascade table & one-to-many
    val relations: List<MovieActorRelation>? = null, // reference list & many-to-many
    @Serialize
    val type: List<String>? = null, // deserialize from string
    @Column("movie_summary")
    val summary: String? = null, // summary with column name
    @Version val version: Long? = null, // version for optimistic lock
    @LogicDelete val deleted: Boolean? = null, // logic delete
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @UpdateTime val updateTime: String? = null, // update time and date format
    @CreateTime val createTime: LocalDateTime? = null // create time
) : KPojo { // KPojo is a marker interface
    var actors: List<Actor>? by manyToMany(::relations) // many-to-many
}
```

### 🎨 Table Model Definition(Dsl Style)

Coming soon...
[#10](https://github.com/Kronos-orm/Kronos-orm/issues/10)

------
### 🔨 Table Operation

```kotlin
//is table exists
dataSource.table.exists<Movie>()

// create table
dataSource.table.createTable<Movie>()

// drop table
dataSource.table.dropTable<Movie>()
// or
dataSource.table.dropTable("tb_movie")

//sync table structure
dataSource.table.syncTable<Movie>()
```

------
### 📜 Query

```kotlin
// single query
val listOfUser: List<User> = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.age >= 18 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// with Pagination
val (total, list) = User()
    .select { it.id + it.username }
    .where { it.id < 10 && it.username like "a%" }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .page(1, 10)
    .withTotal()
    .queryList()

// multi-table query
val listOfMap = User().join(UserRelation(), UserRole()) { user, relation, role ->
    on { user.id == relation.userId && user.id == role.userId }
    select {
        user.id + user.username + relation.id.as_("relationId") + 
                role.role + f.count(1).as_("count")
    }
    where { user.id < 10 }
}.query()

// Using Native SQL Queries with Named Parameters
val result: Map<String, Any> = dataSource.query("select * from tb_user where id = :id", mapOf("id" to 1))
```

### ➕ Insert

```kotlin
// single insert
user.insert().execute()

// batch insert
listOfUser.insert().execute()
```

### ✏️ Update

```kotlin
// update by some conditions use `set`
user.update()
    .set {
        it.username = "123"
        it.score += 10
    }
    .by { it.id }
    .execute()

// update by some conditions, data from record
user.update { it.username + it.gender }
    .by { it.id }
    .execute()
```

### 🔄 Upsert

```kotlin
// upsert on some columns
user.upsert { it.username }
    .on { it.id }
//  .lock(PessimisticLock.X) // You can specify the type of lock, and pessimistic lock is used by default
    .execute()

// upsert on duplicate key
user.upsert { it.username }
    .onConfict() // We have achieved compatibility with different databases
    .execute()
```

### 🗑 Delete

```kotlin
// delete rows by some conditions
user.delete()
    .where { it.id == 1 }
    .execute()
```

------

### 📝 Create `KPojo` Instance & Transform between `KPojo` and Map & Get Meta Information

- Kronos provides `KPojo` with the ability to dynamically create instances based on type parameters through compile-time operations so that instances can be easily created.
- Kronos provides functions to easily convert between `KPojo` and `Map`.
- Kronos provides classes inheriting the `KPojo` interface with a number of functions including `kronosTable` and `kronosColumns` for obtaining meta information such as table names and columns.

```kotlin
val instance = Movie::class.newInstance() // -> Movie() ,dynamic create KPojo instance by reference, NO REFLECTION used
fun <T: KPojo> dynamic(kClass: KClass<T>): T = kClass.newInstance() // dynamic create KPojo instance by reference, NO REFLECTION used

val movie = Movie(1)
val dataMap: Map<String, Any?> = movie.toDataMap() // Map("id" = 1), NO REFLECTION used
val movie2: Movie = dataMap.mapperTo<Movie>() // or dataMap.mapperTo(Movie::class), NO REFLECTION used

val tableName = movie.kronosTable() // "tb_movie", NO REFLECTION used
val columns = movie.kronosColumns() // [Field(id), Field(name), ...], NO REFLECTION used
// We actually provide more functions such as getting the table creation time, update time, logical deletion of field settings and so on

instance["fieldName"] // get field value by field name, NO REFLECTION used
instance["fieldName"] = "value" // set field value by field name, NO REFLECTION used
```

------


## 🛠️ Working with Spring or Other Frameworks

Please refer to the following example projects for more information:

- [Example for SpringBoot](https://github.com/Kronos-orm/kronos-example-spring-boot)
- [Example for Ktor](https://github.com/Kronos-orm/kronos-example-ktor)
- [Example for Vert.x](https://github.com/Kronos-orm/kronos-example-vertx)
- [Example for Solon](https://github.com/Kronos-orm/kronos-example-solon)
- [Example for Android](https://github.com/Kronos-orm/kronos-example-android)

## 📚 Documentation

For more information, please visit the [official website](https://www.kotlinorm.com) or
the [documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start).

## 📜 License

Kronos-ORM is released under the Apache 2.0 license.

## 🤝 Contributing

Please refer to the [CONTRIBUTING.md](https://github.com/Kronos-orm/Kronos-orm/blob/main/CONTRIBUTING.md) for more.

## Contributors

We would like to express our gratitude to all the individuals who have already contributed to Kronos-ORM!

<a href="https://github.com/Kronos-orm/Kronos-orm/graphs/contributors">
  <img alt="contributors" src="https://contrib.rocks/image?repo=Kronos-orm/Kronos-orm"/>
</a>

-------------------

If you are interested in Kronos-ORM, please join our community and chat with us!

[![discord](https://img.shields.io/badge/Discord-Kronos_ORM-blue?logo=discord&logoColor=white)](https://discord.gg/Vn8EzxRX)

[![QQ群](https://img.shields.io/badge/QQ%E7%BE%A4-991790071-red?logo=qq)](https://qm.qq.com/q/V821JzFUcM)

![QRCode](https://github.com/Kronos-orm/Kronos-orm/blob/main/assets/invitation_qrcode.jpg?raw=true)

**If you like Kronos-ORM, please give us a star ⭐️, thank you!**
