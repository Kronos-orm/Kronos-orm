{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 准备项目

添加 ORM 运行时、Kronos 编译插件、JDBC wrapper 和本示例使用的数据库驱动。

```kotlin group="Gradle" name="build.gradle.kts" icon="gradlekts"
plugins {
    kotlin("jvm") version "2.4.0"
    id("com.kotlinorm.kronos-gradle-plugin") version "{{ $.kronosVersion() }}"
}

dependencies {
    implementation("com.kotlinorm:kronos-core:{{ $.kronosVersion() }}")
    implementation("com.kotlinorm:kronos-jdbc-wrapper:{{ $.kronosVersion() }}")
    implementation("org.apache.commons:commons-dbcp2:<latest-stable>")
    implementation("com.mysql:mysql-connector-j:<latest-stable>")
}
```

下面的代码使用名为 `kronos` 的 MySQL 数据库。将 `<latest-stable>` 替换为当前 JDK 和数据库服务端可用的驱动、连接池稳定版本。

> **Note**
> 编译插件是可运行配置的一部分。它会生成 `KPojo` 元数据、动态成员和查询投影类型。完整配置见 {{ $.keyword("configuration/compiler-plugins", ["编译器插件"]) }}。

## 配置 wrapper

在执行 ORM 操作前创建一个 wrapper，并设置到 `Kronos.dataSource`。本示例把 wrapper 放在文件作用域，DDL 和 CRUD 代码都能复用同一个实例。

```kotlin group="Setup" name="Db.kt" icon="kotlin"
import com.kotlinorm.Kronos
import com.kotlinorm.wrappers.KronosJdbcWrapper
import com.kotlinorm.beans.config.KronosCommonStrategy
import com.kotlinorm.beans.dsl.Field
import org.apache.commons.dbcp2.BasicDataSource
import java.time.ZoneId

val wrapper by lazy {
    KronosJdbcWrapper(
        BasicDataSource().apply {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            url = "jdbc:mysql://localhost:3306/kronos?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC"
            username = "user"
            password = "******"
        }
    )
}

fun configureKronos() {
    with(Kronos) {
        dataSource = { wrapper }
        tableNamingStrategy = lineHumpNamingStrategy
        fieldNamingStrategy = lineHumpNamingStrategy
        timeZone = ZoneId.systemDefault()
        defaultDateFormat = "yyyy-MM-dd HH:mm:ss"
        createTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("create_time", "createTime"))
        updateTimeStrategy = KronosCommonStrategy(enabled = true, field = Field("update_time", "updateTime"))
        logicDeleteStrategy = KronosCommonStrategy(enabled = true, field = Field("deleted"))
        optimisticLockStrategy = KronosCommonStrategy(enabled = true, field = Field("version"))
    }
}
```

自定义 wrapper 和数据库类型识别见 {{ $.keyword("database/connect-to-db", ["连接到数据库"]) }} 和 {{ $.keyword("database/custom-wrapper", ["自定义 Wrapper"]) }}。

## 定义 KPojo 类

创建 Kotlin data class，实现 `KPojo`，并使用注解声明表名、主键、索引和通用字段。

```kotlin group="Model" name="Director.kt" icon="kotlin"
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

data class Director(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    var name: String? = null,
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false,
    @Version
    var version: Int? = 0
) : KPojo
```

```kotlin group="Model" name="Movie.kt" icon="kotlin"
import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.Column
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.interfaces.KPojo
import java.time.LocalDateTime

@Table("tb_movie")
@TableIndex("idx_movie_director", ["name", "director_id"], "UNIQUE", "BTREE")
data class Movie(
    @PrimaryKey(identity = true)
    var id: Int? = null,
    @Column("name")
    var name: String? = null,
    var directorId: Int? = null,
    @Cascade(["directorId"], ["id"])
    var director: Director? = null,
    @CreateTime
    var createTime: LocalDateTime? = null,
    @UpdateTime
    var updateTime: LocalDateTime? = null,
    @LogicDelete
    @Default("0") // @Default("false") for Postgres
    var deleted: Boolean? = false
) : KPojo
```

映射规则见 {{ $.keyword("mapping/kpojo", ["KPojo"]) }}、{{ $.keyword("mapping/annotations", ["注解"]) }} 和 {{ $.keyword("mapping/indexes", ["索引"]) }}。

## 运行第一条闭环

调用 `configureKronos()`，创建表，插入一个导演，再用 query-by-example 查询回来。

```kotlin group="Run 1" name="Main.kt" icon="kotlin"
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select

fun main() {
    configureKronos()

    wrapper.table.createTable(Director())
    wrapper.table.createTable(Movie())

    val result = Director(name = "Kronos")
        .insert()
        .execute()

    val affectedRows = result.affectedRows

    val director = Director(name = "Kronos")
        .select()
        .where()
        .first()
}
```

在 MySQL 下，插入和查询会生成类似 SQL。

```sql group="Run 2" name="MySQL" icon="mysql"
INSERT INTO `director` (`name`, `create_time`, `update_time`, `deleted`, `version`)
VALUES (:name, :createTime, :updateTime, :deleted, :version)

SELECT `id`, `name`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted`, `version`
FROM `director`
WHERE `director`.`name` = :name
  AND `deleted` = 0
```

DDL 用法见 {{ $.keyword("database/table-operations", ["表操作"]) }}，写入用法见 {{ $.keyword("mutation/insert", ["插入"]) }}，`where()` 行为见 {{ $.keyword("query/conditions", ["条件 DSL"]) }}。构建或运行结果与本流程不一致时，见 {{ $.keyword("resources/troubleshooting", ["故障排查"]) }}。
