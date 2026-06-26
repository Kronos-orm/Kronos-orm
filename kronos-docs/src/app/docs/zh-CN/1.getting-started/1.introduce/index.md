{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

# 什么是Kronos
Kronos 是为 Kotlin 设计的现代 ORM 框架，**适用于后台和移动应用程序，支持多数据库。功能强大、高性能、且简单易用**。

我们支持{{ $.noun("Code First") }}和{{ $.noun("Database First") }}两种模式，提供了**数据库表结构的自动创建、自动同步，以及对表结构、索引**和代码生成的支持。

基于编译器插件实现的表达式树分析支持以及Kotlin本身泛型、高阶函数、扩展函数等语法特性，让Kronos拥有**强大的表现力和简洁、语义化的写法**，**使操作数据库变得更加简单**。

```mermaid
graph LR
    A[Kronos] --> B[Kronos-core]
    B --> I[ORM for KPojo]
    I --> J[Query、Insert、Update、Delete，etc.]
    A --> C[Kronos-compiler-plugin]
    C --> L[Kronos-compiler-kotlin-plugin]
    C --> D[Kronos-maven-plugin]
    C --> E[Kronos-gradle-plugin]
    A --> F[Kronos-data-source-wrapper]
    F --> G[DataSource]
    G --> H[Mysql, Sqlite, Postgresql, Oracle, SqlServer, etc.]
    A --> K[Kronos-logging, Kronos-codegen and Other Plugins]
```

# 为什么使用Kronos

* 可以使用jvm平台的**全部生态和资源**，如**数据库驱动和日志框架**等，未来可能支持kotlin多平台
* **Kotlin 编译器插件** 和 **协程**驱动，**完全不依赖反射**，Kronos 为用户提供 **超高性能** 的数据库操作体验。
* 支持大多数**主流数据库**且支持自由地通过插件**添加数据库扩展**
* **书写简洁、富有表现力，支持 Kotlin 原生语法** `==`、`>`、`<`、`in` 等，而不是 .eq、.gt、.lt 等
* 强类型检查
* 支持**事务**、**无外键复杂级联操作（一对一，一对多，多对多）**、**序列化反序列化**、**跨数据库查询**、**数据库表/索引/备注创建和结构同步**等功能
* 支持**逻辑删除**、**乐观锁**、**创建时间**、**更新时间**，且支持灵活的自定义设置
* 轻松与任何第三方框架集成，如`Spring`、`Ktor`、`Vert.x`、`Solon`等，请查看我们的示例项目
* **基于命名参数的原生SQL数据库操作**
* 通过编译期操作，支持轻松将**数据实体类转换为Map或从Map转换为数据实体类**，并且**无反射，近乎零开销**
* 数据类就可以当作数据库表模型，**显著减少额外的类定义**

# 示例
> **Note**
> 以下是一个简单的示例。

```kotlin name="demo" icon="kotlin"
val wrapper = KronosBasicWrapper(
    MysqlDataSource("jdbc:mysql://localhost:3306/test")
)

Kronos.init {
    dataSource = { wrapper }
}

// 创建一个User对象
val user: User = User(
    id = 1,
    name = "Kronos",
    age = 18
)

 // 如果表不存在则创建表，否则同步表结构，包括表列、索引、备注等
dataSource.table.syncTable(user)

// 插入数据
user.insert().execute()

// 根据id更新name字段
user.update().set { it.name = "Kronos ORM" }.by{ it.id }.execute()
// 或
user.update{ it.name }.by{ it.id }.execute()

// 根据对象值动态查询记录
val name: User = user.select().queryOne()

// 根据id查询name字段
val name: String = user.select{ it.name }.where{ it.id == 1 }.queryOne<String>()

// 删除id为1的数据
User().delete().where{ it.id == 1 }.execute()
// 或
User(1).delete().execute()
```

{{ NgDocActions.demo("FeatureCardsComponent", {container: false}) }}
