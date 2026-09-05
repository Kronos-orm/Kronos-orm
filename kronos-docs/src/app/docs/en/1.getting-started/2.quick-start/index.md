{% import "../../../macros/macros-en.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## Prepare the project

Add the ORM runtime, the Kronos compiler plugin, the JDBC wrapper, and the database driver used by this example.

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

Use a MySQL database named `kronos` for the following snippets. Replace `<latest-stable>` with driver and pool versions that match your JDK and database server.

> **Note**
> The compiler plugin is part of the runnable setup. It generates `KPojo` metadata, dynamic members, and query projection types. The full setup is documented in {{ $.keyword("configuration/compiler-plugins", ["Compiler Plugins"]) }}.

## Configure the wrapper

Create one wrapper and set it on `Kronos.dataSource` before running ORM operations. This example keeps the wrapper at file scope so the same instance is available to DDL and CRUD code.

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

For custom wrappers and database detection, see {{ $.keyword("database/connect-to-db", ["Connect to DB"]) }} and {{ $.keyword("database/custom-wrapper", ["Custom Wrapper"]) }}.

## Define KPojo classes

Create Kotlin data classes, implement `KPojo`, and add annotations for table names, keys, indexes, and common fields.

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

Mapping rules are covered in {{ $.keyword("mapping/kpojo", ["KPojo"]) }}, {{ $.keyword("mapping/annotations", ["Annotations"]) }}, and {{ $.keyword("mapping/indexes", ["Indexes"]) }}.

## Run the first flow

Call `configureKronos()`, create the tables, insert a director, and read it back with query-by-example.

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

The insert and select generate SQL like this for MySQL.

```sql group="Run 2" name="MySQL" icon="mysql"
INSERT INTO `director` (`name`, `create_time`, `update_time`, `deleted`, `version`)
VALUES (:name, :createTime, :updateTime, :deleted, :version)

SELECT `id`, `name`, `create_time` AS `createTime`, `update_time` AS `updateTime`, `deleted`, `version`
FROM `director`
WHERE `director`.`name` = :name
  AND `deleted` = 0
```

Use {{ $.keyword("database/table-operations", ["Table Operations"]) }} for DDL, {{ $.keyword("mutation/insert", ["Insert"]) }} for writes, {{ $.keyword("query/conditions", ["Conditions"]) }} for `where()` behavior, and {{ $.keyword("resources/troubleshooting", ["Troubleshooting"]) }} when build or runtime behavior differs from this flow.
