# Kronos ORM 注解参考

## @Table

指定实体类对应的数据库表名。不使用此注解时，表名由 `tableNamingStrategy` 自动转换。

```kotlin
@Target(AnnotationTarget.CLASS)
annotation class Table(val name: String)
```

```kotlin
@Table("tb_user")
data class User(val id: Int? = null) : KPojo
```

## @PrimaryKey

标记主键字段。`identity = true` 表示自增主键。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class PrimaryKey(val identity: Boolean = false)
```

```kotlin
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null
) : KPojo
```

## @Column

指定列名、长度、精度等属性。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class Column(
    val name: String = "",
    val length: Int = 0,
    val precision: Int = 0,
    val scale: Int = 0,
    val nullable: Boolean = true,
    val defaultValue: String = ""
)
```

```kotlin
data class User(
    @Column("user_name", length = 100)
    var name: String? = null
) : KPojo
```

## @ColumnType

显式指定列的数据库类型。

```kotlin
@ColumnType(CHAR)
var name: String? = null

@ColumnType(TEXT)
var description: String? = null
```

## @Default

指定列的默认值。

```kotlin
@Default("0")
var deleted: Boolean? = false

@Default("CURRENT_TIMESTAMP")
var createTime: String? = null
```

## @LogicDelete

标记逻辑删除字段。启用后 delete 操作变为 UPDATE（设置标记值），select 自动过滤已删除记录。

```kotlin
// 属性级别
data class User(
    @LogicDelete
    var deleted: Boolean? = false
) : KPojo

// 类级别（使用全局策略中配置的字段）
@LogicDelete
data class User(var id: Int? = null) : KPojo
```

## @Version

标记乐观锁版本字段。update 时自动检查并递增版本号。

```kotlin
data class User(
    @Version
    var version: Int? = null
) : KPojo
```

## @CreateTime / @UpdateTime

自动填充创建时间和更新时间。

```kotlin
data class User(
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null
) : KPojo
```

支持的时间类型：`LocalDateTime`、`Date`、`String`（配合 `@DateTimeFormat`）。

## @DateTimeFormat

指定时间字段的格式化模式（当字段类型为 String 时）。

```kotlin
@UpdateTime
@DateTimeFormat("yyyy-MM-dd HH:mm:ss")
var updateTime: String? = null
```

## @Cascade

定义级联关系。`properties` 是当前实体的外键字段，`targetProperties` 是目标实体的关联字段。

```kotlin
@Target(AnnotationTarget.PROPERTY)
annotation class Cascade(
    val properties: Array<String>,
    val targetProperties: Array<String>,
    val onDelete: String = "NO ACTION"
)
```

一对一：
```kotlin
data class Child(
    var parentId: Long? = null,
    @Cascade(["parentId"], ["id"])
    var parent: Parent? = null
) : KPojo
```

一对多：
```kotlin
data class Parent(
    var id: Long? = null,
    @Cascade(["id"], ["parentId"])
    var children: List<Child>? = null
) : KPojo
```

多对多（通过中间表）：
```kotlin
data class Student(
    var id: Long? = null,
    @Cascade(["id"], ["studentId"])
    var enrollments: List<Enrollment>? = null
) : KPojo

data class Enrollment(
    var studentId: Long? = null,
    var courseId: Long? = null,
    @Cascade(["courseId"], ["id"])
    var course: Course? = null
) : KPojo
```

## @TableIndex

定义表索引，可重复使用。

```kotlin
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class TableIndex(
    val name: String,
    val columns: Array<String>,
    val type: String = "",
    val method: String = "",
    val concurrently: Boolean = false
)
```

```kotlin
@TableIndex("idx_name", ["name"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
@TableIndex("idx_age", ["age"])
data class User(...) : KPojo
```

## @Serialize

标记需要 JSON 序列化存储的复杂类型字段。

```kotlin
data class User(
    @Serialize
    var tags: List<String>? = null,
    @Serialize
    var profile: Profile? = null
) : KPojo
```

需要配置 `serializeProcessor`（如 `GsonProcessor`、`JacksonProcessor`）。

## @Ignore

忽略字段，不参与 ORM 操作。可指定忽略的操作类型。

```kotlin
data class User(
    @Ignore
    var tempField: String? = null
) : KPojo
```

## @Necessary

标记字段为必填（DDL 中生成 NOT NULL）。

```kotlin
data class User(
    @Necessary
    var name: String? = null
) : KPojo
```
