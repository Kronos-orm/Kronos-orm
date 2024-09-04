Declaring a data class as a table class in Kronos is very simple. You just need to make the class inherit from `KPojo`. Here is a simple example:

```kotlin
import com.kotlinorm.beans.dsl.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

For more complex data classes and when working with database table operations, here are examples of table creation statements in different databases:

```kotlin group="KPojo" name="User.kt" icon="kotlin"
@Table(name = "tb_user")
@TableIndex("idx_username", ["name"], "UNIQUE")
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE")
data class User(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("name")
    @NotNull
    var username: String? = null,
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,
    @NotNull
    var companyId: Int? = null,
    @Reference(["companyId"], ["id"])
    var company: Company? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo
```

```sql group="KPojo" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `age` TINYINT DEFAULT 0,
  `create_time` VARCHAR(255),
  `companyId` INT NOT NULL,
  `update_time` DATETIME,
  `deleted` TINYINT
);
CREATE UNIQUE INDEX idx_username ON `tb_user` (`name`) USING BTREE;
CREATE UNIQUE INDEX idx_multi ON `tb_user` (`id`,`name`) USING BTREE
```

```sql group="KPojo" name="Sqlite" icon="sqlite"
CREATE TABLE IF NOT EXISTS "tb_user" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "name" TEXT NOT NULL,
  "age" INTEGER DEFAULT 0,
  "create_time" TEXT,
  "companyId" INTEGER NOT NULL,
  "update_time" TEXT,"deleted" INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX IF NOT EXISTS idx_multi ON "tb_user" ("id", "name");
```

```sql group="KPojo" name="PostgreSql" icon="postgres"
CREATE TABLE IF NOT EXISTS "tb_user" (
  "id" SERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "age" SMALLINT DEFAULT 0,
  "create_time" VARCHAR(255),
  "companyId" INTEGER NOT NULL,
  "update_time" TIMESTAMP,
  "deleted" SMALLINT
);
CREATE UNIQUE INDEX idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX idx_multi ON "tb_user" ("id", "name");

```

```sql group="KPojo" name="Mssql" icon="sqlserver"
IF NOT EXISTS (
  SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[tb_user]') AND type in (N'U')
) 
BEGIN 
  CREATE TABLE [dbo].[tb_user](
    [id] INT NOT NULL PRIMARY KEY IDENTITY,
    [name] VARCHAR(255) NOT NULL,
    [age] TINYINT DEFAULT 0,
    [create_time] VARCHAR(255),
    [companyId] INT NOT NULL,
    [update_time] DATETIME,
    [deleted] TINYINT
  );
END;
CREATE  UNIQUE INDEX [idx_username] ON [dbo].[tb_user] ([name]);
CREATE  UNIQUE INDEX [idx_multi] ON [dbo].[tb_user] ([id],[name]);
```

```sql group="KPojo" name="Oracle" icon="oracle"
CREATE TABLE "tb_user" (
  "id" NUMBER(11) GENERATED ALWAYS AS IDENTITY NOT NULL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "age" NUMBER(3) DEFAULT 0,
  "create_time" VARCHAR(255),
  "companyId" NUMBER(11) NOT NULL,
  "update_time" DATE,
  "deleted" NUMBER(3)
);
CREATE UNIQUE INDEX idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX idx_multi ON "tb_user" ("id", "name");
```

We define the properties of the data table through annotations and configuration options, such as primary keys, auto-increment, unique keys, and indexes.

> **Note**
> Why use annotations instead of Kotlin DSL?
>
> We read annotation information through a compiler plugin and store the information in the entity class definition, so that all table structure parsing occurs at **compile time** rather than runtime. While this approach sacrifices some flexibility, it helps avoid runtime performance overhead.

## Global Configuration Options

### Global Table Naming Strategy

| Parameter               | Type                   | Default Value          |
|------------------------|------------------------|------------------------|
| `tableNamingStrategy`  | `KronosNamingStrategy` | `NoneNamingStrategy`   |

You can create a custom table naming strategy by implementing a class that extends `KronosNamingStrategy` (see: [Naming Strategy](/documentation/en/class-definition/naming-strategy)), and then specify this implementation in the configuration file.

We provide a default `LineHumpNamingStrategy` for table naming:

This strategy converts Kotlin class names to lowercase strings separated by underscores, e.g., `ADataClass` -> `a_data_class`, and converts database table/column names to camel case, e.g., `user_name` -> `userName`.

```kotlin
Kronos.tableNamingStrategy = LineHumpNamingStrategy
```

### Global Field Naming Strategy

| Parameter               | Type                   | Default Value          |
|------------------------|------------------------|------------------------|
| `fieldNamingStrategy`  | `KronosNamingStrategy` | `NoneNamingStrategy`   |

Similar to the table naming strategy, you can create a custom field naming strategy by implementing a class that extends `KronosNamingStrategy`, and then specify this implementation in the configuration file.

We also provide the `LineHumpNamingStrategy` as the default field naming strategy.

```kotlin
Kronos.fieldNamingStrategy = LineHumpNamingStrategy
```

### Create Time Strategy

| Parameter              | Type                    | Default Value                                                                      |
|-----------------------|-------------------------|----------------------------------------------------------------------------------|
| `createTimeStrategy`  | `KronosCommonStrategy`  | `KronosCommonStrategy(false, "create_time", "createTime")`                        |

You can customize the create time strategy by creating a class that implements `KronosCommonStrategy` (see: [Common Strategy](/documentation/en/class-definition/common-strategy)), and then specify this implementation in the configuration file.

The create time strategy is globally disabled by default and needs to be manually enabled.

```kotlin
Kronos.createTimeStrategy = KronosCommonStrategy(true, Field("create_time", "createTime"))
```

Even after setting the global create time strategy, you can still override it in a `KPojo` class using the `@CreateTime` annotation.

### Update Time Strategy

| Parameter              | Type                    | Default Value                                                                      |
|-----------------------|-------------------------|----------------------------------------------------------------------------------|
| `updateTimeStrategy`  | `KronosCommonStrategy`  | `KronosCommonStrategy(false, "update_time", "updateTime")`                        |

Similar to the create time strategy, you can customize the update time strategy by creating a class that implements `KronosCommonStrategy`, and then specify this implementation in the configuration file.

The update time strategy is globally disabled by default and needs to be manually enabled.

```kotlin
Kronos.updateTimeStrategy = KronosCommonStrategy(true, Field("update_time", "updateTime"))
```

Even after setting the global update time strategy, you can still override it in a `KPojo` class using the `@UpdateTime` annotation.

### Logic Delete Strategy

| Parameter               | Type                    | Default Value                                   |
|------------------------|-------------------------|-------------------------------------------------|
| `logicDeleteStrategy`  | `KronosCommonStrategy`  | `KronosCommonStrategy(false, "deleted")`         |

You can customize the logic delete strategy by creating a class that implements `KronosCommonStrategy`, and then specify this implementation in the configuration file.

The logic delete strategy is globally disabled by default and needs to be manually enabled.

```kotlin
Kronos.logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
```

Even after setting the global logic delete strategy, you can still override it in a `KPojo` class using the `@LogicDelete` annotation.

### Optimistic Lock Strategy

| Parameter                   | Type                    | Default Value                                   |
|---------------------------|-------------------------|-------------------------------------------------|
| `optimisticLockStrategy`  | `KronosCommonStrategy`  | `KronosCommonStrategy(false, "version")`         |

You can customize the optimistic lock strategy by creating a class that implements `KronosCommonStrategy`, and then specify this implementation in the configuration file.

You can also configure each entity object individually using [Column Optimistic Lock](/documentation/en/class-definition/table-class-definition#column-optimistic-lock).

The optimistic lock strategy is globally disabled by default and needs to be manually enabled.

```kotlin
Kronos.optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
```

Even after setting the global optimistic lock strategy, you can still override it in a `KPojo` class using the `@Version` annotation.

### Default Date/Time Format

| Parameter             | Type       | Default Value            |
|---------------------|------------|--------------------------|
| `defaultDateFormat`  | `String`   | `yyyy-MM-dd HH:mm:ss`    |

Kronos uses the `yyyy-MM-dd HH:mm:ss` format by default for dates/times. You can modify the default format as follows:

```kotlin
Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

### Default Timezone

| Parameter           | Type                        | Default Value                  |
|-------------------|---------------------------|-------------------------------|
| `defaultTimeZone`  | `kotlinx.datetime.TimeZone` | `currentSystemDefault()`      |

Kronos uses the current system timezone by default. You can modify the default timezone as follows:

```kotlin
Kronos.defaultTimeZone = TimeZone.UTC
Kronos.defaultTimeZone = TimeZone.of("Asia/Shanghai")
Kronos.defaultTimeZone = TimeZone.currentSystemDefault()
Kronos.defaultTimeZone = TimeZone.of("GMT+8")
```

### Serialization Resolver

| Parameter             | Type                      | Default Value            |
|---------------------|-------------------------|--------------------------|
| `serializeResolver`  | `KronosSerializeResolver` | `NoneSerializeResolver`  |

You can create a custom serialization resolver by implementing a class that extends `KronosSerializeResolver`, and then specify this implementation in the configuration file.

For example, you can use the `GSON` library to implement a serialization resolver:

```kotlin group="GsonResolver" name="GsonResolver.kt" icon="kotlin"
object GsonResolver : KronosSerializeResolver {
    override fun <T> deserialize(serializedStr: String, kClass: KClass<*>): T {
        return Gson().fromJson<T>(serializedStr, kClass.java)
    }

    override fun deserializeObj(serializedStr: String, kClass: KClass<*>): Any {
        return Gson().fromJson(serializedStr, kClass.java)
    }

    override fun serialize(obj: Any): String {
        return Gson().toJson(obj)
    }
}
```

```kotlin group="GsonResolver" name="KronosConfig.kt" icon="kotlin"
Kronos.serializeResolver = GsonResolver
```

In this example, we use the `GSON` library to implement a serialization resolver. You can use any library such as `Kotlinx.serialization`, `Jackson`, `Moshi`, `FastJson`, etc.

## Annotation Configuration Options

### Table Name

`@Table(name: String)`

Specifies the table name for a data table. If not specified, the default table naming strategy will be used.

**Parameters**:

- name `string`: Table name

```kotlin
@Table("tb_user")
data class User(
    val id: Int,
    val name: String,
    val age: Int
) : KPojo
```

### Table Index

`@TableIndex(name: String, columns: Array<String>, type: String, method: String)`

Specifies an index for the data table. If not specified, no index will be created.

> Only effective when using `tables.create<Table>()` or `tables.syncStructure<Table>()`.

**Parameters**:

- name `string`: Index name
- columns `Array<String>`: Index column names
- type `String`: [Index type](/documentation/en/class-definition/table-index#hash) (optional)
- method `String`: [Index method](/documentation/en/class-definition/table-index#hash) (optional)

```kotlin
@TableIndex("idx_name_age", ["name", "age"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

### Table Creation Time

`@CreateTime(enabled: Boolean)`

Used to specify whether the data table enables the creation time strategy. If not specified, the global setting will be used.

**Parameters**:

- enabled `Boolean`: Whether to enable

```kotlin
@CreateTime(enabled = false)
data class User(
    val id: Int? = null,
    val createTime: String? = null
) : KPojo
```

### Table Update Time

`@UpdateTime(enabled: Boolean)`

Used to specify whether the data table enables the update time strategy. If not specified, the global setting will be used.

**Parameters**:

- enabled `Boolean`: Whether to enable

```kotlin
@UpdateTime(enabled = false)
data class User(
    val id: Int? = null,
    val updateTime: String? = null
) : KPojo
```

### Table Logical Deletion

`@LogicDelete(enabled: Boolean)`

Used to specify whether the data table enables the logical deletion strategy. If not specified, the global setting will be used.

**Parameters**:

- enabled `Boolean`: Whether to enable

```kotlin
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null,
    val deleted: Boolean? = null
) : KPojo
```

### Column Name Setting

`@Column(name: String)`

Used to specify the column name of the data table. If not specified, the default column name strategy will be used.

**Parameters**:

- name `String`: Column name

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

### Column Date Formatting

`@DateTimeFormat(pattern: String)`

Used to specify the date/time format of the data table. If not specified, the default date/time format will be used.

**Parameters**:

- pattern `String`: Date/time format

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

### Column Deserialization Setting

`@ColumnDeserialize`

Used to declare whether the column needs to be deserialized. If not specified, deserialization will be disabled by default. If enabled, the `Kronos.serializeResolver.deserialize` method will be called to deserialize the value of the column into the specified type.

```kotlin
data class User(
    @ColumnDeserialize
    val info: List<String>? = emptyList()
) : KPojo
```

### Column Association Setting

`@Reference(reference: String[], target: String[], onDelete: CascadeDeleteAction, defaultValue: String, mapperBy: String)`

This annotation is used to declare column associations, including association queries, association inserts, association updates, association deletions, etc. It supports one-to-one, one-to-many, many-to-one, and many-to-many associations.

Kronos treats associated columns as custom properties and does not recognize them as database fields.

The association feature in Kronos **does not require defining foreign keys**. Only defining the association relationship in the entity class is needed to achieve association queries, association inserts, association updates, association deletions, etc.

**Parameters**:

- referenceFields `Array<String>`: Associated property names
- targetFields `Array<String>`: Associated target table property names
- onDelete `CascadeDeleteAction`: Associated deletion strategy (optional, default is no action)
- defaultValue `Array<String>`: Default values to be set when the cascade delete action is "SET DEFAULT" (optional)
- mapperBy `Array<String>`: Used to specify the maintenance end of this association relationship (empty means the maintenance end is the current entity; it cannot be empty when both ends have this annotation)
- usage `Array<KOperationType>`: Used to declare the association operations needed for this entity (optional, default is `[Insert, Update, Delete, Upsert, Select]`)

```kotlin
@Table("tb_user")
data class Employee(
    val id: Int? = null,
    val companyId: Int? = null,
    @Reference(["companyId"], ["id"], SET_DEFAULT, ["0"])
    val company: Company? = null
): KPojo

@Table("tb_company")
data class Company(
    val id: Int? = null,
    val employees: List<Employee>? = null
): KPojo
```

### Column Primary Key Setting

`@PrimaryKey(identity: Boolean)`

This annotation is used to declare a column as the primary key.

**Parameters**:

- identity `Boolean`: Whether it is auto-incremented

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
): KPojo
```

### Column Type and Length

`@ColumnType(type: String, length: Int)`

For different database types, Kronos will automatically convert the Kotlin types based on the Kotlin type's mapping to the database types. You can refer to [Kotlin Type to KColumn Type](/documentation/en/class-definition/kotlin-type-to-kcolumn-type) for the mapping relationship between Kotlin data types and database types.
You can use this annotation to declare the column type and length. If not specified, the default type and length will be used. Please refer to [Kronos Column Types](/documentation/en/class-definition/kcolumn-type) for all type information.

**Parameters**:

- type `String`: Type
- length `Int`: Length

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
): KPojo
```

### Column Not Null Constraint

`@NotNull`

This annotation is used to declare a column as not null. If not specified, the default not null constraint will be used.

```kotlin
@Table("tb_user")
data class User(
    @NotNull
    val name: String? = null
): KPojo
```

### Column Creation Time

`@CreateTime`

This annotation is used to declare a column as the creation time field. If not specified, the default creation time strategy will be used.

**Parameters**:

- enabled `Boolean`: Whether it is enabled

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
): KPojo
```

### Column Update Time

`@UpdateTime`

This annotation is used to declare a column as the update time field. If not specified, the default update time strategy will be used.

**Parameters**:

- enabled `Boolean`: Whether it is enabled

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
): KPojo
```

### Column Logical Deletion

`@LogicDelete`

This annotation is used to declare a column as the logical deletion field. If not specified, the default logical deletion strategy will be used.

**Parameters**:

- enabled `Boolean`: Whether it is enabled

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
): KPojo
```

### Column Optimistic Lock

`@LogicDelete`

This annotation is used to declare a column as the optimistic lock field. If not specified, the default optimistic lock strategy will be used.

**Parameters**:

- enabled `Boolean`: Whether it is enabled

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
): KPojo
```
