{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

请确保您的KPojo具有正确的注解，以便Kronos能够正确识别表名、字段、索引等信息。

> **Note**
> 相关功能可可以通过`Kronos.dataSource(() -> KronosDataSourceWrapper)`或某个具体的数据源对象`KronosDataSourceWrapper`
> 来调用。

## 1. {{ $.title("exists(tableName)")}} 表是否存在

通过表名判断表是否存在

- **函数声明**

 ```kotlin
 fun exists(tableName: String): Boolean
 ```

- **使用示例**

 ```kotlin
 val exists = wrapper.table.exists("user")
 ```

- **接收参数**

{{ $.params([['tableName', '表名', 'String']]) }}

- **返回值**

`Boolean` 表是否存在

{{ $.hr() }}

## 2. {{ $.title("exists<T>(KPojo)")}} 表是否存在

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过KPojo判断表是否存在

- **函数声明**

```kotlin
fun <T : KPojo> exists(kPojo: T = new T()): Boolean
```

<small>_{{ $.keyword("concept/kpojo-generic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

```kotlin
val exists = wrapper.table.exists(User())
// 或
val exists = wrapper.table.exists<User>()
```

- **接收参数**

{{ $.params([['kPojo', '实体对象', 'T', 'new T()']]) }}

- **返回值**

`Boolean` - 表是否存在

## 3. {{ $.title("createTable(KPojo)")}} 创建表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过实体类创建表

- **函数声明**

```kotlin
fun createTable<T: KPojo>(kPojo: T = new T())
```

<small>_{{ $.keyword("concept/kpojo-generic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

```kotlin
wrapper.table.createTable(User())
// 或
wrapper.table.createTable<User>()
```

- **接收参数**

{{ $.params([['kPojo', '实体对象', 'T', 'new T()']]) }}

{{ $.hr() }}

## 4. {{ $.title("truncateTable(tableName, restartIdentity)")}} 清空表

通过表名清空表

- **函数声明**

```kotlin
fun truncateTable(tableName: String, restartIdentity: Boolean = true)
```

- **使用示例**

```kotlin
wrapper.table.truncateTable("user")
```

- **接收参数**

{{ $.params([['tableName', '表名', 'String'], ['restartIdentity', '是否重置自动递增值，适用于 PostgreSQL 和 sqlite', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 5. {{ $.title("truncateTable(KPojo, restartIdentity)")}} 清空表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

{{ $.hr(50) }}

通过实体类清空表

- **函数声明**

```kotlin
fun <T: KPojo> truncateTable(kPojo: T = new T(), restartIdentity: Boolean = true)
```

<small>_{{ $.keyword("concept/kpojo-generic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

```kotlin
wrapper.table.truncateTable(User())
// 或
wrapper.table.truncateTable<User>()
```

- **接收参数**

{{ $.params([['kPojo', '实体对象', 'T', 'new T()'], ['restartIdentity', '是否重置自动递增值，适用于 PostgreSQL 和 sqlite', 'Boolean', 'true']]) }}

{{ $.hr() }}

## 6. {{ $.title("dropTable(tableName)")}} 删除表

通过表名删除表

- **函数声明**

```kotlin
fun dropTable(tableName: String)
```

- **使用示例**

```kotlin
wrapper.table.dropTable("user")
```

- **接收参数**

{{ $.params([['tableName', '表名', 'String']]) }}

{{ $.hr() }}

## 7. {{ $.title("dropTable(KPojo)")}} 删除表

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

通过实体类删除表

- **函数声明**

```kotlin
fun <T: KPojo> dropTable(kPojo: T = new T())
```

<small>_{{ $.keyword("concept/kpojo-generic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

```kotlin
wrapper.table.dropTable(User())
// 或
wrapper.table.dropTable<User>()
```

- **接收参数**

{{ $.params([['kPojo', '实体对象', 'T', 'new T()']]) }}

{{ $.hr() }}

## 8. {{ $.title("syncTable(KPojo)")}} 表结构同步和变更

- **泛型参数**： `<T>` 实体对象类型，继承自`KPojo`

通过实体类同步表结构

- **函数声明**

```kotlin

fun syncTable<T: KPojo>(kPojo: T = new T())
```

<small>_{{ $.keyword("concept/kpojo-generic-instantiate", ["Kronos是如何不依赖反射实现将KClass&lt;KPojo&gt;实例化的？"])}}_</small>

- **使用示例**

```kotlin
wrapper.table.syncTable(User())
// 或
wrapper.table.syncTable<User>()
```

- **接收参数**

{{ $.params([['kPojo', '实体对象', 'T', 'new T()']]) }}

{{ $.hr() }}

## 9. {{ $.title("getTableCreateSqlList")}} 动态建表

部分情况下会存在动态建表的需求，此时可以通过`getTableCreateSqlList`方法动态获取建表语句并执行。

- **函数声明**

```kotlin
fun getTableCreateSqlList(
    dbType: DBType,
    tableName: String,
    fields: List<Field>,
    indexes: List<KTableIndex> = emptyList()
): List<String>
```

- **使用示例**

```kotlin

val listOfSql =
    getTableCreateSqlList(
        dbType = DBType.Mysql,
        tableName = "user",
        fields = listOf(
            Field(
                name = "id",
                type = KColumnType.fromString("INT"),
                primaryKey = true,
                identity = true
            ),
            Field(
                name = "name",
                type = KColumnType.fromString("VARCHAR"),
                length = 255
            ),
            Field(
                name = "age",
                type = KColumnType.fromString("INT"),
            )
        ),
        indexes = listOf(
            KTableIndex(
                name = "idx_name",
                columns = listOf("name"),
                type = "UNIQUE"
            )
        )
    )
    
listOfSql.forEach { db.execute(it) }
```

- **接收参数**

{{$.params([
['dbType', '数据库类型', 'DBType'],
['tableName', '表名', 'String'],
['fields', '字段列表', 'List<Field>'],
['indexes', '索引列表', 'List<KTableIndex>', '[]']
])}}

{{ $.hr() }}

> **Warning**
> 若需要对同一个实体对象连续执行多个数据库操作，建议不要使用`createTable<KPojo>()`的写法，而是使用`createTable(kPojo)`，以避免多次创建KPojo对象，产生不必要的开销。
