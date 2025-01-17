{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Creating a Table Data Class

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

Note that properties declared by the `var` keyword can only be modified in the `set` of **update** or **upsert**.

### Column annotations

Kronos supports adding comments to columns. We read the comments you make on attribute definitions through compiler plugins and add them to the column remarks when creating/synchronizing tables, such as:

We support the following 4 types of comments:

```kotlin
import com.kotlinorm.interface.KPojo

data class User(
    // Add column comments
    // Supports multiline
    var property1: Int? = null,
    var property2: String? = null, // Add column comments
    /* Add column comments */
    var property3: Int? = null,
    /* 
    * Add column comments
    * Supports multiline
    The asterisk "*" can be omitted at the beginning.
    */
    var property3: Int? = null,
    var property4: String? = null /* Add column comments */
) : KPojo
```

Generally speaking, comments defined on the same line as the attribute are read first, and if there are no comments defined on the same line, the preceding comments are read.

### Using delegates to implement cascading many-to-many relationships across intermediate tables

Please refer to {{ $.keyword("advanced/cascade-definition", ["Advanced Usage", "Implementing Cascade Many-to-Many Relationships Across Intermediate Tables Using Delegation"]) }}.

### Using delegates to serialize and deserialize properties.

Kronos' serialization and deserialization function supports two modes: automatic processing and delegate processing. The delegate processing mode needs to be declared within the class, in the form of:

```kotlin
data class User(
    val id: Int? = null,
    // The propertie that require serialization and deserialization
    val listStr: String? = null
) : KPojo {
    // Delegate serialization deserialization properties
    var list: List<String>? by serializable(::listStr)
}
```
### Creating objects for database operations using anonymous objects

In Kronos, we can flexibly perform database operations by creating anonymous class objects, such as:

```kotlin
val obj = @Table("tb_name") object : KPojo { @PrimaryKey(true) var id = 1 }

if(!dataSource.table.exists(obj)) {
    dataSource.table.createTable(obj)
}

obj.insert().execute()
```

## Setting Table Properties with Annotations

In Kronos, we can set table attributes through annotations, such as: **table name**, **column name**, **column type**, **column length**, **primary key**, **auto-increment**, **unique key**, **index**, **default value**, **not null**, and so on.

Kronos also provides features such as **cascading**, **serialization/deserialization**, **date formatting**, **logical deletion**, **creation time**, **update time**, **optimistic locking**, and so on.
The following is an example that includes some commonly used annotations:

```kotlin group="KPojo" name="User.kt" icon="kotlin"
// Table name settings. If not set, the table name will be generated based on the class name using [table name strategy].
@Table(name = "tb_user")
@TableIndex("idx_username", ["name"], "UNIQUE") // Add a unique key for the name field.
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE") // Add a unique key for the id and name fields.
data class User(
    // Set id as the primary key, auto-increment.
    @PrimaryKey(identity = true)
    var id: Int? = null,
    // Set the column name as name. If not set, the column name will be generated based on the property name using [column name strategy].
    // Set the name field as not null.
    // Set the column type to VARCHAR, length 128.
    @Column("name")
    @NotNull
    @ColumnType(VARCHAR, 128)
    var username: String? = null,
    // Set the column type to TINYINT.
    // Set the default value to 0.
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,
    // Set companyId field as not null.
    @NotNull
    var companyId: Int? = null,
    // Cascade settings, no need for entity foreign key, relate to the id of the Company table through companyId.
    @Cascade(["companyId"], ["id"])
    var company: Company? = null,
    // Set createTime as the creation time field.
    // Set the time format as a string format.
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,
    // Set updateTime as the update time field.
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    // Set deleted as the logical delete field.
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

Click {{ $.keyword("class-definition/annotation-config", ["annotation-config"]) }} to see more annotation settings.
