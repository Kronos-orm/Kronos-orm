<p align="center">
    <a href="https://www.kotlinorm.com">
        <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" alt="logo" height="256" width="256">
    </a>
</p>

---------

<h1 align="center">
    Kronos-ORM
</h1>

<div align="center">

A **modern** & **lightweight** kotlin ORM that provides developers with a concise and efficient way to interact with
databases

for both **backend** and **mobile applications**, support **multi-database**, and **high performance**.

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1839b25a691b49ee93a0ace2a6255cb0)](https://app.codacy.com/gh/Kronos-orm/Kronos-orm/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![build](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/reviewdog.yml)
[![Maven central](https://img.shields.io/maven-central/v/com.kotoframework/koto-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.kotoframework%22)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.kotoframework/koto-core.svg)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/kotoframework/)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

[Official Website](https://www.kotlinorm.com) | [Documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start)

[官网](https://www.kotlinorm.com) | [中文文档](https://kotlinorm.com/#/documentation/zh-CN/getting-started/quick-start)
</div>

--------
# Re-define KotlinORM

❓ **Why Kronos?**

- [x] K2 Compiler Plugin Powered **High Performance**
- [x] Mainstream database support and easy to extend
- [x] **Concise** and **expressive** writing, supports **kotlin syntax** `==`, `>`, `<`, `in`, etc. instead of `.eq`,
  `.gt`, `.lt`, etc.
- [x] Strong type checking
- [x] Supports **logical deletion**, **locks**, **transactions**, **cascading operations**, **cross-database queries**,
  **database table structure synchronization**, and **one-click switching of database types**, and more
- [x] Easily integrate with any third-party framework
- [x] Naming parameter based sql operation
- [x] Easy to transform `KPojo` to Map or from Map
- [x] Greatly reduce additional `class` definitions

-------

## 🖥 JDK、Kotlin and Build Tools

- **JDK 8+**
- **Kotlin 2.0.0+**
- **Maven 3.6.3+** or **Gradle 6.8.3+**

> Please make sure your kotlin plugin for ide supports kotlin 2.0.0 or higher.
>
> If you built failed in **Intellij IDEA**(or **Android Studio**) and build with **Maven**,
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
    @Reference(["directorId"], ["id"])
    val director: Director? = null, // reference table & one-to-many
    val movieActorId: Long? = null, //  intermediate table for many-to-many
    @Reference(["movieActorId"], ["id"])
    val actors: List<MovieActor>? = null, // reference list & many-to-many
    @ColumnDeserialize
    val type: List<String>? = null, // deserialize from json string
    @Column("movie_summary")
    val summary: String? = null, // summary with column name
    @Version val version: Long? = null, // version for optimistic lock
    @LogicDelete val deleted: Boolean? = null, // logic delete
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @UpdateTime val updateTime: String? = null, // update time and date format
    @CreateTime val createTime: LocalDateTime? = null // create time
) : KPojo // KPojo is a marker interface
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

//sync table structure
dataSource.table.schemeSync<Movie>()
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
    on {
        user.id == relation.userId && user.id == role.userId && relation.roleId == role.id
    }
    select {
        user.id + user.username + relation.relation + role.role
    }
    where {
        user.id < 10
    }
}.query()
```

### ➕ Insert

```kotlin
// single insert
user.insert()

// batch insert
listOfUser.insert()
```

### ✏️ Update

```kotlin
// update by some conditions use `set`
user.update()
    .set {
        it.username = "123"
        it.gender = 1
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

[Example for Spring](https://github.com/Kronos-orm/kronos-spring-demo)

## 📚 Documentation

For more information, please visit the [official website](https://www.kotlinorm.com) or
the [documentation](https://kotlinorm.com/#/documentation/en/getting-started/quick-start).

## 📜 License

Kronos-ORM is released under the Apache 2.0 license.

## 🤝 Contributing

Please refer to the [CONTRIBUTING.md](https://github.com/Kronos-orm/Kronos-orm/blob/main/CONTRIBUTING.md) for more.

-------------------

**If you like Kronos-ORM, please give us a star ⭐️, thank you!**