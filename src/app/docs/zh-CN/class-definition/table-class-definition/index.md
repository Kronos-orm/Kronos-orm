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

## 全局配置项

### 全局表名策略

| 参数名                   | 类型                     | 默认值                  |
|-----------------------|------------------------|----------------------|
| `tableNamingStrategy` | `KronosNamingStrategy` | `NoneNamingStrategy` |

通过创建`KronosNamingStrategy`的实现类来自定义表名策略（详见：[命名策略](/documentation/class-definition/naming-strategy)），然后在配置文件中指定该实现类。

我们默认提供了`LineHumpNamingStrategy`表名策略：

该策略将kotlin类名转换为下划线分隔的小写字符串，如：`ADataClass` -> `a_data_class`，将数据库表/列名转为驼峰命名，如：`user_name` -> `userName`。

```kotlin
Kronos.tableNamingStrategy = LineHumpNamingStrategy
```

### 全局列名策略

| 参数名                   | 类型                     | 默认值                  |
|-----------------------|------------------------|----------------------|
| `fieldNamingStrategy` | `KronosNamingStrategy` | `NoneNamingStrategy` |

通过创建`KronosNamingStrategy`的实现类来自定义表名策略（详见：[命名策略](/documentation/class-definition/naming-strategy)），然后在配置文件中指定该实现类。

我们默认提供了`LineHumpNamingStrategy`表名策略：

该策略将kotlin类名转换为下划线分隔的小写字符串，如：`ADataClass` -> `a_data_class`，将数据库表/列名转为驼峰命名，如：`user_name` -> `userName`。

```kotlin
Kronos.fieldNamingStrategy = LineHumpNamingStrategy
```

### 创建时间策略

| 参数名                  | 类型                     | 默认值                                                        |
|----------------------|------------------------|------------------------------------------------------------|
| `createTimeStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "create_time", "createTime")` |

通过创建`KronosCommonStrategy`的实现类来自定义创建时间策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

创建时间策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.createTimeStrategy = KronosCommonStrategy(true, Field("create_time", "createTime"))
```

全局设置创建时间策略后，仍可在`KPojo`类中通过`@CreateTime`注解覆盖全局设置。

### 更新时间策略

| 参数名                  | 类型                     | 默认值                                                        |
|----------------------|------------------------|------------------------------------------------------------|
| `updateTimeStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "update_time", "updateTime")` |

通过创建`KronosCommonStrategy`的实现类来自定义更新时间策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

更新时间策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.updateTimeStrategy = KronosCommonStrategy(true, Field("update_time", "updateTime"))
```

全局设置逻更新时间策略后，仍可在`KPojo`类中通过`@UpdateTime`注解覆盖全局设置。

### 逻辑删除策略

| 参数名                   | 类型                     | 默认值                                      |
|-----------------------|------------------------|------------------------------------------|
| `logicDeleteStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "deleted")` |

通过创建`KronosCommonStrategy`的实现类来自定义逻辑删除策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

逻辑删除策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.logicDeleteStrategy = KronosCommonStrategy(true, Field("deleted"))
```

全局设置逻辑删除策略后，仍可在`KPojo`类中通过`@LogicDelete`注解覆盖全局设置。

### 乐观锁策略

| 参数名                   | 类型                     | 默认值                                      |
|-----------------------    |------------------------|------------------------------------------|
| `optimisticLockStrategy` | `KronosCommonStrategy` | `KronosCommonStrategy(false, "version")` |

通过创建`KronosCommonStrategy`的实现类来自定义乐观锁策略（详见：[通用策略](/documentation/class-definition/common-strategy)），然后在配置文件中指定该实现类。

也可通过<a href="/documentation/class-definition/table-class-definition#列乐观锁">[列乐观锁]</a>对每一个实体对象单独配置

乐观锁策略的全局默认关闭，需要手动开启。

```kotlin
Kronos.optimisticLockStrategy = KronosCommonStrategy(true, Field("version"))
```

全局设置乐观锁策略后，仍可在`KPojo`类中通过`@Version`注解覆盖全局设置。

### 默认日期/时间格式

| 参数名                 | 类型       | 默认值                   |
|---------------------|----------|-----------------------|
| `defaultDateFormat` | `String` | `yyyy-MM-dd HH:mm:ss` |

Kronos默认使用`yyyy-MM-dd HH:mm:ss`格式化日期/时间，你可以通过以下方式修改默认格式：

```kotlin
Kronos.defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
```

### 默认时区

| 参数名               | 类型                          | 默认值                      |
|-------------------|-----------------------------|--------------------------|
| `defaultTimeZone` | `kotlinx.datetime.TimeZone` | `currentSystemDefault()` |

Kronos默认使用当前系统时区，你可以通过以下方式修改默认时区：

```kotlin
Kronos.defaultTimeZone = TimeZone.UTC
Kronos.defaultTimeZone = TimeZone.of("Asia/Shanghai")
Kronos.defaultTimeZone = TimeZone.currentSystemDefault()
Kronos.defaultTimeZone = TimeZone.of("GMT+8")
```

### 序列化解析器

| 参数名                 | 类型                        | 默认值                     |
|---------------------|---------------------------|-------------------------|
| `serializeResolver` | `KronosSerializeResolver` | `NoneSerializeResolver` |

通过创建`KronosSerializeResolver`的实现类来自定义序列化解析器（详见：[序列化解析器](/documentation/class-definition/serialize-resolver)），然后在配置文件中指定该实现类。

如可以通过引入`GSON`库来实现序列化解析器：

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

这里我们使用`GSON`库来实现序列化解析器，你可以使用任何库如`Kotlinx.serialization`、`Jackson`、`Moshi`、`FastJson`等。

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

### 列关联设置

`@Reference(reference: String[], target: String[], onDelete: CascadeDeleteAction, defaultValue: String, mapperBy: String)`

此注解用于声明列关联，包括关联查询、关联插入、关联更新、关联删除等。支持一对一、一对多、多对一、多对多关联。

kronos将关联列视为自定义属性，不会将其识别为数据库字段。

kronos的关联功能**无需定义外键**，只需在实体类中定义关联关系即可实现关联查询、关联插入、关联更新、关联删除等操作。

**参数**：

- referenceFields `Array<String>`：关联属性名
- targetFields `Array<String>`：关联目标表属性名
- onDelete `CascadeDeleteAction`：关联删除策略（可选，默认为无操作）
- defaultValue `Array<String>`：指定级联删除方式为"SET DEFAULT"时设置的默认值（可选）
- mapperBy `Array<String>`：用于指定本关联关系的维护端（为空时表示维护端为本实体，若两端都有该注解时不能为空）
- usage `Array<KOperationType>`: 用于声明本实体需要用到的关联操作（可选，默认为`[Insert, Update, Delete, Upsert, Select]`）

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
