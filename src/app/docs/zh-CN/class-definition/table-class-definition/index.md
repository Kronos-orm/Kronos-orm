{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

本文将指导您如何定义一个Kronos数据表类。

在Kronos中声明一个data class为数据表类非常简单，只需要让该类继承`KPojo`即可，以下是一个简单示例：

```kotlin
import com.kotlinorm.beans.dsl.KPojo

data class User(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null
) : KPojo
```

## 使用注解设置表属性

在Kronos中，我们可以通过注解来设置表的属性，如：表名、列名、列类型、列长度、主键、自增、唯一键、索引、默认值、非空等。
Kronos还提供了级联、序列化/反序列化、日期格式化、逻辑删除、创建时间、更新时间、乐观锁等注解用于加强ORM功能。

以下是一个示例，包含了一些常用的注解：

```kotlin group="KPojo" name="User.kt" icon="kotlin"
@Table(name = "tb_user") // 表名设置，如果不设置则将使用[表名策略]根据类名生成表名
@TableIndex("idx_username", ["name"], "UNIQUE") // 为name字段添加唯一键
@TableIndex(name = "idx_multi", columns = ["id", "name"], "UNIQUE") // 为id和name字段添加唯一键
data class User(
    @PrimaryKey(identity = true) // 设置id为主键，自增
    var id: Int? = null,
    @Column("name") // 设置列名为name，如果不设置则使用[列名策略]根据属性名生成列名
    @NotNull // 设置name字段为非空
    @ColumnType(VARCHAR, 128) // 设置列类型为VARCHAR，长度为128
    var username: String? = null,
    @ColumnType(TINYINT) // 设置列类型为TINYINT
    @Default("0") // 设置默认值为0
    var age: Int? = null,
    @CreateTime // 设置createTime为创建时间字段
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss") // 设置时间格式为字符串的格式
    var createTime: String? = null,
    @NotNull // 设置companyId字段为非空
    var companyId: Int? = null,
    @Reference(["companyId"], ["id"]) // 级联设置，无需实体外键，通过companyId关联Company表的id
    var company: Company? = null,
    @UpdateTime // 设置updateTime为更新时间字段
    var updateTime: LocalDateTime? = null,
    @LogicDelete // 设置deleted为逻辑删除字段
    var deleted: Boolean? = null
) : KPojo
```

```sql group="KPojo" name="Mysql" icon="mysql"
CREATE TABLE IF NOT EXISTS `tb_user`
(
    `id`
    INT
    NOT
    NULL
    PRIMARY
    KEY
    AUTO_INCREMENT,
    `name`
    VARCHAR
(
    255
) NOT NULL,
    `age` TINYINT DEFAULT 0,
    `create_time` VARCHAR
(
    255
),
    `companyId` INT NOT NULL,
    `update_time` DATETIME,
    `deleted` TINYINT
    );
CREATE UNIQUE INDEX idx_username ON `tb_user` (`name`) USING BTREE;
CREATE UNIQUE INDEX idx_multi ON `tb_user` (`id`, `name`) USING BTREE
```

```sql group="KPojo" name="Sqlite" icon="sqlite"
CREATE TABLE IF NOT EXISTS "tb_user"
(
    "id"
    INTEGER
    NOT
    NULL
    PRIMARY
    KEY
    AUTOINCREMENT,
    "name"
    TEXT
    NOT
    NULL,
    "age"
    INTEGER
    DEFAULT
    0,
    "create_time"
    TEXT,
    "companyId"
    INTEGER
    NOT
    NULL,
    "update_time"
    TEXT,
    "deleted"
    INTEGER
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX IF NOT EXISTS idx_multi ON "tb_user" ("id", "name");
```

```sql group="KPojo" name="PostgreSql" icon="postgres"
CREATE TABLE IF NOT EXISTS "tb_user"
(
    "id"
    SERIAL
    NOT
    NULL
    PRIMARY
    KEY,
    "name"
    VARCHAR
(
    255
) NOT NULL,
    "age" SMALLINT DEFAULT 0,
    "create_time" VARCHAR
(
    255
),
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
CREATE TABLE [dbo].[tb_user]
(
    [
    id]
    INT
    NOT
    NULL
    PRIMARY
    KEY
    IDENTITY, [
    name]
    VARCHAR
(
    255
) NOT NULL,
    [age] TINYINT DEFAULT 0,
    [create_time] VARCHAR
(
    255
),
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
    "name"        VARCHAR(255) NOT NULL,
    "age"         NUMBER(3) DEFAULT 0,
    "create_time" VARCHAR(255),
    "companyId"   NUMBER(11) NOT NULL,
    "update_time" DATE,
    "deleted"     NUMBER(3)
);
CREATE UNIQUE INDEX idx_username ON "tb_user" ("name");
CREATE UNIQUE INDEX idx_multi ON "tb_user" ("id", "name");
```

点击[这里](/documentation/zh-CN/class-definition/annotation-setting)查看更多注解设置。