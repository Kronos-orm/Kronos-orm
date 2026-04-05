---
name: kronos-orm-guide
description: >
  Kronos ORM 使用指南。当用户询问如何使用 Kronos ORM 进行数据库操作时触发此技能，包括：
  定义数据类(KPojo)、CRUD操作(select/insert/update/delete/upsert)、条件DSL(where/having/on)、
  联表查询(join)、表操作(DDL)、事务、级联(cascade)、逻辑删除、乐观锁、内置函数等。
  当用户提到 Kronos、KPojo、kronos-core、kronos-orm 或相关数据库操作时，务必使用此技能。
---

# Kronos ORM 使用指南

Kronos 是一个基于 Kotlin 编译器插件的现代 ORM 框架，零反射、强类型、支持多数据库（MySQL、PostgreSQL、SQLite、SQL Server、Oracle）。

## 目录

1. [项目配置](#项目配置)
2. [数据类定义](#数据类定义)
3. [全局配置](#全局配置)
4. [Insert 插入](#insert)
5. [Delete 删除](#delete)
6. [Update 更新](#update)
7. [Select 查询](#select)
8. [Upsert 存在则更新](#upsert)
9. [条件DSL](#条件dsl)
10. [Join 联表查询](#join)
11. [事务](#事务)
12. [表操作 DDL](#表操作)

高级主题（级联、内置函数、自定义函数等）见 `references/advanced.md`。
注解完整参考见 `references/annotations.md`。

---

## 项目配置

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:0.1.0-SNAPSHOT")
    // JDBC 包装器（可选，提供开箱即用的数据源支持）
    implementation("com.kotlinorm:kronos-jdbc-wrapper:0.1.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>com.kotlinorm</groupId>
    <artifactId>kronos-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

在 `kotlin-maven-plugin` 中添加编译器插件：
```xml
<compilerPlugins>
    <plugin>kronos-maven-plugin</plugin>
</compilerPlugins>
```

要求：JDK 8+，Kotlin 2.3.0+

---

## 数据类定义

所有实体类必须实现 `KPojo` 接口，属性使用可空类型并提供默认值：

```kotlin
import com.kotlinorm.interfaces.KPojo
import com.kotlinorm.annotations.*

@Table("tb_movie")
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("name") @ColumnType(CHAR)
    var name: String? = null,
    var directorId: Long? = null,
    @Cascade(["directorId"], ["id"])
    var director: Director? = null,
    @LogicDelete @Default("0")
    var deleted: Boolean? = false,
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: Date? = null
) : KPojo
```

常用注解速查：

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Table("name")` | 指定表名 | `@Table("tb_user")` |
| `@PrimaryKey(identity=true)` | 主键，自增 | 标注在 id 属性上 |
| `@Column("name")` | 指定列名 | `@Column("user_name")` |
| `@ColumnType(type)` | 指定列类型 | `@ColumnType(CHAR)` |
| `@Default("value")` | 默认值 | `@Default("0")` |
| `@LogicDelete` | 逻辑删除标记 | 标注在 deleted 属性上 |
| `@Version` | 乐观锁版本号 | 标注在 version 属性上 |
| `@CreateTime` | 自动填充创建时间 | 标注在 createTime 上 |
| `@UpdateTime` | 自动填充更新时间 | 标注在 updateTime 上 |
| `@Cascade` | 级联关系 | `@Cascade(["fkId"], ["id"])` |
| `@TableIndex` | 表索引 | 标注在类上 |
| `@Serialize` | JSON序列化存储 | 复杂对象字段 |
| `@Ignore` | 忽略字段 | 非数据库字段 |
| `@DateTimeFormat` | 日期格式 | `@DateTimeFormat("yyyy-MM-dd")` |

完整注解说明见 `references/annotations.md`。

---

## 全局配置

```kotlin
import com.kotlinorm.Kronos
import com.kotlinorm.KronosBasicWrapper

val wrapper by lazy {
    BasicDataSource().apply {
        driverClassName = "com.mysql.cj.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/mydb"
        username = "user"
        password = "pass"
    }.let { KronosBasicWrapper(it) }
}

Kronos.init {
    dataSource = { wrapper }
    tableNamingStrategy = lineHumpNamingStrategy  // userName -> user_name
    fieldNamingStrategy = lineHumpNamingStrategy
    createTimeStrategy = KronosCommonStrategy(true, Field("createTime"))
    updateTimeStrategy = KronosCommonStrategy(true, Field("updateTime"))
    logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
    optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
    defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
    timeZone = ZoneId.of("Asia/Shanghai")
}
```

---

## Insert

```kotlin
// 单条插入
val user = User(name = "Kronos", age = 18)
user.insert().execute()

// 获取自增ID
val lastId = user.insert().execute().lastInsertId

// 批量插入
listOf(user1, user2, user3).insert().execute()

// 指定插入字段
user.insert { it.name + it.age }.execute()
```

---

## Delete

```kotlin
// 按条件删除
User().delete().where { it.id == 1 }.execute()

// 按对象非空字段删除
User(id = 1).delete().execute()

// 批量删除
listOf(user1, user2).delete().execute()
```

如果实体配置了 `@LogicDelete`，delete 会自动执行逻辑删除（UPDATE 而非 DELETE）。

---

## Update

```kotlin
// 使用 set DSL 更新
user.update().set { it.name = "New Name" }.by { it.id }.execute()

// 按字段选择器更新（使用对象中的值）
user.update { it.name }.by { it.id }.execute()

// 批量更新
listOf(user1, user2).update { it.name }.by { it.id }.execute()

// 条件更新
User().update().set { it.age = 20 }.where { it.name == "Kronos" }.execute()
```

`by` 用于指定匹配条件字段（取对象中的值），`where` 用于自定义条件表达式。

---

## Select

```kotlin
// 查询单条
val user: User = User(id = 1).select().queryOne()

// 查询列表
val users: List<User> = User().select().where { it.age > 18 }.queryList()

// 选择特定字段
val name: String = User().select { it.name }.where { it.id == 1 }.queryOne<String>()

// 排序 + 分页
val users = User().select()
    .where { it.age > 18 }
    .orderBy { it.age.desc() }
    .page(1, 10)
    .queryList()

// 分组 + 聚合
val result = User().select { it.age + f.count(it.id) }
    .groupBy { it.age }
    .having { f.count(it.id) > 5 }
    .queryList()

// 去重
User().select { it.name }.distinct().queryList()
```

<!-- APPEND_MARKER_1 -->

---

## Upsert

存在则更新，不存在则插入：

```kotlin
// 基本用法：按 onFields 判断是否存在
user.upsert().on { it.id }.execute()

// 指定更新字段
user.upsert().on { it.id }.set { it.name + it.age }.execute()

// 批量 upsert
listOf(user1, user2).upsert().on { it.id }.execute()
```

`on` 指定用于判断记录是否存在的字段，`set` 指定存在时要更新的字段。

---

## 条件DSL

Kronos 使用 Kotlin 原生语法构建条件，支持 `where`、`having`、`on` 子句：

```kotlin
// 等于
where { it.age == 18 }
where { it.age.eq(18) }
where { it.age eq 18 }

// 不等于
where { it.age != 18 }

// 比较
where { it.age > 18 }    // gt
where { it.age >= 18 }   // ge
where { it.age < 18 }    // lt
where { it.age <= 18 }   // le

// 范围
where { it.age.between(1..10) }
where { it.age between 1..10 }

// IN
where { it.id in listOf(1, 2, 3) }

// 模糊查询
where { it.name.like("Kronos%") }
where { it.name.startsWith("Kronos") }
where { it.name.endsWith("ORM") }
where { it.name.contains("ron") }

// NULL 判断
where { it.name.isNull }
where { it.name.notNull }

// 正则
where { it.name.regexp("Kronos.*") }

// 逻辑组合
where { (it.age > 18) and (it.name like "K%") }
where { (it.age > 18) or (it.name == "Kronos") }
where { !(it.age > 18) }

// 使用对象值的无参形式（取对象中对应属性的值）
val user = User(age = 18)
user.select().where { it.age.eq }  // WHERE age = 18

// 自动生成所有非空字段的等值条件
user.select().where { it.eq }

// 排除字段
user.select().where { (it - it.id).eq }

// 原生 SQL 条件
where { "name = 'Kronos' and age > 18".asSql() }
where { "name = :name".asSql() }.patch("name" to "Kronos")

// 空值策略
val age: Int? = null
where { (it.age == age).ifNoValue(ignore) }      // 忽略该条件
where { (it.age == age).ifNoValue(alwaysFalse) }  // 条件恒假
```

---

## Join

```kotlin
// 内连接
val result = User().join(Order()) { user, order ->
    on { user.id == order.userId }
    select { user.name + order.amount }
    where { user.age > 18 }
}.queryList()

// 左连接
User().leftJoin(Order()) { user, order ->
    on { user.id == order.userId }
    select { user.name + order.amount }
}.queryList()

// 右连接
User().rightJoin(Order()) { ... }.queryList()

// 多表连接
User().join(Order(), Product()) { user, order, product ->
    on { user.id == order.userId }
    on { order.productId == product.id }
    select { user.name + product.name + order.amount }
}.queryList()

// 跨数据库连接
User().join(Order()) { user, order ->
    on { user.id == order.userId }
    select { user.name + order.amount }
}.withTotal().queryList()  // 同时返回总数
```

---

## 事务

```kotlin
// 基本事务
transact {
    user1.insert().execute()
    user2.update().set { it.name = "test" }.by { it.id }.execute()
    // 抛出异常自动回滚
}

// 指定数据源的事务
transact(wrapper) {
    user.insert().execute()
}
```

---

## 表操作

```kotlin
val dataSource = Kronos.dataSource()

// 创建表（如果不存在）
dataSource.table.createTable(User())

// 同步表结构（自动添加/修改列、索引）
dataSource.table.syncTable(User())

// 删除表
dataSource.table.dropTable(User())
```

`syncTable` 会对比当前数据类定义与数据库表结构的差异，自动执行 ALTER TABLE 添加新列、修改列类型、创建/删除索引等操作。

---

更多高级用法（级联操作、内置函数、自定义函数、多租户等）请参阅 `references/advanced.md`。
