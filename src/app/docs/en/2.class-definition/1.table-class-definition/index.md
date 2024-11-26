{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Declaring Data Table Classes

Declaring a class as a datasheet class in Kronos is very simple, just make the class inherit from `KPojo`, here is a simple example:

```kotlin
import com.kotlinorm.interface.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

In Kronos, we call such a class `KPojo`, which is a markup interface for marking a class as a Kronos data table class for generic operations. At the same time, kronos adds some properties and methods to the class at compile time to read data table information efficiently.

We, recommend declaring the class as `data class` and adding **defaults** to it to make the data class more flexible for ORM operations.

Only **the properties declared with the `var` keyword** can be modified in the `set` of **update** or **upsert**.

### Table Comments

Kronos supports adding comments to tables, we read your comments on class definitions via the compiler plugin and add them to the table notes when creating/synchronizing the table, for example:

```kotlin
// Adding Table Comments
// Supports multiline
data class User

data class User( // Adding Table Comments
    val id: Int? = null,
)

/* Adding Table Comments */
data class User

/*
* Adding Table Comments
* Supports multiline
It is allowed not to add "*" at the front.
*/
@Table("table_name")
// The line where the Annotation is located will be skipped
data class User
```
Class annotations can read comments on the same line as the class; if there are no comments defined on the same line, it will read the comments from the preceding lines (the line where the annotation is located will not be read).

### Column Comments

Kronos supports adding comments to columns, we read your comments on attribute definitions via the compiler plugin and add them to the column notes when creating/synchronizing the table, for example:

```kotlin
import com.kotlinorm.interface.KPojo

data class User(
    // Adding Column Comments
    // Supports multiline
    var property1: Int? = null,
    var property2: String? = null, // Adding Column Comments
    /* Adding Column Comments */
    var property3: Int? = null,
    /* 
    * Adding Column Comments
    * Supports multiline
    It is allowed not to add "*" at the front.
    */
    var property3: Int? = null,
    var property4: String? = null /* Adding Column Comments */
) : KPojo
```

Normally, comments defined on the same line of an attribute are read first, and if there are no comments defined on the same line, the previous comments are read.

### Using Delegates to Implement Cascading Many-to-Many Relationships Across Intermediate Tables

Please refer to {{ $.keyword("advanced/cascade-definition", ["Advanced Usage", "Using Delegation to Achieve Cascading Many-to-Many Cross Intermediate Table Relationships"]) }}.

### Using delegate proxy for serialization and deserialization of properties.

Kronos' serialization and deserialization functions support both automatic processing and delegated processing methods. The delegated processing method needs to be declared within the class, in the following form:

```kotlin
data class User(
    val id: Int? = null,
    // Properties that need to be serialized deserialized
    val listStr: String? = null
) : KPojo {
    // Proxy Serialization Deserialization Properties
    var list: List<String>? by serializable(::listStr)
}
```
### Creating objects for database operations using anonymous classes

In Kronos, we have the flexibility to perform database operations by creating anonymous class objects such as:

```kotlin
val obj = @Table("tb_name") object : KPojo { @PrimaryKey(true) var id = 1 }

if(!dataSource.table.exists(obj)) {
    dataSource.table.createTable(obj)
}

obj.insert().execute()
```

## Setting Table Properties with Annotations

In Kronos, we can set table attributes through annotations, such as **table name**, **column name**, **column type**, **column length**, **primary key**, **self-augmenting**, **unique key**, **index**, **default value**, **non-null**, and so on.
Kronos also provides **Cascade**, **Serialization/Deserialization**, **Date Formatting**, **Logical Deletion**, **Creation Time**, **Update Time**, **Optimistic Locking, and other **functionalities.

The following is an example with some commonly used annotations:

```kotlin group="KPojo" name="User.kt" icon="kotlin"
//Table name setting, if not set then [Table Name Policy] will be used to generate table name based on class name
@Table(name = "tb_user")
@TableIndex("idx_username", ["name"], "UNIQUE") // Adding a unique key to the `name` field
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE") // Adding a unique key to the `id` and `name` fields
data class User(
    // Setting the primary key, if not set, the primary key will be generated automatically
    @PrimaryKey(identity = true)
    var id: Int? = null,

    // Set the column name to `name`, instead of using Column Naming Strategy
    // Set the name field to non-null
    // Set the column type to VARCHAR and the length to 128
    @Column("name")
    @NotNull
    @ColumnType(VARCHAR, 128)
    var username: String? = null,

    // Set column type to TINYINT
    // Set the default value to 0
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,

    // Setting the companyId field to be non-null
    @NotNull
    var companyId: Int? = null,

    // Cascade setup, no need for entity foreign key, through companyId associated with the id of the Company table
    @Cascade(["companyId"], ["id"])
    var company: Company? = null,

    // Set `createTime` as the creation time field
    // Set the time format to the format of a string
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,

    // Set `updateTime` as the update time field
    @UpdateTime
    var updateTime: LocalDateTime? = null,

    // Set `deleted` as a logically deleted field
    @LogicDelete
    var deleted: Boolean? = null
) : KPojo
```

```sql group="KPojo" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_user` (
    `id` INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (128) NOT NULL,
    `age` TINYINT DEFAULT 0,
    `create_time` DATETIME,
    `companyId` INT NOT NULL,
    `update_time` DATETIME,
    `deleted` TINYINT
);
CREATE UNIQUE INDEX idx_username ON `tb_user` ( `name` ) USING BTREE;
CREATE UNIQUE INDEX idx_multi ON `tb_user` ( `id`, `name` ) USING BTREE
```

```sql group="KPojo" name="Sqlite" icon="sqlite"
CREATE TABLE IF NOT EXISTS "tb_user" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "name" TEXT NOT NULL,
    "age" INTEGER DEFAULT 0,
    "create_time" TEXT,
    "companyId" INTEGER NOT NULL,
    "update_time" TEXT,
    "deleted" INTEGER
 );
CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX IF NOT EXISTS idx_multi ON "tb_user" ("id","name");
```

```sql group="KPojo" name="PostgreSql" icon="postgres"
CREATE TABLE IF NOT EXISTS "tb_user"(
    "id" SERIAL NOT NULL PRIMARY KEY,
    "name" VARCHAR(128) NOT NULL,
    "age" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP,
    "companyId" INTEGER NOT NULL,
    "update_time" TIMESTAMP,
    "deleted" SMALLINT
);
CREATE UNIQUE INDEX idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX idx_multi ON "tb_user" ("id", "name");

```

```sql group="KPojo" name="Mssql" icon="sqlserver"
IF
NOT EXISTS (
  SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[tb_user]') AND type in (N'U')
)
BEGIN
CREATE TABLE [dbo].[tb_user] (
    [id] INT NOT NULL PRIMARY KEY IDENTITY,
    [NAME] VARCHAR (128) NOT NULL,
    [age] TINYINT DEFAULT 0,
    [create_time] DATETIME,
    [companyId] INT NOT NULL,
    [update_time] DATETIME,
    [deleted] TINYINT
    );
END;
CREATE UNIQUE INDEX [idx_username] ON [dbo].[tb_user] ([name]);
CREATE UNIQUE INDEX [idx_multi] ON [dbo].[tb_user] ([id],[name]);
```

```sql group="KPojo" name="Oracle" icon="oracle"
CREATE TABLE "tb_user"
(
    "id"          NUMBER(11) GENERATED ALWAYS AS IDENTITY NOT NULL PRIMARY KEY,
    "name"        VARCHAR(128) NOT NULL,
    "age"         NUMBER(3) DEFAULT 0,
    "create_time" DATE,
    "companyId"   NUMBER(11) NOT NULL,
    "update_time" DATE,
    "deleted"     NUMBER(3)
);
CREATE UNIQUE INDEX idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX idx_multi ON "tb_user" ("id", "name");
```

Click {{ $.keyword("class-definition/annotation-config", ["Annotation Config"]) }} to see more annotation settings.
