{% import "../../../macros/macros-zh-CN.njk" as $ %}

## 连接 Native 应用

共享 `KPojo` 模型和查询通过应用内的 SQLite 连接执行。Gradle 配置见 {{ $.keyword("getting-started/kmp", ["Kotlin Multiplatform"]) }}。

[Native 参考应用](https://github.com/Kronos-orm/kronos-example-ios) 包含连接适配器、平台驱动、桌面可执行程序和 iOS 应用。其中 `KronosSqliteDataSource` 实现 `KronosExecutionDataSource`，由 core 中的 `KronosExecutionWrapper` 接入 ORM。

Apple 和 Linux 使用 AndroidX bundled SQLite，Windows 使用示例中的 SQLite C API 驱动。两种驱动执行同一组 ORM 和连接测试。驱动创建、SQLite 原生源码和连接生命周期由应用管理。

## 创建连接

将参考适配器和驱动 source set 放入应用，或从参考项目开始。项目中已有 `KronosSqliteDataSource` 后，初始化 Kronos 并打开数据库。

```kotlin name="nativeMain/Database.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.example.ios.KronosSqliteDataSource
import com.kotlinorm.utils.initializeKronosMetadata
import com.kotlinorm.wrappers.KronosExecutionWrapper

val source = KronosSqliteDataSource(databaseName = ":memory:")
val wrapper = KronosExecutionWrapper(source)

fun openDatabase() {
    initializeKronosMetadata()
    source.open()
    Kronos.dataSource = { wrapper }
}

fun closeDatabase() {
    source.close()
}
```

`:memory:` 数据库随连接存活。需要持久化时，将应用可写数据目录中的文件路径传给 `databaseName`。

## 建表、写入和查询

以下操作使用 KMP 指南中的 `Person` 模型，并复用同一个连接。

```kotlin name="nativeMain/People.kt" icon="kotlin"
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import com.kotlinorm.orm.update.update

wrapper.table.createTable(Person())
val inserted = Person(name = "Ada", age = 28).insert().withId().execute()
val saved = Person().select().where { it.name == "Ada" }.first()

Person(id = saved.id, name = "Grace")
    .update { it.name }
    .by { it.id }
    .execute()

val people = Person().select { [it.id, it.name] }.toList()
Person(id = saved.id).delete().by { it.id }.execute()
```

调用 `withId()` 请求生成键。`inserted.lastInsertId` 来自执行插入的同一个连接和执行过程。没有影响任何行的写入不返回生成键。

## 事务与保存点

通过 `Kronos.transact` 使用 SQLite 默认事务设置。抛出异常时事务回滚；保存点用于撤销事务中的部分操作。

```kotlin name="nativeMain/Transactions.kt" icon="kotlin"
Kronos.transact(wrapper = wrapper) {
    Person(name = "Keep", age = 30).insert().execute()
    val checkpoint = savepoint("optional_person")
    Person(name = "Discard", age = 20).insert().execute()
    rollbackToSavepoint(checkpoint)
    releaseSavepoint(checkpoint)
}
```

事务 scope 和保存点 token 在所属事务回调内使用。行数据在 mapper 回调内读取，并将映射后的业务值返回给调用方。

## 时间字段

共享模型使用 `kotlin.time.Instant` 或 `kotlinx.datetime.LocalDateTime`。读取 SQLite `CURRENT_TIMESTAMP` 生成的 `yyyy-MM-dd HH:mm:ss` 文本时，通过 `Kronos.timeZone` 指定无时区文本的时区；SQLite 的这个默认值使用 UTC。

```kotlin name="commonMain/Event.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.orm.select.select
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.Table
import com.kotlinorm.interfaces.KPojo
import kotlin.time.Instant

@Table("events")
data class Event(
    @Column("created_at") var createdAt: Instant? = null,
) : KPojo

fun readEvent(): Event {
    Kronos.timeZone = "UTC"
    return Event().select().first()
}
```

```sql name="SQLite" icon="sqlite"
CREATE TABLE events(created_at TEXT DEFAULT CURRENT_TIMESTAMP);
INSERT INTO events DEFAULT VALUES;
```

ISO 时间文本可直接读取。字段的 `@DateTimeFormat` 和 `Kronos.defaultDateFormat` 用于配置格式的文本转换；显式指定字段格式时，写入也使用该格式。

## 连接生命周期与并发

为每个数据库所属对象创建一个数据源。参考适配器串行访问连接，并将生成键读取和对应写入放在一起。事务在其所属线程执行。

`close()` 释放连接和 statement。仍在进行的事务会回滚，其 scope 随之失效。`reopen()` 显式创建新连接；重新打开内存数据库会得到空数据库。

SQL 语法、约束和锁冲突等语句错误会关闭当前 statement，并保留可用连接与已有数据。事务中的驱动错误会使整个事务中止并回滚；即使回调捕获了异常，也不能继续写入或提交该事务。连接损坏或未知驱动故障会进入 `FAILED` 状态，需显式调用 `reopen()`。

## 运行示例

示例提供桌面可执行程序和 SwiftUI 应用。按宿主平台执行对应的 Gradle 命令：

```bash name="Apple Silicon macOS" icon="terminal"
./gradlew :shared:macosArm64Test :shared:runDebugExecutableMacosArm64
```

```bash name="Intel macOS" icon="terminal"
./gradlew :shared:macosX64Test :shared:runDebugExecutableMacosX64
```

```bash name="Linux x64" icon="terminal"
./gradlew :shared:linuxX64Test :shared:runDebugExecutableLinuxX64
```

```bash name="Windows x64" icon="terminal"
./gradlew :shared:mingwX64Test :shared:runDebugExecutableMingwX64
```

Windows 构建需要将 MinGW GCC 和 `ar` 加入 `PATH`。iOS 应用通过 XcodeGen 根据 `iosApp/project.yml` 生成 Xcode 项目，使用 `KronosIosApp` scheme 运行应用和 UI 测试。

仓库的 [KMP 工作流](https://github.com/Kronos-orm/Kronos-orm/actions/workflows/kronos-kmp-testing.yml) 会在 Apple、Linux 和 Windows 上使用当前 ORM 源码运行参考应用。Linux ARM64 测试程序在 x64 上完成链接，再到 ARM64 runner 执行。
