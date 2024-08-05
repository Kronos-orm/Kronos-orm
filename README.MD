![logo](https://cdn.leinbo.com/assets/images/kronos/logo_dark.png)

[![build](https://github.com/kronos-orm/kronos-orm/actions/workflows/code_quality.yml/badge.svg)](https://github.com/kronos-orm/kronos-orm/actions/workflows/code_quality.yml)
[![License](https://img.shields.io/:license-apache-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# Re-define KotlinORM

Kronos(Kotlin Reactive ORM, No-extra-class, Optimized, Simplified) is a lightweight framework that provides developers
with a concise and efficient data persistence solution.

❓ **Why Kronos?**

- [x] K2 Compiler Plugin Powered
- [x] Multi-Database Support
- [x] Concise writing
- [x] Strong type checking
- [x] High performance(less runtime reflection)
- [x] Low intrusiveness
- [x] No Extra Class
- [x] Strong Coroutines Support
- [x] Task handling mechanism

-------
**Code First KPojo Definition:**

```kotlin
@Table(name = "tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    val id: Long? = null, // primary key and auto increment
    val name: String? = null, // movie name
    val year: Int? = null, // the year of the movie
    val director: String? = null, // director
    val actor: String? = null, // actor
    @UseSerializeResolver
    val type: List<String>? = null, // type
    val country: String? = null, // country
    val language: String? = null, // language
    val description: String? = null, // description
    val poster: String? = null, // poster
    val video: String? = null, // video
    @Column("move_summary")
    val summary: String? = null, // summary
    val tags: String? = null, // tags
    val score: Double? = null, // score
    val vote: Int? = null, // vote
    val favorite: Int? = null, // favorite
    @LogicDelete
    val deleted: Boolean? = null, // logic delete
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @UpdateTime val updateTime: String? = null, // update time
) : KPojo()
```

> Why use annotations instead of Kotlin DSL?
>
> We read annotation information through a compiler plugin and store the information in the entity class definition, so that all table structure parsing occurs at compile time rather than runtime. While this approach sacrifices some flexibility, it helps avoid runtime performance overhead.

------
**Table Operation:**

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
**Table Query:**

```kotlin
// single query
val listOfUser: List<User> = User()
    .select { it.id + it.username }
    .where { it.id < 10 }
    .distinct()
    .groupBy { it.id }
    .orderBy { it.username.desc() }
    .queryList()

// with Pagination
val (total, list) = User()
    .select { it.id + it.username }
    .where { it.id < 10 }
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

**Table Insert:**

```kotlin
// single insert
user.insert()

// batch insert
listOfUser.insert()
```

**Table Update:**

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

**Table Upsert:**

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

**Table Delete:**

```kotlin
// delete rows by some conditions
user.delete()
    .where { it.id == 1 }
    .execute()
```

------
> [Read more](https://www.kotlinorm.com)
>
> [Official Website](https://www.kotlinorm.com)
>
> [docs](https://www.kotlinorm.com)