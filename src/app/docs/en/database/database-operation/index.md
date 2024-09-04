Compared to kotoframework, Kronos is a Code-First ORM framework that adds operations on database table structures.

Related functions can be called through **Kronos.dataSource**(`() -> KronosDataSourceWrapper`) or a specific data source object (`KronosDataSourceWrapper`).

## 1. Check if the table exists

Use the `exists` method to check if the table exists, which returns a Boolean value.

```kotlin name="demo" icon="kotlin"
val exists: Boolean = db.table.exists(user)
// or
val exists: Boolean = db.table.exists<User>()
// or
val exists: Boolean = db.table.exists("user")
```

## 2. Create a table

Use the `createTable` method to create a table.

```kotlin name="demo" icon="kotlin"
db.table.createTable(user)
// or
db.table.createTable<User>()
```

## 3. Delete a table

Use the `dropTable` method to delete a table.

```kotlin name="demo" icon="kotlin"
db.table.dropTable(user)
// or
db.table.dropTable<User>()
// or
db.table.dropTable("user")
```

## 4. Synchronize table structure

Use the `syncSchema` method to synchronize the table structure.

```kotlin name="demo" icon="kotlin"
db.table.syncSchema(user)
// or
db.table.syncSchema<User>()
```

## 5. Dynamic table creation

The `getTableCreateSqlList` method is used to dynamically generate SQL statements for creating tables. The parameters received by `getTableCreateSqlList` include:
 - `dbType`：`DBType` Database type
 - `tableName`：`String` Table name
 - `fields`：`List<Field>` Field list
 - `indexes`：`List<KTableIndex>` Index list

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
> If you need to perform multiple database operations on the same entity object in succession, it is recommended not to use generics to avoid creating KPojo objects multiple times and incurring unnecessary overhead.
