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

Kronos is a modern ORM framework designed for Kotlin, which is **suitable for both backend and mobile applications, support multi-database, and high performance**.

KCP implementation of the expression tree analysis support as well as Kotlin itself generic, higher-order functions, extended functions and other syntactic features, so that Kronos has **powerful expressive power and concise , semantic writing to make the operation of the database has become more simple**.

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1839b25a691b49ee93a0ace2a6255cb0)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![build](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml)
[![Maven central](https://img.shields.io/maven-central/v/com.kotoframework/koto-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.kotoframework%22)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.kotoframework/koto-core.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/kotoframework/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[Official Website](https://www.kotlinorm.com) | [Documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start)
</div>

--------
# Re-define KotlinORM

❓ **Why Kronos?**

* Utilizes the **entire ecosystem and resources** of the JVM platform, such as **database drivers and logging frameworks**, with potential future support for Kotlin Multiplatform.
* **Kotlin compiler plugins and coroutine-driven** design significantly **reduce the use of reflection**, providing a **high-performance** database operation experience.
* Supports most **mainstream databases** and allows **freely adding database extensions through plugins**.
* **Concise and expressive writing, supporting Kotlin native syntax** `==`, `>`, `<`, `in`, etc., instead of .eq, .gt, .lt, etc.
* Strong type checking.
* Supports **transactions**, **complex cascading operations without foreign keys (one-to-one, one-to-many, many-to-many)**, **serialization and deserialization**, **cross-database queries**, and **database table/index/remarks creation and structure synchronization**, etc.
* Supports **Logical Deletion**, **Optimistic Lock**, **Creation Time**, **Update Time**, and offers flexible customization settings.
* Easy integration with any third-party framework.
* **Native SQL database manipulation based on named parameters**.
* Supports easy conversion of **data entity classes to Map or from Map to data entity classes** via compile-time operations with **no reflection, near-zero overhead**.
* Data classes can be treated as database table models, **significantly reducing additional class definitions**.

-------

## 🖥 JDK、Kotlin and Build Tools

- **JDK 8+**
- **Kotlin 2.0.0+**
- **Maven 3.6.3+** or **Gradle 6.8.3+**

> Please make sure your kotlin plugin for ide supports kotlin 2.0.0 or higher.
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

## 🚀 Quick Start

### 🔗 Connect to Database

You can set default data source by:

```kotlin
Kronos.dataSource = { KronosBasicWrapper(SomeDataSource()) }
// for example:
//Kronos.dataSource = { KronosBasicWrapper(MysqlDataSource("jdbc:mysql://localhost:3306/test", "root", "***")) }
//Kronos.dataSource = { KronosBasicWrapper(HikariDataSource().apply { jdbcUrl = "jdbc:mysql://localhost:3306/test" ... }) }
//Kronos.dataSource = { KronosBasicWrapper(BasicDataSource().apply { url = "jdbc:sqlite:path/to/db" ... }) }
```

More details about connecting to the database and use dynamic data source or multiple data sources, please refer to the [docs](https://kotlinorm.com/#/documentation/en/database/connect-to-db).

### 🎨 Table Model Definition(Annotation Style)

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null, // primary key and auto increment
    val name: String? = null, // movie name
    val directorId: Long? = null, // director id
    @Cascade(["directorId"], ["id"])
    val director: Director? = null, // cascade table & one-to-many
    val relations: List<MovieActorRelation>? = null, // reference list & many-to-many
    @Serializable
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

------------

### 📝 Transform `KPojo` to Map or from Map

It's easy to transform `KPojo` to Map or from Map, and you can get the table name and column information through the
`kronosTable` and `kronosColumns` methods in a **NO REFLECTION** way.

```kotlin
val movie = Movie(1)
val dataMap: Map<String, Any?> = movie.toDataMap() // Map("id" = 1)
val movie2: Movie = dataMap.mapperTo<Movie>() // or dataMap.mapperTo(Movie::class)
val tableName = movie.kronosTable() // "tb_movie", NO REFLECTION used
val columns = movie.kronosColumns() // [Field(id), Field(name), ...], NO REFLECTION used
```

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
        user.id + user.username + relation.id.`as`("relationId") + 
                role.role + "count(1) as count"
    }
    where { user.id < 10 }
}.query()
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

## 🛠️ Working with Spring or Other Frameworks

Please refer to the following example projects for more information:

[Example for SpringBoot](https://github.com/Kronos-orm/kronos-spring-demo)

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

**If you like Kronos-ORM, please give us a star ⭐️, thank you!**
