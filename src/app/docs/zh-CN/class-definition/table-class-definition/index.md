# {{ NgDocPage.title }}

在Kronos中声明一个data class为数据表类非常简单，只需要让该类继承`KPojo`即可，以下是一个简单示例：

```kotlin
import com.kotlinorm.beans.dsl.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

复杂数据类及使用数据库表操作时在不同数据库中建表语句示例：

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

我们通过一些注解和配置项来定义数据表的属性，如：主键、自增、唯一键、索引等。

> **Note**
> 为什么使用注解而不是kotlin dsl？
>
> 我们通过编译器插件读取注解信息并将信息保存在实体类定义中，使全部的表结构解析都发生在**编译期**而不是运行期，这样虽然失去了部分灵活性，但是可以避免运行时的性能损耗。


## 注解配置项

### 表名

`@Table(name: String)`

用于指定数据表的表名，如果不指定则使用默认的表名策略。

**参数**：

- name `string`：表名

```kotlin
@Table("tb_user")
data class User(
    val id: Int,
    val name: String,
    val age: Int
) : KPojo
```

### 表索引

`@TableIndex(name: String, columns: Array<String>, type: String, method: String)`

用于指定数据表的索引，如果不指定则不创建索引。

> 仅在使用`tables.create<Table>()`或`tables.syncStructure<Table>()`时生效。

**参数**：

- name `string`：索引名
- columns `Array<String>`：索引列名
- type `String`：[索引类型](/documentation/class-definition/table-index#hash)（可选）
- method `String`：[索引方法](/documentation/class-definition/table-index#hash)（可选）

```kotlin
@TableIndex("idx_name_age", ["name", "age"], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)
data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

### 表创建时间

`@CreateTime(enabled: Boolean)`

用于指定数据表是否开启创建时间策略，如果不指定则使用全局设置。

**参数**：

- enabled `Boolean`：是否开启

```kotlin
@CreateTime(enabled = false)
data class User(
    val id: Int? = null,
    val createTime: String? = null
) : KPojo
```

### 表更新时间

`@UpdateTime(enabled: Boolean)`

用于指定数据表是否开启更新时间策略，如果不指定则使用全局设置。

**参数**：

- enabled `Boolean`：是否开启

```kotlin
@UpdateTime(enabled = false)
data class User(
    val id: Int? = null,
    val updateTime: String? = null
) : KPojo
```

### 表逻辑删除

`@LogicDelete(enabled: Boolean)`

用于指定数据表是否开启逻辑删除策略，如果不指定则使用全局设置。

**参数**：

- enabled `Boolean`：是否开启

```kotlin
@LogicDelete(enabled = false)
data class User(
    val id: Int? = null,
    val deleted: Boolean? = null
) : KPojo
```

### 列名设置

`@Column(name: String)`

用于指定数据表的列名，如果不指定则使用默认的列名策略。

**参数**：

- name `String`：列名

```kotlin
data class User(
    @Column("user_name")
    val name: String? = null
) : KPojo
```

### 列日期格式化

`@DateTimeFormat(pattern: String)`

用于指定数据表的日期/时间格式，如果不指定则使用默认的日期/时间格式。

**参数**：

- pattern `String`：日期/时间格式

```kotlin
data class User(
    @DateTimeFormat("yyyy-MM-dd")
    val birthday: String? = null
) : KPojo
```

### 列反序列化设置

`@ColumnDeserialize`

用于声明该列是否需要反序列化，如果不指定则默认不反序列化，若启用反序列化，将调用`Kronos.serializeResolver.deserialize`方法将该列的值反序列化为指定类型。

```kotlin
data class User(
    @ColumnDeserialize
    val info: List<String>? = emptyList()
) : KPojo
```

### 级联关系声明

`@Cascade(properties: String[], targetProps: String[],`
`onDelete: CascadeDeleteAction, defaultValue: String)`

此注解用于声明列的级联设置，用于**级联查询**、**级联插入**、**级联更新**、**级联删除**等。支持**一对一**、**一对多**、**多对多**关联。

类型为`KPojo`或`Collection<KPojo>`，kronos将不会将该列识别为数据库表列。

kronos的关联功能**无需定义外键**，只需在实体类中定义关联关系即可实现关联查询、关联插入、关联更新、关联删除等操作。

**参数**：

- properties `Array<String>`：本表的`KPojo`属性名，如以下示例中`companyId`用于关联`Company`实体。
- targetFields `Array<String>`：关联目标表`KPojo`属性名，如以下示例中`companyId`关联到`Company`的`id`属性。
- onDelete `CascadeDeleteAction`
  ：关联删除策略（可选，详见：([级联删除操作选项](/documentation/zh-CN/class-definition/cascade-delete-action))
  ，包括`CASCADE`、`RISTRICT`、`SET_DEFAULT`、`SET_NULL`、`NO_ACTION`等，默认为`NO_ACTION`无操作）
- defaultValue `Array<String>`：指定级联删除方式为"SET DEFAULT"时设置的默认值（可选）
- usage `Array<KOperationType>`: 用于声明本实体需要用到的关联操作（可选，默认为`[Insert, Update, Delete, Upsert, Select]`）

```kotlin
@Table("tb_user")
data class Employee(
    val id: Int? = null,
    val companyId: Int? = null,
    @Cascade(["companyId"], ["id"], SET_DEFAULT, ["0"])
    val company: Company? = null
): KPojo

@Table("tb_company")
data class Company(
    val id: Int? = null,
    val employees: List<Employee>? = null
): KPojo
```

### 列主键设置

`@PrimaryKey(identity: Boolean)`

此注解用于声明列为主键。

**参数**：

- identity `Boolean`：是否自增

```kotlin
@Table("tb_user")
data class User(
    @PrimaryKey(identity = true)
    val id: Int? = null
): KPojo
```

### 列类型及长度

`@ColumnType(type: String, length: Int)`

对于不同的数据库类型，kronos会根据kotlin类型自动转换类型，您可以参考[Kotlin列类型推断](/documentation/class-definition/kotlin-type-to-kcolumn-type)查看Kotlin数据类型在各个数据库中的映射关系。
您可以通过此注解声明列类型及长度，如果不指定则使用默认的类型及长度，全部类型信息请参考：[Kronos列类型](/documentation/class-definition/kcolumn-type)

**参数**：

- type `String`：类型
- length `Int`：长度

```kotlin
@Table("tb_user")
data class User(
    @ColumnType(KColumnType.Char, 10)
    val name: String? = null
): KPojo
```

### 列非空约束

`@NotNull`

此注解用于声明列为非空，如果不指定则使用默认的非空约束

```kotlin
@Table("tb_user")
data class User(
    @NotNull
    val name: String? = null
): KPojo
```

### 列创建时间

`@CreateTime`

此注解用于声明列为创建时间字段，如果不指定则使用默认的创建时间策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @CreateTime
    val created: String? = null
): KPojo
```

### 列更新时间

`@UpdateTime`

此注解用于声明列为更新时间字段，如果不指定则使用默认的更新时间策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @UpdateTime
    val updated: String? = null
): KPojo
```

### 列逻辑删除

`@LogicDelete`

此注解用于声明列为逻辑删除字段，如果不指定则使用默认的逻辑删除策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @LogicDelete
    val deleted: String? = null
): KPojo
```

### 列乐观锁

`@LogicDelete`

此注解用于声明列为乐观锁字段，如果不指定则使用默认的乐观锁策略。

**参数**：

- enabled `Boolean`：是否启用

```kotlin
@Table("tb_user")
data class User(
    @Version
    val version: Int? = null
): KPojo
```
