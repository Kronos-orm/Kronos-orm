# {{ NgDocPage.title }}

相比于kotoframework, Kronos是一个Code-First的ORM框架，它增加了对于数据库表结构的操作。

相关功能可可以通过**Kronos.dataSource**(`() -> KronosDataSourceWrapper`)或某个具体的数据源对象(`KronosDataSourceWrapper`)来调用。

## 1. 表是否存在

通过`exists`方法，可以判断表是否存在，返回`true`表示存在，`false`表示不存在。

```kotlin name="demo" icon="kotlin"
val exists: Boolean = db.table.exists(user)
// 或
val exists: Boolean = db.table.exists<User>()
// 或
val exists: Boolean = db.table.exists("user")
```

## 2. 创建表

通过`createTable`方法，可以创建表。

```kotlin name="demo" icon="kotlin"
db.table.createTable(user)
// 或
db.table.createTable<User>()
```

## 3. 删除表

通过`dropTable`方法，可以删除表。

```kotlin name="demo" icon="kotlin"
db.table.dropTable(user)
// 或
db.table.dropTable<User>()
// 或
db.table.dropTable("user")
```

## 4. 表结构同步和变更

通过`syncSchema`方法，可以根据实体类的定义，自动同步表结构。

```kotlin name="demo" icon="kotlin"
db.table.syncSchema(user)
// 或
db.table.syncSchema<User>()
```

## 5. 动态建表

部分情况下会存在动态建表的需求，此时可以通过`getTableCreateSqlList`方法动态获取建表语句并执行。

`getTableCreateSqlList`接收的参数包括：
 - `dbType`：`DBType` 数据库类型
 - `tableName`：`String` 表名
 - `fields`：`List<Field>` 字段列表
 - `indexes`：`List<KTableIndex>` 索引列表

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
