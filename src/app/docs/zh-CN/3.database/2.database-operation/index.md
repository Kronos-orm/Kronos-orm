{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

请确保您的KPojo具有正确的注解，以便Kronos能够正确识别表名、字段、索引等信息。

> **Note**
> 相关功能可可以通过`Kronos.dataSource(() -> KronosDataSourceWrapper)`或某个具体的数据源对象`KronosDataSourceWrapper`
> 来调用。

## 1. {{ $.title("table.exists") }} 表是否存在

- ### {{ $.title("table.exists(KPojo)") }}

  _通过实体类判断表是否存在_

 {{ $.hr() }}

  ```kotlin
  dataSource.table.exists(user)
  ```

- ### {{ $.title("table.exists<KPojo>()") }}

  通过泛型判断表是否存在

   ```kotlin
   dataSource.table.exists<User>()
   ```

- ### {{ $.title("table.exists(vararg String)") }}

  通过表名判断表是否存在

   ```kotlin
   dataSource.table.exists("user")
   ```

## 2. {{ $.title("table.createTable") }} 创建表

- ### {{ $.title("table.createTable(KPojo)") }}

  通过实体类创建表

   ```kotlin
   dataSource.table.createTable(user)
   ```

- ### {{ $.title("table.createTable<KPojo>()") }}

  通过泛型创建表

   ```kotlin
   dataSource.table.createTable<User>()
   ```

## 3. {{ $.title("table.truncateTable") }} 清空表

- ### {{ $.title("table.truncateTable(KPojo)") }}

  通过实体类清空表

   ```kotlin
   dataSource.table.truncateTable(user)
   ```

- ### {{ $.title("table.truncateTable<KPojo>()") }}

  通过泛型清空表

   ```kotlin
   dataSource.table.truncateTable<User>()
   ```

- ### {{ $.title("table.truncateTable(vararg String)") }}

  通过表名清空表

   ```kotlin
   dataSource.table.truncateTable("user")
   ```

## 4. {{ $.title("table.dropTable") }} 删除表

- ### {{ $.title("table.dropTable(KPojo)") }}

  通过实体类删除表

   ```kotlin
   dataSource.table.dropTable(user)
   ```

- ### {{ $.title("table.dropTable<KPojo>()") }}

  通过泛型删除表

    ```kotlin
    dataSource.table.dropTable<User>()
    ```

- ### {{ $.title("table.dropTable(vararg String)") }}

  通过表名删除表

    ```kotlin
    dataSource.table.dropTable("user")
    ```

## 5. {{ $.title("table.syncTable") }} 表结构同步和变更

- ### {{ $.title("table.syncTable(KPojo)") }}

  通过实体类同步表结构

   ```kotlin
   dataSource.table.syncTable(user)
   ```

- ### {{ $.title("table.syncTable<KPojo>()") }}

  通过泛型同步表结构

     ```kotlin
     dataSource.table.syncTable<User>()
     ```

## 6. 动态建表

部分情况下会存在动态建表的需求，此时可以通过`getTableCreateSqlList`方法动态获取建表语句并执行。

**参数**：
{{$.params([
['dbType', '数据库类型', 'DBType'],
['tableName', '表名', 'String'],
['fields', '字段列表', 'List<Field>'],
['indexes', '索引列表', 'List<KTableIndex>', '[]']
])}}

```kotlin name="demo" icon="kotlin" {2,31}
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

> **Warning**
> 若需要对同一个实体对象连续执行多个数据库操作，建议不要使用泛型，以避免多次创建KPojo对象，产生不必要的开销。
