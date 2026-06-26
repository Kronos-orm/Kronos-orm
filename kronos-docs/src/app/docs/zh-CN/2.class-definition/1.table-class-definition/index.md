{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 声明数据表类

在Kronos中声明一个class为数据表类非常简单，只需要让该类继承`KPojo`即可，以下是一个简单示例：

```kotlin
import com.kotlinorm.interface.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

在Kronos中，我们将这样的类称为`KPojo`，它是一个标记接口，用于标记一个类为Kronos数据表类进行泛型操作。同时，kronos会在编译期为类添加一些属性和方法，以便高效地读取数据表信息。

我们，推荐将类声明为`data class`并为其添加**默认值**，这样可以使数据类在ORM操作时更加灵活。

注意，`var`关键词声明的的属性才能在**update**或**upsert**的`set`中修改。

### 表注释

Kronos支持为表添加注释，我们通过编译器插件读取您在类定义上的注释并在创建/同步表时添加到表的备注上，如：

```kotlin
// 添加表注释
// 支持多行
data class User

data class User( // 添加表注释
    val id: Int? = null,
)

/* 添加表注释 */
data class User

/*
* 添加表注释
* 支持多行
前面可以不加“*”
*/
@Table("table_name")
// 注解所在行将会被跳过
data class User
```
类注释可以读取在类同一行的注释，如果没有定义在同一行的注释则会读取前面的注释（注解所在行不会被读取）。

### 列注释

Kronos支持为列添加注释，我们通过编译器插件读取您在属性定义上的注释并在创建/同步表时添加到列的备注上，如：

我们支持以下4种注释：

```kotlin
import com.kotlinorm.interface.KPojo

data class User(
    // 添加列注释
    // 支持多行
    var property1: Int? = null,
    var property2: String? = null, // 添加列注释
    /* 添加列注释 */
    var property3: Int? = null,
    /* 
    * 添加列注释
    * 支持多行
    前面可以不加“*”
    */
    var property3: Int? = null,
    var property4: String? = null /* 添加列注释 */
) : KPojo
```

通常来说定义在属性同一行的注释会被优先读取，如果没有定义在同一行的注释则会读取前面的注释。

### 使用委托实现级联多对多跨中间表关系

请参考 {{ $.keyword("advanced/cascade-definition", ["进阶用法", "使用委托实现级联多对多跨中间表关系"]) }}。

### 使用委托代理序列化反序列化属性

Kronos的序列化反序列化功能支持自动处理和委托处理两种方式，委托处理方式需要在class内声明，形如：

```kotlin
data class User(
    val id: Int? = null,
    // 需要序列化反序列化的属性
    val listStr: String? = null
) : KPojo {
    // 委托序列化反序列化属性
    var list: List<String>? by serialize(::listStr)
}
```
### 使用匿名类创建对象进行数据库操作

在Kronos中，我们可以通过创建匿名类对象灵活地进行数据库操作，如：

```kotlin
val obj = @Table("tb_name") object : KPojo { @PrimaryKey(true) var id = 1 }

if(!dataSource.table.exists(obj)) {
    dataSource.table.createTable(obj)
}

obj.insert().execute()
```

## 使用注解设置表属性

在Kronos中，我们可以通过注解来设置表的属性，如：**表名**、**列名**、**列类型**、**列长度**、**主键**、**自增**、**唯一键**、**索引**、**默认值**、**非空**等。
Kronos还提供了**级联**、**序列化/反序列化**、**日期格式化**、**逻辑删除**、**创建时间**、**更新时间**、**乐观锁等**功能。

以下是一个示例，包含了一些常用的注解：

```kotlin group="KPojo" name="User.kt" icon="kotlin"
// 表名设置，如果不设置则将使用[表名策略]根据类名生成表名
@Table(name = "tb_user")
@TableIndex("idx_username", ["name"], "UNIQUE") // 为name字段添加唯一键
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE") // 为id和name字段添加唯一键
data class User(
    // 设置id为主键，自增
    @PrimaryKey(identity = true)
    var id: Int? = null,

    // 设置列名为name，如果不设置则使用[列名策略]根据属性名生成列名
    // 设置name字段为非空
    // 设置列类型为VARCHAR，长度为128
    @Column("name")
    @Necessary
    @ColumnType(VARCHAR, 128)
    var username: String? = null,

    // 设置列类型为TINYINT
    // 设置默认值为0
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,

    // 设置companyId字段为非空
    @Necessary
    var companyId: Int? = null,

    // 级联设置，无需实体外键，通过companyId关联Company表的id
    @Cascade(["companyId"], ["id"])
    var company: Company? = null,

    // 设置createTime为创建时间字段
    // 设置时间格式为字符串的格式
    @CreateTime
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    var createTime: String? = null,

    // 设置updateTime为更新时间字段
    @UpdateTime
    var updateTime: LocalDateTime? = null,

    // 设置deleted为逻辑删除字段
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

点击{{ $.keyword("class-definition/annotation-config", ["注解配置"]) }}查看更多注解设置。
