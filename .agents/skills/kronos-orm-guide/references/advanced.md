# Kronos ORM 高级功能

## 目录

1. [级联操作](#级联操作)
2. [内置函数](#内置函数)
3. [逻辑删除](#逻辑删除)
4. [乐观锁](#乐观锁)
5. [命名策略](#命名策略)
6. [空值策略](#空值策略)
7. [序列化](#序列化)
8. [原生SQL](#原生sql)
9. [多数据源](#多数据源)

---

## 级联操作

级联通过 `@Cascade` 注解定义关系，支持级联查询、插入、更新、删除。

### 模型定义

```kotlin
// 一对多
data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @Cascade(["id"], ["directorId"])
    var movies: List<Movie>? = null
) : KPojo

data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    var directorId: Int? = null,
    @Cascade(["directorId"], ["id"])
    var director: Director? = null
) : KPojo
```

### 级联查询

```kotlin
// 查询 Director 时自动加载关联的 movies
val director = Director(id = 1).select()
    .cascade(depth = 1)  // 级联深度
    .queryOne()
// director.movies 自动填充
```

### 级联插入

```kotlin
val director = Director(
    name = "Spielberg",
    movies = listOf(
        Movie(name = "Jaws"),
        Movie(name = "E.T.")
    )
)
director.insert().cascade().execute()
// 自动插入 director 和关联的 movies，并填充外键
```

### 级联更新

```kotlin
director.update().cascade().set { it.name }.by { it.id }.execute()
```

### 级联删除

```kotlin
director.delete().cascade().where { it.id == 1 }.execute()
// 自动删除关联的 movies
```

---

## 内置函数

Kronos 提供数据库函数的类型安全调用，通过 `f` 对象访问：

```kotlin
// 聚合函数
User().select { f.count(it.id) }.queryOne<Int>()
User().select { f.sum(it.age) }.queryOne<Int>()
User().select { f.avg(it.age) }.queryOne<Double>()
User().select { f.max(it.age) }.queryOne<Int>()
User().select { f.min(it.age) }.queryOne<Int>()

// 字符串函数
User().select { f.upper(it.name) }.queryList()
User().select { f.lower(it.name) }.queryList()
User().select { f.length(it.name) }.queryList()
User().select { f.concat(it.name, it.age) }.queryList()

// 数学函数
User().select { f.abs(it.age) }.queryList()
User().select { f.round(it.score, 2) }.queryList()

// 在条件中使用函数
User().select().where { f.length(it.name) > 5 }.queryList()

// 在分组中使用
User().select { it.age + f.count(it.id) }
    .groupBy { it.age }
    .having { f.count(it.id) > 5 }
    .queryList()
```

---

## 逻辑删除

配置方式：

1. 全局策略：
```kotlin
Kronos.init {
    logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
}
```

2. 注解方式：
```kotlin
data class User(
    @LogicDelete
    var deleted: Boolean? = false
) : KPojo
```

启用后：
- `delete()` 自动变为 `UPDATE ... SET deleted = 1`
- `select()` 自动添加 `WHERE deleted = 0`

---

## 乐观锁

```kotlin
data class User(
    @Version
    var version: Int? = null
) : KPojo
```

或全局配置：
```kotlin
Kronos.init {
    optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
}
```

update 时自动添加 `WHERE version = :currentVersion` 并递增版本号。

---

## 命名策略

```kotlin
Kronos.init {
    // 驼峰转下划线：userName -> user_name
    tableNamingStrategy = lineHumpNamingStrategy
    fieldNamingStrategy = lineHumpNamingStrategy
    // 不转换
    tableNamingStrategy = NoneNamingStrategy
}
```

---

## 空值策略

控制条件中值为 null 时的行为：

```kotlin
where { (it.age == nullableAge).ifNoValue(ignore) }       // 忽略该条件
where { (it.age == nullableAge).ifNoValue(alwaysTrue) }    // 恒真
where { (it.age == nullableAge).ifNoValue(alwaysFalse) }   // 恒假
```

---

## 序列化

复杂对象字段可通过 `@Serialize` 注解以 JSON 形式存储：

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null
) : KPojo
```

需要配置序列化处理器：
```kotlin
Kronos.init {
    serializeProcessor = JacksonProcessor()  // 或 GsonProcessor
}
```

---

## 原生SQL

```kotlin
val users = dataSource.forList(
    KronosAtomicQueryTask(
        "SELECT * FROM user WHERE name = :name AND age > :age",
        mapOf("name" to "Kronos", "age" to 18)
    )
)
```

---

## 多数据源

```kotlin
val mysqlWrapper = KronosBasicWrapper(mysqlDataSource)
val pgWrapper = KronosBasicWrapper(pgDataSource)

// 默认数据源
Kronos.init { dataSource = { mysqlWrapper } }

// 指定数据源执行
user.insert().execute(pgWrapper)
user.select().queryList(pgWrapper)

// 指定数据源的事务
transact(pgWrapper) {
    user.insert().execute()
}
```
