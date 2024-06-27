![logo](https://cdn.leinbo.com/assets/images/kronos/logo_dark.png)

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

------
**Table Operation:**

```kotlin
//is table exists
dataSource.table.isTableExists<Movie>()

// create table
dataSource.table.createTable<Movie>()

// drop table
dataSource.table.deleteTable<Movie>()

//sync table structure
dataSource.table.structureSync<Movie>()
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
    leftJoin(relation) {
        user.id == relation.userId
    }
    rightJoin(role) {
        user.id == role.userId && relation.roleId == role.id
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
    .execute()

// upsert on duplicate key
user.upsert { it.username }
    .onDuplicateKey()
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
> [Read more](https://kotoframework.com/#v2)
>
> [Official Website](https://kronos-orm.fun)
>
> [docs](https://kotlinorm.com/docs/)