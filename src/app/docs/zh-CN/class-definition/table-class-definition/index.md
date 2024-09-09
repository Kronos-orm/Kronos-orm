{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

本文将指导您如何定义一个Kronos数据表类。

## 创建数据表类

在Kronos中声明一个class为数据表类非常简单，只需要让该类继承`KPojo`即可，以下是一个简单示例：

```kotlin
import com.kotlinorm.beans.dsl.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

在Kronos中，我们将这样的类称为`KPojo`，它是一个标记接口，用于标记一个类为Kronos数据表类进行泛型操作。同时，kronos会在编译期为类添加一些属性和方法，以便高效地读取数据表信息。

我们，推荐将类声明为`data class`并为其添加**默认值**，这样可以使数据类在ORM操作时更加灵活。

注意，`var`关键词声明的的属性才能在**update**或**upsert**的`set`中修改。

### 使用委托实现级联多对多跨中间表关系

级联功能中多对多关系中的目标属性通过**委托**方式来定义，需要放在class内声明，形如：

```kotlin
data class User(
    val id: Int? = null,
    val name: String? = null,
    // 多对多关系的中间表
    val relations: List<UserRoleRelation>? = emptyList()
) : KPojo {
    // 多对多关系的目标表
    var roles: List<Role>? by manyToMany(::UserRoleRelation)
}
```

### 使用委托代理序列化反序列化属性

Kronos的序列化反序列化功能支持自动处理和委托处理两种方式，委托处理方式需要在class内声明，形如：

```kotlin
data class User(
    val id: Int? = null,
    // 需要序列化反序列化的属性
    val listStr: String? = null
) : KPojo {
    // 代理序列化反序列化属性
    var list: List<String>? by serializable(::listStr)
}
```

## 使用注解设置表属性

在Kronos中，我们可以通过注解来设置表的属性，如：表名、列名、列类型、列长度、主键、自增、唯一键、索引、默认值、非空等。
Kronos还提供了级联、序列化/反序列化、日期格式化、逻辑删除、创建时间、更新时间、乐观锁等功能。

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
    @NotNull
    @ColumnType(VARCHAR, 128)
    var username: String? = null,

    // 设置列类型为TINYINT
    // 设置默认值为0
    @ColumnType(TINYINT)
    @Default("0")
    var age: Int? = null,

    // 设置companyId字段为非空
    @NotNull
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